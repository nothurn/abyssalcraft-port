"""Owned Shadow-family atlases and native gameplay metadata. No upstream media read.

Run after generate_darklands_biomes.py: retain the old rare vanilla fallback,
but give the real Darklands their complete source-native family weights.
"""
import json
from pathlib import Path
import random
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
DATA = ROOT / "data/abyssalport"
ASSETS = ROOT / "assets/abyssalport"

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False)+"\n", encoding="utf-8")

def append(path, entries):
    value = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
    for entry in entries:
        if entry not in value["values"]: value["values"].append(entry)
    write(path, value)

def atlas(name, beast):
    rng = random.Random(513 if beast else 512)
    image = Image.new("RGBA", (128,128), (0,0,0,0))
    draw = ImageDraw.Draw(image)
    head, width, height, armw, armh, upper, lower = (10,12,18,4,18,8,6) if beast else (8,8,10,3,12,6,4)
    cubes = [(0,0,head,head,head), (0,24,width,height,6), (48,24,armw,armh,4),
             (72,24,armw,armh,4), (0,56,4,upper,4), (24,56,4,upper,4),
             (48,56,4,lower,4), (72,56,4,lower,4)]
    if beast: cubes.append((104,24,1,4,1))
    for u,v,w,h,d in cubes:
        # Full native box net; discrete low-contrast facets, not noise-over-a-render.
        for y in range(v,v+d+h):
            for x in range(u,u+2*(w+d)):
                tone = 25+rng.randrange(10)+(x//3+y//4)%4
                image.putpixel((x,y),(tone,tone,tone+4,255))
    # Exact front head face is at (depth,depth), not the neighbouring side UV.
    d=head
    for x in [d+2,d+head-3]:
        draw.rectangle((x,d+3,x+1,d+4),fill=(238,206,153,255))
        draw.point((x,d+3),fill=(255,232,189,255))
    draw.line((6,30,6+width-1,30),fill=(45,43,50,255))
    path=ASSETS/"textures/entity"/(name+".png"); path.parent.mkdir(parents=True,exist_ok=True); image.save(path)

def generate():
    atlas("shadow_monster",False); atlas("shadow_beast",True)
    family=["abyssalport:"+name for name in ["shadow_creature","shadow_monster","shadow_beast","shadow_ghoul"]]
    append(ROOT/"data/minecraft/tags/entity_type/can_breathe_under_water.json",family[:3])
    append(DATA/"tags/entity_type/shadow_rending_targets.json",family)
    write(DATA/"damage_type/shadow.json", {"message_id":"shadow","exhaustion":.1,"scaling":"never"})
    for tag in ["bypasses_armor","bypasses_effects","bypasses_enchantments","bypasses_resistance","bypasses_shield"]:
        append(ROOT/"data/minecraft/tags/damage_type"/(tag+".json"),["abyssalport:shadow"])
    for name,item in [("shadow_monster","shadow_shard"),("shadow_beast","shadow_gem")]:
        write(DATA/"loot_table/entities"/(name+".json"),{"type":"minecraft:entity","pools":[{"rolls":1,"entries":[{
            "type":"minecraft:item","name":"abyssalport:"+item,"functions":[
            {"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":0,"max":2}},
            {"function":"minecraft:enchanted_count_increase","enchantment":"minecraft:looting",
             "count":{"type":"minecraft:uniform","min":0,"max":1}}]}]}]})
    fallback=DATA/"neoforge/biome_modifier/shadow_creature_spawns.json"
    value=json.loads(fallback.read_text(encoding="utf-8"))
    value["biomes"]=[biome for biome in value["biomes"] if not biome.startswith("abyssalport:darklands")]
    write(fallback,value)
    names=["darklands","darklands_plains","darklands_forest","darklands_highlands","darklands_mountains"]
    for biome in names:
        weights=[100,80,40] if biome.endswith("mountains") else [70,50,20]
        entries=[{"type":family[i],"weight":weight,"minCount":count,"maxCount":count}
                 for i,(weight,count) in enumerate(zip(weights,[3,2,1]))]
        write(DATA/"neoforge/biome_modifier"/("shadow_family_"+biome+".json"),{
            "type":"neoforge:add_spawns","biomes":["abyssalport:"+biome],"spawners":entries})
    for mob in family[:3]:
        name=mob.split(":")[1]
        write(ASSETS/"models/item"/(name+"_spawn_egg.json"),{"parent":"minecraft:item/template_spawn_egg"})
    for locale,titles,guide in [
        ("en_us",["Shadow Creature","Shadow Monster","Shadow Beast"],
         "Darklands host three Shadow forms: Creatures drop 1–3 Fragments; Monsters can drop 0–2 Shards; Beasts can drop 0–2 Gems. All breathe underwater and resist fire, poison and attacks from their own Shadow family. Beasts gather breath after 300 active ticks, then exhale for 100 ticks. Close-range Shadow breath bypasses armour and adds blindness and slowness; walls stop it. Deaths caused by Shadows can transform non-Shadow creatures; player deaths leave a named persistent Monster while normal player respawn and inventory rules remain intact. A transformation refused by native protection creates no replacement. All three forms and Shadow Ghouls supply the Staff of Rending's Shadow channel."),
        ("de_de",["Schattenkreatur","Schattenmonster","Schattenbestie"],
         "In den Dunkellanden leben drei Schattenformen: Kreaturen hinterlassen 1–3 Fragmente, Monster können 0–2 Splitter und Bestien 0–2 Edelsteine geben. Alle atmen unter Wasser und widerstehen Feuer, Gift und Angriffen ihrer Schattenfamilie. Bestien sammeln 300 aktive Ticks lang Atem und atmen dann 100 Ticks aus. Schattenatem wirkt aus kurzer Entfernung durch Rüstung und verursacht Blindheit sowie Verlangsamung; Wände stoppen ihn. Schattentode können andere Kreaturen verwandeln. Spielertode hinterlassen ein benanntes, dauerhaftes Monster; Respawn und Inventarregeln bleiben nativ. Verweigert nativer Schutz die Verwandlung, entsteht kein Ersatz. Alle drei Formen und Schattenghouls laden den Schattenkanal des Zerstückelungsstabs.")]:
        path=ASSETS/"lang"/(locale+".json"); lang=json.loads(path.read_text(encoding="utf-8"))
        for mob,title in zip(family[:3],titles):
            name=mob.split(":")[1];lang["entity.abyssalport."+name]=title
            lang["item.abyssalport."+name+"_spawn_egg"]=title+(" Spawn Egg" if locale=="en_us" else "-Spawn-Ei")
        lang["death.attack.shadow"]="%1$s was consumed by shadow" if locale=="en_us" else "%1$s wurde von Schatten verschlungen"
        lang["death.attack.shadow.player"]="%1$s was consumed by %2$s's shadow" if locale=="en_us" else "%1$s wurde von %2$s' Schatten verschlungen"
        lang["screen.abyssalport.shadow_family.guide"]=guide
        write(path,lang)

if __name__ == "__main__": generate()
