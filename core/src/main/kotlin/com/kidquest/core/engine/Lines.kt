// 상태가 바뀔 때 쓰는 대사. 임무 대사는 에피소드 파일에 있다.
package com.kidquest.core.engine

internal object Lines {
    const val CREW_FOUND = "승무원을 확인했어요. 벨트를 매면 발사 준비가 끝나요."
    const val READY = "선장님, 발사 준비 끝났어요."
    const val NEED_BELT = "벨트를 매면 발사할 수 있어요."
    const val TAKE_OFF = "발사! 우리 우주선이 출발했어요."
    const val BELT_OFF = "벨트를 매면 계속할 수 있어요."
    const val LANDED = "착륙 완료! 오늘의 배지를 확인해 보세요."
}
