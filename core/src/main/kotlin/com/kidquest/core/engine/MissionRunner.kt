// 임무를 순서대로 진행한다. 브리핑 → 신호 기다리기 → 효과음 → 사건 → 질문 → 답 기다리기 → 답 대사 → 보상.
// 대사가 끝났다는 사건을 받아야 다음으로 간다. 그래서 말이 겹치지 않고, 끊긴 대사는 넘어가지 않는다.
// 엔진이 비행 중일 때만 부른다. 멈춤과 이어 가기는 엔진이 정하고, 여기서는 지금 대사를 다시 말하기만 한다.
// 시간은 Tick으로만 잰다. 각 단계에 들어간 뒤 처음 받은 Tick부터 센다.
// 신호를 기다리는 동안 멈춘 시간도 기다린 시간에 들어간다(가정). 답을 기다리다 멈추면 질문부터 다시 해서 새로 센다.
// 고른 목적지에 출발 문장이 있으면 첫 임무 전에 한 번 말한다. 장소는 차례가 오기 전에 지나가도 기억했다가 그 임무 차례에 연다.
// 다른 길로 와서 못 지난 장소 임무는 보물 없이 건너뛰고, 도착이 가까우면 남은 중간 장소를 건너뛰어 마지막 장소 임무를 연다.
// 도착 직전 문장 같은 알림은 대사를 끊지 않고, 신호를 기다리거나 임무 사이, 임무를 다 끝내 조용할 때 끼워 넣는다.
// 차 신호 기억(내비 갈림길을 지났는지 포함)과 반응 소리는 CarCues, 답 맞춰 보기는 Answers가 한다.
package com.kidquest.core.engine

import com.kidquest.core.contract.CarSignal
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Mission
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.RoadKind
import com.kidquest.core.contract.TurnSide

internal class MissionRunner(private val plan: TripPlan) {
    private enum class Step { INTRO, NOTICE, BRIEFING, WAIT_TRIGGER, SOUND, EVENT, QUESTION, WAIT_ANSWER, ANSWER_LINE }

    /** 고른 목적지와 나이로 거른 임무. 시작 전에만 바뀐다. */
    private val missions: List<Mission> get() = plan.missions
    private val itemNames get() = plan.episode?.items.orEmpty().associate { it.id to it.name }
    private val cues = CarCues(plan)
    private val endingText get() = plan.episode?.endingLine.orEmpty()

    private var index = 0

    /** null이면 이 임무를 아직 시작하지 않았다. */
    private var step: Step? = null
    private var line: Command.Say? = null

    /** 아이 답 듣기. 부모 동의는 이번 이동이 끝나도 남긴다. */
    private val listen = Listening()

