"""Generate distinct, independently authored PE-device block and item models.

Coordinates use the Minecraft 0..16 block grid. Keeping the three silhouettes
in one place makes it harder for a block and its hotbar item to drift apart.
"""

import json
from pathlib import Path


MODELS = Path(__file__).resolve().parents[1] / "src/main/resources/assets/abyssalport/models"
DIRECTIONS = ("north", "south", "east", "west", "up", "down")
TEXTURE_ROOT = "abyssalport:block/"


def cube(start, end, sides, top=None, bottom=None, front=None):
    textures = {face: sides for face in DIRECTIONS}
    textures["up"] = top or sides
    textures["down"] = bottom or sides
    if front:
        textures["north"] = front
    return {
        "from": start,
        "to": end,
        "faces": {
            face: {"texture": f"#{textures[face]}", "uv": [0, 0, 16, 16]}
            for face in DIRECTIONS
        },
    }


DEVICES = {
    "energy_pedestal": {
        "textures": {"stone": "pe_stone", "carved": "pe_carved", "trim": "pe_trim", "panel": "pe_collector_panel"},
        "elements": [
            cube([4, 0, 4], [12, 2, 12], "stone", "carved"),
            cube([5, 2, 5], [11, 3, 11], "trim", "carved"),
            cube([6, 3, 6], [10, 12, 10], "carved"),
            cube([5, 4, 6], [6, 11, 10], "panel"),
            cube([10, 4, 6], [11, 11, 10], "panel"),
            cube([6, 4, 5], [10, 11, 6], "panel"),
            cube([6, 4, 10], [10, 11, 11], "panel"),
            cube([5, 12, 5], [11, 13, 11], "trim", "carved"),
            cube([4, 13, 4], [12, 15, 12], "stone", "carved"),
            cube([5, 15, 5], [11, 16, 11], "trim", "panel"),
        ],
        "gui_scale": 0.8,
    },
    "energy_collector": {
        "textures": {
            "stone": "pe_stone", "carved": "pe_carved",
            "trim": "pe_trim", "panel": "pe_collector_panel",
            "crystal": "pe_crystal",
        },
        "elements": [
            cube([3, 0, 3], [13, 2, 13], "stone", "carved"),
            cube([4, 2, 4], [12, 3, 12], "panel", "stone"),
            cube([5, 3, 5], [11, 10, 11], "crystal"),
            cube([3, 2, 3], [5, 10, 5], "trim"),
            cube([11, 2, 3], [13, 10, 5], "trim"),
            cube([3, 2, 11], [5, 10, 13], "trim"),
            cube([11, 2, 11], [13, 10, 13], "trim"),
            cube([4, 10, 4], [12, 12, 6], "trim", "carved"),
            cube([4, 10, 10], [12, 12, 12], "trim", "carved"),
            cube([4, 10, 6], [6, 12, 10], "trim", "carved"),
            cube([10, 10, 6], [12, 12, 10], "trim", "carved"),
            cube([6, 10, 6], [10, 13, 10], "panel", "crystal"),
        ],
        "gui_scale": 0.9,
    },
    "energy_relay": {
        "textures": {
            "stone": "pe_stone", "carved": "pe_carved",
            "trim": "pe_trim",
            "front": "pe_relay_front", "side": "pe_relay_side",
            "top": "pe_relay_top",
        },
        "elements": [
            cube([3, 1, 14], [13, 15, 16], "stone", "carved"),
            cube([2, 1, 12], [4, 15, 14], "carved", "stone"),
            cube([12, 1, 12], [14, 15, 14], "carved", "stone"),
            cube([4, 1, 12], [12, 3, 14], "carved", "stone"),
            cube([4, 13, 12], [12, 15, 14], "carved", "stone"),
            cube([5, 5, 11], [11, 11, 12], "side", front="front"),
            cube([7, 7, 5], [9, 9, 11], "side", "top", front="front"),
            cube([5, 5, 4], [11, 11, 6], "carved", "top", front="front"),
            cube([5, 7, 6], [7, 9, 10], "trim"),
            cube([9, 7, 6], [11, 9, 10], "trim"),
            cube([7, 5, 6], [9, 7, 10], "trim"),
            cube([7, 9, 6], [9, 11, 10], "trim"),
        ],
        "gui_scale": 0.78,
    },
    "energy_container": {
        "textures": {
            "stone": "pe_stone", "carved": "pe_carved",
            "trim": "pe_trim",
            "front": "pe_container_front", "side": "pe_container_side",
            "top": "pe_container_top",
        },
        "elements": [
            cube([0, 0, 0], [16, 2, 16], "stone", "carved"),
            cube([5, 2, 5], [11, 5, 11], "front", "top", "stone"),
            cube([2, 2, 2], [5, 12, 5], "trim", "stone"),
            cube([11, 2, 2], [14, 12, 5], "trim", "stone"),
            cube([2, 2, 11], [5, 12, 14], "trim", "stone"),
            cube([11, 2, 11], [14, 12, 14], "trim", "stone"),
            cube([5, 2, 3], [11, 6, 5], "side", "carved", front="front"),
            cube([5, 2, 11], [11, 6, 13], "side", "carved"),
            cube([3, 2, 5], [5, 6, 11], "side", "carved"),
            cube([11, 2, 5], [13, 6, 11], "side", "carved"),
            cube([5, 6, 4], [11, 8, 5], "side", "carved", front="front"),
            cube([5, 6, 11], [11, 8, 12], "side", "carved"),
            cube([4, 6, 5], [5, 8, 11], "side", "carved"),
            cube([11, 6, 5], [12, 8, 11], "side", "carved"),
            cube([5, 5, 5], [11, 7.5, 11], "front", "top", "stone"),
        ],
        "gui_scale": 0.72,
    },
}


def write_json(path, contents):
    path.write_text(json.dumps(contents, indent=2) + "\n", encoding="utf-8")


def main():
    for name, device in DEVICES.items():
        textures = {
            key: TEXTURE_ROOT + value for key, value in device["textures"].items()
        }
        textures["particle"] = "#stone"
        write_json(MODELS / "block" / f"{name}.json", {
            "textures": textures,
            "elements": device["elements"],
        })
        scale = device["gui_scale"]
        write_json(MODELS / "item" / f"{name}.json", {
            "parent": f"abyssalport:block/{name}",
            "display": {
                "gui": {
                    "rotation": [30, 225, 0],
                    "translation": [0, 0, 0],
                    "scale": [scale, scale, scale],
                },
                "ground": {"scale": [0.35, 0.35, 0.35]},
                "fixed": {"scale": [0.5, 0.5, 0.5]},
            },
        })


if __name__ == "__main__":
    main()
