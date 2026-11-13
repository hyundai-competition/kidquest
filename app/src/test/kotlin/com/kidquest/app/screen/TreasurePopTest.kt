// 사진 보물 연출 시험. 가운데 큰 사진에서 왼쪽 작은 사진으로 줄어들고, P가 아니거나 사진이 없으면 처음부터 목록 모양이다.
package com.kidquest.app.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class TreasurePopTest {
    @Test fun startsBigInTheMiddleWithNameHidden() {
        val pop = photoPop(0f, 600f)
        assertEquals(200f, pop.sizeDp, 0.01f)
        assertEquals(200f, pop.offsetDp, 0.01f) // (600 - 200) / 2
        assertEquals(0f, pop.nameAlpha, 0.01f)
    }

    @Test fun endsAsTheSmallListPhotoOnTheLeft() {
        val pop = photoPop(1f, 600f)
        assertEquals(96f, pop.sizeDp, 0.01f)
        assertEquals(0f, pop.offsetDp, 0.01f)
        assertEquals(1f, pop.nameAlpha, 0.01f)
    }

    @Test fun shrinksAndMovesTogetherOnTheWay() {
        val pop = photoPop(0.5f, 600f)
        assertEquals(148f, pop.sizeDp, 0.01f)
        assertEquals(113f, pop.offsetDp, 0.01f) // (600 - 148) / 2 × 0.5
        assertEquals(0.5f, pop.nameAlpha, 0.01f)
    }

    @Test fun narrowRowNeverOverflows() {
        val pop = photoPop(0f, 150f)
        assertEquals(150f, pop.sizeDp, 0.01f)
        assertEquals(0f, pop.offsetDp, 0.01f)
    }

    @Test fun onlyParkedPhotoTreasuresStartBig() {
        assertEquals(0f, startSettle(animate = true, hasPhoto = true), 0f)
        assertEquals(1f, startSettle(animate = false, hasPhoto = true), 0f) // 주행 중이거나 규제 중이면 처음부터 작은 사진
        assertEquals(1f, startSettle(animate = true, hasPhoto = false), 0f) // 사진 없는 보물은 이름만
    }
}
