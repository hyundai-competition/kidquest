// 엔진 대사를 지금 소리로 낼지 정한다. 부모가 껐으면 말하지 않고, 내비 안내 중이면 마지막 대사 하나만 미뤘다가 안내가 끝나면 말한다.
// 벨트 안내는 한 이동에 세 번까지만 한다(F09). 멈춘 이유는 엔진이 정하고, 여기서는 그 목록을 읽기만 한다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Command

internal class SpeechGate(private val pauseReasons: Set<PauseReason>) {
    /** 내비 안내 때문에 못 한 대사. 소리를 다시 낼 수 있을 때 말한다. */
    private var held: Command.Say? = null
    private var beltGuides = 0

    /** 부모가 껐으면 말하지 않고, 내비 안내 중이면 끝날 때까지 미룬다. 미룬 대사는 마지막 하나만 남긴다. */
    fun speak(line: Command.Say): List<Command> = when {
        PauseReason.PARENT in pauseReasons -> emptyList()
        PauseReason.AUDIO_FOCUS in pauseReasons -> {
            held = line
            emptyList()
        }
        else -> listOf(line)
    }

    /** 내비 안내가 끝났다. 미뤄 둔 대사가 있으면 말한다. */
    fun release(): List<Command> {
        val line = held ?: return emptyList()
        held = null
        return speak(line)
    }

    fun beltGuide(line: Command.Say): List<Command> {
        if (beltGuides >= MAX_BELT_GUIDES) return emptyList()
        beltGuides++
        return speak(line)
    }

    /** 벨트를 다시 맸으면 미뤄 둔 「벨트를 매면」 안내는 버린다. */
    fun dropBeltGuide() {
        if (held == Lines.BELT_OFF) held = null
    }

    /** 다시 하기. 미룬 대사와 벨트 안내 횟수를 지운다. */
    fun reset() {
        held = null
        beltGuides = 0
    }

    private companion object {
        const val MAX_BELT_GUIDES = 3
    }
}
