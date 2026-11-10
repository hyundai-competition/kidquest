// 언어 고르기 시험. 영어를 고르면 고정 대사와 에피소드 글이 영어로 나오고, 고른 목적지와 차 신호 기억은 그대로인지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.CarSignal
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Destination
import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.Language
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.Route
import com.kidquest.core.contract.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageTest : MissionTestBase() {
    /** 한국어 파일과 id가 같고 글만 영어인 파일. */
    private fun english(korean: Episode) = korean.copy(
        missions = korean.missions.map {
            it.copy(
                briefing = "${it.id} briefing", event = "${it.id} event", question = "${it.id} question",
                answers = listOf("Green"), correctLine = "${it.id} correct", wrongLine = "${it.id} wrong",
            )
        },
        items = korean.items.map { it.copy(name = "${it.id} piece") },
        destinations = korean.destinations.map { d ->
            d.copy(departureLine = "${d.id} departure", routes = d.routes.map { it.copy(departureLine = "${it.id} departure") })
        },
        endingLine = "The end",
    )

    private fun withEnglish(korean: Episode) {
        engine = MissionEngine(korean, mapOf(Language.EN to english(korean)))
    }

    @Test
    fun `영어를 고르면 고정 대사와 임무 대사가 영어로 나온다`() {
        withEnglish(episode(mission("a")))
        send(AppEvent.LanguageSelected(Language.EN))
        assertEquals(Language.EN, engine.language)
        assertTrue(takeOff().contains(EnglishLines.TAKE_OFF))
        assertEquals(listOf("a briefing"), said(finish("take_off")))
    }

    @Test
    fun `영어 파일이 없으면 한국어로 둔다`() {
        engine = MissionEngine(episode(mission("a")))
        send(AppEvent.LanguageSelected(Language.EN))
        assertEquals(Language.KO, engine.language)
        assertTrue(takeOff().contains(Lines.TAKE_OFF))
    }

    @Test
    fun `시작한 뒤에는 언어를 바꾸지 않는다`() {
        withEnglish(episode(mission("a")))
        takeOff()
        send(AppEvent.LanguageSelected(Language.EN))
        assertEquals(Language.KO, engine.language)
        assertTrue(send(AppEvent.GearChanged(Gear.P)).contains(Lines.DOCKED))
    }

    @Test
    fun `한국어로 돌아오면 한국어 파일을 쓴다`() {
        withEnglish(episode(mission("a")))
        send(AppEvent.LanguageSelected(Language.EN), AppEvent.LanguageSelected(Language.KO))
        assertTrue(takeOff().contains(Lines.TAKE_OFF))
        assertEquals(listOf("a 브리핑"), said(finish("take_off")))
    }

    @Test
    fun `멈춤 화면의 상태 한 줄도 영어로 나온다`() {
        withEnglish(episode(mission("a")))
        send(AppEvent.LanguageSelected(Language.EN))
        takeOff()
        assertTrue(buckle(false).contains(Command.ShowScreen(Screen.PAUSED, EnglishLines.STATUS_BELT)))
    }

    @Test
    fun `영어 답은 대소문자를 가리지 않는다`() {
        withEnglish(episode(mission("a")))
        send(AppEvent.LanguageSelected(Language.EN))
        untilAnswer()
        assertEquals(listOf("a correct"), said(send(AppEvent.AnswerHeard("GREEN."))))
    }

    @Test
    fun `언어를 바꿔도 고른 목적지와 경로가 남는다`() {
        val korean = episode(mission("common"), mission("r1_stop"), mission("r2_stop")).copy(destinations = listOf(destination()))
        withEnglish(korean)
        send(AppEvent.DestinationSelected("d", "r2"), AppEvent.LanguageSelected(Language.EN))
        takeOff()
        // 이륙 대사 → 고른 경로의 출발 문장 → 공통 임무 → 고른 경로의 임무 순서다. 경로를 잃으면 첫 경로(r1)로 간다.
        assertEquals(listOf("r2 departure"), said(finish("take_off")))
        assertEquals(listOf("common briefing"), said(finish("departure")))
        finish("common/briefing")
        wiper(true)
        tick(0)
        tick(3)
        finish("common/event")
        finish("common/question")
        send(AppEvent.AnswerSelected("Green"))
        assertEquals(listOf("r2_stop briefing"), said(finish("common/answer")))
    }

    @Test
    fun `언어를 바꿔도 위치를 못 읽는 차라는 기억은 남는다`() {
        val place = mission("p", sound = null, trigger = MissionTrigger.PLACE)
        withEnglish(episode(place))
        send(AppEvent.SignalUnavailable(CarSignal.LOCATION), AppEvent.LanguageSelected(Language.EN))
        untilBriefing()
        finish("p/briefing")
        tick(0)
        assertEquals(listOf("p event"), said(tick(120)))
    }

    @Test
    fun `보상 이름과 결말도 고른 언어로 나온다`() {
        withEnglish(episode(mission("a")))
        send(AppEvent.LanguageSelected(Language.EN))
        untilAnswer()
        send(AppEvent.AnswerHeard("green"))
        val award = finish("a/answer").filterIsInstance<Command.AwardItem>()
        assertEquals(listOf(Command.AwardItem("shield", "shield piece")), award)
        assertTrue(said(send(AppEvent.GearChanged(Gear.P))).contains("The end"))
    }

    private fun destination() = Destination(
        id = "d", name = "서울 → 경주", planetName = "경주",
        departureLine = "d 출발", approachLine = "d 예고", deliveryLine = "d 배달", landingFact = "d 사실", factSource = "출처",
        routes = listOf(Route("r1", "경로 1", listOf("r1_stop"), "r1 출발"), Route("r2", "경로 2", listOf("r2_stop"), "r2 출발")),
    )
}
