// 실제 Activity의 종료를 확인한다. Pleos 런처에 빈 테스트 앱 창을 띄우지 않는다.
package com.kidquest.app

import android.app.Activity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun pleosComposeRule(): AndroidComposeTestRule<PleosActivityRule, MainActivity> {
    val activity = PleosActivityRule()
    return AndroidComposeTestRule(activity) { it.activity }
}

class PleosActivityRule : TestRule {
    lateinit var scenario: ActivityScenario<MainActivity>
        private set
    val activity: MainActivity get() {
        var current: MainActivity? = null
        scenario.onActivity { current = it }
        return checkNotNull(current)
    }
    private val test = InstrumentationRegistry.getInstrumentation()
    private val monitor = ActivityLifecycleMonitorRegistry.getInstance()
    private val pending = mutableSetOf<Activity>()
    private val destroyed = CountDownLatch(1)
    private var closing = false
    private val callback = ActivityLifecycleCallback { activity, stage ->
        if (stage == Stage.DESTROYED) {
            pending.remove(activity)
            if (pending.isEmpty()) destroyed.countDown()
        }
    }
    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            var failure: Throwable? = null
            try { scenario = ActivityScenario.launch(MainActivity::class.java); base.evaluate() }
            catch (error: Throwable) { failure = error; throw error }
            finally {
                // 빈 테스트 Activity를 띄우지 않고 실제 Activity의 종료를 확인한다.
                try { close() }
                catch (error: Throwable) { if (failure != null) failure.addSuppressed(error) else throw error }
            }
        }
    }
    internal fun close() {
        beginClose()
        try {
            // 실제 종료를 확인한 뒤 ActivityScenario의 상태를 정리한다.
            check(destroyed.await(10, TimeUnit.SECONDS)) { "MainActivity 종료 통지 없음" }
            if (::scenario.isInitialized) scenario.close()
        } finally { test.runOnMainSync { monitor.removeLifecycleCallback(callback) } }
    }
    private fun beginClose() {
        if (closing) return
        closing = true
        test.runOnMainSync {
            monitor.addLifecycleCallback(callback)
            Stage.entries.filter { it != Stage.DESTROYED }.forEach { stage ->
                pending += monitor.getActivitiesInStage(stage).filterIsInstance<MainActivity>()
            }
            // API 34에서도 빈 Activity를 띄우는 ActivityScenario.close 경로를 피한다.
            if (pending.isEmpty()) destroyed.countDown()
            else pending.toList().forEach { it.finishAndRemoveTask() }
        }
    }
}
