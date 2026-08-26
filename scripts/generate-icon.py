#!/usr/bin/env python3
"""Generate the Chocolatey package icon (docs/img/console-chess-icon.png).

A terminal-style icon: the literal prompt ``C:\\`` followed by a chess knight
(horse head) on a dark, rounded background. Run from anywhere in the repo:

    python3 scripts/generate-icon.py

Requires Pillow:  python3 -m pip install Pillow
"""

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
    print(f"saved {OUT} ({SIZE}x{SIZE})")


if __name__ == "__main__":
    main()
