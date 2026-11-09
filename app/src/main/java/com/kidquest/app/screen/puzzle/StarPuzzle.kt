// 별을 차례로 누르거나 손가락으로 이어 그려 완성하는 북두칠성 모양 체험.
package com.kidquest.app.screen.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.screen.model.MissionSession

@Composable
fun StarPuzzle(s: MissionSession, choose: (String) -> Unit) {
    val points = listOf(.12f to .70f, .28f to .53f, .44f to .49f, .60f to .40f, .79f to .31f, .85f to .64f, .64f to .75f).take(s.starCount)
    val action = rememberUpdatedState(choose)
    Text("${s.starCount}개의 별을 순서대로 이어 보세요. 누르거나 따라 그릴 수 있어요.", fontSize = 28.sp)
    val touch = Modifier.pointerInput(s) {
        fun connect(position: Offset) {
            points.forEachIndexed { i, (x, y) ->
                if ((position - Offset(size.width * x, size.height * y)).getDistance() < 32.dp.toPx()) action.value((i + 1).toString())
            }
        }
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            connect(down.position)
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { change ->
                    connect(change.position)
                    if (change.positionChanged()) change.consume()
                }
            } while (event.changes.any { it.pressed })
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(280.dp).testTag("star-canvas").then(touch)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(Color(0xFF182A45), cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()))
            repeat(36) { i -> drawCircle(Color.White.copy(alpha = .3f), 1.5.dp.toPx(), Offset(size.width * ((i * 31 % 97) / 100f), size.height * ((i * 17 % 89) / 100f))) }
            for (i in 1 until s.sequence.size) {
                val a = points[i - 1]; val b = points[i]
                drawLine(Color(0xFFF4D071), Offset(size.width * a.first, size.height * a.second), Offset(size.width * b.first, size.height * b.second), 5.dp.toPx())
            }
            if (!s.preschool) {
                val a = points[0]; val b = points[1]
                drawLine(Color.White.copy(alpha = .25f), Offset(size.width * a.first, size.height * a.second), Offset(size.width * b.first, size.height * b.second), 2.dp.toPx())
            }
            points.forEachIndexed { index, (x, y) ->
                val center = Offset(size.width * x, size.height * y)
                if (index == s.sequence.size && s.preschool) drawCircle(Color(0xFFFFD777).copy(alpha = .3f), 26.dp.toPx(), center)
                drawCircle(if (index < s.sequence.size) Color(0xFFFFD777) else Color.White, 9.dp.toPx(), center)
            }
        }
        points.forEachIndexed { i, (x, y) ->
            TextButton(onClick = { choose((i + 1).toString()) }, modifier = Modifier.offset(maxWidth * x - 24.dp, 280.dp * y - 24.dp).size(48.dp)) {
                Text("${i + 1}", color = Color(0xFFFEDB80), fontSize = 26.sp)
            }
        }
    }
    Text("연결한 별 ${s.sequence.size} / ${s.starCount}")
    if (s.done) Text(if (s.preschool) "별 모양을 완성했어요!" else "북두칠성은 국자처럼 보이는 일곱 별이에요.", fontSize = 28.sp)
    Text("천문 교육을 위한 창작 퍼즐이며 첨성대의 실제 사용 방식을 재현한 것은 아니에요.", fontSize = 20.sp)
}
