// 시연 선택·일시정지·진행 예약과 타이머를 ViewModel 수명 동안 보관한다.
package com.kidquest.app.replay

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kidquest.app.AppController
import com.kidquest.core.contract.*

class ShowcaseSession(private val controller: AppController, private val interrupted: () -> Unit) {
    var selected by mutableStateOf(false)
        private set
    var notice by mutableStateOf("")
        private set
    var finished by mutableStateOf(false)
        private set
    var nextQueued by mutableStateOf(false)
        private set
    val active get() = controller.simulationMode
    val paused get() = controller.vehicle.parentStopped || controller.parentStopRequested
    private val main = Handler(Looper.getMainLooper())
    private var runner: ShowcaseRunner? = null
    init { controller.simulationInterrupted = {
        interrupted()
        notice = "실제 차량의 주행 상태가 바뀌어 시연을 종료했어요. 주차 후 다시 시작해 주세요."
    } }
    private val ticker = object : Runnable {
        override fun run() {
            val current = runner ?: return
            val snapshot = ShowcaseSnapshot(controller.screen, controller.presentation.mission?.id,
                controller.presentation.stage, controller.lastFinishedLine, controller.presentation.answerReady,
                controller.items.size, controller.parentStopRequested,
                controller.voiceUi.phase in setOf(com.kidquest.app.voice.VoicePhase.SPEAKING, com.kidquest.app.voice.VoicePhase.PREPARING))
            current.tick(snapshot)
            finished = current.finished
            nextQueued = current.advanceRequested
            if (!finished) main.postDelayed(this, 1000)
        }
    }
    fun select(value: Boolean) { selected = value; notice = "" }
    fun start(missions: List<Mission>, places: List<Place>, begin: () -> Unit) {
        if (active) return
        if (!selected) { begin(); return }
        if (!controller.setSimulationMode(true)) {
            notice = "시연은 실제 차량이 주차(P) 중일 때 사용할 수 있어요."
            return
        }
        notice = ""
        finished = false
        nextQueued = false
        runner = ShowcaseRunner(missions, places, SystemClock.elapsedRealtime(), controller.replay::send)
        begin()
        main.postDelayed(ticker, 1000)
    }
    fun next() { if (!active || paused || finished) return; runner?.next(); nextQueued = runner?.advanceRequested == true }
    fun togglePause() {
        if (!active || finished) return
        if (paused) controller.resumeAudio() else controller.dispatch(AppEvent.ParentStopChanged(true))
    }
    fun stop() {
        main.removeCallbacksAndMessages(null)
        runner = null
        controller.setSimulationMode(false)
        selected = false
        finished = false
        nextQueued = false
        notice = ""
    }
}
