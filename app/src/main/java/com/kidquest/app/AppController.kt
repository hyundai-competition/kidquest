// 앱 전체를 잇는다. 사건을 엔진에 넣고, 나온 명령을 화면과 음성에 나눠 준다.
// 사건은 메인 스레드에서 넣는다고 가정한다. 차 신호가 다른 스레드에서 오면 car에서 옮겨 준다.
package com.kidquest.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kidquest.app.car.CarSignalCollector
import com.kidquest.app.replay.ReplaySource
import com.kidquest.app.voice.VoiceOutput
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.CommandHandler
import com.kidquest.core.contract.EventListener
import com.kidquest.core.contract.EventSource
import com.kidquest.core.contract.Screen
import com.kidquest.core.engine.MissionEngine

class AppController(
    private val engine: MissionEngine = MissionEngine(),
    private val car: EventSource = CarSignalCollector(),
    private val voice: CommandHandler = VoiceOutput(),
    val replay: ReplaySource = ReplaySource(),
) {
    var screen by mutableStateOf(Screen.START)
        private set

    private val listener = EventListener { dispatch(it) }

    fun start() {
        car.start(listener)
        replay.start(listener)
    }

    fun stop() {
        car.stop()
        replay.stop()
    }

    fun dispatch(event: AppEvent) {
        engine.onEvent(event).forEach { command ->
            if (command is Command.ShowScreen) screen = command.screen else voice.handle(command)
        }
    }
}
