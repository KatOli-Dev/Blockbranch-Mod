#!/usr/bin/env python3
"""Deterministic original mallard coats, food icons and short voiced quacks."""
from pathlib import Path
import json,random,math,struct,zlib,wave,subprocess,tempfile,sys
from duck_food_item import write_duck_food
ROOT=Path(__file__).resolve().parents[2];A=ROOT/'src/main/resources/assets/blockbranch';D=ROOT/'src/main/resources/data/blockbranch'
def png(path,w,h,pixel):
 def chunk(k,b):return struct.pack('>I',len(b))+k+b+struct.pack('>I',zlib.crc32(k+b)&0xffffffff)
 rows=b''.join(b'\0'+b''.join(bytes(pixel(x,y)) for x in range(w)) for y in range(h));path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(rows))+chunk(b'IEND',b''))
# Atlas regions follow the unchanged seven-cuboid model. Feather detail stays in pixels.
HEN_TONES=((103,73,43),(132,96,56),(157,120,74),(179,143,94))
DRAKE_TONES=((117,123,110),(139,143,126),(158,158,138),(176,173,148))
HEN_FLANK=("222222222222","212232122321","101221012210","221012221012","132221132221","221122211222")
HEN_BACK=("22222222","22322232","21012210","22122222","23222322","10122101",
          "22212222","22322232","21012210","22222222","23222322","12122121")
