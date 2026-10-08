"""Own articulated Dread family atlases; audited creature rules, not upstream media."""
import json
import random
from PIL import Image, ImageDraw
from generate_crystallizer import ASSETS, DATA, write, texture, append_tag


def atlas(name,base,eyes):
    image=Image.new("RGBA",(64,64)); rng=random.Random(93012+sum(map(ord,name)))
    def box(u,v,w,h,d):
        for x,y,width,height in [(u+d,v,w,d),(u+d+w,v,w,d),(u,v+d,d,h),(u+d,v+d,w,h),(u+d+w,v+d,d,h),(u+2*d+w,v+d,w,h)]:
            for py in range(y,y+height):
                for px in range(x,x+width):
                    shade=rng.randrange(-9,10)
                    image.putpixel((px,py),tuple(max(0,min(255,c+shade)) for c in base)+(255,))
    box(0,0,10,8,10); box(0,20,8,6,6); box(32,20,7,2,2); box(32,26,6,2,2); box(32,32,2,3,3)
    draw=ImageDraw.Draw(image)
    for x in [7,10,13]: draw.point((x,28),fill=eyes)
    draw.line((7,30,13,30),fill="#251b28")
    for y in [12,15]: draw.line((12,y,17,y),fill=tuple(min(255,c+28) for c in base))
    texture("entity/"+name+".png",image)


def generate():
    names=["dread_spawn","greater_dread_spawn","lesser_dreadbeast"]
    for name,base,eyes in [(names[0],(93,45,56),"#dca77a"),(names[1],(78,38,48),"#e37e51"),(names[2],(61,32,45),"#f4be82")]:
        atlas(name,base,eyes)
        write(ASSETS,"models/item/"+name+"_spawn_egg.json",{"parent":"minecraft:item/template_spawn_egg"})
        drop="dread_fragment" if name==names[0] else "dreaded_shard"
        write(DATA,"loot_table/entities/"+name+".json",{"type":"minecraft:entity","pools":[{"rolls":1,"entries":[
            {"type":"minecraft:item","name":"abyssalport:"+drop,"functions":[
                {"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":0,"max":2}},
                {"function":"minecraft:enchanted_count_increase","enchantment":"minecraft:looting","count":{"type":"minecraft:uniform","min":0,"max":1}}]}]}]})
    image=Image.new("RGBA",(32,32)); draw=ImageDraw.Draw(image)
    for x,y,w,h in [(6,0,4,6),(10,0,4,6),(0,6,6,3),(6,6,4,3),(10,6,6,3),(16,6,4,3)]:
        draw.rectangle((x,y,x+w-1,y+h-1),fill="#97534e"); draw.line((x,y,x+w-1,y),fill="#d99871")
    texture("entity/dread_slug.png",image)
    ids=["abyssalport:"+name for name in names]
    for tag in ["dread_immune","dread_rending_targets"]: append_tag(DATA,"tags/entity_type/"+tag+".json",ids)
    minecraft=DATA.parent/"minecraft"
    append_tag(minecraft,"tags/entity_type/can_breathe_under_water.json",ids)
    append_tag(minecraft,"tags/entity_type/arthropod.json",[ids[2]])
    biome=json.loads((DATA/"worldgen/biome/dreadlands.json").read_text(encoding="utf-8"))
    for name,weight,maximum in [(names[0],30,2),(names[1],5,1),(names[2],1,1)]:
        if not any(row["type"]=="abyssalport:"+name for row in biome["spawners"]["monster"]):
            biome["spawners"]["monster"].append({"type":"abyssalport:"+name,"weight":weight,"minCount":1,"maxCount":maximum})
    write(DATA,"worldgen/biome/dreadlands.json",biome)
    pairs={
        "entity.abyssalport.dread_spawn":("Dread Spawn","Dread-Brut"),"entity.abyssalport.greater_dread_spawn":("Greater Dread Spawn","Große Dread-Brut"),
        "entity.abyssalport.lesser_dreadbeast":("Lesser Dreadbeast","Niedere Dreadbestie"),"entity.abyssalport.dread_slug":("Dread Slug","Dread-Geschoss"),
        "screen.abyssalport.dreadlands.ecology":(
            "Dread Spawn climb and inflict five seconds of plague after an accepted melee hit. Five nearby small Spawns can become one Greater Spawn; five nearby Greater Spawns one Lesser Dreadbeast. The replacement must actually enter the world before exactly five participants disappear. Larger forms use six-damage plague slugs at range, and smaller offspring may emerge periodically or after their real deaths. Small Spawns drop Dread Fragments; larger forms drop Dreaded Shards for the native Dreadium route. Local population limits bound natural spawning and periodic offspring. The original's hardcore overrides and bespoke audio are not yet present.",
            "Dread-Brut klettert und verursacht nach einem angenommenen Nahkampftreffer fünf Sekunden Pest. Fünf kleine Bruten in der Nähe können zu einer großen Brut werden; fünf große zu einer niederen Dreadbestie. Erst wenn der Nachfolger tatsächlich in der Welt aufgenommen wurde, verschwinden genau fünf Beteiligte. Größere Formen verschießen Pest-Geschosse mit sechs Schaden und können zeitweise oder nach ihrem tatsächlichen Tod kleinere Nachkommen erzeugen. Kleine Bruten droppen Dread-Fragmente, größere Dreaded-Splitter für den nativen Dreadium-Pfad. Lokale Populationsgrenzen beschränken Naturspawns und periodische Nachkommen. Hardcore-Sonderregeln und eigene Sounds des Originals sind noch nicht enthalten.")}
    for name in names: pairs["item.abyssalport."+name+"_spawn_egg"]=(pairs["entity.abyssalport."+name][0]+" Spawn Egg",pairs["entity.abyssalport."+name][1]+"-Spawn-Ei")
    for index,locale in enumerate(["en_us","de_de"]):
        lang=json.loads((ASSETS/"lang"/(locale+".json")).read_text(encoding="utf-8")); lang.update({key:value[index] for key,value in pairs.items()}); write(ASSETS,"lang/"+locale+".json",lang)


if __name__=="__main__": generate()
