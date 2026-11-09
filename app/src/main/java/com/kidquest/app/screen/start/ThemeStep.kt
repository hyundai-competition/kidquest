// 역사 보물찾기만 선택 가능하다. 나머지 세 테마는 잠금 상태로 눌러도 선택되지 않는다.
package com.kidquest.app.screen.start

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.app.AppViewModel
import com.kidquest.app.screen.common.Mint

@Composable
fun ThemeStep(model: AppViewModel) {
    Text("어떤 모험을 할까요?", fontSize = 42.sp, lineHeight = 56.sp)
    Text(model.selectedRoute.label, fontSize = 30.sp, lineHeight = 42.sp, color = Mint)
    model.themes.chunked(2).forEach { themes ->
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            themes.forEach { theme ->
                OutlinedButton(
                    onClick = { model.selectTheme(theme.id) }, enabled = theme.available,
                    modifier = Modifier.weight(1f).heightIn(min = 124.dp).semantics { selected = model.themeId == theme.id },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (theme.available) Mint else MaterialTheme.colorScheme.outline.copy(alpha = .25f)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (theme.available) Mint.copy(alpha = .08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .2f)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (theme.available) "선택됨" else "잠김 · 곧 만나요", fontSize = 26.sp, lineHeight = 36.sp)
                        Text(theme.name, fontSize = 32.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    Button(onClick = { model.changeStep(2) }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)) {
        Text("출발 준비 →", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 34.sp, lineHeight = 48.sp)
    }
}
