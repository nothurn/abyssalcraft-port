"""Own dragon UV atlas, Eye icon and source-informed Asorah summon/progression data.

No upstream model, texture, sound or other media is read. Coordinates match our
articulated AbyssalDragonModel. XP waves are consolidated into one native payout;
the source's accidental per-XP-orb item loop is not reproduced.
"""
import json
import random
from PIL import Image, ImageDraw
from generate_wasteland_minerals import ROOT, ASSETS, DATA, write, append_tag
from generate_abyssalnite_assets import texture


def dragon_atlas(name, base, accent):
    image = Image.new("RGBA", (256, 128))
    rng = random.Random(18841)
    draw = ImageDraw.Draw(image)
    def box(u, v, w, h, d):
        faces = [(u+d, v, w, d), (u+d+w, v, w, d),
                 (u, v+d, d, h), (u+d, v+d, w, h),
                 (u+d+w, v+d, d, h), (u+2*d+w, v+d, w, h)]
        for x, y, width, height in faces:
            for py in range(y, y+height):
                for px in range(x, x+width):
                    noise = rng.randrange(-7, 8)
                    image.putpixel((px, py), tuple(max(0, min(255, c+noise)) for c in base)+(255,))
            draw.rectangle((x, y, x+width-1, y+height-1), outline=tuple(max(0, c-10) for c in base)+(255,))
            for row in range(y+2, y+height-1, 4):
                for col in range(x+2+(row//4%2), x+width-1, 5):
                    draw.line((col, row, min(col+2, x+width-2), row), fill=accent)
    for cube in [(0, 0, 12, 10, 24), (80, 0, 8, 8, 8), (116, 0, 10, 8, 12), (180, 0, 3, 6, 3),
                 (0, 40, 10, 3, 12), (80, 40, 28, 1, 18), (80, 62, 20, 1, 14),
                 (0, 64, 6, 6, 16), (0, 90, 4, 4, 15), (180, 40, 4, 8, 4), (200, 40, 5, 2, 8)]:
        box(*cube)
    # Bone ridges on the wing membranes and jaw; front-face eyes are exact model UVs.
    for u, v, w, d in [(80, 40, 28, 18), (80, 62, 20, 14)]:
        for edge in [u+d, u+d+w]:
            draw.line((edge, v, edge+w-1, v+d-1), fill=accent, width=1)
            draw.line((edge, v+d-1, edge+w-1, v+d-1), fill=accent, width=1)
    draw.rectangle((129, 14, 130, 15), fill="#d6edb5")
    draw.rectangle((135, 14, 136, 15), fill="#d6edb5")
    draw.line((130, 18, 135, 18), fill="#152b29")
    for x in range(13, 22, 2): draw.point((x, 52), fill="#cfddae")
    texture("entity/"+name+".png", image)


def media():
    dragon_atlas("asorah", (27, 46, 53), "#587c69")
    dragon_atlas("spectral_dragon", (70, 105, 108), "#a3c4a6")
    image = Image.new("RGBA", (16, 16)); draw = ImageDraw.Draw(image)
    draw.polygon([(1, 7), (4, 3), (8, 2), (12, 4), (15, 8), (11, 12), (7, 13), (3, 10)], fill="#376658", outline="#172d2d")
    draw.polygon([(3, 7), (6, 4), (10, 4), (13, 8), (10, 10), (6, 10)], fill="#a5c68e")
    draw.rectangle((7, 4, 9, 10), fill="#142525")
    draw.line((7, 5, 7, 8), fill="#436e68"); draw.point((6, 5), fill="#e6edc0")
    texture("item/eye_of_the_abyss.png", image)
    write(ASSETS, "models/item/eye_of_the_abyss.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "abyssalport:item/eye_of_the_abyss"}})
    write(ASSETS, "models/item/spectral_dragon_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})


def data():
    write(DATA, "rituals/summon_asorah.json", {"outcome": "summon", "entity": "abyssalport:asorah",
        "energy_cost": 1000, "duration_ticks": 20, "book_tier": 1, "altar_tier": 1,
        "dimension": "abyssalport:abyssal_wasteland", "offerings": ["minecraft:gold_ingot"]*4
        + ["abyssalport:transmutation_gem", "abyssalport:coralium_bucket", "minecraft:enchanting_table"]})
    def entry(name, count=1):
        return {"type": "minecraft:item", "name": name, "functions": [{"function": "minecraft:set_count", "count": count}]}
    write(DATA, "loot_table/entities/asorah.json", {"type": "minecraft:entity", "pools": [{"rolls": 1, "entries": [entry(name, count)]}
        for name, count in [("abyssalport:eye_of_the_abyss", 1), ("abyssalport:coralium_chunk", 2),
                            ("abyssalport:refined_coralium_ingot", 2), ("abyssalport:coralium_plagued_flesh", 2)]]})
    write(DATA, "loot_table/entities/spectral_dragon.json", {"type": "minecraft:entity", "pools": [{"rolls": 1,
        "entries": [{"type": "minecraft:item", "name": "abyssalport:coralium_plagued_flesh", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}},
            {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]}]})
    append_tag(DATA, "tags/entity_type/coralium_immune.json", ["abyssalport:asorah", "abyssalport:spectral_dragon"])
    append_tag(ROOT / "data/minecraft", "tags/entity_type/can_breathe_under_water.json", ["abyssalport:asorah", "abyssalport:spectral_dragon"])
    for name in ["abyssal_wasteland", "coralium_lake", "abyssal_plateau"]:
        value = json.loads((DATA / "worldgen/biome" / (name+".json")).read_text(encoding="utf-8"))
        if not any(entry["type"] == "abyssalport:spectral_dragon" for entry in value["spawners"]["monster"]):
            value["spawners"]["monster"].append({"type": "abyssalport:spectral_dragon", "weight": 1, "minCount": 1, "maxCount": 1})
        write(DATA, "worldgen/biome/"+name+".json", value)
    texts = {
        "entity.abyssalport.asorah": ["Asorah, the Fallen", "Asorah, der Gefallene"],
        "entity.abyssalport.spectral_dragon": ["Spectral Dragon", "Spektraldrache"],
        "item.abyssalport.spectral_dragon_spawn_egg": ["Spectral Dragon Spawn Egg", "Spektraldrachen-Spawn-Ei"],
        "item.abyssalport.eye_of_the_abyss": ["Eye of the Abyss", "Auge des Abgrunds"],
        "research.abyssalport.asorah": ["Asorah defeated", "Asorah bezwungen"],
        "message.abyssalport.summon_blocked": ["The creature could not appear. Clear the space above the altar and check the difficulty. Your PE and offerings were retained.",
            "Die Kreatur konnte nicht erscheinen. Räume über dem Altar frei und prüfe den Schwierigkeitsgrad. PE und Opfergaben bleiben erhalten."],
        "screen.abyssalport.page.17": ["Asorah, the Fallen", "Asorah, der Gefallene"],
        "screen.abyssalport.locked.17": ["Craft the Wasteland Necronomicon to study Asorah's summoning.", "Fertige das Ödland-Necronomicon, um Asorahs Beschwörung zu erforschen."],
        "screen.abyssalport.asorah": ["Prepare an Abyssal ritual ground in the Abyssal Wasteland. Leave the central altar empty; place four Gold Ingots, one Transmutation Gem, one Liquid Coralium Bucket and one Enchanting Table on seven pedestals in any order. Leave the eighth empty. Use a tier-one Necronomicon with 1,000 PE. Asorah appears above the altar after one second; a blocked or refused spawn consumes nothing. Peaceful difficulty cannot summon him.",
            "Bereite im Abyssalen Ödland einen abyssalen Ritualplatz vor. Mitte leer lassen; vier Goldbarren, einen Transmutationsedelstein, einen Flüssig-Coralium-Eimer und einen Zaubertisch beliebig auf sieben Podeste legen. Das achte bleibt leer. Stufe-eins-Necronomicon mit 1.000 PE benutzen. Asorah erscheint nach einer Sekunde über dem Altar. Blockierte/verweigerte Spawns verbrauchen nichts; Friedlich verhindert die Beschwörung."],
        "screen.abyssalport.asorah_fight": ["Asorah has 400 health and alternates airborne sweeps with lower approaches. Aim for the head: body, tail and wing hits are reduced. Nearby Spectral Dragons heal him by giving up their own health, so remove them first. He rejects potion effects and ordinary mob damage. After defeat, his ten-second death sequence releases one Eye of the Abyss, Coralium materials and experience. The Eye records your discovery when collected. Drops obey doMobLoot.",
            "Asorah hat 400 Lebenspunkte und wechselt zwischen Flugkreisen und tiefen Anflügen. Ziele auf den Kopf; Treffer an Körper, Schwanz und Flügeln sind schwächer. Nahe Spektraldrachen heilen ihn aus ihrem eigenen Leben: beseitige sie zuerst. Tränke und normale Mob-Angriffe wirken nicht. Nach dem Sieg gibt die zehnsekündige Todessequenz ein Auge des Abgrunds, Coralium-Materialien und Erfahrung frei. Aufnehmen des Auges speichert die Entdeckung. Drops beachten doMobLoot."]}
    for index, locale in enumerate(["en_us", "de_de"]):
        value = json.loads((ASSETS / "lang" / (locale+".json")).read_text(encoding="utf-8"))
        value.update({key: pair[index] for key, pair in texts.items()})
        write(ASSETS, "lang/"+locale+".json", value)


if __name__ == "__main__":
    media(); data()
