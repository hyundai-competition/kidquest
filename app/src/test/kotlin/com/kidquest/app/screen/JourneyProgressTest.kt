// 미리 읽는 대사와 실제 진입을 구분하고 경로·재시작 기록을 검증한다.
package com.kidquest.app.screen

import com.kidquest.app.screen.model.*
import com.kidquest.core.contract.*
import com.kidquest.core.episode.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class JourneyProgressTest {
    private val episode = (readEpisode(File("src/main/assets/episodes/episode_demo.json").readText()) as EpisodeResult.Ok).episode
    private val catalog = TripCatalog(episode)
    private val destination = catalog.destinations.first()
    private val stops = catalog.stops(destination.id, destination.routes.first().id, AgeGroup.PRESCHOOL)
    private fun progress(reached: Set<String> = emptySet(), rewards: Set<String> = emptySet(), arrived: Boolean = false) =
        journeyProgress("출발지", stops, reached, rewards, arrived)

    @Test fun noEntryAndNoRewardStaysAtOriginEvenIfTripEndsEarly() {
        assertEquals(0, progress().percent)
        assertEquals(emptySet<Int>(), progress().completedIndices)
    }
    @Test fun enteringSectionMovesMarkerBeforeReward() {
        val result = progress(setOf(stops[1].placeId))
        assertEquals(2, result.currentIndex)
        assertEquals(emptySet<Int>(), result.completedIndices)
    }
    @Test fun finishedPlaceIsShownWhenEntryWasMissed() {
        val result = progress(rewards = setOf(stops[0].mission.rewardItem))
        assertEquals(1, result.currentIndex)
        assertEquals(setOf(1), result.completedIndices)
    }
    @Test fun oldOrForeignEntriesDoNotMoveMarkerBackwards() {
        assertEquals(2, progress(setOf(stops[1].placeId, stops[0].placeId, "other-route")).currentIndex)
        assertEquals(0, progress(setOf("other-route")).currentIndex)
    }
    @Test fun arrivalDoesNotInventRewardsAndFreshSessionResets() {
        assertEquals(100, progress(arrived = true).percent)
        assertTrue(progress(arrived = true).completedIndices.isEmpty())
        assertEquals(0, progress().currentIndex)
    }
}
