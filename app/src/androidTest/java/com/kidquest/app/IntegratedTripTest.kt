// 실제 main 에피소드로 가짜 차량·위치 사건과 번호 답변을 넣어 다섯 보물과 도착 결말까지 검증한다.
package com.kidquest.app

import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.app.episode.EpisodeFile
import com.kidquest.app.screen.model.TripCatalog
import com.kidquest.core.contract.*
import com.kidquest.core.engine.MissionEngine
import com.kidquest.core.episode.EpisodeResult
import org.junit.Assert.*
import org.junit.Test

class IntegratedTripTest {
    @Test fun replayEventsAndNumbersReachRealEngineEnding() {
        val test = InstrumentationRegistry.getInstrumentation()
        val episode = (EpisodeFile.load(test.targetContext, EpisodeFile.DEMO) as EpisodeResult.Ok).episode
        val lines = mutableListOf<String>()
        lateinit var listener: EventListener
        val voice = object : VoicePort {
            override fun start(value: EventListener) { listener = value }
            override fun stop() = Unit
            override fun handle(command: Command) {
                if (command is Command.Say) { lines += command.lineId; listener.onEvent(AppEvent.LineFinished(command.lineId)) }
                if (command is Command.Listen) listener.onEvent(AppEvent.AnswerHeard("1번"))
            }
        }
        val car = object : EventSource { override fun start(listener: EventListener) = Unit; override fun stop() = Unit }
        lateinit var controller: AppController
        fun send(event: AppEvent) { test.runOnMainSync { controller.dispatch(event) }; test.waitForIdleSync() }
        test.runOnMainSync {
            controller = AppController(test.targetContext, MissionEngine(episode), car, voice, episode = episode)
            controller.start()
        }
        try {
            send(AppEvent.Tick(1000))
            send(AppEvent.AgeSelected(AgeGroup.PRESCHOOL))
            send(AppEvent.DestinationSelected("gyeongju", "gyeongju_gyeongbu"))
            send(AppEvent.ListeningConsentChanged(true))
            send(AppEvent.SeatOccupied(RearSeat.LEFT, true))
            send(AppEvent.BeltBuckled(RearSeat.LEFT, true))
            send(AppEvent.GearChanged(Gear.D))
            send(AppEvent.StartPressed)
            assertTrue(lines.contains("rain_map/briefing"))
            send(AppEvent.DrivingRestricted(true))
            send(AppEvent.WiperChanged(true))
            send(AppEvent.Tick(2000))
            send(AppEvent.Tick(6000))
            assertTrue(lines.contains("crossroads/briefing"))
            send(AppEvent.NextTurnDistance(200))
            send(AppEvent.NextTurnDistance(1500))
            send(AppEvent.Tick(11000))
            TripCatalog(episode).stops("gyeongju", "gyeongju_gyeongbu", AgeGroup.PRESCHOOL).forEach { send(AppEvent.PlaceEntered(it.placeId)) }
            test.runOnMainSync { assertEquals(5, controller.items.size) }
            send(AppEvent.DestinationArrived)
            send(AppEvent.GearChanged(Gear.P))
            test.runOnMainSync {
                assertEquals(Screen.LANDED, controller.screen)
                assertTrue(lines.indexOf("ending") < lines.indexOf("delivery"))
                assertTrue(lines.indexOf("delivery") < lines.indexOf("landing_fact"))
                assertTrue(lines.contains("landed"))
            }
        } finally { test.runOnMainSync { controller.stop() } }
    }
}
