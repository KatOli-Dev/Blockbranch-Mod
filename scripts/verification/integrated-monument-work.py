#!/usr/bin/env python3
"""Compare actual saved status/start transitions for the one local final-code pair.
No world mutations. Counts persisted work, not transient/recomputed worker tasks.
"""
from pathlib import Path
import ast,collections,json,runpy
ROOT=Path(__file__).resolve().parents[2]
chunks=runpy.run_path(str(ROOT/'scripts/verification/compare-terrain-profiles.py'))['chunks']
evidence=ROOT/'build/integration-evidence';out={}
for mode,directory in [('control','integrated-reimagined-monument-control'),('combined','integrated-final-monument')]:
 before={ast.literal_eval(k):v for k,v in json.loads((evidence/f'monument-{mode}-before.json').read_text()).items()}
 after=chunks(ROOT/f'build/{directory}/structures/dimensions/minecraft/overworld')
 # Broad enough to contain the complete generation dependency halo, far from all prior sites.
 relevant={k:v for k,v in after.items() if abs(k[0]-9806)<=32 and abs(k[1]+35430)<=32}
 prior={k:v for k,v in before.items() if abs(k[0]-9806)<=32 and abs(k[1]+35430)<=32}
 assert not prior,'Pair must begin with no persisted chunks near the monument'
 statuses=collections.Counter(v['Status'] for v in relevant.values());starts=[]
 for pos,v in relevant.items():
  for key,tag in v.get('structures',{}).get('starts',{}).items():
   if tag.get('id') in (None,'INVALID'):continue
   packed=(pos[0]&0xffffffff)|((pos[1]&0xffffffff)<<32)
   if packed>=1<<63:packed-=1<<64
   referenced=[list(k) for k,q in relevant.items() if q['Status'] in {'minecraft:features','minecraft:initialize_light','minecraft:light','minecraft:spawn','minecraft:full'} and packed in q.get('structures',{}).get('References',{}).get(key,[])]
   starts.append({'id':key,'chunk':list(pos),'startChunkStatus':v['Status'],'pieces':len(tag.get('Children',[])),'featuresCompleteChunksReferencingStart':len(referenced)})
 row=json.loads((ROOT/f'build/{directory}/monument-saved.json').read_text())
 out[mode]={'beforePersistedNearSite':len(prior),'afterPersistedNearSite':len(relevant),'statuses':dict(statuses),'validStructureStarts':starts,'extraCoverageStarts':[s for s in starts if s['id']!='minecraft:monument'],'requestedFootprintChunks':row['chunks'],'admissionWallNanos':row['admissionNanos'],'admissionProcessCpuNanos':row['admissionProcessCpuNanos'],'admissionCumulativeProcessAllocatedBytes':row['admissionProcessAllocatedBytes'],'planningWallNanos':row['planningNanos'],'planningCumulativeProcessAllocatedBytes':row['planningProcessAllocatedBytes'],'geometryHash':row['hash']}
 assert all(abs(k[0]-9806)<32 and abs(k[1]+35430)<32 for k in relevant),'Halo hit analysis boundary'
 assert statuses['minecraft:full']>=49
out['limits']=['One serial pair, not a statistical benchmark.','Identical49 requested chunks and identical monument hash do not mean identical total work.','Status counts are persisted dependency-halo outcomes, not every transient/recomputed worker operation.','Combined mode executes additional native coverage checks/features and gated biome work; it is not isolated layout overhead.','Allocations are cumulative process bytes, not retained or peak heap.','Process counters include concurrent generation, lighting, GC and background work. No connected-player latency inference.']
(evidence/'final-monument-pair.json').write_text(json.dumps(out,indent=2)+'\n')
print(json.dumps(out,indent=2))
