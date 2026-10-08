"""Paint original, deterministic 16x16 Darkstone material textures."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/assets/abyssalport/textures/block"
PREVIEW = ROOT / "test-artifacts/darkstone-textures-preview.png"

GROUT = "#1d2625"
DEEP = "#29332f"
SHADE = "#34413b"
STONE = "#425048"
LIT = "#526358"
EDGE = "#68796a"


def paint_darkstone() -> Image.Image:
    image = Image.new("RGB", (16, 16), STONE)
    draw = ImageDraw.Draw(image)
    # Broad, irregular layers keep this readable as natural rock, not brickwork.
    draw.polygon([(0, 0), (8, 0), (10, 2), (7, 3), (3, 2), (0, 3)], fill=LIT)
    draw.polygon([(11, 0), (15, 0), (15, 4), (12, 4), (10, 2)], fill=SHADE)
    draw.polygon([(0, 7), (5, 6), (8, 8), (6, 10), (0, 10)], fill=SHADE)
    draw.polygon([(9, 5), (15, 6), (15, 10), (11, 9), (8, 8)], fill=LIT)
    draw.polygon([(0, 13), (4, 12), (7, 14), (6, 15), (0, 15)], fill=DEEP)
    draw.polygon([(9, 11), (13, 10), (15, 12), (15, 15), (10, 15)], fill=SHADE)
    draw.line([(0, 4), (3, 4), (5, 5), (8, 4), (10, 5)], fill=DEEP)
    draw.line([(11, 5), (13, 4), (15, 5)], fill=GROUT)
    draw.line([(1, 11), (5, 10), (7, 11), (10, 10)], fill=DEEP)
    draw.line([(8, 11), (9, 12), (9, 15)], fill=GROUT)
    draw.line([(2, 3), (5, 3)], fill=EDGE)
    draw.line([(10, 7), (13, 7)], fill=EDGE)
    for x, y, color in [
        (2, 1, EDGE), (7, 2, EDGE), (14, 2, DEEP), (3, 8, GROUT),
        (6, 7, LIT), (12, 10, DEEP), (4, 13, LIT), (13, 14, LIT),
    ]:
        draw.point((x, y), fill=color)
    return image


def paint_cobblestone() -> Image.Image:
    image = Image.new("RGB", (16, 16), GROUT)
    draw = ImageDraw.Draw(image)
    rocks = [
        ([(0, 0), (5, 0), (7, 2), (6, 5), (2, 6), (0, 4)], STONE),
        ([(8, 0), (15, 0), (15, 4), (12, 6), (8, 5), (7, 2)], SHADE),
        ([(0, 6), (3, 7), (5, 10), (4, 13), (0, 12)], SHADE),
        ([(6, 6), (11, 6), (13, 9), (10, 12), (6, 11), (5, 8)], LIT),
        ([(14, 7), (15, 7), (15, 14), (12, 13), (11, 11), (14, 9)], STONE),
        ([(0, 14), (4, 14), (6, 12), (9, 13), (11, 15), (0, 15)], STONE),
    ]
    for points, color in rocks:
        draw.polygon(points, fill=color, outline=DEEP)
    for start, end in [
        ((1, 1), (4, 1)), ((9, 1), (13, 1)), ((1, 8), (2, 8)),
        ((7, 7), (10, 7)), ((13, 8), (14, 8)), ((2, 14), (5, 14)),
    ]:
        draw.line([start, end], fill=EDGE)
    for x, y in [(3, 4), (10, 4), (2, 10), (8, 10), (14, 12), (8, 14)]:
        draw.point((x, y), fill=STONE)
    return image


def paint_bricks(cracked: bool) -> Image.Image:
    image = Image.new("RGB", (16, 16), GROUT)
    draw = ImageDraw.Draw(image)
    for row, top in enumerate((0, 5, 10)):
        offset = 0 if row % 2 == 0 else -4
        for left in range(offset, 16, 8):
            right = min(left + 6, 15)
            draw.rectangle((max(left, 0), top, right, top + 3), fill=STONE if row != 1 else SHADE)
            draw.line((max(left, 0), top, right, top), fill=LIT)
            draw.line((max(left, 0), top + 3, right, top + 3), fill=DEEP)
    draw.line((0, 15, 15, 15), fill=SHADE)
    if cracked:
        for points in [
            [(2, 1), (4, 2), (3, 4), (5, 5), (4, 7)],
            [(11, 5), (10, 7), (12, 8), (11, 10), (13, 11), (12, 13)],
            [(6, 11), (5, 13), (7, 14)],
        ]:
            draw.line(points, fill=GROUT)
        for x, y in ((3, 1), (12, 9), (6, 13)):
            draw.point((x, y), fill=EDGE)
    return image


def paint_chiseled_bricks() -> Image.Image:
    image = Image.new("RGB", (16, 16), GROUT)
    draw = ImageDraw.Draw(image)
    draw.rectangle((1, 1, 14, 14), fill=STONE, outline=DEEP)
    draw.line((2, 2, 13, 2), fill=LIT)
    draw.line((2, 3, 2, 12), fill=LIT)
    draw.line((3, 13, 13, 13), fill=SHADE)
    draw.line((13, 3, 13, 12), fill=SHADE)
    draw.polygon([(8, 3), (12, 8), (8, 12), (3, 8)], fill=DEEP)
    draw.polygon([(8, 4), (11, 8), (8, 11), (4, 8)], fill=SHADE)
    draw.line((8, 4, 11, 8, 8, 11), fill=LIT)
    draw.rectangle((7, 7, 8, 8), fill=GROUT)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        draw.point((x, y), fill=EDGE)
    return image


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    darkstone = paint_darkstone()
    cobblestone = paint_cobblestone()
    bricks = paint_bricks(False)
    cracked_bricks = paint_bricks(True)
    chiseled_bricks = paint_chiseled_bricks()
    darkstone.save(OUTPUT / "darkstone.png")
    cobblestone.save(OUTPUT / "darkstone_cobblestone.png")
    bricks.save(OUTPUT / "darkstone_bricks.png")
    cracked_bricks.save(OUTPUT / "cracked_darkstone_bricks.png")
    chiseled_bricks.save(OUTPUT / "chiseled_darkstone_bricks.png")
    preview = Image.new("RGB", (80, 16), GROUT)
    preview.paste(darkstone, (0, 0))
    preview.paste(cobblestone, (16, 0))
    preview.paste(bricks, (32, 0))
    preview.paste(cracked_bricks, (48, 0))
    preview.paste(chiseled_bricks, (64, 0))
    preview.resize((1200, 240), Image.Resampling.NEAREST).save(PREVIEW)


if __name__ == "__main__":
    main()
