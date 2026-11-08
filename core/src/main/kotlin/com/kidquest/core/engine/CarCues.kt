// 임무를 여는 차 신호와 반응 소리를 맡는다. 와이퍼가 켜져 있는지, 어느 장소를 지나왔는지 기억해서
// 그 임무 차례가 오면 바로 열 수 있게 한다. 임무와 상관없는 신호에는 에피소드 파일의 반응 소리를 낸다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Mission
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.RoadKind
import com.kidquest.core.contract.TurnSide

internal class CarCues(episode: Episode?, private val plan: TripPlan) {
    private val reactions = SignalReactions(episode)

    /** 비 오는 날은 와이퍼가 계속 켜져 있다. 켜짐 사건을 다시 못 받아도 임무를 열 수 있게 기억한다. */
    var wiperOn = false

    /** 이번 이동에서 지나온 장소의 임무 id. 차례가 오기 전에 지나가도 기억한다. */
    private val reached = mutableSetOf<String>()

    /** 이 임무를 여는 신호가 이미 와 있나. 깜빡이는 순간 신호라 기억하지 않는다. */
    fun active(mission: Mission): Boolean = when (mission.trigger) {
        MissionTrigger.WIPER -> wiperOn
        MissionTrigger.PLACE -> mission.id in reached
        else -> false
    }

    /** 장소에 들어왔다. 처음 지나는 장소면 그 임무 id를, 없는 장소나 두 번째면 null을 돌려준다. */
    fun reach(placeId: String): String? {
        val missionId = plan.missionAt(placeId) ?: return null
        return missionId.takeIf { reached.add(it) }
    }

    fun wiperSound(clock: Long?): List<Command> = reactions.play("wiper_on", clock)

    fun turnSound(side: TurnSide, clock: Long?): List<Command> =
        reactions.play(if (side == TurnSide.LEFT) "turn_left" else "turn_right", clock)

    /** 앞길에 터널·톨게이트·휴게소가 가까우면 효과음을 낸다. 거리가 바뀔 때마다 다시 오니 쿨다운 안에서는 한 번만 낸다. */
    fun roadSound(kind: RoadKind, meters: Int, clock: Long?): List<Command> =
        if (meters in 0..ROAD_NEAR_METERS) reactions.play("road_${kind.name.lowercase()}", clock) else emptyList()

    /** 이번 이동의 기록만 지운다. 와이퍼는 지금 차의 값이라 그대로 둔다. */
    fun reset() {
        reached.clear()
        reactions.reset()
    }

    private companion object {
        /** 앞길 사건이 이 거리 안이면 가깝다고 본다(가정). */
        const val ROAD_NEAR_METERS = 300
    }
}
