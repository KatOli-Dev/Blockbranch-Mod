#!/usr/bin/env python3
"""Build immutable native shipwreck templates (v1 retained for saved starts); no third-party NBT dependency."""
from pathlib import Path
# Minecraft26.3 structure schema: id/properties palette keys, data version5023.
# Native loaded/saved equality against the legacy-labelled v1 fixtures is required.
import gzip,struct,hashlib,json
# Basic vanilla blocks/properties used here have the same names in this source version.
DATA_VERSION=5023
ROOT=Path(__file__).resolve().parents[2]/'src/main/resources/data/blockbranch/structure/shipwreck'
def string(v):
 b=v.encode('utf-8');return struct.pack('>H',len(b))+b
def payload(t,v):
 if t==3:return struct.pack('>i',v)
 if t==8:return string(v)
 if t==9:
  item,values=v;return bytes([item])+struct.pack('>i',len(values))+b''.join(payload(item,x) for x in values)
 if t==10:return b''.join(bytes([kind])+string(k)+payload(kind,value) for k,(kind,value) in v.items())+b'\0'
 raise ValueError(t)
def build(variant,revision=2):
 blocks={}
 def put(x,y,z,name,**props):blocks[x,y,z]=(name,props,None)
 def box(x0,x1,y0,y1,z0,z1,name,**props):
  for z in range(z0,z1+1):
   for y in range(y0,y1+1):
    for x in range(x0,x1+1):put(x,y,z,name,**props)
 plank='oak_planks' if variant==0 else 'spruce_planks'
 repair='spruce_planks' if variant==0 else 'oak_planks'
 ribs='stripped_spruce_log' if variant==0 else 'stripped_oak_log'
 stair='oak_stairs' if variant==0 else 'spruce_stairs'
 fence='spruce_fence' if variant==0 else 'oak_fence'
 # Clear only the above-ground walkable interior, not the soil under the wreck.
 box(2,6,3,5,4,28,'cave_air')
 for z in range(31):
  w=min(4,z) if z<26 else 3
  put(4,0,z,ribs,axis='z')
  for x in range(4-max(0,w-2),5+max(0,w-2)):put(x,1,z,repair)
  for x in range(4-max(0,w-1),5+max(0,w-1)):put(x,2,z,plank)
  for side in [-1,1]:
   x=4+side*w
   for y in [3,4]:
    if side==1 and 10<=z<=14:continue # A readable, traversable starboard breach.
    put(x,y,z,repair if y==3 and z%7 in [0,1] else plank)
   if z in [4,9,16,22,28]:
    for y in range(1,6):
     if side==1 and 10<=z<=14:continue
     put(x,y,z,ribs,axis='y')
   if 4<=z<=25 and not(side==1 and 10<=z<=14):
    for dx in [w-1,max(0,w-2)]:
     if variant==1 and z in [6,7] and side==-1:continue
     put(4+side*dx,5,z,plank)
    if z%6 not in [0,1]:put(x,6,z,fence)
  if 1<=z<=7:
   for x in range(4-w,5+w):put(x,5,z,plank)
  if 21<=z<=29:
   for x in range(4-(w-1),5+(w-1)):put(x,6,z,plank)
 # Half-block boarding steps across the starboard breach.
 for z in [11,12,13]:put(8,2,z,stair,facing='west',half='bottom',shape='straight',waterlogged='false')
 # Sturdy transom and aft room support; the raised deck is an accessible chart deck.
 box(1,7,3,5,30,30,plank)
 for x in [1,7]:
  for z in [21,25,29]:box(x,x,3,6,z,z,ribs,axis='y')
  for z in range(22,30):
   if z not in ([24] if variant==0 else [23,24]):put(x,7,z,fence)
 for x in range(2,7):
  if x!=5:put(x,7,30,fence)
 # Keep the legacy resources byte-identical. Revision2 grounds the rear rail
 # and gives the diagonal stair treads a continuous wooden stringer.
 if revision>=2:box(1,7,6,6,30,30,plank)
 for step in range(4):
  if revision>=2:box(4,4,3,2+step,17+step,17+step,repair)
  put(4,3+step,17+step,stair,facing='south',half='bottom',shape='straight',waterlogged='false')
 # Broken mast and short remaining yard: restrained silhouette without a full sail.
 for y in range(3,11):put(4,y,12,ribs,axis='y')
 for x in range(1,7):put(x,8,12,ribs,axis='x')
 put(6,7,12,repair);put(2,7,12,'oak_fence' if variant==0 else 'spruce_fence')
 # Three ordinary vanilla compartments. Native ShipwreckPiece installs the loot.
 for name,(x,y,z) in {'supply_chest':(3,3,6),'map_chest':(3,7,25),'treasure_chest':(5,3,25)}.items():
  put(x,y,z,'chest',facing='north',type='single',waterlogged='false')
  blocks[x,y,z]=('chest',blocks[x,y,z][1],{'id':(8,'minecraft:chest')})
  blocks[x,y+1,z]=('structure_block',{'mode':'data'},{'mode':(8,'DATA'),'metadata':(8,name)})
 # Explicit fence connections rotate with the template; no dangling standalone decorations.
 for (x,y,z),(name,props,nbt) in list(blocks.items()):
  if name.endswith('_fence'):
   p={d:'true' if (x+dx,y,z+dz) in blocks and blocks[x+dx,y,z+dz][0] not in ['cave_air','structure_block'] else 'false' for d,dx,dz in [('north',0,-1),('south',0,1),('west',-1,0),('east',1,0)]};p['waterlogged']='false';blocks[x,y,z]=(name,p,nbt)
 palette=[];index={};entries=[]
 for (x,y,z),(name,props,nbt) in sorted(blocks.items(),key=lambda p:(p[0][1],p[0][2],p[0][0])):
  key=(name,tuple(sorted(props.items())))
  if key not in index:
   index[key]=len(palette);state={'id':(8,'minecraft:'+name)}
   if props:state['properties']=(10,{k:(8,v) for k,v in sorted(props.items())})
   palette.append(state)
  entry={'pos':(9,(3,[x,y,z])),'state':(3,index[key])}
  if nbt:entry['nbt']=(10,nbt)
  entries.append(entry)
 root={'DataVersion':(3,DATA_VERSION),'size':(9,(3,[9,12,31])),'palette':(9,(10,palette)),'blocks':(9,(10,entries)),'entities':(9,(10,[]))}
 data=gzip.compress(b'\x0a\0\0'+payload(10,root),mtime=0)
 ROOT.mkdir(parents=True,exist_ok=True);path=ROOT/f'coastal_sloop_v{revision}_{variant}.nbt';path.write_bytes(data)
 return {'file':str(path),'blocks':len(blocks),'palette':len(palette),'sha256':hashlib.sha256(data).hexdigest()}
if __name__=='__main__':print(json.dumps([build(v,r) for r in [1,2] for v in [0,1]],indent=2))
