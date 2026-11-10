// ViewModel 설정과 엔진 명령을 화면에 연결한다. 주행 규제 중에는 상태와 멈춤 조작만 표시한다.
package com.kidquest.app.screen

import com.kidquest.app.screen.AppText as Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.replay.ReplayPanel
import com.kidquest.app.screen.common.*
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Background
import androidx.compose.ui.graphics.Color
import com.kidquest.core.contract.Screen
import com.kidquest.core.contract.Language

@Composable
fun KidQuestScreen(model: AppViewModel, onExit: () -> Unit = {}) {
    val controller = model.controller
    CompositionLocalProvider(LocalAppLanguage provides model.language) {
        KidQuestTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = if (controller.screen in listOf(Screen.DOCKED, Screen.LANDED)) when (controller.background) {
                Background.COOL -> Color(0xFFE8F1F6)
                Background.WARM -> Color(0xFFFFF0DF)
                Background.NORMAL -> Night
            } else Night) {
                VehicleBackdrop(controller.vehicle, model.weatherStyle) {
                    AppNavigation(model, onExit) {
                        if (controller.vehicle.restricted) {
                            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                                Text(controller.status ?: titleOf(controller.screen), fontSize = 32.sp, modifier = Modifier.testTag("restricted-status"))
                                StopButton(model)
                            }
                        } else when {
                            model.journeyEndingVisible -> KoreanPlayground { JourneyEnding(model) }
                            model.demoMission != null -> KoreanPlayground { DemoMissionDialog(model) }
                            model.miniGamesVisible -> KoreanPlayground { MiniGamesScreen(model) }
                            controller.screen == Screen.START || controller.screen == Screen.CREW_CHECK -> SetupLayout(model)
                            else -> ExpeditionLayout(model)
                        }
                    }
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
                DeveloperPanel(model, 480)
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
                    ParentBeltConfirmation(controller)
                    StopButton(model)
                    Button(onClick = { controller.dispatch(AppEvent.EndTripPressed) }) { Text("여기서 탐험 마치기", fontSize = 36.sp) }
                }
                Screen.LANDED -> EngineTripEnding(model)
                else -> Unit
            }
        }
    }
}

@Composable
private fun StopButton(model: AppViewModel) {
    val inMission = (model.demoMission != null || model.journeyEndingVisible)
    val stopped = if (inMission) model.missionAudioMuted else model.controller.vehicle.parentStopped
    Button(onClick = {
        if (inMission) model.toggleMissionAudio()
        else if (stopped) model.controller.resumeAudio()
        else model.controller.dispatch(AppEvent.ParentStopChanged(true))
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
        EngineJourneyBoard(model)
        Column(
            Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(28.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CurrentScreen(model)
            if (model.controller.screen != Screen.LANDED) EngineMissionPanel(model)
            HorizontalDivider()
            DeveloperPanel(model, 320)
        }
    }
}

@Composable
private fun KoreanPlayground(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppLanguage provides Language.KO, content = content)
}

@Composable
private fun DeveloperPanel(model: AppViewModel, maxHeight: Int) {
    KoreanPlayground {
        Column(Modifier.testTag("developer-panel"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("개발 확인 · ${model.controller.voiceStatus}", fontSize = 24.sp)
            if (model.controller.replayUsed) Text("재생 모드 입력이 포함된 시험입니다", color = Mint, fontSize = 24.sp)
            // 재생 패널 자체도 스크롤하므로 바깥 스크롤과 충돌하지 않게 높이를 제한한다.
            Column(Modifier.heightIn(max = maxHeight.dp)) { ReplayPanel(model.controller.replay, model.episode) }
        }
    }
}
