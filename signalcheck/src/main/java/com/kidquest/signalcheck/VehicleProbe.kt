// Vehicle SDK 값이 실제로 오는지 본다. 콘솔에서 값을 바꾸면 줄이 찍혀야 한다.
// 문서와 컴파일로 함수 이름만 확인했고, 값이 오는지는 이 앱으로 처음 본다.
package com.kidquest.signalcheck

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.HvacTemperatureValueListener
import ai.pleos.playground.vehicle.listener.SeatBeltBuckledStateListener
import ai.pleos.playground.vehicle.listener.SeatBeltNotBuckledWarningListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.content.Context

class VehicleProbe(context: Context, private val log: SignalLog) {
    private val vehicle = Vehicle(context.applicationContext)
    private val rearSeats = listOf(SeatValue.ROW_2_LEFT, SeatValue.ROW_2_CENTER, SeatValue.ROW_2_RIGHT)

    fun start() {
        log.attempt("Vehicle 초기화") { vehicle.initialize() }
        watchGear()
        checkSeatCapabilities()
        rearSeats.forEach(::watchSeat)
        watchTurnSignalAndWiper()
        watchEnergyAndTemperature()
    }

    fun stop() {
        log.attempt("Vehicle 해제") { vehicle.release() }
    }

    private fun watchGear() = log.attempt("기어 등록") {
        vehicle.getGear().registerGearSelection(object : GearSelectionListener {
            override fun onSelected(selection: VehicleGear?) = log.line("기어", selection.toString())
            override fun onFailed(e: Exception) = log.line("기어", "실패 ${e.message}")
        })
    }

    /** 이 차에서 어느 자리 값을 읽을 수 있는지 먼저 본다. 뒷좌석이 목록에 있는지가 핵심이다. */
    private fun checkSeatCapabilities() {
        log.attempt("탑승 지원 범위") {
            vehicle.getSeat().checkSeatOccupancyCapabilities(
                onComplete = { areas: List<SeatArea> -> log.line("탑승 지원 범위", areas.toString()) },
                onFailed = { e: Exception -> log.line("탑승 지원 범위", "실패 ${e.message}") },
            )
        }
        log.attempt("벨트 지원 범위") {
            vehicle.getSeat().checkSeatBeltBuckledCapabilities(
                onComplete = { areas: List<SeatArea> -> log.line("벨트 지원 범위", areas.toString()) },
                onFailed = { e: Exception -> log.line("벨트 지원 범위", "실패 ${e.message}") },
            )
        }
    }

    private fun watchSeat(seat: SeatValue) {
        val area = SeatArea(seat)
        log.attempt("탑승 등록 $seat") {
            vehicle.getSeat().registerSeatOccupancy(area, object : SeatOccupancyListener {
                override fun onSeatOccupancyUpdated(occupancy: Boolean?) = log.line("탑승 $seat", occupancy.toString())
                override fun onFailed(e: Exception) = log.line("탑승 $seat", "실패 ${e.message}")
            })
        }
        log.attempt("벨트 등록 $seat") {
            vehicle.getSeat().registerSeatBeltBuckledState(area, object : SeatBeltBuckledStateListener {
                override fun onSeatBeltBuckledStateUpdated(state: Boolean?) = log.line("벨트 $seat", state.toString())
                override fun onFailed(e: Exception) = log.line("벨트 $seat", "실패 ${e.message}")
            })
        }
        log.attempt("벨트 경고 등록 $seat") {
            vehicle.getSafety().registerSeatBeltNotBuckledWarning(area, object : SeatBeltNotBuckledWarningListener {
                override fun onSeatBeltNotBuckledWarningUpdated(state: Boolean?) = log.line("벨트 경고 $seat", state.toString())
                override fun onFailed(e: Exception) = log.line("벨트 경고 $seat", "실패 ${e.message}")
            })
        }
    }

    private fun watchTurnSignalAndWiper() {
        log.attempt("방향지시등 등록") {
            vehicle.getTurnSignal().registerTurnSignalState(object : TurnSignalStateListener {
                override fun onTurnSignalStateUpdated(state: TurnSignalValue?) = log.line("방향지시등", state.toString())
                override fun onFailed(e: Exception) = log.line("방향지시등", "실패 ${e.message}")
            })
        }
        log.attempt("와이퍼 등록") {
            vehicle.getWiper().registerWiperState(WindowArea(WindowValue.FRONT_WINDSHIELD), object : WiperStateListener {
                override fun onWiperStateUpdated(state: WiperState?) = log.line("와이퍼", state.toString())
                override fun onFailed(e: Exception) = log.line("와이퍼", "실패 ${e.message}")
            })
        }
    }

    private fun watchEnergyAndTemperature() {
        log.attempt("잔량 등록") {
            vehicle.getEvBattery().registerEvBatteryStateOfCharge(object : EvBatteryStateOfChargeListener {
                override fun onEvBatteryStateOfChargeUpdated(value: Int?) = log.line("잔량", value.toString())
                override fun onFailed(e: Exception) = log.line("잔량", "실패 ${e.message}")
            })
        }
        log.attempt("실내 온도 등록") {
            vehicle.getHVAC().registerHvacTemperatureCurrent(SeatArea(SeatValue.ROW_1_LEFT), object : HvacTemperatureValueListener {
                override fun onHvacTemperatureUpdated(celsius: Float?, fahrenheit: Float?) = log.line("실내 온도", "$celsius")
                override fun onFailed(e: Exception) = log.line("실내 온도", "실패 ${e.message}")
            })
        }
    }
}
