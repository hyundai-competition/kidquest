// 상태 흐름 시험. 벨트와 기어의 순서가 달라도 PRD대로 움직이는지 본다. 에뮬레이터가 필요 없다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.RearSeat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionEngineTest {
    private val engine = MissionEngine()

    private fun send(vararg events: AppEvent): List<Command> = events.flatMap { engine.onEvent(it) }

    private fun readyWithLeftSeat() {
        send(AppEvent.StartPressed, AppEvent.SeatOccupied(RearSeat.LEFT, true))
        assertEquals(EngineState.READY, engine.state)
    }

    private fun flying() {
        readyWithLeftSeat()
        send(AppEvent.BeltBuckled(RearSeat.LEFT, true), AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `뒷좌석에 아무도 없으면 승무원 확인에서 멈춘다`() {
        send(AppEvent.StartPressed, AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.CREW_CHECK, engine.state)
    }

    @Test
    fun `벨트를 매고 기어를 넣으면 이륙한다`() = flying()

    @Test
    fun `R을 거쳐 D로 가도 이륙한다`() {
        readyWithLeftSeat()
        send(AppEvent.BeltBuckled(RearSeat.LEFT, true), AppEvent.GearChanged(Gear.R), AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `기어를 먼저 넣으면 안내만 하다가 벨트를 맨 순간 이륙한다`() {
        readyWithLeftSeat()
        val guide = send(AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.READY, engine.state)
        assertTrue(guide.contains(Command.Say(Lines.NEED_BELT)))
        send(AppEvent.BeltBuckled(RearSeat.LEFT, true))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `탑승한 자리의 벨트가 모두 매져야 이륙한다`() {
        readyWithLeftSeat()
        send(AppEvent.SeatOccupied(RearSeat.RIGHT, true), AppEvent.BeltBuckled(RearSeat.LEFT, true), AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.READY, engine.state)
        send(AppEvent.BeltBuckled(RearSeat.RIGHT, true))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `부모 확인은 벨트 값을 대신한다`() {
        readyWithLeftSeat()
        send(AppEvent.ParentConfirmedBelt, AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `벨트를 풀면 멈추고 다시 매면 이어진다`() {
        flying()
        val off = send(AppEvent.BeltBuckled(RearSeat.LEFT, false))
        assertEquals(EngineState.PAUSED, engine.state)
        assertTrue(off.contains(Command.StopAudio))
        send(AppEvent.BeltBuckled(RearSeat.LEFT, true))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `내비 안내로 멈췄다가 벨트까지 풀리면 둘 다 풀려야 이어진다`() {
        flying()
        send(AppEvent.AudioFocusChanged(false), AppEvent.BeltBuckled(RearSeat.LEFT, false))
        send(AppEvent.AudioFocusChanged(true))
        assertEquals(EngineState.PAUSED, engine.state)
        send(AppEvent.BeltBuckled(RearSeat.LEFT, true))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `R과 N에서는 쉬고 D로 돌아오면 이어진다`() {
        flying()
        send(AppEvent.GearChanged(Gear.N))
        assertEquals(EngineState.PAUSED, engine.state)
        send(AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `멈춘 상태에서도 기어 P면 착륙하고 다시 하기로 돌아간다`() {
        flying()
        send(AppEvent.BeltBuckled(RearSeat.LEFT, false), AppEvent.GearChanged(Gear.P))
        assertEquals(EngineState.LANDED, engine.state)
        send(AppEvent.RestartPressed)
        assertEquals(EngineState.WAITING, engine.state)
    }
}
