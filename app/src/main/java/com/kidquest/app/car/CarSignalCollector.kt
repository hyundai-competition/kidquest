// 차 신호를 AppEvent로 바꾸는 입구. 신호 종류별 일은 VehicleSignals, NaviSignals, DrivingStateSignals, PlaceSignals가 맡는다.
// 차 SDK는 car 폴더에서만 부른다. 에뮬레이터에서 값이 오는지는 아직 시험하지 않았다.
// 장소(places)는 에피소드 파일에서 받는다. 비어 있으면 위치 알림을 걸지 않는다.
// 보낸 사건은 KQCar 로그에 남긴다. 첫 시험에서 콘솔 값이 사건이 되는지 본다. 좌표는 남기지 않고 장소 id만 남는다.
package com.kidquest.app.car

import android.content.Context
import android.util.Log
import com.kidquest.core.contract.EventListener
import com.kidquest.core.contract.EventSource
import com.kidquest.core.contract.Place

class CarSignalCollector(context: Context, private val places: List<Place> = emptyList()) : EventSource {
    private val appContext = context.applicationContext
    private var vehicle: VehicleSignals? = null
    private var navi: NaviSignals? = null
    private var drivingState: DrivingStateSignals? = null
    private var place: PlaceSignals? = null

    override fun start(listener: EventListener) {
        val filter = CarEventFilter { event ->
            Log.i(CAR_LOG_TAG, event.toString())
            listener.onEvent(event)
        }
        vehicle = VehicleSignals(appContext, filter).also { it.start() }
        navi = NaviSignals(appContext, filter).also { it.start() }
        drivingState = DrivingStateSignals(appContext, filter).also { it.start() }
        place = PlaceSignals(appContext, places, filter).also { it.start() }
    }

    override fun stop() {
        vehicle?.stop()
        navi?.stop()
        drivingState?.stop()
        place?.stop()
        vehicle = null
        navi = null
        drivingState = null
        place = null
    }
}

/** car 폴더가 함께 쓰는 로그 태그. 로그 보기 : adb logcat -s KQCar */
internal const val CAR_LOG_TAG = "KQCar"
