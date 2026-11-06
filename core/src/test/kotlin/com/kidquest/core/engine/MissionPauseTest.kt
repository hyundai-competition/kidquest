// 임무 중에 멈췄다 이어지는 시험. 끊긴 문장을 처음부터 다시 말하고, 멈춘 동안에는 진행하지 않는지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Gear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionPauseTest : MissionTestBase() {
    @Test
    fun `벨트를 풀었다 매면 끊긴 문장을 처음부터 다시 말한다`() {
        untilBriefing()
        assertTrue(buckle(false).contains(Command.StopAudio))
        val back = buckle(true)
        assertEquals(listOf("a 브리핑"), said(back))
        assertEquals("겹치지 않게 먼저 멈춘다", Command.StopAudio, back[back.indexOfFirst { it is Command.Say } - 1])
    }

    @Test
    fun `내비 안내가 끝나면 끊긴 문장을 처음부터 다시 말한다`() {
        untilBriefing()
        finish("a/briefing")
        send(AppEvent.AudioFocusChanged(false))
        assertEquals(listOf("a 사건"), said(send(AppEvent.AudioFocusChanged(true))))
    }

    @Test
    fun `이어 말할 때 효과음은 다시 내지 않는다`() {
        untilBriefing()
        finish("a/briefing")
        buckle(false)
        assertTrue(buckle(true).none { it is Command.PlaySound })
    }

    @Test
    fun `답을 기다리다 멈추면 질문을 다시 하고 그 뒤에 듣는다`() {
        untilAnswer()
        buckle(false)
        assertEquals(listOf("a 질문"), said(buckle(true)))
        assertEquals(listOf(Command.Listen(listOf("초록색", "초록"), 8)), finish("a/question"))
    }

    @Test
    fun `질문을 다시 하는 중에 동의를 꺼도 질문이 끊기지 않는다`() {
        untilAnswer()
        buckle(false)
        buckle(true)
        assertTrue(send(AppEvent.ListeningConsentChanged(false)).isEmpty())
        assertTrue(finish("a/question").none { it is Command.Listen })
    }

    @Test
    fun `멈춘 동안에는 대사 끝남과 답과 시간을 쓰지 않는다`() {
        untilAnswer(consent = false)
        tick(1)
        buckle(false)
        assertTrue(said(tick(100)).isEmpty())
        assertTrue(said(finish("a/question")).isEmpty())
        assertTrue(said(send(AppEvent.AnswerHeard("초록색"))).isEmpty())
    }

    @Test
    fun `멈췄다 이어지면 8초를 다시 잰다`() {
        untilAnswer(consent = false)
        tick(100)
        tick(107)
        buckle(false)
        buckle(true)
        finish("a/question")
        assertTrue(tick(108).isEmpty())
        assertTrue(tick(115).isEmpty())
        assertEquals(listOf("a 정답은 초록색"), said(tick(116)))
    }

    @Test
    fun `답 대사 중에 멈춰도 보상은 한 번만 준다`() {
        untilAnswer()
        send(AppEvent.AnswerHeard("초록색"))
        buckle(false)
        assertEquals(listOf("a 맞아요"), said(buckle(true)))
        assertEquals(1, finish("a/answer").filterIsInstance<Command.AwardItem>().size)
        assertTrue(finish("a/answer").isEmpty())
    }

    @Test
    fun `이륙 대사가 끊겨도 다시 매면 브리핑을 시작한다`() {
        takeOff()
        buckle(false)
        assertEquals(listOf("a 브리핑"), said(buckle(true)))
    }

    @Test
    fun `내비 안내로 미룬 이륙 대사가 끝난 뒤에 브리핑을 시작한다`() {
        send(AppEvent.AudioFocusChanged(false))
        takeOff()
        val back = send(AppEvent.AudioFocusChanged(true))
        assertEquals("이륙 대사만 먼저 말한다", listOf(Lines.TAKE_OFF.text), said(back))
        assertEquals(listOf("a 브리핑"), said(finish(Lines.TAKE_OFF.lineId)))
    }

    @Test
    fun `부모가 소리를 껐다 켜면 끊긴 문장부터 이어진다`() {
        untilBriefing()
        send(AppEvent.ParentStopChanged(true))
        assertEquals(listOf("a 브리핑"), said(send(AppEvent.ParentStopChanged(false))))
    }

    @Test
    fun `정박했다 다시 출발하면 끊긴 문장부터 이어진다`() {
        untilBriefing()
        send(AppEvent.GearChanged(Gear.P))
        assertTrue("정박 중에는 진행하지 않는다", said(finish("a/briefing")).isEmpty())
        assertEquals(listOf("a 브리핑"), said(send(AppEvent.GearChanged(Gear.D))))
    }
}
