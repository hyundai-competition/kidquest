// 약속 3. 에피소드 파일의 형식을 적는다. 예시는 app/src/main/assets/episodes/에 있다.
// 한 이동의 임무들은 하나의 이야기로 이어지고, 보상 아이템이 마지막 미션에 쓰인다.
package com.kidquest.core.contract

import kotlinx.serialization.Serializable

/** 미리 정한 좌표. 이 근처에 들어가면 missionId의 미션이 시작된다. */
@Serializable
data class Place(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val missionId: String,
)

/** 임무를 여는 사건. 신호를 못 읽거나 안 오면 autoAfterSeconds 뒤에 저절로 열린다. */
enum class MissionTrigger { TIME, WIPER, TURN_SIGNAL, PLACE, ROAD }

/** 임무 하나. 브리핑, 사건, 질문, 답, 보상 순서로 진행한다. */
@Serializable
data class Mission(
    val id: String,
    /** 비어 있으면 모든 연령에 나온다. */
    val ageGroups: List<AgeGroup>,
    /** 이 목적지일 때만 나오는 임무. null이면 어디로 가든 나온다. */
    val destinationId: String?,
    val trigger: MissionTrigger,
    val autoAfterSeconds: Int,
    val briefing: String,
    /** 사건 효과음. 말보다 먼저 나온다. 없으면 null. */
    val eventSoundId: String?,
    val event: String,
    val question: String,
    val answers: List<String>,
    val correctLine: String,
    val wrongLine: String,
    val rewardItem: String,
)

/** 임무 보상과 착륙 화면에 보이는 이름. */
@Serializable
data class Item(
    val id: String,
    val name: String,
)

/**
 * 임무와 상관없이 신호에 바로 답하는 짧은 사건.
 * signal 값 : turn_left, turn_right, wiper_on, energy_changed, temperature_changed, road_tunnel, road_toll, road_rest_area.
 * 운전이 바쁠 수 있는 때는 line을 null로 두고 효과음만 낸다.
 */
@Serializable
data class SignalReaction(
    val signal: String,
    val soundId: String?,
    val line: String?,
    val cooldownSeconds: Int,
)

/**
 * 목적지 이야기. 실제 목적지를 이번 항해에서 임무 보상을 배달할 행성으로 바꾼다.
 * 달리는 동안 지역을 길게 설명하지 않는다. 실제 지식은 전용 임무 1개의 단서(Mission.destinationId)와 착륙 때 한 문장에만 쓴다.
 */
@Serializable
data class Destination(
    val id: String,
    /** 부모가 고르는 실제 이름. */
    val name: String,
    val planetName: String,
    /** 승무원 확인 때 말한다. 어디로 무엇을 배달하는지 알려 준다. */
    val departureLine: String,
    /** 도착이 가까울 때 말한다. */
    val approachLine: String,
    /** 착륙할 때 말한다. 모은 보상을 전달하며 이번 이동의 이야기를 끝낸다. */
    val deliveryLine: String,
    /** 전달 뒤에 말하는 실제 사실 한 문장. 사람이 출처를 확인한다. */
    val landingFact: String,
    val factSource: String,
)

@Serializable
data class Episode(
    val id: String,
    val title: String,
    val destinations: List<Destination>,
    val places: List<Place>,
    val missions: List<Mission>,
    val items: List<Item>,
    val reactions: List<SignalReaction>,
    val finalLine: String,
    /** 임무를 다 끝내고 착륙할 때 말하는 결말 2~3문장. 모은 조각으로 목표를 이룬다. 비어 있으면 결말 없이 착륙 대사만 말한다. */
    val endingLine: String = "",
)
