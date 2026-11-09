// 네 도시의 장소 화면에서 그림 비율, 지도 가림, 창 크기 변경 뒤 선택 유지와 고정 버튼을 검증한다.
package com.kidquest.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.app.screen.KidQuestScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class MissionLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun allCitiesKeepArtworkRatioAndPageStateWhenResized() {
        lateinit var model: AppViewModel
        val width = mutableStateOf(1500)
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        compose.waitUntil(10_000) { model.connectionsReady }
        compose.runOnIdle { model.controller.dispatch(com.kidquest.core.contract.AppEvent.GearChanged(com.kidquest.core.contract.Gear.P)) }
        compose.waitUntil { model.canPlayMiniGames }
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                Box(Modifier.requiredSize(width.value.dp, 900.dp)) { KidQuestScreen(model) }
            }
        }
        model.routes.forEach { route ->
            compose.runOnIdle {
                model.selectDestination(route.id)
                model.openDemoMission(route.missions.first())
                model.stopNarration()
            }
            listOf(1500, 628, 1500).forEach { size ->
                compose.runOnIdle { width.value = size }
                compose.onNodeWithTag("mission-page").assertIsDisplayed()
                compose.onNodeWithTag("journey-board").assertDoesNotExist()
                compose.onNodeWithTag("mission-art").performScrollTo().assertIsDisplayed()
                val art = compose.onNodeWithTag("mission-art").fetchSemanticsNode().boundsInRoot
                assertEquals("${route.to} $size", 1.6f, art.width / art.height, .02f)
                compose.onNodeWithText("미션 시작").assertIsDisplayed()
                compose.onNodeWithText("미니게임 목록으로").assertIsDisplayed()
                compose.runOnIdle { assertEquals(route.missions.first().id, model.demoMission?.id) }
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(route.missions.first().description).performScrollTo()
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertTrue("${route.to} $size 설명 잘림", layouts.isNotEmpty() && layouts.none { it.hasVisualOverflow })
                if (route.to == "경주" && size == 628) capture("mission-narrow.png")
            }
            compose.onNodeWithText("미니게임 목록으로").performClick()
            compose.onNodeWithTag("mission-page").assertDoesNotExist()
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.cacheDir, name).outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
