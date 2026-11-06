// 임무를 순서대로 진행한다. 브리핑 → 신호 기다리기 → 효과음 → 사건 → 질문 → 답 기다리기 → 답 대사 → 보상.
// 대사가 끝났다는 사건을 받아야 다음으로 간다. 그래서 말이 겹치지 않고, 끊긴 대사는 넘어가지 않는다.
// 엔진이 비행 중일 때만 부른다. 멈춤과 이어 가기는 엔진이 정하고, 여기서는 지금 대사를 다시 말하기만 한다.
// 시간은 Tick으로만 잰다. 각 단계에 들어간 뒤 처음 받은 Tick부터 센다.
// 신호를 기다리는 동안 멈춘 시간도 기다린 시간에 들어간다(가정). 답을 기다리다 멈추면 질문부터 다시 해서 새로 센다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Mission
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.TurnSide

internal class MissionRunner(episode: Episode?) {
    private enum class Step { BRIEFING, WAIT_TRIGGER, SOUND, EVENT, QUESTION, WAIT_ANSWER, ANSWER_LINE }

    private val missions = episode?.missions.orEmpty()
    private val itemNames = episode?.items.orEmpty().associate { it.id to it.name }
    private val reactions = SignalReactions(episode)
    private val endingText = episode?.endingLine.orEmpty()

    private var index = 0

    /** null이면 이 임무를 아직 시작하지 않았다. */
    private var step: Step? = null
    private var line: Command.Say? = null

    /** 부모가 동의해야 듣는다. 기본은 듣지 않는다. 이번 이동이 끝나도 부모 설정이니 남겨 둔다. */
    private var listeningAllowed = false
    private var listening = false

    /** 비 오는 날은 와이퍼가 계속 켜져 있다. 켜짐 사건을 다시 못 받아도 임무를 열 수 있게 기억한다. */
    private var wiperOn = false

    /** 지금 단계에 들어간 뒤 처음 받은 시계 값. */
    private var stepStartedAt: Long? = null

    /** 마지막으로 받은 시계 값. 반응 소리의 간격을 잴 때 쓴다. */
    private var clockMillis: Long? = null

    private val mission: Mission? get() = missions.getOrNull(index)

    /** 이번 이동의 임무를 다 끝냈다. 임무가 없는 에피소드는 끝낸 것으로 보지 않는다. */
    val allDone: Boolean get() = missions.isNotEmpty() && index >= missions.size

    /** 다 끝냈을 때만 결말을 말한다. 덜 끝냈는데 「모은 조각으로 고쳤다」고 하면 이야기가 안 맞는다. */
    fun ending(): Command.Say? =
        if (allDone && endingText.isNotBlank()) Command.Say(Lines.ENDING_ID, endingText) else null

