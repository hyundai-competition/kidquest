# SDK 경로 한 판과 기기 시험 생명주기 점검 (2026-11-11)

기준 커밋은 `482e381`, 로컬 브랜치는 `feature/route-lifecycle-check`다. 커밋·푸시 없이 차량 연결과 시험 도구를 보완했다.

## 성공한 SDK 한 판

서울→경주 경부 경로를 가상 차량 콘솔·ADB GPS·실제 Pleos SDK·ON_DEVICE TTS로 진행했다. 앱의 PlaceEntered, GearChanged, WiperChanged, LineFinished를 직접 만들어 넣지 않았다.

- 출발 설정과 부모의 카시트·벨트 확인은 화면과 같은 조작 사건을 사용했다.
- 가상 차량 콘솔의 P→D, 와이퍼 Low, 마지막 P 전환을 차량 SDK에서 받았다.
- GPS를 경유지로 옮긴 뒤 FusedLocation이 `cheonan`, `daegu`, `cheomseongdae` 세 곳의 진입을 보냈다.
- 마이크 동의는 껐다. 질문은 무응답으로 두고 기존 엔진의 8초 규칙을 따랐다.
- 내비 경로 안내가 없는 조건이어서 갈림길은 기존 3분 대비 규칙으로 열렸다.
- 보물 5개를 모은 뒤 P로 바꾸자 LANDED 화면이 나왔다.
- 결말 → 배달 → 사실 → 마지막 인사 순서로 실제 TTS 완료 콜백을 받았다.
- 결과: 약 406.8초, TTS 28개 정상 완료, 재생 오류 0개, 녹음 대체 0개, `replayUsed=false`.

이것은 **에뮬레이터 GPS 경유지 재생으로 실제 SDK 연결을 검증한 한 판**이다. 실제 차로 서울→경주 도로를 달린 시험이나, 기본 내비의 연속 경로 안내·도착 사건까지 검증한 시험으로 표현하지 않는다. 좌표는 저장소의 공개 시연 장소를 사용했고 아이의 목소리·위치를 저장하지 않았다.

별도의 `SdkLocationRouteTest`도 실제 SDK의 세 진입을 받아 약 13.8초에 통과했다.

## 기본 내비 자체의 진행은 별도 미완료

`NaviProgressProbeTest`에서 첨성대의 고정 좌표를 NaviHelper.requestRoute로 요청했다. 이 호출에는 기존 조회 권한 NAVI_ROUTE 외에 NAVI_ROUTE_SEARCH가 필요했다. SDK 소스의 권한 설명에 따라 **debug 전용 manifest에만 Normal 권한**을 선언했다. release 권한이나 앱의 목적지 검색 화면은 바꾸지 않았다.

- 요청한 목적지 정보는 실제 SDK에서 받았다.
- GPS를 첨성대 근처에서 이동하고 올바른 체크섬의 GGA/RMC도 입력했다.
- 그러나 60초 동안 내비 SDK의 현재 위치는 한 지점만 보고했고 목적지 근처로 갱신되지 않았다.
- 최종 응답: `destinationReceived=true`, `nearTarget=false`, `distinctPositions=1`, `remainingSeconds=12600`, `arrived=false`.
- 자동 생성 NMEA의 체크섬 오류(`expected=47`)도 계속 관측됐다. 이 오류가 유일한 원인이라고 단정하지 않는다.

따라서 기본 내비 도착 사건, 실제 안내 음성과의 전체 연동, 실제 연속 주행은 완료로 표시하지 않는다. 가짜 도착 사건을 넣어 성공 처리하지 않았다. 실패하는 진단은 opt-in이며 기본 테스트에서는 건너뛴다. 진단이 끝나면 요청한 내비 경로를 취소한다.

## Activity 생명주기 조사와 시작 정체 수정

기존 규칙으로 TreasureDisplayTest·GameplayFixesTest 4개를 연속 실행하면 1개가 `Current state was null unexpectedly. Last stage = STARTED`로 종료 정리 단계에서 실패했다. 이전의 `Activity has been destroyed already`와 함께 창 생명주기 경합으로 조사했다.

