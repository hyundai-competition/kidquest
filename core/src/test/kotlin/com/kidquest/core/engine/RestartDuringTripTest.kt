// 확인받은 처음으로 동작이 주행·멈춤·정박에서도 소리를 끄고 진행을 초기화하는지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Test

class RestartDuringTripTest : MissionTestBase() {
    @Test fun `주행 중 다시 시작하면 중단 후 시작 화면을 보이고 옛 완료를 무시한다`() {
        untilAnswer()
        val result = send(AppEvent.RestartPressed)
        assertEquals(listOf(Command.StopAudio, Command.ShowScreen(Screen.START)), result)
        assertEquals(EngineState.WAITING, engine.state)
        assertTrue(finish("a/question").isEmpty())
        assertTrue(send(AppEvent.AnswerSelected("초록색")).isEmpty())
        send(AppEvent.StartPressed)
        assertEquals(EngineState.FLYING, engine.state) // 기어·탑승·벨트 현재값은 유지한다.
    }
    @Test fun `부모 멈춤과 정박에서도 처음으로 갈 수 있다`() {
        untilAnswer()
        send(AppEvent.ParentStopChanged(true))
        send(AppEvent.RestartPressed)
        assertEquals(EngineState.WAITING, engine.state)
        send(AppEvent.StartPressed, AppEvent.GearChanged(Gear.P))
        assertEquals(EngineState.DOCKED, engine.state)
        send(AppEvent.RestartPressed)
        assertEquals(EngineState.WAITING, engine.state)
    }
    @Test fun `처음으로 가도 내비가 소리를 쓰고 있으면 출발 대사는 기다린다`() {
        untilAnswer()
        send(AppEvent.AudioFocusChanged(false), AppEvent.RestartPressed)
        assertTrue(said(send(AppEvent.StartPressed)).isEmpty())
        assertEquals(EngineState.PAUSED, engine.state)
    }
}
