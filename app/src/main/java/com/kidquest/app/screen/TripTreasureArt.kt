// 본편 AwardItem 개수로 완성하는 정적인 보물 길. 주행 규제 화면에서는 구성하지 않는다.
package com.kidquest.app.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.screen.common.Mint

@Composable
fun TripTreasureArt(collected: Int, total: Int) {
    val slots = total.coerceAtLeast(1)
    val found = collected.coerceIn(0, slots)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (found == 0) "오늘은 이야기를 모았어요. 보물은 다음에 찾아도 좋아요." else if (found == slots) "모든 보물을 모아 탐험 지도를 완성했어요!" else "찾아낸 보물로 오늘의 탐험 지도를 채웠어요.", fontSize = 28.sp)
        Canvas(Modifier.fillMaxWidth().height(180.dp).testTag("trip-treasure-art").semantics {
            contentDescription = "탐험 지도 보물 $found / $total"
        }) {
            val margin = size.width * .08f
            val step = (size.width - margin * 2) / slots
            val radius = minOf(step * .28f, 22.dp.toPx())
            val y = size.height * .46f
            drawRoundRect(Color(0xFFF0E5C9), Offset(margin * .4f, size.height * .08f),
                Size(size.width - margin * .8f, size.height * .88f), androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
            val road = Path().apply { moveTo(margin, y); lineTo(size.width - margin, y) }
            drawPath(road, Color(0xFFD9CFB5), style = Stroke(12.dp.toPx()))
            repeat(slots) { i ->
                val x = margin + step * (i + .5f)
                drawCircle(if (i < found) Mint else Color(0xFFD9DCD4), radius, Offset(x, y))
                if (i < found) {
                    drawPath(Path().apply {
                        moveTo(x - radius * .45f, y); lineTo(x - radius * .1f, y + radius * .35f); lineTo(x + radius * .5f, y - radius * .35f)
                    }, Color.White, style = Stroke(3.dp.toPx()))
                }
            }
            val box = Size(64.dp.toPx(), 36.dp.toPx())
            val chest = Offset((size.width - box.width) / 2, size.height * .72f)
            drawRoundRect(Color(0xFFC5973C), chest, box, androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
            drawLine(Color(0xFF936824), chest + Offset(0f, box.height * .35f), chest + Offset(box.width, box.height * .35f), 3.dp.toPx())
            drawRect(Color(0xFFFFE8A6), chest + Offset(box.width * .43f, box.height * .2f), Size(box.width * .14f, box.height * .45f))
        }
    }
}
