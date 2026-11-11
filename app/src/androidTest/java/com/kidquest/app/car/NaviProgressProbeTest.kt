// 기본 내비에 고정 목적지를 요청하고 실제 SDK의 위치·도착 갱신을 보는 선택 진단. 성공을 가짜 사건으로 만들지 않는다.
package com.kidquest.app.car

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.*
import ai.pleos.playground.navi.constants.NaviErrorCode
import android.os.Handler
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NaviProgressProbeTest {
    @Test fun presetRouteCanReportArrivalFromRealNavigation() {
        val test = InstrumentationRegistry.getInstrumentation()
        assumeTrue("고정 내비 목적지와 GPS 구동을 함께 시험할 때만 실행", InstrumentationRegistry.getArguments().getString("verifyNavigationProgress") == "true")
        val main = Handler(Looper.getMainLooper())
        val arrived = CountDownLatch(1)
        val points = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        var destinationReceived = false
        var nearTarget = false
        var error = ""
        var remaining = -1
        var running = true
        lateinit var helper: NaviHelper
        val report = File(test.targetContext.cacheDir, "navi-progress-probe.json")
        fun save() = report.writeText(JSONObject().apply {
            put("destinationReceived", destinationReceived); put("nearTarget", nearTarget)
            put("distinctPositions", points.size); put("remainingSeconds", remaining)
            put("arrived", arrived.count == 0L); put("error", error)
        }.toString())
        val listener = object : NaviHelperEventListener {
            override fun onCurrentLocationInfo(value: CurrentLocationInfo) = main.post {
                points += "${(value.latitude * 10000).toInt()},${(value.longitude * 10000).toInt()}"
                nearTarget = kotlin.math.abs(value.latitude - 35.83472) < .01 && kotlin.math.abs(value.longitude - 129.21917) < .01
                save()
            }.let { Unit }
            override fun onDestinationInfo(value: DestinationInfo) = main.post {
                destinationReceived = kotlin.math.abs(value.latitude - 35.83472) < .01
                remaining = value.remainTime; save()
            }.let { Unit }
            override fun onDestinationArrived(value: DestinationArrivedInfo) { arrived.countDown(); main.post { save() } }
            override fun onError(code: NaviErrorCode) { main.post { error = code.name; save() } }
        }
        val poll = object : Runnable {
            override fun run() {
                if (!running) return
                helper.getCurrentLocationInfo(); helper.getDestinationInfo(); helper.getRouteStateInfo()
                main.postDelayed(this, 1000)
            }
        }
        test.runOnMainSync {
            helper = NaviHelper(test.targetContext)
            helper.initialize({ main.post {
                if (running) {
                    try {
                        helper.addListener(listener)
                        helper.requestRoute(RouteInfo(35.83472, 129.21917, poiName = "첨성대", poiId = "", address = "경주", poiSubId = ""))
                        poll.run()
                    } catch (reason: Exception) { error = reason.javaClass.simpleName; save() }
                }
            } }, { reason: Throwable -> main.post { error = reason.javaClass.simpleName; save() } })
        }
        try {
            val completed = arrived.await(60, TimeUnit.SECONDS)
            assertTrue("실제 내비 도착 미확인: ${if (report.exists()) report.readText() else "SDK 응답 없음"}", completed)
        } finally {
            test.runOnMainSync {
                running = false; main.removeCallbacksAndMessages(null)
                helper.cancelRoute(); helper.removeListener(listener); helper.release(); save()
            }
        }
    }
}
