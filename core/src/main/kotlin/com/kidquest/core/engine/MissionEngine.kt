// 앱의 상태 흐름을 정한다. 대기 → 승무원 확인 → 발사 준비 → 비행 ⇄ 멈춤 → 착륙.
// 앱은 차를 막지 않는다. 벨트가 안 매져 있으면 이륙하지 않고 안내만 한다.
// 임무 진행(브리핑, 질문, 답, 보상)은 아직 없다. 판단 로직 역할이 이어서 만든다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.RearSeat
import com.kidquest.core.contract.Screen

class MissionEngine {
    var state: EngineState = EngineState.WAITING
        private set

    private val occupied = mutableSetOf<RearSeat>()
    private val buckled = mutableSetOf<RearSeat>()
    private val pauseReasons = mutableSetOf<PauseReason>()
    private var parentConfirmed = false
    private var gear = Gear.P

    /** 사건 하나를 받아 상태를 바꾸고, 음성과 화면에 시킬 명령을 돌려준다. */
    fun onEvent(event: AppEvent): List<Command> = when (event) {
        AppEvent.StartPressed -> onStart()
        is AppEvent.SeatOccupied -> onSeat(event)
        is AppEvent.BeltBuckled -> onBelt(event)
        AppEvent.ParentConfirmedBelt -> onParentConfirmed()
        is AppEvent.GearChanged -> onGear(event.gear)
        is AppEvent.AudioFocusChanged -> setPause(PauseReason.AUDIO_FOCUS, !event.hasFocus)
        AppEvent.RestartPressed -> onRestart()
        else -> emptyList() // 임무 진행을 만들 때 나머지 사건을 받는다.
    }

    /** 탑승한 뒷좌석의 벨트가 모두 매졌나. 카시트처럼 값이 안 바뀌면 부모 확인으로 대신한다. */
    private fun beltsReady(): Boolean =
        occupied.isNotEmpty() && (parentConfirmed || buckled.containsAll(occupied))

    private fun onStart(): List<Command> {
        if (state != EngineState.WAITING) return emptyList()
        state = EngineState.CREW_CHECK
        val show = listOf(Command.ShowScreen(Screen.CREW_CHECK))
        return if (occupied.isEmpty()) show else show + enterReady()
    }

    private fun onSeat(event: AppEvent.SeatOccupied): List<Command> {
        if (event.occupied) occupied += event.seat else occupied -= event.seat
        return if (state == EngineState.CREW_CHECK && occupied.isNotEmpty()) enterReady() else emptyList()
    }

    private fun enterReady(): List<Command> {
        state = EngineState.READY
        return listOf(Command.Say(Lines.CREW_FOUND)) + tryTakeOff()
    }

    private fun onBelt(event: AppEvent.BeltBuckled): List<Command> {
        if (event.buckled) buckled += event.seat else buckled -= event.seat
        return afterBeltChange()
    }

    private fun onParentConfirmed(): List<Command> {
        parentConfirmed = true
        return afterBeltChange()
    }

    private fun afterBeltChange(): List<Command> = when (state) {
        EngineState.READY -> tryTakeOff()
        EngineState.FLYING, EngineState.PAUSED -> setPause(PauseReason.BELT, !beltsReady())
        else -> emptyList()
    }

    private fun onGear(newGear: Gear): List<Command> {
        gear = newGear
        return when (state) {
            EngineState.READY -> if (gear == Gear.D) tryTakeOff() else emptyList()
            EngineState.FLYING, EngineState.PAUSED ->
                if (gear == Gear.P) land() else setPause(PauseReason.GEAR, gear != Gear.D)
            else -> emptyList()
        }
    }

    /** 주차장에서 R을 거쳐 D로 가도 이륙한다. 기어를 먼저 넣었으면 벨트를 맨 순간 이륙한다. */
    private fun tryTakeOff(): List<Command> = when {
        gear == Gear.D && beltsReady() -> {
            state = EngineState.FLYING
            pauseReasons.clear()
            listOf(Command.Say(Lines.TAKE_OFF), Command.ShowScreen(Screen.FLYING))
        }
        gear == Gear.D -> listOf(Command.Say(Lines.NEED_BELT))
        beltsReady() -> listOf(Command.Say(Lines.READY))
        else -> emptyList()
    }

    /** 멈춘 이유를 더하거나 뺀다. 이유가 다 없어져야 이어진다. */
    private fun setPause(reason: PauseReason, on: Boolean): List<Command> {
        if (on) pauseReasons += reason else pauseReasons -= reason
        return when {
            state == EngineState.FLYING && pauseReasons.isNotEmpty() -> {
                state = EngineState.PAUSED
                val stop = listOf(Command.StopAudio, Command.ShowScreen(Screen.PAUSED))
                if (reason == PauseReason.BELT) stop + Command.Say(Lines.BELT_OFF) else stop
            }
            state == EngineState.PAUSED && pauseReasons.isEmpty() -> {
                state = EngineState.FLYING
                listOf(Command.ShowScreen(Screen.FLYING))
            }
            else -> emptyList()
        }
    }

    private fun land(): List<Command> {
        state = EngineState.LANDED
        pauseReasons.clear()
        return listOf(Command.StopAudio, Command.Say(Lines.LANDED), Command.ShowScreen(Screen.LANDED))
    }

    private fun onRestart(): List<Command> {
        if (state != EngineState.LANDED) return emptyList()
        state = EngineState.WAITING
        parentConfirmed = false
        return listOf(Command.ShowScreen(Screen.START))
    }
}
