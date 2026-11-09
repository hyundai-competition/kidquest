// 엔진 명령을 실제 ON_DEVICE TTS와 효과음으로 실행한다. 녹음 대체와 완료·중단을 구분한다.
// 말하기와 듣기를 겹치지 않게 하고 동의한 경우에만 한국어 ON_DEVICE 인식을 시작한다.
package com.kidquest.app.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.EventListener
import com.kidquest.core.contract.VoicePort

class VoiceOutput(
    context: Context,
    private val ui: (VoiceUiState) -> Unit = {},
    private val recognitionFactory: RecognitionFactory? = null,
    focus: AudioFocusPort? = null,
    private val report: (String) -> Unit = {},
) : VoicePort {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val focus = focus ?: AudioFocusSource(app)
    private var hasFocus = true
    private val recordings = runCatching { RecordingCatalog(app) }.getOrNull()
    private var listener: EventListener? = null
    private var instrument: SpeechPlayback? = null
    private var screenSerial = 0L
    private var allowed = false
    private var recognition: RecognitionJob? = null
    private var generation = 0L
    private var screenDone: Pair<String, () -> Unit>? = null
    private var voiceUi = VoiceUiState()
    private fun show(state: VoiceUiState) { voiceUi = state; ui(state) }
    private val effects = EffectPlayer(app, ::status)
    private val speech = SpeechQueue(
        tts = SpeechFactory { line, callback -> PleosSpeechPlayback(app, line.text, callback) },
        recordings = SpeechFactory { line, callback -> recordings?.pathFor(line)?.let { AssetPlayback(app, it, callback) } },
        finished = { id ->
            show(voiceUi.copy(phase = VoicePhase.IDLE, hint = "브루미의 이야기가 끝났어요"))
            if (!id.startsWith("screen_mission_")) listener?.onEvent(AppEvent.LineFinished(id))
            else screenDone?.takeIf { it.first == id }?.second?.let { action -> screenDone = null; action() }
        },
        report = ::status,
        unavailable = { id ->
            if (!id.startsWith("screen_mission_")) listener?.onEvent(AppEvent.LineFinished(id))
            else screenDone = null // 듣지 못한 보너스 질문 뒤 마이크를 자동으로 열지 않는다.
        },
        onSpeaking = { show(VoiceUiState(VoicePhase.SPEAKING, it.text, "브루미가 이야기하고 있어요 · 마이크는 꺼져 있어요")) },
    )

    override fun start(listener: EventListener) {
        this.listener = listener
        focus.start { available -> onMain {
            if (listener !== this.listener || available == hasFocus) return@onMain
            hasFocus = available
            if (!available) {
                stopAudio()
                show(VoiceUiState(VoicePhase.IDLE, hint = "다른 안내가 끝날 때까지 기다려요"))
            }
            listener.onEvent(AppEvent.AudioFocusChanged(available))
        } }
    }
    fun retryAudioFocus() = onMain { focus.request(userInitiated = true) }

    override fun handle(command: Command) = onMain {
        if (listener == null) return@onMain
        when (command) {
            is Command.Say -> { if (focus.request()) { cancelListening(); speech.say(command) } }
            is Command.PlaySound -> if (focus.request()) effects.play(command.soundId)
            Command.StopAudio -> stopAudio()
            is Command.Listen -> listen(command.timeoutSeconds) { listener?.onEvent(AppEvent.AnswerHeard(it)) }
            else -> Unit
        }
    }

    // 체험 대사도 같은 재생기를 사용해 SDK 인스턴스 간 중단 경쟁을 피한다.
    fun readScreen(text: String, finished: (() -> Unit)? = null) = onMain {
        stopAudio()
        if (!focus.request()) return@onMain
        val id = "screen_mission_${++screenSerial}"
        val token = generation
        screenDone = finished?.let { action -> id to {
            show(VoiceUiState(VoicePhase.PREPARING, hint = "보기를 다 읽었어요. 곧 마이크를 켤게요"))
            main.postDelayed({ if (token == generation && listener != null) action() }, 450)
        } }
        show(VoiceUiState(VoicePhase.PREPARING, text, "브루미가 이야기를 준비하고 있어요"))
        speech.say(Command.Say(id, text))
    }
    fun playInstrument(name: String) = onMain {
        stopAudio()
        if (!focus.request()) return@onMain
        instrument = AssetPlayback(app, "audio/instruments/${if (name == "편종") "metal_preview" else "stone_preview"}.wav", object : PlaybackCallback {
            override fun completed() { instrument?.finish(); instrument = null }
            override fun failed(reason: String) { status(reason); instrument?.cancel(); instrument = null }
            override fun interrupted() { instrument?.cancel(); instrument = null }
        }).also { it.start() }
    }

    fun setListeningAllowed(value: Boolean) = onMain { allowed = value; if (!value) cancelListening() }
    fun listenScreen(result: (String?) -> Unit) = onMain { listen(8, numbered = true, result = result) }
    private fun listen(seconds: Int, numbered: Boolean = false, result: (String?) -> Unit) {
        cancelListening()
        speech.stop()
        effects.stop()
        instrument?.cancel(); instrument = null
        screenDone = null
        if (!allowed) {
            show(VoiceUiState(VoicePhase.IDLE, hint = "음성 답변은 출발 전에 동의한 경우에만 사용할 수 있어요"))
            result(null)
            return
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(app, "pleos.car.permission.STT_SERVICE") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            show(VoiceUiState(VoicePhase.ERROR, hint = "음성 인식 권한을 확인해 주세요"))
            result(null)
            return
        }
        if (!focus.request()) return
        val token = ++generation
        show(VoiceUiState(VoicePhase.PREPARING, hint = "마이크를 준비하고 있어요"))
        val callback = object : RecognitionCallback {
            override fun ready() = listening()
            override fun listening() { if (token == generation) show(VoiceUiState(VoicePhase.LISTENING, hint = if (numbered) "지금 말해 주세요 · 1번, 2번, 3번 또는 건너뛰기" else "지금 브루미의 질문에 대답해 주세요")) }
            override fun finished(text: String?) {
                if (token != generation) return
                generation++
                recognition = null
                show(VoiceUiState(if (text == null) VoicePhase.IDLE else VoicePhase.PROCESSING, hint = if (text == null) "잘 듣지 못했어요. 다시 말하거나 쉬어도 좋아요" else "답을 확인하고 있어요"))
                result(text)
            }
            override fun failed(reason: String) {
                if (token != generation) return
                generation++; recognition = null
                show(VoiceUiState(VoicePhase.ERROR, hint = reason))
                result(null)
            }
        }
        val job = recognitionFactory?.create(callback) ?: PleosRecognition(app, seconds, callback)
        recognition = job
        job.start()
    }

    override fun stop() = onMain {
        listener = null
        focus.stop()
        stopAudio()
        main.removeCallbacksAndMessages(null)
    }

    private fun stopAudio() { screenDone = null; cancelListening(); speech.stop(); effects.stop(); instrument?.cancel(); instrument = null }
    private fun cancelListening() {
        generation++
        recognition?.cancel(); recognition = null
        if (voiceUi.phase == VoicePhase.LISTENING || voiceUi.phase == VoicePhase.PREPARING) show(voiceUi.copy(phase = VoicePhase.IDLE, hint = "마이크를 껐어요"))
    }
    private fun status(message: String) {
        Log.i("KidQuestVoice", message); report(message)
        if (message.startsWith("재생 오류") || message.startsWith("음성 재생 불가")) show(voiceUi.copy(phase = VoicePhase.ERROR, hint = "소리를 재생하지 못했어요. 화면 설명으로도 진행할 수 있어요"))
        else if (message == "음성 중단") show(voiceUi.copy(phase = VoicePhase.IDLE, hint = "음성을 멈췄어요"))
    }
    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else main.post { action() }
    }
}
