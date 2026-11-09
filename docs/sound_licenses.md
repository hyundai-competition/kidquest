# 효과음 라이선스 정리 (2026-11-05 확인)

법률 자문이 아니라 공개 페이지를 읽고 정리한 것이다. 소리는 아직 듣지 않았다. 내려받기 전에 사람이 파일 페이지를 다시 확인한다.

## 와이퍼 임무 후보

별가루 소리는 1번을 제안한다. 아래 3개는 Freesound 페이지에서 라이선스를 Creative Commons 0으로 확인했다.

| 순서 | 후보 | 쓰는 곳 | 길이 | 앱, 공모전 제출 | 출처 표기 | 주의 |
|---|---|---|---|---|---|---|
| 1 | [GLEAM-GLOW-SFX-CHIME](https://freesound.org/people/newagesoup/sounds/351408/) (newagesoup) | 별가루, 보상 | 5초 | CC0 조건상 가능해 보임 | 의무 아님 | 없음 |
| 2 | [Magic Stars Retro Sparkle](https://freesound.org/people/smokinghotdog/sounds/584244/) (smokinghotdog) | 별가루 | 35초 | CC0 조건상 가능해 보임 | 의무 아님 | 앞부분만 잘라 쓴다 |
| 3 | [Heavy Rain on Windshield](https://freesound.org/people/Fabrizio84/sounds/458389/) (Fabrizio84) | 비 배경 | 3분 29초 | CC0 조건상 가능해 보임 | 의무 아님 | 잘라 쓴다. 실제 차 소리와 헷갈리지 않는지 들어 본다 |

## 나머지 소리 후보

와이퍼 임무 1개에는 필요 없다. 페이지 라이선스는 모두 Creative Commons 0으로 확인했고, 출처 표기 의무는 없다. 페이지의 "Get attribution text" 내용은 읽지 못했다.

| 소리 id | 후보 | 길이 | 주의 |
|---|---|---|---|
| meteor | [meteor flyby](https://freesound.org/people/humanoide9000/sounds/329029/) (humanoide9000) | 18초 | 직접 녹음했다고 적혀 있음. 잘라 쓴다 |
| thruster_left, thruster_right | [Large Rocket Engine](https://freesound.org/people/Zovex/sounds/237974/) (Zovex) | 11초 | 직접 만들었다고 적혀 있음. 잘라 쓰고 깜빡이 소리는 짧게 한다 |
| wormhole | [Air portal](https://freesound.org/people/humanoide9000/sounds/329028/) (humanoide9000) | 28초 | 소리를 섞었다고만 적혀 있어 원본 출처는 모름 |
| wormhole 예비 | [Wormhole .wav](https://freesound.org/people/doug914/sounds/423636/) (doug914) | 18초 | 멀티 샘플이라고만 적혀 있어 원본 출처는 모름 |

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
