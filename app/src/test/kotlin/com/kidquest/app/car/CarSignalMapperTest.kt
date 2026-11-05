// 차 SDK 값이 약속의 값으로 맞게 바뀌는지 본다. 에뮬레이터가 필요 없다.
package com.kidquest.app.car

import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.RearSeat
import com.kidquest.core.contract.RoadKind
import com.kidquest.core.contract.TurnSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarSignalMapperTest {
    @Test
    fun `기어를 바꾼다`() {
        assertEquals(Gear.P, CarSignalMapper.gear(VehicleGear.GEAR_PARK))
        assertEquals(Gear.R, CarSignalMapper.gear(VehicleGear.GEAR_REVERSE))
        assertEquals(Gear.N, CarSignalMapper.gear(VehicleGear.GEAR_NEUTRAL))
        assertEquals(Gear.D, CarSignalMapper.gear(VehicleGear.GEAR_DRIVE))
        assertEquals(Gear.D, CarSignalMapper.gear(VehicleGear.GEAR_3))
        assertNull(CarSignalMapper.gear(VehicleGear.GEAR_UNKNOWN))
        assertNull(CarSignalMapper.gear(null))
    }

    @Test
    fun `와이퍼는 켜짐과 꺼짐만 값이다`() {
        assertEquals(true, CarSignalMapper.wiperOn(WiperState.ON))
        assertEquals(false, CarSignalMapper.wiperOn(WiperState.OFF))
        assertNull(CarSignalMapper.wiperOn(WiperState.SERVICE))
    }

    @Test
    fun `방향지시등 방향을 바꾼다`() {
        assertEquals(TurnSide.LEFT, CarSignalMapper.turnSide(TurnSignalValue.LEFT))
        assertEquals(TurnSide.RIGHT, CarSignalMapper.turnSide(TurnSignalValue.RIGHT))
        assertNull(CarSignalMapper.turnSide(TurnSignalValue.OFF))
    }

    @Test
    fun `뒷좌석만 2열 자리로 바꾼다`() {
        assertEquals(SeatValue.ROW_2_LEFT, CarSignalMapper.sdkSeat(RearSeat.LEFT))
        assertEquals(SeatValue.ROW_2_CENTER, CarSignalMapper.sdkSeat(RearSeat.CENTER))
        assertEquals(SeatValue.ROW_2_RIGHT, CarSignalMapper.sdkSeat(RearSeat.RIGHT))
    }

    @Test
    fun `회전 안내에서 도로 사건을 찾는다`() {
        assertEquals(RoadKind.TUNNEL, CarSignalMapper.roadKind(TurnType.TURN_TUNNEL))
        assertEquals(RoadKind.TOLL, CarSignalMapper.roadKind(TurnType.TURN_TOLL))
        assertEquals(RoadKind.REST_AREA, CarSignalMapper.roadKind(TurnType.TURN_RESTAREA))
        assertEquals(RoadKind.REST_AREA, CarSignalMapper.roadKind(TurnType.TURN_ENTER_SERVICEAREA))
        assertEquals(RoadKind.INTERCHANGE, CarSignalMapper.roadKind(TurnType.TURN_EXIT_IC_R))
        assertNull(CarSignalMapper.roadKind(TurnType.TURN_LEFT))
    }

    @Test
    fun `터널은 회전이 아니다`() {
        assertTrue(CarSignalMapper.isTurn(TurnType.TURN_LEFT))
        assertTrue(CarSignalMapper.isTurn(TurnType.TURN_ENTER_ROUNDABOUT_03))
        assertFalse(CarSignalMapper.isTurn(TurnType.TURN_TUNNEL))
        assertFalse(CarSignalMapper.isTurn(TurnType.TURN_STRAIGHT))
    }
}
