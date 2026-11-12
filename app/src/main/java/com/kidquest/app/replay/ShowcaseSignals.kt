// 시연 중 실제 차량 사건의 혼입을 막고, 종료할 때 복구할 최신 차량 값을 보관한다.
package com.kidquest.app.replay

import com.kidquest.app.screen.model.VehicleUiState
import com.kidquest.core.contract.*

class ShowcaseSignals(private val requirePark: Boolean = false) {
    var active = false
        private set
    var live = VehicleUiState()
        private set
    val allowed get() = !live.restricted && (live.gear == Gear.P || (!requirePark && live.gear == null))

    fun receive(event: AppEvent): Boolean {
        live = live.accept(event)
        return !active
    }
    fun select(enabled: Boolean): Boolean {
        if (enabled && !allowed) return false
        active = enabled
        return true
    }
    fun snapshot(): VehicleUiState = if (active) VehicleUiState(
        gear = Gear.P, occupied = mapOf(RearSeat.LEFT to true), buckled = mapOf(RearSeat.LEFT to true),
    ) else live

    fun restoreEvents(): List<AppEvent> {
        val state = snapshot()
        return RearSeat.entries.flatMap { seat -> listOf(
            AppEvent.SeatOccupied(seat, state.occupied[seat] == true),
            AppEvent.BeltBuckled(seat, state.buckled[seat] == true),
        ) } + listOf(
            // 모르는 기어를 P나 D로 간주하지 않는다.
            AppEvent.GearChanged(state.gear ?: Gear.N), AppEvent.WiperChanged(state.wiperOn),
            AppEvent.DrivingRestricted(state.restricted),
        ) + state.unavailable.map { AppEvent.SignalUnavailable(it) }
    }
}
