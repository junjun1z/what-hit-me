"""Draws the What Hit Me? icon as 32x32 pixel art and scales it up (nearest neighbour).

A cracked red heart on a dark rounded panel, with a magnifying glass showing a "?".

python tools/make_icon.py  ->  docs/icon.png (512), docs/icon_128.png, docs/icon_64.png
"""
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
N = 32

COLORS = {
    ".": (0, 0, 0, 0),
    "B": (88, 64, 96, 255),       # panel border
    "D": (30, 24, 38, 255),       # panel background
    "K": (40, 6, 12, 255),        # heart outline
    "r": (200, 30, 40, 255),      # heart red
    "s": (140, 16, 28, 255),      # heart shade
    "h": (255, 120, 120, 255),    # heart highlight
    "c": (28, 4, 8, 255),         # crack
    "G": (24, 24, 30, 255),       # magnifier outline
    "F": (200, 160, 60, 255),     # magnifier frame (brass)
    "f": (250, 215, 110, 255),    # frame highlight
    "L": (150, 205, 235, 255),    # lens
    "l": (225, 245, 255, 255),    # lens glint
    "q": (20, 30, 60, 255),       # question mark
    "H": (92, 54, 26, 255),       # handle
    "j": (140, 88, 44, 255),      # handle light
}

# Heart, 22 x 19, top-left at (2, 4). '#' = inside.
HEART = [
    "...######....######...",
    "..########..########..",
    ".####################.",
    "######################",
    "######################",
    "######################",
    "######################",
    "######################",
    ".####################.",
    "..##################..",
    "...################...",
    "....##############....",
    ".....############.....",
    "......##########......",
    ".......########.......",
    "........######........",
    ".........####.........",
    "..........##..........",
]
HEART_X, HEART_Y = 3, 4

# Crack path inside the heart (absolute coords), zig-zag from the top notch downwards.
CRACK = [(14, 5), (14, 6), (14, 7), (13, 7), (13, 8), (13, 9), (12, 9), (12, 10),
         (13, 10), (14, 10), (14, 11), (14, 12), (13, 12), (13, 13), (12, 13), (12, 14),
         (12, 15), (13, 15), (13, 16)]

# Question mark, 5 x 7, drawn in the lens.
QMARK = [
    ".qqqq.",
    "qq..qq",
    "....qq",
    "...qq.",
    "..qq..",
    "......",
    "..qq..",
]


def build():
    g = [["." for _ in range(N)] for _ in range(N)]
    # rounded panel
    for y in range(N):
        for x in range(N):
            cut = {0: 3, 1: 2, 2: 1}
            dy = min(y, N - 1 - y)
            if dy in cut and min(x, N - 1 - x) < cut[dy]:
                continue
            g[y][x] = "D"
    for y in range(N):
        for x in range(N):
            if g[y][x] == ".":
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < N and 0 <= ny < N) or g[ny][nx] == ".":
                    g[y][x] = "B"
                    break

    # heart body
    inside = set()
    for j, row in enumerate(HEART):
        for i, ch in enumerate(row):
            if ch == "#":
                inside.add((HEART_X + i, HEART_Y + j))
    for (x, y) in inside:
        g[y][x] = "r"
    # outline around the heart
    for (x, y) in list(inside):
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                p = (x + dx, y + dy)
                if p not in inside and 0 <= p[0] < N and 0 <= p[1] < N:
                    g[p[1]][p[0]] = "K"
    # shading: lower-right edge darker, top-left highlight
    for (x, y) in inside:
        if (x + 1, y + 1) not in inside or (x + 2, y + 1) not in inside:
            g[y][x] = "s"
    for (x, y) in [(6, 6), (7, 6), (8, 6), (5, 7), (6, 7), (5, 8), (5, 9),
                   (18, 6), (19, 6)]:
        g[y][x] = "h"
    # crack
    for (x, y) in CRACK:
        g[y][x] = "c"

    # magnifying glass: lens + brass ring + dark outline
    cx, cy = 20.0, 18.0
    for y in range(N):
        for x in range(N):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= 5.2:
                g[y][x] = "L"
            elif d <= 6.6:
                g[y][x] = "f" if (x < cx - 1 and y < cy - 1) else "F"
            elif d <= 7.6:
                g[y][x] = "G"
    for (x, y) in [(16, 15), (16, 16), (15, 17)]:  # lens glint
        g[y][x] = "l"
    for j, row in enumerate(QMARK):
        for i, ch in enumerate(row):
            if ch == "q":
                g[15 + j][17 + i] = "q"

    # handle: a thick diagonal towards the bottom-right corner, outlined
    handle = {}
    for t in range(5):
        x, y = 25 + t, 23 + t
        handle[(x, y)] = "H"
        handle[(x + 1, y)] = "j"
        handle[(x, y + 1)] = "H"
    lens = ("L", "l", "q", "F", "f")
    for (x, y), c in handle.items():
        if g[y][x] not in lens:
            g[y][x] = c
    for (x, y) in handle:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                p = (x + dx, y + dy)
                if p not in handle and g[p[1]][p[0]] not in lens + ("B", "."):
                    g[p[1]][p[0]] = "G"
    return g


def main():
    g = build()
    img = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            img.putpixel((x, y), COLORS[g[y][x]])
    out = os.path.join(ROOT, "docs")
    os.makedirs(out, exist_ok=True)
    img.resize((512, 512), Image.NEAREST).save(os.path.join(out, "icon.png"))
    img.resize((128, 128), Image.NEAREST).save(os.path.join(out, "icon_128.png"))
    img.resize((64, 64), Image.NEAREST).save(os.path.join(out, "icon_64.png"))
    print("written to", out)


if __name__ == "__main__":
    main()
