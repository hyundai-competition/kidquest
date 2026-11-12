// 주행 제한 중에는 캐릭터와 음성 상태, 소리 버튼을 한가운데 모은다.
package com.kidquest.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.screen.common.Panel

@Composable
internal fun DrivingStatusCard(model: AppViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        VroomieCompanion(model.controller.vehicle, Modifier.size(260.dp))
        Surface(Modifier.widthIn(max = 760.dp).fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = Panel) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                AppText(model.controller.status ?: titleOf(model.controller.screen),
                    Modifier.fillMaxWidth().testTag("restricted-status"), fontSize = 32.sp,
                    lineHeight = 46.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                StopButton(model)
            }
        }
    }
}
