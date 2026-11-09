// 화면 미션의 진행 상태. 연령별 목표 수와 정답·조립·순서 규칙을 한 곳에서 관리한다.
package com.kidquest.app.screen.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kidquest.core.contract.AgeGroup

class MissionSession(val mission: CityMission, age: AgeGroup) {
    val preschool = age == AgeGroup.PRESCHOOL
    val targets = if (preschool) mission.young else mission.older
    val starCount = if (preschool) 4 else 7
    var voiceMode by mutableStateOf(false)
    var voiceCorrect by mutableStateOf(false)
        private set
    var lastChoice by mutableStateOf<Int?>(null)
        private set
    val quiz get() = CityVoiceQuizzes.forPlace(mission.place)
    fun answerNumber(index: Int): Boolean {
        if (!voiceMode || done || index !in quiz.options.indices) return false
        lastChoice = index
        if (index != quiz.answer) { feedback = "다시 생각해 볼까요? 장소 설명에 단서가 있어요."; return false }
        voiceCorrect = true
        feedback = "탐험 성공! 보물 조각을 모았어요."
        return true
    }
    var playing by mutableStateOf(false)
    var found by mutableStateOf(setOf<String>())
        private set
    var sequence by mutableStateOf(listOf<String>())
        private set
    var selectedPiece by mutableStateOf<String?>(null)
    var colors by mutableStateOf(mapOf<String, Int>())
        private set
    var paint by mutableStateOf(0)
    var feedback by mutableStateOf("")
        private set
    val done get() = voiceCorrect || when (mission.kind) {
        PuzzleKind.STARS -> sequence.size == starCount
        PuzzleKind.ORDER, PuzzleKind.BRIDGE -> sequence.size == targets.size
        PuzzleKind.COLOR -> colors.size == targets.size
        else -> found.size == targets.size
    }
    val objects get() = (targets + mission.distractors.take(if (preschool) maxOf(0, (if (mission.place == "대릉원") 3 else 4) - targets.size) else 3)).sortedDescending()
    fun choose(value: String): Boolean {
        if (done && mission.kind != PuzzleKind.COLOR) return false
        when (mission.kind) {
            PuzzleKind.ORDER, PuzzleKind.BRIDGE -> {
                if (value != targets.getOrNull(sequence.size)) { feedback = "다음 순서를 다시 살펴볼까요?"; return false }
                sequence = sequence + value
            }
            PuzzleKind.STARS -> {
                if (value != (sequence.size + 1).toString()) return false
                sequence = sequence + value
            }
            PuzzleKind.COLOR -> colors = colors + (value to paint)
            else -> {
                if (value !in targets) { feedback = "옛이야기에 어울리는 물건을 다시 살펴봐요."; return false }
                found = found + value
            }
        }
        feedback = if (done) "탐험 성공! 보물 조각을 모았어요." else MissionNotes.objects[value] ?: "좋아요! 조금 더 완성해 볼까요?"
        return true
    }
    fun place(slot: String): Boolean {
        if (selectedPiece != slot) { feedback = "조각과 같은 모양의 자리를 찾아봐요."; return false }
        selectedPiece = null
        return choose(slot)
    }
}
