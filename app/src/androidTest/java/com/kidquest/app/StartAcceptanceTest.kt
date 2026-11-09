// 브루미의 경로·테마·연령 설정과 좁은 창, 재생성, 연령 저장을 실제 기기에서 확인한다.
package com.kidquest.app

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.app.screen.KidQuestScreen
import com.kidquest.core.contract.AgeGroup
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class StartAcceptanceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun fourRoutesLockedThemesAndSelectionsSurviveResizing() {
        lateinit var model: AppViewModel
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        val width = mutableStateOf(1500)
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                Box(Modifier.requiredSize(width.value.dp, 900.dp)) { KidQuestScreen(model) }
            }
        }
        compose.waitForIdle()
        for (route in model.routes) compose.onNodeWithText(route.label).performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("서울 → 경주").performScrollTo().performClick()
        capture("vroomie-route.png")
        compose.runOnIdle { width.value = 628 }
        model.routes.forEach { checkText(it.label) }
        compose.onNodeWithText("테마 고르기 →").performScrollTo().performClick()
        compose.onNodeWithText("역사 보물찾기").assertIsEnabled().assertIsSelected()
        listOf("준비 중 01", "준비 중 02", "준비 중 03").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsNotEnabled()
        }
        compose.runOnIdle { model.selectTheme("coming_1"); assertEquals("history", model.themeId) }
        compose.runOnIdle { width.value = 1500 }
        capture("vroomie-theme.png")
        compose.onNodeWithText("출발 준비 →").performScrollTo().performClick()
        compose.onNodeWithText("초등 저학년").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("listening-consent").performScrollTo().performClick().assertIsOn()
        compose.runOnIdle { width.value = 628 }
        listOf("미취학", "초등 저학년", "말로 대답하기", "탐험 시작하기 →").forEach(::checkText)
        compose.onNodeWithText("초등 저학년").assertIsSelected()
        compose.onNodeWithTag("listening-consent").assertIsOn()
        compose.runOnIdle { width.value = 1500 }
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("초등 저학년").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("listening-consent").performScrollTo().assertIsOn()
        compose.runOnIdle {
            assertEquals("seoul_gyeongju", model.destinationId)
            assertEquals(2, model.setupStep)
        }
        compose.onNodeWithText("탐험 시작하기 →").performScrollTo().performClick()
        compose.onNodeWithText("탐험대 확인").assertExists()
    }

    @Test fun defaultRouteStartsWithoutConsentAndAgeUsesIntegrationStore() {
        lateinit var model: AppViewModel
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        compose.onNodeWithText("테마 고르기 →").performScrollTo().performClick()
        compose.onNodeWithText("출발 준비 →").performScrollTo().performClick()
        compose.onNodeWithTag("listening-consent").performScrollTo().assertIsOff()
        compose.runOnIdle {
            model.selectDestination("unknown")
            assertEquals("seoul_gyeongju", model.destinationId)
            model.selectAge(AgeGroup.EARLY_GRADE)
            val store = ViewModelStore()
            try {
                val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(compose.activity.application)
                val fresh = ViewModelProvider(store, factory)[AppViewModel::class.java]
                assertEquals(AgeGroup.EARLY_GRADE, fresh.age)
                assertFalse(fresh.consent)
                assertEquals("seoul_gyeongju", fresh.destinationId)
            } finally { store.clear() }
        }
        compose.onNodeWithText("탐험 시작하기 →").performScrollTo().performClick()
        compose.onNodeWithText("탐험대 확인").assertExists()
    }

    private fun checkText(label: String) {
        val node = compose.onNodeWithText(label).performScrollTo().assertIsDisplayed()
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertTrue("$label 잘림", results.isNotEmpty() && results.none { it.hasVisualOverflow })
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val test = InstrumentationRegistry.getInstrumentation()
        test.waitForIdleSync()
        val screenshot = test.uiAutomation.takeScreenshot()
        File(compose.activity.cacheDir, name).outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
