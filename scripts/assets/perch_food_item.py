#!/usr/bin/env python3
"""Hand-drawn 16px perch food icons. Uses only the Python standard library.

Each character is one final pixel: outlines, fins and stripes never pass through
image resampling. Three separated bars leave broad flank color; the tail has
a visible fork notch. Writes only the raw and cooked perch inventory textures.
"""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2]
PALETTE = {
    '.': (0, 0, 0, 0),
    'o': (58, 70, 39, 255),   # contour
    's': (84, 101, 51, 255),  # back and body shadow
    'b': (98, 113, 57, 255),  # perch bars
    'm': (155, 167, 83, 255), # flank
    'h': (183, 190, 111, 255),
    'c': (217, 210, 158, 255),# belly
    'r': (161, 73, 38, 255),  # fin shadow
    'f': (211, 117, 54, 255),
    'e': (32, 39, 27, 255),
}
COOKED_PALETTE = {
    '.': (0, 0, 0, 0),
    'o': (89, 53, 31, 255),
    's': (128, 76, 37, 255),
    'b': (145, 85, 39, 255),
    'm': (190, 127, 59, 255),
    'h': (219, 166, 88, 255),
    'c': (237, 197, 124, 255),
    'r': (117, 65, 31, 255),
    'f': (163, 100, 42, 255),
    'e': (59, 39, 25, 255),
}
PIXELS = (
    '................',
    '............o...',
    '...........ofo..',
    '...........fo...',
    '......o...orffo.',
    '.....osoosfro...',
    '....oshmbmso....',
    '...oshbmmbmso...',
    '...obmmbmmbco...',
    '..ohmbmmbcco....',
    '..ohhmbcccor....',
    '.ohehmccofro....',
    '.ohhmccofro.....',
    '..occcoo........',
    '...ooo..........',
    '................',
)


def write_perch_food(cooked=False, destination=None):
    name = 'cooked_perch' if cooked else 'perch'
    destination = Path(destination) if destination else ROOT / f'src/main/resources/assets/blockbranch/textures/item/{name}.png'
    palette = COOKED_PALETTE if cooked else PALETTE
    assert len(PIXELS) == 16 and all(len(row) == 16 for row in PIXELS)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    rows = b''.join(b'\0' + b''.join(bytes(palette[p]) for p in row) for row in PIXELS)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return destination


if __name__ == '__main__':
    for cooked in (False, True):
        print(write_perch_food(cooked))
