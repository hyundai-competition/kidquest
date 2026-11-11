// 사진·한영 보물 이름·좁은 결과 화면과 P→D 전환 시 효과 중단을 실제 렌더링으로 확인한다.
package com.kidquest.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.kidquest.app.screen.KidQuestScreen
import com.kidquest.app.screen.TreasureReveal
import com.kidquest.app.screen.common.KidQuestTheme
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TreasureDisplayTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var model: AppViewModel
    private val width = mutableStateOf(628)
    private val photoIds = listOf("cheomseongdae_piece", "capital_piece", "hahoe_piece", "palace_piece")
    private fun prepare() {
        compose.activityRule.scenario.onActivity { activity -> model = ViewModelProvider(activity)[AppViewModel::class.java] }
        compose.waitUntil(10_000) { model.connectionsReady }
        compose.runOnIdle { model.controller.stop() }
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent { Box(Modifier.requiredSize(width.value.dp, 850.dp)) { KidQuestScreen(model) } }
        }
        send(AppEvent.AudioFocusChanged(true), AppEvent.DrivingRestricted(false), AppEvent.GearChanged(Gear.P))
    }
    private fun send(vararg events: AppEvent) {
        compose.runOnIdle { events.forEach(model.controller::dispatch) }
        compose.waitForIdle()
    }
    private fun arrive(language: Language) {
        compose.runOnIdle { model.restartTrip(); model.selectLanguage(language) }
        compose.waitForIdle()
        compose.runOnIdle { model.startTrip() }
        send(AppEvent.ParentConfirmedBelt, AppEvent.GearChanged(Gear.D), AppEvent.GearChanged(Gear.P), AppEvent.EndTripPressed)
        compose.waitUntil { model.controller.screen == Screen.LANDED }
    }
    private fun award(ids: List<String> = photoIds) {
        // 보상 판단 시험이 아니라 결과 표시 시험이므로 에피소드의 실제 이름으로 화면 목록만 채운다.
        compose.runOnIdle {
            model.controller.items.addAll(ids.map { id ->
                val item = model.tripCatalog.episode!!.items.first { it.id == id }
                Command.AwardItem(item.id, item.name)
            })
        }
    }
    @Test fun emptyAndFourPhotosFitBothLanguagesAndWidths() {
        prepare()
        for (language in Language.entries) {
            arrive(language)
            compose.onNodeWithTag("engine-trip-ending").assertExists()
            compose.onAllNodes(hasTestTag("treasure-photo-cheomseongdae_piece")).assertCountEquals(0)
            award()
            for (size in listOf(628, 1200)) {
                compose.runOnIdle { width.value = size }
                photoIds.forEach { id ->
                    val name = model.tripCatalog.episode!!.items.first { it.id == id }.name
                    val layouts = mutableListOf<TextLayoutResult>()
                    compose.onNodeWithText("✦ $name").performScrollTo().assertIsDisplayed()
                        .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertTrue("$language $size $name", layouts.isNotEmpty())
                    assertTrue(layouts.all { it.multiParagraph.height <= it.size.height + 1f && !it.multiParagraph.didExceedMaxLines })
                    compose.onNodeWithTag("treasure-photo-$id").assertIsDisplayed().assertWidthIsEqualTo(96.dp).assertHeightIsEqualTo(96.dp)
                }
                if (size == 628) {
                    compose.onNodeWithTag("treasure-list").performScrollTo()
                    capture("treasure-${language.name}-628.png")
                }
            }
            compose.onNodeWithText(if (language == Language.EN) "Prepare a new adventure" else "새로운 탐험 준비하기").performScrollTo().assertIsDisplayed()
        }
    }
    @Test fun leavingParkFinishesRevealImmediatelyWithoutReplayingOnReturn() {
        prepare()
        arrive(Language.EN)
        // 스크롤 이동 자체의 애니메이션과 섞이지 않도록 보물 목록을 고정 영역에서 비교한다.
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                KidQuestTheme {
                    androidx.compose.material3.Surface(Modifier.requiredSize(628.dp, 200.dp)) {
                        TreasureReveal(model.controller.items, animate = model.controller.vehicle.decorationsMayAnimate)
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        try {
            award(listOf(photoIds.first()))
            compose.mainClock.advanceTimeBy(100)
            val row = compose.onNodeWithTag("treasure-list")
            compose.mainClock.advanceTimeBy(100)
            row.assertIsDisplayed()
            val parked = row.captureToImage()
            send(AppEvent.GearChanged(Gear.D)) // 규제 신호가 오지 않아도 정적으로 전환한다.
            compose.mainClock.advanceTimeByFrame()
            val driving = row.captureToImage()
            assertTrue("P에서 효과가 진행 중이어야 함", changedPixels(parked, driving) > 100)
            send(AppEvent.GearChanged(Gear.R))
            compose.mainClock.advanceTimeByFrame()
            assertEquals("R에서도 정적이어야 함", 0, changedPixels(driving, row.captureToImage()))
            send(AppEvent.GearChanged(Gear.N))
            compose.mainClock.advanceTimeBy(2500)
            assertEquals("D에서 움직임이 남음", 0, changedPixels(driving, row.captureToImage()))
            send(AppEvent.GearChanged(Gear.P))
            compose.mainClock.advanceTimeByFrame()
            assertEquals("P 복귀 때 다시 숨기면 안 됨", 0, changedPixels(driving, row.captureToImage()))
        } finally { compose.mainClock.autoAdvance = true }
    }
    private fun capture(name: String) {
        val test = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        compose.waitForIdle()
        test.waitForIdleSync()
        // 에뮬레이터의 앱 영역 합성 프레임이 반영된 뒤 캡처한다.
        android.os.SystemClock.sleep(250)
        val bitmap = test.uiAutomation.takeScreenshot()
        try {
            java.io.File(test.targetContext.cacheDir, name).outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally { bitmap.recycle() }
    }
    private fun changedPixels(a: ImageBitmap, b: ImageBitmap): Int {
        assertEquals(a.width, b.width); assertEquals(a.height, b.height)
        val x = a.toPixelMap(); val y = b.toPixelMap()
        var changed = 0
        for (row in 0 until a.height) for (column in 0 until a.width) {
            val p = x[column, row]; val q = y[column, row]
            if (kotlin.math.abs(p.red - q.red) + kotlin.math.abs(p.green - q.green) + kotlin.math.abs(p.blue - q.blue) > .03f) changed++
        }
        return changed
    }
}
