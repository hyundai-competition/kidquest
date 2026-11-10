// 에뮬레이터 화면에서 시작 버튼과 엔진 화면 전환을 확인한다. 이동 신호는 명시적인 재생 모드로 넣는다.
package com.kidquest.app

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class StartScreenConnectionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun settingsThenCrewThenEngineFlight() {
        lateinit var model: AppViewModel
        compose.runOnIdle { model = ViewModelProvider(compose.activity)[AppViewModel::class.java] }
        // 실제 수집기 값을 먼저 보관해 시험 보고서에 남긴다. 재생 입력과 섞어 성공이라 하지 않는다.
        compose.waitUntil(10_000) {
            model.controller.vehicle.gear != null || CarSignal.GEAR in model.controller.vehicle.unavailable
        }
        compose.runOnIdle {
            val vehicle = model.controller.vehicle
            val report = "gear=${vehicle.gear}; occupied=${vehicle.occupied}; belts=${vehicle.buckled}; unavailable=${vehicle.unavailable}"
            android.util.Log.i("KQScreenTest", report)
            File(compose.activity.getExternalFilesDir(null), "vehicle-check.txt").writeText(report)
        }
        compose.onNodeWithText("한국어").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("경로 고르기 →").performScrollTo().performClick()
        compose.onNodeWithText("서울 → 경주").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("테마 고르기 →").performScrollTo().performClick()
        compose.onNodeWithText("탐험 시작하기 →").performScrollTo().performClick()
        compose.onNodeWithText("탐험대 확인").assertExists()
        compose.onNodeWithText("재생 모드").performScrollTo().performClick()
        compose.onNodeWithText("탑승").assertExists()
        compose.onNodeWithText("재생 모드 접기").performScrollTo().performClick()
        capture("crew-check.png")
        compose.runOnIdle {
            RearSeat.entries.forEach { seat ->
                model.controller.replay.send(AppEvent.SeatOccupied(seat, true))
                model.controller.replay.send(AppEvent.BeltBuckled(seat, true))
            }
            model.controller.replay.send(AppEvent.GearChanged(Gear.D))
        }
        compose.waitUntil(5_000) { model.controller.screen == Screen.FLYING }
        compose.onNodeWithText("보물 탐험 중", substring = true).assertExists()
        compose.onNodeWithTag("journey-board").assertExists()
        compose.onNodeWithText("서울에서 경주까지").performScrollTo().assertIsDisplayed()
        listOf("대릉원", "첨성대", "동궁과 월지", "불국사").forEach {
            compose.onNodeWithText(it).assertExists()
        }
        compose.waitForIdle()
        capture("flight-check.png")
        compose.onNodeWithText("대릉원").performScrollTo().performClick()
        compose.onNodeWithText("대릉원 · 꼬마 탐험").assertExists()
        compose.onNodeWithTag("mission-page").assertIsDisplayed()
        compose.onNodeWithTag("journey-board").assertDoesNotExist()
        val art = compose.onNodeWithTag("mission-art").fetchSemanticsNode().boundsInRoot
        assertEquals(1.6f, art.width / art.height, .02f)
        compose.onNodeWithText("설명 다시 듣기").assertExists()
        compose.waitUntil(15_000) { model.narrationStatus.startsWith("재생 중:") || model.narrationStatus.startsWith("재생 완료:") }
        capture("mission-briefing.png")
        compose.onNodeWithText("미션 시작").assertIsDisplayed().performClick()
        compose.onNodeWithText("스마트폰").performScrollTo().performClick()
        compose.onNodeWithText("옛이야기에 어울리는 물건을 다시 살펴봐요.").assertExists()
        compose.onNodeWithText("금관").performScrollTo().performClick()
        compose.onNodeWithTag("mission-success").assertExists()
        capture("mission-check.png")
        compose.onNodeWithText("지도로 돌아가기").performClick()
        compose.onNodeWithText("완료 ✓").assertExists()
        compose.runOnIdle { assertTrue("대릉원" in model.completedDemoMissions) }
        compose.onNodeWithText("첨성대").performScrollTo().performClick()
        compose.onNodeWithText("미션 시작").assertIsDisplayed().performClick()
        compose.onNodeWithText("연결한 별 0 / 4").performScrollTo()
        compose.onNodeWithTag("star-canvas").performScrollTo().performTouchInput {
            down(Offset(width * .12f, height * .70f))
            moveTo(Offset(width * .28f, height * .53f), 200)
            moveTo(Offset(width * .44f, height * .49f), 200)
            moveTo(Offset(width * .60f, height * .40f), 200)
            up()
        }
        compose.onNodeWithTag("mission-success").assertExists()
        compose.onNodeWithText("완성한 미션 다시 보기").performScrollTo().performClick()
        compose.onNodeWithText("별 모양을 완성했어요!").assertExists()
        capture("stars-check.png")
        compose.onNodeWithText("지도로 돌아가기").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue("첨성대" in model.completedDemoMissions) }
        compose.onNodeWithText("동궁과 월지").performScrollTo().performClick()
        compose.onNodeWithText("미션 시작").assertIsDisplayed().performClick()
        compose.onNodeWithText("연못").performScrollTo().performClick()
        compose.onNodeWithText("연못 자리").performScrollTo().performClick()
        compose.onNodeWithText("전통 건물").performScrollTo().performClick()
        compose.onNodeWithText("전통 건물 자리").performScrollTo().performClick()
        compose.onNodeWithTag("mission-success").assertExists()
        capture("build-check.png")
        compose.onNodeWithText("지도로 돌아가기").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue("동궁과 월지" in model.completedDemoMissions) }
        compose.onNodeWithText("불국사").performScrollTo().performClick()
        compose.onNodeWithText("미션 시작").performClick()
        compose.onNodeWithText("다보탑").performScrollTo().performClick()
        compose.onNodeWithTag("mission-success").assertExists()
        compose.onNodeWithText("여행 마무리 보기").performScrollTo().performClick()
        compose.onNodeWithTag("journey-ending").assertIsDisplayed()
        compose.onNodeWithText("브루미와 함께한 경주 탐험이 끝났어요.").assertExists()
        capture("journey-ending.png")
        compose.onNodeWithText("완성한 지도 보기").performScrollTo().performClick()
        compose.onNodeWithTag("journey-board").assertExists()


        compose.runOnIdle {
            assertTrue(model.controller.replayUsed)
            model.controller.replay.send(AppEvent.BeltBuckled(RearSeat.LEFT, false))
        }
        compose.waitUntil(5_000) { model.controller.screen == Screen.PAUSED }
        compose.onNodeWithText("잠시 멈췄어요").assertExists()
    }

    private fun capture(name: String) {
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(compose.activity.cacheDir, name).outputStream().use {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
