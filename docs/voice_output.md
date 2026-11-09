# F07 음성 출력

## 실행

`VoiceOutput`이 엔진의 Say / PlaySound / StopAudio를 실행한다. AppController의 기본 음성 구현이 실제 출력으로 바뀌었다. 서버 호출이나 별도 계정 등록 없이 Pleos TTS 3.3.1의 ON_DEVICE 모드를 사용한다.

- `Say`는 순서대로 읽는다. 정상 완료만 `LineFinished(lineId)`를 한 번 보낸다.
- `StopAudio`는 대기 중 대사, 재생 중 대사, 녹음 대체, 효과음과 듣기 대기 타이머를 모두 취소한다. 완료 사건은 보내지 않는다.
- 엔진이 같은 대사를 다시 보내면 새 재생으로 처음부터 읽는다. 이전 실행의 늦은 완료·오류는 무시한다.
- TTS 콜백에는 대사 ID가 없어서 재생마다 독립 SDK 인스턴스와 실행 번호를 사용한다. 초기화·재생에 실패하면 해당 문장과 일치하는 녹음으로 대체한다.
- 효과음은 대사를 끊지 않고 따로 재생하며, 동시에 최대 네 개다.
- 음성 상태는 정차 중 개발 영역과 `KidQuestVoice` 로그에서 확인한다. 대사 내용이나 아이 음성은 로그에 남기지 않는다.
- Listen은 동의한 경우에만 Locale.KOREA의 ON_DEVICE STT를 요청한다. 취소·최종 결과·시간 초과를 구분한다. [음성 참여 흐름](voice_interaction.md)을 참고한다. 실제 발화 정확도와 오디오 포커스는 별도 검증이 남아 있다.

## 녹음 대체 파일 추가

현재 실제 대사 녹음 파일은 없다. 사용자 요청에 따라 교체 가능한 구조와 빈 목록만 준비했다. 파일이 없거나 문장이 다르면 오류를 남기고 완료 사건을 보내지 않는다. 이때 임무는 음성이 성공한 것으로 진행하지 않는다.

1. 사용할 수 있는 음원인지 확인한 한국어 대사 녹음을 `app/src/main/assets/audio/recordings/`에 넣는다.
2. `catalog.json`의 `recordings` 배열에 아래 항목을 추가한다. 파일명은 소문자 영문·숫자·밑줄·하이픈과 wav/ogg/mp3 확장자만 허용한다.
3. `lineId`와 `text`가 엔진 명령과 정확히 같아야 재생한다. 대사를 수정하면 녹음과 목록도 함께 바꾼다.

```json
{"lineId":"ready","text":"선장님, 발사 준비 끝났어요.","file":"ready.wav"}
```

테스트 APK의 `fixture.wav`는 대체 경로 시험용 합성음이며 실제 대사 녹음이 아니다. 배포 앱에는 포함되지 않는다.

## 효과음 출처

현재 네 파일은 `scripts/generate_test_effects.py`에서 직접 합성한 **임시 시험용**이다. 외부 후보 음원을 내려받거나 최종 승인된 것으로 표시하지 않았다.

| ID | 파일 | 길이 |
|---|---|---|
| meteor | audio/effects/meteor.wav | 1.4초 |
| thruster_left | audio/effects/thruster_left.wav | 0.35초 |
| thruster_right | audio/effects/thruster_right.wav | 0.35초 |
| wormhole | audio/effects/wormhole.wav | 1초 |

시연·품질 담당이 최종 파일을 고르면 효과음 경로와 `docs/sound_licenses.md`를 갱신한다. 임시 파일의 ID를 유지하면 엔진 수정 없이 교체할 수 있다.

## 확인 범위

단위 시험은 완료 중복, 순서, 중단 후 동일 ID 재시작, 늦은 콜백, TTS 실패 후 녹음 대체, 녹음 없음, 외부 중단을 다룬다. 기기 시험은 ON_DEVICE 정상 완료, 재생 도중 중단, 초기화 직후 중단·재시작, 실제 MediaPlayer 파일 재생을 본다. 기기 결과는 실행 뒤 아래에 기록한다.

공식 문서:
- https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro
- https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/event-listeners
- https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/start-and-stop

## 2026-11-06 시험 결과

- JDK 21로 core/app 단위 시험과 앱·테스트 APK 빌드 통과. 음성 큐 단위 시험 6개 통과.
- Pleos v2.1.0 API 34에서 전체 기기 시험 10개 통과(음성 6개, 기존 화면·상태 4개).
- 실제 ON_DEVICE 정상 완료를 받고 이륙 뒤 임무 브리핑으로 연결되는 것을 확인했다.
- 실제 재생 도중 StopAudio 뒤 완료 사건 없음, 초기화 직후 취소 뒤 동일 ID 재시작, 네 효과음 파일 완료를 확인했다.
- 정상 종료에서 stop까지 부르면 다음 SDK 요청이 중단되는 문제가 있어, 정상 종료는 finish/release, 중단은 cancel/stop/release로 분리했다.
- 녹음 대체는 의도적으로 TTS를 실패시킨 뒤 테스트 APK의 합성음 파일을 MediaPlayer로 재생해 확인했다. 실제 대사 녹음은 아직 없다.
- 소리가 들렸다는 사람의 청취 판정과 음량 적절성, 내비 음성과의 양보는 별도 확인이 필요하다. 이번 결과는 SDK·플레이어 콜백과 엔진 연결 시험이다.
