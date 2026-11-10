#!/usr/bin/env python3
"""Check resource references consumed by native item integration; no asset regeneration."""
from pathlib import Path
import json,struct,zlib
R=Path(__file__).resolve().parents[2];A=R/'src/main/resources/assets/blockbranch';D=R/'src/main/resources/data/blockbranch'
load=lambda p:json.loads(p.read_text())
names=load(A/'lang/en_us.json')
species=['wild_boar','red_deer','american_bison','brown_bear','mallard','eurasian_beaver','european_perch','harbor_seal']
items=[s+'_spawn_egg' for s in species]+['venison','cooked_venison','duck','cooked_duck','perch','cooked_perch','perch_bucket']
for item in items:
 assert names['item.blockbranch.'+item].strip(),item
 model=load(A/f'items/{item}.json')['model'];assert model['type']=='minecraft:model',item
 path=model['model'].removeprefix('blockbranch:');definition=load(A/f'models/{path}.json')
 for texture in definition.get('textures',{}).values():
  if texture.startswith('blockbranch:'):
   p=A/f"textures/{texture.removeprefix('blockbranch:')}.png";b=p.read_bytes();assert b[:8]==b'\x89PNG\r\n\x1a\n',p
   offset=8
   while offset<len(b):
    n=struct.unpack('>I',b[offset:offset+4])[0];chunk=b[offset+4:offset+8+n];crc=struct.unpack('>I',b[offset+8+n:offset+12+n])[0]
    assert zlib.crc32(chunk)&0xffffffff==crc,p
    offset+=n+12
   assert offset==len(b),p
for raw in ['venison','duck','perch']:
 for method in ['smelting','smoking','campfire_cooking']:
  key=f'cooked_{raw}_from_{method}';recipe=load(D/f'recipe/{key}.json');unlock=load(D/f'advancement/recipes/food/{key}.json')
  assert recipe['type']=='minecraft:'+method,key
  assert unlock['rewards']['recipes']==['blockbranch:'+key],key
  assert any(c.get('trigger')=='minecraft:inventory_changed' and any('blockbranch:'+raw==i.get('items') for i in c['conditions']['items']) for c in unlock['criteria'].values()),key
for s in species:
 loot=load(D/f'loot_table/entities/{s}.json');assert loot['type']=='minecraft:entity',s
print('PASS 15 item names/models/PNG CRCs;9 cooking recipe/unlock pairs;8 entity loot tables')
