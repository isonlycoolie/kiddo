"""Renders .github/assets/kiddo-banner.png: the mark and wordmark on ink with the Kiddo glow in the corners.

Run from the repo root: python brand/banner.py. Needs Pillow.
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
FONT = ROOT / "brand/fonts/InterVariable.woff2"
OUT = ROOT / ".github/assets/kiddo-banner.png"

W, H, S = 1600, 320, 3  # final size and supersampling factor
INK = (17, 17, 17)
WHITE = (255, 255, 255)
GLOW = {"violet": (124, 92, 255), "sky": (43, 181, 232), "peach": (255, 157, 107)}


def glow_layer(w: int, h: int) -> Image.Image:
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    blobs = [("violet", 0.1, -0.35, 0.2, 70), ("peach", 0.92, -0.3, 0.18, 55), ("sky", 0.55, 1.15, 0.2, 45)]
    for name, cx, cy, r, alpha in blobs:
        x, y, rr = cx * w, cy * h, r * w
        d.ellipse((x - rr, y - rr * 0.8, x + rr, y + rr * 0.8), fill=(*GLOW[name], alpha))
    return layer.filter(ImageFilter.GaussianBlur(w * 0.06))


def cubic(p0, p1, p2, p3, n=40):
    pts = []
    for i in range(n + 1):
        t = i / n
        a, b, c, e = (1 - t) ** 3, 3 * (1 - t) ** 2 * t, 3 * (1 - t) * t**2, t**3
        pts.append((a * p0[0] + b * p1[0] + c * p2[0] + e * p3[0], a * p0[1] + b * p1[1] + c * p2[1] + e * p3[1]))
    return pts


def draw_mark(d: ImageDraw.ImageDraw, x: float, y: float, size: float) -> None:
    # The mark on a 32 grid (kit.tsx Mark): face r15, eyes at (11.6, 13.4) and (20.4, 13.4), smile from 10.6 to 21.4.
    k = size / 32
    p = lambda px, py: (x + px * k, y + py * k)  # noqa: E731
    d.ellipse((*p(1, 1), *p(31, 31)), fill=WHITE)
    for ex in (11.6, 20.4):
        d.ellipse((*p(ex - 2, 13.4 - 2.2), *p(ex + 2, 13.4 + 2.2)), fill=INK)
    left = cubic(p(10.6, 19.2), p(12.1, 21.5), p(13.9, 22.6), p(16, 22.6))
    right = cubic(p(16, 22.6), p(18.1, 22.6), p(19.9, 21.5), p(21.4, 19.2))
    # A round pen dragged along the curve: dense dots give a smooth stroke with round caps.
    r = 1.1 * k
    path = left + right[1:]
    for (ax, ay), (bx, by) in zip(path, path[1:]):
        for i in range(8):
            t = i / 8
            cx, cy = ax + (bx - ax) * t, ay + (by - ay) * t
            d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=INK)
    d.ellipse((path[-1][0] - r, path[-1][1] - r, path[-1][0] + r, path[-1][1] + r), fill=INK)


def draw_word(d: ImageDraw.ImageDraw, x: float, baseline: float, size: float) -> None:
    font = ImageFont.truetype(str(FONT), round(size))
    font.set_variation_by_name("Bold")
    for ch in "kiddo":
        d.text((x, baseline), ch, font=font, fill=WHITE, anchor="ls")
        x += font.getlength(ch) - size * 0.045


def word_width(size: float) -> float:
    font = ImageFont.truetype(str(FONT), round(size))
    font.set_variation_by_name("Bold")
    return sum(font.getlength(ch) - size * 0.045 for ch in "kiddo") + size * 0.045


def main() -> None:
    w, h = W * S, H * S
    card = Image.new("RGBA", (w, h), (*INK, 255))
    card.alpha_composite(glow_layer(w, h))

    d = ImageDraw.Draw(card)
    mark, word, gap = 116 * S, 128 * S, 28 * S
    total = mark + gap + word_width(word)
    x0 = (w - total) / 2
    draw_mark(d, x0, (h - mark) / 2, mark)
    draw_word(d, x0 + mark + gap, h / 2 + word * 0.36, word)

    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h - 1), radius=28 * S, fill=255)
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out.paste(card, mask=mask)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    out.resize((W, H), Image.LANCZOS).save(OUT, optimize=True)
    print(f"wrote {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
