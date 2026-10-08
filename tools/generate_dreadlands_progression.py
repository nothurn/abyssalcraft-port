"""Own native media and source-informed Dreadlands book/ritual progression.

Reads only locally authored media. Upstream recipes/Java inform gameplay, not pixels.
"""
import copy
import json
from PIL import Image, ImageDraw, ImageOps
from generate_wasteland_minerals import ROOT, ASSETS, DATA, write, append_tag
from generate_abyssalnite_assets import texture


def tinted(source, dark, light):
    image = Image.open(ASSETS / "textures" / source).convert("RGBA")
    colored = ImageOps.colorize(ImageOps.grayscale(image), dark, light).convert("RGBA")
    colored.putalpha(image.getchannel("A"))
    return colored


def media():
    for name, source in [("dreadlands_essence", "abyssal_wasteland_essence"),
                         ("skin_of_the_dreadlands", "skin_of_the_abyssal_wasteland")]:
        texture("item/"+name+".png", tinted("item/"+source+".png", "#30151b", "#e8a293"))
        write(ASSETS, "models/item/"+name+".json", {"parent": "minecraft:item/generated", "textures": {"layer0": "abyssalport:item/"+name}})
    model = json.loads((ASSETS / "models/item/abyssal_wasteland_necronomicon.json").read_text(encoding="utf-8"))
    for part in ["cover", "spine", "clasp"]:
        texture("item/dreadlands_book_"+part+".png", tinted("item/wasteland_book_"+part+".png", "#1e1218", "#dc9982"))
        model["textures"][part] = "abyssalport:item/dreadlands_book_"+part
    write(ASSETS, "models/item/dreadlands_necronomicon.json", model)
    model = json.loads((ASSETS / "models/item/abyssal_wasteland_staff_of_rending.json").read_text(encoding="utf-8"))
    for part in ["metal", "core"]:
        texture("item/dreadlands_staff_"+part+".png", tinted("item/wasteland_staff_"+part+".png", "#341e23", "#edb294"))
        model["textures"][part] = "abyssalport:item/dreadlands_staff_"+part
    write(ASSETS, "models/item/dreadlands_staff_of_rending.json", model)
    rune = Image.new("RGBA", (16, 16), "#39232c"); draw = ImageDraw.Draw(rune)
    draw.rectangle((1, 1, 14, 14), outline="#9a555b")
    draw.polygon([(8, 3), (12, 8), (8, 12), (3, 8)], outline="#e5a68c")
    draw.line((6, 6, 9, 9), fill="#e5a68c"); draw.line((9, 6, 6, 9), fill="#e5a68c")
    texture("block/dreadlands_ritual_top.png", rune)
    for kind in ["altar", "pedestal"]:
        name = "dreadlands_ritual_"+kind
        model = copy.deepcopy(json.loads((ASSETS / ("models/block/abyssal_ritual_"+kind+".json")).read_text(encoding="utf-8")))
        model["textures"].update({"stone": "abyssalport:block/dreadstone", "rune": "abyssalport:block/dreadlands_ritual_top"})
        write(ASSETS, "models/block/"+name+".json", model)
        write(ASSETS, "blockstates/"+name+".json", {"variants": {"": {"model": "abyssalport:block/"+name}}})
        write(ASSETS, "models/item/"+name+".json", {"parent": "abyssalport:block/"+name})
        write(DATA, "loot_table/blocks/"+name+".json", {"type": "minecraft:block", "pools": [{"rolls": 1,
            "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item", "name": "abyssalport:dreadstone_cobblestone"}]}]})
        append_tag(ROOT / "data/minecraft", "tags/block/mineable/pickaxe.json", ["abyssalport:"+name])


