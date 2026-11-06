// 정차 중 연령, 목적지, 말 듣기 동의를 고른다. 선택 처리는 AppViewModel이 맡는다.
package com.kidquest.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.screen.common.SelectionCard
import com.kidquest.app.AppViewModel
import com.kidquest.core.contract.AgeGroup

@Composable
fun StartScreen(model: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("출발 준비", fontSize = 40.sp, lineHeight = 52.sp)
        Text("아이의 연령", fontSize = 30.sp, lineHeight = 42.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AgeGroup.entries.forEach { age ->
                SelectionCard(
                    label = if (age == AgeGroup.PRESCHOOL) "미취학" else "초등 저학년",
                    isSelected = model.age == age,
                    onClick = { model.selectAge(age) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text("어디로 떠날까요?", fontSize = 30.sp, lineHeight = 42.sp)
        val destinations = listOf(null to "기본 이야기") + model.destinations.map { it.id to it.name }
        destinations.chunked(2).forEach { choices ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                choices.forEach { (id, name) ->
                    SelectionCard(
                        label = name,
                        isSelected = model.destinationId == id,
                        onClick = { model.selectDestination(id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (choices.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Switch(checked = model.consent, onCheckedChange = model::setListeningConsent, modifier = Modifier.testTag("listening-consent"))
            Column(Modifier.weight(1f)) {
                Text("말로 대답하기", modifier = Modifier.fillMaxWidth(), fontSize = 28.sp, lineHeight = 40.sp)
                Text("꺼도 이야기를 들을 수 있어요", fontSize = 24.sp, lineHeight = 34.sp)
            }
        }
        Button(onClick = model::startTrip, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)) {
            Text("승무원 확인하기 →", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 30.sp, lineHeight = 42.sp)
        }
        Text("정차 중 설정해 주세요.\n차량의 안전벨트 경고를 대신하지 않아요.", fontSize = 24.sp, lineHeight = 34.sp)
    }
}
