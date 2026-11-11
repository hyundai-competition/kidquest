// 와이퍼에 반응하는 장식 배경. P에서만 빗방울이 움직이고 주행 중에는 정적으로 그린다.
package com.kidquest.app.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kidquest.app.screen.model.VehicleUiState
import com.kidquest.core.contract.Gear

enum class WeatherStyle { RAIN, SNOW }

@Composable
fun VehicleBackdrop(vehicle: VehicleUiState, style: WeatherStyle, content: @Composable () -> Unit) {
    var offset = 0f
    if (vehicle.wiperOn && vehicle.gear == Gear.P && !vehicle.restricted) {
        val motion = rememberInfiniteTransition(label = "parked-weather")
        offset = motion.animateFloat(0f, 1f, infiniteRepeatable(tween(3500, easing = LinearEasing)), label = "drops").value
    }
    Box(Modifier.fillMaxSize()) {
        content()
        Canvas(Modifier.fillMaxSize()) {
            if (vehicle.wiperOn) {
                drawRect(if (style == WeatherStyle.RAIN) Color(0xFF76A6CE).copy(alpha = .05f) else Color(0xFFA2C9D6).copy(alpha = .07f))
                repeat(32) { i ->
                    // 글을 가리지 않도록 가장자리에만 연출한다.
                    val x = if (i % 2 == 0) size.width * ((i % 5 + 1) * .009f) else size.width * (1f - (i % 5 + 1) * .009f)
                    val y = ((i * .071f + offset) % 1f) * size.height
                    if (style == WeatherStyle.RAIN) drawLine(Color(0xFF6296BE).copy(alpha = .5f), Offset(x, y), Offset(x - 3.dp.toPx(), y + 12.dp.toPx()), 2.dp.toPx())
                    else {
                        drawCircle(Color.White.copy(alpha = .9f), 4.dp.toPx(), Offset(x, y))
                        drawCircle(Color(0xFF91BACA), 4.dp.toPx(), Offset(x, y), style = Stroke(1.dp.toPx()))
                    }
                }
            }
        }
    }
}
