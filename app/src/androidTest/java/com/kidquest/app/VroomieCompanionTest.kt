// 실제 화면에서 와이퍼·좌우 신호의 조합과 좁은 창·주행 규제 상태의 캐릭터 표시를 확인한다.
package com.kidquest.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.app.screen.KidQuestScreen
import com.kidquest.core.contract.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class VroomieCompanionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun weatherAndDirectionRemainVisibleAcrossWindowSizes() {
        lateinit var model: AppViewModel
        val width = mutableStateOf(1600)
        compose.runOnIdle {
            model = ViewModelProvider(compose.activity)[AppViewModel::class.java]
            model.controller.stop()
        }
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent { Box(Modifier.requiredSize(width.value.dp, 1120.dp)) { KidQuestScreen(model) } }
        }
        fun send(vararg events: AppEvent) { compose.runOnIdle { events.forEach(model.controller::dispatch) } }
        fun pose(value: String) {
            compose.onNodeWithTag("vroomie-companion").assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value))
        }
        fun capture(name: String) {
            compose.waitForIdle()
            val test = InstrumentationRegistry.getInstrumentation()
            File(test.targetContext.cacheDir, name).outputStream().use {
                test.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        send(AppEvent.GearChanged(Gear.P), AppEvent.DrivingRestricted(false), AppEvent.WiperChanged(false), AppEvent.Tick(Long.MAX_VALUE))
        pose("dry-idle"); capture("vroomie-idle.png")
        send(AppEvent.WiperChanged(true)); pose("rain-idle"); capture("vroomie-rain.png")
        send(AppEvent.TurnSignalOn(TurnSide.LEFT)); pose("rain-left"); capture("vroomie-rain-left.png")
        send(AppEvent.WiperChanged(false), AppEvent.TurnSignalOn(TurnSide.RIGHT)); pose("dry-right")
        compose.runOnIdle { width.value = 628 }
        pose("dry-right"); capture("vroomie-narrow-right.png")
        send(AppEvent.GearChanged(Gear.D), AppEvent.WiperChanged(true), AppEvent.DrivingRestricted(true))
        pose("rain-right"); capture("vroomie-restricted.png")
        compose.onNodeWithTag("app-back").assertIsDisplayed()
        compose.onNodeWithTag("app-exit").assertIsDisplayed()
        compose.runOnIdle { assertEquals(Screen.START, model.controller.screen) }
    }
}
