// 실제 음성 완료가 엔진으로 돌아가 이륙 다음 임무 브리핑을 여는지 확인한다. 차량 입력은 시험용이다.
package com.kidquest.app.voice

import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.app.AppController
import com.kidquest.app.episode.EpisodeFile
import com.kidquest.core.contract.*
import com.kidquest.core.engine.MissionEngine
import com.kidquest.core.episode.EpisodeResult
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class VoiceEngineConnectionTest {
    @Test fun actualTakeoffCompletionStartsMissionBriefing() {
        val test = InstrumentationRegistry.getInstrumentation()
        val briefing = CountDownLatch(1)
        val reports = CopyOnWriteArrayList<String>()
        lateinit var controller: AppController
        test.runOnMainSync {
            val episode = (EpisodeFile.load(test.targetContext, EpisodeFile.DEMO) as EpisodeResult.Ok).episode
            val voice = VoiceOutput(test.targetContext) {
                reports += it
                if (it == "재생 중: rain_map/briefing") briefing.countDown()
            }
            val car = object : EventSource {
                override fun start(listener: EventListener) = Unit
                override fun stop() = Unit
            }
            controller = AppController(test.targetContext, MissionEngine(episode), car, voice)
            controller.start()
            controller.dispatch(AppEvent.StartPressed)
            controller.dispatch(AppEvent.SeatOccupied(RearSeat.LEFT, true))
            controller.dispatch(AppEvent.BeltBuckled(RearSeat.LEFT, true))
            controller.dispatch(AppEvent.GearChanged(Gear.D))
        }
        try {
            val completed = briefing.await(40, TimeUnit.SECONDS)
            assertTrue("실제 이륙 음성 뒤 브리핑 없음: $reports", completed)
            assertTrue(reports.contains("재생 완료: take_off"))
            assertFalse(reports.any { it.contains("녹음 대체") || it.contains("재생 오류") })
            test.runOnMainSync { assertEquals(Screen.FLYING, controller.screen) }
        } finally { test.runOnMainSync { controller.stop() } }
    }
}
