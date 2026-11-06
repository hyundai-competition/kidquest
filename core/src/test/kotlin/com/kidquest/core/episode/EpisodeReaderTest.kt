// 에피소드 파일 읽기 시험. 실제 파일을 읽고, 대본을 고치다 나올 만한 실수를 넣어 본다.
package com.kidquest.core.episode

import com.kidquest.core.contract.AgeGroup
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.MissionTrigger
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class EpisodeReaderTest {
    /** 시험의 바탕. 틀린 경우는 이 글에서 한 곳만 바꿔 만든다. */
    private val good = """
        {
          "id": "test",
          "title": "시험",
          "destinations": [
            {
              "id": "planet", "name": "시험 목적지", "planetName": "시험 행성",
              "departureLine": "출발", "approachLine": "접근", "deliveryLine": "전달",
              "landingFact": "사실", "factSource": "출처"
            }
          ],
          "places": [
            { "id": "river", "latitude": 37.5, "longitude": 127.0, "radiusMeters": 300, "missionId": "meteor" }
          ],
          "missions": [
            {
              "id": "meteor", "ageGroups": ["PRESCHOOL"], "destinationId": null,
              "trigger": "WIPER", "autoAfterSeconds": 120,
              "briefing": "브리핑", "eventSoundId": "meteor", "event": "사건", "question": "질문",
              "answers": ["초록"], "correctLine": "맞아요", "wrongLine": "정답은 초록", "rewardItem": "shield"
            }
          ],
          "items": [ { "id": "shield", "name": "방어막 조각" } ],
          "reactions": [
            { "signal": "turn_left", "soundId": "thruster", "line": null, "cooldownSeconds": 60 }
          ],
          "finalLine": "마지막"
        }
    """.trimIndent()

    private fun expectOk(text: String): Episode = when (val result = readEpisode(text)) {
        is EpisodeResult.Ok -> result.episode
        is EpisodeResult.Failed -> {
            fail("읽혀야 하는데 실패했다 : ${result.problems}")
            throw AssertionError()
        }
    }

    /** 앱이 죽지 않고 실패로 돌려주는지, 틀린 곳을 알 수 있게 적었는지 본다. */
    private fun expectProblem(text: String, clue: String) {
        when (val result = readEpisode(text)) {
            is EpisodeResult.Ok -> fail("실패해야 하는데 읽혔다 : $clue")
            is EpisodeResult.Failed -> assertTrue(
                "「$clue」가 없다 : ${result.problems}",
                result.problems.any { clue in it },
            )
        }
    }

    private fun String.swap(old: String, new: String): String {
        assertTrue("시험 바탕에 없는 글 : $old", old in this)
        return replace(old, new)
    }

    @Test
    fun `앱에 들어가는 에피소드 파일은 전부 읽힌다`() {
        val files = File("../app/src/main/assets/episodes").listFiles { f -> f.extension == "json" }.orEmpty()
        assertTrue("에피소드 파일을 못 찾았다", files.isNotEmpty())
        files.forEach { file ->
            val result = readEpisode(file.readText())
            assertTrue("${file.name} : $result", result is EpisodeResult.Ok)
        }
    }

    @Test
    fun `칸의 값이 그대로 들어온다`() {
        val episode = expectOk(good)
        val mission = episode.missions.single()
        assertEquals("test", episode.id)
        assertEquals(MissionTrigger.WIPER, mission.trigger)
        assertEquals(listOf(AgeGroup.PRESCHOOL), mission.ageGroups)
        assertNull(mission.destinationId)
        assertEquals(listOf("초록"), mission.answers)
        assertEquals(300f, episode.places.single().radiusMeters)
        assertNull(episode.reactions.single().line)
    }

    @Test
    fun `JSON이 아니면 실패로 돌려준다`() {
        expectProblem("{ 이건 JSON이 아니다", "파일 형식")
        expectProblem("", "파일 형식")
    }

    @Test
    fun `칸이 빠지면 빠진 칸 이름을 알려 준다`() {
        expectProblem(good.swap(""""briefing": "브리핑", """, ""), "briefing")
    }

    @Test
    fun `칸 이름이 틀리면 실패한다`() {
        expectProblem(good.swap(""""question":""", """"qestion":"""), "qestion")
    }

    @Test
    fun `값의 종류가 틀리면 실패한다`() {
        expectProblem(good.swap(""""autoAfterSeconds": 120""", """"autoAfterSeconds": "두 분""""), "파일 형식")
        expectProblem(good.swap(""""trigger": "WIPER"""", """"trigger": "RAIN""""), "RAIN")
        expectProblem(good.swap("""["PRESCHOOL"]""", """["BABY"]"""), "BABY")
    }

    @Test
    fun `없는 아이템을 보상으로 주면 실패한다`() {
        expectProblem(good.swap(""""rewardItem": "shield"""", """"rewardItem": "sword""""), "sword")
    }

    @Test
    fun `좌표가 없는 임무를 가리키면 실패한다`() {
        expectProblem(good.swap(""""missionId": "meteor"""", """"missionId": "ghost""""), "ghost")
    }

    @Test
    fun `임무가 없는 목적지를 가리키면 실패한다`() {
        expectProblem(good.swap(""""destinationId": null""", """"destinationId": "mars""""), "mars")
    }

    @Test
    fun `같은 아이디가 두 번 나오면 실패한다`() {
        val twoItems = """[ { "id": "shield", "name": "하나" }, { "id": "shield", "name": "둘" } ]"""
        expectProblem(good.swap("""[ { "id": "shield", "name": "방어막 조각" } ]""", twoItems), "shield")
    }

    @Test
    fun `말할 대사가 비어 있으면 실패한다`() {
        // 대사가 비면 말이 끝났다는 사건을 기다리다 임무가 멈출 수 있다.
        expectProblem(good.swap(""""question": "질문"""", """"question": "  """"), "question")
        expectProblem(good.swap(""""finalLine": "마지막"""", """"finalLine": """""), "finalLine")
        expectProblem(good.swap(""""deliveryLine": "전달"""", """"deliveryLine": """""), "deliveryLine")
    }

    @Test
    fun `정답이 없으면 실패한다`() {
        expectProblem(good.swap("""["초록"]""", "[]"), "answers")
        expectProblem(good.swap("""["초록"]""", """[" "]"""), "answers")
    }

    @Test
    fun `임무가 하나도 없으면 실패한다`() {
        val noMissions = """{ "id": "e", "title": "t", "destinations": [], "places": [], "missions": [],
            "items": [], "reactions": [], "finalLine": "끝" }"""
        expectProblem(noMissions, "임무가 하나도 없다")
    }

    @Test
    fun `저절로 열리는 시간이 0 이하면 실패한다`() {
        expectProblem(good.swap(""""autoAfterSeconds": 120""", """"autoAfterSeconds": 0"""), "autoAfterSeconds")
    }

    @Test
    fun `틀린 곳이 여럿이면 한 번에 다 적는다`() {
        val text = good
            .swap(""""rewardItem": "shield"""", """"rewardItem": "sword"""")
            .swap(""""missionId": "meteor"""", """"missionId": "ghost"""")
        val result = readEpisode(text) as EpisodeResult.Failed
        assertTrue(result.problems.any { "sword" in it })
        assertTrue(result.problems.any { "ghost" in it })
    }
}
