// 본편의 조용한 구간에만 낮은 음량으로 자체 제작 루프를 튼다. 발화 사이 짧은 틈에는 시작하지 않는다.
package com.kidquest.app.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log

class QuietMusic(private val context: Context, private val canUseFocus: () -> Boolean) {
    private val main = Handler(Looper.getMainLooper())
    private var allowed = false
    private var player: MediaPlayer? = null
    private var failed = false
    internal val isPlaying get() = player?.isPlaying == true
    private val start = Runnable {
        if (!allowed || failed || player != null || !canUseFocus()) return@Runnable
        try {
            val media = MediaPlayer()
            player = media
            media.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            context.assets.openFd("audio/music/vroomie_trail.wav").use { media.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            media.isLooping = true
            media.setVolume(.1f, .1f)
            media.setOnPreparedListener { if (allowed && player === it) { it.start(); Log.i("KQBgm", "started") } }
            media.setOnErrorListener { _, _, _ -> failed = true; stop(); Log.w("KQBgm", "playback failed"); true }
            media.prepareAsync()
        } catch (_: Exception) { failed = true; stop(); Log.w("KQBgm", "unavailable") }
    }
    fun allow(value: Boolean) {
        if (allowed == value) return
        allowed = value
        if (value) main.postDelayed(start, 1500) else stop()
    }
    private fun stop() {
        main.removeCallbacks(start)
        player?.let { runCatching { it.release() }; Log.i("KQBgm", "stopped") }
        player = null
    }
    fun release() { allowed = false; stop() }
}

data class QuietMusicState(
    val flying: Boolean = false, val enabled: Boolean = true, val focus: Boolean = true,
    val speech: Boolean = false, val effects: Boolean = false, val listening: Boolean = false,
    val instrument: Boolean = false, val muted: Boolean = false,
) {
    val shouldPlay get() = flying && enabled && focus && !speech && !effects && !listening && !instrument && !muted
}