    /** 지금 말할 대사를 말한다. 시작 전이면 시작하고, 끊긴 단계는 다시 말할 수 있는 곳부터 한다. */
    fun speakCurrent(): List<Command> {
        val now = mission ?: return emptyList()
        return when (step) {
            null -> sayStep(Step.BRIEFING, "briefing", now.briefing)
            Step.WAIT_TRIGGER -> if (signalActive(now)) openEvent(now) else emptyList()
            Step.SOUND -> sayStep(Step.EVENT, "event", now.event) // 효과음이 끊겼으니 사건 대사부터 한다.
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
            Step.BRIEFING -> waitForTrigger(now)
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

    /** 와이퍼 값은 멈춘 동안에도 기억하고, 소리와 임무는 비행 중일 때만 낸다. */
    fun onWiper(on: Boolean, flying: Boolean): List<Command> {
        wiperOn = on
        if (!on || !flying) return emptyList()
        val now = mission
        if (now != null && step == Step.WAIT_TRIGGER && now.trigger == MissionTrigger.WIPER) return openEvent(now)
        return reactions.play("wiper_on", clockMillis)
    }

    fun onTurnSignal(side: TurnSide): List<Command> {
        val now = mission
        if (now != null && step == Step.WAIT_TRIGGER && now.trigger == MissionTrigger.TURN_SIGNAL) return openEvent(now)
        return reactions.play(if (side == TurnSide.LEFT) "turn_left" else "turn_right", clockMillis)
    }

    fun onTick(nowMillis: Long): List<Command> {
        clockMillis = nowMillis
        val now = mission ?: return emptyList()
        val startedAt = stepStartedAt
        if (startedAt == null) {
            stepStartedAt = nowMillis
            return emptyList()
        }
        val waited = nowMillis - startedAt
        return when (step) {
            Step.WAIT_TRIGGER -> if (waited >= now.autoAfterSeconds * 1_000L) openEvent(now) else emptyList()
            Step.SOUND -> if (waited >= EVENT_DELAY_SECONDS * 1_000L) sayStep(Step.EVENT, "event", now.event) else emptyList()
            Step.WAIT_ANSWER -> answerTimeout(now, waited)
            else -> emptyList()
        }
    }

    fun setListeningAllowed(allowed: Boolean): List<Command> {
        listeningAllowed = allowed
        if (allowed || !listening) return emptyList()
        listening = false
        stepStartedAt = null
        return listOf(Command.StopAudio)
    }

    /** 이번 이동의 기록만 지운다. */
    fun reset() {
        index = 0
        reactions.reset()
        clearStep()
    }

    private fun sayStep(next: Step, part: String, text: String): List<Command> {
        val say = Command.Say("${mission?.id}/$part", text)
        enter(next)
        line = say
        return listOf(say)
    }

    private fun enter(next: Step) {
        step = next
        line = null
        stepStartedAt = null
    }

    /** 이 임무를 여는 신호가 이미 켜져 있나. 깜빡이는 순간 신호라 기억하지 않는다. */
    private fun signalActive(now: Mission) = now.trigger == MissionTrigger.WIPER && wiperOn

    /** 신호가 오거나 시간이 지나면 연다. 신호를 못 읽는 차도 시간이 지나면 열린다. */
    private fun waitForTrigger(now: Mission): List<Command> {
        enter(Step.WAIT_TRIGGER)
        return if (signalActive(now)) openEvent(now) else emptyList()
    }

    /** 효과음을 먼저 내고 몇 초 뒤에 사건을 말한다. 효과음이 없으면 바로 말한다. */
    private fun openEvent(now: Mission): List<Command> {
        val sound = now.eventSoundId ?: return sayStep(Step.EVENT, "event", now.event)
        enter(Step.SOUND)
        return listOf(Command.PlaySound(sound))
    }

    private fun waitForAnswer(now: Mission): List<Command> {
        enter(Step.WAIT_ANSWER)
        listening = listeningAllowed
        return if (listening) listOf(Command.Listen(now.answers, ANSWER_SECONDS)) else emptyList()
    }

    /** 듣지 않으면 8초 뒤에 넘어간다. 들을 때는 음성이 답을 안 줘도 임무가 멈추지 않게 조금 더 기다린다. */
    private fun answerTimeout(now: Mission, waited: Long): List<Command> {
        val limitSeconds = if (listening) ANSWER_SECONDS + LISTEN_GRACE_SECONDS else ANSWER_SECONDS
        if (waited < limitSeconds * 1_000L) return emptyList()
        val stop = if (listening) listOf(Command.StopAudio) else emptyList()
        return stop + sayAnswer(now, null)
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
        stepStartedAt = null
    }

    private companion object {
        const val ANSWER_SECONDS = 8
        const val LISTEN_GRACE_SECONDS = 2

        /** 효과음 뒤 사건 대사까지 기다리는 시간. 정해진 값이 없어서 3초로 둔다(가정). */
        const val EVENT_DELAY_SECONDS = 3
    }
}
