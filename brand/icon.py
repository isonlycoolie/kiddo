"""Renders the iOS app icon: the mark, white on ink, as the Android launcher icon draws it.

Run from the repo root: python brand/icon.py. Needs Pillow.
"""

from pathlib import Path

from PIL import Image, ImageDraw

from banner import INK, draw_mark

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app/iosApp/Kiddo/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png"

SIZE, S = 1024, 4  # final size and supersampling factor
MARK = 0.5  # the face's share of the icon width; iOS rounds the corners itself


def main() -> None:
    w = SIZE * S
    icon = Image.new("RGB", (w, w), INK)
    mark = w * MARK
    draw_mark(ImageDraw.Draw(icon), (w - mark) / 2, (w - mark) / 2, mark)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    icon.resize((SIZE, SIZE), Image.LANCZOS).save(OUT, optimize=True)
    print(f"wrote {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
