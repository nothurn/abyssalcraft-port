"""Independent pixel art and repetitive resources for the first metal chain.

No upstream media is read. Source-informed mechanics are listed in SOURCE-AUDIT.
"""
from pathlib import Path
import json
import random
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS, DATA = ROOT / "assets/abyssalport", ROOT / "data/abyssalport"
METAL = ["#211d33", "#3f355b", "#675780", "#9485ad", "#cab9d7"]
PATTERNS = {"pickaxe": ["III", " S ", " S "], "axe": ["II", "IS", " S"],
            "shovel": ["I", "S", "S"], "sword": ["I", "I", "S"], "hoe": ["II", " S", " S"],
            "helmet": ["III", "I I"], "chestplate": ["I I", "III", "III"],
            "leggings": ["III", "I I", "I I"], "boots": ["I I", "I I"]}
TOOLS = {"pickaxe", "axe", "shovel", "sword", "hoe"}


def resource(root: Path, relative: str, value: dict) -> None:
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def texture(relative: str, image: Image.Image) -> None:
    path = ASSETS / "textures" / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def equipment_tags(prefix: str) -> None:
    """Native 1.21 enchantments/recipes query item tags, not just ArmorItem/TieredItem."""
    names = {"pickaxes": "pickaxe", "axes": "axe", "shovels": "shovel", "swords": "sword", "hoes": "hoe",
             "head_armor": "helmet", "chest_armor": "chestplate", "leg_armor": "leggings", "foot_armor": "boots"}
    for tag, suffix in names.items():
        relative = "tags/item/"+tag+".json"
        base = ROOT / "data/minecraft"
        path = base / relative
        value = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
        item = "abyssalport:"+prefix+"_"+suffix
        if item not in value["values"]: value["values"].append(item)
        resource(base, relative, value)


def blocks() -> None:
    rng = random.Random(9017)
    ore = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            shade = rng.choice([77, 84, 90, 95, 101])
            ore.putpixel((x, y), (shade, shade, shade + 3))
    draw = ImageDraw.Draw(ore)
    for x, y in [(3, 3), (10, 4), (6, 9), (12, 12), (2, 13)]:
        draw.polygon([(x - 1, y), (x, y - 1), (x + 2, y), (x + 1, y + 2), (x, y + 2)], fill=METAL[0])
        draw.line((x, y, x + 1, y + 1), fill=METAL[2])
        draw.point((x, y), fill=METAL[4])
        draw.point((x + 1, y + 2), fill=METAL[1])
    texture("block/abyssalnite_ore.png", ore)
    metal = Image.new("RGB", (16, 16), METAL[1])
    draw = ImageDraw.Draw(metal)
    draw.rectangle((1, 1, 14, 14), fill=METAL[2], outline=METAL[0])
    draw.line((2, 2, 13, 2), fill=METAL[3])
    draw.line((2, 3, 2, 13), fill=METAL[3])
    draw.line((3, 13, 13, 13), fill=METAL[1])
    draw.line((13, 3, 13, 12), fill=METAL[1])
    draw.polygon([(5, 7), (7, 5), (10, 7), (7, 10)], outline=METAL[1])
    draw.point((7, 6), fill=METAL[3])
    texture("block/abyssalnite_block.png", metal)


