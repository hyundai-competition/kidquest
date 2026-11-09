// 아이 답 듣기를 맡는다. 부모가 동의해야 듣고, 답을 얼마나 기다릴지 정한다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Command

internal class Listening {
    /** 부모가 동의해야 듣는다. 기본은 듣지 않는다. 이번 이동이 끝나도 부모 설정이니 남겨 둔다. */
    var allowed = false

    /** 지금 듣는 중이다. */
    var active = false
        private set

    /** 질문을 다 말했다. 부모가 동의했으면 듣기 시작한다. */
    fun start(answers: List<String>): List<Command> {
        active = allowed
        return if (active) listOf(Command.Listen(answers, ANSWER_SECONDS)) else emptyList()
    }

    fun stop() {
        active = false
    }

    /** 듣지 않으면 8초 뒤에 넘어간다. 들을 때는 음성이 답을 안 줘도 임무가 멈추지 않게 조금 더 기다린다. */
    fun timedOut(waitedMillis: Long): Boolean {
        val limitSeconds = if (active) ANSWER_SECONDS + LISTEN_GRACE_SECONDS else ANSWER_SECONDS
        return waitedMillis >= limitSeconds * 1_000L
    }

    private companion object {
        const val ANSWER_SECONDS = 8
        const val LISTEN_GRACE_SECONDS = 2
    }
}
