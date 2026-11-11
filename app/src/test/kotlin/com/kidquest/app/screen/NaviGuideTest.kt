// 화면 아래 내비 안내 한 줄의 값과 글자를 본다. 에뮬레이터가 필요 없다.
package com.kidquest.app.screen

import com.kidquest.app.screen.model.NaviGuide
import com.kidquest.app.screen.model.NaviGuideText
import com.kidquest.app.screen.model.VehicleUiState
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Language
import com.kidquest.core.contract.TurnKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NaviGuideTest {
    @Test
    fun `회전 안내가 오면 한 줄 값을 바꾸고 도착하면 지운다`() {
        val guided = VehicleUiState().accept(AppEvent.NextTurnDistance(300, TurnKind.LEFT))
        assertEquals(NaviGuide(300, TurnKind.LEFT), guided.navi)
        assertNull(guided.accept(AppEvent.DestinationArrived).navi)
    }

    @Test
    fun `30초 동안 새 안내가 없으면 지난 안내를 지운다`() {
        val guided = VehicleUiState().accept(AppEvent.NextTurnDistance(300, TurnKind.LEFT), nowMillis = 1_000L)
        assertEquals(NaviGuide(300, TurnKind.LEFT), guided.accept(AppEvent.Tick(30_999L)).navi)
        assertNull(guided.accept(AppEvent.Tick(31_000L)).navi)
        val renewed = guided.accept(AppEvent.NextTurnDistance(120, TurnKind.LEFT), nowMillis = 25_000L)
        assertEquals(NaviGuide(120, TurnKind.LEFT), renewed.accept(AppEvent.Tick(40_000L)).navi)
    }

    @Test
    fun `한국어와 영어로 거리와 회전을 쓴다`() {
        assertEquals("내비 · 300m 앞 좌회전", NaviGuideText.line(NaviGuide(300, TurnKind.LEFT), Language.KO))
        assertEquals("내비 · 1.5km 앞 나들목 나가기", NaviGuideText.line(NaviGuide(1500, TurnKind.IC_EXIT), Language.KO))
        assertEquals("Navi · Keep right in 450 m", NaviGuideText.line(NaviGuide(450, TurnKind.KEEP_RIGHT), Language.EN))
        assertEquals("Navi · Next guidance in 2.0 km", NaviGuideText.line(NaviGuide(2000, TurnKind.UNKNOWN), Language.EN))
    }
}
