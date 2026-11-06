// 첫 이슈의 선택·동의·창 크기·재생성·연령 저장을 실제 안드로이드 화면에서 확인한다.
package com.kidquest.app

import android.app.Application
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
import com.kidquest.app.screen.KidQuestScreen
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class StartAcceptanceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun choicesSurviveNarrowWindowAndActivityRecreation() {
        lateinit var model: AppViewModel
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        val width = mutableStateOf(1500)
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                Box(Modifier.requiredSize(width.value.dp, 800.dp)) { KidQuestScreen(model) }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("초등 저학년").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("시험 목적지").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("listening-consent").performScrollTo().performClick().assertIsOn()
        compose.runOnIdle { width.value = 628 }
        listOf("미취학", "초등 저학년", "기본 이야기", "시험 목적지", "말로 대답하기", "승무원 확인하기 →").forEach { label ->
            val node = compose.onNodeWithText(label).performScrollTo().assertIsDisplayed()
            val results = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            assertTrue("$label 글자 넘침: ${results.map { "size=${it.size}, width=${it.didOverflowWidth}, height=${it.didOverflowHeight}, paragraph=${it.multiParagraph.width}/${it.multiParagraph.height}" }}", results.isNotEmpty() && results.none { it.hasVisualOverflow })
        }
        assertChoicesSelected()
        compose.runOnIdle { width.value = 1500 }
        assertChoicesSelected()
        // 실제 Activity 재생성에서도 같은 ViewModel과 선택값이 남아야 한다.
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        assertChoicesSelected()
        compose.onNodeWithText("승무원 확인하기 →").performScrollTo().performClick()
        compose.onNodeWithText("승무원 확인").assertExists()
    }

    @Test fun basicStoryStartsWithoutConsentAndAgeUsesIntegrationStore() {
        lateinit var model: AppViewModel
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        compose.onNodeWithText("미취학").performScrollTo().performClick()
        compose.onNodeWithText("기본 이야기").performScrollTo().performClick()
        compose.onNodeWithTag("listening-consent").performScrollTo().assertIsOff()
        compose.runOnIdle {
            model.selectDestination("unknown")
            assertNull(model.destinationId)
            model.selectAge(AgeGroup.EARLY_GRADE)
            val store = ViewModelStore()
            try {
                val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(compose.activity.application as Application)
                val fresh = ViewModelProvider(store, factory)[AppViewModel::class.java]
                assertEquals(AgeGroup.EARLY_GRADE, fresh.age)
                assertFalse(fresh.consent)
                assertNull(fresh.destinationId)
            } finally { store.clear() }
        }
        compose.onNodeWithText("승무원 확인하기 →").performScrollTo().performClick()
        compose.onNodeWithText("승무원 확인").assertExists()
    }

    private fun assertChoicesSelected() {
        compose.onNodeWithText("초등 저학년").performScrollTo().assertIsSelected()
        compose.onNodeWithText("시험 목적지").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("listening-consent").performScrollTo().assertIsOn()
    }
}
