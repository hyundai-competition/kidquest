// 명령의 효과음 ID를 시험용 파일에 연결한다. 효과음은 대사를 끊지 않으며 StopAudio에 함께 멈춘다.
package com.kidquest.app.voice

import android.content.Context

class EffectPlayer(private val context: Context, private val report: (String) -> Unit, private val changed: (Boolean) -> Unit = {}) {
    private val active = mutableSetOf<SpeechPlayback>()
    // 정식 음원 확보 전 같은 임시 합성음을 새 대본 ID에 명시적으로 연결한다.
    private val files = mapOf(
        "meteor" to "meteor", "thruster_left" to "thruster_left", "thruster_right" to "thruster_right", "wormhole" to "wormhole",
        "wipe_drops" to "meteor", "turn_tick_left" to "thruster_left", "turn_tick_right" to "thruster_right", "time_tunnel" to "wormhole",
    )

    fun play(id: String) {
        if (id !in files) { report("등록되지 않은 효과음: $id"); return }
        // 신호가 몰려도 소리가 쌓이지 않도록 최근 네 개까지만 허용한다.
        if (active.size >= 4) { report("효과음 동시 재생 제한: $id"); return }
        lateinit var playback: SpeechPlayback
        fun finish() { active.remove(playback); playback.cancel(); changed(active.isNotEmpty()) }
        playback = AssetPlayback(context, "audio/effects/${files.getValue(id)}.wav", object : PlaybackCallback {
            override fun completed() = finish()
            override fun failed(reason: String) { report("효과음 오류: $id / $reason"); finish() }
            override fun interrupted() = finish()
        })
        active += playback
        changed(true)
        report("시험용 효과음: $id")
        playback.start()
    }

    fun stop() {
        val previous = active.toList()
        active.clear()
        changed(false)
        previous.forEach { it.cancel() }
    }
}
