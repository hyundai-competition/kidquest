// 시험 결과를 화면과 logcat에 같이 남긴다. 값이 안 오거나 오류가 나도 앱이 죽지 않게 attempt로 감싼다.
package com.kidquest.signalcheck

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SignalLog(private val show: (String) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun line(name: String, value: String) {
        val text = "${clock.format(Date())} [$name] $value"
        Log.i(TAG, text)
        main.post { show(text) }
    }

    /** SDK 호출 하나를 시도한다. 실패하면 오류 문구를 그대로 남긴다. 결과표에 그대로 옮겨 적는다. */
    fun attempt(name: String, action: () -> Unit) {
        try {
            action()
            line(name, "호출 성공")
        } catch (e: Throwable) {
            line(name, "실패 ${e.javaClass.simpleName} : ${e.message}")
        }
    }

    private companion object {
        const val TAG = "KQSignal"
    }
}
