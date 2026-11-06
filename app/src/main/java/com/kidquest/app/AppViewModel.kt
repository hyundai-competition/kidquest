// 시작 설정과 엔진 연결을 보관한다. 차량 수집기는 화면이 다시 만들어져도 한 번만 실행한다.
package com.kidquest.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.kidquest.app.episode.EpisodeFile
import com.kidquest.app.storage.LocalStore
import com.kidquest.core.contract.AgeGroup
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.engine.MissionEngine
import com.kidquest.core.episode.EpisodeResult

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val episode = (EpisodeFile.load(app, EpisodeFile.DEMO) as? EpisodeResult.Ok)?.episode
    val destinations = episode?.destinations.orEmpty()
    var age by mutableStateOf(AgeGroup.entries.firstOrNull { it.name == store.ageGroup } ?: AgeGroup.PRESCHOOL)
        private set
    var consent by mutableStateOf(false)
        private set
    var destinationId by mutableStateOf<String?>(null)
        private set
    val controller = AppController(app, engine = MissionEngine(episode)).also { it.start() }

    fun selectAge(value: AgeGroup) {
        age = value
        store.ageGroup = value.name
    }
    fun setListeningConsent(value: Boolean) { consent = value }
    fun selectDestination(id: String?) {
        destinationId = id?.takeIf { candidate -> destinations.any { it.id == candidate } }
    }
    fun startTrip() {
        controller.dispatch(AppEvent.AgeSelected(age))
        controller.dispatch(AppEvent.DestinationSelected(destinationId))
        controller.dispatch(AppEvent.ListeningConsentChanged(consent))
        controller.dispatch(AppEvent.StartPressed)
    }
    fun restartTrip() {
        consent = false // 새 항해도 동의를 다시 확인한다. 동의는 저장하지 않는다.
        controller.dispatch(AppEvent.RestartPressed)
        controller.dispatch(AppEvent.ListeningConsentChanged(false))
    }
    override fun onCleared() { controller.stop() }
}
