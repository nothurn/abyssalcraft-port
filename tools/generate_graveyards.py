"""Own graveyards/UV pixels, informed by Java rules, not original reserved NBT/media.

Three own sizes, native saved starts, source night/cap mechanics, buried chest
50% omission and source-informed remains. Omothol/Dark Realm remain absent.
"""
import json
import random
from PIL import Image, ImageDraw
from generate_wasteland_stronghold import Room, ROOT, ASSETS, DATA, ID, write, texture
from generate_wasteland_minerals import append_tag
from generate_crystallizer import processing, output
from build_pe_models import cube

PLANS=["small","medium","large"]
MATERIALS=[("stone","stone_tombstone","minecraft:block/stone","minecraft:grass_block","minecraft:dirt","graveyard"),
           ("dark","darkstone_tombstone",ID+"block/darkstone","minecraft:grass_block","minecraft:dirt","graveyard"),
           ("abyssal","abyssal_stone_tombstone",ID+"block/abyssal_stone",ID+"fused_abyssal_sand",ID+"abyssal_stone","wasteland_graveyard"),
           ("coralium","coralium_stone_tombstone",ID+"block/coralium_stone",ID+"fused_abyssal_sand",ID+"abyssal_stone","wasteland_graveyard"),
           ("dread","dreadstone_tombstone",ID+"block/dreadstone",ID+"dreadlands_grass",ID+"dreadlands_dirt","dreadlands_graveyard"),
           ("elysian","elysian_stone_tombstone",ID+"block/elysian_stone",ID+"dreadlands_grass",ID+"dreadlands_dirt","dreadlands_graveyard")]


def templates():
    for prefix,marker,stone,top,filler,loot in MATERIALS:
        for size,half,rows in [("small",3,2),("medium",4,3),("large",5,4)]:
            room=Room.__new__(Room); room.name=prefix+"_"+size
            room.size=(13,6,13); room.blocks={}; room.rng=random.Random(97561+rows)
            left,right=6-half,6+half
            for x in range(left,right+1):
                for z in range(1,12):
                    room.set(x,0,z,filler); room.set(x,1,z,top,{"snowy":"false"} if top=="minecraft:grass_block" else None)
                    for y in range(2,6): room.set(x,y,z,"minecraft:air")
                    if z in (1,11) or x in (left,right):
                        if not(z==1 and abs(x-6)<=1): room.set(x,2,z,"minecraft:cobblestone_wall",{"east":"none","west":"none","north":"none","south":"none","up":"true","waterlogged":"false"})
            for x in (6-2,6+2):
                for row in range(rows):
                    z=3+row*2
                    room.set(x,2,z,ID+marker,{"facing":"north"})
                    room.set(x,2,z-1,"minecraft:cobblestone_slab",{"type":"bottom","waterlogged":"false"})
            # Native rules may replace this buried chest with dirt. No manual
            # generation-time inventory or session-only treasure bookkeeping.
            room.set(6,0,6,"minecraft:chest",{"facing":"north","type":"single","waterlogged":"false"},
                     {"id":(8,"minecraft:chest"),"LootTable":(8,ID+"chests/"+loot)})
            room.save("graveyard")


