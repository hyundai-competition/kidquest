// 목적지별 랜드마크와 보물상자를 그리는 장식 일러스트.
package com.kidquest.app.screen.common

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun TreasureMapArt(modifier: Modifier, city: String = "경주") {
    Canvas(modifier) {
        // 고정된 원화 비율로 확대·축소해 가로로 긴 영역에서도 그림이 눌리지 않게 한다.
        val scale = minOf(size.width / 800f, size.height / 500f)
        withTransform({
            translate((size.width - 800f * scale) / 2f, (size.height - 500f * scale) / 2f)
            scale(scale, scale, Offset.Zero)
        }) { drawDestination(city) }
    }
}

private fun DrawScope.drawDestination(city: String) {
    val w = 800f
    val h = 500f
    val green = Color(0xFF85A889)
    val gold = Color(0xFFCA9B45)
    drawOval(Color(0xFFE7EBD9), Offset(w * .03f, h * .1f), Size(w * .91f, h * .84f))
    val hill = Path().apply {
        moveTo(w * .02f, h * .75f)
        cubicTo(w * .20f, h * .20f, w * .25f, h * .63f, w * .45f, h * .5f)
        cubicTo(w * .6f, h * .17f, w * .8f, h * .35f, w * .98f, h * .75f)
        close()
    }
    drawPath(hill, green.copy(alpha = .5f))
    when (city) {
        "부산" -> drawBusan(w, h)
        "안동" -> drawHanok(w, h, palace = false)
        "서울" -> drawHanok(w, h, palace = true)
        else -> {
            // 탑의 곡선과 수평 돌층은 장식이며 실제 건축 도면은 아니다.
            val tower = Path().apply {
                moveTo(w * .18f, h * .77f)
                quadraticBezierTo(w * .27f, h * .48f, w * .25f, h * .25f)
                lineTo(w * .4f, h * .25f)
                quadraticBezierTo(w * .38f, h * .48f, w * .47f, h * .77f)
                close()
            }
            drawPath(tower, Color(0xFFD6C5A1))
            for (i in 0..7) {
                val y = h * (.30f + i * .055f)
                drawLine(Color(0xFFA99877), Offset(w * .26f, y), Offset(w * .39f, y), 2f)
            }
            drawRect(Color(0xFF596758), Offset(w * .29f, h * .42f), Size(w * .065f, h * .085f))
            drawRect(Color(0xFFC5B38F), Offset(w * .23f, h * .20f), Size(w * .19f, h * .045f))
        }
    }
    cityDetails(city, w, h)
    for (i in 0..12) {
        val t = i / 12f
        val x = w * (.48f + .3f * t)
        val y = h * (.8f - .13f * kotlin.math.sin(t * 3.14f))
        drawCircle(Color(0xFFF9FAF2), h * .009f, Offset(x, y))
    }
    drawRoundRect(gold, Offset(w * .69f, h * .49f), Size(w * .22f, h * .19f), androidx.compose.ui.geometry.CornerRadius(12f))
    drawLine(Color(0xFF765E37), Offset(w * .69f, h * .56f), Offset(w * .91f, h * .56f), 4f)
    drawRect(Color(0xFFFFE3A0), Offset(w * .78f, h * .54f), Size(w * .04f, h * .07f))
    listOf(.62f to .28f, .86f to .32f, .55f to .45f).forEach { (x, y) ->
        drawLine(gold, Offset(w * x - 8, h * y), Offset(w * x + 8, h * y), 3f, StrokeCap.Round)
        drawLine(gold, Offset(w * x, h * y - 8), Offset(w * x, h * y + 8), 3f, StrokeCap.Round)
    }
}

// 부산은 바다와 현수교, 안동은 기와집, 서울은 궁궐 문루로 구분한다.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBusan(w: Float, h: Float) {
    drawOval(Color(0xFF9CCDD0), Offset(w * .04f, h * .56f), Size(w * .57f, h * .30f))
    val ink = Color(0xFF3D777E)
    listOf(.19f, .46f).forEach { x ->
        drawLine(ink, Offset(w * x, h * .25f), Offset(w * x, h * .73f), w * .018f)
    }
    drawLine(ink, Offset(w * .08f, h * .62f), Offset(w * .58f, h * .62f), h * .04f)
    val cable = Path().apply {
        moveTo(w * .08f, h * .57f)
        lineTo(w * .19f, h * .28f)
        quadraticBezierTo(w * .32f, h * .69f, w * .46f, h * .28f)
        lineTo(w * .58f, h * .57f)
    }
    drawPath(cable, ink, style = Stroke(w * .008f))
    for (i in 0..6) {
        val x = w * (.20f + i * .04f)
        drawLine(ink.copy(alpha = .5f), Offset(x, h * .48f), Offset(x, h * .61f), 2f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHanok(w: Float, h: Float, palace: Boolean) {
    val wood = Color(if (palace) 0xFFA75C47 else 0xFF94714F)
    drawRect(Color(0xFFE9D7B1), Offset(w * .12f, h * .49f), Size(w * .40f, h * .26f))
    listOf(.14f, .30f, .49f).forEach { x ->
        drawRect(wood, Offset(w * x, h * .48f), Size(w * .025f, h * .29f))
    }
    drawRect(Color(0xFF596758), Offset(w * .25f, h * .57f), Size(w * .13f, h * .18f))
    fun roof(y: Float, left: Float, right: Float) {
        val path = Path().apply {
            moveTo(w * left, h * (y + .14f))
            quadraticBezierTo(w * .20f, h * (y + .15f), w * .32f, h * y)
            quadraticBezierTo(w * .46f, h * (y + .15f), w * right, h * (y + .14f))
            lineTo(w * (right - .02f), h * (y + .20f))
            lineTo(w * (left + .02f), h * (y + .20f))
            close()
        }
        drawPath(path, Color(0xFF4D6C64))
    }
    roof(.28f, .06f, .59f)
    if (palace) {
        drawRect(wood, Offset(w * .23f, h * .26f), Size(w * .18f, h * .12f))
        roof(.10f, .16f, .48f)
    }
}
