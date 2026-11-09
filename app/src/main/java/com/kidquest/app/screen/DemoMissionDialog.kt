// 장소 설명과 체험을 앱 영역 전체에 표시한다. 지도는 가리고 콘텐츠 폭과 그림 비율은 일정하게 유지한다.
package com.kidquest.app.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.screen.common.*
import com.kidquest.app.screen.model.MissionNotes
import com.kidquest.app.screen.puzzle.MissionGame

@Composable
fun DemoMissionDialog(model: AppViewModel) {
    val mission = model.demoMission ?: return
    val session = model.missionSession ?: return
    BackHandler(onBack = model::closeDemoMission)
    Surface(Modifier.fillMaxSize().testTag("mission-page"), color = Night) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Vroomie · ${if (session.preschool) "꼬마" else "어린이"} 탐험", color = Mint, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = model::closeDemoMission, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("지도로 돌아가기", fontSize = 26.sp)
                }
            }
            HorizontalDivider()
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val wide = maxWidth >= 900.dp
                if (!session.playing) {
                    MissionBriefing(model, wide)
                } else {
                    Column(
                        Modifier.align(Alignment.TopCenter).widthIn(max = 1100.dp).fillMaxWidth()
                            .verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(mission.place, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                        Text(mission.task, fontSize = 30.sp)
                        if (session.done && !model.reviewingCompletedMission) {
                            MissionSuccess(model)
                        } else {
                            VoiceStatusPanel(model.controller.voiceUi)
                            if (session.voiceMode) VoiceChoicePanel(model)
                            else {
                                MissionGame(session, onAction = model::missionAction)
                                OutlinedButton(onClick = model::beginVoiceQuiz) { Text("소리로 퀴즈 풀기", fontSize = 26.sp) }
                            }
                            TextButton(onClick = model::skipCurrentMission, modifier = Modifier.heightIn(min = 64.dp)) { Text("이번 미션 건너뛰기", fontSize = 26.sp) }
                        }
                        if (!session.done) Text(session.feedback, fontSize = 28.sp)
                        OutlinedButton(onClick = { session.playing = false; model.readMission() }) { Text("장소 설명 다시 보기") }
                    }
                }
            }
            if (!session.playing) {
                HorizontalDivider()
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Button(onClick = model::beginMission, modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = 72.dp)) {
                        Text("미션 시작", fontSize = 30.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionBriefing(model: AppViewModel, wide: Boolean) {
    val mission = model.demoMission ?: return
    val session = model.missionSession ?: return
    val picture: @Composable () -> Unit = {
        Surface(color = Panel, shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${model.selectedRoute.to} · 역사 보물찾기", color = Mint, fontSize = 26.sp)
                TreasureMapArt(Modifier.fillMaxWidth().aspectRatio(1.6f).testTag("mission-art"), model.selectedRoute.to)
                Text(mission.place, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                Text(mission.task, color = Muted, fontSize = 28.sp, lineHeight = 40.sp)
            }
        }
    }
    val explanation: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("${mission.place} · ${if (session.preschool) "꼬마" else "어린이"} 탐험", fontSize = 34.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold)
            VoiceStatusPanel(model.controller.voiceUi)
            Text(mission.description, modifier = Modifier.fillMaxWidth(), fontSize = 28.sp, lineHeight = 44.sp)
            // 좁은 창에서도 조작이 잘리지 않도록 음성 버튼은 줄을 나눠 배치한다.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = model::readMission, modifier = Modifier.heightIn(min = 56.dp)) { Text("설명 다시 듣기", fontSize = 26.sp) }
                TextButton(onClick = model::stopNarration, modifier = Modifier.heightIn(min = 56.dp)) { Text("음성 멈추기", fontSize = 26.sp) }
                OutlinedButton(onClick = model::beginVoiceQuiz, modifier = Modifier.heightIn(min = 64.dp)) { Text("소리로 퀴즈 풀기", fontSize = 26.sp) }
                TextButton(onClick = model::skipCurrentMission, modifier = Modifier.heightIn(min = 64.dp)) { Text("이번 미션 건너뛰기", fontSize = 26.sp) }
            }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (wide) {
            Row(
                Modifier.widthIn(max = 1280.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) { picture() }
                Box(Modifier.weight(1.15f)) { explanation() }
            }
        } else {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                picture()
                explanation()
            }
        }
    }
}
