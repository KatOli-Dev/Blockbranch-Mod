#!/usr/bin/env python3
"""Deterministic native NBT templates; stable v1 assets after adoption."""
from pathlib import Path
# Minecraft26.3 structure schema: id/properties palette keys, data version5023.
# Native loaded/saved equality against the legacy-labelled v1 fixtures is required.
import gzip,struct,json,hashlib
ROOT=Path(__file__).resolve().parents[2]/'src/main/resources/data/blockbranch/structure/ruined_portal'
def string(s):
 b=s.encode();return struct.pack('>H',len(b))+b
def payload(t,v):
 if t==3:return struct.pack('>i',v)
 if t==8:return string(v)
 if t==9:
  k,a=v;return bytes([k])+struct.pack('>i',len(a))+b''.join(payload(k,x) for x in a)
 if t==10:return b''.join(bytes([k])+string(n)+payload(k,x) for n,(k,x) in v.items())+b'\0'
 raise ValueError(t)
def build(variant):
 blocks={}
 def put(x,y,z,name,**props):blocks[x,y,z]=(name,props,None)
 def box(xa,xb,ya,yb,za,zb,name,**props):
  for z in range(za,zb+1):
   for y in range(ya,yb+1):
    for x in range(xa,xb+1):put(x,y,z,name,**props)
 # Buried footings support a modest open forecourt, with wind-worn uneven edges.
 for z in range(1,12):
  for x in range(1,12):
   if (x,z) in [(1,1),(11,1),(1,11),(11,11)]:continue
   put(x,0,z,'sandstone');put(x,1,z,'sandstone')
   if 2<=x<=10 and 2<=z<=10:put(x,2,z,'cut_sandstone' if (x+z)%4 else 'smooth_sandstone')
 # Three shallow steps on the open front; approach has its own dry terrain check.
 for x in range(4,9):
  put(x,1,0,'sandstone_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')
  put(x,2,1,'sandstone_stairs',facing='south',half='bottom',shape='straight',waterlogged='false')
 box(2,10,3,9,2,10,'cave_air')
 # A familiar minimum 4x5 portal frame. Crying and missing obsidian require repair.
 for x in range(5,9):
  for y in range(3,8):
   if x not in [5,8] and y not in [3,7]:continue
   name='obsidian'
   if (x,y) in [(5,5),(7,7)]:name='crying_obsidian'
   if (x,y) in ([(8,6),(8,7)] if variant==0 else [(5,6),(6,7)]):continue
   put(x,y,7,name)
 # Distinct archaeological silhouettes, with the same ordinary repairable frame.
 # A narrow surviving route through the forecourt explains how this place was used.
 for z in range(2,7):
  for x in [6,7]:put(x,2,z,'smooth_sandstone')
 for x,z in [(4,2),(9,2),(4,5),(9,5)]:put(x,2,z,'chiseled_sandstone')
 if variant==0:
  # Split caravan gate: two unequal tapering pylons with a weathered red repair band.
  for xa,xb,top in [(2,4,7),(9,11,10)]:
   box(xa,xb,3,5,7,9,'smooth_sandstone')
   box(xa,xb,4,4,7,9,'red_sandstone')
   box(xa,xb-1,6,top-1,7,8,'cut_sandstone')
   for x in range(xa,xb):put(x,top,7,'sandstone_slab',type='bottom',waterlogged='false')
   for x in range(xa,xb+1):put(x,3,6,'chiseled_sandstone')
  # Only half of the elevated lintel survives; the opening remains unroofed.
  for x in range(4,7):put(x,8,7,'cut_sandstone');put(x,8,8,'sandstone_slab',type='bottom',waterlogged='false')
  for x,z in [(9,3),(10,3),(10,4),(11,4)]:put(x,3,z,'red_sandstone_slab',type='bottom',waterlogged='false')
  for z in range(3,6):put(1,3,z,'sandstone');put(1,4,z,'sandstone_slab',type='bottom',waterlogged='false')
 else:
  # Shelter gate: a shaded loading bay survives beside the collapsed gatehouse.
  box(9,11,3,5,7,9,'smooth_sandstone');box(9,10,6,8,7,8,'cut_sandstone')
  for x in [8,9]:put(x,8,7,'sandstone_stairs',facing='west',half='top',shape='straight',waterlogged='false')
  for z in [3,6]:
   box(1,1,3,5,z,z,'sandstone');put(1,4,z,'brown_terracotta')
   box(4,4,3,4,z,z,'oak_log',axis='y')
  for z in range(3,7):
   for x in range(1,5):put(x,6,z,'sandstone_slab',type='bottom',waterlogged='false')
   put(4,5,z,'oak_log',axis='z')
  box(2,4,3,3,9,9,'sandstone');put(2,4,9,'chiseled_sandstone')
  for x,y,z in [(9,3,3),(10,3,3),(10,4,4),(11,3,4),(10,3,5)]:put(x,y,z,'cut_sandstone' if y==3 else 'sandstone_stairs',**({} if y==3 else {'facing':'south','half':'bottom','shape':'straight','waterlogged':'false'}))
  put(10,7,8,'gold_block')
 # Move the broken corner into the collapse pile; no unsupported floating fragment.
 if variant==1:
  blocks.pop((5,7,7),None);put(9,3,5,'obsidian')
 # A restrained scar of the old portal, rather than a spreading hazard field.
 for x,z in [(5,6),(6,6),(7,6),(8,6),(6,8),(7,8),(6,9)]:put(x,2,z,'netherrack')
 put(3,3,4,'chest',facing='south',type='single',waterlogged='false');blocks[3,3,4]=('chest',blocks[3,3,4][1],{'id':(8,'minecraft:chest')})
 blocks[3,4,4]=('structure_block',{'mode':'data'},{'mode':(8,'DATA'),'metadata':(8,'chest')})
 # Every low wall/pier needs a plinth even where the surrounding sand is two blocks lower.
 for (x,y,z),(name,props,nbt) in list(blocks.items()):
  if y==3 and name not in ['cave_air','structure_block','air'] and blocks.get((x,2,z),('cave_air',{},None))[0] in ['cave_air','air']:put(x,2,z,'sandstone')
 if variant==1:put(10,3,4,'cut_sandstone')
 # Authoring invariant: all solid fragments connect to the buried footing. No floating debris.
 solids={p for p,(name,props,nbt) in blocks.items() if name not in ['cave_air','structure_block','air']}
 reached={p for p in solids if p[1]==0};todo=list(reached)
 while todo:
  x,y,z=todo.pop()
  for dx,dy,dz in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]:
   q=(x+dx,y+dy,z+dz)
   if q in solids and q not in reached:reached.add(q);todo.append(q)
 if reached!=solids:raise ValueError('Unsupported template component: '+repr(sorted(solids-reached)))
 palette=[];indices={};entries=[]
 for (x,y,z),(name,props,nbt) in sorted(blocks.items(),key=lambda e:(e[0][1],e[0][2],e[0][0])):
  key=(name,tuple(sorted(props.items())))
  if key not in indices:
   indices[key]=len(palette);p={'id':(8,'minecraft:'+name)}
   if props:p['properties']=(10,{n:(8,v) for n,v in sorted(props.items())})
   palette.append(p)
  e={'pos':(9,(3,[x,y,z])),'state':(3,indices[key])}
  if nbt:e['nbt']=(10,nbt)
  entries.append(e)
 root={'DataVersion':(3,5023),'size':(9,(3,[13,12,13])),'palette':(9,(10,palette)),'blocks':(9,(10,entries)),'entities':(9,(10,[]))}
 data=gzip.compress(b'\x0a\0\0'+payload(10,root),mtime=0);ROOT.mkdir(parents=True,exist_ok=True);p=ROOT/f'desert_waygate_v1_{variant}.nbt';p.write_bytes(data)
 return {'file':str(p),'blocks':len(blocks),'sha256':hashlib.sha256(data).hexdigest()}
if __name__=='__main__':print(json.dumps([build(0),build(1)],indent=2))
