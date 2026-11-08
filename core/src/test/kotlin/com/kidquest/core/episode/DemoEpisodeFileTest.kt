// 앱에 들어가는 예시 에피소드 파일이 틀린 곳 없이 읽히는지 본다.
// 대본을 고치다 틀리면 앱은 꺼지지 않고 임무 없이 돌아서 눈치채기 어렵다. 그래서 여기서 먼저 잡는다.
package com.kidquest.core.episode

import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.MissionTrigger
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class DemoEpisodeFileTest {
    /** 시험은 core 폴더에서 돈다. */
    private val file = File("../app/src/main/assets/episodes/episode_demo.json")

    @Test
    fun `예시 에피소드 파일이 틀린 곳 없이 읽힌다`() {
        when (val result = readEpisode(file.readText())) {
            is EpisodeResult.Ok -> assertTrue(result.episode.missions.isNotEmpty())
            is EpisodeResult.Failed -> fail(result.problems.joinToString("\n"))
        }
    }

    private fun episode(): Episode = (readEpisode(file.readText()) as EpisodeResult.Ok).episode

    @Test
    fun `출처 줄에 역사 내용 출처와 OpenStreetMap 표시가 있다`() {
        // 좌표를 OpenStreetMap에서 가져와서 앱에 표시가 있어야 한다.
        val credits = episode().credits.joinToString(" ")
        assertTrue(credits, "한국민족문화대백과사전" in credits)
        assertTrue(credits, "OpenStreetMap" in credits && "ODbL" in credits)
    }

    @Test
    fun `장소 임무의 정답은 그 장소에서 들은 사건 대사 안에 있다`() {
        // 말이 안 되는 오답 보기로 쉽게 고르는 대신, 방금 들은 말에서 답을 찾게 한다.
        episode().missions.filter { it.trigger == MissionTrigger.PLACE }.forEach { mission ->
            val heard = mission.event.filterNot { it.isWhitespace() }
            assertTrue(mission.id, mission.answers.any { answer -> answer.filterNot { it.isWhitespace() } in heard })
        }
    }
}
