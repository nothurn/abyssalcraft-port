"""Owned species UV atlases and actual native ecology metadata. Never reads upstream media."""
import json
import random
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = ROOT / "assets/abyssalport"
DATA = ROOT / "data/abyssalport"
KINDS = ("cow", "chicken", "pig", "sheep")

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

def append(path, entries):
    value = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
    for entry in entries:
        if entry not in value["values"]: value["values"].append(entry)
    write(path, value)

def nets(kind):
    if kind == "chicken":
        return [(0,0,4,5,4),(40,0,4,2,2),(56,0,2,2,2),(0,32,6,6,6),
                (64,32,1,5,6),(80,32,1,5,6),(0,64,1,3,1),(24,64,1,3,1),(48,64,3,1,4)]
    head = (8,8,6) if kind == "cow" else (6,6,7 if kind == "pig" else 6)
    values = [(0,0,*head),(40,0,4,3,2),(56,0,3,2,2),
              (0,32,10,7 if kind == "pig" else 10,14 if kind == "pig" else 16)]
    values += [(i*24,64,3,6 if kind == "pig" else 10,3) for i in range(4)]
    if kind == "cow": values.append((72,0,1,3,1))
    if kind == "pig": values.append((88,0,2,2,3))
    return values

def atlas(name, kind, demon):
    rng = random.Random(860 + KINDS.index(kind) + (20 if demon else 0))
    image = Image.new("RGBA", (128,128), (0,0,0,0)); draw = ImageDraw.Draw(image)
    palette = {"cow": (104,76,61), "chicken": (194,183,168), "pig": (177,119,108), "sheep": (205,196,182)}
    base = (97,51,46) if demon else palette[kind]
    for u,v,w,h,d in nets(kind):
        for y in range(v,v+d+h):
            for x in range(u,u+2*(w+d)):
                shift = rng.randrange(-7,8) + (x//3+y//3)%3
                color = tuple(max(0,min(255,c+shift)) for c in base)
                if kind == "cow" and not demon and ((x//4)*3+(y//3)*5)%7 < 2: color=(172+shift,157+shift,134+shift)
                if demon and (x+2*y)%13==0: color=(126,65,54)
                image.putpixel((x,y), (*color,255))
    w,h,d = nets(kind)[0][2:]
    # Native front-face UV: depth,depth. Keep distinct eyes visible at the real head face.
    for x in (d+1,d+w-2): draw.point((x,d+2),fill=(237,71,43,255))
    draw.line((d+1,d+h-2,d+w-2,d+h-2),fill=(64,32,29,255))
    if kind == "chicken":
        draw.rectangle((40,0,51,3), fill=(169,118,53,255));draw.rectangle((56,0,63,3),fill=(121,43,34,255))
    if kind == "cow": draw.rectangle((72,0,75,3),fill=(187,171,124,255))
    if demon:
        # Discrete seams/wounds, not a copied original cow/pig skin.
        draw.line((20,51,23,53,25,50),fill=(166,54,41,255),width=1)
    path=ASSETS/"textures/entity"/(name+".png");path.parent.mkdir(parents=True,exist_ok=True);image.save(path)

def offspring_atlas():
    image=Image.new("RGBA",(128,128),(0,0,0,0));rng=random.Random(890);draw=ImageDraw.Draw(image)
    cubes=[(0,0,8,8,8),(0,24,10,16,8),(104,56,3,1,4),(104,24,1,5,1)]
    cubes += [(i*24,56,3,7,3) for i in range(4)] + [(i*24,80,2,5,2) for i in range(4)]
    cubes += [(48+i*12,0,1,8,1) for i in range(6)]
    for u,v,w,h,d in cubes:
        for y in range(v,v+d+h):
            for x in range(u,u+2*(w+d)):
                shade=rng.randrange(9)+(x//2+y//4)%4
                image.putpixel((x,y),(53+shade,41+shade,40+shade,255))
    for x in (9,11,13,15): draw.point((x,11),fill=(221,186,120,255))
    draw.line((10,14,14,14),fill=(109,66,51,255))
    image.save(ASSETS/"textures/entity/shub_offspring.png")

def loot_pool(item, minimum, maximum, looting=True):
    functions=[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":minimum,"max":maximum}}]
    if looting: functions.append({"function":"minecraft:enchanted_count_increase","enchantment":"minecraft:looting","count":{"type":"minecraft:uniform","min":0,"max":1}})
    return {"rolls":1,"entries":[{"type":"minecraft:item","name":"minecraft:"+item,"functions":functions}]}

def generate():
    demons=[];animals=[]
    evil_loot={"cow":[("leather",0,2,True),("beef",1,3,True)],"chicken":[("feather",0,2,True),("chicken",1,1,True)],
               "pig":[("porkchop",1,3,True)],"sheep":[("white_wool",1,1,False),("mutton",1,2,True)]}
    for demon in (False,True):
        for kind in KINDS:
            name=("demon_" if demon else "evil_")+kind; animals.append("abyssalport:"+name)
            if demon: demons.append("abyssalport:"+name)
            atlas(name,kind,demon)
            write(ASSETS/"models/item"/(name+"_spawn_egg.json"),{"parent":"minecraft:item/template_spawn_egg"})
            entries=[("rotten_flesh",0,2,True)] if demon else evil_loot[kind]
            write(DATA/"loot_table/entities"/(name+".json"),{"type":"minecraft:entity","pools":[loot_pool(*entry) for entry in entries]})
    offspring_atlas()
    write(ASSETS/"models/item/shub_offspring_spawn_egg.json",{"parent":"minecraft:item/template_spawn_egg"})
    write(DATA/"loot_table/entities/shub_offspring.json",{"type":"minecraft:entity","pools":[]})
    append(DATA/"tags/entity_type/dread_immune.json",demons);append(DATA/"tags/entity_type/dread_rending_targets.json",demons)
    append(DATA/"tags/entity_type/shoggoth_food.json",animals)
    append(ROOT/"data/minecraft/tags/block/fire.json",["abyssalport:mimic_fire"])
    # Actual vanilla FireBlock states and native Minecraft models, no reserved upstream flame texture.
    no_sides={direction:"false" for direction in ("north","east","south","west","up")}
    parts=[{"when":no_sides,"apply":[{"model":"minecraft:block/fire_floor0"},{"model":"minecraft:block/fire_floor1"}]}]
    for direction,rotation in [("north",0),("east",90),("south",180),("west",270)]:
        parts.append({"when":{"OR":[{direction:"true"},no_sides]},"apply":[{"model":"minecraft:block/fire_side0","y":rotation}, {"model":"minecraft:block/fire_side1","y":rotation}]})
    parts.append({"when":{"up":"true"},"apply":[{"model":"minecraft:block/fire_up0"},{"model":"minecraft:block/fire_up1"}]})
    write(ASSETS/"blockstates/mimic_fire.json",{"multipart":parts})
    write(DATA/"loot_table/blocks/mimic_fire.json",{"type":"minecraft:block","pools":[]})
    modifiers=[("evil_animals","evil",False,["minecraft:"+name for name in
       ("taiga","plains","forest","savanna","beach","windswept_hills","jungle","savanna_plateau","swamp","snowy_plains","birch_forest","dark_forest")]),
       ("demon_animals","demon",False,"#minecraft:is_nether"),
       ("shub_forests","offspring",False,["minecraft:"+name for name in ("forest","old_growth_pine_taiga","old_growth_spruce_taiga","old_growth_birch_forest","flower_forest","birch_forest")]),
       ("shub_dark_forests","offspring",True,["minecraft:dark_forest","abyssalport:darklands_forest"])]
    for name,group,dense,biomes in modifiers: write(DATA/"neoforge/biome_modifier"/(name+".json"),{"type":"abyssalport:animal_ecology_spawns","biomes":biomes,"group":group,"dense":dense})
    guides={"en_us":"Evil cows, chickens, pigs and sheep hunt players. By default they appear only at night during a new moon. Shearing an Evil animal yields 1–3 leather, feathers, pork or wool and admits its matching Demon form; rejected transformations spend neither tool durability nor drops. Evil sheep that killed a player wear that player's skin and yield rotten flesh instead. Accepted Evil deaths normally leave a Demon; Shoggoth kills do not. Demons drop 0–2 rotten flesh and resist Dread Plague. They spawn in the Nether; Overworld burning is disabled by default. Enabled Demon trails respect mobGriefing and native block protection; water/rain extinguish them. Mimic Fire spreads less than normal fire, but still burns flammable blocks. Shub offspring appear above sea level on eligible nights in forests, more often in dark forests; they have 40 health and no item loot. Common ecology options are in abyssalport-common.toml; biome weights apply when the world loads.",
        "de_de":"Böse Kühe, Hühner, Schweine und Schafe jagen Spieler. Standardmäßig erscheinen sie nur nachts bei Neumond. Scheren liefert 1–3 Leder, Federn, Schweinefleisch oder Wolle und erzeugt die passende Dämonform; verweigerte Verwandlungen verbrauchen weder Werkzeughaltbarkeit noch Drops. Böse Schafe, die einen Spieler getötet haben, tragen dessen Skin und geben stattdessen verrottetes Fleisch. Akzeptierte Evil-Tode hinterlassen normalerweise einen Dämon; Shoggoth-Tode nicht. Dämonen geben 0–2 verrottetes Fleisch und widerstehen der Dread-Seuche. Sie erscheinen im Nether; Oberweltfeuer ist standardmäßig aus. Aktivierte Feuerspuren beachten mobGriefing und nativen Blockschutz; Wasser/Regen löschen sie. Mimic Fire breitet sich schwächer als normales Feuer aus, verbrennt aber brennbare Blöcke. Shub-Nachkommen erscheinen über dem Meeresspiegel in geeigneten Waldnächten, häufiger in dunklen Wäldern; sie haben 40 Leben und keine Itemdrops. Optionen stehen in abyssalport-common.toml; Biomgewichte gelten beim Laden der Welt."}
    labels={"en_us":("Evil","Demon",("Cow","Chicken","Pig","Sheep"),"Shub-Niggurath's Offspring","Mimic Fire"),
            "de_de":("Böses","Dämonen",("Rind","Huhn","Schwein","Schaf"),"Shub-Nigguraths Nachkomme","Mimic-Feuer")}
    for locale,values in labels.items():
        path=ASSETS/"lang"/(locale+".json");lang=json.loads(path.read_text(encoding="utf-8"));evil,demon,kinds,offspring,fire=values
        for is_demon in (False,True):
            for kind,title in zip(KINDS,kinds):
                name=("demon_" if is_demon else "evil_")+kind;label=(demon+(" " if locale=="en_us" else "-") if is_demon else evil+" ")+title
                lang["entity.abyssalport."+name]=label;lang["item.abyssalport."+name+"_spawn_egg"]=label+(" Spawn Egg" if locale=="en_us" else "-Spawn-Ei")
        lang["entity.abyssalport.shub_offspring"]=offspring;lang["item.abyssalport.shub_offspring_spawn_egg"]=offspring+(" Spawn Egg" if locale=="en_us" else "-Spawn-Ei")
        lang["block.abyssalport.mimic_fire"]=fire;lang["screen.abyssalport.animal_ecology.guide"]=guides[locale];write(path,lang)

if __name__ == "__main__": generate()
