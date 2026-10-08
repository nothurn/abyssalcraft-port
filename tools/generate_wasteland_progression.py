"""Independent book/staff media and the source-informed first dimension upgrade chain.

Uses only our own existing book model/painters, never upstream textures or models.
Recipes follow drainstaff_0, skin_0 and necronomicon_cor from the read-only source.
"""
import json
from PIL import Image, ImageDraw
from generate_wasteland_minerals import ROOT, ASSETS, DATA, write, append_tag
from generate_abyssalnite_assets import texture
from build_pe_models import cube
import paint_necronomicon as book


def media():
    for name in ["abyssal_wasteland_essence", "skin_of_the_abyssal_wasteland"]:
        image = Image.new("RGBA", (16, 16)); draw = ImageDraw.Draw(image)
        if name.endswith("essence"):
            draw.polygon([(7, 1), (11, 3), (13, 7), (11, 12), (7, 14), (3, 11), (2, 7), (4, 3)], fill="#183d39", outline="#376752")
            draw.polygon([(7, 3), (10, 5), (11, 8), (8, 12), (5, 10), (4, 7)], fill="#52947b")
            draw.line((7, 4, 7, 10), fill="#acdcab", width=2)
            draw.point((9, 7), fill="#d1edc3"); draw.point((5, 8), fill="#8cb7a0")
        else:
            draw.polygon([(3, 1), (7, 2), (11, 1), (14, 4), (12, 8), (14, 12), (9, 14), (6, 12), (2, 14), (1, 9), (3, 6), (1, 3)], fill="#437063", outline="#1f443f")
            draw.line((4, 4, 10, 4), fill="#819b74")
            draw.line((5, 7, 11, 8), fill="#6b9b7d")
            draw.line((4, 11, 9, 11), fill="#2c584e")
            draw.line((3, 3, 3, 5), fill="#a6b78a"); draw.point((10, 10), fill="#9ab487")
        texture("item/"+name+".png", image)
        write(ASSETS, "models/item/"+name+".json", {"parent": "minecraft:item/generated", "textures": {"layer0": "abyssalport:item/"+name}})

    # Keep the existing item geometry and hand-facing transforms, with distinct own tier-one covers.
    palette = {book.BLACK: "#142d2b", book.LEATHER: "#244e47", book.LEATHER_LIGHT: "#4d7b65",
               book.GOLD_DARK: "#4f6854", book.GOLD: "#8bb080", book.GOLD_LIGHT: "#cee0b2",
               book.JADE: "#366960", book.JADE_LIGHT: "#a5e2be"}
    def recolor(image):
        pixels = image.load()
        colors = {Image.new("RGBA", (1, 1), source).getpixel((0, 0)): Image.new("RGBA", (1, 1), target).getpixel((0, 0)) for source, target in palette.items()}
        for y in range(image.height):
            for x in range(image.width): pixels[x, y] = colors.get(pixels[x, y], pixels[x, y])
        return image
    for name, painter in [("cover", book.cover), ("spine", book.spine), ("clasp", book.clasp)]:
        texture("item/wasteland_book_"+name+".png", recolor(painter()))
    model = json.loads((ASSETS / "models/item/necronomicon.json").read_text(encoding="utf-8"))
    model["textures"].update({name: "abyssalport:item/wasteland_book_"+name for name in ["cover", "spine", "clasp"]})
    write(ASSETS, "models/item/abyssal_wasteland_necronomicon.json", model)

    # Small block-grid staff: dark shaft, carved head and jade inset, legible in the hotbar.
    for name, base, bright in [("shaft", "#28232e", "#635864"), ("metal", "#34473b", "#a6b984"), ("core", "#204b43", "#7ed0a6")]:
        image = Image.new("RGBA", (16, 16), base); draw = ImageDraw.Draw(image)
        for y in range(2, 16, 4): draw.line((1, y, 14, y), fill=bright)
        if name == "core": draw.rectangle((5, 4, 10, 11), fill=bright); draw.point((7, 5), fill="#d2efbf")
        texture("item/rending_staff_"+name+".png", image)
    staff_model = {
        "textures": {name: "abyssalport:item/rending_staff_"+name for name in ["shaft", "metal", "core"]} | {"particle": "#shaft"},
        "elements": [cube([7, 0, 7], [9, 12, 9], "shaft"), cube([5, 10, 6], [7, 15, 10], "metal"),
                     cube([9, 10, 6], [11, 15, 10], "metal"), cube([7, 11, 7], [9, 15, 9], "core"),
                     cube([6, 15, 6], [10, 16, 10], "metal"), cube([6, 2, 6], [10, 3, 10], "metal")],
        "display": {"gui": {"rotation": [12, 25, -28], "scale": [.9, .9, .9]},
            "ground": {"scale": [.4, .4, .4]}, "fixed": {"scale": [.8, .8, .8]},
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.85, .85, .85]},
            "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [.85, .85, .85]},
            "firstperson_righthand": {"rotation": [0, -35, 0], "translation": [1, 1, 0], "scale": [.85, .85, .85]},
            "firstperson_lefthand": {"rotation": [0, 35, 0], "translation": [1, 1, 0], "scale": [.85, .85, .85]}}}
    write(ASSETS, "models/item/staff_of_rending.json", staff_model)
    for name, base, bright in [("metal", "#31564e", "#98c7a1"), ("core", "#37705c", "#b3e4b2")]:
        image = Image.new("RGBA", (16, 16), base); draw = ImageDraw.Draw(image)
        for y in range(2, 16, 4): draw.line((1, y, 14, y), fill=bright)
        if name == "core": draw.rectangle((5, 4, 10, 11), fill=bright); draw.point((7, 5), fill="#e1f5cb")
        texture("item/wasteland_staff_"+name+".png", image)
        staff_model["textures"][name] = "abyssalport:item/wasteland_staff_"+name
    write(ASSETS, "models/item/abyssal_wasteland_staff_of_rending.json", staff_model)

    # Reuse our block-grid pedestal geometry, not any upstream ritual model.
    rune = Image.new("RGBA", (16, 16), "#344841"); draw = ImageDraw.Draw(rune)
    draw.rectangle((1, 1, 14, 14), outline="#779684")
    draw.polygon([(8, 3), (12, 8), (8, 12), (3, 8)], outline="#b7cca0")
    draw.line((8, 5, 8, 10), fill="#b7cca0")
    texture("block/abyssal_ritual_top.png", rune)
    for name, geometry in [("abyssal_ritual_altar", [cube([0, 0, 0], [16, 14, 16], "stone"), cube([0, 14, 0], [16, 16, 16], "rune")]),
                           ("abyssal_ritual_pedestal", [cube([4, 0, 4], [12, 12, 12], "stone"), cube([2, 12, 2], [14, 15, 14], "rune")])]:
        write(ASSETS, "models/block/"+name+".json", {"textures": {"stone": "abyssalport:block/abyssal_stone", "rune": "abyssalport:block/abyssal_ritual_top", "particle": "#stone"}, "elements": geometry})
        write(ASSETS, "blockstates/"+name+".json", {"variants": {"": {"model": "abyssalport:block/"+name}}})
        write(ASSETS, "models/item/"+name+".json", {"parent": "abyssalport:block/"+name})
        # As in the source, broken higher-tier ritual blocks return their ground material.
        write(DATA, "loot_table/blocks/"+name+".json", {"type": "minecraft:block", "pools": [{"rolls": 1,
              "conditions": [{"condition": "minecraft:survives_explosion"}],
              "entries": [{"type": "minecraft:item", "name": "abyssalport:abyssal_cobblestone"}]}]})
        append_tag(ROOT / "data/minecraft", "tags/block/mineable/pickaxe.json", ["abyssalport:"+name])


