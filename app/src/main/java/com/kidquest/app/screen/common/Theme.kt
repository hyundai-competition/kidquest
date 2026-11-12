// 시작 화면에서 쓰는 공통 그림과 색상.
package com.kidquest.app.screen.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Night = Color(0xFFF5F3EC)
val Panel = Color(0xFFFFFEFA)
val Mint = Color(0xFF27685B)
val Muted = Color(0xFF747D72)

@Composable
fun KidQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Mint, onPrimary = Color.White, background = Night, surface = Panel, onSurface = Color(0xFF25392F), onBackground = Color(0xFF25392F)),
        typography = Typography(
            bodyLarge = TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading, hyphens = androidx.compose.ui.text.style.Hyphens.None, fontSize = 28.sp, lineHeight = 40.sp),
            bodyMedium = TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading, hyphens = androidx.compose.ui.text.style.Hyphens.None, fontSize = 26.sp, lineHeight = 38.sp),
            bodySmall = TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading, hyphens = androidx.compose.ui.text.style.Hyphens.None, fontSize = 22.sp, lineHeight = 32.sp),
            labelLarge = TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading, hyphens = androidx.compose.ui.text.style.Hyphens.None, fontSize = 26.sp, lineHeight = 36.sp),
            labelMedium = TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading, hyphens = androidx.compose.ui.text.style.Hyphens.None, fontSize = 24.sp, lineHeight = 34.sp),
            labelSmall = TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak.Heading, hyphens = androidx.compose.ui.text.style.Hyphens.None, fontSize = 22.sp, lineHeight = 32.sp),
        ),
        content = content,
    )
}
