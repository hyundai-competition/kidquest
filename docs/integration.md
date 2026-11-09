# 연결 순서

엔진과 앱 연결이 main에 들어간 뒤, 역할끼리 붙일 때 따른다. 판단은 엔진(`core`)이 하고, 각 역할은 사건을 보내거나 명령을 받아 실행한다. 사건과 명령의 기준은 [contract_map.md](contract_map.md)다.

## 0. 공통

- main을 받고 `gradlew.bat :core:test :app:assembleDebug`가 통과하는지 본다.
- 앱 로그 : `adb logcat -s KQEpisode KidQuestVoice`. 음성에 간 명령(`Say`, `PlaySound`, `Listen`, `StopAudio`)이 `KidQuestVoice`에 찍힌다.
- 지금 음성은 가짜다. 말을 받으면 바로 끝냈다고 알리고, 듣기를 받으면 바로 「못 알아들음」을 돌려준다. 그래서 대사가 순식간에 넘어간다.

## 1. 앱 통합 : 앱이 켜지고 에피소드가 읽히나

- 설치해서 켠다. `KQEpisode`에 `episode_demo.json 읽음 : 임무 14개`가 나와야 한다(공통 2개, 중간 도시 8개, 목적지 4개).
- 앱이 바로 꺼지면 `AppViewModel`과 에피소드 파일부터 본다. 파일이 틀리면 앱은 켜지고, 틀린 곳이 `KQEpisode`에 찍힌다.

## 2. 시연·품질 : 재생 패널로 한 판 돌리기

예시 파일 기준이다. 재생 패널을 펼치고 위에서부터 누른다. 시작 화면에서 목적지를 안 고른 기본 이야기다. 목적지를 고르면 5절을 본다.

| 순서 | 누를 것 | `KidQuestVoice`에 나와야 하는 것 |
|---|---|---|
| 1 | 화면의 「시작」 | 없음(승무원 확인 화면) |
| 2 | 탑승 | `crew_found` |
| 3 | 벨트 매기 | `ready` |
| 4 | D | `take_off`, 이어서 `rain_map/briefing` |
| 5 | 와이퍼 켜기(D를 누르고 2분 안에) | `PlaySound(wipe_drops)`, 3~4초 뒤 `rain_map/event`, `rain_map/question`. 2분이 지나면 파일의 120초가 되어 임무가 저절로 열리고, 그 뒤 와이퍼를 눌러도 아무것도 안 나온다 |
| 6 | 기다리기 | 듣기 동의가 없으면 8초 뒤 `rain_map/answer`. 이어서 다음 임무 `crossroads/briefing` |
| 6-1 | 기다리기 | 3분 뒤 `crossroads/event`(효과음 없음). 재생 패널은 내비 회전 안내를 안 보내서 시간으로 열린다. 깜빡이 버튼으로는 안 열린다. 갈림길 버튼을 만들면 `NextTurnDistance(200)` 다음 `NextTurnDistance(1500)`을 보낸다 |
| 7 | 벨트 풀기, 벨트 매기 | 풀면 `StopAudio`와 `belt_off`. 매면 하던 단계에 따라 `StopAudio` 뒤 다시 말한다. 대사 중이었으면 그 대사를, 효과음 뒤 기다리던 중이면 사건 대사를, 답을 기다리던 중이면 질문을 말한다. 신호를 기다리던 중이면 아무것도 안 나온다(6 다음이 이 경우다) |
| 8 | P | 임무가 남았으니 `StopAudio`, `docked`(정박) |
| 9 | 화면의 「여기서 착륙」 | `StopAudio`, `landed` |

