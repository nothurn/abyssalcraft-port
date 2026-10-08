"""Seven independently authored, block-scale stone sculptures.

Only source deity names, one-block scale and facing semantics are referenced.
No upstream OBJ, bitmap or audio is read. The PE/charms layer is implemented
separately; these resources do not assert complete source-mechanic parity.
"""
import json
from PIL import Image
from generate_wasteland_stronghold import ROOT, ASSETS, DATA, ID, write, texture
from generate_wasteland_minerals import append_tag

NAMES = ("cthulhu", "hastur", "jzahar", "azathoth", "nyarlathotep", "yog_sothoth", "shub_niggurath")
LABELS = ("Cthulhu", "Hastur", "J'zahar", "Azathoth", "Nyarlathotep", "Yog-Sothoth", "Shub-Niggurath")


def cube(lower, upper):
    x, y, z = (upper[i] - lower[i] for i in range(3))
    faces = {}
    for face, width, height in (("north", x, y), ("south", x, y), ("west", z, y),
                               ("east", z, y), ("up", x, z), ("down", x, z)):
        faces[face] = {"texture": "#stone", "uv": [0, 0, width, height]}
    return {"from": list(lower), "to": list(upper), "faces": faces}


def geometry(name):
    pieces = [cube((4, 0, 4), (12, 2, 12)), cube((5, 2, 5), (11, 3, 11))]
    if name == "cthulhu":
        pieces += [cube((6, 3, 6), (10, 9, 11)), cube((5, 9, 5), (11, 14, 11))]
        for x in (5, 7, 9):
            pieces += [cube((x, 8, 4), (x + 1, 12, 5)), cube((x, 7, 4), (x + 1, 8, 6))]
        for x in (3, 11):
            pieces += [cube((x, 5, 9), (x + 2, 12, 10)), cube((x, 7, 10), (x + 2, 14, 11))]
        pieces += [cube((6, 14, 7), (10, 15, 10))]
    elif name == "hastur":
        pieces += [cube((5, 3, 5), (11, 5, 11)), cube((6, 5, 6), (10, 10, 10)),
                   cube((5, 10, 6), (11, 14, 11)), cube((6, 14, 7), (10, 16, 10)),
                   cube((5, 11, 5), (6, 14, 6)), cube((10, 11, 5), (11, 14, 6)),
                   cube((5, 7, 5), (6, 11, 7)), cube((10, 7, 5), (11, 11, 7)),
                   cube((7, 11, 6), (9, 13, 7))]
    elif name == "jzahar":
        pieces += [cube((6, 3, 7), (10, 5, 10)), cube((6, 5, 7), (10, 10, 10)),
                   cube((5, 10, 6), (11, 13, 11)), cube((6, 13, 7), (10, 15, 10)),
                   cube((4, 7, 6), (6, 11, 9)), cube((10, 7, 6), (12, 11, 9)),
                   cube((4, 4, 5), (5, 8, 6)), cube((11, 4, 5), (12, 8, 6)),
                   cube((6, 14, 6), (7, 16, 7)), cube((9, 14, 6), (10, 16, 7))]
    elif name == "azathoth":
        pieces += [cube((5, 3, 5), (11, 7, 11)), cube((4, 7, 6), (12, 11, 10)),
                   cube((6, 11, 5), (10, 14, 11)), cube((7, 14, 6), (9, 16, 9)),
                   cube((3, 6, 7), (5, 10, 9)), cube((11, 5, 7), (13, 9, 9)),
                   cube((6, 7, 4), (9, 10, 6)), cube((8, 8, 10), (11, 12, 12))]
    elif name == "nyarlathotep":
        pieces += [cube((6, 3, 6), (7, 7, 9)), cube((9, 3, 6), (10, 7, 9)),
                   cube((6, 7, 6), (10, 11, 10)), cube((7, 11, 6), (9, 14, 9)),
                   cube((6, 14, 6), (10, 16, 10)), cube((5, 9, 6), (6, 13, 9)),
                   cube((10, 8, 6), (11, 12, 9)), cube((4, 5, 5), (5, 10, 7)),
                   cube((11, 4, 5), (12, 9, 7))]
    elif name == "yog_sothoth":
        pieces += [cube((6, 3, 6), (10, 6, 10)), cube((5, 6, 5), (9, 10, 9)),
                   cube((8, 9, 7), (12, 13, 11)), cube((5, 11, 7), (9, 15, 11)),
                   cube((9, 13, 5), (12, 16, 8)), cube((3, 8, 7), (6, 11, 10)),
                   cube((7, 7, 10), (10, 10, 13)), cube((7, 10, 4), (9, 12, 6))]
    else:
        pieces += [cube((6, 3, 6), (10, 9, 10)), cube((5, 9, 5), (11, 13, 11)),
                   cube((6, 13, 6), (10, 15, 10)), cube((4, 13, 7), (6, 16, 9)),
                   cube((10, 13, 7), (12, 16, 9)), cube((3, 5, 6), (6, 9, 8)),
                   cube((10, 4, 8), (13, 8, 10)), cube((5, 3, 5), (6, 6, 6)),
                   cube((10, 3, 5), (11, 6, 6))]
    return pieces


