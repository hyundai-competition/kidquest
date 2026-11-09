// 시연 경로 네 개의 ID가 겹치지 않고 역사 테마 하나만 열려 있는지 확인한다.
package com.kidquest.app.screen

import com.kidquest.app.screen.model.AdventureOptions
import org.junit.Assert.*
import org.junit.Test

class AdventureOptionsTest {
    @Test fun routeBoardHasOrderedEndpointsAndThreeStops() {
        AdventureOptions.routes.forEach { route ->
            assertEquals(route.from, route.stops.first())
            assertEquals(route.missions.last().place, route.stops.last())
            assertEquals(5, route.stops.distinct().size)
            assertEquals(4, route.missions.size)
            route.missions.forEach { assertTrue(it.description.isNotBlank()) }
        }
    }

    @Test fun exactlyFourRoutesAndOnlyHistoryUnlocked() {
        assertEquals(listOf("서울 → 경주", "서울 → 부산", "서울 → 안동", "대전 → 서울"), AdventureOptions.routes.map { it.label })
        assertEquals(4, AdventureOptions.routes.map { it.id }.distinct().size)
        assertEquals(4, AdventureOptions.themes.size)
        assertEquals(listOf("history"), AdventureOptions.themes.filter { it.available }.map { it.id })
    }
}
