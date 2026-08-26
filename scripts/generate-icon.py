#!/usr/bin/env python3
"""Generate the Chocolatey package icon (docs/img/console-chess-icon.png).

A terminal-style icon: the literal prompt ``C:\\`` followed by a chess knight
(horse head) on a dark, rounded background. Run from anywhere in the repo:

    python3 scripts/generate-icon.py

Requires Pillow:  python3 -m pip install Pillow
"""

import struct
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

# --- design constants ---------------------------------------------------------
SIZE = 512
BACKGROUND = (18, 19, 24, 255)     # near-black, slightly blue
FOREGROUND = (248, 248, 248, 255)  # white: prompt and horse
RADIUS = 96                        # background corner radius

TEXT = "C:\\"       # keep the literal prompt as-is
HORSE = "\u265E"    # U+265E black chess knight (filled horse head)
TEXT_SIZE = 140
HORSE_SIZE = 310
GAP = 4             # space between the prompt and the horse

REPO_ROOT = Path(__file__).resolve().parent.parent
OUT = REPO_ROOT / "docs" / "img" / "console-chess-icon.png"
ICO = REPO_ROOT / "docs" / "img" / "console-chess-icon.ico"
ICO_SIZES = [(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)]

# macOS font locations; first hit wins.
MONO_CANDIDATES = [
    "/System/Library/Fonts/Menlo.ttc",
    "/System/Library/Fonts/Monaco.ttf",
]
SYMBOL_CANDIDATES = [
    "/System/Library/Fonts/Apple Symbols.ttf",
    "/Users/dmitryledentsov/Library/Fonts/DejaVuSans-Bold.ttf",
]


def load_font(candidates, size, index=0):
    for path in candidates:
        try:
            return ImageFont.truetype(path, size, index=index)
        except OSError:
            continue
    return ImageFont.load_default()


def _xor_dib(img):
    """BGRA pixel data (bottom-up) for the XOR mask of a 32bpp icon."""
    w, h = img.size
    rgba = img.convert("RGBA")
    data = bytearray()
    for y in range(h - 1, -1, -1):
        for x in range(w):
            r, g, b, a = rgba.getpixel((x, y))
            data += bytes((b, g, r, a))
    return bytes(data)


def save_ico(img, path, sizes):
    """Write a classic (BMP DIB entries) multi-resolution Windows icon."""
    images = []
    entries = []
    for w, h in sizes:
        resized = img.resize((w, h), Image.Resampling.LANCZOS)
        xor = _xor_dib(resized)
        # 1bpp AND mask, all zeros: the alpha channel carries transparency.
        and_mask = bytes(((w + 31) // 32) * 4 * h)
        dib = struct.pack(
            "<IiiHHIIiiII",
            40, w, h * 2, 1, 32, 0, len(xor) + len(and_mask), 0, 0, 0, 0,
        )
        blob = dib + xor + and_mask
        images.append(blob)
        entries.append((w if w < 256 else 0, h if h < 256 else 0, len(blob)))

    data = bytearray(struct.pack("<HHH", 0, 1, len(images)))
    offset = 6 + 16 * len(images)
    for w, h, size in entries:
        data += struct.pack("<BBBBHHII", w, h, 0, 0, 1, 32, size, offset)
        offset += size
    for blob in images:
        data += blob

    path.write_bytes(bytes(data))


def main():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))

    mask = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, SIZE - 1, SIZE - 1], radius=RADIUS, fill=255
    )
    img = Image.composite(Image.new("RGBA", (SIZE, SIZE), BACKGROUND), img, mask)
    draw = ImageDraw.Draw(img)

    text_font = load_font(MONO_CANDIDATES, TEXT_SIZE)
    horse_font = load_font(SYMBOL_CANDIDATES, HORSE_SIZE)

    text_width = draw.textlength(TEXT, font=text_font)
    horse_width = draw.textlength(HORSE, font=horse_font)
    total_width = text_width + horse_width + GAP
    x = (SIZE - total_width) / 2
    center_y = SIZE / 2

    draw.text((x, center_y), TEXT, font=text_font, fill=FOREGROUND, anchor="lm")
    draw.text(
        (x + text_width + GAP, center_y),
        HORSE,
        font=horse_font,
        fill=FOREGROUND,
        anchor="lm",
    )

    img.save(OUT)
    save_ico(img, ICO, ICO_SIZES)
    print(f"saved {OUT} ({SIZE}x{SIZE})")
    print(f"saved {ICO} {ICO_SIZES}")


if __name__ == "__main__":
    main()
