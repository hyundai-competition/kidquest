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
                NaviGuideFrame(controller.vehicle.navi, controller.screen) {
                    VehicleBackdrop(controller.vehicle, model.weatherStyle) {
                        AppNavigation(model, onExit) {
                            if (controller.vehicle.restricted) {
                                DrivingStatusCard(model)
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
}

@Composable
private fun SetupLayout(model: AppViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val panel: @Composable () -> Unit = {
            Column(Modifier.background(Panel, RoundedCornerShape(28.dp)).padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                CurrentScreen(model)
            }
        }
        if (maxWidth >= 1000.dp) {
            Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(40.dp), horizontalArrangement = Arrangement.spacedBy(40.dp), verticalAlignment = Alignment.CenterVertically) {
                SetupIntro(model, Modifier.weight(1f), large = true)
                Box(Modifier.weight(1.2f)) { panel() }
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SetupIntro(model, Modifier.fillMaxWidth(), large = false)
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
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(titleOf(controller.screen), fontSize = 46.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                controller.status?.takeUnless { it == titleOf(controller.screen) }?.let { Text(it, Modifier.fillMaxWidth(), fontSize = 36.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
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
}

@Composable
internal fun StopButton(model: AppViewModel) {
    val inMission = (model.demoMission != null || model.journeyEndingVisible)
    val stopped = if (inMission) model.missionAudioMuted else model.controller.vehicle.parentStopped
    Button(onClick = {
        if (inMission) model.toggleMissionAudio()
        else if (stopped) model.controller.resumeAudio()
        else model.controller.dispatch(AppEvent.ParentStopChanged(true))
    }, modifier = Modifier.widthIn(min = 240.dp, max = 400.dp).heightIn(min = 88.dp).testTag("stop-audio")) {
        Text(if (stopped) "다시 켜기" else "소리 끄기", fontSize = 38.sp)
    }
}

internal fun titleOf(screen: Screen) = when (screen) {
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
        }
    }
}

@Composable
private fun KoreanPlayground(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppLanguage provides Language.KO, content = content)
}
