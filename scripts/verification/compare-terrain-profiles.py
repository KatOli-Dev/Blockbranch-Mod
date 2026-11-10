#!/usr/bin/env python3
"""Compare native NBT terrain profiles/biomes in two Overworld region directories.

Usage: python3 scripts/verification/compare-terrain-profiles.py WORLD_OVERWORLD WORLD_OVERWORLD
Reads local saved fixtures only; requires no external Python packages.
"""
import struct,zlib,gzip,json,hashlib,sys
from pathlib import Path
class NBT:
 def __init__(self,b):self.b=b;self.p=0
 def take(self,n):v=self.b[self.p:self.p+n];self.p+=n;return v
 def num(self,f):return struct.unpack('>'+f,self.take(struct.calcsize('>'+f)))[0]
 def string(self):return self.take(self.num('H')).decode('utf8')
 def value(self,t):
  if t in range(1,7):return self.num({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
  if t==8:return self.string()
  if t in (7,11,12):return [self.num({7:'b',11:'i',12:'q'}[t]) for _ in range(self.num('i'))]
  if t==9:
   child=self.num('B');return [self.value(child) for _ in range(self.num('i'))]
  if t==10:
   out={}
   while (child:=self.num('B'))!=0:
    key=self.string();out[key]=self.value(child)
   return out
  raise ValueError(t)
 def root(self):
  t=self.num('B');self.string();return self.value(t)
def chunks(world):
 out={}
 for path in Path(world).glob('region/*.mca'):
  _,rx,rz,_=path.name.split('.');rx=int(rx);rz=int(rz);b=path.read_bytes()
  for i in range(1024):
   offset=int.from_bytes(b[i*4:i*4+3],'big')*4096
   if not offset:continue
   size=int.from_bytes(b[offset:offset+4],'big');kind=b[offset+4];raw=b[offset+5:offset+4+size]
   if kind==2:raw=zlib.decompress(raw)
   elif kind==1:raw=gzip.decompress(raw)
   elif kind!=3:raise ValueError((path,kind))
   root=NBT(raw).root();out[(rx*32+i%32,rz*32+i//32)]=root
 return out
if __name__=='__main__':
 a=chunks(sys.argv[1]);b=chunks(sys.argv[2]);shared=set(a)&set(b)
 keys=sorted(set().union(*(x.keys() for x in a.values())))
 print('root fields',keys)
 mismatches=[];compared=0;native=0
 for key in sorted(shared):
  av=a[key];bv=b[key]
  if 'blockbranch:terrain' not in av or 'blockbranch:terrain' not in bv:continue
  compared+=1
  if av['blockbranch:terrain']!=bv['blockbranch:terrain']:mismatches.append(key)
  # Biome containers are unaffected by later block edits or entity simulation.
  aa=[(s['Y'],s.get('biomes')) for s in av.get('sections',[])];bb=[(s['Y'],s.get('biomes')) for s in bv.get('sections',[])]
  if aa!=bb:native+=1
 print(json.dumps({'leftChunks':len(a),'rightChunks':len(b),'sharedChunks':len(shared),'terrainProfilesCompared':compared,'profileMismatches':mismatches,'nativeBiomeMismatches':native},indent=2))
