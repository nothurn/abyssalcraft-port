"""Independent Darklands wood pixels with native Minecraft shape templates.

Reads only the local mapped Minecraft resource JAR for vanilla blockstate/model
syntax. No upstream textures, models or sounds are read or copied.
"""
import json
import random
import zipfile
from pathlib import Path
from PIL import Image, ImageDraw
from generate_crystallizer import ROOT, ASSETS, DATA, write, texture, append_tag

PREFIX = "darklands_oak_"
PARTS = ["log", "hollow_log", "planks", "leaves", "sapling", "stairs", "slab",
         "fence", "fence_gate", "door", "button", "pressure_plate"]


def media():
    rng = random.Random(19046)
    for name in ["log", "log_top", "hollow_log", "hollow_log_top", "planks", "leaves"]:
        image = Image.new("RGBA", (16, 16)); draw = ImageDraw.Draw(image)
        for y in range(16):
            for x in range(16):
                noise = rng.randrange(-9, 10)
                if name == "leaves":
                    if rng.random() < .19: continue
                    shade = (7 if (x+y) % 4 else 22) + noise
                    color = (37+shade, 64+shade, 57+shade, 255)
                elif name == "planks":
                    shade = noise + (9 if y % 4 == 0 else -12 if y % 4 == 3 else 0)
                    if x == ((y//4)*7+4) % 16: shade -= 11
                    color = (77+shade, 67+shade, 63+shade, 255)
                elif name.endswith("top"):
                    radius = max(abs(x-7.5), abs(y-7.5))
                    shade = noise + (-18 if int(radius) % 3 == 0 else 3)
                    color = (84+shade, 70+shade, 64+shade, 255)
                    if name.startswith("hollow") and radius < 4: color = (22+noise//3, 26+noise//3, 26+noise//3, 255)
                else:
                    shade = noise + (-14 if x % 4 == 0 else 5)
                    color = (55+shade, 51+shade, 54+shade, 255)
                    if name.startswith("hollow") and 5 <= x <= 10 and 5 <= y <= 10:
                        color = (17+noise//3, 22+noise//3, 23+noise//3, 255)
                image.putpixel((x, y), color)
        texture("block/"+PREFIX+name+".png", image)
    image = Image.new("RGBA", (16,16)); draw = ImageDraw.Draw(image)
    draw.line((8,14,8,6), fill="#4b4344", width=2)
    draw.line((8,9,3,6), fill="#4b4344"); draw.line((8,10,12,6), fill="#4b4344")
    for x,y in [(6,1),(2,4),(9,3),(10,6),(4,8)]:
        draw.rectangle((x,y,x+3,y+2), fill="#345e51"); draw.point((x+1,y), fill="#719a76")
    texture("block/"+PREFIX+"sapling.png", image)
    for half in ["bottom", "top"]:
        image = Image.new("RGBA", (16,16), "#483d3c"); draw = ImageDraw.Draw(image)
        for x in range(0,16,4): draw.line((x,0,x,15), fill="#302c32")
        draw.line((0,0,15,0), fill="#7a685e"); draw.line((0,15,15,15), fill="#292a30")
        if half == "top":
            draw.rectangle((4,3,11,11), fill=(0,0,0,0))
            draw.line((7,3,7,11), fill="#65574f"); draw.line((4,7,11,7), fill="#65574f")
        else:
            draw.rectangle((3,4,12,12), outline="#64554f"); draw.point((12,2), fill="#b49c67")
        texture("block/"+PREFIX+"door_"+half+".png", image)
    image = Image.new("RGBA", (16,16)); draw = ImageDraw.Draw(image)
    draw.rectangle((4,1,11,14), fill="#544644", outline="#292b31")
    draw.rectangle((6,3,9,6), fill=(0,0,0,0)); draw.point((10,8), fill="#c2ad75")
    draw.line((6,10,9,10), fill="#8f7765"); texture("item/"+PREFIX+"door.png", image)


def shapes():
    native = Path(__file__).resolve().parents[1] / "build/moddev/artifacts/neoforge-21.1.234-client-extra-aka-minecraft-resources.jar"
    def convert(value):
        return json.loads(json.dumps(value).replace("minecraft:block/oak_", "abyssalport:block/"+PREFIX))
    with zipfile.ZipFile(native) as jar:
        for part in PARTS:
            name = PREFIX+part
            if part.endswith("log"):
                variants = {}
                for axis in ["x","y","z"]:
                    for bark in [False,True]:
                        model = name+"_bark" if bark else name
                        state = {"model": "abyssalport:block/"+model}
                        if not bark and axis != "y": state.update({"x":90,"y":90 if axis=="x" else 0})
                        variants["axis="+axis+",bark="+str(bark).lower()] = state
                write(ASSETS,"blockstates/"+name+".json", {"variants":variants})
                write(ASSETS,"models/block/"+name+".json", {"parent":"minecraft:block/cube_column", "textures":{
                    "side":"abyssalport:block/"+name,"end":"abyssalport:block/"+name+"_top"}})
                write(ASSETS,"models/block/"+name+"_bark.json", {"parent":"minecraft:block/cube_all", "textures":{"all":"abyssalport:block/"+name}})
            else:
                write(ASSETS,"blockstates/"+name+".json", convert(json.loads(jar.read("assets/minecraft/blockstates/oak_"+part+".json"))))
                for path in jar.namelist():
                    base = "assets/minecraft/models/block/oak_"+part
                    if path == base+".json" or path.startswith(base+"_") and path.endswith(".json"):
                        modelname = path.rsplit("/",1)[-1].replace("oak_",PREFIX,1)
                        write(ASSETS,"models/block/"+modelname,convert(json.loads(jar.read(path))))
            item = {"parent":"abyssalport:block/"+name+("_inventory" if part in ["fence","button"] else "")}
            if part == "door": item = {"parent":"minecraft:item/generated","textures":{"layer0":"abyssalport:item/"+name}}
            write(ASSETS,"models/item/"+name+".json",item)


def resources():
    for part in PARTS:
        name = PREFIX+part
        loot = {"type":"minecraft:block", "pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"abyssalport:"+name}],
                                                    "conditions":[{"condition":"minecraft:survives_explosion"}]}]}
        if part == "door": loot["pools"][0]["conditions"].append({"condition":"minecraft:block_state_property","block":"abyssalport:"+name,"properties":{"half":"lower"}})
        if part == "slab": loot["pools"][0]["entries"][0]["functions"]=[{"function":"minecraft:set_count","count":2,
            "conditions":[{"condition":"minecraft:block_state_property","block":"abyssalport:"+name,"properties":{"type":"double"}}]},
            {"function":"minecraft:explosion_decay"}]
        if part == "leaves":
            tool = {"condition":"minecraft:any_of","terms":[
                {"condition":"minecraft:match_tool","predicate":{"items":["minecraft:shears"]}},
                {"condition":"minecraft:match_tool","predicate":{"predicates":{"minecraft:enchantments":[
                    {"enchantments":"minecraft:silk_touch","levels":{"min":1}}]}}}]}
            loot = {"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:alternatives","children":[
                {"type":"minecraft:item","name":"abyssalport:"+name,"conditions":[tool]},
                {"type":"minecraft:item","name":"abyssalport:"+PREFIX+"sapling","conditions":[{"condition":"minecraft:survives_explosion"},
                    {"condition":"minecraft:table_bonus","enchantment":"minecraft:fortune","chances":[.05,.0625,.083333336,.1]}]}]}]}]}
        write(DATA,"loot_table/blocks/"+name+".json",loot)
    recipes = {
        "planks": (["L"], {"L":{"tag":"abyssalport:darklands_oak_logs"}},4),
        "stairs": (["P  ","PP ","PPP"], {"P":{"item":"abyssalport:"+PREFIX+"planks"}},4),
        "slab": (["PPP"], {"P":{"item":"abyssalport:"+PREFIX+"planks"}},6),
        "fence": (["PSP","PSP"], {"P":{"item":"abyssalport:"+PREFIX+"planks"},"S":{"item":"minecraft:stick"}},3),
        "fence_gate": (["SPS","SPS"], {"P":{"item":"abyssalport:"+PREFIX+"planks"},"S":{"item":"minecraft:stick"}},1),
        "door": (["PP","PP","PP"], {"P":{"item":"abyssalport:"+PREFIX+"planks"}},3),
        "button": (["P"], {"P":{"item":"abyssalport:"+PREFIX+"planks"}},1),
        "pressure_plate": (["PP"], {"P":{"item":"abyssalport:"+PREFIX+"planks"}},1),
    }
    for suffix,(pattern,key,count) in recipes.items():
        name = PREFIX+suffix
        write(DATA,"recipe/"+name+".json",{"type":"minecraft:crafting_shaped","category":"building","group":name,
            "pattern":pattern,"key":key,"result":{"id":"abyssalport:"+name,"count":count}})
    write(DATA,"recipe/darklands_oak_charcoal.json",{"type":"minecraft:smelting","category":"misc","ingredient":{"tag":"abyssalport:darklands_oak_logs"},
          "result":{"id":"minecraft:charcoal"},"experience":1.0,"cookingtime":200})
    for suffix in list(recipes)+["charcoal"]:
        name=PREFIX+suffix
        write(DATA,"advancement/recipes/"+name+".json",{"criteria":{"has_wood":{"trigger":"minecraft:inventory_changed",
            "conditions":{"items":[{"items":["abyssalport:"+PREFIX+"log","abyssalport:"+PREFIX+"hollow_log","abyssalport:"+PREFIX+"planks"]}]}}},
            "rewards":{"recipes":["abyssalport:"+name]}})
    own = ["abyssalport:"+PREFIX+n for n in ["log","hollow_log"]]
    append_tag(DATA,"tags/block/darklands_oak_logs.json",own); append_tag(DATA,"tags/item/darklands_oak_logs.json",own)
    minecraft = DATA.parent / "minecraft"
    for kind in ["block","item"]:
        append_tag(minecraft,"tags/"+kind+"/logs_that_burn.json",["#abyssalport:darklands_oak_logs"])
        append_tag(minecraft,"tags/"+kind+"/logs.json",["#abyssalport:darklands_oak_logs"])
        for part,tag in {"planks":"planks","stairs":"wooden_stairs","slab":"wooden_slabs","fence":"wooden_fences",
                         "door":"wooden_doors","button":"wooden_buttons","pressure_plate":"wooden_pressure_plates",
                         "fence_gate":"fence_gates","leaves":"leaves","sapling":"saplings"}.items():
            append_tag(minecraft,"tags/"+kind+"/"+tag+".json",["abyssalport:"+PREFIX+part])
    append_tag(minecraft,"tags/block/mineable/axe.json",["abyssalport:"+PREFIX+n for n in PARTS if n not in ["leaves","sapling"]])
    append_tag(minecraft,"tags/block/mineable/hoe.json",["abyssalport:"+PREFIX+"leaves"])
    append_tag(minecraft,"tags/block/dirt.json",["abyssalport:darklands_soil"])
    write(DATA,"worldgen/configured_feature/darklands_oak.json",{"type":"abyssalport:darklands_oak","config":{}})
    write(DATA,"worldgen/placed_feature/darklands_oak.json",{"feature":"abyssalport:darklands_oak","placement":[
        {"type":"minecraft:count","count":10},{"type":"minecraft:in_square"},
        {"type":"minecraft:surface_water_depth_filter","max_water_depth":0},
        {"type":"minecraft:heightmap","heightmap":"OCEAN_FLOOR"},{"type":"minecraft:biome"}]})
    biome=json.loads((DATA/"worldgen/biome/darklands.json").read_text(encoding="utf-8"))
    biome["features"][9]=["abyssalport:darklands_oak" if x=="minecraft:dark_forest_vegetation" else x for x in biome["features"][9]]
    write(DATA,"worldgen/biome/darklands.json",biome)
    # Already-updated files are not changed repeatedly when this generator is rerun.
    if "abyssalport:darklands_oak" not in biome["features"][9]:
        raise ValueError("Darklands vegetation integration is absent")


