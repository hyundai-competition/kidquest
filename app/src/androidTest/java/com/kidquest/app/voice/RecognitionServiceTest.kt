// 실제 Pleos ON_DEVICE 인식 요청의 준비 통지와 취소를 확인한다. 발화·음향 입력 성공과 구분한다.
package com.kidquest.app.voice

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.CopyOnWriteArrayList

class RecognitionServiceTest {
    @Test fun onDeviceRequestReachesReadyAndCanBeStopped() {
        val test = InstrumentationRegistry.getInstrumentation()
        val ready = CountDownLatch(1)
        val errors = CopyOnWriteArrayList<String>()
        val stages = CopyOnWriteArrayList<String>()
        lateinit var job: PleosRecognition
        test.runOnMainSync {
            job = PleosRecognition(test.targetContext, 3, object : RecognitionCallback {
                override fun ready() { ready.countDown() }
                override fun listening() = Unit
                override fun finished(text: String?) = Unit
                override fun failed(reason: String) { errors += reason; ready.countDown() }
            }, diagnostic = { stages += it })
            job.start()
        }
        try {
            assertTrue("인식 준비 통지 없음: $stages / $errors", ready.await(12, TimeUnit.SECONDS))
            assertTrue("ON_DEVICE 인식 서비스: $stages / $errors", errors.isEmpty())
        } finally { test.runOnMainSync { job.cancel() } }
    }
}
