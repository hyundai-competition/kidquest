// 한 번의 음성 재생 수명. 실제 SDK와 녹음 파일을 같은 규칙으로 제어한다.
package com.kidquest.app.voice

import com.kidquest.core.contract.Command

interface SpeechPlayback {
    fun start()
    fun finish() = cancel()
    fun cancel()
}

interface PlaybackCallback {
    fun started() = Unit
    fun completed()
    fun failed(reason: String)
    fun interrupted()
}

fun interface SpeechFactory {
    fun create(line: Command.Say, callback: PlaybackCallback): SpeechPlayback?
}
