"""Generate 24 original offline JPG wallpaper assets for WALLORA. Requires Pillow."""
from pathlib import Path
import math
import random
from PIL import Image, ImageDraw, ImageFilter

OUT = Path(__file__).resolve().parents[1] / "app/src/main/assets/wallpapers"
OUT.mkdir(parents=True, exist_ok=True)
W, H = 720, 1280
PALETTES = {
    "amoled": [("#080311", "#8339F5"), ("#09040F", "#F13E9B"), ("#020C18", "#249CE0"), ("#0C0709", "#ED8243")],
    "nature": [("#112B58", "#FCB58F"), ("#173E51", "#F5D490"), ("#172D51", "#A586E9"), ("#10342D", "#95C8A3")],
    "space": [("#05051A", "#5B38B9"), ("#080B21", "#2F65D1"), ("#12091C", "#C34599"), ("#110B1A", "#BB6838")],
    "neon": [("#080519", "#B42FF1"), ("#041528", "#13DBD7"), ("#180817", "#FB5388"), ("#0D1324", "#EBC547")],
    "abstract": [("#0C1741", "#6643D8"), ("#1A0E30", "#EB4DAD"), ("#071E32", "#3683CB"), ("#24122A", "#E27B62")],
    "minimal": [("#102032", "#658CA8"), ("#20233C", "#988BAE"), ("#1E2927", "#7AA899"), ("#241F28", "#AA8F9D")],
}
def rgb(code):
    return tuple(bytes.fromhex(code[1:]))
for category, pairs in PALETTES.items():
    for index, (background, accent) in enumerate(pairs, 1):
        rng = random.Random(category + str(index))
        a, b = rgb(background), rgb(accent)
        im = Image.new("RGB", (W, H))
        d = ImageDraw.Draw(im)
        for y in range(H):
            t = min(1.0, y / H * 0.9)
            d.line((0, y, W, y), fill=tuple(int(a[i] * (1-t) + b[i] * t * 0.64) for i in range(3)))
        layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        p = ImageDraw.Draw(layer, "RGBA")
        if category in ("space", "amoled"):
            for _ in range(210 if category == "space" else 70):
                x, y = rng.randrange(W), rng.randrange(H)
                r = rng.randint(1, 2)
                p.ellipse((x-r, y-r, x+r, y+r), fill=(250, 239, 255, rng.randint(30, 150)))
            cx, cy = W * 0.55, H * 0.48
            for k in range(9):
                r = 175 + k * 19
                p.arc((cx-r, cy-r, cx+r, cy+r), 15+k*13, 225+k*10, fill=(*b, 180-k*12), width=8)
            p.ellipse((cx-115, cy-115, cx+115, cy+115), fill=(*a, 255), outline=(*b, 230), width=4)
        elif category == "nature":
            p.ellipse((460, 430, 560, 530), fill=(255, 231, 187, 210))
            for k in range(5):
                baseline = 670 + k * 143
                poly = [(0, H)]
                for x in range(-100, W+150, 80):
                    yy = baseline + int(75 * math.sin(x / 120 + k)) + rng.randint(-75, 75)
                    poly.append((x, yy))
                poly.append((W, H))
                p.polygon(poly, fill=(10+k*7, 22+k*10, 40+k*8, 235))
        elif category == "neon":
            for k in range(24):
                x = rng.randint(-120, W)
                p.line((x, 150, x + 180, H), fill=(*b, 35+k*3), width=2+k%7)
            for k in range(10):
                r = 85+k*40
                p.arc((W//2-r, H//2-r, W//2+r, H//2+r), 20, 275, fill=(*b, 230-k*16), width=7)
        elif category == "abstract":
            for k in range(22):
                x, y = rng.randrange(W), rng.randrange(H)
                r = rng.randrange(60, 270)
                p.ellipse((x-r, y-r, x+r, y+r), outline=(*b, 55+k*6), width=rng.randrange(3, 17))
        else:
            p.ellipse((-190, 230, 530, 950), fill=(*b, 36))
            p.ellipse((280, 420, 1050, 1290), fill=(235, 226, 240, 25))
            for k in range(7):
                r = 60+k*85
                p.arc((350-r, 660-r, 350+r, 660+r), 180, 300, fill=(255,255,255,38), width=4)
        im = Image.alpha_composite(im.convert("RGBA"), layer).convert("RGB")
        path = OUT / (category + "_" + str(index).zfill(2) + ".jpg")
        im.save(path, quality=87, optimize=True)
        print(path.name)
assert len(list(OUT.glob("*.jpg"))) == 24
