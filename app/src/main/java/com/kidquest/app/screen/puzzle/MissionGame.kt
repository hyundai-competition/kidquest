// 수집, 배치, 순서, 짝 연결 체험. 선택한 조각을 자리에 터치해 놓는 접근 가능한 퍼즐이다.
package com.kidquest.app.screen.puzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.screen.model.MissionSession
import com.kidquest.app.screen.model.PuzzleKind

@Composable
fun MissionGame(session: MissionSession, onAction: (String, Boolean) -> Unit) {
    when (session.mission.kind) {
        PuzzleKind.STARS -> StarPuzzle(session) { onAction(it, false) }
        PuzzleKind.COLOR -> ColorVillage(session) { onAction(it, false) }
        PuzzleKind.BRIDGE -> BridgePuzzle(session) { onAction(it, false) }
        PuzzleKind.BUILD -> BuildPuzzle(session, onAction)
        PuzzleKind.MATCH -> MatchPuzzle(session) { onAction(it, false) }
        PuzzleKind.ORDER -> OrderPuzzle(session) { onAction(it, false) }
        PuzzleKind.FIND -> FindPuzzle(session) { onAction(it, false) }
    }
}

@Composable
private fun FindPuzzle(s: MissionSession, choose: (String) -> Unit) {
    Text("찾을 보물: ${s.targets.joinToString(" · ")}", fontSize = 28.sp)
    Text("그림을 살펴보고 보물을 하나씩 모아보세요.")
    s.objects.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { item ->
                OutlinedButton(onClick = { choose(item) }, enabled = item !in s.found && !s.done, modifier = Modifier.weight(1f).heightIn(min = 110.dp)) {
                    Column {
                        ObjectPicture(item, Modifier.fillMaxWidth().height(54.dp))
                        Text(if (item in s.found) "$item ✓" else item, fontSize = 24.sp)
                    }
                }
            }
        }
    }
    Text("보물함 ${s.found.size} / ${s.targets.size}: ${s.found.joinToString()}")
    if (s.mission.place == "종묘") Text("악기를 찾으면 임시 시연음이 나와요. 실제 악기 녹음은 추후 연결해요.")
}

@Composable
private fun BuildPuzzle(s: MissionSession, action: (String, Boolean) -> Unit) {
    Text("조각을 고른 뒤 같은 모양의 빈자리를 눌러 놓아요.", fontSize = 28.sp)
    s.targets.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { slot ->
                OutlinedButton(onClick = { action(slot, true) }, enabled = slot !in s.found, modifier = Modifier.weight(1f).heightIn(min = 120.dp)) {
                    Column {
                        ObjectPicture(slot, Modifier.fillMaxWidth().height(64.dp), faded = slot !in s.found)
                        Text(if (slot in s.found) "$slot ✓" else "$slot 자리")
                    }
                }
            }
        }
    }
    Text("조각 상자 · ${s.selectedPiece ?: "조각을 선택하세요"}")
    s.objects.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { piece ->
                FilterChip(selected = s.selectedPiece == piece, onClick = { s.selectedPiece = piece }, enabled = piece !in s.found, label = { Text(piece) })
            }
        }
    }
}

@Composable
private fun OrderPuzzle(s: MissionSession, choose: (String) -> Unit) {
    Text("순서대로 카드를 연결해요.", fontSize = 28.sp)
    Text(if (s.sequence.isEmpty()) "빈 이야기 길" else s.sequence.joinToString(" → "), modifier = Modifier.fillMaxWidth().background(Color(0xFFE6EEDB), RoundedCornerShape(16.dp)).padding(20.dp), fontSize = 30.sp)
    s.targets.reversed().forEach { card ->
        OutlinedButton(onClick = { choose(card) }, enabled = card !in s.sequence, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text(card, fontSize = 28.sp) }
    }
}

@Composable
private fun MatchPuzzle(s: MissionSession, choose: (String) -> Unit) {
    val target = s.targets.firstOrNull { it !in s.found }
    Text(if (s.done) "짝을 모두 찾았어요!" else "${target?.substringBefore(" → ")}에 어울리는 짝을 찾아요.", fontSize = 30.sp)
    val answers = (s.targets.map { it.substringAfter(" → ") } + s.mission.distractors + when (s.mission.place) {
        "불국사" -> listOf("석가탑", "다보탑")
        "하회마을" -> listOf("양반탈", "각시탈")
        else -> listOf("일반 주택")
    }).distinct().reversed()
    answers.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { answer ->
                OutlinedButton(onClick = { choose("${target?.substringBefore(" → ")} → $answer") }, enabled = !s.done, modifier = Modifier.weight(1f).heightIn(min = 140.dp)) {
                    Column {
                        ObjectPicture(answer, Modifier.fillMaxWidth().height(80.dp))
                        Text(answer, fontSize = 28.sp)
                    }
                }
            }
        }
    }
    Text(s.found.joinToString("\n"), fontSize = 26.sp)
}
