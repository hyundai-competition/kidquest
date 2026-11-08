// 임무를 여는 차 신호와 반응 소리를 맡는다. 와이퍼가 켜져 있는지, 어느 장소를 지나왔는지 기억해서
// 그 임무 차례가 오면 바로 열 수 있게 한다. 임무와 상관없는 신호에는 에피소드 파일의 반응 소리를 낸다.
// 장소 임무는 시간으로 열지 않고(위치를 못 읽는 차만 예외), 다른 길로 와서 못 지난 장소는 건너뛰고,
// 도착이 가까우면 마지막 장소에 온 것으로 본다.
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

    /** 위치를 못 읽는 차. 이때만 장소 임무를 시간으로 연다. 와이퍼처럼 차의 값이라 다시 하기에도 남긴다. */
    var locationMissing = false

    /** 이번 이동에서 지나온 장소의 임무 id. 차례가 오기 전에 지나가도 기억한다. */
    private val reached = mutableSetOf<String>()

    /** 이 임무를 여는 신호가 이미 와 있나. 깜빡이는 순간 신호라 기억하지 않는다. */
    fun active(mission: Mission): Boolean = when (mission.trigger) {
        MissionTrigger.WIPER -> wiperOn
        MissionTrigger.PLACE -> mission.id in reached
        else -> false
    }

    /** 신호가 안 와도 시간이 지나면 여나. 장소 임무를 시간으로 열면 서울에서 「경주에 왔어요」라고 할 수 있어서, 위치를 못 읽는 차에서만 연다. */
    fun opensByTime(mission: Mission): Boolean = mission.trigger != MissionTrigger.PLACE || locationMissing

    /** 지금 장소 임무의 장소는 못 지났는데 뒤 장소 임무의 장소에 벌써 왔나. 다른 길로 왔다는 뜻이라 지금 임무를 건너뛴다. */
    fun missed(missions: List<Mission>, index: Int): Boolean {
        val now = missions.getOrNull(index) ?: return false
        if (now.trigger != MissionTrigger.PLACE || now.id in reached) return false
        return missions.drop(index + 1).any { it.trigger == MissionTrigger.PLACE && it.id in reached }
    }

    /** 도착이 가까우면 마지막 장소 임무의 장소에 온 것으로 본다. 도착지 원을 비껴가도 마지막 보물은 받게 한다. */
    fun reachFinal(missions: List<Mission>) {
        missions.lastOrNull { it.trigger == MissionTrigger.PLACE }?.let { reached += it.id }
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

    /** 이번 이동의 기록만 지운다. 와이퍼와 위치를 못 읽는 것은 차의 값이라 그대로 둔다. */
    fun reset() {
        reached.clear()
        reactions.reset()
    }

    private companion object {
        /** 앞길 사건이 이 거리 안이면 가깝다고 본다(가정). */
        const val ROAD_NEAR_METERS = 300
    }
}
