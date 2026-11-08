# 연결 순서

엔진과 앱 연결이 main에 들어간 뒤, 역할끼리 붙일 때 따른다. 판단은 엔진(`core`)이 하고, 각 역할은 사건을 보내거나 명령을 받아 실행한다. 사건과 명령의 기준은 [contract_map.md](contract_map.md)다.

## 0. 공통

- main을 받고 `gradlew.bat :core:test :app:assembleDebug`가 통과하는지 본다.
- 앱 로그 : `adb logcat -s KQEpisode KidQuestVoice`. 음성에 간 명령(`Say`, `PlaySound`, `Listen`, `StopAudio`)이 `KidQuestVoice`에 찍힌다.
- 지금 음성은 가짜다. 말을 받으면 바로 끝냈다고 알리고, 듣기를 받으면 바로 「못 알아들음」을 돌려준다. 그래서 대사가 순식간에 넘어간다.

## 1. 앱 통합 : 앱이 켜지고 에피소드가 읽히나

- 설치해서 켠다. `KQEpisode`에 `episode_demo.json 읽음 : 임무 6개`가 나와야 한다(공통 2개, 목적지마다 1개).
- 앱이 바로 꺼지면 `AppViewModel`과 에피소드 파일부터 본다. 파일이 틀리면 앱은 켜지고, 틀린 곳이 `KQEpisode`에 찍힌다.

## 2. 시연·품질 : 재생 패널로 한 판 돌리기

예시 파일 기준이다. 재생 패널을 펼치고 위에서부터 누른다. 시작 화면에서 목적지를 안 고른 기본 이야기다. 목적지를 고르면 5절을 본다.

| 순서 | 누를 것 | `KidQuestVoice`에 나와야 하는 것 |
|---|---|---|
| 1 | 화면의 「시작」 | 없음(승무원 확인 화면) |
| 2 | 탑승 | `crew_found` |
| 3 | 벨트 매기 | `ready` |
| 4 | D | `take_off`, 이어서 `rain_map/briefing` |
| 5 | 와이퍼 켜기(D를 누르고 2분 안에) | `PlaySound(stardust)`, 3~4초 뒤 `rain_map/event`, `rain_map/question`. 2분이 지나면 파일의 120초가 되어 임무가 저절로 열리고, 그 뒤 와이퍼를 눌러도 아무것도 안 나온다 |
| 6 | 기다리기 | 듣기 동의가 없으면 8초 뒤 `rain_map/answer`. 이어서 다음 임무 `crossroads/briefing` |
| 6-1 | 깜빡이 | `crossroads/event`(효과음 없음). 3분이 지나면 저절로 열린다 |
| 7 | 벨트 풀기, 벨트 매기 | 풀면 `StopAudio`와 `belt_off`. 매면 하던 단계에 따라 `StopAudio` 뒤 다시 말한다. 대사 중이었으면 그 대사를, 효과음 뒤 기다리던 중이면 사건 대사를, 답을 기다리던 중이면 질문을 말한다. 신호를 기다리던 중이면 아무것도 안 나온다(6 다음이 이 경우다) |
| 8 | P | 임무가 남았으니 `StopAudio`, `docked`(정박) |
| 9 | 화면의 「여기서 착륙」 | `StopAudio`, `landed` |

