# 브루미 · Vroomie

## 앱 이름 및 한 줄 소개

브루미는 가족의 이동을 역사 보물찾기 모험으로 바꾸는 차량용 앱이다. 부모가 경로와 테마를 고르면 아이는 벨트를 매고 이야기를 듣는다. 지역별 역사 퀴즈와 별자리 연결은 개발 예정이며, 현재는 창작 보물 단서로 음성 흐름을 시험한다.

지금은 개발 중이다. 시작 설정과 차량 상태 화면, 임무 엔진, Pleos ON_DEVICE TTS와 임시 효과음을 연결했다. 에뮬레이터에서 차량 최초 상태 조회와 재생 신호, 음성 완료 뒤 임무 진행을 시험했다. 한국어 ON_DEVICE STT와 번호 퀴즈도 연결했고 인식 서비스 준비·취소와 번호 해석을 시험했다. 실제 발화 정확도, 오디오 포커스, 최종 음원과 대체 대사 녹음 검증은 남아 있다.

## 실행 방법

1. Android Studio에서 이 폴더를 연다.
2. Pleos Connect 에뮬레이터를 켠다.
3. Windows는 `gradlew.bat installDebug`, 그 밖에는 `./gradlew installDebug`로 설치한다.

앱의 상태 흐름(`core`)은 에뮬레이터 없이 시험할 수 있다.

```
gradlew.bat :core:test
```

## 테스트 환경 및 필요한 설정

- JDK 17 또는 21 (이 브랜치는 JDK 21로 확인)
- Android SDK Platform 35
- Pleos Connect v2.1.0 에뮬레이터(API 34)
- 차 신호는 에뮬레이터의 Virtual Console이나 앱의 재생 모드로 넣는다.

## APK 파일 위치 또는 GitHub Release 링크

`apk/` 폴더에 `팀명_앱이름_release.apk`로 둔다. 아직 올리지 않았다.

## 더 보기

- 폴더와 역할 : [docs/structure.md](docs/structure.md)
- 작업 규칙 : [docs/rules.md](docs/rules.md)

- 음성 출력과 녹음 교체 : [docs/voice_output.md](docs/voice_output.md)

- 브루미 경로·테마 화면 : [docs/vroomie_ui.md](docs/vroomie_ui.md)
