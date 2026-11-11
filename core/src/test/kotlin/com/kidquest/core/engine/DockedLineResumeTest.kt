// 정박 대사(「잠깐 쉬어 갈게요…」)가 내비 안내에 끊기면 안내가 끝난 뒤 다시 말하는지 본다(#68).
// 다 말한 뒤에 끊긴 것, 끊긴 채로 출발한 것, 안내 중에 정박한 것은 다시 말하지 않거나 한 번만 말한다.
package com.kidquest.core.engine

import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Test

class DockedLineResumeTest {
    private val engine = MissionEngine()
    private fun send(vararg events: AppEvent) = events.flatMap(engine::onEvent)
    private fun fly() = send(AppEvent.StartPressed, AppEvent.SeatOccupied(RearSeat.LEFT, true),
        AppEvent.BeltBuckled(RearSeat.LEFT, true), AppEvent.GearChanged(Gear.D))
    private fun docked(commands: List<Command>) = commands.count { it == Lines.DOCKED }

    @Test fun `정박 대사가 내비 안내에 끊기면 안내가 끝난 뒤 다시 말한다`() {
        fly()
        assertEquals(1, docked(send(AppEvent.GearChanged(Gear.P))))
        assertTrue(send(AppEvent.AudioFocusChanged(false)).contains(Command.StopAudio))
        assertEquals(1, docked(send(AppEvent.AudioFocusChanged(true))))
        assertEquals(EngineState.DOCKED, engine.state)
    }

    @Test fun `정박 대사를 다 말한 뒤에 끊기면 다시 말하지 않는다`() {
        fly()
        send(AppEvent.GearChanged(Gear.P), AppEvent.LineFinished(Lines.DOCKED.lineId), AppEvent.AudioFocusChanged(false))
        assertEquals(0, docked(send(AppEvent.AudioFocusChanged(true))))
    }

    @Test fun `정박 대사가 끊긴 채로 출발하면 안내가 끝나도 정박 대사를 말하지 않는다`() {
        fly()
        send(AppEvent.GearChanged(Gear.P), AppEvent.AudioFocusChanged(false), AppEvent.GearChanged(Gear.D))
        assertEquals(0, docked(send(AppEvent.AudioFocusChanged(true))))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test fun `안내 중에 정박하면 정박 대사는 안내가 끝난 뒤 한 번만 말한다`() {
        fly()
        send(AppEvent.AudioFocusChanged(false))
        assertEquals(0, docked(send(AppEvent.GearChanged(Gear.P))))
        assertEquals(1, docked(send(AppEvent.AudioFocusChanged(true))))
    }

    @Test fun `부모가 끈 정박 대사는 다시 켜도 처음부터 다시 말하지 않는다`() {
        fly()
        send(AppEvent.GearChanged(Gear.P), AppEvent.ParentStopChanged(true))
        assertEquals(0, docked(send(AppEvent.ParentStopChanged(false))))
    }
}
