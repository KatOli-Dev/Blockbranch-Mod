#!/usr/bin/env python3
"""Compare semantic native outputs, excluding timestamps/entities and tick order.

Arguments are two saved Overworld directories containing region/*.mca.
Uses the repository's NBT reader; no external packages or world mutations.
"""
import importlib.util
import json
import sys
from pathlib import Path

sys.dont_write_bytecode = True
reader = Path(__file__).resolve().parents[1] / 'verification/compare-terrain-profiles.py'
spec = importlib.util.spec_from_file_location('terrain_nbt', reader)
nbt = importlib.util.module_from_spec(spec)
spec.loader.exec_module(nbt)
a, b = (nbt.chunks(path) for path in sys.argv[1:3])
shared = set(a) & set(b)
report = {'baselineChunks': len(a), 'candidateChunks': len(b),
          'sameCoordinates': set(a) == set(b), 'terrainProfilesCompared': 0,
          'biomeChunksCompared': len(shared), 'structureChunksCompared': 0}
for field in ['terrain', 'biome', 'status', 'structure', 'nativeBlockState', 'heightmap']:
    report[field + 'Mismatches'] = []
for key in sorted(shared):
    x, y = a[key], b[key]
    report['terrainProfilesCompared'] += 'blockbranch:terrain' in x
    report['structureChunksCompared'] += 'structures' in x
    for name, field in [('terrain', 'blockbranch:terrain'), ('status', 'Status'),
                        ('structure', 'structures'), ('heightmap', 'Heightmaps')]:
        if x.get(field) != y.get(field):
            report[name + 'Mismatches'].append(key)
    for name, field in [('biome', 'biomes'), ('nativeBlockState', 'block_states')]:
        left = [(s['Y'], s.get(field)) for s in x.get('sections', [])]
        right = [(s['Y'], s.get(field)) for s in y.get('sections', [])]
        if left != right:
            report[name + 'Mismatches'].append(key)
print(json.dumps(report, indent=2))
passed = (bool(shared) and report['sameCoordinates'] and
          report['terrainProfilesCompared'] > 0 and report['structureChunksCompared'] > 0 and
          all(not value for name, value in report.items() if name.endswith('Mismatches')))
raise SystemExit(0 if passed else 1)
