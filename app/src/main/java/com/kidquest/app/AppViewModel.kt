// 엔진을 화면 수명 밖에 둔다. 창 크기가 바뀌어 화면이 다시 만들어져도 진행 상태가 남는다.
package com.kidquest.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.kidquest.app.episode.EpisodeFile
import com.kidquest.core.engine.MissionEngine
import com.kidquest.core.episode.EpisodeResult

class AppViewModel(app: Application) : AndroidViewModel(app) {
    /** 파일이 틀리면 에피소드 없이 상태 흐름만 돈다. 틀린 곳은 KQEpisode 로그에 남는다. */
    private val episode = (EpisodeFile.load(app, EpisodeFile.DEMO) as? EpisodeResult.Ok)?.episode

    val controller = AppController(engine = MissionEngine(episode)).also { it.start() }

    override fun onCleared() {
        controller.stop()
    }
}
