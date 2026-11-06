// 에뮬레이터 화면에서 시작 버튼과 엔진 화면 전환을 확인한다. 이동 신호는 명시적인 재생 모드로 넣는다.
package com.kidquest.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class StartScreenConnectionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun settingsThenCrewThenEngineFlight() {
        lateinit var model: AppViewModel
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        // 실제 수집기 값을 먼저 보관해 시험 보고서에 남긴다. 재생 입력과 섞어 성공이라 하지 않는다.
        compose.waitUntil(10_000) {
            model.controller.vehicle.gear != null || CarSignal.GEAR in model.controller.vehicle.unavailable
        }
        compose.runOnIdle {
            val vehicle = model.controller.vehicle
            val report = "gear=${vehicle.gear}; occupied=${vehicle.occupied}; belts=${vehicle.buckled}; unavailable=${vehicle.unavailable}"
            android.util.Log.i("KQScreenTest", report)
            File(compose.activity.getExternalFilesDir(null), "vehicle-check.txt").writeText(report)
        }
        compose.onNodeWithText("미취학").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("시험 목적지").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("기본 이야기").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("승무원 확인하기 →").performScrollTo().performClick()
        compose.onNodeWithText("승무원 확인").assertExists()
        compose.onNodeWithText("재생 모드").performScrollTo().performClick()
        compose.onNodeWithText("탑승").assertExists()
        compose.onNodeWithText("재생 모드 접기").performScrollTo().performClick()
        capture("crew-check.png")
        compose.runOnIdle {
            RearSeat.entries.forEach { seat ->
                model.controller.replay.send(AppEvent.SeatOccupied(seat, true))
                model.controller.replay.send(AppEvent.BeltBuckled(seat, true))
            }
            model.controller.replay.send(AppEvent.GearChanged(Gear.D))
        }
        compose.waitUntil(5_000) { model.controller.screen == Screen.FLYING }
        compose.onNodeWithText("비행 중", substring = true).assertExists()
        capture("flight-check.png")
        compose.runOnIdle {
            assertTrue(model.controller.replayUsed)
            model.controller.replay.send(AppEvent.BeltBuckled(RearSeat.LEFT, false))
        }
        compose.waitUntil(5_000) { model.controller.screen == Screen.PAUSED }
        compose.onNodeWithText("잠시 멈췄어요").assertExists()
    }

    private fun capture(name: String) {
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.getExternalFilesDir(null), name).outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
