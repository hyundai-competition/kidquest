// 위치 진입과 보상 기록을 화면용 구간 진행도로 바꾼다. 실시간 GPS 비율이 아니다.
package com.kidquest.app.screen.model

data class JourneyProgress(val labels: List<String>, val currentIndex: Int, val completedIndices: Set<Int>) {
    val percent: Int get() = if (labels.size < 2) 0 else currentIndex * 100 / labels.lastIndex
}

fun journeyProgress(origin: String, stops: List<TripStop>, reached: Set<String>, rewards: Set<String>, arrived: Boolean): JourneyProgress {
    val completed = stops.mapIndexedNotNull { index, stop -> if (stop.mission.rewardItem in rewards) index + 1 else null }.toSet()
    val entered = stops.mapIndexedNotNull { index, stop -> if (stop.placeId in reached) index + 1 else null }
    val current = if (arrived) stops.size else (entered + completed).maxOrNull() ?: 0
    return JourneyProgress(listOf(origin) + stops.map { it.label }, current, completed)
}
