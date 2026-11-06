// 임무 진행 시험에서 같이 쓰는 에피소드와 사건 보내기.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.Item
import com.kidquest.core.contract.Mission
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.RearSeat

abstract class MissionTestBase {
    protected fun mission(id: String, sound: String? = "meteor", reward: String = "shield") = Mission(
        id = id,
        ageGroups = emptyList(),
        destinationId = null,
        trigger = MissionTrigger.WIPER,
        autoAfterSeconds = 120,
        briefing = "$id 브리핑",
        eventSoundId = sound,
        event = "$id 사건",
        question = "$id 질문",
        answers = listOf("초록색", "초록"),
        correctLine = "$id 맞아요",
        wrongLine = "$id 정답은 초록색",
        rewardItem = reward,
    )

    protected fun episode(vararg missions: Mission) = Episode(
        id = "test",
        title = "시험",
        destinations = emptyList(),
        places = emptyList(),
        missions = missions.toList(),
        items = listOf(Item("shield", "방어막 조각"), Item("map", "지도 조각")),
        reactions = emptyList(),
        finalLine = "마지막",
    )

    protected var engine = MissionEngine(episode(mission("a")))

    protected fun send(vararg events: AppEvent): List<Command> = events.flatMap { engine.onEvent(it) }

    protected fun finish(lineId: String) = send(AppEvent.LineFinished(lineId))

    protected fun tick(seconds: Long) = send(AppEvent.Tick(seconds * 1_000))

    protected fun said(commands: List<Command>) = commands.filterIsInstance<Command.Say>().map { it.text }

    protected fun buckle(on: Boolean) = send(AppEvent.BeltBuckled(RearSeat.LEFT, on))

    /** 이륙까지 간다. 이륙 대사는 아직 안 끝났다. */
    protected fun takeOff(): List<Command> {
        send(AppEvent.StartPressed, AppEvent.SeatOccupied(RearSeat.LEFT, true))
        buckle(true)
        return send(AppEvent.GearChanged(Gear.D))
    }

    /** 브리핑 대사를 말하는 데까지 간다. */
    protected fun untilBriefing(): List<Command> {
        takeOff()
        return finish(Lines.TAKE_OFF.lineId)
    }

    /** 질문까지 다 말해서 답을 기다리는 데까지 간다. */
    protected fun untilAnswer(consent: Boolean = true): List<Command> {
        send(AppEvent.ListeningConsentChanged(consent))
        untilBriefing()
        finish("a/briefing")
        finish("a/event")
        return finish("a/question")
    }
}
