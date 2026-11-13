#!/usr/bin/env python3
"""Hand-drawn emergent reeds with continuous stems, staggered seed heads and arching strap blades.

Python standard library only. Segment borders keep the two culms aligned (columns 5 and 10 in every row of the stem
segment and the lower rows of the tip); the blades are drawn by `blade` so their shape is one rule, not eleven sprites.
The item matches the tip art.
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
CULMS = (5, 10)


def blank():
    return [['.'] * 16 for _ in range(16)]


def put(grid, cells, ch, thick=False):
    for x, y in cells:
        if not (0 <= x < 16 and 0 <= y < 16):
            raise AssertionError(('blade leaves the texture', x, y))
        free = grid[y][x] == '.'
        if thick:
            if free:
                grid[y][x] = ch
        else:
            assert free or grid[y][x] in 'sc', (x, y, grid[y][x])
            grid[y][x] = ch


def strap(grid, cells, below, above=None):
    """Draw a leaf as a filled lens: `cells` is the centre line (base first), `below`/`above` the extra cells under/over it.

    The upper surface is the lightest green and the underside the darkest, so a three-cell-thick blade reads as a flat
    strap catching light rather than a one-cell twig.
    """
    above = above or [0] * len(cells)
    for i, (x, y) in enumerate(cells):
        column = [(y - k, 'h') for k in range(above[i], 0, -1)] + [(y, 'l' if i < len(cells) - 1 else 'h')]
        column += [(y + k, 's' if k < below[i] else 'c') for k in range(1, below[i] + 1)]
        for yy, ch in column:
            if not (0 <= yy < 16) or not (0 <= x < 16):
                raise AssertionError(('blade leaves the texture', x, yy))
            if grid[yy][x] == '.' or (i == 0 and grid[yy][x] in 'sc'):
                grid[yy][x] = ch


def up(grid, x0, y0, step):
    """Blade leaving a culm and leaning outward: three cells thick near the middle, one at the tip."""
    strap(grid, [(x0 + i * step, y0 - i) for i in range(5)], [1, 2, 2, 1, 0], [0, 0, 1, 0, 0])


def droop(grid, x0, y0, step):
    """Blade that arches over and hangs: base, rise, level, then a fall that thins to the border."""
    strap(grid, [(x0, y0), (x0 + step, y0 - 1), (x0 + 2 * step, y0 - 1), (x0 + 3 * step, y0), (x0 + 3 * step, y0 + 1), (x0 + 4 * step, y0 + 2)],
          [1, 1, 1, 1, 1, 0], [0, 0, 1, 0, 0, 0])


def inward(grid, x0, y0, step):
    """Short blade between the two culms."""
    strap(grid, [(x0, y0), (x0 + step, y0 - 1), (x0 + 2 * step, y0)], [1, 1, 0])


def culms(grid):
    for row in range(16):
        for x in CULMS:
            grid[row][x] = 's'
    for row in (4, 10):
        grid[row][CULMS[0]] = grid[row][CULMS[1]] = 'c'


def rows_of(grid):
    return tuple(''.join(row) for row in grid)


def stem_art():
    g = blank()
    culms(g)
    up(g, 11, 6, 1)
    droop(g, 4, 2, -1)
    up(g, 4, 11, -1)
    droop(g, 11, 13, 1)
    inward(g, 9, 3, -1)
    inward(g, 6, 9, 1)
    return rows_of(g)


def tip_art():
    g = blank()
    culms(g)
    for y in range(0, 11):
        g[y][CULMS[0]] = g[y][CULMS[1]] = '.'
    for y in range(7, 11):
        g[y][CULMS[0]] = 's'
    for y in range(10, 16):
        g[y][CULMS[1]] = 's'
    # staggered seed heads (the published silhouette: left one taller, right one lower)
    for x, y, ch in (
        (5, 1, 't'), (4, 2, 'a'), (5, 2, 'b'), (4, 3, 'b'), (5, 3, 'o'), (4, 4, 'b'), (5, 4, 'o'), (4, 5, 'b'), (5, 5, 'o'),
        (4, 6, 'b'), (5, 6, 'o'),
        (10, 5, 't'), (9, 6, 'a'), (10, 6, 'b'), (9, 7, 'b'), (10, 7, 'o'), (9, 8, 'b'), (10, 8, 'o'), (9, 9, 'b'), (10, 9, 'o'),
    ):
        g[y][x] = ch
    up(g, 11, 4, 1)
    droop(g, 11, 11, 1)
    droop(g, 4, 7, -1)
    up(g, 4, 14, -1)
    inward(g, 9, 13, -1)
    inward(g, 6, 11, 1)
    return rows_of(g)


STEM = stem_art()
TIP = tip_art()


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
