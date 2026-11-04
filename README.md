# KidQuest(가칭)

## 앱 이름 및 한 줄 소개

KidQuest는 뒷좌석 아이가 우리 차의 선장이 되어, 실제 차 신호와 지나는 곳으로 벌어지는 우주 임무를 듣고 말로 푸는 Pleos Connect 앱이다.

지금은 개발 초기다. 상태 흐름과 재생 모드 화면만 있고, 차 신호, 음성, 임무 진행은 아직 연결하지 않았다.

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

## 더 보기

- 폴더와 역할 : [docs/structure.md](docs/structure.md)
- 작업 규칙 : [docs/rules.md](docs/rules.md)
