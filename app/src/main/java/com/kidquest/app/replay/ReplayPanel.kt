// 재생 모드 버튼. 개발과 시연에서 가짜 신호를 넣는다. 시연·품질 역할이 늘린다.
// 평소에는 접어 둔다. 주행 중 화면에 조작 버튼이 늘 보이면 안 되기 때문이다.
// 펼치면 "재생 모드"라고 보여 준다. 시연에서 이 모드를 쓰면 그 사실을 밝히기로 했다.
package com.kidquest.app.replay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.RearSeat
import com.kidquest.core.contract.TurnSide
import com.kidquest.core.contract.Episode

@Composable
fun ReplayPanel(replay: ReplaySource, episode: Episode? = null) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { open = !open }) {
        Text(text = if (open) "재생 모드 접기" else "재생 모드", fontSize = LABEL_SIZE.sp)
    }
    if (!open) return
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SetupButtons(replay)
        CrewButtons(replay)
        GearButtons(replay)
        DrivingButtons(replay)
        EnergyButtons(replay)
        TemperatureButtons(replay)
        TripButtons(replay, episode)
        AnswerButtons(replay, episode)
    }
}

@Composable
private fun CrewButtons(replay: ReplaySource) = ReplayRow {
    ReplayButton("탑승") { replay.send(AppEvent.SeatOccupied(RearSeat.LEFT, true)) }
    ReplayButton("벨트 매기") { replay.send(AppEvent.BeltBuckled(RearSeat.LEFT, true)) }
    ReplayButton("벨트 풀기") { replay.send(AppEvent.BeltBuckled(RearSeat.LEFT, false)) }
}

@Composable
private fun GearButtons(replay: ReplaySource) = ReplayRow {
    Gear.entries.forEach { gear ->
        ReplayButton(gear.name) { replay.send(AppEvent.GearChanged(gear)) }
    }
}

@Composable
private fun DrivingButtons(replay: ReplaySource) = ReplayRow {
    ReplayButton("왼쪽 깜빡이") { replay.send(AppEvent.TurnSignalOn(TurnSide.LEFT)) }
    ReplayButton("오른쪽 깜빡이") { replay.send(AppEvent.TurnSignalOn(TurnSide.RIGHT)) }
    ReplayButton("와이퍼 켜기") { replay.send(AppEvent.WiperChanged(true)) }
    ReplayButton("와이퍼 끄기") { replay.send(AppEvent.WiperChanged(false)) }
}

@Composable
private fun EnergyButtons(replay: ReplaySource) = ReplayRow {
    listOf(80, 40, 15).forEach { percent ->
        ReplayButton("배터리 $percent%") { replay.send(AppEvent.EnergyChanged(percent)) }
    }
}

@Composable
private fun TemperatureButtons(replay: ReplaySource) = ReplayRow {
    listOf(18f, 24f, 30f).forEach { celsius ->
        ReplayButton("실내 ${celsius.toInt()}도") { replay.send(AppEvent.CabinTemperatureChanged(celsius)) }
    }
}

/**
 * 시작 화면이 들어오기 전에도 시연할 수 있게 부모가 고르는 값을 넣는다.
 * 목적지는 「시작」을 누르기 전에만 엔진이 받는다. id는 에피소드 파일의 destinations와 같다.
 */
@Composable
private fun SetupButtons(replay: ReplaySource) = ReplayRow {
    DESTINATIONS.forEach { (id, label) ->
        ReplayButton("목적지 : $label") { replay.send(AppEvent.DestinationSelected(id)) }
    }
    ReplayButton("듣기 동의") { replay.send(AppEvent.ListeningConsentChanged(true)) }
    ReplayButton("듣기 거절") { replay.send(AppEvent.ListeningConsentChanged(false)) }
}

/** 장소 id는 에피소드 파일의 places와 같아야 임무가 열린다. 진짜 위치 대신 「근처에 왔다」를 넣는다. */
@Composable
private fun TripButtons(replay: ReplaySource, episode: Episode?) = ReplayRow {
    val places = episode?.places?.map { it.id to com.kidquest.app.screen.model.TripCatalog(episode).placeName(it.id) } ?: PLACES
    ReplayButton("갈림길 통과") {
        replay.send(AppEvent.NextTurnDistance(200))
        replay.send(AppEvent.NextTurnDistance(1500))
    }
    places.forEach { (id, label) ->
        ReplayButton("$label 근처") { replay.send(AppEvent.PlaceEntered(id)) }
    }
    ReplayButton("20분 남음") { replay.send(AppEvent.RemainingTimeChanged(20 * 60)) }
    ReplayButton("5분 남음") { replay.send(AppEvent.RemainingTimeChanged(5 * 60)) }
    ReplayButton("도착") { replay.send(AppEvent.DestinationArrived) }
}

/** 실제 아이 목소리를 쓰지 않고 답을 넣는다. 단어는 예시 에피소드의 정답이다. 듣기 동의는 마이크에만 적용하고 화면 답은 별개다. */
@Composable
private fun AnswerButtons(replay: ReplaySource, episode: Episode?) = ReplayRow {
    val answers = episode?.missions?.mapNotNull { it.answers.firstOrNull() }?.distinct() ?: ANSWERS
    answers.forEach { word ->
        ReplayButton("답 : $word") { replay.send(AppEvent.AnswerSelected(word)) }
    }
    ReplayButton("못 알아들음") { replay.send(AppEvent.AnswerHeard(null)) }
}

private val DESTINATIONS = listOf("gyeongju" to "경주", "busan" to "부산", "andong" to "안동", "seoul" to "서울")
private val PLACES = listOf(
    "cheomseongdae" to "첨성대",
    "temporary_capital" to "임시수도",
    "hahoe" to "하회마을",
    "gyeongbokgung" to "경복궁",
)
private val ANSWERS = listOf("파란색", "열쇠", "별", "부산", "옛날 집", "임금님")

/** 화면 폭이 차마다 다르니 넘치는 버튼은 다음 줄로 보낸다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReplayRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun ReplayButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick) { Text(text = label, fontSize = LABEL_SIZE.sp) }
}

private const val LABEL_SIZE = 24
