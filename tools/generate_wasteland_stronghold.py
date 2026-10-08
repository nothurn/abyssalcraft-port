"""Own modular stronghold templates and pixel art; never reads upstream media.

Source facts: MapGenAbyStronghold's 128 rings/distance 32/spread 3, chamber
powerstone, tier-five BlockPSDL and eight-Gem/four-Tracker recipe. Room plans,
loot mixture and pixels are independently authored, not the 1.12 maze algorithm.
"""
import gzip
import json
import random
import struct
from pathlib import Path
from PIL import Image, ImageDraw
import generate_abyssalnite_assets as assets
from generate_wasteland_minerals import append_tag

ROOT, ASSETS, DATA = assets.ROOT, assets.ASSETS, assets.DATA
write, texture = assets.resource, assets.texture
BRICKS = "abyssal_stone_bricks"
SHAPES = "abyssal_stone_brick"
POWERSTONE = "dreadlands_infused_powerstone"
ID = "abyssalport:"


def nbt_string(value):
    raw = value.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def nbt_payload(kind, value):
    if kind == 3: return struct.pack(">i", value)
    if kind == 8: return nbt_string(value)
    if kind == 9:
        child, entries = value
        return struct.pack(">Bi", child, len(entries)) + b"".join(nbt_payload(child, entry) for entry in entries)
    if kind == 10:
        return b"".join(bytes([child])+nbt_string(name)+nbt_payload(child, entry) for name, (child, entry) in value.items()) + b"\0"
    raise ValueError(f"Unsupported authored NBT tag {kind}")


