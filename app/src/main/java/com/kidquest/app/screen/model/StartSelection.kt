// 시작 버튼이 선택값을 엔진에 전달하는 순서를 한곳에 둔다. 브루미 시작 화면은 네 시연 경로 중 하나의 ID를 보낸다.
package com.kidquest.app.screen.model

import com.kidquest.core.contract.AgeGroup
import com.kidquest.core.contract.AppEvent

data class StartSelection(val age: AgeGroup, val destinationId: String?, val consent: Boolean, val routeId: String? = null) {
    fun events(): List<AppEvent> = listOf(
        AppEvent.AgeSelected(age),
        AppEvent.DestinationSelected(destinationId, routeId),
        AppEvent.ListeningConsentChanged(consent),
        AppEvent.StartPressed,
    )
}
