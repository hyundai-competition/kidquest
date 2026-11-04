// 약속 1. 차 신호와 버튼이 앱 안에서 어떤 사건이 되는지 적는다.
// 차 신호 수집(car)과 재생 모드(replay)가 만들고, 임무 엔진(engine)이 읽는다.
package com.kidquest.core.contract

/** 뒷좌석 자리. 앞좌석은 다루지 않는다. */
enum class RearSeat { LEFT, CENTER, RIGHT }

enum class Gear { P, R, N, D }

enum class TurnSide { LEFT, RIGHT }

sealed interface AppEvent {
    // 차 신호
    data class SeatOccupied(val seat: RearSeat, val occupied: Boolean) : AppEvent
    data class BeltBuckled(val seat: RearSeat, val buckled: Boolean) : AppEvent
    data class GearChanged(val gear: Gear) : AppEvent
    data class TurnSignalOn(val side: TurnSide) : AppEvent
    data object WiperOn : AppEvent
    data class EnergyChanged(val percent: Int) : AppEvent
    data class CabinTemperatureChanged(val celsius: Float) : AppEvent
    data class DrivingRestricted(val restricted: Boolean) : AppEvent

    // 내비와 위치
    data class PlaceEntered(val placeId: String) : AppEvent
    data class RemainingTimeChanged(val seconds: Int) : AppEvent
    data class NextTurnDistance(val meters: Int) : AppEvent
    data object DestinationArrived : AppEvent

    // 소리와 답
    data class AudioFocusChanged(val hasFocus: Boolean) : AppEvent

    /** 아이 답. 못 알아듣거나 시간이 지나면 word가 null이다. */
    data class AnswerHeard(val word: String?) : AppEvent

    // 버튼
    data object StartPressed : AppEvent
    data object ParentConfirmedBelt : AppEvent
    data object RestartPressed : AppEvent
}
