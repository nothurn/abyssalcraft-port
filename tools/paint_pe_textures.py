"""Paint original, deterministic 16x16 PE-device textures for the port study.

The concept sheet under art-concepts/ is visual direction only. These pixels are
drawn here so the game assets remain editable, reproducible and atlas-safe.
"""

from pathlib import Path
from PIL import Image, ImageDraw


OUTPUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/abyssalport/textures/block"
OUTPUT.mkdir(parents=True, exist_ok=True)
PREVIEW = Path(__file__).resolve().parents[1] / "test-artifacts/pe-textures.png"

INK = "#19231e"
DEEP = "#2b3830"
SHADE = "#3e4e43"
STONE = "#617467"
MID = "#7b8d7d"
LIT = "#a2b29f"
EDGE = "#c3d0ba"
ENERGY_DARK = "#245c38"
ENERGY = "#328a51"
ENERGY_MID = "#5cbd78"
ENERGY_LIT = "#a5e6ae"
GLINT = "#e0fbd9"


def canvas(color: str = STONE):
    image = Image.new("RGBA", (16, 16), color)
    return image, ImageDraw.Draw(image)


def stone_marks(draw: ImageDraw.ImageDraw):
    # Hand-placed pixel clusters, not random texture noise.
    for x, y, color in [
        (2, 2, MID), (3, 2, MID), (11, 2, LIT), (12, 2, LIT),
        (5, 5, SHADE), (10, 5, MID), (2, 10, SHADE), (13, 10, MID),
        (6, 12, LIT), (7, 12, LIT),
    ]:
        draw.point((x, y), fill=color)


def paint_stone():
    image, draw = canvas(SHADE)
    draw.rectangle((1, 1, 14, 14), fill=STONE)
    draw.line((0, 0, 15, 0), fill=LIT)
    draw.line((0, 15, 15, 15), fill=INK)
    draw.line((0, 5, 6, 5), fill=DEEP)
    draw.line((8, 9, 15, 9), fill=DEEP)
    draw.line((4, 4, 4, 6), fill=MID)
    draw.line((11, 9, 11, 11), fill=MID)
    stone_marks(draw)
    return image


def paint_carved():
    image, draw = canvas(DEEP)
    draw.rectangle((1, 1, 14, 14), fill=STONE, outline=LIT)
    draw.rectangle((3, 3, 12, 12), fill=SHADE, outline=DEEP)
    draw.line((4, 4, 11, 4), fill=MID)
    draw.line((4, 11, 11, 11), fill=INK)
    draw.rectangle((7, 5, 8, 10), fill=STONE)
    draw.point((7, 5), fill=LIT)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        draw.point((x, y), fill=EDGE)
    return image


def paint_trim():
    # Quiet material for narrow posts: detailed full-face patterns alias badly there.
    image, draw = canvas(STONE)
    draw.line((0, 0, 0, 15), fill=DEEP)
    draw.line((1, 0, 1, 15), fill=SHADE)
    draw.line((2, 0, 2, 15), fill=LIT)
    draw.line((13, 0, 13, 15), fill=MID)
    draw.rectangle((14, 0, 15, 15), fill=DEEP)
    draw.line((3, 1, 12, 1), fill=EDGE)
    draw.line((3, 14, 12, 14), fill=SHADE)
    return image


def paint_crystal():
    image, draw = canvas(ENERGY_DARK)
    draw.rectangle((1, 1, 14, 14), fill=ENERGY)
    draw.polygon([(8, 0), (13, 5), (13, 11), (8, 15), (2, 11), (2, 5)], fill=ENERGY_MID)
    draw.polygon([(7, 1), (9, 1), (12, 6), (10, 12), (7, 14), (5, 10)], fill=ENERGY_LIT)
    draw.polygon([(7, 2), (9, 3), (9, 7), (7, 9), (5, 7)], fill=GLINT)
    draw.line((3, 5, 3, 11), fill=ENERGY_DARK)
    draw.line((12, 6, 12, 11), fill=ENERGY_DARK)
    draw.point((10, 13), fill=GLINT)
    return image


def paint_collector_panel():
    image, draw = canvas(STONE)
    draw.rectangle((0, 0, 15, 15), outline=DEEP)
    draw.line((1, 1, 14, 1), fill=LIT)
    draw.line((1, 14, 14, 14), fill=INK)
    draw.rectangle((4, 3, 11, 12), fill=INK, outline=MID)
    draw.rectangle((5, 4, 10, 11), fill=ENERGY_DARK)
    draw.rectangle((6, 5, 9, 10), fill=ENERGY_MID)
    draw.rectangle((7, 5, 8, 7), fill=ENERGY_LIT)
    draw.point((7, 5), fill=GLINT)
    for x in (2, 13):
        draw.point((x, 4), fill=EDGE)
        draw.point((x, 11), fill=EDGE)
    return image


