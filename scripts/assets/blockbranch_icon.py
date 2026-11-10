#!/usr/bin/env python3
"""Original branch-and-terrain emblem, nearest-scaled for mod metadata.

Optional Python standard library only; never required for builds or gameplay.
No vanilla artwork is sampled. This is a metadata icon, not a block texture.
"""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2]
SIZE = 32  # Metadata emblem only; actual block textures remain untouched at 16px.
PALETTE = {'L': (185, 208, 112, 255), 'g': (115, 161, 72, 255),
           'G': (49, 94, 54, 255)}


def emblem():
    pixels = [(0, 0, 0, 0)] * SIZE**2

    def polygon(points, shade):
        for y in range(SIZE):
            for x in range(SIZE):
                px, py, inside = x + .5, y + .5, False
                for a, b in zip(points, points[1:] + points[:1]):
                    if (a[1] > py) != (b[1] > py) and px < (b[0] - a[0]) * (py - a[1]) / (b[1] - a[1]) + a[0]:
                        inside = not inside
                if inside:
                    pixels[y * SIZE + x] = (*shade(x, y), 255)

    # Equal projected width/height gives a cube rather than a shallow soil platter.
    # A few two-pixel clusters keep the side faces textured without copying game art.
    def left(x, y):
        depth = y + .5 - (15.5 + (x + .5 - 5) / 2)
        return (97, 131, 62) if depth < 1.2 else (176, 132, 83) if (x//2+3*(y//2))%9<2 else (144, 103, 65)
    def right(x, y):
        depth = y + .5 - (21 - (x + .5 - 16) / 2)
        return (62, 99, 52) if depth < 1.2 else (126, 89, 57) if (x//2+2*(y//2))%9<2 else (100, 72, 49)
    polygon([(5, 15.5), (16, 21), (16, 32), (5, 26.5)], left)
    polygon([(16, 21), (27, 15.5), (27, 26.5), (16, 32)], right)
    polygon([(16, 10), (27, 15.5), (16, 21), (5, 15.5)],
            lambda x, y: (131, 157, 71) if (x//2+3*(y//2))%7<2 else (104, 139, 62))
    # Dark bark stays distinct from both grass and the side-face soil.
    def branch(points):
        for a, b in zip(points, points[1:]):
            steps=max(abs(b[0]-a[0]),abs(b[1]-a[1]))
            for step in range(steps+1):
                x=round(a[0]+(b[0]-a[0])*step/steps);y=round(a[1]+(b[1]-a[1])*step/steps)
                for dx in range(3):
                    pixels[y*SIZE+x+dx]=(80, 53, 33, 255) if dx else (142, 100, 55, 255)
    branch([(15, 18),(15, 13),(10, 8)])
    branch([(15, 14),(20, 9),(21, 5)])
    # Unequal, offset crowns keep the vegetation silhouette recognizably asymmetric.
    for x, y, rows in [(19, 1, ('.LLLL.', 'LLLggg', 'Lggggg', 'ggGGGg', '.GGGG.', '..GG..')),
                       (6, 5, ('.LLL..', 'LLLggg', 'Lggggg', '.gGGG.', '..GG..'))]:
        for dy, row in enumerate(rows):
            for dx, mark in enumerate(row):
                if mark != '.':
                    pixels[(y + dy) * SIZE + x + dx] = PALETTE[mark]
    return pixels


def write_icon(destination=None):
    pixels = emblem()
    # Preserve the authored grid in the existing 128px metadata canvas.
    scale = 128 // SIZE
    raw = b''.join(b'\0' + bytes(channel for x in range(128)
                    for channel in pixels[(y // scale) * SIZE + x // scale])
                   for y in range(128))
    def chunk(kind, payload):
        return struct.pack('!I', len(payload)) + kind + payload + struct.pack('!I', zlib.crc32(kind + payload) & 0xffffffff)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', 128, 128, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    target = Path(destination) if destination else ROOT / 'src/main/resources/assets/blockbranch/icon.png'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)


if __name__ == '__main__':
    write_icon()
