// 앱 화면. 화면·음성 역할이 정차 화면과 주행 중 화면을 채운다. 지금은 상태 이름과 버튼만 보인다.
// 글자 크기는 문서 값(Title 40, Body 30, Label 24)을 따른다.
package com.kidquest.app.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppController
import com.kidquest.app.replay.ReplayPanel
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Screen

@Composable
fun KidQuestScreen(controller: AppController) {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = titleOf(controller.screen), fontSize = 40.sp)
            controller.status?.let { Text(text = it, fontSize = 30.sp) }
            when (controller.screen) {
                Screen.START -> ActionButton("시작") { controller.dispatch(AppEvent.StartPressed) }
                Screen.CREW_CHECK -> Unit
                Screen.FLYING, Screen.PAUSED -> Unit
                Screen.DOCKED -> ActionButton("여기서 착륙") { controller.dispatch(AppEvent.EndTripPressed) }
                Screen.LANDED -> ActionButton("다시 하기") { controller.dispatch(AppEvent.RestartPressed) }
            }
            ReplayPanel(controller.replay)
        }
    }
}

private fun titleOf(screen: Screen): String = when (screen) {
    Screen.START -> "KidQuest"
    Screen.CREW_CHECK -> "승무원 확인"
    Screen.FLYING -> "비행 중"
    Screen.PAUSED -> "멈춤"
    Screen.DOCKED -> "잠깐 정박"
    Screen.LANDED -> "착륙"
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick) { Text(text = label, fontSize = 30.sp) }
}
