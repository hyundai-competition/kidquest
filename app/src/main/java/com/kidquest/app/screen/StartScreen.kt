// 정차 중 연령, 목적지, 말 듣기 동의를 고른다. 선택 처리는 AppViewModel이 맡는다.
package com.kidquest.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.core.contract.AgeGroup

@Composable
fun StartScreen(model: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("출발 준비", fontSize = 40.sp)
        Text("아이의 연령", fontSize = 30.sp)
        AgeGroup.entries.forEach { age ->
            OutlinedButton(onClick = { model.selectAge(age) }, modifier = Modifier.fillMaxWidth()) {
                Text("${if (model.age == age) "●" else "○"} ${if (age == AgeGroup.PRESCHOOL) "미취학" else "초등 저학년"}", fontSize = 26.sp)
            }
        }
        Text("어디로 떠날까요?", fontSize = 30.sp)
        DestinationButton("기본 이야기", model.destinationId == null) { model.selectDestination(null) }
        model.destinations.forEach { destination ->
            DestinationButton(destination.name, model.destinationId == destination.id) {
                model.selectDestination(destination.id)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Switch(checked = model.consent, onCheckedChange = model::setListeningConsent)
            Column {
                Text("말로 대답하기", fontSize = 28.sp)
                Text("꺼도 이야기를 들을 수 있어요", fontSize = 24.sp)
            }
        }
        Button(onClick = model::startTrip, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)) {
            Text("승무원 확인하기 →", fontSize = 30.sp)
        }
        Text("정차 중 설정해 주세요.\n차량의 안전벨트 경고를 대신하지 않아요.", fontSize = 24.sp)
    }
}

@Composable
private fun DestinationButton(label: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("${if (selected) "●" else "○"} $label", fontSize = 26.sp)
    }
}
