// 경로 고르기와 장소 건너뛰기 시험. 고른 경로의 장소 임무를 경로 순서대로 하는지, 같은 도시 임무를 여러 경로가 같이 쓰는지,
// 다른 길로 와서 못 지난 장소는 건너뛰는지, 도착이 가까우면 도착 직전 문장 다음에 마지막 보물을 여는지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Destination
import com.kidquest.core.contract.Mission
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.Place
import com.kidquest.core.contract.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTest : MissionTestBase() {
    private val busan = Destination(
        id = "busan",
        name = "서울 → 부산",
        planetName = "부산",
        departureLine = "부산 출발",
        approachLine = "부산 접근",
        deliveryLine = "부산 전달",
        landingFact = "부산 사실",
        factSource = "시험 출처",
        routes = listOf(
            Route("gyeongbu", "경부고속도로(수원·대전)", listOf("suwon", "daejeon", "port"), departureLine = "경부 출발"),
            Route("jungbu", "중부내륙고속도로(충주)", listOf("chungju", "port")),
        ),
    )
    private val seoul = busan.copy(
        id = "seoul",
        name = "대전 → 서울",
        departureLine = "서울 출발",
        routes = listOf(Route("north", "경부고속도로(천안·수원)", listOf("cheonan", "suwon", "palace"))),
    )

    /** 효과음 없이 사건 대사를 바로 말하는 장소 임무. 좌표 id는 「임무 id_area」다. */
    private fun place(id: String) = mission(id, sound = null, trigger = MissionTrigger.PLACE)

    private fun world(vararg missions: Mission) = episode(*missions).copy(
        destinations = listOf(busan, seoul),
        places = missions.filter { it.trigger == MissionTrigger.PLACE }.map { Place("${it.id}_area", 36.0, 127.0, 3_000f, it.id) },
    )

    /** 파일 순서는 일부러 경로 순서와 다르게 둔다. */
    private val allPlaces = arrayOf(place("port"), place("daejeon"), place("suwon"), place("chungju"), place("cheonan"), place("palace"))

    init {
        engine = MissionEngine(world(*allPlaces))
    }

    private fun choose(destination: String?, route: String? = null) =
        send(AppEvent.ListeningConsentChanged(true), AppEvent.DestinationSelected(destination, route))

    /** 출발 문장이 끝나 첫 임무 브리핑을 말하는 데까지 간다. */
    private fun untilFirstBriefing(): List<Command> {
        untilBriefing()
        return finish(TripPlan.DEPARTURE_ID)
    }

    private fun enter(id: String) = send(AppEvent.PlaceEntered("${id}_area"))

    /** 브리핑이 끝난 장소 임무를 장소로 열고 답까지 끝낸다. 다음에 나온 명령을 돌려준다. */
    private fun clear(id: String): List<Command> {
        finish("$id/briefing")
        enter(id)
        finish("$id/event")
        finish("$id/question")
        send(AppEvent.AnswerHeard("초록색"))
        return finish("$id/answer")
    }

    private fun awards(commands: List<Command>) = commands.filterIsInstance<Command.AwardItem>().map { it.itemId }

    @Test
    fun `경로를 고르면 그 경로의 출발 문장과 첫 장소 임무를 말한다`() {
        choose("busan", "gyeongbu")
        assertEquals(listOf("경부 출발"), said(untilBriefing()))
        assertEquals(listOf("suwon 브리핑"), said(finish(TripPlan.DEPARTURE_ID)))
    }

    @Test
    fun `경로를 안 고르거나 없는 경로면 첫 경로로 간다`() {
        choose("busan")
        assertEquals(listOf("suwon 브리핑"), said(untilFirstBriefing()))

        engine = MissionEngine(world(*allPlaces))
        choose("busan", "nowhere")
        assertEquals(listOf("suwon 브리핑"), said(untilFirstBriefing()))
    }

    @Test
    fun `경로에 출발 문장이 없으면 목적지 출발 문장을 쓴다`() {
        choose("busan", "jungbu")
        assertEquals(listOf("부산 출발"), said(untilBriefing()))
        assertEquals(listOf("chungju 브리핑"), said(finish(TripPlan.DEPARTURE_ID)))
    }

    @Test
    fun `파일 순서가 아니라 경로에 적은 순서대로 가고, 같은 도시 임무를 다른 경로도 쓴다`() {
        choose("seoul")
        assertEquals(listOf("cheonan 브리핑"), said(untilFirstBriefing()))
        assertEquals(listOf("suwon 브리핑"), said(clear("cheonan")))
        assertEquals(listOf("palace 브리핑"), said(clear("suwon")))
    }

    @Test
    fun `공통 임무를 먼저 하고, 목적지를 안 고르면 경로 임무는 안 나온다`() {
        engine = MissionEngine(world(*allPlaces, mission("common", sound = null, trigger = MissionTrigger.TIME)))
        choose("busan", "jungbu")
        assertEquals(listOf("common 브리핑"), said(untilFirstBriefing()))

        engine = MissionEngine(world(*allPlaces, mission("common", sound = null, trigger = MissionTrigger.TIME)))
        takeOff()
        assertEquals(listOf("common 브리핑"), said(finish(Lines.TAKE_OFF.lineId)))
    }

    @Test
    fun `다른 경로의 장소에 들어와도 지금 장소 임무를 그대로 기다린다`() {
        choose("busan", "gyeongbu")
        untilFirstBriefing()
        finish("suwon/briefing")
        assertTrue(enter("chungju").isEmpty())
        assertEquals(listOf("suwon 사건"), said(enter("suwon")))
    }

    @Test
    fun `기다리던 장소를 못 지나고 다음 장소에 오면 보물 없이 건너뛰고 다음 장소 임무를 연다`() {
        choose("busan", "gyeongbu")
        untilFirstBriefing()
        finish("suwon/briefing")
        val skipped = enter("daejeon")
        assertEquals(listOf("daejeon 브리핑"), said(skipped))
        assertTrue(awards(skipped).isEmpty())
        assertEquals(listOf("daejeon 사건"), said(finish("daejeon/briefing")))
    }

    @Test
    fun `차례가 오기 전에 뒤 장소를 먼저 지났으면 앞 장소 임무는 브리핑 없이 건너뛴다`() {
        engine = MissionEngine(world(mission("common", sound = null, trigger = MissionTrigger.TIME), *allPlaces))
        choose("busan", "gyeongbu")
        assertEquals(listOf("common 브리핑"), said(untilFirstBriefing()))
        assertTrue(enter("daejeon").isEmpty())
        finish("common/briefing")
        tick(0)
        tick(120)
        finish("common/event")
        finish("common/question")
        send(AppEvent.AnswerHeard("초록색"))
        assertEquals(listOf("daejeon 브리핑"), said(finish("common/answer")))
    }

    @Test
    fun `장소를 기다리다 도착 5분 전이 되면 도착 직전 문장 다음에 남은 중간 장소를 건너뛰고 마지막 장소 임무를 연다`() {
        choose("busan", "gyeongbu")
        untilFirstBriefing()
        finish("suwon/briefing")
        assertEquals(listOf("부산 접근"), said(send(AppEvent.RemainingTimeChanged(300))))
        assertEquals(listOf("port 브리핑"), said(finish(TripPlan.APPROACH_ID)))
        assertEquals(listOf("port 사건"), said(finish("port/briefing")))
    }

    @Test
    fun `임무 중에 도착 5분 전이 되면 그 임무를 끝낸 뒤 도착 직전 문장, 그다음 마지막 장소 임무를 말한다`() {
        choose("busan", "gyeongbu")
        untilFirstBriefing()
        finish("suwon/briefing")
        enter("suwon")
        assertTrue(said(send(AppEvent.RemainingTimeChanged(300))).isEmpty())
        finish("suwon/event")
        finish("suwon/question")
        send(AppEvent.AnswerHeard("초록색"))
        val afterAnswer = finish("suwon/answer")
        assertEquals(listOf("shield"), awards(afterAnswer))
        assertEquals(listOf("부산 접근"), said(afterAnswer))
        assertEquals(listOf("port 브리핑"), said(finish(TripPlan.APPROACH_ID)))
    }

    @Test
    fun `도착하면 기다리던 중간 장소를 건너뛰고 마지막 장소 임무를 연다`() {
        choose("busan", "gyeongbu")
        untilFirstBriefing()
        finish("suwon/briefing")
        assertEquals(listOf("port 브리핑"), said(send(AppEvent.DestinationArrived)))
        assertEquals(listOf("port 사건"), said(finish("port/briefing")))
    }

    @Test
    fun `다시 하기 뒤에는 지나온 장소를 잊는다`() {
        choose("busan", "gyeongbu")
        untilFirstBriefing()
        enter("daejeon")
        send(AppEvent.DestinationArrived, AppEvent.GearChanged(com.kidquest.core.contract.Gear.P))
        finish(TripPlan.DELIVERY_ID)
        finish(TripPlan.FACT_ID)
        send(AppEvent.RestartPressed, AppEvent.StartPressed, AppEvent.GearChanged(com.kidquest.core.contract.Gear.D))
        finish(Lines.TAKE_OFF.lineId)
        assertEquals(listOf("suwon 브리핑"), said(finish(TripPlan.DEPARTURE_ID)))
        finish("suwon/briefing")
        assertTrue(said(tick(0)).isEmpty())
    }
}
