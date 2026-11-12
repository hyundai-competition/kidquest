// 본편 UI 언어를 선택값에 맞춘다. 보너스 퍼즐과 개발용 재생 패널은 별도 한국어 화면이다.
package com.kidquest.app.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.kidquest.core.contract.Language

val LocalAppLanguage = staticCompositionLocalOf { Language.KO }

@Composable
fun AppText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified, fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified, textAlign: TextAlign? = null,
    separateSentences: Boolean = false,
    maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip,
) {
    val label = appLabel(text).let { if (separateSentences) it.replace(". ", ".\n") else it }
    androidx.compose.material3.Text(label,
        style = androidx.compose.material3.LocalTextStyle.current.copy(
            lineBreak = androidx.compose.ui.text.style.LineBreak.Heading,
            hyphens = androidx.compose.ui.text.style.Hyphens.None,
            localeList = androidx.compose.ui.text.intl.LocaleList(if (LocalAppLanguage.current == Language.KO) "ko-KR" else "en-US")),
        modifier = modifier, color = color, fontSize = fontSize, fontWeight = fontWeight,
        lineHeight = lineHeight, textAlign = textAlign, maxLines = maxLines, overflow = overflow)
}

@Composable
internal fun appLabel(text: String): String = if (LocalAppLanguage.current == Language.EN) englishUi(text) else text

internal fun englishUi(text: String): String {
    UiEnglish.labels[text]?.let { return it }
    if (text.matches(Regex("[1-3]번 · .*"))) return text.replace("번 ·", " ·")
    if (text.startsWith("탐험 지도 보물 ")) return text.replace("탐험 지도 보물", "Adventure map treasures")
    if (text.startsWith("오늘 모은 보물 ")) return text.replace("오늘 모은 보물", "Treasures found today:").removeSuffix("개")
    if (text.startsWith("탑승 ")) return text.replace("탑승", "Occupied:").replace("벨트", "Belt:").replace("미확인", "Unknown").replace("미착용", "Unbuckled").replace("착용", "Buckled").replace("확인", "Yes").replace("없음", "No")
    if (text.startsWith("모은 보물 ")) return text.replace("모은 보물", "Treasures")
    if (text.startsWith("현재 기어:")) return text.replace("현재 기어:", "Gear:").replace("확인 중", "Checking")
    if (text.contains(" / 3  ·")) return text.substringBefore("·") + "· Language → Route → Theme"
    if (text.startsWith("미션 ")) return text.replace("미션", "Mission")
    if (text.startsWith("준비 중 ")) return text.replace("준비 중", "Coming soon")
    if (text.endsWith("에서 경주까지") || text.endsWith("에서 부산까지") || text.endsWith("에서 안동까지") || text.endsWith("에서 서울까지"))
        return text.replace("에서 ", " → ").replace("까지", "").let(::englishCities)
    return englishCities(text)
}

private fun englishCities(text: String): String = mapOf("서울" to "Seoul", "경주" to "Gyeongju", "부산" to "Busan", "안동" to "Andong", "대전" to "Daejeon", "천안" to "Cheonan", "대구" to "Daegu", "수원" to "Suwon", "충주" to "Chungju", "문경" to "Mungyeong", "단양" to "Danyang", "영주" to "Yeongju", "첨성대" to "Cheomseongdae", "임시수도" to "Provisional Capital", "하회마을" to "Hahoe Village", "경복궁" to "Gyeongbokgung").entries.fold(text) { value, entry -> value.replace(entry.key, entry.value) }