- 답 버튼은 부모가 듣기에 동의했을 때만 쓰인다. 동의 버튼은 시작 화면 일(#23)이다. 그 전에는 답 버튼을 눌러도 8초 뒤 넘어간다.
- 임무를 다 끝낸 뒤 P를 누르면 결말(`ending`)을 말하고 착륙한다.
- 재생 패널의 「강 근처」(`river`)와 「답 : 고래」는 예전 파일 것이라 이제 쓰이지 않는다. 장소 버튼은 5절의 장소 id로 바꾼다.

## 3. 화면·음성 : 가짜 음성을 진짜로 바꿀 때

| 명령 | 지킬 것 |
|---|---|
| `Say(lineId, text)` | 끝까지 말하면 `LineFinished(lineId)`를 보낸다. 어떤 경우에도 보낸다. 안 보내면 임무가 그 자리에 멈춘다. `StopAudio`로 끊긴 대사는 보내지 않는다 |
| `PlaySound(soundId)` | 말하는 중에도 온다. 말을 끊지 말고 같이 낸다 |
| `Listen(words, 8)` | 결과를 `AnswerHeard(word)`로, 못 알아들으면 `AnswerHeard(null)`로 보낸다. 10초 안에 안 오면 엔진이 `StopAudio`를 보내고 넘어간다 |
| `StopAudio` | 말하기와 듣기를 둘 다 멈춘다 |

- 내비 안내로 소리를 잃으면 `AudioFocusChanged(false)`, 돌아오면 `AudioFocusChanged(true)`를 보낸다.
- 시작 화면 버튼은 `AgeSelected`, `DestinationSelected`, `ListeningConsentChanged`, `StartPressed`를 이 순서로 보낸다. 고른 값은 `StartPressed` 전에만 받는다.
- 장소 카드를 눌러도 화면이 직접 임무를 열지 않는다. 위치가 들어왔을 때와 같은 `PlaceEntered(placeId)`를 보낸다. 그래야 카드, 재생 버튼, 진짜 위치가 같은 길로 간다.
- 주행 중(기어 D)에는 앱 화면이 가려진다(PR #35). 화면 변화는 정박(P)과 착륙 화면에서 보인다.
- 화면 상태(`screen`, `status`, `items`)는 `AppController`가 들고 있다. 착륙 화면의 아이템은 임무마다 받은 `AwardItem`이다.

## 4. 차량·내비 연결 : `CarSignalCollector`가 보낼 사건

- `SeatOccupied(seat, occupied)`, `BeltBuckled(seat, buckled)`, `GearChanged(gear)`
- `TurnSignalOn(side)`, `WiperChanged(on)`. 와이퍼는 켜짐과 꺼짐을 둘 다 보낸다.
- 못 읽는 신호는 `SignalUnavailable(signal)`로 알린다.
- 에피소드 파일 `places`의 좌표 반경(`radiusMeters`)에 들어가면 `PlaceEntered(placeId)`를 보낸다. 에뮬레이터에서는 `adb emu geo fix <경도> <위도>`(경도가 먼저)로 옮긴다. 첫 시험에서 위치가 바뀌고 ENTER가 왔다.
- 어느 스레드에서 와도 `listener.onEvent`만 부르면 된다. `AppController`가 메인 스레드로 옮긴다.
- 확인 : 콘솔 값을 바꾸면 2절의 재생 버튼을 눌렀을 때와 같은 로그가 나와야 한다.

## 5. 목적지와 장소 : 판단 로직이 하는 일

시작 화면에서 목적지를 고르면 그 목적지 임무와 공통 임무만 한다. 목적지를 안 고르면 공통 임무만 한다.

| 목적지 id | 시작 화면 이름 | 장소 id | 좌표(위도, 경도) | 장소 임무 |
|---|---|---|---|---|
| `gyeongju` | 서울 → 경주 | `cheomseongdae` | 35.83472, 129.21917 | `gyeongju_star` |
| `busan` | 서울 → 부산 | `temporary_capital` | 35.1034, 129.0174 | `busan_capital` |
| `andong` | 서울 → 안동 | `hahoe` | 36.53806, 128.52056 | `andong_village` |
| `seoul` | 대전 → 서울 | `gyeongbokgung` | 37.57861, 126.97722 | `seoul_palace` |

- 순서 : `take_off` → 출발 문장(`departure`) → 공통 임무 `rain_map`(와이퍼), `crossroads`(깜빡이) → 목적지 장소 임무 → P에서 결말(`ending`) → 전달 문장(`delivery`) → 실제 사실(`landing_fact`) → `landed`.
- 장소 임무의 브리핑은 「가까워지면 알려 줄게요」로 미리 알리고, 장소에 들어온 사건 대사에서 「들어왔어요」라고 말한다. 브리핑은 앞 임무가 끝나자마자 나가기 때문이다.
- 장소는 차례가 오기 전에 지나가도 기억했다가 그 임무 차례에 브리핑 뒤 바로 연다. 같은 장소는 한 번만 센다. 다른 목적지의 장소나 없는 장소는 무시한다.
- 장소가 안 오면 600초 뒤 저절로 열린다.
- 도착(`DestinationArrived`)했거나 임무를 다 끝내고 착륙하면 전달 문장과 실제 사실을 말한다. 휴게소에서 「여기서 착륙」을 누르면 도착 전이라 말하지 않는다.
- 실제 사실은 `factSource`에 출처와 확인한 날을 적었다. 문장을 고치면 출처를 다시 확인한다.