설치된 AndroidX test core 1.6.1 소스의 finishActivity는 API 15~19 대응 설명과 달리 API 34에서도 EmptyActivity를 띄운 뒤 실제 Activity를 끝낸다. 시스템 로그에서는 Pleos 런처가 이 빈 Activity도 별도 앱 창으로 받아 앞뒤 창을 전환하는 것이 확인됐다. [AndroidX ActivityScenario 설명](https://developer.android.com/reference/androidx/test/core/app/ActivityScenario)과 설치된 소스의 동작을 대조했다.

`PleosActivityRule`은 ActivityScenario의 시작·재생성 API를 유지하되, 종료할 때 MainActivity.finishAndRemoveTask를 호출하고 실제 DESTROYED 통지를 기다린 뒤 scenario를 닫는다. 실패를 무시하거나 재시도해 통과시키지 않는다. 원래 시험 실패와 정리 실패도 보존한다.

초기 6개 시험을 3회 반복한 18개 실행은 통과했지만, 범위를 넓힌 13개 시험 중 2개에서 종료 대기가 다시 실패했다. 이후 시작 자체도 정체되어 디버거로 Java 스레드를 조사했다.

### 실제로 확인한 시작 정체와 앱 수정

- 메인 스레드: MainActivity.onCreate → startConnections → DrivingStateSignals.start → Car.getCarManager → ICar.getCarService Binder 대기.
- 단순히 조회만 작업 스레드로 옮기면 Car 내부 잠금을 기다리는 onServiceConnected가 메인 스레드를 다시 막았다.
- DrivingStateSignals에서 **Car 생성·조회·연결 해제를 전부 공용 작업 스레드에서 처리**하도록 바꿨다.
- CAR_WAIT_TIMEOUT_DO_NOT_WAIT와 준비 완료 콜백을 쓰고 준비 통지를 받은 뒤에만 manager를 조회한다. [공식 Car 연결 API](https://developer.android.com/reference/android/car/Car)의 준비 순서를 따른다.
- 5초 동안 준비되지 않으면 기존 SignalUnavailable(DRIVING_STATE)를 보낸다. 뒤늦게 정상 연결되면 실제 규제 값을 받을 수 있다.
- 종료 시에는 연결을 비활성화해 늦은 콜백을 차단하고, 화면에서 Binder 종료를 기다리지 않는다. 취소된 대기 연결은 실행하지 않으며 재시작마다 스레드를 늘리지 않는다.
- 소프트웨어 그래픽에서 내장 DrivingView의 libUnreal.so 충돌도 별도로 관측됐다. 브루미 예외로 오인하지 않으며 그 시스템 앱을 수정하지 않았다.

재부팅 후 검증용 규칙으로 UI·음성 시험 19개를 실행해 16개 통과, 3개 실패했다.

- TreasureDisplayTest.emptyAndFourPhotosFitBothLanguagesAndWidths: Compose 화면 트리 없음.
- NavigationControlsTest.disabledChoicesHaveNoNativeAccessibilityClickAction: 접근성 노드 조회 시간 초과.
- NavigationControlsTest.exitWaitsForConfirmation: Compose 화면 트리 없음.

기존 시험 10개에 새 규칙을 일괄 적용하는 변경은 철회했다. PleosActivityRule은 선택 SDK 한 판 시험에서만 사용한다. 최종 시작 정체 수정은 DrivingStateSignals에 한정한다. 모든 Pleos 시스템 충돌이나 Activity 종료 가능성이 해결됐다는 뜻이 아니다.

단위 시험은 core 192개 + app 49개로 총 241개 통과했고 debug 앱·시험 APK 빌드와 저장소 규칙 검사도 통과했다. 최종 코드의 SDK 한 판 재실행은 시작 후 약 8.7초에 Activity 소실로 실패했다. 따라서 위 406.8초 성공은 **차량 연결 수정 전 기준 코드의 최초 시험 결과**이고, 최종 수정 상태의 완주 성공으로 바꿔 적지 않는다.

실패 시각 14:01:36.993에 Pleos 홈 런처(ai.umos.homescreen)가 IllegalArgumentException: Listener for taskId=1001722 already exists로 충돌했다. 스택은 ShellTaskOrganizer.addListenerForTaskId → TaskViewManager.handleMultiWindowTaskAppeared였고, 해당 task는 충돌·재실행 중인 내장 DrivingView였다. 직후 브루미 Activity가 소실됐고 시험이 Activity has been destroyed already로 실패했다. 브루미의 예외와는 다른 시스템 런처 충돌이다.

시험 이미지 식별값은 Pleos RELEASE.release_connect_v2.1.0이다. 이 시스템 앱을 비활성화하거나 이미지 파일을 수정하지 않았다. 최종 코드로 안정적인 반복 완주 및 기본 내비 진행을 확인하는 작업은 남아 있다.

## 다시 실행

JDK 21과 실행 중인 Pleos AVD가 필요하다. 기본 내비 경로 안내는 종료한 조건으로 시작한다. 브루미의 실제 STT 시험은 별도다.

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell pm grant --user 10 com.kidquest.app pleos.car.permission.FUSED_LOCATION
adb -s emulator-5554 forward tcp:8088 tcp:8088
python3 scripts/run_sdk_trip.py
```

구동기는 콘솔 세션을 유지하고 가상 차량과 GPS를 조작한다. 끝나면 속도 0·P로 정리하고 원래 와이퍼·콘솔 모드를 복구한다. GPS 복원은 별도로 한다. 테스트 중 다른 instrumentation을 실행하면 현재 시험이 중단되므로 동시에 실행하지 않는다.

실행 로그는 기본 `/private/tmp/kq-sdk-trip/`에 남긴다. 마지막 상태뿐 아니라 SDK의 세 장소 사건과 TTS 오류·녹음 대체 여부도 확인한다. `adb emu`는 KO 응답에도 프로세스 종료 코드가 0일 수 있어 응답 본문까지 검사한다.

올바른 위치 명령의 순서는 `geo fix 경도 위도 고도 위성수 속도(knots)`다. 예를 들어 위성 수 8, 속도 0은 마지막 인자를 `20 8 0`으로 쓴다.
