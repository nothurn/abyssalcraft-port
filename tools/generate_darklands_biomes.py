"""Four connected native Overworld variants; own existing assets, no upstream media.

Biome mechanics are informed by original Forest/Plains/Hills/Mountains Java.
Native 1.21 erosion/noise regions replace legacy fixed biome height fields.
"""
import copy
import json
from generate_crystallizer import ASSETS, DATA, write, append_tag


def generate():
    base=json.loads((DATA/"worldgen/biome/darklands.json").read_text(encoding="utf-8"))
    names=["darklands_plains","darklands_forest","darklands_highlands","darklands_mountains"]
    write(DATA,"worldgen/configured_feature/sparse_darklands_oak.json",{"type":"abyssalport:sparse_darklands_oak","config":{"probability":.08}})
    placed=json.loads((DATA/"worldgen/placed_feature/darklands_oak.json").read_text(encoding="utf-8"))
    placed["feature"]="abyssalport:sparse_darklands_oak"; placed["placement"][0]["count"]=1
    write(DATA,"worldgen/placed_feature/sparse_darklands_oak.json",placed)
    for name in names:
        biome=copy.deepcopy(base)
        biome["features"][9]=[f for f in biome["features"][9] if f!="abyssalport:darklands_oak"]
        if name.endswith("forest"): biome["features"][9].insert(1,"abyssalport:darklands_oak")
        elif not name.endswith("mountains"): biome["features"][9].insert(1,"abyssalport:sparse_darklands_oak")
        if name.endswith("mountains"):
            biome["temperature"]=.35; biome["downfall"]=.65
            biome["features"][9]=[f for f in biome["features"][9] if f not in ["minecraft:forest_flowers","minecraft:flower_default","minecraft:patch_grass_forest"]]
        write(DATA,"worldgen/biome/"+name+".json",biome)
    for modifier in ["abandoned_monolith","shadow_creature_spawns","lesser_shoggoth_spawns"]:
        value=json.loads((DATA/"neoforge/biome_modifier"/(modifier+".json")).read_text(encoding="utf-8"))
        for name in names:
            if "abyssalport:"+name not in value["biomes"]: value["biomes"].append("abyssalport:"+name)
        write(DATA,"neoforge/biome_modifier/"+modifier+".json",value)
    append_tag(DATA,"tags/worldgen/biome/darklands.json",["abyssalport:darklands"]+["abyssalport:"+name for name in names])
    translations=[("Darklands Plains","Dunkellande-Ebenen"),("Darklands Forest","Dunkellande-Wald"),("Darklands Highlands","Dunkellande-Hochland"),("Darklands Mountains","Dunkellande-Berge")]
    for index,locale in enumerate(["en_us","de_de"]):
        lang=json.loads((ASSETS/"lang"/(locale+".json")).read_text(encoding="utf-8"))
        lang.update({"biome.abyssalport."+name:pair[index] for name,pair in zip(names,translations)})
        lang["screen.abyssalport.darklands.variants"]=[
            "Darklands Plains and Highlands have sparse Oaks; the Forest has dense Oaks. Highlands have Darkstone under their soil; Mountains expose Darkstone on top and contain no trees. All variants retain the early ore and monolith paths and unlock the same Darklands discovery. The older Darklands biome key is retained for saves. Native climate and erosion place these variants in compatible terrain; old 1.12 height fields are not copied. Original buildings, lairs and the remaining creature ecology are still separate development.",
            "Dunkellande-Ebenen und Hochland besitzen wenige Eichen; der Wald ist dicht bewachsen. Unter dem Hochland-Boden liegt Dunkelstein; die Berge zeigen Dunkelstein an der Oberfläche und enthalten keine Bäume. Alle Varianten behalten die frühen Erz-/Monolith-Pfade und schalten dieselbe Dunkellande-Entdeckung frei. Der bisherige Darklands-Biomschlüssel bleibt für Saves erhalten. Native Klima-/Erosionsbereiche platzieren die Varianten in passendem Terrain, statt alte 1.12-Höhenfelder zu kopieren. Gebäude, Lairs und die übrige Kreaturenökologie bleiben getrennte Entwicklungsarbeit."][index]
        write(ASSETS,"lang/"+locale+".json",lang)


if __name__=="__main__": generate()
