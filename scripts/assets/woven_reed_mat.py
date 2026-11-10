#!/usr/bin/env python3
"""Original 16px reed twill, drawn from directional over/under strand cells.

Python standard library only. Long lit ridges describe reed fibers; recessed
crossings stay small. The two-over/two-under pattern tiles without a rug border.
"""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2]
PALETTE = {
    'r': (123, 101, 61, 255),
    's': (157, 131, 78, 255),
    'd': (171, 143, 86, 255),
    'm': (189, 161, 99, 255),
    'f': (204, 178, 113, 255),
    'h': (220, 196, 133, 255),
}
HORIZONTAL = ('hfff', 'mmfm', 'ssds', 'hmsr')
VERTICAL = ('hmsf', 'hmsm', 'fmsd', 'fmsr')
WEAVE = ('HHVV', 'VHHV', 'VVHH', 'HVVH')


def write_mat(destination=None):
    destination = Path(destination) if destination else ROOT / 'src/main/resources/assets/blockbranch/textures/block/woven_reed_mat.png'
    pixels = []
    for row in WEAVE:
        for y in range(4):
            pixels.append(''.join((HORIZONTAL if cell == 'H' else VERTICAL)[y] for cell in row))
    assert len(pixels) == 16 and all(len(row) == 16 for row in pixels)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    rows = b''.join(b'\0' + b''.join(bytes(PALETTE[p]) for p in row) for row in pixels)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return destination


if __name__ == '__main__':
    print(write_mat())
