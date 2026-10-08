"""Own low-poly tablet/machine and 16px art; only source mechanics are referenced."""
import copy
import json
from PIL import Image, ImageDraw
from build_pe_models import cube
from generate_abyssalnite_assets import ROOT, ASSETS, DATA, resource, texture
from generate_wasteland_minerals import append_tag


def main():
    for cursed, ink in ((False, "#7cab94"), (True, "#b57aca")):
        image = Image.new("RGBA", (16, 16), "#292d30")
        draw = ImageDraw.Draw(image)
        draw.rectangle((1, 1, 14, 14), outline="#626560")
        draw.rectangle((2, 2, 13, 13), outline="#151a1d")
        for start, end in (((4, 4), (7, 4)), ((7, 4), (7, 7)), ((4, 7), (11, 7)),
                           ((9, 4), (11, 4)), ((9, 4), (9, 11)), ((4, 10), (7, 10)), ((7, 10), (7, 12))):
            draw.line((*start, *end), fill=ink)
        draw.point((11, 11), fill="#ded7b2")
        suffix = "_cursed" if cursed else ""
        texture("item/stone_tablet"+suffix+".png", image)
        tablet = {"textures": {"stone": "abyssalport:block/pe_stone", "rune": "abyssalport:item/stone_tablet"+suffix},
                  "elements": [cube([3, 1, 6.5], [13, 15, 9.5], "stone", front="rune")],
                  "display": {"gui": {"rotation": [8, -15, 0], "scale": [.95]*3},
                              "ground": {"translation": [0, 3, 0], "scale": [.4]*3},
                              "firstperson_righthand": {"rotation": [0, 10, 0], "translation": [0, 2, 0], "scale": [.65]*3},
                              "firstperson_lefthand": {"rotation": [0, -10, 0], "translation": [0, 2, 0], "scale": [.65]*3},
                              "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [.45]*3},
                              "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [.45]*3},
                              "fixed": {"scale": [.7]*3}}}
        if not cursed:
            tablet["overrides"] = [{"predicate": {"abyssalport:cursed": 1}, "model": "abyssalport:item/stone_tablet_cursed"}]
        resource(ASSETS, "models/item/stone_tablet"+suffix+".json", tablet)
    machine_art = Image.new("RGBA", (16, 16), "#24292c")
    draw = ImageDraw.Draw(machine_art)
    draw.rectangle((2, 3, 13, 12), outline="#6f7472")
    draw.rectangle((4, 5, 11, 10), fill="#141a1e")
    draw.line((5, 8, 10, 8), fill="#7cab94")
    draw.line((8, 6, 10, 8, 8, 10), fill="#7cab94")
    texture("block/state_transformer_front.png", machine_art)
    elements = [cube([1.6, 0, 1.6], [14.4, 2, 14.4], "stone"),
                cube([2.8, 2, 2.8], [13.2, 9, 13.2], "carved", front="front"),
                cube([2, 9, 2], [14, 10, 14], "trim"),
                cube([1.6, 10, 1.6], [14.4, 11, 14.4], "stone", "carved"),
                cube([3, 11, 3], [5, 12, 13], "trim"), cube([11, 11, 3], [13, 12, 13], "trim")]
    textures = {"stone": "abyssalport:block/pe_stone", "carved": "abyssalport:block/pe_carved",
                "trim": "abyssalport:block/pe_trim", "front": "abyssalport:block/state_transformer_front",
                "rune": "abyssalport:item/stone_tablet", "particle": "#stone"}
    for present in (False, True):
        geometry = copy.deepcopy(elements)
        if present: geometry.append(cube([5.2, 11, 3.5], [10.8, 12, 12.5], "stone", "rune"))
        resource(ASSETS, "models/block/state_transformer"+("_tablet" if present else "")+".json", {"textures": textures, "elements": geometry})
    variants = {}
    for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for present in (False, True):
            variants[f"facing={facing},tablet={str(present).lower()}"] = {
                "model": "abyssalport:block/state_transformer"+("_tablet" if present else ""), "y": rotation}
    resource(ASSETS, "blockstates/state_transformer.json", {"variants": variants})
    resource(ASSETS, "models/item/state_transformer.json", {"parent": "abyssalport:block/state_transformer", "display": {
        "gui": {"rotation": [30, 225, 0], "scale": [.8]*3}, "ground": {"translation": [0, 3, 0], "scale": [.3]*3}, "fixed": {"scale": [.5]*3}}})
    resource(DATA, "loot_table/blocks/state_transformer.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "conditions": [{"condition": "minecraft:survives_explosion"}], "entries": [{"type": "minecraft:item", "name": "abyssalport:state_transformer"}]}]})
    crafts = {
        "stone_tablet": (["#%#", "%&%", "#%#"], {"#": {"item": "abyssalport:shadow_shard"}, "%": {"item": "abyssalport:monolith_stone"}, "&": {"item": "abyssalport:shadow_gem"}}),
        "state_transformer": (["###", "#&#", "#@#"], {"#": {"item": "abyssalport:monolith_stone"}, "&": {"item": "abyssalport:transmutation_gem"}, "@": {"tag": "c:chests/wooden"}})}
    for name, (pattern, key) in crafts.items():
        resource(DATA, "recipe/"+name+".json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                 "key": key, "result": {"id": "abyssalport:"+name}})
        resource(DATA, "advancement/recipes/"+name+".json", {"criteria": {"has_material": {"trigger": "minecraft:inventory_changed",
                 "conditions": {"items": [{"items": ["abyssalport:shadow_gem"]}]}}}, "rewards": {"recipes": ["abyssalport:"+name]}})
    append_tag(ROOT/"data/minecraft", "tags/block/mineable/pickaxe.json", ["abyssalport:state_transformer"])
    texts = {
        "item.abyssalport.stone_tablet": ("Stone Tablet", "Steintafel"),
        "block.abyssalport.state_transformer": ("State Transformer", "Zustandswandler"),
        "tooltip.abyssalport.stone_tablet.cursed": ("Cursed — cannot be transformed", "Verflucht — kann nicht umgewandelt werden"),
        "tooltip.abyssalport.stone_tablet.value": ("Cargo value: %s PE", "Inhaltswert: %s PE"),
        "tooltip.abyssalport.stone_tablet.contents": ("Sealed slots: %s / %s", "Versiegelte Plätze: %s / %s"),
        "tooltip.abyssalport.stone_tablet.blank": ("Seal items using a State Transformer", "Gegenstände im Zustandswandler versiegeln"),
        "tooltip.abyssalport.stone_tablet.invalid": ("Invalid sealed data — contents left untouched", "Ungültige versiegelte Daten — Inhalt bleibt unverändert"),
        "screen.abyssalport.state_transformer.pack": ("Pack", "Packen"),
        "screen.abyssalport.state_transformer.unpack": ("Unpack", "Lösen"),
        "screen.abyssalport.state_transformer.guide": (
            "State Transformer: seven Monolith Stones, a wooden chest and one use of a Transmutation Gem. Stone Tablet: four Shadow Shards, four Monolith Stones and one Shadow Gem. In Pack mode, a blank tablet seals up to 49 cargo slots in 10 seconds; Unpack requires every cargo slot to be empty and returns the original items to their exact positions. Names, damage and components are retained. Nested bags, shulker boxes, bundles and other inventories are not cargo. Slots and mode are locked during processing. Automation: Pack accepts cargo from above and a blank tablet from the sides, returning the sealed tablet below. Unpack accepts a sealed tablet above and returns cargo and blank tablet below. The tablet's potential value is count × (64 / maximum stack size) per cargo stack, not rechargeable PE. Cursed tablets cannot be processed. The Energy Depositioner is not yet implemented; this path currently provides reversible item storage, not biome conversion.",
            "Zustandswandler: sieben Monolithsteine, eine Holztruhe und eine Nutzung eines Transmutationsedelsteins. Steintafel: vier Schattensplitter, vier Monolithsteine und ein Schattenedelstein. Packen versiegelt bis zu 49 Inventarplätze in 10 Sekunden; Lösen benötigt vollständig leere Inhaltsplätze und gibt die Originalgegenstände an ihre genauen Positionen zurück. Namen, Schaden und Komponenten bleiben erhalten. Taschen, Shulkerkisten, Bündel und andere Inventare sind kein zulässiger Inhalt. Plätze und Modus sind während der Verarbeitung gesperrt. Automatik: Packen nimmt Inhalt von oben und leere Tafeln seitlich an; die versiegelte Tafel kommt unten heraus. Lösen nimmt versiegelte Tafeln von oben an, Inhalt und leere Tafel kommen unten heraus. Der Inhaltswert ist Anzahl × (64 / maximale Stapelgröße), keine aufladbare PE. Verfluchte Tafeln sind gesperrt. Der Energiedeponierer ist noch nicht implementiert; der Pfad bietet bisher rückgängig machbare Inventarspeicherung, keine Biomkonversion.")}
    for index, locale in enumerate(("en_us", "de_de")):
        # This generator remains usable on its own after the connected fuel path lands.
        if index == 0:
            key = "screen.abyssalport.state_transformer.guide"
            text = texts[key][0].replace("The Energy Depositioner is not yet implemented; this path currently provides reversible item storage, not biome conversion.", "A filled tablet can instead fuel the Energy Depositioner described below; that consumes its cargo permanently.")
            texts[key] = (text, texts[key][1])
        else:
            key = "screen.abyssalport.state_transformer.guide"
            text = texts[key][1].replace("Der Energiedeponierer ist noch nicht implementiert; der Pfad bietet bisher rückgängig machbare Inventarspeicherung, keine Biomkonversion.", "Eine gefüllte Tafel kann stattdessen den unten beschriebenen Energiedeponierer speisen; dabei geht ihr Inhalt endgültig verloren.")
            texts[key] = (texts[key][0], text)
        values = json.loads((ASSETS/("lang/"+locale+".json")).read_text(encoding="utf-8"))
        values.update({key: pair[index] for key, pair in texts.items()})
        resource(ASSETS, "lang/"+locale+".json", values)
    print("Owned tablet + State Transformer art, eight oriented blockstates, exact source crafts and bilingual connected guide.")


if __name__ == "__main__": main()
