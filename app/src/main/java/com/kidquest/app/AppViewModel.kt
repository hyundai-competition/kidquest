// 엔진을 화면 수명 밖에 둔다. 창 크기가 바뀌어 화면이 다시 만들어져도 진행 상태가 남는다.
package com.kidquest.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.kidquest.app.episode.EpisodeFile

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val controller = AppController().also { it.start() }

    /** 임무 진행을 만들 때 엔진에 넘긴다. 지금은 읽히는지만 로그로 본다. */
    val episode = EpisodeFile.load(app, EpisodeFile.DEMO)

    override fun onCleared() {
        controller.stop()
    }
}
