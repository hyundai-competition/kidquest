// 실제 엔진의 대사 완료를 기다려 가상 차량·위치 사건만 넣는다. 대사 완료·정답·보상은 만들지 않는다.
package com.kidquest.app.replay

import com.kidquest.core.contract.*

data class ShowcaseSnapshot(
    val screen: Screen, val missionId: String?, val stage: String,
    val lastFinishedLine: String, val answerReady: Boolean, val items: Int,
    val parentStopped: Boolean = false, val speaking: Boolean = false,
)

class ShowcaseRunner(
    private val missions: List<Mission>, private val places: List<Place>, initialClock: Long,
    private val send: (AppEvent) -> Unit,
) {
    var finished = false
        private set
    var advanceRequested = false
        private set
    private var clock = initialClock
    private var startedDriving = false
    private var parked = false
    private var triggered = mutableSetOf<String>()
    private var waitingMission: String? = null
    private var waitSeconds = 0
    private var crewSeconds = 0

    fun next() { if (!finished) advanceRequested = true }

    fun tick(state: ShowcaseSnapshot) {
        if (finished || state.parentStopped || state.screen == Screen.PAUSED) return
        clock += 1000
        send(AppEvent.Tick(clock))
        if (state.screen == Screen.CREW_CHECK && !state.speaking) crewSeconds++
        if (!startedDriving && state.screen == Screen.CREW_CHECK && !state.speaking &&
            (state.lastFinishedLine in setOf("already_ready", "ready") || crewSeconds >= 4)) {
            startedDriving = true
            advanceRequested = false
            send(AppEvent.GearChanged(Gear.D))
        }
        if (state.screen == Screen.FLYING) progress(state)
        if (state.screen == Screen.LANDED && state.lastFinishedLine == "landed") finished = true
    }

    private fun progress(state: ShowcaseSnapshot) {
        if (!parked && missions.isNotEmpty() && state.items >= missions.size) {
            parked = true
            advanceRequested = false
            send(AppEvent.WiperChanged(false))
            send(AppEvent.DestinationArrived)
            send(AppEvent.GearChanged(Gear.P))
            return
        }
        val mission = missions.firstOrNull { it.id == state.missionId } ?: return
        if (state.stage == "briefing" && state.lastFinishedLine == "${mission.id}/briefing" && mission.id !in triggered) {
            if (waitingMission != mission.id) { waitingMission = mission.id; waitSeconds = 0 }
            waitSeconds++
            if (advanceRequested || waitSeconds >= 4) {
                advanceRequested = false
                triggered += mission.id
                trigger(mission)
            }
        }
        // 다음 구간도 질문을 끊거나 가짜 정답을 만들지 않는다. 실제 질문 완료 후 무응답 시간만 줄인다.
        if (advanceRequested && state.stage == "question" && state.answerReady) {
            advanceRequested = false
            send(AppEvent.Tick(clock))
            clock += 11_000
            send(AppEvent.Tick(clock))
        }
    }

    private fun trigger(mission: Mission) {
        when (mission.trigger) {
            MissionTrigger.WIPER -> {
                send(AppEvent.WiperChanged(true))
                send(AppEvent.TurnSignalOn(TurnSide.LEFT))
            }
            MissionTrigger.NAVI_TURN -> {
                send(AppEvent.WiperChanged(false))
                send(AppEvent.TurnSignalOn(TurnSide.RIGHT))
                send(AppEvent.NextTurnDistance(200))
                send(AppEvent.NextTurnDistance(1500))
            }
            MissionTrigger.PLACE -> places.firstOrNull { it.missionId == mission.id }?.let { send(AppEvent.PlaceEntered(it.id)) }
            // 현재 대본에는 없지만 시간·도로 임무도 같은 엔진의 대기 규칙을 따른다.
            else -> {
                send(AppEvent.Tick(clock))
                clock += mission.autoAfterSeconds * 1000L + 1000
                send(AppEvent.Tick(clock))
            }
        }
    }
}
