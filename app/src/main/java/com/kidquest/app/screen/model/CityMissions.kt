// 도시별 체험 미션과 연령별 조작 구성. 교육 설명은 별도 콘텐츠 파일에서 가져온다.
package com.kidquest.app.screen.model

enum class PuzzleKind { FIND, STARS, BUILD, MATCH, ORDER, COLOR, BRIDGE }
data class CityMission(
    val id: String, val place: String, val task: String, val kind: PuzzleKind,
    val young: List<String>, val older: List<String>, val distractors: List<String> = emptyList(),
) {
    val description get() = MissionEducation.descriptions.getValue(place)
}

object CityMissions {
    private fun m(place: String, task: String, kind: PuzzleKind, young: String, older: String, wrong: String = "") =
        CityMission(place, place, task, kind, young.split("|"), older.split("|"), wrong.split("|").filter { it.isNotEmpty() })
    fun forCity(city: String): List<CityMission> = when (city) {
        "경주" -> listOf(
            m("대릉원", "왕의 황금 보물을 찾아라!", PuzzleKind.FIND, "금관", "금관|금귀걸이|토기", "스마트폰|운동화|자동차"),
            m("첨성대", "별의 암호를 풀어라!", PuzzleKind.STARS, "별 4개", "별 7개"),
            m("동궁과 월지", "사라진 궁궐을 복원하라!", PuzzleKind.BUILD, "연못|전통 건물", "연못|전통 건물|작은 섬|나무", "현대식 빌딩"),
            m("불국사", "두 석탑의 비밀", PuzzleKind.MATCH, "화려한 장식 → 다보탑", "단정한 3층 구조 → 석가탑|화려한 장식 → 다보탑"),
        )
        "안동" -> listOf(
            m("하회마을", "사라진 하회탈을 찾아라!", PuzzleKind.MATCH, "양반 역할 → 양반탈", "양반 역할 → 양반탈|각시 역할 → 각시탈|부네 역할 → 부네탈|승려 역할 → 중탈"),
            m("병산서원", "선비의 공부방", PuzzleKind.FIND, "붓|종이", "붓|먹|종이|벼루", "노트북|헤드폰"),
            m("봉정사", "오래된 나무 건축물의 비밀", PuzzleKind.BUILD, "기둥|지붕", "기둥|공포|지붕|문"),
            m("월영교", "달빛 다리의 비밀", PuzzleKind.ORDER, "다리 조각 1|다리 조각 2", "다리 조각 1|다리 조각 2|다리 조각 3|다리 조각 4"),
        )
        "부산" -> listOf(
            m("감천문화마을", "마을의 색깔을 되찾아라!", PuzzleKind.COLOR, "집 1|집 2|집 3", "아래 집|가운데 집|언덕 위 집"),
            m("BIFF 광장", "영화 제작자가 되어라!", PuzzleKind.ORDER, "촬영|상영", "대본 작성|촬영|편집|상영"),
            m("영도대교", "움직이는 다리의 비밀", PuzzleKind.BRIDGE, "다리 올리기", "다리 올리기|배 통과|다리 내리기"),
            m("국립해양박물관", "마지막 항해", PuzzleKind.FIND, "나침반|망원경", "나침반|해도|망원경", "축구공|운동화|게임기"),
        )
        else -> listOf(
            m("경복궁", "왕의 궁궐을 찾아라!", PuzzleKind.MATCH, "공식 의식 → 근정전", "공식 의식 → 근정전|연회 → 경회루|정문 → 광화문", "일반 주택|현대식 빌딩"),
            m("국립고궁박물관", "왕실의 보물 찾기", PuzzleKind.FIND, "왕실 의복", "어보|왕실 의복|궁중 장식품", "스마트폰|티셔츠|운동화"),
            m("창덕궁", "왕의 정원을 복원하라!", PuzzleKind.BUILD, "연못|나무", "연못|정자|나무|돌길", "현대식 빌딩"),
            m("종묘", "왕실의 음악을 완성하라!", PuzzleKind.FIND, "편종", "편종|편경", "기타|전자 키보드"),
        )
    }
}
