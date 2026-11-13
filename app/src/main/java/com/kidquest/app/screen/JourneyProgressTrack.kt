// 현재 이야기 구간에 브루미를 세우고 방문·보상 상태를 함께 보여준다.
package com.kidquest.app.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.screen.common.Mint
import com.kidquest.app.screen.model.JourneyProgress
import com.kidquest.app.screen.model.VehicleUiState
import com.kidquest.core.contract.Language

@Composable
internal fun JourneyProgressTrack(progress: JourneyProgress, vehicle: VehicleUiState) {
    val english = LocalAppLanguage.current == Language.EN
    AppText(if (english) "Story progress · ${progress.percent}%" else "이야기 진행도 · ${progress.percent}%",
        fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Mint)
    BoxWithConstraints(Modifier.fillMaxWidth().height(220.dp).testTag("journey-progress")) {
        val fraction = if (progress.labels.size < 2) 0f else progress.currentIndex.toFloat() / progress.labels.lastIndex
        val inset = 32.dp
        val travel = (maxWidth - inset * 2).coerceAtLeast(0.dp)
        val markerSize = minOf(240.dp, maxWidth)
        Canvas(Modifier.fillMaxWidth().height(210.dp)) {
            val start = Offset(inset.toPx(), size.height - 16.dp.toPx())
            val end = Offset(size.width - inset.toPx(), start.y)
            drawLine(Color(0xFFC8D3BD), start, end, 20.dp.toPx(), StrokeCap.Round)
            drawLine(Mint, start, Offset(start.x + (end.x - start.x) * fraction, start.y), 12.dp.toPx(), StrokeCap.Round)
            progress.labels.indices.forEach { index ->
                val x = start.x + (end.x - start.x) * index / maxOf(1, progress.labels.lastIndex)
                drawCircle(if (index <= progress.currentIndex) Mint else Color.White, 9.dp.toPx(), Offset(x, start.y))
            }
        }
        VroomieCompanion(vehicle, Modifier.offset(x = (inset + travel * fraction - markerSize / 2).coerceIn(0.dp, maxWidth - markerSize), y = (-20).dp).size(markerSize))
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 900.dp) progress.labels.size.coerceAtLeast(1) else 2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            progress.labels.indices.toList().chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { index ->
                        Column(Modifier.weight(1f).background(if (index == progress.currentIndex) Color(0xFFF5DCA0) else Color(0xFFFFFEF8), RoundedCornerShape(20.dp)).padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppText(progress.labels[index], fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            val label = when {
                                index == progress.currentIndex && index in progress.completedIndices -> if (english) "Current · Treasure found ✓" else "현재 구간 · 보물 획득 ✓"
                                index == progress.currentIndex -> if (english) "Current section" else "현재 구간"
                                index in progress.completedIndices -> if (english) "Treasure found ✓" else "보물 획득 ✓"
                                index < progress.currentIndex -> if (english) "Passed" else "지나온 구간"
                                else -> if (english) "Coming up" else "다음 구간"
                            }
                            AppText(label, fontSize = 20.sp, textAlign = TextAlign.Center, color = Mint)
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
