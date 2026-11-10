#!/usr/bin/env python3
"""Perch entity art and food/bucket resources; preserves authored food icons."""
from pathlib import Path
import json,struct,zlib,subprocess,sys
ROOT=Path(__file__).resolve().parents[2];A=ROOT/'src/main/resources/assets/blockbranch';D=ROOT/'src/main/resources/data/blockbranch'
def png_bytes(w,h,pixel):
 def chunk(k,b):return struct.pack('>I',len(b))+k+b+struct.pack('>I',zlib.crc32(k+b)&0xffffffff)
 data=b''.join(b'\0'+b''.join(bytes(pixel(x,y)) for x in range(w)) for y in range(h))
 return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(data))+chunk(b'IEND',b'')
def png(path,w,h,pixel):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(png_bytes(w,h,pixel))
def coat(x,y,land=False):
 c=(137,146,77)
 if y<6:c=(83,105,57)
 if 6<=y<9 and x<16:
  c=(183,178,117) if y==8 else (127,141,70)
  if x in (0,2,4,9,11,13):c=(132,143,79) if y==8 else (58,77,43)
  elif y==6:c=(105,124,62)
  elif y==7 and x in (1,5,8,12):c=(148,155,82)
 if 20<=x<28 and 2<=y<5:c=(148,151,81)
 if x in (21,24) and y==2:c=(14,18,12)
 if 20<=x<28 and y==4:c=(182,174,110)
 # A restrained two-pixel mouth gives the narrow front a readable lower edge.
 if 22<=x<24 and y==3:c=(102,111,65)
 if y>=16:c=(178,81,42) if x<10 or x>=20 else (80,100,56)
 # Opposing faces of each zero-width fin overlap. Mirror both masks so the
 # rear face cannot fill a transparent pixel in the visible face.
 if 19<=y<23 and 0<=x<6:
  column=2-x if x<3 else x-3
  row=y-19
  if (column==0 and row in (0,3)) or (column==2 and row==2):return (0,0,0,0)
  c=(225,126,58) if row in (0,3) else (193,87,35)
 if 20<=y<22 and 10<=x<18:
  column=13-x if x<14 else x-14
  if y==20 and column in (0,1):return (0,0,0,0)
  c=(70,91,49) if y==20 else (112,128,65)
 if 19<=y<20 and 20<=x<26:
  column=22-x if x<23 else x-23
  if column==2:return (0,0,0,0)
  c=(213,106,43)
 # Center the land eye inside the tiny side face; a pale upper rim separates
 # it from the dark dorsal edge when the fish lies flat. Swimming stays exact.
 if land and x in (21,24) and y==2:c=(204,198,128)
 if land and x in (21,24) and y==3:c=(12,17,10)
 return (*c,255)
if '--land-coat-only' in sys.argv:
 source=A/'textures/entity/european_perch.png'
 if source.read_bytes()!=png_bytes(64,32,coat):raise SystemExit('Accepted perch source coat differs from authoring input')
 png(A/'textures/entity/european_perch_land.png',64,32,lambda x,y:coat(x,y,True))
 sys.exit(0)
png(A/'textures/entity/european_perch.png',64,32,coat)
png(A/'textures/entity/european_perch_land.png',64,32,lambda x,y:coat(x,y,True))
# Isolate optional coat authoring from gameplay resources, food icons and audio.
if '--coats-only' in sys.argv:sys.exit(0)
for kind in ['perch','cooked_perch']:
 # Preserve the checked-in food sprites here. Their explicit pixel grids and
 # palettes are maintained separately in perch_food_item.py.
 for folder,data in [('items',{'model':{'type':'minecraft:model','model':f'blockbranch:item/{kind}'}}),('models/item',{'parent':'minecraft:item/generated','textures':{'layer0':f'blockbranch:item/{kind}'}})]:
  p=A/folder/f'{kind}.json';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
for kind,ticks in [('smelting',200),('smoking',100),('campfire_cooking',600)]:
 p=D/'recipe'/f'cooked_perch_from_{kind}.json';p.write_text(json.dumps({'type':f'minecraft:{kind}','category':'food','cookingtime':ticks,'experience':.35,'ingredient':'blockbranch:perch','result':{'id':'blockbranch:cooked_perch'}},indent=2)+'\n')
loot=json.loads((D/'loot_table/entities/mallard.json').read_text());loot['random_sequence']='blockbranch:entities/european_perch';loot['pools']=loot['pools'][:1];loot['pools'][0]['entries'][0]['name']='blockbranch:perch';loot['pools'][0]['entries'][0]['modifier']=[m for m in loot['pools'][0]['entries'][0]['modifier'] if m['type']!='minecraft:enchanted_count_increase'];(D/'loot_table/entities/european_perch.json').write_text(json.dumps(loot,indent=2)+'\n')
p=A/'lang/en_us.json';lang=json.loads(p.read_text());lang.update({'entity.blockbranch.european_perch':'European Perch','item.blockbranch.perch':'Raw Perch','item.blockbranch.cooked_perch':'Cooked Perch','item.blockbranch.perch_bucket':'Bucket of Perch'});p.write_text(json.dumps(lang,indent=2)+'\n')

for tag,values in [('fishes',['blockbranch:perch','blockbranch:cooked_perch']),('cat_food',['blockbranch:perch']),('ocelot_food',['blockbranch:perch'])]:
 p=ROOT/'src/main/resources/data/minecraft/tags/item'/(tag+'.json');p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps({'replace':False,'values':values},indent=2)+'\n')

sounds=json.loads((A/'sounds.json').read_text());lang=json.loads((A/'lang/en_us.json').read_text())
for name,label in [('hurt','Perch hurts'),('death','Perch dies'),('flop','Perch flops')]:
 sounds['european_perch.'+name]={'subtitle':'subtitles.blockbranch.european_perch.'+name,'sounds':[{'name':'minecraft:entity.cod.'+name,'type':'event'}]};lang['subtitles.blockbranch.european_perch.'+name]=label
(A/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n');(A/'lang/en_us.json').write_text(json.dumps(lang,indent=2)+'\n')

# Keep the separately generated bucket sprite on regeneration.
subprocess.run([sys.executable,str(ROOT/"scripts/assets/generate-perch-bucket-icon.py")],check=True)
