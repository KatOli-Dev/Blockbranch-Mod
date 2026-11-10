#!/usr/bin/env python3
"""Hand-authored sage foliage, woody fibers and irregular shrub crowns.

Standard-library Python only. Writes the three shrub textures, both growth-state
models and their quarter-turn variants. Gameplay state/outline sizes are retained.
"""
from pathlib import Path
import json
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/blockbranch'
PALETTE = {
    '.': (0, 0, 0, 0),
    's': (87, 112, 88, 255),
    'g': (119, 143, 113, 255),
    'm': (143, 164, 132, 255),
    'l': (167, 184, 154, 255),
    'h': (184, 197, 171, 255),
    't': (109, 91, 63, 255),
    'b': (132, 111, 75, 255),
    'k': (158, 138, 95, 255),
    'q': (90, 77, 58, 255),
}
SPRAYS = (
    '................',
    '................',
    '..........h.....',
    '........htlm....',
    '.......mlt.gh...',
    '...h.h.gt.htlm..',
    '...lml..t.lm.g..',
    '....gthth.tgh...',
    '...h.mltlmhmlh..',
    '...lmgt.tmlg.lm.',
    '..htmlt.hg.tt.h.',
    '..lmgthmltt...lm',
    '...gh.lgt..h...g',
    '...ml..gthml....',
    '........t.......',
    '................',
)
LEAVES = (
    '..lmgs..hmlg.lm.',
    '.hlmg.lmgs.hmlg.',
    'lmg..hlmg..lmg..',
    'mg.lmg..hlmg..lm',
    '.hlmg.lmgs.lmg..',
    '..gslmg.hlmg.lmg',
    '.lmg..lmgs..hmlg',
    'hmlg.lmg..lmg..s',
    'lmg..hmlg.lmg..l',
    '.gslmg.lmgs.hmlg',
    'hmlg..lmg..lmg..',
    'lmg.lmgs.hmlg.lm',
    '.lmg.hmlg..lmgs.',
    '..hmlg.lmg.lmg..',
    'lmg..lmgs.hmlg.l',
    '.lmg.hmlg..lmg..',
)
STEM = (
    'tbkbtbbqtbkbtbbt',
    'tbkbtbbqtbkbtbbt',
    'tbkbtbbqtbkbtbbt',
    'tbkbtbbqttkbtbbt',
    'tbkbtbbqttkbtbbt',
    'tbkbtbbqtbkbtbbt',
    'tbkbtkbqtbkbtbbt',
    'tbkbtkkqtbkbtbbt',
    'tbkbtbqqtbkbtbbt',
    'tbkbtbbqtbkbtbbt',
    'tbkbtbbqtbkbtbbt',
    'tbkbtbbqtbkbtbbt',
    'tbkbtbbqtbkktbbt',
    'tbkbtbbqtbkqtbbt',
    'tbkbtbbqtbkbtbbt',
    'tbkbtbbqtbkbtbbt',
)


def write_texture(name, pixels):
    assert len(pixels) == 16 and all(len(row) == 16 for row in pixels)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    rows = b''.join(b'\0' + b''.join(bytes(PALETTE[p]) for p in row) for row in pixels)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    path = ROOT / f'textures/block/silver_steppe_shrub_{name}.png'
    path.write_bytes(data)


def box(start, end, texture, axis=None, angle=0, origin=None):
    element = {
        'from': start,
        'to': end,
        'faces': {side: {'texture': '#' + texture} for side in ('down', 'up', 'north', 'south', 'west', 'east')},
    }
    if axis:
        element['rotation'] = {'origin': origin, 'axis': axis, 'angle': angle}
    return element


def model(clipped=False):
    # Small foliage masses anchor cutout sprays to the forked woody branches.
    height = 4 if clipped else 13
    uv = [0, 10, 16, 16] if clipped else [0, 0, 16, 16]
    elements = [box([7.25, 0, 7.25], [8.75, 3 if clipped else 5.5, 8.75], 'stem')]
    if clipped:
        elements += [
            box([6, 1, 7.5], [7.25, 3.75, 8.5], 'stem', 'z', 22.5, [7, 1, 8]),
            box([8.75, .75, 7.5], [10, 3.25, 8.75], 'stem', 'z', -22.5, [9, 1, 8]),
            box([4.5, .75, 5], [10.5, 2.75, 11], 'leaves'),
            box([9, 1.5, 7], [12, 3.25, 10.5], 'leaves'),
        ]
    else:
        elements += [
            box([6.5, 2.5, 7.5], [7.75, 8.75, 8.5], 'stem', 'z', 22.5, [7.5, 3, 8]),
            box([8.25, 2.75, 7.25], [9.5, 8.25, 8.5], 'stem', 'z', -22.5, [8.5, 3, 8]),
            box([7.4, 3.1, 8.2], [8.4, 7.6, 9.2], 'stem', 'x', 22.5, [8, 3, 8.4]),
            box([3.5, 5, 5], [7.25, 8, 8.5], 'leaves', 'y', 22.5, [5.5, 6, 6.75]),
            box([9, 4, 7], [12.5, 7, 10.5], 'leaves', 'y', -22.5, [10.75, 5.5, 8.75]),
            box([6.5, 8, 7], [10, 11, 10], 'leaves', 'y', 22.5, [8.25, 9, 8.5]),
        ]
    for axis in ('z', 'x'):
        plane = {
            'from': [4 if clipped else 1.6, 0, 8] if axis == 'z' else [8, 0, 4 if clipped else 1.6],
            'to': [12 if clipped else 14.4, height, 8] if axis == 'z' else [8, height, 12 if clipped else 14.4],
            'rotation': {'origin': [8, 0, 8], 'axis': 'y', 'angle': 45, 'rescale': True},
            'shade_direction_override': 'up',
            'faces': {side: {'uv': uv, 'texture': '#sprays'} for side in
                      (('north', 'south') if axis == 'z' else ('west', 'east'))},
        }
        elements.append(plane)
    return {
        'parent': 'minecraft:block/block',
        'ambientocclusion': False,
        'textures': {
            'particle': 'blockbranch:block/silver_steppe_shrub_leaves',
            'leaves': 'blockbranch:block/silver_steppe_shrub_leaves',
            'sprays': 'blockbranch:block/silver_steppe_shrub_sprays',
            'stem': 'blockbranch:block/silver_steppe_shrub_stem',
        },
        'elements': elements,
    }


def write_shrub():
    write_texture('leaves', LEAVES)
    write_texture('sprays', SPRAYS)
    write_texture('stem', STEM)
    variants = {}
    for clipped in (False, True):
        name = 'silver_steppe_shrub' + ('_clipped' if clipped else '')
        path = ROOT / f'models/block/{name}.json'
        path.write_text(json.dumps(model(clipped), indent=2) + '\n')
        variants['clipped=' + str(clipped).lower()] = [
            dict(model='blockbranch:block/' + name, **({'y': angle} if angle else {}))
            for angle in (0, 90, 180, 270)
        ]
    (ROOT / 'blockstates/silver_steppe_shrub.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')


if __name__ == '__main__':
    write_shrub()