def generation():
    rules=[{"input_predicate":{"predicate_type":"minecraft:random_block_match","block":"minecraft:chest","probability":.5},
            "location_predicate":{"predicate_type":"minecraft:always_true"},"output_state":{"Name":"minecraft:dirt"},
            "block_entity_modifier":{"type":"minecraft:clear"}}]
    # Overworld source has two independent one-in-ten Shadow replacement checks.
    rules.append({"input_predicate":{"predicate_type":"minecraft:random_block_match","block":ID+"stone_tombstone","probability":.19},
                  "location_predicate":{"predicate_type":"minecraft:always_true"},
                  "output_state":{"Name":ID+"darkstone_tombstone","Properties":{"facing":"north"}},
                  "block_entity_modifier":{"type":"minecraft:clear"}})
    write(DATA,"worldgen/processor_list/graveyard.json",{"processors":[{"processor_type":"minecraft:rule","rules":rules}]})
    for name,prefixes,biomes in [("graveyard",["stone"],"#minecraft:is_overworld"),("darklands_graveyard",["dark"],"#abyssalport:darklands"),
                               ("wasteland_graveyard",["abyssal","coralium"],"#abyssalport:wasteland_graveyards"),
                               ("dreadlands_graveyard",["dread","elysian"],ID+"dreadlands")]:
        processor="graveyard"
        if name in ("wasteland_graveyard","dreadlands_graveyard"):
            processor=name
            materials=MATERIALS[2:4] if name.startswith("wasteland") else MATERIALS[4:6]
            dim_rules=[{**rules[0],"output_state":{"Name":materials[0][4]}}]
            for current,other in (materials,materials[::-1]):
                dim_rules.append({"input_predicate":{"predicate_type":"minecraft:random_block_match","block":ID+current[1],"probability":.1},
                     "location_predicate":{"predicate_type":"minecraft:always_true"},"output_state":{"Name":ID+"darkstone_tombstone","Properties":{"facing":"north"}},
                     "block_entity_modifier":{"type":"minecraft:clear"}})
                dim_rules.append({"input_predicate":{"predicate_type":"minecraft:random_block_match","block":ID+current[1],"probability":.5},
                     "location_predicate":{"predicate_type":"minecraft:always_true"},"output_state":{"Name":ID+other[1],"Properties":{"facing":"north"}},
                     "block_entity_modifier":{"type":"minecraft:clear"}})
            write(DATA,"worldgen/processor_list/"+processor+".json",{"processors":[{"processor_type":"minecraft:rule","rules":dim_rules}]})
        write(DATA,"worldgen/template_pool/"+name+"/start.json",{"fallback":"minecraft:empty","elements":[
            {"weight":1,"element":{"element_type":"minecraft:single_pool_element","location":ID+"graveyard/"+prefix+"_"+size,
             "processors":ID+processor,"projection":"rigid"}} for prefix in prefixes for size in PLANS]})
        write(DATA,"worldgen/structure/"+name+".json",{"type":"minecraft:jigsaw","biomes":biomes,"step":"surface_structures",
            "spawn_overrides":{},"terrain_adaptation":"beard_thin","start_pool":ID+name+"/start","size":1,
            "start_height":{"absolute":-1},"project_start_to_heightmap":"WORLD_SURFACE_WG","max_distance_from_center":32,"use_expansion_hack":False})
    # A shared regional attempt selects the biome-compatible start. These offsets
    # conservatively exceed source 150-block separation; frequency is a modern
    # regional analogue, not a claim of identical 1.12 per-chunk random draws.
    write(DATA,"worldgen/structure_set/graveyards.json",{"structures":[{"structure":ID+name,"weight":1} for name in ("graveyard","darklands_graveyard")],
        "placement":{"type":"minecraft:random_spread","salt":824297,"spacing":24,"separation":14}})
    for name,salt in [("wasteland_graveyard",824298),("dreadlands_graveyard",824299)]:
        write(DATA,"worldgen/structure_set/"+name+".json",{"structures":[{"structure":ID+name,"weight":1}],
            "placement":{"type":"minecraft:random_spread","salt":salt,"spacing":16,"separation":10}})
    write(DATA,"tags/worldgen/biome/wasteland_graveyards.json",{"replace":False,"values":[ID+name for name in ("abyssal_wasteland","abyssal_plateau")]})
    write(DATA,"tags/worldgen/biome/ghoul_spawns.json",{"replace":False,"values":["minecraft:"+name for name in ("plains","desert","taiga","savanna")]})
    write(DATA,"neoforge/biome_modifier/ghouls.json",{"type":"neoforge:add_spawns","biomes":"#abyssalport:ghoul_spawns",
        "spawners":{"type":ID+"ghoul","weight":1,"minCount":1,"maxCount":3}})
    for name,biomes,count,weight in [("depths_ghouls_wasteland","#abyssalport:wasteland_graveyards",5,1),
         ("depths_ghouls_darklands_forest",ID+"darklands_forest",5,60),
         ("depths_ghouls_darklands_plains",ID+"darklands_plains",3,1),
         ("depths_ghouls_darklands_highlands",ID+"darklands_highlands",5,1),
         ("depths_ghouls_darklands_mountains",ID+"darklands_mountains",2,1),
         ("depths_ghouls_darklands_legacy",ID+"darklands",3,1),
         ("dreaded_ghouls",ID+"dreadlands",2,1)]:
        write(DATA,"neoforge/biome_modifier/"+name+".json",{"type":"neoforge:add_spawns","biomes":biomes,
            "spawners":{"type":ID+("dreaded_ghoul" if name.startswith("dreaded") else "depths_ghoul"),"weight":weight,"minCount":1,"maxCount":count}})


