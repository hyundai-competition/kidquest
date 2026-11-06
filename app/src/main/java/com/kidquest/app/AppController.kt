// 앱 전체를 잇는다. 사건을 엔진에 넣고, 나온 명령을 화면과 음성에 나눠 준다.
// 사건은 어느 스레드에서 와도 메인 스레드의 줄에 넣어 하나씩 처리한다. 그래서 엔진은 스레드를 신경 쓰지 않는다.
package com.kidquest.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kidquest.app.screen.model.VehicleUiState
import com.kidquest.app.car.CarSignalCollector
import com.kidquest.app.replay.ReplaySource
import com.kidquest.app.voice.VoiceOutput
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
    private val voice: VoicePort = VoiceOutput(),
    val replay: ReplaySource = ReplaySource(),
) {
    var vehicle by mutableStateOf(VehicleUiState())
        private set

    var screen by mutableStateOf(Screen.START)
        private set
    var status by mutableStateOf<String?>(null)
        private set
    var background by mutableStateOf(Background.NORMAL)
        private set
    var replayUsed by mutableStateOf(false)
        private set
    val items = mutableStateListOf<Command.AwardItem>()

    private val main = Handler(Looper.getMainLooper())
    private val listener = EventListener { dispatch(it) }
    private val ticker = object : Runnable {
        override fun run() {
            dispatch(AppEvent.Tick(SystemClock.elapsedRealtime()))
            main.postDelayed(this, TICK_MILLIS)
        }
    }

    fun start() {
        car.start(listener)
        voice.start(listener)
        replay.start(EventListener { event ->
            main.post {
                replayUsed = true
                dispatch(event)
            }
        })
        main.postDelayed(ticker, TICK_MILLIS)
    }

    fun stop() {
        main.removeCallbacksAndMessages(null)
        car.stop()
        voice.stop()
        replay.stop()
    }

    fun dispatch(event: AppEvent) {
        main.post {
            vehicle = vehicle.accept(event)
            engine.onEvent(event).forEach(::perform)
        }
    }

    private fun perform(command: Command) {
        when (command) {
            is Command.ShowScreen -> {
                screen = command.screen
                status = command.status
                if (command.screen == Screen.START) items.clear()
            }
            is Command.AwardItem -> items += command
            is Command.SetBackground -> background = command.background
            else -> voice.handle(command)
        }
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
    }
}
