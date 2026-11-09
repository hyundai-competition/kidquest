// 성공 시 한 번만 커지는 보물 지도와 흩어지는 빛 조각. 주행 규제 화면에서는 구성하지 않는다.
package com.kidquest.app.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun RewardCelebration(collected: Int) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1100)) }
    val scale = .7f + .3f * progress.value
    Canvas(Modifier.fillMaxWidth().height(240.dp).testTag("reward-art").graphicsLayer { scaleX = scale; scaleY = scale }) {
        val side = minOf(size.width * .60f, size.height * .68f)
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        repeat(4) { i ->
            val x = left + (i % 2) * side * .52f
            val y = top + (i / 2) * side * .52f
            val color = if (i < collected) Color(0xFFE4BA5E) else Color(0xFFDDDCCF)
            drawRoundRect(color, Offset(x, y), Size(side * .47f, side * .47f), androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()))
            drawCircle(Color.White.copy(alpha = .55f), side * .04f, Offset(x + side * .23f, y + side * .23f))
        }
        repeat(14) { i ->
            val angle = (i * 2 * PI / 14).toFloat()
            val radius = side * (.65f + progress.value * .30f)
            val center = Offset(size.width / 2 + cos(angle) * radius, size.height / 2 + sin(angle) * radius * .65f)
            drawCircle(if (i % 2 == 0) Color(0xFFC5973C) else Color(0xFF54947A), (3 + i % 3).dp.toPx(), center)
        }
    }
}
