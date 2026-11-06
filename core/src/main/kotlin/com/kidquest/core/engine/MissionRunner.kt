// 임무를 순서대로 진행한다. 브리핑 → 사건 → 질문 → 답 기다리기 → 답 대사 → 보상.
// 대사가 끝났다는 사건을 받아야 다음으로 간다. 그래서 말이 겹치지 않고, 끊긴 대사는 넘어가지 않는다.
// 엔진이 비행 중일 때만 부른다. 멈춤과 이어 가기는 엔진이 정하고, 여기서는 지금 대사를 다시 말하기만 한다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Mission

internal class MissionRunner(episode: Episode?) {
    private enum class Step { BRIEFING, EVENT, QUESTION, WAIT_ANSWER, ANSWER_LINE }

    private val missions = episode?.missions.orEmpty()
    private val itemNames = episode?.items.orEmpty().associate { it.id to it.name }

    private var index = 0

    /** null이면 이 임무를 아직 시작하지 않았다. */
    private var step: Step? = null
    private var line: Command.Say? = null

    /** 부모가 동의해야 듣는다. 기본은 듣지 않는다. 이번 이동이 끝나도 부모 설정이니 남겨 둔다. */
    private var listeningAllowed = false
    private var listening = false

    /** 답을 기다리기 시작한 뒤 처음 받은 시계 값. 시간은 Tick으로만 잰다. */
    private var waitStartedAt: Long? = null

    private val mission: Mission? get() = missions.getOrNull(index)

    /** 지금 말할 대사를 말한다. 시작 전이면 시작하고, 답을 기다리던 중이면 질문부터 다시 한다. */
    fun speakCurrent(): List<Command> {
        val now = mission ?: return emptyList()
        return when (step) {
            null -> sayStep(Step.BRIEFING, "briefing", now.briefing)
            Step.WAIT_ANSWER -> {
                listening = false // 멈출 때 듣기도 멈췄다. 질문을 다 말한 뒤 다시 듣는다.
                sayStep(Step.QUESTION, "question", now.question)
            }
            else -> listOfNotNull(line)
        }
    }

    /** 멈췄다 이어질 때. 다른 소리가 남아 있을 수 있으니 먼저 멈추고, 끊긴 문장을 처음부터 말한다. */
    fun resume(): List<Command> {
        val speech = speakCurrent()
        return if (speech.isEmpty()) emptyList() else listOf(Command.StopAudio) + speech
    }

    fun onLineFinished(lineId: String): List<Command> {
        val now = mission ?: return emptyList()
        if (lineId != line?.lineId) return emptyList()
        return when (step) {
            Step.BRIEFING -> listOfNotNull(now.eventSoundId?.let { Command.PlaySound(it) }) +
                sayStep(Step.EVENT, "event", now.event)
            Step.EVENT -> sayStep(Step.QUESTION, "question", now.question)
            Step.QUESTION -> waitForAnswer(now)
            Step.ANSWER_LINE -> completeMission(now)
            else -> emptyList()
        }
    }

    fun onAnswer(word: String?): List<Command> {
        val now = mission ?: return emptyList()
        if (step != Step.WAIT_ANSWER || !listening) return emptyList()
        return sayAnswer(now, word)
    }

    /** 듣지 않으면 8초 뒤에 넘어간다. 들을 때는 음성이 답을 안 줘도 임무가 멈추지 않게 조금 더 기다린다. */
    fun onTick(nowMillis: Long): List<Command> {
        val now = mission ?: return emptyList()
        if (step != Step.WAIT_ANSWER) return emptyList()
        val startedAt = waitStartedAt
        if (startedAt == null) {
            waitStartedAt = nowMillis
            return emptyList()
        }
        val limitSeconds = if (listening) ANSWER_SECONDS + LISTEN_GRACE_SECONDS else ANSWER_SECONDS
        if (nowMillis - startedAt < limitSeconds * 1_000L) return emptyList()
        val stop = if (listening) listOf(Command.StopAudio) else emptyList()
        return stop + sayAnswer(now, null)
    }

    fun setListeningAllowed(allowed: Boolean): List<Command> {
        listeningAllowed = allowed
        if (allowed || !listening) return emptyList()
        listening = false
        waitStartedAt = null
        return listOf(Command.StopAudio)
    }

    /** 이번 이동의 기록만 지운다. */
    fun reset() {
        index = 0
        clearStep()
    }

    private fun sayStep(next: Step, part: String, text: String): List<Command> {
        val say = Command.Say("${mission?.id}/$part", text)
        step = next
        line = say
        return listOf(say)
    }

    private fun waitForAnswer(now: Mission): List<Command> {
        step = Step.WAIT_ANSWER
        line = null
        waitStartedAt = null
        listening = listeningAllowed
        return if (listening) listOf(Command.Listen(now.answers, ANSWER_SECONDS)) else emptyList()
    }

    /** 못 알아들었거나 틀려도 혼내지 않고 정답을 말해 준다. */
    private fun sayAnswer(now: Mission, word: String?): List<Command> {
        listening = false
        return sayStep(Step.ANSWER_LINE, "answer", if (isCorrect(now, word)) now.correctLine else now.wrongLine)
    }

    /** 아이는 「초록색이요」처럼 덧붙여 말한다. 띄어쓰기를 빼고 정답이 들어 있는지 본다. */
    private fun isCorrect(now: Mission, word: String?): Boolean {
        val heard = word?.withoutSpaces() ?: return false
        return now.answers.any { it.withoutSpaces() in heard }
    }

    private fun String.withoutSpaces() = filterNot { it.isWhitespace() }

    private fun completeMission(now: Mission): List<Command> {
        val award = Command.AwardItem(now.rewardItem, itemNames[now.rewardItem] ?: now.rewardItem)
        index++
        clearStep()
        return listOf(award) + speakCurrent()
    }

    private fun clearStep() {
        step = null
        line = null
        listening = false
        waitStartedAt = null
    }

    private companion object {
        const val ANSWER_SECONDS = 8
        const val LISTEN_GRACE_SECONDS = 2
    }
}
