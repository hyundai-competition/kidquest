// 최신 탑승·벨트·기어 값을 표시하고 부모 확인 사건을 보낸다. 차량 SDK는 호출하지 않는다.
package com.kidquest.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppController
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal
import com.kidquest.core.contract.RearSeat

@Composable
fun CrewCheckScreen(controller: AppController) {
    val vehicle = controller.vehicle
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("승무원 확인", fontSize = 40.sp)
        Text("현재 기어: ${vehicle.gear?.name ?: "확인 중"}", fontSize = 30.sp)
        RearSeat.entries.forEach { seat ->
            val label = when (seat) {
                RearSeat.LEFT -> "뒷좌석 왼쪽"
                RearSeat.CENTER -> "뒷좌석 가운데"
                RearSeat.RIGHT -> "뒷좌석 오른쪽"
            }
            val occupied = vehicle.occupied[seat]
            val belt = vehicle.buckled[seat]
            Text(label, fontSize = 28.sp)
            Text("탑승 ${stateLabel(occupied, "확인", "없음")} · 벨트 ${stateLabel(belt, "착용", "미착용")}", fontSize = 24.sp)
        }
        if (CarSignal.SEAT in vehicle.unavailable || CarSignal.BELT in vehicle.unavailable) {
            Text("일부 탑승·벨트 신호를 읽지 못했어요. 직접 확인해 주세요.", fontSize = 24.sp)
        }
        Button(onClick = { controller.dispatch(AppEvent.ParentConfirmedBelt) }, modifier = Modifier.heightIn(min = 80.dp)) {
            Text("아이 탑승과 벨트를 직접 확인했어요", fontSize = 26.sp)
        }
        if (vehicle.parentConfirmed) Text("부모가 직접 확인했어요. 차량 센서 값과는 별개입니다.", fontSize = 24.sp)
        Text("벨트를 확인한 뒤 기어가 D이면 이야기가 시작돼요.", fontSize = 24.sp)
    }
}

private fun stateLabel(value: Boolean?, yes: String, no: String) = when (value) {
    true -> yes
    false -> no
    null -> "미확인"
}
