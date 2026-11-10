#!/usr/bin/env python3
"""Hand-drawn 16px fescue: arching blades and loose, ripe panicles.

Python standard library only. Writes just the mature/cropped block textures.
Both states share their rooted crown; staggered culms and bent blades vary the outline.
"""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2]
PALETTE = {
    '.': (0, 0, 0, 0),
    's': (73, 91, 42, 255),
    'g': (109, 131, 59, 255),
    'l': (144, 158, 77, 255),
    'h': (171, 175, 98, 255),
    't': (133, 135, 66, 255),
    'k': (135, 119, 63, 255),
    'a': (170, 153, 83, 255),
    'b': (195, 177, 108, 255),
}
MATURE = (
    '................',
    '................',
    '..........b.....',
    '.........aa.....',
    '..........ta....',
    '.........at.....',
    '.........t......',
    '....b....t......',
    '...at....t......',
    '....la...l......',
    '..hh.l..lh......',
    '....ll..l....l..',
    '.....lg.g..ll...',
    '......g.g.g.....',
    '.......ggg......',
    '......sss.......',
)
CROPPED = (
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '................',
    '..hh.h...h......',
    '....ll..l....l..',
    '.....ll.l..ll...',
    '......g.g.g.....',
    '.......ggg......',
    '......sss.......',
)


def write_fescue(cropped=False, destination=None):
    name = 'meadow_fescue_cropped' if cropped else 'meadow_fescue'
    destination = Path(destination) if destination else ROOT / f'src/main/resources/assets/blockbranch/textures/block/{name}.png'
    pixels = CROPPED if cropped else MATURE
    assert len(pixels) == 16 and all(len(row) == 16 for row in pixels)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    rows = b''.join(b'\0' + b''.join(bytes(PALETTE[p]) for p in row) for row in pixels)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return destination


if __name__ == '__main__':
    for cropped in (False, True):
        print(write_fescue(cropped))
