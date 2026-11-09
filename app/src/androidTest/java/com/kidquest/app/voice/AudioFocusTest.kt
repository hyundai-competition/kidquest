// 실제 Android focus 상실·복귀와 인식 대역의 취소를 본다. 내비·실발화 검증과 구분한다.
package com.kidquest.app.voice

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class AudioFocusTest {
    @Test fun androidDuckRequestPausesAndReturnsFocus() {
        val test = InstrumentationRegistry.getInstrumentation()
        val changes = LinkedBlockingQueue<Boolean>()
        val source = AudioFocusSource(test.targetContext)
        val manager = test.targetContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val rival = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).build())
            .build()
        try {
            test.runOnMainSync { source.start { changes.offer(it) }; assertTrue(source.request()) }
            assertEquals(true, changes.poll(3, TimeUnit.SECONDS))
            test.runOnMainSync { assertEquals(AudioManager.AUDIOFOCUS_REQUEST_GRANTED, manager.requestAudioFocus(rival)) }
            assertEquals(false, changes.poll(3, TimeUnit.SECONDS))
            test.runOnMainSync { assertFalse(source.request()); assertFalse(source.request(userInitiated = true)) }
            test.runOnMainSync { manager.abandonAudioFocusRequest(rival) }
            assertEquals(true, changes.poll(3, TimeUnit.SECONDS))
        } finally { test.runOnMainSync { manager.abandonAudioFocusRequest(rival); source.stop() } }
    }

    @Test fun focusLossCancelsListeningAndIgnoresLateAnswer() {
        val test = InstrumentationRegistry.getInstrumentation()
        val focus = FakeFocus()
        val events = mutableListOf<AppEvent>()
        lateinit var output: VoiceOutput
        lateinit var callback: RecognitionCallback
        var cancelled = false
        test.runOnMainSync {
            output = VoiceOutput(test.targetContext, focus = focus, recognitionFactory = RecognitionFactory {
                callback = it
                object : RecognitionJob {
                    override fun start() { callback.ready() }
                    override fun cancel() { cancelled = true }
                }
            })
            output.start(EventListener { events += it })
            output.setListeningAllowed(true)
            output.handle(Command.Listen(emptyList(), 8))
            focus.change(false)
            assertTrue(cancelled)
            callback.finished("1번")
            assertTrue(events.none { it is AppEvent.AnswerHeard })
            assertTrue(events.contains(AppEvent.AudioFocusChanged(false)))
            focus.change(true)
            assertTrue(events.contains(AppEvent.AudioFocusChanged(true)))
            output.stop()
            assertTrue(focus.stopped)
        }
    }
    private class FakeFocus : AudioFocusPort {
        var stopped = false
        private var granted = true
        lateinit var callback: (Boolean) -> Unit
        override fun start(changed: (Boolean) -> Unit) { callback = changed }
        override fun request(userInitiated: Boolean) = granted
        fun change(value: Boolean) { granted = value; callback(value) }
        override fun stop() { stopped = true }
    }
}
