// 628dp와 기본 폭에서 주요 화면·한국어 놀이터를 검사하고 영어 본편의 번역 누락을 찾는다.
package com.kidquest.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.kidquest.app.screen.KidQuestScreen
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class WindowLanguageAuditTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var model: AppViewModel
    private val width = mutableStateOf(628)
    private val errors = mutableSetOf<String>()
    private fun send(vararg events: AppEvent) {
        compose.runOnIdle { events.forEach(model.controller::dispatch) }
        compose.waitForIdle()
    }
    @Test fun bothLanguagesFitSetupTripBreakResultsAndPlayground() {
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        compose.waitUntil(10_000) { model.connectionsReady }
        compose.runOnIdle { model.controller.stop() }
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent { Box(Modifier.requiredSize(width.value.dp, 800.dp)) { KidQuestScreen(model) } }
        }
        for (language in Language.entries) {
            compose.runOnIdle { model.restartTrip(); model.selectLanguage(language) }
            send(AppEvent.GearChanged(Gear.P), AppEvent.DrivingRestricted(false), AppEvent.AudioFocusChanged(true))
            for (step in 0..2) {
                compose.runOnIdle { model.changeStep(step) }
                inspectWidths(language, "setup-$step")
            }
            compose.runOnIdle { model.startTrip() }
            compose.waitUntil { model.controller.screen == Screen.CREW_CHECK }
            inspectWidths(language, "crew")
            send(AppEvent.ParentConfirmedBelt, AppEvent.GearChanged(Gear.D))
            compose.waitUntil { model.controller.screen == Screen.FLYING }
            inspectWidths(language, "flying")
            send(AppEvent.ParentStopChanged(true))
            inspectWidths(language, "paused")
            send(AppEvent.ParentStopChanged(false), AppEvent.GearChanged(Gear.P))
            inspectWidths(language, "docked")
            compose.runOnIdle { model.openMiniGames() }
            inspectWidths(language, "playground", english = false)
            compose.runOnIdle { model.openDemoMission(model.selectedRoute.missions.first()) }
            inspectWidths(language, "puzzle-briefing", english = false)
            compose.runOnIdle { model.beginMission() }
            inspectWidths(language, "puzzle", english = false)
            compose.runOnIdle { model.closeMiniGames() }
            send(AppEvent.EndTripPressed)
            compose.waitUntil { model.controller.screen == Screen.LANDED }
            inspectWidths(language, "result")
        }
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }
    private fun inspectWidths(language: Language, state: String, english: Boolean = language == Language.EN) {
        for (size in listOf(628, 1200)) {
            compose.runOnIdle { width.value = size }
            compose.waitForIdle()
            val nodes = compose.onAllNodes(hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            for (node in nodes) {
                val text = node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.joinToString { it.text }
                var ancestor: androidx.compose.ui.semantics.SemanticsNode? = node
                var developer = false
                while (ancestor != null) {
                    if (ancestor.config.getOrElse(SemanticsProperties.TestTag) { "" } == "developer-panel") developer = true
                    ancestor = ancestor.parent
                }
                if (developer) continue
                val layouts = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                // Compose may keep a full-width paragraph inside an intrinsically sized Text.
                // Compare glyph width, since centred lines retain the paragraph coordinate origin.
                val clipped = layouts.any { layout ->
                    layout.multiParagraph.didExceedMaxLines || layout.multiParagraph.height > layout.size.height + 1f || (0 until layout.lineCount).any { line ->
                        layout.getLineRight(line) - layout.getLineLeft(line) > layout.size.width + 1f
                    }
                }
                if (clipped) errors += "$language $state $size text clipped: $text ${layouts.map { "size=${it.size} h=${it.multiParagraph.height} max=${it.multiParagraph.didExceedMaxLines}" }}"
                if (english && text != "한국어" && text.any { it in '가'..'힣' }) errors += "$state untranslated: $text"
            }
        }
    }
}
