"""Own entry-stage Dreadlands media and native data, informed by Java mechanics.

No upstream media is read. Native noise replaces the old 1.12 generator; full
forest/ocean ecology, underworld ruins and late progression remain separate work.
"""
import json
import random
from PIL import Image, ImageDraw
from generate_wasteland_minerals import ROOT, ASSETS, DATA, write, append_tag, loot
from generate_abyssalnite_assets import texture

ID = "abyssalport:"
BLOCKS = ["dreadstone", "dreadstone_cobblestone", "elysian_stone", "elysian_cobblestone",
          "dreaded_abyssalnite_ore", "dreadlands_abyssalnite_ore"]


def media():
    for index, name in enumerate(BLOCKS):
        image = Image.new("RGB", (16, 16)); rng = random.Random(24590 + index)
        base = (88, 42, 48) if not name.startswith("elysian") else (75, 101, 119)
        for y in range(16):
            for x in range(16):
                shade = rng.randint(-8, 8)
                image.putpixel((x, y), tuple(n+shade for n in base))
        draw = ImageDraw.Draw(image)
        if "cobblestone" in name:
            for y in (3, 8, 13):
                draw.line((0, y, 15, y), fill=tuple(max(0, n-23) for n in base))
            for x in (3, 9, 14):
                draw.line((x, 0, x, 15), fill=tuple(max(0, n-17) for n in base))
        if "ore" in name:
            colors = ("#392a47", "#927092", "#c9a5be") if name.startswith("dreaded") else ("#461721", "#bb414b", "#ea8f86")
            for x, y in ((3, 3), (10, 4), (6, 9), (12, 12), (2, 13)):
                draw.polygon([(x-1, y), (x, y-1), (x+2, y), (x+1, y+2)], fill=colors[0])
                draw.line((x, y, x+1, y+1), fill=colors[1]); draw.point((x, y), fill=colors[2])
        texture("block/"+name+".png", image)
    image = Image.new("RGBA", (16, 16)); draw = ImageDraw.Draw(image)
    draw.polygon([(10, 1), (14, 4), (11, 8), (8, 6)], fill="#3e2229", outline="#ab6559")
    draw.polygon([(10, 2), (12, 4), (11, 6), (9, 5)], fill="#d24e50")
    draw.line((8, 6, 3, 12), fill="#8a685b", width=3); draw.line((8, 6, 3, 12), fill="#ccb095")
    draw.line((3, 12, 5, 14), fill="#664635", width=2); draw.point((2, 12), fill="#d8c5a2")
    texture("item/dreadlands_infused_gateway_key.png", image)
    image = Image.new("RGBA", (16, 16)); draw = ImageDraw.Draw(image)
    draw.polygon([(3, 4), (9, 2), (13, 6), (10, 12), (5, 14), (2, 9)], fill="#692731", outline="#301c27")
    draw.line((4, 5, 8, 4), fill="#c2796b", width=2); draw.line((6, 10, 10, 7), fill="#a64850")
    texture("item/dread_fragment.png", image)
    image = Image.new("RGBA", (18, 18)); draw = ImageDraw.Draw(image)
    draw.ellipse((1, 1, 16, 16), fill="#4a1825", outline="#cb6e69")
    draw.line((5, 5, 12, 12), fill="#eab18c", width=2); draw.line((5, 12, 12, 5), fill="#eab18c", width=2)
    texture("mob_effect/dread_plague.png", image)
    image = Image.new("RGBA", (64, 64)); rng = random.Random(92211)
    # Exact UV nets of the own model; all six faces stay within the atlas.
    def box(u, v, w, h, d, color):
        for x, y, width, height in [(u+d, v, w, d), (u+d+w, v, w, d), (u, v+d, d, h),
                                    (u+d, v+d, w, h), (u+d+w, v+d, d, h), (u+2*d+w, v+d, w, h)]:
            for py in range(y, y+height):
                for px in range(x, x+width):
                    shade = rng.randint(-7, 7)
                    image.putpixel((px, py), tuple(max(0, n+shade) for n in color)+(255,))
    box(0, 0, 8, 8, 8, (83, 42, 49)); box(0, 20, 8, 9, 4, (58, 31, 40))
    box(32, 20, 3, 7, 4, (87, 43, 49)); box(48, 20, 3, 11, 4, (79, 38, 44))
    box(0, 40, 4, 4, 4, (60, 31, 40)); box(20, 40, 4, 3, 4, (80, 40, 44))
    draw = ImageDraw.Draw(image)
    draw.rectangle((9, 10, 10, 11), fill="#d2b086"); draw.rectangle((13, 10, 14, 11), fill="#d2b086")
    draw.line((10, 13, 13, 13), fill="#211c25")
    draw.line((7, 26, 11, 26), fill="#af7666"); draw.line((7, 28, 11, 28), fill="#8e5654")
    texture("entity/dreadling.png", image)


