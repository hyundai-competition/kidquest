// 인식 대역으로 동의, 취소, 최종 응답 1회와 늦은 응답 무시를 검증한다. 실제 마이크 성공과 구분한다.
package com.kidquest.app.voice

import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Test

class RecognitionLifecycleTest {
    @Test fun consentCancelAndLateCallbackAreSafe() {
        val test = InstrumentationRegistry.getInstrumentation()
        val jobs = mutableListOf<FakeRecognition>()
        val answers = mutableListOf<String?>()
        lateinit var output: VoiceOutput
        test.runOnMainSync {
            output = VoiceOutput(test.targetContext, recognitionFactory = RecognitionFactory { callback ->
                FakeRecognition(callback).also { jobs += it }
            })
            output.start(EventListener {})
            output.listenScreen { answers += it }
            assertTrue(jobs.isEmpty())
            assertEquals(listOf<String?>(null), answers)
            answers.clear()
            output.setListeningAllowed(true)
            output.listenScreen { answers += it }
            val first = jobs.last()
            output.handle(Command.StopAudio)
            assertTrue(first.cancelled)
            first.callback.finished("1번")
            assertTrue(answers.isEmpty())
            output.listenScreen { answers += it }
            val second = jobs.last()
            second.callback.finished("2번")
            second.callback.finished("3번")
            assertEquals(listOf("2번"), answers)
            output.listenScreen { answers += it }
            output.setListeningAllowed(false)
            assertTrue(jobs.last().cancelled)
            jobs.last().callback.finished("3번")
            assertEquals(listOf("2번"), answers)
            output.stop()
        }
    }
    private class FakeRecognition(val callback: RecognitionCallback) : RecognitionJob {
        var cancelled = false
        override fun start() { callback.listening() }
        override fun cancel() { cancelled = true }
    }
}
