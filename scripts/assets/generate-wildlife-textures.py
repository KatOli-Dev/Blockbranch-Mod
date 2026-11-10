#!/usr/bin/env python3
"""Generate native-resolution boar coats with model UV details.

--piglet-only or --adult-only writes just that age's PNG; standard library only.
"""
from pathlib import Path
import random
import struct
import sys
import zlib

DESTINATION = Path(__file__).resolve().parents[2] / "src/main/resources/assets/blockbranch/textures/entity"


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)


# Rows follow the back from rump to shoulder; columns cross its ten-texel width.
# Tapered lanes carry over the rump without tracing a rectangular border.
BACK_STRIPES = (
    '.s..ss..s.',
    '.s..ss..s.',
    '.s..ss..s.',
    '.s..ss..s.',
    '.ss.ss.ss.',
    '..s.ss.s..',
    '..s.ss.s..',
    '..s.s..s..',
    '..s.ss.s..',
    '.s..ss..s.',
    '.s..ss..s.',
    '.s..ss..s.',
    '....ss..s.',
    '....s.....',
    '..........',
    '..........',
)
FLANK_STRIPES = (
    '................',
    '.....sss........',
    '.ssssssssssssss.',
    '...ssss...sss...',
    '.......ss.......',
    '..sssssssssss...',
    '....ss...ss.....',
    '................',
)
RUMP_STRIPES = (
    '.s..ss..s.',
    '.s..ss..s.',
    '..s.ss.s..',
    '..s.s..s..',
    '....s.....',
    '..........',
    '..........',
    '..........',
)
HEAD_STRIPE = (
    '...ss...',
    '...ss...',
    '...s....',
    '...ss...',
    '...ss...',
    '....s...',
    '...ss...',
    '...ss...',
)
assert len(RUMP_STRIPES)==8 and all(len(row)==10 for row in RUMP_STRIPES)
assert len(HEAD_STRIPE)==8 and all(len(row)==8 for row in HEAD_STRIPE)
assert len(BACK_STRIPES)==16 and all(len(row)==10 for row in BACK_STRIPES)
assert len(FLANK_STRIPES)==8 and all(len(row)==16 for row in FLANK_STRIPES)


def piglet_body_stripe(x, y):
    """Lengthwise lanes on the five exposed faces of the 10×8×16 body UV net."""
    if 16 <= x < 26 and 6 <= y < 22:
        return BACK_STRIPES[y-6][x-16]=='s'
    if (0 <= x < 16 or 26 <= x < 42) and 22 <= y < 30:
        along=15-x if x<16 else x-26
        return FLANK_STRIPES[y-22][along]=='s'
    if 42 <= x < 52 and 22 <= y < 30:
        return RUMP_STRIPES[y-22][x-42]=="s"
    if 16 <= x < 26 and 22 <= y < 30:
        return False
    return None


def piglet_head_stripe(x,y):
    if 76<=x<84 and 24<=y<32:
        return HEAD_STRIPE[y-24][x-76]=='s'
    if 68<=x<100 and y==32:
        return x in (79,80,95,96)
    return None


