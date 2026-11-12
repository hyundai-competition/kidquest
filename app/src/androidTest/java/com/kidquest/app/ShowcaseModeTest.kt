// 실제 화면의 시연 진입·일시정지·종료와 선택형 ON_DEVICE TTS 한 판을 확인한다.
package com.kidquest.app

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class ShowcaseModeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var model: AppViewModel
    private fun prepare() {
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        compose.waitUntil(20_000) { model.connectionsReady && model.controller.vehicle.gear == Gear.P }
        compose.runOnIdle { model.restartTrip() }
        compose.waitUntil { model.controller.screen == Screen.START }
    }
    private fun begin() {
        compose.onNodeWithText("재생 모드").assertDoesNotExist()
        compose.onNodeWithTag("developer-panel").assertDoesNotExist()
        compose.onNodeWithTag("select-showcase").performScrollTo().performClick()
        compose.onNodeWithText("경로 고르기 →").performScrollTo().performClick()
        compose.onNodeWithText("테마 고르기 →").performScrollTo().performClick()
        compose.onNodeWithText("시연 시작 →").performScrollTo().performClick()
        compose.waitUntil { model.showcase.active }
        compose.onNodeWithTag("showcase-banner").assertIsDisplayed()
    }
    @Test fun controlsPauseAndReturnToLiveSetupWithoutKeepingFakeBelts() {
        prepare(); begin()
        compose.onNodeWithTag("showcase-pause").performClick()
        compose.waitUntil { model.showcase.paused }
        compose.onNodeWithTag("showcase-next").assertIsNotEnabled()
        capture("showcase-paused.png")
        compose.onNodeWithTag("showcase-pause").performClick()
        compose.waitUntil { !model.showcase.paused }
        compose.onNodeWithTag("showcase-next").performClick()
        compose.onNodeWithTag("showcase-exit").performClick()
        compose.onNodeWithText("계속 탐험하기").performClick()
        compose.runOnIdle { assertTrue(model.showcase.active) }
        compose.onNodeWithTag("showcase-exit").performClick()
        compose.onNodeWithTag("confirm-showcase-exit").performClick()
        compose.waitUntil { !model.showcase.active && model.controller.screen == Screen.START }
        compose.onNodeWithTag("showcase-banner").assertDoesNotExist()
        compose.runOnIdle { assertFalse(model.consent); assertFalse(model.controller.replayUsed); assertEquals(Gear.P, model.controller.vehicle.gear) }
    }
    @Test fun automaticTripWithActualOnDeviceNarration() {
        assumeTrue("긴 음성 시연은 -e verifyShowcase true로 실행", InstrumentationRegistry.getArguments().getString("verifyShowcase") == "true")
        prepare()
        compose.runOnIdle { model.selectDestination("gyeongju"); model.setListeningConsent(false) }
        begin()
        val deadline = SystemClock.elapsedRealtime() + 480_000
        var captured = false
        var questionShown = false
        var resumed = false
        while (SystemClock.elapsedRealtime() < deadline && !model.showcase.finished) {
            compose.runOnIdle {
                File(model.getApplication<android.app.Application>().cacheDir, "showcase-trip.txt").writeText(
                    "screen=${model.controller.screen} mission=${model.controller.presentation.mission?.id} stage=${model.controller.presentation.stage} items=${model.controller.items.size} voice=${model.controller.voiceStatus} finished=${model.showcase.finished}")
            }
            if (!captured && model.controller.vehicle.wiperOn) { capture("showcase-rain.png"); captured = true }
            if (!resumed && model.controller.presentation.stage == "briefing" &&
                model.controller.voiceUi.phase == com.kidquest.app.voice.VoicePhase.SPEAKING) {
                compose.onNodeWithTag("showcase-pause").performClick()
                compose.waitUntil { model.showcase.paused }
                compose.onNodeWithTag("showcase-next").assertIsNotEnabled()
                SystemClock.sleep(1200)
                compose.onNodeWithTag("showcase-pause").performClick()
                compose.waitUntil { !model.showcase.paused }
                resumed = true
            }
            if (!questionShown && model.controller.presentation.stage == "question" && model.controller.presentation.answerReady) {
                compose.onNodeWithTag("showcase-caption").assertIsDisplayed()
                compose.onNodeWithTag("showcase-answer-0").assertIsDisplayed().assertIsEnabled()
                compose.onNodeWithTag("voice-status").assertDoesNotExist()
                capture("showcase-question.png"); questionShown = true
            }
            SystemClock.sleep(1000)
        }
        compose.runOnIdle {
            assertTrue("시연 완주 시간 초과", model.showcase.finished)
            assertEquals(Screen.LANDED, model.controller.screen)
            assertEquals(model.plannedMissions.size, model.controller.items.size)
            assertEquals("landed", model.controller.lastFinishedLine)
            assertTrue(model.controller.replayUsed)
            assertFalse(model.consent)
            assertTrue(questionShown)
            assertTrue(resumed)
            File(model.getApplication<android.app.Application>().cacheDir, "showcase-trip.txt").writeText(
                "screen=${model.controller.screen} items=${model.controller.items.size} voice=${model.controller.voiceStatus} finished=${model.showcase.finished}")
        }
        capture("showcase-complete.png")
        compose.runOnIdle { model.restartTrip() }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val test = InstrumentationRegistry.getInstrumentation()
        File(test.targetContext.cacheDir, name).outputStream().use {
            test.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
