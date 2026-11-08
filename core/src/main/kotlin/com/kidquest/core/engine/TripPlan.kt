// 이번 이동에 쓸 것을 고른다. 고른 목적지와 나이로 임무를 거르고, 출발 문장, 도착 문장, 장소 좌표를 이어 준다.
// 시작 화면이 시작 버튼 전에 고른 값을 보낸다. 고르지 않으면 목적지와 나이가 정해지지 않은 공통 임무만 쓴다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AgeGroup
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Destination
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Mission

internal class TripPlan(private val episode: Episode?) {
    private var destination: Destination? = null
    private var age: AgeGroup? = null

    /** 이번 이동의 임무. 목적지 전용 임무는 그 목적지를, 나이가 정해진 임무는 그 나이를 골랐을 때만 들어간다. */
    var missions: List<Mission> = pick()
        private set

    /** 없는 목적지 id는 고르지 않은 것으로 본다. */
    fun selectDestination(id: String?) {
        destination = episode?.destinations?.firstOrNull { it.id == id }
        missions = pick()
    }

    fun selectAge(group: AgeGroup) {
        age = group
        missions = pick()
    }

    /** 첫 임무 전에 말한다. 어디로 무엇을 찾으러 가는지 알려 준다. */
    fun departure(): Command.Say? = say(DEPARTURE_ID, destination?.departureLine)

    /** 도착해서 착륙할 때 결말 다음에 말한다. 찾은 보물을 전하고, 실제 사실 한 문장으로 끝낸다. */
    fun arrivalLines(): List<Command.Say> =
        listOfNotNull(say(DELIVERY_ID, destination?.deliveryLine), say(FACT_ID, destination?.landingFact))

    /** 이 장소에 들어가면 열리는 임무 id. 없는 장소면 null. */
    fun missionAt(placeId: String): String? = episode?.places?.firstOrNull { it.id == placeId }?.missionId

    private fun pick(): List<Mission> {
        val chosenAge = age
        val chosenId = destination?.id
        return episode?.missions.orEmpty().filter { mission ->
            (mission.destinationId == null || mission.destinationId == chosenId) &&
                (mission.ageGroups.isEmpty() || (chosenAge != null && chosenAge in mission.ageGroups))
        }
    }

    private fun say(id: String, text: String?): Command.Say? =
        text?.takeIf { it.isNotBlank() }?.let { Command.Say(id, it) }

    companion object {
        const val DEPARTURE_ID = "departure"
        const val DELIVERY_ID = "delivery"
        const val FACT_ID = "landing_fact"
    }
}