def resources():
    for name in BLOCKS:
        write(ASSETS, "blockstates/"+name+".json", {"variants": {"": {"model": ID+"block/"+name}}})
        write(ASSETS, "models/block/"+name+".json", {"parent": "minecraft:block/cube_all", "textures": {"all": ID+"block/"+name}})
        write(ASSETS, "models/item/"+name+".json", {"parent": ID+"block/"+name})
        if name in ("dreadstone", "elysian_stone"):
            entry = {"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": ID+name, "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                    "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]},
                {"type": "minecraft:item", "name": ID+("dreadstone_cobblestone" if name == "dreadstone" else "elysian_cobblestone"),
                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]}
            write(DATA, "loot_table/blocks/"+name+".json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [entry]}]})
        else:
            loot(name, None, (1, 1))
    for name in ("dread_fragment", "dreadlands_infused_gateway_key"):
        write(ASSETS, "models/item/"+name+".json", {"parent": "minecraft:item/handheld" if name.endswith("key") else "minecraft:item/generated",
              "textures": {"layer0": ID+"item/"+name}})
    write(ASSETS, "models/item/dreadling_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    write(DATA, "loot_table/entities/dreadling.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": ID+"dread_fragment", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}},
            {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]}]})
    for input_name, output, xp in [("dreadstone_cobblestone", "dreadstone", .1), ("elysian_cobblestone", "elysian_stone", .1),
                                   ("dreaded_abyssalnite_ore", "abyssalnite_ingot", 3.0)]:
        write(DATA, "recipe/"+input_name+"_smelting.json", {"type": "minecraft:smelting", "category": "blocks" if "cobble" in input_name else "misc",
              "ingredient": {"item": ID+input_name}, "result": {"id": ID+output}, "experience": xp, "cookingtime": 200})
    write(DATA, "rituals/dreadlands_key.json", {"energy_cost": 10000, "duration_ticks": 200,
        "altar_input": ID+"gateway_key", "book_tier": 1, "altar_tier": 1,
        "dimension": ID+"abyssal_wasteland", "offerings": [ID+"transmutation_gem", ID+"dreadlands_infused_powerstone", ID+"eye_of_the_abyss"],
        "result": {"item": ID+"dreadlands_infused_gateway_key", "count": 1}})
    write(DATA, "rituals/dreadlands_gateway.json", {"energy_cost": 1000, "duration_ticks": 40, "altar_input": ID+"dreadlands_infused_gateway_key",
        "outcome": "gateway", "offerings": [ID+"shadow_gem"]*4, "offering_layout": [ID+"shadow_gem"]*4+[None]*4,
        "result": {"item": ID+"dreadlands_infused_gateway_key", "count": 1}})
    values = [ID+n for n in BLOCKS]
    append_tag(ROOT/"data/minecraft", "tags/block/mineable/pickaxe.json", values)
    append_tag(ROOT/"data/minecraft", "tags/block/overworld_carver_replaceables.json", values)
    for tier in ("wooden", "stone", "iron", "diamond", "netherite"):
        append_tag(ROOT/"data/minecraft", "tags/block/incorrect_for_"+tier+"_tool.json", values)
    # Tier-four Dreadstone is harvestable by Abyssalnite (four) and Refined Coralium (five).
    append_tag(DATA, "tags/block/gateway_natural_floor.json", [ID+"dreadstone", ID+"elysian_stone"])
    append_tag(DATA, "tags/entity_type/dread_immune.json", [ID+"dreadling"])
    append_tag(ROOT/"data/minecraft", "tags/entity_type/undead.json", [ID+"dreadling"])
    generation()


