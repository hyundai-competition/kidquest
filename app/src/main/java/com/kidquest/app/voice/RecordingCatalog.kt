// 대사 ID와 문장이 모두 일치하는 녹음만 대체 재생한다. 오래된 대사를 잘못 틀지 않는다.
package com.kidquest.app.voice

import android.content.Context
import com.kidquest.core.contract.Command
import org.json.JSONObject

class RecordingCatalog(context: Context) {
    private val entries = context.assets.open("audio/recordings/catalog.json").bufferedReader().use {
        JSONObject(it.readText()).getJSONArray("recordings")
    }

    fun pathFor(line: Command.Say): String? {
        for (index in 0 until entries.length()) {
            val entry = entries.getJSONObject(index)
            val file = entry.optString("file")
            if (entry.optString("lineId") == line.lineId && entry.optString("text") == line.text &&
                file.matches(Regex("[a-z0-9_-]+\\.(wav|ogg|mp3)"))) {
                return "audio/recordings/$file"
            }
        }
        return null
    }
}
