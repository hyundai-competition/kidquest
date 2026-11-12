// 차 SDK 값을 약속(AppEvent)의 값으로 바꾼다. 상태가 없는 변환만 둬서 에뮬레이터 없이 시험할 수 있다.
package com.kidquest.app.car

import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.RearSeat
import com.kidquest.core.contract.RoadKind
import com.kidquest.core.contract.TurnSide

internal object CarSignalMapper {
    private val forwardGears = setOf(
        VehicleGear.GEAR_DRIVE, VehicleGear.GEAR_1, VehicleGear.GEAR_2, VehicleGear.GEAR_3, VehicleGear.GEAR_4,
        VehicleGear.GEAR_5, VehicleGear.GEAR_6, VehicleGear.GEAR_7, VehicleGear.GEAR_8, VehicleGear.GEAR_9,
    )

    /** 앞으로 가는 기어는 모두 D로 본다. 알 수 없는 값은 null이라 사건을 보내지 않는다. */
    fun gear(value: VehicleGear?): Gear? = when (value) {
        VehicleGear.GEAR_PARK -> Gear.P
        VehicleGear.GEAR_REVERSE -> Gear.R
        VehicleGear.GEAR_NEUTRAL -> Gear.N
        in forwardGears -> Gear.D
        else -> null
    }

    /** OTHER, SERVICE는 켜짐과 꺼짐을 알 수 없어서 null이다. */
    fun wiperOn(state: WiperState?): Boolean? = when (state) {
        WiperState.ON -> true
        WiperState.OFF -> false
        else -> null
    }

    fun turnSide(value: TurnSignalValue?): TurnSide? = when (value) {
        TurnSignalValue.LEFT -> TurnSide.LEFT
        TurnSignalValue.RIGHT -> TurnSide.RIGHT
        else -> null
    }

    /**
     * SDK 탑승 값을 「아이가 앉았나」로 바꾼다. SDK 값을 뒤집는다.
     * SDK(Vehicle 2.3.2)는 SEAT_OCCUPANCY 원시값이 1일 때 true를 준다. 그런데 AAOS의 1은 빈 자리(VACANT)이고 앉음은 2(OCCUPIED)다.
     * 그래서 SDK의 true는 빈 자리, false는 앉음이나 모름(0)이다. 11-09 시연에서 빈 뒷좌석 3자리가 모두 탄 것으로 읽힌 원인이다.
     * 모름(0)은 앉은 것으로 본다. 벨트를 매거나 부모가 확인해야 출발하므로 안전한 쪽이다. SDK가 고쳐지면 이 함수만 바꾼다.
     */
    fun occupied(sdkValue: Boolean?): Boolean? = sdkValue?.not()

    fun sdkSeat(seat: RearSeat): SeatValue = when (seat) {
        RearSeat.LEFT -> SeatValue.ROW_2_LEFT
        RearSeat.CENTER -> SeatValue.ROW_2_CENTER
        RearSeat.RIGHT -> SeatValue.ROW_2_RIGHT
    }

    fun roadKind(type: TurnType?): RoadKind? = when (type) {
        TurnType.TURN_TUNNEL -> RoadKind.TUNNEL
        TurnType.TURN_TOLL -> RoadKind.TOLL
        TurnType.TURN_RESTAREA, TurnType.TURN_ENTER_SERVICEAREA -> RoadKind.REST_AREA
        TurnType.TURN_ENTER_OVER_R, TurnType.TURN_ENTER_OVER_L, TurnType.TURN_ENTER_OVER_C -> RoadKind.OVERPASS
        TurnType.TURN_ENTER_UNDER_R, TurnType.TURN_ENTER_UNDER_L, TurnType.TURN_ENTER_UNDER_C -> RoadKind.UNDERPASS
        TurnType.TURN_ENTER_IC_R, TurnType.TURN_ENTER_IC_L, TurnType.TURN_ENTER_IC_C,
        TurnType.TURN_EXIT_IC_R, TurnType.TURN_EXIT_IC_L, TurnType.TURN_EXIT_IC_C,
        -> RoadKind.INTERCHANGE
        else -> null
    }

    /** 운전자가 방향을 바꿔야 하는 안내인가. 터널처럼 직진하는 안내는 빠진다. */
    fun isTurn(type: TurnType?): Boolean = type != null && (type in sharpTurns || type.name.contains("ROUNDABOUT"))

    private val sharpTurns = setOf(
        TurnType.TURN_LEFT, TurnType.TURN_RIGHT, TurnType.TURN_KEEP_L, TurnType.TURN_KEEP_R,
        TurnType.TURN_SHARP_L, TurnType.TURN_SHARP_R, TurnType.TURN_UTURN,
        TurnType.TURN_ENTER_IC_R, TurnType.TURN_ENTER_IC_L, TurnType.TURN_ENTER_IC_C,
        TurnType.TURN_EXIT_IC_R, TurnType.TURN_EXIT_IC_L, TurnType.TURN_EXIT_IC_C,
    )

    /** 회전 안내 목록에서 가장 가까운 회전과 가장 가까운 도로 사건을 하나씩 뽑는다. 목록은 가까운 순이라고 가정한다(미시험). */
    fun roadEvents(list: List<TBTInfo>): List<AppEvent> {
        val turn = list.firstOrNull { isTurn(it.type) }
        val road = list.firstOrNull { roadKind(it.type) != null }
        return listOfNotNull(
            turn?.let { AppEvent.NextTurnDistance(it.distance.toInt()) },
            road?.let { info -> roadKind(info.type)?.let { AppEvent.RoadAhead(it, info.distance.toInt()) } },
        )
    }
}
