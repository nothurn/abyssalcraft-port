"""Own altar cuboids/pixels and connected source recipe. No upstream media read."""
import copy
import json
from PIL import Image, ImageDraw
from build_pe_models import cube
from generate_abyssalnite_assets import ROOT, ASSETS, DATA, resource, texture
from generate_wasteland_minerals import append_tag

TIERS = (("", "", ""), ("overworld_", "Overworld ", "Oberwelt-"),
         ("abyssal_wasteland_", "Abyssal Wasteland ", "Abyssales-Ödland-"),
         ("dreadlands_", "Dreadlands ", "Dreadlands-"))


def main():
    # Hand-authored repeating inset; not an original texture or generated photo.
    image = Image.new("RGBA", (16, 16), "#25272b")
    draw = ImageDraw.Draw(image)
    draw.rectangle((1, 1, 14, 14), outline="#44494b")
    draw.rectangle((3, 3, 12, 12), outline="#101417")
    draw.line((4, 7, 7, 7, 7, 4, 8, 4, 8, 11, 11, 11), fill="#ae6659", width=1)
    draw.rectangle((6, 6, 9, 9), fill="#652f30")
    draw.point((7, 6), fill="#e7b3a1")
    texture("block/pe_sacrifice.png", image)
    elements = [cube([2.4, 0, 2.4], [13.6, 2, 13.6], "stone", "carved"),
                cube([3.5, 2, 3.5], [12.5, 3, 12.5], "trim"),
                cube([4.5, 3, 4.5], [11.5, 11.5, 11.5], "carved", "stone"),
                cube([3.2, 11.5, 3.2], [12.8, 12.5, 12.8], "trim"),
                cube([2.4, 12.5, 2.4], [13.6, 13.5, 13.6], "stone", "inset"),
                cube([2.4, 13.5, 2.4], [13.6, 14.4, 3.4], "carved"),
                cube([2.4, 13.5, 12.6], [13.6, 14.4, 13.6], "carved"),
                cube([2.4, 13.5, 3.4], [3.4, 14.4, 12.6], "carved"),
                cube([12.6, 13.5, 3.4], [13.6, 14.4, 12.6], "carved")]
    names = []
    texts = {}
    for prefix, english, german in TIERS:
        name = prefix+"sacrificial_altar"; names.append("abyssalport:"+name)
        model = {"textures": {key: "abyssalport:block/"+prefix+source for key, source in
                              (("stone", "pe_stone"), ("carved", "pe_carved"), ("trim", "pe_trim"))},
                 "elements": copy.deepcopy(elements)}
        model["textures"].update({"inset": "abyssalport:block/pe_sacrifice", "particle": "#stone"})
        resource(ASSETS, "models/block/"+name+".json", model)
        resource(ASSETS, "models/item/"+name+".json", {"parent": "abyssalport:block/"+name, "display": {
            "gui": {"rotation": [30, 225, 0], "scale": [.8]*3},
            "ground": {"translation": [0, 3, 0], "scale": [.3]*3}, "fixed": {"scale": [.5]*3}}})
        resource(ASSETS, "blockstates/"+name+".json", {"variants": {"": {"model": "abyssalport:block/"+name}}})
        resource(DATA, "loot_table/blocks/"+name+".json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:item", "name": "abyssalport:"+name, "functions": [{"function": "minecraft:copy_custom_data",
            "source": "block_entity", "ops": [{"source": field, "target": field, "op": "replace"}
                                                for field in ("PotEnergy", "CollectionLimit", "CoolDown")]}]}]}]})
        texts["block.abyssalport."+name] = (english+"Sacrificial Altar", german+"Opferaltar")
    resource(DATA, "recipe/sacrificial_altar.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["#%#", "&$&", "&&&"], "key": {"#": {"item": "abyssalport:shadow_fragment"},
        "%": {"item": "abyssalport:coralium_pearl"}, "$": {"item": "abyssalport:shadow_gem"},
        "&": {"item": "abyssalport:monolith_stone"}}, "result": {"id": "abyssalport:sacrificial_altar"}})
    resource(DATA, "advancement/recipes/sacrificial_altar.json", {"criteria": {"has_material": {"trigger": "minecraft:inventory_changed",
        "conditions": {"items": [{"items": ["abyssalport:shadow_gem"]}]}}}, "rewards": {"recipes": ["abyssalport:sacrificial_altar"]}})
    for tag in ("mineable/pickaxe", "needs_stone_tool"):
        append_tag(ROOT/"data/minecraft", "tags/block/"+tag+".json", names)
    texts.update({"message.abyssalport.sacrificial_altar_status": (
        "Sacrificial Altar: %s / %s PE · Cooldown: %s s · Targets: %s / %s",
        "Opferaltar: %s / %s PE · Abklingzeit: %s s · Ziele: %s / %s"),
        "screen.abyssalport.sacrificial_altar": (
        "Sacrificial Altar: five Monolith Stones, two Shadow Fragments, one Coralium Pearl and one Shadow Gem. Tracks adult living mobs within 8 blocks horizontally and 3 vertically; undead and Shadow creatures are excluded. A tracked, accepted death yields its maximum health as PE, never a removed living creature or cancelled death. Place a book on top to charge it at 20 PE/tick. After collecting one fifth of capacity, cooldown begins: base 60 s, Overworld 50 s, Wasteland 40 s, Dreadlands 30 s. Capacities: 5,000 / 7,500 / 10,000 / 12,500 PE; targets: 1 / 2 / 3 / 4. Ring upgrades preserve PE and cooldown. No placed altar accepts external PE; its Relay output works. The base altar item cannot be charged, higher-tier items can. Breaking returns the displayed item separately.",
        "Opferaltar: fünf Monolithsteine, zwei Schattenfragmente, eine Coralium-Perle und ein Schattenedelstein. Erfasst erwachsene lebende Mobs in 8 Blöcken horizontaler und 3 Blöcken vertikaler Reichweite, außer Untoten und Schattenwesen. Ein erfasster, akzeptierter Tod liefert maximale HP als PE; entfernte lebende Wesen oder abgebrochene Todesereignisse nicht. Buch auflegen: 20 PE/Tick. Nach einem Fünftel der Kapazität beginnt die Abklingzeit: Basis 60 s, Oberwelt 50 s, Ödland 40 s, Dreadlands 30 s. Kapazitäten: 5.000 / 7.500 / 10.000 / 12.500 PE; Ziele: 1 / 2 / 3 / 4. Ring-Upgrades erhalten PE und Cooldown. Platzierte Altäre nehmen keine externe PE an, können sie aber an Relais abgeben. Als Item ist nur der Basisaltar nicht aufladbar. Abbau gibt das aufgelegte Item separat zurück.")})
    for index, locale in enumerate(("en_us", "de_de")):
        values=json.loads((ASSETS/("lang/"+locale+".json")).read_text(encoding="utf-8"))
        values.update({key: pair[index] for key, pair in texts.items()})
        resource(ASSETS, "lang/"+locale+".json", values)
    print("Four connected altar tiers: owned cuboids/pixels, canonical craft, native persisted-counter loot and bilingual guide.")


if __name__ == "__main__": main()
