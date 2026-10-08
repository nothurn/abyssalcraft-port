"""Own pixel-art bags and reachable source-informed 18/36/54-slot crafting tiers.

Only this project's resources are used. No upstream media are read or copied.
"""
import json
from PIL import Image, ImageDraw
from generate_crystallizer import ASSETS, DATA, write, texture


def generate():
    tiers = [("small", "#dab565", "Small Crystal Bag", "Kleine Kristalltasche"),
             ("medium", "#71989d", "Medium Crystal Bag", "Mittlere Kristalltasche"),
             ("large", "#b86572", "Large Crystal Bag", "Große Kristalltasche")]
    for name, accent, english, german in tiers:
        ident = name + "_crystal_bag"
        image = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); draw = ImageDraw.Draw(image)
        draw.polygon([(5, 1), (10, 1), (12, 3), (13, 7), (13, 12), (11, 14), (4, 14), (2, 12), (2, 7), (3, 3)],
                     fill="#372c32", outline="#211e29")
        draw.rectangle((4, 5, 11, 12), fill="#64504b")
        draw.line((3, 8, 3, 12), fill="#836653"); draw.line((11, 5, 11, 12), fill="#4d3c3b")
        draw.rectangle((4, 3, 11, 4), fill=accent); draw.line((5, 2, 10, 2), fill="#957661")
        draw.line((6, 4, 6, 6), fill="#c6a77b"); draw.line((9, 4, 9, 6), fill="#c6a77b")
        draw.polygon([(7, 6), (10, 9), (7, 12), (5, 9)], fill=accent, outline="#292838")
        draw.line((7, 7, 7, 10), fill="#e1e3dc")
        texture("item/" + ident + ".png", image)
        write(ASSETS, "models/item/" + ident + ".json", {"parent": "minecraft:item/generated", "textures": {"layer0": "abyssalport:item/" + ident}})
        if name == "small":
            recipe = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SLS", "LGL", "LLL"],
                      "key": {"S": {"item": "minecraft:string"}, "L": {"item": "minecraft:leather"}, "G": {"item": "minecraft:gold_ingot"}}}
        else:
            source = "small" if name == "medium" else "medium"
            skin = "skin_of_the_abyssal_wasteland" if name == "medium" else "skin_of_the_dreadlands"
            recipe = {"type": "abyssalport:crystal_bag_upgrade", "category": "misc", "pattern": ["SSS", "SBS", "SSS"],
                      "key": {"S": {"item": "abyssalport:" + skin}, "B": {"item": "abyssalport:" + source + "_crystal_bag"}}}
        recipe["result"] = {"id": "abyssalport:" + ident}
        write(DATA, "recipe/" + ident + ".json", recipe)
        trigger = "minecraft:leather" if name == "small" else "abyssalport:" + skin
        write(DATA, "advancement/recipes/" + ident + ".json", {"criteria": {"has_ingredient": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [trigger]}]}}},
              "rewards": {"recipes": ["abyssalport:" + ident]}})
    pairs = {
        "tooltip.abyssalport.crystal_bag": ["Crystals: %s / %s slots", "Kristalle: %s / %s Plätze"],
        "tooltip.abyssalport.crystal_bag.open": ["Right-click to open. Crystals only.", "Rechtsklick zum Öffnen. Nur Kristalle."],
        "message.abyssalport.crystal_bag.invalid": ["This bag contains invalid or oversized data. Nothing was removed.", "Diese Tasche enthält ungültige oder zu große Daten. Nichts wurde entfernt."],
        "screen.abyssalport.crystal_bag.capacity": ["%s crystal slots", "%s Kristallplätze"],
        "research.abyssalport.crystal_bags": ["Crystal storage", "Kristallaufbewahrung"],
        "screen.abyssalport.page.25": ["Crystal bags", "Kristalltaschen"],
        "screen.abyssalport.crystal_bags": [
            "Two String, six Leather and one Gold Ingot make a Small Crystal Bag with 18 slots. Right-click it in either hand to open it. Store crystal fragments, shards, crystals and crystal cluster blocks. Shift-click moves crystals between the bag and your inventory. Other items and nested bags are not accepted. The held bag stays locked while its inventory is open; switching away closes access. Each bag keeps its own contents through saves, drops and travel.",
            "Zwei Fäden, sechs Leder und ein Goldbarren ergeben eine kleine Kristalltasche mit 18 Plätzen. Öffne sie per Rechtsklick in einer beliebigen Hand. Sie nimmt Kristallfragmente, Splitter, Kristalle und Kristall-Clusterblöcke auf. Shift-Klick verschiebt Kristalle zwischen Tasche und Inventar. Andere Gegenstände und verschachtelte Taschen werden abgewiesen. Die gehaltene Tasche bleibt während des Öffnens gesperrt; ein Handwechsel beendet den Zugriff. Jede Tasche behält ihren eigenen Inhalt beim Speichern, Fallenlassen und Reisen."],
        "screen.abyssalport.crystal_bags.upgrade": [
            "Surround the small bag with eight Skins of the Abyssal Wasteland for 36 slots. Surround that medium bag with eight Skins of the Dreadlands for 54 slots. Both upgrades preserve crystal contents, occupied positions and custom names, while consuming the old bag exactly once. The original's 72-slot Omothol tier is not available until its genuine late-game ingredients are implemented; it is not replaced by a cheap placeholder recipe.",
            "Umgib die kleine Tasche mit acht Häuten des Abyssalen Ödlands für 36 Plätze. Acht Häute der Dreadlands um diese mittlere Tasche ergeben 54 Plätze. Beide Upgrades bewahren Kristalle, belegte Positionen und eigene Namen; die alte Tasche wird genau einmal verbraucht. Die 72-Platz-Omothol-Stufe des Originals folgt erst mit ihren tatsächlichen späteren Zutaten und erhält kein billiges Platzhalterrezept."]}
    for name, _, english, german in tiers:
        pairs["item.abyssalport."+name+"_crystal_bag"] = [english, german]
    for index, locale in enumerate(["en_us", "de_de"]):
        lang = json.loads((ASSETS / "lang" / (locale + ".json")).read_text(encoding="utf-8"))
        lang.update({key: text[index] for key, text in pairs.items()}); write(ASSETS, "lang/" + locale + ".json", lang)


if __name__ == "__main__":
    generate()
