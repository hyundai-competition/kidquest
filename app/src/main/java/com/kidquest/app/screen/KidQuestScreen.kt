// ViewModel 설정과 엔진 명령을 화면에 연결한다. 주행 규제 중에는 상태와 멈춤 조작만 표시한다.
package com.kidquest.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.replay.ReplayPanel
import com.kidquest.app.screen.common.*
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Screen

@Composable
fun KidQuestScreen(model: AppViewModel) {
    val controller = model.controller
    LaunchedEffect(controller.vehicle.restricted) {
        model.onDrivingRestrictionChanged(controller.vehicle.restricted)
    }
    KidQuestTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Night) {
            if (controller.vehicle.restricted) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp), verticalArrangement = Arrangement.Center) {
                    Text(if (model.demoMission != null || model.journeyEndingVisible) "소리로 이어가는 보물 탐험" else controller.status ?: titleOf(controller.screen), fontSize = 40.sp)
                    VoiceStatusPanel(controller.voiceUi, compact = true)
                    Text("퍼즐은 주차·휴식 중에 이어갈 수 있어요", fontSize = 26.sp)
                    StopButton(model)
                }
            } else {
                if (model.journeyEndingVisible) {
                    JourneyEnding(model)
                } else if (model.demoMission != null) {
                    DemoMissionDialog(model)
                } else if (controller.screen == Screen.START || controller.screen == Screen.CREW_CHECK) {
                    SetupLayout(model)
                } else {
                    ExpeditionLayout(model)
                }
            }
        }
    }
}

@Composable
private fun SetupLayout(model: AppViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val panel: @Composable () -> Unit = {
            Column(Modifier.background(Panel, RoundedCornerShape(28.dp)).padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                CurrentScreen(model)
                HorizontalDivider()
                Text("개발 확인 · ${model.controller.voiceStatus}", fontSize = 26.sp)
                if (model.controller.replayUsed) Text("재생 모드 입력이 포함된 시험입니다", color = Mint, fontSize = 28.sp)
                // 재생 패널 자체도 스크롤하므로 높이를 제한해 바깥 스크롤과 충돌하지 않게 한다.
                Column(Modifier.heightIn(max = 480.dp)) { ReplayPanel(model.controller.replay) }
            }
        }
        if (maxWidth >= 1000.dp) {
            Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(40.dp), horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Vroomie", color = Mint, fontSize = 52.sp, lineHeight = 66.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                    Text("브루미와 떠나는 역사 보물찾기", fontSize = 28.sp, lineHeight = 40.sp)
                    Text(model.selectedRoute.title, fontSize = 50.sp, lineHeight = 66.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text(model.selectedRoute.subtitle, color = Muted, fontSize = 30.sp, lineHeight = 44.sp)
                    TreasureMapArt(Modifier.fillMaxWidth().height(280.dp), model.selectedRoute.to)

                }
                Box(Modifier.weight(1.2f)) { panel() }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("Vroomie", color = Mint, fontSize = 42.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                panel()
            }
        }
    }
}

@Composable
internal fun CurrentScreen(model: AppViewModel) {
    val controller = model.controller
    when (controller.screen) {
        Screen.START -> StartScreen(model)
        Screen.CREW_CHECK -> CrewCheckScreen(controller)
        else -> {
            Text(titleOf(controller.screen), fontSize = 46.sp)
            controller.status?.takeUnless { it == titleOf(controller.screen) }?.let { Text(it, fontSize = 36.sp) }
            when (controller.screen) {
                Screen.FLYING, Screen.PAUSED -> StopButton(model)
                Screen.DOCKED -> {
                    StopButton(model)
                    Button(onClick = { controller.dispatch(AppEvent.EndTripPressed) }) { Text("여기서 탐험 마치기", fontSize = 36.sp) }
                }
                Screen.LANDED -> {
                    Text("브루미와 함께한 오늘의 탐험이 끝났어요. 함께해 줘서 고마워요! 남은 여행도 즐겁고 안전하게 이어가요.", fontSize = 30.sp, lineHeight = 44.sp)
                    controller.items.forEach { Text(it.name, fontSize = 36.sp) }
                    Button(onClick = model::restartTrip) { Text("다시 하기", fontSize = 36.sp) }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun StopButton(model: AppViewModel) {
    val inMission = model.demoMission != null || model.journeyEndingVisible
    val stopped = if (inMission) model.missionAudioMuted else model.controller.vehicle.parentStopped
    Button(onClick = {
        if (inMission) model.toggleMissionAudio()
        else model.controller.dispatch(AppEvent.ParentStopChanged(!stopped))
    }, modifier = Modifier.heightIn(min = 88.dp)) {
        Text(if (stopped) "다시 켜기" else "소리 끄기", fontSize = 38.sp)
    }
}

private fun titleOf(screen: Screen) = when (screen) {
    Screen.START -> "출발 전 설정이 필요해요"
    Screen.CREW_CHECK -> "탑승과 벨트를 확인해 주세요"
    Screen.FLYING -> "보물 탐험 중"
    Screen.PAUSED -> "잠시 멈췄어요"
    Screen.DOCKED -> "탐험 쉬는 중"
    Screen.LANDED -> "탐험 완료"
}

@Composable
private fun ExpeditionLayout(model: AppViewModel) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        JourneyBoard(model.selectedRoute, model.completedDemoMissions, model::openDemoMission, skipped = model.skippedDemoMissions)
        if (model.journeyReadyToEnd) {
            Button(onClick = model::showJourneyEnding, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)) {
                Text("여행 마무리 보기", fontSize = 32.sp)
            }
        }
        Column(
            Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(28.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CurrentScreen(model)
            HorizontalDivider()
            Text("개발 확인 · ${model.controller.voiceStatus}", fontSize = 24.sp)
            if (model.controller.replayUsed) Text("재생 모드 입력이 포함된 시험입니다", color = Mint)
            Column(Modifier.heightIn(max = 320.dp)) { ReplayPanel(model.controller.replay) }
        }
    }
}
