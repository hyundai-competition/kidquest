// 음성 완료·취소·지연 콜백·녹음 대체를 가짜 재생기로 시험한다. 실제 오디오 없이 수명 오류를 재현한다.
package com.kidquest.app.voice

import com.kidquest.core.contract.Command
import org.junit.Assert.*
import org.junit.Test

class SpeechQueueTest {
    private class Job(val line: Command.Say, val callback: PlaybackCallback) : SpeechPlayback {
        var started = false
        var cancelled = false
        var releasedNormally = false
        override fun start() { started = true }
        override fun cancel() { cancelled = true }
        override fun finish() { releasedNormally = true }
    }
    private val tts = mutableListOf<Job>()
    private val recordings = mutableListOf<Job>()
    private val finished = mutableListOf<String>()
    private val reports = mutableListOf<String>()
    private fun queue(withRecording: Boolean = true) = SpeechQueue(
        SpeechFactory { line, callback -> Job(line, callback).also(tts::add) },
        SpeechFactory { line, callback -> if (withRecording) Job(line, callback).also(recordings::add) else null },
        finished::add, reports::add,
    )
    private fun line(id: String = "a") = Command.Say(id, "처음부터 읽을 문장")

    @Test fun onlyCompletedLineFinishesOnceAndQueuedLinesPlayInOrder() {
        val queue = queue()
        queue.say(line())
        queue.say(line("b"))
        assertEquals(1, tts.size)
        assertTrue(finished.isEmpty())
        tts[0].callback.completed()
        tts[0].callback.completed()
        assertEquals(listOf("a"), finished)
        assertTrue(tts[0].releasedNormally)
        assertFalse(tts[0].cancelled)
        assertEquals(2, tts.size)
        tts[1].callback.completed()
        assertEquals(listOf("a", "b"), finished)
    }

    @Test fun stopDropsQueuedLinesAndRejectsAllOldCallbacksWhenSameIdRestarts() {
        val queue = queue()
        queue.say(line())
        queue.say(line("queued"))
        queue.stop()
        assertTrue(tts[0].cancelled)
        queue.say(line())
        assertEquals(line(), tts[1].line)
        tts[0].callback.completed()
        tts[0].callback.failed("늦은 오류")
        tts[0].callback.interrupted()
        assertTrue(finished.isEmpty())
        assertTrue(recordings.isEmpty())
        tts[1].callback.completed()
        assertEquals(listOf("a"), finished)
        assertEquals(2, tts.size)
    }

    @Test fun failureUsesRecordingAndOnlyItsCompletionFinishes() {
        val queue = queue()
        queue.say(line())
        tts[0].callback.failed("TTS 실패")
        assertTrue(tts[0].cancelled)
        assertTrue(recordings.single().started)
        tts[0].callback.completed()
        assertTrue(finished.isEmpty())
        recordings[0].callback.completed()
        recordings[0].callback.completed()
        assertEquals(listOf("a"), finished)
    }

    @Test fun stopDuringFallbackDoesNotFinish() {
        val queue = queue()
        queue.say(line())
        tts[0].callback.failed("TTS 실패")
        queue.stop()
        recordings[0].callback.completed()
        assertTrue(recordings[0].cancelled)
        assertTrue(finished.isEmpty())
    }

    @Test fun missingRecordingDoesNotPretendSuccessAndReportsFailure() {
        val queue = queue(false)
        queue.say(line())
        tts[0].callback.failed("TTS 실패")
        assertTrue(finished.isEmpty())
        assertTrue(reports.last().contains("음성 재생 불가"))
    }

    @Test fun externalInterruptionDoesNotAutomaticallyStartRecording() {
        val queue = queue()
        queue.say(line())
        tts[0].callback.interrupted()
        assertTrue(tts[0].cancelled)
        assertTrue(recordings.isEmpty())
        assertTrue(finished.isEmpty())
    }
}
