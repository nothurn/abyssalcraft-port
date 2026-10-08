"""Independent Wasteland ore / refined-metal resources. No upstream media is read.

Mechanical values: BiomeAbyssalWastelandBase, AbyssalCraftWorldGenerator,
BlockACOre, AbyssalCraftAPI, AbyssalCrafting and ItemCoraliumArmor.
The native 1.21 terrain/vein placement is not the original 1.12 RNG sequence.
"""
import json
import random
from PIL import Image, ImageDraw
import generate_abyssalnite_assets as metal

ROOT, ASSETS, DATA = metal.ROOT, metal.ASSETS, metal.DATA
write = metal.resource
PREFIX = "refined_coralium"
ORES = {
    # block: drop, amount, vein range, attempts, height range, tool tier, pixels
    "nitre_ore": ("abyssalport:nitre", (1, 3), (4, 5), 3, (0, 29), 2, ("#414741", "#aebfac", "#e3ebda")),
    "abyssal_nitre_ore": ("abyssalport:nitre", (1, 3), (2, 7), 8, (5, 64), 2, ("#414741", "#aebfac", "#e3ebda")),
    "abyssal_coralium_ore": ("abyssalport:coralium_gem", (1, 3), (2, 9), (8, 15), (5, 79), 3, ("#244d40", "#519d73", "#9bd2a2")),
    "abyssal_iron_ore": (None, (1, 1), (2, 7), 8, (5, 64), 2, ("#554636", "#bca389", "#dbc5ae")),
    "abyssal_gold_ore": (None, (1, 1), (2, 4), 5, (5, 39), 2, ("#6e541e", "#d3ab37", "#f2d872")),
    "abyssal_diamond_ore": ("minecraft:diamond", (1, 1), (1, 4), 5, (5, 24), 2, ("#2d6d6e", "#51b9b0", "#a4ede3")),
    "liquified_coralium_ore": (None, (1, 1), (1, 4), 8, (5, 34), 4, ("#163e39", "#2b9d8a", "#65d2b3")),
    "pearlescent_coralium_ore": ("abyssalport:coralium_pearl", (1, 2), (1, 4), (3, 8), (5, 19), 5, ("#366f5e", "#83bca0", "#dcf1cf")),
}


def append_tag(base, relative, values):
    path = base / relative
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
    for value in values:
        if value not in tag["values"]: tag["values"].append(value)
    write(base, relative, tag)


def textures():
    for index, (name, settings) in enumerate(ORES.items()):
        rng = random.Random(8073+index)
        image = Image.new("RGB", (16, 16))
        base = (87, 88, 91) if name == "nitre_ore" else (50, 54, 51)
        for y in range(16):
            for x in range(16):
                noise = rng.randint(-7, 7)
                image.putpixel((x, y), tuple(n+noise for n in base))
        draw = ImageDraw.Draw(image)
        dark, mid, light = settings[-1]
        for x, y in [(3, 3), (10, 4), (6, 9), (12, 12), (2, 13)]:
            draw.polygon([(x-1, y), (x, y-1), (x+2, y), (x+1, y+2), (x, y+2)], fill=dark)
            draw.line((x, y, x+1, y+1), fill=mid)
            draw.point((x, y), fill=light)
        if name == "liquified_coralium_ore":
            draw.line((9, 6, 9, 8), fill=mid)
            draw.line((4, 10, 4, 12), fill=mid)
        metal.texture("block/"+name+".png", image)
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    for x, y in [(4, 8), (8, 5), (9, 10), (11, 6)]:
        draw.polygon([(x-2, y), (x, y-3), (x+2, y), (x+1, y+2)], fill="#8fae9b", outline="#4d6155")
        draw.line((x, y-2, x, y), fill="#e3eddf")
    metal.texture("item/nitre.png", image)
    metal.METAL = ["#17382f", "#28574b", "#488c76", "#80b69b", "#c1dfb8"]
    for suffix in ["ingot", "nugget", *metal.PATTERNS]: metal.item(suffix, PREFIX)
    metal.armor(PREFIX)
    image = Image.new("RGB", (16, 16), metal.METAL[1])
    draw = ImageDraw.Draw(image)
    draw.rectangle((1, 1, 14, 14), fill=metal.METAL[2], outline=metal.METAL[0])
    draw.line((2, 2, 13, 2), fill=metal.METAL[3])
    draw.line((2, 3, 2, 13), fill=metal.METAL[3])
    draw.polygon([(5, 7), (7, 5), (10, 7), (7, 10)], outline=metal.METAL[1])
    draw.point((7, 6), fill=metal.METAL[4])
    metal.texture(f"block/{PREFIX}_block.png", image)


