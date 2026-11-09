// 내비 갈림길 임무 시험. 내비가 알려 준 회전을 실제로 지났을 때만 열리고 깜빡이로는 안 열리는지,
// 회전 안내가 안 오는 이동에서는 시간이 지나면 열리는지, 갈림길을 못 지나고 뒤 장소에 오면 건너뛰는지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.Place
import com.kidquest.core.contract.TurnSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaviTurnTest : MissionTestBase() {
    private val opened = listOf(Command.PlaySound("meteor"))

    init {
        engine = MissionEngine(episode(mission("a", trigger = MissionTrigger.NAVI_TURN)))
    }

    private fun turnAt(meters: Int) = send(AppEvent.NextTurnDistance(meters))

    /** 브리핑이 끝나 갈림길을 기다리는 데까지 간다. */
    private fun waiting() {
        untilBriefing()
        finish("a/briefing")
    }

    /** 갈림길 임무 다음에 장소 임무가 있는 이동. */
    private fun withPlaceAfter() {
        engine = MissionEngine(
            episode(mission("a", trigger = MissionTrigger.NAVI_TURN), mission("b", sound = null, trigger = MissionTrigger.PLACE))
                .copy(places = listOf(Place("b_area", 36.0, 127.0, 3_000f, "b"))),
        )
    }

    @Test
    fun `가까이 온 회전을 지나면 갈림길 임무가 열린다`() {
        waiting()
        assertTrue(turnAt(800).isEmpty())
        assertTrue(turnAt(250).isEmpty())
        assertTrue("아직 안 지났다", turnAt(40).isEmpty())
        assertEquals(opened, turnAt(1_200))
    }

    @Test
    fun `회전이 가까이 오지 않았으면 거리가 늘어도 열리지 않는다`() {
        // 먼 회전의 거리가 경로를 다시 찾아 늘었을 뿐이다.
        waiting()
        turnAt(900)
        assertTrue(turnAt(1_500).isEmpty())
    }

    @Test
    fun `거리가 조금 흔들린 것은 지난 것으로 보지 않는다`() {
        waiting()
        turnAt(120)
        assertTrue(turnAt(190).isEmpty())
        assertEquals(opened, turnAt(900))
    }

    @Test
    fun `깜빡이로는 갈림길 임무가 열리지 않는다`() {
        waiting()
        assertTrue(send(AppEvent.TurnSignalOn(TurnSide.LEFT), AppEvent.TurnSignalOn(TurnSide.RIGHT)).isEmpty())
    }

    @Test
    fun `회전 안내가 오는 동안에는 파일의 시간이 지나도 열지 않고 갈림길을 기다린다`() {
        waiting()
        turnAt(2_000)
        tick(0)
        assertTrue(tick(300).isEmpty())
        turnAt(150)
        assertEquals(opened, turnAt(3_000))
    }

    @Test
    fun `회전 안내가 와도 갈림길을 10분 동안 못 지나면 연다`() {
        // 회전 거리가 드물게 오거나 이동 중에 안내를 꺼서 지난 것을 못 알아챌 때도 멈추지 않게 한다.
        waiting()
        turnAt(2_000)
        tick(0)
        assertTrue(tick(599).isEmpty())
        assertEquals(opened, tick(600))
    }

    @Test
    fun `회전 안내가 한 번도 안 오면 시간이 지나 열린다`() {
        // 경로 안내 없이 출발했거나 재생 패널로 돌릴 때다.
        waiting()
        tick(0)
        assertTrue(tick(119).isEmpty())
        assertEquals(opened, tick(120))
    }

    @Test
    fun `내비를 못 읽으면 회전 안내를 받았어도 시간이 지나 열린다`() {
        waiting()
        turnAt(2_000)
        send(AppEvent.SignalUnavailable(CarSignal.NAVIGATION))
        tick(0)
        assertEquals(opened, tick(120))
    }

    @Test
    fun `브리핑 중에 지난 갈림길로는 열지 않고 다음 갈림길을 기다린다`() {
        untilBriefing()
        turnAt(100)
        assertTrue("브리핑을 끊지 않는다", turnAt(900).isEmpty())
        assertTrue(finish("a/briefing").isEmpty())
        turnAt(200)
        assertEquals(opened, turnAt(1_000))
    }

    @Test
    fun `멈춘 동안 지난 갈림길로는 열지 않는다`() {
        waiting()
        buckle(false)
        turnAt(100)
        assertTrue(turnAt(900).none { it is Command.PlaySound })
        assertTrue("다시 비행해도 지난 갈림길은 기억하지 않는다", buckle(true).none { it is Command.PlaySound })
    }

    @Test
    fun `갈림길을 못 지나고 뒤 장소에 오면 갈림길 임무는 보물 없이 건너뛴다`() {
        withPlaceAfter()
        waiting()
        turnAt(2_000)
        val skipped = send(AppEvent.PlaceEntered("b_area"))
        assertTrue(skipped.none { it is Command.AwardItem })
        assertEquals(listOf("b 브리핑"), said(skipped))
        assertEquals(listOf("b 사건"), said(finish("b/briefing")))
    }

    @Test
    fun `도착이 가까우면 갈림길을 기다리지 않고 마지막 장소 임무로 간다`() {
        withPlaceAfter()
        waiting()
        turnAt(2_000)
        assertEquals(listOf("마지막"), said(send(AppEvent.RemainingTimeChanged(300))))
        assertEquals(listOf("b 브리핑"), said(finish(TripPlan.APPROACH_ID)))
    }

    @Test
    fun `다시 하면 회전 안내를 받았는지도 새로 센다`() {
        // 첫 이동에서 안내를 받았어도, 다음 이동에서 안내가 안 오면 시간으로 연다.
        waiting()
        turnAt(2_000)
        send(AppEvent.GearChanged(Gear.P), AppEvent.EndTripPressed)
        assertEquals(EngineState.LANDED, engine.state)
        send(AppEvent.RestartPressed, AppEvent.StartPressed, AppEvent.GearChanged(Gear.D))
        finish(Lines.TAKE_OFF.lineId)
        finish("a/briefing")
        tick(1_000)
        assertEquals(opened, tick(1_120))
    }
}
