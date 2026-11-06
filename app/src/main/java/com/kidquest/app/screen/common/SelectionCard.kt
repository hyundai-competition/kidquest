// 연령과 목적지 선택에 공통으로 쓰는 카드. 선택한 카드는 민트색 테두리와 배경으로 구분한다.
package com.kidquest.app.screen.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SelectionCard(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 88.dp).semantics { selected = isSelected },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Mint else Color(0xFF65718A)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) Mint.copy(alpha = 0.15f) else Color.Transparent,
            contentColor = if (isSelected) Mint else Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp),
    ) {
        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
    }
}
