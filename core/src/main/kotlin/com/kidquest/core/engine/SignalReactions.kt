// 임무와 상관없이 차 신호에 바로 내는 짧은 소리. 깜빡이를 켜면 추진기 소리를 내는 식이다.
// 같은 소리는 에피소드 파일에 적힌 시간(cooldownSeconds) 안에 다시 내지 않는다.
// 반응 대사(line)는 아직 쓰지 않는다. 임무 대사와 겹치거나 끊을 수 있어서, 대사를 넣을 때 순서를 정한다.
// 에피소드는 부를 때마다 받아 온다. 시작 전에 언어를 바꾸면 그 언어 파일의 반응을 쓴다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Episode

internal class SignalReactions(private val episode: () -> Episode?) {
    private val lastPlayedAt = mutableMapOf<String, Long>()

    /** 시계 값을 아직 못 받았으면 간격을 잴 수 없으니 소리만 낸다. */
    fun play(signal: String, nowMillis: Long?): List<Command> {
        val reaction = episode()?.reactions?.firstOrNull { it.signal == signal } ?: return emptyList()
        val sound = reaction.soundId ?: return emptyList()
        val last = lastPlayedAt[signal]
        if (last != null && nowMillis != null && nowMillis - last < reaction.cooldownSeconds * 1_000L) return emptyList()
        if (nowMillis != null) lastPlayedAt[signal] = nowMillis
        return listOf(Command.PlaySound(sound))
    }

    fun reset() = lastPlayedAt.clear()
}
