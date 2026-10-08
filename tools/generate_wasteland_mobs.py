"""Own humanoid UV skin, drops and progression for the first Wasteland creature.

Original media is never read. Loot probabilities and food/recipe facts are
source-informed; sound playback currently uses Minecraft's native zombie sounds.
"""
import json
import random
from PIL import Image, ImageDraw
from generate_wasteland_minerals import ROOT, ASSETS, DATA, write, append_tag
from generate_abyssalnite_assets import texture


def media():
    image = Image.new("RGBA", (64, 64))
    rng = random.Random(38420)
    def fill(rect, color):
        for y in range(rect[1], rect[3]+1):
            for x in range(rect[0], rect[2]+1):
                noise = rng.randint(-8, 8)
                image.putpixel((x, y), tuple(max(0, min(255, n+noise)) for n in color)+(255,))
    # Standard native humanoid UV, six solid faces per head/body/limb.
    for rect in [(8, 0, 23, 7), (0, 8, 31, 15)]: fill(rect, (77, 106, 82))
    for u, v, skin in [(0, 16, False), (40, 16, True), (16, 48, False), (32, 48, True)]:
        for rect in [(u+4, v, u+11, v+3), (u, v+4, u+15, v+15)]: fill(rect, (44, 61, 56))
        if skin: fill((u, v+12, u+15, v+15), (75, 99, 74))
    for rect in [(20, 16, 35, 19), (16, 20, 39, 31)]: fill(rect, (49, 70, 56))
    draw = ImageDraw.Draw(image)
    draw.line((23, 21, 23, 30), fill="#283e34")
    draw.rectangle((9, 10, 10, 11), fill="#14281f")
    draw.rectangle((13, 10, 14, 11), fill="#14281f")
    draw.point((10, 10), fill="#aac5a3"); draw.point((13, 10), fill="#aac5a3")
    draw.line((10, 13, 13, 13), fill="#304836")
    draw.line((8, 14, 10, 14), fill="#7ba78a")
    draw.line((25, 22, 25, 25), fill="#829b69")
    draw.line((29, 29, 31, 29), fill="#688978")
    texture("entity/abyssal_zombie.png", image)
    for name in ["coralium_plagued_flesh", "coralium_chunk"]:
        image = Image.new("RGBA", (16, 16)); draw = ImageDraw.Draw(image)
        if name.endswith("flesh"):
            draw.polygon([(2, 5), (5, 3), (9, 4), (13, 6), (12, 11), (8, 13), (4, 11)], fill="#476449", outline="#243d32")
            draw.line((4, 5, 8, 7), fill="#8aa35c", width=2)
            draw.line((8, 9, 11, 8), fill="#9eb87a")
            draw.point((5, 10), fill="#477963")
        else:
            draw.polygon([(3, 5), (7, 2), (12, 4), (14, 9), (10, 13), (4, 12), (2, 8)], fill="#225249", outline="#153d32")
            draw.polygon([(4, 5), (7, 3), (11, 5), (8, 7)], fill="#5cb18c")
            draw.line((5, 9, 10, 11), fill="#2f826b", width=2)
            draw.point((7, 4), fill="#b5d9af")
        texture("item/"+name+".png", image)
        write(ASSETS, f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "abyssalport:item/"+name}})
    write(ASSETS, "models/item/abyssal_zombie_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})


def data():
    def looting(): return {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}
    def count(minimum, maximum): return {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": minimum, "max": maximum}}
    write(DATA, "loot_table/entities/abyssal_zombie.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "abyssalport:coralium_plagued_flesh", "functions": [count(0, 2), looting()]}]},
        {"rolls": 1, "conditions": [{"condition": "minecraft:killed_by_player"},
            {"condition": "minecraft:random_chance_with_enchanted_bonus", "unenchanted_chance": .025, "enchanted_chance": {"type": "minecraft:linear", "base": .035, "per_level_above_first": .01}, "enchantment": "minecraft:looting"}],
         "entries": [{"type": "minecraft:item", "name": "minecraft:bone"},
                     {"type": "minecraft:item", "name": "abyssalport:coralium_chunk", "functions": [count(0, 3), looting()]}]}]})
    for kind, ticks in [("smelting", 200), ("blasting", 100)]:
        write(DATA, "recipe/coralium_chunk_"+kind+".json", {"type": "minecraft:"+kind, "category": "misc", "ingredient": {"item": "abyssalport:coralium_chunk"},
              "result": {"id": "abyssalport:refined_coralium_ingot", "count": 2}, "experience": 3.0, "cookingtime": ticks})
    append_tag(DATA, "tags/entity_type/coralium_immune.json", ["abyssalport:abyssal_zombie"])
    append_tag(ROOT / "data/minecraft", "tags/entity_type/undead.json", ["abyssalport:abyssal_zombie"])
    append_tag(ROOT / "data/minecraft", "tags/entity_type/can_breathe_under_water.json", ["abyssalport:abyssal_zombie"])
    for name in ["abyssal_wasteland", "coralium_lake", "abyssal_plateau"]:
        path = DATA / "worldgen/biome" / (name+".json")
        value = json.loads(path.read_text(encoding="utf-8"))
        if not any(entry["type"] == "abyssalport:abyssal_zombie" for entry in value["spawners"]["monster"]):
            value["spawners"]["monster"].append({"type": "abyssalport:abyssal_zombie", "weight": 60, "minCount": 1, "maxCount": 5})
        write(DATA, "worldgen/biome/"+name+".json", value)
    texts = {"entity.abyssalport.abyssal_zombie": ["Abyssal Zombie", "Abyssaler Zombie"],
             "item.abyssalport.abyssal_zombie_spawn_egg": ["Abyssal Zombie Spawn Egg", "Abyssaler-Zombie-Spawn-Ei"],
             "item.abyssalport.coralium_chunk": ["Coralium Chunk", "Coraliumbrocken"],
             "item.abyssalport.coralium_plagued_flesh": ["Coralium-Plagued Flesh", "Coraliumpestverseuchtes Fleisch"],
             "screen.abyssalport.wasteland_creatures": ["Abyssal Zombies inhabit this dimension and spread Coralium Plague through successful melee attacks. They breathe underwater, resist the plague, attack ordinary zombies and can infect defeated zombies or villagers. Their flesh is unsafe food: it causes hunger, nausea and plague. Rare Coralium Chunks from player kills smelt into two Refined Coralium Ingots; mining Liquified Coralium is the reliable route.",
                 "Abyssale Zombies bewohnen diese Dimension und übertragen Coraliumpest bei erfolgreichen Nahkampfangriffen. Sie atmen unter Wasser, widerstehen der Pest, greifen normale Zombies an und können besiegte Zombies oder Dorfbewohner verwandeln. Ihr Fleisch verursacht Hunger, Übelkeit und Pest. Seltene Coraliumbrocken von Spielerkills ergeben zwei raffinierte Coraliumbarren; der Erzabbau bleibt der verlässliche Weg."]}
    for index, locale in enumerate(["en_us", "de_de"]):
        path = ASSETS / "lang" / (locale+".json"); value = json.loads(path.read_text(encoding="utf-8"))
        value.update({key: pair[index] for key, pair in texts.items()})
        write(ASSETS, "lang/"+locale+".json", value)


if __name__ == "__main__":
    media()
    data()
