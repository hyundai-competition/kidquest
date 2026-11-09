// 회의에서 정한 시연 경로 네 개와 역사 테마. 다른 테마는 이름 미정의 잠금 항목이다.
package com.kidquest.app.screen.model

data class DemoRoute(val id: String, val from: String, val to: String, val title: String, val subtitle: String) {
    val missions get() = CityMissions.forCity(to)
    val stops get() = listOf(from) + missions.map { it.place }
    val label get() = "$from → $to"
}

data class AdventureTheme(val id: String, val name: String, val available: Boolean)

object AdventureOptions {
    val routes = listOf(
        DemoRoute("seoul_gyeongju", "서울", "경주", "신라의 잃어버린\n보물을 찾아라!", "대릉원의 유물과 첨성대의 별을 따라"),
        DemoRoute("seoul_busan", "서울", "부산", "바다 도시\n탐험대", "바다를 향해 이어지는 우리 가족의 탐험"),
        DemoRoute("seoul_andong", "서울", "안동", "조선 시대\n시간여행", "옛 이야기를 따라 숨은 보물을 찾아요"),
        DemoRoute("daejeon_seoul", "대전", "서울", "조선의\n비밀 임무", "도시에 숨은 옛 이야기 속으로"),
    )
    val themes = listOf(
        AdventureTheme("history", "역사 보물찾기", true),
        AdventureTheme("coming_1", "준비 중 01", false),
        AdventureTheme("coming_2", "준비 중 02", false),
        AdventureTheme("coming_3", "준비 중 03", false),
    )
}
