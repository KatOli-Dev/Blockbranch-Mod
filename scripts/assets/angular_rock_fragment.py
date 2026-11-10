#!/usr/bin/env python3
"""Hand-drawn chipped rock fragment; Python standard library only.

Three broad fracture planes and a chipped edge give the fragment a solid stone mass.
Writes only the item texture, preserving the existing scree terrain artwork.
"""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2]
# Broad lit fracture, front plane and dark thickness keep the fragment matte.
PALETTE = {
    '.': (0, 0, 0, 0),
    'o': (74, 72, 69, 255),   # broken outer edge
    's': (105, 102, 96, 255), # shaded thickness
    'm': (139, 135, 125, 255),# front fracture plane
    'l': (170, 166, 151, 255),# lit fracture plane
    'h': (196, 191, 174, 255),# small upper facet
}
PIXELS = (
    '................',
    '................',
    '.......ooo......',
    '.....oohhloo....',
    '....ohhhllmmo...',
    '...ohhlllmmsso..',
    '...ohlllmmsoo...',
    '..ohlllmmmsso...',
    '..olllmmmmsso...',
    '...ollmmmssso...',
    '....ommmssso....',
    '.....ommsso.....',
    '......oooo......',
    '................',
    '................',
    '................',
)


def write_fragment(destination=None):
    destination = Path(destination) if destination else ROOT / 'src/main/resources/assets/blockbranch/textures/item/angular_rock_fragment.png'
    assert len(PIXELS) == 16 and all(len(row) == 16 for row in PIXELS)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    rows = b''.join(b'\0' + b''.join(bytes(PALETTE[p]) for p in row) for row in PIXELS)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return destination


if __name__ == '__main__':
    print(write_fragment())
