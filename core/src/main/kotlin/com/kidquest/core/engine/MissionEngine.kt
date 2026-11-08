// 앱의 상태 흐름을 정한다. 대기 → 승무원 확인 → 발사 준비 → 비행 ⇄ 멈춤, 비행 ⇄ 정박 → 착륙.
// 앱은 차를 막지 않는다. 벨트가 안 매져 있으면 이륙하지 않고 안내만 한다.
// 임무 진행(브리핑, 질문, 답, 보상)은 MissionRunner가 한다. 비행 중일 때만 넘겨서 멈춘 동안에는 진행하지 않는다.
// 탑승과 벨트 값은 Crew가, 이번 이동에 쓸 임무와 목적지 문장은 TripPlan이 기억한다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.Screen

class MissionEngine(episode: Episode? = null) {
    var state: EngineState = EngineState.WAITING
        private set

    private val plan = TripPlan(episode)
    private val missions = MissionRunner(episode, plan)
    private val crew = Crew()

    /** 이 대사가 끝나면 임무를 말한다. 이륙 대사나 미룬 대사와 겹치지 않게 한다. */
    private var continueAfterLine: String? = null

    private val pauseReasons = mutableSetOf<PauseReason>()
    private var gear = Gear.P
    private var arrived = false
    private var readyAnnounced = false
    private var beltGuides = 0

    /** 내비 안내 때문에 못 한 대사. 소리를 다시 낼 수 있을 때 말한다. */
    private var heldLine: Command.Say? = null

    /** 착륙 때 차례로 말할 대사. 앞 대사가 끝나면 다음 대사를 말한다. */
    private var landingLines: List<Command.Say> = emptyList()

    /** 사건 하나를 받아 상태를 바꾸고, 음성과 화면에 시킬 명령을 돌려준다. */
    fun onEvent(event: AppEvent): List<Command> = when (event) {
        AppEvent.StartPressed -> onStart()
        is AppEvent.DestinationSelected -> beforeStart { plan.selectDestination(event.destinationId) }
        is AppEvent.AgeSelected -> beforeStart { plan.selectAge(event.group) }
        is AppEvent.SeatOccupied -> if (crew.seat(event.seat, event.occupied)) afterCrewChange() else emptyList()
        is AppEvent.BeltBuckled -> if (crew.belt(event.seat, event.buckled)) afterCrewChange() else emptyList()
        AppEvent.ParentConfirmedBelt -> onParentConfirmed()
        is AppEvent.GearChanged -> onGear(event.gear)
        is AppEvent.AudioFocusChanged -> setPause(PauseReason.AUDIO_FOCUS, !event.hasFocus)
        is AppEvent.ParentStopChanged -> setPause(PauseReason.PARENT, event.stopped)
        AppEvent.DestinationArrived -> onArrived()
        AppEvent.EndTripPressed -> if (state == EngineState.DOCKED) land() else emptyList()
        AppEvent.RestartPressed -> onRestart()
        is AppEvent.LineFinished -> when (state) {
            EngineState.FLYING -> onLineFinished(event.lineId)
            EngineState.LANDED -> nextLandingLine(event.lineId)
            else -> emptyList()
        }
        is AppEvent.AnswerHeard -> whileFlying { missions.onAnswer(event.word) }
        is AppEvent.Tick -> whileFlying { missions.onTick(event.nowMillis) }
        is AppEvent.ListeningConsentChanged -> missions.setListeningAllowed(event.allowed)
        is AppEvent.WiperChanged -> missions.onWiper(event.on, state == EngineState.FLYING)
        is AppEvent.TurnSignalOn -> whileFlying { missions.onTurnSignal(event.side) }
        is AppEvent.PlaceEntered -> missions.onPlace(event.placeId, state == EngineState.FLYING)
        else -> emptyList() // 에너지, 실내 온도, 남은 시간은 #10에서 받는다. 못 읽는 신호는 시간이 지나면 임무가 열려서 따로 안 받는다.
    }

    private fun whileFlying(action: () -> List<Command>): List<Command> =
        if (state == EngineState.FLYING) action() else emptyList()

    /** 고른 값은 시작 전에만 받는다. 이동 중에 임무 목록이 바뀌면 진행 중인 임무가 사라진다. */
    private fun beforeStart(action: () -> Unit): List<Command> {
        if (state == EngineState.WAITING) action()
        return emptyList()
    }

    private fun onLineFinished(lineId: String): List<Command> {
        if (lineId != continueAfterLine) return missions.onLineFinished(lineId)
        continueAfterLine = null
        return missions.speakCurrent()
    }

    /** 다시 비행으로 돌아왔다. 먼저 말할 대사가 있으면 그 대사가 끝난 뒤에 임무를 잇는다. */
    private fun continueMissions(spoken: List<Command>): List<Command> {
        val line = spoken.filterIsInstance<Command.Say>().lastOrNull()
        continueAfterLine = line?.lineId
        return if (line == null) missions.resume() else emptyList()
    }

