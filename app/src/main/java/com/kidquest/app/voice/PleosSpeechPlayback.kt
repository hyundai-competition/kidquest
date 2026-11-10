// Pleos TTS를 ON_DEVICE로 읽는다. SDK 콜백에 대사 ID가 없어 각 재생에 독립 인스턴스를 사용한다.
// 취소된 인스턴스의 늦은 콜백은 SpeechQueue 실행 번호로 무시한다.
package com.kidquest.app.voice

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.Locale

class PleosSpeechPlayback(
    private val context: Context,
    private val text: String,
    private val callback: PlaybackCallback,
    private val locale: Locale = Locale.KOREA,
) : SpeechPlayback {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var closed = false
    private var requested = false
    private val timeout = Runnable { callback.failed("TTS 응답 시간 초과") }
    private val events = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() = onMain {
            callback.started()
            main.removeCallbacks(timeout)
            main.postDelayed(timeout, 90_000)
        }
        override fun onDone() = onMain { callback.completed() }
        override fun onStop() = onMain { callback.interrupted() }
        override fun onError(errorType: ErrorType) = onMain { callback.failed("TTS $errorType") }
        override fun onUpdatedRms(rms: Double) = Unit
    }

    override fun start() {
        main.postDelayed(timeout, 12_000)
        try {
            val sdk = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)
            tts = sdk
            sdk.initialize(object : OnInitializeListener {
                override fun onSuccess() = onMain {
                    if (!requested) {
                        requested = true
                        try {
                            sdk.addEventListener(events)
                            sdk.setLocale(locale)
                            sdk.speak(text, true)
                        } catch (error: Exception) { callback.failed("TTS ${error.javaClass.simpleName}") }
                    }
                }
                override fun onFailure(errorType: ErrorType) = onMain { callback.failed("TTS 초기화 $errorType") }
            })
        } catch (error: Exception) { callback.failed("TTS ${error.javaClass.simpleName}") }
    }

    override fun finish() = close(stopPlayback = false)
    override fun cancel() = close(stopPlayback = true)

    private fun close(stopPlayback: Boolean) {
        if (closed) return
        closed = true
        main.removeCallbacksAndMessages(null)
        tts?.let { sdk ->
            runCatching { sdk.removeEventListener(events) }
            if (stopPlayback) runCatching { sdk.stop() }
            runCatching { sdk.release() }
        }
        tts = null
    }

    private fun onMain(action: () -> Unit) {
        main.post { if (!closed) action() }
    }
}
