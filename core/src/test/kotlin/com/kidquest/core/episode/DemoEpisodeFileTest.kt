// 앱에 들어가는 예시 에피소드 파일이 틀린 곳 없이 읽히는지 본다.
// 대본을 고치다 틀리면 앱은 꺼지지 않고 임무 없이 돌아서 눈치채기 어렵다. 그래서 여기서 먼저 잡는다.
package com.kidquest.core.episode

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
}
