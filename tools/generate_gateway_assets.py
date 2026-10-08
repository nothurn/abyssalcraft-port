"""Independent gateway / Wasteland art and native 1.21 datapack definitions.

No upstream textures or models are read. Terrain uses native noise codecs, not a
claimed bit-for-bit copy of the 1.12 generator. Full Wasteland structures / ecology
remain on the development checklist.
"""
import json
import math
import random
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = ROOT / "assets/abyssalport"
DATA = ROOT / "data/abyssalport"


def write(base, relative, value):
    path = base / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def save(relative, image):
    path = ASSETS / "textures" / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def paint():
    for index, name in enumerate(["abyssal_stone", "abyssal_cobblestone", "abyssal_sand", "fused_abyssal_sand", "monolith_pillar", "gateway_anchor"]):
        if name == "monolith_pillar": continue  # Owned by the shared PE range-pillar generator.
        rng = random.Random(845+index)
        image = Image.new("RGB", (16, 16))
        for y in range(16):
            for x in range(16):
                shade = rng.randrange(-8, 9)
                base = [50, 54, 51] if "sand" not in name else [85, 113, 101]
                image.putpixel((x, y), tuple(n+shade for n in base))
        draw = ImageDraw.Draw(image)
        if "cobble" in name:
            for y in range(3, 16, 5): draw.line((0, y, 15, y), fill="#242e2a")
            for x in range(3, 16, 5): draw.line((x, 0, x, 15), fill="#242e2a")
        if "pillar" in name:
            for x in (2, 5, 10, 13): draw.line((x, 0, x, 15), fill="#26332f")
            draw.line((0, 1, 15, 1), fill="#657469")
            draw.line((0, 14, 15, 14), fill="#26332f")
        if "anchor" in name:
            draw.rectangle((1, 1, 14, 14), outline="#192c29")
            draw.rectangle((3, 3, 12, 12), outline="#748b77")
            draw.line((7, 3, 7, 12), fill="#3d796a")
            draw.line((3, 8, 12, 8), fill="#3d796a")
        save("block/"+name+".png", image)
    for name, size in [("gateway_portal", 16), ("coralium_still", 16), ("coralium_flow", 32)]:
        image = Image.new("RGBA", (size, size*8))
        for frame in range(8):
            for y in range(size):
                for x in range(size):
                    wave = math.sin((x+y+frame*2)*math.pi/8)*math.cos((x-y-frame)*math.pi/8)
                    n = round(13*wave)
                    image.putpixel((x, y+frame*size), (28+n, 100+n, 87+n, 190 if name == "gateway_portal" else 255))
        save("block/"+name+".png", image)
        write(ASSETS, "textures/block/"+name+".png.mcmeta", {"animation": {"frametime": 4, "interpolate": False}})
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    draw.polygon([(10, 1), (14, 4), (11, 8), (8, 6)], fill="#384b42")
    draw.polygon([(10, 2), (12, 4), (11, 6), (9, 5)], fill="#65b89e")
    draw.line((8, 6, 3, 12), fill="#ba934e", width=3)
    draw.line((8, 6, 3, 12), fill="#dfbd6d")
    draw.line((3, 12, 5, 14), fill="#8c6a35", width=2)
    draw.point((2, 12), fill="#e7d292")
    save("item/gateway_key.png", image)
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    draw.polygon([(2, 4), (13, 4), (11, 13), (4, 13)], fill="#47585b")
    draw.line((2, 4, 13, 4), fill="#d7dee0", width=2)
    draw.polygon([(4, 6), (11, 6), (10, 11), (5, 11)], fill="#269587")
    draw.line((5, 6, 10, 6), fill="#89d2b7")
    save("item/coralium_bucket.png", image)
    image = Image.new("RGBA", (18, 18))
    draw = ImageDraw.Draw(image)
    draw.ellipse((1, 1, 16, 16), fill="#193c37", outline="#68b299")
    draw.line((5, 5, 12, 12), fill="#8ee0b9", width=2)
    draw.line((5, 12, 12, 5), fill="#8ee0b9", width=2)
    save("mob_effect/coralium_plague.png", image)


