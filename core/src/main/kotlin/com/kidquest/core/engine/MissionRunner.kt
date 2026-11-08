// 임무를 순서대로 진행한다. 브리핑 → 신호 기다리기 → 효과음 → 사건 → 질문 → 답 기다리기 → 답 대사 → 보상.
// 대사가 끝났다는 사건을 받아야 다음으로 간다. 그래서 말이 겹치지 않고, 끊긴 대사는 넘어가지 않는다.
// 엔진이 비행 중일 때만 부른다. 멈춤과 이어 가기는 엔진이 정하고, 여기서는 지금 대사를 다시 말하기만 한다.
// 시간은 Tick으로만 잰다. 각 단계에 들어간 뒤 처음 받은 Tick부터 센다.
// 신호를 기다리는 동안 멈춘 시간도 기다린 시간에 들어간다(가정). 답을 기다리다 멈추면 질문부터 다시 해서 새로 센다.
// 고른 목적지에 출발 문장이 있으면 첫 임무 전에 한 번 말한다. 장소는 차례가 오기 전에 지나가도 기억했다가 그 임무 차례에 연다.
// 도착 직전 문장 같은 알림은 대사를 끊지 않고, 신호를 기다리거나 임무를 다 끝내 조용할 때 끼워 넣는다.
// 차 신호 기억과 반응 소리는 CarCues, 답 맞춰 보기는 Answers가 한다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Mission
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.RoadKind
import com.kidquest.core.contract.TurnSide

internal class MissionRunner(episode: Episode?, private val plan: TripPlan) {
    private enum class Step { INTRO, NOTICE, BRIEFING, WAIT_TRIGGER, SOUND, EVENT, QUESTION, WAIT_ANSWER, ANSWER_LINE }

    /** 고른 목적지와 나이로 거른 임무. 시작 전에만 바뀐다. */
    private val missions: List<Mission> get() = plan.missions
    private val itemNames = episode?.items.orEmpty().associate { it.id to it.name }
    private val cues = CarCues(episode, plan)
    private val endingText = episode?.endingLine.orEmpty()

    private var index = 0

    /** null이면 이 임무를 아직 시작하지 않았다. */
    private var step: Step? = null
    private var line: Command.Say? = null

    /** 부모가 동의해야 듣는다. 기본은 듣지 않는다. 이번 이동이 끝나도 부모 설정이니 남겨 둔다. */
    private var listeningAllowed = false
    private var listening = false

    /** 출발 문장을 이미 말했거나 말할 게 없다. */
    private var introDone = false

    /** 아직 못 한 알림과, 알림 때문에 잠깐 멈춘 신호 기다리기를 시작한 시계 값. */
    private var notice: Command.Say? = null
    private var waitStartedAt: Long? = null

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
            null -> sayIntro() ?: sayStep(Step.BRIEFING, "briefing", now.briefing)
            Step.WAIT_TRIGGER -> if (cues.active(now)) openEvent(now) else sayNoticeIfQuiet()
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
        if (step == Step.NOTICE && lineId == line?.lineId) return afterNotice()
        val now = mission ?: return emptyList()
        if (lineId != line?.lineId) return emptyList()
        return when (step) {
            Step.INTRO -> {
                clearStep()
                speakCurrent()
            }
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
        cues.wiperOn = on
        if (!on || !flying) return emptyList()
        val now = mission
        if (now != null && step == Step.WAIT_TRIGGER && now.trigger == MissionTrigger.WIPER) return openEvent(now)
        return cues.wiperSound(clockMillis)
    }

    fun onTurnSignal(side: TurnSide): List<Command> {
        val now = mission
        if (now != null && step == Step.WAIT_TRIGGER && now.trigger == MissionTrigger.TURN_SIGNAL) return openEvent(now)
        return cues.turnSound(side, clockMillis)
    }

    /** 알림을 맡긴다. 비행 중이고 조용하면 바로 말하고, 아니면 조용해질 때 말한다. */
    fun announce(say: Command.Say, flying: Boolean): List<Command> {
        notice = say
        return if (flying) sayNoticeIfQuiet() else emptyList()
    }

    fun onRoad(kind: RoadKind, meters: Int): List<Command> = cues.roadSound(kind, meters, clockMillis)

    /** 장소에 들어왔다. 그 장소 임무가 신호를 기다리는 중이면 열고, 아직 차례가 아니면 기억만 한다. 같은 장소는 한 번만 센다. */
    fun onPlace(placeId: String, flying: Boolean): List<Command> {
        val missionId = cues.reach(placeId) ?: return emptyList()
        val now = mission
        if (!flying || now == null || now.id != missionId || step != Step.WAIT_TRIGGER) return emptyList()
        return if (cues.active(now)) openEvent(now) else emptyList()
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
        introDone = false
        notice = null
        waitStartedAt = null
        cues.reset()
        clearStep()
    }

    private fun sayStep(next: Step, part: String, text: String): List<Command> {
        val say = Command.Say("${mission?.id}/$part", text)
        enter(next)
        line = say
        return listOf(say)
    }

    /** 첫 임무 전에 출발 문장을 한 번 말한다. 말할 게 없으면 null. */
    private fun sayIntro(): List<Command>? {
        if (introDone) return null
        introDone = true
        val intro = plan.departure() ?: return null
        enter(Step.INTRO)
        line = intro
        return listOf(intro)
    }

    private fun enter(next: Step) {
        step = next
        line = null
        stepStartedAt = null
    }

    /** 신호가 오거나 시간이 지나면 연다. 신호를 못 읽는 차도 시간이 지나면 열린다. */
    private fun waitForTrigger(now: Mission): List<Command> {
        enter(Step.WAIT_TRIGGER)
        return if (cues.active(now)) openEvent(now) else sayNoticeIfQuiet()
    }

    /** 알림을 끼워 넣어도 되는 때만 말한다. 신호를 기다리는 중이거나 임무를 다 끝내 조용할 때다. */
    private fun sayNoticeIfQuiet(): List<Command> {
        val say = notice ?: return emptyList()
        val waiting = step == Step.WAIT_TRIGGER
        if (!waiting && !(allDone && step == null)) return emptyList()
        notice = null
        waitStartedAt = if (waiting) stepStartedAt else null
        enter(Step.NOTICE)
        line = say
        return listOf(say)
    }

    /** 알림이 끝나면 하던 신호 기다리기로 돌아간다. 기다린 시간은 이어서 세고, 그사이 온 신호는 바로 연다. */
    private fun afterNotice(): List<Command> {
        val now = mission
        if (now == null) {
            clearStep()
            return emptyList()
        }
        enter(Step.WAIT_TRIGGER)
        stepStartedAt = waitStartedAt
        return if (cues.active(now)) openEvent(now) else emptyList()
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
        return sayStep(Step.ANSWER_LINE, "answer", if (Answers.isCorrect(now, word)) now.correctLine else now.wrongLine)
    }

    private fun completeMission(now: Mission): List<Command> {
        val award = Command.AwardItem(now.rewardItem, itemNames[now.rewardItem] ?: now.rewardItem)
        index++
        clearStep()
        val next = speakCurrent()
        return listOf(award) + next + (if (mission == null) sayNoticeIfQuiet() else emptyList())
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
