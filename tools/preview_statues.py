"""Offline geometry overview using the existing cuboid preview tool.

This is not Minecraft lighting, UV acceptance or an in-game screenshot.
"""
from PIL import Image, ImageDraw
from generate_statues import NAMES, LABELS
from preview_pe_models import ROOT, render_model


def main():
    canvas = Image.new("RGB", (1260, 180), "#171a20")
    draw = ImageDraw.Draw(canvas)
    for index, (name, label) in enumerate(zip(NAMES, LABELS)):
        center = 90 + index * 180
        render_model(draw, name + "_statue", center)
        draw.text((center - 40, 152), label, fill="#dfe3e7")
    output = ROOT / "test-artifacts/statues-offline-geometry.png"
    canvas.save(output)
    print(output)


if __name__ == "__main__":
    main()
