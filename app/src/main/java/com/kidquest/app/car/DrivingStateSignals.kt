// AAOS Car API로 주행 규제 값을 받아 사건으로 바꾼다. 화면을 줄일지 정하는 데 쓴다.
package com.kidquest.app.car

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal

internal class DrivingStateSignals(private val context: Context, private val filter: CarEventFilter) {
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    fun start() {
        try {
            car = Car.createCar(context)
            manager = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
            val connected = manager
            if (connected == null) {
                filter.unavailable(CarSignal.DRIVING_STATE)
                return
            }
            send(connected.currentCarUxRestrictions)
            connected.registerListener(::send)
        } catch (_: Exception) {
            filter.unavailable(CarSignal.DRIVING_STATE)
        } catch (_: LinkageError) {
            // android.car 라이브러리가 없는 기기다. 일반 안드로이드 에뮬레이터가 여기에 든다.
            filter.unavailable(CarSignal.DRIVING_STATE)
        }
    }

    fun stop() {
        try {
            manager?.unregisterListener()
            car?.disconnect()
        } catch (_: Exception) {
            // 끝나는 중이라 할 일이 없다.
        }
    }

    private fun send(restrictions: CarUxRestrictions) {
        filter.send(AppEvent.DrivingRestricted(restrictions.isRequiresDistractionOptimization))
    }
}
