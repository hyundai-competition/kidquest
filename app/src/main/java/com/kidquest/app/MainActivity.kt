// 앱의 시작점. 연결을 시작하고 화면을 띄운다.
package com.kidquest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kidquest.app.screen.KidQuestScreen

class MainActivity : ComponentActivity() {
    private val controller = AppController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller.start()
        setContent { KidQuestScreen(controller) }
    }

    override fun onDestroy() {
        controller.stop()
        super.onDestroy()
    }
}
