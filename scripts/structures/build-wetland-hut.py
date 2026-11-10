#!/usr/bin/env python3
"""Immutable native hut bodies. Native SwampHutPiece keeps supports and spawning."""
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
 box(0,6,0,6,0,8,'air')
 wall='spruce_planks' if v==0 else 'oak_planks';patch='oak_planks' if v==0 else 'spruce_planks';roof='dark_oak_stairs' if v==0 else 'spruce_stairs';ridge='dark_oak_planks' if v==0 else 'spruce_planks'
 box(1,5,1,1,1,7,wall)
 box(2,4,1,1,0,0,patch)
 for x in [1,5]:
  for z in [2,7]:box(x,x,0,4,z,z,'oak_log',axis='y')
 # A compact stillhouse with an offset gable; the second variant shifts the ridge.
 for z in range(2,8):
  for x in [1,5]:
   for y in [2,3]:put(x,y,z,patch if (z+v)%4==0 else wall)
 for x in range(1,6):
  for y in [2,3]:put(x,y,7,wall);put(x,y,2,wall)
 # Open entrance and barred side windows. No added light changes witch spawning.
 for y in [2,3]:put(3,y,2,'air')
 for z in [4,5]:put(1,3,z,'oak_fence',north='true',south='true',east='false',west='false',waterlogged='false')
 for z in [4,5]:put(5,3,z,'oak_fence',north='true',south='true',east='false',west='false',waterlogged='false')
 r=2 if v==0 else 4
 for z in range(1,9):
  for x in range(7):
   distance=abs(x-r);y=max(4,6-distance)
   if x==r:put(x,y,z,ridge)
   else:put(x,y,z,roof,facing='east' if x<r else 'west',half='bottom',shape='straight',waterlogged='false')
  # Close gables under the high ridge without a free-floating roof fragment.
  if z in [2,7]:
   for x in range(1,6):
    for y in range(4,max(4,6-abs(x-r))):put(x,y,z,wall)
 # Unequal front eave and a genuinely supported work porch, not a second building.
 for x in [2,4]:
  box(x,x,0,1,0,0,'oak_log',axis='y')
  box(x,x,2,3,0,0,'oak_fence',north='false',south='false',east='false',west='false',waterlogged='false')
 for x in range(2,5):put(x,4,0,roof,facing='south',half='bottom',shape='straight',waterlogged='false')
 put(3,1,0,'air')
 put(3,0,0,'oak_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')
 put(3,0,1,patch)
 put(3,1,1,'oak_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')
 # Recognizable vanilla hut furnishings, no new containers or loot.
 put(2,2,6,'crafting_table');put(4,2,6,'cauldron');put(4,3,7,'potted_red_mushroom')
 # Pot needs an actual shelf; do not hang it against the wall.
 put(4,2,7,patch)
 palette=[];index={};blocks=[]
 for (x,y,z),(name,props) in sorted(b.items(),key=lambda e:(e[0][1],e[0][2],e[0][0])):
  # Preserve bank soil below/outside the floor, but clear the native porch
  # floor cell directly above the first stair. Higher air replaces old walls.
  if name=='air' and y<=1 and (x,y,z)!=(3,1,0):continue
  key=name,tuple(sorted(props.items()))
  if key not in index:
   index[key]=len(palette);p={'id':(8,'minecraft:'+name)}
   if props:p['properties']=(10,{k:(8,val) for k,val in sorted(props.items())})
   palette.append(p)
  blocks.append({'pos':(9,(3,[x,y,z])),'state':(3,index[key])})
 nbt={'DataVersion':(3,5023),'size':(9,(3,[7,7,9])),'palette':(9,(10,palette)),'blocks':(9,(10,blocks)),'entities':(9,(10,[]))}
 out=ROOT/f'src/main/resources/data/blockbranch/structure/swamp_hut/stillhouse_v1_{v}.nbt';out.parent.mkdir(parents=True,exist_ok=True);data=gzip.compress(b'\x0a\0\0'+payload(10,nbt),mtime=0);out.write_bytes(data)
 return {'file':str(out),'sha256':hashlib.sha256(data).hexdigest()}
if __name__=='__main__':print(json.dumps([build(0),build(1)],indent=2))
