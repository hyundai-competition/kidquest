// 수집된 최신 차량 값을 표시용으로 보관한다. 이륙과 멈춤 판단은 엔진만 한다.
package com.kidquest.app.screen.model

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.RearSeat

data class VehicleUiState(
    val occupied: Map<RearSeat, Boolean> = emptyMap(),
    val buckled: Map<RearSeat, Boolean> = emptyMap(),
    val gear: Gear? = null,
    val unavailable: Set<CarSignal> = emptySet(),
    val restricted: Boolean = false,
    val parentConfirmed: Boolean = false,
    val parentStopped: Boolean = false,
) {
    fun accept(event: AppEvent): VehicleUiState = when (event) {
        is AppEvent.SeatOccupied -> copy(occupied = occupied + (event.seat to event.occupied))
        is AppEvent.BeltBuckled -> copy(buckled = buckled + (event.seat to event.buckled))
        is AppEvent.GearChanged -> copy(gear = event.gear)
        is AppEvent.DrivingRestricted -> copy(restricted = event.restricted)
        is AppEvent.SignalUnavailable -> copy(unavailable = unavailable + event.signal)
        is AppEvent.ParentStopChanged -> copy(parentStopped = event.stopped)
        AppEvent.ParentConfirmedBelt -> copy(parentConfirmed = true)
        AppEvent.RestartPressed -> copy(parentStopped = false, parentConfirmed = false)
        else -> this
    }
}
