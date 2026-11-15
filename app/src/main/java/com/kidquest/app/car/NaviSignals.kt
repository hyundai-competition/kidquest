// NaviHelper로 남은 시간, 다음 회전, 도로 사건, 도착을 받아 사건으로 바꾼다.
// 남은 시간과 회전 안내는 요청해야 답이 오는 함수라서 일정 간격으로 묻는다. 값이 스스로 오는지는 아직 시험하지 않았다.
// remainTime은 초, TBTInfo.distance는 미터라고 가정했다(미시험). 단위가 다르면 이 파일만 고친다.
// 「내비 없음(SignalUnavailable)」은 초기화를 몇 번 다시 해도 안 되거나 조회 권한이 없을 때만 보낸다.
// 엔진은 이 값을 지우지 않고 갈림길 임무를 시간으로 열기 때문이다.
package com.kidquest.app.car

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal

internal class NaviSignals(private val context: Context, private val filter: CarEventFilter) : NaviHelperEventListener {
    private var helper: NaviHelper? = null
    private val main = Handler(Looper.getMainLooper())

    /** 메인 스레드에서 쓰고 SDK 콜백 스레드에서도 읽는다. 멈춘 뒤 늦게 온 콜백과 성공 콜백이 수집을 다시 켜지 않게 한다. */
    @Volatile
    private var running = false

    /** 지금까지 한 초기화 횟수. 차가 막 켜져 내비 서비스가 늦게 뜨는 경우를 위해 몇 번 더 해 본다. */
    private var attempts = 0

    /**
     * 두 조회를 따로 묻는다. 하나가 실패해도 다른 하나는 묻고, 실패는 로그만 남긴 채 다음 차례에 다시 묻는다.
     * 권한(NAVI_ROUTE)이 없으면 다시 물어도 안 되므로 내비 없음을 알리고 묻기를 멈춘다.
     */
    private val poll = object : Runnable {
        override fun run() {
            val current = helper ?: return
            val allowed = ask("남은 시간") { current.getDestinationInfo() } and ask("회전 안내") { current.getTBTInfo() }
            if (!allowed) return filter.unavailable(CarSignal.NAVIGATION)
            if (running) main.postDelayed(this, POLL_MILLIS)
        }
    }

    private val retry = Runnable { if (running) connect() }

    /** 내비가 계속 없으면 두 번째 초기화부터 SDK가 성공도 실패도 돌려주지 않는다(#98). 정해진 시간 안에 답이 없으면 실패로 본다. */
    private val noAnswer = Runnable {
        if (running && helper != null) {
            Log.w(CAR_LOG_TAG, "내비 초기화 응답 없음 ${INIT_TIMEOUT_MILLIS / 1000}초")
            initFailed()
        }
    }

    fun start() {
        running = true
        attempts = 0
        connect()
    }

    /** 메인 스레드에서만 부른다. 새 NaviHelper로 초기화하고, 실패하거나 답이 없으면 정해진 횟수까지 다시 한다. */
    private fun connect() {
        attempts++
        val next = newHelperOrNull() ?: return initFailed()
        helper = next
        main.postDelayed(noAnswer, INIT_TIMEOUT_MILLIS)
        try {
            next.initialize(
                { main.post { onReady(next) } },
                { _: Throwable -> main.post { if (helper === next) initFailed() } },
            )
        } catch (_: Exception) {
            initFailed()
        }
    }

    private fun newHelperOrNull(): NaviHelper? = try {
        NaviHelper(context)
    } catch (_: Exception) {
        null
    }

    private fun initFailed() {
        if (!running) return
        main.removeCallbacks(noAnswer)
        release(helper)
        helper = null
        if (attempts < MAX_ATTEMPTS) {
            Log.w(CAR_LOG_TAG, "내비 초기화 실패 $attempts/$MAX_ATTEMPTS, 다시 시도")
            main.postDelayed(retry, RETRY_MILLIS)
        } else {
            filter.unavailable(CarSignal.NAVIGATION)
        }
    }

    /** 성공 콜백이 어느 스레드에서 와도 메인 스레드에서 stop()과 차례로 돈다. 그새 다른 초기화로 바뀌었으면 무시한다. */
    private fun onReady(ready: NaviHelper) {
        if (!running || helper !== ready) return
        main.removeCallbacks(noAnswer)
        try {
            ready.addListener(this)
        } catch (_: Exception) {
            initFailed()
            return
        }
        poll.run()
    }

    fun stop() {
        running = false
        main.removeCallbacks(poll)
        main.removeCallbacks(retry)
        main.removeCallbacks(noAnswer)
        release(helper)
        helper = null
    }

    /** 리스너 해제가 실패해도 연결은 마저 푼다. */
    private fun release(old: NaviHelper?) {
        old ?: return
        try {
            old.removeListener(this)
        } catch (_: Exception) {
            // 끝나는 중이라 할 일이 없다.
        }
        try {
            old.release()
        } catch (_: Exception) {
            // 끝나는 중이라 할 일이 없다.
        }
    }

    /**
     * 권한이 없을 때만 false를 돌려준다. SDK(NaviHelper 3.0.5)는 조회 전에 권한을 보고 없으면 SecurityException을 던진다.
     * 서비스가 끊긴 동안의 조회는 SDK가 모아 두었다가 다시 연결되면 보내서 여기서 던지지 않는다.
     */
    private inline fun ask(name: String, request: () -> Unit): Boolean = try {
        request()
        true
    } catch (e: SecurityException) {
        Log.w(CAR_LOG_TAG, "내비 $name 조회 권한 없음 : ${e.message}")
        false
    } catch (e: Exception) {
        Log.w(CAR_LOG_TAG, "내비 $name 조회 실패, 다음 차례에 다시 묻는다 : ${e.javaClass.simpleName}")
        true
    }

    /** 경로가 없으면 0이 올 수 있어서 보내지 않는다. 0을 보내면 도착이 임박한 것처럼 보인다. */
    override fun onDestinationInfo(destinationInfo: DestinationInfo) {
        if (running && destinationInfo.remainTime > 0) {
            filter.send(AppEvent.RemainingTimeChanged(destinationInfo.remainTime))
        }
    }

    override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
        if (running) CarSignalMapper.roadEvents(tbtInfoList).forEach(filter::send)
    }

    override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
        if (running) filter.send(AppEvent.DestinationArrived)
    }

    /**
     * SDK의 오류 코드는 경로 찾기 실패(CAUSE_SEARCH_ROUTE 등)와 배터리 부족(CAUSE_LOW_BATTERY)뿐이다.
     * 경로를 못 찾은 것이지 내비 값을 못 읽는 것이 아니라서 SignalUnavailable을 보내지 않는다.
     * 경로가 생기면 다음 차례 조회에서 값이 온다. 내비 값이 안 올 때 원인을 보려고 로그에는 남긴다.
     */
    override fun onError(errorCode: NaviErrorCode) {
        Log.w(CAR_LOG_TAG, "내비 오류 $errorCode")
    }

    private companion object {
        const val POLL_MILLIS = 10_000L
        const val RETRY_MILLIS = 5_000L
        const val INIT_TIMEOUT_MILLIS = 15_000L
        const val MAX_ATTEMPTS = 3
    }
}
