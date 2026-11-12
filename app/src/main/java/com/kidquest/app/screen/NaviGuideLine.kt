// 화면 맨 아래에 내비 다음 안내 한 줄을 고정해 보여 준다(11-09 회의 결정 5번).
// 주행 중에도 보이므로 움직임 없이 글자 한 줄만 그린다. 탐험 중·멈춤·쉬는 중에만 보이고, 시작 설정과 착륙 화면에서는 숨긴다.
package com.kidquest.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.screen.model.NaviGuide
import com.kidquest.app.screen.model.NaviGuideText
import com.kidquest.core.contract.Screen

private val shownOn = setOf(Screen.FLYING, Screen.PAUSED, Screen.DOCKED)

/** 앱 화면(content)이 남은 높이를 다 쓰고, 그 아래에 안내 한 줄을 둔다. 스크롤해도 한 줄은 그대로 있다. */
@Composable
fun NaviGuideFrame(guide: NaviGuide?, screen: Screen, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { content() }
        NaviGuideLine(guide, screen)
    }
}

@Composable
private fun NaviGuideLine(guide: NaviGuide?, screen: Screen) {
    if (guide == null || screen !in shownOn) return
    Text(
        NaviGuideText.line(guide, LocalAppLanguage.current),
        modifier = Modifier.fillMaxWidth().background(Color(0xFF25392F)).navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp).testTag("navi-guide-line"),
        color = Color.White,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
