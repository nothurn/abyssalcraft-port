"""Independently authored surface ruins using the existing own NBT writer.

DarklandsStructureGenerator, RitualGrounds, shrine/scion/house Java and the
original dsglow recipe inform roles/materials, not copied models or templates.
The original Dark Shrine's Idol of Fading and seven named deity statues are
separate unfinished systems, not substituted here or counted complete.
"""
import json
import random
from PIL import Image, ImageDraw
from generate_wasteland_stronghold import Room, ROOT, ASSETS, DATA, ID, write, texture
from generate_wasteland_minerals import append_tag
from build_pe_models import cube

PLANS=["house_small","house_large","ritual_ground","ritual_ground_columns",
       "circular_shrine","circular_shrine_columns","elevated_shrine",
       "elevated_shrine_large","memorial_cross","memorial_arch"]
BRICK=ID+"darkstone_bricks"
SLAB=ID+"darkstone_brick_slab"
STAIR=ID+"darkstone_brick_stairs"
CHISELED=ID+"chiseled_darkstone_bricks"
GLOW=ID+"glowing_darkstone_bricks"
COBBLE=ID+"darkstone_cobblestone"
OFFSETS=[(-3,0),(0,-3),(3,0),(0,3),(-2,2),(-2,-2),(2,2),(2,-2)]


class SurfacePlan(Room):
    def __init__(self,name):
        self.name,self.size=name,(13,8,13)
        self.blocks={}; self.rng=random.Random(8360+sum(map(ord,name)))
        # Omitted corners are not giant square terraformed patches. Explicit air
        # owns only the building's footprint, leaving surrounding terrain alone.
        for x in range(13):
            for z in range(13):
                if (x-6)**2+(z-6)**2>38: continue
                for y in range(8): self.set(x,y,z,"minecraft:air" if y else self.brick())
    def brick(self): return ID+"cracked_darkstone_bricks" if self.rng.randrange(5)==0 else BRICK
    def column(self,x,z,height):
        for y in range(1,height): self.set(x,y,z,CHISELED if y==height-1 else self.brick())
        self.set(x,height,z,GLOW)
    def slab(self,x,y,z,top=False): self.set(x,y,z,SLAB,{"type":"top" if top else "bottom","waterlogged":"false"})
    def stair(self,x,y,z,direction): self.set(x,y,z,STAIR,{"facing":direction,"half":"bottom","shape":"straight","waterlogged":"false"})


def house(name,large):
    room=SurfacePlan(name); left,right=(1,11) if large else (2,10)
    front,back=2,10
    for x in range(left,right+1):
        for z in range(front,back+1):
            room.set(x,0,z,COBBLE)
            for y in range(1,5):
                wall=x in (left,right) or z in (front,back)
                room.set(x,y,z,room.brick() if wall else "minecraft:air")
            if x in (left,right) and z in (front,back):
                for y in range(1,5): room.set(x,y,z,ID+"darklands_oak_log",{"axis":"y","bark":"false"})
    # Accessible two-block-high doorway and real native paired wood door.
    for y,half in ((1,"lower"),(2,"upper")):
        room.set(6,y,front,ID+"darklands_oak_door",{"facing":"south","half":half,"hinge":"left","open":"false","powered":"false"})
    for x in (left,right):
        for z in (4,7): room.set(x,2,z,"minecraft:glass_pane",{"north":"true","south":"true","east":"false","west":"false","waterlogged":"false"})
    # A stepped pitched roof, not a flat unrelated box. Preserve interior headroom.
    for x in range(left-1,right+2):
        distance=min(x-(left-1),(right+1)-x); y=4+min(distance,2)
        for z in range(front-1,back+2):
            if distance<2: room.stair(x,y,z,"east" if x<6 else "west")
            else: room.slab(x,y,z)
    room.set(left+1,1,back-1,"minecraft:crafting_table")
    room.set(right-1,1,back-1,"minecraft:furnace",{"facing":"north","lit":"false"})
    # Missing roof corner/worn gable gives abandoned masonry, not random holes in
    # the critical access route. No invented chest progression or boss treasure.
    room.set(left-1,4,back+1,"minecraft:air")
    room.save("darklands_ruins")


def ritual(name,columns):
    room=SurfacePlan(name)
    room.set(6,1,6,COBBLE)
    for x,z in OFFSETS: room.set(6+x,1,6+z,COBBLE)
    for x,z in ((1,6),(11,6),(6,1),(6,11)): room.slab(x,1,z)
    if columns:
        for x,z in ((2,2),(10,2),(2,10),(10,10)): room.column(x,z,3)
    else:
        for x,z in ((1,3),(1,9),(11,3),(11,9)): room.set(x,1,z,GLOW)
    room.save("darklands_ruins")


