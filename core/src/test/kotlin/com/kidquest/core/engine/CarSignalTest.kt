// 차 신호 사건 시험. 와이퍼와 깜빡이가 임무 속 사건이 되고, 신호가 안 와도 임무가 멈추지 않는지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.RearSeat
import com.kidquest.core.contract.SignalReaction
import com.kidquest.core.contract.TurnSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarSignalTest : MissionTestBase() {
    private val thrusters = listOf(
        SignalReaction("turn_left", "thruster_left", null, 60),
        SignalReaction("turn_right", "thruster_right", null, 60),
    )

    private fun turn(side: TurnSide) = send(AppEvent.TurnSignalOn(side))

    @Test
    fun `와이퍼를 켜면 효과음 뒤 3초 있다가 사건을 말한다`() {
        untilBriefing()
        assertTrue(finish("a/briefing").isEmpty())
        assertEquals(listOf(Command.PlaySound("meteor")), wiper(true))
        tick(10)
        assertTrue(tick(12).isEmpty())
        assertEquals(listOf(Command.Say("a/event", "a 사건")), tick(13))
    }

    @Test
    fun `비 오는 날 와이퍼가 계속 켜져 있어도 임무가 열린다`() {
        wiper(true)
        untilBriefing()
        assertEquals(listOf(Command.PlaySound("meteor")), finish("a/briefing"))
    }

    @Test
    fun `브리핑 중에 와이퍼를 켜면 브리핑이 끝난 뒤에 열린다`() {
        untilBriefing()
        assertTrue("브리핑을 끊지 않는다", wiper(true).isEmpty())
        assertEquals(listOf(Command.PlaySound("meteor")), finish("a/briefing"))
    }

    @Test
    fun `와이퍼를 꺼 두면 켤 때까지 기다린다`() {
        wiper(true)
        wiper(false)
        untilBriefing()
        assertTrue(finish("a/briefing").isEmpty())
        assertEquals(listOf(Command.PlaySound("meteor")), wiper(true))
    }

    @Test
    fun `못 읽는 신호의 임무는 파일에 적힌 시간이 지나면 저절로 열린다`() {
        send(AppEvent.SignalUnavailable(CarSignal.WIPER))
        untilBriefing()
        finish("a/briefing")
        tick(0)
        assertTrue(tick(119).isEmpty())
        assertEquals(listOf(Command.PlaySound("meteor")), tick(120))
    }

    /** 와이퍼를 안 켠 맑은 날에 말할 대사가 있는 임무. */
    private fun withDryLine() {
        engine = MissionEngine(episode(mission("a").copy(fallbackEvent = "a 맑은 날")))
    }

    @Test
    fun `와이퍼 없이 시간이 지나 열리면 맑은 날 대사를 효과음 없이 바로 말한다`() {
        withDryLine()
        untilBriefing()
        finish("a/briefing")
        tick(0)
        assertEquals(listOf(Command.Say("a/event", "a 맑은 날")), tick(120))
        assertEquals("다음은 같은 질문이다", listOf("a 질문"), said(finish("a/event")))
    }

    @Test
    fun `맑은 날 대사가 있어도 와이퍼로 열리면 원래 사건 대사를 말한다`() {
        withDryLine()
        untilBriefing()
        finish("a/briefing")
        assertEquals(listOf(Command.PlaySound("meteor")), wiper(true))
        tick(10)
        assertEquals(listOf(Command.Say("a/event", "a 사건")), tick(13))
    }

    @Test
    fun `맑은 날 대사를 말하다 멈추면 이어 갈 때 맑은 날 대사를 다시 말한다`() {
        withDryLine()
        untilBriefing()
        finish("a/briefing")
        tick(0)
        tick(120)
        buckle(false)
        assertTrue(said(buckle(true)).contains("a 맑은 날"))
    }

    @Test
    fun `탑승과 벨트는 못 읽어도 저절로 넘어가지 않는다`() {
        send(AppEvent.SignalUnavailable(CarSignal.SEAT), AppEvent.SignalUnavailable(CarSignal.BELT))
        send(AppEvent.StartPressed)
        (0L..600L step 60).forEach { tick(it) }
        assertEquals(EngineState.CREW_CHECK, engine.state)
        send(AppEvent.SeatOccupied(RearSeat.LEFT, true), AppEvent.GearChanged(Gear.D))
        (600L..1200L step 60).forEach { tick(it) }
        assertEquals("벨트를 안 맸으면 이륙하지 않는다", EngineState.READY, engine.state)
    }

    @Test
    fun `깜빡이는 짧은 효과음만 낸다`() {
        engine = MissionEngine(episode(mission("a"), reactions = thrusters))
        untilBriefing()
        assertEquals(listOf(Command.PlaySound("thruster_left")), turn(TurnSide.LEFT))
    }

    @Test
    fun `같은 소리는 1분에 한 번만 낸다`() {
        engine = MissionEngine(episode(mission("a"), reactions = thrusters))
        untilBriefing()
        tick(10)
        assertEquals(listOf(Command.PlaySound("thruster_left")), turn(TurnSide.LEFT))
        tick(40)
        assertTrue(turn(TurnSide.LEFT).isEmpty())
        assertEquals("다른 쪽 소리는 따로 센다", listOf(Command.PlaySound("thruster_right")), turn(TurnSide.RIGHT))
        tick(70)
        assertEquals(listOf(Command.PlaySound("thruster_left")), turn(TurnSide.LEFT))
    }

    @Test
    fun `깜빡이 임무는 깜빡이를 켜면 열리고 반응 소리는 내지 않는다`() {
        engine = MissionEngine(episode(mission("a", trigger = MissionTrigger.TURN_SIGNAL), reactions = thrusters))
        untilBriefing()
        finish("a/briefing")
        assertEquals(listOf(Command.PlaySound("meteor")), turn(TurnSide.RIGHT))
    }

    @Test
    fun `멈춘 동안에는 깜빡이 소리를 내지 않는다`() {
        engine = MissionEngine(episode(mission("a"), reactions = thrusters))
        untilBriefing()
        buckle(false)
        assertTrue(turn(TurnSide.LEFT).isEmpty())
    }

    @Test
    fun `멈춘 동안 켠 와이퍼도 다시 비행하면 임무를 연다`() {
        untilBriefing()
        finish("a/briefing")
        buckle(false)
        assertTrue(wiper(true).none { it is Command.PlaySound })
        assertTrue(buckle(true).contains(Command.PlaySound("meteor")))
    }

    @Test
    fun `임무가 열린 뒤 와이퍼를 다시 켜도 같은 임무가 또 열리지 않는다`() {
        untilEventLine()
        wiper(false)
        assertTrue(wiper(true).isEmpty())
    }
}
