# 연결 순서

엔진과 앱 연결이 main에 들어간 뒤, 역할끼리 붙일 때 따른다. 판단은 엔진(`core`)이 하고, 각 역할은 사건을 보내거나 명령을 받아 실행한다. 사건과 명령의 기준은 [contract_map.md](contract_map.md)다.

## 0. 공통

- main을 받고 `gradlew.bat :core:test :app:assembleDebug`가 통과하는지 본다.
- 앱 로그 : `adb logcat -s KQEpisode KidQuestVoice`. 음성에 간 명령(`Say`, `PlaySound`, `Listen`, `StopAudio`)이 `KidQuestVoice`에 찍힌다.
- 지금 음성은 가짜다. 말을 받으면 바로 끝냈다고 알리고, 듣기를 받으면 바로 「못 알아들음」을 돌려준다. 그래서 대사가 순식간에 넘어간다.

## 1. 앱 통합 : 앱이 켜지고 에피소드가 읽히나

- 설치해서 켠다. `KQEpisode`에 `episode_demo.json 읽음 : 임무 2개`가 나와야 한다.
- 앱이 바로 꺼지면 `AppViewModel`과 에피소드 파일부터 본다. 파일이 틀리면 앱은 켜지고, 틀린 곳이 `KQEpisode`에 찍힌다.

## 2. 시연·품질 : 재생 패널로 한 판 돌리기

예시 파일 기준이다. 재생 패널을 펼치고 위에서부터 누른다.

| 순서 | 누를 것 | `KidQuestVoice`에 나와야 하는 것 |
|---|---|---|
| 1 | 화면의 「시작」 | 없음(승무원 확인 화면) |
| 2 | 탑승 | `crew_found` |
| 3 | 벨트 매기 | `ready` |
| 4 | D | `take_off`, 이어서 `meteor_shower/briefing` |
| 5 | 와이퍼 켜기 | `PlaySound(meteor)`, 3~4초 뒤 `meteor_shower/event`, `meteor_shower/question` |
| 6 | 기다리기 | 듣기 동의가 없으면 8초 뒤 `meteor_shower/answer`. 이어서 다음 임무 `water_planet/briefing` |
| 7 | 벨트 풀기, 벨트 매기 | 풀면 `StopAudio`와 `belt_off`. 매면 말하던 대사가 있었을 때만 `StopAudio` 뒤 그 대사를 처음부터 말한다. 신호를 기다리던 중이면 아무것도 안 나온다 |
| 8 | P | 임무가 남았으니 `docked`(정박) |
| 9 | 화면의 「여기서 착륙」 | `landed` |

- 답 버튼(「답 : 초록색」 등)은 부모가 듣기에 동의했을 때만 쓰인다. 동의 버튼은 시작 화면 일(#23)이다. 그 전에는 답 버튼을 눌러도 8초 뒤 넘어간다.
- 두 번째 임무(강 근처)는 위치 판단(#10) 전이라 「강 근처」를 눌러도 반응이 없다. 300초 뒤 저절로 열린다.
- 임무를 다 끝낸 뒤 P를 누르면 결말(`ending`)을 말하고 착륙한다.

## 3. 화면·음성 : 가짜 음성을 진짜로 바꿀 때

| 명령 | 지킬 것 |
|---|---|
| `Say(lineId, text)` | 끝까지 말하면 `LineFinished(lineId)`를 보낸다. 어떤 경우에도 보낸다. 안 보내면 임무가 그 자리에 멈춘다. `StopAudio`로 끊긴 대사는 보내지 않는다 |
| `PlaySound(soundId)` | 말하는 중에도 온다. 말을 끊지 말고 같이 낸다 |
| `Listen(words, 8)` | 결과를 `AnswerHeard(word)`로, 못 알아들으면 `AnswerHeard(null)`로 보낸다. 10초 안에 안 오면 엔진이 `StopAudio`를 보내고 넘어간다 |
| `StopAudio` | 말하기와 듣기를 둘 다 멈춘다 |

- 내비 안내로 소리를 잃으면 `AudioFocusChanged(false)`, 돌아오면 `AudioFocusChanged(true)`를 보낸다.
- 시작 화면 버튼은 `AgeSelected`, `ListeningConsentChanged`, `StartPressed`를 보낸다.
- 화면 상태(`screen`, `status`, `items`)는 `AppController`가 들고 있다. 착륙 화면의 아이템은 임무마다 받은 `AwardItem`이다.

## 4. 차량·내비 연결 : `CarSignalCollector`가 보낼 사건

- `SeatOccupied(seat, occupied)`, `BeltBuckled(seat, buckled)`, `GearChanged(gear)`
- `TurnSignalOn(side)`, `WiperChanged(on)`. 와이퍼는 켜짐과 꺼짐을 둘 다 보낸다.
- 못 읽는 신호는 `SignalUnavailable(signal)`로 알린다.
- 어느 스레드에서 와도 `listener.onEvent`만 부르면 된다. `AppController`가 메인 스레드로 옮긴다.
- 확인 : 콘솔 값을 바꾸면 2절의 재생 버튼을 눌렀을 때와 같은 로그가 나와야 한다.
