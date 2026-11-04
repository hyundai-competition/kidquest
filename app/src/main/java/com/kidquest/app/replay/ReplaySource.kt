// 재생 모드의 입구. 콘솔에 없는 신호를 앱 안에서 넣는다. 실제 차 신호와 같은 입구를 쓴다.
// 시연에서 이 모드를 쓰면 그 사실을 밝힌다.
package com.kidquest.app.replay

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.EventListener
import com.kidquest.core.contract.EventSource

class ReplaySource : EventSource {
    private var listener: EventListener? = null

    override fun start(listener: EventListener) {
        this.listener = listener
    }

    override fun stop() {
        listener = null
    }

    fun send(event: AppEvent) {
        listener?.onEvent(event)
    }
}
