// 차 신호를 AppEvent로 바꾼다. 차량·내비 연결 역할이 채운다. 지금은 아무 신호도 받지 않는다.
// 문서에서 찾은 함수(미시험) :
//   뒷좌석 탑승 registerSeatOccupancy, 벨트 registerSeatBeltBuckledState,
//   기어 registerGearSelection, 방향지시등 registerTurnSignalState, 와이퍼 registerWiperState,
//   잔량 registerEvBatteryStateOfCharge, 실내 온도 getHvacTemperatureCurrent,
//   남은 시간 onTripInfo의 eta, 다음 회전 onTBTInfo, 도착 onDestinationArrived,
//   위치 FusedLocation의 addProximityAlert, 주행 규제 CarUxRestrictionsManager.
package com.kidquest.app.car

import com.kidquest.core.contract.EventListener
import com.kidquest.core.contract.EventSource

class CarSignalCollector : EventSource {
    private var listener: EventListener? = null

    override fun start(listener: EventListener) {
        this.listener = listener
    }

    override fun stop() {
        listener = null
    }
}
