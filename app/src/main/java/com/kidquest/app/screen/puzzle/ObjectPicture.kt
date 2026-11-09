// 퍼즐 조각용 벡터 그림. 유물과 건축 요소를 단순한 모양으로 구분해 외부 이미지 없이 표시한다.
package com.kidquest.app.screen.puzzle

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate

@Composable
fun ObjectPicture(name: String, modifier: Modifier, faded: Boolean = false) {
    Canvas(modifier) {
        val w = minOf(size.width, size.height * 1.6f); val h = size.height
        val ink = Color(0xFF4E7165).copy(alpha = if (faded) .22f else 1f)
        val gold = Color(0xFFD4AA55).copy(alpha = if (faded) .22f else 1f)
        fun rect(x: Float, y: Float, width: Float, height: Float, color: Color = ink) = drawRect(color, Offset(w * x, h * y), Size(w * width, h * height))
        translate(left = (size.width - w) / 2f) {
        when {
            name.contains("스마트폰") || name.contains("노트북") || name.contains("키보드") -> {
                rect(.25f, .08f, .5f, .8f)
                rect(.3f, .16f, .4f, .55f, Color(0xFF9BC4D0))
                drawCircle(gold, h * .04f, Offset(w * .5f, h * .79f))
            }
            name.contains("자동차") -> {
                rect(.12f, .45f, .76f, .3f, gold); rect(.28f, .22f, .43f, .3f)
                listOf(.28f, .73f).forEach { x -> drawCircle(ink, h * .12f, Offset(w * x, h * .78f)) }
            }
            name.contains("운동화") -> {
                val shoe = Path().apply { moveTo(w * .2f, h * .15f); lineTo(w * .4f, h * .2f); lineTo(w * .55f, h * .55f); lineTo(w * .84f, h * .65f); lineTo(w * .85f, h * .85f); lineTo(w * .15f, h * .85f); close() }
                drawPath(shoe, ink)
            }
            name.contains("의복") || name.contains("티셔츠") -> {
                val clothes = Path().apply { moveTo(w * .35f, h * .15f); lineTo(w * .65f, h * .15f); lineTo(w * .9f, h * .4f); lineTo(w * .72f, h * .6f); lineTo(w * .65f, h * .49f); lineTo(w * .7f, h * .92f); lineTo(w * .3f, h * .92f); lineTo(w * .35f, h * .49f); lineTo(w * .28f, h * .6f); lineTo(w * .1f, h * .4f); close() }
                drawPath(clothes, if (name.contains("왕실")) gold else ink)
            }

            name.contains("금관") || name.contains("어보") -> {
                val crown = Path().apply {
                    moveTo(w * .2f, h * .8f); lineTo(w * .15f, h * .2f); lineTo(w * .35f, h * .45f)
                    lineTo(w * .5f, h * .08f); lineTo(w * .65f, h * .45f); lineTo(w * .85f, h * .2f); lineTo(w * .8f, h * .8f); close()
                }
                drawPath(crown, gold)
            }
            name.contains("탑") -> {
                rect(.28f, .82f, .44f, .09f)
                repeat(3) { i ->
                    val y = .65f - i * .23f
                    rect(.38f, y, .24f, .16f)
                    rect(.25f + i * .035f, y - .06f, .5f - i * .07f, .07f)
                }
                if (name.contains("다보")) repeat(4) { i -> rect(.25f + i * .15f, .54f, .035f, .3f, gold) }
            }
            name.contains("탈") -> {
                drawOval(gold, Offset(w * .3f, h * .06f), Size(w * .4f, h * .87f))
                drawLine(ink, Offset(w * .37f, h * .37f), Offset(w * .44f, h * .34f), h * .05f)
                drawLine(ink, Offset(w * .56f, h * .34f), Offset(w * .63f, h * .37f), h * .05f)
                drawArc(ink, 0f, 180f, false, Offset(w * .4f, h * .53f), Size(w * .2f, h * .16f), style = Stroke(h * .05f))
                if (name.contains("각시")) drawCircle(Color(0xFFC47468), h * .05f, Offset(w * .38f, h * .51f))
            }
            name.contains("연못") || name.contains("섬") -> {
                drawOval(Color(0xFF92C3CD).copy(alpha = if (faded) .22f else 1f), Offset(w * .1f, h * .25f), Size(w * .8f, h * .6f))
                if (name.contains("섬")) drawOval(ink, Offset(w * .35f, h * .38f), Size(w * .3f, h * .25f))
            }
            name.contains("나무") -> { rect(.47f, .45f, .07f, .48f, gold); drawCircle(ink, h * .32f, Offset(w * .5f, h * .35f)) }
            name.contains("귀걸이") -> {
                listOf(.35f, .65f).forEach { x -> drawCircle(gold, h * .18f, Offset(w * x, h * .3f), style = Stroke(h * .07f)); drawLine(gold, Offset(w * x, h * .5f), Offset(w * x, h * .9f), h * .07f) }
            }
            name.contains("토기") -> { drawOval(gold, Offset(w * .27f, h * .3f), Size(w * .46f, h * .6f)); rect(.4f, .13f, .2f, .25f, gold) }
            name.contains("붓") -> { drawLine(gold, Offset(w * .62f, h * .08f), Offset(w * .4f, h * .75f), h * .09f); drawCircle(ink, h * .09f, Offset(w * .39f, h * .8f)) }
            name.contains("나침반") -> { drawCircle(gold, h * .4f, Offset(w * .5f, h * .5f), style = Stroke(h * .06f)); drawLine(ink, Offset(w * .42f, h * .78f), Offset(w * .58f, h * .22f), h * .07f) }
            name.contains("망원경") -> { drawLine(gold, Offset(w * .25f, h * .75f), Offset(w * .75f, h * .25f), h * .24f) }
            name.contains("편종") || name.contains("편경") -> {
                rect(.18f, .15f, .64f, .08f, gold)
                repeat(5) { i -> rect(.22f + i * .12f, .3f, .07f, .35f + (i % 2) * .15f) }
            }
            name.contains("건물") || name.contains("전") || name.contains("루") || name.contains("문") || name.contains("정자") || name.contains("지붕") -> {
                rect(.25f, .42f, .5f, .43f, gold)
                val roof = Path().apply { moveTo(w * .1f, h * .43f); lineTo(w * .5f, h * .08f); lineTo(w * .9f, h * .43f); close() }
                drawPath(roof, ink); rect(.44f, .55f, .12f, .3f)
            }
            else -> { drawRoundRect(gold, Offset(w * .25f, h * .12f), Size(w * .5f, h * .76f), androidx.compose.ui.geometry.CornerRadius(h * .08f)); rect(.32f, .26f, .36f, .04f); rect(.32f, .4f, .36f, .04f) }
        }
        }
    }
}
