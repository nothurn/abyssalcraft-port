"""Own Dread elite UV atlas/materials; source Java/recipe facts, no upstream media."""
import json
import random
from PIL import Image, ImageDraw
from generate_wasteland_minerals import ROOT, ASSETS, DATA, write, append_tag, loot
from generate_abyssalnite_assets import texture, PATTERNS, equipment_tags
from generate_dreadlands_progression import tinted


def media():
    image = Image.new("RGBA", (128, 128)); rng = random.Random(51792)
    def box(u, v, w, h, d, base):
        for x, y, width, height in [(u+d,v,w,d),(u+d+w,v,w,d),(u,v+d,d,h),(u+d,v+d,w,h),(u+d+w,v+d,d,h),(u+2*d+w,v+d,w,h)]:
            for py in range(y, y+height):
                for px in range(x, x+width):
                    shade = rng.randint(-5, 5)
                    image.putpixel((px, py), tuple(max(0, c+shade) for c in base)+(255,))
    box(0,0,12,10,10,(58,41,46)); box(80,20,6,2,7,(105,54,50))
    box(0,26,14,18,8,(43,37,44)); box(48,26,12,4,8,(72,43,48))
    box(0,54,4,10,5,(62,41,45)); box(70,40,6,4,7,(99,53,50))
    box(24,72,5,14,5,(50,36,43)); box(0,96,6,8,6,(53,38,44))
    box(28,112,5,8,5,(78,43,46)); box(80,98,6,3,9,(38,33,40))
    draw = ImageDraw.Draw(image)
    draw.rectangle((12,13,14,15),fill="#e3a076"); draw.rectangle((18,13,20,15),fill="#e3a076")
    draw.line((11,19,20,19),fill="#221c26")
    for y in [37,43,49]: draw.line((10,y,19,y),fill="#ab7161",width=2)
    texture("entity/dreadguard.png", image)
    image = Image.new("RGBA", (16,16)); draw = ImageDraw.Draw(image)
    draw.polygon([(11,1),(14,5),(10,9),(8,14),(3,12),(4,7)],fill="#71363c",outline="#27202b")
    draw.line((10,3,7,10),fill="#d2796a",width=2); draw.line((4,11,7,12),fill="#ac5757")
    texture("item/dreaded_shard.png",image)
    for name, source in [("dreadium_ingot","refined_coralium_ingot"),("dreadium_nugget","refined_coralium_nugget")]:
        texture("item/"+name+".png",tinted("item/"+source+".png","#48202e","#e09a7b"))
    for suffix in ["pickaxe","axe","shovel","sword","hoe"]:
        texture("item/dreadium_"+suffix+".png",tinted("item/refined_coralium_"+suffix+".png","#492232","#e49b7c"))
    for suffix in ["helmet","chestplate","leggings","boots"]:
        texture("item/dreadium_"+suffix+".png",tinted("item/refined_coralium_"+suffix+".png","#35212a","#cc8771"))
    for layer in [1,2]:
        texture("models/armor/dreadium_layer_"+str(layer)+".png",tinted("models/armor/refined_coralium_layer_"+str(layer)+".png","#302027","#cd896e"))
    texture("block/dreadium_block.png",tinted("block/refined_coralium_block.png","#492a32","#c57767"))
    image = tinted("block/dreadstone.png","#3c252d","#976258"); draw = ImageDraw.Draw(image)
    for y in [3,8,13]: draw.line((0,y,15,y),fill="#34232b")
    for y, x in [(0,7),(4,3),(9,11),(14,7)]: draw.line((x,y,x,min(15,y+3)),fill="#34232b")
    texture("block/dreadstone_bricks.png",image)


