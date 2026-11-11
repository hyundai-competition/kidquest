// 차가 알려 주는 이동 상황을 이야기에 맞는 명령으로 바꾼다. 남은 시간은 도착 직전 문장, 실내 온도는 착륙 화면 배경, 잔량은 절반 문장이 된다.
// 차량·내비 쪽(car/)은 값이 바뀔 때만 보내고, 남은 시간은 10초마다 물어서 보낸다. 그래서 문장은 한 번만 내고, 배경은 띠가 바뀔 때만 보낸다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Background
import com.kidquest.core.contract.Command

internal class TripSignals(private val plan: TripPlan) {
    private var approachDone = false
    private var background: Background? = null
    private var lastEnergy: Int? = null
    private var energyDone = false

    /** 남은 시간이 처음으로 5분 이하가 되면 도착 직전 문장을 돌려준다. 0 이하는 값을 모르는 것으로 본다. */
    fun approach(seconds: Int): Command.Say? {
        if (approachDone || seconds <= 0 || seconds > APPROACH_SECONDS) return null
        approachDone = true
        return plan.approach()
    }

    /** 착륙 화면 배경. 20도 이하는 시원하게, 26도 이상은 따뜻하게 본다(가정). 띠가 바뀔 때만 보낸다. */
    fun temperature(celsius: Float): List<Command> {
        val next = when {
            celsius <= COOL_MAX -> Background.COOL
            celsius >= WARM_MIN -> Background.WARM
            else -> Background.NORMAL
        }
        if (next == background) return emptyList()
        background = next
        return listOf(Command.SetBackground(next))
    }

    /**
     * 잔량이 절반 이상이었다가 처음으로 절반 아래가 되면 한 번 말할 대사를 돌려준다.
     * 처음 받은 값이 이미 절반 아래면 내려가는 것을 본 적이 없어서 말하지 않는다. 값은 이동 밖에서도 따라간다.
     */
    fun energy(percent: Int, lines: LineSet): Command.Say? {
        val before = lastEnergy
        lastEnergy = percent
        if (energyDone || before == null || before < ENERGY_HALF || percent >= ENERGY_HALF) return null
        energyDone = true
        return lines.ENERGY_HALF
    }

    /** 다시 하기. 배경과 마지막 잔량은 지금 차의 값이라 그대로 둔다. */
    fun reset() {
        approachDone = false
        energyDone = false
    }

    private companion object {
        /** 도착 직전으로 보는 남은 시간(가정). 내비 남은 시간은 초 단위로 온다고 가정했다(car/ 문서). */
        const val APPROACH_SECONDS = 300
        const val COOL_MAX = 20f
        const val WARM_MIN = 26f

        /** 잔량 문장을 말하는 기준(%, 가정). 낮은 잔량은 운전자를 재촉할 수 있어서 쓰지 않는다. */
        const val ENERGY_HALF = 50
    }
}
