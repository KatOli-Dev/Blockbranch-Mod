#!/usr/bin/env python3
"""Original 16px raw and cooked duck breast icons. Writes only these two items.

Hand-authored color clusters describe a pale fat edge, lit upper-left flesh and
a darker underside. No borrowed game pixels or random per-pixel noise.
"""
from pathlib import Path
import argparse
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2]
PALETTE = {
    '.': (0, 0, 0, 0),
    'o': (88, 34, 49, 255),  # contour / deepest underside
    's': (124, 45, 61, 255),  # shaded thickness
    'd': (146, 52, 69, 255),  # recessed flesh folds
    'm': (172, 69, 82, 255),  # raw flesh body
    'l': (198, 91, 101, 255),
    'h': (217, 116, 117, 255),
    'f': (218, 173, 148, 255),  # fat / connective edge
    'F': (241, 203, 173, 255),
}
COOKED_PALETTE = {
    '.': (0, 0, 0, 0),
    'o': (79, 43, 30, 255),
    's': (112, 59, 34, 255),
    'd': (139, 73, 37, 255),
    'm': (169, 99, 47, 255),
    'l': (192, 127, 61, 255),
    'h': (216, 159, 84, 255),
    'f': (206, 148, 69, 255),
    'F': (237, 192, 113, 255),
}
# Narrow fat cap and connected oblique muscle clusters retain the shared outline.
PIXELS = (
    '................',
    '................',
    '.......oooo.....',
    '.....ooFfffoo...',
    '....ofhllmmmmo..',
    '...ohllmmddmmmo.',
    '..ohllmmdmmlmmo.',
    '..ollmmdmmlmmso.',
    '..ollmdmmlmmsoo.',
    '..ommdmmlmmsoo..',
    '...omdmmlmdso...',
    '...osmmldsso....',
    '....osssso......',
    '.....oooo.......',
    '................',
    '................',
)


# Browned skin has sparse scoring and a crisp edge rather than the raw muscle
# folds. The shared breast silhouette keeps the cooked/raw pair recognizable.
COOKED_PIXELS = (
    '................',
    '................',
    '.......oooo.....',
    '.....ooffffoo...',
    '....offhhlmmmo..',
    '...ofhhllmmdmoo.',
    '..ofhllmdmmlmmo.',
    '..ofllmdmmlmdmo.',
    '..ohllmmmmdmmso.',
    '..omlmdmmdmmso..',
    '...ommdmmlmso...',
    '...osmmmdmso....',
    '....osssso......',
    '.....oooo.......',
    '................',
    '................',
)


def write_duck_food(cooked=False, destination=None):
    name = 'cooked_duck' if cooked else 'duck'
    destination = Path(destination) if destination else ROOT / f'src/main/resources/assets/blockbranch/textures/item/{name}.png'
    palette = COOKED_PALETTE if cooked else PALETTE
    pixels = COOKED_PIXELS if cooked else PIXELS
    assert len(pixels) == 16 and all(len(row) == 16 for row in pixels)
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    rows = b''.join(b'\0' + b''.join(bytes(palette[p]) for p in row) for row in pixels)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return destination


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--raw-only', action='store_true', help='write only raw duck; leave cooked bytes untouched')
    options = parser.parse_args()
    for cooked in ((False,) if options.raw_only else (False, True)):
        print(write_duck_food(cooked))
