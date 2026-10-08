"""Paint original 16x16 atlas-safe textures for the first 3D Necronomicon."""

from pathlib import Path
from PIL import Image, ImageDraw


OUTPUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/abyssalport/textures/item"
OUTPUT.mkdir(parents=True, exist_ok=True)

BLACK = "#211c1b"
LEATHER = "#3a2d2a"
LEATHER_LIGHT = "#604a42"
GOLD_DARK = "#6d593b"
GOLD = "#ad9160"
GOLD_LIGHT = "#d6c18e"
PARCHMENT = "#d1c5a5"
PARCHMENT_DARK = "#a99c7f"
JADE = "#487b5c"
JADE_LIGHT = "#9dcca4"


def cover():
    image = Image.new("RGBA", (16, 16), LEATHER)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 15, 15), outline=BLACK)
    draw.rectangle((1, 1, 14, 14), outline=LEATHER_LIGHT)
    draw.line((3, 2, 12, 2), fill=GOLD_DARK)
    draw.line((3, 13, 12, 13), fill=GOLD_DARK)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        draw.rectangle((x, y, x + 1, y + 1), fill=GOLD)
        draw.point((x, y), fill=GOLD_LIGHT)
    draw.polygon([(8, 4), (12, 8), (8, 12), (4, 8)], outline=GOLD_DARK)
    draw.ellipse((5, 6, 10, 10), outline=GOLD_LIGHT)
    draw.polygon([(8, 5), (10, 8), (8, 11), (6, 8)], fill=JADE)
    draw.rectangle((7, 7, 8, 9), fill=BLACK)
    draw.point((7, 6), fill=JADE_LIGHT)
    return image


def pages():
    image = Image.new("RGBA", (16, 16), PARCHMENT)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 15, 15), outline=PARCHMENT_DARK)
    for y in (2, 5, 8, 11, 14):
        draw.line((1, y, 14, y), fill=PARCHMENT_DARK)
    for x, y in ((3, 3), (11, 6), (5, 9), (13, 12)):
        draw.point((x, y), fill=GOLD_DARK)
    return image


def spine():
    image = Image.new("RGBA", (16, 16), LEATHER)
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 15, 15), outline=BLACK)
    draw.line((2, 1, 2, 14), fill=LEATHER_LIGHT)
    draw.line((13, 1, 13, 14), fill=BLACK)
    for y in (2, 6, 10, 13):
        draw.rectangle((3, y, 12, y + 1), fill=GOLD_DARK)
        draw.line((4, y, 11, y), fill=GOLD)
    draw.rectangle((7, 7, 8, 9), fill=JADE)
    return image


def clasp():
    image = Image.new("RGBA", (16, 16), GOLD_DARK)
    draw = ImageDraw.Draw(image)
    draw.rectangle((1, 1, 14, 14), fill=GOLD, outline=GOLD_LIGHT)
    draw.rectangle((5, 5, 10, 10), fill=GOLD_DARK)
    draw.rectangle((7, 6, 8, 9), fill=JADE)
    draw.point((7, 6), fill=JADE_LIGHT)
    return image


def main():
    for name, painter in {
        "necronomicon_cover": cover,
        "necronomicon_pages": pages,
        "necronomicon_spine": spine,
        "necronomicon_clasp": clasp,
    }.items():
        painter().save(OUTPUT / f"{name}.png")


if __name__ == "__main__":
    main()
