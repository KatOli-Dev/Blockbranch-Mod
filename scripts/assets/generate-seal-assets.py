#!/usr/bin/env python3
"""Original restrained seal coats and short contact/hurt/death grunts."""
from pathlib import Path
import json,random,math,struct,zlib,wave,subprocess,tempfile,sys
ROOT=Path(__file__).resolve().parents[2];A=ROOT/'src/main/resources/assets/blockbranch';D=ROOT/'src/main/resources/data/blockbranch'
def chunk(k,b):return struct.pack('>I',len(b))+k+b+struct.pack('>I',zlib.crc32(k+b)&0xffffffff)
def coat(kit):
 # One texture pixel per model unit, matching the other vanilla-style wildlife models.
 base=(132,139,137) if not kit else (165,174,168)
 spots={(2,14),(5,17),(8,13),(11,16),(14,15),(18,17),(22,13),(25,16),(29,14),(31,17),(35,15),(38,17),
        (14,3),(17,7),(19,10),(2,50),(5,52),(8,51),(12,53),(16,50),(20,52),(23,49),(9,44),(12,46)}
 rows=[]
 for y in range(64):
  row=bytearray([0])
  for x in range(128):
   color=base;shade=0
   chest=x<40 and y<19;rear=x<27 and 42<=y<55
   if chest or rear:
    # Restrained two-pixel tone clusters; geometry and normal face shading carry the volume.
    shade=(-2,0,2)[(x//2*7+y//2*11)%3]
    if chest and y<12 or rear and y<49:shade+=4
    if (x,y) in spots:shade-=12 if kit else 24
   if y>=30 and not rear:color=(89,103,101) if not kit else (124,139,128)
   if 58<=x<78 and y<10:
    shade=2 if y<5 else 0
    if x in (61,69,64,66) and y==6:color=(25,32,30);shade=0
   if 58<=x<68 and 12<=y<15:
    color=(170,180,174) if not kit else (191,201,191);shade=0
    if x in (60,61) and y==13:color=(28,34,33)
    if x in (60,61) and y==14:color=(118,130,121)
   row.extend((*[max(0,min(255,v+shade)) for v in color],255))
  rows.append(bytes(row))
 return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',128,64,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows)))+chunk(b'IEND',b'')

for kit in (False,True):
 p=A/'textures/entity'/('harbor_seal'+('_pup' if kit else '')+'.png');p.write_bytes(coat(kit))
if '--coats-only' in sys.argv:sys.exit(0)
loot=json.loads((D/'loot_table/entities/brown_bear.json').read_text());loot['random_sequence']='blockbranch:entities/harbor_seal';(D/'loot_table/entities/harbor_seal.json').write_text(json.dumps(loot,indent=2)+'\n')
lang=A/'lang/en_us.json';s=json.loads(lang.read_text());s['entity.blockbranch.harbor_seal']='Harbor Seal'
for name,label in [('contact','Seal grunts'),('hurt','Seal hurts'),('death','Seal dies')]:s['subtitles.blockbranch.harbor_seal.'+name]=label
lang.write_text(json.dumps(s,indent=2)+'\n');sounds=json.loads((A/'sounds.json').read_text())
for name,duration,freq in [('contact',.55,115),('hurt',.35,160),('death',.7,95)]:
 rate=22050;rng=random.Random(812);samples=[]
 for i in range(int(rate*duration)):
  t=i/rate;e=min(1,t/.02)*max(0,1-t/duration)**1.5;phase=2*math.pi*(freq*t-35*t*t);v=e*(.45*math.sin(phase)+.16*math.sin(phase*2)+(rng.random()*2-1)*.06)*(1+.12*math.sin(t*60))
  samples.append(struct.pack('<h',int(max(-1,min(1,v))*20000)))
 dest=A/'sounds/harbor_seal'/f'{name}.ogg';dest.parent.mkdir(parents=True,exist_ok=True)
 with tempfile.NamedTemporaryFile(suffix='.wav') as tmp:
  with wave.open(tmp.name,'wb') as w:w.setparams((1,2,rate,0,'NONE','not compressed'));w.writeframes(b''.join(samples))
  subprocess.run(['ffmpeg','-y','-loglevel','error','-i',tmp.name,'-c:a','libvorbis','-fflags','+bitexact','-flags:a','+bitexact',str(dest)],check=True)
 sounds['harbor_seal.'+name]={'subtitle':'subtitles.blockbranch.harbor_seal.'+name,'sounds':['blockbranch:harbor_seal/'+name]}
(A/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n')

sounds['harbor_seal.step']={'subtitle':'subtitles.blockbranch.harbor_seal.step','sounds':[{'name':'minecraft:entity.turtle.shamble','type':'event'}]};s['subtitles.blockbranch.harbor_seal.step']='Seal shuffles'
(A/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n');lang.write_text(json.dumps(s,indent=2)+'\n')
