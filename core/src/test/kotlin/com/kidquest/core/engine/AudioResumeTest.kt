// 포커스가 돌아와도 부모 끄기·벨트 중단을 지키고, 도착 대사는 현재 문장부터 재개하는지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Test

class AudioResumeTest {
    private val engine = MissionEngine()
    private fun send(vararg events: AppEvent) = events.flatMap(engine::onEvent)
    private fun fly() = send(AppEvent.StartPressed, AppEvent.SeatOccupied(RearSeat.LEFT, true),
        AppEvent.BeltBuckled(RearSeat.LEFT, true), AppEvent.GearChanged(Gear.D))

    @Test fun `포커스와 벨트가 돌아와도 부모가 끈 상태를 유지한다`() {
        fly()
        send(AppEvent.ParentStopChanged(true), AppEvent.AudioFocusChanged(false),
            AppEvent.BeltBuckled(RearSeat.LEFT, false), AppEvent.GearChanged(Gear.P), AppEvent.GearChanged(Gear.D))
        val resumed = send(AppEvent.AudioFocusChanged(true), AppEvent.BeltBuckled(RearSeat.LEFT, true))
        assertEquals(EngineState.PAUSED, engine.state)
        assertTrue(resumed.none { it is Command.Say || it is Command.Listen })
        send(AppEvent.ParentStopChanged(false))
        assertEquals(EngineState.FLYING, engine.state)
    }
    @Test fun `포커스가 돌아와도 벨트가 풀렸으면 임무를 재개하지 않는다`() {
        fly()
        send(AppEvent.AudioFocusChanged(false), AppEvent.BeltBuckled(RearSeat.LEFT, false))
        val after = send(AppEvent.AudioFocusChanged(true))
        assertEquals(EngineState.PAUSED, engine.state)
        assertTrue(after.none { it is Command.Listen })
        assertFalse(after.contains(Lines.TAKE_OFF))
    }
    @Test fun `도착 대사를 끊으면 포커스 복귀 때 처음부터 읽는다`() {
        fly()
        send(AppEvent.GearChanged(Gear.P), AppEvent.EndTripPressed)
        send(AppEvent.AudioFocusChanged(false))
        assertTrue(send(AppEvent.AudioFocusChanged(true)).contains(Lines.LANDED))
        send(AppEvent.LineFinished(Lines.LANDED.lineId), AppEvent.AudioFocusChanged(false))
        assertTrue(send(AppEvent.AudioFocusChanged(true)).none { it is Command.Say })
    }
    @Test fun `도착 후 포커스 복귀보다 부모 다시 켜기를 기다린다`() {
        fly()
        send(AppEvent.GearChanged(Gear.P), AppEvent.EndTripPressed, AppEvent.AudioFocusChanged(false), AppEvent.ParentStopChanged(true))
        assertTrue(send(AppEvent.AudioFocusChanged(true)).none { it is Command.Say })
        assertTrue(send(AppEvent.ParentStopChanged(false)).contains(Lines.LANDED))
    }
}
