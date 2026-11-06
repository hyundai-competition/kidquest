// ViewModel 설정과 엔진 명령을 화면에 연결한다. 주행 규제 중에는 상태와 멈춤 조작만 표시한다.
package com.kidquest.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    KidQuestTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Night) {
            if (controller.vehicle.restricted) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp), verticalArrangement = Arrangement.Center) {
                    Text(controller.status ?: titleOf(controller.screen), fontSize = 40.sp)
                    StopButton(model)
                }
            } else {
                SetupLayout(model)
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
                Text("개발 확인 · 음성은 아직 연결 전", fontSize = 20.sp)
                if (model.controller.replayUsed) Text("재생 모드 입력이 포함된 시험입니다", color = Mint, fontSize = 22.sp)
                // 재생 패널 자체도 스크롤하므로 높이를 제한해 바깥 스크롤과 충돌하지 않게 한다.
                Column(Modifier.heightIn(max = 480.dp)) { ReplayPanel(model.controller.replay) }
            }
        }
        if (maxWidth >= 1000.dp) {
            Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(40.dp), horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("KIDQUEST", color = Mint, fontSize = 30.sp)
                    Text("벨트를 매면,\n모험이 시작돼요.", fontSize = 48.sp, lineHeight = 64.sp)
                    RocketArt(Modifier.fillMaxWidth().height(240.dp))
                }
                Box(Modifier.weight(1.2f)) { panel() }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("KIDQUEST", color = Mint, fontSize = 28.sp)
                panel()
            }
        }
    }
}

@Composable
private fun CurrentScreen(model: AppViewModel) {
    val controller = model.controller
    when (controller.screen) {
        Screen.START -> StartScreen(model)
        Screen.CREW_CHECK -> CrewCheckScreen(controller)
        else -> {
            Text(titleOf(controller.screen), fontSize = 40.sp)
            controller.status?.takeUnless { it == titleOf(controller.screen) }?.let { Text(it, fontSize = 30.sp) }
            when (controller.screen) {
                Screen.FLYING, Screen.PAUSED -> StopButton(model)
                Screen.DOCKED -> {
                    StopButton(model)
                    Button(onClick = { controller.dispatch(AppEvent.EndTripPressed) }) { Text("여기서 착륙", fontSize = 30.sp) }
                }
                Screen.LANDED -> {
                    controller.items.forEach { Text(it.name, fontSize = 30.sp) }
                    Button(onClick = model::restartTrip) { Text("다시 하기", fontSize = 30.sp) }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun StopButton(model: AppViewModel) {
    val stopped = model.controller.vehicle.parentStopped
    Button(onClick = { model.controller.dispatch(AppEvent.ParentStopChanged(!stopped)) }, modifier = Modifier.heightIn(min = 88.dp)) {
        Text(if (stopped) "다시 켜기" else "소리 끄기", fontSize = 32.sp)
    }
}

private fun titleOf(screen: Screen) = when (screen) {
    Screen.START -> "출발 전 설정이 필요해요"
    Screen.CREW_CHECK -> "탑승과 벨트를 확인해 주세요"
    Screen.FLYING -> "비행 중"
    Screen.PAUSED -> "잠시 멈췄어요"
    Screen.DOCKED -> "잠깐 정박"
    Screen.LANDED -> "항해 완료"
}
