#!/usr/bin/env python3
"""Exact outcome and actual-work audit for the serial crypt cache pair. Read-only fixtures."""
import argparse,json,collections,runpy,sys,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
chunks=runpy.run_path(str(ROOT/'scripts/verification/compare-terrain-profiles.py'))['chunks']
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--baseline',type=Path,default=ROOT/'build/crypt-perf-baseline')
parser.add_argument('--candidate',type=Path,default=ROOT/'build/crypt-perf-candidate')
parser.add_argument('--output',type=Path,default=ROOT/'build/crypt-performance-evidence/matched-audit.json')
args=parser.parse_args()
dirs=[args.baseline,args.candidate]
worlds=[chunks(d/'structures/dimensions/minecraft/overworld') for d in dirs]
assert worlds[0].keys()==worlds[1].keys(),'Persisted chunk coordinate set changed'
out={};canonical=[]
for name,d,w in zip(('baseline','candidate'),dirs,worlds):
 p=json.loads((d/'admission-boundaries.json').read_text());a=p['marks'][0];timings={}
 for m in p['marks']:
  if m['name'] not in ('regional-plan','structure-start','forced-calls-returned','entities-ready'):continue
  timings[m['name']]={k:m[k]-a[k] for k in ('elapsedNanos','processCpuNanos','processAllocatedBytes','serverCpuNanos','serverAllocatedBytes','gcMillis')}
  timings[m['name']]['heapUsed']=m['heapUsed'];timings[m['name']]['quotaDelta']={k:m['quota'][k]-a['quota'][k] for k in m['quota']}
 rows=json.loads((d/'work-trace.json').read_text());counts=collections.Counter();cpu=collections.Counter();keys=[]
 for r in rows:
  counts[r['kind']]+=r['calls'];cpu[r['kind']]+=r['cpuNanos'];keys.append((r['kind'],r['x'],r['z'],r['calls'],r['failures']))
 # Phase boundaries move with throughput; compare total work by coordinate, never scheduling order/owner IDs.
 merged=collections.Counter()
 for k,x,z,c,f in keys:merged[k,x,z,"calls"]+=c;merged[k,x,z,"false"]+=f
 canonical.append(merged)
 trace=json.loads((d/'generation-trace.json').read_text());steps=collections.Counter()
 for key,v in trace['steps'].items():
  parts=key.split(':');assert parts[0]!='FAIL',key
  del parts[2];steps[':'.join(parts)]+=v
 nearby={k:v for k,v in w.items() if abs(k[0]-14281)<=20 and abs(k[1]+16734)<=20}
 starts=[{'chunk':k,'id':key,'pieces':len(tag.get('Children',[]))} for k,v in nearby.items() for key,tag in v.get('structures',{}).get('starts',{}).items() if tag.get('id') not in (None,'INVALID')]
 out[name]={'boundaries':timings,'allWorkCounts':counts,'allWorkCpuNanos':cpu,'stepsIgnoringPhase':dict(sorted(steps.items())),'activeAtStopped':[trace['activeTasks'],trace['activeSteps']],'nearbyStatuses':collections.Counter(v['Status'] for v in nearby.values()),'starts':starts,'crypt':json.loads((d/'crypt-saved.json').read_text())}
profiles=0;biomes=0;blockentities=0;structures=0
for pos,a in worlds[0].items():
 b=worlds[1][pos];assert a['Status']==b['Status'],('Status',pos)
 if 'blockbranch:terrain' in a:
  assert a['blockbranch:terrain']==b.get('blockbranch:terrain'),('Terrain',pos);profiles+=1
 assert a.get('structures')==b.get('structures'),('Structure native seed/graph/references',pos);structures+=1
 assert a.get('block_entities')==b.get('block_entities'),('Block entity/loot NBT',pos);blockentities+=len(a.get('block_entities',[]))
 aa=[(s['Y'],s.get('biomes')) for s in a.get('sections',[])];bb=[(s['Y'],s.get('biomes')) for s in b.get('sections',[])];assert aa==bb,('Biome',pos);biomes+=len(aa)
assert out['baseline']['crypt']['hash']==out['candidate']['crypt']['hash']
out['comparison']={'samePersistedCoordinateStatuses':len(worlds[0]),'exactTerrainProfiles':profiles,'exactNativeBiomeSections':biomes,'exactNativeStructureGraphsReferences':structures,'exactBlockEntityNbt':blockentities,'sameStepCoordinateFromToCounts':out['baseline']['stepsIgnoringPhase']==out['candidate']['stepsIgnoringPhase'],'workCountDeltas':{k:out['candidate']['allWorkCounts'][k]-out['baseline']['allWorkCounts'][k] for k in set(out['baseline']['allWorkCounts'])|set(out['candidate']['allWorkCounts']) if out['candidate']['allWorkCounts'][k]!=out['baseline']['allWorkCounts'][k]}}
# Cache access counts may differ due concurrent eviction; actual native fits/placement and profile identities must not.
for k in set(canonical[0])|set(canonical[1]):
 if k[0].startswith(('fit.','place.','feature.')) or k[0]=='profile':assert canonical[0][k]==canonical[1][k],('Actual work changed',k,canonical[0][k],canonical[1][k])
args.output.parent.mkdir(parents=True,exist_ok=True)
args.output.write_text(json.dumps(out,indent=2)+'\n')
print(json.dumps(out['comparison'],indent=2))
