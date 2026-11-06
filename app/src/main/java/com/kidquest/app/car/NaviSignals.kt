// NaviHelper로 남은 시간, 다음 회전, 도로 사건, 도착을 받아 사건으로 바꾼다.
// 남은 시간과 회전 안내는 요청해야 답이 오는 함수라서 일정 간격으로 묻는다. 값이 스스로 오는지는 아직 시험하지 않았다.
// remainTime은 초, TBTInfo.distance는 미터라고 가정했다(미시험). 단위가 다르면 이 파일만 고친다.
package com.kidquest.app.car

import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal

internal class NaviSignals(context: Context, private val filter: CarEventFilter) : NaviHelperEventListener {
    private val helper = NaviHelper(context)
    private val main = Handler(Looper.getMainLooper())

    private val poll = object : Runnable {
        override fun run() {
            try {
                helper.getDestinationInfo()
                helper.getTBTInfo()
            } catch (_: Exception) {
                filter.unavailable(CarSignal.NAVIGATION)
                return
            }
            main.postDelayed(this, POLL_MILLIS)
        }
    }

    fun start() {
        try {
            helper.initialize(
                {
                    helper.addListener(this)
                    main.post(poll)
                },
                { _: Throwable -> filter.unavailable(CarSignal.NAVIGATION) },
            )
        } catch (_: Exception) {
            filter.unavailable(CarSignal.NAVIGATION)
        }
    }

    fun stop() {
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

    private companion object {
        const val POLL_MILLIS = 10_000L
    }
}
