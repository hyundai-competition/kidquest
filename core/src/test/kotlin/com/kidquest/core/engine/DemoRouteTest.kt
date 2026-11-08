// 앱에 들어가는 예시 파일로 목적지마다 처음부터 착륙까지 돌려 본다.
// 장소 id, 정답, 보상 이름이 파일 안에서 엇갈리면 임무가 안 열리거나 오답으로 넘어가는데, 앱은 꺼지지 않아서 눈치채기 어렵다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.RearSeat
import com.kidquest.core.contract.TurnSide
import com.kidquest.core.episode.EpisodeResult
import com.kidquest.core.episode.readEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DemoRouteTest {
    /** 시험은 core 폴더에서 돈다. */
    private val episode: Episode =
        (readEpisode(File("../app/src/main/assets/episodes/episode_demo.json").readText()) as EpisodeResult.Ok).episode

    private lateinit var engine: MissionEngine
    private var clock = 0L

    private fun send(vararg events: AppEvent): List<Command> = events.flatMap { engine.onEvent(it) }
    private fun finish(lineId: String) = send(AppEvent.LineFinished(lineId))
    private fun said(commands: List<Command>) = commands.filterIsInstance<Command.Say>().map { it.text }
    private fun tick(seconds: Long): List<Command> {
        clock += seconds * 1_000
        return send(AppEvent.Tick(clock))
    }

    /** 고른 목적지로 끝까지 간다. 받은 보상 이름과 착륙 때 말한 문장을 돌려준다. */
    private fun play(destinationId: String?): Pair<List<String>, List<String>> {
        engine = MissionEngine(episode)
        clock = 0
        send(AppEvent.ListeningConsentChanged(true), AppEvent.DestinationSelected(destinationId), AppEvent.StartPressed)
        send(AppEvent.SeatOccupied(RearSeat.LEFT, true), AppEvent.BeltBuckled(RearSeat.LEFT, true), AppEvent.GearChanged(Gear.D))
        val destination = episode.destinations.firstOrNull { it.id == destinationId }
        val afterTakeOff = finish(Lines.TAKE_OFF.lineId)
        if (destination != null) {
            assertEquals(listOf(destination.departureLine), said(afterTakeOff))
            finish(TripPlan.DEPARTURE_ID)
        }
        val trip = episode.missions.filter { it.destinationId == null || it.destinationId == destinationId }
        val awards = mutableListOf<String>()
        trip.forEach { mission ->
            finish("${mission.id}/briefing")
            val opened = when (mission.trigger) {
                MissionTrigger.WIPER -> send(AppEvent.WiperChanged(true)) + tick(0) + tick(5)
                MissionTrigger.TURN_SIGNAL -> send(AppEvent.TurnSignalOn(TurnSide.LEFT))
                MissionTrigger.PLACE -> send(AppEvent.PlaceEntered(episode.places.single { it.missionId == mission.id }.id))
                else -> tick(0) + tick(mission.autoAfterSeconds.toLong() + 5)
            }
            assertEquals("${mission.id} 사건", listOf(mission.event), said(opened))
            finish("${mission.id}/event")
            finish("${mission.id}/question")
            assertEquals("${mission.id} 정답", listOf(mission.correctLine), said(send(AppEvent.AnswerHeard(mission.answers.first()))))
            awards += finish("${mission.id}/answer").filterIsInstance<Command.AwardItem>().map { it.name }
        }
        val landing = mutableListOf<String>()
        var spoken = send(AppEvent.GearChanged(Gear.P)).filterIsInstance<Command.Say>()
        while (spoken.isNotEmpty()) {
            landing += spoken.map { it.text }
            spoken = finish(spoken.last().lineId).filterIsInstance<Command.Say>()
        }
        assertEquals(EngineState.LANDED, engine.state)
        return awards to landing
    }

    @Test
    fun `목적지마다 출발부터 착륙까지 모든 임무가 열리고 정답을 알아듣는다`() {
        assertTrue("목적지가 4곳이어야 한다", episode.destinations.size == 4)
        episode.destinations.forEach { destination ->
            val (awards, landing) = play(destination.id)
            assertEquals("${destination.id} 보상 수", 3, awards.size)
            assertEquals(
                destination.id,
                listOf(episode.endingLine, destination.deliveryLine, destination.landingFact, Lines.LANDED.text),
                landing,
            )
        }
    }

    @Test
    fun `목적지를 안 고르면 공통 임무만 하고 전달 문장 없이 착륙한다`() {
        val (awards, landing) = play(null)
        assertEquals(2, awards.size)
        assertEquals(listOf(episode.endingLine, Lines.LANDED.text), landing)
    }
}
