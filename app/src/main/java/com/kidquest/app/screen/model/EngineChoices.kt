// 현재 대본의 명시적인 보기 어댑터. 질문에서 보기가 확인되지 않으면 원문 답변만 전달한다.
package com.kidquest.app.screen.model

import com.kidquest.core.contract.Mission

object EngineChoices {
    private val choices = mapOf(
        "rain_map" to listOf("파란색", "빨간색"), "crossroads" to listOf("하트", "별"),
        "cheonan_independence" to listOf("8월 15일", "3월 1일"), "daegu_debt" to listOf("나라 빚을 갚기", "나라 땅을 넓히기"),
        "suwon_hwaseong" to listOf("세종", "정조"), "daejeon_hanbat" to listOf("한밭", "한양"),
        "chungju_tangeumdae" to listOf("거문고", "가야금"), "mungyeong_saejae" to listOf("세 개", "다섯 개"),
        "danyang_dodam" to listOf("낙동강", "남한강"), "yeongju_buseoksa" to listOf("의상", "원효"),
        "gyeongju_star" to listOf("조선", "신라"), "busan_capital" to listOf("부산", "제주"),
        "andong_village" to listOf("여러 나라", "한 집안"), "seoul_palace" to listOf("신라", "조선"),
    )
    private val english = mapOf(
        "rain_map" to listOf("blue", "red"), "crossroads" to listOf("heart", "star"),
        "cheonan_independence" to listOf("August 15", "March 1"), "daegu_debt" to listOf("repaying debt", "expanding territory"),
        "suwon_hwaseong" to listOf("Sejong", "Jeongjo"), "daejeon_hanbat" to listOf("Hanbat", "Hanyang"),
        "chungju_tangeumdae" to listOf("geomungo", "gayageum"), "mungyeong_saejae" to listOf("three", "five"),
        "danyang_dodam" to listOf("Nakdong", "Namhan"), "yeongju_buseoksa" to listOf("Uisang", "Wonhyo"),
        "gyeongju_star" to listOf("Joseon", "Silla"), "busan_capital" to listOf("Busan", "Jeju"),
        "andong_village" to listOf("many countries", "one clan"), "seoul_palace" to listOf("Silla", "Joseon"),
    )
    // 실제 질문의 보기 순서가 일치할 때만 번호를 연결한다.
    fun forMission(mission: Mission): List<String> {
        for (table in listOf(choices, english)) {
            val options = table[mission.id] ?: continue
            val terms = if (table === choices && mission.id == "daegu_debt") listOf("빚", "땅") else options
            val positions = terms.map { mission.question.indexOf(it, ignoreCase = true) }
            if (positions.all { it >= 0 } && positions.zipWithNext().all { (a, b) -> a < b }) return options
        }
        return emptyList()
    }
}
