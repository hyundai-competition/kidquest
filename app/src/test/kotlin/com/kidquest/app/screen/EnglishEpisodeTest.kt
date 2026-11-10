// 영어 파일이 같은 경로·사건·보상 ID를 유지하며 모든 질문 보기가 영어로 연결되는지 확인한다.
package com.kidquest.app.screen

import com.kidquest.app.screen.model.*
import com.kidquest.core.contract.*
import com.kidquest.core.episode.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EnglishEpisodeTest {
    private fun load(name: String) = (readEpisode(File("src/main/assets/episodes/$name").readText()) as EpisodeResult.Ok).episode
    @Test fun translatedEpisodePreservesSignalsAndSupportsAllNumberedQuestions() {
        val ko = load("episode_demo.json")
        val en = load("episode_demo_en.json")
        assertEquals(ko.places, en.places)
        assertEquals(ko.missions.map { it.id }, en.missions.map { it.id })
        assertEquals(ko.items.map { it.id }, en.items.map { it.id })
        ko.missions.zip(en.missions).forEach { (original, translated) ->
            assertEquals(original.trigger, translated.trigger)
            assertEquals(original.autoAfterSeconds, translated.autoAfterSeconds)
            assertEquals(original.rewardItem, translated.rewardItem)
            val presentation = EnginePresentation(en)
            assertEquals(translated.id, 2, EngineChoices.forMission(translated).size)
            val spoken = presentation.say(Command.Say("${translated.id}/question", translated.question))
            assertTrue(spoken.text.contains("Option 1"))
            assertEquals(EngineChoices.forMission(translated).first(), presentation.answer("one"))
            assertFalse(spoken.text.any { it in '가'..'힣' })
        }
        ko.destinations.zip(en.destinations).forEach { (original, translated) ->
            assertEquals(original.id, translated.id)
            assertEquals(original.routes.map { it.id to it.missionIds }, translated.routes.map { it.id to it.missionIds })
        }
    }
}
