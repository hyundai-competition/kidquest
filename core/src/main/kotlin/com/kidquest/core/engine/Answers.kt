// 아이 답을 정답과 맞춰 본다. 아이는 「초록색이요」처럼 덧붙여 말해서, 띄어쓰기를 빼고 정답이 들어 있는지 본다.
package com.kidquest.core.engine

import com.kidquest.core.contract.Mission

internal object Answers {
    fun isCorrect(mission: Mission, word: String?): Boolean {
        val heard = word?.withoutSpaces() ?: return false
        return mission.answers.any { it.withoutSpaces() in heard }
    }

    private fun String.withoutSpaces() = filterNot { it.isWhitespace() }
}