class Room:
    def __init__(self, name, width, height, depth):
        self.name, self.size = name, (width, height, depth)
        self.blocks = {}
        self.rng = random.Random(90218 + sum(map(ord, name)))
        for y in range(height):
            for z in range(depth):
                for x in range(width):
                    shell = y in (0, height-1) or x in (0, width-1) or z in (0, depth-1)
                    block = ID+("cracked_"+BRICKS if self.rng.randrange(9)==0 else BRICKS) if shell else "minecraft:air"
                    self.set(x, y, z, block)

    def set(self, x, y, z, name, properties=None, nbt=None):
        assert 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2], (self.name,x,y,z)
        self.blocks[(x,y,z)] = (name, properties or {}, nbt)

    def fill(self, x1, y1, z1, x2, y2, z2, name, properties=None):
        for y in range(y1, y2+1):
            for z in range(z1, z2+1):
                for x in range(x1, x2+1): self.set(x,y,z,name,properties)

    def door(self, side, pool, offset=None):
        w,h,d = self.size
        x,z = {"north": (w//2,0), "south": (w//2,d-1), "west": (0,d//2), "east": (w-1,d//2)}[side]
        if offset is not None:
            if side in ("north","south"): x = offset
            else: z = offset
        for t in range(-1,2):
            for y in range(1,4): self.set(x+t if side in ("north","south") else x,y,z+t if side in ("west","east") else z,"minecraft:air")
        self.set(x,1,z,"minecraft:jigsaw", {"orientation": side+"_up"}, {
            "id": (8,"minecraft:jigsaw"), "name": (8,ID+"stronghold_door"), "target": (8,ID+"stronghold_door"),
            "pool": (8,ID+"wasteland_stronghold/"+pool if pool else "minecraft:empty"),
            "final_state": (8,"minecraft:air"), "joint": (8,"aligned"),
            "selection_priority": (3,0), "placement_priority": (3,0)})

    def chest(self,x,y,z,kind,facing="north"):
        self.set(x,y,z,"minecraft:chest", {"facing":facing,"type":"single","waterlogged":"false"},
                 {"id": (8,"minecraft:chest"), "LootTable": (8,ID+"chests/wasteland_stronghold_"+kind)})

    def light(self,x,y,z): self.set(x,y,z,"minecraft:sea_lantern")

    def save(self, folder="wasteland_stronghold"):
        palette, lookup, blocks = [], {}, []
        for pos,(name,properties,nbt) in sorted(self.blocks.items(),key=lambda entry:(entry[0][1],entry[0][2],entry[0][0])):
            key = (name,tuple(sorted(properties.items())))
            if key not in lookup:
                lookup[key] = len(palette)
                state = {"Name": (8,name)}
                if properties: state["Properties"] = (10,{key:(8,val) for key,val in properties.items()})
                palette.append(state)
            block = {"pos": (9,(3,list(pos))), "state": (3,lookup[key])}
            if nbt: block["nbt"] = (10,nbt)
            blocks.append(block)
        tag = {"DataVersion": (3,3955), "size": (9,(3,list(self.size))), "palette": (9,(10,palette)),
               "blocks": (9,(10,blocks)), "entities": (9,(10,[]))}
        path = DATA/"structure"/folder/(self.name+".nbt")
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_bytes(gzip.compress(b"\x0a\0\0"+nbt_payload(10,tag),mtime=0))


def templates():
    main = Room("ritual_chamber",21,11,21)
    for side in ("north","south","east","west"): main.door(side,"corridors")
    for x,z in ((4,4),(16,4),(4,16),(16,16)):
        main.fill(x,1,z,x,8,z,ID+"chiseled_"+BRICKS)
        main.light(x,7,z)
    # Dry ring/walkway around two visibly hazardous trenches. No fluid in the
    # access route or directly underneath the progression-critical powerstone.
    for x in (6,14):
        for z in range(7,14):
            main.set(x,0,z,ID+"liquid_coralium",{"level":"0"})
            main.set(x-1 if x==6 else x+1,1,z,ID+SHAPES+"_fence",{"north":"true","south":"true","east":"false","west":"false","waterlogged":"false"})
    main.fill(9,1,13,11,1,15,ID+"chiseled_"+BRICKS)
    main.set(10,2,14,ID+POWERSTONE)
    for x,z in ((8,13),(12,13),(8,15),(12,15)):
        main.set(x,1,z,ID+SHAPES+"_slab",{"type":"bottom","waterlogged":"false"})
    main.chest(3,1,10,"supplies","east")
    main.save()

    corridor = Room("corridor",7,7,15)
    corridor.door("north","corridors"); corridor.door("south","rooms")
    corridor.door("east","rooms"); corridor.door("west","rooms")
    for z in (3,11):
        corridor.light(1,4,z); corridor.light(5,4,z)
        for x in (1,5): corridor.fill(x,1,z,x,3,z,ID+"chiseled_"+BRICKS)
    corridor.save()

    library = Room("archive",15,9,13)
    library.door("south",""); library.door("east","corridors")
    for x in (2,3,11,12):
        for z in range(2,10):
            library.fill(x,1,z,x,3,z,"minecraft:bookshelf")
    library.fill(5,1,3,9,1,4,ID+SHAPES+"_slab",{"type":"bottom","waterlogged":"false"})
    library.chest(7,1,2,"archive"); library.light(7,6,6)
    library.save()

    storage = Room("store",13,7,13)
    storage.door("south",""); storage.door("north","corridors")
    for x,z in ((2,2),(10,2),(2,9),(10,9)):
        storage.chest(x,1,z,"supplies","south" if z==2 else "north")
        storage.set(x,2,z,ID+SHAPES+"_slab",{"type":"top","waterlogged":"false"})
    storage.fill(3,1,4,3,3,6,"minecraft:barrel",{"facing":"up","open":"false"})
    storage.light(6,5,6); storage.save()

    guard = Room("guard_room",11,8,11)
    guard.door("south","")
    guard.set(5,1,5,"minecraft:spawner",nbt={"id":(8,"minecraft:mob_spawner"),
        "SpawnData":(10,{"entity":(10,{"id":(8,"abyssalport:abyssal_zombie")})})})
    for x,z in ((3,3),(7,3),(3,7),(7,7)):
        guard.fill(x,1,z,x,3,z,ID+SHAPES+"_fence",{"north":"false","south":"false","east":"false","west":"false","waterlogged":"false"})
    guard.chest(2,1,2,"supplies","south"); guard.save()

    landing = Room("stair_hall",11,9,15)
    landing.door("south",""); landing.door("east","corridors")
    for z in range(3,9):
        height = z-2
        landing.fill(3,1,z,7,height,z,ID+BRICKS)
        for x in range(3,8): landing.set(x,height,z,ID+SHAPES+"_stairs",{"facing":"south","half":"bottom","shape":"straight","waterlogged":"false"})
    landing.fill(3,6,9,7,6,11,ID+BRICKS)
    landing.chest(5,7,10,"archive","north"); landing.light(5,7,1)
    landing.save()

    cap = Room("end",7,7,5)
    cap.door("south","")
    cap.fill(2,1,1,4,4,1,ID+"chiseled_"+BRICKS); cap.light(3,5,2); cap.save()


def pool(name, members, fallback="ends"):
    write(DATA,"worldgen/template_pool/wasteland_stronghold/"+name+".json",{
        "fallback": ID+"wasteland_stronghold/"+fallback if fallback else "minecraft:empty",
        "elements": [{"weight":weight,"element":{"element_type":"minecraft:single_pool_element",
            "location":ID+"wasteland_stronghold/"+room,"processors":"minecraft:empty","projection":"rigid"}} for room,weight in members]})


def generation():
    pool("start",[("ritual_chamber",1)],"")
    pool("corridors",[("corridor",1)])
    pool("rooms",[("archive",3),("store",3),("guard_room",2),("stair_hall",2)])
    pool("ends",[("end",1)],"")
    write(DATA,"worldgen/structure/wasteland_stronghold.json",{
        "type":"minecraft:jigsaw","biomes":"#abyssalport:has_structure/wasteland_stronghold",
        "step":"underground_structures","spawn_overrides":{},"terrain_adaptation":"none",
        "start_pool":ID+"wasteland_stronghold/start","size":4,"use_expansion_hack":False,
        "start_height":{"type":"minecraft:uniform","min_inclusive":{"absolute":24},"max_inclusive":{"absolute":40}},
        "max_distance_from_center":96})
    write(DATA,"worldgen/structure_set/wasteland_strongholds.json",{
        "structures":[{"structure":ID+"wasteland_stronghold","weight":1}],
        "placement":{"type":"minecraft:concentric_rings","salt":0,"distance":32,"spread":3,"count":128,
                     "preferred_biomes":"#abyssalport:has_structure/wasteland_stronghold"}})
    write(DATA,"tags/worldgen/biome/has_structure/wasteland_stronghold.json",{"replace":False,"values":[ID+"abyssal_wasteland"]})
    write(DATA,"tags/worldgen/structure/powerstone_tracker_located.json",{"replace":False,"values":[ID+"wasteland_stronghold"]})
    for kind,items in {"supplies":[("minecraft:bread",3,6,5),("minecraft:coal",3,8,5),("minecraft:iron_ingot",1,4,3),
                                  (ID+"coralium_gem",1,4,3),(ID+"refined_coralium_ingot",1,2,1),("minecraft:gold_ingot",1,3,2)],
                       "archive":[("minecraft:book",2,5,6),("minecraft:paper",3,8,5),(ID+"coralium_gem",1,3,3),
                                  (ID+"shadow_gem",1,2,1)]}.items():
        write(DATA,"loot_table/chests/wasteland_stronghold_"+kind+".json",{"type":"minecraft:chest","pools":[{
            "rolls":{"type":"minecraft:uniform","min":3,"max":5},"entries":[{"type":"minecraft:item","name":name,"weight":weight,
                "functions":[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":low,"max":high}}]} for name,low,high,weight in items]}]})


def models():
    names = [BRICKS,"cracked_"+BRICKS,"chiseled_"+BRICKS]
    for index,name in enumerate(names):
        image = Image.new("RGB",(16,16)); rng=random.Random(15833+index)
        for y in range(16):
            for x in range(16):
                shade=rng.randint(-5,5); image.putpixel((x,y),(37+shade,48+shade,43+shade))
        draw=ImageDraw.Draw(image)
        if index<2:
            for y in (0,8):
                draw.line((0,y,15,y),fill="#17221e"); draw.line((0,y+1,15,y+1),fill="#3b4b40")
                for x in (0,8) if y==0 else (4,12): draw.line((x,y,x,y+7),fill="#17221e")
            if index==1:
                draw.line((4,2,5,4,4,6,6,7),fill="#111c18"); draw.line((11,10,9,11,10,14),fill="#111c18")
        else:
            draw.rectangle((2,2,13,13),outline="#17221e"); draw.rectangle((3,3,12,12),outline="#46584a")
            draw.polygon([(7,4),(11,7),(8,11),(4,8)],outline="#7c9373")
        texture("block/"+name+".png",image)
        write(ASSETS,"blockstates/"+name+".json",{"variants":{"":{"model":ID+"block/"+name}}})
        write(ASSETS,"models/block/"+name+".json",{"parent":"minecraft:block/cube_all","textures":{"all":ID+"block/"+name}})
        write(ASSETS,"models/item/"+name+".json",{"parent":ID+"block/"+name})
    # Reuse already authored, vanilla-compatible state topology, not another palette.
    for suffix in ("slab","stairs"):
        source=json.loads((ASSETS/"blockstates"/("darkstone_brick_"+suffix+".json")).read_text())
        converted=json.loads(json.dumps(source).replace("darkstone_brick",SHAPES))
        write(ASSETS,"blockstates/"+SHAPES+"_"+suffix+".json",converted)
    for suffix,parent in (("slab","slab"),("slab_top","slab_top"),("stairs","stairs"),("stairs_inner","inner_stairs"),("stairs_outer","outer_stairs")):
        write(ASSETS,"models/block/"+SHAPES+"_"+suffix+".json",{"parent":"minecraft:block/"+parent,"textures":{side:ID+"block/"+BRICKS for side in ("bottom","top","side")}})
    for suffix in ("slab","stairs","fence"):
        write(ASSETS,"models/item/"+SHAPES+"_"+suffix+".json",{"parent":ID+"block/"+SHAPES+"_"+("fence_inventory" if suffix=="fence" else suffix)})
    for suffix,parent in (("post","fence_post"),("side","fence_side"),("inventory","fence_inventory")):
        write(ASSETS,"models/block/"+SHAPES+"_fence_"+suffix+".json",{"parent":"minecraft:block/"+parent,"textures":{"texture":ID+"block/"+BRICKS}})
    write(ASSETS,"blockstates/"+SHAPES+"_fence.json",{"multipart":[{"apply":{"model":ID+"block/"+SHAPES+"_fence_post"}}]+[
        {"when":{direction:"true"},"apply":{"model":ID+"block/"+SHAPES+"_fence_side","y":angle,"uvlock":True}} for direction,angle in (("north",0),("east",90),("south",180),("west",270))]})
    image=Image.new("RGB",(16,16)); rng=random.Random(434901)
    for y in range(16):
        for x in range(16):
            noise=rng.randint(-8,8); image.putpixel((x,y),(57+noise,37+noise,49+noise))
    draw=ImageDraw.Draw(image); draw.line((3,0,5,4,4,8,7,11,6,15),fill="#af5455",width=2); draw.line((12,1,10,5,11,8,9,12),fill="#db9480")
    texture("block/"+POWERSTONE+".png",image)
    write(ASSETS,"blockstates/"+POWERSTONE+".json",{"variants":{"":{"model":ID+"block/"+POWERSTONE}}})
    write(ASSETS,"models/block/"+POWERSTONE+".json",{"parent":"minecraft:block/block","textures":{"all":ID+"block/"+POWERSTONE,"particle":ID+"block/"+POWERSTONE},
        "elements":[{"from":[3.2,0,3.2],"to":[12.8,16,12.8],"faces":{face:{"texture":"#all","uv":[0,0,16,16]} for face in ("up","down","north","south","east","west")}}]})
    write(ASSETS,"models/item/"+POWERSTONE+".json",{"parent":ID+"block/"+POWERSTONE})
    image=Image.new("RGBA",(16,16)); draw=ImageDraw.Draw(image)
    draw.ellipse((2,4,13,12),fill="#193d32",outline="#789370"); draw.ellipse((4,5,11,11),fill="#55836b"); draw.polygon([(7,4),(10,8),(7,12),(5,8)],fill="#b46b61"); draw.line((7,6,7,10),fill="#241c26"); draw.point((6,5),fill="#e9cdb4")
    texture("item/powerstone_tracker.png",image)
    write(ASSETS,"models/item/powerstone_tracker.json",{"parent":"minecraft:item/generated","textures":{"layer0":ID+"item/powerstone_tracker"}})


def recipes_and_tags():
    names=[BRICKS,"cracked_"+BRICKS,"chiseled_"+BRICKS,*[SHAPES+"_"+suffix for suffix in ("slab","stairs","fence")],POWERSTONE]
    for name in names:
        entry={"type":"minecraft:item","name":ID+name,"conditions":[{"condition":"minecraft:survives_explosion"}]}
        if name.endswith("slab"):
            entry["functions"]=[{"function":"minecraft:set_count","count":2,"conditions":[{"condition":"minecraft:block_state_property","block":ID+name,"properties":{"type":"double"}}]}]
        write(DATA,"loot_table/blocks/"+name+".json",{"type":"minecraft:block","pools":[{"rolls":1,"entries":[entry]}]})
    for suffix,pattern,count,ingredient in [("",["SS","SS"],4,"abyssal_stone"),("_slab",["SSS"],6,BRICKS),("_stairs",["S  ","SS ","SSS"],4,BRICKS),
        ("_fence",["SSS","SSS"],6,BRICKS)]:
        name = BRICKS if not suffix else SHAPES+suffix
        write(DATA,"recipe/"+name+".json",{"type":"minecraft:crafting_shaped","category":"building","pattern":pattern,"key":{"S":{"item":ID+ingredient}},"result":{"id":ID+name,"count":count}})
    write(DATA,"recipe/chiseled_"+BRICKS+".json",{"type":"minecraft:crafting_shaped","category":"building","pattern":["S","S"],"key":{"S":{"item":ID+SHAPES+"_slab"}},"result":{"id":ID+"chiseled_"+BRICKS}})
    write(DATA,"recipe/cracked_"+BRICKS+".json",{"type":"minecraft:smelting","category":"blocks","ingredient":{"item":ID+BRICKS},"result":{"id":ID+"cracked_"+BRICKS},"experience":.1,"cookingtime":200})
    for suffix,count in (("_slab",2),("_stairs",1),("_fence",1)):
        write(DATA,"recipe/"+SHAPES+suffix+"_stonecutting.json",{"type":"minecraft:stonecutting","ingredient":{"item":ID+BRICKS},"result":{"id":ID+SHAPES+suffix,"count":count}})
    write(DATA,"recipe/powerstone_tracker.json",{"type":"minecraft:crafting_shaped","category":"misc","pattern":["GGG","GEG","GGG"],"key":{"G":{"item":ID+"coralium_gem"},"E":{"item":"minecraft:ender_eye"}},"result":{"id":ID+"powerstone_tracker","count":4}})
    append_tag(ROOT/"data/minecraft","tags/block/mineable/pickaxe.json",[ID+n for n in names])
    append_tag(ROOT/"data/minecraft","tags/block/fences.json",[ID+SHAPES+"_fence"])
    append_tag(ROOT/"data/minecraft","tags/block/slabs.json",[ID+SHAPES+"_slab"])
    append_tag(ROOT/"data/minecraft","tags/block/stairs.json",[ID+SHAPES+"_stairs"])
    for tier in ("wooden","stone","iron","diamond","netherite"):
        append_tag(ROOT/"data/minecraft","tags/block/incorrect_for_"+tier+"_tool.json",[ID+POWERSTONE])
    append_tag(DATA,"tags/block/incorrect_for_abyssalnite_tool.json",[ID+POWERSTONE])
    write(DATA,"advancement/recipes/powerstone_tracker.json",{"criteria":{"coralium":{"trigger":"minecraft:inventory_changed","conditions":{"items":[{"items":[ID+"coralium_gem"]}]}}},"rewards":{"recipes":[ID+"powerstone_tracker"]}})


def language():
    for locale,entries in {
        "en_us":{"block.abyssalport.abyssal_stone_bricks":"Abyssal Stone Bricks","block.abyssalport.cracked_abyssal_stone_bricks":"Cracked Abyssal Stone Bricks",
            "block.abyssalport.chiseled_abyssal_stone_bricks":"Chiseled Abyssal Stone Bricks","block.abyssalport.abyssal_stone_brick_slab":"Abyssal Stone Brick Slab",
            "block.abyssalport.abyssal_stone_brick_stairs":"Abyssal Stone Brick Stairs","block.abyssalport.abyssal_stone_brick_fence":"Abyssal Stone Brick Fence",
            "block.abyssalport.dreadlands_infused_powerstone":"Dreadlands Infused Powerstone","item.abyssalport.powerstone_tracker":"Powerstone Tracker",
            "message.abyssalport.tracker_dimension":"The tracker only answers in the Abyssal Wasteland.","message.abyssalport.tracker_missing":"No unclaimed Wasteland Stronghold could be found.",
            "tooltip.abyssalport.powerstone_tracker":"Throw in the Wasteland to seek an unclaimed Stronghold. May shatter.","research.abyssalport.powerstone":"A Powerstone from the Stronghold",
            "screen.abyssalport.page.18":"Wasteland Strongholds","screen.abyssalport.stronghold":"Eight Coralium Gems around an Eye of Ender craft four Powerstone Trackers. Throw one in the Wasteland: it flies towards an underground Stronghold, then returns with an 80% chance. Repeat throws do not claim the structure. Dig carefully when it descends; Coralium trenches and Abyssal Zombie spawners guard the chambers.",
            "screen.abyssalport.stronghold_note":"In the central ritual chamber, a Dreadlands Infused Powerstone stands on a dry pedestal. A Refined Coralium Pickaxe (tier five) is required to collect it; weaker tools lose the drop. Collecting it marks that Stronghold as explored, so trackers seek another. The Powerstone and Asorah's Eye are the ingredients for the next Gateway Key."},
        "de_de":{"block.abyssalport.abyssal_stone_bricks":"Abyssale Steinziegel","block.abyssalport.cracked_abyssal_stone_bricks":"Rissige abyssale Steinziegel",
            "block.abyssalport.chiseled_abyssal_stone_bricks":"Gemeißelte abyssale Steinziegel","block.abyssalport.abyssal_stone_brick_slab":"Abyssale Steinziegelstufe",
            "block.abyssalport.abyssal_stone_brick_stairs":"Abyssale Steinziegeltreppe","block.abyssalport.abyssal_stone_brick_fence":"Abyssaler Steinziegelzaun",
            "block.abyssalport.dreadlands_infused_powerstone":"Dreadlands-durchsetzter Kraftstein","item.abyssalport.powerstone_tracker":"Kraftstein-Sucher",
            "message.abyssalport.tracker_dimension":"Der Sucher reagiert nur im Abyssalen Ödland.","message.abyssalport.tracker_missing":"Keine unerkundete Ödland-Festung gefunden.",
            "tooltip.abyssalport.powerstone_tracker":"Im Ödland werfen, um eine unerkundete Festung zu finden. Kann zerbrechen.","research.abyssalport.powerstone":"Ein Kraftstein aus der Festung",
            "screen.abyssalport.page.18":"Ödland-Festungen","screen.abyssalport.stronghold":"Acht Coralium-Edelsteine um ein Enderauge ergeben vier Kraftstein-Sucher. Im Ödland geworfen fliegt einer zu einer unterirdischen Festung und kehrt mit 80 % Wahrscheinlichkeit zurück. Wiederholtes Werfen markiert die Festung nicht als erkundet. Grabe vorsichtig, sobald er absinkt: Coralium-Gräben und Abyssal-Zombie-Spawner bewachen die Kammern.",
            "screen.abyssalport.stronghold_note":"Im zentralen Ritualraum steht ein Dreadlands-durchsetzter Kraftstein auf einem trockenen Podest. Eine Refined-Coralium-Spitzhacke (Stufe fünf) ist zum Einsammeln nötig; schwächere Werkzeuge verlieren den Drop. Nach dem Einsammeln gilt die Festung als erkundet und Sucher weisen zur nächsten. Kraftstein und Asorahs Auge sind Zutaten für den nächsten Gateway Key."}
    }.items():
        path=ASSETS/"lang"/(locale+".json"); text=json.loads(path.read_text(encoding="utf-8")); text.update(entries); write(ASSETS,"lang/"+locale+".json",text)


if __name__=="__main__":
    templates(); generation(); models(); recipes_and_tags(); language()
    # Canonicalize this generator's first-pass plural shape references. Older
    # authored files remain recoverable outside packaged resources.
    for base in (ROOT/"data/minecraft/tags/block", DATA/"tags/block"):
        for path in base.rglob("*.json"):
            tag=json.loads(path.read_text(encoding="utf-8"))
            values=[value.replace(ID+BRICKS+"_", ID+SHAPES+"_") if isinstance(value,str) else value for value in tag.get("values",[])]
            if values != tag.get("values",[]):
                tag["values"]=list(dict.fromkeys(values)); write(base,str(path.relative_to(base)),tag)
