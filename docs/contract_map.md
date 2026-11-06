# 기능과 약속 대조표

P0 기능을 사건, 명령, 에피소드 칸으로 어떻게 적는지 정리한다. 약속은 만들면서 수정하고 보완한다. 자기 기능을 이 표로 적을 수 없으면 `core/.../contract/`를 고치는 PR에 `약속 변경` 라벨을 달아 올린다.

「지금」 칸의 **엔진 됨**은 `:core:test`로 확인했다는 뜻이다. 차 신호 수집 코드는 붙였지만 값이 실제로 오는지는 에뮬레이터에서 아직 시험하지 않았다. 실제 소리는 아직 붙이지 않았다.

| 기능 | 보내는 사건 | 엔진이 시키는 명령 | 에피소드 칸 | 지금 |
|---|---|---|---|---|
| F01 연령 선택 | `AgeSelected` | | `Mission.ageGroups` | 약속만 |
| F02 승무원 확인 | `SeatOccupied`, `ParentConfirmedBelt` | `Say`, `ShowScreen(CREW_CHECK)` | | 엔진 됨 |
| F03 벨트 발사 | `BeltBuckled`, `ParentConfirmedBelt`, `GearChanged` | `Say` | | 엔진 됨 |
| F04 이륙, 정박, 착륙 | `GearChanged`, `DestinationArrived`, `EndTripPressed` | `Say`, `StopAudio`, `ShowScreen(FLYING, DOCKED, LANDED)` | `Episode.endingLine` | 엔진 됨 |
| F05 임무 진행 | `LineFinished`, `AnswerHeard`, `Tick` | `Say`, `PlaySound`, `Listen`, `AwardItem` | `Mission`, `Item` | 엔진 됨, 앱 연결 됨(가짜 음성) |
| F06 차 신호 사건 | `TurnSignalOn`, `WiperChanged`, `Tick` | `PlaySound`, `Say` | `SignalReaction`, `Mission.trigger` | 엔진 됨, 반응 대사(`line`)는 아직 안 씀 |
| F07 음성 출력 | `LineFinished` | `Say`, `StopAudio` | | 가짜 음성 |
| F08 아이 답 받기 | `ListeningConsentChanged`, `AnswerHeard` | `Listen` | `Mission.answers` | 엔진 됨, 실제 듣기 전 |
| F09 벨트 풀림 멈춤 | `BeltBuckled` | `StopAudio`, `Say`, `ShowScreen(PAUSED, 이유)` | | 엔진 됨 |
| F10 소리 양보 | `AudioFocusChanged` | `StopAudio`, `ShowScreen(PAUSED, 이유)` | | 엔진 됨 |
| F11 주행 중 화면 | `DrivingRestricted` | `ShowScreen(FLYING, 상태 한 줄)` | | 약속만 |
| F12 착륙 화면 | | `ShowScreen(LANDED)`, `AwardItem` | `Item` | 약속만 |
| F13 끄기 | `ParentStopChanged` | `StopAudio`, `ShowScreen(PAUSED, 이유)` | | 엔진 됨 |
| F14 신호 없는 차 처리 | `SignalUnavailable`, `Tick` | | `Mission.autoAfterSeconds` | 엔진 됨(시간이 지나면 열림) |
| F15, F16 연령별 임무 | `AgeSelected` | | `Mission.ageGroups` | 약속만 |
| F17 남은 시간 맞춤 | `RemainingTimeChanged` | | | 약속만 |
| F19 우주선 에너지 | `EnergyChanged` | `PlaySound`, `Say` | `SignalReaction` | 약속만 |
| F21 재생 모드 | 위의 차 신호 사건 전부 | | | 일부 버튼만 |
| F24 위치 미션 | `PlaceEntered` | `Say`, `Listen` | `Place`, `Mission.trigger` | 약속만 |
| F25 목적지 이야기 | `DestinationSelected`, `DestinationArrived` | `Say` | `Destination`, `Mission.destinationId` | 약속만 |
| F32 실내 환경 | `CabinTemperatureChanged` | `Say`, `SetBackground` | `SignalReaction` | 약속만 |
| F33 마지막 미션 | `RemainingTimeChanged`, `DestinationArrived` | `Say`, `AwardItem`, `ShowScreen(LANDED)` | `Episode.finalLine`, `Destination.approachLine` | 약속만 |

