"""Generate the local Darkstone slab/stair model variants for Minecraft 1.21.1."""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/abyssalport"
TEXTURE = "abyssalport:block/darkstone_bricks"


def write(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    blockstates = ASSETS / "blockstates"
    block_models = ASSETS / "models/block"
    item_models = ASSETS / "models/item"
    slab = "darkstone_brick_slab"
    stairs = "darkstone_brick_stairs"

    write(blockstates / f"{slab}.json", {"variants": {
        "type=bottom": {"model": f"abyssalport:block/{slab}"},
        "type=top": {"model": f"abyssalport:block/{slab}_top"},
        "type=double": {"model": "abyssalport:block/darkstone_bricks"},
    }})
    for suffix, parent in (("", "slab"), ("_top", "slab_top")):
        write(block_models / f"{slab}{suffix}.json", {
            "parent": f"minecraft:block/{parent}",
            "textures": {"bottom": TEXTURE, "top": TEXTURE, "side": TEXTURE},
        })
    write(item_models / f"{slab}.json", {"parent": f"abyssalport:block/{slab}"})

    # Match vanilla 1.21.1's 40 stair states; only the authored texture differs.
    variants = {}
    base_y = {"east": 0, "north": 270, "south": 90, "west": 180}
    for facing, base in base_y.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                suffix = "" if shape == "straight" else "_inner" if shape.startswith("inner") else "_outer"
                rotation = (base - 90 if shape.endswith("left") and half == "bottom"
                            else base + 90 if shape.endswith("right") and half == "top"
                            else base) % 360
                state = {"model": f"abyssalport:block/{stairs}{suffix}", "uvlock": True}
                if half == "top":
                    state["x"] = 180
                if rotation:
                    state["y"] = rotation
                variants[f"facing={facing},half={half},shape={shape}"] = state
    write(blockstates / f"{stairs}.json", {"variants": variants})
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        write(block_models / f"{stairs}{suffix}.json", {
            "parent": f"minecraft:block/{parent}",
            "textures": {"bottom": TEXTURE, "top": TEXTURE, "side": TEXTURE},
        })
    write(item_models / f"{stairs}.json", {"parent": f"abyssalport:block/{stairs}"})


if __name__ == "__main__":
    main()
