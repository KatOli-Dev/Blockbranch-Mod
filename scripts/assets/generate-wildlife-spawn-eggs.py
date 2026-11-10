#!/usr/bin/env python3
"""Original 16px species-marked spawn eggs; no vanilla pixels or animal coats copied.

Hand-authored silhouettes, directional shading and species cues follow the installed
26.3 item vocabulary. The grids are intentionally distinct, not a recolored mask.
Use --perch-only, --bear-only, --seal-only, --beaver-only, --deer-only, --bison-only, --boar-only or --mallard-only to write one icon; optional Python standard library only.
"""
from pathlib import Path
import argparse
import struct,zlib
ROOT=Path(__file__).resolve().parents[2]/'src/main/resources/assets/blockbranch/textures/item'
# Each material has a deep outline, shadow, midtone, light and small highlight.
PALETTE={
 '.':(0,0,0,0),
 'k':(33,26,23,255),'d':(58,39,30,255),'s':(85,55,37,255),
 'b':(115,75,46,255),'t':(146,99,59,255),'l':(172,124,77,255),'h':(196,151,98,255),
 'q':(129,103,76,255),'a':(173,146,107,255),'c':(207,185,143,255),'w':(237,222,183,255),'W':(252,242,211,255),
 'r':(127,66,36,255),'R':(162,88,43,255),'o':(199,116,46,255),'O':(236,157,64,255),'y':(249,194,90,255),
 'g':(24,63,40,255),'G':(34,98,52,255),'j':(55,135,67,255),'J':(83,164,85,255),'v':(121,171,98,255),
 'f':(71,86,44,255),'F':(106,122,59,255),'i':(143,151,76,255),'I':(174,176,99,255),'e':(198,193,124,255),
 'z':(63,76,80,255),'n':(93,108,111,255),'m':(130,144,146,255),'u':(166,177,177,255),'p':(195,204,199,255),'P':(219,226,217,255),
 'B':(29,77,111,255),'C':(47,121,161,255),'D':(83,165,196,255),
 'M':(179,157,130,255),'N':(143,120,95,255),
 # Desaturated snout planes match the boar's earthy muzzle.
 'Q':(104,89,72,255),'U':(137,119,94,255),'V':(171,149,116,255),
 # Warm charcoal paddle tones, distinct from the beaver's short brown fur.
 'A':(43,39,31,255),'E':(66,59,45,255),'K':(82,73,55,255),'H':(101,90,68,255),
}
GRIDS={
'wild_boar':[
'................','......sssd......','....ssbbbss.....','...sbtttbbbs....',
'..sbtllttbbbs...','..sbtltttbbbs...','..sbbttbbbsbs...','..sbakbssbkas...',
'..sbbssbbssbs...','..sbQUVVVUQbs...','.cWbQUkUkUQbWc..','..cbsQUUUQsbw...',
'...dsQQQQQsd....','....ddsssdd.....','.....ddddd......','................'],
'red_deer':[
'................','..a..a....a..a..','..ca.ac..ca.ac..','...ccac..cacc...',
'....cac..cac....','.....ca..ac.....','.sbbhlhllhlhbbs.','..sqblttttlbqs..',
'....blkttklb....','....blttttlb....','....bltcctlb....','.....blcclb.....',
'.....acwwca.....','.....qqkkqq.....','......ssss......','................'],
'american_bison':[
'................','................','......sdds......','.....sbbdds.....',
'.....sbtbdds....','.c..asbbssbbss..','.Wc.cabssbbtbs..','..Wcsddssbbbbbss',
'..dkcdddssbbbbsd','.dqqddddssbbbbsd','.kqqsddddssbbbsd','..ddkddddddddss.',
'...kkdsd..sdks..','.....kkk..kkk...','................','................'],
'brown_bear':[
'................','...ss...........','..btbs....sss...','..sbttbbbbtbs...',
'..blltttttttbs..','..blhlltttttbs..','..blttttttttbs..','..blktbbtbklbs..',
'..bttbMMMMMMbs..','..sttMMkkkMMbs..','..sttMNNkNNMbs..',
'..sbttMNNNNMbs..','...sbttMMMtbs...','....sbbttbbs....','.....ssssss.....','................'],
'mallard':[
'................','...ggg..........','..gJJjG.........','..GJkjG.........',
'.yOOjjG.........','.oOOGjG.........','....GWG.........','....cWc..nnn....',
'...rRtRnnuppnz..','..rRttRmuummnz..','..sRttRnwCBmnz..','...sRrnnmBBnz...',
'....snnnnnnz....','.....o..o.......','....OO.OO.......','................'],
'eurasian_beaver':[
'................','................','..ss............','..bbs...........',
'.btlbs.ssss.....','.bllttbttttbs...','sbkttttlllttbs..','sNqtttttllttbs..',
'kNMMtttttttbbs..','.sWWbtbtttbbss..','..wwbsbbbssAEEA.','....ssbbsAEHHKEA',
'....ss..sAKEKEAA','.........sAAAAs.','................','................'],
'european_perch':[
'................','................','......r.r.......','.....roOor......',
'....rOoOoRf.....','...fIIeIIeiif...','rO.fIFIFIeekIf..','.roFIFIFIFeeif..',
'.roFIFIFIFiIiff.','rO.fIeeeweeeif..','....fieeeeif....','.....fFooFf.....',
'..BBCCieeCCDBB..','..BBCCDCDBDBB...','...BBCCCCBBB....','................'],
'harbor_seal':[
'................','................','...nnn..........','..nuppn.........',
'..mupkn.........','.nppuum.........','.zppuunmmn......','..npuumuppumn...',
'...nummunmmum.n.','....nmmuuummnmn.','...nmmnnnnmmnzn.','..nmmn....nmmnz.',
'..BDnnBCCnnDCB..','...BCCCDCDCCB...','....BBBBBBB.....','................'],
}
def png(path,grid):
 assert len(grid)==16,(path,len(grid))
 assert all(len(row)==16 for row in grid),(path,[(i,len(row),row)for i,row in enumerate(grid)if len(row)!=16])
 def chunk(k,b):return struct.pack('>I',len(b))+k+b+struct.pack('>I',zlib.crc32(k+b)&0xffffffff)
 data=b''.join(b'\0'+b''.join(bytes(PALETTE[c]) for c in row)for row in grid)
 path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(data))+chunk(b'IEND',b''))
