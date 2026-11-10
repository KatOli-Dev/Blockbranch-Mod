#!/usr/bin/env python3
"""Original restrained beaver coats and short contact/hurt/death chirps.

--feet-only updates only the dedicated foot region after verifying all protected
coat pixels. It uses the standard library and never writes audio or gameplay data.
"""
from pathlib import Path
import json,random,math,struct,zlib,wave,subprocess,tempfile,sys
ROOT=Path(__file__).resolve().parents[2];A=ROOT/'src/main/resources/assets/blockbranch';D=ROOT/'src/main/resources/data/blockbranch'
def chunk(k,b):return struct.pack('>I',len(b))+k+b+struct.pack('>I',zlib.crc32(k+b)&0xffffffff)
def coat(kit):
 # Clustered short fur; paddle and eye pixels occupy explicit atlas regions.
 rng=random.Random(6193)
 clusters=[[rng.randrange(-5,6) for _ in range(64)] for _ in range(32)]
 patches=[[rng.randrange(-3,4) for _ in range(17)] for _ in range(9)]
 density=[[rng.choice((.4,.7,1,1)) for _ in range(16)] for _ in range(8)]
 def fur(x,y):
  gx,gy=x//8,y//8;tx,ty=(x%8)/8,(y%8)/8
  tone=(patches[gy][gx]*(1-tx)+patches[gy][gx+1]*tx)*(1-ty)
  tone+=(patches[gy+1][gx]*(1-tx)+patches[gy+1][gx+1]*tx)*ty
  return round(tone+clusters[y//2][x//2]*density[gy][gx]*1.2)
 rows=[]
 for y in range(64):
  row=bytearray([0])
  for x in range(128):
   base=(116,82,49) if not kit else (144,108,69);shade=0
   if y<12 and 12<=x<20:shade=-7
   if 12<=y<18 and x<40:shade=round(5-abs(y-14)*3)+(3 if y>=16 else 0)
   if 40<=x<64 and 6<=y<11:shade=round((y-6)*1.2)
   if 40<=x<56 and 16<=y<21:base=(151,119,80) if not kit else (170,137,94)
   furry=(x<40 and y<24 or 80<=x<96 and y<8 or 40<=x<64 and y<11
          or 40<=x<56 and 16<=y<21 or 68<=x<76 and y<4
          or 32<=x<46 and 24<=y<28)
   if furry:shade+=fur(x,y)
   if 7<=y<13 and (4<=x<7 or 15<=x<18):shade-=7
   if 11<=y<13 and (x<7 or 15<=x<22):shade+=7
   if x in (44,45,52,53) and y==7:base=(20,17,12);shade=0
   if x in (47,50) and y==7:base=(20,17,12);shade=0
   if x in (44,46,51,53) and y==7:base=(205,193,162);shade=0
   if 43<=x<47 and y==18:base=(40,31,22);shade=0
   if x in (44,46) and y==20:base=(211,162,72);shade=0
   if 68<=x<76 and y<4:base=(78,58,38)
   if x<32 and 24<=y<36:
    base=(66,59,45)
    shade=-10 if (x+y)%4==0 or (x-y)%4==0 else 3
   if 32<=x<46 and 24<=y<30:
    # Broad planes separate the webbed foot from the shaded belly; no toe detail.
    base=(88,67,46);shade=0
    if 36<=x<39 and y<28:base=(99,77,51)
    if 39<=x<42 and y<28:base=(60,43,28)
    if y==28:base=(95,73,49)
    if y==29:base=(78,56,36)
   if x<40 and 12<=y<18 and (x//3+y//2)%7==0:shade-=3
   row.extend((*[max(0,min(255,v+shade)) for v in base],255))
  rows.append(bytes(row))
 return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',128,64,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows)))+chunk(b'IEND',b'')
def generated_pixels(data):
 # This authoring tool's RGBA outputs deliberately use unfiltered scanlines.
 assert data[:8]==b'\x89PNG\r\n\x1a\n',"PNG input required"
 payload=[];i=8;size=None
 while i<len(data):
  length=struct.unpack('>I',data[i:i+4])[0];kind=data[i+4:i+8];body=data[i+8:i+8+length]
  assert zlib.crc32(kind+body)&0xffffffff==struct.unpack('>I',data[i+8+length:i+12+length])[0],"PNG CRC mismatch"
  if kind==b'IHDR':size=struct.unpack('>IIBBBBB',body)
  if kind==b'IDAT':payload.append(body)
  i+=12+length
 assert size==(128,64,8,6,0,0,0),"Expected generated 128x64 RGBA coat"
 raw=zlib.decompress(b''.join(payload));assert len(raw)==64*513
 assert all(raw[y*513]==0 for y in range(64)),"Feet-only mode expects unfiltered generated scanlines"
 return b''.join(raw[y*513+1:(y+1)*513] for y in range(64))
outputs=[]
for kit in (False,True):
 p=A/'textures/entity'/('eurasian_beaver'+('_kit' if kit else '')+'.png');data=coat(kit)
 if '--feet-only' in sys.argv:
  before=generated_pixels(p.read_bytes());after=generated_pixels(data)
  for y in range(64):
   for x in range(128):
    if 32<=x<46 and 24<=y<30:continue
    offset=(y*128+x)*4
    assert before[offset:offset+4]==after[offset:offset+4],f"Protected coat pixel differs at {(x,y)}; refusing to overwrite {p}"
 outputs.append((p,data))
for p,data in outputs:p.write_bytes(data)
if '--coats-only' in sys.argv or '--feet-only' in sys.argv:sys.exit(0)
loot=json.loads((D/'loot_table/entities/brown_bear.json').read_text());loot['random_sequence']='blockbranch:entities/eurasian_beaver';(D/'loot_table/entities/eurasian_beaver.json').write_text(json.dumps(loot,indent=2)+'\n')
lang=A/'lang/en_us.json';s=json.loads(lang.read_text());s['entity.blockbranch.eurasian_beaver']='Eurasian Beaver'
for name,label in [('contact','Beaver chirps'),('hurt','Beaver hurts'),('death','Beaver dies')]:s['subtitles.blockbranch.eurasian_beaver.'+name]=label
lang.write_text(json.dumps(s,indent=2)+'\n');sounds=json.loads((A/'sounds.json').read_text())
for name,duration,freq in [('contact',.42,380),('hurt',.28,510),('death',.65,300)]:
 rate=22050;rng=random.Random(812);samples=[]
 for i in range(int(rate*duration)):
  t=i/rate;e=min(1,t/.02)*max(0,1-t/duration)**1.5;phase=2*math.pi*(freq*t-90*t*t);v=e*(.45*math.sin(phase)+.16*math.sin(phase*2)+(rng.random()*2-1)*.06)*(1+.12*math.sin(t*60))
  samples.append(struct.pack('<h',int(max(-1,min(1,v))*20000)))
 dest=A/'sounds/eurasian_beaver'/f'{name}.ogg';dest.parent.mkdir(parents=True,exist_ok=True)
 with tempfile.NamedTemporaryFile(suffix='.wav') as tmp:
  with wave.open(tmp.name,'wb') as w:w.setparams((1,2,rate,0,'NONE','not compressed'));w.writeframes(b''.join(samples))
  subprocess.run(['ffmpeg','-y','-loglevel','error','-i',tmp.name,'-c:a','libvorbis','-fflags','+bitexact','-flags:a','+bitexact',str(dest)],check=True)
 sounds['eurasian_beaver.'+name]={'subtitle':'subtitles.blockbranch.eurasian_beaver.'+name,'sounds':['blockbranch:eurasian_beaver/'+name]}
(A/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n')
