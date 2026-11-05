// TTS, STT, 오디오 포커스를 본다. 기어 D에서 소리가 나는지, 내비 안내와 어떻게 겹치는지가 핵심이다.
// 시험 앱이라 인식한 말을 화면에 보여 준다. 제출 앱은 남기지 않는다. 어른 목소리로만 시험한다.
package com.kidquest.signalcheck

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.EventListener
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import java.util.Locale
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInit
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInit

class VoiceProbe(context: Context, private val log: SignalLog) {
    private val app = context.applicationContext
    private val tts = TextToSpeech(app, TtsMode.ON_DEVICE)
    private val stt = SpeechToText(app, SttMode.ON_DEVICE)
    private val audio = app.getSystemService(AudioManager::class.java)
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setOnAudioFocusChangeListener { change -> log.line("오디오 포커스", focusName(change)) }
        .build()
    private var zoneId = 0

    private val ttsEvents = object : EventListener {
        override fun onReady() = log.line("TTS", "준비")
        override fun onStart() = log.line("TTS", "말하기 시작")
        override fun onDone() = log.line("TTS", "말하기 끝")
        override fun onStop() = log.line("TTS", "멈춤")
        override fun onError(errorType: TtsError) = log.line("TTS", "오류 $errorType")
        override fun onUpdatedRms(rms: Double) = Unit
    }

    private val sttEvents = object : ResultListener {
        override fun onReady() = log.line("STT", "준비")
        override fun onStartedRecognition() = log.line("STT", "듣기 시작")
        override fun onUpdated(stt: String, completed: Boolean) {
            if (completed) log.line("STT", "결과 : $stt")
        }
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onEndedRecognition() = log.line("STT", "듣기 끝")
        override fun onError(errorType: SttError) = log.line("STT", "오류 $errorType")
    }

    fun start() {
        log.attempt("TTS 초기화") {
            tts.initialize(object : TtsInit {
                override fun onSuccess() = log.line("TTS 초기화", "성공")
                override fun onFailure(errorType: TtsError) = log.line("TTS 초기화", "실패 $errorType")
            })
            tts.addEventListener(ttsEvents)
            tts.setLocale(Locale.KOREA)
        }
        log.attempt("STT 초기화") {
            stt.initialize(object : SttInit {
                override fun onSuccess() = log.line("STT 초기화", "성공")
                override fun onFailure(errorType: SttError) = log.line("STT 초기화", "실패 $errorType")
            })
            stt.addListener(sttEvents)
            stt.setLocale(Locale.KOREA)
        }
    }

    fun speak() = log.attempt("TTS 말하기") {
        tts.speak(TEST_LINE, true)
    }

    fun stopSpeaking() = log.attempt("TTS 멈춤") { tts.stop() }

    fun listen() = log.attempt("STT 듣기") { stt.request() }

    fun requestFocus() = log.line("오디오 포커스", "요청 결과 ${audio.requestAudioFocus(focus)}")

    fun abandonFocus() = log.line("오디오 포커스", "반납 결과 ${audio.abandonAudioFocusRequest(focus)}")

    /** 값과 좌석의 대응을 문서에서 못 찾았다. 0부터 올려 가며 소리 나는 곳을 귀로 확인한다. */
    fun nextZone() = log.attempt("소리 구역") {
        zoneId = (zoneId + 1) % ZONE_COUNT
        tts.setAudioZoneId(zoneId)
        log.line("소리 구역", "zoneId=$zoneId")
    }

    fun stop() {
        log.attempt("음성 해제") {
            audio.abandonAudioFocusRequest(focus)
            tts.removeEventListener(ttsEvents)
            tts.release()
            stt.removeListener(sttEvents)
            stt.release()
        }
    }

    private fun focusName(change: Int): String = when (change) {
        AudioManager.AUDIOFOCUS_GAIN -> "받음"
        AudioManager.AUDIOFOCUS_LOSS -> "잃음"
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> "잠깐 잃음"
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> "줄이라고 함"
        else -> change.toString()
    }

    private companion object {
        const val TEST_LINE = "선장님, 발사 준비 끝났어요. 소리 시험 중입니다."
        const val ZONE_COUNT = 4
    }
}
