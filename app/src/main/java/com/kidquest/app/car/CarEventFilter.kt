// 차 사건을 앱으로 보내기 전에 같은 값의 반복을 거른다.
// 차 값은 현재값 조회와 구독으로 두 번 오거나 값이 그대로여도 올 수 있어서, 값이 바뀔 때만 보낸다.
package com.kidquest.app.car

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal
import com.kidquest.core.contract.EventListener
import com.kidquest.core.contract.TurnSide

internal class CarEventFilter(private val out: EventListener) {
    private val last = HashMap<Any, AppEvent>()

    /** SDK 콜백이 여러 스레드에서 올 수 있어서 한 번에 하나씩 처리한다. */
    @Synchronized
    fun send(event: AppEvent) {
        val key = keyOf(event)
        if (key != null) {
            if (last[key] == event) return
            last[key] = event
        }
        out.onEvent(event)
    }

    fun unavailable(signal: CarSignal) = send(AppEvent.SignalUnavailable(signal))

    /** 방향지시등의 직전 값. 꺼짐은 null이다. */
    private var lastTurn: TurnSide? = null

    /** 방향지시등은 켜질 때만 사건이다. 껐다가 같은 쪽을 다시 켜면 다시 보낸다. 같은 켜짐이 두 번 와도 한 번만 보낸다. */
    @Synchronized
    fun turnSignal(side: TurnSide?) {
        if (side != null && side != lastTurn) out.onEvent(AppEvent.TurnSignalOn(side))
        lastTurn = side
    }

    /**
     * 같은 칸끼리 비교한다. null이면 거르지 않는다. 위치 사건과 도착처럼 값이 없는 사건이 여기에 든다.
     * 다음 회전(NextTurnDistance)도 거르지 않는다. 화면 안내 한 줄은 30초 동안 새 값이 없으면 지우는데,
     * 정체로 거리가 그대로일 때 거르면 아직 맞는 안내가 지워진다. 엔진은 같은 거리가 다시 와도 한 번만 쓴다.
     */
    private fun keyOf(event: AppEvent): Any? = when (event) {
        is AppEvent.SeatOccupied -> "seat" to event.seat
        is AppEvent.BeltBuckled -> "belt" to event.seat
        is AppEvent.SignalUnavailable -> "unavailable" to event.signal
        is AppEvent.GearChanged, is AppEvent.WiperChanged, is AppEvent.EnergyChanged,
        is AppEvent.CabinTemperatureChanged, is AppEvent.DrivingRestricted, is AppEvent.RemainingTimeChanged,
        is AppEvent.RoadAhead,
        -> event::class
        else -> null
    }
}