WING=("dddwdgdgd","kdbbbdgdd","kkbBbbdgd","dkwwgdggd")
def coat_pixel(variant,x,y):
 drake=variant=='drake';hen=variant=='hen';young=variant=='duckling'
 c=(143,143,125) if drake else (143,106,65) if hen else (215,190,93)
 # Body sides and back: paired coarse feather clusters, not a periodic checker.
 if x<40 and y<18:
  if hen:
   if 12<=x<20 and y<12:c=HEN_TONES[int(HEN_BACK[y][x-12])]
   if 12<=y<18 and (x<12 or 20<=x<32):
    along=x if x<12 else 31-x;c=HEN_TONES[int(HEN_FLANK[y-12][along])]
  elif drake:
   if 12<=x<20 and y<12:
    c=DRAKE_TONES[int(HEN_BACK[y][x-12])]
   if 12<=y<18 and (x<12 or 20<=x<32):
    along=x if x<12 else 31-x;c=(148,149,131) if y<16 else (172,170,146)
    if (along,y-12) in ((2,2),(5,1),(8,3),(10,2)):c=(114,120,109)
   if 12<=x<20 and 12<=y<18:c=(111,67,46) if y<16 else (126,79,53)
   if 32<=x<40 and 12<=y<18:c=(49,56,49) if y<16 else (178,175,150)
  else:
   if 12<=x<20 and y<12:
    if 13+(y%4==0)<=x<19-(y%5==0) and 1<=y<11:c=(111,94,48)
   if 12<=y<16 and (x<12 or 20<=x<32):
    along=x if x<12 else 31-x
    if along<10-(y-12)//2:c=(153,128,61)
 # Wing side UVs run in opposite directions. The blue speculum occupies only
 # the rear feather group; forewing coverts retain gray or mottled brown.
 if 40<=x<60 and y<13:
  c=(134,137,121) if drake else (151,114,68) if hen else (132,112,55)
  if 9<=y<13 and (x<49 or 50<=x<59):
   along=x-40 if x<49 else 58-x
   if young:
    c=(143,121,57) if (along+y)%4 else (181,155,72)
   else:
    code=WING[y-9][along]
    palette={'d':(141,144,127) if drake else (151,114,68),
             'g':(111,120,112) if drake else (110,77,44),
             'k':(45,55,56),'b':(47,72,139),'B':(65,91,159),'w':(218,213,184)}
    c=palette[code]
 # Head: side eye stripes stop before the forehead, leaving visible front eyes.
 if x<20 and 20<=y<31:
  c=(27,96,58) if drake else (153,117,72) if hen else (221,192,91)
  if drake:
   if y<25:c=(25,78,49)
   if 27<=y<30:c=(34,108,65)
   if y==30:c=(224,220,190)
  elif hen:
   if y<25:c=(124,89,51) if (x+y)%4 else (160,123,75)
   if y in (27,28):c=(174,139,88)
   if y==25 and (2<=x<13):c=(199,161,106)
   if y==26 and (x<3 or 12<=x<15):c=(91,63,37)
   if y==27 and (x<5 or 10<=x<15):c=(193,155,100)
   if 15<=x<20 and 25<=y<29:c=(121,86,47)
  else:
   if 5<=x<10 and 20<=y<25:c=(103,87,45) if 6<=x<9 and 21<=y<24 else (221,192,91)
   if y==25 and (x<2 or 13<=x<20):c=(116,96,48)
   if y==26 and (x<2 or 14<=x<15):c=(95,78,39)
   if 15<=x<20 and 25<=y<29:c=(118,98,49)
   if 5<=x<10 and 25<=y<30:c=(238,207,109)
  # Each eye is one connected cluster across the front/side cube seam.
  if y==26 and x in (3,4,5,9,10,11):c=(19,20,14)
  if not young and y==26 and x in (3,6,8,11):c=(168,172,108) if drake else (194,153,96)
 # Bills have a small dark nostril and a darker underside, rather than a flat slab.
 if 24<=x<38 and 20<=y<25:
  c=(220,183,54) if drake else (172,123,55) if hen else (163,129,60)
  if 31<=x<35 and y<23:c=(174,142,39) if drake else (129,87,40) if hen else (131,102,44)
  if y==24:c=(183,145,40) if drake else (139,94,40) if hen else (141,105,45)
  if (x,y) in ((28,21),(30,21)):c=(156,137,45) if drake else (121,86,40) if hen else (126,103,45)
 if x<10 and 34<=y<39:c=(209,119,32) if not young else (183,132,49)
 n=random.Random(x//2*413+y//2*739).randrange(-4,5)
 return (*[max(0,min(255,v+n)) for v in c],255)
for variant in ['drake','hen','duckling']:
 png(A/'textures/entity'/('mallard_'+variant+'.png'),64,64,lambda x,y:coat_pixel(variant,x,y))
if '--coats-only' in sys.argv:sys.exit(0)
for cooked in [False,True]:
 name='cooked_duck' if cooked else 'duck'
 write_duck_food(cooked,A/'textures/item'/f'{name}.png')
 for folder,data in [('items',{'model':{'type':'minecraft:model','model':f'blockbranch:item/{name}'}}),('models/item',{'parent':'minecraft:item/generated','textures':{'layer0':f'blockbranch:item/{name}'}})]:
  p=A/folder/f'{name}.json';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
for kind,ticks in [('smelting',200),('smoking',100),('campfire_cooking',600)]:
 p=D/'recipe'/f'cooked_duck_from_{kind}.json';p.write_text(json.dumps({'type':f'minecraft:{kind}','category':'food','cookingtime':ticks,'experience':.35,'ingredient':'blockbranch:duck','result':{'id':'blockbranch:cooked_duck'}},indent=2)+'\n')
loot=json.loads((D/'loot_table/entities/red_deer.json').read_text());loot['random_sequence']='blockbranch:entities/mallard';loot['pools'][0]['entries'][0]['name']='blockbranch:duck';loot['pools'][0]['entries'][0]['modifier'][0]['count']={'min':1,'max':1,'type':'minecraft:uniform'};loot['pools'][1]['entries'][0]['name']='minecraft:feather';(D/'loot_table/entities/mallard.json').write_text(json.dumps(loot,indent=2)+'\n')
lang=A/'lang/en_us.json';s=json.loads(lang.read_text());s.update({'entity.blockbranch.mallard':'Mallard','item.blockbranch.duck':'Raw Duck','item.blockbranch.cooked_duck':'Cooked Duck','subtitles.blockbranch.mallard.quack':'Mallard quacks','subtitles.blockbranch.mallard.hurt':'Mallard hurts','subtitles.blockbranch.mallard.death':'Mallard dies'});lang.write_text(json.dumps(s,indent=2)+'\n')
sounds=json.loads((A/'sounds.json').read_text())
for name,duration,freq in [('quack',.52,180),('hurt',.3,240),('death',.65,150)]:
 rate=22050;rng=random.Random(721);samples=[]
 for i in range(int(rate*duration)):
  t=i/rate;envelope=min(1,t/.025)*max(0,1-t/duration)**1.5;f=freq*(1-.3*t/duration)
  voiced=sum(math.sin(2*math.pi*f*t*k)/k for k in range(1,9));v=envelope*(voiced*.24+(rng.random()*2-1)*.12)*(1+.2*math.sin(t*45))
  samples.append(struct.pack('<h',int(max(-1,min(1,v)) * 20000)))
 dest=A/'sounds/mallard'/f'{name}.ogg';dest.parent.mkdir(parents=True,exist_ok=True)
 with tempfile.NamedTemporaryFile(suffix='.wav') as tmp:
  with wave.open(tmp.name,'wb') as w:w.setparams((1,2,rate,0,'NONE','not compressed'));w.writeframes(b''.join(samples))
  subprocess.run(['ffmpeg','-y','-loglevel','error','-i',tmp.name,'-c:a','libvorbis','-fflags','+bitexact','-flags:a','+bitexact',str(dest)],check=True)
 sounds[f'mallard.{name}']={'subtitle':f'subtitles.blockbranch.mallard.{name}','sounds':[f'blockbranch:mallard/{name}']}
(A/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n')
