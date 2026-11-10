#!/usr/bin/env python3
"""Hand-drawn 16px lean venison cuts; writes only the two food textures.

Uses the Python standard library. Raw clusters follow the muscle grain;
the cooked cut has broader browned planes and a few broken seared patches.
"""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2]
PALETTE = {
    '.': (0, 0, 0, 0),
    'o': (91, 36, 42, 255),
    's': (126, 46, 54, 255),
    'd': (155, 56, 64, 255),
    'm': (177, 67, 75, 255),
    'l': (199, 89, 92, 255),
    'h': (214, 112, 108, 255),
    'f': (203, 151, 126, 255),
    'F': (231, 193, 159, 255),
}
COOKED_PALETTE = {
    '.': (0, 0, 0, 0),
    'o': (70, 37, 25, 255),
    's': (104, 52, 28, 255),
    'd': (130, 65, 31, 255),
    'm': (157, 83, 39, 255),
    'l': (180, 106, 54, 255),
    'h': (201, 132, 73, 255),
    'f': (176, 113, 64, 255),
    'F': (208, 153, 94, 255),
}
PIXELS = (
    '................',
    '................',
    '.........oooo...',
    '......oohhhfFo..',
    '....oohlllmmfo..',
    '...ohhllmmdmfo..',
    '..ohllmmdmmso...',
    '..ollmmdmmsso...',
    '.ollmddmmsso....',
    '.ommdmmmsso.....',
    '..ommmmsso......',
    '..osmmsso.......',
    '...ossoo........',
    '....oo..........',
    '................',
    '................',
)

# Preserve the lean cut's contour. Broad highlights and short, offset dark
# clusters describe a browned surface without repeating the raw muscle pattern.
COOKED_PIXELS = (
    '................',
    '................',
    '.........oooo...',
    '......oohhhfFo..',
    '....oohlllmmfo..',
    '...ohhllsddmfo..',
    '..ohlllsdmmdo...',
    '..olllmmmlsso...',
    '.olllsddmsso....',
    '.omlsdmmddo.....',
    '..ommmmddo......',
    '..osddsso.......',
    '...ossoo........',
    '....oo..........',
    '................',
    '................',
)


def write_venison_food(cooked=False, destination=None):
    name = 'cooked_venison' if cooked else 'venison'
    destination = Path(destination) if destination else ROOT / f'src/main/resources/assets/blockbranch/textures/item/{name}.png'
    palette = COOKED_PALETTE if cooked else PALETTE
    pixels = COOKED_PIXELS if cooked else PIXELS
    assert len(pixels) == 16 and all(len(row) == 16 for row in pixels)
    assert all((a == '.') == (b == '.') for raw, browned in zip(PIXELS, COOKED_PIXELS) for a, b in zip(raw, browned))

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    rows = b''.join(b'\0' + b''.join(bytes(palette[p]) for p in row) for row in pixels)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return destination


if __name__ == '__main__':
    for cooked in (False, True):
        print(write_venison_food(cooked))
