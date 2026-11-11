// 앱 전체를 잇는다. 사건을 엔진에 넣고, 나온 명령을 화면과 음성에 나눠 준다.
// 사건은 어느 스레드에서 와도 메인 스레드의 줄에 넣어 하나씩 처리한다. 그래서 엔진은 스레드를 신경 쓰지 않는다.
package com.kidquest.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kidquest.app.screen.model.VehicleUiState
import com.kidquest.app.screen.model.EnginePresentation
import com.kidquest.core.contract.Episode
import com.kidquest.app.car.CarSignalCollector
import com.kidquest.app.replay.ReplaySource
import com.kidquest.app.replay.ShowcaseSignals
import com.kidquest.app.voice.VoiceOutput
import com.kidquest.app.voice.VoiceUiState
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Background
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.EventListener
import com.kidquest.core.contract.EventSource
import com.kidquest.core.contract.Screen
import com.kidquest.core.contract.VoicePort
import com.kidquest.core.engine.MissionEngine

class AppController(
    context: Context,
    private val engine: MissionEngine = MissionEngine(),
    private val car: EventSource = CarSignalCollector(context),
    voice: VoicePort? = null,
    val replay: ReplaySource = ReplaySource(),
    episode: Episode? = null,
) {
    val presentation = EnginePresentation { engine.episode ?: episode }
    var musicEnabled by mutableStateOf(true)
        private set
    fun toggleMusic() { musicEnabled = !musicEnabled; (voice as? VoiceOutput)?.enableMusic(musicEnabled) }
    var voiceStatus by mutableStateOf("음성 대기 · 시험용 효과음")
        private set
    var voiceUi by mutableStateOf(VoiceUiState())
        private set
    private val voice: VoicePort = voice ?: VoiceOutput(context, report = { voiceStatus = it }, ui = { voiceUi = it })

    var vehicleChanged: ((VehicleUiState) -> Unit)? = null
    var vehicle by mutableStateOf(VehicleUiState())
        private set

    var screen by mutableStateOf(Screen.START)
        private set
    var status by mutableStateOf<String?>(null)
        private set
    var background by mutableStateOf(Background.NORMAL)
        private set
    var parentStopRequested = false
        private set
    var replayUsed by mutableStateOf(false)
        private set
    var simulationMode by mutableStateOf(false)
        private set
    var lastFinishedLine by mutableStateOf("")
        private set
    var simulationInterrupted: (() -> Unit)? = null
    private val signalGate = ShowcaseSignals(context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_AUTOMOTIVE))
    private var sourceGeneration = 0
    val items = mutableStateListOf<Command.AwardItem>()

    private val main = Handler(Looper.getMainLooper())
    private val listener = EventListener { event ->
        enqueue(event, if (event is AppEvent.LineFinished) sourceGeneration else null)
    }
    private val carListener = EventListener { event ->
        main.post {
            if (signalGate.receive(event)) enqueue(event, sourceGeneration)
            else if (!signalGate.allowed) {
                if (simulationInterrupted != null) simulationInterrupted?.invoke() else setSimulationMode(false)
            }
        }
    }
    private val ticker = object : Runnable {
        override fun run() {
            if (!simulationMode) dispatch(AppEvent.Tick(SystemClock.elapsedRealtime()))
            main.postDelayed(this, TICK_MILLIS)
        }
    }

    fun start() {
        car.start(carListener)
        voice.start(listener)
        replay.start(EventListener { event ->
            val generation = sourceGeneration
            main.post {
                if (generation != sourceGeneration) return@post
                replayUsed = true
                enqueue(event, generation)
            }
        })
        main.postDelayed(ticker, TICK_MILLIS)
    }

    fun stop() {
        Log.i("KQFlow", "stop source=controller")
        main.removeCallbacksAndMessages(null)
        car.stop()
        voice.stop()
        replay.stop()
    }

    fun setSimulationMode(enabled: Boolean): Boolean {
        if (enabled == simulationMode) return true
        if (!signalGate.select(enabled)) return false
        sourceGeneration++ // 이전 입력원의 대기 사건이 새 세션으로 넘어오지 않게 한다.
        simulationMode = enabled
        presentation.reset()
        lastFinishedLine = ""
        engine.onEvent(AppEvent.RestartPressed).forEach(::perform)
        // 엔진 재시작은 탑승·벨트를 유지하므로, 입력원을 바꿀 때는 전부 새 값으로 채운다.
        signalGate.restoreEvents().forEach(::process)
        vehicle = signalGate.snapshot()
        vehicleChanged?.invoke(vehicle)
        replayUsed = enabled
        return true
    }

    fun answerChoice(index: Int) {
        if (screen != Screen.FLYING || !presentation.answerReady) return
        val word = presentation.options.getOrNull(index) ?: return
        presentation.closeAnswer()
        voice.handle(Command.StopAudio)
        dispatch(AppEvent.AnswerSelected(word))
    }
    fun retryAudioFocus() { (voice as? VoiceOutput)?.retryAudioFocus() }
    fun resumeAudio() { retryAudioFocus(); dispatch(AppEvent.ParentStopChanged(false)) }
    fun speakScreen(text: String, finished: (() -> Unit)? = null) { main.post { (voice as? VoiceOutput)?.readScreen(text, finished) } }
    fun listenScreen(result: (String?) -> Unit) { main.post { (voice as? VoiceOutput)?.listenScreen(result) } }
    fun playScreenInstrument(name: String) { main.post { (voice as? VoiceOutput)?.playInstrument(name) } }
    fun stopScreenAudio() { main.post { Log.i("KQFlow", "StopAudio source=screen"); voice.handle(Command.StopAudio) } }

    fun dispatch(event: AppEvent) = enqueue(event, null)
    fun startTrip(events: List<AppEvent>) { events.forEach { enqueue(it, sourceGeneration) } }

    private fun enqueue(event: AppEvent, generation: Int?) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post { enqueue(event, generation) }; return }
        // 화면 전환이 연속돼도 처리 대기 중인 부모 끄기 요청을 내부 퍼즐 멈춤과 혼동하지 않는다.
        if (event is AppEvent.ParentStopChanged) parentStopRequested = event.stopped
        main.post {
            if (generation != null && generation != sourceGeneration) return@post
            process(event)
        }
    }

    private fun process(event: AppEvent) {
        val actual = if (event is AppEvent.AnswerHeard) AppEvent.AnswerHeard(presentation.answer(event.word)) else event
        if (event == AppEvent.RestartPressed) { presentation.reset(); lastFinishedLine = "" }
        if (actual is AppEvent.LineFinished) lastFinishedLine = actual.lineId
        vehicle = vehicle.accept(actual, SystemClock.elapsedRealtime())
        vehicleChanged?.invoke(vehicle)
        if (actual is AppEvent.LineFinished && screen == Screen.FLYING) presentation.lineFinished(actual.lineId)
        if (event is AppEvent.LanguageSelected) (voice as? VoiceOutput)?.language(event.language)
        if (event is AppEvent.ListeningConsentChanged) (voice as? VoiceOutput)?.setListeningAllowed(event.allowed)
        engine.onEvent(actual).forEach { command ->
            if (command == Command.StopAudio) Log.i("KQFlow", "StopAudio source=engine event=${actual.javaClass.simpleName}")
            perform(command)
        }
        (voice as? VoiceOutput)?.tripScreen(screen, vehicle.parentStopped)
    }

    private fun perform(command: Command) {
        when (command) {
            is Command.ShowScreen -> {
                screen = command.screen
                status = command.status
                if (screen != Screen.FLYING) presentation.closeAnswer()
                if (command.screen == Screen.START) { items.clear(); parentStopRequested = false }
            }
            is Command.AwardItem -> if (items.none { it.itemId == command.itemId }) items += command
            is Command.Say -> voice.handle(presentation.say(command))
            is Command.SetBackground -> background = command.background
            else -> voice.handle(command)
        }
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
    }
}
