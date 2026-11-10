// 모든 선택 조합이 시작 전에 엔진에 전달되며 기본 이야기와 동의 없음도 명시하는지 확인한다.
// 언어는 고르지 않으면 한국어를 보낸다.
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
                        AppEvent.LanguageSelected(Language.KO),
                        AppEvent.AgeSelected(age), AppEvent.DestinationSelected(destination),
                        AppEvent.ListeningConsentChanged(consent), AppEvent.StartPressed,
                    ), StartSelection(age, destination, consent).events())
                }
            }
        }
    }

    @Test fun chosenLanguageIsSentFirst() {
        assertEquals(AppEvent.LanguageSelected(Language.EN),
            StartSelection(AgeGroup.PRESCHOOL, null, false, language = Language.EN).events().first())
    }
}