def shrine(name,columns,elevated,large=False):
    room=SurfacePlan(name); stage=2 if elevated else 0
    if elevated:
        half=3 if large else 2
        for x in range(6-half,7+half):
            for z in range(6-half,7+half):
                for y in range(1,stage+1): room.set(x,y,z,room.brick())
        for x in range(5,8):
            room.stair(x,1,2,"south"); room.stair(x,2,3,"south")
    else:
        for x in range(4,9):
            for z in range(4,9): room.set(x,0,z,COBBLE)
    room.set(6,stage+1,6,ID+"monolith_pillar")
    # An actually functioning existing PE source. The missing original deity
    # catalogue is not asserted to be implemented by this independent effigy.
    room.set(6,stage+2,6,ID+"prototype_effigy")
    for x,z in ((2,6),(10,6),(6,2),(6,10)): room.slab(x,1,z)
    if columns or large:
        for x,z in ((2,2),(10,2),(2,10),(10,10)): room.column(x,z,4)
    room.save("darklands_ruins")


def memorial(name,arch):
    room=SurfacePlan(name)
    if arch:
        for x in (3,9): room.column(x,6,4)
        for x in range(3,10): room.set(x,5,6,room.brick())
        for x in (3,9): room.slab(x,6,6)
        room.set(6,0,6,CHISELED)
    else:
        for y in range(1,5): room.set(6,y,6,CHISELED)
        for x in range(4,9): room.set(x,3,6,room.brick())
        for x in range(4,9): room.slab(x,1,5); room.slab(x,1,7)
        room.set(6,5,6,GLOW)
    room.save("darklands_ruins")


def templates():
    house("house_small",False); house("house_large",True)
    ritual("ritual_ground",False); ritual("ritual_ground_columns",True)
    shrine("circular_shrine",False,False); shrine("circular_shrine_columns",True,False)
    shrine("elevated_shrine",False,True); shrine("elevated_shrine_large",True,True,True)
    memorial("memorial_cross",False); memorial("memorial_arch",True)


def generation():
    write(DATA,"worldgen/template_pool/darklands_ruins/start.json",{
        "fallback":"minecraft:empty","elements":[{"weight":1,"element":{
            "element_type":"minecraft:single_pool_element","location":ID+"darklands_ruins/"+name,
            "processors":"minecraft:empty","projection":"rigid"}} for name in PLANS]})
    write(DATA,"worldgen/structure/darklands_ruins.json",{
        "type":"minecraft:jigsaw","biomes":"#abyssalport:darklands","step":"surface_structures",
        "spawn_overrides":{},"terrain_adaptation":"beard_thin","start_pool":ID+"darklands_ruins/start",
        # Native JigsawPlacement only submits the starting piece to the builder
        # with positive depth. There are no jigsaw connectors in these templates,
        # so depth one still produces exactly one saved surface structure.
        "size":1,"start_height":{"absolute":-1},"project_start_to_heightmap":"WORLD_SURFACE_WG",
        "max_distance_from_center":32,"use_expansion_hack":False})
    # Native saved structure starts/references replace a session-only set. One
    # structure per region, no per-chunk multi-attempt cascade and no global scan.
    write(DATA,"worldgen/structure_set/darklands_ruins.json",{
        "structures":[{"structure":ID+"darklands_ruins","weight":1}],"placement":{
            "type":"minecraft:random_spread","salt":734092,"spacing":12,"separation":6}})


