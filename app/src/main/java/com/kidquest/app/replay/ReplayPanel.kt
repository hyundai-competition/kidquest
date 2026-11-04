// 재생 모드 버튼. 개발과 시연에서 가짜 신호를 넣는다. 시연·품질 역할이 늘린다.
package com.kidquest.app.replay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.RearSeat

@Composable
fun ReplayPanel(replay: ReplaySource) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReplayButton("탑승") { replay.send(AppEvent.SeatOccupied(RearSeat.LEFT, true)) }
        ReplayButton("벨트 매기") { replay.send(AppEvent.BeltBuckled(RearSeat.LEFT, true)) }
        ReplayButton("벨트 풀기") { replay.send(AppEvent.BeltBuckled(RearSeat.LEFT, false)) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReplayButton("P") { replay.send(AppEvent.GearChanged(Gear.P)) }
        ReplayButton("R") { replay.send(AppEvent.GearChanged(Gear.R)) }
        ReplayButton("N") { replay.send(AppEvent.GearChanged(Gear.N)) }
        ReplayButton("D") { replay.send(AppEvent.GearChanged(Gear.D)) }
    }
}

@Composable
private fun ReplayButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick) { Text(label) }
}
