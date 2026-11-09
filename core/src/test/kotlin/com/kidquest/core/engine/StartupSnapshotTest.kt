// 차량 신호가 시작보다 먼저 와도 이륙하며, 재시작에도 최신 차량 값이 남는지 확인한다.
package com.kidquest.core.engine

import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Test

class StartupSnapshotTest {
    @Test fun alreadyBuckledInDriveTakesOffAfterStartAndRestart() {
        val engine = MissionEngine()
        engine.onEvent(AppEvent.SeatOccupied(RearSeat.LEFT, true))
        engine.onEvent(AppEvent.BeltBuckled(RearSeat.LEFT, true))
        engine.onEvent(AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.WAITING, engine.state)
        val start = engine.onEvent(AppEvent.StartPressed)
        assertTrue(start.any { it is Command.ShowScreen && it.screen == Screen.FLYING })
        engine.onEvent(AppEvent.GearChanged(Gear.P))
        if (engine.state == EngineState.DOCKED) engine.onEvent(AppEvent.EndTripPressed)
        assertEquals(EngineState.LANDED, engine.state)
        engine.onEvent(AppEvent.RestartPressed)
        engine.onEvent(AppEvent.StartPressed)
        assertEquals(EngineState.READY, engine.state)
        engine.onEvent(AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.FLYING, engine.state)
    }
}
