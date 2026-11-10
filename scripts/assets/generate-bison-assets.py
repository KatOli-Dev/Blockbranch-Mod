#!/usr/bin/env python3
"""Original deterministic bison pixel coats; no third-party animal art or audio."""
from pathlib import Path
import random,struct,zlib
ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'src/main/resources/assets/blockbranch/textures/entity'
def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
def coat(calf):
    # Deer-like two-pixel clusters, with irregular quiet/full patches rather than
    # evenly distributed speckles. Neutral tonal offsets keep the existing hues.
    rng=random.Random(61317)
    clusters=[[rng.randrange(-5,6) for _ in range(64)] for _ in range(64)]
    patches=[[rng.randrange(-3,4) for _ in range(17)] for _ in range(17)]
    density=[[rng.choice((.4,.65,1,1)) for _ in range(16)] for _ in range(16)]
    # Short broken ridges suggest hair without long stripes across the cuboid.
    tuft_rng=random.Random(9813);tufts={}
    for faces,top,bottom in ((((0,18),(18,34),(34,52),(52,68)),18,29),
                             (((0,14),(14,30),(30,44),(44,60)),112,118)):
        for left,right in faces:
            for row,cy in enumerate(range(top,bottom,5)):
                for cx in range(left-2,right,5):
                    if tuft_rng.random()<.15:continue
                    sx=cx+(row%2)*2+tuft_rng.randrange(2);sy=cy+tuft_rng.randrange(-1,2)
                    mirror=tuft_rng.choice((False,True))
                    lock=((0,0,10),(1,0,18),(0,1,12),(1,1,22),(2,1,10),
                          (1,2,10),(2,2,-4),(1,3,-8))
                    for dx,dy,tone in lock:
                        px=sx+(2-dx if mirror else dx);py=sy+dy
                        if left<=px<right and top<=py<bottom:tufts[px,py]=tone
    def fur(x,y,shaggy):
        gx,gy=x//8,y//8;tx,ty=(x%8)/8,(y%8)/8
        tone=(patches[gy][gx]*(1-tx)+patches[gy][gx+1]*tx)*(1-ty)
        tone+=(patches[gy+1][gx]*(1-tx)+patches[gy+1][gx+1]*tx)*ty
        # Shaggy forequarters use elongated tufts; rump fur stays in small clusters.
        cy=y//3 if shaggy else y//2
        detail=clusters[cy][x//2]*density[gy][gx]*(.45 if shaggy and x<68 and y>=18 else 1.5 if shaggy else 1.1)
        return round(tone+detail)
    rows=[]
    for y in range(128):
        row=bytearray([0])
        for x in range(128):
            base=(119,83,54) if not calf else (183,121,63)
            variation=0;alpha=255
            if y<40 and x<68:
                base=(100,72,49) if not calf else (165,106,52)
                # Grouped highlights and dark ragged tips give the forequarter its shaggy material.
                variation += -4 if (x//4+y//3)%7==0 else 0
                if y>=30: variation-=5
                variation+=round(tufts.get((x,y),0)*(.8 if calf else 1.2))
                if 18<=y<29 and (x<4 or 48<=x<68):
                    variation+=9 if (x//2+y//3)%3 else 3
            if x<72 and 40<=y<76:
                # Smooth rump: broad upper-flank highlight and lower-belly shade.
                base=(123,87,57) if not calf else (185,124,66)
                variation += round(7-abs(y-53)*.65)
                if 66<=y<76:variation+=5
                # Sparse diagonal coat breaks within the shoulder transition.
                if x<18 and (x//3+y//4)%7==0:variation-=4
                # Sparse dark coat patches cross the join onto the front flank.
                front=21-x if x<22 else x-36 if 36<=x<58 else 99
                if y>=62 and front<8 and (front//2+y//3)%3:
                    variation-=max(0,(14 if calf else 18)-front*2)
            if x<60 and 98<=y<118:
                base=(100,72,49) if not calf else (165,106,52)
                variation=fur(x,y,True)+round(tufts.get((x,y),0)*(.8 if calf else 1.2))
                if y>=112 and (x<4 or 40<=x<60):
                    variation+=9 if (x//2+y//3)%3 else 3
            if 72<=x<120 and y<26:
                base=(80,57,40) if not calf else (164,105,54)
                variation += round(y/7)-2
                if 12<=y<18:variation+=6 if not calf else 2
                # Short stepped forehead locks stop above the eyes; cheek planes stay quiet.
                if 12<=y<15:
                    hx=(x+(y-12)//2*2)%6
                    variation+=(12 if hx<3 else -3) if y<14 else (5 if hx<2 else -2)
            if 96<=x<120 and 32<=y<46:base=(69,48,33) if not calf else (157,99,47)
            if 72<=x<100 and 32<=y<42:base=(86,68,49) if not calf else (109,80,53)
            if 76<=x<128 and 64<=y<79:
                base=(100,72,49) if not calf else (165,106,52)
                variation=fur(x,y,False)
            if (76<=x<104 and 94<=y<97) or (76<=x<108 and 104<=y<107):
                side=y<104;width=14 if side else 16;top=94 if side else 104
                strand=(x-76)%width;strand=min(strand,width-1-strand)
                tips=(2,3,2,1,2,3,3,2,1,2,3,2,2,3,1,2,3,2)
                alpha=255 if y-top<tips[strand] else 0
                base=(100,72,49) if not calf else (165,106,52)
                variation=(5 if strand%3==1 else -4)-(y-top)*3
            if 40<=x<48 and 82<=y<90:base=(191,180,147);variation=2 if x%4==0 else -1
            if x<20 and 82<=y<96:
                base=(83,60,41) if not calf else (163,105,51)
                variation=0
                if y>=92:base=(42,34,27)
            if 24<=x<32 and 91<=y<96:base=(51,38,27)
            if 56<=x<68 and 82<=y<87:base=(93,64,44) if not calf else (172,110,54)
            furry=(x<68 and y<40 or x<72 and 40<=y<76
                   or 72<=x<120 and y<26 or 72<=x<120 and 32<=y<46
                   or x<20 and 82<=y<92 or 24<=x<32 and 82<=y<91
                   or 56<=x<68 and 82<=y<87)
            if furry:variation+=fur(x,y,x<68 and y<40 or 96<=x<120 and 32<=y<46)
            # Small pale outer corners and brown irises distinguish eyes from
            # the solid dark nostrils. Side eyes sit forward of the ear planes.
            if x in (77,78,86,87,92,93,101,102) and y in (15,16):
                base=(18,14,10)
                if x in (77,86,93,102):base=(214,207,188) if y==15 else ((112,88,56) if not calf else (128,91,47))
                variation=0
            if x in (77,78,83,84) and y in (38,39):base=(32,25,18);variation=0
            row.extend((*[max(0,min(255,c+variation)) for c in base],alpha))
        rows.append(bytes(row))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',128,128,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows)))+chunk(b'IEND',b'')
if __name__=='__main__':
    ASSETS.mkdir(parents=True,exist_ok=True)
    for calf in (False,True):(ASSETS/('american_bison'+('_calf' if calf else '')+'.png')).write_bytes(coat(calf))
