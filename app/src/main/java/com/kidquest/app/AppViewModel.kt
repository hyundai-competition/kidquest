// 시작 설정과 엔진 연결을 보관한다. 차량 수집기는 화면이 다시 만들어져도 한 번만 실행한다.
package com.kidquest.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.kidquest.app.episode.EpisodeFile
import com.kidquest.app.car.PermissionCarSource
import com.kidquest.app.screen.model.TripCatalog
import com.kidquest.app.screen.model.AdventureOptions
import com.kidquest.app.screen.model.StartSelection
import com.kidquest.app.screen.model.CityMission
import com.kidquest.app.screen.model.MissionSession
import com.kidquest.app.voice.NumberAnswer
import com.kidquest.app.storage.LocalStore
import com.kidquest.core.contract.AgeGroup
import com.kidquest.core.contract.AppEvent
import com.kidquest.core.contract.Gear
import com.kidquest.core.contract.Language
import com.kidquest.core.engine.MissionEngine
import com.kidquest.core.episode.EpisodeResult

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    val episode = (EpisodeFile.load(app, EpisodeFile.DEMO) as? EpisodeResult.Ok)?.episode
    private val english = EpisodeFile.loadOptional(app, EpisodeFile.DEMO_EN)

    /** 고를 수 있는 언어. 영어 예시 파일(#60)이 있을 때만 영어가 들어간다. 화면은 #61. */
    val languages = listOfNotNull(Language.KO, Language.EN.takeIf { english != null })
    var language by mutableStateOf(Language.KO)
        private set
    fun selectLanguage(value: Language) { if (value in languages) language = value }
    private val activeEpisode get() = if (language == Language.EN) english ?: episode else episode
    val tripCatalog get() = TripCatalog(activeEpisode)
    val routes = AdventureOptions.fromEpisode(episode).ifEmpty { AdventureOptions.routes }
    var routeId by mutableStateOf<String?>(tripCatalog.routes(routes.first().id).firstOrNull()?.id)
        private set
    val travelRoutes get() = tripCatalog.routes(destinationId)
    val tripStops get() = tripCatalog.stops(destinationId, routeId, age)
    val plannedMissions get() = tripCatalog.plan(destinationId, routeId, age)
    val credits get() = activeEpisode?.credits.orEmpty()
    val activeTravelRoute get() = travelRoutes.firstOrNull { it.id == routeId } ?: travelRoutes.firstOrNull()
    val themes = AdventureOptions.themes
    var weatherStyle by mutableStateOf(com.kidquest.app.screen.WeatherStyle.RAIN)
        private set
    fun selectWeatherStyle(value: com.kidquest.app.screen.WeatherStyle) { if (canPlayMiniGames) weatherStyle = value }
    var setupStep by mutableStateOf(0)
        private set
    var themeId by mutableStateOf("history")
        private set
    val selectedRoute get() = routes.first { it.id == destinationId }
    fun changeStep(step: Int) { setupStep = step.coerceIn(0, 2) }
    fun selectTheme(id: String) {
        if (themes.any { it.id == id && it.available }) themeId = id
    }
    var age by mutableStateOf(AgeGroup.PRESCHOOL)
        private set
    var consent by mutableStateOf(false)
        private set
    var destinationId by mutableStateOf(routes.first().id)
        private set
    private val car = PermissionCarSource(app, episode?.places.orEmpty())
    private val translations = listOfNotNull(english?.let { Language.EN to it }).toMap()
    val controller = AppController(app, engine = MissionEngine(episode, translations), car = car, episode = episode)
    init { controller.vehicleChanged = { vehicle ->
        if (vehicle.gear != Gear.P || vehicle.restricted) closeMiniGames()
    } }
    var connectionsReady by mutableStateOf(false)
        private set
    fun startConnections(locationAllowed: Boolean) {
        if (connectionsReady) return
        car.locationAllowed = locationAllowed
        controller.start()
        connectionsReady = true
    }
    fun selectTravelRoute(id: String) { if (travelRoutes.any { it.id == id }) routeId = id }
    var placeNotice by mutableStateOf("")
        private set
    fun visitPlace(id: String) {
        val stop = tripStops.firstOrNull { it.placeId == id } ?: return
        if (controller.vehicle.restricted) return
        controller.replay.send(AppEvent.PlaceEntered(id))
        placeNotice = if (language == Language.EN) "Demo location selected: ${com.kidquest.app.screen.englishUi(stop.label)}. It follows the current dialogue and earlier missions. Resume audio if muted." else "시연 위치를 ${stop.label}(으)로 선택했어요. 현재 대사와 앞 임무가 끝나면 이어져요. 소리가 꺼져 있으면 다시 켜 주세요."
    }
    var miniGamesVisible by mutableStateOf(false)
        private set
    val canPlayMiniGames get() = controller.vehicle.gear == Gear.P && !controller.vehicle.restricted
    fun openMiniGames() { if (canPlayMiniGames) miniGamesVisible = true }
    fun closeMiniGames() {
        if (demoMission != null || journeyEndingVisible) closeDemoMission()
        miniGamesVisible = false
    }

    fun selectAge(value: AgeGroup) {
        age = value
        store.ageGroup = value.name
    }
    fun setListeningConsent(value: Boolean) {
        consent = value
        controller.dispatch(AppEvent.ListeningConsentChanged(value))
    }
    fun selectDestination(id: String?) {
        destinationId = routes.firstOrNull { it.id == id }?.id ?: routes.first().id
        routeId = tripCatalog.routes(destinationId).firstOrNull()?.id
    }
    fun startTrip() {
        StartSelection(age, destinationId, consent, routeId, language).events().forEach(controller::dispatch)
    }
    var demoMission by mutableStateOf<CityMission?>(null)
        private set
    var missionSession by mutableStateOf<MissionSession?>(null)
        private set
    val narrationStatus get() = controller.voiceStatus
    var completedDemoMissions by mutableStateOf(setOf<String>())
        private set
    var skippedDemoMissions by mutableStateOf(setOf<String>())
        private set
    val journeyReadyToEnd get() = selectedRoute.missions.all { it.id in completedDemoMissions || it.id in skippedDemoMissions }
    var missionAudioMuted by mutableStateOf(false)
        private set
    var reviewingCompletedMission by mutableStateOf(false)
        private set
    var journeyEndingVisible by mutableStateOf(false)
        private set
    private var soundWasStopped = false
    private var voiceRetryCount = 0
    fun openDemoMission(mission: CityMission) {
        if (!canPlayMiniGames || mission !in selectedRoute.missions) return
        miniGamesVisible = true
        soundWasStopped = controller.parentStopRequested
        controller.dispatch(AppEvent.ParentStopChanged(true))
        reviewingCompletedMission = false
        missionAudioMuted = soundWasStopped
        voiceRetryCount = 0
        missionSession = MissionSession(mission, age)
        demoMission = mission
        readMission()
    }
    fun readMission() {
        if (canPlayMiniGames && !missionAudioMuted) demoMission?.let {
            controller.retryAudioFocus()
            controller.speakScreen("${it.place}. ${it.description}")
        }
    }
    fun beginMission() {
        if (!canPlayMiniGames || missionSession == null) return
        controller.stopScreenAudio()
        missionSession?.playing = true
    }
    fun missionAction(value: String, placement: Boolean = false) {
        if (!canPlayMiniGames) return
        val session = missionSession ?: return
        val wasDone = session.done
        val accepted = if (placement) session.place(value) else session.choose(value)
        if (!accepted) return
        if (session.done && !wasDone) {
            completeMission(session)
        } else if (value == "편종" || value == "편경") {
            if (!missionAudioMuted) controller.playScreenInstrument(value)
        } else if (!missionAudioMuted && !session.done && session.mission.kind == com.kidquest.app.screen.model.PuzzleKind.FIND) {
            controller.speakScreen(session.feedback)
        }
    }

    private fun completeMission(session: MissionSession) {
        skippedDemoMissions = skippedDemoMissions - session.mission.id
        completedDemoMissions = completedDemoMissions + session.mission.id
        if (!missionAudioMuted) controller.speakScreen("${session.mission.place} 탐험 성공! 보물 조각을 찾았어요. ${completedDemoMissions.size}개의 조각이 모였어요.")
    }
    fun skipCurrentMission() {
        val session = missionSession ?: return
        if (session.done) return
        skippedDemoMissions = skippedDemoMissions + session.mission.id
        closeDemoMission()
    }
    fun beginVoiceQuiz() {
        val session = missionSession ?: return
        if (session.done) return
        session.voiceMode = true
        session.playing = true
        voiceRetryCount = 0
        readVoiceQuiz()
    }
    fun readVoiceQuiz(withDescription: Boolean = false) {
        val session = missionSession ?: return
        if (!canPlayMiniGames || session.done || missionAudioMuted) return
        val prefix = if (withDescription) session.mission.description.substringBefore("\n") + " " else ""
        val question = if (consent) session.quiz.spoken else session.quiz.question + " " +
            session.quiz.options.mapIndexed { i, text -> "${i + 1}번, $text." }.joinToString(" ") +
            " 지금은 듣기만 해도 좋아요. 정차한 뒤 화면으로 답하거나 건너뛸 수 있어요."
        speakThenListen(prefix + question, session)
    }
    fun repeatVoiceQuiz() { voiceRetryCount = 0; if (!missionAudioMuted) controller.retryAudioFocus(); readVoiceQuiz() }
    fun resumeAnswering() { voiceRetryCount = 0; if (!missionAudioMuted) controller.retryAudioFocus(); listenAgain() }
    fun listenAgain() {
        val session = missionSession ?: return
        if (!canPlayMiniGames || !consent || missionAudioMuted || session.done || !session.voiceMode) return
        controller.listenScreen { answer -> handleVoiceAnswer(session, answer) }
    }
    private fun handleVoiceAnswer(session: MissionSession, text: String?) {
        if (missionSession !== session || !consent || missionAudioMuted || !session.voiceMode || session.done || text == null) return
        if (NumberAnswer.isSkip(text)) { skipCurrentMission(); return }
        val index = NumberAnswer.parse(text)
        if (index == null) {
            retryVoiceQuestion("번호를 잘 듣지 못했어요. 1번, 2번, 3번 중 하나로 다시 말해 주세요.", session)
        } else answerVoiceChoice(index)
    }
    fun answerVoiceChoice(index: Int) {
        val session = missionSession ?: return
        if (!canPlayMiniGames || !session.voiceMode || session.done) return
        if (session.answerNumber(index)) completeMission(session)
        else retryVoiceQuestion(session.feedback, session)
    }
    private fun retryVoiceQuestion(text: String, session: MissionSession) {
        voiceRetryCount++
        if (voiceRetryCount >= 2) controller.speakScreen("지금은 듣기만 해도 괜찮아요. 정차한 뒤 다시 대답하거나 다음 장소로 넘어가도 좋아요.")
        else speakThenListen(text, session)
    }
    private fun speakThenListen(text: String, session: MissionSession) {
        val after: (() -> Unit)? = if (consent) ({
            if (missionSession === session && consent && !missionAudioMuted && !session.done) listenAgain()
        }) else null
        controller.speakScreen(text, after)
    }
    fun toggleMissionAudio() {
        missionAudioMuted = !missionAudioMuted
        soundWasStopped = missionAudioMuted
        if (!missionAudioMuted) controller.retryAudioFocus()
        if (missionAudioMuted) controller.stopScreenAudio()
        else if (journeyEndingVisible) readJourneyEnding()
        else if (missionSession?.voiceMode == true) readVoiceQuiz()
        else readMission()
    }

    fun reviewCompletedMission() { reviewingCompletedMission = true }
    fun showJourneyEnding() {
        if (!canPlayMiniGames || !journeyReadyToEnd) return
        if (demoMission == null) {
            soundWasStopped = controller.parentStopRequested
            controller.dispatch(AppEvent.ParentStopChanged(true))
        }
        controller.stopScreenAudio()
        demoMission = null
        missionSession = null
        journeyEndingVisible = true
        readJourneyEnding()
    }
    fun readJourneyEnding() {
        if (journeyEndingVisible && !missionAudioMuted) controller.speakScreen(
            "브루미와 함께한 ${selectedRoute.to} 탐험이 끝났어요. ${completedDemoMissions.size}개의 보물 조각을 모았네요! 하지 못한 퍼즐은 다음에 만나도 좋아요. 남은 여행도 즐겁고 안전하게 이어가요. 다음 모험에서 또 만나요!"
        )
    }
    fun returnToCompletedMap() {
        controller.stopScreenAudio()
        journeyEndingVisible = false
        if (!soundWasStopped) controller.dispatch(AppEvent.ParentStopChanged(false))
    }
    fun stopNarration() { if (demoMission != null || journeyEndingVisible) controller.stopScreenAudio() }
    fun closeDemoMission() {
        controller.stopScreenAudio()
        demoMission = null
        missionSession = null
        journeyEndingVisible = false
        if (!soundWasStopped) controller.dispatch(AppEvent.ParentStopChanged(false))
    }
    fun restartTrip() {
        journeyEndingVisible = false
        miniGamesVisible = false
        placeNotice = ""
        soundWasStopped = false
        missionAudioMuted = false
        reviewingCompletedMission = false
        controller.stopScreenAudio()
        demoMission = null
        missionSession = null
        completedDemoMissions = emptySet()
        skippedDemoMissions = emptySet()

        setupStep = 0
        consent = false // 새 탐험도 동의를 다시 확인한다. 동의는 저장하지 않는다.
        controller.dispatch(AppEvent.RestartPressed)
        controller.dispatch(AppEvent.ListeningConsentChanged(false))
    }
    override fun onCleared() { controller.stopScreenAudio(); controller.stop() }
}