def languages():
    names = {"log":("Darklands Oak Log","Dunkellande-Eichenstamm"),"hollow_log":("Hollow Darklands Oak Log","Hohler Dunkellande-Eichenstamm"),
        "planks":("Darklands Oak Planks","Dunkellande-Eichenbretter"),"leaves":("Darklands Oak Leaves","Dunkellande-Eichenlaub"),
        "sapling":("Darklands Oak Sapling","Dunkellande-Eichensetzling"),"stairs":("Darklands Oak Stairs","Dunkellande-Eichentreppe"),
        "slab":("Darklands Oak Slab","Dunkellande-Eichenstufe"),"fence":("Darklands Oak Fence","Dunkellande-Eichenzaun"),
        "fence_gate":("Darklands Oak Fence Gate","Dunkellande-Eichenzauntor"),"door":("Darklands Oak Door","Dunkellande-Eichentür"),
        "button":("Darklands Oak Button","Dunkellande-Eichenknopf"),"pressure_plate":("Darklands Oak Pressure Plate","Dunkellande-Eichendruckplatte")}
    guide = ["Darklands Oak grows a 9–11-block trunk, thick roots and wide ascending branches. Both normal and hollow logs make four planks, or one Charcoal in a furnace (200 ticks, 1 XP). Use planks for the native early wood recipes and the complete door, stairs, slab, fence, gate, button and pressure-plate family. Leaves can drop saplings (Fortune improves the chance); shears or Silk Touch preserve the leaves, not apples. Replant on dirt or Darklands Soil. Bone Meal advances a sapling before attempting growth. Allow room for six-block branches: a blocked tree refuses to grow rather than destroying your building.",
             "Dunkellande-Eichen bilden einen 9–11 Blöcke hohen Stamm, kräftige Wurzeln und breite aufsteigende Äste. Normale und hohle Stämme ergeben vier Bretter oder eine Holzkohle im Ofen (200 Ticks, 1 XP). Die Bretter funktionieren in frühen Holzrezepten und als Türen, Treppen, Stufen, Zäune, Tore, Knöpfe und Druckplatten. Laub kann Setzlinge droppen (Glück erhöht die Chance); Schere oder Behutsamkeit bewahren das Laub, keine Äpfel. Pflanze auf Erde oder Dunkellande-Boden. Knochenmehl entwickelt den Setzling und versucht anschließend das Wachstum. Halte Platz für sechs Blöcke lange Äste frei: Bei Hindernissen wächst der Baum nicht, statt dein Gebäude zu zerstören."]
    for index,locale in enumerate(["en_us","de_de"]):
        lang=json.loads((ASSETS/"lang"/(locale+".json")).read_text(encoding="utf-8"))
        lang.update({"block.abyssalport."+PREFIX+suffix:text[index] for suffix,text in names.items()})
        lang["screen.abyssalport.darklands.wood"]=guide[index]
        write(ASSETS,"lang/"+locale+".json",lang)


if __name__ == "__main__":
    media(); shapes(); resources(); languages()
