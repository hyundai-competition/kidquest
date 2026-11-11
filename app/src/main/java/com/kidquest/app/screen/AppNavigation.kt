// 모든 화면의 뒤로·끝내기와 확인 대화상자. 취소는 진행을 바꾸지 않고, 확인 후에만 초기화·종료한다.
package com.kidquest.app.screen

import androidx.activity.compose.BackHandler
import com.kidquest.app.screen.AppText as Text
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.core.contract.Screen

@Composable
fun AppNavigation(model: AppViewModel, onExit: () -> Unit, content: @Composable () -> Unit) {
    var confirmation by rememberSaveable { mutableStateOf("") }
    fun back() {
        when {
            model.journeyEndingVisible -> model.returnToCompletedMap()
            model.demoMission != null -> model.closeDemoMission()
            model.miniGamesVisible -> model.closeMiniGames()
            model.controller.screen == Screen.START && model.setupStep > 0 -> model.changeStep(model.setupStep - 1)
            model.controller.screen == Screen.START -> confirmation = "exit"
            else -> confirmation = "restart"
        }
    }
    BackHandler { back() }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        CompanionNavigationBar(model, ::back, { confirmation = "exit" })
        if (model.controller.vehicle.wiperOn && !model.controller.vehicle.restricted) {
            Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (model.weatherStyle == WeatherStyle.RAIN) "와이퍼 · 비 연출" else "와이퍼 · 눈 연출", fontSize = 22.sp)
                if (model.canPlayMiniGames) TextButton(onClick = { model.selectWeatherStyle(if (model.weatherStyle == WeatherStyle.RAIN) WeatherStyle.SNOW else WeatherStyle.RAIN) }) {
                    Text(if (model.weatherStyle == WeatherStyle.RAIN) "눈으로 바꾸기" else "비로 바꾸기", fontSize = 22.sp)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
    if (confirmation.isNotEmpty()) {
        val exiting = confirmation == "exit"
        AlertDialog(
            onDismissRequest = { confirmation = "" },
            title = { Text(if (exiting) "브루미를 끝낼까요?" else "처음으로 돌아갈까요?", fontSize = 32.sp) },
            text = { Text(if (exiting) "현재 탐험을 끝내고 소리와 마이크를 꺼요." else "현재 탐험 진행을 정리하고 출발 전 설정으로 돌아가요.", fontSize = 26.sp, lineHeight = 38.sp) },
            dismissButton = { TextButton(onClick = { confirmation = "" }) { Text("계속 탐험하기", fontSize = 26.sp) } },
            confirmButton = { TextButton(onClick = {
                confirmation = ""
                if (exiting) { model.controller.stop(); onExit() } else model.restartTrip()
            }, modifier = Modifier.testTag("confirm-navigation")) { Text(if (exiting) "끝내기" else "처음으로", fontSize = 26.sp) } },
        )
    }
}

@Composable
private fun CompanionNavigationBar(model: AppViewModel, back: () -> Unit, exit: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val setupHero = maxWidth >= 1000.dp && !model.controller.vehicle.restricted &&
            model.controller.screen in listOf(Screen.START, Screen.CREW_CHECK) &&
            model.demoMission == null && !model.miniGamesVisible && !model.journeyEndingVisible
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = back, modifier = Modifier.heightIn(min = 64.dp).testTag("app-back")) { Text("← 뒤로", fontSize = 26.sp) }
            if (!setupHero) VroomieCompanion(model.controller.vehicle, Modifier.width(160.dp).height(128.dp))
            TextButton(onClick = exit, modifier = Modifier.heightIn(min = 64.dp).testTag("app-exit")) { Text("끝내기", fontSize = 26.sp) }
        }
    }
}
