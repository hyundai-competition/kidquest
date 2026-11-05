// 위치와 근처 진입 알림을 본다. adb emu geo fix로 좌표를 바꾸면 위치 줄이 찍히고, 시험 좌표에 드나들면 ENTER와 EXIT가 와야 한다.
package com.kidquest.signalcheck

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.listener.ProximityAlertListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.location.fused.model.ProximityAlertEvent
import ai.pleos.playground.location.fused.model.ProximityAlertRequest
import ai.pleos.playground.location.fused.model.ProximityTransition
import android.content.Context

class LocationProbe(context: Context, private val log: SignalLog) {
    private val location = FusedLocation(context.applicationContext)

    private val locationEvents = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) = log.line("위치", location.toString())
        override fun onError(exception: Exception) = log.line("위치", "오류 ${exception.message}")
    }

    fun start() {
        log.attempt("위치 지원") { log.line("위치 지원", location.isSupported().toString()) }
        log.attempt("위치 등록") {
            location.initialize()
            location.registerFusedLocationCallback(locationEvents)
        }
        log.attempt("근처 알림 등록") {
            val request = ProximityAlertRequest(
                TEST_LATITUDE,
                TEST_LONGITUDE,
                TEST_RADIUS_METERS,
                setOf(ProximityTransition.ENTER, ProximityTransition.EXIT),
            )
            location.addProximityAlert(request, object : ProximityAlertListener {
                override fun onRegistered(alertId: String) = log.line("근처 알림", "등록 $alertId")
                override fun onTransition(event: ProximityAlertEvent) = log.line("근처 알림", event.toString())
                override fun onError(exception: Exception) = log.line("근처 알림", "오류 ${exception.message}")
            })
        }
    }

    fun stop() = log.attempt("위치 해제") {
        location.unregisterFusedLocationCallback(locationEvents)
        location.release()
    }

    private companion object {
        const val TEST_LATITUDE = 37.4979
        const val TEST_LONGITUDE = 127.0276
        const val TEST_RADIUS_METERS = 300f
    }
}
