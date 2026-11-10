// 사용자에게 표시하는 음성 실패·상태 문구와 접근성 설명을 영어로 바꾸는지 확인한다.
package com.kidquest.app.screen

import org.junit.Assert.*
import org.junit.Test

class EnglishUiTest {
    @Test fun voiceFailuresAndDynamicDescriptionsAreTranslated() {
        listOf("마이크가 시작되지 않았어요. 기기 마이크 설정을 확인해 주세요.",
            "음성을 듣지 못했어요. 다시 시도할 수 있어요.", "마이크를 시작하지 못했어요. 다시 시도해 주세요.",
            "음성 인식을 준비하지 못했어요.", "이 기기에서 음성 인식을 시작하지 못했어요.",
            "이야기 언어 / Story language", "탐험 지도 보물 2 / 5").forEach {
            assertFalse(it, englishUi(it).any { c -> c in '가'..'힣' })
        }
        assertEquals("Adventure map treasures 2 / 5", englishUi("탐험 지도 보물 2 / 5"))
    }
}
