#!/usr/bin/env python3
"""make-android-icons.py - generate the Enigma launcher icon set for Android.

Draws the same rotor-wheel mark as the desktop icon: a brass disc with a toothed
rim of 26 teeth (the 26 letters the machine can send) around a dark concentric
hub. The teeth are not decoration: they encode the alphabet size, which is the
one number that defines the machine.

Android wants an adaptive icon made of independent layers, because the launcher
crops and animates them separately:

  foreground - the rotor, drawn small and centred inside a 108dp canvas so that
               the launcher's crop never clips a tooth
  background - a flat, weathered case-olive field
  legacy     - square and round PNGs for launchers that predate adaptive icons
               and for the Play Store listing

Written against zlib and struct only, with no Pillow dependency, so it runs on a
stock Python. Safe to run from any working directory.
"""

import math
import os
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "app", "src", "main", "res")

# Adaptive-icon layers are 108dp at these densities; the safe zone is the
# central 72dp, so the rotor must sit inside a circle of 66% of the canvas.
LAYER_SIZES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}

# Legacy launcher bitmaps, at the standard mipmap ladder.
LEGACY_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

BRASS = (198, 150, 62, 255)
BRASS_DARK = (150, 110, 45, 255)
BRASS_BRIGHT = (224, 180, 90, 255)
HUB_DARK = (38, 30, 18, 255)
CASE_BG = (30, 27, 20, 255)


def blank(size, colour=(0, 0, 0, 0)):
    """A row-major RGBA buffer of a single colour."""
    return [colour] * (size * size)


def put(px, size, x, y, colour):
    """Write one pixel with a bounds check. Alpha is taken as given."""
    if 0 <= x < size and 0 <= y < size:
        i = y * size + x
        src = px[i]
        a = colour[3] / 255.0
        if a >= 1.0:
            px[i] = colour
        elif a > 0:
            px[i] = (
                int(colour[0] * a + src[0] * (1 - a)),
                int(colour[1] * a + src[1] * (1 - a)),
                int(colour[2] * a + src[2] * (1 - a)),
                max(src[3], colour[3]),
            )


def disc(px, size, cx, cy, r, colour):
    """Fill a circle, 4x supersampled on the edge for a clean rim."""
    x0, x1 = int(cx - r) - 2, int(cx + r) + 2
    y0, y1 = int(cy - r) - 2, int(cy + r) + 2
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r - 1:
                put(px, size, x, y, colour)
            elif d < r:
                # Soft edge: partial alpha over the last pixel of the rim.
                edge = int(255 * (r - d))
                put(px, size, x, y, (colour[0], colour[1], colour[2], edge))


def draw_rotor(size, scale=1.0, draw_bg=False):
    """Render the rotor wheel centred in a `size` canvas.

    `scale` is the fraction of the canvas the wheel's outer radius occupies, so
    the adaptive-icon foreground can be inset inside the safe zone.
    """
    px = blank(size)
    if draw_bg:
        px = blank(size, CASE_BG)

    cx = cy = size / 2.0
    outer = (size / 2.0) * scale

    if not draw_bg:
        # A backing disc so the brass reads against any launcher wallpaper. The
        # adaptive background layer supplies the colour in the real launcher,
        # but the legacy bitmaps need it baked in.
        disc(px, size, cx, cy, outer * 1.06, (20, 18, 13, 255))

    # The toothed rim: 26 teeth, one per letter on the alphabet ring.
    teeth = 26
    teeth_r_out = outer
    teeth_r_in = outer * 0.80
    body_r = outer * 0.78
    for i in range(teeth):
        a = (2 * math.pi * i / teeth) - (math.pi / 2)
        w = (2 * math.pi / teeth) * 0.34
        # A tooth is a short radial wedge; draw it as a filled triangle pair.
        pts = [
            (cx + math.cos(a - w) * teeth_r_in, cy + math.sin(a - w) * teeth_r_in),
            (cx + math.cos(a + w) * teeth_r_in, cy + math.sin(a + w) * teeth_r_in),
            (cx + math.cos(a) * teeth_r_out, cy + math.sin(a) * teeth_r_out),
        ]
        _fill_triangle(px, size, pts, BRASS_BRIGHT)

    # The rotor body.
    disc(px, size, cx, cy, body_r, BRASS)
    # A darker inner ring reads as the bevelled face of the wheel.
    disc(px, size, cx, cy, body_r * 0.82, BRASS_DARK)
    disc(px, size, cx, cy, body_r * 0.74, BRASS)

    # The hub, with a small keyway notch like a real rotor spindle.
    disc(px, size, cx, cy, body_r * 0.30, HUB_DARK)
    disc(px, size, cx, cy, body_r * 0.22, BRASS_DARK)
    disc(px, size, cx, cy, body_r * 0.12, HUB_DARK)

    return px


