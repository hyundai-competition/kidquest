// assets/episodes/의 파일을 열어 core의 읽기 함수에 넘긴다. 틀린 파일이어도 앱은 죽지 않고 로그로 알린다.
package com.kidquest.app.episode

import android.content.Context
import android.util.Log
import com.kidquest.core.episode.EpisodeResult
import com.kidquest.core.episode.readEpisode
import java.io.IOException

object EpisodeFile {
    /** 로그 보기 : adb logcat -s KQEpisode */
    private const val TAG = "KQEpisode"
    const val DEMO = "episode_demo.json"

    fun load(context: Context, fileName: String): EpisodeResult {
        val result = try {
            readEpisode(context.assets.open("episodes/$fileName").bufferedReader().use { it.readText() })
        } catch (e: IOException) {
            EpisodeResult.Failed(listOf("파일을 열 수 없다 : $fileName"))
        }
        when (result) {
            is EpisodeResult.Ok -> Log.i(TAG, "$fileName 읽음 : 임무 ${result.episode.missions.size}개")
            is EpisodeResult.Failed -> result.problems.forEach { Log.e(TAG, "$fileName : $it") }
        }
        return result
    }
}
