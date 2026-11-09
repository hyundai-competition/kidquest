// 도시별 탐험 소재를 더하는 장식. 역사 유물의 실측 재현이 아닌 코드 일러스트다.
package com.kidquest.app.screen.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.cityDetails(city: String, w: Float, h: Float) {
    val gold = Color(0xFFCA9B45)
    when (city) {
        "경주" -> {
            // 왕관과 연못, 밤하늘의 별을 보물찾기 소재로 함께 배치한다.
            val crown = Path().apply {
                moveTo(w * .48f, h * .43f)
                lineTo(w * .46f, h * .26f)
                lineTo(w * .52f, h * .33f)
                lineTo(w * .56f, h * .18f)
                lineTo(w * .60f, h * .33f)
                lineTo(w * .66f, h * .26f)
                lineTo(w * .64f, h * .43f)
                close()
            }
            drawPath(crown, gold)
            drawOval(Color(0xFF9BBFB2), Offset(w * .12f, h * .81f), Size(w * .48f, h * .10f))
            drawCircle(Color(0xFFFFE3A0), h * .025f, Offset(w * .56f, h * .34f))
        }
        "부산" -> {
            // 감천의 색색 집과 영화 필름, 바다 위 작은 배.
            listOf(0xFFD98C70, 0xFFE5BC63, 0xFF87A9BA).forEachIndexed { i, color ->
                val x = w * (.07f + i * .09f)
                val y = h * (.36f - i * .06f)
                drawRect(Color(color), Offset(x, y), Size(w * .075f, h * .16f))
                drawRect(Color(0xFFFFF5D8), Offset(x + w * .02f, y + h * .04f), Size(w * .02f, h * .04f))
            }
            drawCircle(Color(0xFF536F75), h * .08f, Offset(w * .73f, h * .24f))
            repeat(4) { i ->
                val angle = i * kotlin.math.PI / 2
                drawCircle(Color(0xFFDCE8DF), h * .018f, Offset(w * .73f + kotlin.math.cos(angle).toFloat() * h * .044f, h * .24f + kotlin.math.sin(angle).toFloat() * h * .044f))
            }
            drawLine(gold, Offset(w * .4f, h * .78f), Offset(w * .52f, h * .78f), h * .035f)
        }
        "안동" -> {
            // 하회탈을 연상시키는 웃는 얼굴과 붓, 작은 목조 다리.
            drawOval(Color(0xFFC69A68), Offset(w * .55f, h * .18f), Size(w * .15f, h * .27f))
            listOf(.59f, .65f).forEach { x ->
                drawLine(Color(0xFF654F3D), Offset(w * x, h * .28f), Offset(w * (x + .018f), h * .27f), h * .012f)
            }
            drawArc(Color(0xFF654F3D), 0f, 180f, false, Offset(w * .59f, h * .32f), Size(w * .075f, h * .06f), style = Stroke(h * .012f))
            drawLine(gold, Offset(w * .13f, h * .84f), Offset(w * .59f, h * .84f), h * .025f)
            drawLine(Color(0xFF654F3D), Offset(w * .78f, h * .22f), Offset(w * .72f, h * .42f), h * .018f)
        }
        else -> {
            // 궁궐의 단청, 왕실 유물을 연상시키는 금빛 장식과 돌길.
            repeat(5) { i ->
                drawCircle(Color(0xFFD8A254), h * .012f, Offset(w * (.17f + i * .075f), h * .49f))
                drawRect(Color(0xFFD4C7AB), Offset(w * (.19f + i * .05f), h * (.80f + (i % 2) * .035f)), Size(w * .04f, h * .025f))
            }
            drawRoundRect(gold, Offset(w * .61f, h * .22f), Size(w * .14f, h * .15f), androidx.compose.ui.geometry.CornerRadius(h * .025f))
            drawCircle(Color(0xFFEED49A), h * .025f, Offset(w * .68f, h * .29f))
        }
    }
}
