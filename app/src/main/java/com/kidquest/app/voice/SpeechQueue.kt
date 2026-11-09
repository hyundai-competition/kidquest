// 대사를 순서대로 읽는다. 정상 완료와 복구 불가 오류는 한 번 진행시키고, 명시적 중단은 완료로 보내지 않는다.
// 모든 호출은 VoiceOutput이 메인 스레드로 보낸다. lineId가 같아도 재생 실행 번호는 다르다.
package com.kidquest.app.voice

import com.kidquest.core.contract.Command

class SpeechQueue(
    private val tts: SpeechFactory,
    private val recordings: SpeechFactory,
    private val finished: (String) -> Unit,
    private val report: (String) -> Unit,
    private val onSpeaking: (Command.Say) -> Unit = {},
    private val unavailable: (String) -> Unit = finished,
) {
    private data class Active(val token: Long, val line: Command.Say, val fallback: Boolean, var job: SpeechPlayback? = null)
    private val pending = ArrayDeque<Command.Say>()
    private var serial = 0L
    private var active: Active? = null

    fun say(line: Command.Say) {
        pending.addLast(line)
        if (active == null) next()
    }

    fun stop() {
        pending.clear()
        val old = active
        active = null // SDK stop 콜백보다 먼저 무효화한다.
        old?.job?.cancel()
        report("음성 중단")
    }

    private fun next() {
        if (pending.isEmpty()) return
        play(pending.removeFirst(), false)
    }

    private fun play(line: Command.Say, fallback: Boolean) {
        val request = Active(++serial, line, fallback)
        active = request
        val callback = object : PlaybackCallback {
            override fun started() {
                if (active?.token == request.token) { onSpeaking(line); report("재생 중: ${line.lineId}") }
            }
            override fun completed() = complete(request.token)
            override fun failed(reason: String) = fail(request.token, reason)
            override fun interrupted() = interrupt(request.token)
        }
        report(if (fallback) "녹음 대체 재생: ${line.lineId}" else "ON_DEVICE 읽기: ${line.lineId}")
        try {
            request.job = (if (fallback) recordings else tts).create(line, callback)
            if (request.job == null) fail(request.token, "일치하는 녹음 파일 없음") else request.job?.start()
        } catch (error: Exception) {
            fail(request.token, error.javaClass.simpleName)
        }
    }

    private fun complete(token: Long) {
        val current = active?.takeIf { it.token == token } ?: return
        active = null
        current.job?.finish()
        report("재생 완료: ${current.line.lineId}")
        finished(current.line.lineId)
        // 완료 사건이 즉시 새 대사를 요청해도 동시에 두 대사를 시작하지 않는다.
        if (active == null) next()
    }

    private fun fail(token: Long, reason: String) {
        val current = active?.takeIf { it.token == token } ?: return
        active = null
        current.job?.cancel()
        report("재생 오류: ${current.line.lineId} / $reason")
        if (!current.fallback) play(current.line, true) else {
            report("음성 재생 불가: 화면으로 이어갑니다 (${current.line.lineId})")
            unavailable(current.line.lineId)
            if (active == null) next()
        }
    }

    private fun interrupt(token: Long) {
        if (active?.token != token) return
        stop() // 외부에서 중단된 소리를 녹음으로 다시 틀지 않는다.
    }
}