def item(suffix: str, prefix: str = "abyssalnite") -> None:
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    if suffix == "ingot":
        draw.polygon([(2, 7), (5, 4), (12, 4), (14, 7), (12, 11), (4, 11), (2, 9)], fill=METAL[0])
        draw.polygon([(3, 7), (6, 5), (11, 5), (13, 7), (10, 8), (4, 8)], fill=METAL[3])
        draw.polygon([(3, 8), (10, 9), (13, 8), (11, 10), (4, 10)], fill=METAL[2])
        draw.line((6, 5, 11, 5), fill=METAL[4])
    elif suffix == "nugget":
        draw.polygon([(6, 5), (9, 4), (11, 6), (10, 10), (7, 11), (4, 9), (4, 7)], fill=METAL[0])
        draw.polygon([(6, 6), (9, 5), (10, 7), (8, 9), (5, 8)], fill=METAL[3])
        draw.line((5, 9, 8, 10), fill=METAL[2])
        draw.point((8, 6), fill=METAL[4])
    elif suffix in TOOLS:
        draw.line((3, 13, 10, 6), fill="#17141d", width=3)
        draw.line((3, 13, 10, 6), fill="#5c4235")
        draw.point((4, 12), fill="#977156")
        heads = {
            "pickaxe": [(3, 3), (5, 1), (10, 2), (14, 6), (14, 10), (12, 7), (9, 4), (6, 3)],
            "axe": [(6, 2), (9, 1), (13, 4), (13, 8), (10, 9), (7, 6)],
            "hoe": [(4, 3), (5, 1), (10, 1), (12, 3), (10, 5), (8, 3)],
            "shovel": [(9, 2), (12, 1), (14, 3), (13, 6), (10, 8), (8, 6)],
            "sword": [(12, 1), (15, 1), (15, 4), (8, 11), (6, 9)],
        }
        if suffix == "sword":
            draw.line((5, 7, 10, 12), fill=METAL[0], width=3)
            draw.line((5, 7, 10, 12), fill=METAL[2])
        shape = heads[suffix]
        draw.polygon(shape, fill=METAL[0])
        draw.line([(x, y + 1) for x, y in shape[:-1]], fill=METAL[3])
        draw.line((9, 3, 12, 5), fill=METAL[2], width=2)
        draw.point((11, 3), fill=METAL[4])
    else:
        silhouettes = {
            "helmet": [(3, 3), (6, 1), (11, 1), (13, 3), (13, 11), (10, 12), (10, 7), (6, 7), (6, 12), (3, 11)],
            "chestplate": [(4, 2), (6, 4), (9, 4), (11, 2), (14, 4), (13, 8), (11, 7), (11, 13), (4, 13), (4, 7), (2, 8), (1, 4)],
            "leggings": [(4, 2), (12, 2), (12, 14), (9, 14), (9, 7), (7, 7), (7, 14), (4, 14)],
            "boots": [(3, 6), (6, 6), (6, 10), (7, 11), (7, 14), (2, 14), (2, 11), (3, 10)],
        }
        draw.polygon(silhouettes[suffix], fill=METAL[1], outline=METAL[0])
        if suffix == "boots":
            draw.polygon([(10, 6), (13, 6), (13, 10), (14, 11), (14, 14), (9, 14), (9, 11), (10, 10)], fill=METAL[1], outline=METAL[0])
            draw.line((3, 7, 5, 7), fill=METAL[3])
            draw.line((10, 7, 12, 7), fill=METAL[3])
        else:
            draw.line((5, 3, 10, 3), fill=METAL[3])
            draw.line((5, 4, 5, 11), fill=METAL[2])
            draw.point((10, 5), fill=METAL[4])
    texture(f"item/{prefix}_{suffix}.png", image)


def armor(prefix: str = "abyssalnite") -> None:
    # 64x32 armor cube UVs: helmet, torso, mirrored arms and mirrored legs.
    for layer in (1, 2):
        image = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
        draw = ImageDraw.Draw(image)
        faces = [(8, 0, 15, 7), (16, 0, 23, 7), (0, 8, 31, 15),
                 (20, 16, 27, 19), (28, 16, 35, 19), (16, 20, 39, 31),
                 (44, 16, 47, 19), (48, 16, 51, 19), (40, 20, 55, 31),
                 (4, 16, 7, 19), (8, 16, 11, 19), (0, 20, 15, 31)]
        for x1, y1, x2, y2 in faces:
            draw.rectangle((x1, y1, x2, y2), fill=METAL[1])
            draw.line((x1, y1, x2, y1), fill=METAL[3])
            draw.line((x1, y2, x2, y2), fill=METAL[0])
        draw.rectangle((9, 10, 14, 14), fill=(0, 0, 0, 0))
        draw.line((8, 9, 15, 9), fill=METAL[4])
        draw.rectangle((21, 21, 26, 28), fill=METAL[2])
        draw.line((20, 30, 27, 30), fill=METAL[3])
        draw.point((23, 23), fill=METAL[4])
        draw.line((4, 29 if layer == 1 else 23, 7, 29 if layer == 1 else 23), fill=METAL[3])
        texture(f"models/armor/{prefix}_layer_{layer}.png", image)


