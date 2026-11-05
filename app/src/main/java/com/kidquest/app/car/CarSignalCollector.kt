// 차 신호를 AppEvent로 바꾸는 입구. 신호 종류별 일은 VehicleSignals, NaviSignals, DrivingStateSignals가 맡는다.
// 차 SDK는 car 폴더에서만 부른다. 에뮬레이터에서 값이 오는지는 아직 시험하지 않았다.
// 위치(PlaceEntered)는 에피소드 좌표가 정해지면 붙인다. 내연기관 차의 연료 잔량도 아직 받지 않는다.
package com.kidquest.app.car

import android.content.Context
import com.kidquest.core.contract.EventListener
import com.kidquest.core.contract.EventSource

class CarSignalCollector(context: Context) : EventSource {
    private val appContext = context.applicationContext
    private var vehicle: VehicleSignals? = null
    private var navi: NaviSignals? = null
    private var drivingState: DrivingStateSignals? = null

    override fun start(listener: EventListener) {
        val filter = CarEventFilter(listener)
        vehicle = VehicleSignals(appContext, filter).also { it.start() }
        navi = NaviSignals(appContext, filter).also { it.start() }
        drivingState = DrivingStateSignals(appContext, filter).also { it.start() }
    }

    override fun stop() {
        vehicle?.stop()
        navi?.stop()
        drivingState?.stop()
        vehicle = null
        navi = null
        drivingState = null
    }
}
