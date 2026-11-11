// 이동 경로와 대사·보상을 엔진 상태로 표시한다. 장소 버튼은 실제 위치와 같은 PlaceEntered를 보낸다.
package com.kidquest.app.screen

import com.kidquest.app.screen.AppText as Text
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.screen.common.*

@Composable
fun EngineJourneyBoard(model: AppViewModel) {
    val controller = model.controller
    val rewards = controller.items.map { it.itemId }.toSet()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        JourneyBoard(
            model.selectedRoute,
            completed = model.tripStops.filter { it.mission.rewardItem in rewards }.map { it.label }.toSet(),
            tripStops = model.tripStops,
            onPlace = model::visitPlace,
            story = model.activeTravelRoute?.name ?: "함께 떠나는 보물 탐험",
        )
        Text("모은 보물 ${controller.items.size} / ${model.plannedMissions.size}", fontSize = 30.sp)
    }
}

@Composable
fun EngineMissionPanel(model: AppViewModel) {
    val controller = model.controller
    val presentation = controller.presentation
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        VoiceStatusPanel(controller.voiceUi.copy(caption = presentation.caption))
        if (presentation.stage == "question") {
            Text(if (presentation.answerReady) "번호나 답을 골라 주세요." else "질문을 다 읽으면 답을 고를 수 있어요.", fontSize = 26.sp)
            presentation.options.forEachIndexed { i, label ->
                AnswerOptionButton("${i + 1}번 · $label", presentation.answerReady && controller.screen == com.kidquest.core.contract.Screen.FLYING) {
                    controller.answerChoice(i)
                }
            }
        }
        if (model.placeNotice.isNotBlank()) Text(model.placeNotice, Modifier.testTag("place-notice"), fontSize = 24.sp, lineHeight = 34.sp)
        OutlinedButton(onClick = model::openMiniGames, enabled = model.canPlayMiniGames,
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).testTag("open-mini-games")) { Text("미니게임 놀이터", fontSize = 30.sp) }
        TextButton(onClick = controller::toggleMusic) { Text(if (controller.musicEnabled) "배경음 켜짐 · 끄기" else "배경음 꺼짐 · 켜기", fontSize = 24.sp) }
        Text("퍼즐은 P 주차 중에만 열 수 있어요. 본편 보물과는 별도예요.", fontSize = 24.sp, lineHeight = 34.sp)
    }
}

@Composable
fun EngineTripEnding(model: AppViewModel) {
    Column(Modifier.fillMaxWidth().testTag("engine-trip-ending"), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("브루미와 함께한 여행을 마쳐요", fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text("오늘 만난 이야기를 마음에 담고, 다음 모험에서 또 만나요!", fontSize = 30.sp, lineHeight = 42.sp)
        Text("오늘 모은 보물 ${model.controller.items.size}개", fontSize = 32.sp)
        TripTreasureArt(model.controller.items.size, model.plannedMissions.size)
        TreasureReveal(model.controller.items, animate = model.controller.vehicle.decorationsMayAnimate)
        Button(onClick = model::restartTrip, modifier = Modifier.heightIn(min = 80.dp)) { Text("새로운 탐험 준비하기", fontSize = 30.sp) }
        HorizontalDivider()
        Text("콘텐츠와 위치 데이터 출처", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        model.credits.forEach { Text(it, fontSize = 22.sp, lineHeight = 34.sp) }
    }
}
