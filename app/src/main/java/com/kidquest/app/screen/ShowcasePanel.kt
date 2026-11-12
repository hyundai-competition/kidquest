// 시연 입력임을 항상 알리고 일시정지·다음 구간 예약·확인 후 종료만 제공한다.
package com.kidquest.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.screen.common.SelectionCard
import com.kidquest.app.screen.common.Mint
import com.kidquest.app.voice.VoicePhase
import com.kidquest.core.contract.Language
import com.kidquest.core.contract.Screen

@Composable
fun ShowcaseChoice(model: AppViewModel) {
    val english = model.language == Language.EN
    Text(if (english) "Choose how to explore" else "어떻게 탐험할까요?", fontSize = 32.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectionCard(if (english) "Real journey" else "실제 탐험", !model.showcase.selected,
            { model.showcase.select(false) }, Modifier.weight(1f).testTag("select-live"))
        SelectionCard(if (english) "Try the demo" else "시연 체험", model.showcase.selected,
            { model.showcase.select(true) }, Modifier.weight(1f).testTag("select-showcase"))
    }
    Text(if (model.showcase.selected) {
        if (english) "A full adventure with simulated vehicle and location signals. Microphone is optional."
        else "가상 차량·위치 신호로 한 판을 체험해요. 마이크 없이도 끝까지 진행돼요."
    } else if (english) "The story follows actual vehicle and location signals."
    else "실제 차량 상태와 경유지 진입에 맞춰 이야기가 이어져요.", fontSize = 24.sp, lineHeight = 34.sp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShowcasePanel(model: AppViewModel) {
    val session = model.showcase
    val english = model.language == Language.EN
    var confirmExit by rememberSaveable(session.active) { mutableStateOf(false) }
    if (!session.active) {
        if (session.notice.isNotBlank()) Text(session.notice, Modifier.padding(16.dp).testTag("showcase-notice"), fontSize = 24.sp)
        return
    }
    Surface(Modifier.fillMaxWidth().testTag("showcase-banner"), color = Mint.copy(alpha = .09f)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (english) "DEMO · Simulated vehicle and location signals" else "시연 모드 · 가상 차량·위치 신호로 진행합니다", fontSize = 24.sp)
            if (model.controller.voiceStatus.startsWith("음성 재생 불가") || model.controller.voiceStatus.startsWith("재생 오류")) {
                Text(if (english) "Audio is unavailable. The demo continues with text." else "음성을 재생하지 못해 화면 설명으로 이어가요.",
                    Modifier.testTag("showcase-audio-error"), fontSize = 22.sp, color = MaterialTheme.colorScheme.error)
            }
            Text(when {
                session.finished -> if (english) "Demo complete!" else "브루미 시연이 끝났어요!"
                session.paused -> if (english) "Paused · Story and demo signals are on hold" else "일시정지 · 이야기와 시연 신호를 멈췄어요"
                session.nextQueued -> if (english) "Next section queued after the current narration" else "현재 대사가 끝난 뒤 다음 구간으로 이어져요"
                else -> if (english) "Playing automatically · ${model.controller.items.size}/${model.plannedMissions.size} treasures"
                    else "자동 진행 중 · 보물 ${model.controller.items.size}/${model.plannedMissions.size}개"
            }, fontSize = 22.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = session::togglePause, enabled = !session.finished, modifier = Modifier.testTag("showcase-pause")) {
                    Text(if (session.paused) { if (english) "Resume" else "계속 진행" } else if (english) "Pause" else "일시정지", fontSize = 24.sp)
                }
                OutlinedButton(onClick = session::next, enabled = !session.paused && !session.finished && !session.nextQueued && model.controller.screen != Screen.LANDED,
                    modifier = Modifier.testTag("showcase-next")) { Text(if (english) "Next section" else "다음 구간", fontSize = 24.sp) }
                TextButton(onClick = { confirmExit = true }, modifier = Modifier.testTag("showcase-exit")) { Text(if (english) "End demo" else "시연 종료", fontSize = 24.sp) }
            }
            ShowcaseNarration(model, english)
        }
    }
    if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false },
        title = { Text(if (english) "End the demo?" else "시연을 종료할까요?", fontSize = 28.sp) },
        text = { Text(if (english) "Return to setup and actual vehicle signals." else "출발 설정으로 돌아가고 실제 차량 신호로 전환해요.", fontSize = 24.sp) },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(if (english) "Keep exploring" else "계속 탐험하기") } },
        confirmButton = { TextButton(onClick = { confirmExit = false; model.restartTrip() }, modifier = Modifier.testTag("confirm-showcase-exit")) { Text(if (english) "End demo" else "시연 종료") } })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShowcaseNarration(model: AppViewModel, english: Boolean) {
    val controller = model.controller
    val presentation = controller.presentation
    if (presentation.caption.isNotBlank()) Text(presentation.caption, Modifier.testTag("showcase-caption"),
        fontSize = 24.sp, lineHeight = 34.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
    if (controller.voiceUi.phase in setOf(VoicePhase.LISTENING, VoicePhase.PREPARING)) {
        Text(if (english) "Microphone is listening" else "마이크로 대답을 듣고 있어요", fontSize = 22.sp, color = Mint)
    }
    if (presentation.stage == "question") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            presentation.options.forEachIndexed { index, answer ->
                OutlinedButton(onClick = { controller.answerChoice(index) },
                    enabled = presentation.answerReady && controller.screen == Screen.FLYING && !model.showcase.paused,
                    modifier = Modifier.testTag("showcase-answer-$index")) {
                    Text("${index + 1} · $answer", fontSize = 24.sp)
                }
            }
        }
        if (!model.consent && presentation.answerReady) Text(if (english) "Microphone off · Choose an answer or listen to continue."
            else "마이크 꺼짐 · 답을 고르거나 설명을 듣고 이어가요.", fontSize = 22.sp)
    }
}
