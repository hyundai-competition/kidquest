// 브루미 말하기와 아이 답변 차례를 구분한다. 주행 규제 화면은 짧고 정적인 상태만 표시한다.
package com.kidquest.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.voice.VoicePhase
import com.kidquest.app.voice.VoiceUiState

@Composable
fun VoiceStatusPanel(state: VoiceUiState, compact: Boolean = false) {
    val color = when (state.phase) {
        VoicePhase.LISTENING -> Color(0xFF27685B)
        VoicePhase.ERROR -> Color(0xFF895A35)
        else -> Color(0xFF31556D)
    }
    Column(Modifier.fillMaxWidth().background(color.copy(alpha = .08f), RoundedCornerShape(24.dp)).padding(24.dp).testTag("voice-status"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(when (state.phase) {
            VoicePhase.SPEAKING -> "브루미가 말하고 있어요"
            VoicePhase.LISTENING -> "이제 내 차례! 듣고 있어요"
            VoicePhase.PREPARING -> "잠깐만 기다려 주세요"
            VoicePhase.PROCESSING -> "답을 확인하고 있어요"
            VoicePhase.ERROR -> "다시 시도해도 괜찮아요"
            VoicePhase.IDLE -> "브루미와 함께해요"
        }, color = color, fontSize = if (compact) 28.sp else 30.sp, fontWeight = FontWeight.Bold)
        Text(when { state.phase == VoicePhase.LISTENING -> "마이크 켜짐 · 지금 대답해 주세요"; state.phase == VoicePhase.PREPARING && state.caption.isBlank() -> "마이크 준비 중"; else -> "마이크 꺼짐" }, color = color, fontSize = 24.sp)
        if (!compact && state.caption.isNotBlank()) Text(state.caption, fontSize = 26.sp, lineHeight = 38.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
        Text(state.hint, fontSize = 24.sp, lineHeight = 34.sp, maxLines = if (compact) 2 else 4, overflow = TextOverflow.Ellipsis)
    }
}
