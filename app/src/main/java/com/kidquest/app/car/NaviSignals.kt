// NaviHelper로 남은 시간, 다음 회전, 도로 사건, 도착을 받아 사건으로 바꾼다.
// 남은 시간과 회전 안내는 요청해야 답이 오는 함수라서 일정 간격으로 묻는다. 값이 스스로 오는지는 아직 시험하지 않았다.
// remainTime은 초, TBTInfo.distance는 미터라고 가정했다(미시험). 단위가 다르면 이 파일만 고친다.
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

internal class NaviSignals(context: Context, private val filter: CarEventFilter) : NaviHelperEventListener {
    private val helper = NaviHelper(context)
    private val main = Handler(Looper.getMainLooper())

    /** 메인 스레드에서만 읽고 쓴다. 초기화가 끝나기 전에 멈추면 늦게 온 성공 콜백이 수집을 다시 켜지 않게 한다. */
    private var running = false

    /** 한 번 실패해도 다음 차례에 다시 묻는다. 잠깐 난 오류로 남은 시간과 회전 안내가 끊기지 않게 한다. */
    private val poll = object : Runnable {
        override fun run() {
            try {
                helper.getDestinationInfo()
                helper.getTBTInfo()
            } catch (_: Exception) {
                filter.unavailable(CarSignal.NAVIGATION)
            }
            if (running) main.postDelayed(this, POLL_MILLIS)
        }
    }

    fun start() {
        running = true
        try {
            helper.initialize(
                { main.post(::onReady) },
                { _: Throwable -> filter.unavailable(CarSignal.NAVIGATION) },
            )
        } catch (_: Exception) {
            filter.unavailable(CarSignal.NAVIGATION)
        }
    }

    /** 성공 콜백이 어느 스레드에서 와도 메인 스레드에서 stop()과 차례로 돈다. */
    private fun onReady() {
        if (!running) return
        helper.addListener(this)
        poll.run()
    }

    fun stop() {
        running = false
        main.removeCallbacks(poll)
        try {
            helper.removeListener(this)
            helper.release()
        } catch (_: Exception) {
            // 끝나는 중이라 할 일이 없다.
        }
    }

    /** 경로가 없으면 0이 올 수 있어서 보내지 않는다. 0을 보내면 도착이 임박한 것처럼 보인다. */
    override fun onDestinationInfo(destinationInfo: DestinationInfo) {
        if (destinationInfo.remainTime > 0) {
            filter.send(AppEvent.RemainingTimeChanged(destinationInfo.remainTime))
        }
    }

    override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
        CarSignalMapper.roadEvents(tbtInfoList).forEach(filter::send)
    }

    override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
        filter.send(AppEvent.DestinationArrived)
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
    }
}
