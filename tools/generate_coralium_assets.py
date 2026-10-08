"""Own mineral pixel art and source-informed recipe/worldgen data. No upstream media is read."""
from pathlib import Path
import json
import random
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = ROOT / "assets/abyssalport"
DATA = ROOT / "data/abyssalport"
JADE = ["#0d3437", "#19676a", "#28a197", "#62cfb3", "#b4edd6"]


def write(root, relative, value):
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def save_image(relative, image):
    path = ASSETS / "textures" / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def crystal(draw, x, y, palette=JADE):
    draw.polygon([(x, y-2), (x+2, y), (x+1, y+3), (x-1, y+3), (x-2, y)], fill=palette[0])
    draw.line((x, y-1, x, y+2), fill=palette[3])
    draw.line((x+1, y, x+1, y+1), fill=palette[2])
    draw.point((x, y-1), fill=palette[4])


def paint():
    for name in ["coralium_ore", "coralium_infused_stone"]:
        rng = random.Random(557)
        image = Image.new("RGB", (16, 16))
        for y in range(16):
            for x in range(16):
                n = rng.choice([82, 90, 98, 102, 107])
                image.putpixel((x, y), (n, n+2, n+4))
        draw = ImageDraw.Draw(image)
        if name.endswith("ore"):
            for x, y in [(3, 4), (10, 3), (7, 10), (13, 12)]: crystal(draw, x, y)
        else:
            for x, y in [(3, 4), (11, 5), (6, 11)]:
                draw.ellipse((x-2, y-2, x+2, y+2), fill=JADE[0])
                draw.ellipse((x-1, y-1, x+1, y+1), fill="#b8d9c9")
                draw.point((x-1, y-1), fill="#edf1d5")
            draw.line((5, 6, 9, 9), fill=JADE[1])
        save_image("block/" + name + ".png", image)
    image = Image.new("RGBA", (16, 16))
    crystal(ImageDraw.Draw(image), 8, 7)
    save_image("item/coralium_gem.png", image)
    positions = [(7, 8), (10, 9), (4, 9), (8, 4), (12, 5), (3, 4), (6, 12), (11, 12), (8, 9)]
    for count in range(2, 10):
        image = Image.new("RGBA", (16, 16))
        draw = ImageDraw.Draw(image)
        for x, y in positions[:count]: crystal(draw, x, y)
        save_image(f"item/coralium_gem_cluster_{count}.png", image)
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    draw.polygon([(7, 2), (11, 3), (13, 7), (12, 11), (8, 14), (4, 12), (2, 8), (3, 4)], fill=JADE[0])
    draw.ellipse((3, 3, 12, 12), fill="#6fa493")
    draw.ellipse((4, 3, 11, 11), fill="#bdd2b5")
    draw.ellipse((5, 4, 9, 8), fill="#e8edca")
    draw.point((6, 5), fill="#ffffff")
    save_image("item/coralium_pearl.png", image)
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    draw.polygon([(7, 1), (12, 4), (14, 9), (10, 14), (5, 14), (1, 9), (3, 4)], fill="#152c36")
    draw.polygon([(7, 2), (11, 5), (12, 9), (9, 12), (5, 12), (3, 9), (4, 5)], fill=JADE[2])
    draw.polygon([(7, 3), (10, 5), (8, 7), (4, 6)], fill=JADE[4])
    draw.polygon([(8, 7), (12, 9), (9, 12), (5, 12)], fill=JADE[1])
    draw.line((2, 9, 6, 13), fill="#ad8651")
    draw.line((12, 5, 13, 9), fill="#c5a26c")
    save_image("item/transmutation_gem.png", image)
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    draw.polygon([(8, 1), (12, 4), (11, 8), (7, 14), (3, 12), (5, 6)], fill="#171522")
    draw.polygon([(8, 2), (10, 4), (8, 9), (5, 12), (6, 6)], fill="#6a657e")
    draw.line((8, 3, 6, 8), fill="#c0c5c9")
    draw.line((10, 6, 7, 12), fill=JADE[1])
    save_image("item/oblivion_shard.png", image)
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    draw.polygon([(7, 1), (12, 3), (14, 8), (11, 13), (7, 15), (2, 12), (1, 7), (3, 3)], fill="#201d2c")
    draw.polygon([(7, 2), (11, 4), (12, 8), (10, 12), (6, 13), (3, 10), (3, 6)], fill="#555064")
    draw.line((5, 3, 4, 8, 7, 12), fill="#b8b9c6", width=1)
    draw.ellipse((5, 5, 10, 10), fill="#102d32")
    draw.ellipse((6, 6, 9, 9), fill=JADE[2])
    draw.point((7, 6), fill=JADE[4])
    save_image("item/oblivion_catalyst.png", image)


