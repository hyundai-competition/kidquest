// 한 번의 한국어 ON_DEVICE 인식. 최종 결과만 전달하고 취소·시간 초과·늦은 콜백을 구분한다.
package com.kidquest.app.voice

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.stt.listener.OnInitializeListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale

interface RecognitionJob { fun start(); fun cancel() }
interface RecognitionCallback {
    fun ready() = Unit
    fun listening()
    fun finished(text: String?)
    fun failed(reason: String)
}
fun interface RecognitionFactory { fun create(callback: RecognitionCallback): RecognitionJob }

class PleosRecognition(context: Context, private val seconds: Int, private val callback: RecognitionCallback, private val diagnostic: (String) -> Unit = { Log.i("KQRecognition", it) }) : RecognitionJob {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var sdk: SpeechToText? = null
    private var closed = false
    private var requested = false
    private var ready = false
    private val timeout = Runnable { if (ready) finish(null) else fail("마이크가 시작되지 않았어요. 기기 마이크 설정을 확인해 주세요.") }
    private val events = object : ResultListener {
        override fun onReady() = post { diagnostic("ready"); if (requested) { ready = true; callback.ready() } }
        override fun onStartedRecognition() = post { diagnostic("started"); callback.listening() }
        override fun onUpdated(stt: String, completed: Boolean) { if (completed) post { finish(stt.takeIf { it.isNotBlank() }) } }
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        // 종료 통지 직후 최종 결과가 오는 경우를 위해 짧게 기다린다.
        override fun onEndedRecognition() = post { diagnostic("ended"); main.postDelayed({ if (!closed) finish(null) }, 350) }
        override fun onError(errorType: ErrorType) = post { diagnostic("error $errorType"); fail("음성을 듣지 못했어요. 다시 시도할 수 있어요.") }
    }
    override fun start() {
        main.postDelayed(timeout, (seconds.coerceIn(1, 15) + 8) * 1000L)
        try {
            sdk = SpeechToText(app, Mode.ON_DEVICE).also { speech ->
                speech.initialize(object : OnInitializeListener {
                    override fun onSuccess() = post {
                        diagnostic("initialized")
                        try {
                            speech.addListener(events)
                            speech.setLocale(Locale.KOREA)
                            requested = true
                            speech.request()
                            diagnostic("requested")
                            main.removeCallbacks(timeout)
                            main.postDelayed(timeout, seconds.coerceIn(1, 15) * 1000L)
                        } catch (_: Exception) { fail("마이크를 시작하지 못했어요. 다시 시도해 주세요.") }
                    }
                    override fun onFailure(errorType: ErrorType) = post { fail("음성 인식을 준비하지 못했어요.") }
                })
            }
        } catch (_: Exception) { fail("이 기기에서 음성 인식을 시작하지 못했어요.") }
    }
    private fun post(action: () -> Unit) { main.post { if (!closed) action() } }
    private fun finish(text: String?) { if (closed) return; cancel(); callback.finished(text) }
    private fun fail(reason: String) { if (closed) return; cancel(); callback.failed(reason) }
    override fun cancel() {
        if (closed) return
        closed = true
        main.removeCallbacksAndMessages(null)
        sdk?.let { speech ->
            runCatching { speech.removeListener(events) }
            runCatching { speech.stop() }
            runCatching { speech.release() }
        }
        sdk = null
    }
}
