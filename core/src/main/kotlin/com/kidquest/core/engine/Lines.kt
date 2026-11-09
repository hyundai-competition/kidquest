// 상태가 바뀔 때 쓰는 대사와 상태 한 줄. 임무 대사는 에피소드 파일에 있다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Command.Say

internal object Lines {
    val CREW_FOUND = Say("crew_found", "탐험대원을 확인했어요. 벨트를 매면 출발 준비가 끝나요.")
    val ALREADY_READY = Say("already_ready", "벨트를 벌써 맸네요! 대장님, 출발 준비 끝났어요.")
    val READY = Say("ready", "대장님, 출발 준비 끝났어요.")
    val NEED_BELT = Say("need_belt", "벨트를 매면 출발할 수 있어요.")
    val TAKE_OFF = Say("take_off", "출발! 보물 탐험을 시작해요.")
    val BELT_OFF = Say("belt_off", "벨트를 매면 계속할 수 있어요.")
    val DOCKED = Say("docked", "잠깐 쉬어 갈게요. 다시 출발하면 이어서 들려줄게요.")
    val LANDED = Say("landed", "탐험 끝! 오늘 모은 보물을 확인해 보세요.")

    /** 결말 대사의 lineId. 결말 문장은 에피소드 파일에 있고, 이 대사가 끝나면 착륙 대사를 말한다. */
    const val ENDING_ID = "ending"

    const val STATUS_FLYING = "탐험 중"
    const val STATUS_BELT = "벨트를 매면 이어져요"
    const val STATUS_GEAR = "잠깐 쉬는 중이에요"
    const val STATUS_NAVI = "내비 안내 중이에요"
    const val STATUS_STOPPED = "소리를 껐어요"
    const val STATUS_DOCKED = "휴게소에서 쉬는 중이에요"
}