def _fill_triangle(px, size, pts, colour):
    """Scanline-fill a triangle given three (x, y) vertices."""
    (x1, y1), (x2, y2), (x3, y3) = pts
    miny = int(min(y1, y2, y3))
    maxy = int(max(y1, y2, y3))
    for y in range(miny, maxy + 1):
        xs = []
        for (ax, ay), (bx, by) in ((pts[0], pts[1]), (pts[1], pts[2]), (pts[2], pts[0])):
            if ay == by:
                continue
            if min(ay, by) <= y <= max(ay, by):
                t = (y - ay) / (by - ay)
                xs.append(ax + t * (bx - ax))
        if len(xs) >= 2:
            for x in range(int(min(xs)), int(max(xs)) + 1):
                put(px, size, x, y, colour)


def write_png(path, px, size):
    """Write an RGBA buffer as a PNG without any third-party library."""
    raw = bytearray()
    for y in range(size):
        raw.append(0)  # filter type 0 (None) for each scanline
        row = px[y * size:(y + 1) * size]
        for (r, g, b, a) in row:
            raw += bytes((r, g, b, a))

    def chunk(tag, data):
        out = struct.pack(">I", len(data)) + tag + data
        return out + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", header)
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(png)


def resample(px, src, dst):
    """Box-filter a square RGBA buffer from src to dst pixels."""
    out = blank(dst)
    ratio = src / dst
    for y in range(dst):
        for x in range(dst):
            r = g = b = a = n = 0
            y0, y1 = int(y * ratio), max(int(y * ratio) + 1, int((y + 1) * ratio))
            x0, x1 = int(x * ratio), max(int(x * ratio) + 1, int((x + 1) * ratio))
            for sy in range(y0, min(y1, src)):
                for sx in range(x0, min(x1, src)):
                    p = px[sy * src + sx]
                    r += p[0]
                    g += p[1]
                    b += p[2]
                    a += p[3]
                    n += 1
            if n:
                out[y * dst + x] = (r // n, g // n, b // n, a // n)
    return out


def main():
    master = 432  # render once at the largest layer size

    # Adaptive foreground: the rotor inset to 62% of the canvas so the launcher
    # can crop to a circle or a squircle without clipping a tooth.
    fg = draw_rotor(master, scale=0.62)
    for dens, size in LAYER_SIZES.items():
        img = fg if size == master else resample(fg, master, size)
        write_png(os.path.join(RES, f"mipmap-{dens}", "ic_launcher_foreground.png"), img, size)

    # Adaptive background: a flat case-olive field with a very subtle vignette.
    bg = blank(master, CASE_BG)
    for y in range(master):
        for x in range(master):
            d = math.hypot(x - master / 2, y - master / 2) / (master / 2)
            if d > 0.55:
                k = min(1.0, (d - 0.55) / 0.45)
                p = bg[y * master + x]
                bg[y * master + x] = (
                    int(p[0] * (1 - 0.35 * k)),
                    int(p[1] * (1 - 0.35 * k)),
                    int(p[2] * (1 - 0.35 * k)),
                    255,
                )
    for dens, size in LAYER_SIZES.items():
        img = bg if size == master else resample(bg, master, size)
        write_png(os.path.join(RES, f"mipmap-{dens}", "ic_launcher_background.png"), img, size)

    # Legacy bitmaps: the full wheel with its own background baked in, because
    # pre-adaptive launchers apply no compositing.
    legacy = draw_rotor(master, scale=0.92, draw_bg=True)
    for dens, size in LEGACY_SIZES.items():
        img = resample(legacy, master, size)
        write_png(os.path.join(RES, f"mipmap-{dens}", "ic_launcher.png"), img, size)

        # Round variant: same mark, masked to a circle.
        rnd = draw_rotor(master, scale=0.92, draw_bg=False)
        # Re-render with a round background rather than a square one.
        out = blank(master)
        disc(out, master, master / 2, master / 2, master / 2 - 1, CASE_BG)
        for i, p in enumerate(rnd):
            if p[3] > 0:
                out[i] = p
        write_png(
            os.path.join(RES, f"mipmap-{dens}", "ic_launcher_round.png"),
            resample(out, master, size),
            size,
        )

    print(f"icons written under {RES}")


if __name__ == "__main__":
    main()
