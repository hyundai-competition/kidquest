// assets/episodes/의 파일을 열어 core의 읽기 함수에 넘긴다. 틀린 파일이어도 앱은 죽지 않고 로그로 알린다.
package com.kidquest.app.episode

import android.content.Context
import android.util.Log
import com.kidquest.core.contract.Episode
import com.kidquest.core.episode.EpisodeResult
import com.kidquest.core.episode.readEpisode
import java.io.IOException

object EpisodeFile {
    /** 로그 보기 : adb logcat -s KQEpisode */
    private const val TAG = "KQEpisode"
    const val DEMO = "episode_demo.json"

    /** 영어판. 임무·장소·보물 id가 DEMO와 같고 글만 영어다(#60). */
    const val DEMO_EN = "episode_demo_en.json"

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

    /** 없어도 되는 파일(영어판)을 연다. 파일이 없으면 로그 없이 null, 있는데 틀리면 load처럼 로그를 남기고 null. */
    fun loadOptional(context: Context, fileName: String): Episode? {
        val exists = try {
            context.assets.list("episodes").orEmpty().contains(fileName)
        } catch (e: IOException) {
            false
        }
        return if (exists) (load(context, fileName) as? EpisodeResult.Ok)?.episode else null
    }
}
