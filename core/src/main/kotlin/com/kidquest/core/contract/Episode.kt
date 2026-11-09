// 약속 3. 에피소드 파일의 형식을 적는다. 예시는 app/src/main/assets/episodes/에 있다.
// 한 이동의 임무들은 하나의 이야기로 이어지고, 보상 아이템이 마지막 미션에 쓰인다.
package com.kidquest.core.contract

import kotlinx.serialization.Serializable

/**
 * 미리 정한 좌표. 이 근처에 들어가면 missionId의 미션이 열린다. 그 미션 차례가 오기 전에 지나가면 기억했다가 차례가 오면 연다. missionId는 PLACE 미션이어야 한다.
 * 고속도로 위 도시는 나들목이나 휴게소처럼 차가 실제로 지나는 곳에 두고 반경을 넓게 잡는다. 원이 겹치면 들어간 원마다 PlaceEntered를 보낸다.
 */
@Serializable
data class Place(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val missionId: String,
)

/**
 * 임무를 여는 사건. 신호를 못 읽거나 안 오면 autoAfterSeconds 뒤에 저절로 열린다.
 * 장소(PLACE) 임무는 위치를 못 읽는 차에서만 시간으로 연다.
 * NAVI_TURN은 내비가 알려 준 갈림길이나 회전을 지났을 때다. 회전 안내가 안 오거나 내비를 못 읽으면 autoAfterSeconds에, 안내가 오는 중이면 10분 동안 못 지났을 때 연다.
 * 깜빡이(TURN_SIGNAL)로 여는 임무는 만들지 않는다(docs/rules.md 아이 안전).
 */
enum class MissionTrigger { TIME, WIPER, TURN_SIGNAL, PLACE, ROAD, NAVI_TURN }

/** 임무 하나. 브리핑, 사건, 질문, 답, 보상 순서로 진행한다. */
@Serializable
data class Mission(
    val id: String,
    /** 비어 있으면 모든 연령에 나온다. */
    val ageGroups: List<AgeGroup>,
    /** 이 목적지일 때만 나오는 임무. null이면 어디로 가든 나온다. 경로(Route)에 든 임무는 그 경로를 골랐을 때만 나온다. */
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
    /** 사건 대사에 넣은 지역 사실의 출처. 사람이 원문을 확인하고 적는다. 사실이 없는 임무는 비워 둔다. */
    val factSource: String = "",
    /** 신호 없이 시간이 지나 열릴 때 말하는 사건 대사(예 : 와이퍼를 안 켠 맑은 날). 효과음 없이 바로 말한다. 비어 있으면 event를 쓴다. */
    val fallbackEvent: String = "",
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
 * 목적지 이야기. 부모가 고른 실제 목적지까지 가며 그 지역의 보물을 찾는다.
 * 지역 이야기는 장소에 들어갔을 때 보물 단서 한두 문장으로만 하고, 출처를 확인한 사실만 쓴다(Mission.destinationId 임무와 착륙 때 한 문장).
 */
@Serializable
data class Destination(
    val id: String,
    /** 부모가 고르는 실제 이름. */
    val name: String,
    val planetName: String,
    /** 출발한 뒤 첫 임무 전에 한 번 말한다. 어디로 무엇을 찾으러 가는지 알려 준다. */
    val departureLine: String,
    /** 도착이 가까울 때(남은 시간 5분 이하) 한 번 말한다. */
    val approachLine: String,
    /** 도착해서 착륙할 때 결말 다음에 말한다. 찾은 보물을 전하며 이번 이동의 이야기를 끝낸다. */
    val deliveryLine: String,
    /** 전달 문장 다음에 말하는 실제 사실 한 문장. 사람이 출처를 확인하고 factSource에 적는다. */
    val landingFact: String,
    val factSource: String,
    /** 이 목적지로 가는 길. 부모가 고르고, 안 고르면 첫 경로로 간다. 비어 있으면 destinationId가 이 목적지인 임무를 파일 순서대로 쓴다. */
    val routes: List<Route> = emptyList(),
)

/**
 * 목적지로 가는 길 하나. 지나는 도시의 장소 임무를 가는 순서대로 적고, 목적지 장소 임무를 맨 끝에 둔다.
 * 공통 임무(destinationId가 null이고 어느 경로에도 없는 임무)를 먼저 하고 이 순서대로 간다.
 * 같은 도시를 지나는 경로끼리는 그 도시 임무 하나를 같이 쓴다.
 */
@Serializable
data class Route(
    val id: String,
    /** 부모가 고르는 이름. 예 : 경부고속도로(대전·대구). */
    val name: String,
    val missionIds: List<String>,
    /** 이 길로 갈 때의 출발 문장. 비어 있으면 목적지의 departureLine을 쓴다. */
    val departureLine: String = "",
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
    /** 앱 정보 화면이나 착륙 화면에 보여 줄 출처 줄. 지역 사실과 좌표를 가져온 곳을 적는다. 엔진은 쓰지 않는다. */
    val credits: List<String> = emptyList(),
)
