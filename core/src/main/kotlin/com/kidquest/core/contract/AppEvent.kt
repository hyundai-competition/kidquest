// 약속 1. 차 신호, 소리, 버튼이 앱 안에서 어떤 사건이 되는지 적는다.
// 보내는 쪽은 차 신호 수집(car), 재생 모드(replay), 음성(voice), 화면(screen), 앱 연결(AppController)이고 받는 쪽은 임무 엔진(engine)이다.
// 기능별로 어떤 사건을 쓰는지는 docs/contract_map.md에 있다.
package com.kidquest.core.contract

/** 뒷좌석 자리. 앞좌석은 다루지 않는다. */
enum class RearSeat { LEFT, CENTER, RIGHT }

enum class Gear { P, R, N, D }

enum class TurnSide { LEFT, RIGHT }

/** 아이 연령. 아이가 둘 이상이면 어린 쪽에 맞춘다(가정). */
enum class AgeGroup { PRESCHOOL, EARLY_GRADE }

/** 내비 회전 안내에서 얻는 도로 사건. 강, 바다, 숲은 여기서 못 얻어서 Place 좌표로 둔다. */
enum class RoadKind { TUNNEL, TOLL, REST_AREA, OVERPASS, UNDERPASS, INTERCHANGE }

/** 차나 에뮬레이터에서 못 읽을 수 있는 신호. 탑승과 벨트는 못 읽어도 저절로 넘어가지 않는다. */
enum class CarSignal {
    SEAT, BELT, GEAR, TURN_SIGNAL, WIPER, ENERGY, CABIN_TEMPERATURE, LOCATION, NAVIGATION, DRIVING_STATE,
}

sealed interface AppEvent {
    // 차 신호. car와 replay가 보낸다.
    data class SeatOccupied(val seat: RearSeat, val occupied: Boolean) : AppEvent
    data class BeltBuckled(val seat: RearSeat, val buckled: Boolean) : AppEvent
    data class GearChanged(val gear: Gear) : AppEvent
    data class TurnSignalOn(val side: TurnSide) : AppEvent

    /** 비 오는 날은 와이퍼가 계속 켜져 있으니, 켜짐과 꺼짐을 둘 다 보낸다. */
    data class WiperChanged(val on: Boolean) : AppEvent
    data class EnergyChanged(val percent: Int) : AppEvent
    data class CabinTemperatureChanged(val celsius: Float) : AppEvent
    data class DrivingRestricted(val restricted: Boolean) : AppEvent

    /** 이 차에서 읽지 못하는 신호를 알린다. */
    data class SignalUnavailable(val signal: CarSignal) : AppEvent

    // 내비와 위치. car와 replay가 보낸다.
    data class PlaceEntered(val placeId: String) : AppEvent
    data class RemainingTimeChanged(val seconds: Int) : AppEvent
    /** 내비 회전 안내(onTBTInfo)의 첫 회전까지 거리(미터). 경로 안내 중에 값이 바뀔 때마다 보낸다. */
    data class NextTurnDistance(val meters: Int) : AppEvent
    data class RoadAhead(val kind: RoadKind, val meters: Int) : AppEvent
    data object DestinationArrived : AppEvent

    // 소리와 답. voice가 보낸다.
    data class AudioFocusChanged(val hasFocus: Boolean) : AppEvent

    /** Command.Say의 대사를 끝까지 말했다. 중간에 멈춘 대사는 보내지 않는다. */
    data class LineFinished(val lineId: String) : AppEvent

    /** 아이 답. 못 알아듣거나 시간이 지나면 word가 null이다. 내용은 저장하지 않는다. */
    data class AnswerHeard(val word: String?) : AppEvent

    // 버튼. screen이 보낸다.
    data class AgeSelected(val group: AgeGroup) : AppEvent

    /** 부모가 고른 목적지와 경로. 목적지가 null이면 기본 이야기로, 경로가 null이거나 없는 경로면 그 목적지의 첫 경로로 간다. */
    data class DestinationSelected(val destinationId: String?, val routeId: String? = null) : AppEvent

    /** 부모가 아이 말 듣기에 동의했나. 동의하지 않으면 듣지 않는다. */
    data class ListeningConsentChanged(val allowed: Boolean) : AppEvent
    data object StartPressed : AppEvent

    /** 카시트처럼 값이 안 바뀔 때 부모가 대신 확인한다. 탑승 값을 못 읽을 때는 승무원 확인도 겸한다. */
    data object ParentConfirmedBelt : AppEvent

    /** 부모나 앞좌석 동승자가 큰 버튼으로 소리를 끄거나 다시 켠다. */
    data class ParentStopChanged(val stopped: Boolean) : AppEvent

    /** 정박 중에 부모가 여기서 끝내기로 했다. */
    data object EndTripPressed : AppEvent
    data object RestartPressed : AppEvent

    // 시계. AppController가 1초마다 보낸다. 엔진이 직접 시계를 읽지 않아서 시험에서 시간을 넣을 수 있다.
    data class Tick(val nowMillis: Long) : AppEvent
}
