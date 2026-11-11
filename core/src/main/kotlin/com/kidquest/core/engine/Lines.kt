// 상태가 바뀔 때 쓰는 대사와 상태 한 줄. 임무 대사는 에피소드 파일에 있다.
// 언어마다 한 벌씩 둔다. 대사 id는 언어와 상관없이 같아서 화면과 음성은 id로 맞춰 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Command.Say

/** 한 언어의 고정 대사와 상태 한 줄. */
internal interface LineSet {
    val CREW_FOUND: Say
    val ALREADY_READY: Say
    val READY: Say
    val NEED_BELT: Say
    val TAKE_OFF: Say
    val BELT_OFF: Say
    val DOCKED: Say
    val LANDED: Say

    /** 차 잔량(전기차 배터리, 못 읽으면 연료)이 절반 아래로 내려갈 때 한 번. 운전자를 부르거나 재촉하지 않는다. */
    val ENERGY_HALF: Say


    val STATUS_FLYING: String
    val STATUS_BELT: String
    val STATUS_GEAR: String
    val STATUS_NAVI: String
    val STATUS_STOPPED: String
    val STATUS_DOCKED: String

    /** 멈춤 화면의 상태 한 줄. 이유가 여럿이면 부모 끄기, 벨트, 기어, 내비 순서로 보인다. */
    fun pausedStatus(reasons: Set<PauseReason>): String = when {
        PauseReason.PARENT in reasons -> STATUS_STOPPED
        PauseReason.BELT in reasons -> STATUS_BELT
        PauseReason.GEAR in reasons -> STATUS_GEAR
        else -> STATUS_NAVI
    }
}

/** 한국어. 기본 언어다. */
internal object Lines : LineSet {
    override val CREW_FOUND = Say("crew_found", "탐험대원을 확인했어요. 벨트를 매면 출발 준비가 끝나요.")
    override val ALREADY_READY = Say("already_ready", "벨트를 벌써 맸네요! 대장님, 출발 준비 끝났어요.")
    override val READY = Say("ready", "대장님, 출발 준비 끝났어요.")
    override val NEED_BELT = Say("need_belt", "벨트를 매면 출발할 수 있어요.")
    override val TAKE_OFF = Say("take_off", "출발! 보물 탐험을 시작해요.")
    override val BELT_OFF = Say("belt_off", "벨트를 매면 계속할 수 있어요.")
    override val DOCKED = Say("docked", "잠깐 쉬어 갈게요. 다시 출발하면 이어서 들려줄게요.")
    override val LANDED = Say("landed", "탐험 끝! 오늘 모은 보물을 확인해 보세요.")
    override val ENERGY_HALF = Say("energy_half", "우리 차 힘이 절반 남았어요. 보물을 향해 계속 달려요!")

    /** 결말 대사의 lineId. 결말 문장은 에피소드 파일에 있고, 이 대사가 끝나면 착륙 대사를 말한다. */
    const val ENDING_ID = "ending"

    override val STATUS_FLYING = "탐험 중"
    override val STATUS_BELT = "벨트를 매면 이어져요"
    override val STATUS_GEAR = "잠깐 쉬는 중이에요"
    override val STATUS_NAVI = "내비 안내 중이에요"
    override val STATUS_STOPPED = "소리를 껐어요"
    override val STATUS_DOCKED = "휴게소에서 쉬는 중이에요"
}

/** 영어. 12세 이상 권장이라 짧고 쉬운 문장으로 쓴다. 차량·내비 역할이 한 번 읽고 고친다(#60). */
internal object EnglishLines : LineSet {
    override val CREW_FOUND = Say("crew_found", "Explorer on board! Buckle up and we're ready to go.")
    override val ALREADY_READY = Say("already_ready", "You're already buckled up! Captain, we're ready to go.")
    override val READY = Say("ready", "Captain, we're ready to go.")
    override val NEED_BELT = Say("need_belt", "Buckle up and we can set off.")
    override val TAKE_OFF = Say("take_off", "Let's go! Our treasure hunt begins.")
    override val BELT_OFF = Say("belt_off", "Buckle up and we can keep going.")
    override val DOCKED = Say("docked", "Let's take a short break. I'll continue the story when we set off again.")
    override val LANDED = Say("landed", "Adventure complete! Take a look at the treasures you found today.")
    override val ENERGY_HALF = Say("energy_half", "Our car has half of its power left. Let's keep going to the treasure!")

    override val STATUS_FLYING = "Exploring"
    override val STATUS_BELT = "Buckle up to continue"
    override val STATUS_GEAR = "Taking a short break"
    override val STATUS_NAVI = "Navigation is speaking"
    override val STATUS_STOPPED = "Sound is off"
    override val STATUS_DOCKED = "Resting at a rest stop"
}
