// 엔진과 바깥을 잇는 입구와 출구. 실제 차 신호와 재생 모드를 같은 입구로 넣는다.
package com.kidquest.core.contract

fun interface EventListener {
    fun onEvent(event: AppEvent)
}

/** 사건을 만드는 쪽. 차 신호 수집과 재생 모드가 구현한다. */
interface EventSource {
    fun start(listener: EventListener)
    fun stop()
}

/** 명령을 받아 실행하는 쪽. */
fun interface CommandHandler {
    fun handle(command: Command)
}

/** 음성. 명령을 실행하고, 대사 끝남과 아이 답과 오디오 포커스를 사건으로 돌려준다. */
interface VoicePort : CommandHandler, EventSource
