// 내비 값을 본다. 남은 시간이 시간이 지나면서 바뀌는지, 회전 안내에 터널과 톨게이트가 오는지, 도착이 오는지가 핵심이다.
// 경로는 NaviDevApp이나 내비 화면에서 넣는다. 이 앱은 듣기만 한다.
package com.kidquest.signalcheck

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.TripInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context

class NaviProbe(context: Context, private val log: SignalLog) : NaviHelperEventListener {
    private val helper = NaviHelper(context.applicationContext)

    fun start() = log.attempt("내비 초기화") {
        helper.initialize()
        helper.addListener(this)
    }

    /** 값을 지금 다시 달라고 한다. 답은 아래 콜백으로 온다. */
    fun readNow() = log.attempt("내비 값 요청") {
        helper.getDestinationInfo()
        helper.getTBTInfo()
        helper.getRouteStateInfo()
    }

    fun stop() = log.attempt("내비 해제") {
        helper.removeListener(this)
        helper.release()
    }

    override fun onRouteStarted(routeStartInfo: RouteStartInfo) = log.line("경로 시작", routeStartInfo.toString())
    override fun onRouteEnded() = log.line("경로 끝", "")
    override fun onDestinationInfo(destinationInfo: DestinationInfo) = log.line("남은 시간", destinationInfo.toString())
    override fun onTBTInfo(tbtInfoList: List<TBTInfo>) = log.line("회전 안내", tbtInfoList.toString())
    override fun onTripInfo(tripInfo: TripInfo) = log.line("이동 정보", tripInfo.toString())
    override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) = log.line("도착", destinationArrivedInfo.toString())
    override fun onError(errorCode: NaviErrorCode) = log.line("내비 오류", errorCode.toString())
}