## P1 중 약속을 먼저 넣은 것

| 기능 | 보내는 사건 | 엔진이 시키는 명령 | 에피소드 칸 |
|---|---|---|---|
| F18 회전 앞 질문 쉬기 | `NextTurnDistance` | | |
| F36 내비 도로 사건 | `RoadAhead` | `PlaySound`, `Say` | `SignalReaction` |

## 정하고 시작한 규칙

- **대사 끝남 :** 음성은 `Say`를 끝까지 말하면 `LineFinished(lineId)`를 보낸다. `StopAudio`로 끊긴 대사는 보내지 않는다. TTS에 일시정지 함수가 안 보여서, 이어 말할 때는 그 문장을 처음부터 다시 말한다. 음성은 어떤 경우에도 말을 끝내면 이 사건을 보내야 한다. 안 보내면 임무가 그 자리에 멈춘다.
- **듣기 :** `StopAudio`는 말하기와 듣기를 둘 다 멈춘다. 동의하지 않으면 `Listen`을 보내지 않고 8초 뒤 넘어간다. `Listen` 뒤 10초 안에 `AnswerHeard`가 안 오면 엔진이 듣기를 멈추고 넘어간다.
- **시간 :** 엔진은 시계를 읽지 않는다. `AppController`가 1초마다 `Tick`을 보낸다.
- **스레드 :** 차 SDK 콜백이 어느 스레드에서 와도 `listener.onEvent`만 부르면 된다. `AppController`가 메인 스레드로 옮긴다.
- **겹침 :** 내비 안내 중에는 말하지 않는다. 그동안 나온 대사는 마지막 하나만 남겼다가 안내가 끝나면 말한다.
- **못 읽는 신호 :** 차 신호를 못 읽으면 `SignalUnavailable`을 보낸다. 탑승과 벨트는 저절로 넘어가지 않고 부모 확인(`ParentConfirmedBelt`)이나 재생 모드로 넘어간다. 임무는 신호가 안 오면 `autoAfterSeconds` 뒤에 저절로 열린다. 위치와 도로 임무도 #10과 F36 전까지는 시간으로 열린다.
- **효과음 :** 임무 효과음 뒤 3초(가정) 있다가 사건 대사를 말한다. 깜빡이 같은 반응 소리는 말하는 중에도 올 수 있다. 음성은 말을 끊지 말고 효과음을 같이 낸다.
- **착륙 :** 도착했거나 임무를 다 끝냈으면 기어 P에서 착륙하고, 아니면 정박한다. 임무를 다 끝냈을 때만 결말(`endingLine`)을 말하고, 결말이 끝나면(`LineFinished("ending")`) 착륙 대사를 말한다. 착륙 화면의 아이템은 임무마다 보낸 `AwardItem`을 모은 것이다.
- **와이퍼 :** 켜짐과 꺼짐을 다 보낸다. 엔진이 켜져 있는지 기억해서, 비 오는 날 계속 켜져 있어도 와이퍼 임무가 열린다.
- **반복 값 :** `car/`는 값이 바뀔 때만 사건을 보낸다. 방향지시등은 켜질 때만 보낸다. 도착은 거르지 않는다.
- **내비 값 :** 남은 시간과 회전 안내는 10초마다 묻는다. `RoadAhead`와 `NextTurnDistance`는 거리가 바뀌면 다시 오니, 엔진이 쿨다운이나 거리 기준으로 한 번만 쓴다. 남은 시간은 초, 거리는 미터로 가정했고 첫 시험에서 확인한다.
