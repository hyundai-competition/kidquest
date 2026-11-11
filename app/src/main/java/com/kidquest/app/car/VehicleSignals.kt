// Vehicle SDK로 탑승, 벨트, 기어, 방향지시등, 와이퍼, 잔량(전기차 배터리, 없으면 연료), 실내 온도를 받아 사건으로 바꾼다.
// 값이 오는지는 에뮬레이터에서 아직 확인하지 않았다. 못 읽는 신호는 SignalUnavailable로 알리고 앱은 멈추지 않는다.
package com.kidquest.app.car

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.FuelPercentListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.HvacTemperatureValueListener
import ai.pleos.playground.vehicle.listener.SeatBeltBuckledStateListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.content.Context
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal
import com.kidquest.core.contract.RearSeat
import java.util.concurrent.atomic.AtomicBoolean

internal class VehicleSignals(context: Context, private val filter: CarEventFilter) {
    private val vehicle = Vehicle(context)

    /** 연료 잔량 구독은 한 번만 건다. 전기차 잔량 실패가 여러 번 와도 다시 걸지 않는다. */
    private val fuelStarted = AtomicBoolean(false)

    /** 전기차 잔량을 한 번이라도 받았으면 전기차다. 그 뒤의 실패는 잠깐 끊긴 것으로 보고 연료로 넘어가지 않는다. */
    @Volatile
    private var evReceived = false

    fun start() {
        try {
            vehicle.initialize()
        } catch (_: Exception) {
            VEHICLE_SIGNALS.forEach(filter::unavailable)
            return
        }
        guarded(CarSignal.GEAR, ::watchGear)
        guarded(CarSignal.SEAT, ::watchOccupancy)
        guarded(CarSignal.BELT, ::watchBelts)
        guarded(CarSignal.TURN_SIGNAL, ::watchTurnSignal)
        guarded(CarSignal.WIPER, ::watchWiper)
        guarded(CarSignal.ENERGY, ::watchEnergy)
        guarded(CarSignal.CABIN_TEMPERATURE, ::watchCabinTemperature)
    }

    fun stop() {
        try {
            vehicle.release()
        } catch (_: Exception) {
            // 끝나는 중이라 할 일이 없다.
        }
    }

    /** 권한이 없거나 함수가 없는 차에서 던지는 오류가 앱을 멈추지 않게 한다. */
    private fun guarded(signal: CarSignal, watch: () -> Unit) {
        try {
            watch()
        } catch (_: Exception) {
            filter.unavailable(signal)
        }
    }

    private fun sendGear(value: VehicleGear?) {
        CarSignalMapper.gear(value)?.let { filter.send(AppEvent.GearChanged(it)) }
    }

    private fun watchGear() {
        val gear = vehicle.getGear()
        val gate = CurrentValueGate<VehicleGear?>(::sendGear)
        gear.getCurrentGearSelectionState({ gate.current(it) }, { })
        gear.registerGearSelection(object : GearSelectionListener {
            override fun onSelected(selection: VehicleGear?) = gate.update(selection)
            override fun onFailed(e: Exception) = filter.unavailable(CarSignal.GEAR)
        })
    }

    /** 지원 범위에 있는 뒷좌석만 고른다. 하나도 없으면 그 신호를 못 읽는다고 알린다. */
    private fun rearSeatsIn(areas: List<SeatArea>, signal: CarSignal): List<RearSeat> {
        val supported = areas.flatMap { it.values.toList<SeatValue>() }
        val seats = RearSeat.entries.filter { CarSignalMapper.sdkSeat(it) in supported }
        if (seats.isEmpty()) filter.unavailable(signal)
        return seats
    }

    private fun watchOccupancy() {
        vehicle.getSeat().checkSeatOccupancyCapabilities(
            onComplete = { areas: List<SeatArea> ->
                rearSeatsIn(areas, CarSignal.SEAT).forEach { guarded(CarSignal.SEAT) { watchSeatOccupancy(it) } }
            },
            onFailed = { _: Exception -> filter.unavailable(CarSignal.SEAT) },
        )
    }

    private fun watchSeatOccupancy(seat: RearSeat) {
        val area = SeatArea(CarSignalMapper.sdkSeat(seat))
        val gate = CurrentValueGate<Boolean?> { occupied ->
            if (occupied != null) filter.send(AppEvent.SeatOccupied(seat, occupied))
        }
        vehicle.getSeat().getCurrentSeatOccupancy(area, { gate.current(it) }, { })
        vehicle.getSeat().registerSeatOccupancy(area, object : SeatOccupancyListener {
            override fun onSeatOccupancyUpdated(occupancy: Boolean?) = gate.update(occupancy)
            override fun onFailed(e: Exception) = filter.unavailable(CarSignal.SEAT)
        })
    }

