// 실제 안드로이드 메인 스레드에서 수집기 사건 → 엔진 → 화면 상태 연결을 시험한다. 차량 값은 시험용이다.
package com.kidquest.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.core.contract.*
import com.kidquest.core.engine.MissionEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupConnectionTest {
    @Test fun earlyCarSignalsAndReplayReachScreenThroughEngine() {
        val test = InstrumentationRegistry.getInstrumentation()
        val car = TestCar()
        lateinit var controller: AppController
        test.runOnMainSync {
            // 이 시험은 차량 사건 연결만 확인한다. 실제 SDK 음성은 전용 시험에서 검증한다.
            val silentVoice = object : VoicePort {
                override fun start(listener: EventListener) = Unit
                override fun handle(command: Command) = Unit
                override fun stop() = Unit
            }
            controller = AppController(test.targetContext, engine = MissionEngine(), car = car, voice = silentVoice)
            controller.start()
            car.emit(AppEvent.SeatOccupied(RearSeat.LEFT, true))
            car.emit(AppEvent.BeltBuckled(RearSeat.LEFT, true))
            car.emit(AppEvent.GearChanged(Gear.D))
        }
        test.waitForIdleSync()
        test.runOnMainSync {
            assertEquals(Screen.START, controller.screen)
            assertEquals(true, controller.vehicle.buckled[RearSeat.LEFT])
            controller.dispatch(AppEvent.StartPressed)
        }
        test.waitForIdleSync()
        test.runOnMainSync {
            assertEquals(Screen.FLYING, controller.screen)
            assertFalse(controller.replayUsed)
            controller.replay.send(AppEvent.BeltBuckled(RearSeat.LEFT, false))
        }
        test.waitForIdleSync()
        test.runOnMainSync {
            assertEquals(Screen.PAUSED, controller.screen)
            assertTrue(controller.replayUsed)
            controller.replay.send(AppEvent.BeltBuckled(RearSeat.LEFT, true))
        }
        test.waitForIdleSync()
        test.runOnMainSync {
            assertEquals(Screen.FLYING, controller.screen)
            controller.stop()
        }
    }

    private class TestCar : EventSource {
        private var listener: EventListener? = null
        override fun start(listener: EventListener) { this.listener = listener }
        override fun stop() { listener = null }
        fun emit(event: AppEvent) { listener?.onEvent(event) }
    }
}