def data():
    for name, kind, pattern, key in [
        ("staff_of_rending", "minecraft:crafting_shaped", [" SO", " SS", "S  "], {"S": "abyssalport:shadow_shard", "O": "abyssalport:oblivion_shard"}),
        ("skin_of_the_abyssal_wasteland", "minecraft:crafting_shaped", ["FFF", "FEF", "FFF"], {"F": "abyssalport:coralium_plagued_flesh", "E": "abyssalport:abyssal_wasteland_essence"}),
        ("abyssal_wasteland_necronomicon", "abyssalport:book_upgrade", ["SS", "SB"], {"S": "abyssalport:skin_of_the_abyssal_wasteland", "B": "abyssalport:necronomicon"})]:
        write(DATA, "recipe/"+name+".json", {"type": kind, "category": "misc", "pattern": pattern,
              "key": {symbol: {"item": item} for symbol, item in key.items()}, "result": {"id": "abyssalport:"+name}})
        trigger_item = "abyssalport:oblivion_shard" if name == "staff_of_rending" else next(iter(key.values()))
        write(DATA, "advancement/recipes/"+name+".json", {"criteria": {"has_material": {"trigger": "minecraft:inventory_changed",
              "conditions": {"items": [{"items": [trigger_item]}]}}}, "rewards": {"recipes": ["abyssalport:"+name]}})
    append_tag(DATA, "tags/entity_type/abyssal_rending_targets.json", ["abyssalport:abyssal_zombie"])
    append_tag(DATA, "tags/entity_type/shadow_rending_targets.json", ["abyssalport:shadow_creature"])
    # APIUtils.areItemStackArraysEqual compares an unordered multiset in the original.
    write(DATA, "rituals/abyssal_wasteland_staff.json", {"energy_cost": 1000, "duration_ticks": 20,
        "book_tier": 1, "altar_tier": 1, "dimension": "abyssalport:abyssal_wasteland",
        "altar_input": "abyssalport:staff_of_rending", "preserve_components": True,
        "offerings": ["abyssalport:shadow_gem"] + ["abyssalport:abyssal_stone"]*4 + ["abyssalport:coralium_plagued_flesh"]*3,
        "result": {"item": "abyssalport:abyssal_wasteland_staff_of_rending", "count": 1}})
    texts = {
        "item.abyssalport.staff_of_rending": ["Staff of Rending", "Stab der Zerrüttung"],
        "item.abyssalport.abyssal_wasteland_staff_of_rending": ["Abyssal Wasteland Staff of Rending", "Ödland-Stab der Zerrüttung"],
        "block.abyssalport.abyssal_ritual_altar": ["Abyssal Ritual Altar", "Abyssaler Ritualaltar"],
        "block.abyssalport.abyssal_ritual_pedestal": ["Abyssal Ritual Pedestal", "Abyssales Ritualpodest"],
        "tooltip.abyssalport.staff_tier": ["Rending tier: %s", "Zerrüttungsstufe: %s"],
        "message.abyssalport.formation.book_too_weak": ["This ritual ground needs a stronger Necronomicon.", "Dieser Ritualplatz benötigt ein stärkeres Necronomicon."],
        "message.abyssalport.formation.wrong_dimension": ["Form this ritual ground in the Abyssal Wasteland.", "Forme diesen Ritualplatz im Abyssalen Ödland."],
        "item.abyssalport.abyssal_wasteland_essence": ["Abyssal Wasteland Essence", "Essenz des Abyssalen Ödlands"],
        "item.abyssalport.skin_of_the_abyssal_wasteland": ["Skin of the Abyssal Wasteland", "Haut des Abyssalen Ödlands"],
        "item.abyssalport.abyssal_wasteland_necronomicon": ["Abyssal Wasteland Necronomicon", "Necronomicon des Abyssalen Ödlands"],
        "tooltip.abyssalport.book_tier": ["Book tier: %s", "Buchstufe: %s"],
        "tooltip.abyssalport.rending_staff": ["Right-click a visible creature to collect rending energy.", "Rechtsklick auf eine sichtbare Kreatur sammelt Zerrüttungsenergie."],
        "tooltip.abyssalport.rending.abyssal": ["Abyssal essence: %s / %s", "Abyssale Essenz: %s / %s"],
        "tooltip.abyssalport.rending.shadow": ["Shadow energy: %s / %s", "Schattenenergie: %s / %s"],
        "research.abyssalport.rending": ["Rending", "Zerrüttung"],
        "research.abyssalport.wasteland_book": ["Wasteland Necronomicon", "Ödland-Necronomicon"],
        "screen.abyssalport.page.15": ["Staff of Rending", "Stab der Zerrüttung"],
        "screen.abyssalport.page.16": ["A stronger Necronomicon", "Ein stärkeres Necronomicon"],
        "screen.abyssalport.locked.15": ["Discover an Oblivion Shard or a Staff of Rending first.", "Entdecke zuerst einen Oblivionssplitter oder den Stab der Zerrüttung."],
        "screen.abyssalport.locked.16": ["Enter the Abyssal Wasteland to learn the next book upgrade.", "Betritt das Abyssale Ödland, um die nächste Buchverbesserung zu erforschen."],
        "screen.abyssalport.rending": ["Craft the Staff of Rending from four Shadow Shards and an Oblivion Shard. Right-click while aiming at a visible creature, at most 50 blocks away. Each successful drain deals one damage and collects one energy. Walls, invulnerable targets and repeated clicks during the half-second cooldown give no energy.",
            "Fertige den Stab aus vier Schattensplittern und einem Oblivionssplitter. Ziele per Rechtsklick auf eine sichtbare Kreatur in höchstens 50 Blöcken Entfernung. Jeder erfolgreiche Entzug verursacht einen Schadenspunkt und sammelt eine Energie. Wände, unverwundbare Ziele und Klicks während der halben Sekunde Abklingzeit liefern nichts."],
        "screen.abyssalport.rending_note": ["Drain Abyssal Zombies in the Abyssal Wasteland: 100 Abyssal energy yields one Wasteland Essence. Shadow Creatures yield Shadow energy in any dimension; 200 creates a Shadow Gem. Full energy is exchanged on right-click; if the inventory and item drop both refuse the reward, the energy stays in the staff.",
            "Entziehe Abyssalen Zombies im Ödland Energie: 100 abyssale Energie ergeben eine Ödland-Essenz. Schattenkreaturen liefern überall Schattenenergie; 200 ergeben einen Schattenedelstein. Volle Energie wird per Rechtsklick eingelöst. Scheitern sowohl Inventar als auch Item-Drop, bleibt sie im Stab."],
        "screen.abyssalport.wasteland_book": ["Surround one Wasteland Essence with eight Coralium-Plagued Flesh to craft one Skin of the Abyssal Wasteland. In a 2 x 2 pattern, place three skins and your Necronomicon in the bottom-right corner. The upgraded book stores 10,000 PE; the old book is consumed, not returned.",
            "Umgib eine Ödland-Essenz mit acht verseuchten Fleischstücken für eine Ödland-Haut. Lege drei Häute und dein Necronomicon unten rechts in ein 2-x-2-Muster. Das verbesserte Buch speichert 10.000 PE; das alte Buch wird verbraucht und nicht zurückgegeben."],
        "screen.abyssalport.wasteland_book_note": ["The upgrade retains stored PE, custom names and other book data. Effigies, collectors, relays and the Energy Container recognize both tiers and obey each book's actual capacity. Your discoveries remain attached to you, not copied from another player's book.",
            "Das Upgrade erhält gespeicherte PE, eigene Namen und weitere Buchdaten. Statuen, Kollektoren, Relais und Energiebehälter erkennen beide Stufen mit ihrer jeweiligen Kapazität. Entdeckungen bleiben an deinen Spieler gebunden, nicht an ein fremdes Buch."],
        "screen.abyssalport.wasteland_rituals": ["In the Abyssal Wasteland, use your upgraded book on the same nine-block ritual pattern made from Abyssal Cobblestone. Put your Staff of Rending on the central altar; place one Shadow Gem, four Abyssal Stone and three Coralium-Plagued Flesh on the eight pedestals in any order. Start with a tier-one book holding at least 1,000 PE. The upgraded staff deals and collects two per successful drain, retaining its stored energy, name and other data. Excess energy survives redemption.",
            "Forme im Abyssalen Ödland mit dem stärkeren Buch den bekannten Neun-Block-Ritualplatz aus Abyssalem Bruchstein. Stab in die Mitte; ein Schattenedelstein, vier Abyssale Steine und drei verseuchte Fleischstücke in beliebiger Reihenfolge auf die acht Podeste. Starte mit mindestens 1.000 PE im Stufe-eins-Buch. Der verbesserte Stab verursacht und sammelt zwei pro Entzug; Energie, Name und weitere Daten bleiben erhalten. Überschüssige Energie bleibt auch beim Einlösen bestehen."]}
    for index, locale in enumerate(["en_us", "de_de"]):
        path = ASSETS / "lang" / (locale+".json"); value = json.loads(path.read_text(encoding="utf-8"))
        value.update({key: pair[index] for key, pair in texts.items()})
        write(ASSETS, "lang/"+locale+".json", value)


if __name__ == "__main__":
    media(); data()
