"""Four reachable charm resources, using independently drawn 16px amulets.

Source facts: charm_0 recipe, AbyssalCrafting.addForgingsFor and ItemCharm.
No deity infusion shortcut or upstream bitmap is used.
"""
import json
from PIL import Image, ImageDraw
from generate_wasteland_stronghold import ASSETS, DATA, write, texture

NAMES = ("ritual_charm", "range_ritual_charm", "power_ritual_charm", "duration_ritual_charm")


def generate():
    for index, name in enumerate(NAMES):
        image = Image.new("RGBA", (16, 16))
        draw = ImageDraw.Draw(image)
        draw.line((5, 4, 5, 1, 10, 1, 10, 4), fill="#8b682b", width=1)
        draw.line((6, 2, 9, 2), fill="#eed592")
        draw.polygon([(4, 4), (11, 4), (13, 6), (13, 11), (10, 14), (5, 14), (2, 11), (2, 6)], fill="#4b3823")
        draw.polygon([(4, 5), (11, 5), (12, 7), (12, 10), (10, 13), (5, 13), (3, 10), (3, 7)], fill="#b68b39")
        draw.line((4, 5, 11, 5, 12, 7), fill="#f2d891")
        draw.line((3, 7, 3, 10, 5, 13), fill="#8c6128")
        draw.rectangle((5, 7, 10, 11), fill="#524337")
        colors = ("#bdb09a", "#94c5ac", "#e4a968", "#aaa1d0")
        if index == 0:
            draw.rectangle((6, 8, 9, 10), outline=colors[index])
        elif index == 1:
            draw.line((5, 9, 10, 9), fill=colors[index])
            draw.line((8, 7, 10, 9, 8, 11), fill=colors[index])
        elif index == 2:
            draw.polygon([(7, 7), (9, 8), (8, 9), (10, 10), (7, 11), (6, 10), (7, 9), (5, 8)], fill=colors[index])
        else:
            draw.rectangle((6, 7, 9, 11), outline=colors[index])
            draw.line((7, 8, 8, 9, 7, 10), fill=colors[index])
        texture("item/" + name + ".png", image)
        write(ASSETS, "models/item/" + name + ".json", {
            "parent": "minecraft:item/generated", "textures": {"layer0": "abyssalport:item/" + name}})
    write(DATA, "recipe/ritual_charm.json", {"type": "minecraft:crafting_shaped", "category": "misc",
          "pattern": ["GGG", "GDG", "GGG"], "key": {"G": {"item": "minecraft:gold_ingot"}, "D": {"item": "minecraft:diamond"}},
          "result": {"id": "abyssalport:ritual_charm", "count": 1}})
    for locale in ("en_us", "de_de"):
        lang = json.loads((ASSETS / "lang" / (locale + ".json")).read_text("utf-8"))
        en = locale == "en_us"
        labels = ("Ritual Charm", "Range Ritual Charm", "Power Ritual Charm", "Duration Ritual Charm") if en else (
            "Ritual-Charm", "Reichweiten-Charm", "Kraft-Charm", "Dauer-Charm")
        for name, label in zip(NAMES, labels): lang["item.abyssalport." + name] = label
        for name, label in zip(("none", "range", "power", "duration"),
                               ("None", "Range", "Power", "Duration") if en else ("Keiner", "Reichweite", "Kraft", "Dauer")):
            lang["amplifier.abyssalport." + name] = label
        lang["tooltip.abyssalport.charm_amplifier"] = "Amplifier: %s" if en else "Verstärker: %s"
        lang["tooltip.abyssalport.charm_reset"] = "Clears a statue or Energy Depositioner modifier; reusable." if en else "Entfernt den Verstärker einer Statue oder eines Energiedeponierers; wiederverwendbar."
        lang["tooltip.abyssalport.charm_use"] = "Use on an unmodified statue or Energy Depositioner. Consumes one charm." if en else "Auf eine Statue oder einen Energiedeponierer ohne Verstärker anwenden. Verbraucht einen Charm."
        lang["message.abyssalport.statue_charm"] = "PE amplifier: %s" if en else "PE-Verstärker: %s"
        lang["message.abyssalport.statue_status"] = "%s · %s PE / %s active ticks · range %s" if en else "%s · %s PE / %s aktive Ticks · Reichweite %s"
        lang["screen.abyssalport.energy"] = (
            "New monoliths carry seven possible deity statues on Monolith Stone above a living biomass core. Mine and relocate the statue. Under open sky, its base rate is 5 PE per 120 active ticks. Eight gold ingots around a diamond craft a blank Ritual Charm. In an anvil, combine ONE blank charm with an arrow (range), gunpowder (power) or redstone (duration), costing one level. Use the modifier on an inactive statue; a blank charm clears it without being consumed. Inspect with an empty hand. Generic modifiers give 10 PE, or 15 for Power; Duration halves the interval and Range adds four blocks. Mining the statue removes its modifier. Dropped-item cadence, tolerance/disruptions and structure bonuses remain unfinished." if en else
            "Neue Monolithen tragen eine von sieben Statuen über einem lebenden Biomassekern. Baue die Statue ab und platziere sie unter freiem Himmel: Basis 5 PE je 120 aktive Ticks. Acht Goldbarren um einen Diamanten ergeben einen leeren Ritual-Charm. Am Amboss EINEN Charm mit Pfeil (Reichweite), Schwarzpulver (Kraft) oder Redstone (Dauer) kombinieren; Kosten: eine Stufe. Auf eine inaktive Statue anwenden; ein leerer Charm entfernt den Verstärker ohne Verbrauch. Mit leerer Hand inspizieren. Generische Verstärker liefern 10 PE, Kraft 15; Dauer halbiert das Intervall, Reichweite gibt vier Blöcke hinzu. Abbau entfernt den Verstärker. Dropped-Item-Takt, Toleranz/Disruptions und Strukturboni bleiben unfertig.")
        write(ASSETS, "lang/" + locale + ".json", lang)


if __name__ == "__main__":
    generate()
