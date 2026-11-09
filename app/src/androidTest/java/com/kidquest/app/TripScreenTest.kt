// 규제 화면의 멈춤 이유와 끄기 유지, 정박 확인, 좁은 결과 화면·다시 하기를 검증한다.
package com.kidquest.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.kidquest.app.screen.KidQuestScreen
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TripScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun stoppedTripAndEndingSurviveResize() {
        lateinit var model: AppViewModel
        val width = mutableStateOf(900)
        compose.runOnIdle {
            model = ViewModelProvider(compose.activity)[AppViewModel::class.java]

        }
        compose.waitUntil(10_000) { model.connectionsReady }
        compose.runOnIdle { model.controller.stop() } // 연결 준비가 끝난 뒤 실제 입력을 끊는다.
        compose.activity.runOnUiThread {
            compose.activity.setContent { Box(Modifier.requiredSize(width.value.dp, 800.dp)) { KidQuestScreen(model) } }
        }
        fun send(vararg events: AppEvent) {
            compose.runOnIdle { events.forEach(model.controller::dispatch) }
            compose.waitForIdle()
        }
        send(AppEvent.AudioFocusChanged(true), AppEvent.GearChanged(Gear.P))
        send(AppEvent.StartPressed, AppEvent.SeatOccupied(RearSeat.LEFT, true),
            AppEvent.BeltBuckled(RearSeat.LEFT, true), AppEvent.ParentConfirmedBelt, AppEvent.GearChanged(Gear.D), AppEvent.DrivingRestricted(true))
        compose.onNodeWithText("소리 끄기").performClick()
        compose.waitUntil { model.controller.vehicle.parentStopped }
        compose.runOnIdle { assertEquals("중단 화면", Screen.PAUSED, model.controller.screen) }
        val stoppedReason = model.controller.status!!
        compose.onNodeWithTag("restricted-status").assertTextEquals(stoppedReason)
        compose.onNodeWithTag("voice-status").assertDoesNotExist()
        compose.onNodeWithTag("journey-board").assertDoesNotExist()
        compose.runOnIdle { width.value = 360 }
        compose.onNodeWithText("다시 켜기").performScrollTo().assertIsDisplayed()
        send(AppEvent.AudioFocusChanged(false), AppEvent.BeltBuckled(RearSeat.LEFT, false),
            AppEvent.AudioFocusChanged(true), AppEvent.BeltBuckled(RearSeat.LEFT, true))
        compose.runOnIdle { assertTrue(model.controller.vehicle.parentStopped); assertEquals(Screen.PAUSED, model.controller.screen) }
        send(AppEvent.GearChanged(Gear.P), AppEvent.DrivingRestricted(false))
        compose.onNodeWithText("카시트·벨트 직접 확인 (부모용)").performScrollTo().performClick()
        compose.waitUntil { model.controller.vehicle.parentConfirmed }
        compose.onNodeWithText("여기서 탐험 마치기").performScrollTo().performClick()
        compose.onNodeWithTag("engine-trip-ending").assertExists()
        compose.onNodeWithTag("trip-treasure-art").performScrollTo().assertIsDisplayed()
        capture("trip-empty-narrow.png")
        compose.onNodeWithText("오늘 모은 보물 0개").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { model.controller.items.add(Command.AwardItem("sample", "시험 보물")); width.value = 628 }
        capture("trip-result-narrow.png")
        compose.onNodeWithText("✦ 시험 보물").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("새로운 탐험 준비하기").performScrollTo().performClick()
        compose.waitUntil { model.controller.screen == Screen.START }
        compose.runOnIdle { assertTrue(model.controller.items.isEmpty()); assertFalse(model.controller.vehicle.parentStopped) }
        compose.runOnIdle {
            model.openDemoMission(model.selectedRoute.missions.first())
            model.toggleMissionAudio()
            model.readMission()
            model.beginVoiceQuiz()
            assertTrue(model.missionAudioMuted)
        }
        send(AppEvent.DrivingRestricted(true))
        compose.waitUntil { model.bonusSuspended }
        compose.runOnIdle { assertTrue(model.controller.vehicle.parentStopped) }
        send(AppEvent.DrivingRestricted(false))
        compose.waitUntil { !model.bonusSuspended }
        compose.runOnIdle { model.closeDemoMission() }
        compose.waitUntil { model.demoMission == null }
        compose.runOnIdle { assertTrue(model.controller.vehicle.parentStopped) }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val test = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val screenshot = test.uiAutomation.takeScreenshot()
        java.io.File(compose.activity.cacheDir, name).outputStream().use {
            screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
