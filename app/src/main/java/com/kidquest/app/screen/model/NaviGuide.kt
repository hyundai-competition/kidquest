// 화면 아래 내비 안내 한 줄의 값과 글자. 내비 회전 안내(NextTurnDistance)의 거리와 회전 종류만 쓴다(11-09 회의 결정 5번).
// 내비 지도를 우리 화면에 넣지 않는다. 내비 앱은 우리 앱 옆에 따로 뜬다.
package com.kidquest.app.screen.model

import com.kidquest.core.contract.Language
import com.kidquest.core.contract.TurnKind

data class NaviGuide(val meters: Int, val turn: TurnKind)

object NaviGuideText {
    /**
     * 예 : 「내비 · 300m 앞 좌회전」, 「Navi · In 300 m, turn left」
     * 좁은 창에서 줄이 넘치면 끝이 말줄임표로 잘린다. 거리가 잘리지 않게 두 언어 모두 거리를 앞에 둔다.
     */
    fun line(guide: NaviGuide, language: Language): String = when (language) {
        Language.KO -> "내비 · ${distance(guide.meters, "")} 앞 ${korean(guide.turn)}"
        Language.EN -> "Navi · In ${distance(guide.meters, " ")}, ${english(guide.turn)}"
    }

    /** 1km 아래는 미터로, 그 위는 소수 한 자리 km로 쓴다. */
    fun distance(meters: Int, space: String): String =
        if (meters < 1000) "$meters${space}m" else "%.1f${space}km".format(java.util.Locale.ROOT, meters / 1000f)

    private fun korean(turn: TurnKind) = when (turn) {
        TurnKind.LEFT -> "좌회전"
        TurnKind.RIGHT -> "우회전"
        TurnKind.KEEP_LEFT -> "왼쪽 길"
        TurnKind.KEEP_RIGHT -> "오른쪽 길"
        TurnKind.UTURN -> "유턴"
        TurnKind.ROUNDABOUT -> "회전교차로"
        TurnKind.IC_ENTER -> "나들목 들어가기"
        TurnKind.IC_EXIT -> "나들목 나가기"
        TurnKind.UNKNOWN -> "다음 안내"
    }

    private fun english(turn: TurnKind) = when (turn) {
        TurnKind.LEFT -> "turn left"
        TurnKind.RIGHT -> "turn right"
        TurnKind.KEEP_LEFT -> "keep left"
        TurnKind.KEEP_RIGHT -> "keep right"
        TurnKind.UTURN -> "U-turn"
        TurnKind.ROUNDABOUT -> "roundabout"
        TurnKind.IC_ENTER -> "enter the interchange"
        TurnKind.IC_EXIT -> "exit the interchange"
        TurnKind.UNKNOWN -> "next guidance"
    }
}
