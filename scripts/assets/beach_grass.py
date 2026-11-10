#!/usr/bin/env python3
"""Author two narrow beach-grass silhouettes with the Python standard library.

Each blade is a continuous one-pixel path from a shared crown. The alternate
tuft is lower and wind-swept so scattered patches do not repeat one outline.
This optional authoring tool is not used by Gradle or the game.
"""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2]
PALETTE = {
    '.': (0, 0, 0, 0),
    's': (83, 107, 65, 255),
    'g': (116, 141, 88, 255),
    'l': (149, 166, 113, 255),
    'h': (179, 190, 142, 255),
    't': (191, 185, 132, 255),
}

# Points progress from the rooted crown to the leaf tip. No leaf forks.
BLADES = (
    (
        ((7, 14), (6, 13), (6, 12), (5, 11), (5, 10), (4, 9), (3, 8)),
        ((7, 14), (7, 13), (7, 12), (6, 11), (6, 10), (6, 9), (5, 8), (5, 7), (5, 6)),
        ((8, 14), (8, 13), (8, 12), (8, 11), (8, 10), (8, 9), (8, 8), (8, 7), (8, 6), (7, 5)),
        ((9, 14), (9, 13), (10, 12), (10, 11), (10, 10), (10, 9), (11, 8), (11, 7)),
        ((9, 14), (10, 13), (10, 12), (11, 11), (11, 10), (12, 9)),
    ),
    (
        ((7, 14), (6, 13), (6, 12), (5, 11), (5, 10), (4, 9), (4, 8)),
        ((7, 14), (7, 13), (7, 12), (7, 11), (6, 10), (6, 9), (6, 8), (5, 7)),
        ((8, 14), (8, 13), (8, 12), (8, 11), (8, 10), (8, 9), (8, 8), (9, 7), (9, 6)),
        ((9, 14), (9, 13), (10, 12), (10, 11), (11, 10), (11, 9), (11, 8)),
        ((9, 14), (10, 13), (10, 12), (11, 11), (12, 10)),
    ),
)


def pixels_for(variant):
    if variant not in (0, 1):
        raise ValueError('beach grass variant must be 0 or 1')
    pixels = [['.'] * 16 for _ in range(16)]
    for x, y in ((7, 15), (8, 15), (9, 15), (7, 14), (8, 14), (9, 14)):
        pixels[y][x] = 's' if y == 15 else 'g'
    for blade, points in enumerate(BLADES[variant]):
        for index, (x, y) in enumerate(points):
            color = 'g' if y >= 12 else 'l' if y >= 8 else 'h'
            if index == len(points) - 1:
                color = 't' if blade in (0, 3, 4) else 'h'
            pixels[y][x] = color
            # Two-pixel lower leaves survive the block atlas at normal view
            # distance; the upper bend and terminal tip remain one pixel wide.
            if y >= 11 and 0 < index < len(points) - 1:
                inner_x = x + (1 if blade < 3 else -1)
                pixels[y][inner_x] = 'g'
    return tuple(''.join(row) for row in pixels)


def write_beach_grass(destination=None, variant=0):
    name = 'beach_grass' if variant == 0 else 'beach_grass_b'
    destination = Path(destination) if destination else ROOT / f'src/main/resources/assets/blockbranch/textures/block/{name}.png'
    pixels = pixels_for(variant)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    rows = b''.join(b'\0' + b''.join(bytes(PALETTE[p]) for p in row) for row in pixels)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return destination


if __name__ == '__main__':
    for variant in (0, 1):
        print(write_beach_grass(variant=variant))
