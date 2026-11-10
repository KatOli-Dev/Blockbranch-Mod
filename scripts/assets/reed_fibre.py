#!/usr/bin/env python3
"""Hand-drawn 16px frayed reed fibre, gathered with a small green reed tie.

Optional Python standard-library authoring tool. Split ends fan from a narrow
waist; diagonal straw highlights describe fibres rather than intact seed stems.
"""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2]
PALETTE = {
    '.': (0, 0, 0, 0),
    'd': (130, 101, 57, 255),
    's': (163, 130, 72, 255),
    'm': (196, 164, 99, 255),
    'f': (222, 195, 132, 255),
    'h': (239, 218, 161, 255),
    'K': (77, 96, 46, 255),
    'g': (113, 132, 61, 255),
    'G': (155, 169, 91, 255),
}
PIXELS = (
    '................',
    '...........h....',
    '.........h.f.h..',
    '........hfmd.f..',
    '.......hfmshms..',
    '......hfmsfms...',
    '......fmsfms..h.',
    '.....Gfmsms..f..',
    '.....fGGsmsmf...',
    '....fmKgGs......',
    '...hfmsKG.......',
    '..hfmssfm.......',
    '..fmshms........',
    '.hfdsfm.........',
    '..s.hd..........',
    '................',
)


def write_fibre(destination=None):
    destination = Path(destination) if destination else ROOT / 'src/main/resources/assets/blockbranch/textures/item/reed_fibre.png'
    assert len(PIXELS) == 16 and all(len(row) == 16 for row in PIXELS)

    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)

    rows = b''.join(b'\0' + b''.join(bytes(PALETTE[p]) for p in row) for row in PIXELS)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return destination


if __name__ == '__main__':
    print(write_fibre())
