// 선택한 시연 경로를 출발지부터 목적지까지 이어지는 정적인 보드 지도로 보여준다.
package com.kidquest.app.screen

import com.kidquest.app.screen.AppText as Text
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.screen.common.Mint
import com.kidquest.app.screen.common.Muted
import com.kidquest.app.screen.common.TreasureMapArt
import com.kidquest.app.screen.model.DemoRoute
import com.kidquest.app.screen.model.CityMission
import com.kidquest.app.screen.model.MissionNotes
import com.kidquest.app.screen.model.TripStop

@Composable
fun JourneyBoard(route: DemoRoute, completed: Set<String> = emptySet(), onMission: (CityMission) -> Unit = {}, skipped: Set<String> = emptySet(), tripStops: List<TripStop>? = null, onPlace: (String) -> Unit = {}, story: String? = null) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFE8EEDD), RoundedCornerShape(32.dp)).padding(24.dp)
            .testTag("journey-board"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Vroomie  /  TREASURE TRAIL", color = Mint, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("${route.from}에서 ${route.to}까지", fontSize = 38.sp, lineHeight = 50.sp, fontWeight = FontWeight.ExtraBold)
        Text(story ?: MissionNotes.stories.getValue(route.to), color = Mint, fontSize = 26.sp, lineHeight = 38.sp)
        RouteTiles(route, completed, onMission, skipped, tripStops, onPlace)
        Text(if (tripStops == null) "시연 모드 · 장소를 눌러 미션을 시작해요" else "시연 · 장소를 선택하면 현재 대사와 앞 임무가 끝난 뒤 이어져요", color = Muted, fontSize = 22.sp, lineHeight = 32.sp)
    }
}

@Composable
private fun RouteTiles(route: DemoRoute, completed: Set<String>, onMission: (CityMission) -> Unit, skipped: Set<String>, tripStops: List<TripStop>?, onPlace: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(490.dp)) {
        val tileWidth = minOf(210.dp, maxWidth * .29f)
        // 세 칸을 오른쪽으로 이동한 뒤 아래로 돌아오는 보드게임 길이다.
        val labels = tripStops?.let { listOf(route.from) + it.map { stop -> stop.label } } ?: route.stops
        val points = if (labels.size == 4) listOf(.16f to .22f, .5f to .22f, .84f to .22f, .16f to .78f)
            else listOf(.16f to .22f, .5f to .22f, .84f to .22f, .84f to .78f, .16f to .78f)
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(size.width * .16f, size.height * .22f)
                lineTo(size.width * .84f, size.height * .22f)
                cubicTo(size.width, size.height * .22f, size.width, size.height * .78f, size.width * .84f, size.height * .78f)
                lineTo(size.width * .16f, size.height * .78f)
            }
            drawPath(path, Color(0xFFCAD5B9), style = Stroke(32.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, Color(0xFFFFFEF8), style = Stroke(19.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, Color(0xFFB7C7A4), style = Stroke(2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))))
            drawCircle(Color(0xFFDCE5CC), 30.dp.toPx(), Offset(size.width * .43f, size.height * .5f))
        }
        Text("✦  보물을 향해 한 걸음씩  ✦", Modifier.align(Alignment.Center), color = Mint, fontSize = 22.sp)
        labels.forEachIndexed { index, city ->
            val (x, y) = points[index]
            Column(
                Modifier.offset(maxWidth * x - tileWidth / 2, 490.dp * y - 96.dp)
                    .width(tileWidth).height(192.dp)
                    .background(if (index == labels.lastIndex) Color(0xFFF5DCA0) else Color(0xFFFFFEF8), RoundedCornerShape(20.dp))
                    .then(if (index > 0) Modifier.testTag(if (tripStops != null) "place-${tripStops[index - 1].placeId}" else "bonus-place-$index").clickable {
                        if (tripStops != null) onPlace(tripStops[index - 1].placeId) else onMission(route.missions[index - 1])
                    } else Modifier)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(when { index == 0 -> "출발"; city in completed -> "완료 ✓"; city in skipped -> "다음 기회에"; else -> "미션 $index" }, color = Mint, fontSize = 20.sp)
                Text(city, fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (index == labels.lastIndex) TreasureMapArt(Modifier.fillMaxWidth().height(42.dp), route.to)
                else Text(if (index == 0) "START" else "눌러서 시작 →", color = Muted, fontSize = 18.sp)
            }
        }
    }
}
