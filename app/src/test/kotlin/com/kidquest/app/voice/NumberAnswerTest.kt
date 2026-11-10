// 한국어 번호 답변과 건너뛰기만 허용하며 애매한 숫자·복수 답변은 추측하지 않는지 검증한다.
package com.kidquest.app.voice

import org.junit.Assert.*
import org.junit.Test

class NumberAnswerTest {
    @Test fun acceptsClearNumberPhrases() {
        listOf("1번!", "일 번", "첫 번째", "1번이요").forEach { assertEquals(it, 0, NumberAnswer.parse(it)) }
        listOf("2번", "이번이요", "두 번째").forEach { assertEquals(it, 1, NumberAnswer.parse(it)) }
        listOf("삼번", "3번이에요", "세 번째").forEach { assertEquals(it, 2, NumberAnswer.parse(it)) }
    }
    @Test fun acceptsEnglishNumberPhrasesWithoutGuessingMultipleAnswers() {
        listOf("one", "Number one!", "option 1").forEach { assertEquals(0, NumberAnswer.parse(it)) }
        assertEquals(1, NumberAnswer.parse("Two"))
        assertEquals(2, NumberAnswer.parse("number three"))
        assertNull(NumberAnswer.parse("one or two"))
    }
    @Test fun rejectsAmbiguousOrUnrelatedSpeech() {
        listOf("", "1번 아니고 2번", "12번", "4번", "오늘 3시에", "잘 모르겠어요").forEach { assertNull(it, NumberAnswer.parse(it)) }
        assertTrue(NumberAnswer.isSkip("건너뛰기"))
        assertFalse(NumberAnswer.isSkip("건너뛰지 마"))
    }
}
