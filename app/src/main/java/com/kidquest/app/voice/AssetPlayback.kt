// 앱에 포함한 효과음과 녹음 파일을 재생한다. 정상 완료·실패·중단을 구분하고 자원을 해제한다.
package com.kidquest.app.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

class AssetPlayback(
    private val context: Context,
    private val path: String,
    private val callback: PlaybackCallback,
) : SpeechPlayback {
    private var player: MediaPlayer? = null
    private var closed = false
    private val main = Handler(Looper.getMainLooper())
    private val timeout = Runnable { if (!closed) callback.failed("음원 응답 시간 초과") }

    override fun start() {
        try {
            val media = MediaPlayer()
            player = media
            media.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            context.assets.openFd(path).use { media.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            media.setVolume(0.65f, 0.65f)
            media.setOnPreparedListener { if (!closed) { it.start(); callback.started() } }
            media.setOnCompletionListener { if (!closed) callback.completed() }
            media.setOnErrorListener { _, what, extra ->
                if (!closed) callback.failed("음원 재생 오류 $what/$extra")
                true
            }
            main.postDelayed(timeout, 90_000)
            media.prepareAsync()
        } catch (error: Exception) { if (!closed) callback.failed("음원 ${error.javaClass.simpleName}") }
    }

    override fun cancel() {
        if (closed) return
        closed = true
        main.removeCallbacksAndMessages(null)
        player?.let {
            it.setOnCompletionListener(null)
            it.setOnErrorListener(null)
            it.setOnPreparedListener(null)
            runCatching { it.release() }
        }
        player = null
    }
}