    private fun watchBelts() {
        vehicle.getSeat().checkSeatBeltBuckledCapabilities(
            onComplete = { areas: List<SeatArea> ->
                rearSeatsIn(areas, CarSignal.BELT).forEach { guarded(CarSignal.BELT) { watchBelt(it) } }
            },
            onFailed = { _: Exception -> filter.unavailable(CarSignal.BELT) },
        )
    }

    private fun watchBelt(seat: RearSeat) {
        val area = SeatArea(CarSignalMapper.sdkSeat(seat))
        val gate = CurrentValueGate<Boolean?> { buckled ->
            if (buckled != null) filter.send(AppEvent.BeltBuckled(seat, buckled))
        }
        vehicle.getSeat().getCurrentSeatBeltBuckledState(area, { gate.current(it) }, { })
        vehicle.getSeat().registerSeatBeltBuckledState(area, object : SeatBeltBuckledStateListener {
            override fun onSeatBeltBuckledStateUpdated(state: Boolean?) = gate.update(state)
            override fun onFailed(e: Exception) = filter.unavailable(CarSignal.BELT)
        })
    }

    private fun watchTurnSignal() {
        vehicle.getTurnSignal().registerTurnSignalState(object : TurnSignalStateListener {
            override fun onTurnSignalStateUpdated(state: TurnSignalValue?) = filter.turnSignal(CarSignalMapper.turnSide(state))

            override fun onFailed(e: Exception) = filter.unavailable(CarSignal.TURN_SIGNAL)
        })
    }

    /** 비 오는 날은 앱을 켜기 전부터 와이퍼가 돌고 있다. 구독은 바뀔 때만 올 수 있어서 시작할 때 현재값을 한 번 읽는다. */
    private fun watchWiper() {
        val wiper = vehicle.getWiper()
        val front = WindowArea(WindowValue.FRONT_WINDSHIELD)
        val gate = CurrentValueGate<WiperState?> { state ->
            CarSignalMapper.wiperOn(state)?.let { filter.send(AppEvent.WiperChanged(it)) }
        }
        wiper.getCurrentWiperState(front, { gate.current(it) }, { })
        wiper.registerWiperState(front, object : WiperStateListener {
            override fun onWiperStateUpdated(state: WiperState?) = gate.update(state)
            override fun onFailed(e: Exception) = filter.unavailable(CarSignal.WIPER)
        })
    }

    private fun sendEnergy(percent: Int?) {
        if (percent != null) filter.send(AppEvent.EnergyChanged(percent))
    }

    /** 전기차 잔량을 먼저 받고, 못 받으면 연료 잔량을 받는다. 둘 다 못 받을 때만 못 읽는다고 알린다. */
    private fun watchEnergy() {
        try {
            vehicle.getEvBattery().registerEvBatteryStateOfCharge(object : EvBatteryStateOfChargeListener {
                override fun onEvBatteryStateOfChargeUpdated(value: Int?) {
                    if (value != null) evReceived = true
                    sendEnergy(value)
                }

                override fun onFailed(e: Exception) {
                    if (!evReceived) watchFuelOnce()
                }
            })
        } catch (_: Exception) {
            watchFuelOnce()
        }
    }

    private fun watchFuelOnce() {
        if (!fuelStarted.compareAndSet(false, true)) return
        guarded(CarSignal.ENERGY) {
            vehicle.getFuel().registerFuelPercent(object : FuelPercentListener {
                override fun onFuelPercentUpdated(value: Int?) = sendEnergy(value)
                override fun onFailed(e: Exception) = filter.unavailable(CarSignal.ENERGY)
            })
        }
    }

    private fun watchCabinTemperature() {
        val driver = SeatArea(SeatValue.ROW_1_LEFT)
        vehicle.getHVAC().registerHvacTemperatureCurrent(driver, object : HvacTemperatureValueListener {
            override fun onHvacTemperatureUpdated(celsius: Float?, fahrenheit: Float?) {
                if (celsius != null) filter.send(AppEvent.CabinTemperatureChanged(celsius))
            }

            override fun onFailed(e: Exception) = filter.unavailable(CarSignal.CABIN_TEMPERATURE)
        })
    }

    private companion object {
        val VEHICLE_SIGNALS = listOf(
            CarSignal.SEAT, CarSignal.BELT, CarSignal.GEAR, CarSignal.TURN_SIGNAL,
            CarSignal.WIPER, CarSignal.ENERGY, CarSignal.CABIN_TEMPERATURE,
        )
    }
}
