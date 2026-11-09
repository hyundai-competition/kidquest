# 직접 합성한 미션 시연음 두 개. 실제 편종·편경 녹음이나 음색 재현이 아니다.
import math
import struct
import wave
from pathlib import Path

output = Path(__file__).resolve().parents[1] / 'app/src/main/assets/audio/instruments'
output.mkdir(parents=True, exist_ok=True)
for name, frequency, overtone in [('metal_preview', 660, 2.7), ('stone_preview', 440, 1.8)]:
    rate = 22050
    samples = []
    for index in range(rate):
        time = index / rate
        envelope = min(time / .008, 1) * math.exp(-5 * time)
        value = .18 * envelope * (math.sin(2 * math.pi * frequency * time) + .25 * math.sin(2 * math.pi * frequency * overtone * time))
        samples.append(struct.pack('<h', round(value * 32767)))
    with wave.open(str(output / (name + '.wav')), 'wb') as audio:
        audio.setparams((1, 2, rate, 0, 'NONE', 'not compressed'))
        audio.writeframes(b''.join(samples))
