#!/usr/bin/env python3
"""Original restrained brown-bear coats for the ten-cuboid model."""
from pathlib import Path
import random,struct,zlib
ROOT=Path(__file__).resolve().parents[2]
DEST=ROOT/'src/main/resources/assets/blockbranch/textures/entity'
def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
def coat(cub):
    tones=((128,92,60),(146,106,69),(157,115,75),(170,128,84)) if cub else ((96,70,48),(113,82,55),(124,91,61),(135,101,68))
    flank=("111122221111","112232221100","122333221100","122332221110","112222211111","111221111111","111111111111")
    rear=("1122211","1223221","1233321","1223321","1122221","1112211","1111111")
    def tufts(points):
        marks={}
        for x,y,length in points:
            marks[x-1,y]=12
            for dy in range(length):marks[x,y+dy]=-24
            if length==3:marks[x+1,y+length-1]=-14
        return marks
    flank_tufts=tufts(((3,4,2),(6,7,2),(8,2,2),(10,5,3),(13,3,2),
                       (15,8,2),(17,5,2),(20,2,3),(21,7,2)))
    rear_tufts=tufts(((2,3,2),(6,2,2),(9,5,2),(4,8,2),(8,10,2)))
    rng=random.Random(7324)
    clusters=[[rng.randrange(-5,6) for _ in range(64)] for _ in range(48)]
    patches=[[rng.randrange(-3,4) for _ in range(17)] for _ in range(13)]
    density=[[rng.choice((.4,.7,1,1)) for _ in range(16)] for _ in range(12)]
    def fur(x,y):
        # Coherent short fur with changing density; no repeating speckle lattice.
        gx,gy=x//8,y//8;tx,ty=(x%8)/8,(y%8)/8
        tone=(patches[gy][gx]*(1-tx)+patches[gy][gx+1]*tx)*(1-ty)
        tone+=(patches[gy+1][gx]*(1-tx)+patches[gy+1][gx+1]*tx)*ty
        return round(tone+clusters[y//2][x//2]*density[gy][gx]*1.3)
    rows=[]
    for y in range(96):
        row=bytearray([0])
        for x in range(128):
            base=(116,83,55) if not cub else (151,108,68)
            if x<76 and 24<=y<38:base=(104,76,51) if not cub else (138,100,65)
            if x<42 and 40<=y<52:base=(126,98,67) if not cub else (157,117,77)
            if 80<=x<120 and y<20:base=(105,77,52) if not cub else (142,105,67)
            if 80<=x<104 and 24<=y<33:base=(146,111,75) if not cub else (184,145,99)
            if x<22 and 56<=y<70:
                base=(103,74,50) if not cub else (135,97,61)
                if y>=66:base=(69,52,37)
            if 112<=x<124 and 24<=y<30:base=(85,62,44) if not cub else (123,89,58)
            if 114<=x<117 and 27<=y<29:base=(151,117,86)
            v=0
            # Anatomy-sized dorsal shade, rounded flank highlight and pale lower coat.
            if 24<=y<38 and (x<24 or 38<=x<62):
                coat_row=y-24;v=round(7-abs(coat_row-5)*1.7)
                if coat_row>=10:v+=5
                # Opposing flank UVs run in reverse; two-pixel clusters carry
                # coat detail without adding contours to the deliberately simple mesh.
                along=x if x<24 else 61-x
                base=tones[int(flank[coat_row//2][along//2])]
                if along<7 and (along//3+coat_row//3)%5==0:v-=4
            if 62<=x<76 and 24<=y<38:base=tones[int(rear[(y-24)//2][(x-62)//2])]
            if cub and 30<=y<36 and abs(x-31)<=(35-y)//2+1:base=(172,130,85)
            if 24<=x<38 and y<24:v=-5+round(4*abs(x-31)/7)
            if 80<=x<120 and 10<=y<19:v=round((y-10)*.7)-3
            if 80<=x<104 and 28<=y<33:v=round((y-28)*1.2)
            if x<42 and 40<=y<52:v=-3 if y<49 else 2
            furry=(x<76 and y<38 or x<42 and 40<=y<52
                   or 80<=x<120 and y<20 or 80<=x<104 and 24<=y<33
                   or x<22 and 56<=y<66 or 112<=x<124 and 24<=y<30)
            if furry:v+=fur(x,y)
            # Sparse connected strokes give fur direction without a speckle lattice.
            if 24<=y<38 and (x<24 or 38<=x<62):v+=flank_tufts.get((x if x<24 else 61-x,y-24),0)
            if 62<=x<76 and 24<=y<38:v+=rear_tufts.get((x-62,y-24),0)
            if ((x in (84,85,104,105) and y in (12,13)) or (x in (91,98) and y==12)):base=(18,15,12);v=0
            if (x in (84,105) and y==12) or (x in (91,98) and y==11):base=(170,137,94) if not cub else (202,161,108);v=0
            if 86<=x<89 and 28<=y<30:base=(38,30,24);v=0
            if 86<=x<89 and y==31:base=(75,53,37);v=0
            if x in (6,8,10) and y==68:base=(184,169,137);v=0
            row.extend((*[max(0,min(255,c+v)) for c in base],255))
        rows.append(bytes(row))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',128,96,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(rows)))+chunk(b'IEND',b'')
if __name__=='__main__':
    DEST.mkdir(parents=True,exist_ok=True)
    for cub in (False,True):(DEST/('brown_bear'+('_cub' if cub else '')+'.png')).write_bytes(coat(cub))
