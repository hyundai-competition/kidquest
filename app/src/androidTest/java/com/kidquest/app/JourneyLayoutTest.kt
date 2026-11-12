// 작은 창과 기본 폭에서 고정 소개, 진행도, 주행 안내 정렬을 확인한다.
package com.kidquest.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.kidquest.app.screen.KidQuestScreen
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import android.graphics.Bitmap

class JourneyLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var model: AppViewModel
    private val width = mutableStateOf(1200)
    private fun send(vararg events: AppEvent) {
        compose.runOnIdle { events.forEach(model.controller::dispatch) }
        compose.waitForIdle()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.takeScreenshot().let { bitmap ->
            File(compose.activity.cacheDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Test fun setupAndJourneyAndDrivingLayout() {
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        compose.waitUntil(15_000) { model.connectionsReady }
        compose.runOnIdle { model.controller.stop() }
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent { Box(Modifier.requiredSize(width.value.dp, 1000.dp)) { KidQuestScreen(model) } }
        }
        send(AppEvent.GearChanged(Gear.P), AppEvent.DrivingRestricted(false), AppEvent.AudioFocusChanged(true))
        for (size in listOf(1200, 628, 360)) {
            compose.runOnIdle { width.value = size }
            compose.onNodeWithText("브루미와 떠나는 역사 보물찾기").assertExists()
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText("정차 중에 설정해 주세요.", substring = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            if (size == 628) capture("layout-setup-compact")
            layouts.forEach { layout ->
                assertFalse("Safety notice clipped at $size", layout.hasVisualOverflow)
                for (line in 0 until layout.lineCount) {
                    val text = layout.layoutInput.text.text.substring(layout.getLineStart(line), layout.getLineEnd(line)).trim()
                    assertNotEquals("Orphan ending at $size", "요.", text)
                }
            }
        }
        compose.runOnIdle { model.selectDestination(model.routes[1].id); model.changeStep(0) }
        compose.onNodeWithText("브루미와 떠나는 역사 보물찾기").assertExists()
        compose.runOnIdle { model.selectDestination(model.routes.first().id); model.selectLanguage(Language.EN) }
        for (size in listOf(1200, 628, 360)) {
            compose.runOnIdle { width.value = size }
            compose.onNodeWithText("Explore history with Vroomie").assertExists()
        }
        compose.runOnIdle { model.selectLanguage(Language.KO); width.value = 1200 }
        capture("layout-setup")
        compose.runOnIdle { model.startTrip() }
        compose.waitUntil { model.controller.screen == Screen.CREW_CHECK }
        send(AppEvent.ParentConfirmedBelt, AppEvent.GearChanged(Gear.D))
        compose.waitUntil { model.controller.screen == Screen.FLYING }
        compose.onAllNodesWithTag("vroomie-companion").assertCountEquals(1)
        compose.onNodeWithTag("journey-progress").assertExists()
        send(AppEvent.PlaceEntered(model.tripStops[0].placeId), AppEvent.WiperChanged(true))
        capture("layout-progress")
        compose.runOnIdle { width.value = 628 }
        compose.onNodeWithTag("journey-progress").assertExists()
        capture("layout-progress-compact")
        compose.runOnIdle { width.value = 1200 }
        send(AppEvent.DrivingRestricted(true))
        val status = compose.onNodeWithTag("restricted-status").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithTag("stop-audio").fetchSemanticsNode().boundsInRoot
        assertEquals(status.center.x, button.center.x, 2f)
        compose.onAllNodesWithTag("vroomie-companion").assertCountEquals(1)
        capture("layout-driving")
        send(AppEvent.GearChanged(Gear.P), AppEvent.DrivingRestricted(false))
        compose.runOnIdle { model.selectWeatherStyle(com.kidquest.app.screen.WeatherStyle.SNOW) }
        send(AppEvent.GearChanged(Gear.D), AppEvent.DrivingRestricted(true))
        capture("layout-driving-snow")
    }
}
