// 언어·경로·테마와 동의 선택이 창 크기 변경과 재생성 후 유지되는지 확인한다.
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

    @Test fun languageRoutesThemesAndConsentSurviveResizing() {
        lateinit var model: AppViewModel
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        val width = mutableStateOf(1500)
        compose.activity.runOnUiThread {
            compose.activity.setContent { Box(Modifier.requiredSize(width.value.dp, 900.dp)) { KidQuestScreen(model) } }
        }
        compose.onNodeWithText("한국어").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("listening-consent").performScrollTo().performClick().assertIsOn()
        compose.runOnIdle { width.value = 628 }
        listOf("한국어", "English · 12세 이상 권장", "말로 대답하기", "경로 고르기 →").forEach(::checkText)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("한국어").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("listening-consent").performScrollTo().assertIsOn()
        compose.onNodeWithText("경로 고르기 →").performScrollTo().performClick()
        for (route in model.routes) compose.onNodeWithText(route.label).performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("서울 → 경주").performScrollTo().performClick()
        compose.onNodeWithText("테마 고르기 →").performScrollTo().performClick()
        compose.onNodeWithText("역사 보물찾기").assertIsEnabled().assertIsSelected()
        listOf("준비 중 01", "준비 중 02", "준비 중 03").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsNotEnabled()
        }
        compose.runOnIdle { model.selectTheme("coming_1"); assertEquals("history", model.themeId) }
        compose.onNodeWithText("탐험 시작하기 →").performScrollTo().performClick()
        compose.onNodeWithText("탐험대 확인").assertExists()
    }

    @Test fun defaultLanguageStartsWithoutConsentAndFreshSetupDoesNotReuseConsent() {
        lateinit var model: AppViewModel
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        compose.onNodeWithTag("listening-consent").performScrollTo().assertIsOff()
        compose.runOnIdle {
            model.selectDestination("unknown")
            assertEquals("gyeongju", model.destinationId)
            assertEquals(com.kidquest.core.contract.Language.KO, model.language)
            val store = ViewModelStore()
            try {
                val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(compose.activity.application)
                val fresh = ViewModelProvider(store, factory)[AppViewModel::class.java]
                assertFalse(fresh.consent)
                assertEquals(com.kidquest.core.contract.Language.KO, fresh.language)
            } finally { store.clear() }
        }
        compose.onNodeWithText("경로 고르기 →").performScrollTo().performClick()
        compose.onNodeWithText("테마 고르기 →").performScrollTo().performClick()
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
