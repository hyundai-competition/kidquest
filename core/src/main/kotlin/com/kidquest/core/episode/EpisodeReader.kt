// 에피소드 파일의 글자를 Episode로 바꾼다. 파일을 여는 일은 앱이 한다.
// 대본을 고치다 틀려도 앱이 죽지 않도록 예외를 던지지 않고 틀린 곳을 결과로 돌려준다.
package com.kidquest.core.episode

import com.kidquest.core.contract.Destination
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Mission
import com.kidquest.core.contract.MissionTrigger
import kotlinx.serialization.json.Json

sealed interface EpisodeResult {
    data class Ok(val episode: Episode) : EpisodeResult

    /** 사람이 보고 고칠 수 있게 틀린 곳을 적는다. */
    data class Failed(val problems: List<String>) : EpisodeResult
}

fun readEpisode(text: String): EpisodeResult {
    val episode = try {
        Json.decodeFromString<Episode>(text)
    } catch (e: IllegalArgumentException) { // 라이브러리의 형식 오류도 이 종류다.
        return EpisodeResult.Failed(listOf("파일 형식이 틀렸다 : ${e.message}"))
    }
    val problems = findProblems(episode)
    return if (problems.isEmpty()) EpisodeResult.Ok(episode) else EpisodeResult.Failed(problems)
}

/** 형식은 맞아도 임무가 멈추거나 조용히 사라지는 실수를 찾는다. 틀린 곳을 한 번에 다 적는다. */
private fun findProblems(episode: Episode): List<String> {
    val itemIds = episode.items.map { it.id }.toSet()
    val missionIds = episode.missions.map { it.id }.toSet()
    val destinationIds = episode.destinations.map { it.id }.toSet()

    val problems = mutableListOf<String>()
    if (episode.missions.isEmpty()) problems += "임무가 하나도 없다"
    if (episode.finalLine.isBlank()) problems += "finalLine이 비어 있다"
    problems += duplicates("임무", episode.missions.map { it.id })
    problems += duplicates("아이템", episode.items.map { it.id })
    problems += duplicates("목적지", episode.destinations.map { it.id })
    problems += duplicates("좌표", episode.places.map { it.id })
    episode.missions.forEach { problems += missionProblems(it, itemIds, destinationIds) }
    episode.destinations.forEach { problems += destinationProblems(it) }
    episode.places.filter { it.missionId !in missionIds }
        .forEach { problems += "좌표 ${it.id} : 없는 임무 ${it.missionId}를 가리킨다" }
    problems += placeProblems(episode)
    return problems
}

/** 장소 임무와 좌표는 짝이 맞아야 한다. 좌표가 없는 장소 임무는 시간이 지나야만 열리고, 장소 임무가 아닌 임무의 좌표는 쓰이지 않는다. */
private fun placeProblems(episode: Episode): List<String> {
    val placeMissions = episode.missions.filter { it.trigger == MissionTrigger.PLACE }.map { it.id }.toSet()
    val pointed = episode.places.map { it.missionId }.toSet()
    val unused = episode.places.filter { it.missionId in episode.missions.map { m -> m.id } && it.missionId !in placeMissions }
        .map { "좌표 ${it.id} : 임무 ${it.missionId}는 장소 임무(PLACE)가 아니다" }
    val missing = (placeMissions - pointed).map { "임무 $it : 장소 임무인데 가리키는 좌표가 없다" }
    return unused + missing
}

private fun duplicates(kind: String, ids: List<String>): List<String> =
    ids.groupBy { it }.filterValues { it.size > 1 }.keys.map { "$kind 아이디 ${it}가 두 번 이상 나온다" }

private fun missionProblems(mission: Mission, itemIds: Set<String>, destinationIds: Set<String>): List<String> {
    val problems = blankLines(
        "임무 ${mission.id}",
        "briefing" to mission.briefing,
        "event" to mission.event,
        "question" to mission.question,
        "correctLine" to mission.correctLine,
        "wrongLine" to mission.wrongLine,
    ).toMutableList()
    val where = "임무 ${mission.id}"
    if (mission.answers.isEmpty() || mission.answers.any { it.isBlank() }) problems += "$where : answers에 정답이 없다"
    if (mission.rewardItem !in itemIds) problems += "$where : 없는 아이템 ${mission.rewardItem}을 보상으로 준다"
    if (mission.destinationId != null && mission.destinationId !in destinationIds) {
        problems += "$where : 없는 목적지 ${mission.destinationId}를 가리킨다"
    }
    if (mission.autoAfterSeconds <= 0) problems += "$where : autoAfterSeconds는 1 이상이어야 한다"
    return problems
}

private fun destinationProblems(destination: Destination): List<String> = blankLines(
    "목적지 ${destination.id}",
    "name" to destination.name,
    "planetName" to destination.planetName,
    "departureLine" to destination.departureLine,
    "approachLine" to destination.approachLine,
    "deliveryLine" to destination.deliveryLine,
    "landingFact" to destination.landingFact,
    "factSource" to destination.factSource,
)

/** 대사가 비면 말이 끝났다는 사건을 기다리다 임무가 멈출 수 있다. */
private fun blankLines(where: String, vararg fields: Pair<String, String>): List<String> =
    fields.filter { it.second.isBlank() }.map { "$where : ${it.first}가 비어 있다" }
