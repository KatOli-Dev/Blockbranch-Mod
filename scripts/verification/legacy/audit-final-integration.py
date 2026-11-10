"""Historical final-integration output audit, preserved from published ae86628.
Requires the original /workspace/Blockbranch-final-local,
/workspace/Blockbranch-crypt-performance and /workspace/Blockbranch-integrated-overworld
fixture worlds. Run with Python 3 after provisioning those exact historical inputs.
Outputs to the final-local checkout's ignored build/final-integration-evidence/.
This is a saved-output correctness audit, not a new performance measurement.
The hardcoded paths and assertions below retain the original audit unchanged.
"""
import json,runpy,collections,pathlib,gzip
ROOT=pathlib.Path('/workspace/Blockbranch-final-local')
parser=runpy.run_path(str(ROOT/'scripts/verification/compare-terrain-profiles.py'))
read=parser['chunks'];reference=pathlib.Path('/workspace/Blockbranch-crypt-performance/build/crypt-perf-baseline')
a=read(reference/'structures/dimensions/minecraft/overworld');out={}
for name in ['final-crypt-fresh','final-crypt-reverse']:
 d=ROOT/'build'/name
 if not (d/'result.txt').exists():continue
 assert (d/'result.txt').read_text()=='PASS'
 b=read(d/'structures/dimensions/minecraft/overworld');assert a.keys()==b.keys()
 counts=collections.Counter()
 for p,x in a.items():
  y=b[p];assert x['Status']==y['Status'],('status',name,p);counts['coordinateStatuses']+=1
  if 'blockbranch:terrain' in x:assert x['blockbranch:terrain']==y.get('blockbranch:terrain'),('terrain',name,p);counts['terrainProfiles']+=1
  assert x.get('structures')==y.get('structures'),('native graphs/references',name,p);counts['structureGraphsReferences']+=1
  assert x.get('block_entities')==y.get('block_entities'),('native block-entity NBT',name,p);counts['blockEntityNbt']+=len(x.get('block_entities',[]))
  xb=[(s['Y'],s.get('biomes')) for s in x.get('sections',[])];yb=[(s['Y'],s.get('biomes')) for s in y.get('sections',[])];assert xb==yb,('biomes',name,p);counts['biomeSectionRecords']+=len(xb)
 crypt=json.loads((d/'crypt-saved.json').read_text());old=json.loads((reference/'crypt-saved.json').read_text());assert crypt['hash']==old['hash']
 out[name]={'parity':dict(counts),'cryptHash':crypt['hash'],'routes':crypt['connectedRoutePositions'],'lootTables':crypt['lootTables'],'note':'Final-checkout correctness execution, not a new performance comparison.'}
for name,source in [('final-old101','integrated-old101'),('final-old102','integrated-old102')]:
 d=ROOT/'build'/name
 if not(d/'result.txt').exists():continue
 assert (d/'result.txt').read_text()=='PASS'
 source=pathlib.Path('/workspace/Blockbranch-integrated-overworld/build')/source
 f='structures/data/minecraft/world_gen_settings.dat'
 before=parser['NBT'](gzip.decompress((source/f).read_bytes())).root();after=parser['NBT'](gzip.decompress((d/f).read_bytes())).root()
 assert before==after,('Saved world generation definition changed',name)
 rows=json.loads((d/'reopen.json').read_text());assert all(x['reopen'] if 'reopen' in x else x['reload'] for x in rows)
 out[name]={'worldGenNbtExactlyUnchanged':True,'reopenedNames':[x['name'] for x in rows]}
d=ROOT/'build/final-crypt-reopen';fresh=ROOT/'build/final-crypt-fresh';assert(d/'result.txt').read_text()=='PASS'
f='structures/data/minecraft/world_gen_settings.dat'
assert parser['NBT'](gzip.decompress((fresh/f).read_bytes())).root()==parser['NBT'](gzip.decompress((d/f).read_bytes())).root()
rows=json.loads((d/'reopen.json').read_text());assert len(rows)==1 and rows[0]['reload'] and rows[0]['connectedRoutePositions']==595
out['final-crypt-reopen']={'newCombined103WorldGenNbtExactlyUnchanged':True,'routes':595,'reload':True,'nativeSaveChecks':'Exact body, consumed loot and repair verified by native harness'}
(ROOT/'build/final-integration-evidence/output-parity.json').write_text(json.dumps(out,indent=2)+'\n');print(json.dumps(out,indent=2))
