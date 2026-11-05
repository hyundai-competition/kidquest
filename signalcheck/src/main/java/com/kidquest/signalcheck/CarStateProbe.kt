// AAOS Car API로 주행 규제 값과 기어를 본다. Pleos 정책 문서가 기어를 AAOS 쪽으로 분류해서 Vehicle SDK 값과 비교한다.
package com.kidquest.signalcheck

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context

class CarStateProbe(private val context: Context, private val log: SignalLog) {
    private var car: Car? = null

    fun start() {
        log.attempt("Car 연결") { car = Car.createCar(context.applicationContext) }
        val connected = car ?: return
        log.attempt("주행 규제 등록") {
            val manager = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            log.line("주행 규제", describe(manager.currentCarUxRestrictions))
            manager.registerListener { restrictions -> log.line("주행 규제", describe(restrictions)) }
        }
        log.attempt("AAOS 기어 등록") {
            val manager = connected.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            manager.registerCallback(gearCallback, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
        }
    }

    fun stop() {
        log.attempt("Car 해제") { car?.disconnect() }
    }

    private val gearCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) = log.line("AAOS 기어", value.value.toString())
        override fun onErrorEvent(propertyId: Int, zone: Int) = log.line("AAOS 기어", "오류 zone=$zone")
    }

    private fun describe(restrictions: CarUxRestrictions): String =
        "DO 필요=${restrictions.isRequiresDistractionOptimization} 제한값=${restrictions.activeRestrictions}"
}
