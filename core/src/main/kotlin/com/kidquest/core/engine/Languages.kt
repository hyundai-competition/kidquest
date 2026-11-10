// 고른 언어의 에피소드와 고정 대사를 정한다. 한국어는 기본 에피소드를 쓰고, 다른 언어는 그 언어 파일이 있을 때만 바꾼다.
// 언어별 파일은 임무·장소·보물·목적지·경로 id와 순서가 같고 글만 다르다고 본다. 그래서 언어를 바꿔도
// 고른 목적지와 경로, 차 신호 기억(와이퍼, 위치를 못 읽는 차, 지나온 장소)은 그대로 둔다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Episode
import com.kidquest.core.contract.Language

internal class Languages(private val korean: Episode?, private val translations: Map<Language, Episode>) {
    var language: Language = Language.KO
        private set

    val episode: Episode? get() = if (language == Language.KO) korean else translations[language]

    val lines: LineSet get() = if (language == Language.EN) EnglishLines else Lines

    /** 언어가 바뀌었으면 true. 파일이 없는 언어를 고르면 한국어로 둔다. 대사와 글이 섞이지 않게 하려는 것이다. */
    fun select(next: Language): Boolean {
        val chosen = if (next == Language.KO || next in translations) next else Language.KO
        if (chosen == language) return false
        language = chosen
        return true
    }
}