def paint_relay_front():
    image, draw = canvas(DEEP)
    draw.rectangle((1, 1, 14, 14), fill=STONE, outline=LIT)
    draw.rectangle((2, 2, 13, 13), fill=INK, outline=SHADE)
    draw.polygon([(8, 2), (14, 8), (8, 14), (2, 8)], fill=ENERGY_DARK)
    draw.polygon([(8, 3), (13, 8), (8, 13), (3, 8)], fill=ENERGY_MID)
    draw.polygon([(8, 4), (11, 8), (8, 11), (5, 8)], fill=ENERGY_LIT)
    draw.line((7, 6, 9, 8), fill=GLINT)
    draw.point((8, 8), fill=GLINT)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        draw.point((x, y), fill=EDGE)
    return image


def paint_relay_side():
    image, draw = canvas(STONE)
    draw.rectangle((0, 0, 15, 15), outline=DEEP)
    draw.line((1, 2, 14, 2), fill=LIT)
    draw.line((1, 13, 14, 13), fill=INK)
    draw.rectangle((2, 6, 13, 9), fill=INK, outline=SHADE)
    draw.line((3, 7, 12, 7), fill=ENERGY_DARK)
    draw.line((5, 8, 10, 8), fill=ENERGY_MID)
    draw.point((9, 8), fill=GLINT)
    for x in (2, 13):
        draw.point((x, 3), fill=EDGE)
        draw.point((x, 12), fill=EDGE)
    return image


def paint_relay_top():
    image, draw = canvas(DEEP)
    draw.rectangle((1, 1, 14, 14), fill=STONE, outline=LIT)
    draw.rectangle((3, 3, 12, 12), fill=SHADE, outline=INK)
    draw.polygon([(8, 3), (4, 8), (7, 8), (7, 12), (9, 12), (9, 8), (12, 8)], fill=ENERGY_MID)
    draw.line((8, 4, 8, 10), fill=GLINT)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        draw.point((x, y), fill=EDGE)
    return image


def paint_container_front():
    image, draw = canvas(DEEP)
    draw.rectangle((1, 1, 14, 14), fill=STONE, outline=LIT)
    draw.rectangle((3, 2, 12, 13), fill=INK, outline=MID)
    draw.rectangle((5, 3, 10, 12), fill=ENERGY_DARK)
    draw.rectangle((6, 4, 9, 11), fill=ENERGY)
    draw.rectangle((7, 5, 8, 8), fill=ENERGY_LIT)
    draw.point((7, 5), fill=GLINT)
    draw.line((6, 10, 9, 10), fill=ENERGY_MID)
    for x in (2, 13):
        draw.point((x, 2), fill=EDGE)
        draw.point((x, 13), fill=EDGE)
    return image


def paint_container_side():
    image, draw = canvas(STONE)
    draw.rectangle((0, 0, 15, 15), outline=DEEP)
    draw.line((1, 1, 14, 1), fill=LIT)
    draw.line((1, 14, 14, 14), fill=INK)
    for x in (3, 7, 11):
        draw.line((x, 3, x, 12), fill=SHADE)
        draw.line((x + 1, 3, x + 1, 12), fill=MID)
    draw.rectangle((6, 6, 9, 9), fill=INK)
    draw.rectangle((7, 7, 8, 8), fill=ENERGY_MID)
    draw.point((7, 7), fill=GLINT)
    return image


def paint_container_top():
    image, draw = canvas(DEEP)
    draw.rectangle((1, 1, 14, 14), fill=STONE, outline=LIT)
    draw.rectangle((3, 3, 12, 12), fill=SHADE, outline=INK)
    draw.rectangle((4, 4, 11, 11), fill=ENERGY_DARK)
    draw.polygon([(8, 4), (12, 8), (8, 12), (4, 8)], fill=ENERGY_MID)
    draw.polygon([(8, 5), (11, 8), (8, 11), (5, 8)], fill=ENERGY_LIT)
    draw.point((8, 6), fill=GLINT)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        draw.point((x, y), fill=EDGE)
    return image


TEXTURES = {
    "pe_stone": paint_stone,
    "pe_carved": paint_carved,
    "pe_trim": paint_trim,
    "pe_crystal": paint_crystal,
    "pe_collector_panel": paint_collector_panel,
    "pe_relay_front": paint_relay_front,
    "pe_relay_side": paint_relay_side,
    "pe_relay_top": paint_relay_top,
    "pe_container_front": paint_container_front,
    "pe_container_side": paint_container_side,
    "pe_container_top": paint_container_top,
}


def main():
    preview_rows = (len(TEXTURES) + 4) // 5
    preview = Image.new("RGB", (700, preview_rows * 172 + 8), "#171a20")
    preview_draw = ImageDraw.Draw(preview)
    for index, (name, paint) in enumerate(TEXTURES.items()):
        image = paint()
        image.save(OUTPUT / f"{name}.png")
        column, row = index % 5, index // 5
        x, y = 12 + column * 138, 16 + row * 172
        preview.paste(image.resize((128, 128), Image.Resampling.NEAREST), (x, y))
        preview_draw.text((x, y + 132), name[3:].replace("_", " "), fill="#d5d9df")
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    preview.save(PREVIEW)


if __name__ == "__main__":
    main()
