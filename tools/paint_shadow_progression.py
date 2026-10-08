"""Generate UV-exact, original pixel textures from the Shadow Creature concept."""

from pathlib import Path
import random
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/abyssalport/textures"


def save_item(name: str, outline: list[tuple[int, int]], facets: list[tuple[str, list[tuple[int, int]]]]) -> None:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.polygon(outline, fill="#17131f")
    for color, points in facets:
        draw.polygon(points, fill=color)
    image.save(TEXTURES / "item" / f"{name}.png")


def paint_creature() -> None:
    image = Image.new("RGBA", (64, 64), "#201f27")
    draw = ImageDraw.Draw(image)
    # Low-contrast planar variation follows the UV islands of the original model.
    for x in range(0, 64, 4):
        for y in range(0, 64, 4):
            tone = (x * 7 + y * 11) % 5
            shade = ["#25232c", "#292731", "#211f28", "#302d38", "#24232d"][tone]
            draw.rectangle((x, y, x + 3, y + 3), fill=shade)
    # Head front face: muted eye sockets with two readable warm eyes.
    draw.rectangle((7, 7, 12, 13), fill="#15151b")
    draw.rectangle((8, 9, 8, 10), fill="#d6b789")
    draw.rectangle((11, 9, 11, 10), fill="#d6b789")
    draw.point((8, 9), fill="#f5dfaa")
    draw.point((11, 9), fill="#f5dfaa")
    # Subtle shoulder and chest planes; no luminous full-body overlay.
    draw.rectangle((7, 26, 12, 29), fill="#292632")
    draw.line((8, 26, 11, 29), fill="#393344")
    image.save(TEXTURES / "entity" / "shadow_creature.png")


def paint_soil() -> None:
    rng = random.Random(417)
    top = Image.new("RGB", (16, 16))
    side = Image.new("RGB", (16, 16))
    top_colors = ["#25252c", "#2c2b32", "#34313a", "#39343e", "#211f28"]
    earth_colors = ["#343035", "#3d353a", "#40383c", "#29262e", "#4a3d40"]
    for y in range(16):
        for x in range(16):
            top.putpixel((x, y), tuple(bytes.fromhex(rng.choice(top_colors)[1:])))
            palette = top_colors if y < 3 or (y < 5 and rng.randrange(4) == 0) else earth_colors
            side.putpixel((x, y), tuple(bytes.fromhex(rng.choice(palette)[1:])))
    top.save(TEXTURES / "block" / "darklands_soil_top.png")
    side.save(TEXTURES / "block" / "darklands_soil_side.png")


def main() -> None:
    (TEXTURES / "item").mkdir(parents=True, exist_ok=True)
    (TEXTURES / "block").mkdir(parents=True, exist_ok=True)
    save_item("shadow_fragment", [(4, 10), (5, 5), (7, 4), (8, 7), (10, 6), (12, 8), (10, 12), (7, 11)], [
        ("#493751", [(5, 6), (7, 5), (8, 8), (6, 10)]),
        ("#766080", [(6, 6), (7, 5), (7, 7)]),
        ("#302b3c", [(8, 8), (10, 7), (11, 9), (9, 11)]),
    ])
    save_item("shadow_shard", [(7, 1), (10, 3), (11, 9), (8, 14), (5, 11), (4, 6)], [
        ("#4b3a5c", [(7, 2), (9, 4), (9, 10), (8, 13), (6, 10)]),
        ("#80669a", [(7, 2), (8, 3), (7, 9), (5, 7)]),
        ("#2d253b", [(9, 4), (11, 9), (8, 13), (9, 10)]),
    ])
    save_item("shadow_gem", [(8, 1), (13, 5), (12, 10), (8, 14), (3, 10), (2, 5)], [
        ("#443451", [(8, 2), (12, 5), (10, 10), (8, 13), (6, 10)]),
        ("#78608b", [(8, 2), (9, 5), (6, 10), (3, 5)]),
        ("#a68bbb", [(8, 3), (9, 5), (8, 8), (6, 9)]),
        ("#30283d", [(9, 5), (12, 5), (11, 10), (8, 13)]),
    ])
    paint_creature()
    paint_soil()


if __name__ == "__main__":
    main()
