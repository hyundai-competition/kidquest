# 효과음 라이선스 정리 (2026-11-05, 2026-11-09, 2026-11-12 확인)

## 제출 판단 (2026-11-12, 팀 확인 전 제안)

- 지금 앱에 들어 있는 소리는 모두 팀이 스크립트로 직접 합성했다. 밖에서 받은 음원은 없다.

| 파일 | 만든 방법 | 쓰는 곳 |
|---|---|---|
| `audio/effects/meteor.wav`, `thruster_left.wav`, `thruster_right.wav`, `wormhole.wav` | `scripts/generate_test_effects.py` | `wipe_drops`, `turn_tick_*`, `time_tunnel` (`EffectPlayer`가 연결) |
| `audio/instruments/metal_preview.wav`, `stone_preview.wav` | `scripts/generate_instrument_previews.py` | 장소 체험 악기 |
| `audio/music/vroomie_trail.wav` | [음원 설명](audio/vroomie_trail.md) | 빈 시간 BGM |

- 그래서 제3자 라이선스나 출처 표기가 필요한 소리는 없다. 접수 양식에는 「효과음과 배경음은 팀이 직접 합성했다」고 적을 수 있다.
- `turn_tick_*`는 `VoiceOutput`에서 재생하지 않는다. 깜빡이 소리는 나지 않는다.
- 아래 CC0 후보로 바꾸는 일은 제출(11-15) 뒤로 미루기를 제안한다. 내려받기에 계정이 필요하고, 바꾸면 소리를 다시 들어 확인하고 시연 영상도 다시 찍어야 한다.

법률 자문이 아니라 공개 페이지를 읽고 정리한 것이다. 소리는 아직 듣지 않았다. 내려받기 전에 사람이 파일 페이지를 다시 확인한다.

## 예시 파일의 소리 id (2026-11-09 보물 탐험에 맞춰 이름 바꿈)

메인 스토리가 지역 역사 보물찾기로 바뀌어 우주 이름 id를 바꿨다. 앱은 새 id에 직접 합성한 임시 효과음을 연결해 재생한다(아래 「제출 판단」).

| 소리 id | 전 id | 쓰는 곳 | 소리 | 후보 |
|---|---|---|---|---|
| `wipe_drops` | stardust | `rain_map` 사건 | 물방울을 쓱싹 닦는 소리 | 아래 와이퍼, 유리 닦기 후보를 들어 보고 고른다 |
| `turn_tick_left`, `turn_tick_right` | thruster_left, thruster_right | 깜빡이 반응 | 아주 짧은 똑딱 소리. 실제 깜빡이 소리와 겹친다 | 나무 「똑」을 먼저 들어 본다. 실제 깜빡이 녹음은 차 소리와 헷갈릴 수 있다 |
| `time_tunnel` | wormhole | 터널 반응 | 슝 하고 지나가는 소리 | 아래 Air portal, Wormhole .wav를 들어 보고 고른다 |

## 찾아 둔 후보

아래는 Freesound 페이지에서 라이선스를 Creative Commons 0으로 확인했다. 출처 표기 의무는 없다. 페이지의 "Get attribution text" 내용은 읽지 못했다.

