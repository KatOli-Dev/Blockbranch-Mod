#!/usr/bin/env python3
"""Local-only prototype check and bounded paired microbenchmark; no native loading claim."""
import hashlib
import json
import os
from pathlib import Path
import statistics
import subprocess

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "build/underground-rich-network-revision"
JAVA = Path(os.environ.get("JAVA_HOME", "/workspace/toolchains/jdk25")) / "bin"
OUT.mkdir(parents=True, exist_ok=True)
sources = sorted(Path(__file__).parent.glob("*.java"))
subprocess.run([str(JAVA / "javac"), "-cp", "build/classes/java/main", "-d", str(OUT), *map(str, sources)], cwd=ROOT, check=True)
command = [str(JAVA / "java"), "-Xmx768m", "-XX:ActiveProcessorCount=2", "-cp", f"{OUT}:build/classes/java/main", "blockbranch.realistic.RichNetworkExperiment"]
with (OUT / "check.log").open("w") as log:
    subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
rows = []
processes = []
for fork, mode in enumerate(["baseline", "rich", "rich", "baseline"]):
    path = OUT / f"paired-{fork}-{mode}.log"
    with path.open("w") as log:
        subprocess.run(command + ["bench", mode], cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
    memory = {}
    for line in path.read_text().splitlines():
        if line.startswith("BENCH "):
            row = dict(part.split("=", 1) for part in line.split()[1:])
            rows.append({"fork": fork, **{k: (v if k == "mode" else int(v)) for k, v in row.items()}})
        elif line.startswith("PROCESS_MEMORY "):
            parts = line.split()
            memory[parts[1].rstrip(":")] = int(parts[2]) * 1024
        elif line.startswith("POST_GC_HEAP_BYTES "):
            memory["post_gc_heap_bytes"] = int(line.split()[1])
    processes.append({"fork": fork, "mode": mode, "memory": memory})
    print(f"Completed {mode} fork {fork}", flush=True)
summary = []
for seed in [424242, 1, -17]:
    data = {mode: [r for r in rows if r["mode"] == mode and r["seed"] == seed] for mode in ["baseline", "rich"]}
    summary.append({"seed": seed, **{mode: {"median_ms": statistics.median(r["ns"] for r in subset) / 1e6,
        "min_ms": min(r["ns"] for r in subset) / 1e6, "max_ms": max(r["ns"] for r in subset) / 1e6,
        "median_allocated_bytes": statistics.median(r["allocatedBytes"] for r in subset),
        "void_blocks": subset[0]["voids"], "blocks": subset[0]["blocks"]} for mode, subset in data.items()}})
evidence = {"scope": "section geometry evaluation plus local descriptor preparation, not native chunk generation or loading",
    "warmup_trials_per_seed_per_fork": 2, "measured_trials_per_seed_per_fork": 4,
    "summary": summary, "processes": processes, "rows": rows,
    "source_hashes": {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},
    "baseline_class_sha256": hashlib.sha256((ROOT / "build/classes/java/main/blockbranch/realistic/UndergroundStructure.class").read_bytes()).hexdigest()}
(OUT / "comparison.json").write_text(json.dumps(evidence, indent=2) + "\n")
print(json.dumps(summary, indent=2))
for helper, args, log_name in [
    ("RichNetworkTopology", [str(OUT / "topology.json")], "topology.log"),
    ("RichNetworkPreview", [], "preview.log"),
]:
    helper_command = command[:-1] + ["blockbranch.realistic." + helper] + args
    with (OUT / log_name).open("w") as log:
        subprocess.run(helper_command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, check=True)
