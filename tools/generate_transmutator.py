"""Own machine assets and native Coralium-stone -> brick -> transmutator chain."""
import copy
import json
from PIL import Image, ImageDraw, ImageOps
from generate_wasteland_minerals import ROOT, ASSETS, DATA, write, append_tag
from generate_abyssalnite_assets import texture
from generate_dreadlands_progression import tinted


def media():
    for name, source in [("coralium_stone", "abyssal_stone"), ("coralium_cobblestone", "abyssal_cobblestone")]:
        texture("block/"+name+".png", tinted("block/"+source+".png", "#233f3f", "#7aa999"))
        write(ASSETS, "blockstates/"+name+".json", {"variants": {"": {"model": "abyssalport:block/"+name}}})
        write(ASSETS, "models/block/"+name+".json", {"parent": "minecraft:block/cube_all", "textures": {"all": "abyssalport:block/"+name}})
        write(ASSETS, "models/item/"+name+".json", {"parent": "abyssalport:block/"+name})
        loot = json.loads((DATA / "loot_table/blocks/abyssal_stone.json").read_text(encoding="utf-8")) if name == "coralium_stone" else {
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "abyssalport:coralium_cobblestone"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
        if name == "coralium_stone":
            loot = json.loads(json.dumps(loot).replace("abyssal_stone", "coralium_stone").replace("abyssal_cobblestone", "coralium_cobblestone"))
        write(DATA, "loot_table/blocks/"+name+".json", loot)
    brick = Image.new("RGBA", (16, 16)); draw = ImageDraw.Draw(brick)
    draw.polygon([(2, 5), (10, 3), (14, 6), (14, 11), (6, 13), (2, 10)], fill="#548c7b", outline="#203e39")
    draw.polygon([(3, 5), (10, 4), (13, 6), (6, 8)], fill="#97b99d")
    draw.line((6, 8, 6, 12), fill="#729b85"); texture("item/coralium_brick.png", brick)
    write(ASSETS, "models/item/coralium_brick.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "abyssalport:item/coralium_brick"}})
    for name in ["side", "top", "front", "front_on"]:
        image = Image.new("RGBA", (16, 16), "#263c38"); draw = ImageDraw.Draw(image)
        draw.rectangle((1, 1, 14, 14), outline="#678778")
        if name.startswith("front"):
            draw.rectangle((3, 5, 12, 12), fill="#111c1c", outline="#8d9c80")
            draw.rectangle((5, 7, 10, 10), fill="#8ee2b0" if name == "front_on" else "#345f51")
            draw.line((4, 3, 11, 3), fill="#a2b399")
        elif name == "side":
            for y in [4, 8, 12]: draw.line((3, y, 12, y), fill="#405d51")
            draw.rectangle((6, 5, 9, 10), outline="#91bda1")
        else: draw.polygon([(8, 3), (12, 8), (8, 12), (3, 8)], outline="#b0c5a0")
        texture("block/transmutator_"+name+".png", image)
    variants = {}
    for lit in [False, True]:
        model = "transmutator_on" if lit else "transmutator"
        write(ASSETS, "models/block/"+model+".json", {"parent": "minecraft:block/orientable", "textures": {
            "front": "abyssalport:block/transmutator_front"+("_on" if lit else ""), "side": "abyssalport:block/transmutator_side", "top": "abyssalport:block/transmutator_top"}})
        for facing, rotation in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
            variants[f"facing={facing},lit={str(lit).lower()}"] = {"model": "abyssalport:block/"+model, "y": rotation}
    write(ASSETS, "blockstates/transmutator.json", {"variants": variants})
    write(ASSETS, "models/item/transmutator.json", {"parent": "abyssalport:block/transmutator"})
    write(DATA, "loot_table/blocks/transmutator.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "entries": [{"type": "minecraft:item", "name": "abyssalport:transmutator"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    append_tag(ROOT / "data/minecraft", "tags/block/mineable/pickaxe.json", ["abyssalport:"+name for name in ["coralium_stone", "coralium_cobblestone", "transmutator"]])


def data():
    for name, source, target in [("coralium_stone", "coralium_cobblestone", "coralium_stone"), ("coralium_brick", "coralium_stone", "coralium_brick")]:
        write(DATA, "recipe/"+name+".json", {"type": "minecraft:smelting", "category": "blocks", "ingredient": {"item": "abyssalport:"+source},
            "result": {"id": "abyssalport:"+target}, "experience": 0.1, "cookingtime": 200})
    write(DATA, "recipe/transmutator.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["BBB", "BGB", "RCR"],
        "key": {"B": {"item": "abyssalport:coralium_brick"}, "G": {"item": "abyssalport:transmutation_gem"},
                "R": {"item": "abyssalport:refined_coralium_block"}, "C": {"item": "abyssalport:coralium_bucket"}}, "result": {"id": "abyssalport:transmutator"}})
    write(DATA, "advancement/recipes/transmutator.json", {"criteria": {"has_material": {"trigger": "minecraft:inventory_changed",
        "conditions": {"items": [{"items": ["abyssalport:coralium_brick"]}]}}}, "rewards": {"recipes": ["abyssalport:transmutator"]}})
    for name, source, target, count in [
        ("darkstone_to_stone", "abyssalport:darkstone", "minecraft:stone", 1),
        ("stone_to_darkstone", "minecraft:stone", "abyssalport:darkstone", 1),
        ("darkstone_bricks_to_stone_bricks", "abyssalport:darkstone_bricks", "minecraft:stone_bricks", 1),
        ("stone_bricks_to_darkstone_bricks", "minecraft:stone_bricks", "abyssalport:darkstone_bricks", 1),
        ("darkstone_cobble_to_cobble", "abyssalport:darkstone_cobblestone", "minecraft:cobblestone", 1),
        ("cobble_to_darkstone_cobble", "minecraft:cobblestone", "abyssalport:darkstone_cobblestone", 1),
        ("coralium_brick_to_stone", "abyssalport:coralium_brick", "abyssalport:coralium_stone", 1)]:
        write(DATA, "recipe/transmutation/"+name+".json", {"type": "abyssalport:transmutation", "ingredient": {"item": source}, "result": {"id": target, "count": count}, "experience": 0.0})
    # Plateau shares the existing Wasteland ores/ecology; surface material follows BiomeAbyssalPlateau.
    source = json.loads((DATA / "worldgen/biome/abyssal_wasteland.json").read_text(encoding="utf-8"))
    source["effects"].update({"fog_color": 1983024, "sky_color": 2830400})
    write(DATA, "worldgen/biome/abyssal_plateau.json", source)
    pairs = {
        "block.abyssalport.coralium_stone": ["Coralium Stone", "Coralium-Stein"],
        "block.abyssalport.coralium_cobblestone": ["Coralium Cobblestone", "Coralium-Bruchstein"],
        "item.abyssalport.coralium_brick": ["Coralium Brick", "Coralium-Ziegel"],
        "block.abyssalport.transmutator": ["Transmutator", "Transmutator"],
        "biome.abyssalport.abyssal_plateau": ["Abyssal Plateau", "Abyssales Plateau"],
        "screen.abyssalport.machine.input": ["Input", "Eingabe"],
        "screen.abyssalport.machine.fuel": ["Fuel", "Brennstoff"],
        "screen.abyssalport.machine.output": ["Output", "Ausgabe"],
        "research.abyssalport.processing": ["Coralium processing", "Coralium-Verarbeitung"],
        "screen.abyssalport.page.22": ["The Transmutator", "Der Transmutator"],
        "screen.abyssalport.transmutator": ["Mine Coralium Stone from the Abyssal Plateau to obtain cobblestone. Smelt the cobblestone into stone, then smelt stone into bricks. Craft the machine from five Coralium Bricks, a Transmutation Gem, two Refined Coralium Blocks and a Coralium Bucket. Crafting returns the empty bucket and spends one catalyst use. Each operation takes ten seconds. Stone, cobblestone and stone bricks can be converted to their Darkstone counterparts and back. A Coralium Brick converts into Coralium Stone.", "Baue Coralium-Stein im Abyssalen Plateau zu Bruchstein ab. Schmelze den Bruchstein zu Stein und anschließend zu Ziegeln. Baue die Maschine aus fünf Coralium-Ziegeln, einem Transmutationsedelstein, zwei veredelten Coraliumblöcken und einem Coraliumeimer. Beim Craften bleiben der leere Eimer und ein um eine Nutzung geschwächter Katalysator erhalten. Eine Verarbeitung dauert zehn Sekunden. Stein, Bruchstein und Steinziegel lassen sich in ihre Dunkelstein-Varianten und zurück verwandeln. Ein Coralium-Ziegel wird zu Coralium-Stein."],
        "screen.abyssalport.transmutator_fuel": ["This is not a coal furnace. Coralium Gems and Bricks fuel one operation; a Pearl fuels ten. Coralium clusters, plagued flesh and Blaze Powder or Rods also work. A Coralium Bucket lasts 1,000 seconds and leaves one empty bucket. Burning a Transmutation Gem consumes the entire gem; its ten reusable charges apply only to crafting. The machine will not start burning fresh fuel if no recipe matches or the output is blocked.", "Dies ist kein Kohleofen. Coralium-Edelsteine und -Ziegel reichen für eine Verarbeitung, eine Perle für zehn. Auch Coralium-Cluster, verseuchtes Fleisch, Lohenstaub und Lohenruten funktionieren. Ein Coraliumeimer reicht für 1.000 Sekunden und hinterlässt einen leeren Eimer. Das Verbrennen eines Transmutationsedelsteins verbraucht den ganzen Stein; seine zehn wiederverwendbaren Ladungen gelten nur beim Craften. Ohne passendes Rezept oder bei blockierter Ausgabe wird kein neuer Brennstoff angezündet."],
        "screen.abyssalport.transmutator_automation": ["Feed ingredients from above and fuel from the sides. Extract finished items and empty buckets from below. The progress, remaining fuel and inventory survive world reloads. Changing the input recipe or blocking the output resets processing progress; already burning fuel still runs down. Shift-click moves materials between your inventory and the machine. Breaking the machine returns its stored items.", "Zutaten gelangen von oben hinein, Brennstoff von den Seiten. Fertige Gegenstände und leere Eimer können unten entnommen werden. Fortschritt, restlicher Brennstoff und Inventar bleiben beim Weltneustart erhalten. Ein anderes Eingaberezept oder eine blockierte Ausgabe setzt den Verarbeitungsfortschritt zurück; bereits brennender Brennstoff läuft weiter ab. Shift-Klick verschiebt Material zwischen deinem Inventar und der Maschine. Beim Abbau werden gespeicherte Gegenstände zurückgegeben."]}
    for index, locale in enumerate(["en_us", "de_de"]):
        value = json.loads((ASSETS / "lang" / (locale+".json")).read_text(encoding="utf-8"))
        value.update({key: pair[index] for key, pair in pairs.items()}); write(ASSETS, "lang/"+locale+".json", value)


if __name__ == "__main__":
    media(); data()
