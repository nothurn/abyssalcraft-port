"""Own dry descending lairs/biomass pixels, not the reserved original templates.

Java sources inform nearby-player/400-tick/6-nearby/5-admission rules and swamp/
river roles. Native regional attempts persist starts. Seven named deity statues,
dimensional Shoggoths and growth are separate development, not implied by lairs.
"""
import random
from PIL import Image, ImageDraw
from generate_wasteland_stronghold import Room, ROOT, ASSETS, DATA, ID, write, texture
from generate_wasteland_minerals import append_tag
from build_pe_models import cube

PLANS=("arched","split","alcoves")
STONE=ID+"monolith_stone"
BIOMASS=ID+"shoggoth_biomass"


class Lair(Room):
    def __init__(self,name):
        self.name,self.size=name,(17,14,27)
        self.blocks={}; self.rng=random.Random(7104+sum(map(ord,name)))
        # Only own the underground chamber and descending access, not a huge
        # rectangular surface patch or air cavity above the whole building.
        for x in range(1,16):
            for z in range(10,26):
                distance=((x-8)/7)**2+((z-17.5)/8)**2
                if distance>1.1: continue
                roof=6+(1 if distance<.5 else 0)
                for y in range(roof+1):
                    self.set(x,y,z,STONE if y in (0,roof) or distance>.85 else "minecraft:air")
        # Three-wide staircase, three-high clearance; the raised entrance collar
        # limits surface water entering from the sides. No hidden solid fallback.
        for z in range(11):
            floor=max(0,9-z)
            for x in range(6,11):
                for y in range(max(0,floor-1),min(14,floor+4)):
                    self.set(x,y,z,STONE if x in (6,10) or y<floor else "minecraft:air")
                if x not in (6,10):
                    self.set(x,floor,z,"minecraft:stone_brick_stairs" if z<9 else STONE,
                        {"facing":"north","half":"bottom","shape":"straight","waterlogged":"false"} if z<9 else None)
        # Small irregular spawn patches, with solid dry standing spaces around
        # them; a node need not spawn inside its own .85-high collision volume.
        patches={"arched":((4,15),(12,15),(8,21)),
                 "split":((4,18),(12,18)),"alcoves":((5,14),(11,14),(5,21),(11,21))}[name]
        for x,z in patches:
            self.set(x,0,z,BIOMASS,nbt={"id":(8,BIOMASS),"Cooldown":(3,self.rng.randrange(100)),"SpawnedShoggoths":(3,0)})
        if name=="split":
            for y in range(1,5): self.set(8,y,18,STONE)
        if name=="alcoves":
            for x in (3,13):
                for y in range(1,4): self.set(x,y,18,STONE)
        # Stone plinths are honest masonry, not a false implementation of the
        # seven missing deity statues or invented original treasure.
        for x,z in ((3,17),(13,17)):
            if (x,0,z) in self.blocks: self.set(x,1,z,STONE)


