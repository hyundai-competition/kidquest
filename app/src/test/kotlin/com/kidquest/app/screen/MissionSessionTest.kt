// 연령별 목표 수, 중복 수집 방지, 순서와 배치 검증, 모든 미션의 완료 가능성을 확인한다.
package com.kidquest.app.screen

import com.kidquest.app.screen.model.*
import com.kidquest.core.contract.AgeGroup
import org.junit.Assert.*
import org.junit.Test

class MissionSessionTest {
    @Test fun everyMissionHasThreeVoiceChoicesAndOnlyCorrectAnswerCompletes() {
        AdventureOptions.routes.flatMap { it.missions }.forEach { mission ->
            val s = MissionSession(mission, AgeGroup.PRESCHOOL)
            s.voiceMode = true
            assertEquals(3, s.quiz.options.distinct().size)
            assertFalse(s.answerNumber((s.quiz.answer + 1) % 3))
            assertFalse(s.done)
            assertTrue(s.answerNumber(s.quiz.answer))
            assertTrue(s.done)
            assertFalse(s.answerNumber(s.quiz.answer))
        }
    }
    @Test fun everyMissionCompletesForBothAges() {
        AdventureOptions.routes.flatMap { it.missions }.forEach { mission ->
            AgeGroup.entries.forEach { age ->
                val s = MissionSession(mission, age)
                when (mission.kind) {
                    PuzzleKind.STARS -> (1..s.starCount).forEach { s.choose(it.toString()) }
                    PuzzleKind.BUILD -> s.targets.forEach { s.selectedPiece = it; assertTrue(s.place(it)) }
                    else -> s.targets.forEach { assertTrue(s.choose(it)) }
                }
                assertTrue("${mission.place} $age", s.done)
            }
        }
    }
    @Test fun wrongOrderAndWrongSlotDoNotAdvance() {
        val stars = MissionSession(CityMissions.forCity("경주")[1], AgeGroup.EARLY_GRADE)
        assertFalse(stars.choose("2")); assertTrue(stars.sequence.isEmpty())
        assertTrue(stars.choose("1")); assertFalse(stars.choose("1"))
        val build = MissionSession(CityMissions.forCity("경주")[2], AgeGroup.PRESCHOOL)
        build.selectedPiece = "연못"
        assertFalse(build.place("전통 건물")); assertTrue(build.found.isEmpty())
        assertTrue(build.place("연못"))
    }
    @Test fun repeatedFindDoesNotCountTwiceAndDistractorDoesNotCount() {
        val s = MissionSession(CityMissions.forCity("경주")[0], AgeGroup.EARLY_GRADE)
        assertFalse(s.choose("스마트폰"))
        s.choose("금관"); s.choose("금관")
        assertEquals(1, s.found.size); assertFalse(s.done)
    }
}
