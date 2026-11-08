// 시작할 때 묻는 현재값과 구독으로 오는 새 값 사이의 순서를 지킨다.
// 두 콜백은 순서가 보장되지 않는다. 구독 값이 먼저 왔는데 현재값 조회 결과가 늦게 오면 옛 값이 새 값을 덮는다.
// 그래서 구독 값이 한 번이라도 오면 그 뒤의 현재값 결과는 버린다.
package com.kidquest.app.car

internal class CurrentValueGate<T>(private val send: (T) -> Unit) {
    private var updated = false

    /** 시작할 때 조회한 현재값. 구독 값이 이미 왔으면 버린다. */
    @Synchronized
    fun current(value: T) {
        if (!updated) send(value)
    }

    /** 구독으로 온 새 값. 언제나 보낸다. */
    @Synchronized
    fun update(value: T) {
        updated = true
        send(value)
    }
}