def materials():
    for prefix,name,stone,top,filler,loot in MATERIALS:
        # Own three-dimensional marker, with native orientation/collision.
        write(ASSETS,"models/block/"+name+".json",{"textures":{"stone":stone,"rune":ID+"block/tombstone_rune","particle":"#stone"},
            "elements":[cube([1.6,0,4.8],[14.4,3,11.2],"stone"),cube([3,3,6],[13,16,10],"stone"),cube([4,5,5.9],[12,13,6],"rune")]})
        write(ASSETS,"blockstates/"+name+".json",{"variants":{"facing="+facing:{"model":ID+"block/"+name,**({"y":angle} if angle else {})}
              for facing,angle in [("north",0),("east",90),("south",180),("west",270)]}})
        write(ASSETS,"models/item/"+name+".json",{"parent":ID+"block/"+name})
        # Source tombstones deliberately never drop, including Silk Touch.
        write(DATA,"loot_table/blocks/"+name+".json",{"type":"minecraft:block","pools":[]})
        append_tag(ROOT/"data/minecraft","tags/block/mineable/pickaxe.json",[ID+name])
    rune=Image.new("RGB",(16,16),"#535a51"); draw=ImageDraw.Draw(rune)
    draw.line((8,3,8,12),fill="#202924",width=2); draw.line((4,6,12,6),fill="#202924",width=2)
    draw.line((5,13,11,13),fill="#899081"); texture("block/tombstone_rune.png",rune)
    for name,base in [("ghoul_flesh",(107,112,72)),("shadow_ghoul_flesh",(58,57,74)),("overworld_shoggoth_flesh",(71,108,81)),
                      ("depths_ghoul_flesh",(74,119,95)),("dreaded_ghoul_flesh",(123,51,63)),
                      ("abyssal_shoggoth_flesh",(41,121,110)),("dreaded_shoggoth_flesh",(123,42,71))]:
        bitmap=Image.new("RGBA",(16,16)); draw=ImageDraw.Draw(bitmap); rng=random.Random(921+len(name))
        points=[(3,6),(6,3),(11,4),(14,8),(11,12),(5,14),(2,11)]
        draw.polygon(points,fill=(*base,255))
        for index in range(35):
            x,z=rng.randrange(3,13),rng.randrange(4,13)
            if bitmap.getpixel((x,z))[3]: draw.point((x,z),fill=tuple(max(0,min(255,c+rng.randrange(-18,19))) for c in base)+(255,))
        draw.line((5,6,10,5,12,8),fill=tuple(min(255,c+40) for c in base)+(255,))
        texture("item/"+name+".png",bitmap)
        write(ASSETS,"models/item/"+name+".json",{"parent":"minecraft:item/generated","textures":{"layer0":ID+"item/"+name}})
    for name,colors in [("ghoul",((106,116,78),(60,70,48),(188,190,123))),("shadow_ghoul",((39,40,52),(20,22,30),(211,201,161))),
         ("depths_ghoul",((60,105,88),(36,65,58),(157,180,116))),("depths_ghoul_1",((58,87,77),(31,50,44),(171,184,152))),
         ("depths_ghoul_2",((92,106,112),(51,65,70),(203,202,176))),("depths_ghoul_3",((154,98,44),(87,54,29),(212,192,137))),
         ("dreaded_ghoul",((89,42,52),(50,23,30),(208,158,105)))]:
        bitmap=Image.new("RGBA",(64,64)); rng=random.Random(817+len(name)); draw=ImageDraw.Draw(bitmap)
        for x1,y1,x2,y2 in [(0,0,32,16),(0,16,26,33),(32,16,44,26),(32,28,44,38),(0,40,12,49),(16,40,28,49)]:
            for x in range(x1,x2):
                for y in range(y1,y2):
                    base=colors[0] if y<16 or y>=40 else colors[1]
                    variation=rng.randrange(-10,11)
                    bitmap.putpixel((x,y),tuple(max(0,min(255,c+variation)) for c in base)+(255,))
        # Two dim, asymmetrical sockets and narrow teeth on the actual head front UV.
        draw.rectangle((9,10,10,11),fill=(*colors[2],255)); draw.rectangle((13,10,14,11),fill=(*colors[2],255))
        draw.line((10,14,14,14),fill="#bbb797")
        texture("entity/"+name+".png",bitmap)
        if name.startswith("depths"):
            head=["depths_ghoul_head","pete_head","wilson_head","orange_head"][int(name[-1]) if name[-1].isdigit() else 0]
            # Independently drawn front/side pixels derived from our own UV only.
            front=bitmap.crop((8,8,16,16)).resize((16,16),Image.Resampling.NEAREST)
            side=bitmap.crop((0,8,8,16)).resize((16,16),Image.Resampling.NEAREST)
            texture("block/"+head+"_front.png",front); texture("block/"+head+"_side.png",side)
            head_cube=cube([4,0,4],[12,8,12],"side"); head_cube["faces"]["north"]["texture"]="#front"
            write(ASSETS,"models/block/"+head+".json",{"textures":{"side":ID+"block/"+head+"_side","front":ID+"block/"+head+"_front","particle":"#side"},"elements":[head_cube]})
            write(ASSETS,"blockstates/"+head+".json",{"variants":{"facing="+facing:{"model":ID+"block/"+head,**({"y":angle} if angle else {})}
                  for facing,angle in [("north",0),("east",90),("south",180),("west",270)]}})
            write(ASSETS,"models/item/"+head+".json",{"parent":ID+"block/"+head,"display":{
                  "gui":{"rotation":[25,225,0],"translation":[0,3,0],"scale":[1.5,1.5,1.5]},
                  "head":{"rotation":[0,0,0],"translation":[0,8,0],"scale":[1.6,1.6,1.6]}}})
            write(DATA,"loot_table/blocks/"+head+".json",{"type":"minecraft:block","pools":[{"rolls":1,"conditions":[{"condition":"minecraft:survives_explosion"}],"entries":[{"type":"minecraft:item","name":ID+head}]}]})
    append_tag(ROOT/"data/minecraft","tags/entity_type/undead.json",[ID+name for name in ("ghoul","depths_ghoul","dreaded_ghoul")])
    append_tag(ROOT/"data/minecraft","tags/entity_type/can_breathe_under_water.json",[ID+"depths_ghoul"])
    append_tag(DATA,"tags/entity_type/shadow_rending_targets.json",[ID+"shadow_ghoul"])
    for tag in ("coralium_immune","abyssal_rending_targets"): append_tag(DATA,"tags/entity_type/"+tag+".json",[ID+"depths_ghoul"])
    for tag in ("dread_immune","dread_rending_targets"): append_tag(DATA,"tags/entity_type/"+tag+".json",[ID+"dreaded_ghoul"])
    for mob,flesh in [("ghoul","ghoul_flesh"),("shadow_ghoul","shadow_ghoul_flesh"),("lesser_shoggoth","overworld_shoggoth_flesh"),
                      ("depths_ghoul","depths_ghoul_flesh"),("dreaded_ghoul","dreaded_ghoul_flesh"),
                      ("depths_ghoul_1","depths_ghoul_flesh"),("depths_ghoul_2","depths_ghoul_flesh"),("depths_ghoul_3","depths_ghoul_flesh")]:
        loot={"type":"minecraft:entity","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":ID+flesh,
          "functions":[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":0,"max":2}},
                       {"function":"minecraft:enchanted_count_increase","enchantment":"minecraft:looting","count":{"type":"minecraft:uniform","min":0,"max":1}}]}]}]}
        if mob.startswith("depths_ghoul"):
            head=["depths_ghoul_head","pete_head","wilson_head","orange_head"][int(mob[-1]) if mob[-1].isdigit() else 0]
            loot["pools"].append({"rolls":1,"conditions":[{"condition":"minecraft:killed_by_player"},
              {"condition":"minecraft:random_chance_with_enchanted_bonus","enchantment":"minecraft:looting","unenchanted_chance":.025,
               "enchanted_chance":{"type":"minecraft:linear","base":.035,"per_level_above_first":.01}}],"entries":[{"type":"minecraft:item","name":ID+head}]})
        write(DATA,"loot_table/entities/"+mob+".json",loot)
    entries=[{"type":"minecraft:item","name":name,"functions":[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":1,"max":16}}]}
             for name in ["minecraft:bone","minecraft:leather","minecraft:rotten_flesh",ID+"ghoul_flesh",ID+"overworld_shoggoth_flesh"]]
    entries.append({"type":"minecraft:empty"})
    write(DATA,"loot_table/chests/graveyard.json",{"type":"minecraft:chest","pools":[{"rolls":{"type":"minecraft:uniform","min":0,"max":9},"entries":entries}]})
    for realm,items in [("wasteland",[ID+"coralium_plagued_flesh",ID+"depths_ghoul_flesh",ID+"abyssal_shoggoth_flesh",ID+"skin_of_the_abyssal_wasteland"]),
                        ("dreadlands",[ID+"dread_fragment",ID+"dreaded_ghoul_flesh",ID+"dreaded_shoggoth_flesh",ID+"skin_of_the_dreadlands"])]:
        dim_entries=[{"type":"minecraft:item","name":name,"functions":[{"function":"minecraft:set_count",
                 "count":{"type":"minecraft:uniform","min":1,"max":2 if "skin_of" in name else 16}}]} for name in ["minecraft:bone","minecraft:leather"]+items]
        dim_entries.append({"type":"minecraft:empty"})
        # Spirit Tablet rare-branch parity is pending the connected tablet system;
        # do not add a made-up placeholder item or claim complete treasure parity.
        write(DATA,"loot_table/chests/"+realm+"_graveyard.json",{"type":"minecraft:chest","pools":[{"rolls":{"type":"minecraft:uniform","min":0,"max":9},"entries":dim_entries}]})
    processing("ghoul_flesh",ID+"ghoul_flesh",output("phosphorus","shard",12),xp=.2)
    processing("shadow_ghoul_flesh",ID+"shadow_ghoul_flesh",output("phosphorus","shard",12),{"id":ID+"shadow_shard"},.2)
    processing("depths_ghoul_flesh",ID+"depths_ghoul_flesh",output("phosphorus","shard",12),output("coralium","shard"),.2)
    processing("dreaded_ghoul_flesh",ID+"dreaded_ghoul_flesh",output("phosphorus","shard",12),output("dreadium","shard"),.2)
    processing("overworld_shoggoth_flesh",ID+"overworld_shoggoth_flesh",output("phosphorus","shard",8),xp=.2)
    processing("abyssal_shoggoth_flesh",ID+"abyssal_shoggoth_flesh",output("phosphorus","shard",8),output("coralium","fragment",2),.2)
    processing("dreaded_shoggoth_flesh",ID+"dreaded_shoggoth_flesh",output("phosphorus","shard",8),output("dreadium","fragment",2),.2)
    for locale,names,guide in [("en_us",["Ghoul","Shadow Ghoul","Ghoul Flesh","Shadow Ghoul Flesh","Overworld Shoggoth Flesh","Stone Tombstone","Darkstone Tombstone"],
      "Graveyards occur naturally in the Overworld and Darklands. At night, active tombstones can raise one Ghoul every 200 ticks, capped at five nearby Ghouls within 15 blocks; daylight, Peaceful and disabled mob spawning pause them. Breaking a tombstone removes the source permanently: even Silk Touch does not return it. Stone raises ordinary Ghouls; Darkstone raises poison-immune Shadow Ghouls. Their flesh has harmful food effects. Crystallizing either produces twelve phosphorus shards; Shadow flesh also gives one Shadow Shard. Buried graves may contain ordinary remains, not boss progression loot. Later dimensional Ghoul/grave variants are still separate development."),
      ("de_de",["Ghoul","Schatten-Ghoul","Ghoulfleisch","Schatten-Ghoulfleisch","Oberwelt-Shoggothfleisch","Steingrabstein","Dunkelstein-Grabstein"],
      "Friedhöfe entstehen in Oberwelt und Dunkellanden. Nachts können aktive Grabsteine alle 200 Ticks einen Ghoul erheben, höchstens fünf Ghouls im 15-Block-Bereich. Tageslicht, Friedlich und deaktiviertes Mob-Spawning pausieren den Timer. Abbau entfernt die Quelle endgültig: Auch Behutsamkeit gibt keinen Grabstein zurück. Stein erhebt normale Ghouls; Dunkelstein giftimmune Schatten-Ghouls. Ihr Fleisch verursacht schädliche Nahrungseffekte. Kristallisieren liefert zwölf Phosphorsplitter, Schattenfleisch zusätzlich einen Schattensplitter. Vergrabene Gräber enthalten gelegentlich gewöhnliche Überreste, keine Boss-Progression. Spätere Dimensionsvarianten bleiben eigene Entwicklungsarbeit.")]:
        lang=json.loads((ASSETS/"lang"/(locale+".json")).read_text(encoding="utf-8"))
        for key,title in zip(["entity.abyssalport.ghoul","entity.abyssalport.shadow_ghoul","item.abyssalport.ghoul_flesh","item.abyssalport.shadow_ghoul_flesh",
                              "item.abyssalport.overworld_shoggoth_flesh","block.abyssalport.stone_tombstone","block.abyssalport.darkstone_tombstone"],names): lang[key]=title
        lang["item.abyssalport.ghoul_spawn_egg"]=names[0]+(" Spawn Egg" if locale=="en_us" else "-Spawn-Ei")
        lang["item.abyssalport.shadow_ghoul_spawn_egg"]=names[1]+(" Spawn Egg" if locale=="en_us" else "-Spawn-Ei")
        lang["screen.abyssalport.page.26"]="Graveyards and Ghouls" if locale=="en_us" else "Friedhöfe und Ghouls"
        lang["screen.abyssalport.ghouls.guide"]=guide.replace("Later dimensional Ghoul/grave variants are still separate development.","Omothol and Dark Realm variants remain separate development.").replace("Spätere Dimensionsvarianten bleiben eigene Entwicklungsarbeit.","Omothol und Dunkles Reich bleiben eigene Entwicklungsarbeit.")
        lang["research.abyssalport.ghouls"]="Ghoul remains" if locale=="en_us" else "Ghoul-Überreste"
        translated={"entity.abyssalport.depths_ghoul":("Depths Ghoul","Tiefen-Ghoul"),"entity.abyssalport.dreaded_ghoul":("Dreaded Ghoul","Schreckens-Ghoul"),
            "item.abyssalport.depths_ghoul_flesh":("Depths Ghoul Flesh","Tiefen-Ghoulfleisch"),"item.abyssalport.dreaded_ghoul_flesh":("Dreaded Ghoul Flesh","Schreckens-Ghoulfleisch"),
            "item.abyssalport.abyssal_shoggoth_flesh":("Abyssal Shoggoth Flesh","Abyssales Shoggothfleisch"),"item.abyssalport.dreaded_shoggoth_flesh":("Dreaded Shoggoth Flesh","Schreckens-Shoggothfleisch")}
        for prefix,marker,stone,top,filler,loot in MATERIALS[2:]: translated["block.abyssalport."+marker]=(marker.replace("_"," ").title(),{"abyssal":"Abyssalstein","coralium":"Coraliumstein","dread":"Schreckensstein","elysian":"Elysischer Stein"}[prefix]+"-Grabstein")
        for index,head in enumerate(("depths_ghoul_head","pete_head","wilson_head","orange_head")):
            name=("Depths Ghoul","Pete","Mr. Wilson","Dr. Orange")[index]
            translated["block.abyssalport."+head]=(name+" Head",("Tiefen-Ghoul" if index==0 else name)+"-Kopf")
            if index: lang["entity.abyssalport.depths_ghoul."+str(index)]=name
        for key,pair in translated.items(): lang[key]=pair[0 if locale=="en_us" else 1]
        for name in ("depths_ghoul","dreaded_ghoul"): lang["item.abyssalport."+name+"_spawn_egg"]=lang["entity.abyssalport."+name]+(" Spawn Egg" if locale=="en_us" else "-Spawn-Ei")
        lang["screen.abyssalport.ghouls.guide"]+= (" Depths Ghouls breathe underwater and have rare named/baby variants; accepted hits spread Coralium Plague only in the Wasteland with the current default rules. Dreaded Ghouls resist fire and spread Dread Plague. Abyssal/Coralium tombstones raise Depths; Dreadstone/Elysian raise Dreaded. Dimensional graves contain the matching ordinary remains and one or two skins; Spirit Tablet rare treasure remains unfinished. Crystallizing their flesh gives twelve phosphorus shards and one matching Coralium/Dreadium shard. Shoggoth remains give eight phosphorus shards, with two matching fragments for dimensional remains. Rare Depths heads can be placed or worn." if locale=="en_us" else
            " Tiefen-Ghouls atmen unter Wasser und haben seltene benannte/Jungtier-Varianten; erfolgreiche Treffer verbreiten die Coraliumseuche bei den aktuellen Standardregeln nur im Ödland. Schreckens-Ghouls sind feuerimmun und verbreiten die Schreckensseuche. Abyssal-/Coraliumstein erhebt Tiefen-Ghouls; Schreckens-/Elysischer Stein Schreckens-Ghouls. Dimensionsgräber enthalten passende Überreste und ein bis zwei Häute; der seltene Geistertafel-Schatzpfad ist noch offen. Ihr Fleisch liefert zwölf Phosphorsplitter und einen passenden Coralium-/Schreckens-Splitter. Shoggothfleisch liefert acht Phosphorsplitter, Dimensionsfleisch dazu zwei passende Fragmente. Seltene Tiefen-Ghoul-Köpfe sind platzier- und tragbar.")
        if "screen.abyssalport.darklands.ruins" in lang:
            lang["screen.abyssalport.darklands.ruins"]=lang["screen.abyssalport.darklands.ruins"].replace("Graves, Shoggoth lairs","Graveyards are connected. Shoggoth lairs").replace("Gräber, Shoggoth-Lairs","Friedhöfe sind verbunden. Shoggoth-Lairs")
        write(ASSETS,"lang/"+locale+".json",lang)


if __name__=="__main__":
    templates(); generation(); materials()
