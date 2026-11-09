// 앱에 들어가는 예시 파일로 목적지와 경로마다 처음부터 착륙까지 돌려 본다.
// 장소 id, 정답, 보상 이름이 파일 안에서 엇갈리면 임무가 안 열리거나 오답으로 넘어가는데, 앱은 꺼지지 않아서 눈치채기 어렵다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.Place
import com.kidquest.core.contract.RearSeat
import com.kidquest.core.contract.TurnSide
import com.kidquest.core.episode.EpisodeResult
import com.kidquest.core.episode.readEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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

    /** 고른 목적지와 경로로 끝까지 간다. 받은 보상 이름과 착륙 때 말한 문장을 돌려준다. */
    private fun play(destinationId: String?, routeId: String? = null): Pair<List<String>, List<String>> {
        engine = MissionEngine(episode)
        clock = 0
        send(AppEvent.ListeningConsentChanged(true), AppEvent.DestinationSelected(destinationId, routeId), AppEvent.StartPressed)
        send(AppEvent.SeatOccupied(RearSeat.LEFT, true), AppEvent.BeltBuckled(RearSeat.LEFT, true), AppEvent.GearChanged(Gear.D))
        val destination = episode.destinations.firstOrNull { it.id == destinationId }
        val route = destination?.routes?.firstOrNull { it.id == routeId }
        val afterTakeOff = finish(Lines.TAKE_OFF.lineId)
        if (destination != null) {
            assertTrue("${destination.id} 경로", route != null)
            assertEquals(listOf(route!!.departureLine.ifBlank { destination.departureLine }), said(afterTakeOff))
            finish(TripPlan.DEPARTURE_ID)
        }
        val routed = episode.destinations.flatMap { it.routes }.flatMap { it.missionIds }.toSet()
        val common = episode.missions.filter { it.destinationId == null && it.id !in routed }
        val trip = common + route?.missionIds.orEmpty().map { id -> episode.missions.single { it.id == id } }
        val awards = mutableListOf<String>()
        trip.forEach { mission ->
            finish("${mission.id}/briefing")
            val opened = when (mission.trigger) {
                MissionTrigger.WIPER -> send(AppEvent.WiperChanged(true)) + tick(0) + tick(5)
                MissionTrigger.TURN_SIGNAL -> send(AppEvent.TurnSignalOn(TurnSide.LEFT))
                MissionTrigger.NAVI_TURN -> send(AppEvent.NextTurnDistance(200), AppEvent.NextTurnDistance(1_500))
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
    fun `목적지와 경로마다 출발부터 착륙까지 모든 임무가 열리고 정답을 알아듣는다`() {
        assertEquals("목적지 수", 4, episode.destinations.size)
        assertEquals("경로 수", 5, episode.destinations.sumOf { it.routes.size })
        episode.destinations.forEach { destination ->
            destination.routes.forEach { route ->
                val (awards, landing) = play(destination.id, route.id)
                assertEquals("${route.id} 보상 수", 2 + route.missionIds.size, awards.size)
                assertEquals(
                    route.id,
                    listOf(episode.endingLine, destination.deliveryLine, destination.landingFact, Lines.LANDED.text),
                    landing,
                )
            }
        }
    }

    @Test
    fun `경로의 장소는 가는 순서대로 목적지에 가까워진다`() {
        // 경로에 도시 순서를 거꾸로 적으면 첫 도시를 못 지난 것으로 보고 건너뛴다.
        episode.destinations.flatMap { it.routes }.forEach { route ->
            val points = route.missionIds.map { id -> episode.places.single { it.missionId == id } }
            val distances = points.map { km(it, points.last()) }
            assertTrue("${route.id} : $distances", distances.zipWithNext().all { (before, after) -> before > after })
        }
    }

    /** 두 좌표 사이의 거리(km). 지구를 공으로 본다. */
    private fun km(a: Place, b: Place): Double {
        val rad = Math.PI / 180
        val h = sin((b.latitude - a.latitude) * rad / 2).pow(2) +
            cos(a.latitude * rad) * cos(b.latitude * rad) * sin((b.longitude - a.longitude) * rad / 2).pow(2)
        return 2 * 6_371 * asin(sqrt(h))
    }

    @Test
    fun `목적지를 안 고르면 공통 임무만 하고 전달 문장 없이 착륙한다`() {
        val (awards, landing) = play(null)
        assertEquals(2, awards.size)
        assertEquals(listOf(episode.endingLine, Lines.LANDED.text), landing)
    }
}
