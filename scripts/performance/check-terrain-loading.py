#!/usr/bin/env python3
"""Bounded native loading regression. Requires cached Minecraft and installed headless tools; installs nothing."""
import argparse
import datetime as dt
import json
import hashlib
import os
from pathlib import Path
import signal
import statistics
import subprocess as sp
import threading
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', default='build/loading-automated')
    parser.add_argument('--desktop', action='store_true', help='Use an existing graphical display instead of Xvfb')
    parser.add_argument('--p95-budget-ms', type=float, default=100.0,
                        help='Stationary and movement p95 alarm for this documented fixture; not a universal hardware guarantee')
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    out = (repo / args.output).resolve()
    if out.exists():
        parser.error('Use a fresh output directory; existing fixture worlds are never deleted')
    out.mkdir(parents=True)
    server_dir, client_dir = out / 'server', out / 'client'
    server_dir.mkdir(); client_dir.mkdir()
    origin = [1027911.5, 73.0, 1792061.5]
    (server_dir / 'origin.txt').write_text(' '.join(map(str, origin)))
    props = [f'-PterrainServerDirectory={server_dir}', f'-PterrainClientDirectory={client_dir}',
             '-PterrainSeed=8454296310461746384', '-PterrainViewDistance=32',
             '-PterrainSimulationDistance=12', '-PterrainInitialHeap=512m',
             '-PterrainServerHeap=4G', '-PterrainClientHeap=4G', '-PterrainStationarySeconds=60',
             '-PterrainDistance=788', '-PterrainNoTeleport=true', '-PsectionCopiesSmoke']
    gradle = ['./gradlew', '--offline', '--no-configuration-cache']
    inputs=sorted([p for d in ['src/main','src/client','gradle'] for p in (repo/d).rglob('*') if p.is_file()]+[repo/p for p in ['build.gradle.kts','gradle.properties','settings.gradle.kts']])
    fingerprint=hashlib.sha256()
    for p in inputs:
        fingerprint.update(str(p.relative_to(repo)).encode()+b'\0'+hashlib.sha256(p.read_bytes()).digest())
    qa_fingerprint = hashlib.sha256()
    for p in sorted([p for d in ['src/integrationTest','src/integrationTestClient'] for p in (repo/d).rglob('*') if p.is_file()] + [Path(__file__).resolve()]):
        qa_fingerprint.update(str(p.relative_to(repo)).encode()+b'\0'+hashlib.sha256(p.read_bytes()).digest())
    result = {'status': 'FAIL', 'qaInputSha256': qa_fingerprint.hexdigest(), 'runtimeInputSha256': fingerprint.hexdigest(), 'sourceHead': sp.check_output(['git', 'rev-parse', 'HEAD'], cwd=repo, text=True).strip(),
              'renderViewDistance': 32, 'simulationDistance': 12, 'seed': 8454296310461746384,
              'p95BudgetMs': args.p95_budget_ms, 'hardwareScope': 'paired native JVMs, not a laptop FPS prediction'}
    processes = []
    stop = threading.Event()

    def resource_samples():
        with (out / 'resources.jsonl').open('w') as stream:
            while not stop.is_set():
                row = {'utcEpochMillis': time.time_ns() // 1_000_000}
                for key, path in [('cgroupCpu', '/sys/fs/cgroup/cpu.stat'), ('cgroupMemoryBytes', '/sys/fs/cgroup/memory.current')]:
                    try:
                        text = Path(path).read_text().strip()
                        row[key] = dict(line.split() for line in text.splitlines()) if key == 'cgroupCpu' else int(text)
                    except (OSError, ValueError):
                        pass
                for name, directory in [('server', server_dir), ('client', client_dir)]:
                    try:
                        pid = int((directory / 'process-id.txt').read_text())
                        status = Path(f'/proc/{pid}/status').read_text()
                        row[name + 'RssKiB'] = int(next(line.split()[1] for line in status.splitlines() if line.startswith('VmRSS:')))
                    except (OSError, ValueError, StopIteration):
                        pass
                stream.write(json.dumps(row) + '\n'); stream.flush()
                stop.wait(.1)

    monitor = threading.Thread(target=resource_samples, daemon=True)
    monitor.start()
    try:
        # Real block-light propagation and storage edits are checked before the measured workload.
        with (out / 'correctness.log').open('w') as log:
            sp.run(gradle + [f'-PloadingLightsDirectory={out / "lights"}',
                            f'-PcolumnLifecycleDirectory={out / "lifecycle"}',
                            'runLoadingLights', 'runColumnLifecycle'], cwd=repo, stdout=log,
                   stderr=sp.STDOUT, timeout=240, check=True)
        for folder in ['lights', 'lifecycle']:
            assert (out / folder / 'result.txt').read_text().startswith('PASS'), folder
        with (out / 'server-console.log').open('w') as server_log, (out / 'client-console.log').open('w') as client_log:
            server = sp.Popen(gradle + props + ['runTerrainAuditServer'], cwd=repo, stdout=server_log,
                              stderr=sp.STDOUT, start_new_session=True)
            processes.append(server)
            deadline = time.monotonic() + 240
            while 'Done (' not in (out / 'server-console.log').read_text():
                assert server.poll() is None, 'server exited before readiness'
                assert time.monotonic() < deadline, 'server readiness deadline'
                stop.wait(.5)
            client_cmd = ([] if args.desktop else ['scripts/performance/run-headless-client.sh']) + gradle + [
                '--no-daemon', '-x', 'downloadAssets'] + props + ['runTerrainAuditClient']
            client = sp.Popen(client_cmd, cwd=repo, stdout=client_log, stderr=sp.STDOUT, start_new_session=True)
            processes.append(client)
            assert client.wait(timeout=300) == 0, 'client exit'
            assert server.wait(timeout=90) == 0, 'server exit'
        for p in [server_dir / 'result.txt', client_dir / 'result.txt', client_dir / 'copies-result.txt']:
            assert p.read_text().startswith('PASS'), str(p)
        rows = json.loads((server_dir / 'ticks.json').read_text())
        assert {row['phase'] for row in rows} == {'local', 'traversal', 'settle'}, 'missing or unexpected workload phase'
        assert max(row['elapsedMs'] for row in rows if row['phase'] == 'local') >= 60_000, 'stationary duration'
        assert rows[-1]['x'] - origin[0] >= 788, 'eastward native flight distance'
        assert abs(rows[-1]['z'] - origin[2]) < 1, 'route drift'
        result['phases'] = {}
        for phase in ['local', 'traversal', 'settle']:
            group = [row for row in rows if row['phase'] == phase]
            values = sorted(row['tickMs'] for row in group)
            result['phases'][phase] = {'ticks': len(group), 'meanTickMs': statistics.mean(values),
                                      'p95TickMs': values[int(.95 * (len(values) - 1))],
                                      'maxTickMs': max(values), 'loaded': group[-1]['loaded']}
        cache = json.loads((client_dir / 'client-metrics.json').read_text())
        assert cache, 'missing client/cache measurements'
        assert max(row['entries'] for row in cache) <= 8192, 'entry bound'
        assert max(row['weightBytes'] for row in cache) <= 64 * 1024 * 1024, 'weight bound'
        last = cache[-1]
        assert last['hits'] > last['misses'] > 0, 'real extraction must reuse snapshots, not regenerate every request'
        result['cache'] = {'hits': last['hits'], 'misses': last['misses'],
                           'observedPeakEntries': max(row['entries'] for row in cache),
                           'observedPeakWeightBytes': max(row['weightBytes'] for row in cache)}
        for side, directory in [('server', server_dir), ('client', client_dir)]:
            assert (directory / f'{side}.jfr').stat().st_size > 0, 'missing JFR'
        for phase in ['local', 'traversal']:
            assert result['phases'][phase]['p95TickMs'] <= args.p95_budget_ms, f'{phase} p95 performance budget exceeded'
        result['status'] = 'PASS'
    except Exception as error:
        result['error'] = str(error)
    finally:
        for proc in reversed(processes):
            if proc.poll() is None:
                os.killpg(proc.pid, signal.SIGTERM)
                try:
                    proc.wait(timeout=10)
                except sp.TimeoutExpired:
                    os.killpg(proc.pid, signal.SIGKILL)
        # A Gradle launcher can exit before its game JVM. Only signal fixture JVMs
        # whose exact audit-result argument still matches this invocation.
        for directory, prop in [(server_dir, 'terrainAuditResult'), (client_dir, 'terrainAuditClientResult')]:
            try:
                pid = int((directory / 'process-id.txt').read_text())
                marker = ('-Dblockbranch.' + prop + '=' + str(directory / 'result.txt')).encode()
                def owns_pid():
                    try:
                        return marker in Path(f'/proc/{pid}/cmdline').read_bytes().split(b'\0')
                    except OSError:
                        return False
                if owns_pid():
                    os.kill(pid, signal.SIGTERM)
                    until = time.monotonic() + 5
                    while owns_pid() and time.monotonic() < until:
                        time.sleep(.1)
                    if owns_pid():
                        os.kill(pid, signal.SIGKILL)
            except (OSError, ValueError):
                pass
        stop.set(); monitor.join(timeout=2)
        result['completedUtc'] = dt.datetime.now(dt.UTC).isoformat()
        (out / 'summary.json').write_text(json.dumps(result, indent=2) + '\n')
        print(json.dumps(result, indent=2))
    return 0 if result['status'] == 'PASS' else 1


if __name__ == '__main__':
    raise SystemExit(main())
