"""Render a quick offline silhouette check for the PE block/item models.

This is not Minecraft's renderer or an in-game acceptance test. It only makes
the authored cuboids and texture colors inspectable without launching a client.
"""

import json
from pathlib import Path
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/abyssalport"
OUT = ROOT / "test-artifacts/pe-items-offline-preview.png"
HOTBAR_OUT = ROOT / "test-artifacts/pe-items-hotbar-preview.png"
NAMES = ("energy_collector", "energy_relay", "energy_container", "necronomicon")
LABELS = ("Collector", "Relay", "Container", "Necronomicon")


def project(x, y, z, center_x, baseline):
    return (round(center_x + (x + z - 16) * 5), round(baseline + (x - z) * 2.5 - y * 5))


def face_quad(element, face, u0, v0, u1, v1):
    x0, y0, z0 = element["from"]
    x1, y1, z1 = element["to"]
    if face == "north":
        def point(u, v):
            return (x0 + (x1 - x0) * u, y1 - (y1 - y0) * v, z0)
    elif face == "east":
        def point(u, v):
            return (x1, y1 - (y1 - y0) * v, z0 + (z1 - z0) * u)
    else:
        def point(u, v):
            return (x0 + (x1 - x0) * u, y1, z0 + (z1 - z0) * v)
    return (point(u0, v0), point(u1, v0), point(u1, v1), point(u0, v1))


def shade(rgb, factor):
    return tuple(max(0, min(255, round(channel * factor))) for channel in rgb[:3])


def render_model(draw, name, center_x):
    model_kind = "item" if name == "necronomicon" else "block"
    model = json.loads((ASSETS / f"models/{model_kind}/{name}.json").read_text(encoding="utf-8"))
    textures = {}
    for key, value in model["textures"].items():
        if value.startswith("abyssalport:"):
            path = ASSETS / "textures" / (value.split(":", 1)[1] + ".png")
            textures[key] = Image.open(path).convert("RGB")
    faces = []
    for element in model["elements"]:
        x0, y0, z0 = element["from"]
        x1, y1, z1 = element["to"]
        for face, depth, light in (
            ("north", (x0 + x1 + y0 + y1) / 2 - z0, 0.86),
            ("east", x1 + (y0 + y1 - z0 - z1) / 2, 0.67),
            ("up", (x0 + x1) / 2 + y1 - (z0 + z1) / 2, 1.08),
        ):
            if face in element["faces"]:
                texture = textures[element["faces"][face]["texture"][1:]]
                faces.append((depth, light, element, face, texture))
    for _, light, element, face, texture in sorted(faces, key=lambda item: item[0]):
        face_polygon = [project(*corner, center_x, 120) for corner in face_quad(element, face, 0, 0, 1, 1)]
        draw.polygon(face_polygon, fill=shade(texture.getpixel((8, 8)), light))
        for v in range(16):
            for u in range(16):
                corners = face_quad(element, face, u / 16, v / 16, (u + 1) / 16, (v + 1) / 16)
                polygon = [project(*corner, center_x, 120) for corner in corners]
                draw.polygon(polygon, fill=shade(texture.getpixel((u, v)), light))


def main():
    image = Image.new("RGB", (960, 256), "#171a20")
    draw = ImageDraw.Draw(image)
    for index, (name, label) in enumerate(zip(NAMES, LABELS)):
        center_x = 120 + index * 240
        render_model(draw, name, center_x)
        draw.text((center_x - 26, 228), label, fill="#e0e2e8")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUT)
    hotbar = Image.new("RGB", (256, 64), "#292a2e")
    hotbar_draw = ImageDraw.Draw(hotbar)
    for index in range(len(NAMES)):
        center_x = 120 + index * 240
        crop = image.crop((center_x - 86, 8, center_x + 86, 160))
        crop.thumbnail((42, 42), Image.Resampling.NEAREST)
        slot_x = 4 + index * 64
        hotbar_draw.rectangle((slot_x, 4, slot_x + 55, 59), outline="#777c80", width=2)
        hotbar.paste(crop, (slot_x + (56 - crop.width) // 2, 9))
    hotbar.save(HOTBAR_OUT)


if __name__ == "__main__":
    main()
