// 미션 보상과 네 장소 탐험 완료 인사. 실제 내비 목적지 도착과는 별개의 체험 완료 화면이다.
package com.kidquest.app.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.screen.common.*
import com.kidquest.app.screen.model.MissionNotes

@Composable
fun MissionSuccess(model: AppViewModel) {
    val mission = model.demoMission ?: return
    val count = model.completedDemoMissions.size
    Column(
        Modifier.fillMaxWidth().testTag("mission-success"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        RewardCelebration(count)
        Text("탐험 성공!", color = Mint, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
        Text("${mission.place}의 보물 조각을 찾았어요!", fontSize = 30.sp, textAlign = TextAlign.Center)
        Text("보물 조각 $count / 4", modifier = Modifier.background(Panel, RoundedCornerShape(20.dp)).padding(20.dp), fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(if (count == 4) "네 조각이 모두 모였어요. 우리 여행을 완성해 볼까요?" else if (model.journeyReadyToEnd) "오늘의 탐험을 마무리해 볼까요? 하지 못한 퍼즐은 다음에 만나도 좋아요." else "하나씩 모이는 보물 지도! 다음 장소에는 어떤 이야기가 기다릴까요?", fontSize = 28.sp, lineHeight = 40.sp, textAlign = TextAlign.Center)
        Button(onClick = if (model.journeyReadyToEnd) model::showJourneyEnding else model::closeDemoMission, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)) {
            Text(if (model.journeyReadyToEnd) "여행 마무리 보기" else "다음 장소로", fontSize = 30.sp)
        }
        Text("오늘 알게 된 이야기", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(mission.description.substringBefore("\n"), fontSize = 28.sp, lineHeight = 40.sp)
        if (model.missionSession?.preschool == false) Text("함께 생각해요 · ${MissionNotes.questions[mission.place]}", fontSize = 28.sp, lineHeight = 40.sp)
        OutlinedButton(onClick = model::reviewCompletedMission, modifier = Modifier.heightIn(min = 64.dp)) { Text("완성한 미션 다시 보기", fontSize = 26.sp) }
    }
}

@Composable
fun JourneyEnding(model: AppViewModel) {
    BackHandler(onBack = model::returnToCompletedMap)
    Surface(Modifier.fillMaxSize().testTag("journey-ending"), color = Night) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.widthIn(max = 1000.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("Vroomie · 여행의 마지막 페이지", color = Mint, fontSize = 28.sp)
                RewardCelebration(model.completedDemoMissions.size)
                Text(if (model.skippedDemoMissions.isEmpty()) "우리의 보물 여행 완성!" else "오늘의 보물 탐험을 마쳐요", fontSize = 44.sp, lineHeight = 56.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Text("브루미와 함께한 ${model.selectedRoute.to} 탐험이 끝났어요.", fontSize = 32.sp, lineHeight = 44.sp, textAlign = TextAlign.Center)
                Text("${model.completedDemoMissions.size}개의 보물 조각을 모았어요. 하지 못한 퍼즐은 다음에 만나도 좋아요. 오늘 만난 이야기를 마음에 담고, 남은 여행도 즐겁고 안전하게 이어가요!", fontSize = 28.sp, lineHeight = 42.sp, textAlign = TextAlign.Center)
                model.selectedRoute.missions.forEach { Text("${if (it.id in model.completedDemoMissions) "✓" else "다음 기회에 ·"} ${it.place}", fontSize = 28.sp, color = Mint) }
                Button(onClick = model::readJourneyEnding, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) { Text("브루미 인사 다시 듣기", fontSize = 28.sp) }
                OutlinedButton(onClick = model::returnToCompletedMap, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) { Text("완성한 지도 보기", fontSize = 28.sp) }
                TextButton(onClick = model::restartTrip, modifier = Modifier.heightIn(min = 72.dp)) { Text("새로운 탐험 준비하기", fontSize = 28.sp) }
            }
        }
    }
}
