// 실제 main 대본과 화면 경로·보기 어댑터를 교차 검증한다. 대본이 바뀌면 조용히 잘못 연결되지 않게 한다.
package com.kidquest.app.screen

import com.kidquest.app.screen.model.*
import com.kidquest.core.contract.*
import com.kidquest.core.episode.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EngineIntegrationTest {
    private val episode = (readEpisode(File("src/main/assets/episodes/episode_demo.json").readText()) as EpisodeResult.Ok).episode
    @Test fun everyRouteHasCanonicalStopsAndRewards() {
        val catalog = TripCatalog(episode)
        assertEquals(4, catalog.destinations.size)
        assertEquals(5, catalog.destinations.sumOf { it.routes.size })
        catalog.destinations.forEach { destination -> destination.routes.forEach { route ->
            assertEquals(5, catalog.plan(destination.id, route.id, AgeGroup.PRESCHOOL).size)
            assertEquals(3, catalog.stops(destination.id, route.id, AgeGroup.PRESCHOOL).size)
        } }
    }
    @Test fun allQuestionsHaveValidatedOptionsAndNumberMapsToWords() {
        val presentation = EnginePresentation(episode)
        episode.missions.forEach { mission ->
            assertEquals(mission.id, 2, EngineChoices.forMission(mission).size)
            val spoken = presentation.say(Command.Say("${mission.id}/question", mission.question))
            assertTrue(spoken.text.contains("1번"))
            assertEquals(EngineChoices.forMission(mission).first(), presentation.answer("1번!"))
        }
        val changed = episode.missions.first().copy(question = "다른 질문")
        assertTrue(EngineChoices.forMission(changed).isEmpty())
        assertEquals(2, episode.credits.size)
    }
    @Test fun onlyTheCurrentFinishedQuestionEnablesChoices() {
        val presentation = EnginePresentation(episode)
        val mission = episode.missions.first()
        presentation.say(Command.Say("${mission.id}/question", mission.question))
        assertFalse(presentation.answerReady)
        presentation.lineFinished("other/question")
        assertFalse(presentation.answerReady)
        presentation.lineFinished("${mission.id}/question")
        assertTrue(presentation.answerReady)
        presentation.say(Command.Say("${mission.id}/answer", mission.correctLine))
        assertFalse(presentation.answerReady)
    }
}
