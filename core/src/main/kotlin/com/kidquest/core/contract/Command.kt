// 약속 2. 임무 엔진이 음성과 화면에 시키는 일을 적는다.
// 엔진은 어떻게 말하고 그리는지 모른다. 그 일은 voice와 screen이 맡는다.
package com.kidquest.core.contract

/** 앱 화면. PRD의 화면 목록과 같다. */
enum class Screen { START, CREW_CHECK, FLYING, PAUSED, LANDED }

sealed interface Command {
    data class Say(val text: String) : Command
    data class PlaySound(val soundId: String) : Command

    /** 정해진 단어를 듣는다. 결과는 AppEvent.AnswerHeard로 돌아온다. */
    data class Listen(val words: List<String>, val timeoutSeconds: Int) : Command
    data object StopAudio : Command
    data class ShowScreen(val screen: Screen) : Command
}