    private fun onStart(): List<Command> {
        if (state != EngineState.WAITING) return emptyList()
        state = EngineState.CREW_CHECK
        val show = listOf(Command.ShowScreen(Screen.CREW_CHECK))
        return if (crew.present()) show + enterReady() else show
    }

    private fun onParentConfirmed(): List<Command> {
        if (state == EngineState.WAITING || state == EngineState.LANDED) return emptyList()
        crew.parentConfirmed()
        return afterCrewChange()
    }

    /** 탑승과 벨트 값이 바뀌면 지금 상태에서 다시 따져 본다. */
    private fun afterCrewChange(): List<Command> = when (state) {
        EngineState.CREW_CHECK -> if (crew.present()) enterReady() else emptyList()
        EngineState.READY -> if (crew.present()) tryTakeOff() else backToCrewCheck()
        EngineState.FLYING, EngineState.PAUSED -> setPause(PauseReason.BELT, !crew.ready())
        else -> emptyList()
    }

    /** 기어가 이미 D면 바로 발사하거나 승무원 확인만 말한다. 대사 두 개를 한꺼번에 보내면 앞 대사가 끊기거나 겹친다. */
    private fun enterReady(): List<Command> {
        state = EngineState.READY
        val already = crew.ready()
        readyAnnounced = already
        if (gear == Gear.D) return if (already) takeOff() else speak(Lines.CREW_FOUND)
        return speak(if (already) Lines.ALREADY_READY else Lines.CREW_FOUND)
    }

    private fun backToCrewCheck(): List<Command> {
        state = EngineState.CREW_CHECK
        readyAnnounced = false
        return listOf(Command.ShowScreen(Screen.CREW_CHECK))
    }

    private fun onGear(newGear: Gear): List<Command> {
        if (newGear == gear) return emptyList()
        gear = newGear
        return when (state) {
            EngineState.READY -> if (gear == Gear.D) tryTakeOff() else emptyList()
            EngineState.FLYING, EngineState.PAUSED ->
                if (gear == Gear.P) parkOrLand() else setPause(PauseReason.GEAR, gear != Gear.D)
            EngineState.DOCKED -> if (gear == Gear.D) undock() else emptyList()
            else -> emptyList()
        }
    }

    /** 주차장에서 R을 거쳐 D로 가도 이륙한다. 기어를 먼저 넣었으면 벨트를 맨 순간 이륙한다. */
    private fun tryTakeOff(): List<Command> {
        if (!crew.ready()) readyAnnounced = false
        return when {
            gear == Gear.D && crew.ready() -> takeOff()
            gear == Gear.D -> beltGuide(Lines.NEED_BELT)
            crew.ready() && !readyAnnounced -> {
                readyAnnounced = true
                speak(Lines.READY)
            }
            else -> emptyList()
        }
    }

    /** 내비 안내 중이거나 부모가 껐으면 그 이유는 남겨 두고, 소리가 나도 될 때 이어진다. */
    private fun takeOff(): List<Command> {
        pauseReasons -= setOf(PauseReason.BELT, PauseReason.GEAR)
        state = if (pauseReasons.isEmpty()) EngineState.FLYING else EngineState.PAUSED
        val screen = if (state == EngineState.FLYING) flyingScreen() else pausedScreen()
        val spoken = speak(Lines.TAKE_OFF)
        continueAfterLine = if (state == EngineState.FLYING) Lines.TAKE_OFF.lineId else null
        return spoken + screen
    }

    /** 멈춘 이유를 더하거나 뺀다. 이유가 다 없어져야 이어진다. */
    private fun setPause(reason: PauseReason, on: Boolean): List<Command> {
        val changed = if (on) pauseReasons.add(reason) else pauseReasons.remove(reason)
        if (!changed) return emptyList()
        if (reason == PauseReason.BELT && !on && heldLine == Lines.BELT_OFF) heldLine = null

        val wasFlying = state == EngineState.FLYING
        val screen = when {
            wasFlying && pauseReasons.isNotEmpty() -> {
                state = EngineState.PAUSED
                pausedScreen()
            }
            state == EngineState.PAUSED && pauseReasons.isEmpty() -> {
                state = EngineState.FLYING
                flyingScreen()
            }
            state == EngineState.PAUSED -> pausedScreen()
            else -> null
        }
        val stop = on && (wasFlying || reason == PauseReason.AUDIO_FOCUS || reason == PauseReason.PARENT)
        val guide = if (reason == PauseReason.BELT && on && state == EngineState.PAUSED) beltGuide(Lines.BELT_OFF) else emptyList()
        val held = if (reason == PauseReason.AUDIO_FOCUS && !on) releaseHeldLine() else emptyList()
        val next = if (!wasFlying && state == EngineState.FLYING) continueMissions(held) else emptyList()
        return listOfNotNull(Command.StopAudio.takeIf { stop }, screen) + guide + held + next
    }

