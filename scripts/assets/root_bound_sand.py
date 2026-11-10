#!/usr/bin/env python3
"""Hand-drawn sand grains and fine root traces; Python standard library only.

Top traces are sparse fragments. Exposed sides carry branching roots rather than
repeating a broad vertical root over the walkable surface.
"""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2]
PALETTE = {
    'a': (224, 213, 172, 255),
    'b': (218, 205, 159, 255),
    'c': (210, 196, 148, 255),
    'd': (202, 187, 139, 255),
    'e': (190, 173, 123, 255),
    'r': (162, 141, 95, 255),
}
GRAINS = (
    'bbabccbbabbbcabb',
    'baabbccbabbaabbb',
    'babcbbbaacbbccab',
    'ccbbabbccbbbccbb',
    'cbbbaabccbabbabb',
    'bbccbbabbbaccaab',
    'abccbbbaabcbbbbb',
    'abbbaaccbbbcdccb',
    'bcbbaabbcbbbcdbb',
    'bcbbbcbbcaabbbbb',
    'bbabccbbbaabccba',
    'aaabccbabbbbccba',
    'bbabbbaabccbbabb',
    'cbcbbaabbcccbaac',
    'cbbbcbbbabbbaabc',
    'bbabccbbbabccbbb',
)
TOP_ROOTS = (
    (11, 1, 'e'), (10, 2, 'e'), (9, 3, 'e'),
    (2, 4, 'e'), (2, 5, 'r'), (3, 5, 'e'), (3, 6, 'e'), (4, 7, 'e'),
    (12, 8, 'e'), (12, 9, 'r'), (11, 9, 'e'), (11, 10, 'e'),
    (6, 12, 'e'), (5, 13, 'e'), (4, 14, 'e'),
)
SIDE_ROOTS = (
    (3, 0, 'r'), (3, 1, 'r'), (3, 2, 'r'), (4, 3, 'r'),
    (4, 4, 'r'), (4, 5, 'r'), (5, 6, 'r'), (5, 7, 'e'),
    (5, 8, 'e'), (6, 9, 'e'), (6, 10, 'e'), (7, 11, 'e'),
    (3, 4, 'e'), (2, 5, 'r'), (2, 6, 'e'), (1, 7, 'e'),
    (6, 6, 'e'), (7, 6, 'r'), (8, 5, 'e'), (9, 5, 'e'),
    (11, 0, 'r'), (11, 1, 'r'), (12, 2, 'r'), (12, 3, 'r'),
    (11, 4, 'r'), (11, 5, 'r'), (10, 6, 'r'), (10, 7, 'r'),
    (10, 8, 'e'), (11, 9, 'e'), (11, 10, 'e'), (11, 11, 'e'),
    (10, 12, 'e'), (10, 13, 'e'), (9, 14, 'e'), (9, 15, 'e'),
    (12, 4, 'e'), (13, 5, 'e'), (13, 6, 'e'), (14, 7, 'e'),
    (9, 10, 'e'), (8, 11, 'e'), (8, 12, 'e'),
)


def write_sand(side=False, destination=None):
    name = 'root_bound_sand_side' if side else 'root_bound_sand'
    destination = Path(destination) if destination else ROOT / f'src/main/resources/assets/blockbranch/textures/block/{name}.png'
    assert len(GRAINS) == 16 and all(len(row) == 16 for row in GRAINS)
    pixels = [list(row) for row in GRAINS]
    for x, y, color in SIDE_ROOTS if side else TOP_ROOTS:
        pixels[y][x] = color

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    rows = b''.join(b'\0' + b''.join(bytes(PALETTE[p]) for p in row) for row in pixels)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return destination


if __name__ == '__main__':
    for side in (False, True):
        print(write_sand(side))
