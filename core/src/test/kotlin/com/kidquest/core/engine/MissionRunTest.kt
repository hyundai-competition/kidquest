// 임무 진행 시험. 브리핑, 사건, 질문, 답, 보상이 순서대로 돌고, 답을 못 받아도 멈추지 않는지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Gear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionRunTest : MissionTestBase() {
    @Test
    fun `이륙 대사가 끝나면 브리핑을 말한다`() {
        val atTakeOff = takeOff()
        assertEquals("발사 대사와 겹치면 안 된다", listOf(Lines.TAKE_OFF.text), said(atTakeOff))
        assertEquals(listOf("a 브리핑"), said(finish(Lines.TAKE_OFF.lineId)))
    }

    @Test
    fun `한 임무가 끝까지 돈다`() {
        send(AppEvent.ListeningConsentChanged(true))
        assertEquals(listOf(Command.Say("a/briefing", "a 브리핑")), untilBriefing())
        assertEquals(
            listOf(Command.PlaySound("meteor"), Command.Say("a/event", "a 사건")),
            finish("a/briefing"),
        )
        assertEquals(listOf(Command.Say("a/question", "a 질문")), finish("a/event"))
        assertEquals(listOf(Command.Listen(listOf("초록색", "초록"), 8)), finish("a/question"))
        assertEquals(listOf(Command.Say("a/answer", "a 맞아요")), send(AppEvent.AnswerHeard("초록색")))
        assertEquals(listOf(Command.AwardItem("shield", "방어막 조각")), finish("a/answer"))
    }

    @Test
    fun `효과음이 없는 임무는 사건 대사만 말한다`() {
        engine = MissionEngine(episode(mission("a", sound = null)))
        untilBriefing()
        assertEquals(listOf(Command.Say("a/event", "a 사건")), finish("a/briefing"))
    }

    @Test
    fun `답 앞뒤에 다른 말이 붙어도 맞는다`() {
        untilAnswer()
        assertEquals(listOf("a 맞아요"), said(send(AppEvent.AnswerHeard("음 초록 색이요"))))
    }

    @Test
    fun `틀리면 혼내지 않고 정답을 말해 준 뒤 보상을 준다`() {
        untilAnswer()
        assertEquals(listOf("a 정답은 초록색"), said(send(AppEvent.AnswerHeard("빨간색"))))
        assertEquals(listOf(Command.AwardItem("shield", "방어막 조각")), finish("a/answer"))
    }

    @Test
    fun `못 알아들으면 정답을 말해 주고 넘어간다`() {
        untilAnswer()
        assertEquals(listOf("a 정답은 초록색"), said(send(AppEvent.AnswerHeard(null))))
    }

    @Test
    fun `동의하지 않으면 듣지 않고 8초 뒤 넘어간다`() {
        val atAnswer = untilAnswer(consent = false)
        assertTrue("듣기 명령이 나가면 안 된다", atAnswer.isEmpty())
        tick(100)
        assertTrue("7초에는 아직 안 넘어간다", tick(107).isEmpty())
        assertEquals(listOf("a 정답은 초록색"), said(tick(108)))
    }

    @Test
    fun `동의 전에는 기본으로 듣지 않는다`() {
        untilBriefing()
        finish("a/briefing")
        finish("a/event")
        assertTrue(finish("a/question").none { it is Command.Listen })
    }

    @Test
    fun `동의하지 않았는데 답이 와도 쓰지 않는다`() {
        untilAnswer(consent = false)
        assertTrue(send(AppEvent.AnswerHeard("초록색")).isEmpty())
    }

    @Test
    fun `듣는 중에 동의를 끄면 듣기를 멈추고 답을 쓰지 않는다`() {
        untilAnswer()
        assertEquals(listOf(Command.StopAudio), send(AppEvent.ListeningConsentChanged(false)))
        assertTrue(send(AppEvent.AnswerHeard("초록색")).isEmpty())
        tick(10)
        assertEquals(listOf("a 정답은 초록색"), said(tick(18)))
    }

    @Test
    fun `듣기 결과가 안 오면 제한 시간 뒤 듣기를 멈추고 넘어간다`() {
        untilAnswer()
        tick(50)
        assertTrue(tick(59).isEmpty())
        val late = tick(60)
        assertEquals(Command.StopAudio, late.first())
        assertEquals(listOf("a 정답은 초록색"), said(late))
        assertTrue("늦게 온 답은 쓰지 않는다", send(AppEvent.AnswerHeard("초록색")).isEmpty())
    }

    @Test
    fun `지금 대사가 아닌 대사 끝남은 진행하지 않는다`() {
        untilBriefing()
        assertTrue(finish("a/question").isEmpty())
        assertTrue(finish(Lines.TAKE_OFF.lineId).isEmpty())
        assertTrue(finish(Lines.BELT_OFF.lineId).isEmpty())
    }

    @Test
    fun `임무가 끝나면 다음 임무로 이어지고 다 끝나면 말하지 않는다`() {
        engine = MissionEngine(episode(mission("a"), mission("b", reward = "map")))
        untilAnswer()
        send(AppEvent.AnswerHeard(null))
        val next = finish("a/answer")
        assertEquals(listOf(Command.AwardItem("shield", "방어막 조각"), Command.Say("b/briefing", "b 브리핑")), next)
        finish("b/briefing")
        finish("b/event")
        finish("b/question")
        send(AppEvent.AnswerHeard(null))
        assertEquals(listOf(Command.AwardItem("map", "지도 조각")), finish("b/answer"))
        buckle(false)
        assertTrue("다 끝난 뒤에는 다시 말할 것이 없다", said(buckle(true)).isEmpty())
    }

    @Test
    fun `착륙 뒤 다시 시작하면 첫 임무부터 한다`() {
        untilAnswer()
        send(AppEvent.AnswerHeard(null))
        finish("a/answer")
        send(AppEvent.DestinationArrived, AppEvent.GearChanged(Gear.P), AppEvent.RestartPressed, AppEvent.StartPressed)
        send(AppEvent.GearChanged(Gear.D))
        assertEquals(listOf("a 브리핑"), said(finish(Lines.TAKE_OFF.lineId)))
    }

    @Test
    fun `에피소드가 없으면 임무 없이 상태 흐름만 돈다`() {
        engine = MissionEngine()
        takeOff()
        assertEquals(EngineState.FLYING, engine.state)
        assertTrue(finish(Lines.TAKE_OFF.lineId).isEmpty())
        assertTrue(buckle(false).contains(Command.StopAudio))
        assertTrue(said(buckle(true)).isEmpty())
    }
}