def resources():
    equipment_tags("dreadium")
    for name in ["dreaded_shard","dreadium_ingot","dreadium_nugget"]:
        write(ASSETS,"models/item/"+name+".json",{"parent":"minecraft:item/generated","textures":{"layer0":"abyssalport:item/"+name}})
    for suffix in ["pickaxe","axe","shovel","sword","hoe"]:
        write(ASSETS,"models/item/dreadium_"+suffix+".json",{"parent":"minecraft:item/handheld","textures":{"layer0":"abyssalport:item/dreadium_"+suffix}})
        write(DATA,"recipe/dreadium_"+suffix+".json",{"type":"minecraft:crafting_shaped","category":"equipment","pattern":PATTERNS[suffix],
            "key":{"I":{"item":"abyssalport:dreadium_ingot"},"S":{"tag":"c:rods/wooden"}},"result":{"id":"abyssalport:dreadium_"+suffix}})
    write(DATA,"advancement/recipes/dreadium_tools.json",{"criteria":{"has_ingot":{"trigger":"minecraft:inventory_changed","conditions":{"items":[{"items":["abyssalport:dreadium_ingot"]}]}}},
        "rewards":{"recipes":["abyssalport:dreadium_"+suffix for suffix in ["pickaxe","axe","shovel","sword","hoe"]]}})
    for suffix in ["helmet","chestplate","leggings","boots"]:
        write(ASSETS,"models/item/dreadium_"+suffix+".json",{"parent":"minecraft:item/generated","textures":{"layer0":"abyssalport:item/dreadium_"+suffix}})
        write(DATA,"recipe/dreadium_"+suffix+".json",{"type":"minecraft:crafting_shaped","category":"equipment","pattern":PATTERNS[suffix],
            "key":{"I":{"item":"abyssalport:dreadium_ingot"}},"result":{"id":"abyssalport:dreadium_"+suffix}})
    write(DATA,"advancement/recipes/dreadium_armor.json",{"criteria":{"has_ingot":{"trigger":"minecraft:inventory_changed","conditions":{"items":[{"items":["abyssalport:dreadium_ingot"]}]}}},
        "rewards":{"recipes":["abyssalport:dreadium_"+suffix for suffix in ["helmet","chestplate","leggings","boots"]]}})
    write(ASSETS,"models/item/dreadguard_spawn_egg.json",{"parent":"minecraft:item/template_spawn_egg"})
    for name in ["dreadium_block","dreadstone_bricks"]:
        write(ASSETS,"blockstates/"+name+".json",{"variants":{"":{"model":"abyssalport:block/"+name}}})
        write(ASSETS,"models/block/"+name+".json",{"parent":"minecraft:block/cube_all","textures":{"all":"abyssalport:block/"+name}})
        write(ASSETS,"models/item/"+name+".json",{"parent":"abyssalport:block/"+name}); loot(name,None,(1,1))
    write(DATA,"loot_table/entities/dreadguard.json",{"type":"minecraft:entity","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"abyssalport:dreaded_shard",
        "functions":[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":0,"max":2}},
                     {"function":"minecraft:enchanted_count_increase","enchantment":"minecraft:looting","count":{"type":"minecraft:uniform","min":0,"max":1}}]}]}]})
    write(DATA,"recipe/transmutation/dreaded_shard_to_dreadium.json",{"type":"abyssalport:transmutation","ingredient":{"item":"abyssalport:dreaded_shard"},
        "result":{"id":"abyssalport:dreadium_ingot","count":1},"experience":0.2})
    def shaped(name, pattern, source, result, count):
        write(DATA,"recipe/"+name+".json",{"type":"minecraft:crafting_shaped","category":"misc","pattern":pattern,
            "key":{"#":{"item":"abyssalport:"+source}},"result":{"id":"abyssalport:"+result,"count":count}})
    shaped("dreadium_block",["###","###","###"],"dreadium_ingot","dreadium_block",1)
    shaped("dreadium_from_block",["#"],"dreadium_block","dreadium_ingot",9)
    shaped("dreadium_nugget",["#"],"dreadium_ingot","dreadium_nugget",9)
    shaped("dreadium_from_nuggets",["###","###","###"],"dreadium_nugget","dreadium_ingot",1)
    shaped("dreadstone_bricks",["##","##"],"dreadstone","dreadstone_bricks",4)
    values = ["abyssalport:dreadium_block","abyssalport:dreadstone_bricks"]
    append_tag(ROOT/"data/minecraft","tags/block/mineable/pickaxe.json",values)
    for tier in ["wooden","stone","iron","diamond","netherite"]:
        append_tag(ROOT/"data/minecraft","tags/block/incorrect_for_"+tier+"_tool.json",values)
    for tier in ["abyssalnite","refined_coralium"]:
        append_tag(DATA,"tags/block/incorrect_for_"+tier+"_tool.json",["abyssalport:dreadium_block"])
    write(DATA,"tags/block/incorrect_for_dreadium_tool.json",{"replace":False,"values":[]})
    append_tag(ROOT/"data/minecraft","tags/block/beacon_base_blocks.json",["abyssalport:dreadium_block"])
    for path in [DATA/"tags/entity_type/dread_immune.json",DATA/"tags/entity_type/dread_rending_targets.json",
                 ROOT/"data/minecraft/tags/entity_type/undead.json",ROOT/"data/minecraft/tags/entity_type/can_breathe_under_water.json"]:
        append_tag(path.parent.parent.parent,path.relative_to(path.parent.parent.parent).as_posix(),["abyssalport:dreadguard"])
    write(DATA,"damage_type/dread.json",{"message_id":"dread","exhaustion":0.1,"scaling":"never"})
    for name in ["bypasses_armor","witch_resistant_to","avoids_guardian_thorns"]:
        append_tag(ROOT/"data/minecraft","tags/damage_type/"+name+".json",["abyssalport:dread"])
    biome_path = DATA/"worldgen/biome/dreadlands.json"
    biome = json.loads(biome_path.read_text(encoding="utf-8")); monsters = biome["spawners"]["monster"]
    monsters[:] = [entry for entry in monsters if entry["type"] != "abyssalport:dreadguard"]
    monsters.append({"type":"abyssalport:dreadguard","weight":8,"minCount":1,"maxCount":1})
    write(DATA,"worldgen/biome/dreadlands.json",biome)
    pairs = {
        "entity.abyssalport.dreadguard":["Dreadguard","Dreadwächter"],"item.abyssalport.dreadguard_spawn_egg":["Dreadguard Spawn Egg","Dreadwächter-Spawn-Ei"],
        "item.abyssalport.dreaded_shard":["Dreaded Shard of Abyssalnite","Gefürchteter Abyssalnit-Splitter"],
        "item.abyssalport.dreadium_ingot":["Dreadium Ingot","Dreadiumbarren"],"item.abyssalport.dreadium_nugget":["Dreadium Nugget","Dreadiumklumpen"],
        "block.abyssalport.dreadium_block":["Dreadium Block","Dreadiumblock"],"block.abyssalport.dreadstone_bricks":["Dreadstone Bricks","Dreadsteinziegel"],
        "item.abyssalport.dreadium_helmet":["Dreadium Helmet","Dreadiumhelm"],"item.abyssalport.dreadium_chestplate":["Dreadium Chestplate","Dreadiumbrustpanzer"],
        "item.abyssalport.dreadium_leggings":["Dreadium Leggings","Dreadiumbeinschutz"],"item.abyssalport.dreadium_boots":["Dreadium Boots","Dreadiumstiefel"],
        "death.attack.dread":["%1$s succumbed to Dread","%1$s erlag dem Dread"],"death.attack.dread.player":["%1$s succumbed to Dread while fighting %2$s","%1$s erlag dem Dread im Kampf gegen %2$s"],
        "research.abyssalport.dreadium":["Dreadium processing","Dreadium-Verarbeitung"],"screen.abyssalport.page.23":["Dreadguards and Dreadium","Dreadwächter und Dreadium"],
        "screen.abyssalport.dreadguard":["Dreadguards are rare, three-block-tall armoured elites in the Dreadlands. They have 60 health, heavy armour and a plague-inflicting melee attack. After their cooldown they stop moving for a three-second spray: stay clear of their face, use solid cover, and do not rely on armour against its magic damage. Dread carriers resist this plague. Ordinary melee damage can still hurt another carrier.","Dreadwächter sind seltene, drei Blöcke hohe gepanzerte Gegner in den Dreadlands. Sie haben 60 Gesundheit, starke Rüstung und einen pestübertragenden Nahkampfangriff. Nach ihrer Abklingzeit bleiben sie für einen dreisekündigen Sprühangriff stehen: Meide ihr Gesicht und nutze feste Deckung; Rüstung schützt nicht vor dem magischen Sprühschaden. Dread-Träger widerstehen der Pest. Gewöhnlicher Nahkampfschaden kann andere Träger trotzdem verletzen."],
        "screen.abyssalport.dreadium":["Defeating Dreadguards can drop Dreaded Shards of Abyssalnite. Process each shard in the Transmutator for one Dreadium Ingot; this is not a furnace recipe or an ore shortcut. Each ingot stores 0.2 experience in the machine. Nine ingots form one storage block; ingots and nuggets also convert nine-to-one and back. Four Dreadstone make four Dreadstone Bricks. These reachable materials prepare the next machinery tier.","Besiegte Dreadwächter können Gefürchtete Abyssalnit-Splitter fallen lassen. Verarbeite jeden Splitter im Transmutator zu einem Dreadiumbarren; dies ist kein Ofenrezept und keine Erz-Abkürzung. Pro Barren speichert die Maschine 0,2 Erfahrung. Neun Barren ergeben einen Speicherblock; auch Barren und Klumpen lassen sich neun zu eins und zurück umwandeln. Vier Dreadsteine ergeben vier Dreadsteinziegel. Diese erreichbaren Materialien bereiten die nächste Maschinenstufe vor."]}
    for index,locale in enumerate(["en_us","de_de"]):
        value=json.loads((ASSETS/"lang"/(locale+".json")).read_text(encoding="utf-8")); value.update({key:pair[index] for key,pair in pairs.items()}); write(ASSETS,"lang/"+locale+".json",value)
        note = [" Dreadium tools have 2,300 uses and mine level-six materials. Its four armour pieces use the usual ingot patterns and repair with Dreadium ingots. The chestplate grants brief Resistance and boots brief Speed only while equipped; the helmet does not add water breathing, and the set does not make you immune to plague.",
                " Dreadiumwerkzeuge haben 2.300 Nutzungen und erschließen Materialien der Stufe sechs. Die vier Rüstungsteile nutzen normale Barrenmuster und lassen sich mit Dreadiumbarren reparieren. Nur beim Tragen verleihen der Brustpanzer kurz Widerstand und die Stiefel kurz Tempo; der Helm gibt keine Unterwasseratmung und das Set macht nicht pestimmun."][index]
        value["screen.abyssalport.dreadium"] += note
        write(ASSETS,"lang/"+locale+".json",value)


if __name__ == "__main__":
    media(); resources()
