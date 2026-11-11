# 실제 가상 차량 콘솔·ADB GPS와 SDK/TTS로 고정 경로를 검증한다. AppEvent 재생은 사용하지 않는다.
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import threading
import time
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description='SDK 기반 서울→경주 UI 통합 시험. 앱/테스트 APK를 먼저 설치한다.')
parser.add_argument('--adb', default=shutil.which('adb') or str(Path.home() / 'Library/Android/sdk/platform-tools/adb'))
parser.add_argument('--device', default='emulator-5554')
parser.add_argument('--user', default='10')
parser.add_argument('--console', default='http://127.0.0.1:8088')
parser.add_argument('--output', default='/private/tmp/kq-sdk-trip')
args = parser.parse_args()
output = Path(args.output)
output.mkdir(parents=True, exist_ok=True)


def adb(*parts, check=True):
    result = subprocess.run([args.adb, '-s', args.device, *parts], text=True, capture_output=True, timeout=15)
    if check and (result.returncode or 'KO:' in result.stdout):
        raise RuntimeError('ADB request failed: ' + result.stderr.strip() + result.stdout.strip())
    return result.stdout


def request(path, method='GET', **params):
    url = args.console + path + ('?' + urllib.parse.urlencode(params) if params else '')
    with urllib.request.urlopen(urllib.request.Request(url, method=method), timeout=5) as response:
        text = response.read().decode()
        return json.loads(text) if text else {}


def gps(latitude, longitude):
    # geo fix 순서: 경도, 위도, 고도, 위성 수, 속도(knots). ADB는 KO 응답에도 0으로 끝날 수 있다.
    adb('emu', 'geo', 'fix', str(longitude), str(latitude), '20', '8', '0')


def heartbeat():
    while not stop.wait(1):
        try:
            request('/browser/session', 'POST', client='kq-sdk-trip-audit')
        except Exception:
            pass


stop = threading.Event()
thread = threading.Thread(target=heartbeat, daemon=True)
adb('forward', 'tcp:8088', 'tcp:8088')
initial = request('/vehicle/state')
initial_mode = initial['hvac']['dds_mode']
initial_wiper = initial['body']['wipers']['front']['mode']
process = None
log = None
voice_process = None
voice_log = None
try:
    adb('forward', 'tcp:8088', 'tcp:8088')
    request('/browser/session', 'POST', client='kq-sdk-trip-audit')
    thread.start()
    request('/vehicle/hvac/mode', 'PUT', mode='subscribe_hvac_base')
    request('/vehicle/controls', 'PUT', speed=0, accelerator=0, brake=0)
    request('/vehicle/gear', 'PUT', gear='Park')
    request('/vehicle/body', 'PUT', front_wiper='Off')
    gps(37.57861, 126.97722)
    adb('shell', 'run-as', 'com.kidquest.app', '--user', args.user, 'rm', '-f', 'cache/sdk-trip-state.json')
    log = (output / 'instrumentation.log').open('w')
    voice_log = (output / 'voice.log').open('w')
    voice_process = subprocess.Popen([args.adb, '-s', args.device, 'logcat', '-T', '1', '-v', 'threadtime', 'KQCar:I', 'KidQuestVoice:I', 'KQFlow:I', '*:S'], stdout=voice_log, stderr=subprocess.STDOUT)
    process = subprocess.Popen([
        args.adb, '-s', args.device, 'shell', 'am', 'instrument', '--user', args.user, '-w',
        '-e', 'verifySdkTrip', 'true', '-e', 'class', 'com.kidquest.app.SdkRouteTripTest',
        'com.kidquest.app.test/androidx.test.runner.AndroidJUnitRunner',
    ], stdout=log, stderr=subprocess.STDOUT)
    episode = json.loads((ROOT / 'app/src/main/assets/episodes/episode_demo.json').read_text())
    places = {place['id']: place for place in episode['places']}
    targets = {'cheonan_independence': 'cheonan', 'daegu_debt': 'daegu', 'gyeongju_star': 'cheomseongdae'}
    visited = set()
    driving = parked = finished = False
    previous = None
    end = time.monotonic() + 510
    while time.monotonic() < end and process.poll() is None:
        text = adb('exec-out', 'run-as', 'com.kidquest.app', '--user', args.user,
                   'cat', 'cache/sdk-trip-state.json', check=False)
        try:
            state = json.loads(text)
        except (ValueError, TypeError):
            time.sleep(1)
            continue
        signature = (state['screen'], state['mission'], state['stage'], state['items'])
        if signature != previous:
            print('progress', signature, flush=True)
            (output / 'last-state.json').write_text(json.dumps(state, ensure_ascii=False, indent=2))
            previous = signature
        if not driving and state['screen'] == 'CREW_CHECK':
            request('/vehicle/gear', 'PUT', gear='Drive')
            request('/vehicle/body', 'PUT', front_wiper='Low')
            driving = True
        place_id = targets.get(state['mission'])
        if place_id and place_id not in visited:
            place = places[place_id]
            gps(place['latitude'], place['longitude'])
            visited.add(place_id)
            print('GPS entered:', place_id, flush=True)
        if state['items'] == 5 and not parked:
            request('/vehicle/controls', 'PUT', speed=0, accelerator=0, brake=0)
            request('/vehicle/gear', 'PUT', gear='Park')
            parked = True
        if state.get('finished'):
            finished = True
            (output / 'last-state.json').write_text(json.dumps(state, ensure_ascii=False, indent=2))
        time.sleep(1)
    process.wait(timeout=20)
    log.flush()
    result = (output / 'instrumentation.log').read_text()
    try:
        final = json.loads(adb('exec-out', 'run-as', 'com.kidquest.app', '--user', args.user, 'cat', 'cache/sdk-trip-state.json'))
        finished = final.get('finished', False)
        (output / 'last-state.json').write_text(json.dumps(final, ensure_ascii=False, indent=2))
    except (ValueError, RuntimeError):
        pass
    voice_process.terminate()
    voice_process.wait(timeout=5)
    voice_log.flush()
    evidence = (output / 'voice.log').read_text()
    real_places = all('PlaceEntered(placeId=' + place + ')' in evidence for place in targets.values())
    playback_ok = '재생 오류:' not in evidence and '녹음 대체 재생:' not in evidence
    if not finished or not real_places or not playback_ok or 'OK (1 test)' not in result or 'FAILURES!!!' in result:
        raise RuntimeError('SDK 통합 시험 미완료. instrumentation.log와 last-state.json을 확인한다.')
    print('PASS: 실제 SDK 경유지 3곳, 보물 5개, P 종료, 마지막 ON_DEVICE 대사 완료', flush=True)
finally:
    if process is not None and process.poll() is None:
        adb('shell', 'am', 'force-stop', '--user', args.user, 'com.kidquest.app', check=False)
        process.terminate()
    if log:
        log.close()
    if voice_process is not None and voice_process.poll() is None:
        voice_process.terminate()
        voice_process.wait(timeout=5)
    if voice_log:
        voice_log.close()
    try:
        request('/vehicle/controls', 'PUT', speed=0, accelerator=0, brake=0)
        request('/vehicle/gear', 'PUT', gear='Park')
        request('/vehicle/body', 'PUT', front_wiper=initial_wiper)
        request('/vehicle/hvac/mode', 'PUT', mode=initial_mode)
        request('/browser/session', 'POST', client='kq-sdk-trip-audit', closing=1)
    finally:
        stop.set()
        if thread.is_alive():
            thread.join(timeout=2)
