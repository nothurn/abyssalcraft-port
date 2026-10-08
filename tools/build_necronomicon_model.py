"""Generate an independently authored, atlas-safe 3D Necronomicon item model."""

from build_pe_models import MODELS, cube, write_json


def main():
    model = {
        "textures": {
            "cover": "abyssalport:item/necronomicon_cover",
            "pages": "abyssalport:item/necronomicon_pages",
            "spine": "abyssalport:item/necronomicon_spine",
            "clasp": "abyssalport:item/necronomicon_clasp",
            "particle": "#cover",
        },
        "elements": [
            cube([4, 2, 7.25], [13, 14, 8.75], "pages"),
            cube([3, 1, 6.75], [14, 15, 7.25], "cover"),
            cube([3, 1, 8.75], [14, 15, 9.25], "cover"),
            cube([2, 1, 6.75], [4, 15, 9.25], "spine"),
            cube([12, 7, 6.25], [14, 9, 6.75], "clasp"),
            cube([14, 7, 6.75], [15, 9, 9.25], "clasp"),
            cube([12, 7, 9.25], [14, 9, 9.75], "clasp"),
        ],
        "display": {
            "gui": {
                "rotation": [10, 180, 0],
                "translation": [0, 0, 0],
                "scale": [0.95, 0.95, 0.95],
            },
            "ground": {"scale": [0.35, 0.35, 0.35]},
            "fixed": {"scale": [0.65, 0.65, 0.65]},
            "thirdperson_righthand": {
                "rotation": [75, 45, 0],
                "translation": [0, 2, 0],
                "scale": [0.55, 0.55, 0.55],
            },
            "thirdperson_lefthand": {
                "rotation": [75, 225, 0],
                "translation": [0, 2, 0],
                "scale": [0.55, 0.55, 0.55],
            },
            "firstperson_righthand": {
                "rotation": [0, 180, 0],
                "translation": [0, 1, 0],
                "scale": [0.75, 0.75, 0.75],
            },
            "firstperson_lefthand": {
                "rotation": [0, 180, 0],
                "translation": [0, 1, 0],
                "scale": [0.75, 0.75, 0.75],
            },
        },
    }
    write_json(MODELS / "item/necronomicon.json", model)


if __name__ == "__main__":
    main()
