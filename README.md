# 브루미 (Vroomie)

## 앱 이름 및 한 줄 소개

브루미는 벨트로 시작하는 지역 역사 보물 탐험이다. 앞쪽 디스플레이에서 출발 전 설정을 하고, 주행 중에는 음성 중심으로 진행하며 퍼즐은 주차·휴식 때 즐긴다.

목적지 4곳·경로 5개·음성 임무 14개와 정차 퍼즐 16개가 들어 있다. 말하기와 답 듣기는 Pleos ON_DEVICE TTS/STT를 쓴다. 정차 퍼즐은 이동 이야기와 보상을 분리했다. 실제 발화 정확도, 내비 오디오 포커스, 최종 음원은 추가 확인이 필요하다. 통합 범위와 시연 방법은 [main 통합 기록](docs/main_integration.md)을 참고한다.

## 실행 방법

1. Android Studio에서 이 폴더를 연다.
2. Pleos Connect 에뮬레이터를 켠다.
3. Windows는 `gradlew.bat installDebug`, 그 밖에는 `./gradlew installDebug`로 설치한다.
   - 제출용 APK로 설치할 때 : `adb install apk/Roadiverse_Vroomie_release.apk`

앱의 상태 흐름(`core`)은 에뮬레이터 없이 시험할 수 있다.

```
gradlew.bat :core:test
```

## 테스트 환경 및 필요한 설정

- JDK 17 이상
- Android SDK Platform 35
- Pleos Connect v2.1.0 에뮬레이터(API 34)
- 실제 탐험은 차량 SDK·에뮬레이터 Virtual Console 신호로 진행한다. 첫 화면의 **시연 체험**은 가상 차량·위치 신호로 한 판을 자동 진행한다. 조작과 입력원 구분은 [자동 시연 안내](docs/showcase_mode.md)를 따른다.

## APK 파일 위치 또는 GitHub Release 링크

[`apk/Roadiverse_Vroomie_release.apk`](apk/Roadiverse_Vroomie_release.apk) (release 서명, 버전 0.1.0)

- 만드는 법 : `gradlew.bat :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk`를 위 이름으로 복사한다.
- 서명 키와 비밀번호는 저장소에 없다. 앱 통합 역할의 컴퓨터에만 있고, 값이 없는 컴퓨터에서는 release가 서명 없이 만들어진다.
- 디버그 앱과 서명이 달라서, 디버그 앱을 지운 뒤 설치한다.

## 출처

- 역사 내용 : 한국민족문화대백과사전(https://encykorea.aks.ac.kr), 유네스코 세계유산센터(https://whc.unesco.org). 항목별 출처는 에피소드 파일의 `factSource`에 있다.
- 위치 데이터 : © OpenStreetMap contributors, ODbL (https://www.openstreetmap.org/copyright). 에피소드 파일 `places`의 고속도로 나들목 좌표를 OpenStreetMap에서 가져왔다.
- 보물 사진(`app/src/main/assets/treasures/`, 도착 화면) : 위키미디어 공용 사진을 잘라서 줄였다
  - 첨성대 : Matt and Nayoung Wilson from Atlanta and surrounding, US, CC BY 2.0, https://commons.wikimedia.org/wiki/File:Korea-Gyeongju-Cheomseongdae-02.jpg
  - 임시수도기념관(옛 대통령관저) : Steve46814, CC BY-SA 4.0, https://commons.wikimedia.org/wiki/File:Provisional_Capital_Memorial_Hall_11-10469.jpg
  - 하회탈(양반탈) : Bernard Gagnon, CC0, https://commons.wikimedia.org/wiki/File:Hahoe_World_Mask_Museum_05.jpg
  - 광화문 해치 : hyolee2, CC BY-SA 3.0, https://commons.wikimedia.org/wiki/File:Gwanghwamun_Haechi2.JPG
- 브루미 캐릭터 그림 4장(`app/src/main/res/drawable-nodpi/vroomie_*.png`) : 생성형 AI 이미지 도구로 만들었다. 만든 방법은 [docs/vroomie_companion.md](docs/vroomie_companion.md)에 있다.
- 놀이터 퍼즐 문장 : AI가 쓴 초안을 팀이 출처와 대조해 고쳤다. 대조 결과는 [docs/puzzle_sources.md](docs/puzzle_sources.md)에 있다.

## 더 보기

- 폴더와 역할 : [docs/structure.md](docs/structure.md)
- 작업 규칙 : [docs/rules.md](docs/rules.md)