def coat(baby):
    randomizer = random.Random(434)
    # Retain the approved eye texels when translating their mirrored UV patches.
    noise = [randomizer.randrange(-9, 10) for _ in range(128 * 80)]
    rows = []
    for y in range(80):
        row = bytearray()
        for x in range(128):
            variation = noise[y * 128 + x]
            base = (99, 72, 47) if baby else (79, 63, 47)
            # Retain established juvenile coloring outside the painted fur regions.
            if baby and y < 36 and (y // 3) % 3 == 1:
                base = (170, 133, 81)
            if baby:
                stripe=piglet_body_stripe(x,y)
                if stripe is not None:
                    pale=(195,157,100) if y>=22 else (170,133,81)
                    base=pale if stripe else (99,72,47)
                head_stripe=piglet_head_stripe(x,y)
                if head_stripe is not None:base=(170,133,81) if head_stripe else (99,72,47)
            if x < 16 and y >= 66:
                base = (37, 32, 27)  # Hooves.
            if x >= 112 and y < 9:
                base = (217, 202, 162)  # Tusks (hidden in the juvenile model).
            # Head sides begin at (68,32)/(84,32). Forward is +U/-U respectively.
            if (72 <= x <= 73 or 86 <= x <= 87) and 34 <= y <= 35:
                base = (12, 10, 8)
                source_x = x - 2 if x < 80 else x + 2
                variation = noise[y * 128 + source_x]
            if (x, y) in ((72, 34), (86, 34)):
                base = (133, 109, 71)
            # Snout cap begins at (75,49); dark paired nostrils remain distinct.
            if x in (76, 79) and y in (50, 51):
                base = (25, 20, 16)
            if not baby:
                # Adult eyes span the front/side corners of the head UV net.
                # Clear the shared side-only patches before moving adult eyes forward.
                if (72<=x<=73 or 86<=x<=87) and 34<=y<=35:
                    base=(79,63,47)
                    variation=noise[y*128+x]
                if (74<=x<=75 or 84<=x<=85) and 34<=y<=35:
                    if y==35:base=(128,106,77) if x in (75,84) else (106,85,61)
                    elif x in (75,84):base=(12,10,8)
                    else:base=(133,109,71)
                    variation=noise[y*128+x]
                if x in (76,77,82,83) and 34<=y<=35:
                    # Join each pupil over the UV fold for continuous quarter views.
                    if y==35:base=(120,98,70) if x in (76,83) else (100,80,57)
                    elif x in (76,83):base=(12,10,8)
                    else:base=(112,92,65)
                    variation=noise[y*128+x]
                if y==33 and x in (74,75,76,77,82,83,84,85):
                    base=(137,114,83) if x in (75,76,83,84) else (96,78,56)
                    variation=noise[y*128+x]//2
                if x in (79,80) and y in (32,33):
                    base=(100,81,58)
                # A leathery cap and shaded side/top planes distinguish the
                # muzzle from the head without competing with the ivory tusks.
                if 75<=x<81 and 49<=y<53 and not (x in (76,79) and y in (50,51)):
                    if y==52:base=(91,76,60)
                    elif y==49 or x in (75,80):base=(100,84,65)
                    else:base=(123,106,84)
                if (72<=x<75 or 81<=x<84) and 49<=y<53:
                    base=(105,84,60) if y<=50 else (87,68,48)
                if 75<=x<81 and 46<=y<49:
                    base=(110,89,64)
                if x in (76,79) and y in (50,51):
                    base=(42,34,26) if y==50 else (79,64,47)
                # Both simple tusk cubes share this 2×2×2 net. A darker root
                # and lower/front edge give the ivory direction.
                if 112<=x<120 and 0<=y<4:
                    if y<2:base=(239,225,188) if x<116 else (181,162,124)
                    elif 114<=x<116:base=(230,216,179) if y==2 else (208,190,151)
                    elif x>=118:base=(159,142,111)
                    else:base=(205,187,149)
            row.extend([max(0, min(255, channel + variation)) for channel in base] + [255])
        rows.append(b"\0" + row)
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", 128, 80, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(b"".join(rows)))
            + chunk(b"IEND", b""))


if __name__ == "__main__":
    DESTINATION.mkdir(parents=True, exist_ok=True)
    if "--piglet-only" in sys.argv and "--adult-only" in sys.argv:
        raise SystemExit("Select one age-only authoring mode")
    if "--piglet-only" in sys.argv:ages=(True,)
    elif "--adult-only" in sys.argv:ages=(False,)
    else:ages=(False,True)
    for juvenile in ages:
        name = "wild_boar_piglet.png" if juvenile else "wild_boar.png"
        (DESTINATION / name).write_bytes(coat(juvenile))
