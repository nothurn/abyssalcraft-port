"""Independent crystal pixels and native recipes from the audited Crystallizer rules.

No original media is read. Optional absent foreign metals and later mob flesh are
not exposed as unreachable items. Modern copper/deepslate use native equivalents.
"""
import json
import random
from PIL import Image, ImageDraw, ImageColor
from generate_wasteland_minerals import ROOT, ASSETS, DATA, write, append_tag
from generate_abyssalnite_assets import texture

MATERIALS = {
    "blaze": ("#edb552", "Blaze", "Lohe"), "phosphorus": ("#bc7d58", "Phosphorus", "Phosphor"),
    "hydrogen": ("#a1c9d7", "Hydrogen", "Wasserstoff"), "oxygen": ("#7cb3d9", "Oxygen", "Sauerstoff"),
    "nitrate": ("#bbc398", "Nitrate", "Nitrat"), "sulfur": ("#d7c268", "Sulfur", "Schwefel"),
    "silica": ("#b6c9c4", "Silica", "Siliciumdioxid"), "magnesia": ("#aba6c0", "Magnesia", "Magnesiumoxid"),
    "silicon": ("#838b9a", "Silicon", "Silicium"), "magnesium": ("#b6b8c6", "Magnesium", "Magnesium"),
    "nitrogen": ("#8c98cb", "Nitrogen", "Stickstoff"), "calcium": ("#d3ccb8", "Calcium", "Calcium"),
    "beryl": ("#91c6b7", "Beryl", "Beryll"), "coralium": ("#65b9a0", "Coralium", "Coralium"),
    "abyssalnite": ("#877bb0", "Abyssalnite", "Abyssalnit"), "dreadium": ("#c06e67", "Dreadium", "Dreadium"),
    "iron": ("#aaa5a4", "Iron", "Eisen"), "gold": ("#ddba5c", "Gold", "Gold"),
    "redstone": ("#c35551", "Redstone", "Redstone"), "carbon": ("#626b78", "Carbon", "Kohlenstoff"),
    "potassium": ("#b5a0c0", "Potassium", "Kalium"), "copper": ("#c48e71", "Copper", "Kupfer"),
}


def color(base, factor):
    return tuple(max(0, min(255, round(channel*factor))) for channel in ImageColor.getrgb(base))