def generate():
    for index, name in enumerate(NAMES):
        bitmap = Image.new("RGBA", (16, 16))
        tint = ((72, 82, 76), (82, 78, 69), (66, 70, 78), (74, 68, 75),
                (61, 67, 70), (71, 74, 80), (70, 77, 64))[index]
        for y in range(16):
            for x in range(16):
                grain = ((x * 31 + y * 17 + index * 7) % 15) - 7
                seam = -9 if (x + y * 3 + index) % 19 == 0 else 0
                edge = 7 if y % 8 == 0 else 0
                bitmap.putpixel((x, y), tuple(v + grain + seam + edge for v in tint) + (255,))
        texture("block/" + name + "_statue.png", bitmap)
        model = {"textures": {"stone": ID + "block/" + name + "_statue", "particle": "#stone"},
                 "elements": geometry(name), "display": {
                     "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [.85, .85, .85]},
                     "ground": {"translation": [0, 3, 0], "scale": [.35, .35, .35]},
                     "fixed": {"rotation": [0, 180, 0], "scale": [.6, .6, .6]},
                     "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
                     "firstperson_righthand": {"rotation": [0, 135, 0], "scale": [.65, .65, .65]}}}
        write(ASSETS, "models/block/" + name + "_statue.json", model)
        write(ASSETS, "models/item/" + name + "_statue.json", {"parent": ID + "block/" + name + "_statue"})
        write(ASSETS, "blockstates/" + name + "_statue.json", {"variants": {
            "facing=" + facing: {"model": ID + "block/" + name + "_statue", "y": rotation}
            for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
        write(DATA, "loot_table/blocks/" + name + "_statue.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
              "entries": [{"type": "minecraft:item", "name": ID + name + "_statue"}],
              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    append_tag(ROOT / "data/minecraft", "tags/block/mineable/pickaxe.json", [ID + n + "_statue" for n in NAMES])
    for locale in ("en_us", "de_de"):
        lang = json.loads((ASSETS / "lang" / (locale + ".json")).read_text("utf-8"))
        for name, label in zip(NAMES, LABELS):
            lang["block.abyssalport." + name + "_statue"] = label + (" Statue" if locale == "en_us" else "-Statue")
        if locale == "en_us":
            lang["screen.abyssalport.energy"] = "New monoliths carry one of seven deity statues on a 3x3 Monolith Stone pillar with a living biomass core. Mine the statue to relocate it. Under open sky, each statue currently charges both held Necronomicons by 5 PE every 120 active ticks. Existing prototype shrines remain usable. Charms, tolerance, disruptions and structure bonuses are the next unfinished layer."
            lang["screen.abyssalport.energy_note"] = "An undisturbed group of four ready Shoggoths can build a monolith on clear supported ground when mob griefing is enabled. Harvesting its statue does not remove the biomass: keep watch for more Shoggoths. Destroyed biomass cannot be collected, even with Silk Touch."
            lang["screen.abyssalport.collector"] = "Place an isolated Energy Collector three blocks horizontally from an open-sky statue or older effigy. It stores up to 1,000 PE. Craft one with eight Darkstone and a Shadow Gem. Do not place another collector directly against its sides, top or bottom."
            for key in ("screen.abyssalport.shoggoth_lairs", "screen.abyssalport.shoggoth_growth"):
                lang[key] = lang[key].replace("the full deity statue catalogue is still development", "the seven statues are reachable through newly built monoliths; full charm/disruption rules remain development").replace("deity catalogue remain development", "full statue-amplifier/disruption rules remain development")
        else:
            lang["screen.abyssalport.energy"] = "Neue Monolithen tragen eine von sieben Gottheiten-Statuen auf einer 3x3-Säule aus Monolithstein mit lebendem Biomassekern. Baue die Statue ab, um sie umzuplatzieren. Unter freiem Himmel lädt jede Statue derzeit beide gehaltenen Necronomicons mit 5 PE je 120 aktiven Ticks. Bisherige Prototyp-Schreine bleiben nutzbar. Charms, Toleranz, Disruptions und Strukturboni sind die nächste noch unfertige Stufe."
            lang["screen.abyssalport.energy_note"] = "Vier ungestörte, bereite Shoggoths können auf freiem, tragfähigem Boden einen Monolithen bauen, wenn Mob-Griefing aktiviert ist. Das Ernten der Statue entfernt nicht die Biomasse: Achte auf weitere Shoggoths. Zerstörte Biomasse lässt sich nicht sammeln, auch nicht mit Behutsamkeit."
            lang["screen.abyssalport.collector"] = "Platziere einen isolierten Energiesammler drei Blöcke horizontal von einer Statue oder einem älteren Bildnis unter freiem Himmel. Er speichert bis zu 1.000 PE. Das Rezept benötigt acht Dunkelstein und einen Schattengem. Kein anderer Sammler darf direkt seitlich, oberhalb oder unterhalb angrenzen."
            for key in ("screen.abyssalport.shoggoth_lairs", "screen.abyssalport.shoggoth_growth"):
                lang[key] = lang[key].replace("der vollständige Gottheitenkatalog ist noch Entwicklung", "die sieben Statuen sind über neue Monolithen erreichbar; vollständige Charm-/Disruptionsregeln bleiben Entwicklung").replace("der Gottheitenkatalog sind noch Entwicklung", "vollständige Statue-Verstärker-/Disruptionsregeln sind noch Entwicklung")
        write(ASSETS, "lang/" + locale + ".json", lang)


if __name__ == "__main__":
    generate()
