# 외부 음원·샘플 없이 만든 브루미용 펜타토닉 루프. 72 BPM, 8마디, 3/4박자.
import math
import pathlib
import struct
import wave

RATE = 22050
BEAT = 60 / 72
LENGTH = BEAT * 24
output = [0.0] * int(RATE * LENGTH)

def note(midi, start, duration, volume):
    freq = 440 * 2 ** ((midi - 69) / 12)
    for i in range(int(duration * RATE)):
        t = i / RATE
        attack = min(1, t / .04)
        release = min(1, (duration - t) / .25)
        tone = (math.sin(2 * math.pi * freq * t) + .18 * math.sin(4 * math.pi * freq * t))
        value = tone * math.exp(-t * 1.5) * attack * release * volume
        output[(int(start * RATE) + i) % len(output)] += value

# 정해진 기존 곡을 모방하지 않는 자체 선율과 화음.
melody = [72, 76, 79, 81, 79, 76, 74, 79, 76, 72, 74, 76, 79, 83, 81, 79]
chords = [(48, 55, 60), (45, 52, 57), (53, 60, 65), (55, 62, 67)]
for bar in range(8):
    for j, pitch in enumerate(chords[bar % 4]):
        note(pitch, (bar * 3 + j) * BEAT, BEAT * 2.8, .065)
    note(melody[bar * 2], (bar * 3 + .5) * BEAT, BEAT * 1.4, .1)
    note(melody[bar * 2 + 1], (bar * 3 + 2) * BEAT, BEAT * 1.5, .08)
peak = max(abs(v) for v in output)
path = pathlib.Path('app/src/main/assets/audio/music/vroomie_trail.wav')
path.parent.mkdir(parents=True, exist_ok=True)
with wave.open(str(path), 'wb') as f:
    f.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
    f.writeframes(b''.join(struct.pack('<h', int(v / max(peak, 1) * 22000)) for v in output))
print(path, f'{LENGTH:.1f}s')
