// 도착 직전 문장 같은 알림을 맡아 둔다. 대사를 끊지 않게, 신호를 기다리거나 임무 사이처럼 조용할 때 꺼낸다.
// 신호를 기다리다 꺼냈으면 그 기다림을 시작한 시계 값을 남겨 둬서, 알림이 끝난 뒤 기다린 시간을 이어서 센다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Command

internal class NoticeSlot {
    /** 아직 못 한 알림. 새 알림이 오면 앞의 것을 덮는다. */
    var pending: Command.Say? = null
        private set

    /** 마지막으로 꺼낸 알림을 신호를 기다리다 꺼냈나. false면 임무 사이나 다 끝난 뒤에 꺼냈다. */
    var fromWait = false
        private set

    /** 신호를 기다리다 꺼냈을 때 그 기다림을 시작한 시계 값. */
    var waitStartedAt: Long? = null
        private set

    fun hold(say: Command.Say) {
        pending = say
    }

    /** 맡은 알림이 없을 때만 맡는다. 잔량 문장 같은 덜 중요한 알림이 도착 예고를 덮지 않게 한다. */
    fun offer(say: Command.Say) {
        if (pending == null) pending = say
    }

    /** 알림을 꺼낸다. 맡은 게 없으면 null. */
    fun take(fromWait: Boolean, waitStartedAt: Long?): Command.Say? {
        val say = pending ?: return null
        pending = null
        this.fromWait = fromWait
        this.waitStartedAt = if (fromWait) waitStartedAt else null
        return say
    }

    fun reset() {
        pending = null
        fromWait = false
        waitStartedAt = null
    }
}
