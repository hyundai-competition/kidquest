// 네 시연 경로를 두 열 카드로 고른다. 선택은 실제 내비 경로 설정과 구분된다.
package com.kidquest.app.screen.start

import com.kidquest.app.screen.AppText as Text
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.screen.common.SelectionCard

@Composable
fun RouteStep(model: AppViewModel) {
    Text("어디로 떠날까요?", fontSize = 42.sp, lineHeight = 56.sp)
    Text("함께 달릴 시연 경로를 골라 주세요", fontSize = 30.sp, lineHeight = 42.sp)
    model.routes.chunked(2).forEach { routes ->
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            routes.forEach { route ->
                SelectionCard(route.label, model.destinationId == route.id,
                    { model.selectDestination(route.id) }, Modifier.weight(1f))
            }
        }
    }
    if (model.travelRoutes.size > 1) {
        Text("가는 길도 골라 주세요", fontSize = 30.sp)
        model.travelRoutes.forEach { route ->
            SelectionCard(route.name, model.routeId == route.id, { model.selectTravelRoute(route.id) }, Modifier.fillMaxWidth())
        }
    } else model.activeTravelRoute?.let { Text(it.name, fontSize = 28.sp) }
    Text("네 가지 시연 경로 중 하나를 골라 탐험을 준비해요. 내비게이션 경로는 별도로 설정해 주세요.", fontSize = 28.sp, lineHeight = 40.sp)
    Button(onClick = { model.changeStep(2) }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)) {
        Text("테마 고르기 →", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 34.sp, lineHeight = 48.sp)
    }
}
