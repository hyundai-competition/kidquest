// 결말과 착륙 시험. 임무를 다 끝내면 기어 P에서 착륙하고, 모은 조각으로 이야기를 끝내는지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LandingTest : MissionTestBase() {
    /** 임무 a를 끝까지 해서 보상을 받는다. */
    private fun finishMissionA(): List<Command> {
        untilAnswer()
        send(AppEvent.AnswerHeard(null))
        return finish("a/answer")
    }

    @Test
    fun `임무를 다 끝낸 뒤 기어 P면 결말을 말하고 착륙한다`() {
        finishMissionA()
        val landing = send(AppEvent.GearChanged(Gear.P))
        assertEquals(EngineState.LANDED, engine.state)
        assertEquals(listOf("결말 문장"), said(landing))
        assertTrue(landing.contains(Command.ShowScreen(Screen.LANDED)))
        assertEquals("결말이 끝난 뒤에 착륙 대사를 말한다", listOf(Lines.LANDED.text), said(finish(Lines.ENDING_ID)))
    }

    @Test
    fun `임무가 남았으면 기어 P는 정박한다`() {
        untilAnswer()
        send(AppEvent.GearChanged(Gear.P))
        assertEquals(EngineState.DOCKED, engine.state)
    }

    @Test
    fun `도착했으면 임무가 남아도 착륙하고 결말은 말하지 않는다`() {
        untilBriefing()
        send(AppEvent.DestinationArrived)
        val landing = send(AppEvent.GearChanged(Gear.P))
        assertEquals(EngineState.LANDED, engine.state)
        assertEquals(listOf(Lines.LANDED.text), said(landing))
    }

    @Test
    fun `정박 중에 끝내기를 누르면 결말 없이 착륙한다`() {
        untilAnswer()
        send(AppEvent.GearChanged(Gear.P))
        val landing = send(AppEvent.EndTripPressed)
        assertEquals(EngineState.LANDED, engine.state)
        assertEquals(listOf(Lines.LANDED.text), said(landing))
    }

    @Test
    fun `결말 문장이 비어 있으면 착륙 대사만 말한다`() {
        engine = MissionEngine(episode(mission("a"), ending = ""))
        finishMissionA()
        assertEquals(listOf(Lines.LANDED.text), said(send(AppEvent.GearChanged(Gear.P))))
    }

    @Test
    fun `에피소드가 없으면 지금처럼 도착해야 착륙한다`() {
        engine = MissionEngine()
        takeOff()
        send(AppEvent.GearChanged(Gear.P))
        assertEquals(EngineState.DOCKED, engine.state)
    }

    @Test
    fun `착륙 화면에 보여 줄 아이템은 임무마다 한 번씩 넘긴다`() {
        engine = MissionEngine(episode(mission("a"), mission("b", reward = "map")))
        val awards = mutableListOf<Command>()
        untilAnswer()
        awards += send(AppEvent.AnswerHeard(null))
        awards += finish("a/answer")
        awards += finish("b/briefing") + tick(10) + tick(13)
        awards += finish("b/event") + finish("b/question") + send(AppEvent.AnswerHeard(null)) + finish("b/answer")
        awards += send(AppEvent.GearChanged(Gear.P)) + finish(Lines.ENDING_ID)
        assertEquals(
            listOf(Command.AwardItem("shield", "방어막 조각"), Command.AwardItem("map", "지도 조각")),
            awards.filterIsInstance<Command.AwardItem>(),
        )
        assertEquals(EngineState.LANDED, engine.state)
    }

    @Test
    fun `결말 중에 내비 안내가 오면 끝난 뒤에 결말을 말한다`() {
        finishMissionA()
        send(AppEvent.AudioFocusChanged(false))
        assertTrue("내비 안내 중에는 말하지 않는다", said(send(AppEvent.GearChanged(Gear.P))).isEmpty())
        assertEquals(listOf("결말 문장"), said(send(AppEvent.AudioFocusChanged(true))))
        assertEquals(listOf(Lines.LANDED.text), said(finish(Lines.ENDING_ID)))
    }

    @Test
    fun `착륙 뒤에는 다른 대사 끝남으로 착륙 대사를 말하지 않는다`() {
        finishMissionA()
        send(AppEvent.GearChanged(Gear.P))
        assertTrue(finish("a/answer").isEmpty())
        assertTrue(finish(Lines.TAKE_OFF.lineId).isEmpty())
    }

    @Test
    fun `다시 시작하면 결말 조건도 처음으로 돌아간다`() {
        finishMissionA()
        send(AppEvent.GearChanged(Gear.P), AppEvent.RestartPressed, AppEvent.StartPressed, AppEvent.GearChanged(Gear.D))
        finish(Lines.TAKE_OFF.lineId)
        send(AppEvent.GearChanged(Gear.P))
        assertEquals("새 이동에서는 임무가 남아 있다", EngineState.DOCKED, engine.state)
    }
}