def generation():
    write(DATA, "dimension_type/dreadlands.json", {"ultrawarm": False, "natural": False, "piglin_safe": False, "respawn_anchor_works": False,
        "bed_works": False, "has_raids": False, "has_skylight": False, "has_ceiling": False, "coordinate_scale": 1.0,
        "ambient_light": .18, "fixed_time": 18000, "logical_height": 256, "min_y": 0, "height": 256,
        "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:the_end",
        "monster_spawn_light_level": {"type": "minecraft:uniform", "min_inclusive": 0, "max_inclusive": 7}, "monster_spawn_block_light_limit": 0})
    placements = []
    for name, minimum, maximum, attempts, height in [("dreaded_abyssalnite_ore", 4, 15, 8, 64), ("dreadlands_abyssalnite_ore", 8, 19, 6, 54), ("elysian_stone", 24, 24, 7, 68)]:
        write(DATA, "worldgen/configured_feature/"+name+".json", {"type": ID+"ranged_ore", "config": {"target": {"predicate_type": "minecraft:block_match", "block": ID+"dreadstone"},
            "state": {"Name": ID+name}, "min_size": minimum, "max_size": maximum}})
        write(DATA, "worldgen/placed_feature/"+name+".json", {"feature": ID+name, "placement": [
            {"type": "minecraft:count", "count": attempts}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 5}, "max_inclusive": {"absolute": height}}},
            {"type": "minecraft:biome"}]})
        placements.append(ID+name)
    features = [[] for _ in range(11)]; features[6] = placements
    write(DATA, "worldgen/biome/dreadlands.json", {"has_precipitation": False, "temperature": .8, "downfall": 0,
        "effects": {"fog_color": 4133667, "sky_color": 3543336, "water_color": 5338935, "water_fog_color": 2692897,
            "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2}},
        "carvers": {"air": ["minecraft:cave", "minecraft:canyon"]}, "features": features, "spawn_costs": {},
        "spawners": {"monster": [{"type": ID+"dreadling", "weight": 40, "minCount": 1, "maxCount": 2},
                                 {"type": ID+"dreadguard", "weight": 8, "minCount": 1, "maxCount": 1}]}})
    write(DATA, "dimension/dreadlands.json", {"type": ID+"dreadlands", "generator": {"type": "minecraft:noise", "settings": ID+"dreadlands",
        "biome_source": {"type": "minecraft:fixed", "biome": ID+"dreadlands"}}})
    write(DATA, "worldgen/noise/dreadlands_terrain.json", {"firstOctave": -5, "amplitudes": [1, .5, .25]})
    density = {"type": "minecraft:add", "argument1": {"type": "minecraft:y_clamped_gradient", "from_y": 0, "to_y": 160, "from_value": .85, "to_value": -.85},
        "argument2": {"type": "minecraft:mul", "argument1": .75, "argument2": {"type": "minecraft:noise", "noise": ID+"dreadlands_terrain", "xz_scale": 1, "y_scale": 0}}}
    router = {key: 0 for key in ("barrier", "fluid_level_floodedness", "fluid_level_spread", "lava", "temperature", "vegetation", "continents", "erosion", "depth", "ridges", "vein_toggle", "vein_ridged", "vein_gap")}
    router.update({"final_density": {"type": "minecraft:interpolated", "argument": density}, "initial_density_without_jaggedness": density})
    rules = {"type": "minecraft:condition", "if_true": {"type": "minecraft:vertical_gradient", "random_name": ID+"dreadlands_bedrock",
        "true_at_and_below": {"above_bottom": 0}, "false_at_and_above": {"above_bottom": 5}},
        "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:bedrock"}}}
    write(DATA, "worldgen/noise_settings/dreadlands.json", {"sea_level": 49, "disable_mob_generation": False, "aquifers_enabled": False,
        "ore_veins_enabled": False, "legacy_random_source": False, "default_block": {"Name": ID+"dreadstone"}, "default_fluid": {"Name": "minecraft:air"},
        "noise": {"min_y": 0, "height": 256, "size_horizontal": 1, "size_vertical": 2}, "noise_router": router, "spawn_target": [], "surface_rule": rules})


