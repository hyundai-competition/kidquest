// 생성한 BGM의 실제 재생·중단과 영어 Locale.US ON_DEVICE TTS 정상 완료를 확인한다.
package com.kidquest.app.voice

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.CopyOnWriteArrayList

class MusicEnglishPlaybackTest {
    private val test = InstrumentationRegistry.getInstrumentation()
    @Test fun englishOnDeviceCompletesWithoutFallback() {
        val done = CountDownLatch(1)
        val errors = CopyOnWriteArrayList<String>()
        lateinit var speech: PleosSpeechPlayback
        test.runOnMainSync {
            speech = PleosSpeechPlayback(test.targetContext, "Welcome to Vroomie. Our treasure hunt begins.", object : PlaybackCallback {
                override fun completed() { done.countDown() }
                override fun failed(reason: String) { errors += reason; done.countDown() }
                override fun interrupted() { errors += "interrupted"; done.countDown() }
            }, Locale.US)
            speech.start()
        }
        try {
            assertTrue("English TTS timeout", done.await(25, TimeUnit.SECONDS))
            assertTrue("English TTS errors: $errors", errors.isEmpty())
        } finally { test.runOnMainSync { speech.cancel() } }
    }
    @Test fun originalLoopStartsOnlyAfterQuietDelayAndStopsOnGate() {
        lateinit var music: QuietMusic
        test.runOnMainSync { music = QuietMusic(test.targetContext) { true }; music.allow(true) }
        try {
            SystemClock.sleep(500)
            test.runOnMainSync { assertFalse(music.isPlaying) }
            val end = SystemClock.elapsedRealtime() + 6000
            var playing = false
            while (!playing && SystemClock.elapsedRealtime() < end) {
                SystemClock.sleep(100)
                test.runOnMainSync { playing = music.isPlaying }
            }
            assertTrue("BGM did not start", playing)
            test.runOnMainSync { music.allow(false); assertFalse(music.isPlaying) }
            test.runOnMainSync { music.allow(true); music.allow(false) }
            SystemClock.sleep(1700)
            test.runOnMainSync { assertFalse(music.isPlaying) }
        } finally { test.runOnMainSync { music.release() } }
    }
}
