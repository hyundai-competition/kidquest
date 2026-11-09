// 위치 권한 결정 뒤 차량 수집을 시작한다. 거절해도 나머지 차량 신호와 시연은 사용할 수 있다.
package com.kidquest.app.car

import android.content.Context
import com.kidquest.core.contract.*

class PermissionCarSource(private val context: Context, private val places: List<Place>) : EventSource {
    var locationAllowed = false
    private var collector: CarSignalCollector? = null
    override fun start(listener: EventListener) {
        collector = CarSignalCollector(context, if (locationAllowed) places else emptyList()).also { it.start(listener) }
        if (!locationAllowed) listener.onEvent(AppEvent.SignalUnavailable(CarSignal.LOCATION))
    }
    override fun stop() { collector?.stop(); collector = null }
}
