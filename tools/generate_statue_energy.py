"""Own range-pillar geometry and pixels; no upstream image or model is read.

Mechanics reference: BlockMonolithPillar, monolithpillar recipe and PEUtils.
Run after generate_statues/generate_charms when regenerating the shared guide.
"""
import json
from PIL import Image
from generate_statues import cube
from generate_wasteland_stronghold import ROOT, ASSETS, DATA, ID, write, texture
from generate_wasteland_minerals import append_tag


def generate_pillar():
    image = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            grain = (x * 17 + y * 31) % 13 - 6
            groove = -15 if x in (3, 7, 11, 15) else 0
            band = 10 if y in (0, 1, 14, 15) else 0
            image.putpixel((x, y), tuple(v + grain + groove + band for v in (65, 71, 67)) + (255,))
    texture("block/monolith_pillar.png", image)
    write(ASSETS, "models/block/monolith_pillar.json", {
        "parent": "minecraft:block/block", "textures": {"stone": ID + "block/monolith_pillar", "particle": "#stone"},
        "elements": [cube((4, 0, 4), (12, 16, 12))]})
    write(ASSETS, "models/item/monolith_pillar.json", {"parent": ID + "block/monolith_pillar"})
    write(ASSETS, "blockstates/monolith_pillar.json", {"variants": {"": {"model": ID + "block/monolith_pillar"}}})
    write(DATA, "recipe/monolith_pillar.json", {"type": "minecraft:crafting_shaped", "category": "building",
          "pattern": ["SS", "SS"], "key": {"S": {"item": ID + "monolith_stone"}},
          "result": {"id": ID + "monolith_pillar", "count": 1}})
    write(DATA, "loot_table/blocks/monolith_pillar.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
          "entries": [{"type": "minecraft:item", "name": ID + "monolith_pillar"}],
          "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    append_tag(ROOT / "data/minecraft", "tags/block/mineable/pickaxe.json", [ID + "monolith_pillar"])


def generate():
    generate_pillar()
    for locale in ("en_us", "de_de"):
        lang = json.loads((ASSETS / "lang" / (locale + ".json")).read_text("utf-8"))
        en = locale == "en_us"
        lang["block.abyssalport.monolith_pillar"] = "Monolith Pillar" if en else "Monolithsäule"
        lang["screen.abyssalport.energy_note"] = (
            "Craft four Monolith Stone in a square into one Monolith Pillar. Up to two consecutive pillars directly below a statue each add four blocks of book range and one collector search layer. Full stone does not count. Held books use a 120-active-tick timer with 0-9 ticks of restart jitter; dropped books and collectors each have an independent 1-in-120 chance per tick. Generic Duration changes these to 60 active ticks and 1-in-80. No player is needed for dropped books or collectors. Tolerance is saved per accepted transfer; its complete disruption catalogue and validated structures are still development." if en else
            "Vier Monolithsteine im Quadrat ergeben eine Monolithsäule. Bis zu zwei aufeinanderfolgende Säulen direkt unter einer Statue geben jeweils vier Blöcke Buchreichweite und eine zusätzliche Sammler-Suchebene. Voller Stein zählt nicht. Gehaltene Bücher nutzen 120 aktive Ticks mit 0-9 Neustartticks; abgelegte Bücher und Sammler haben unabhängig je Tick eine 1:120-Ladechance. Generischer Dauer-Charm ändert dies auf 60 aktive Ticks und 1:80. Abgelegte Bücher/Sammler brauchen keinen Spieler. Toleranz wird je angenommener Übertragung gespeichert; kompletter Disruptionskatalog und validierte Strukturen sind noch Entwicklung.")
        lang["screen.abyssalport.energy"] = lang["screen.abyssalport.energy"].replace(
            "Dropped-item cadence, tolerance/disruptions and structure bonuses remain unfinished.",
            "Dropped-book/collector timing and range pillars are implemented; full disruptions and structure bonuses remain unfinished.").replace(
            "Dropped-Item-Takt, Toleranz/Disruptions und Strukturboni bleiben unfertig.",
            "Abgelegte Bücher, Sammlertakte und Reichweitensäulen sind implementiert; volle Disruptions und Strukturboni bleiben unfertig.")
        write(ASSETS, "lang/" + locale + ".json", lang)


if __name__ == "__main__":
    generate()
