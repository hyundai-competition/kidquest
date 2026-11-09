// 모든 선택 조합이 시작 전에 엔진에 전달되며 기본 이야기와 동의 없음도 명시하는지 확인한다.
package com.kidquest.app.screen

import com.kidquest.app.screen.model.StartSelection
import com.kidquest.core.contract.*
import org.junit.Assert.*
import org.junit.Test

class StartSelectionTest {
    @Test fun allChoicesAreSentBeforeStart() {
        AgeGroup.entries.forEach { age ->
            listOf(null, "demo_destination").forEach { destination ->
                listOf(false, true).forEach { consent ->
                    assertEquals(listOf(
                        AppEvent.AgeSelected(age), AppEvent.DestinationSelected(destination),
                        AppEvent.ListeningConsentChanged(consent), AppEvent.StartPressed,
                    ), StartSelection(age, destination, consent).events())
                }
            }
        }
    }
}
