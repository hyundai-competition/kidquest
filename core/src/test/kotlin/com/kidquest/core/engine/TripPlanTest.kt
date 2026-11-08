// 목적지와 나이 고르기, 장소 임무 시험. 고른 목적지 임무만 나오는지, 출발 문장과 도착 문장을 차례로 말하는지,
// 장소를 차례가 오기 전에 지나가도 기억했다가 그 임무 차례에 여는지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.AgeGroup
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Command
import com.kidquest.core.contract.Destination
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.Mission
import com.kidquest.core.contract.MissionTrigger
import com.kidquest.core.contract.Place
import com.kidquest.core.contract.RearSeat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripPlanTest : MissionTestBase() {
    private val gyeongju = Destination(
        id = "gyeongju",
        name = "서울 → 경주",
        planetName = "경주",
        departureLine = "경주 출발",
        approachLine = "경주 접근",
        deliveryLine = "경주 전달",
        landingFact = "경주 사실",
        factSource = "시험 출처",
    )
    private val busan = gyeongju.copy(
        id = "busan",
        name = "서울 → 부산",
        departureLine = "부산 출발",
        deliveryLine = "부산 전달",
        landingFact = "부산 사실",
    )

    /** 효과음 없이 사건 대사를 바로 말하는 장소 임무. */
    private fun placeMission(id: String, destination: String?) =
        mission(id, sound = null, trigger = MissionTrigger.PLACE).copy(destinationId = destination)

    private fun timeMission(id: String) = mission(id, sound = null, trigger = MissionTrigger.TIME)

    private fun routes(vararg missions: Mission) = episode(*missions).copy(
        destinations = listOf(gyeongju, busan),
        places = listOf(
            Place("cheonan", 36.8, 127.2, 500f, "g1"),
            Place("tower", 35.8, 129.2, 500f, "g2"),
            Place("port", 35.1, 129.0, 500f, "b1"),
        ),
    )

    init {
        engine = MissionEngine(routes(placeMission("g1", "gyeongju"), placeMission("g2", "gyeongju"), placeMission("b1", "busan")))
    }

    private fun choose(id: String?) = send(AppEvent.DestinationSelected(id))

    /** 출발 문장이 끝나 첫 임무 브리핑을 말하는 데까지 간다. */
    private fun untilFirstBriefing(): List<Command> {
        untilBriefing()
        return finish(TripPlan.DEPARTURE_ID)
    }

    /** 브리핑이 끝난 임무를 장소로 열고 답까지 끝낸다. 다음 임무 브리핑을 돌려준다. */
    private fun clearPlaceMission(id: String, place: String): List<Command> {
        finish("$id/briefing")
        send(AppEvent.PlaceEntered(place))
        finish("$id/event")
        finish("$id/question")
        send(AppEvent.AnswerHeard("초록색"))
        return finish("$id/answer")
    }

    @Test
    fun `목적지를 고르면 출발 문장을 이륙 대사 다음, 첫 브리핑 전에 말한다`() {
        choose("gyeongju")
        assertEquals(listOf(Lines.TAKE_OFF.text), said(takeOff()))
        assertEquals(listOf("경주 출발"), said(finish(Lines.TAKE_OFF.lineId)))
        assertEquals(listOf("g1 브리핑"), said(finish(TripPlan.DEPARTURE_ID)))
    }

    @Test
    fun `고른 목적지 임무와 공통 임무만 나오고 다른 목적지 임무는 빠진다`() {
        engine = MissionEngine(routes(placeMission("g1", "gyeongju"), placeMission("b1", "busan"), timeMission("common")))
        send(AppEvent.ListeningConsentChanged(true))
        choose("busan")
        assertEquals(listOf("b1 브리핑"), said(untilFirstBriefing()))
        assertEquals(listOf("common 브리핑"), said(clearPlaceMission("b1", "port")))
    }

    @Test
    fun `목적지를 안 고르면 출발 문장 없이 공통 임무만 한다`() {
        engine = MissionEngine(routes(placeMission("g1", "gyeongju"), timeMission("common")))
        takeOff()
        assertEquals(listOf("common 브리핑"), said(finish(Lines.TAKE_OFF.lineId)))
    }

    @Test
    fun `없는 목적지를 고르면 고르지 않은 것으로 본다`() {
        engine = MissionEngine(routes(placeMission("g1", "gyeongju"), timeMission("common")))
        choose("mars")
        takeOff()
        assertEquals(listOf("common 브리핑"), said(finish(Lines.TAKE_OFF.lineId)))
    }

    @Test
    fun `시작한 뒤에 고른 목적지는 이번 이동에 쓰지 않는다`() {
        choose("gyeongju")
        send(AppEvent.StartPressed)
        choose("busan")
        send(AppEvent.SeatOccupied(RearSeat.LEFT, true))
        buckle(true)
        send(AppEvent.GearChanged(Gear.D))
        assertEquals(listOf("경주 출발"), said(finish(Lines.TAKE_OFF.lineId)))
    }

    @Test
    fun `출발 문장을 말하다 멈추면 다시 이어질 때 출발 문장부터 말한다`() {
        choose("gyeongju")
        untilBriefing()
        send(AppEvent.ParentStopChanged(true))
        assertTrue(said(send(AppEvent.ParentStopChanged(false))).contains("경주 출발"))
    }

    @Test
    fun `임무를 다 끝내고 착륙하면 결말, 전달, 사실, 착륙 대사를 차례로 말한다`() {
        engine = MissionEngine(routes(placeMission("g1", "gyeongju")))
        send(AppEvent.ListeningConsentChanged(true))
        choose("gyeongju")
        untilFirstBriefing()
        clearPlaceMission("g1", "cheonan")
        assertEquals(listOf("결말 문장"), said(send(AppEvent.GearChanged(Gear.P))))
        assertEquals(listOf("경주 전달"), said(finish(Lines.ENDING_ID)))
        assertEquals(listOf("경주 사실"), said(finish(TripPlan.DELIVERY_ID)))
        assertEquals(listOf(Lines.LANDED.text), said(finish(TripPlan.FACT_ID)))
        assertTrue(said(finish(Lines.LANDED.lineId)).isEmpty())
    }

    @Test
    fun `도착했으면 임무가 남아도 전달과 사실을 말하고 결말은 뺀다`() {
        choose("gyeongju")
        untilFirstBriefing()
        send(AppEvent.DestinationArrived)
        assertEquals(listOf("경주 전달"), said(send(AppEvent.GearChanged(Gear.P))))
        assertEquals(listOf("경주 사실"), said(finish(TripPlan.DELIVERY_ID)))
        assertEquals(listOf(Lines.LANDED.text), said(finish(TripPlan.FACT_ID)))
    }

    @Test
    fun `휴게소에서 끝내기를 누르면 도착 전이라 전달 문장 없이 착륙 대사만 말한다`() {
        choose("gyeongju")
        untilFirstBriefing()
        send(AppEvent.GearChanged(Gear.P))
        assertEquals(EngineState.DOCKED, engine.state)
        assertEquals(listOf(Lines.LANDED.text), said(send(AppEvent.EndTripPressed)))
    }

    @Test
    fun `다시 하기 뒤에도 고른 목적지의 출발 문장을 다시 말한다`() {
        choose("gyeongju")
        untilFirstBriefing()
        send(AppEvent.DestinationArrived, AppEvent.GearChanged(Gear.P), AppEvent.RestartPressed, AppEvent.StartPressed)
        send(AppEvent.GearChanged(Gear.D))
        assertEquals(listOf("경주 출발"), said(finish(Lines.TAKE_OFF.lineId)))
    }

    @Test
    fun `나이를 고르면 그 나이 임무만 나오고, 안 고르면 나이가 정해진 임무는 빠진다`() {
        val young = timeMission("young").copy(ageGroups = listOf(AgeGroup.PRESCHOOL))
        val older = timeMission("older").copy(ageGroups = listOf(AgeGroup.EARLY_GRADE))
        engine = MissionEngine(episode(young, older))
        send(AppEvent.AgeSelected(AgeGroup.EARLY_GRADE))
        takeOff()
        assertEquals(listOf("older 브리핑"), said(finish(Lines.TAKE_OFF.lineId)))

        engine = MissionEngine(episode(young, older, timeMission("everyone")))
        takeOff()
        assertEquals(listOf("everyone 브리핑"), said(finish(Lines.TAKE_OFF.lineId)))
    }

    @Test
    fun `장소 임무를 기다리는 중에 그 장소에 들어오면 바로 사건을 말한다`() {
        choose("gyeongju")
        untilFirstBriefing()
        finish("g1/briefing")
        assertEquals(listOf("g1 사건"), said(send(AppEvent.PlaceEntered("cheonan"))))
    }

    @Test
    fun `차례가 오기 전에 지나간 장소는 기억했다가 그 임무 브리핑이 끝나면 바로 연다`() {
        send(AppEvent.ListeningConsentChanged(true))
        choose("gyeongju")
        untilFirstBriefing()
        assertTrue(send(AppEvent.PlaceEntered("tower")).isEmpty())
        assertEquals(listOf("g2 브리핑"), said(clearPlaceMission("g1", "cheonan")))
        assertEquals(listOf("g2 사건"), said(finish("g2/briefing")))
    }

    @Test
    fun `같은 장소에 두 번 들어와도 한 번만 연다`() {
        choose("gyeongju")
        untilFirstBriefing()
        finish("g1/briefing")
        send(AppEvent.PlaceEntered("cheonan"))
        assertTrue(send(AppEvent.PlaceEntered("cheonan")).isEmpty())
    }

    @Test
    fun `없는 장소나 다른 목적지의 장소는 무시한다`() {
        choose("gyeongju")
        untilFirstBriefing()
        finish("g1/briefing")
        assertTrue(send(AppEvent.PlaceEntered("nowhere")).isEmpty())
        assertTrue(send(AppEvent.PlaceEntered("port")).isEmpty())
        assertEquals(listOf("g1 사건"), said(send(AppEvent.PlaceEntered("cheonan"))))
    }

    @Test
    fun `멈춘 동안 들어온 장소는 다시 이어질 때 연다`() {
        choose("gyeongju")
        untilFirstBriefing()
        finish("g1/briefing")
        send(AppEvent.ParentStopChanged(true))
        assertTrue(said(send(AppEvent.PlaceEntered("cheonan"))).isEmpty())
        assertTrue(said(send(AppEvent.ParentStopChanged(false))).contains("g1 사건"))
    }

    @Test
    fun `장소가 안 오면 정해 둔 시간이 지나 열린다`() {
        choose("gyeongju")
        untilFirstBriefing()
        finish("g1/briefing")
        tick(0)
        assertEquals(listOf("g1 사건"), said(tick(120)))
    }
}
