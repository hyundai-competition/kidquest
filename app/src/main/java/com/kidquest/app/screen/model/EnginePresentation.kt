// 엔진 명령의 대사 id를 에피소드와 대조해 표시한다. 임무 순서·완료·보상을 직접 판단하지 않는다.
// 에피소드는 부를 때마다 받아 온다. 시작 전에 언어를 바꾸면 엔진이 쓰는 그 언어 파일과 맞춰 본다.
package com.kidquest.app.screen.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kidquest.app.voice.NumberAnswer
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Mission

class EnginePresentation(private val source: () -> Episode?) {
    constructor(episode: Episode?) : this({ episode })
    private val episode get() = source()
    var mission by mutableStateOf<Mission?>(null)
        private set
    var stage by mutableStateOf("")
        private set
    var caption by mutableStateOf("")
        private set
    var answerReady by mutableStateOf(false)
        private set
    private var lineId = ""
    fun lineFinished(id: String) { if (id == lineId && stage == "question") answerReady = true }
    fun closeAnswer() { answerReady = false }
    val options get() = mission?.let(EngineChoices::forMission).orEmpty()
    fun say(command: Command.Say): Command.Say {
        lineId = command.lineId
        answerReady = false
        caption = command.text
        val id = command.lineId.substringBeforeLast('/', "")
        mission = episode?.missions?.firstOrNull { it.id == id }
        stage = if (mission != null) command.lineId.substringAfterLast('/') else ""
        return if (stage == "question" && options.isNotEmpty()) command.copy(
            text = command.text + " " + options.mapIndexed { i, text -> "${i + 1}번, $text." }.joinToString(" ") + " 번호나 답으로 말해 주세요."
        ) else command
    }
    fun answer(text: String?): String? {
        if (stage != "question" || text == null) return text
        return NumberAnswer.parse(text)?.let { options.getOrNull(it) } ?: text
    }
    fun reset() { mission = null; stage = ""; caption = ""; answerReady = false; lineId = "" }
}
