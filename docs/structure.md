# 폴더와 역할

## 폴더

| 폴더 | 하는 일 | 맡는 역할 | 별칭 |
|---|---|---|---|
| `core/.../contract/` | 사건, 명령, 에피소드 형식. 팀 전체의 약속 | 판단 로직·데이터 | `kq-logic` |
| `core/.../engine/` | 상태와 임무 판단. 안드로이드와 차 SDK를 쓰지 않음 | 판단 로직·데이터 | `kq-logic` |
| `app/.../assets/episodes/` | 에피소드와 위치 좌표 파일 | 판단 로직·데이터 | `kq-logic` |
| `app/.../car/` | 차 신호와 위치를 사건으로 바꿈 | 차량·내비 연결 | `kq-car` |
| `app/.../voice/` | 말하기, 효과음, 답 듣기, 소리 양보 | 화면·음성 | `kq-voice` |
| `app/.../screen/` | 정차 화면과 주행 중 화면 | 화면·음성 | `kq-voice` |
| `app/.../storage/` | 연령과 배지 저장 | 앱 통합 | `kq-integration` |
| `app/.../AppController.kt`, 빌드 파일, `apk/` | 앱 연결, 빌드, 권한, APK | 앱 통합 | `kq-integration` |
| `app/.../replay/` | 가짜 신호를 넣는 재생 모드 | 시연·품질 | `kq-demo` |

## 약속 3개

코드가 맞물리도록 이 세 파일을 기준으로 삼는다.

1. **사건 목록 :** `core/.../contract/AppEvent.kt`. 차 신호와 버튼이 앱 안에서 무엇이 되는지 적는다.
2. **명령 목록 :** `core/.../contract/Command.kt`. 임무 엔진이 음성과 화면에 무엇을 시키는지 적는다.
3. **에피소드 파일 형식 :** `core/.../contract/Episode.kt`와 `app/.../assets/episodes/`의 예시 파일.

```
차 신호, 재생 모드 → AppEvent → MissionEngine → Command → 음성, 화면
```

## 먼저 할 일

- 상태 흐름 시험 : `gradlew.bat :core:test`. 에뮬레이터가 필요 없다. 임무 진행은 아직 없다.
- 앱 빌드 : `gradlew.bat :app:assembleDebug`
- 저장소 검사 : `sh scripts/check_repo.sh --all`
