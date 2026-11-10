#!/usr/bin/env python3
"""Versioned igloo tops. Native basement choice, shaft, loot and residents stay native."""
from pathlib import Path
import runpy,gzip,json,hashlib
ROOT=Path(__file__).resolve().parents[2]
payload=runpy.run_path(str(ROOT/'scripts/structures/build-coastal-shipwreck.py'))['payload']
def build(v):
 b={}
 def put(x,y,z,name,**props):b[x,y,z]=(name,props)
 def box(x0,x1,y0,y1,z0,z1,name,**props):
  for x in range(x0,x1+1):
   for y in range(y0,y1+1):
    for z in range(z0,z1+1):put(x,y,z,name,**props)
 box(0,6,1,5,0,7,'air')
 stone='cobblestone' if v==0 else 'stone_bricks';wood='spruce_log' if v==0 else 'stripped_spruce_log'
 box(0,6,0,0,2,7,'snow_block');box(2,4,0,0,0,1,'spruce_planks')
 for z in range(2,8):
  for x in [0,6]:put(x,0,z,stone);box(x,x,1,2,z,z,'snow_block')
 for x in range(7):put(x,0,7,stone);box(x,x,1,2,7,7,'snow_block')
 for x in [0,1,2,4,5,6]:box(x,x,1,2,2,2,'snow_block')
 # Low snow vault, supported by continuous walls; stepped cap sheds the silhouette.
 box(0,6,3,3,2,7,'snow_block');box(1,5,4,4,3,6,'snow_block');box(2,4,5,5,4,5,'snow_block')
 # Short timber wind porch, with a three-block snow-safe entrance and full foundations.
 for x in [2,4]:box(x,x,1,3,0,1,wood,axis='y')
 box(2,4,4,4,0,1,wood,axis='x');box(2,4,5,5,1,1,'snow_block')
 for x in [0,6]:
  for z in ([3,6] if v==0 else [2,6]):box(x,x,1,2,z,z,wood,axis='y')
  put(x,2,4,'ice')
 # Same native furniture positions, lighting level, bed orientation and shaft cover.
 for z in range(3,7):
  for x in range(1,6):put(x,1,z,'white_carpet')
 put(1,1,3,'furnace',facing='east',lit='false');put(1,1,4,'redstone_torch',lit='true');put(1,1,5,'crafting_table')
 put(5,1,4,'red_bed',facing='south',part='foot',occupied='false');put(5,1,5,'red_bed',facing='south',part='head',occupied='false')
 for x in range(2,5):put(x,1,6,'light_gray_carpet' if v==0 else 'gray_carpet')
 put(3,0,0,'spruce_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')
 # Keep native trapdoor when a shaft exists, or native snow seal otherwise.
 b.pop((3,0,5),None)
 palette=[];indices={};blocks=[]
 for (x,y,z),(name,props) in sorted(b.items(),key=lambda e:(e[0][1],e[0][2],e[0][0])):
  key=name,tuple(sorted(props.items()))
  if key not in indices:
   indices[key]=len(palette);p={'id':(8,'minecraft:'+name)}
   if props:p['properties']=(10,{k:(8,val) for k,val in sorted(props.items())})
   palette.append(p)
  blocks.append({'pos':(9,(3,[x,y,z])),'state':(3,indices[key])})
 nbt={'DataVersion':(3,5023),'size':(9,(3,[7,6,8])),'palette':(9,(10,palette)),'blocks':(9,(10,blocks)),'entities':(9,(10,[]))}
 out=ROOT/f'src/main/resources/data/blockbranch/structure/igloo/snow_shelter_v1_{v}.nbt';out.parent.mkdir(parents=True,exist_ok=True);data=gzip.compress(b'\x0a\0\0'+payload(10,nbt),mtime=0);out.write_bytes(data)
 return {'file':str(out.relative_to(ROOT)),'sha256':hashlib.sha256(data).hexdigest()}
if __name__=='__main__':print(json.dumps([build(0),build(1)],indent=2))
