// 사용자가 제공한 교육 원고에서 만든 세 보기 음성 퀴즈. 터치 퍼즐 대신 같은 장소를 소리로 탐험한다.
package com.kidquest.app.screen.model

data class CityVoiceQuiz(val question: String, val options: List<String>, val answer: Int) {
    val spoken get() = question + " " + options.mapIndexed { i, text -> "${i + 1}번, $text." }.joinToString(" ") + " 번호로 대답해 주세요. 건너뛰기라고 말해도 좋아요."
}
object CityVoiceQuizzes {
    fun forPlace(place: String): CityVoiceQuiz {
        val (question, correct, wrong) = when (place) {
            "대릉원" -> Triple("신라 왕이 머리에 썼을 보물은 무엇일까요?", "금관", listOf("스마트폰", "운동화"))
            "첨성대" -> Triple("옛날 사람들은 하늘에서 무엇을 관찰했을까요?", "별과 달", listOf("자동차", "텔레비전"))
            "동궁과 월지" -> Triple("신라 사람들이 만든 연못의 이름은 무엇일까요?", "월지", listOf("광화문", "영도대교"))
            "불국사" -> Triple("장식이 많고 복잡한 구조의 탑은 무엇일까요?", "다보탑", listOf("석가탑", "첨성대"))
            "하회마을" -> Triple("양반 역할을 표현한 탈은 무엇일까요?", "양반탈", listOf("각시탈", "부네탈"))
            "병산서원" -> Triple("선비가 글씨를 쓸 때 사용한 도구는 무엇일까요?", "붓", listOf("리모컨", "헤드폰"))
            "봉정사" -> Triple("전통 목조 건축물의 주요 재료는 무엇일까요?", "나무", listOf("얼음", "플라스틱"))
            "월영교" -> Triple("월영교가 있는 도시는 어디일까요?", "안동", listOf("부산", "경주"))
            "감천문화마을" -> Triple("감천문화마을의 집들은 어떤 지형을 따라 모여 있을까요?", "산비탈", listOf("바다 한가운데", "사막"))
            "BIFF 광장" -> Triple("촬영한 장면을 하나의 영화로 만드는 과정은 무엇일까요?", "편집", listOf("항해", "제례"))
            "영도대교" -> Triple("배가 지나갈 공간을 만들려면 다리를 어떻게 할까요?", "위로 들어 올려요", listOf("얼려요", "색칠해요"))
            "국립해양박물관" -> Triple("바다에서 방향을 알려주는 도구는 무엇일까요?", "나침반", listOf("축구공", "붓"))
            "경복궁" -> Triple("왕이 공식 의식을 치르던 건물은 어디일까요?", "근정전", listOf("경회루", "광화문"))
            "국립고궁박물관" -> Triple("조선 왕실의 의례와 관련된 도장은 무엇일까요?", "어보", listOf("스마트폰", "카메라"))
            "창덕궁" -> Triple("창덕궁 후원에서 함께 어우러지는 것은 무엇일까요?", "연못과 정자와 나무", listOf("현대식 빌딩", "공항 활주로"))
            else -> Triple("편경은 어떤 재료로 만든 악기일까요?", "돌", listOf("금속", "종이"))
        }
        val answer = (place.hashCode() and Int.MAX_VALUE) % 3
        val options = wrong.toMutableList().apply { add(answer, correct) }
        return CityVoiceQuiz(question, options, answer)
    }
}