def data() -> None:
    equipment_tags("abyssalnite")
    for name in ("abyssalnite_ore", "abyssalnite_block"):
        resource(ASSETS, f"blockstates/{name}.json", {"variants": {"": {"model": f"abyssalport:block/{name}"}}})
        resource(ASSETS, f"models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"abyssalport:block/{name}"}})
        resource(ASSETS, f"models/item/{name}.json", {"parent": f"abyssalport:block/{name}"})
        resource(DATA, f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
                 "entries": [{"type": "minecraft:item", "name": f"abyssalport:{name}"}],
                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    for suffix in ["ingot", "nugget", *PATTERNS]:
        item(suffix)
        resource(ASSETS, f"models/item/abyssalnite_{suffix}.json", {
            "parent": "minecraft:item/handheld" if suffix in TOOLS else "minecraft:item/generated",
            "textures": {"layer0": f"abyssalport:item/abyssalnite_{suffix}"}})
    for suffix, pattern in {**PATTERNS, "block": ["III", "III", "III"]}.items():
        keys = {"I": {"item": "abyssalport:abyssalnite_ingot"}}
        if suffix in TOOLS:
            keys["S"] = {"tag": "c:rods/wooden"}
        resource(DATA, f"recipe/abyssalnite_{suffix}.json", {"type": "minecraft:crafting_shaped",
                 "pattern": pattern, "key": keys, "result": {"id": f"abyssalport:abyssalnite_{suffix}"}})
    for name, source, output, count in [
            ("abyssalnite_ingot_from_block", "abyssalnite_block", "abyssalnite_ingot", 9),
            ("abyssalnite_nugget", "abyssalnite_ingot", "abyssalnite_nugget", 9)]:
        resource(DATA, f"recipe/{name}.json", {"type": "minecraft:crafting_shapeless",
                 "ingredients": [{"item": f"abyssalport:{source}"}], "result": {"id": f"abyssalport:{output}", "count": count}})
    resource(DATA, "recipe/abyssalnite_ingot_from_nuggets.json", {"type": "minecraft:crafting_shaped",
             "pattern": ["NNN", "NNN", "NNN"], "key": {"N": {"item": "abyssalport:abyssalnite_nugget"}}, "result": {"id": "abyssalport:abyssalnite_ingot"}})
    for cooking, ticks in [("smelting", 200), ("blasting", 100)]:
        resource(DATA, f"recipe/abyssalnite_ingot_from_{cooking}.json", {"type": f"minecraft:{cooking}",
                 "category": "misc", "ingredient": {"item": "abyssalport:abyssalnite_ore"},
                 "result": {"id": "abyssalport:abyssalnite_ingot"}, "experience": 3.0, "cookingtime": ticks})
    resource(DATA, "worldgen/configured_feature/ore_abyssalnite.json", {"type": "abyssalport:abyssalnite_ore", "config": {}})
    resource(DATA, "worldgen/placed_feature/ore_abyssalnite.json", {"feature": "abyssalport:ore_abyssalnite", "placement": [
        {"type": "minecraft:count", "count": {"type": "minecraft:uniform", "min_inclusive": 3, "max_inclusive": 8}},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 4}, "max_inclusive": {"absolute": 31}}},
        {"type": "minecraft:biome"}]})
    for kind, name in [("ingots", "abyssalnite_ingot"), ("nuggets", "abyssalnite_nugget"),
                       ("ores", "abyssalnite_ore"), ("storage_blocks", "abyssalnite_block")]:
        resource(ROOT / "data/c", f"tags/item/{kind}/abyssalnite.json", {"replace": False, "values": [f"abyssalport:{name}"]})
        resource(ROOT / "data/c", f"tags/item/{kind}.json", {"replace": False, "values": [f"#c:{kind}/abyssalnite"]})
    recipes = [f"abyssalport:abyssalnite_{suffix}" for suffix in PATTERNS]
    recipes += ["abyssalport:abyssalnite_block", "abyssalport:abyssalnite_nugget", "abyssalport:abyssalnite_ingot_from_nuggets", "abyssalport:abyssalnite_ingot_from_block"]
    resource(DATA, "advancement/recipes/abyssalnite_equipment.json", {"criteria": {"metal": {
        "trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": ["abyssalport:abyssalnite_ingot"]}]}}}, "rewards": {"recipes": recipes}})


if __name__ == "__main__":
    blocks()
    armor()
    data()
