// 약속 2. 임무 엔진이 음성과 화면에 시키는 일을 적는다.
// 엔진은 어떻게 말하고 그리는지 모른다. 그 일은 voice와 screen이 맡는다.
package com.kidquest.core.contract

/** 앱 화면. PRD의 화면 목록과 같다. */
enum class Screen { START, CREW_CHECK, FLYING, PAUSED, DOCKED, LANDED }

/** 착륙 화면 배경. 실내 온도에 따라 바뀐다. */
enum class Background { NORMAL, COOL, WARM }

sealed interface Command {
    /** lineId는 대사 끝남(AppEvent.LineFinished)을 맞춰 보는 데 쓴다. */
    data class Say(val lineId: String, val text: String) : Command
    data class PlaySound(val soundId: String) : Command

    /** 정해진 단어를 듣는다. 결과는 AppEvent.AnswerHeard로 돌아온다. */
    data class Listen(val words: List<String>, val timeoutSeconds: Int) : Command
    data object StopAudio : Command

    /** status는 비행 중과 멈춤 화면의 상태 한 줄이다. */
    data class ShowScreen(val screen: Screen, val status: String? = null) : Command

    /** 임무 보상. 착륙 화면에 보여 주고, 배지는 기기에 저장한다. */
    data class AwardItem(val itemId: String, val name: String) : Command
    data class SetBackground(val background: Background) : Command
}
