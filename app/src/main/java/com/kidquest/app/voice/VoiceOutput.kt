// 명령을 소리로 옮긴다. 화면·음성 역할이 TTS, 효과음, STT, 오디오 포커스로 채운다.
// 지금은 로그만 남겨서 흐름을 확인한다. 아이 말 내용은 로그에도 남기지 않는다.
package com.kidquest.app.voice

import android.util.Log
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.CommandHandler

class VoiceOutput : CommandHandler {
    override fun handle(command: Command) {
        Log.d(TAG, command.toString())
    }

    private companion object {
        const val TAG = "KidQuestVoice"
    }
}
