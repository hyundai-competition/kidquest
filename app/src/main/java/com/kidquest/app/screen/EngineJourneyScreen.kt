// 이동 경로와 대사·보상을 엔진 상태로 표시한다. 장소 버튼은 실제 위치와 같은 PlaceEntered를 보낸다.
package com.kidquest.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.screen.common.*
import com.kidquest.core.contract.AppEvent

@Composable
fun EngineJourneyBoard(model: AppViewModel) {
    val controller = model.controller
    val rewards = controller.items.map { it.itemId }.toSet()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        JourneyBoard(
            model.selectedRoute,
            completed = model.tripStops.filter { it.mission.rewardItem in rewards }.map { it.label }.toSet(),
            tripStops = model.tripStops,
            onPlace = model::visitPlace,
            story = model.activeTravelRoute?.name ?: "함께 떠나는 보물 탐험",
        )
        Text("모은 보물 ${controller.items.size} / ${model.plannedMissions.size}", fontSize = 30.sp)
    }
}

@Composable
fun EngineMissionPanel(model: AppViewModel) {
    val controller = model.controller
    val presentation = controller.presentation
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        VoiceStatusPanel(controller.voiceUi)
        if (presentation.caption.isNotBlank()) Text(presentation.caption, fontSize = 30.sp, lineHeight = 44.sp)
        if (presentation.stage == "question") presentation.options.forEachIndexed { i, label ->
            OutlinedButton(onClick = { controller.replay.send(AppEvent.AnswerHeard(label)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
                Text("${i + 1}번 · $label", fontSize = 28.sp)
            }
        }
        Text("주차·휴식 중 지역 퍼즐", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("이동 이야기와 별도로 즐기는 체험이에요. 여기서 얻는 조각은 이동 보물과 구분해요.", fontSize = 24.sp, lineHeight = 34.sp)
        model.selectedRoute.missions.forEach { mission ->
            OutlinedButton(onClick = { model.openDemoMission(mission) }, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) { Text(mission.place, fontSize = 28.sp) }
        }
    }
}

@Composable
fun EngineTripEnding(model: AppViewModel) {
    Column(Modifier.fillMaxWidth().testTag("engine-trip-ending"), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("브루미와 함께한 여행을 마쳐요", fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text("오늘 만난 이야기를 마음에 담고, 다음 모험에서 또 만나요!", fontSize = 30.sp, lineHeight = 42.sp)
        Text("오늘 모은 보물 ${model.controller.items.size}개", fontSize = 32.sp)
        TripTreasureArt(model.controller.items.size, model.plannedMissions.size)
        model.controller.items.forEach { Text("✦ ${it.name}", fontSize = 28.sp) }
        Button(onClick = model::restartTrip, modifier = Modifier.heightIn(min = 80.dp)) { Text("새로운 탐험 준비하기", fontSize = 30.sp) }
        HorizontalDivider()
        Text("콘텐츠와 위치 데이터 출처", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        model.credits.forEach { Text(it, fontSize = 22.sp, lineHeight = 34.sp) }
    }
}