def materials():
    name="glowing_darkstone_bricks"
    source=Image.open(ASSETS/"textures/block/darkstone_bricks.png").convert("RGB")
    draw=ImageDraw.Draw(source)
    draw.line((3,3,5,3,5,5,7,5,7,3,10,3),fill="#738a56")
    draw.line((4,11,6,11,6,13,9,13,9,10,12,10),fill="#d8db93")
    draw.point((5,3),fill="#e4e8b6"); draw.point((9,13),fill="#e4e8b6")
    texture("block/"+name+".png",source)
    write(ASSETS,"models/block/"+name+".json",{"parent":"minecraft:block/cube_all","textures":{"all":ID+"block/"+name}})
    write(ASSETS,"blockstates/"+name+".json",{"variants":{"":{"model":ID+"block/"+name}}})
    write(ASSETS,"models/item/"+name+".json",{"parent":ID+"block/"+name})
    write(DATA,"recipe/"+name+".json",{"type":"minecraft:crafting_shaped","pattern":["BDB","OGO","BOB"],
        "key":{"B":{"item":ID+"darkstone_bricks"},"D":{"item":"minecraft:diamond"},
               "O":{"item":"minecraft:obsidian"},"G":{"item":"minecraft:glowstone"}},
        "result":{"id":ID+name,"count":4}})
    rune=Image.new("RGB",(16,16),"#25272b"); draw=ImageDraw.Draw(rune)
    draw.rectangle((1,1,14,14),outline="#53584d")
    draw.polygon([(8,3),(12,8),(8,12),(3,8)],outline="#859272")
    draw.line((8,5,8,10),fill="#bcc395")
    texture("block/darkstone_ritual_top.png",rune)
    for name,geometry in (("darkstone_ritual_altar",[cube([0,0,0],[16,14,16],"stone"),cube([0,14,0],[16,16,16],"rune")]),
                          ("darkstone_ritual_pedestal",[cube([4,0,4],[12,12,12],"stone"),cube([2,12,2],[14,15,14],"rune")])):
        model={"textures":{"stone":ID+"block/darkstone_bricks","rune":ID+"block/darkstone_ritual_top","particle":"#stone"},"elements":geometry}
        write(ASSETS,"models/block/"+name+".json",model)
        write(ASSETS,"blockstates/"+name+".json",{"variants":{"":{"model":ID+"block/"+name}}})
        write(ASSETS,"models/item/"+name+".json",{"parent":ID+"block/"+name})
    for name in ["glowing_darkstone_bricks","darkstone_ritual_altar","darkstone_ritual_pedestal"]:
        drop=name if name.startswith("glowing") else "darkstone_cobblestone"
        write(DATA,"loot_table/blocks/"+name+".json",{"type":"minecraft:block","pools":[{
            "rolls":1,"conditions":[{"condition":"minecraft:survives_explosion"}],
            "entries":[{"type":"minecraft:item","name":ID+drop}]}]})
        append_tag(ROOT/"data/minecraft","tags/block/mineable/pickaxe.json",[ID+name])
    append_tag(ROOT/"data/minecraft","tags/block/needs_diamond_tool.json",[ID+"glowing_darkstone_bricks"])
    for locale,titles,guide in [
        ("en_us",["Glowing Darkstone Bricks","Darkstone Ritual Altar","Darkstone Ritual Pedestal"],
         "Small Darklands houses, shrines, memorials and unused ritual grounds occur naturally. Use the basic book on the centre of a complete nine-block Darkstone Cobblestone ring to form its tier-zero altar; do not mix the two cobblestone materials. The ring must remain within one chunk. Shrines contain the currently available PE effigy. Glowing Darkstone Bricks emit full light and require a diamond-tier pickaxe. Graves, Shoggoth lairs and the named deity/Idol of Fading systems are still separate development."),
        ("de_de",["Leuchtende Dunkelsteinziegel","Dunkelstein-Ritualaltar","Dunkelstein-Ritualpodest"],
         "Kleine Dunkellande-Häuser, Schreine, Gedenkbauten und unbenutzte Ritualplätze entstehen natürlich. Verwende das Basisbuch auf der Mitte eines vollständigen Neuner-Rings aus Dunkelstein-Bruchstein für den Altar der Stufe null. Vermische die beiden Bruchsteinmaterialien nicht; der Ring muss innerhalb eines Chunks bleiben. Schreine enthalten das aktuell verfügbare PE-Bildnis. Leuchtende Dunkelsteinziegel strahlen volles Licht aus und benötigen eine Diamantspitzhacke. Gräber, Shoggoth-Lairs und benannte Gottheiten/Idol of Fading bleiben getrennte Entwicklungsarbeit.")]:
        lang=json.loads((ASSETS/"lang"/(locale+".json")).read_text(encoding="utf-8"))
        for name,title in zip(["glowing_darkstone_bricks","darkstone_ritual_altar","darkstone_ritual_pedestal"],titles): lang["block.abyssalport."+name]=title
        lang["screen.abyssalport.darklands.ruins"]=guide.replace("Graves, Shoggoth lairs","Graveyards are connected. Shoggoth lairs").replace("Gräber, Shoggoth-Lairs","Friedhöfe sind verbunden. Shoggoth-Lairs")
        if "screen.abyssalport.darklands.variants" in lang:
            lang["screen.abyssalport.darklands.variants"]=lang["screen.abyssalport.darklands.variants"].replace(
                "Original buildings, lairs and the remaining creature ecology are still separate development.",
                "The remaining creature ecology and larger structures are separate development.").replace(
                "Gebäude, Lairs und die übrige Kreaturenökologie bleiben getrennte Entwicklungsarbeit.",
                "Die übrige Kreaturenökologie und größere Bauten bleiben getrennte Entwicklungsarbeit.")
        write(ASSETS,"lang/"+locale+".json",lang)


if __name__=="__main__":
    templates(); generation(); materials()
