#!/usr/bin/env python3
"""Render simulated Glyph Matrix faces as PNGs for the README.

Dependency-free (pure stdlib). Reproduces the 13x13 face that the app builds
in MatrixRenderer.renderScheduleFace / renderWheelBitmap, then draws it the way
the physical Nothing Phone (4a) Pro matrix looks: round white LEDs on a dark
circular field, corners absent. These are *simulations* of the rear matrix
(which can't be screen-captured), not photos of hardware.

    python3 tools/render_matrix.py            # writes docs/images/matrix-*.png
"""
import math
import os
import struct
import zlib

GRID = 13                      # 4a Pro matrix is 13x13
CELL = 40                      # px per cell in the output image
SIZE = GRID * CELL
RADIUS = 6.5                   # circular boundary (cells from center 6,6)
DOT_R = CELL * 0.40            # lit dot radius
BG = (11, 11, 11)              # near-black field
DIM = (42, 42, 42)            # unlit dot inside the circle
LIT = (245, 245, 245)         # lit LED (white)

# --- 3x5 pixel font, mirrored from PixelFont.kt ---------------------------
GLYPHS = {
    'A': [".#.", "#.#", "###", "#.#", "#.#"], 'B': ["##.", "#.#", "##.", "#.#", "##."],
    'C': [".##", "#..", "#..", "#..", ".##"], 'D': ["##.", "#.#", "#.#", "#.#", "##."],
    'E': ["###", "#..", "##.", "#..", "###"], 'F': ["###", "#..", "##.", "#..", "#.."],
    'G': [".##", "#..", "#.#", "#.#", ".##"], 'H': ["#.#", "#.#", "###", "#.#", "#.#"],
    'I': ["###", ".#.", ".#.", ".#.", "###"], 'J': ["..#", "..#", "..#", "#.#", ".#."],
    'K': ["#.#", "#.#", "##.", "#.#", "#.#"], 'L': ["#..", "#..", "#..", "#..", "###"],
    'M': ["#.#", "###", "###", "#.#", "#.#"], 'N': ["##.", "#.#", "#.#", "#.#", "#.#"],
    'O': [".#.", "#.#", "#.#", "#.#", ".#."], 'P': ["##.", "#.#", "##.", "#..", "#.."],
    'Q': [".#.", "#.#", "#.#", ".#.", "..#"], 'R': ["##.", "#.#", "##.", "#.#", "#.#"],
    'S': [".##", "#..", ".#.", "..#", "##."], 'T': ["###", ".#.", ".#.", ".#.", ".#."],
    'U': ["#.#", "#.#", "#.#", "#.#", "###"], 'V': ["#.#", "#.#", "#.#", "#.#", ".#."],
    'W': ["#.#", "#.#", "###", "###", "#.#"], 'X': ["#.#", "#.#", ".#.", "#.#", "#.#"],
    'Y': ["#.#", "#.#", ".#.", ".#.", ".#."], 'Z': ["###", "..#", ".#.", "#..", "###"],
    '0': [".#.", "#.#", "#.#", "#.#", ".#."], '1': [".#.", "##.", ".#.", ".#.", "###"],
    '2': ["##.", "..#", ".#.", "#..", "###"], '3': ["##.", "..#", ".#.", "..#", "##."],
    '4': ["#.#", "#.#", "###", "..#", "..#"], '5': ["###", "#..", "##.", "..#", "##."],
    '6': [".##", "#..", "##.", "#.#", ".#."], '7': ["###", "..#", "..#", ".#.", ".#."],
    '8': [".#.", "#.#", ".#.", "#.#", ".#."], '9': [".#.", "#.#", ".##", "..#", "##."],
    '-': ["...", "...", "###", "...", "..."], ' ': ["...", "...", "...", "...", "..."],
}
GW, GH, TRACK = 3, 5, 1


def width_of(text):
    return 0 if not text else len(text) * GW + (len(text) - 1) * TRACK


