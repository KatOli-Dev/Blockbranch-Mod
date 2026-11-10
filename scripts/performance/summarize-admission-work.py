#!/usr/bin/env python3
"""Summarize one bounded native admission audit. Does not infer wall-time causality."""
import collections
import json
import pathlib
import sys

root=pathlib.Path(sys.argv[1])
rows=json.loads((root/'work-counts.json').read_text())
admissions=json.loads((root/'admission.json').read_text())
ocean={r['phase']:r for r in json.loads((root/'ocean-counts.json').read_text())}
quota={r['phase']:r for r in json.loads((root/'quota.json').read_text())}
result={}
for phase in (1,2,3):
    group=[r for r in rows if r['phase']==phase]
    kinds=collections.defaultdict(list)
    for r in group:kinds[r['kind']].append(r)
    def calls(kind):return sum(r['calls'] for r in kinds[kind])
    def owners(kind):return {(r['seed'],r['x'],r['z'],r['owner']) for r in kinds[kind]}
    profiles=owners('profile'); biomes=owners('status.minecraft:biomes'); terrain=owners('status.minecraft:terrain')
    repeated={(r['seed'],r['x'],r['z'],r['owner']) for r in kinds['profile'] if r['calls']>1}
    evicted=owners('prepared.evict'); missed=owners('prepared.consume.miss')
    q=quota[phase];delta={k:q['quotaAfter'][k]-q['quotaBefore'][k] for k in q['quotaBefore']}
    result[str(phase)]={
        'admission':admissions[phase-1],
        'statusCalls':{k.removeprefix('status.'):calls(k) for k in sorted(kinds) if k.startswith('status.')},
        'profileCalls':calls('profile'),'profileOwners':len(profiles),'profileFailures':sum(r['failures'] for r in kinds['profile']),
        'biomesOnlyProfileOwners':len(profiles & (biomes-terrain)),
        'repeatedProfileOwners':len(repeated),'evictedAndRebuiltSameOwners':len(evicted & repeated & missed),
        'consumedPreparedOwners':len(owners('prepared.consume.nonNull')),
        'preparedEvictions':calls('prepared.evict'),
        'profileInclusiveCpuSeconds':sum(r['cpuNanos'] for r in kinds['profile'])/1e9,
        'canopyCalls':calls('canopy'),'canopyCreations':calls('canopy.put.new')+calls('canopy.put.replace'),
        'canopyInclusiveCpuSeconds':sum(r['cpuNanos'] for r in kinds['canopy'])/1e9,
        'ocean':ocean[phase], 'quotaDeltas':delta,
        'throttledPeriodPercent':100*delta['nr_throttled']/max(1,delta['nr_periods'])}
json.dump(result,sys.stdout,indent=2);print()
