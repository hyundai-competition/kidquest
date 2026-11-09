// 남은 시간, 실내 온도, 앞길 시험. 도착 직전 문장을 대사를 끊지 않고 한 번만 말하는지,
// 배경은 띠가 바뀔 때만 바꾸는지, 앞길 효과음은 가까울 때 쿨다운 안에서 한 번만 내는지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Background
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Destination
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.RoadKind
import com.kidquest.core.contract.SignalReaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripSignalsTest : MissionTestBase() {
    private fun remaining(seconds: Int) = send(AppEvent.RemainingTimeChanged(seconds))

    /** 첫 임무 브리핑이 끝나 와이퍼를 기다리는 데까지 간다. */
    private fun waitingForWiper() {
        untilBriefing()
        finish("a/briefing")
    }

    @Test
    fun `남은 시간이 5분 이하가 되면 신호를 기다리는 중에 도착 직전 문장을 말한다`() {
        waitingForWiper()
        assertEquals(listOf("마지막"), said(remaining(290)))
    }

    @Test
    fun `목적지를 고르면 그 목적지의 도착 직전 문장을 말한다`() {
        val gyeongju = Destination("gyeongju", "서울 → 경주", "경주", "경주 출발", "경주 접근", "경주 전달", "경주 사실", "시험 출처")
        engine = MissionEngine(episode(mission("a")).copy(destinations = listOf(gyeongju)))
        send(AppEvent.DestinationSelected("gyeongju"))
        untilBriefing()
        finish(TripPlan.DEPARTURE_ID)
        finish("a/briefing")
        assertEquals(listOf("경주 접근"), said(remaining(200)))
    }

    @Test
    fun `말하는 중이면 끊지 않고 신호를 기다리기 시작할 때 말한다`() {
        untilBriefing()
        assertTrue(said(remaining(200)).isEmpty())
        assertEquals(listOf("마지막"), said(finish("a/briefing")))
    }

    @Test
    fun `한 번만 말하고 5분보다 길거나 값이 없으면 말하지 않는다`() {
        waitingForWiper()
        assertTrue(said(remaining(600)).isEmpty())
        assertTrue(said(remaining(0)).isEmpty())
        assertEquals(listOf("마지막"), said(remaining(200)))
        finish(TripPlan.APPROACH_ID)
        assertTrue(said(remaining(100)).isEmpty())
    }

    @Test
    fun `도착 직전 문장을 말하는 동안 켠 와이퍼는 문장이 끝나면 임무를 연다`() {
        waitingForWiper()
        remaining(200)
        assertTrue(wiper(true).none { it is Command.PlaySound })
        assertTrue(finish(TripPlan.APPROACH_ID).contains(Command.PlaySound("meteor")))
    }

    @Test
    fun `신호를 기다린 시간은 도착 직전 문장 뒤에도 이어서 센다`() {
        waitingForWiper()
        tick(0)
        tick(100)
        remaining(200)
        finish(TripPlan.APPROACH_ID)
        assertTrue("120초가 지나면 저절로 열린다", tick(121).contains(Command.PlaySound("meteor")))
    }

    @Test
    fun `멈춘 동안 온 남은 시간은 다시 이어질 때 말한다`() {
        waitingForWiper()
        send(AppEvent.ParentStopChanged(true))
        assertTrue(said(remaining(200)).isEmpty())
        assertTrue(said(send(AppEvent.ParentStopChanged(false))).contains("마지막"))
    }

    @Test
    fun `출발 전이나 보물을 다 찾은 뒤에는 말하지 않는다`() {
        send(AppEvent.StartPressed)
        assertTrue(said(remaining(200)).isEmpty())

        engine = MissionEngine(episode(mission("a")))
        untilAnswer()
        send(AppEvent.AnswerHeard("초록색"))
        finish("a/answer")
        assertTrue(said(remaining(200)).isEmpty())
    }

    @Test
    fun `다시 하기 뒤에는 도착 직전 문장을 다시 말할 수 있다`() {
        waitingForWiper()
        remaining(200)
        send(AppEvent.DestinationArrived, AppEvent.GearChanged(Gear.P), AppEvent.RestartPressed)
        send(AppEvent.StartPressed, AppEvent.GearChanged(Gear.D))
        finish(Lines.TAKE_OFF.lineId)
        finish("a/briefing")
        assertEquals(listOf("마지막"), said(remaining(250)))
    }

    @Test
    fun `실내 온도는 띠가 바뀔 때만 착륙 화면 배경을 바꾼다`() {
        assertEquals(listOf(Command.SetBackground(Background.COOL)), send(AppEvent.CabinTemperatureChanged(19.1f)))
        assertTrue(send(AppEvent.CabinTemperatureChanged(18f)).isEmpty())
        assertEquals(listOf(Command.SetBackground(Background.NORMAL)), send(AppEvent.CabinTemperatureChanged(23f)))
        assertEquals(listOf(Command.SetBackground(Background.WARM)), send(AppEvent.CabinTemperatureChanged(27.5f)))
    }

    @Test
    fun `앞길 터널이 가까우면 효과음을 내고 쿨다운 안에서는 다시 내지 않는다`() {
        val tunnel = SignalReaction("road_tunnel", "wormhole", null, 60)
        engine = MissionEngine(episode(mission("a"), reactions = listOf(tunnel)))
        untilBriefing()
        tick(0)
        assertTrue(send(AppEvent.RoadAhead(RoadKind.TUNNEL, 800)).isEmpty())
        assertEquals(listOf(Command.PlaySound("wormhole")), send(AppEvent.RoadAhead(RoadKind.TUNNEL, 250)))
        assertTrue(send(AppEvent.RoadAhead(RoadKind.TUNNEL, 120)).isEmpty())
        tick(70)
        assertEquals(listOf(Command.PlaySound("wormhole")), send(AppEvent.RoadAhead(RoadKind.TUNNEL, 90)))
    }
}
