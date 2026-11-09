# KidQuest(가칭)

## 앱 이름 및 한 줄 소개

KidQuest는 벨트가 재생 버튼인 오디오 모험이다. 뒷좌석 벨트를 매야 시작되고, 풀면 멈춘다. 달리는 동안에는 실제 차 신호가 우주 사건이 되고, 아이는 듣고 말로 푼다.

지금은 개발 초기다. 상태 흐름과 재생 모드 화면만 있고, 음성, 임무 진행은 아직 연결하지 않았다. 차 신호 수집 코드는 붙였지만 에뮬레이터에서 아직 시험하지 않았다.

## 실행 방법

1. Android Studio에서 이 폴더를 연다.
2. Pleos Connect 에뮬레이터를 켠다.
3. Windows는 `gradlew.bat installDebug`, 그 밖에는 `./gradlew installDebug`로 설치한다.

앱의 상태 흐름(`core`)은 에뮬레이터 없이 시험할 수 있다.

```
gradlew.bat :core:test
```

## 테스트 환경 및 필요한 설정

- JDK 17 이상
- Android SDK Platform 35
- Pleos Connect v2.1.0 에뮬레이터(API 34)
- 차 신호는 에뮬레이터의 Virtual Console이나 앱의 재생 모드로 넣는다.

## APK 파일 위치 또는 GitHub Release 링크

`apk/` 폴더에 `팀명_앱이름_release.apk`로 둔다. 아직 올리지 않았다.

## 출처

- 역사 내용 : 한국민족문화대백과사전(https://encykorea.aks.ac.kr), 유네스코 세계유산센터(https://whc.unesco.org). 항목별 출처는 에피소드 파일의 `factSource`에 있다.
- 위치 데이터 : © OpenStreetMap contributors, ODbL (https://www.openstreetmap.org/copyright). 에피소드 파일 `places`의 고속도로 나들목 좌표를 OpenStreetMap에서 가져왔다.

## 더 보기

- 폴더와 역할 : [docs/structure.md](docs/structure.md)
- 작업 규칙 : [docs/rules.md](docs/rules.md)
