// 시연 운전기를 실제 엔진·한국어/영어 대본과 붙인다. 음성 완료만 시험 대역이며 시연기는 이를 만들지 않는다.
package com.kidquest.app.replay

import com.kidquest.app.screen.model.EnginePresentation
import com.kidquest.app.screen.model.TripCatalog
import com.kidquest.core.contract.*
import com.kidquest.core.engine.MissionEngine
import com.kidquest.core.episode.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ShowcaseRunnerTest {
    private fun episode(english: Boolean = false) = (readEpisode(File("src/main/assets/episodes/episode_demo${if (english) "_en" else ""}.json").readText()) as EpisodeResult.Ok).episode

    @Test fun everyRouteCompletesThroughEngineWithoutManufacturedAnswersOrRewards() {
        for (english in listOf(false, true)) {
            val episode = episode(english)
            episode.destinations.forEach { destination -> destination.routes.forEach { route ->
                val fixture = Trip(episode, destination.id, route.id)
                repeat(1000) { fixture.step() }
                assertTrue("${episode.id}/${route.id}", fixture.runner.finished)
                assertEquals(Screen.LANDED, fixture.screen)
                assertEquals(fixture.plan.map { it.rewardItem }.toSet(), fixture.items)
                assertEquals("landed", fixture.lastFinished)
                assertFalse(fixture.generated.any { it is AppEvent.LineFinished || it is AppEvent.AnswerSelected || it is AppEvent.AnswerHeard })
                assertEquals(fixture.plan.count { it.trigger == MissionTrigger.PLACE }, fixture.generated.filterIsInstance<AppEvent.PlaceEntered>().size)
            } }
        }
    }

    @Test fun nextWaitsForNarrationAndDoesNotRepeatLocationEntry() {
        val episode = episode()
        val mission = episode.missions.first { it.trigger == MissionTrigger.PLACE }
        val events = mutableListOf<AppEvent>()
        val runner = ShowcaseRunner(listOf(mission), episode.places, 0, events::add)
        val state = ShowcaseSnapshot(Screen.FLYING, mission.id, "briefing", "", false, 0)
        runner.next()
        runner.tick(state)
        assertTrue(events.all { it is AppEvent.Tick })
        runner.tick(state.copy(lastFinishedLine = "${mission.id}/briefing"))
        repeat(5) { runner.tick(state.copy(lastFinishedLine = "${mission.id}/briefing")) }
        assertEquals(1, events.filterIsInstance<AppEvent.PlaceEntered>().size)
    }

    @Test fun pauseDoesNotAdvanceClockOrEmitSignals() {
        val events = mutableListOf<AppEvent>()
        val runner = ShowcaseRunner(emptyList(), emptyList(), 0, events::add)
        val paused = ShowcaseSnapshot(Screen.CREW_CHECK, null, "", "ready", false, 0, parentStopped = true)
        repeat(50) { runner.tick(paused) }
        assertTrue(events.isEmpty())
        runner.tick(paused.copy(parentStopped = false))
        assertEquals(AppEvent.Tick(1000), events.first())
        assertEquals(AppEvent.GearChanged(Gear.D), events.last())
    }

    private class Trip(episode: Episode, destination: String, route: String) {
        val plan = TripCatalog(episode).plan(destination, route, AgeGroup.PRESCHOOL)
        private val engine = MissionEngine(episode)
        private val presentation = EnginePresentation(episode)
        val generated = mutableListOf<AppEvent>()
        val items = mutableSetOf<String>()
        var screen = Screen.START
        var lastFinished = ""
        private var spoken: Command.Say? = null
        private var speechTicks = 0
        val runner = ShowcaseRunner(plan, episode.places, 0) { generated += it; feed(it) }
        init {
            val gate = ShowcaseSignals().apply { select(true) }
            gate.restoreEvents().forEach(::feed)
            feed(AppEvent.AgeSelected(AgeGroup.PRESCHOOL))
            feed(AppEvent.DestinationSelected(destination, route))
            feed(AppEvent.StartPressed)
        }
        private fun feed(event: AppEvent) {
            if (event is AppEvent.LineFinished) { lastFinished = event.lineId; presentation.lineFinished(event.lineId) }
            engine.onEvent(event).forEach { command -> when (command) {
                is Command.Say -> { presentation.say(command); spoken = command; speechTicks = 2 }
                is Command.ShowScreen -> screen = command.screen
                is Command.AwardItem -> items += command.itemId
                Command.StopAudio -> spoken = null
                else -> Unit
            } }
        }
        fun step() {
            if (runner.finished) return
            if (spoken != null && --speechTicks <= 0) {
                val line = spoken!!; spoken = null
                feed(AppEvent.LineFinished(line.lineId))
            }
            runner.tick(ShowcaseSnapshot(screen, presentation.mission?.id, presentation.stage,
                lastFinished, presentation.answerReady, items.size, speaking = spoken != null))
        }
    }
}
