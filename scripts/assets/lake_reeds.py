#!/usr/bin/env python3
"""Hand-drawn emergent reeds with continuous stems and staggered seed heads.

Python standard library only. Segment borders keep the two culms aligned; long
arching blades interrupt the repeated verticals. The item matches the tip art.
"""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/blockbranch/textures'
PALETTE = {
    '.': (0, 0, 0, 0),
    'c': (74, 100, 48, 255),
    's': (101, 128, 57, 255),
    'l': (131, 153, 73, 255),
    'h': (161, 176, 100, 255),
    't': (177, 171, 112, 255),
    'o': (106, 76, 43, 255),
    'b': (143, 108, 56, 255),
    'a': (170, 133, 74, 255),
}
STEM = (
    '.....s....s.....',
    '.....s....s.....',
    '.....s....c..h..',
    '..h..s....c.l...',
    '...l.s....sl....',
    '....ls....s.....',
    '.....l....s.....',
    '.....s....s.....',
    '.h...c....s.....',
    '..l..c....s.....',
    '...l.c....s...h.',
    '....ls....s.ll..',
    '.....l....ssl...',
    '.....s....ls....',
    '.....s....s.....',
    '.....s....s.....',
)
TIP = (
    '................',
    '.....t..........',
    '....ab..........',
    '....bo..........',
    '....bo..........',
    '....bo....t.....',
    '....bo...ab.....',
    '.....s...bo.....',
    '..h..s...bo.....',
    '...l.s...bo..h..',
    '....ls...bo.l...',
    '.....l....sl....',
    '.....s....s.....',
    '..h..s....s.....',
    '...l.s....s.....',
    '....ls....s.....',
)


def write_reeds(tip=False, destination=None):
    name = 'lake_reed_tip' if tip else 'lake_reed_stem'
    destination = Path(destination) if destination else ROOT / f'block/{name}.png'
    pixels = TIP if tip else STEM
    assert len(pixels) == 16 and all(len(row) == 16 for row in pixels)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    rows = b''.join(b'\0' + b''.join(bytes(PALETTE[p]) for p in row) for row in pixels)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return destination


if __name__ == '__main__':
    write_reeds()
    write_reeds(tip=True)
    write_reeds(tip=True, destination=ROOT / 'item/lake_reed.png')