def stamp(grid, text, top_row):
    """Center [text] horizontally, top edge at top_row (mirrors PixelFont.stamp)."""
    x = max((GRID - width_of(text)) // 2, 0)
    for ch in text.upper():
        g = GLYPHS.get(ch, GLYPHS[' '])
        for dy in range(GH):
            y = top_row + dy
            if not 0 <= y < GRID:
                continue
            for dx in range(GW):
                if g[dy][dx] == '#' and 0 <= x + dx < GRID:
                    grid[y][x + dx] = 1.0
        x += GW + TRACK


def schedule_face(top, bottom):
    """MatrixRenderer.renderScheduleFace: top row 1, bottom row 7."""
    grid = [[0.0] * GRID for _ in range(GRID)]
    stamp(grid, top, 1)
    stamp(grid, bottom, 7)
    return grid


def wheel_face(frame_index=1):
    """MatrixRenderer.renderWheelBitmap: 5-spoke spinning wheel."""
    grid = [[0.0] * GRID for _ in range(GRID)]
    cx = cy = (GRID - 1) / 2.0
    outer = GRID / 2.0 - 0.5
    for row in range(GRID):
        for col in range(GRID):
            r = math.hypot(row - cy, col - cx)
            if abs(r - outer) < 1.0:
                grid[row][col] = 0.30
    base = frame_index * (math.pi / 4)
    for spoke in range(5):
        angle = base + spoke * 2 * math.pi / 5
        ca, sa = math.cos(angle), math.sin(angle)
        t = 0.0
        while t <= outer:
            r = round(cy + t * sa)
            c = round(cx + t * ca)
            if 0 <= r < GRID and 0 <= c < GRID:
                grid[r][c] = 1.0 if t < 2.0 else 0.80
            t += 0.5
    for row in range(GRID):
        for col in range(GRID):
            if math.hypot(row - cy, col - cx) < 1.5:
                grid[row][col] = 1.0
    return grid


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def render_png(grid, path):
    px = bytearray()
    center = (GRID - 1) / 2.0
    for py in range(SIZE):
        row = bytearray()
        for pxn in range(SIZE):
            col_f, row_f = pxn / CELL, py / CELL
            ci, ri = int(col_f), int(row_f)
            color = BG
            if math.hypot(ri - center, ci - center) <= RADIUS:
                # distance from this pixel to its cell center, in px
                ccx = ci * CELL + CELL / 2.0
                ccy = ri * CELL + CELL / 2.0
                d = math.hypot(pxn - ccx, py - ccy)
                v = grid[ri][ci]
                base = LIT if v > 0.15 else DIM
                r = DOT_R * (max(min(v, 1.0), 0.55) if v > 0.15 else 0.30)
                edge = r - d
                if edge > 0:
                    a = min(edge, 1.0)          # 1px anti-alias
                    color = lerp(BG, base, a)
            row += bytes(color)
        px += b'\x00' + row                     # filter byte 0 per scanline
    raw = zlib.compress(bytes(px), 9)

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff))

    ihdr = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 2, 0, 0, 0)  # 8-bit RGB
    with open(path, "wb") as f:
        f.write(b'\x89PNG\r\n\x1a\n')
        f.write(chunk(b'IHDR', ihdr))
        f.write(chunk(b'IDAT', raw))
        f.write(chunk(b'IEND', b''))


STATES = {
    "matrix-countdown":  schedule_face("GP", "2D"),   # race week, days out
    "matrix-qualifying": schedule_face("Q", "5H"),    # qualifying, hours out
    "matrix-imminent":   schedule_face("SQ", "42M"),  # sprint quali, minutes out
    "matrix-live":       wheel_face(frame_index=1),   # live session, spinning wheel
    "matrix-result":     schedule_face("VER", "GP"),  # post-session winner
    "matrix-standings":  schedule_face("VER", "437"), # WDC leader + points cycle
}


def main():
    out = os.path.join(os.path.dirname(__file__), "..", "docs", "images")
    os.makedirs(out, exist_ok=True)
    for name, grid in STATES.items():
        path = os.path.normpath(os.path.join(out, name + ".png"))
        render_png(grid, path)
        print("wrote", path)


if __name__ == "__main__":
    main()
