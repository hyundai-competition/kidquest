// 마이크 동의 없는 화면 선택은 채점하되, 동의 없는 음성·질문 전 선택·중단 중 답은 쓰지 않는다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import org.junit.Assert.*
import org.junit.Test

class ScreenAnswerTest : MissionTestBase() {
    @Test fun `동의 없이도 화면 정답은 맞아요 대사를 쓴다`() {
        assertTrue(untilAnswer(consent = false).isEmpty())
        assertEquals(listOf("a 맞아요"), said(send(AppEvent.AnswerSelected("초록색"))))
        assertTrue(send(AppEvent.AnswerSelected("빨간색")).isEmpty())
    }
    @Test fun `화면 오답과 무응답은 정답 안내를 쓴다`() {
        untilAnswer(consent = false)
        assertEquals(listOf("a 정답은 초록색"), said(send(AppEvent.AnswerSelected("빨간색"))))
    }
    @Test fun `질문을 아직 읽는 중인 화면 답은 받지 않는다`() {
        untilEventLine()
        finish("a/event")
        assertTrue(send(AppEvent.AnswerSelected("초록색")).isEmpty())
        finish("a/question")
        assertEquals(listOf("a 맞아요"), said(send(AppEvent.AnswerSelected("초록색"))))
    }
    @Test fun `동의를 철회한 음성은 무시하고 화면 답은 받는다`() {
        untilAnswer()
        send(AppEvent.ListeningConsentChanged(false))
        assertTrue(send(AppEvent.AnswerHeard("초록색")).isEmpty())
        assertEquals(listOf("a 맞아요"), said(send(AppEvent.AnswerSelected("초록색"))))
    }
    @Test fun `부모가 끈 동안 화면 답으로 임무를 진행하지 않는다`() {
        untilAnswer(consent = false)
        send(AppEvent.ParentStopChanged(true))
        assertTrue(send(AppEvent.AnswerSelected("초록색")).isEmpty())
    }
}