def resources():
    from generate_statue_energy import generate_pillar
    generate_pillar()
    for name in ["abyssal_stone", "abyssal_cobblestone", "abyssal_sand", "fused_abyssal_sand"]:
        write(ASSETS, f"blockstates/{name}.json", {"variants": {"": {"model": "abyssalport:block/"+name}}})
        write(ASSETS, f"models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "abyssalport:block/"+name}})
        write(ASSETS, f"models/item/{name}.json", {"parent": "abyssalport:block/"+name})
        write(DATA, f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": "abyssalport:"+name}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(DATA, "loot_table/blocks/abyssal_stone.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": "abyssalport:abyssal_stone", "conditions": [{"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
        {"type": "minecraft:item", "name": "abyssalport:abyssal_cobblestone", "conditions": [{"condition": "minecraft:survives_explosion"}]}]}]}]})
    write(DATA, "recipe/abyssal_stone.json", {"type": "minecraft:smelting", "category": "blocks", "ingredient": {"item": "abyssalport:abyssal_cobblestone"},
          "result": {"id": "abyssalport:abyssal_stone"}, "experience": 0.1, "cookingtime": 200})
    write(ASSETS, "blockstates/gateway_anchor.json", {"variants": {"active=false": {"model": "abyssalport:block/gateway_anchor"}, "active=true": {"model": "abyssalport:block/gateway_anchor"}}})
    write(ASSETS, "models/block/gateway_anchor.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "abyssalport:block/gateway_anchor"}})
    write(ASSETS, "models/item/gateway_anchor.json", {"parent": "abyssalport:block/gateway_anchor"})
    write(DATA, "loot_table/blocks/gateway_anchor.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "abyssalport:gateway_anchor"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(ASSETS, "blockstates/gateway_portal.json", {"variants": {"axis=x": {"model": "abyssalport:block/gateway_portal"}, "axis=z": {"model": "abyssalport:block/gateway_portal", "y": 90}}})
    faces = {direction: {"texture": "#portal", "uv": [0, 0, 16, 16]} for direction in ["north", "south", "east", "west", "up", "down"]}
    write(ASSETS, "models/block/gateway_portal.json", {"render_type": "minecraft:translucent", "ambientocclusion": False,
          "textures": {"portal": "abyssalport:block/gateway_portal", "particle": "#portal"}, "elements": [{"from": [0, 0, 6], "to": [16, 16, 10], "faces": faces}]})
    write(ASSETS, "blockstates/liquid_coralium.json", {"variants": {"": {"model": "abyssalport:block/liquid_coralium"}}})
    write(ASSETS, "models/block/liquid_coralium.json", {"textures": {"particle": "abyssalport:block/coralium_still"}})
    write(ASSETS, "models/item/coralium_bucket.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "abyssalport:item/coralium_bucket"}})
    write(ASSETS, "models/item/gateway_key.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": "abyssalport:item/gateway_key"}})
    write(ROOT / "assets/minecraft", "atlases/blocks.json", {"sources": [{"type": "minecraft:single", "resource": "abyssalport:block/"+name}
          for name in ["coralium_still", "coralium_flow"]]})
    write(DATA, "recipe/gateway_key.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" PC", " BP", "B  "],
          "key": {"P": {"item": "abyssalport:coralium_pearl"}, "C": {"item": "abyssalport:oblivion_catalyst"}, "B": {"item": "minecraft:blaze_rod"}}, "result": {"id": "abyssalport:gateway_key", "count": 1}})
    write(DATA, "advancement/recipes/gateway_key.json", {"criteria": {"catalyst": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "abyssalport:oblivion_catalyst"}]}}},
          "requirements": [["catalyst"]], "rewards": {"recipes": ["abyssalport:gateway_key"]}})
    write(DATA, "rituals/gateway.json", {"energy_cost": 1000, "duration_ticks": 40, "altar_input": "abyssalport:gateway_key", "outcome": "gateway",
          "offerings": ["abyssalport:shadow_gem"]*4,
          "offering_layout": ["abyssalport:shadow_gem"]*4 + [None]*4,
          "result": {"item": "abyssalport:gateway_key", "count": 1}})
    floor_path = DATA / "tags/block/gateway_natural_floor.json"
    floor_tag = json.loads(floor_path.read_text(encoding="utf-8")) if floor_path.exists() else {"replace": False, "values": []}
    for name in ["minecraft:stone", "minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt",
                 "minecraft:podzol", "minecraft:deepslate", "minecraft:granite", "minecraft:andesite", "minecraft:diorite", "abyssalport:abyssal_stone", "abyssalport:abyssal_sand", "abyssalport:fused_abyssal_sand"]:
        if name not in floor_tag["values"]: floor_tag["values"].append(name)
    write(DATA, "tags/block/gateway_natural_floor.json", floor_tag)
    immune_path = DATA / "tags/entity_type/coralium_immune.json"
    if not immune_path.exists():
        write(DATA, "tags/entity_type/coralium_immune.json", {"replace": False, "values": []})
    write(DATA, "tags/fluid/coralium.json", {"replace": False, "values": ["abyssalport:coralium", "abyssalport:flowing_coralium"]})
    for tag, items in [("mineable/pickaxe", ["abyssal_stone", "abyssal_cobblestone", "monolith_pillar", "gateway_anchor"]),
                       ("mineable/shovel", ["abyssal_sand", "fused_abyssal_sand"]),
                       ("overworld_carver_replaceables", ["abyssal_stone", "abyssal_sand", "fused_abyssal_sand"])]:
        path = ROOT / "data/minecraft/tags/block" / (tag+".json")
        existing = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
        for item in items:
            if "abyssalport:"+item not in existing["values"]: existing["values"].append("abyssalport:"+item)
        write(ROOT / "data/minecraft/tags/block", tag+".json", existing)
    write(DATA, "dimension_type/abyssal_wasteland.json", {"ultrawarm": False, "natural": False, "piglin_safe": False, "respawn_anchor_works": False, "bed_works": False,
          "has_raids": False, "has_skylight": False, "has_ceiling": False, "coordinate_scale": 1.0, "ambient_light": 0.25, "fixed_time": 18000,
          "logical_height": 256, "min_y": 0, "height": 256, "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:the_end",
          "monster_spawn_light_level": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 7}, "monster_spawn_block_light_limit": 0})
    biomes = {"abyssal_wasteland": (-0.35, 0.2), "coralium_lake": (-1.0, -0.35), "abyssal_plateau": (0.2, 1.0)}
    for name in biomes:
        path = DATA / "worldgen/biome" / (name+".json")
        existing = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
        write(DATA, "worldgen/biome/"+name+".json", {"has_precipitation": False, "temperature": 0.8, "downfall": 0,
              "effects": {"fog_color": 1718833, "sky_color": 2247482, "water_color": 2725263, "water_fog_color": 993847,
                          "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2}},
              "carvers": {"air": ["minecraft:cave", "minecraft:canyon"]}, "features": existing.get("features", [[] for _ in range(11)]), "spawn_costs": existing.get("spawn_costs", {}),
              "spawners": existing.get("spawners", {"monster": [{"type": "minecraft:zombie", "weight": 50, "minCount": 1, "maxCount": 5}, {"type": "minecraft:skeleton", "weight": 50, "minCount": 1, "maxCount": 5}]})})
    biome_parameters = [{"biome": "abyssalport:"+name, "parameters": {"temperature": [-1, 1], "humidity": [-1, 1], "continentalness": list(band), "erosion": [-1, 1],
                        "weirdness": [-1, 1], "depth": 0, "offset": 0}} for name, band in biomes.items()]
    write(DATA, "dimension/abyssal_wasteland.json", {"type": "abyssalport:abyssal_wasteland", "generator": {"type": "minecraft:noise", "settings": "abyssalport:abyssal_wasteland",
          "biome_source": {"type": "minecraft:multi_noise", "biomes": biome_parameters}}})
    write(DATA, "worldgen/noise/wasteland_terrain.json", {"firstOctave": -5, "amplitudes": [1.0, 0.5, 0.25]})
    density = {"type": "minecraft:add", "argument1": {"type": "minecraft:y_clamped_gradient", "from_y": 0, "to_y": 128, "from_value": 0.8, "to_value": -0.8},
               "argument2": {"type": "minecraft:mul", "argument1": 0.6, "argument2": {"type": "minecraft:noise", "noise": "abyssalport:wasteland_terrain", "xz_scale": 1.0, "y_scale": 0.0}}}
    router = {key: 0.0 for key in ["barrier", "fluid_level_floodedness", "fluid_level_spread", "lava", "temperature", "vegetation", "continents", "erosion", "depth", "ridges", "vein_toggle", "vein_ridged", "vein_gap"]}
    router.update({"final_density": {"type": "minecraft:interpolated", "argument": density}, "initial_density_without_jaggedness": density,
                   "continents": {"type": "minecraft:noise", "noise": "abyssalport:wasteland_terrain", "xz_scale": 1.0, "y_scale": 0.0}})
    def block(name): return {"type": "minecraft:block", "result_state": {"Name": name}}
    def condition(check, result): return {"type": "minecraft:condition", "if_true": check, "then_run": result}
    rules = {"type": "minecraft:sequence", "sequence": [
        condition({"type": "minecraft:vertical_gradient", "random_name": "abyssalport:bedrock", "true_at_and_below": {"above_bottom": 0}, "false_at_and_above": {"above_bottom": 5}}, block("minecraft:bedrock")),
        condition({"type": "minecraft:biome", "biome_is": ["abyssalport:abyssal_plateau"]},
                  condition({"type": "minecraft:stone_depth", "surface_type": "floor", "offset": 3, "add_surface_depth": False, "secondary_depth_range": 0}, block("abyssalport:coralium_stone"))),
        condition({"type": "minecraft:stone_depth", "surface_type": "floor", "offset": 0, "add_surface_depth": False, "secondary_depth_range": 0}, block("abyssalport:fused_abyssal_sand")),
        condition({"type": "minecraft:stone_depth", "surface_type": "floor", "offset": 3, "add_surface_depth": False, "secondary_depth_range": 0}, block("abyssalport:abyssal_sand"))]}
    write(DATA, "worldgen/noise_settings/abyssal_wasteland.json", {"sea_level": 55, "disable_mob_generation": False, "aquifers_enabled": False, "ore_veins_enabled": False, "legacy_random_source": False,
          "default_block": {"Name": "abyssalport:abyssal_stone"}, "default_fluid": {"Name": "abyssalport:liquid_coralium", "Properties": {"level": "0"}},
          "noise": {"min_y": 0, "height": 256, "size_horizontal": 1, "size_vertical": 2}, "noise_router": router, "spawn_target": [], "surface_rule": rules})


def language():
    pairs = {"dimension.abyssalport.abyssal_wasteland": ["Abyssal Wasteland", "Abyssales Ödland"], "dimension.minecraft.overworld": ["Overworld", "Oberwelt"],
             "research.abyssalport.gateway": ["Gateway Key", "Portalschlüssel"], "research.abyssalport.wasteland": ["Abyssal Wasteland", "Abyssales Ödland"],
             "screen.abyssalport.page.12": ["Beyond the Overworld", "Jenseits der Oberwelt"], "screen.abyssalport.page.13": ["The Abyssal Wasteland", "Das Abyssale Ödland"],
             "screen.abyssalport.locked.12": ["First create an Oblivion Catalyst to learn about gateways.", "Erzeuge zuerst einen Vergessenheitskatalysator, um Portale zu erforschen."],
             "screen.abyssalport.locked.13": ["Visit the Abyssal Wasteland to reveal these notes.", "Betritt das Abyssale Ödland, um diese Aufzeichnungen freizuschalten."],
             "screen.abyssalport.gateway": ["Craft a Gateway Key with two Coralium Pearls, two Blaze Rods and an Oblivion Catalyst. Right-click air until its destination is Abyssal Wasteland. Put the key on a formed ritual altar. Place one Shadow Gem on each cardinal pedestal (north, east, south, west); leave the four diagonals empty. Activate with a Necronomicon holding at least 1,000 PE.", "Fertige den Portalschlüssel aus zwei Coraliumperlen, zwei Lohenruten und einem Vergessenheitskatalysator. Wähle per Rechtsklick in die Luft das Abyssale Ödland. Lege den Schlüssel auf einen geformten Ritualaltar. Je ein Schattenedelstein gehört auf die vier Podeste in den Himmelsrichtungen; die Diagonalen bleiben leer. Aktiviere mit einem Necronomicon mit mindestens 1.000 PE."],
             "screen.abyssalport.gateway_note": ["Keep two blocks above the altar clear and remain nearby for two seconds. The altar becomes a gateway anchor, all eight pedestals become pillars, and your selected key is returned. Step into the portal: a linked return gateway is created at a safe destination. Keep both anchors intact. An unknown dimension or obstructed ritual cancels without consuming ingredients or PE.", "Halte zwei Blöcke über dem Altar frei und bleibe zwei Sekunden in der Nähe. Der Altar wird zum Portalanker, alle acht Podeste werden zu Säulen. Dein ausgewählter Schlüssel bleibt erhalten. Beim Betreten entsteht ein verbundenes Rückportal an einem sicheren Ort. Erhalte beide Anker. Unbekannte Dimensionen oder blockierte Rituale verbrauchen weder Zutaten noch PE."],
             "screen.abyssalport.wasteland": ["An eternally dark wasteland of Abyssal Stone, fused sand and Coralium lakes. The toxic liquid causes Coralium Plague: avoid direct contact, and use solid paths or a bucket with care. Milk does not cure this disease. This world does not support beds or respawn anchors; do not try to sleep here.", "Ein dauerhaft dunkles Ödland aus Abyssalstein, verschmolzenem Sand und Coraliumseen. Die giftige Flüssigkeit verursacht Coraliumpest: Meide direkten Kontakt und nutze feste Wege oder Eimer vorsichtig. Milch heilt diese Krankheit nicht. Betten und Respawnanker funktionieren hier nicht; versuche nicht, hier zu schlafen."],
             "screen.abyssalport.wasteland_note": ["Bring food, spare blocks, tools and your key. Record your return gateway coordinates before exploring. Entering that linked portal returns you to its original anchor, including after a world reload. If its landing becomes blocked, travel searches for another safe site without clearing buildings. Take the key back to the Overworld to form another gateway when needed.", "Nimm Nahrung, Baublöcke, Werkzeuge und deinen Schlüssel mit. Notiere vor der Erkundung die Rückportal-Koordinaten. Das verbundene Portal führt zum ursprünglichen Anker, auch nach Weltneustart. Bei blockiertem Ziel wird ein anderer sicherer Ort gesucht, ohne Gebäude abzuräumen. Mit dem Schlüssel kannst du in der Oberwelt ein weiteres Portal formen."],
             "biome.abyssalport.abyssal_wasteland": ["Abyssal Wasteland", "Abyssales Ödland"], "biome.abyssalport.coralium_lake": ["Coralium Lake", "Coraliumsee"],
             "block.abyssalport.abyssal_stone": ["Abyssal Stone", "Abyssalstein"], "block.abyssalport.abyssal_cobblestone": ["Abyssal Cobblestone", "Abyssal-Bruchstein"],
             "block.abyssalport.abyssal_sand": ["Abyssal Sand", "Abyssalsand"], "block.abyssalport.fused_abyssal_sand": ["Fused Abyssal Sand", "Verschmolzener Abyssalsand"],
             "block.abyssalport.monolith_pillar": ["Monolith Pillar", "Monolithsäule"], "block.abyssalport.gateway_anchor": ["Gateway Anchor", "Portalanker"],
             "block.abyssalport.gateway_portal": ["Gateway", "Portal"], "block.abyssalport.liquid_coralium": ["Liquid Coralium", "Flüssiges Coralium"],
             "item.abyssalport.gateway_key": ["Gateway Key", "Portalschlüssel"], "item.abyssalport.coralium_bucket": ["Coralium Bucket", "Coraliumeimer"],
             "fluid_type.abyssalport.coralium": ["Liquid Coralium", "Flüssiges Coralium"], "effect.abyssalport.coralium_plague": ["Coralium Plague", "Coraliumpest"],
             "message.abyssalport.gateway_unknown": ["Unknown destination", "Unbekanntes Ziel"], "message.abyssalport.gateway_destination": ["Gateway destination: %s", "Portalziel: %s"],
             "message.abyssalport.gateway_created": ["Gateway formed. Your key has been returned.", "Portal geformt. Dein Schlüssel wurde zurückgegeben."],
             "tooltip.abyssalport.gateway_destination": ["Destination: %s", "Ziel: %s"], "tooltip.abyssalport.gateway_key": ["Right-click air to select a destination. Place on the altar for the 1,000-PE portal ritual.", "Rechtsklick in die Luft wählt das Ziel. Für das 1.000-PE-Portalritual auf den Altar legen."]}
    for index, locale in enumerate(["en_us", "de_de"]):
        path = ASSETS / "lang" / (locale+".json")
        values = json.loads(path.read_text(encoding="utf-8"))
        values.update({key: text[index] for key, text in pairs.items()})
        write(ASSETS, "lang/"+locale+".json", values)


if __name__ == "__main__":
    paint()
    resources()
    language()
