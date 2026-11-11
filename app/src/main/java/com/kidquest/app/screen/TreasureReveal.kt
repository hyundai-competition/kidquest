// 도착 화면에서 이번 탐험에 모은 보물을 차례로 보여 준다. 차를 세운 뒤에 나오는 화면이라 움직임이 운전을 방해하지 않는다.
// 글자는 처음부터 그려 두고 투명도와 크기만 움직인다. 그래서 화면 읽기와 시험은 바로 보물 이름을 찾는다.
// 사진은 assets/treasures/<보물 id>.jpg(또는 .png, .webp)가 있을 때만 붙이고, 없으면 이름만 보인다. 사진 출처는 에피소드 credits에 적는다.
package com.kidquest.app.screen

import com.kidquest.app.screen.AppText as Text
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidquest.core.contract.Command
import java.io.IOException

@Composable
fun TreasureReveal(items: List<Command.AwardItem>) {
    val context = LocalContext.current
    val files = remember { treasureFiles(context) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { index, item ->
            TreasureRow(item, index, remember(item.itemId) { loadTreasurePhoto(context, pickTreasurePhoto(files, item.itemId)) })
        }
    }
}

@Composable
private fun TreasureRow(item: Command.AwardItem, index: Int, photo: ImageBitmap?) {
    val appear = remember(item.itemId) { Animatable(0f) }
    LaunchedEffect(item.itemId) { appear.animateTo(1f, tween(durationMillis = APPEAR_MILLIS, delayMillis = index * STEP_MILLIS)) }
    Row(
        Modifier.fillMaxWidth().testTag("treasure-${item.itemId}").graphicsLayer {
            alpha = appear.value
            scaleX = START_SCALE + (1f - START_SCALE) * appear.value
            scaleY = scaleX
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (photo != null) {
            Image(photo, contentDescription = item.name, contentScale = ContentScale.Crop,
                modifier = Modifier.size(PHOTO_DP.dp).clip(RoundedCornerShape(12.dp)).testTag("treasure-photo-${item.itemId}"))
        }
        Text("✦ ${item.name}", fontSize = 28.sp)
    }
}

/** assets/treasures/ 안의 파일 이름들. 폴더가 없으면 빈 목록. */
private fun treasureFiles(context: Context): Set<String> = try {
    context.assets.list(PHOTO_DIR).orEmpty().toSet()
} catch (e: IOException) {
    emptySet()
}

/** 이 보물의 사진 파일 경로. jpg, png, webp 순으로 찾고 없으면 null. */
internal fun pickTreasurePhoto(files: Set<String>, itemId: String): String? =
    PHOTO_TYPES.map { "$itemId.$it" }.firstOrNull { it in files }?.let { "$PHOTO_DIR/$it" }

private fun loadTreasurePhoto(context: Context, path: String?): ImageBitmap? {
    path ?: return null
    return try {
        context.assets.open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
    } catch (e: IOException) {
        null
    }
}

private const val PHOTO_DIR = "treasures"
private val PHOTO_TYPES = listOf("jpg", "png", "webp")
private const val PHOTO_DP = 96
private const val APPEAR_MILLIS = 450
private const val STEP_MILLIS = 500
private const val START_SCALE = 0.85f