def shapeless(name, ingredients, output, count=1):
    write(DATA, "recipe/" + name + ".json", {"type": "minecraft:crafting_shapeless", "category": "misc",
          "ingredients": [{"item": x} for x in ingredients], "result": {"id": "abyssalport:" + output, "count": count}})


def shaped(name, pattern, keys, output):
    write(DATA, "recipe/" + name + ".json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
          "key": {k: {"item": v} for k, v in keys.items()}, "result": {"id": "abyssalport:" + output, "count": 1}})


def ore_loot(name, output, maximum, fortune):
    gem = {"type": "minecraft:item", "name": "abyssalport:" + output}
    functions = []
    if maximum > 1: functions.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": maximum}})
    if fortune: functions.append({"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"})
    if functions: gem["functions"] = functions
    gem["conditions"] = [{"condition": "minecraft:survives_explosion"}]
    write(DATA, f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": "abyssalport:"+name, "conditions": [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}, gem]}]}]})


def resources():
    for name in ["coralium_ore", "coralium_infused_stone"]:
        write(ASSETS, "blockstates/"+name+".json", {"variants": {"": {"model": "abyssalport:block/"+name}}})
        write(ASSETS, "models/block/"+name+".json", {"parent": "minecraft:block/cube_all", "textures": {"all": "abyssalport:block/"+name}})
        write(ASSETS, "models/item/"+name+".json", {"parent": "abyssalport:block/"+name})
    ore_loot("coralium_ore", "coralium_gem", 3, True)
    ore_loot("coralium_infused_stone", "coralium_pearl", 1, True)
    names = ["coralium_gem", "coralium_pearl", "transmutation_gem", "oblivion_shard", "oblivion_catalyst"] + [f"coralium_gem_cluster_{n}" for n in range(2, 10)]
    for name in names:
        write(ASSETS, "models/item/"+name+".json", {"parent": "minecraft:item/generated", "textures": {"layer0": "abyssalport:item/"+name}})
    for n in range(2, 10):
        cluster = f"coralium_gem_cluster_{n}"
        shapeless(cluster, ["abyssalport:coralium_gem"]*n, cluster)
        shapeless(f"coralium_gems_from_cluster_{n}", ["abyssalport:"+cluster], "coralium_gem", n)
        if n >= 4:
            shapeless(cluster+"_combined", [f"abyssalport:coralium_gem_cluster_{n//2}", f"abyssalport:coralium_gem_cluster_{n-n//2}"], cluster)
    shaped("coralium_infused_stone", ["SSS", "CCC", "SSS"], {"S": "minecraft:stone", "C": "abyssalport:coralium_gem_cluster_3"}, "coralium_infused_stone")
    shaped("coralium_infused_stone_dense", ["SSS", "SCS", "SSS"], {"S": "minecraft:stone", "C": "abyssalport:coralium_gem_cluster_9"}, "coralium_infused_stone")
    shaped("oblivion_shard", [" G ", "GTG", " G "], {"G": "abyssalport:shadow_gem", "T": "abyssalport:transmutation_gem"}, "oblivion_shard")
    for source, output in [("coralium_ore", "coralium_gem"), ("coralium_infused_stone", "coralium_pearl")]:
        for cooking, ticks in [("smelting", 200), ("blasting", 100)]:
            write(DATA, f"recipe/{output}_from_{cooking}.json", {"type": "minecraft:"+cooking, "category": "misc",
                  "ingredient": {"item": "abyssalport:"+source}, "result": {"id": "abyssalport:"+output}, "experience": 3.0, "cookingtime": ticks})
    write(DATA, "rituals/transmutation_gem.json", {"energy_cost": 300, "duration_ticks": 20, "altar_input": "abyssalport:coralium_pearl",
          "offerings": ["minecraft:diamond", "minecraft:blaze_powder", "minecraft:ender_pearl", "minecraft:blaze_powder",
                        "minecraft:diamond", "minecraft:blaze_powder", "minecraft:ender_pearl", "minecraft:blaze_powder"],
          "result": {"item": "abyssalport:transmutation_gem", "count": 1}})
    write(DATA, "rituals/oblivion_catalyst.json", {"energy_cost": 5000, "duration_ticks": 200,
          "altar_input": "minecraft:ender_eye", "living_sacrifice": True,
          "offerings": ["minecraft:redstone", "abyssalport:oblivion_shard"]*4,
          "result": {"item": "abyssalport:oblivion_catalyst", "count": 1}})
    mineral_recipes = [f"abyssalport:coralium_gem_cluster_{n}" for n in range(2, 10)]
    mineral_recipes += [f"abyssalport:coralium_gems_from_cluster_{n}" for n in range(2, 10)]
    mineral_recipes += [f"abyssalport:coralium_gem_cluster_{n}_combined" for n in range(4, 10)]
    mineral_recipes += ["abyssalport:coralium_infused_stone", "abyssalport:coralium_infused_stone_dense",
                        "abyssalport:coralium_pearl_from_smelting", "abyssalport:coralium_pearl_from_blasting",
                        "abyssalport:coralium_gem_from_smelting", "abyssalport:coralium_gem_from_blasting"]
    for name, ingredient, recipes in [("coralium_processing", "coralium_gem", mineral_recipes),
                                     ("oblivion_shard", "transmutation_gem", ["abyssalport:oblivion_shard"])]:
        write(DATA, f"advancement/recipes/{name}.json", {"criteria": {"sample": {"trigger": "minecraft:inventory_changed",
              "conditions": {"items": [{"items": "abyssalport:"+ingredient}]}}}, "requirements": [["sample"]],
              "rewards": {"recipes": recipes}})
    write(DATA, "worldgen/configured_feature/ore_coralium.json", {"type": "minecraft:ore", "config": {"size": 8, "discard_chance_on_air_exposure": 0.0,
          "targets": [{"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}, "state": {"Name": "abyssalport:coralium_ore"}}]}})
    groups = {"swamps": (6, 0, 39, ["minecraft:swamp", "minecraft:mangrove_swamp"]),
              "oceans": (12, 0, 39, ["minecraft:ocean", "minecraft:cold_ocean", "minecraft:frozen_ocean", "minecraft:lukewarm_ocean", "minecraft:warm_ocean"]),
              "deep_oceans": (12, -20, 19, ["minecraft:deep_ocean", "minecraft:deep_cold_ocean", "minecraft:deep_frozen_ocean", "minecraft:deep_lukewarm_ocean"])}
    for group, (attempts, low, high, biomes) in groups.items():
        name = "ore_coralium_"+group
        write(DATA, f"tags/worldgen/biome/coralium_{group}.json", {"replace": False, "values": biomes})
        write(DATA, f"worldgen/placed_feature/{name}.json", {"feature": "abyssalport:ore_coralium", "placement": [
              {"type": "minecraft:count", "count": attempts}, {"type": "minecraft:in_square"},
              {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": low}, "max_inclusive": {"absolute": high}}},
              {"type": "minecraft:biome"}]})
        write(DATA, f"neoforge/biome_modifier/{name}.json", {"type": "neoforge:add_features", "biomes": "#abyssalport:coralium_"+group,
              "features": "abyssalport:"+name, "step": "underground_ores"})


def language():
    common = {"message.abyssalport.sacrifice_missing": ["No eligible adult creature is within four blocks of the altar.", "Keine geeignete erwachsene Kreatur im Vier-Block-Bereich des Altars."],
        "message.abyssalport.sacrifice_selected": ["Sacrifice selected: %s. Slay the glowing creature near the altar before the ritual ends.", "Opfer ausgewählt: %s. Töte die leuchtende Kreatur beim Altar vor Ritualende."],
        "message.abyssalport.sacrifice_interrupted": ["Sacrifice ritual interrupted. PE and altar ingredients were not consumed.", "Opferritual abgebrochen. PE und Altarzutaten wurden nicht verbraucht."],
        "research.abyssalport.oblivion": ["Oblivion catalyst", "Vergessenheitskatalysator"],
        "screen.abyssalport.page.11": ["Living sacrifice", "Lebendes Opfer"],
        "screen.abyssalport.locked.11": ["Create a Transmutation Gem to study the Oblivion ritual.", "Stelle einen Transmutationsstein her, um das Vergessenheitsritual zu erforschen."],
        "screen.abyssalport.sacrifice": ["Place an Eye of Ender on the altar and put 4 redstone dust and 4 Oblivion Shards on the pedestals. Keep a fully charged 5,000-PE Necronomicon in hand. An adult animal, bat, water creature, spider or villager must be within four blocks of the altar; children and undead are excluded.", "Lege ein Enderauge auf den Altar und 4 Redstone-Staub sowie 4 Vergessenheitssplitter auf die Podeste. Halte ein voll geladenes 5.000-PE-Necronomicon. Ein erwachsenes Tier, eine Fledermaus, Wasserkreatur, Spinne oder ein Dorfbewohner muss im Vier-Block-Bereich sein; Jungtiere und Untote sind ausgeschlossen."],
        "screen.abyssalport.sacrifice_note": ["Starting the ten-second ritual marks the selected creature with a glow. Slay that creature near the altar before time runs out. Stay within eight blocks and keep the book in hand. On completion, retrieve the Oblivion Catalyst with an empty hand. Despawn, escape and unloaded creatures never count as a sacrifice; cancellation preserves PE and ingredients, but cannot undo a creature's death.", "Das zehnsekündige Ritual markiert die ausgewählte Kreatur leuchtend. Töte sie vor Ablauf beim Altar. Bleibe im Acht-Block-Bereich und halte das Buch. Nimm den Vergessenheitskatalysator danach mit leerer Hand. Despawn, Flucht und entladene Kreaturen gelten nie als Opfer. Abbruch erhält PE und Zutaten, kann aber den Tod einer Kreatur nicht rückgängig machen."],
        "message.abyssalport.altar_occupied": ["The altar is occupied or a ritual is running. Use an empty hand to retrieve the ingredient.", "Der Altar ist belegt oder ein Ritual läuft. Nimm die Zutat mit leerer Hand zurück."],
        "tooltip.abyssalport.transmutation_gem": ["Crafting catalyst · %s uses remaining", "Herstellungskatalysator · %s Anwendungen übrig"],
        "research.abyssalport.coralium": ["Coralium", "Coralium"], "research.abyssalport.transmutation": ["Infusion and transmutation", "Infusion und Transmutation"],
        "screen.abyssalport.page.9": ["Coralium", "Coralium"], "screen.abyssalport.page.10": ["Infusion", "Infusion"],
        "screen.abyssalport.locked.9": ["Find Coralium beneath an ocean or swamp.", "Finde Coralium unter einem Ozean oder Sumpf."],
        "screen.abyssalport.locked.10": ["Find Coralium to reveal the first infusion ritual.", "Finde Coralium, um das erste Infusionsritual aufzudecken."],
        "screen.abyssalport.coralium": ["Mine Coralium Ore below oceans and swamps with an iron pickaxe. It drops 1–3 gems. Fortune increases the yield; Silk Touch preserves the ore. Combine 2–9 gems into a cluster or unpack a cluster without loss.", "Baue Coralium-Erz unter Ozeanen und Sümpfen mit einer Eisenspitzhacke ab. Es liefert 1–3 Edelsteine; Glück erhöht die Ausbeute, Behutsamkeit erhält das Erz. Bündle 2–9 Edelsteine zu einem Cluster oder löse ihn verlustfrei auf."],
        "screen.abyssalport.coralium_note": ["Surround a nine-gem cluster with eight stone blocks, then smelt the infused stone into one Coralium Pearl. Alternatively, place three three-gem clusters between two rows of stone.", "Umgib einen Neunercluster mit acht Steinblöcken und schmilz den durchdrungenen Stein zu einer Coralium-Perle. Alternativ: drei Dreiercluster zwischen zwei Steinreihen."],
        "screen.abyssalport.infusion": ["Put one Coralium Pearl on the central altar. Fill the eight pedestals with 2 diamonds, 2 ender pearls and 4 blaze powder, in any order. Activate with a Necronomicon holding at least 300 PE. The resulting Transmutation Gem remains on the altar.", "Lege eine Coralium-Perle auf den Altar. Belege die acht Podeste mit 2 Diamanten, 2 Enderperlen und 4 Lohenstaub, beliebig angeordnet. Aktiviere mit einem Necronomicon mit mindestens 300 PE. Der Transmutationsstein bleibt auf dem Altar."],
        "screen.abyssalport.infusion_note": ["Use an empty hand to collect the result. A Transmutation Gem surrounded by four Shadow Gems crafts an Oblivion Shard. The catalyst survives ten crafts, losing one use each time. Breaking the altar returns its stored ingredient.", "Nimm das Ergebnis mit leerer Hand. Ein Transmutationsstein, von vier Schattenedelsteinen umgeben, ergibt einen Vergessenheitssplitter. Der Katalysator hält zehn Herstellvorgänge. Beim Altarabbau bleibt seine gespeicherte Zutat erhalten."]}
    for i, locale in enumerate(["en_us", "de_de"]):
        path = ASSETS / "lang" / (locale+".json")
        values = json.loads(path.read_text(encoding="utf-8"))
        values.update({key: texts[i] for key, texts in common.items()})
        for name, labels in {"coralium_gem": ["Coralium Gem", "Coralium-Edelstein"], "coralium_pearl": ["Coralium Pearl", "Coralium-Perle"],
                            "transmutation_gem": ["Transmutation Gem", "Transmutationsstein"], "oblivion_shard": ["Oblivion Shard", "Vergessenheitssplitter"],
                            "oblivion_catalyst": ["Oblivion Catalyst", "Vergessenheitskatalysator"]}.items():
            values["item.abyssalport."+name] = labels[i]
        values["block.abyssalport.coralium_ore"] = ["Coralium Ore", "Coralium-Erz"][i]
        values["block.abyssalport.coralium_infused_stone"] = ["Coralium-Infused Stone", "Coralium-durchdrungener Stein"][i]
        for n in range(2, 10): values[f"item.abyssalport.coralium_gem_cluster_{n}"] = [f"Coralium Gem Cluster ({n})", f"Coralium-Cluster ({n})"][i]
        write(ASSETS, "lang/"+locale+".json", values)


if __name__ == "__main__":
    paint()
    resources()
    language()
