// 뒷좌석 탑승과 벨트를 센다. 값이 안 바뀌는 자리는 부모 확인으로 대신한다.
// 엔진이 상태를 정하고, 여기서는 지금 몸의 값만 기억한다.
package com.kidquest.core.engine

import com.kidquest.core.contract.RearSeat

internal class Crew {
    private val occupied = mutableSetOf<RearSeat>()
    private val buckled = mutableSetOf<RearSeat>()

    /** 부모 확인으로 대신한 자리. 벨트 값이 읽히는 자리는 들어가지 않아서, 그 자리를 풀면 멈춘다. */
    private val parentCovered = mutableSetOf<RearSeat>()

    /** 탑승 값을 못 읽어서 부모가 승무원을 확인했다. */
    private var manual = false

    fun present(): Boolean = occupied.isNotEmpty() || manual

    /** 탑승한 뒷좌석의 벨트가 모두 매졌나. */
    fun ready(): Boolean = present() && occupied.all { it in buckled || it in parentCovered }

    /** 탑승 값을 바꾼다. 같은 값이 다시 오면 false. */
    fun seat(seat: RearSeat, on: Boolean): Boolean {
        val changed = if (on) occupied.add(seat) else occupied.remove(seat)
        if (changed && !on) parentCovered -= seat // 자리를 비웠다 돌아오면 다시 확인한다.
        return changed
    }

    /** 벨트 값을 바꾼다. 같은 값이 다시 오면 false. */
    fun belt(seat: RearSeat, on: Boolean): Boolean {
        val changed = if (on) buckled.add(seat) else buckled.remove(seat)
        if (changed && on) parentCovered -= seat // 값이 읽히는 자리이니 이제 값으로 본다.
        return changed
    }

    fun parentConfirmed() {
        if (occupied.isEmpty()) manual = true
        parentCovered += occupied - buckled
    }

    /** 몸의 값(탑승, 벨트)은 두고, 이번 이동에서 부모가 대신 확인한 것만 지운다. */
    fun clearTrip() {
        parentCovered.clear()
        manual = false
    }
}