def main():
 parser=argparse.ArgumentParser(description=__doc__)
 selection=parser.add_mutually_exclusive_group()
 selection.add_argument('--perch-only',action='store_true',help='Write only the European perch icon')
 selection.add_argument('--bear-only',action='store_true',help='Write only the brown bear icon')
 selection.add_argument('--seal-only',action='store_true',help='Write only the harbor seal icon')
 selection.add_argument('--beaver-only',action='store_true',help='Write only the Eurasian beaver icon')
 selection.add_argument('--deer-only',action='store_true',help='Write only the red deer icon')
 selection.add_argument('--bison-only',action='store_true',help='Write only the American bison icon')
 selection.add_argument('--boar-only',action='store_true',help='Write only the wild boar icon')
 selection.add_argument('--mallard-only',action='store_true',help='Write only the mallard icon')
 args=parser.parse_args()
 for name,grid in GRIDS.items():
  assert len(grid)==16 and all(len(row)==16 for row in grid),(name,[(i,len(row)) for i,row in enumerate(grid)])
 selected=['european_perch'] if args.perch_only else ['brown_bear'] if args.bear_only else ['harbor_seal'] if args.seal_only else ['eurasian_beaver'] if args.beaver_only else ['red_deer'] if args.deer_only else ['american_bison'] if args.bison_only else ['wild_boar'] if args.boar_only else ['mallard'] if args.mallard_only else GRIDS
 for name in selected:png(ROOT/f'{name}_spawn_egg.png',GRIDS[name])
if __name__=='__main__':main()
