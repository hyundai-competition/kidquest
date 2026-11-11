// 수집된 최신 차량 값을 표시용으로 보관한다. 이륙과 멈춤 판단은 엔진만 한다.
package com.kidquest.app.screen.model

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.RearSeat
import com.kidquest.core.contract.TurnSide

data class VehicleUiState(
    val occupied: Map<RearSeat, Boolean> = emptyMap(),
    val buckled: Map<RearSeat, Boolean> = emptyMap(),
    val gear: Gear? = null,
    val unavailable: Set<CarSignal> = emptySet(),
    val restricted: Boolean = false,
    val parentConfirmed: Boolean = false,
    val parentStopped: Boolean = false,
    val wiperOn: Boolean = false,
    val turnSide: TurnSide? = null,
    val turnUntilMillis: Long = 0L,
    /** 내비 다음 회전. 아직 안내가 없거나, 도착했거나, 한동안 새 안내가 없으면 null이다. */
    val navi: NaviGuide? = null,
    val naviUntilMillis: Long = 0L,
) {
    fun accept(event: AppEvent, nowMillis: Long = 0L): VehicleUiState = when (event) {
        is AppEvent.WiperChanged -> copy(wiperOn = event.on)
        is AppEvent.TurnSignalOn -> copy(turnSide = event.side, turnUntilMillis = nowMillis + 3500L)
        is AppEvent.Tick -> expire(event.nowMillis)
        is AppEvent.SeatOccupied -> copy(occupied = occupied + (event.seat to event.occupied))
        is AppEvent.BeltBuckled -> copy(buckled = buckled + (event.seat to event.buckled))
        is AppEvent.GearChanged -> copy(gear = event.gear)
        is AppEvent.DrivingRestricted -> copy(restricted = event.restricted)
        is AppEvent.NextTurnDistance -> copy(navi = NaviGuide(event.meters, event.turn), naviUntilMillis = nowMillis + NAVI_KEEP_MILLIS)
        AppEvent.DestinationArrived -> copy(navi = null)
        is AppEvent.SignalUnavailable -> copy(unavailable = unavailable + event.signal)
        is AppEvent.ParentStopChanged -> copy(parentStopped = event.stopped)
        AppEvent.ParentConfirmedBelt -> copy(parentConfirmed = true)
        AppEvent.RestartPressed -> copy(parentStopped = false, parentConfirmed = false)
        else -> this
    }

    /** 깜빡이 표시와 내비 안내 한 줄은 시간이 지나면 지운다. 바뀐 것이 없으면 같은 값을 돌려준다. */
    private fun expire(nowMillis: Long): VehicleUiState {
        val side = if (nowMillis >= turnUntilMillis) null else turnSide
        val guide = if (nowMillis >= naviUntilMillis) null else navi
        return if (side == turnSide && guide == navi) this else copy(turnSide = side, navi = guide)
    }

    private companion object {
        /**
         * 내비는 10초마다 묻는다. 회전이 없는 목록(마지막 회전을 지난 고속도로, 경로 취소)은 사건을 보내지 않아서
         * 지난 안내가 남는다. 30초(조회 3번) 동안 새 안내가 없으면 지운다.
         */
        const val NAVI_KEEP_MILLIS = 30_000L
    }
}
