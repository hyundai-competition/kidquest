// 기술 로그와 분리한 사용자용 음성 상태. 인식된 원문이나 음성은 보관하지 않는다.
package com.kidquest.app.voice

enum class VoicePhase { IDLE, PREPARING, SPEAKING, LISTENING, PROCESSING, ERROR }
data class VoiceUiState(val phase: VoicePhase = VoicePhase.IDLE, val caption: String = "", val hint: String = "브루미가 함께할게요")