- 5에서 와이퍼를 안 켜 임무가 저절로 열리면 효과음 없이 맑은 날 대사(`rain_map/event`, 「오늘은 비가 안 와서…」)를 말한다.
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
- 시작 화면 버튼은 `AgeSelected`, `DestinationSelected(목적지 id, 경로 id)`, `ListeningConsentChanged`, `StartPressed`를 이 순서로 보낸다. 고른 값은 `StartPressed` 전에만 받는다. 경로 목록은 파일의 `destinations[].routes`이고, 경로 id를 빼면 첫 경로로 간다.
- 장소 카드를 눌러도 화면이 직접 임무를 열지 않는다. 위치가 들어왔을 때와 같은 `PlaceEntered(placeId)`를 보낸다. 그래야 카드, 재생 버튼, 진짜 위치가 같은 길로 간다.
- 주행 중(기어 D)에는 앱 화면이 가려진다(PR #35). 화면 변화는 정박(P)과 착륙 화면에서 보인다.
- 화면 상태(`screen`, `status`, `items`)는 `AppController`가 들고 있다. 착륙 화면의 아이템은 임무마다 받은 `AwardItem`이다.
- 출처 표시 : 에피소드 파일의 `credits` 두 줄(역사 내용 출처, OpenStreetMap 위치 데이터 표시)을 앱 정보 화면이나 착륙 화면에 보여 준다. 좌표를 OpenStreetMap에서 가져와서 앱에 표시가 있어야 한다.
- 착륙 화면 배경은 `AppController.background`(`NORMAL`, `COOL`, `WARM`)다. 엔진이 실내 온도 띠가 바뀔 때만 `SetBackground`를 보낸다.

## 4. 차량·내비 연결 : `CarSignalCollector`가 보낼 사건

- `SeatOccupied(seat, occupied)`, `BeltBuckled(seat, buckled)`, `GearChanged(gear)`
- `TurnSignalOn(side)`, `WiperChanged(on)`. 와이퍼는 켜짐과 꺼짐을 둘 다 보낸다. 깜빡이는 짧은 반응 소리만 내고 임무는 안 연다.
- 못 읽는 신호는 `SignalUnavailable(signal)`로 알린다.
- 남은 시간 `RemainingTimeChanged(seconds)`는 초, 앞길 `RoadAhead(kind, meters)`는 미터, 실내 온도 `CabinTemperatureChanged(celsius)`는 섭씨로 보낸다. 같은 값은 다시 안 보내도 되고, 다시 와도 엔진이 한 번만 쓴다.
- 내비 회전 안내 `onTBTInfo`가 오면 목록 첫 회전까지 거리를 `NextTurnDistance(meters)`로 보낸다. 거리가 줄어드는 동안 10초보다 드물지 않게 계속 보내야 한다.
  - 엔진은 500m 안에 왔던 회전의 거리가 100m 넘게 늘면 그 갈림길을 지난 것으로 보고 갈림길 임무(`crossroads`)를 연다. 두 값 다 가정이다. 10초마다 받아도 시속 150km(10초에 약 420m)까지는 지나기 전 마지막 값이 500m 안에 든다.
  - 콜백이 목록이 바뀔 때만 오면 엔진이 500m 안의 값을 못 봐서 갈림길을 지나도 모르고, 아래 10분이 지나야 열린다. 그때는 `getTBTInfo()`(샘플 코드에 이름이 있음)로 몇 초마다 물어서 보낸다. 에뮬레이터와 실제 차에서 얼마나 자주 오는지는 아직 확인하지 않았다.
  - `TURN_TUNNEL`, `TURN_TOLL`처럼 길이 갈라지지 않는 안내 지점은 넣지 않고 `RoadAhead`로 보낸다. 휴게소 안내(`TURN_RESTAREA`)는 지나가기만 할 수 있어서 넣지 않는다. 좌우 회전과 `TURN_ENTER_IC`, `TURN_EXIT_IC`는 넣는다. 모두 가정이고, Pleos 문서에서 옮긴 이름이라 SDK에서 철자를 확인한다.
  - 내비를 아예 못 읽으면 `SignalUnavailable(NAVIGATION)`을 보낸다. 그때와 회전 안내가 한 번도 안 온 이동에서만 갈림길 임무가 시간으로 열린다.
  - 안내가 오는 중이면 3분으로는 안 열고 실제 갈림길을 기다린다. 10분 동안 못 지나면 연다(가정). 회전 거리가 드물게 오거나 이동 중에 안내를 꺼도 멈추지 않게 하려는 것이다. 그 전에 뒤 장소에 닿거나 도착 5분 전이 되면 보물 없이 건너뛴다.
- 에피소드 파일 `places`의 좌표 반경(`radiusMeters`)에 들어가면 `PlaceEntered(placeId)`를 보낸다. 원이 겹치면 들어간 원마다 보낸다. 에뮬레이터에서는 `adb emu geo fix <경도> <위도>`(경도가 먼저)로 옮긴다. 첫 시험에서 위치가 바뀌고 ENTER가 왔다.
- 위치를 아예 못 읽는 차면 `SignalUnavailable(LOCATION)`을 보낸다. 그때만 장소 임무가 시간으로 열린다.
- 어느 스레드에서 와도 `listener.onEvent`만 부르면 된다. `AppController`가 메인 스레드로 옮긴다.
- 확인 : 콘솔 값을 바꾸면 2절의 재생 버튼을 눌렀을 때와 같은 로그가 나와야 한다.

## 5. 목적지, 경로, 장소 : 판단 로직이 하는 일

시작 화면에서 목적지와 경로를 고르면 공통 임무를 먼저 하고, 그 경로에 적힌 장소 임무를 가는 순서대로 한다. 경로를 안 고르면 그 목적지의 첫 경로로 간다. 목적지를 안 고르면 공통 임무만 한다.

| 목적지 id | 시작 화면 이름 | 경로 id | 경로 이름 | 장소 임무(가는 순서) |
|---|---|---|---|---|
| `gyeongju` | 서울 → 경주 | `gyeongju_gyeongbu` | 경부고속도로 (천안·대구) | `cheonan_independence` → `daegu_debt` → `gyeongju_star` |
| `busan` | 서울 → 부산 | `busan_gyeongbu` | 경부고속도로 (수원·대전) | `suwon_hwaseong` → `daejeon_hanbat` → `busan_capital` |
| `busan` | 서울 → 부산 | `busan_jungbu` | 중부내륙고속도로 (충주·문경) | `chungju_tangeumdae` → `mungyeong_saejae` → `busan_capital` |
| `andong` | 서울 → 안동 | `andong_jungang` | 중앙고속도로 (단양·영주) | `danyang_dodam` → `yeongju_buseoksa` → `andong_village` |
| `seoul` | 대전 → 서울 | `seoul_gyeongbu` | 경부고속도로 (천안·수원) | `cheonan_independence` → `suwon_hwaseong` → `seoul_palace` |

| 장소 id | 좌표(위도, 경도) | 반경 | 자리 | 장소 임무 | 보물 |
|---|---|---|---|---|---|
| `cheonan` | 36.82641, 127.16881 | 3km | 천안IC, 경부 | `cheonan_independence` | 독립 태극기 조각 |
| `suwon` | 37.26441, 127.10384 | 3km | 수원신갈IC, 경부(용인시 기흥구) | `suwon_hwaseong` | 화성 벽돌 |
| `daejeon` | 36.36544, 127.44937 | 3km | 대전IC, 경부 | `daejeon_hanbat` | 한밭 씨앗 |
| `daegu` | 35.90882, 128.57985 | 3km | 북대구IC, 경부(중앙과 겹치는 구간) | `daegu_debt` | 나라 사랑 동전 |
| `chungju` | 36.98098, 127.84602 | 3km | 충주IC, 중부내륙 | `chungju_tangeumdae` | 가야금 줄 |
| `mungyeong` | 36.70141, 128.11131 | 3km | 문경새재IC, 중부내륙 | `mungyeong_saejae` | 새재 짚신 |
| `danyang` | 36.91789, 128.36526 | 3km | 단양IC, 중앙 | `danyang_dodam` | 삼봉 조약돌 |
| `yeongju` | 36.76771, 128.57196 | 3km | 영주IC, 중앙 | `yeongju_buseoksa` | 부석사 기와 조각 |
| `cheomseongdae` | 35.83472, 129.21917 | 1km | 첨성대 | `gyeongju_star` | 첨성대 보물 조각 |
| `temporary_capital` | 35.1034, 129.0174 | 1km | 임시수도기념관 | `busan_capital` | 임시수도 보물 조각 |
| `hahoe` | 36.53806, 128.52056 | 1km | 하회마을 | `andong_village` | 하회마을 보물 조각 |
| `gyeongbokgung` | 37.57861, 126.97722 | 1km | 경복궁 | `seoul_palace` | 경복궁 보물 조각 |

중간 도시 좌표는 OpenStreetMap에서 그 나들목의 양쪽 방향 분기 노드 두 개를 평균한 값이다(위키데이터나 위키백과 좌표와 가장 많이 다른 곳이 북대구IC 470m, 2026-11-09). OpenStreetMap 자료라 앱이나 기획서에 「위치 데이터 © OpenStreetMap contributors, ODbL」 표시가 필요하다. 앱은 이 좌표를 파일에서만 읽고, 실행 중에 지도 서버에 묻지 않는다.

- 순서 : `take_off` → 출발 문장(`departure`, 경로의 `departureLine`이 비면 목적지 것) → 공통 임무 `rain_map`(와이퍼), `crossroads`(내비 갈림길) → 경로의 장소 임무 → P에서 결말(`ending`) → 전달 문장(`delivery`) → 실제 사실(`landing_fact`) → `landed`.
- 중간 도시 장소는 고속도로 위 나들목에 반경 3km로 둔다. 천안과 수원 임무는 두 경로가 같이 쓴다.
- 장소 임무는 시간이 지나도 열지 않는다. 시간으로 열면 서울에서 「경주에 들어왔어요」라고 할 수 있어서다. 위치를 못 읽는 차(`SignalUnavailable(LOCATION)`)에서만 600초 뒤에 연다.
- 다른 길로 가서 기다리던 장소를 못 지나고 뒤 장소에 먼저 오면, 기다리던 장소 임무는 보물 없이 건너뛰고 들어온 장소 임무를 연다.
- 남은 시간이 5분 이하가 되면 도착 직전 문장(`approach`)을 한 번 말하고, 남은 중간 장소를 건너뛰어 마지막 장소 임무를 연다. 말하는 중이면 끊지 않고 신호를 기다리기 시작할 때나 지금 임무가 끝난 뒤에 말한다. 보물을 다 찾은 뒤에는 말하지 않는다. 에뮬레이터 내비는 남은 시간이 안 바뀌어서(첫 시험) 재생 버튼으로 넣는다.
- 이동 중에 도착(`DestinationArrived`)하면 남은 중간 장소를 건너뛰고 마지막 장소 임무를 연다. 도착지 원을 비껴가도 마지막 보물은 받게 하려는 것이다.
- 장소 임무의 브리핑은 「가까워지면 알려 줄게요」로 미리 알리고, 장소에 들어온 사건 대사에서 「들어왔어요」라고 말한다. 브리핑은 앞 임무가 끝나자마자 나가기 때문이다.
- 장소는 차례가 오기 전에 지나가도 기억했다가 그 임무 차례에 브리핑 뒤 바로 연다. 같은 장소는 한 번만 센다. 다른 경로의 장소나 없는 장소는 무시한다.
- 도착(`DestinationArrived`)했거나 임무를 다 끝내고 착륙하면 전달 문장과 실제 사실을 말한다. 휴게소에서 「여기서 착륙」을 누르면 도착 전이라 말하지 않는다.
- 실제 사실은 목적지와 임무의 `factSource`에 출처와 확인한 날을 적었다. 문장을 고치면 출처를 다시 확인한다. 대전의 옛 이름 한밭을 큰 밭으로 푸는 것과 문경새재를 선비들이 과거 보러 넘었다는 설명은 백과사전에서 못 찾아 넣지 않았다.
