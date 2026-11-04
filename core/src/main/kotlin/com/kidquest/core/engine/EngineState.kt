// 앱의 상태와 멈춘 이유. PRD 상태도의 6개 상태와 같다.
package com.kidquest.core.engine

enum class EngineState { WAITING, CREW_CHECK, READY, FLYING, PAUSED, LANDED }

/** 멈춘 이유. 이유가 하나라도 남아 있으면 이어지지 않는다. */
enum class PauseReason { BELT, AUDIO_FOCUS, GEAR }
