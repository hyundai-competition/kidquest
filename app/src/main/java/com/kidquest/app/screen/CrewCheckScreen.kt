// 최신 탑승·벨트·기어 값을 표시하고 부모 확인 사건을 보낸다. 차량 SDK는 호출하지 않는다.
package com.kidquest.app.screen

import com.kidquest.app.screen.AppText as Text
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
        Text("탐험대 확인", fontSize = 46.sp)
        Text("현재 기어: ${vehicle.gear?.name ?: "확인 중"}", fontSize = 36.sp)
        RearSeat.entries.forEach { seat ->
            val label = when (seat) {
                RearSeat.LEFT -> "뒷좌석 왼쪽"
                RearSeat.CENTER -> "뒷좌석 가운데"
                RearSeat.RIGHT -> "뒷좌석 오른쪽"
            }
            val occupied = vehicle.occupied[seat]
            val belt = vehicle.buckled[seat]
            Text(label, fontSize = 34.sp)
            Text("탑승 ${stateLabel(occupied, "확인", "없음")} · 벨트 ${stateLabel(belt, "착용", "미착용")}", fontSize = 30.sp)
        }
        if (CarSignal.SEAT in vehicle.unavailable || CarSignal.BELT in vehicle.unavailable) {
            Text("일부 탑승·벨트 신호를 읽지 못했어요. 직접 확인해 주세요.", fontSize = 30.sp)
        }
        Text("탑승과 벨트는 자동으로 확인해요. 카시트 등으로 확인이 어려울 때만 아래 버튼을 사용해 주세요.", separateSentences = true, fontSize = 30.sp)
        ParentBeltConfirmation(controller)
        Text("모두 벨트를 매면 출발 준비 완료! 기어가 D이면 보물 탐험을 시작해요.", fontSize = 30.sp)
    }
}

private fun stateLabel(value: Boolean?, yes: String, no: String) = when (value) {
    true -> yes
    false -> no
    null -> "미확인"
}

@Composable
fun ParentBeltConfirmation(controller: AppController) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { controller.dispatch(AppEvent.ParentConfirmedBelt) }, modifier = Modifier.heightIn(min = 80.dp)) {
            Text("카시트·벨트 직접 확인 (부모용)", fontSize = 32.sp)
        }
        if (controller.vehicle.parentConfirmed) Text("부모가 직접 확인했어요. 차량 센서 값과는 별개입니다.", fontSize = 30.sp)
    }
}
