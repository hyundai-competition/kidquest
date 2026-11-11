// 일반 탐험의 설정·승무원 확인을 검증한다. 시연 선택 없이 가상 탑승·D 입력을 만들지 않아야 한다.
package com.kidquest.app

import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.Screen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class StartScreenConnectionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun actualJourneyWaitsForRealSignalsWithoutDeveloperControls() {
        lateinit var model: AppViewModel
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        compose.waitUntil(20_000) { model.connectionsReady && model.controller.vehicle.gear == Gear.P }
        compose.onNodeWithTag("select-live").assertIsSelected()
        compose.onNodeWithTag("select-showcase").assertIsNotSelected()
        compose.onNodeWithText("재생 모드").assertDoesNotExist()
        compose.onNodeWithTag("developer-panel").assertDoesNotExist()
        compose.onNodeWithText("한국어").performScrollTo().performClick()
        compose.onNodeWithText("경로 고르기 →").performScrollTo().performClick()
        compose.onNodeWithText("서울 → 경주").performScrollTo().performClick()
        compose.onNodeWithText("테마 고르기 →").performScrollTo().performClick()
        compose.onNodeWithText("탐험 시작하기 →").performScrollTo().performClick()
        compose.waitUntil { model.controller.screen == Screen.CREW_CHECK }
        SystemClock.sleep(1500)
        compose.runOnIdle {
            assertFalse(model.showcase.active)
            assertFalse(model.controller.replayUsed)
            assertEquals(Gear.P, model.controller.vehicle.gear)
            assertEquals(Screen.CREW_CHECK, model.controller.screen)
        }
        compose.onNodeWithTag("showcase-banner").assertDoesNotExist()
    }
}
