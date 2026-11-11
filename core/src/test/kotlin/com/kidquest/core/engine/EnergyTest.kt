// 잔량 문장 시험. 잔량이 절반 아래로 내려갈 때 한 번만, 대사를 끊지 않고, 도착 예고를 덮지 않고 말하는지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyTest : MissionTestBase() {
    private fun energy(percent: Int) = send(AppEvent.EnergyChanged(percent))

    /** 브리핑이 끝나 와이퍼를 기다리는 조용한 때까지 간다. */
    private fun waitingForWiper() {
        untilBriefing()
        finish("a/briefing")
    }

    @Test
    fun `잔량이 절반 아래로 내려가면 한 번만 말한다`() {
        waitingForWiper()
        assertTrue(energy(80).isEmpty())
        assertEquals(listOf(Lines.ENERGY_HALF), energy(40))
        assertTrue(energy(60).isEmpty())
        assertTrue("다시 내려가도 두 번 말하지 않는다", energy(30).isEmpty())
    }

    @Test
    fun `처음 받은 값이 이미 절반 아래면 말하지 않는다`() {
        waitingForWiper()
        assertTrue(energy(40).isEmpty())
        assertTrue(energy(30).isEmpty())
    }

    @Test
    fun `대사 중에 내려가면 끊지 않고 조용해질 때 말한다`() {
        untilBriefing()
        energy(80)
        assertTrue(energy(40).isEmpty())
        assertEquals(listOf(Lines.ENERGY_HALF.text), said(finish("a/briefing")))
    }

    @Test
    fun `잔량 문장은 미뤄 둔 도착 예고를 덮지 않는다`() {
        untilBriefing()
        send(AppEvent.RemainingTimeChanged(200))
        energy(80)
        energy(40)
        assertEquals(listOf("마지막"), said(finish("a/briefing")))
    }

    @Test
    fun `출발 전에 내려간 잔량은 말하지 않는다`() {
        energy(80)
        assertTrue(energy(40).isEmpty())
        waitingForWiper()
        assertTrue(energy(30).isEmpty())
    }

    @Test
    fun `영어를 고르면 잔량 문장도 영어다`() {
        val same = episode(mission("a"))
        engine = MissionEngine(same, mapOf(Language.EN to same))
        send(AppEvent.LanguageSelected(Language.EN))
        waitingForWiper()
        energy(80)
        assertEquals(listOf(EnglishLines.ENERGY_HALF), energy(40))
    }
}
