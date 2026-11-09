// 앱의 시작점. 화면을 띄운다. 연결과 진행 상태는 AppViewModel이 가지고 있다.
package com.kidquest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import com.kidquest.core.contract.Screen
import androidx.activity.viewModels
import com.kidquest.app.screen.KidQuestScreen

class MainActivity : ComponentActivity() {
    private val model: AppViewModel by viewModels()

    override fun onStop() {
        model.stopNarration()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BackHandler(enabled = model.controller.screen == Screen.START && model.setupStep > 0) {
                model.changeStep(model.setupStep - 1)
            }
            KidQuestScreen(model)
        }
    }
}