    private fun flyingScreen() = Command.ShowScreen(Screen.FLYING, Lines.STATUS_FLYING)

    private fun pausedScreen(): Command.ShowScreen {
        val status = when {
            PauseReason.PARENT in pauseReasons -> Lines.STATUS_STOPPED
            PauseReason.BELT in pauseReasons -> Lines.STATUS_BELT
            PauseReason.GEAR in pauseReasons -> Lines.STATUS_GEAR
            else -> Lines.STATUS_NAVI
        }
        return Command.ShowScreen(Screen.PAUSED, status)
    }

    /** 부모가 껐으면 말하지 않고, 내비 안내 중이면 끝날 때까지 미룬다. 미룬 대사는 마지막 하나만 남긴다. */
    private fun speak(line: Command.Say): List<Command> = when {
        PauseReason.PARENT in pauseReasons -> emptyList()
        PauseReason.AUDIO_FOCUS in pauseReasons -> {
            heldLine = line
            emptyList()
        }
        else -> listOf(line)
    }

    private fun releaseHeldLine(): List<Command> {
        val line = heldLine ?: return emptyList()
        heldLine = null
        return speak(line)
    }

    /** 벨트 안내는 한 이동에 세 번까지만 한다(F09). */
    private fun beltGuide(line: Command.Say): List<Command> {
        if (beltGuides >= MAX_BELT_GUIDES) return emptyList()
        beltGuides++
        return speak(line)
    }

    /** 도착했거나 임무를 다 끝냈으면 착륙하고, 아니면 휴게소일 수 있으니 잠깐 정박한다. */
    private fun parkOrLand(): List<Command> = if (arrived || missions.allDone) land() else dock()

    private fun dock(): List<Command> {
        state = EngineState.DOCKED
        pauseReasons -= setOf(PauseReason.BELT, PauseReason.GEAR)
        return listOf(Command.StopAudio) + speak(Lines.DOCKED) +
            Command.ShowScreen(Screen.DOCKED, Lines.STATUS_DOCKED)
    }

    /** 정박 중에 벨트를 풀었을 수 있으니 출발할 때 다시 본다. */
    private fun undock(): List<Command> {
        state = EngineState.PAUSED
        if (!crew.ready()) pauseReasons += PauseReason.BELT
        if (pauseReasons.isEmpty()) {
            state = EngineState.FLYING
            return listOf(flyingScreen()) + continueMissions(emptyList())
        }
        val guide = if (PauseReason.BELT in pauseReasons) beltGuide(Lines.BELT_OFF) else emptyList()
        return listOf(pausedScreen()) + guide
    }

    private fun onArrived(): List<Command> {
        arrived = true
        return if (state == EngineState.DOCKED) land() else emptyList()
    }

    /**
     * 결말(임무를 다 끝냈을 때) → 목적지 전달 문장과 실제 사실(도착했거나 다 끝냈을 때) → 착륙 대사 순서로 말한다.
     * 휴게소에서 부모가 끝내기를 누르면 도착하지 않았으니 전달 문장은 말하지 않는다.
     */
    private fun land(): List<Command> {
        state = EngineState.LANDED
        pauseReasons.retainAll(setOf(PauseReason.AUDIO_FOCUS, PauseReason.PARENT))
        val arrivalLines = if (arrived || missions.allDone) plan.arrivalLines() else emptyList()
        landingLines = listOfNotNull(missions.ending()) + arrivalLines + Lines.LANDED
        return listOf(Command.StopAudio) + speak(landingLines.first()) + Command.ShowScreen(Screen.LANDED)
    }

    private fun nextLandingLine(lineId: String): List<Command> {
        val at = landingLines.indexOfFirst { it.lineId == lineId }
        if (at < 0) return emptyList()
        return landingLines.getOrNull(at + 1)?.let { speak(it) }.orEmpty()
    }

    /** 몸과 차의 값(탑승, 벨트, 기어, 포커스)과 고른 목적지는 그대로 두고, 이번 이동의 기록만 지운다. */
    private fun onRestart(): List<Command> {
        if (state != EngineState.LANDED) return emptyList()
        state = EngineState.WAITING
        crew.clearTrip()
        pauseReasons -= PauseReason.PARENT
        arrived = false
        readyAnnounced = false
        beltGuides = 0
        heldLine = null
        continueAfterLine = null
        landingLines = emptyList()
        missions.reset()
        return listOf(Command.ShowScreen(Screen.START))
    }

    private companion object {
        const val MAX_BELT_GUIDES = 3
    }
}
