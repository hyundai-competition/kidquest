// 확인 취소·주행 중 초기화·종료와 실제 접근성 노드의 비활성 보기 눌림 불가를 검증한다.
package com.kidquest.app

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.app.screen.KidQuestScreen
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class NavigationControlsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var model: AppViewModel
    private fun prepare() {
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        compose.waitUntil(10_000) { model.connectionsReady }
        compose.runOnIdle { model.controller.stop(); model.startTrip() }
        send(AppEvent.SeatOccupied(RearSeat.LEFT, true), AppEvent.BeltBuckled(RearSeat.LEFT, true),
            AppEvent.ParentConfirmedBelt, AppEvent.GearChanged(Gear.D))
        compose.waitUntil { model.controller.screen == Screen.FLYING }
    }
    private fun send(vararg events: AppEvent) {
        compose.runOnIdle { events.forEach(model.controller::dispatch) }
        compose.waitForIdle()
    }
    @Test fun confirmationCancelPreservesTripAndConfirmRestartsEvenWhenRestricted() {
        prepare()
        send(AppEvent.DrivingRestricted(true))
        compose.onNodeWithTag("app-back").performClick()
        compose.onNodeWithText("처음으로 돌아갈까요?").assertExists()
        capture("restart-confirmation.png")
        compose.onNodeWithText("계속 탐험하기").performClick()
        compose.runOnIdle { assertEquals(Screen.FLYING, model.controller.screen) }
        compose.onNodeWithText("소리 끄기").performClick()
        compose.waitUntil { model.controller.screen == Screen.PAUSED }
        compose.onNodeWithTag("app-back").performClick()
        compose.onNodeWithTag("confirm-navigation").performClick()
        compose.waitUntil { model.controller.screen == Screen.START }
        compose.runOnIdle { assertFalse(model.consent); assertTrue(model.controller.items.isEmpty()); assertFalse(model.controller.vehicle.parentStopped) }
    }
    @Test fun exitWaitsForConfirmation() {
        prepare()
        var exits = 0
        compose.activity.runOnUiThread { compose.activity.setContent { KidQuestScreen(model, onExit = { exits++ }) } }
        compose.onNodeWithTag("app-exit").performClick()
        compose.onNodeWithText("계속 탐험하기").performClick()
        compose.runOnIdle { assertEquals(0, exits) }
        compose.onNodeWithTag("app-exit").performClick()
        compose.onNodeWithTag("confirm-navigation").performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }
    @Test fun disabledChoicesHaveNoNativeAccessibilityClickAction() {
        prepare()
        send(AppEvent.LineFinished("take_off"), AppEvent.LineFinished("departure"), AppEvent.LineFinished("rain_map/briefing"),
            AppEvent.WiperChanged(true), AppEvent.Tick(1000), AppEvent.Tick(5000), AppEvent.LineFinished("rain_map/event"))
        compose.onNodeWithText("1번 · 파란색").performScrollTo().assertIsNotEnabled()
        var nodes = emptyList<android.view.accessibility.AccessibilityNodeInfo>()
        compose.waitUntil(5_000) {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val roots = automation.windows.mapNotNull { it.root } + listOfNotNull(automation.rootInActiveWindow)
            fun scan(node: android.view.accessibility.AccessibilityNodeInfo): List<android.view.accessibility.AccessibilityNodeInfo> =
                listOf(node) + (0 until node.childCount).flatMap { i -> node.getChild(i)?.let(::scan).orEmpty() }
            nodes = roots.flatMap(::scan).filter { it.text?.toString() == "1번 · 파란색" }
            nodes.isNotEmpty()
        }
        nodes.forEach { assertFalse("비활성 보기 clickable", it.isClickable); assertFalse("비활성 보기 enabled", it.isEnabled) }
        java.io.File(compose.activity.cacheDir, "disabled-choice.txt").writeText(nodes.joinToString("\n") { "class=${it.className} clickable=${it.isClickable} enabled=${it.isEnabled}" })
        capture("disabled-choice.png")
        send(AppEvent.LineFinished("rain_map/question"))
        compose.onNodeWithText("1번 · 파란색").assertIsEnabled().performClick()
        compose.waitUntil { model.controller.presentation.stage == "answer" }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        java.io.File(compose.activity.cacheDir, name).outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
