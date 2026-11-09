// 시작 전 차량 상태와 재시작 뒤 상태 표시가 유지되는지 확인한다.
package com.kidquest.app.screen

import com.kidquest.app.screen.model.VehicleUiState
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Test

class VehicleUiStateTest {
    @Test fun unknownIsDifferentFromEmptyAndUnbuckled() {
        val initial = VehicleUiState()
        assertNull(initial.occupied[RearSeat.LEFT])
        val state = initial.accept(AppEvent.SeatOccupied(RearSeat.LEFT, false))
            .accept(AppEvent.BeltBuckled(RearSeat.LEFT, false))
        assertEquals(false, state.occupied[RearSeat.LEFT])
        assertEquals(false, state.buckled[RearSeat.LEFT])
    }

    @Test fun tripButtonsDoNotResetVehicleSnapshot() {
        val state = VehicleUiState().accept(AppEvent.SeatOccupied(RearSeat.LEFT, true))
            .accept(AppEvent.BeltBuckled(RearSeat.LEFT, true))
            .accept(AppEvent.GearChanged(Gear.D))
            .accept(AppEvent.ParentStopChanged(true))
        val restarted = state.accept(AppEvent.RestartPressed).accept(AppEvent.StartPressed)
        assertEquals(true, restarted.occupied[RearSeat.LEFT])
        assertEquals(true, restarted.buckled[RearSeat.LEFT])
        assertEquals(Gear.D, restarted.gear)
        assertFalse(restarted.parentStopped)
    }
}
