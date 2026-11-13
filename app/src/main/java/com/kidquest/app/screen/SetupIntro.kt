// 고정 소개와 브루미를 설정 화면의 폭에 맞춰 배치한다.
package com.kidquest.app.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.screen.common.Mint

@Composable
internal fun SetupIntro(model: AppViewModel, modifier: Modifier, large: Boolean) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppText("Vroomie", color = Mint, fontSize = if (large) 52.sp else 36.sp, fontWeight = FontWeight.ExtraBold)
        AppText("브루미와 떠나는 역사 보물찾기", fontSize = if (large) 48.sp else 34.sp,
            lineHeight = if (large) 64.sp else 48.sp, fontWeight = FontWeight.Bold)
        if (large) VroomieCompanion(model.controller.vehicle, Modifier.fillMaxWidth().height(300.dp))
    }
}
