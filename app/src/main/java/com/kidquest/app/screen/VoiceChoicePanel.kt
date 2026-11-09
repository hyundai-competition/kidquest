// 세 번호를 읽고 TTS 완료 후 마이크를 여는 퀴즈. 터치는 정차 중의 보조 입력이다.
package com.kidquest.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.voice.VoicePhase

@Composable
fun VoiceChoicePanel(model: AppViewModel) {
    val session = model.missionSession ?: return
    Text(session.quiz.question, fontSize = 32.sp, lineHeight = 44.sp)
    Text(if (model.consent) "브루미가 보기를 다 읽으면 번호로 말해 주세요." else "보기를 듣고 정차 중 화면으로 골라 주세요. 듣기만 해도 좋아요.", fontSize = 26.sp)
    session.quiz.options.forEachIndexed { index, choice ->
        OutlinedButton(onClick = { model.answerVoiceChoice(index) }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)) {
            Text("${index + 1}번 · $choice", fontSize = 30.sp)
        }
    }
    session.lastChoice?.let { Text("${it + 1}번을 골랐어요", fontSize = 28.sp) }
    if (model.consent) {
        Button(onClick = model::resumeAnswering, enabled = model.controller.voiceUi.phase !in listOf(VoicePhase.SPEAKING, VoicePhase.LISTENING, VoicePhase.PREPARING), modifier = Modifier.heightIn(min = 72.dp)) { Text("다시 대답하기", fontSize = 28.sp) }
    } else {
        Text("말로 답하기 동의가 꺼져 있어요. 지금은 화면으로 답하거나 이야기를 듣고 넘어갈 수 있어요.", fontSize = 26.sp, lineHeight = 38.sp)
    }
    OutlinedButton(onClick = model::repeatVoiceQuiz, modifier = Modifier.heightIn(min = 64.dp)) { Text("문제와 보기 다시 듣기", fontSize = 26.sp) }
}
