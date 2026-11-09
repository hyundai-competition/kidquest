// 같은 값의 반복을 거르는지 본다. 달라지면 보내고, 자리가 다르면 따로 보는지, 방향지시등은 켜질 때만 보내는지 확인한다.
package com.kidquest.app.car

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.RearSeat
import com.kidquest.core.contract.TurnSide
import org.junit.Assert.assertEquals
import org.junit.Test

class CarEventFilterTest {
    private val sent = mutableListOf<AppEvent>()
    private val filter = CarEventFilter { sent += it }

    @Test
    fun `같은 기어가 반복되면 한 번만 보낸다`() {
        filter.send(AppEvent.GearChanged(Gear.D))
        filter.send(AppEvent.GearChanged(Gear.D))
        filter.send(AppEvent.GearChanged(Gear.P))
        assertEquals(listOf(AppEvent.GearChanged(Gear.D), AppEvent.GearChanged(Gear.P)), sent)
    }

    @Test
    fun `자리가 다르면 따로 본다`() {
        filter.send(AppEvent.BeltBuckled(RearSeat.LEFT, true))
        filter.send(AppEvent.BeltBuckled(RearSeat.RIGHT, true))
        filter.send(AppEvent.BeltBuckled(RearSeat.LEFT, true))
        assertEquals(2, sent.size)
    }

    @Test
    fun `풀렸다 다시 매면 다시 보낸다`() {
        filter.send(AppEvent.BeltBuckled(RearSeat.LEFT, true))
        filter.send(AppEvent.BeltBuckled(RearSeat.LEFT, false))
        filter.send(AppEvent.BeltBuckled(RearSeat.LEFT, true))
        assertEquals(3, sent.size)
    }

    @Test
    fun `못 읽는 신호는 신호마다 한 번만 알린다`() {
        filter.unavailable(CarSignal.BELT)
        filter.unavailable(CarSignal.BELT)
        filter.unavailable(CarSignal.WIPER)
        assertEquals(2, sent.size)
    }

    @Test
    fun `방향지시등은 켜질 때만 보낸다`() {
        filter.turnSignal(TurnSide.LEFT)
        filter.turnSignal(TurnSide.LEFT)
        filter.turnSignal(null)
        filter.turnSignal(TurnSide.LEFT)
        filter.turnSignal(TurnSide.RIGHT)
        val expected = listOf(TurnSide.LEFT, TurnSide.LEFT, TurnSide.RIGHT).map { AppEvent.TurnSignalOn(it) }
        assertEquals(expected, sent)
    }

    @Test
    fun `방향지시등과 도착은 거르지 않는다`() {
        filter.send(AppEvent.TurnSignalOn(TurnSide.LEFT))
        filter.send(AppEvent.TurnSignalOn(TurnSide.LEFT))
        filter.send(AppEvent.DestinationArrived)
        filter.send(AppEvent.DestinationArrived)
        assertEquals(4, sent.size)
    }
}
