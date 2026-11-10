#!/usr/bin/env python3
"""Original red-deer pixel coats, venison icons and stylized synthesized vocal assets.

Vocal assets are synthesis, not field recordings. Audition is a separate review gate.
Coats-only needs the Python standard library. Full audio authoring additionally
requires numpy/scipy and ffmpeg; never rewrites boar assets.
"""
from pathlib import Path
import json
import math
import sys
import random
import struct
import subprocess
import tempfile
import wave
import zlib
from venison_food_item import write_venison_food

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/blockbranch"


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)


def png(path, width, height, pixel):
    rows = [b"\0" + bytes(channel for x in range(width) for channel in pixel(x, y)) for y in range(height)]
    data = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(b"".join(rows))) + chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def coat(variant):
    # Broad connected flank shading and short directional tufts survive normal
    # camera distances. Avoid independent per-texel grain on this small UV area.
    flank = (
        'ddddddddddddddd',
        'mmmdmmmmmdmmmmm',
        'mmmmmmmmmmmmmmm',
        'mmlmmmmlmmmmmmm',
        'mmmmmmllmmmlmmm',
        'llllhllllllhhll',
        'hhbhhhhhbhhhhhh',
        'bbbbbbbbbbbbbbb',
    )
    rng = random.Random(481)
    clusters = [[rng.randrange(-3, 4) for _ in range(64)] for _ in range(48)]
    grain = [[rng.randrange(-2, 3) for _ in range(128)] for _ in range(96)]
    calf = variant == "calf"
    base_coat = (161, 104, 56) if not calf else (175, 119, 64)
    def mix(a, b, t):
        return tuple(round(x+(y-x)*t) for x, y in zip(a, b))
    def pixel(x, y):
        base = base_coat
        # Body top: subdued dorsal shading. Side faces: warmer flank into pale belly.
        if 15 <= x < 23 and y < 15:
            base = mix(base_coat, (123, 79, 43), .4 if 18 <= x <= 20 else .18)
        if (0 <= x < 15 or 23 <= x < 38) and 15 <= y < 23:
            row = y-15
            along = 14-x if x < 15 else x-23
            tones = {'d':(145,92,50),'m':base_coat,
                     'l':(173,118,65),'h':(186,138,82),'b':(203,163,109)}
            base = tones[flank[row][along]]
            if calf:
                base = mix(base, (190,130,71), .22)
                if (along,row) in {(2,2),(3,2),(7,2),(12,2),(12,3),(4,4),(8,5),(9,5),(13,5)}:
                    base = (233,211,161)
        if 15 <= x < 23 and 15 <= y < 23:
            row=y-15;column=x-15
            base=mix(base_coat,(204,170,116),max(0,row-2)/9)
            if variant=="stag" and row<4:
                edge=(3,2,4,3,4,2,3,2)[column]
                if row<edge:base=(119,79,45) if row+1<edge else (145,96,53)
            elif row==1 and column in (1,5):base=mix(base,(194,140,80),.35)
            if calf and (column,row) in {(1,2),(5,3),(5,4)}:base=(229,206,156)
        # Creamy rump with a soft stepped edge; no fallow-deer black border.
        if 38 <= x < 46 and 15 <= y < 23:
            rx, ry = x-38, y-15
            if 1 <= rx <= 6 and 2 <= ry <= 7 and not (ry == 2 and rx in (1,6)):
                base = (209, 186, 135)
            else: base = mix(base_coat, (179, 130, 76), .25)
        # Short mane tufts taper from the dark throat into the warm chest.
        if x < 18 and 32 <= y < 48:
            mane=(103,69,41) if variant=="stag" else (151,98,53)
            base=mix(mane,base_coat,.25 if y<37 else .08)
            if 37<=y<47:
                row=y-37
                front=5<=x<9
                along=x-5 if front else x%5
                edge=((8,6,7,5) if front else (7,5,6,8,6))[along]
                if variant=="stag":
                    if row>=edge:base=mix(base_coat,(185,132,75),.2)
                    elif row==edge-1:base=mix(mane,base_coat,.55)
                    elif row in (2,4,5) and (along+row)%3==0:base=mix(mane,base_coat,.22)
                elif front and row>=6:
                    base=mix(base_coat,(207,171,119),.25+(row-6)*.08)
        if 32 <= x < 54 and 32 <= y < 43:
            base = mix(base_coat, (126, 83, 46), .2 if y < 39 else .05)
        # Preserve the original unused head net; the painted face lives below.
        if (x in (36,37,44,45)) and 39<=y<=41:base=(187,151,102)
        if (x in (37,38,43,44)) and y in (40,41):base=(24,20,15)
        if (x,y) in ((37,40),(44,40)):base=(113,96,67)
        if 39<=x<43 and 39<=y<43:
            base=(176,122,68) if y==39 else (158,105,58)
            if x in (39,42) and y==40:base=(24,20,15)
            if x in (40,41) and y==41:base=(187,143,88)
        if 37 <= x < 48 and 48 <= y < 55: base = (177, 139, 91)
        if 37 <= x < 40 and 53 <= y < 55:base=(43,35,26)
        if 40 <= x < 43 and 48 <= y < 53: base = (197, 166, 116)
        # Slender legs taper in tone into actual dark hoof markings.
        if 64 <= x < 72 and y < 15:
            base = mix((155, 105, 59), (108, 78, 46), max(0,y-5)/12)
            if y >= 13: base = (48, 39, 28)
        # Short tail blends into the rump; ears have quiet tan inner faces.
        if 64 <= x < 72 and 24 <= y < 31:
            base = (153, 103, 58) if x < 66 or x >= 70 else (209, 184, 132)
        if 64 <= x < 72 and 48 <= y < 54:
            base = (137, 94, 55)
            if 65 <= x < 67 and 50 <= y < 53: base = (188, 151, 111)
        if 96 <= x < 112 and y < 16:
            base = (187, 168, 132) if y % 4 else (172, 151, 115)
        # Reserved head net: compact corner eyes, with no horizontal light band.
        # Front face is 94..101 by 46..53; one pupil spans each front/side seam.
        head_pixel=80<=x<124 and 32<=y<54
        if head_pixel:
            base=mix(base_coat,(126,83,46),.2 if y<46 else .05)
            if (x,y) in ((97,46),(98,47)):base=mix(base,(185,133,76),.3)
            if x in (93,94,101,102) and y==48:base=(42,32,21)
            if x in (92,95,100,103) and y==48:base=mix(base,(205,164,107),.35)
        # Reserved muzzle net: twice the sampling of the unchanged geometry.
        # Front cap is 90..95 by 74..77, with clear rows between nose and mouth.
        muzzle_pixel=80<=x<112 and 64<=y<78
        if muzzle_pixel:
            base=(177,139,91)
            if y>=74:base=((177,139,91),(185,148,98),(195,160,106),(195,160,106))[y-74]
            if x in (92,93) and y==74:base=(43,35,26)
            if x in (92,93) and y==76:base=(98,67,43)
            if y==76 and (86<=x<90 or 96<=x<100):base=(112,79,49)
        flank_pixel = (x < 15 or 23 <= x < 38) and 15 <= y < 23
        neck_pixel = x < 18 and 32 <= y < 48
        chest_pixel = 15<=x<23 and 15<=y<23
        eye_pixel = 32<=x<54 and 39<=y<=41
        # Preserve existing legs, ears, tail and antler texels exactly.
        variation = 0 if flank_pixel or neck_pixel or chest_pixel or eye_pixel or head_pixel or muzzle_pixel else clusters[y//2][x//2] + grain[y][x]
        return tuple(max(0,min(255,c+variation)) for c in base) + (255,)
    return pixel


def vocal(kind, seed, sample_rate=22050):
    rng = np.random.default_rng(seed)
    duration, frequency, formants, breath = {
        "contact": (.58, 290, (650, 1250, 2300), .12),
        "bellow": (1.9, 78, (400, 820, 1400), .24),
        "hurt": (.42, 390, (700, 1350, 2100), .24),
        "death": (.95, 160, (520, 1100, 1750), .35),
    }[kind]
    duration *= rng.uniform(.93, 1.07)
    t = np.arange(int(sample_rate*duration))/sample_rate
    p = t/duration
    contour = (0.85 + .45*np.sin(np.pi*p)**.8) if kind == "bellow" else (1.12-.42*p+.15*np.sin(np.pi*p))
    jitter = np.interp(t, np.arange(0, duration+.012, .012), rng.normal(0, .015, int(math.ceil(duration/.012))+1))
    f0 = frequency*rng.uniform(.94,1.06)*contour*(1+.025*np.sin(2*np.pi*5.2*t)+jitter)
    phase = np.cumsum(f0)*2*np.pi/sample_rate
    signal = np.zeros_like(t)
    for harmonic in range(1, 65):
        hz = harmonic*frequency
        weight = .06 + sum(np.exp(-.5*((hz-center)/(center*.18))**2) for center in formants)
        signal += np.sin(harmonic*phase + .08*np.sin(phase*.071+harmonic))*weight/harmonic**.95
    noise = sosfilt(butter(3, [280, 3100], btype="bandpass", fs=sample_rate, output="sos"), rng.normal(size=len(t)))
    envelope = np.minimum(1,p/.08)*np.minimum(1,(1-p)/.22)*np.sin(np.pi*p)**.32
    roughness = 1+.16*np.sin(2*np.pi*(24 if kind=="bellow" else 34)*t)
    signal = envelope*(signal*roughness + breath*noise)
    signal = np.tanh(signal*1.7)
    signal *= .75/max(.001, np.max(np.abs(signal)))
    return sample_rate, signal


if __name__ == "__main__":
    for variant in ("stag", "hind", "calf"):
        png(ASSETS / f"textures/entity/red_deer_{variant}.png", 128, 96, coat(variant))
    if "--coats-only" in sys.argv:
        raise SystemExit(0)
    import numpy as np
    from scipy.signal import butter, sosfilt
    for cooked in (False, True):
        write_venison_food(cooked)
    directory = ASSETS / "sounds/red_deer"
    directory.mkdir(parents=True, exist_ok=True)
    for index, kind in enumerate(("contact", "bellow", "hurt", "death")):
        for variant in (1, 2):
            rate, signal = vocal(kind, 131+index*10+variant)
            with tempfile.TemporaryDirectory() as temporary:
                path = Path(temporary) / "call.wav"
                with wave.open(str(path), "wb") as output:
                    output.setnchannels(1);output.setsampwidth(2);output.setframerate(rate)
                    output.writeframes((signal*32767).astype("<i2").tobytes())
                subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", str(path), "-c:a", "libvorbis", "-q:a", "4",
                                str(directory/f"{kind}{variant}.ogg")], check=True)
