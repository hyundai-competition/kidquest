// 실제 이동에서 깨지기 쉬운 순서를 시험한다. 정박, 내비 안내와 겹침, 카시트, 같은 값 반복을 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.RearSeat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionEngineEdgeTest {
    private val engine = MissionEngine()
    private val left = RearSeat.LEFT
    private val right = RearSeat.RIGHT

    private fun send(vararg events: AppEvent): List<Command> = events.flatMap { engine.onEvent(it) }

    private fun flying() {
        send(
            AppEvent.StartPressed,
            AppEvent.SeatOccupied(left, true),
            AppEvent.BeltBuckled(left, true),
            AppEvent.GearChanged(Gear.D),
        )
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `내비 안내 중에 기어를 넣으면 이륙 대사를 미뤘다가 안내가 끝나면 말한다`() {
        send(
            AppEvent.StartPressed,
            AppEvent.SeatOccupied(left, true),
            AppEvent.BeltBuckled(left, true),
            AppEvent.AudioFocusChanged(false),
        )
        val during = send(AppEvent.GearChanged(Gear.D))
        assertFalse(during.contains(Lines.TAKE_OFF))
        assertEquals(EngineState.PAUSED, engine.state)
        val after = send(AppEvent.AudioFocusChanged(true))
        assertTrue(after.contains(Lines.TAKE_OFF))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `내비 안내 중에 벨트가 풀리면 안내가 끝난 뒤 벨트 안내를 한다`() {
        flying()
        val during = send(AppEvent.AudioFocusChanged(false), AppEvent.BeltBuckled(left, false))
        assertFalse(during.contains(Lines.BELT_OFF))
        val after = send(AppEvent.AudioFocusChanged(true))
        assertEquals(EngineState.PAUSED, engine.state)
        assertTrue(after.contains(Lines.BELT_OFF))
    }

    @Test
    fun `내비 안내 중에 벨트를 다시 매면 미룬 벨트 안내는 하지 않는다`() {
        flying()
        send(AppEvent.AudioFocusChanged(false), AppEvent.BeltBuckled(left, false), AppEvent.BeltBuckled(left, true))
        val after = send(AppEvent.AudioFocusChanged(true))
        assertFalse(after.contains(Lines.BELT_OFF))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `부모 확인은 값이 안 바뀌는 자리만 대신하고 값이 읽히는 자리를 풀면 멈춘다`() {
        send(
            AppEvent.StartPressed,
            AppEvent.SeatOccupied(left, true),
            AppEvent.SeatOccupied(right, true),
            AppEvent.BeltBuckled(right, true),
            AppEvent.ParentConfirmedBelt,
            AppEvent.GearChanged(Gear.D),
        )
        assertEquals(EngineState.FLYING, engine.state)
        send(AppEvent.BeltBuckled(right, false))
        assertEquals(EngineState.PAUSED, engine.state)
    }

    @Test
    fun `카시트 자리에서 안 매짐 값이 다시 와도 멈추지 않는다`() {
        send(
            AppEvent.StartPressed,
            AppEvent.SeatOccupied(left, true),
            AppEvent.ParentConfirmedBelt,
            AppEvent.GearChanged(Gear.D),
        )
        send(AppEvent.BeltBuckled(left, false))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `탑승 값을 못 읽어도 부모 확인으로 출발한다`() {
        send(AppEvent.StartPressed, AppEvent.ParentConfirmedBelt, AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `부모 확인이 없으면 탑승과 벨트를 못 읽는 차는 저절로 이륙하지 않는다`() {
        send(AppEvent.StartPressed, AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.CREW_CHECK, engine.state)
    }

    @Test
    fun `값이 모두 맞으면 어떤 순서로 돌아와도 멈춤에서 빠져나온다`() {
        flying()
        send(
            AppEvent.BeltBuckled(left, false),
            AppEvent.SeatOccupied(left, false),
            AppEvent.BeltBuckled(left, true),
            AppEvent.SeatOccupied(left, true),
        )
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `비행 중에 벨트 안 맨 둘째가 타면 멈춘다`() {
        flying()
        send(AppEvent.SeatOccupied(right, true))
        assertEquals(EngineState.PAUSED, engine.state)
    }

    @Test
    fun `발사 준비에서 자리가 비면 승무원 확인으로 돌아간다`() {
        send(AppEvent.StartPressed, AppEvent.SeatOccupied(left, true), AppEvent.SeatOccupied(left, false))
        assertEquals(EngineState.CREW_CHECK, engine.state)
    }

    @Test
    fun `같은 벨트 값이 두 번 와도 발사 준비 안내는 한 번이다`() {
        send(AppEvent.StartPressed, AppEvent.SeatOccupied(left, true))
        val out = send(AppEvent.BeltBuckled(left, true), AppEvent.BeltBuckled(left, true))
        assertEquals(1, out.count { it == Lines.READY })
    }

    @Test
    fun `비어 있는 다른 자리 벨트 값이 와도 발사 준비 안내를 또 하지 않는다`() {
        send(AppEvent.StartPressed, AppEvent.SeatOccupied(left, true), AppEvent.BeltBuckled(left, true))
        val out = send(AppEvent.BeltBuckled(right, true))
        assertFalse(out.contains(Lines.READY))
    }

    @Test
    fun `풀었다 다시 매면 발사 준비를 다시 말한다`() {
        send(AppEvent.StartPressed, AppEvent.SeatOccupied(left, true), AppEvent.BeltBuckled(left, true))
        val out = send(AppEvent.BeltBuckled(left, false), AppEvent.BeltBuckled(left, true))
        assertEquals(1, out.count { it == Lines.READY })
    }

    @Test
    fun `벨트를 이미 맨 채 시작하면 칭찬하고 기어를 기다린다`() {
        send(AppEvent.SeatOccupied(left, true), AppEvent.BeltBuckled(left, true))
        val out = send(AppEvent.StartPressed)
        assertTrue(out.contains(Lines.ALREADY_READY))
        assertFalse(out.contains(Lines.CREW_FOUND))
        assertEquals(EngineState.READY, engine.state)
    }

    private fun said(out: List<Command>) = out.filterIsInstance<Command.Say>()

    @Test
    fun `기어가 이미 D이고 벨트를 맨 채 시작하면 발사 대사 하나만 말하고 이륙한다`() {
        send(AppEvent.SeatOccupied(left, true), AppEvent.BeltBuckled(left, true), AppEvent.GearChanged(Gear.D))
        val out = send(AppEvent.StartPressed)
        assertEquals(listOf(Lines.TAKE_OFF), said(out))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `기어가 이미 D인데 벨트를 안 맸으면 승무원 확인 대사 하나만 말한다`() {
        send(AppEvent.SeatOccupied(left, true), AppEvent.GearChanged(Gear.D))
        val out = send(AppEvent.StartPressed)
        assertEquals(listOf(Lines.CREW_FOUND), said(out))
        assertEquals(EngineState.READY, engine.state)
        assertEquals(listOf(Lines.TAKE_OFF), said(send(AppEvent.BeltBuckled(left, true))))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `착륙 뒤 기어를 D로 둔 채 다시 하면 발사 대사 하나만 말하고 이륙한다`() {
        flying()
        send(AppEvent.GearChanged(Gear.P), AppEvent.EndTripPressed)
        assertEquals(EngineState.LANDED, engine.state)
        send(AppEvent.GearChanged(Gear.D), AppEvent.RestartPressed)
        val out = send(AppEvent.StartPressed)
        assertEquals(listOf(Lines.TAKE_OFF), said(out))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `벨트 안내는 세 번까지만 한다`() {
        flying()
        val out = send(
            AppEvent.BeltBuckled(left, false), AppEvent.BeltBuckled(left, true),
            AppEvent.BeltBuckled(left, false), AppEvent.BeltBuckled(left, true),
            AppEvent.BeltBuckled(left, false), AppEvent.BeltBuckled(left, true),
            AppEvent.BeltBuckled(left, false),
        )
        assertEquals(3, out.count { it == Lines.BELT_OFF })
        assertEquals(EngineState.PAUSED, engine.state)
    }

    @Test
    fun `도착 전 기어 P는 정박이고 다시 D면 이어진다`() {
        flying()
        val parked = send(AppEvent.GearChanged(Gear.P))
        assertEquals(EngineState.DOCKED, engine.state)
        assertTrue(parked.contains(Lines.DOCKED))
        send(AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `정박 중에 벨트를 풀었으면 출발할 때 멈추고 매면 이어진다`() {
        flying()
        send(AppEvent.GearChanged(Gear.P), AppEvent.BeltBuckled(left, false))
        assertEquals(EngineState.DOCKED, engine.state)
        val out = send(AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.PAUSED, engine.state)
        assertTrue(out.contains(Lines.BELT_OFF))
        send(AppEvent.BeltBuckled(left, true))
        assertEquals(EngineState.FLYING, engine.state)
    }

    @Test
    fun `정박 중에 도착 값이 오거나 부모가 끝내면 착륙한다`() {
        flying()
        send(AppEvent.GearChanged(Gear.P), AppEvent.DestinationArrived)
        assertEquals(EngineState.LANDED, engine.state)

        send(AppEvent.RestartPressed, AppEvent.StartPressed, AppEvent.GearChanged(Gear.D))
        assertEquals(EngineState.FLYING, engine.state)
        send(AppEvent.GearChanged(Gear.P), AppEvent.EndTripPressed)
        assertEquals(EngineState.LANDED, engine.state)
    }

    @Test
    fun `부모가 끄면 멈추고 다시 켜야 이어지고 꺼 둔 동안에는 말하지 않는다`() {
        flying()
        val off = send(AppEvent.ParentStopChanged(true))
        assertEquals(EngineState.PAUSED, engine.state)
        assertTrue(off.contains(Command.StopAudio))
        val belt = send(AppEvent.BeltBuckled(left, false))
        assertFalse(belt.contains(Lines.BELT_OFF))
        send(AppEvent.BeltBuckled(left, true), AppEvent.ParentStopChanged(false))
        assertEquals(EngineState.FLYING, engine.state)
    }
}
