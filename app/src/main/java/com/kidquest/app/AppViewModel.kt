// 엔진을 화면 수명 밖에 둔다. 창 크기가 바뀌어 화면이 다시 만들어져도 진행 상태가 남는다.
package com.kidquest.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class AppViewModel(application: Application) : AndroidViewModel(application) {
    val controller = AppController(application).also { it.start() }

    override fun onCleared() {
        controller.stop()
    }
}
