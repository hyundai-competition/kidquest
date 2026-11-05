// 앱의 시작점. 화면을 띄운다. 연결과 진행 상태는 AppViewModel이 가지고 있다.
package com.kidquest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.kidquest.app.screen.KidQuestScreen

class MainActivity : ComponentActivity() {
    private val model: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KidQuestScreen(model.controller) }
    }
}
