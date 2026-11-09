// 본편 지도와 분리한 P 주차 전용 놀이터. 선택한 도시의 네 퍼즐과 별도 보상을 보여준다.
package com.kidquest.app.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel

@Composable
fun MiniGamesScreen(model: AppViewModel) {
    BackHandler(onBack = model::closeMiniGames)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp)
        .testTag("mini-games-page"), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = model::closeMiniGames) { Text("본편 지도로 돌아가기", fontSize = 28.sp) }
        Text("${model.selectedRoute.to} · 미니게임 놀이터", fontSize = 38.sp)
        Text("P 주차 중에 즐기는 별도 퍼즐이에요. 기어를 바꾸면 닫히고 본편으로 돌아가요.", fontSize = 28.sp, lineHeight = 40.sp)
        Text("놀이터 보물 ${model.completedDemoMissions.size} / 4", fontSize = 30.sp)
        model.selectedRoute.missions.forEach { mission ->
            OutlinedCard(onClick = { model.openDemoMission(mission) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${if (mission.id in model.completedDemoMissions) "✓ " else ""}${mission.place}", fontSize = 32.sp)
                    Text(mission.task, fontSize = 26.sp, lineHeight = 38.sp)
                }
            }
        }
        if (model.journeyReadyToEnd) Button(onClick = model::showJourneyEnding) { Text("놀이터 탐험 마무리", fontSize = 28.sp) }
    }
}
