// 에피소드의 목적지·경로·임무를 화면용 목록으로 만든다. 진행과 보상 판단은 엔진에 둔다.
package com.kidquest.app.screen.model

import com.kidquest.core.contract.*

class TripCatalog(val episode: Episode?) {
    val destinations get() = episode?.destinations.orEmpty()
    fun routes(destinationId: String) = destinations.firstOrNull { it.id == destinationId }?.routes.orEmpty()
    fun plan(destinationId: String, routeId: String?, age: AgeGroup): List<Mission> {
        val all = episode?.missions.orEmpty().filter { it.ageGroups.isEmpty() || age in it.ageGroups }
        val routed = destinations.flatMap { it.routes }.flatMap { it.missionIds }.toSet()
        val route = routes(destinationId).firstOrNull { it.id == routeId } ?: routes(destinationId).firstOrNull()
        val common = all.filter { it.id !in routed && it.destinationId == null }
        return common + route?.missionIds.orEmpty().mapNotNull { id -> all.firstOrNull { it.id == id } }
    }
    fun stops(destinationId: String, routeId: String?, age: AgeGroup) = plan(destinationId, routeId, age).mapNotNull { mission ->
        episode?.places?.firstOrNull { it.missionId == mission.id }?.let { TripStop(it.id, placeName(it.id), mission) }
    }
    fun placeName(id: String) = mapOf(
        "cheonan" to "천안", "daegu" to "대구", "suwon" to "수원", "daejeon" to "대전",
        "chungju" to "충주", "mungyeong" to "문경", "danyang" to "단양", "yeongju" to "영주",
        "cheomseongdae" to "경주 첨성대", "temporary_capital" to "부산 임시수도", "hahoe" to "안동 하회마을", "gyeongbokgung" to "서울 경복궁",
    )[id] ?: id
}
data class TripStop(val placeId: String, val label: String, val mission: Mission)
