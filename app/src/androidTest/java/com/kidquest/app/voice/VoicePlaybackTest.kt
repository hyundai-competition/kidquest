// Pleos ON_DEVICE 완료와 중단, 실제 MediaPlayer 효과음 및 시험용 녹음 대체를 기기에서 확인한다.
// 녹음 대체 시험 파일은 테스트 APK에만 들어가는 짧은 합성음이며 실제 대사 녹음이 아니다.
package com.kidquest.app.voice

import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class VoicePlaybackTest {
    private val test = InstrumentationRegistry.getInstrumentation()

    @Test fun onDeviceSpeechCompletesOnce() {
        val finished = CopyOnWriteArrayList<AppEvent>()
        val done = CountDownLatch(1)
        val errors = CopyOnWriteArrayList<String>()
        lateinit var output: VoiceOutput
        test.runOnMainSync {
            output = VoiceOutput(test.targetContext) { errors += it }
            output.start(EventListener { finished += it; done.countDown() })
            output.handle(Command.Say("voice_check", "선장님, 발사 준비 끝났어요."))
        }
        try {
            assertTrue("ON_DEVICE 미완료: $errors", done.await(25, TimeUnit.SECONDS))
            test.waitForIdleSync()
            assertEquals(listOf(AppEvent.LineFinished("voice_check")), finished.toList())
            assertFalse(errors.any { it.contains("녹음 대체") || it.contains("재생 오류") })
        } finally { test.runOnMainSync { output.stop() } }
    }

    @Test fun stopBeforeInitializationAndRestartSameLineDoesNotFinishCancelledRun() {
        val finished = CopyOnWriteArrayList<AppEvent>()
        val done = CountDownLatch(1)
        lateinit var output: VoiceOutput
        test.runOnMainSync {
            output = VoiceOutput(test.targetContext)
            output.start(EventListener { finished += it; done.countDown() })
            output.handle(Command.Say("same", "이 문장은 중단할 거예요."))
            output.handle(Command.PlaySound("meteor"))
            output.handle(Command.StopAudio)
        }
        try {
            assertFalse("중단한 대사를 완료로 처리함", done.await(2, TimeUnit.SECONDS))
            test.runOnMainSync { output.handle(Command.Say("same", "이제 처음부터 다시 읽어요.")) }
            assertTrue(done.await(25, TimeUnit.SECONDS))
            assertEquals(listOf(AppEvent.LineFinished("same")), finished.toList())
        } finally { test.runOnMainSync { output.stop() } }
    }

    @Test fun stopDuringRealSpeechSuppressesCompletion() {
        val started = CountDownLatch(1)
        val done = CountDownLatch(1)
        val reports = CopyOnWriteArrayList<String>()
        lateinit var output: VoiceOutput
        test.runOnMainSync {
            output = VoiceOutput(test.targetContext) {
                reports += it
                if (it == "재생 중: interrupt_check") started.countDown()
            }
            output.start(EventListener { done.countDown() })
            output.handle(Command.Say("interrupt_check", "선장님, 지금은 음성 중단 시험 중이에요. 이 이야기는 끝까지 읽기 전에 멈출 거예요. 다시 시작하면 처음부터 읽어요."))
        }
        try {
            assertTrue("실제 재생 시작 없음: $reports", started.await(20, TimeUnit.SECONDS))
            test.runOnMainSync { output.handle(Command.StopAudio) }
            assertFalse("중단 대사 완료 사건이 발생함", done.await(3, TimeUnit.SECONDS))
        } finally { test.runOnMainSync { output.stop() } }
    }

    @Test fun prerecordedFallbackReallyPlaysAndRejectsStaleText() {
        val done = CountDownLatch(1)
        val result = CopyOnWriteArrayList<String>()
        val failures = CopyOnWriteArrayList<String>()
        lateinit var queue: SpeechQueue
        test.runOnMainSync {
            val catalog = RecordingCatalog(test.context)
            assertNull(catalog.pathFor(Command.Say("fixture", "바뀐 대사")))
            queue = SpeechQueue(
                SpeechFactory { _, callback -> object : SpeechPlayback {
                    override fun start() { callback.failed("시험에서 강제로 TTS 실패") }
                    override fun cancel() = Unit
                } },
                SpeechFactory { line, callback -> catalog.pathFor(line)?.let { AssetPlayback(test.context, it, callback) } },
                { result += it; done.countDown() }, failures::add,
            )
            queue.say(Command.Say("fixture", "시험 파일"))
        }
        try {
            assertTrue("녹음 대체 미완료: $failures", done.await(10, TimeUnit.SECONDS))
            assertEquals(listOf("fixture"), result.toList())
        } finally { test.runOnMainSync { queue.stop() } }
    }

    @Test fun effectFilesPlayToCompletionAndCancelDoesNotComplete() {
        for (id in listOf("meteor", "thruster_left", "thruster_right", "wormhole")) {
            val done = CountDownLatch(1)
            val failures = CopyOnWriteArrayList<String>()
            lateinit var playback: AssetPlayback
            test.runOnMainSync {
                playback = AssetPlayback(test.targetContext, "audio/effects/$id.wav", object : PlaybackCallback {
                    override fun completed() { done.countDown() }
                    override fun failed(reason: String) { failures += reason }
                    override fun interrupted() = Unit
                })
                playback.start()
            }
            try {
                assertTrue("효과음 $id: $failures", done.await(5, TimeUnit.SECONDS))
            } finally { test.runOnMainSync { playback.cancel() } }
        }
        val completed = CountDownLatch(1)
        test.runOnMainSync {
            val playback = AssetPlayback(test.targetContext, "audio/effects/meteor.wav", object : PlaybackCallback {
                override fun completed() { completed.countDown() }
                override fun failed(reason: String) = Unit
                override fun interrupted() = Unit
            })
            playback.start()
            playback.cancel()
        }
        assertFalse(completed.await(2, TimeUnit.SECONDS))
    }
}