    /** 도착 직전 문장 같은 알림. 조용할 때 끼워 넣는다. */
    private val notice = NoticeSlot()

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
            null -> plan.takeDeparture()?.let { sayLine(Step.INTRO, it) } ?: startMission()
            Step.WAIT_TRIGGER -> waitOrOpen(now)
            Step.SOUND -> sayStep(Step.EVENT, "event", now.event) // 효과음이 끊겼으니 사건 대사부터 한다.
            Step.WAIT_ANSWER -> {
                listen.stop() // 멈출 때 듣기도 멈췄다. 질문을 다 말한 뒤 다시 듣는다.
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

    fun onAnswer(word: String?, fromScreen: Boolean = false): List<Command> {
        val now = mission ?: return emptyList()
        if (step != Step.WAIT_ANSWER || (!fromScreen && !listen.active)) return emptyList()
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

    /** 내비가 알려 준 다음 회전까지 거리. 거리는 멈춘 동안에도 따라가고, 갈림길을 지나면 비행 중일 때만 갈림길 임무를 연다. */
    fun onNextTurn(meters: Int, flying: Boolean): List<Command> {
        if (!cues.passedTurn(meters) || !flying) return emptyList()
        val now = mission
        return if (now != null && step == Step.WAIT_TRIGGER && now.trigger == MissionTrigger.NAVI_TURN) openEvent(now) else emptyList()
    }

    /** 알림을 맡긴다. 비행 중이고 조용하면 바로 말하고, 아니면 조용해질 때 말한다. minor면 맡은 알림이 없을 때만 맡는다. */
    fun announce(say: Command.Say, flying: Boolean, minor: Boolean = false): List<Command> {
        if (minor) notice.offer(say) else notice.hold(say)
        return if (flying) sayNoticeIfQuiet() else emptyList()
    }

    fun onRoad(kind: RoadKind, meters: Int): List<Command> = cues.roadSound(kind, meters, clockMillis)

    /** 장소에 들어왔다. 그 장소 임무가 신호를 기다리는 중이면 열고, 아직 차례가 아니면 기억만 한다. 같은 장소는 한 번만 센다. */
    fun onPlace(placeId: String, flying: Boolean): List<Command> {
        cues.reach(placeId) ?: return emptyList()
        return recheck(flying) // 뒤 장소에 먼저 왔으면 지금 장소 임무를 건너뛴다.
    }

    /** 위치를 못 읽는 차면 장소 임무도, 내비를 못 읽는 차면 갈림길 임무도 시간으로 연다. */
    fun onSignalUnavailable(signal: CarSignal) {
        if (signal == CarSignal.LOCATION) cues.locationMissing = true
        if (signal == CarSignal.NAVIGATION) cues.navigationMissing = true
    }

    /** 도착이 가깝다(남은 시간 5분 이하). 마지막 장소에 온 것으로 보고, 남은 중간 장소는 차례가 오면 건너뛴다. 도착 직전 문장이 먼저 나가게 여기서는 말하지 않는다. */
    fun nearArrival() = cues.reachFinal(missions)

    /** 도착했다. 마지막 장소에 온 것으로 보고, 비행 중에 신호를 기다리고 있었으면 바로 따진다. */
    fun onArrived(flying: Boolean): List<Command> {
        cues.reachFinal(missions)
        return recheck(flying)
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
            Step.WAIT_TRIGGER -> if (cues.openAfterSeconds(now)?.let { waited >= it * 1_000L } == true) openEvent(now, byTime = true) else emptyList()
            Step.SOUND -> if (waited >= EVENT_DELAY_SECONDS * 1_000L) sayStep(Step.EVENT, "event", now.event) else emptyList()
            Step.WAIT_ANSWER -> answerTimeout(now, waited)
            else -> emptyList()
        }
    }

    fun setListeningAllowed(allowed: Boolean): List<Command> {
        listen.allowed = allowed
        if (allowed || !listen.active) return emptyList()
        listen.stop()
        stepStartedAt = null
        return listOf(Command.StopAudio)
    }

    /** 이번 이동의 기록만 지운다. */
    fun reset() {
        index = 0
        plan.newTrip()
        notice.reset()
        cues.reset()
        clearStep()
    }

    private fun sayStep(next: Step, part: String, text: String) = sayLine(next, Command.Say("${mission?.id}/$part", text))

    /** 다음 단계로 들어가 대사 하나를 말한다. 이 대사가 끝났다는 사건을 받아야 다음으로 간다. */
    private fun sayLine(next: Step, say: Command.Say): List<Command> {
        enter(next)
        line = say
        return listOf(say)
    }

    private fun enter(next: Step) {
        step = next
        line = null
        stepStartedAt = null
    }

    /** 임무를 시작한다. 다른 길로 와서 못 지난 장소 임무는 보물 없이 건너뛴다. */
    private fun startMission(): List<Command> {
        while (cues.missed(missions, index)) index++
        val now = mission ?: return emptyList()
        return sayStep(Step.BRIEFING, "briefing", now.briefing)
    }

    /** 신호가 오거나 시간이 지나면 연다. 신호를 못 읽는 차도 시간이 지나면 열린다(장소 임무는 위치를 못 읽을 때만). */
    private fun waitForTrigger(now: Mission): List<Command> {
        enter(Step.WAIT_TRIGGER)
        return waitOrOpen(now)
    }

    /** 차 신호가 바뀌었다. 비행 중에 신호를 기다리고 있으면 지금 임무를 다시 따진다. */
    private fun recheck(flying: Boolean): List<Command> {
        val now = mission
        return if (flying && now != null && step == Step.WAIT_TRIGGER) waitOrOpen(now) else emptyList()
    }

    /** 신호를 기다리는 중에 따진다. 신호가 이미 와 있으면 열고, 미뤄 둔 알림이 있으면 먼저 말하고, 못 지난 장소면 건너뛴다. */
    private fun waitOrOpen(now: Mission): List<Command> = when {
        cues.active(now) -> openEvent(now)
        notice.pending != null -> sayNoticeIfQuiet()
        cues.missed(missions, index) -> {
            clearStep()
            startMission()
        }
        else -> emptyList()
    }

    /** 알림을 끼워 넣어도 되는 때만 말한다. 신호를 기다리는 중이거나 임무를 다 끝내 조용할 때다. */
    private fun sayNoticeIfQuiet(): List<Command> = when {
        step == Step.WAIT_TRIGGER -> sayNotice(waiting = true)
        allDone && step == null -> sayNotice(waiting = false)
        else -> emptyList()
    }

    /** 알림을 말한다. 신호를 기다리던 중이었으면 기다린 시간을 남겨 둔다. */
    private fun sayNotice(waiting: Boolean): List<Command> =
        notice.take(waiting, stepStartedAt)?.let { sayLine(Step.NOTICE, it) }.orEmpty()

    /** 알림이 끝나면 하던 일로 돌아간다. 신호를 기다리던 중이면 기다린 시간을 이어서 세고 그사이 온 신호를 바로 연다. 임무 사이였으면 다음 임무를 시작한다. */
    private fun afterNotice(): List<Command> {
        val now = mission
        if (!notice.fromWait || now == null) {
            clearStep()
            return speakCurrent()
        }
        enter(Step.WAIT_TRIGGER)
        stepStartedAt = notice.waitStartedAt
        return waitOrOpen(now)
    }

    /** 효과음을 먼저 내고 몇 초 뒤에 사건을 말한다. 효과음이 없으면 바로 말한다. 신호 없이 시간으로 열 때 맑은 날 대사가 있으면 효과음 없이 그 대사를 말한다. */
    private fun openEvent(now: Mission, byTime: Boolean = false): List<Command> {
        if (byTime && now.fallbackEvent.isNotBlank()) return sayStep(Step.EVENT, "event", now.fallbackEvent)
        val sound = now.eventSoundId ?: return sayStep(Step.EVENT, "event", now.event)
        enter(Step.SOUND)
        return listOf(Command.PlaySound(sound))
    }

    private fun waitForAnswer(now: Mission): List<Command> {
        enter(Step.WAIT_ANSWER)
        return listen.start(now.answers)
    }

    /** 답을 기다릴 시간이 지나면 정답을 말해 준다. 듣는 중이었으면 듣기를 먼저 멈춘다. */
    private fun answerTimeout(now: Mission, waited: Long): List<Command> {
        if (!listen.timedOut(waited)) return emptyList()
        val stop = if (listen.active) listOf(Command.StopAudio) else emptyList()
        return stop + sayAnswer(now, null)
    }

    /** 못 알아들었거나 틀려도 혼내지 않고 정답을 말해 준다. */
    private fun sayAnswer(now: Mission, word: String?): List<Command> {
        listen.stop()
        return sayStep(Step.ANSWER_LINE, "answer", if (Answers.isCorrect(now, word)) now.correctLine else now.wrongLine)
    }

    private fun completeMission(now: Mission): List<Command> {
        val award = Command.AwardItem(now.rewardItem, itemNames[now.rewardItem] ?: now.rewardItem)
        index++
        clearStep()
        return listOf(award) + sayNotice(waiting = false).ifEmpty { speakCurrent() } // 미뤄 둔 알림은 다음 임무 전에 말한다.
    }

    private fun clearStep() {
        step = null
        line = null
        listen.stop()
        stepStartedAt = null
    }

    private companion object {
        /** 효과음 뒤 사건 대사까지 기다리는 시간. 정해진 값이 없어서 3초로 둔다(가정). */
        const val EVENT_DELAY_SECONDS = 3
    }
}
