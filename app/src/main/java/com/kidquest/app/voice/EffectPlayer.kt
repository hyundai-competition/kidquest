// 명령의 효과음 ID를 시험용 파일에 연결한다. 효과음은 대사를 끊지 않으며 StopAudio에 함께 멈춘다.
package com.kidquest.app.voice

import android.content.Context

class EffectPlayer(private val context: Context, private val report: (String) -> Unit) {
    private val active = mutableSetOf<SpeechPlayback>()
    private val ids = setOf("meteor", "thruster_left", "thruster_right", "wormhole")

    fun play(id: String) {
        if (id !in ids) { report("등록되지 않은 효과음: $id"); return }
        // 신호가 몰려도 소리가 쌓이지 않도록 최근 네 개까지만 허용한다.
        if (active.size >= 4) { report("효과음 동시 재생 제한: $id"); return }
        lateinit var playback: SpeechPlayback
        fun finish() { active.remove(playback); playback.cancel() }
        playback = AssetPlayback(context, "audio/effects/$id.wav", object : PlaybackCallback {
            override fun completed() = finish()
            override fun failed(reason: String) { report("효과음 오류: $id / $reason"); finish() }
            override fun interrupted() = finish()
        })
        active += playback
        report("시험용 효과음: $id")
        playback.start()
    }

    fun stop() {
        val previous = active.toList()
        active.clear()
        previous.forEach { it.cancel() }
    }
}
