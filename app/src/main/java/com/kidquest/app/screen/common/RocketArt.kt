// 시작 화면에서 쓰는 공통 그림과 색상.
package com.kidquest.app.screen.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

import com.kidquest.app.screen.common.*

@Composable
fun RocketArt(modifier: Modifier) {
    Canvas(modifier) {
        val x = size.width * .48f
        val y = size.height * .48f
        drawCircle(Color(0xFF1C3150), size.height * .46f, Offset(x, y))
        listOf(.12f to .22f, .8f to .18f, .88f to .72f, .18f to .78f).forEach { (sx, sy) ->
            drawCircle(Mint, 4.dp.toPx(), Offset(size.width * sx, size.height * sy))
        }
        val unit = size.height / 220f
        val hull = Path().apply {
            moveTo(x, y - 86 * unit)
            cubicTo(x + 60 * unit, y - 30 * unit, x + 42 * unit, y + 44 * unit, x + 30 * unit, y + 64 * unit)
            lineTo(x - 30 * unit, y + 64 * unit)
            cubicTo(x - 42 * unit, y + 44 * unit, x - 60 * unit, y - 30 * unit, x, y - 86 * unit)
            close()
        }
        drawPath(hull, Color(0xFFE8EFF7))
        drawCircle(Night, 23 * unit, Offset(x, y - 12 * unit))
        drawCircle(Mint, 15 * unit, Offset(x, y - 12 * unit))
        val flame = Path().apply {
            moveTo(x - 18 * unit, y + 72 * unit)
            lineTo(x, y + 104 * unit)
            lineTo(x + 18 * unit, y + 72 * unit)
            close()
        }
        drawPath(flame, Color(0xFFFFCB80))
    }
}
