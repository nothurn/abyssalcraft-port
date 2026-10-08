"""Own soil pixels/native resources; Java DreadGrass/DreadlandsDirt inform rules."""
import json
import random
from PIL import Image, ImageDraw
from generate_wasteland_stronghold import ROOT, ASSETS, DATA, ID, write, texture
from generate_wasteland_minerals import append_tag


def generate():
    rng=random.Random(97185)
    for name,base in (("dreadlands_dirt",(74,41,45)),("dreadlands_grass_top",(106,45,54)),("dreadlands_grass_side",(74,41,45))):
        bitmap=Image.new("RGB",(16,16))
        for y in range(16):
            for x in range(16):
                color=(106,45,54) if name.endswith("side") and y<=2+(x%3==0) else base
                value=rng.randrange(-13,14); bitmap.putpixel((x,y),tuple(max(0,min(255,c+value)) for c in color))
        if name.endswith("top"):
            draw=ImageDraw.Draw(bitmap)
            for x,y in ((2,3),(7,9),(12,5),(4,13)): draw.line((x,y,x+2,y-1),fill="#8b3c48")
        texture("block/"+name+".png",bitmap)
    for name in ("dreadlands_dirt","dreadlands_grass"):
        model={"parent":"minecraft:block/cube_all","textures":{"all":ID+"block/dreadlands_dirt"}} if name.endswith("dirt") else {
            "parent":"minecraft:block/cube_bottom_top","textures":{"bottom":ID+"block/dreadlands_dirt","top":ID+"block/dreadlands_grass_top","side":ID+"block/dreadlands_grass_side"}}
        write(ASSETS,"models/block/"+name+".json",model)
        write(ASSETS,"models/item/"+name+".json",{"parent":ID+"block/"+name})
        write(ASSETS,"blockstates/"+name+".json",{"variants":{"":{"model":ID+"block/"+name}}})
        entry={"type":"minecraft:item","name":ID+"dreadlands_dirt"}
        if name.endswith("grass"):
            entry={"type":"minecraft:alternatives","children":[{"type":"minecraft:item","name":ID+name,"conditions":[
                {"condition":"minecraft:match_tool","predicate":{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:silk_touch","levels":{"min":1}}]}}}]},entry]}
        write(DATA,"loot_table/blocks/"+name+".json",{"type":"minecraft:block","pools":[{"rolls":1,"conditions":[{"condition":"minecraft:survives_explosion"}],"entries":[entry]}]})
    for tag in ("tags/block/dirt.json","tags/block/mineable/shovel.json"):
        append_tag(ROOT/"data/minecraft",tag,[ID+"dreadlands_dirt",ID+"dreadlands_grass"])
    for locale,names in (("en_us",("Dreadlands Dirt","Dreadlands Grass")),("de_de",("Dreadlands-Erde","Dreadlands-Gras"))):
        lang=json.loads((ASSETS/"lang"/(locale+".json")).read_text(encoding="utf-8"))
        for block,title in zip(("dreadlands_dirt","dreadlands_grass"),names): lang["block.abyssalport."+block]=title
        write(ASSETS,"lang/"+locale+".json",lang)


if __name__=="__main__": generate()
