// 첫 시험 전용 앱의 화면. 위에 버튼, 아래에 값 기록이 나온다. 같은 기록을 logcat의 KQSignal 태그로도 볼 수 있다.
package com.kidquest.signalcheck

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var logView: TextView
    private lateinit var log: SignalLog

    // SDK 하나가 만들어지지 않아도 나머지 값은 볼 수 있게, 각각 따로 시도한다.
    private var vehicle: VehicleProbe? = null
    private var carState: CarStateProbe? = null
    private var voice: VoiceProbe? = null
    private var navi: NaviProbe? = null
    private var location: LocationProbe? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logView = TextView(this).apply {
            textSize = LOG_TEXT_SP
            setTextIsSelectable(true)
        }
        log = SignalLog { line -> logView.text = "$line\n${logView.text}" }
        setContentView(buildScreen())

        log.attempt("Vehicle 준비") { vehicle = VehicleProbe(this, log).also { it.start() } }
        log.attempt("Car 준비") { carState = CarStateProbe(this, log).also { it.start() } }
        log.attempt("음성 준비") { voice = VoiceProbe(this, log).also { it.start() } }
        log.attempt("내비 준비") { navi = NaviProbe(this, log).also { it.start() } }
        requestPermissions(RUNTIME_PERMISSIONS, REQUEST_PERMISSIONS)
    }

    /** 위치는 허용 창의 답을 받은 뒤에 시작한다. */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_PERMISSIONS || location != null) return
        log.attempt("위치 준비") { location = LocationProbe(this, log).also { it.start() } }
    }

    override fun onDestroy() {
        location?.stop()
        navi?.stop()
        voice?.stop()
        carState?.stop()
        vehicle?.stop()
        super.onDestroy()
    }

    private fun buildScreen(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(PADDING, PADDING, PADDING, PADDING)
        addView(
            buttonRow(
                button("TTS 말하기") { voice?.speak() },
                button("TTS 멈춤") { voice?.stopSpeaking() },
                button("말 듣기") { voice?.listen() },
            ),
        )
        addView(
            buttonRow(
                button("포커스 요청") { voice?.requestFocus() },
                button("포커스 반납") { voice?.abandonFocus() },
                button("소리 구역 바꾸기") { voice?.nextZone() },
            ),
        )
        addView(buttonRow(button("내비 값 읽기") { navi?.readNow() }))
        addView(
            ScrollView(context).apply { addView(logView) },
            LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f),
        )
    }

    private fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun buttonRow(vararg buttons: Button): LinearLayout = LinearLayout(this).apply {
        buttons.forEach { addView(it, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)) }
    }

    private companion object {
        const val REQUEST_PERMISSIONS = 1
        const val LOG_TEXT_SP = 18f
        const val PADDING = 24
        val RUNTIME_PERMISSIONS = arrayOf(
            "pleos.car.permission.FUSED_LOCATION",
            "android.car.permission.CAR_POWERTRAIN",
        )
    }
}
