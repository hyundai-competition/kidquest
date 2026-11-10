// 와이퍼 해제와 좌우 방향 장식의 만료를 검증한다. 방향 신호로 임무를 열지 않는다.
package com.kidquest.app.screen

import com.kidquest.app.screen.model.VehicleUiState
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Test

class VehicleWeatherTest {
    @Test fun weatherFollowsWiperAndDoesNotGuessActualWeather() {
        val on = VehicleUiState().accept(AppEvent.WiperChanged(true))
        assertTrue(on.wiperOn)
        assertFalse(on.accept(AppEvent.WiperChanged(false)).wiperOn)
    }
    @Test fun directionChangesAndExpiresAfterLatestSignal() {
        val left = VehicleUiState().accept(AppEvent.TurnSignalOn(TurnSide.LEFT), 1000)
        assertEquals(TurnSide.LEFT, left.accept(AppEvent.Tick(4499)).turnSide)
        val right = left.accept(AppEvent.TurnSignalOn(TurnSide.RIGHT), 3000)
        assertEquals(TurnSide.RIGHT, right.accept(AppEvent.Tick(4500)).turnSide)
        assertNull(right.accept(AppEvent.Tick(6500)).turnSide)
    }
}
