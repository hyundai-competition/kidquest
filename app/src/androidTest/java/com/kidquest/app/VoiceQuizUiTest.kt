// 시연 카드 방식에서 음성 퀴즈 선택과 건너뛰기를 확인한다. 터치 대체 입력이며 실제 발화 성공 시험은 아니다.
package com.kidquest.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.core.contract.AppEvent
import com.kidquest.app.voice.VoicePhase
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class VoiceQuizUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun voiceAlternativeAndSkipDoNotInventRewards() {
        lateinit var model: AppViewModel
        compose.runOnIdle {
            model = ViewModelProvider(compose.activity)[AppViewModel::class.java]
            model.openDemoMission(model.selectedRoute.missions.first())
            model.stopNarration()
        }
        compose.onNodeWithText("소리로 퀴즈 풀기").performScrollTo().performClick()
        compose.onNodeWithText(model.missionSession!!.quiz.question).assertExists()
        compose.onNodeWithTag("voice-status").assertExists()
        val answer = model.missionSession!!.quiz.answer
        compose.onNodeWithText("${answer + 1}번 · ${model.missionSession!!.quiz.options[answer]}").performScrollTo().performClick()
        compose.onNodeWithTag("mission-success").assertExists()
        compose.onNodeWithText("지도로 돌아가기").performClick()
        compose.runOnIdle {
            assertEquals(1, model.completedDemoMissions.size)
            model.openDemoMission(model.selectedRoute.missions[1])
            model.stopNarration()
        }
        compose.onNodeWithText("이번 미션 건너뛰기").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(setOf("첨성대"), model.skippedDemoMissions)
            assertEquals(1, model.completedDemoMissions.size)
            model.selectedRoute.missions.drop(2).forEach { model.openDemoMission(it); model.skipCurrentMission() }
            assertTrue(model.journeyReadyToEnd)
            model.showJourneyEnding()
        }
        compose.onNodeWithTag("journey-ending").assertExists()
        compose.onNodeWithText("오늘의 보물 탐험을 마쳐요").assertExists()
        compose.runOnIdle { model.returnToCompletedMap(); model.openDemoMission(model.selectedRoute.missions[1]); model.beginMission() }
        compose.runOnIdle { model.controller.replay.send(AppEvent.DrivingRestricted(true)) }
        compose.waitUntil { model.controller.vehicle.restricted && model.missionSession?.voiceMode == true }
        compose.onNodeWithTag("mission-page").assertDoesNotExist()
        compose.onNodeWithText("퍼즐은 주차·휴식 중에 이어갈 수 있어요").assertExists()
        compose.runOnIdle { model.controller.replay.send(AppEvent.DrivingRestricted(false)) }
        compose.waitUntil { !model.controller.vehicle.restricted }
        compose.onNodeWithText(model.missionSession!!.quiz.question).assertExists()
        compose.runOnIdle {
            model.stopNarration()
            model.setListeningConsent(true)
            model.listenAgain()
        }
        compose.waitUntil(12_000) { model.controller.voiceUi.phase == VoicePhase.LISTENING || model.controller.voiceUi.phase == VoicePhase.ERROR }
        compose.runOnIdle { assertEquals(VoicePhase.LISTENING, model.controller.voiceUi.phase) }
        capture()
        compose.runOnIdle { model.stopNarration() }
    }
    private fun capture() {
        compose.waitForIdle()
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.cacheDir, "voice-quiz-ui.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
