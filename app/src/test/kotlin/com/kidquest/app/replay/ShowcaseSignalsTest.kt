// 시연 동안 실제 신호를 격리하되 주행 감지는 유지하고, 종료 시 실제 탑승·벨트를 복구하는지 확인한다.
package com.kidquest.app.replay

import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Test

class ShowcaseSignalsTest {
    @Test fun demoCachesLiveValuesWithoutForwardingThem() {
        val gate = ShowcaseSignals()
        assertTrue(gate.receive(AppEvent.GearChanged(Gear.P)))
        assertTrue(gate.select(true))
        assertFalse(gate.receive(AppEvent.WiperChanged(true)))
        assertFalse(gate.receive(AppEvent.PlaceEntered("cheomseongdae")))
        assertTrue(gate.live.wiperOn)
        assertFalse(gate.snapshot().wiperOn)
        assertTrue(gate.snapshot().buckled[RearSeat.LEFT] == true)
        assertTrue(gate.select(false))
        assertTrue(gate.restoreEvents().contains(AppEvent.BeltBuckled(RearSeat.LEFT, false)))
        assertTrue(gate.restoreEvents().contains(AppEvent.WiperChanged(true)))
        assertFalse(gate.restoreEvents().any { it is AppEvent.PlaceEntered })
    }
    @Test fun actualDrivingCannotBeOverriddenBySimulatedPark() {
        val gate = ShowcaseSignals()
        gate.receive(AppEvent.GearChanged(Gear.D))
        assertFalse(gate.select(true))
        gate.receive(AppEvent.GearChanged(Gear.P))
        assertTrue(gate.select(true))
        assertFalse(gate.receive(AppEvent.GearChanged(Gear.D)))
        assertFalse(gate.allowed)
        gate.select(false)
        assertEquals(Gear.D, gate.snapshot().gear)
        gate.receive(AppEvent.GearChanged(Gear.P))
        gate.receive(AppEvent.DrivingRestricted(true))
        assertFalse(gate.select(true))
    }
    @Test fun unknownGearDoesNotBecomeParkWhenLeavingDemo() {
        val gate = ShowcaseSignals()
        gate.select(true)
        gate.select(false)
        assertNull(gate.snapshot().gear)
        assertTrue(gate.restoreEvents().contains(AppEvent.GearChanged(Gear.N)))
    }
    @Test fun automotiveRequiresAnActualParkSignal() {
        val gate = ShowcaseSignals(requirePark = true)
        assertFalse(gate.select(true))
        gate.receive(AppEvent.GearChanged(Gear.P))
        assertTrue(gate.select(true))
    }
}
