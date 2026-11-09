// 자유 색칠과 도개교 레버 조작. 조작 결과가 화면 장면에 바로 반영된다.
package com.kidquest.app.screen.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.screen.model.MissionSession

private val paints = listOf(Color(0xFFE4927B), Color(0xFFE9C35A), Color(0xFF79AECA), Color(0xFF8BB78B))

@Composable
fun ColorVillage(s: MissionSession, choose: (String) -> Unit) {
    Text("색을 고른 뒤 집을 눌러 알록달록한 언덕 마을을 만들어요. 정답은 없어요!", fontSize = 28.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        paints.forEachIndexed { i, color ->
            Box(Modifier.size(60.dp).background(color, CircleShape).clickable { s.paint = i }.semantics { contentDescription = "색상 ${i + 1}" }) {
                if (s.paint == i) Text("✓", modifier = Modifier.padding(16.dp), fontSize = 30.sp)
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(260.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val hill = Path().apply { moveTo(0f, size.height); lineTo(size.width, size.height * .12f); lineTo(size.width, size.height); close() }
            drawPath(hill, Color(0xFFDCE5CC))
        }
        s.targets.forEachIndexed { i, house ->
            val color = s.colors[house]?.let { paints[it] } ?: Color(0xFFCDD0C8)
            Button(onClick = { choose(house) }, colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color(0xFF25392F)), modifier = Modifier.offset(maxWidth * (.04f + i * .31f), (135 - i * 47).dp).width(maxWidth * .28f).height(108.dp)) {
                Column { Text("▣  ▣", fontSize = 30.sp); Text(house); if (house in s.colors) Text("색칠 완료") }
            }
        }
    }
    if (!s.preschool) Text("산비탈을 따라 집이 계단처럼 모여 있어요. 아래쪽과 위쪽 집의 위치를 살펴봐요.", fontSize = 28.sp)
}

@Composable
fun BridgePuzzle(s: MissionSession, choose: (String) -> Unit) {
    val raised = s.sequence.isNotEmpty() && (s.preschool || s.sequence.size < 3)
    val passed = s.done || s.sequence.size >= 2
    Text("배가 지나갈 수 있도록 다리 레버를 조작해요.", fontSize = 28.sp)
    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        val w = size.width; val h = size.height
        drawRect(Color(0xFFADD7E0), Offset(0f, h * .55f), Size(w, h * .45f))
        drawLine(Color(0xFF6D837A), Offset(0f, h * .55f), Offset(w * .3f, h * .55f), 18.dp.toPx())
        drawLine(Color(0xFF6D837A), Offset(w * .3f, h * .55f), Offset(w * (if (raised) .48f else .7f), h * (if (raised) .12f else .55f)), 18.dp.toPx())
        drawLine(Color(0xFF6D837A), Offset(w * .7f, h * .55f), Offset(w, h * .55f), 18.dp.toPx())
        val boatX = w * if (passed) .82f else .48f
        drawRoundRect(Color(0xFFD59A51), Offset(boatX - w * .07f, h * .75f), Size(w * .14f, h * .12f), androidx.compose.ui.geometry.CornerRadius(12f))
        drawLine(Color(0xFF43566A), Offset(boatX, h * .75f), Offset(boatX, h * .43f), 4.dp.toPx())
    }
    Text(if (s.done) "배가 안전하게 지나갔어요!" else "다음 조작: ${s.targets.getOrNull(s.sequence.size)}", fontSize = 28.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        s.targets.forEach { step ->
            Button(onClick = { choose(step) }, enabled = !s.done, modifier = Modifier.weight(1f).heightIn(min = 72.dp)) { Text(step, fontSize = 26.sp) }
        }
    }
}