def generate():
    for name in PLANS: Lair(name).save("shoggoth_lair")
    write(DATA,"worldgen/template_pool/shoggoth_lair/start.json",{"fallback":"minecraft:empty","elements":[
        {"weight":1,"element":{"element_type":"minecraft:single_pool_element","location":ID+"shoggoth_lair/"+name,
         "processors":"minecraft:empty","projection":"rigid"}} for name in PLANS]})
    write(DATA,"tags/worldgen/biome/shoggoth_lairs.json",{"replace":False,"values":["minecraft:swamp","minecraft:mangrove_swamp","minecraft:river","minecraft:frozen_river"]})
    write(DATA,"worldgen/structure/shoggoth_lair.json",{"type":"minecraft:jigsaw","biomes":"#abyssalport:shoggoth_lairs",
        "step":"surface_structures","spawn_overrides":{},"terrain_adaptation":"none","start_pool":ID+"shoggoth_lair/start",
        "size":1,"start_height":{"absolute":-9},"project_start_to_heightmap":"WORLD_SURFACE_WG","max_distance_from_center":48,"use_expansion_hack":False})
    # Every neighbouring regional attempt stays at least 112 blocks apart.
    # Frequency is a modern regional analogue, not original per-chunk equality.
    write(DATA,"worldgen/structure_set/shoggoth_lairs.json",{"structures":[{"structure":ID+"shoggoth_lair","weight":1}],
        "placement":{"type":"minecraft:random_spread","salt":824300,"spacing":16,"separation":7}})
    bitmap=Image.new("RGB",(16,16)); rng=random.Random(7614); draw=ImageDraw.Draw(bitmap)
    for y in range(16):
        for x in range(16):
            variation=rng.randrange(-8,9); bitmap.putpixel((x,y),tuple(max(0,c+variation) for c in (38,63,44)))
    for x,y,radius in ((4,4,2),(11,7,3),(6,12,2)):
        draw.ellipse((x-radius,y-radius,x+radius,y+radius),fill="#172b24",outline="#69834c")
        draw.line((x-radius,y,x+radius,y),fill="#b1b66d")
    texture("block/shoggoth_biomass.png",bitmap)
    stone=Image.new("RGB",(16,16)); rng=random.Random(7615)
    for y in range(16):
        for x in range(16):
            shade=34+rng.randrange(-6,7)+(2 if (x+2*y)%7==0 else 0)
            stone.putpixel((x,y),(shade,max(0,shade-1),shade+3))
    texture("block/monolith_stone.png",stone)
    write(ASSETS,"models/block/monolith_stone.json",{"parent":"minecraft:block/cube_all","textures":{"all":ID+"block/monolith_stone"}})
    write(ASSETS,"models/item/monolith_stone.json",{"parent":ID+"block/monolith_stone"})
    write(ASSETS,"blockstates/monolith_stone.json",{"variants":{"":{"model":ID+"block/monolith_stone"}}})
    write(DATA,"loot_table/blocks/monolith_stone.json",{"type":"minecraft:block","pools":[{"rolls":1,"conditions":[{"condition":"minecraft:survives_explosion"}],
        "entries":[{"type":"minecraft:item","name":STONE}]}]})
    append_tag(ROOT/"data/minecraft","tags/block/mineable/pickaxe.json",[STONE])
    write(ASSETS,"models/block/shoggoth_biomass.json",{"textures":{"body":ID+"block/shoggoth_biomass","particle":"#body"},
        "elements":[cube([0,0,0],[16,13.6,16],"body"),cube([1,13.6,1],[6,15,6],"body"),cube([9,13.6,8],[15,15.5,14],"body")]})
    write(ASSETS,"models/item/shoggoth_biomass.json",{"parent":ID+"block/shoggoth_biomass"})
    write(ASSETS,"blockstates/shoggoth_biomass.json",{"variants":{"":{"model":ID+"block/shoggoth_biomass"}}})
    # Native biomass is unharvestable even with Silk Touch. Finite lair supplies
    # cannot be moved into an unrestricted portable spawner via block loot.
    write(DATA,"loot_table/blocks/shoggoth_biomass.json",{"type":"minecraft:block","pools":[]})
    append_tag(ROOT/"data/minecraft","tags/block/mineable/shovel.json",[BIOMASS])
    for language,value in (("en_us","Shoggoth Biomass"),("de_de","Shoggoth-Biomasse")):
        import json
        path=ASSETS/"lang"/(language+".json"); entries=json.loads(path.read_text("utf-8"))
        entries["block.abyssalport.shoggoth_biomass"]=value
        entries["block.abyssalport.monolith_stone"]="Monolith Stone" if language=="en_us" else "Monolithstein"
        entries["screen.abyssalport.shoggoth_lairs"]=(
            "Look for descending stone lairs in swamps and rivers, the Abyssal Wasteland and Dreadlands. Their biomass attempts a spawn every 400 active ticks when a non-spectator player is within 16 blocks. Six living Shoggoths of any form within 32 blocks stop further spawns. After five successful admissions a node becomes ordinary Monolith Stone. Peaceful and disabled mob spawning pause the timer. Breaking biomass removes it permanently, even with Silk Touch. Matching remains crystallize into eight phosphorus shards; dimensional remains add two corresponding crystal fragments. Groups can build monoliths at clear surface sites; the full deity statue catalogue is still development."
            if language=="en_us" else
            "Suche absteigende Steinhöhlen in Sümpfen und Flüssen, dem Abyssalen Ödland und den Dreadlands. Ihre Biomasse versucht alle 400 aktiven Ticks einen Spawn, wenn ein Spieler außerhalb des Zuschauermodus höchstens 16 Blöcke entfernt ist. Sechs lebende Shoggoths beliebiger Form innerhalb von 32 Blöcken halten weitere Spawns an. Nach fünf erfolgreichen Aufnahmen wird ein Knoten zu normalem Monolithstein. Friedlich und deaktivierte Mob-Spawns pausieren den Timer. Abbau entfernt Biomasse dauerhaft, auch mit Behutsamkeit. Passende Reste kristallisieren zu acht Phosphor-Splittern; Dimensionsreste liefern zwei zugehörige Kristallfragmente zusätzlich. Gruppen können auf freien Oberflächen Monolithen errichten; der vollständige Gottheitenkatalog ist noch Entwicklung.")
        write(ASSETS,"lang/"+language+".json",entries)


if __name__=="__main__": generate()
