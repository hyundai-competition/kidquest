// 늦게 온 현재값 조회 결과가 구독으로 온 새 값을 덮지 않는지 본다.
package com.kidquest.app.car

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentValueGateTest {
    private val sent = mutableListOf<Boolean>()
    private val gate = CurrentValueGate<Boolean> { sent += it }

    @Test
    fun `현재값이 먼저 오면 보내고 구독 값도 보낸다`() {
        gate.current(true)
        gate.update(false)
        assertEquals(listOf(true, false), sent)
    }

    @Test
    fun `구독 값 뒤에 늦게 온 현재값은 버린다`() {
        gate.update(false)
        gate.current(true)
        assertEquals(listOf(false), sent)
    }

    @Test
    fun `구독 값은 몇 번이 와도 보낸다`() {
        gate.update(true)
        gate.update(false)
        gate.update(true)
        assertEquals(listOf(true, false, true), sent)
    }
}
