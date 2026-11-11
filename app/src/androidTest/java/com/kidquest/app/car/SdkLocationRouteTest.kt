// ADB 좌표를 실제 FusedLocation 근처 알림으로 받는 선택 시험. PlaceEntered를 직접 주입하지 않는다.
package com.kidquest.app.car

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.kidquest.app.episode.EpisodeFile
import com.kidquest.core.contract.*
import com.kidquest.core.episode.EpisodeResult
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class SdkLocationRouteTest {
    @Test fun actualSdkReceivesThreePlacesOnGyeongjuRoute() {
        val test = InstrumentationRegistry.getInstrumentation()
        assumeTrue("ADB 위치 재생과 함께 -e verifySdkRoute true로 실행", InstrumentationRegistry.getArguments().getString("verifySdkRoute") == "true")
        val episode = (EpisodeFile.load(test.targetContext, EpisodeFile.DEMO) as EpisodeResult.Ok).episode
        val required = setOf("cheonan", "daegu", "cheomseongdae")
        val seen = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        val events = CopyOnWriteArrayList<String>()
        val done = CountDownLatch(1)
        lateinit var collector: CarSignalCollector
        test.runOnMainSync {
            collector = CarSignalCollector(test.targetContext, episode.places)
            collector.start(EventListener { event ->
                events += event.toString()
                Log.i("KQSdkRoute", event.toString())
                if (event is AppEvent.PlaceEntered) {
                    seen += event.placeId
                    if (seen.containsAll(required)) done.countDown()
                }
            })
        }
        Log.i("KQSdkRoute", "ready: feed cheonan, daegu, cheomseongdae through emulator GPS")
        try {
            val completed = done.await(75, TimeUnit.SECONDS)
            assertTrue("실제 SDK 장소 진입 누락: $seen", completed)
            assertFalse(events.contains(AppEvent.SignalUnavailable(CarSignal.LOCATION).toString()))
        } finally {
            test.runOnMainSync { collector.stop() }
            File(test.targetContext.cacheDir, "sdk-location-route.txt").writeText(events.joinToString("\n"))
        }
    }
}
