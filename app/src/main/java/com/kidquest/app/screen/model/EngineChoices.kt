// 현재 대본의 명시적인 보기 어댑터. 질문에서 보기가 확인되지 않으면 원문 답변만 전달한다.
package com.kidquest.app.screen.model

import com.kidquest.core.contract.Mission

object EngineChoices {
    private val choices = mapOf(
        "rain_map" to listOf("파란색", "빨간색"), "crossroads" to listOf("별", "하트"),
        "cheonan_independence" to listOf("8월 15일", "3월 1일"), "daegu_debt" to listOf("나라 빚을 갚기", "나라 땅을 넓히기"),
        "suwon_hwaseong" to listOf("정조", "세종"), "daejeon_hanbat" to listOf("한밭", "한양"),
        "chungju_tangeumdae" to listOf("가야금", "거문고"), "mungyeong_saejae" to listOf("세 개", "다섯 개"),
        "danyang_dodam" to listOf("남한강", "낙동강"), "yeongju_buseoksa" to listOf("의상", "원효"),
        "gyeongju_star" to listOf("신라", "조선"), "busan_capital" to listOf("부산", "제주"),
        "andong_village" to listOf("한 집안", "여러 나라"), "seoul_palace" to listOf("조선", "신라"),
    )
    // 번호와 보기의 순서가 현재 질문의 순서와 같은지 실제 단어 위치로 확인한다.
    fun forMission(mission: Mission): List<String> {
        val options = choices[mission.id] ?: return emptyList()
        val terms = when (mission.id) { "daegu_debt" -> listOf("빚", "땅"); else -> options }
        val positions = terms.map { mission.question.indexOf(it) }
        return options.takeIf { positions.all { it >= 0 } && positions.zipWithNext().all { (a, b) -> a < b } }.orEmpty()
    }
}
