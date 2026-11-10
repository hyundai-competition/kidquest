// 명확한 번호 답만 선택으로 바꾼다. 여러 번호·문장 속 숫자·부분 인식은 정답으로 추측하지 않는다.
package com.kidquest.app.voice

object NumberAnswer {
    fun parse(text: String): Int? {
        val word = text.lowercase().replace(Regex("[\\s.!?。]"), "")
        return when (word) {
            "one", "first", "numberone", "optionone", "number1", "option1", "1", "1번", "일번", "첫번째", "첫째", "하나", "1번이요", "일번이요", "1번이에요", "일번이에요" -> 0
            "two", "second", "numbertwo", "optiontwo", "number2", "option2", "2", "2번", "이번", "두번째", "둘째", "둘", "2번이요", "이번이요", "2번이에요", "이번이에요" -> 1
            "three", "third", "numberthree", "optionthree", "number3", "option3", "3", "3번", "삼번", "세번째", "셋째", "셋", "3번이요", "삼번이요", "3번이에요", "삼번이에요" -> 2
            else -> null
        }
    }
    fun isSkip(text: String) = text.replace(" ", "").trim() in setOf("건너뛰기", "건너뛰어", "넘어가", "다음으로")
}
