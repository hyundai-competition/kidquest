// FusedLocation의 근처 알림으로 에피소드 장소에 들어온 것을 알고 PlaceEntered로 보낸다.
// 장소마다 알림을 따로 걸어서, 원이 겹치면 들어간 원마다 보낸다. 같은 장소에 다시 들어와도 보내고, 엔진은 그 임무가 기다릴 때만 연다.
// 위치는 기기 안에서 좌표 비교에만 쓰고 저장하거나 밖으로 보내지 않는다. 앱에서 값이 오는지는 아직 시험하지 않았다.
package com.kidquest.app.car

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.ProximityAlertListener
import ai.pleos.playground.location.fused.model.ProximityAlertEvent
import ai.pleos.playground.location.fused.model.ProximityAlertRequest
import ai.pleos.playground.location.fused.model.ProximityTransition
import android.content.Context
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal
import com.kidquest.core.contract.Place
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

internal class PlaceSignals(
    context: Context,
    private val places: List<Place>,
    private val filter: CarEventFilter,
) {
    private val location = FusedLocation(context)
    private val alertIds = CopyOnWriteArrayList<String>()

    /** 알림 하나가 실패했다고 위치 전체를 못 읽는다고 하지 않는다. 모든 장소가 실패해야 알린다. 같은 장소의 오류는 한 번만 센다. */
    private val failedPlaces = ConcurrentHashMap.newKeySet<String>()

    fun start() {
        if (places.isEmpty()) return
        try {
            location.initialize()
            if (!location.isSupported()) {
                filter.unavailable(CarSignal.LOCATION)
                return
            }
        } catch (_: Exception) {
            filter.unavailable(CarSignal.LOCATION)
            return
        }
        places.forEach(::watch)
    }

    fun stop() {
        try {
            alertIds.forEach(location::removeProximityAlert)
            location.release()
        } catch (_: Exception) {
            // 끝나는 중이라 할 일이 없다.
        }
        alertIds.clear()
    }

    private fun watch(place: Place) {
        val request = ProximityAlertRequest(
            place.latitude,
            place.longitude,
            place.radiusMeters,
            setOf(ProximityTransition.ENTER),
        )
        try {
            location.addProximityAlert(request, object : ProximityAlertListener {
                override fun onRegistered(alertId: String) {
                    alertIds += alertId
                }

                override fun onTransition(event: ProximityAlertEvent) {
                    if (event.transition == ProximityTransition.ENTER) filter.send(AppEvent.PlaceEntered(place.id))
                }

                override fun onError(exception: Exception) = placeFailed(place)
            })
        } catch (_: Exception) {
            placeFailed(place)
        }
    }

    private fun placeFailed(place: Place) {
        if (failedPlaces.add(place.id) && failedPlaces.size == places.size) filter.unavailable(CarSignal.LOCATION)
    }
}
