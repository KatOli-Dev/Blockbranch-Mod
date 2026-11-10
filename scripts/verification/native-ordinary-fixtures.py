#!/usr/bin/env python3
"""Create metadata-only disposable schema100/101 fixtures. Never mutate source worlds."""
import gzip,struct,io,sys,json,hashlib,shutil
from pathlib import Path
source,destination,mode=Path(sys.argv[1]),Path(sys.argv[2]),int(sys.argv[3])
assert mode in (0,1) and source.is_file() and not destination.exists()
raw=source.read_bytes();stream=io.BytesIO(gzip.decompress(raw))
def number(fmt):return struct.unpack('>'+fmt,stream.read(struct.calcsize('>'+fmt)))[0]
def string():return stream.read(number('H')).decode()
def read(t):
 if t in (1,2,3,4,5,6):return number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
 if t==7:return stream.read(number('i'))
 if t==8:return string()
 if t==9:
  e=number('B');return e,[read(e) for _ in range(number('i'))]
 if t==10:
  d={}
  while (e:=number('B')):k=string();d[k]=(e,read(e))
  return d
 if t in (11,12):return [number('i' if t==11 else 'q') for _ in range(number('i'))]
 raise ValueError(t)
t=number('B');name=string();root=read(t);changes=[]
def visit(t,v,path):
 if t==10:
  if 'native_ordinary_structures' in v:
   assert v['shape_version'][1]==101 and v['native_ordinary_structures'][1]==1
   if mode==0:v['shape_version']=(3,100);del v['native_ordinary_structures']
   changes.append(path)
  for k,(e,q) in v.items():visit(e,q,path+'/'+k)
 elif t==9:
  e,items=v
  for i,q in enumerate(items):visit(e,q,path+'/'+str(i))
visit(t,root,'');assert len(changes)==1
out=io.BytesIO()
def put(fmt,v):out.write(struct.pack('>'+fmt,v))
def text(s):b=s.encode();put('H',len(b));out.write(b)
def write(t,v):
 if t in (1,2,3,4,5,6):put({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t],v)
 elif t==7:put('i',len(v));out.write(v)
 elif t==8:text(v)
 elif t==9:
  e,items=v;put('B',e);put('i',len(items))
  for q in items:write(e,q)
 elif t==10:
  for k,(e,q) in v.items():put('B',e);text(k);write(e,q)
  put('B',0)
 elif t in (11,12):
  put('i',len(v))
  for q in v:put('i' if t==11 else 'q',q)
 else:raise ValueError(t)
put('B',t);text(name);write(t,root);destination.mkdir(parents=True);target=destination/'data/minecraft/world_gen_settings.dat';target.parent.mkdir(parents=True);target.write_bytes(gzip.compress(out.getvalue(),mtime=0));world=source.parents[2];shutil.copy2(world/'level.dat',destination/'level.dat');shutil.copy2(world/'blockbranch-columns.txt',destination/'blockbranch-columns.txt');registry=destination/'data/fabric/registry_entries.dat';registry.parent.mkdir(parents=True);shutil.copy2(world/'data/fabric/registry_entries.dat',registry);assert source.read_bytes()==raw
(destination.parent/'fixture.json').write_text(json.dumps({'source':str(source),'sourceSha256':hashlib.sha256(raw).hexdigest(),'destination':str(destination),'nativeOrdinary':mode,'generatorPath':changes,'noSourceMutation':True,'noChunksCopied':True},indent=2)+'\n')