| 후보 | 쓸 수 있는 곳 | 길이 | 주의 |
|---|---|---|---|
| [Wiping Window](https://freesound.org/people/WolffVisuals/sounds/399587/) (WolffVisuals) | `wipe_drops` | 8초 | 앞유리를 닦는 소리. 잘라 쓴다 |
| [Window Wipe](https://freesound.org/people/greenlinker/sounds/757518/) (greenlinker) | `wipe_drops` 예비 | 9초 | 창문을 눌러 닦는 소리 |
| [Squeegee Window.WAV](https://freesound.org/people/13GPanska_David_Koci/sounds/377854/) (13GPanska_David_Koci) | `wipe_drops` 예비 | 14초 | 설명 없음. 들어 보고 확인한다 |
| [wipers spray water squeaky](https://freesound.org/people/RutgerMuller/sounds/50905/) (RutgerMuller) | `wipe_drops` | 10초 | 물을 뿌리며 와이퍼가 닦는 소리. 한 번 쓱 지나가는 부분만 자른다 |
| [wipers loop 2 slow squeaky](https://freesound.org/people/RutgerMuller/sounds/50903/) (RutgerMuller) | `wipe_drops` 예비 | 8초 | 느린 와이퍼 소리 |
| [Water Drop Sound](https://freesound.org/people/metaepitome/sounds/165206/) (metaepitome) | `wipe_drops` 앞에 붙일 물방울 | 0.25초 | 「톡」 물방울 뒤에 닦는 소리를 이어 붙일 수 있다 |
| [Basic Click Wooden](https://freesound.org/people/GameAudio/sounds/220200/) (GameAudio) | `turn_tick_*` | 0.5초 | 나무 「똑」. 실제 깜빡이와 소리가 달라 구분된다 |
| [Wooden Click](https://freesound.org/people/BenjaminNelan/sounds/321083/) (BenjaminNelan) | `turn_tick_*` 예비 | 0.1초 | 아주 짧은 나무 「딱」 |
| [NISSAN SENTRA TURNING SIGNAL](https://freesound.org/people/MPierluissi/sounds/446322/) (MPierluissi) | `turn_tick_*` | 3초 | 실제 차 깜빡이 녹음. 잡음을 지웠다고 적혀 있음 |
| [Car left right indicator tick](https://freesound.org/people/Caitlin_100/sounds/384405/) (Caitlin_100) | `turn_tick_*` 예비 | 8초 | 실제 차 깜빡이 녹음. 한두 번만 잘라 쓴다 |
| [GLEAM-GLOW-SFX-CHIME](https://freesound.org/people/newagesoup/sounds/351408/) (newagesoup) | 보물을 찾았을 때 (지금 파일에는 이 id가 없다) | 5초 | 없음 |
| [Magic Stars Retro Sparkle](https://freesound.org/people/smokinghotdog/sounds/584244/) (smokinghotdog) | 보물을 찾았을 때 예비 | 35초 | 앞부분만 잘라 쓴다 |
| [Heavy Rain on Windshield](https://freesound.org/people/Fabrizio84/sounds/458389/) (Fabrizio84) | 비 배경 | 3분 29초 | 잘라 쓴다. 실제 차 소리와 헷갈리지 않는지 들어 본다 |
| [Air portal](https://freesound.org/people/humanoide9000/sounds/329028/) (humanoide9000) | `time_tunnel` | 28초 | 소리를 섞었다고만 적혀 있어 원본 출처는 모름 |
| [Wormhole .wav](https://freesound.org/people/doug914/sounds/423636/) (doug914) | `time_tunnel` 예비 | 18초 | 멀티 샘플이라고만 적혀 있어 원본 출처는 모름 |

우주 이야기용이라 뺀 후보 : [meteor flyby](https://freesound.org/people/humanoide9000/sounds/329029/), [Large Rocket Engine](https://freesound.org/people/Zovex/sounds/237974/)

## 뺀 후보

- [Magic Wand Glitter](https://freesound.org/people/qubodup/sounds/211624/) : 페이지에 CC0와 CC BY 안내가 함께 있다.
- [Sci-fi Engine Startup](https://freesound.org/people/JapanYoshiTheGamer/sounds/361250/) : 다른 무료 소리로 만들었다는데 원본 출처가 없다.

## 근거

- [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/deed.ko) : 상업 목적을 포함해 복사, 수정, 배포할 수 있다. 보증은 없다. 앱을 직접 언급하지는 않는다.
- [Freesound FAQ](https://freesound.org/help/faq/) : 파일마다 라이선스가 다르다. 내려받으려면 계정이 필요하다. CC BY-NC는 상업 이용이 안 되니 쓰지 않는다.
- 공모전 [참가 유의사항](https://appdevchallenge.auto-hmg.io/notice/c0a1a000-2026-4923-8000-000000000006) : 제3자 권리 침해 시 책임은 응모자에게 있다.
- [Pixabay 약관](https://pixabay.com/service/terms/)은 예비로 둔다. 원본을 저장소에 그대로 올려도 되는지 직접 적혀 있지 않다.
- Mixkit은 조항을 못 읽었고, Zapsplat과 OpenGameArt는 자동 읽기가 막혀 확인하지 못했다.

## 쓰기 전 확인

- [ ] 파일 페이지의 라이선스가 여전히 CC0다
- [ ] 소리를 직접 들어 아이가 놀랄 만큼 크거나 무섭지 않다
- [ ] 내가 만들었다고 적지 않는다

## 2026-11-06 앱 연결 상태

후보 파일은 아직 확보·청취 확인 전이다. 현재 앱은 `scripts/generate_test_effects.py`에서 직접 합성한 임시 시험용 효과음 네 개를 재생한다. 후보 음원을 적용했다고 표시하지 않는다. ID별 파일과 교체 방법은 [voice_output.md](voice_output.md)에 있다.

## 장소 체험 임시 시연음

`audio/instruments/metal_preview.wav`, `stone_preview.wav`는 `scripts/generate_instrument_previews.py`로 직접 합성했다. 외부 음원을 복사하지 않았고 실제 편종·편경 녹음으로 표시하지 않는다. 실제 악기 음원 교체는 후속 작업이다.