def language():
    en = {"dimension.abyssalport.dreadlands": "The Dreadlands", "biome.abyssalport.dreadlands": "Dreadlands",
        "block.abyssalport.dreadstone": "Dreadstone", "block.abyssalport.dreadstone_cobblestone": "Dreadstone Cobblestone",
        "block.abyssalport.elysian_stone": "Elysian Stone", "block.abyssalport.elysian_cobblestone": "Elysian Cobblestone",
        "block.abyssalport.dreaded_abyssalnite_ore": "Dreaded Abyssalnite Ore", "block.abyssalport.dreadlands_abyssalnite_ore": "Dreadlands Abyssalnite Ore",
        "item.abyssalport.dreadlands_infused_gateway_key": "Dreadlands Infused Gateway Key", "item.abyssalport.dread_fragment": "Dread Fragment",
        "item.abyssalport.dreadling_spawn_egg": "Dreadling Spawn Egg", "entity.abyssalport.dreadling": "Dreadling", "effect.abyssalport.dread_plague": "Dread Plague",
        "research.abyssalport.dreadlands_key": "A Key to the Dreadlands", "research.abyssalport.dreadlands": "The Dreadlands",
        "screen.abyssalport.page.19": "Infusing the Gateway Key", "screen.abyssalport.page.20": "The Dreadlands",
        "screen.abyssalport.locked.19": "Collect the Stronghold Powerstone to reveal the next key infusion.", "screen.abyssalport.locked.20": "Enter the Dreadlands to reveal these notes.",
        "screen.abyssalport.dreadlands_key": "In the Abyssal Wasteland, form a tier-one ritual ground from nine Abyssal Cobblestone blocks. Put your basic Gateway Key on the altar. Place one Transmutation Gem, the Stronghold Powerstone and Asorah's Eye on any three pedestals; leave five empty. Use a tier-one Necronomicon with 10,000 PE and remain nearby for ten seconds. The infused key stays on the altar.",
        "screen.abyssalport.dreadlands_key_note": "Right-click air to select a destination; sneak to cycle backwards. This key retains the Overworld/Wasteland link and opens Wasteland/Dreadlands travel. It cannot jump directly from the Overworld to the Dreadlands. Use the existing 1,000-PE cardinal Shadow Gem portal ritual with the infused key on a basic ritual ground. A linked return anchor is created on arrival. Keep both anchors intact.",
        "screen.abyssalport.dreadlands": "Dark red Dreadstone terrain contains Dreaded Abyssalnite, Dreadlands Abyssalnite and blue Elysian Stone deposits. A tier-four Abyssalnite or tier-five Refined Coralium Pickaxe can collect this stone; ordinary Netherite cannot. Dreaded Abyssalnite smelts to an Abyssalnite Ingot. Dreadlands Abyssalnite requires later processing.",
        "screen.abyssalport.dreadlands_note": "Fireproof, undead Dreadlings have 30 health and six melee damage. Successful hits inflict five seconds of Dread Plague: one magic damage per 25 ticks and hunger exhaustion. Milk cannot cure it. Dreadlings resist their own plague and may drop zero to two Dread Fragments, increased by Looting. This entry-stage dimension does not yet contain its full forest/ocean ecology, mineshafts or late bosses."}
    de = {"dimension.abyssalport.dreadlands": "Die Dreadlands", "biome.abyssalport.dreadlands": "Dreadlands",
        "block.abyssalport.dreadstone": "Dreadstone", "block.abyssalport.dreadstone_cobblestone": "Dreadstone-Bruchstein",
        "block.abyssalport.elysian_stone": "Elysischer Stein", "block.abyssalport.elysian_cobblestone": "Elysischer Bruchstein",
        "block.abyssalport.dreaded_abyssalnite_ore": "Dreaded-Abyssalnit-Erz", "block.abyssalport.dreadlands_abyssalnite_ore": "Dreadlands-Abyssalnit-Erz",
        "item.abyssalport.dreadlands_infused_gateway_key": "Dreadlands-durchsetzter Portalschlüssel", "item.abyssalport.dread_fragment": "Dread-Fragment",
        "item.abyssalport.dreadling_spawn_egg": "Dreadling-Spawn-Ei", "entity.abyssalport.dreadling": "Dreadling", "effect.abyssalport.dread_plague": "Dread-Pest",
        "research.abyssalport.dreadlands_key": "Ein Schlüssel zu den Dreadlands", "research.abyssalport.dreadlands": "Die Dreadlands",
        "screen.abyssalport.page.19": "Den Portalschlüssel durchsetzen", "screen.abyssalport.page.20": "Die Dreadlands",
        "screen.abyssalport.locked.19": "Sammle den Kraftstein aus der Festung, um die nächste Schlüsselinfusion zu entdecken.", "screen.abyssalport.locked.20": "Betritt die Dreadlands, um diese Aufzeichnungen freizuschalten.",
        "screen.abyssalport.dreadlands_key": "Forme im Abyssalen Ödland einen Stufe-eins-Ritualplatz aus neun Abyssal-Bruchsteinen. Lege deinen einfachen Portalschlüssel auf den Altar. Ein Transmutationsedelstein, der Festungs-Kraftstein und Asorahs Auge gehören auf drei beliebige Podeste; fünf bleiben leer. Aktiviere mit einem Stufe-eins-Necronomicon und 10.000 PE und bleibe zehn Sekunden in der Nähe. Der durchsetzte Schlüssel bleibt auf dem Altar.",
        "screen.abyssalport.dreadlands_key_note": "Rechtsklick in die Luft wählt das Ziel, Schleichen wechselt rückwärts. Der Schlüssel erhält Oberwelt/Ödland und öffnet Ödland/Dreadlands. Ein direkter Sprung Oberwelt/Dreadlands bleibt unmöglich. Nutze das bestehende 1.000-PE-Portalritual mit vier Schattenedelsteinen in den Himmelsrichtungen und dem durchsetzten Schlüssel auf einem Basis-Ritualplatz. Bei der Ankunft entsteht ein verbundenes Rückportal. Erhalte beide Anker.",
        "screen.abyssalport.dreadlands": "Dunkelrotes Dreadstone-Gelände enthält Dreaded-Abyssalnit, Dreadlands-Abyssalnit und blauen Elysischen Stein. Eine Abyssalnit-Spitzhacke (Stufe vier) oder Refined Coralium (fünf) erschließt diese Steine; normale Netheritwerkzeuge nicht. Dreaded-Abyssalnit schmilzt zu einem Abyssalnit-Barren. Dreadlands-Abyssalnit benötigt spätere Verarbeitung.",
        "screen.abyssalport.dreadlands_note": "Feuerfeste, untote Dreadlings besitzen 30 Leben und sechs Nahkampfschaden. Erfolgreiche Treffer verursachen fünf Sekunden Dread-Pest: einen Magieschaden je 25 Ticks und Hungererschöpfung. Milch heilt sie nicht. Dreadlings widerstehen ihrer eigenen Pest und lassen null bis zwei Dread-Fragmente fallen, mit Plünderung mehr. Diese Einstiegsdimension enthält noch nicht die gesamte Wald-/Ozean-Ökologie, Minenschächte oder spätere Bosse."}
    for locale, entries in (("en_us", en), ("de_de", de)):
        path = ASSETS/"lang"/(locale+".json"); existing = json.loads(path.read_text(encoding="utf-8")); existing.update(entries)
        write(ASSETS, "lang/"+locale+".json", existing)


if __name__ == "__main__":
    media(); resources(); language()
