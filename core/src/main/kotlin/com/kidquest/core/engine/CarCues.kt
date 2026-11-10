// 임무를 여는 차 신호와 반응 소리를 맡는다. 와이퍼가 켜져 있는지, 어느 장소를 지나왔는지 기억해서
// 그 임무 차례가 오면 바로 열 수 있게 한다. 임무와 상관없는 신호에는 에피소드 파일의 반응 소리를 낸다.
// 장소 임무는 시간으로 열지 않고(위치를 못 읽는 차만 예외), 다른 길로 와서 못 지난 장소는 건너뛰고,
// 도착이 가까우면 마지막 장소에 온 것으로 본다.
// 갈림길 임무는 내비가 알려 준 회전을 실제로 지났을 때 연다. 깜빡이로 열면 아이가 운전자에게 깜빡이를 조르게 되어서다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Mission
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.RoadKind
import com.kidquest.core.contract.TurnSide

internal class CarCues(private val plan: TripPlan) {
    private val reactions = SignalReactions { plan.episode }

    /** 비 오는 날은 와이퍼가 계속 켜져 있다. 켜짐 사건을 다시 못 받아도 임무를 열 수 있게 기억한다. */
    var wiperOn = false

    /** 위치를 못 읽는 차. 이때만 장소 임무를 시간으로 연다. 와이퍼처럼 차의 값이라 다시 하기에도 남긴다. */
    var locationMissing = false

    /** 내비를 못 읽는 차. 이때는 갈림길 임무도 시간으로 연다. 차의 값이라 다시 하기에도 남긴다. */
    var navigationMissing = false

    /** 이번 이동에서 내비 회전 안내를 받았나. 받았으면 갈림길 임무는 실제 갈림길을 지날 때만 연다. */
    private var guided = false

    /** 마지막으로 받은 다음 회전까지 거리, 그리고 지금 기다리는 회전이 가까이(500m 안) 온 적이 있나. */
    private var lastTurnMeters: Int? = null
    private var turnNear = false

    /** 이번 이동에서 지나온 장소의 임무 id. 차례가 오기 전에 지나가도 기억한다. */
    private val reached = mutableSetOf<String>()

    /** 이 임무를 여는 신호가 이미 와 있나. 깜빡이는 순간 신호라 기억하지 않는다. */
    fun active(mission: Mission): Boolean = when (mission.trigger) {
        MissionTrigger.WIPER -> wiperOn
        MissionTrigger.PLACE -> mission.id in reached
        else -> false
    }

    /**
     * 신호가 안 와도 이만큼(초) 기다리면 연다. null이면 시간으로 열지 않는다.
     * 장소 임무를 시간으로 열면 서울에서 「경주에 왔어요」라고 할 수 있어서, 위치를 못 읽는 차에서만 연다.
     * 갈림길 임무는 내비 회전 안내가 안 오거나(경로 안내 없이 출발, 재생 패널) 내비를 못 읽으면 파일의 시간에 연다.
     * 안내가 오는 중이면 실제 갈림길을 기다리되, 10분 동안 못 지나면 연다. 회전 거리가 드물게 오거나 안내를 꺼서 못 알아챌 때도 멈추지 않게 한다.
     */
    fun openAfterSeconds(mission: Mission): Int? = when (mission.trigger) {
        MissionTrigger.PLACE -> mission.autoAfterSeconds.takeIf { locationMissing }
        MissionTrigger.NAVI_TURN ->
            if (navigationMissing || !guided) mission.autoAfterSeconds else maxOf(mission.autoAfterSeconds, GUIDED_TURN_WAIT_SECONDS)
        else -> mission.autoAfterSeconds
    }

    /**
     * 지금 임무의 신호는 아직인데 뒤 장소 임무의 장소에 벌써 왔나. 그러면 지금 임무를 보물 없이 건너뛴다.
     * 장소 임무는 다른 길로 왔다는 뜻이다. 갈림길 임무는 갈림길을 못 지나고 거기까지 왔다는 뜻이라, 마지막 보물을 막지 않게 건너뛴다.
     */
    fun missed(missions: List<Mission>, index: Int): Boolean {
        val now = missions.getOrNull(index) ?: return false
        val waiting = when (now.trigger) {
            MissionTrigger.PLACE -> now.id !in reached
            MissionTrigger.NAVI_TURN -> true
            else -> false
        }
        if (!waiting) return false
        return missions.drop(index + 1).any { it.trigger == MissionTrigger.PLACE && it.id in reached }
    }

    /**
     * 내비가 알려 준 다음 회전까지 거리를 받았다. 가까이 왔던 회전의 거리가 크게 늘면 그 회전을 지나 다음 회전으로 넘어간 것이라 true를 돌려준다.
     * 거리는 조금씩 흔들릴 수 있어서 100m 넘게 늘 때만 지난 것으로 본다. 회전을 놓쳐 경로를 다시 찾아도 그 갈림길은 지난 것이다.
     */
    fun passedTurn(meters: Int): Boolean {
        guided = true
        val last = lastTurnMeters
        val passed = turnNear && last != null && meters > last + TURN_PASSED_JUMP_METERS
        if (passed) turnNear = false
        if (meters in 0..TURN_NEAR_METERS) turnNear = true
        lastTurnMeters = meters
        return passed
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

    /** 이번 이동의 기록만 지운다. 와이퍼, 위치와 내비를 못 읽는 것은 차의 값이라 그대로 둔다. */
    fun reset() {
        reached.clear()
        reactions.reset()
        guided = false
        lastTurnMeters = null
        turnNear = false
    }

    private companion object {
        /** 앞길 사건이 이 거리 안이면 가깝다고 본다(가정). */
        const val ROAD_NEAR_METERS = 300

        /** 다음 회전이 이 거리 안이면 가까이 왔다고 본다(가정). 회전 거리가 10초마다 와도 시속 150km(10초에 약 420m)까지는 지나기 전 마지막 값이 이 안에 든다. */
        const val TURN_NEAR_METERS = 500

        /** 가까이 왔던 회전의 거리가 이만큼 넘게 늘면 지난 것으로 본다(가정). */
        const val TURN_PASSED_JUMP_METERS = 100

        /** 회전 안내가 오는 중에 갈림길을 못 지나도 이만큼(초) 기다리면 연다(가정). */
        const val GUIDED_TURN_WAIT_SECONDS = 600
    }
}
