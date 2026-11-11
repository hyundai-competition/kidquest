// 결과 화면이 남아 있어도 P와 규제 해제 상태에서만 장식 효과를 허용하는지 확인한다.
package com.kidquest.app.screen

import com.kidquest.app.screen.model.VehicleUiState
import com.kidquest.core.contract.Gear
import org.junit.Assert.*
import org.junit.Test

class DecorationSafetyTest {
    @Test fun onlyKnownParkWithoutRestrictionAllowsMotion() {
        assertFalse(VehicleUiState().decorationsMayAnimate)
        Gear.entries.forEach { gear ->
            assertEquals(gear == Gear.P, VehicleUiState(gear = gear).decorationsMayAnimate)
            assertFalse(VehicleUiState(gear = gear, restricted = true).decorationsMayAnimate)
        }
    }
}
