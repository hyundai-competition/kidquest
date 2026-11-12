// 단일 대사 표시·마이크 동의 없는 화면 정오답과 P 전용 놀이터의 자동 닫힘을 검증한다.
package com.kidquest.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GameplayFixesTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var model: AppViewModel
    private fun prepare() {
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        compose.waitUntil(10_000) { model.connectionsReady }
        compose.runOnIdle { model.controller.stop() }
        send(AppEvent.AudioFocusChanged(true), AppEvent.GearChanged(Gear.P))
        compose.runOnIdle { model.startTrip() }
        send(AppEvent.SeatOccupied(RearSeat.LEFT, true), AppEvent.BeltBuckled(RearSeat.LEFT, true),
            AppEvent.ParentConfirmedBelt, AppEvent.GearChanged(Gear.D))
        compose.waitUntil { model.controller.screen == Screen.FLYING }
    }
    private fun send(vararg events: AppEvent) {
        compose.runOnIdle { events.forEach(model.controller::dispatch) }
        compose.waitForIdle()
    }
    @Test fun captionAndScreenAnswerUseTheRightOutcomeWithoutMicrophoneConsent() {
        prepare()
        send(AppEvent.LineFinished("take_off"), AppEvent.LineFinished("departure"), AppEvent.LineFinished("rain_map/briefing"),
            AppEvent.WiperChanged(true), AppEvent.Tick(1000), AppEvent.Tick(5000), AppEvent.LineFinished("rain_map/event"))
        capture("main-single-caption.png")
        val question = model.episode!!.missions.first { it.id == "rain_map" }.question
        compose.onAllNodesWithText(question, useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithText("1번 · 파란색").performScrollTo().assertIsNotEnabled()
        send(AppEvent.LineFinished("rain_map/question"))
        compose.onNodeWithText("1번 · 파란색").performScrollTo().performClick()
        compose.waitUntil { model.controller.presentation.stage == "answer" }
        compose.runOnIdle {
            assertFalse(model.consent)
            assertEquals(model.episode!!.missions.first { it.id == "rain_map" }.correctLine, model.controller.presentation.caption)
        }
        send(AppEvent.LineFinished("rain_map/answer"), AppEvent.LineFinished("crossroads/briefing"),
            AppEvent.NextTurnDistance(200), AppEvent.NextTurnDistance(1500), AppEvent.LineFinished("crossroads/event"), AppEvent.LineFinished("crossroads/question"))
        compose.onNodeWithText("1번 · 하트").performScrollTo().performClick()
        compose.waitUntil { model.controller.presentation.stage == "answer" }
        compose.runOnIdle { assertEquals(model.episode!!.missions.first { it.id == "crossroads" }.wrongLine, model.controller.presentation.caption) }
    }
    @Test fun miniGamesOnlyOpenInParkAndGearChangesReleaseTheirPause() {
        prepare()
        compose.runOnIdle { model.openMiniGames(); model.openDemoMission(model.selectedRoute.missions.first()); assertNull(model.demoMission) }
        send(AppEvent.GearChanged(Gear.P))
        compose.onNodeWithTag("open-mini-games").performScrollTo().performClick()
        compose.onNodeWithTag("mini-games-page").assertExists()
        capture("mini-games-hub.png")
        compose.onNodeWithTag("journey-board").assertDoesNotExist()
        compose.onNodeWithText(model.selectedRoute.missions.first().place, substring = true).performScrollTo().performClick()
        compose.onNodeWithTag("mission-page").assertExists()
        compose.waitUntil { model.controller.vehicle.parentStopped }
        send(AppEvent.GearChanged(Gear.R)) // DrivingRestricted를 보내지 않는다.
        compose.waitUntil { model.demoMission == null && !model.miniGamesVisible && !model.controller.vehicle.parentStopped }
        compose.onNodeWithTag("mission-page").assertDoesNotExist()
        send(AppEvent.GearChanged(Gear.D))
        compose.runOnIdle { assertEquals(Screen.FLYING, model.controller.screen) }
        send(AppEvent.GearChanged(Gear.P))
        compose.runOnIdle { model.openMiniGames(); model.openDemoMission(model.selectedRoute.missions.first()); model.toggleMissionAudio() }
        send(AppEvent.GearChanged(Gear.D))
        compose.waitUntil { model.demoMission == null }
        compose.runOnIdle { assertTrue(model.controller.vehicle.parentStopped); assertEquals(Screen.PAUSED, model.controller.screen) }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val test = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        java.io.File(compose.activity.cacheDir, name).outputStream().use {
            test.uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
