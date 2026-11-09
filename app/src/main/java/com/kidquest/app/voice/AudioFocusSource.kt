// 앱의 소리 사용권을 요청한다. 내비 안내의 duck 요청도 음량 감소 대신 완전 중단으로 전달한다.
package com.kidquest.app.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper

interface AudioFocusPort {
    fun start(changed: (Boolean) -> Unit)
    fun request(userInitiated: Boolean = false): Boolean
    fun stop()
}

class AudioFocusSource(context: Context) : AudioFocusPort {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val main = Handler(Looper.getMainLooper())
    private var changed: ((Boolean) -> Unit)? = null
    private var granted = false
    private var waiting = false
    private var requested = false
    private var transientLoss = false
    private val listener = AudioManager.OnAudioFocusChangeListener { value ->
        if (changed != null) {
            granted = value == AudioManager.AUDIOFOCUS_GAIN
            waiting = !granted
            transientLoss = value == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || value == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
            changed?.invoke(granted)
        }
    }
    private val focus = if (Build.VERSION.SDK_INT >= 26) AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .setWillPauseWhenDucked(true)
        .setAcceptsDelayedFocusGain(true)
        .setOnAudioFocusChangeListener(listener, main).build() else null

    override fun start(changed: (Boolean) -> Unit) { this.changed = changed }
    @Suppress("DEPRECATION")
    override fun request(userInitiated: Boolean): Boolean {
        if (granted) return true
        // 내비가 소리를 쓰는 동안 새 대사·효과음마다 사용권을 빼앗지 않는다.
        if (waiting && (transientLoss || !userInitiated)) return false
        requested = true
        val result = runCatching {
            if (Build.VERSION.SDK_INT >= 26) manager.requestAudioFocus(focus!!)
            else manager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }.getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        waiting = !granted
        transientLoss = result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED
        changed?.invoke(granted)
        return granted
    }
    @Suppress("DEPRECATION")
    override fun stop() {
        changed = null
        if (requested) {
            if (Build.VERSION.SDK_INT >= 26) manager.abandonAudioFocusRequest(focus!!)
            else manager.abandonAudioFocus(listener)
        }
        main.removeCallbacksAndMessages(null)
        granted = false; waiting = false; requested = false; transientLoss = false
    }
}
