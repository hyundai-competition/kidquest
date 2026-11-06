// 시작 화면에서 쓰는 공통 그림과 색상.
package com.kidquest.app.screen.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Night = Color(0xFF0B1224)
val Panel = Color(0xFF151F36)
val Mint = Color(0xFF91E6C5)
val Muted = Color(0xFFAAB8D0)

@Composable
fun KidQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Mint, onPrimary = Night, background = Night, surface = Panel),
        content = content,
    )
}
