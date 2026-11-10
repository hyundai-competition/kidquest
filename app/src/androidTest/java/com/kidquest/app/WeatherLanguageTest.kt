// 언어 선택부터 영어 본편 보기까지와 차량 장식 화면을 에뮬레이터에서 확인한다.
package com.kidquest.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.app.screen.WeatherStyle
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WeatherLanguageTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var model: AppViewModel
    private fun prepare() {
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        compose.waitUntil(10_000) { model.connectionsReady }
        compose.runOnIdle { model.controller.stop() }
        send(AppEvent.AudioFocusChanged(true), AppEvent.GearChanged(Gear.P), AppEvent.DrivingRestricted(false))
    }
    private fun send(vararg events: AppEvent) {
        compose.runOnIdle { events.forEach(model.controller::dispatch) }
        compose.waitForIdle()
    }
    @Test fun englishSelectionLeadsToEnglishEpisodeAndNumberedAnswer() {
        prepare()
        compose.onNodeWithText("미취학").assertDoesNotExist()
        compose.onNodeWithText("English · 12세 이상 권장").performScrollTo().performClick()
        compose.onNodeWithText("Choose a route →").performScrollTo().performClick()
        compose.onNodeWithText("Seoul → Gyeongju").performScrollTo().performClick()
        compose.onNodeWithText("Choose a theme →").performScrollTo().performClick()
        compose.onNodeWithText("Start exploring →").performScrollTo().performClick()
        compose.waitUntil { model.controller.screen == Screen.CREW_CHECK }
        compose.runOnIdle { assertEquals(Language.EN, model.language) }
        send(AppEvent.ParentConfirmedBelt, AppEvent.GearChanged(Gear.D))
        compose.waitUntil { model.controller.screen == Screen.FLYING }
        send(AppEvent.LineFinished("take_off"), AppEvent.LineFinished("departure"), AppEvent.LineFinished("rain_map/briefing"),
            AppEvent.WiperChanged(true), AppEvent.Tick(1000), AppEvent.Tick(5000), AppEvent.LineFinished("rain_map/event"))
        compose.runOnIdle { assertEquals(listOf("blue", "red"), model.controller.presentation.options) }
        send(AppEvent.LineFinished("rain_map/question"))
        compose.onNodeWithText("1 · blue").performScrollTo().performClick()
        compose.waitUntil { model.controller.presentation.stage == "answer" }
        compose.runOnIdle { assertFalse(model.controller.presentation.caption.any { it in '가'..'힣' }); assertFalse(model.consent) }
        capture("english-answer.png")
    }
    @Test fun wiperSnowAndBothTurnDirectionsShowWithoutOpeningQuest() {
        prepare()
        send(AppEvent.WiperChanged(true), AppEvent.TurnSignalOn(TurnSide.LEFT))
        compose.runOnIdle { model.selectWeatherStyle(WeatherStyle.SNOW) }
        compose.onNodeWithText("와이퍼 · 눈 연출").assertExists()
        capture("parked-snow-left.png")
        send(AppEvent.GearChanged(Gear.D), AppEvent.TurnSignalOn(TurnSide.RIGHT))
        compose.runOnIdle { assertEquals(Screen.START, model.controller.screen); assertEquals(TurnSide.RIGHT, model.controller.vehicle.turnSide) }
        compose.onNodeWithText("비로 바꾸기").assertDoesNotExist()
        capture("driving-snow-right.png")
        send(AppEvent.WiperChanged(false), AppEvent.Tick(Long.MAX_VALUE))
        compose.runOnIdle { assertFalse(model.controller.vehicle.wiperOn); assertNull(model.controller.vehicle.turnSide) }
    }
    private fun capture(name: String) {
        val test = InstrumentationRegistry.getInstrumentation()
        java.io.File(compose.activity.cacheDir, name).outputStream().use {
            test.uiAutomation.takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
