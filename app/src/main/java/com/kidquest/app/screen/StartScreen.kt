// 언어 → 경로 → 테마의 세 단계. 선택값과 단계는 ViewModel이 보관한다.
package com.kidquest.app.screen

import com.kidquest.app.screen.AppText as Text
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.screen.common.SelectionCard
import com.kidquest.app.screen.common.Mint
import com.kidquest.app.screen.start.RouteStep
import com.kidquest.app.screen.start.ThemeStep
import com.kidquest.core.contract.Language

@Composable
fun StartScreen(model: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("${model.setupStep + 1} / 3  ·  언어 → 경로 → 테마", color = Mint, fontSize = 28.sp, lineHeight = 40.sp)
        when (model.setupStep) {
            0 -> DepartureSettings(model)
            1 -> RouteStep(model)
            else -> ThemeStep(model)
        }
        if (model.setupStep > 0) {
            TextButton(onClick = { model.changeStep(model.setupStep - 1) }) {
                Text("← 이전 단계", fontSize = 30.sp, lineHeight = 42.sp)
            }
        }
    }
}

@Composable
private fun DepartureSettings(model: AppViewModel) {
    ShowcaseChoice(model)
    Text("작은 탐험가, 준비됐나요?", fontSize = 40.sp, lineHeight = 54.sp)
    Text("한국어 또는 영어로 함께 떠나요", fontSize = 30.sp, lineHeight = 42.sp, color = Mint)
    Text("이야기 언어 / Story language", fontSize = 34.sp, lineHeight = 48.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        model.languages.forEach { language ->
            SelectionCard(if (language == Language.KO) "한국어" else "English · 12세 이상 권장",
                model.language == language, { model.selectLanguage(language) }, Modifier.weight(1f))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Switch(checked = model.consent, onCheckedChange = model::setListeningConsent, modifier = Modifier.testTag("listening-consent"))
        Column(Modifier.weight(1f)) {
            Text("말로 대답하기", modifier = Modifier.fillMaxWidth(), fontSize = 34.sp, lineHeight = 48.sp)
            Text("선택 사항 · 꺼도 이야기를 들을 수 있어요", fontSize = 28.sp, lineHeight = 40.sp)
        }
    }
    Button(onClick = { model.changeStep(1) }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)) {
        Text("경로 고르기 →", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 34.sp, lineHeight = 48.sp)
    }
    Text("정차 중에 설정해 주세요. 브루미는 차량의 안전벨트 경고를 대신하지 않아요.", separateSentences = true, fontSize = 28.sp, lineHeight = 40.sp)
}
