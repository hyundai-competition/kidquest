// 약속 3. 에피소드 파일의 형식을 적는다. 예시는 app/src/main/assets/episodes/에 있다.
// 한 이동의 임무들은 하나의 이야기로 이어지고, 보상 아이템이 마지막 미션에 쓰인다.
package com.kidquest.core.contract

/** 미리 정한 좌표. 이 근처에 들어가면 missionId의 미션이 시작된다. */
data class Place(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val missionId: String,
)

/** 임무 하나. 브리핑, 사건, 질문, 답, 보상 순서로 진행한다. */
data class Mission(
    val id: String,
    val briefing: String,
    val event: String,
    val question: String,
    val answers: List<String>,
    val correctLine: String,
    val wrongLine: String,
    val rewardItem: String,
)

data class Episode(
    val id: String,
    val title: String,
    val places: List<Place>,
    val missions: List<Mission>,
    val finalLine: String,
)
