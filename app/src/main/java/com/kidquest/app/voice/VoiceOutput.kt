// 명령을 소리로 옮긴다. 화면·음성 역할이 TTS, 효과음, STT, 오디오 포커스로 채운다.
// 지금은 가짜다. 로그만 남기고, 말을 다 했다고 바로 알려서 소리 없이 흐름을 돌려 볼 수 있다.
// 아이 말 내용은 로그에도 남기지 않는다.
package com.kidquest.app.voice

import android.util.Log
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.EventListener
import com.kidquest.core.contract.VoicePort

class VoiceOutput : VoicePort {
    private var listener: EventListener? = null

    override fun start(listener: EventListener) {
        this.listener = listener
    }

    override fun stop() {
        listener = null
    }

    override fun handle(command: Command) {
        Log.d(TAG, command.toString())
        when (command) {
            is Command.Say -> listener?.onEvent(AppEvent.LineFinished(command.lineId))
            is Command.Listen -> listener?.onEvent(AppEvent.AnswerHeard(null))
            else -> Unit
        }
    }

    private companion object {
        const val TAG = "KidQuestVoice"
    }
}
