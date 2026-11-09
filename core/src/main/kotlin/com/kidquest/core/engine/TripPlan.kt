// 이번 이동에 쓸 것을 고른다. 고른 목적지, 경로, 나이로 임무를 거르고, 출발 문장, 도착 문장, 장소 좌표를 이어 준다.
// 시작 화면이 시작 버튼 전에 고른 값을 보낸다. 고르지 않으면 목적지와 나이가 정해지지 않은 공통 임무만 쓴다.
// 경로가 있는 목적지는 공통 임무 다음에 경로에 적힌 장소 임무를 그 순서대로 쓴다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AgeGroup
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Destination
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Mission
import com.kidquest.core.contract.Route

internal class TripPlan(private val episode: Episode?) {
    private var destination: Destination? = null
    private var route: Route? = null
    private var age: AgeGroup? = null

    /** 이번 이동에서 출발 문장을 이미 꺼냈다. */
    private var departed = false

    /** 어느 경로에든 든 임무. 그 경로를 골랐을 때만 나온다. */
    private val routed: Set<String> =
        episode?.destinations.orEmpty().flatMap { it.routes }.flatMap { it.missionIds }.toSet()

    /** 이번 이동의 임무. 목적지 전용 임무는 그 목적지를, 경로 임무는 그 경로를, 나이가 정해진 임무는 그 나이를 골랐을 때만 들어간다. */
    var missions: List<Mission> = pick()
        private set

    /** 없는 목적지 id는 고르지 않은 것으로 본다. 경로를 안 골랐거나 없는 경로면 그 목적지의 첫 경로로 간다. */
    fun selectDestination(id: String?, routeId: String? = null) {
        destination = episode?.destinations?.firstOrNull { it.id == id }
        val routes = destination?.routes.orEmpty()
        route = routes.firstOrNull { it.id == routeId } ?: routes.firstOrNull()
        missions = pick()
    }

    fun selectAge(group: AgeGroup) {
        age = group
        missions = pick()
    }

    /** 첫 임무 전에 한 번 말한다. 어디로 무엇을 찾으러 가는지 알려 준다. 경로에 출발 문장이 있으면 그것을 쓴다. 두 번째부터는 null. */
    fun takeDeparture(): Command.Say? {
        if (departed) return null
        departed = true
        return say(DEPARTURE_ID, route?.departureLine?.takeIf { it.isNotBlank() } ?: destination?.departureLine)
    }

    /** 다시 하기. 고른 값은 두고 출발 문장을 다시 말할 수 있게 한다. */
    fun newTrip() {
        departed = false
    }

    /** 도착이 가까울 때 한 번 말한다. 목적지를 안 골랐으면 에피소드의 마지막 문장(finalLine)을 쓴다. */
    fun approach(): Command.Say? = say(APPROACH_ID, destination?.approachLine ?: episode?.finalLine)

    /** 도착해서 착륙할 때 결말 다음에 말한다. 찾은 보물을 전하고, 실제 사실 한 문장으로 끝낸다. */
    fun arrivalLines(): List<Command.Say> =
        listOfNotNull(say(DELIVERY_ID, destination?.deliveryLine), say(FACT_ID, destination?.landingFact))

    /** 이 장소에 들어가면 열리는 임무 id. 없는 장소면 null. */
    fun missionAt(placeId: String): String? = episode?.places?.firstOrNull { it.id == placeId }?.missionId

    private fun pick(): List<Mission> {
        val chosenAge = age
        val chosenId = destination?.id
        val all = episode?.missions.orEmpty().filter { mission ->
            mission.ageGroups.isEmpty() || (chosenAge != null && chosenAge in mission.ageGroups)
        }
        val unrouted = all.filter { it.id !in routed }
        val chosenRoute = route ?: return unrouted.filter { it.destinationId == null || it.destinationId == chosenId }
        val byId = all.associateBy { it.id }
        return unrouted.filter { it.destinationId == null } + chosenRoute.missionIds.mapNotNull { byId[it] }
    }

    private fun say(id: String, text: String?): Command.Say? =
        text?.takeIf { it.isNotBlank() }?.let { Command.Say(id, it) }

    companion object {
        const val DEPARTURE_ID = "departure"
        const val DELIVERY_ID = "delivery"
        const val FACT_ID = "landing_fact"
        const val APPROACH_ID = "approach"
    }
}
