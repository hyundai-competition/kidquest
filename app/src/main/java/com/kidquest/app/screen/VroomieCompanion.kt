// 차량 신호에 맞춰 브루미의 정적인 포즈를 바꾼다. 좌우 반응은 같은 원화를 반전해 표시한다.
package com.kidquest.app.screen

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.kidquest.app.R
import com.kidquest.app.screen.model.VehicleUiState
import com.kidquest.core.contract.TurnSide

@Composable
fun VroomieCompanion(vehicle: VehicleUiState, modifier: Modifier = Modifier) {
    val turning = vehicle.turnSide != null
    val image = when {
        vehicle.wiperOn && turning -> R.drawable.vroomie_rain_turn
        vehicle.wiperOn -> R.drawable.vroomie_rain
        turning -> R.drawable.vroomie_turn
        else -> R.drawable.vroomie_idle
    }
    val pose = (if (vehicle.wiperOn) "rain" else "dry") + "-" +
        (vehicle.turnSide?.name?.lowercase() ?: "idle")
    // 포즈는 즉시 교체한다. 주행·규제 중에도 흔들림·깜빡임·이동 애니메이션이 없다.
    Image(
        painterResource(image), contentDescription = null, contentScale = ContentScale.Fit,
        modifier = modifier.testTag("vroomie-companion").semantics { stateDescription = pose }
            .graphicsLayer { scaleX = if (vehicle.turnSide == TurnSide.RIGHT) -1f else 1f },
    )
}
