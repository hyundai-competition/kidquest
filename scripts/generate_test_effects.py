# 외부 음원을 복사하지 않고 개발 시험용의 짧고 낮은 음량 효과음을 합성한다.
import math
import struct
import wave
from pathlib import Path

output = Path(__file__).resolve().parents[1] / 'app/src/main/assets/audio/effects'
output.mkdir(parents=True, exist_ok=True)
for name, seconds, base in [
    ('meteor', 1.4, 660), ('thruster_left', .35, 220),
    ('thruster_right', .35, 270), ('wormhole', 1, 330),
]:
    samples = []
    rate = 22050
    for index in range(int(rate * seconds)):
        time = index / rate
        phase = time / seconds
        envelope = min(time / .04, 1) * min((seconds - time) / .12, 1)
        frequency = base * (1 - .5 * phase)
        sample = .12 * envelope * (
            math.sin(2 * math.pi * frequency * time)
            + .15 * math.sin(2 * math.pi * frequency * 1.5 * time)
        )
        samples.append(struct.pack('<h', round(sample * 32767)))
    with wave.open(str(output / (name + '.wav')), 'wb') as audio:
        audio.setnchannels(1)
        audio.setsampwidth(2)
        audio.setframerate(rate)
        audio.writeframes(b''.join(samples))