def loot(name, drop, amount):
    normal = {"type": "minecraft:item", "name": drop or "abyssalport:"+name,
              "conditions": [{"condition": "minecraft:survives_explosion"}]}
    if drop:
        normal["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": amount[0], "max": amount[1]}},
                               {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"}]
        entry = {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "abyssalport:"+name, "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}, normal]}
    else: entry = normal
    write(DATA, "loot_table/blocks/"+name+".json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [entry]}]})


def data():
    metal.equipment_tags(PREFIX)
    for name in [*ORES, PREFIX+"_block"]:
        write(ASSETS, f"blockstates/{name}.json", {"variants": {"": {"model": "abyssalport:block/"+name}}})
        write(ASSETS, f"models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "abyssalport:block/"+name}})
        write(ASSETS, f"models/item/{name}.json", {"parent": "abyssalport:block/"+name})
        loot(name, ORES[name][0] if name in ORES else None, ORES[name][1] if name in ORES else (1, 1))
    for name in ["nitre", *[PREFIX+"_"+suffix for suffix in ["ingot", "nugget", *metal.PATTERNS]]]:
        write(ASSETS, f"models/item/{name}.json", {"parent": "minecraft:item/handheld" if name.removeprefix(PREFIX+"_") in metal.TOOLS else "minecraft:item/generated",
              "textures": {"layer0": "abyssalport:item/"+name}})
    for suffix, pattern in {**metal.PATTERNS, "block": ["III", "III", "III"]}.items():
        keys = {"I": {"item": "abyssalport:"+PREFIX+"_ingot"}}
        if suffix in metal.TOOLS: keys["S"] = {"tag": "c:rods/wooden"}
        write(DATA, "recipe/"+PREFIX+"_"+suffix+".json", {"type": "minecraft:crafting_shaped", "category": "equipment" if suffix != "block" else "building",
              "pattern": pattern, "key": keys, "result": {"id": "abyssalport:"+PREFIX+"_"+suffix}})
    for name, source, result, count in [(PREFIX+"_ingot_from_block", PREFIX+"_block", PREFIX+"_ingot", 9),
                                      (PREFIX+"_nugget", PREFIX+"_ingot", PREFIX+"_nugget", 9)]:
        write(DATA, f"recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "ingredients": [{"item": "abyssalport:"+source}],
              "result": {"id": "abyssalport:"+result, "count": count}})
    write(DATA, f"recipe/{PREFIX}_ingot_from_nuggets.json", {"type": "minecraft:crafting_shaped", "pattern": ["NNN", "NNN", "NNN"],
          "key": {"N": {"item": f"abyssalport:{PREFIX}_nugget"}}, "result": {"id": f"abyssalport:{PREFIX}_ingot"}})
    smelting = {"nitre_ore": ("abyssalport:nitre", 1), "abyssal_nitre_ore": ("abyssalport:nitre", 1),
                "abyssal_iron_ore": ("minecraft:iron_ingot", .7), "abyssal_gold_ore": ("minecraft:gold_ingot", 1),
                "abyssal_diamond_ore": ("minecraft:diamond", 1), "abyssal_coralium_ore": ("abyssalport:coralium_gem", 1),
                "liquified_coralium_ore": (f"abyssalport:{PREFIX}_ingot", 3), "pearlescent_coralium_ore": ("abyssalport:coralium_pearl", 3)}
    for name, (result, xp) in smelting.items():
        for kind, ticks in [("smelting", 200), ("blasting", 100)]:
            write(DATA, f"recipe/{name}_{kind}.json", {"type": "minecraft:"+kind, "category": "misc", "ingredient": {"item": "abyssalport:"+name},
                  "result": {"id": result}, "experience": xp, "cookingtime": ticks})
    recipes = [PREFIX+"_"+suffix for suffix in metal.PATTERNS] + [PREFIX+"_block", PREFIX+"_nugget", PREFIX+"_ingot_from_nuggets", PREFIX+"_ingot_from_block"]
    write(DATA, f"advancement/recipes/{PREFIX}_equipment.json", {"criteria": {"metal": {"trigger": "minecraft:inventory_changed",
          "conditions": {"items": [{"items": [f"abyssalport:{PREFIX}_ingot"]}]}}}, "rewards": {"recipes": ["abyssalport:"+r for r in recipes]}})
    for tag, members in [("mineable/pickaxe", [*ORES, PREFIX+"_block"]),
                         ("needs_iron_tool", [name for name, config in ORES.items() if config[5] == 2]+[PREFIX+"_block"]),
                         ("needs_diamond_tool", ["abyssal_coralium_ore"])]:
        append_tag(ROOT / "data/minecraft", "tags/block/"+tag+".json", ["abyssalport:"+name for name in members])
    for tier in ["wooden", "stone", "iron", "diamond", "netherite"]:
        append_tag(ROOT / "data/minecraft", "tags/block/incorrect_for_"+tier+"_tool.json",
                   ["abyssalport:liquified_coralium_ore", "abyssalport:pearlescent_coralium_ore"])
    append_tag(DATA, "tags/block/incorrect_for_abyssalnite_tool.json", ["abyssalport:pearlescent_coralium_ore"])
    write(DATA, "tags/block/incorrect_for_refined_coralium_tool.json", {"replace": False, "values": []})
    for kind, name in [("ingots", PREFIX+"_ingot"), ("nuggets", PREFIX+"_nugget"), ("storage_blocks", PREFIX+"_block"), ("dusts", "nitre")]:
        material = "saltpeter" if kind == "dusts" else PREFIX
        append_tag(ROOT / "data/c", f"tags/item/{kind}/{material}.json", ["abyssalport:"+name])
        append_tag(ROOT / "data/c", f"tags/item/{kind}.json", [f"#c:{kind}/{material}"])


def generation():
    write(DATA, "tags/block/abyssal_ore_replaceables.json", {"replace": False, "values": ["abyssalport:abyssal_stone"]})
    wasteland_features = []
    for name, config in ORES.items():
        overworld = name == "nitre_ore"
        write(DATA, f"worldgen/configured_feature/ore_{name}.json", {"type": "abyssalport:ranged_ore", "config": {
            "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables" if overworld else "abyssalport:abyssal_ore_replaceables"},
            "state": {"Name": "abyssalport:"+name}, "min_size": config[2][0], "max_size": config[2][1]}})
        counts = config[3] if isinstance(config[3], int) else {"type": "minecraft:uniform", "min_inclusive": config[3][0], "max_inclusive": config[3][1]}
        write(DATA, f"worldgen/placed_feature/ore_{name}.json", {"feature": "abyssalport:ore_"+name, "placement": [
            {"type": "minecraft:count", "count": counts}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": config[4][0]}, "max_inclusive": {"absolute": config[4][1]}}},
            {"type": "minecraft:biome"}]})
        if not overworld: wasteland_features.append("abyssalport:ore_"+name)
    write(DATA, "neoforge/biome_modifier/ore_nitre.json", {"type": "neoforge:add_features", "biomes": "#c:is_overworld", "features": "abyssalport:ore_nitre_ore", "step": "underground_ores"})
    for biome in ["abyssal_wasteland", "coralium_lake", "abyssal_plateau"]:
        path = DATA / "worldgen/biome" / (biome+".json")
        value = json.loads(path.read_text(encoding="utf-8"))
        for feature in wasteland_features:
            if feature not in value["features"][6]: value["features"][6].append(feature)
        write(DATA, "worldgen/biome/"+biome+".json", value)


def language():
    blocks = {"nitre_ore": ["Nitre Ore", "Salpetererz"], "abyssal_nitre_ore": ["Abyssal Nitre Ore", "Abyssales Salpetererz"],
              "abyssal_coralium_ore": ["Abyssal Coralium Ore", "Abyssales Coraliumerz"], "abyssal_iron_ore": ["Abyssal Iron Ore", "Abyssales Eisenerz"],
              "abyssal_gold_ore": ["Abyssal Gold Ore", "Abyssales Golderz"], "abyssal_diamond_ore": ["Abyssal Diamond Ore", "Abyssales Diamanterz"],
              "liquified_coralium_ore": ["Liquified Coralium Ore", "Verflüssigtes Coraliumerz"], "pearlescent_coralium_ore": ["Pearlescent Coralium Ore", "Perlenschimmerndes Coraliumerz"],
              PREFIX+"_block": ["Block of Refined Coralium", "Block aus raffiniertem Coralium"]}
    suffixes = {"ingot": ["Ingot", "barren"], "nugget": ["Nugget", "klumpen"], "pickaxe": ["Pickaxe", "spitzhacke"], "axe": ["Axe", "axt"], "shovel": ["Shovel", "schaufel"],
                "sword": ["Sword", "schwert"], "hoe": ["Hoe", "hacke"], "helmet": ["Helmet", "helm"], "chestplate": ["Chestplate", "brustpanzer"], "leggings": ["Leggings", "beinschutz"], "boots": ["Boots", "stiefel"]}
    pairs = {"block.abyssalport."+name: text for name, text in blocks.items()}
    prefixes = {"pickaxe": "Raffinierte", "axe": "Raffinierte", "shovel": "Raffinierte", "sword": "Raffiniertes", "hoe": "Raffinierte", "boots": "Raffinierte"}
    pairs.update({"item.abyssalport."+PREFIX+"_"+suffix: ["Refined Coralium "+text[0], prefixes.get(suffix, "Raffinierter")+" Coralium"+text[1]] for suffix, text in suffixes.items()})
    pairs.update({"item.abyssalport.nitre": ["Nitre", "Salpeter"], "research.abyssalport.refined_coralium": ["Refined Coralium", "Raffiniertes Coralium"],
        "screen.abyssalport.page.14": ["Wasteland Minerals", "Mineralien des Ödlands"], "screen.abyssalport.locked.14": ["Enter the Abyssal Wasteland or obtain Refined Coralium to reveal this chapter.", "Betritt das Abyssale Ödland oder finde raffiniertes Coralium für dieses Kapitel."],
        "screen.abyssalport.wasteland_mining": ["Wasteland ores replace Abyssal Stone. Iron tools mine Abyssal Iron, Gold, Diamond and Nitre; Abyssal Coralium needs diamond or better and drops gems. Bring an Abyssalnite Pickaxe to mine Liquified Coralium at Y 5-34. Smelt this ore into Refined Coralium: one ingot per ore, or use a blast furnace. Craft tools and armor with ingots and the usual equipment patterns.",
            "Ödlanderze ersetzen Abyssalstein. Eisenwerkzeuge bauen Abyssales Eisen, Gold, Diamant und Salpeter ab; Abyssales Coralium benötigt Diamant oder besser und liefert Edelsteine. Baue mit einer Abyssalnit-Spitzhacke verflüssigtes Coralium bei Y 5-34 ab. Schmelze es im Ofen oder Hochofen zu je einem raffinierten Coraliumbarren. Werkzeuge und Rüstung folgen den üblichen Herstellungsmustern."],
        "screen.abyssalport.refined_coralium_note": ["Refined Coralium tools have 1,800 uses and can mine Pearlescent Coralium at Y 5-19 for 1-2 Coralium Pearls (Fortune increases drops; Silk Touch preserves ore). Abyssalnite and vanilla tools cannot harvest pearlescent ore. Refined helmets grant Water Breathing, chestplates Resistance and boots Speed only while equipped. These effects do not make Coralium Plague harmless. Ingots repair the equipment in an anvil; nine ingots or nuggets reversibly form a block or ingot.",
            "Raffinierte Coraliumwerkzeuge haben 1.800 Nutzungen und bauen perlenschimmerndes Coralium bei Y 5-19 für 1-2 Coraliumperlen ab. Glück erhöht die Ausbeute, Behutsamkeit erhält das Erz. Abyssalnit- und Vanilla-Werkzeuge können dieses Erz nicht ernten. Getragener Helm gewährt Wasseratmung, Brustpanzer Resistenz und Stiefel Schnelligkeit. Die Rüstung schützt nicht vollständig vor Coraliumpest. Barren reparieren sie im Amboss; neun Barren oder Klumpen bilden umkehrbar einen Block oder Barren."]})
    for index, locale in enumerate(["en_us", "de_de"]):
        path = ASSETS / "lang" / (locale+".json")
        values = json.loads(path.read_text(encoding="utf-8"))
        values.update({key: text[index] for key, text in pairs.items()})
        write(ASSETS, "lang/"+locale+".json", values)


if __name__ == "__main__":
    textures()
    data()
    generation()
    language()
