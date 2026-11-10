// BGM이 음성·마이크·다른 안내와 겹치거나 음소거·휴식 중 재생되지 않는지 확인한다.
package com.kidquest.app.voice

import org.junit.Assert.*
import org.junit.Test

class QuietMusicStateTest {
    @Test fun onlyQuietActiveStoryAllowsMusic() {
        val idle = QuietMusicState(flying = true)
        assertTrue(idle.shouldPlay)
        listOf(idle.copy(flying = false), idle.copy(enabled = false), idle.copy(focus = false),
            idle.copy(speech = true), idle.copy(effects = true), idle.copy(listening = true),
            idle.copy(instrument = true), idle.copy(muted = true)).forEach { assertFalse(it.shouldPlay) }
    }
}
