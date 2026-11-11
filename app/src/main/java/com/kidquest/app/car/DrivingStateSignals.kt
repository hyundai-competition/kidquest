// 차량 주행 규제 조회·연결 해제는 작업 스레드에서 처리해 지연된 Car 서비스가 화면을 막지 않게 한다.
package com.kidquest.app.car

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

internal class DrivingStateSignals(private val context: Context, private val filter: CarEventFilter) {
    private val main = Handler(Looper.getMainLooper())
    private var connection: Connection? = null

    fun start() {
        stop()
        val next = Connection()
        connection = next
        main.postDelayed(next.timeout, 5_000)
        worker.execute { if (next.active.get()) next.start() }
    }

    fun stop() {
        val previous = connection ?: return
        connection = null
        previous.active.set(false)
        main.removeCallbacks(previous.timeout)
        // Binder의 disconnect도 멈출 수 있으므로 Activity 종료에서 기다리지 않는다.
        worker.execute { previous.close() }
    }

    private inner class Connection {
        val active = AtomicBoolean(true)
        val timeout = Runnable { if (active.get()) filter.unavailable(CarSignal.DRIVING_STATE) }
        private var car: Car? = null
        private var manager: CarUxRestrictionsManager? = null

        fun start() {
            try {
                // 준비 콜백 이전에 getCarManager를 부르면 Car 내부 잠금을 잡은 채
                // 서비스 Binder를 기다려 메인 스레드의 연결 콜백까지 막을 수 있다.
                car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { readyCar, ready ->
                    if (ready) worker.execute { connect(readyCar) }
                    else unavailable()
                }
            } catch (_: Exception) { unavailable(); main.removeCallbacks(timeout) }
            catch (_: LinkageError) { unavailable(); main.removeCallbacks(timeout) }
        }

        fun connect(readyCar: Car) {
            if (!active.get()) return
            try {
                manager = readyCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
                val connected = manager
                if (connected == null) unavailable()
                else if (active.get()) {
                    connected.registerListener(::send)
                    send(connected.currentCarUxRestrictions)
                }
            } catch (_: Exception) {
                unavailable()
            } catch (_: LinkageError) {
                unavailable()
            } finally {
                main.removeCallbacks(timeout)
                if (!active.get()) close()
            }
        }

        private fun unavailable() { if (active.get()) filter.unavailable(CarSignal.DRIVING_STATE) }
        private fun send(restrictions: CarUxRestrictions) {
            if (active.get()) filter.send(AppEvent.DrivingRestricted(restrictions.isRequiresDistractionOptimization))
        }
        fun close() {
            try { manager?.unregisterListener() } catch (_: Exception) { /* 종료 중 */ }
            try { car?.disconnect() } catch (_: Exception) { /* 종료 중 */ }
            manager = null
            car = null
        }
    }

    companion object {
        // Binder가 멈춰도 재시작마다 스레드를 늘리지 않는다. 대기 중 취소된 연결은 실행하지 않는다.
        private val worker = Executors.newSingleThreadExecutor { task ->
            Thread(task, "kq-driving-state").apply { isDaemon = true }
        }
    }
}