def data():
    for name, kind, pattern, ingredients in [
        ("skin_of_the_dreadlands", "minecraft:crafting_shaped", ["FFF", "FEF", "FFF"],
            {"F": "dread_fragment", "E": "dreadlands_essence"}),
        ("dreadlands_necronomicon", "abyssalport:book_upgrade", ["SS", "SB"],
            {"S": "skin_of_the_dreadlands", "B": "abyssal_wasteland_necronomicon"})]:
        write(DATA, "recipe/"+name+".json", {"type": kind, "category": "misc", "pattern": pattern,
            "key": {key: {"item": "abyssalport:"+value} for key, value in ingredients.items()}, "result": {"id": "abyssalport:"+name}})
        write(DATA, "advancement/recipes/"+name+".json", {"criteria": {"has_material": {"trigger": "minecraft:inventory_changed",
            "conditions": {"items": [{"items": ["abyssalport:"+next(iter(ingredients.values()))]}]}}}, "rewards": {"recipes": ["abyssalport:"+name]}})
    append_tag(DATA, "tags/entity_type/dread_rending_targets.json", ["abyssalport:dreadling"])
    write(DATA, "rituals/dreadlands_staff.json", {"energy_cost": 2000, "duration_ticks": 20,
        "book_tier": 2, "altar_tier": 2, "dimension": "abyssalport:dreadlands", "preserve_components": True,
        "altar_input": "abyssalport:abyssal_wasteland_staff_of_rending",
        "offerings": ["abyssalport:shadow_gem"]+["abyssalport:dreadstone"]*4+["abyssalport:dread_fragment"]*3,
        "result": {"item": "abyssalport:dreadlands_staff_of_rending", "count": 1}})
    write(DATA, "rituals/dreadlands_powerstone.json", {"energy_cost": 5000, "duration_ticks": 60,
        "book_tier": 2, "altar_tier": 2, "dimension": "abyssalport:dreadlands",
        "altar_input": "abyssalport:coralium_infused_stone", "offerings": ["abyssalport:dreadlands_essence"]*8,
        "result": {"item": "abyssalport:dreadlands_infused_powerstone", "count": 1}})
    texts = {
        "item.abyssalport.dreadlands_essence": ["Dreadlands Essence", "Dreadlands-Essenz"],
        "item.abyssalport.skin_of_the_dreadlands": ["Skin of the Dreadlands", "Haut der Dreadlands"],
        "item.abyssalport.dreadlands_necronomicon": ["Dreadlands Necronomicon", "Dreadlands-Necronomicon"],
        "item.abyssalport.dreadlands_staff_of_rending": ["Dreadlands Staff of Rending", "Dreadlands-Stab der Zerrüttung"],
        "block.abyssalport.dreadlands_ritual_altar": ["Dreadlands Ritual Altar", "Dreadlands-Ritualaltar"],
        "block.abyssalport.dreadlands_ritual_pedestal": ["Dreadlands Ritual Pedestal", "Dreadlands-Ritualpodest"],
        "tooltip.abyssalport.rending.dread": ["Dread energy: %s / %s", "Dread-Energie: %s / %s"],
        "research.abyssalport.dreadlands_book": ["Dreadlands Necronomicon", "Dreadlands-Necronomicon"],
        "screen.abyssalport.page.21": ["Dreadlands book and rituals", "Dreadlands-Buch und Rituale"],
        "screen.abyssalport.locked.21": ["Reach the Dreadlands to learn this book upgrade.", "Erreiche die Dreadlands für diese Buchverbesserung."],
        "screen.abyssalport.dreadlands_book": [
            "Drain Dreadlings with a Staff of Rending in the Dreadlands. 100 Dread energy creates one Dreadlands Essence; creatures outside this dimension give no Dread energy. Surround an essence with eight Dread Fragments to craft one Skin of the Dreadlands. Three skins and the Wasteland Necronomicon in a 2 x 2 pattern upgrade the book to 20,000 PE.",
            "Entziehe Dreadlings in den Dreadlands mit einem Zerrüttungsstab Energie. 100 Dread-Energie ergeben eine Dreadlands-Essenz; außerhalb dieser Dimension gibt es keine Dread-Energie. Acht Dread-Fragmente um eine Essenz ergeben eine Dreadlands-Haut. Drei Häute und das Ödland-Necronomicon im 2-x-2-Muster verbessern das Buch auf 20.000 PE."],
        "screen.abyssalport.dreadlands_book_note": [
            "The book upgrade preserves PE, name and other components. The eight fragments must come from actual Dreadling drops. Use the same nine-block ritual pattern, now made of Dreadstone Cobblestone in the Dreadlands, and form it with a tier-two book. A weaker book, a mixed pattern or the wrong dimension changes nothing.",
            "Die Buchverbesserung erhält PE, Namen und weitere Komponenten. Dreadlings liefern die benötigten Fragmente. Forme mit dem Stufe-zwei-Buch in den Dreadlands das bekannte Neun-Block-Muster aus Dreadstone-Bruchstein. Schwächeres Buch, Materialmix oder falsche Dimension verändern nichts."],
        "screen.abyssalport.dreadlands_rituals": [
            "Upgrade the Wasteland Staff on the central altar: one Shadow Gem, four Dreadstone and three Dread Fragments on the pedestals in any order; 2,000 PE in a tier-two book. The new staff drains three per successful hit and keeps all stored channels and its name. A second ritual infuses one Coralium-Infused Stone with eight Dreadlands Essences for 5,000 PE to create another Powerstone. This later alternative cannot replace the first Wasteland Stronghold visit.",
            "Verbessere den Ödland-Stab auf dem Altar: ein Schattenedelstein, vier Dreadstone und drei Dread-Fragmente ungeordnet auf den Podesten; 2.000 PE im Stufe-zwei-Buch. Der neue Stab entzieht drei pro erfolgreichem Treffer und behält alle Ladungen sowie Namen. Ein zweites Ritual infundiert Coralium-Infused Stone mit acht Dreadlands-Essenzen für 5.000 PE zu einem weiteren Powerstone. Dieser spätere Weg ersetzt nicht den ersten Ödland-Festungsbesuch."]}
    for index, locale in enumerate(["en_us", "de_de"]):
        value = json.loads((ASSETS / "lang" / (locale+".json")).read_text(encoding="utf-8"))
        value.update({key: pair[index] for key, pair in texts.items()})
        write(ASSETS, "lang/"+locale+".json", value)


if __name__ == "__main__":
    media(); data()
