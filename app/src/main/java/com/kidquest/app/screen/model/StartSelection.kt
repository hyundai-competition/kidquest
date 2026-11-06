// 시작 버튼이 선택값을 엔진에 전달하는 순서를 한곳에 둔다. 기본 이야기는 목적지 null이다.
package com.kidquest.app.screen.model

import com.kidquest.core.contract.AgeGroup
import com.kidquest.core.contract.AppEvent

data class StartSelection(val age: AgeGroup, val destinationId: String?, val consent: Boolean) {
    fun events(): List<AppEvent> = listOf(
        AppEvent.AgeSelected(age),
        AppEvent.DestinationSelected(destinationId),
        AppEvent.ListeningConsentChanged(consent),
        AppEvent.StartPressed,
    )
}
