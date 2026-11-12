// 실제 Android 사건 큐에서 입력원 전환, 늦은 가상 사건 폐기, 실제 D 진입 시 시연 중단을 확인한다.
package com.kidquest.app

import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.app.episode.EpisodeFile
import com.kidquest.core.contract.*
import com.kidquest.core.episode.EpisodeResult
import com.kidquest.core.engine.MissionEngine
import org.junit.Assert.*
import org.junit.Test

class ShowcaseControllerTest {
    @Test fun sourceSwitchDiscardsQueuedFakeSignalsAndActualDriveAbortsDemo() {
        val test = InstrumentationRegistry.getInstrumentation()
        val car = TestCar()
        lateinit var controller: AppController
        val episode = (EpisodeFile.load(test.targetContext, EpisodeFile.DEMO) as EpisodeResult.Ok).episode
        test.runOnMainSync {
            controller = AppController(test.targetContext, engine = MissionEngine(episode), car = car,
                voice = object : VoicePort {
                    override fun start(listener: EventListener) = Unit
                    override fun handle(command: Command) = Unit
                    override fun stop() = Unit
                }, episode = episode)
            controller.start()
            car.emit(AppEvent.GearChanged(Gear.P))
        }
        test.waitForIdleSync()
        try {
            test.runOnMainSync {
                assertTrue(controller.setSimulationMode(true))
                assertTrue(controller.vehicle.buckled[RearSeat.LEFT] == true)
                controller.replay.send(AppEvent.WiperChanged(true))
                controller.startTrip(listOf(AppEvent.StartPressed))
                controller.setSimulationMode(false)
            }
            test.waitForIdleSync()
            test.runOnMainSync {
                assertFalse(controller.simulationMode)
                assertFalse(controller.vehicle.wiperOn)
                assertFalse(controller.replayUsed)
                assertEquals(Screen.START, controller.screen)
                assertNull(controller.vehicle.buckled[RearSeat.LEFT])
                assertTrue(controller.setSimulationMode(true))
                car.emit(AppEvent.WiperChanged(true)) // 실제 신호는 기억만 하고 가상 화면에 섞지 않는다.
            }
            test.waitForIdleSync()
            test.runOnMainSync {
                assertFalse(controller.vehicle.wiperOn)
                car.emit(AppEvent.GearChanged(Gear.D))
            }
            test.waitForIdleSync()
            test.runOnMainSync {
                assertFalse(controller.simulationMode)
                assertEquals(Gear.D, controller.vehicle.gear)
                assertTrue(controller.vehicle.wiperOn)
                assertNull(controller.vehicle.buckled[RearSeat.LEFT])
                assertFalse(controller.setSimulationMode(true))
            }
        } finally { test.runOnMainSync { controller.stop() } }
    }

    private class TestCar : EventSource {
        private var listener: EventListener? = null
        override fun start(listener: EventListener) { this.listener = listener }
        override fun stop() { listener = null }
        fun emit(event: AppEvent) { listener?.onEvent(event) }
    }
}