def media():
    for name in ["carbon_cluster", "dense_carbon_cluster", "sulfur"]:
        image = Image.new("RGBA", (16, 16)); draw = ImageDraw.Draw(image)
        if name == "sulfur":
            draw.polygon([(1, 11), (4, 7), (10, 5), (14, 10), (11, 13), (3, 13)], fill="#b9a04f", outline="#695530")
            for x, y in [(3, 10), (6, 8), (10, 7), (11, 10), (7, 11)]: draw.point((x, y), fill="#ecd784")
        else:
            dark, mid, light = ("#222633", "#596071", "#a5a1b6") if name.startswith("dense") else ("#28313a", "#68727c", "#c0c7cf")
            if name.startswith("dense"): draw.polygon([(7, 2), (13, 5), (13, 11), (7, 14), (2, 11), (2, 5)], fill=dark, outline="#121725")
            for x, y in [(3, 5), (7, 3), (10, 6), (4, 9), (8, 10)]:
                draw.polygon([(x, y+2), (x+1, y), (x+3, y+1), (x+3, y+4), (x+1, y+5)], fill=mid, outline=dark)
                draw.line((x+1, y+1, x+1, y+3), fill=light)
        texture("item/"+name+".png", image)
        write(ASSETS, "models/item/"+name+".json", {"parent": "minecraft:item/generated", "textures": {"layer0": "abyssalport:item/"+name}})
    for index, (material, (base, _, _)) in enumerate(MATERIALS.items()):
        rng = random.Random(73130+index)
        for fraction, points in [
            ("fragment", [(6, 8), (9, 6), (11, 10), (8, 12), (5, 10)]),
            ("shard", [(7, 2), (11, 5), (10, 12), (6, 14), (4, 10), (5, 5)]),
            ("crystal", [(7, 1), (12, 5), (12, 11), (8, 15), (3, 11), (3, 5)])]:
            name = material+"_crystal"+("_"+fraction if fraction != "crystal" else "")
            image = Image.new("RGBA", (16, 16)); draw = ImageDraw.Draw(image)
            draw.polygon(points, fill=color(base, .85), outline=color(base, .40))
            if fraction == "fragment":
                draw.line((6, 8, 9, 8, 10, 10), fill=color(base, 1.42))
                draw.point((8, 11), fill=color(base, 1.05))
            else:
                draw.line((7, 3, 6, 6, 6, 10), fill=color(base, 1.42))
                draw.line((7, 5, 9, 6, 9, 12), fill=color(base, 1.12))
                draw.line((5, 11, 8, 13), fill=color(base, .58))
            texture("item/"+name+".png", image)
            write(ASSETS, "models/item/"+name+".json", {"parent": "minecraft:item/generated", "textures": {"layer0": "abyssalport:item/"+name}})
        name = material+"_crystal_cluster"
        image = Image.new("RGB", (16, 16)); draw = ImageDraw.Draw(image)
        for y in range(16):
            for x in range(16): image.putpixel((x, y), color(base, rng.uniform(.40, .62)))
        for x, y, w, h in [(1, 3, 4, 6), (6, 1, 5, 8), (11, 8, 4, 6), (3, 10, 6, 5)]:
            draw.polygon([(x, y+2), (x+2, y), (x+w-1, y+2), (x+w-1, y+h-2), (x+1, y+h-1)], fill=color(base, .91), outline=color(base, .36))
            draw.line((x+1, y+2, x+1, y+h-3), fill=color(base, 1.39))
        texture("block/"+name+".png", image)
        write(ASSETS, "blockstates/"+name+".json", {"variants": {"": {"model": "abyssalport:block/"+name}}})
        write(ASSETS, "models/block/"+name+".json", {"parent": "minecraft:block/cube_all", "textures": {"all": "abyssalport:block/"+name}})
        write(ASSETS, "models/item/"+name+".json", {"parent": "abyssalport:block/"+name})
        write(DATA, "loot_table/blocks/"+name+".json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "abyssalport:"+name}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    for face in ["side", "top", "front", "front_on"]:
        image = Image.new("RGB", (16, 16), "#353038"); draw = ImageDraw.Draw(image)
        draw.rectangle((1, 1, 14, 14), outline="#6c5960")
        if face.startswith("front"):
            draw.rectangle((2, 4, 13, 12), fill="#141920", outline="#907179")
            for x in [4, 9]:
                draw.polygon([(x+1, 5), (x+3, 8), (x+2, 11), (x, 10), (x, 7)], fill="#c8def0" if face == "front_on" else "#6d829b", outline="#3b4c63")
        elif face == "top":
            draw.polygon([(8, 3), (12, 8), (8, 12), (3, 8)], outline="#afa0b1")
            draw.line((8, 4, 8, 11), fill="#877893")
        else:
            for y in [4, 8, 12]: draw.line((3, y, 12, y), fill="#4e454f")
            draw.rectangle((6, 5, 9, 10), outline="#967883")
        texture("block/crystallizer_"+face+".png", image)
    variants = {}
    for lit in [False, True]:
        name = "crystallizer_on" if lit else "crystallizer"
        write(ASSETS, "models/block/"+name+".json", {"parent": "minecraft:block/orientable", "textures": {
            "front": "abyssalport:block/crystallizer_front"+("_on" if lit else ""), "side": "abyssalport:block/crystallizer_side", "top": "abyssalport:block/crystallizer_top"}})
        for facing, angle in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
            variants[f"facing={facing},lit={str(lit).lower()}"] = {"model": "abyssalport:block/"+name, "y": angle}
    write(ASSETS, "blockstates/crystallizer.json", {"variants": variants})
    write(ASSETS, "models/item/crystallizer.json", {"parent": "abyssalport:block/crystallizer"})
    write(DATA, "loot_table/blocks/crystallizer.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "abyssalport:crystallizer"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    append_tag(ROOT / "data/minecraft", "tags/block/mineable/pickaxe.json", ["abyssalport:crystallizer"]+["abyssalport:"+m+"_crystal_cluster" for m in MATERIALS])


def output(material, fraction="crystal", count=1):
    return {"id": "abyssalport:"+material+"_crystal"+("_"+fraction if fraction != "crystal" else ""), "count": count}


def processing(name, inputs, first, second=None, xp=.1, water=False):
    if isinstance(inputs, str): inputs = [inputs]
    ingredients = [{"item": item} for item in inputs]
    value = {"type": "abyssalport:crystallization", "ingredient": ingredients[0] if len(ingredients) == 1 else ingredients,
             "result": first, "experience": xp}
    if second: value["secondary"] = second
    if water: value["water_only"] = True
    write(DATA, "recipe/crystallization/"+name+".json", value)


def data():
    write(DATA, "tags/item/crystals.json", {"replace": False, "values": ["abyssalport:"+material+"_crystal"+suffix
        for material in MATERIALS for suffix in ["_fragment", "_shard", "", "_cluster"]]})
    write(DATA, "recipe/crystallizer.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["BBB", "RFR", "BBB"],
        "key": {"B": {"item": "abyssalport:dreadstone_bricks"}, "R": {"item": "abyssalport:dreadium_block"}, "F": {"item": "minecraft:furnace"}}, "result": {"id": "abyssalport:crystallizer"}})
    write(DATA, "advancement/recipes/crystallizer.json", {"criteria": {"has_material": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": ["abyssalport:dreadium_ingot"]}]}}}, "rewards": {"recipes": ["abyssalport:crystallizer"]}})
    for material in MATERIALS:
        names = [material+"_crystal_fragment", material+"_crystal_shard", material+"_crystal", material+"_crystal_cluster"]
        for lower, upper in zip(names, names[1:]):
            write(DATA, "recipe/crystals/"+upper+".json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["###", "###", "###"],
                "key": {"#": {"item": "abyssalport:"+lower}}, "result": {"id": "abyssalport:"+upper}})
            write(DATA, "recipe/crystals/"+lower+"_from_"+upper+".json", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": "abyssalport:"+upper}],
                "result": {"id": "abyssalport:"+lower, "count": 9}})
    processing("blaze_powder", "minecraft:blaze_powder", output("blaze", "shard", 4))
    processing("bone_meal", "minecraft:bone_meal", output("phosphorus", "shard", 4), xp=0)
    processing("water_bucket", "minecraft:water_bucket", output("hydrogen", count=12), output("oxygen", "shard", 6))
    processing("water_bottle", "minecraft:potion", output("hydrogen", "shard", 6), output("oxygen", "shard", 3), water=True)
    processing("gunpowder", "minecraft:gunpowder", output("nitrate", "shard", 16), output("sulfur", "shard", 4))
    processing("stone_and_obsidian", ["minecraft:stone", "minecraft:obsidian"], output("silica", "shard", 4), output("magnesia", "shard", 4))
    for kind, target, oxygen in [("silica", "silicon", 2), ("magnesia", "magnesium", 1), ("nitrate", "nitrogen", 3)]:
        for fraction in ["crystal", "shard"]:
            processing(kind+"_"+fraction, output(kind, fraction)["id"], output(target, fraction), output("oxygen", fraction, oxygen))
    processing("rotten_flesh", "minecraft:rotten_flesh", output("phosphorus", "shard", 8))
    processing("plagued_flesh", "abyssalport:coralium_plagued_flesh", output("phosphorus", "shard", 8), output("coralium", "shard"), .2)
    processing("ghoul_flesh", "abyssalport:ghoul_flesh", output("phosphorus", "shard", 12), xp=.2)
    processing("shadow_ghoul_flesh", "abyssalport:shadow_ghoul_flesh", output("phosphorus", "shard", 12), {"id":"abyssalport:shadow_shard"}, .2)
    processing("depths_ghoul_flesh", "abyssalport:depths_ghoul_flesh", output("phosphorus", "shard", 12), output("coralium", "shard"), .2)
    processing("dreaded_ghoul_flesh", "abyssalport:dreaded_ghoul_flesh", output("phosphorus", "shard", 12), output("dreadium", "shard"), .2)
    processing("overworld_shoggoth_flesh", "abyssalport:overworld_shoggoth_flesh", output("phosphorus", "shard", 8), xp=.2)
    processing("abyssal_shoggoth_flesh", "abyssalport:abyssal_shoggoth_flesh", output("phosphorus", "shard", 8), output("coralium", "fragment", 2), .2)
    processing("dreaded_shoggoth_flesh", "abyssalport:dreaded_shoggoth_flesh", output("phosphorus", "shard", 8), output("dreadium", "fragment", 2), .2)
    processing("wasteland_skin", "abyssalport:skin_of_the_abyssal_wasteland", output("phosphorus", "shard", 8), {"id": "abyssalport:abyssal_wasteland_essence"}, .2)
    processing("dreadlands_skin", "abyssalport:skin_of_the_dreadlands", output("phosphorus", "shard", 8), {"id": "abyssalport:dreadlands_essence"}, .2)
    processing("dreaded_shard", "abyssalport:dreaded_shard", output("abyssalnite", "shard"), output("dreadium", "shard", 4), .2)
    processing("bone", "minecraft:bone", output("calcium", "shard", 4), xp=.2)
    processing("prismarine_items", ["minecraft:prismarine_shard", "minecraft:prismarine_crystals"], output("silica", "shard", 4), output("beryl", "shard", 4))
    processing("prismarine", "minecraft:prismarine", output("silica", "shard", 16), output("beryl", "shard", 27))
    processing("prismarine_bricks", "minecraft:prismarine_bricks", output("silica", count=4), output("beryl", count=4))
    processing("dark_prismarine", "minecraft:dark_prismarine", output("silica", "shard", 32), output("beryl", "shard", 32))
    processing("egg", "minecraft:egg", output("calcium", "shard", 4), output("phosphorus", "shard", 4))
    for material, ingots, ores, blocks in [
        ("coralium", ["abyssalport:refined_coralium_ingot", "abyssalport:coralium_chunk", "abyssalport:liquified_coralium_ore"], ["abyssalport:coralium_ore", "abyssalport:abyssal_coralium_ore"], ["abyssalport:refined_coralium_block"]),
        ("abyssalnite", ["abyssalport:abyssalnite_ingot"], ["abyssalport:abyssalnite_ore", "abyssalport:dreadlands_abyssalnite_ore"], ["abyssalport:abyssalnite_block"]),
        ("dreadium", ["abyssalport:dreadium_ingot"], [], ["abyssalport:dreadium_block"]),
        ("iron", ["minecraft:iron_ingot"], ["minecraft:iron_ore", "minecraft:deepslate_iron_ore", "abyssalport:abyssal_iron_ore"], ["minecraft:iron_block"]),
        ("gold", ["minecraft:gold_ingot"], ["minecraft:gold_ore", "minecraft:deepslate_gold_ore", "abyssalport:abyssal_gold_ore"], ["minecraft:gold_block"]),
        ("copper", ["minecraft:copper_ingot"], ["minecraft:copper_ore", "minecraft:deepslate_copper_ore"], ["minecraft:copper_block"]),
        ("carbon", ["minecraft:coal", "minecraft:charcoal"], ["minecraft:coal_ore", "minecraft:deepslate_coal_ore"], ["minecraft:coal_block"]),
        ("redstone", ["minecraft:redstone"], ["minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"], ["minecraft:redstone_block"])]:
        processing(material+"_materials", ingots, output(material))
        if ores: processing(material+"_ores", ores, output(material, count=2))
        processing(material+"_blocks", blocks, output(material, "cluster"), xp=.9)
    processing("dreaded_ore", "abyssalport:dreaded_abyssalnite_ore", output("abyssalnite", count=2), output("dreadium", count=2), .2)
    processing("nitre", "abyssalport:nitre", output("potassium"), output("nitrate"))
    processing("nitre_ores", ["abyssalport:nitre_ore", "abyssalport:abyssal_nitre_ore"], output("potassium", count=2), output("nitrate", count=2))
    processing("lapis", "minecraft:lapis_lazuli", output("silica", count=6), output("sulfur", count=4), .15)
    processing("lapis_ores", ["minecraft:lapis_ore", "minecraft:deepslate_lapis_ore"], output("silica", count=21), output("potassium", count=8), .15)
    processing("lapis_block", "minecraft:lapis_block", output("silica", "cluster", 6), output("sulfur", "cluster", 4), 1.)
    processing("gold_nugget", "minecraft:gold_nugget", output("gold", "shard"))
    processing("iron_nugget", "minecraft:iron_nugget", output("iron", "shard"))
    processing("coralium_gem", "abyssalport:coralium_gem", output("coralium", "shard"))
    processing("sulfur_dust", "abyssalport:sulfur", output("sulfur"))
    write(DATA, "recipe/carbon_cluster.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["###", "# #", "###"],
        "key": {"#": {"item": "abyssalport:carbon_crystal"}}, "result": {"id": "abyssalport:carbon_cluster"}})
    write(DATA, "recipe/dense_carbon_cluster.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["###", "#O#", "###"],
        "key": {"#": {"item": "abyssalport:carbon_cluster"}, "O": {"item": "minecraft:obsidian"}}, "result": {"id": "abyssalport:dense_carbon_cluster"}})
    append_tag(ROOT / "data/c", "tags/item/dusts/sulfur.json", ["abyssalport:sulfur"])
    append_tag(ROOT / "data/c", "tags/item/dusts/saltpeter.json", ["abyssalport:nitre"])
    append_tag(ROOT / "data/c", "tags/item/dusts.json", ["#c:dusts/sulfur", "#c:dusts/saltpeter"])
    write(DATA, "recipe/gunpowder_from_nitre.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["NSN", "NCN", "NNN"],
        "key": {"N": {"tag": "c:dusts/saltpeter"}, "S": {"tag": "c:dusts/sulfur"}, "C": {"item": "minecraft:charcoal"}}, "result": {"id": "minecraft:gunpowder", "count": 4}})
    reverse = [("dense_carbon_cluster", "minecraft:diamond", .5)]
    reverse += [(material+"_crystal_cluster", result, .2) for material, result in [
        ("abyssalnite", "abyssalport:abyssalnite_ingot"), ("coralium", "abyssalport:refined_coralium_ingot"), ("dreadium", "abyssalport:dreadium_ingot"),
        ("iron", "minecraft:iron_ingot"), ("gold", "minecraft:gold_ingot"), ("sulfur", "abyssalport:sulfur"), ("blaze", "minecraft:blaze_powder"), ("redstone", "minecraft:redstone")]]
    reverse += [(material+"_crystal", result, xp) for material, result, xp in [
        ("abyssalnite", "abyssalport:abyssalnite_nugget", .1), ("coralium", "abyssalport:refined_coralium_nugget", .1),
        ("dreadium", "abyssalport:dreadium_nugget", .1), ("iron", "minecraft:iron_nugget", .2), ("gold", "minecraft:gold_nugget", .2)]]
    for source, result, xp in reverse:
        write(DATA, "recipe/transmutation/"+source+"_recovery.json", {"type": "abyssalport:transmutation", "ingredient": {"item": "abyssalport:"+source},
            "result": {"id": result, "count": 1}, "experience": xp})
    write(DATA, "advancement/recipes/crystal_processing.json", {"criteria": {"has_crystal": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": ["abyssalport:carbon_crystal"]}]}}},
        "rewards": {"recipes": ["abyssalport:carbon_cluster", "abyssalport:dense_carbon_cluster"]}})
    write(DATA, "advancement/recipes/nitre_gunpowder.json", {"criteria": {"has_nitre": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": ["abyssalport:nitre"]}]}}},
        "rewards": {"recipes": ["abyssalport:gunpowder_from_nitre"]}})


def language():
    pairs = {
        "block.abyssalport.crystallizer": ["Crystallizer", "Kristallisator"],
        "screen.abyssalport.machine.outputs": ["Outputs", "Ausgaben"],
        "item.abyssalport.carbon_cluster": ["Carbon Cluster", "Kohlenstoffcluster"],
        "item.abyssalport.dense_carbon_cluster": ["Dense Carbon Cluster", "Dichter Kohlenstoffcluster"],
        "item.abyssalport.sulfur": ["Sulfur", "Schwefel"],
        "research.abyssalport.crystallization": ["Crystallization", "Kristallisation"],
        "screen.abyssalport.page.24": ["Crystals and separation", "Kristalle und Trennung"],
        "screen.abyssalport.crystallizer": ["Build the Crystallizer from six Dreadstone Bricks, two Dreadium Blocks and a furnace. An operation takes ten seconds and may produce two different materials. Both outputs need enough space: if either is blocked, no fresh fuel is burned and the input remains intact. Stone separates into silica and magnesia shards; water provides hydrogen and oxygen. Only plain water bottles work, not other potions. Empty buckets and bottles remain in the input slot.", "Baue den Kristallisator aus sechs Dreadstone-Ziegeln, zwei Dreadiumblöcken und einem Ofen. Eine Verarbeitung dauert zehn Sekunden und kann zwei verschiedene Materialien liefern. Beide Ausgaben brauchen ausreichend Platz: Ist eine blockiert, wird kein neuer Brennstoff angezündet und die Eingabe bleibt erhalten. Stein ergibt Siliciumdioxid- und Magnesiumoxid-Splitter, Wasser liefert Wasserstoff und Sauerstoff. Nur reine Wasserflaschen funktionieren, keine anderen Tränke. Leere Eimer und Flaschen bleiben im Eingabeslot."],
        "screen.abyssalport.crystallizer_fractions": ["Nine fragments make a shard; nine shards make a crystal; nine crystals make a cluster block. Every step is reversible without loss. Nitre separates into potassium and nitrate, lapis into silica and sulfur, and Dreaded Abyssalnite Ore produces both Abyssalnite and Dreadium crystals. Crystal chemistry is available for silica, magnesia and nitrate. These processing recipes are separate from furnace recipes.", "Neun Fragmente ergeben einen Splitter, neun Splitter einen Kristall und neun Kristalle einen Clusterblock. Jede Stufe lässt sich verlustfrei umkehren. Nitre trennt sich in Kalium und Nitrat, Lapislazuli in Siliciumdioxid und Schwefel. Dreaded-Abyssalnit-Erz liefert sowohl Abyssalnit- als auch Dreadiumkristalle. Siliciumdioxid, Magnesiumoxid und Nitrat können weiter zerlegt werden. Diese Verarbeitung ist von normalen Ofenrezepten getrennt."],
        "screen.abyssalport.crystallizer_fuel": ["Coal is an ingredient, not direct fuel. Use Dread Fragments, Dreaded Shards, Blaze Powder or Rods, or processed Carbon Crystals and cluster blocks. A Carbon Crystal powers one operation, a cluster nine. Feed ingredients from above and fuel from a side. Extract outputs and returned buckets/bottles from below. World reloads preserve inventory, fuel and progress; changing the recipe resets progress. Taking either output pays saved experience once.", "Kohle ist eine Zutat, kein direkter Brennstoff. Nutze Dread-Fragmente, Dreaded-Splitter, Lohenstaub oder -ruten sowie verarbeitete Kohlenstoffkristalle und Clusterblöcke. Ein Kohlenstoffkristall reicht für eine Verarbeitung, ein Cluster für neun. Zutaten kommen von oben, Brennstoff von der Seite. Ausgaben und leere Eimer/Flaschen können unten entnommen werden. Inventar, Brennstoff und Fortschritt überstehen Weltneustarts; ein anderes Rezept setzt den Fortschritt zurück. Die Entnahme einer beliebigen Ausgabe zahlt gespeicherte Erfahrung genau einmal aus."]}
    pairs["screen.abyssalport.crystal_processing"] = [
        "Eight Carbon Crystals around an empty center form a Carbon Cluster. Eight clusters around Obsidian form a Dense Carbon Cluster: transmute it into one diamond. Their Crystallizer fuel values are eight and 64 operations. A Sulfur Crystal Cluster transmutes into one Sulfur dust. Combine seven Nitre dust, Sulfur and Charcoal in the recorded shaped recipe for four gunpowder. Recovery in the Transmutator is intentionally lossy: nine-crystal metal clusters return only one ingot, and a single metal crystal returns one nugget. Crystal fraction crafting itself remains lossless.",
        "Acht Kohlenstoffkristalle um eine leere Mitte ergeben einen Kohlenstoffcluster. Acht Cluster um Obsidian ergeben einen dichten Kohlenstoffcluster, den der Transmutator zu einem Diamanten verarbeitet. Im Kristallisator liefern sie Brennstoff für acht beziehungsweise 64 Vorgänge. Ein Schwefel-Kristallcluster wird im Transmutator zu einem Schwefelstaub. Sieben Nitre, Schwefel und Holzkohle ergeben im festgelegten Craftingmuster vier Schwarzpulver. Die Rückgewinnung im Transmutator ist bewusst verlustbehaftet: Ein Metallcluster aus neun Kristallen liefert nur einen Barren, ein einzelner Metallkristall nur einen Klumpen. Das reine Craften der Kristallfraktionen bleibt verlustfrei."]
    for material, (_, english, german) in MATERIALS.items():
        pairs["item.abyssalport."+material+"_crystal_fragment"] = [english+" Crystal Fragment", german+"-Kristallfragment"]
        pairs["item.abyssalport."+material+"_crystal_shard"] = [english+" Crystal Shard", german+"-Kristallsplitter"]
        pairs["item.abyssalport."+material+"_crystal"] = [english+" Crystal", german+"kristall"]
        pairs["block.abyssalport."+material+"_crystal_cluster"] = [english+" Crystal Cluster", german+"-Kristallcluster"]
    for index, locale in enumerate(["en_us", "de_de"]):
        value = json.loads((ASSETS / "lang" / (locale+".json")).read_text(encoding="utf-8"))
        value.update({key: pair[index] for key, pair in pairs.items()}); write(ASSETS, "lang/"+locale+".json", value)


if __name__ == "__main__":
    media(); data(); language()
