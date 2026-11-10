#!/usr/bin/env python3
"""Hand-drawn 16px perch overlay; bucket/water come from Minecraft at runtime.

Optional Python standard-library authoring. Three flank bars, a pale belly,
a dark eye and a short orange dorsal crest read above the native bucket rim.
"""
from pathlib import Path
import json,struct,zlib
ROWS=[
'................',
'......r.........',
'....hhffss......',
'...sehbmbms.....',
'..shhhbmbmbsrf..',
'...cccbcbcsr....',
'............rf..',
'................',
'................',
'................',
'................',
'................',
'................',
'................',
'................',
'................',
]
COLORS={'.':(0,0,0,0),'s':'546533','b':'47542e','m':'adb971','h':'c4c98a','c':'d9d29e','e':'171c15','r':'a14926','f':'d37536'}
assert len(ROWS)==16 and all(len(r)==16 for r in ROWS)
palette={k:tuple(bytes.fromhex(v))+(255,) if isinstance(v,str) else v for k,v in COLORS.items()}
data=b''.join(b'\0'+b''.join(bytes(palette[c]) for c in row) for row in ROWS)
def chunk(k,b):return struct.pack('>I',len(b))+k+b+struct.pack('>I',zlib.crc32(k+b)&0xffffffff)
a=Path(__file__).resolve().parents[2]/'src/main/resources/assets/blockbranch'
p=a/'textures/item/perch_bucket_overlay.png'
p.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(data))+chunk(b'IEND',b''))
# Only a resource identifier points to the installed game texture; no native pixels read.
model={'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/water_bucket','layer1':'blockbranch:item/perch_bucket_overlay'}}
(a/'models/item/perch_bucket.json').write_text(json.dumps(model,indent=2)+'\n')
(a/'textures/item/perch_bucket.png').unlink(missing_ok=True)
print('Generated transparent perch overlay + runtime Minecraft water-bucket model reference')
