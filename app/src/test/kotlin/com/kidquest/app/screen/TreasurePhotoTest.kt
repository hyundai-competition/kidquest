// 도착 화면 보물 사진 고르기 시험. 사진이 있을 때만 그 경로를 돌려주고, 확장자는 jpg, png, webp 순으로 고른다.
package com.kidquest.app.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TreasurePhotoTest {
    @Test fun noPhotoMeansNameOnly() {
        assertNull(pickTreasurePhoto(emptySet(), "hahoe_piece"))
        assertNull(pickTreasurePhoto(setOf("palace_piece.jpg"), "hahoe_piece"))
    }

    @Test fun findsThePhotoOfThatTreasure() {
        assertEquals("treasures/hahoe_piece.png", pickTreasurePhoto(setOf("hahoe_piece.png", "palace_piece.jpg"), "hahoe_piece"))
    }

    @Test fun jpgComesFirst() {
        assertEquals("treasures/hahoe_piece.jpg", pickTreasurePhoto(setOf("hahoe_piece.webp", "hahoe_piece.jpg"), "hahoe_piece"))
    }

    @Test fun otherExtensionsAreIgnored() {
        assertNull(pickTreasurePhoto(setOf("hahoe_piece.gif", "hahoe_piece.jpg.txt"), "hahoe_piece"))
    }
}
