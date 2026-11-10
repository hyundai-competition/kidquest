// 비활성 보기는 클릭 동작 자체가 없는 Surface로 그린다. 접근성에도 눌림 불가와 버튼 역할을 전달한다.
package com.kidquest.app.screen.common

import com.kidquest.app.screen.AppText as Text
import com.kidquest.app.screen.LocalAppLanguage
import com.kidquest.app.screen.englishUi
import com.kidquest.core.contract.Language
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnswerOptionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val accessibleLabel = if (LocalAppLanguage.current == Language.EN) englishUi(label) else label
    val modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
    if (enabled) {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label, fontSize = 28.sp) }
    } else {
        Surface(modifier = modifier.clearAndSetSemantics { role = Role.Button; disabled(); text = AnnotatedString(accessibleLabel) },
            shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .3f))) {
            Box(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(label, fontSize = 28.sp)
            }
        }
    }
}
