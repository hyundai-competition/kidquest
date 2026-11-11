// 고정 경로를 실제 차량·위치 SDK와 TTS로 끝까지 진행하는 선택 시험. 재생 사건은 넣지 않는다.
package com.kidquest.app

import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.core.contract.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class SdkRouteTripTest {
    @get:Rule val compose = pleosComposeRule()
    private lateinit var model: AppViewModel
    private val test = InstrumentationRegistry.getInstrumentation()
    private val report get() = File(test.targetContext.cacheDir, "sdk-trip-state.json")

    @Test fun seoulToGyeongjuWithSdkSignalsAndOnDeviceSpeech() {
        assumeTrue("가상 차량 콘솔·GPS 구동기와 함께 -e verifySdkTrip true로 실행", InstrumentationRegistry.getArguments().getString("verifySdkTrip") == "true")
        compose.activityRule.scenario.onActivity { model = ViewModelProvider(it)[AppViewModel::class.java] }
        compose.waitUntil(20_000) {
            val vehicle = model.controller.vehicle
            model.connectionsReady && vehicle.gear == Gear.P &&
                (CarSignal.BELT in vehicle.unavailable || vehicle.buckled.size == RearSeat.entries.size)
        }
        compose.runOnIdle {
            model.selectDestination("gyeongju")
            model.selectTravelRoute("gyeongju_gyeongbu")
            model.setListeningConsent(false) // 실발화 시험과 구분한다. 무응답은 기존 엔진 규칙을 따른다.
            model.startTrip()
        }
        compose.waitUntil { model.controller.screen == Screen.CREW_CHECK }
        // 카시트 확인 버튼과 같은 부모의 명시적 조작이다. 센서 사건을 만드는 것이 아니다.
        compose.runOnIdle { model.controller.dispatch(AppEvent.ParentConfirmedBelt) }
        val end = SystemClock.elapsedRealtime() + 480_000
        var completed = false
        try {
            while (SystemClock.elapsedRealtime() < end) {
                compose.runOnIdle {
                    val controller = model.controller
                    completed = controller.screen == Screen.LANDED && controller.voiceStatus == "재생 완료: landed"
                    report.writeText(JSONObject().apply {
                        put("screen", controller.screen.name)
                        put("mission", controller.presentation.mission?.id ?: "")
                        put("stage", controller.presentation.stage)
                        put("items", controller.items.size)
                        put("gear", controller.vehicle.gear?.name ?: "unknown")
                        put("voice", controller.voiceStatus)
                        put("replayUsed", controller.replayUsed)
                        put("finished", completed)
                    }.toString())
                }
                if (completed) break
                SystemClock.sleep(500)
            }
            compose.runOnIdle {
                assertTrue("SDK 경로 진행 시간 초과: ${report.readText()}", completed)
                assertFalse(model.controller.replayUsed)
                assertEquals(model.plannedMissions.map { it.rewardItem }.toSet(), model.controller.items.map { it.itemId }.toSet())
                assertEquals(5, model.controller.items.size)
                assertEquals(Screen.LANDED, model.controller.screen)
                assertEquals(Gear.P, model.controller.vehicle.gear)
            }
        } finally { compose.runOnIdle { model.controller.stop() } }
    }
}
