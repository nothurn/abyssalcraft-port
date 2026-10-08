"""Owned Depositioner art and connected native recipes; never reads upstream media."""
import copy
import json
import zipfile
from pathlib import Path
from PIL import Image, ImageDraw
from build_pe_models import cube
from generate_abyssalnite_assets import ROOT, ASSETS, DATA, resource, texture
from generate_wasteland_minerals import append_tag


def shapes():
    native = Path(__file__).resolve().parents[1]/"build/moddev/artifacts/neoforge-21.1.234-client-extra-aka-minecraft-resources.jar"
    with zipfile.ZipFile(native) as jar:
        for name, template, surface in (("darkstone_slab", "stone_slab", "darkstone"),
                                        ("darkstone_cobblestone_slab", "cobblestone_slab", "darkstone_cobblestone"),
                                        ("darkstone_cobblestone_stairs", "cobblestone_stairs", "darkstone_cobblestone"),
                                        ("darkstone_cobblestone_wall", "cobblestone_wall", "darkstone_cobblestone")):
            def convert(value):
                if isinstance(value, str):
                    value = value.replace("minecraft:block/"+template, "abyssalport:block/"+name)
                    for material in ("cobblestone", "stone"):
                        if value == "minecraft:block/"+material: value = "abyssalport:block/"+surface
                    return value
                if isinstance(value, dict): return {k: convert(v) for k, v in value.items()}
                if isinstance(value, list): return [convert(v) for v in value]
                return value
            resource(ASSETS, "blockstates/"+name+".json", convert(json.loads(jar.read("assets/minecraft/blockstates/"+template+".json"))))
            base = "assets/minecraft/models/block/"+template
            for path in jar.namelist():
                if path == base+".json" or path.startswith(base+"_") and path.endswith(".json"):
                    model = convert(json.loads(jar.read(path)))
                    # The vanilla double-slab state uses its full-block model, already converted below.
                    resource(ASSETS, "models/block/"+path.rsplit("/", 1)[-1].replace(template, name, 1), model)
            blockstate = json.loads((ASSETS/("blockstates/"+name+".json")).read_text())
            if name.endswith("slab"):
                blockstate["variants"]["type=double"]["model"] = "abyssalport:block/"+surface
                resource(ASSETS, "blockstates/"+name+".json", blockstate)
            resource(ASSETS, "models/item/"+name+".json", {"parent": "abyssalport:block/"+name+("_inventory" if name.endswith("wall") else "")})
            entry = {"type": "minecraft:item", "name": "abyssalport:"+name}
            if name.endswith("slab"):
                entry["functions"] = [{"function": "minecraft:set_count", "count": 2, "conditions": [{"condition": "minecraft:block_state_property", "block": "abyssalport:"+name, "properties": {"type": "double"}}]}]
            resource(DATA, "loot_table/blocks/"+name+".json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [entry], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
            pattern, count = (["###"], 6) if name.endswith("slab") else (["#  ", "## ", "###"], 4) if name.endswith("stairs") else (["###", "###"], 6)
            resource(DATA, "recipe/"+name+".json", {"type": "minecraft:crafting_shaped", "pattern": pattern, "key": {"#": {"item": "abyssalport:"+surface}}, "result": {"id": "abyssalport:"+name, "count": count}})
            resource(DATA, "recipe/"+name+"_stonecutting.json", {"type": "minecraft:stonecutting", "ingredient": {"item": "abyssalport:"+surface}, "result": {"id": "abyssalport:"+name, "count": 2 if name.endswith("slab") else 1}})
            append_tag(ROOT/"data/minecraft", "tags/block/mineable/pickaxe.json", ["abyssalport:"+name])
            append_tag(ROOT/"data/minecraft", "tags/block/"+("slabs" if name.endswith("slab") else "stairs" if name.endswith("stairs") else "walls")+".json", ["abyssalport:"+name])


def main():
    image = Image.new("RGBA", (16, 16), "#24292c"); draw = ImageDraw.Draw(image)
    draw.rectangle((2, 2, 13, 13), outline="#666d68")
    draw.rectangle((4, 4, 11, 11), fill="#111b18")
    draw.line((7, 5, 7, 10, 5, 8), fill="#8bc4a0")
    draw.line((10, 5, 10, 10, 8, 8), fill="#8bc4a0")
    texture("block/energy_depositioner_front.png", image)
    textures = {"stone": "abyssalport:block/pe_stone", "carved": "abyssalport:block/pe_carved", "trim": "abyssalport:block/pe_trim",
                "front": "abyssalport:block/energy_depositioner_front", "rune": "abyssalport:item/stone_tablet", "particle": "#stone"}
    geometry = [cube([1.92, 0, 1.92], [14.08, 2, 14.08], "stone"),
                cube([3, 2, 3], [13, 8, 13], "carved", front="front"),
                cube([2.5, 8, 2.5], [13.5, 10, 13.5], "trim"),
                cube([2, 10, 2], [14, 11, 14], "stone"),
                cube([2, 11, 2], [4, 12, 14], "carved"), cube([12, 11, 2], [14, 12, 14], "carved")]
    for active in (False, True):
        elements = copy.deepcopy(geometry)
        if active: elements.append(cube([5, 11, 4], [11, 12, 12], "stone", "rune"))
        resource(ASSETS, "models/block/energy_depositioner"+("_active" if active else "")+".json", {"textures": textures, "elements": elements})
    variants = {}
    for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for active in (False, True):
            variants[f"active={str(active).lower()},facing={facing}"] = {"model": "abyssalport:block/energy_depositioner"+("_active" if active else ""), "y": rotation}
    resource(ASSETS, "blockstates/energy_depositioner.json", {"variants": variants})
    resource(ASSETS, "models/item/energy_depositioner.json", {"parent": "abyssalport:block/energy_depositioner", "display": {
        "gui": {"rotation": [30, 225, 0], "scale": [.8]*3}, "ground": {"translation": [0, 3, 0], "scale": [.3]*3}}})
    fields = ("PotEnergy", "Items", "ProcessingStack", "ProcessingTime", "Tolerance", "ActiveAmplifier")
    resource(DATA, "loot_table/blocks/energy_depositioner.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "abyssalport:energy_depositioner",
        "functions": [{"function": "minecraft:copy_custom_data", "source": "block_entity", "ops": [{"source": key, "target": key, "op": "replace"} for key in fields]}]}]}]})
    resource(DATA, "recipe/energy_depositioner.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["###", "%&%", "#@#"], "key": {
        "#": {"item": "abyssalport:monolith_stone"}, "%": {"item": "abyssalport:shadow_gem"}, "&": {"item": "abyssalport:transmutation_gem"}, "@": {"item": "abyssalport:energy_relay"}}, "result": {"id": "abyssalport:energy_depositioner"}})
    resource(DATA, "advancement/recipes/energy_depositioner.json", {"criteria": {"has_material": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": ["abyssalport:energy_relay"]}]}}}, "rewards": {"recipes": ["abyssalport:energy_depositioner"]}})
    append_tag(ROOT/"data/minecraft", "tags/block/mineable/pickaxe.json", ["abyssalport:energy_depositioner"])
    shapes()
    texts = {
        "block.abyssalport.energy_depositioner": ("Energy Depositioner", "Energiedeponierer"),
        "block.abyssalport.darkstone_slab": ("Darkstone Slab", "Dunkelsteinstufe"),
        "block.abyssalport.darkstone_cobblestone_slab": ("Darkstone Cobblestone Slab", "Dunkelstein-Bruchsteinstufe"),
        "block.abyssalport.darkstone_cobblestone_stairs": ("Darkstone Cobblestone Stairs", "Dunkelstein-Bruchsteintreppe"),
        "block.abyssalport.darkstone_cobblestone_wall": ("Darkstone Cobblestone Wall", "Dunkelstein-Bruchsteinmauer"),
        "tooltip.abyssalport.energy_depositioner": ("Consumes sealed cargo; can spread Darklands", "Verbraucht versiegelten Inhalt; kann Dunkellande verbreiten"),
        "tooltip.abyssalport.charm_reset": ("Clears a statue or Energy Depositioner modifier; reusable.", "Entfernt den Verstärker einer Statue oder eines Energiedeponierers; wiederverwendbar."),
        "tooltip.abyssalport.charm_use": ("Use on an unmodified statue or Energy Depositioner. Consumes one charm.", "Auf eine Statue oder einen Energiedeponierer ohne Verstärker anwenden. Verbraucht einen Charm."),
        "message.abyssalport.statue_charm": ("PE amplifier: %s", "PE-Verstärker: %s"),
        "screen.abyssalport.energy_depositioner.guide": (
            "Energy Depositioner: five Monolith Stones, two Shadow Gems, one Energy Relay and one use of a Transmutation Gem. WARNING: a filled, non-cursed Stone Tablet is fuel: its sealed cargo is permanently consumed, not returned. Processing takes 10 seconds and generates its cargo value as PE into a 10,000 PE buffer. The empty tablet returns at the right; a full output prevents starting or continuing another job. Place isolated collectors at least three blocks away to receive PE. Power, Range and Duration charms modify the original transfer factors; Blank Charm clears the modifier. Overflow at full capacity is lost. Unprotected transfers can cause disruptions. Processing also attempts to spread Darklands in a 17-by-17 footprint, changing ordinary stone/oak building blocks; existing machines are never replaced. Modern whole 4-by-4 biome cells entirely inside this footprint are used across the actual world height. Unloaded chunks are skipped; protection hooks can veto a cell. More Darklands increases the chance of a cursed output tablet. Use away from builds you want to preserve. Hoppers: filled tablet above, empty output below. Mining retains PE, both slots, modifier and the exact unfinished job in one non-stackable machine item.",
            "Energiedeponierer: fünf Monolithsteine, zwei Schattenedelsteine, ein Energierelais und eine Nutzung eines Transmutationsedelsteins. ACHTUNG: Eine gefüllte, unverfluchte Steintafel ist Brennstoff: Der versiegelte Inhalt wird endgültig verbraucht, nicht zurückgegeben. Die Verarbeitung dauert 10 Sekunden und erzeugt den Inhaltswert als PE im 10.000-PE-Puffer. Rechts kommt die geleerte Tafel heraus; ein voller Ausgang verhindert den Start oder die Fortsetzung weiterer Vorgänge. Isolierte Sammler ab drei Blöcken Entfernung empfangen PE. Kraft-, Reichweiten- und Daueramulette verändern die ursprünglichen Übertragungsfaktoren; ein leeres Amulett entfernt den Effekt. Überlauf bei vollem Puffer geht verloren. Ungeschützte Übertragungen können Störungen auslösen. Die Verarbeitung versucht außerdem, in 17 mal 17 Blöcken Dunkellande zu verbreiten und gewöhnliche Stein-/Eichenbaublöcke zu verändern; vorhandene Maschinen bleiben unberührt. Ganze moderne 4-mal-4-Biomzellen innerhalb der Fläche werden über die tatsächliche Welthöhe verändert. Ungeladene Chunks werden übersprungen; Schutz-Hooks können eine Zelle ablehnen. Mehr Dunkellande erhöht das Risiko einer verfluchten Ausgabetafel. Nicht neben Bauten verwenden, die erhalten bleiben sollen. Trichter: gefüllte Tafel oben, geleerte Ausgabe unten. Beim Abbau bleiben PE, beide Plätze, Amulett und der genaue laufende Vorgang im einzelnen, nicht stapelbaren Maschinengegenstand erhalten.")}
    for index, locale in enumerate(("en_us", "de_de")):
        values = json.loads((ASSETS/("lang/"+locale+".json")).read_text(encoding="utf-8"))
        values.update({key: pair[index] for key, pair in texts.items()})
        guide = "screen.abyssalport.energy_depositioner.guide"
        values[guide] = values[guide].replace("Processing also attempts to spread Darklands", "In the Overworld, processing also attempts to spread Darklands").replace("Die Verarbeitung versucht außerdem", "In der Oberwelt versucht die Verarbeitung außerdem")
        # Also repair the previous stage's factual "not yet implemented" sentence.
        key = "screen.abyssalport.state_transformer.guide"
        values[key] = values[key].replace("The Energy Depositioner is not yet implemented; this path currently provides reversible item storage, not biome conversion.", "A filled tablet can instead fuel the Energy Depositioner described below; that consumes its cargo permanently.").replace("Der Energiedeponierer ist noch nicht implementiert; der Pfad bietet bisher rückgängig machbare Inventarspeicherung, keine Biomkonversion.", "Eine gefüllte Tafel kann stattdessen den unten beschriebenen Energiedeponierer speisen; dabei geht ihr Inhalt endgültig verloren.")
        resource(ASSETS, "lang/"+locale+".json", values)
    print("Owned Depositioner, original craft, four native Darkstone shapes, persistent job loot, bilingual safety guide.")


if __name__ == "__main__": main()
