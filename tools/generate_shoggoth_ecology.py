"""Source food/three-form/dimensional rules on native types and own existing UV.

No reserved source bitmap/model/structure is read. Native head/body surfaces and
acid use own geometry/pixels. Later Omothol/Dark Realm ecology remains separate work.
"""
import json,math
from PIL import Image
from generate_wasteland_stronghold import ROOT,ASSETS,DATA,ID,write,texture
from generate_wasteland_minerals import append_tag

HABITATS=("overworld","abyssal","dreaded")
KINDS=("lesser","normal","greater")


def name(kind,habitat):
    return (kind+"_" if kind!="normal" else "")+(habitat+"_" if habitat!="overworld" else "")+"shoggoth"


def generate():
    glob=Image.new("RGBA",(16,16),(0,0,0,0))
    for y in range(2,14):
        for x in range(2,14):
            distance=((x-7.5)/5.8)**2+((y-7.5)/5.4)**2
            if distance<1:
                grain=((x*31+y*19)%13)-6; highlight=max(0,1-math.hypot(x-5,y-4)/5)*52
                glob.putpixel((x,y),(int(52+grain+highlight),int(107+grain+highlight),int(43+grain+highlight*.6),255))
    texture("item/shoggoth_acid_glob.png",glob)
    write(ASSETS,"models/item/shoggoth_acid_glob.json",{"parent":"minecraft:item/generated","textures":{"layer0":ID+"item/shoggoth_acid_glob"}})
    write(DATA,"damage_type/shoggoth_acid.json",{"message_id":"shoggoth_acid","exhaustion":.1,"scaling":"never"})
    for tag in ("bypasses_armor","bypasses_enchantments","bypasses_effects","bypasses_resistance"):
        append_tag(ROOT/"data/minecraft","tags/damage_type/"+tag+".json",[ID+"shoggoth_acid"])
    write(DATA,"tags/item/acid_resistant_armor.json",{"replace":False,"values":[]})
    owned=Image.open(ASSETS/"textures/entity/lesser_shoggoth.png").convert("RGBA")
    for habitat,gains in (("abyssal",(.65,1.02,1.15)),("dreaded",(1.25,.66,.9))):
        bitmap=Image.new("RGBA",owned.size)
        for y in range(owned.height):
            for x in range(owned.width):
                red,green,blue,alpha=owned.getpixel((x,y))
                bitmap.putpixel((x,y),tuple(min(255,round(c*g)) for c,g in zip((red,green,blue),gains))+(alpha,))
        texture("entity/"+habitat+"_shoggoth.png",bitmap)
    for habitat in HABITATS:
        ids=[ID+name(kind,habitat) for kind in KINDS]
        append_tag(ROOT/"data/minecraft","tags/entity_type/can_breathe_under_water.json",ids)
        if habitat!="overworld":
            for tag in (("coralium_immune","abyssal_rending_targets") if habitat=="abyssal" else ("dread_immune","dread_rending_targets")):
                append_tag(DATA,"tags/entity_type/"+tag+".json",ids)
        flesh={"overworld":"overworld_shoggoth_flesh","abyssal":"abyssal_shoggoth_flesh","dreaded":"dreaded_shoggoth_flesh"}[habitat]
        for kind in KINDS:
            write(DATA,"loot_table/entities/"+name(kind,habitat)+".json",{"type":"minecraft:entity","pools":[{"rolls":1,"entries":[
                {"type":"minecraft:item","name":ID+flesh,"functions":[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":0,"max":2}},
                    {"function":"minecraft:enchanted_count_increase","enchantment":"minecraft:looting","count":{"type":"minecraft:uniform","min":0,"max":1}}]}]}]})
            write(ASSETS,"models/item/"+name(kind,habitat)+"_spawn_egg.json",{"parent":"minecraft:item/template_spawn_egg"})
    write(DATA,"tags/entity_type/shoggoth_food.json",{"replace":False,"values":[]})
    base=json.loads((DATA/"worldgen/structure/shoggoth_lair.json").read_text("utf-8"))
    for structure,biomes,salt in (("wasteland_shoggoth_lair","#abyssalport:wasteland_graveyards",824301),("dreadlands_shoggoth_lair",ID+"dreadlands",824302)):
        write(DATA,"worldgen/structure/"+structure+".json",{**base,"biomes":biomes})
        write(DATA,"worldgen/structure_set/"+structure+".json",{"structures":[{"structure":ID+structure,"weight":1}],
            "placement":{"type":"minecraft:random_spread","salt":salt,"spacing":24,"separation":10}})
    for locale in ("en_us","de_de"):
        lang=json.loads((ASSETS/"lang"/(locale+".json")).read_text("utf-8"))
        lang["item.abyssalport.shoggoth_acid_glob"]="Shoggoth Acid" if locale=="en_us" else "Shoggoth-Säure"
        lang["entity.abyssalport.shoggoth_acid_projectile"]=lang["item.abyssalport.shoggoth_acid_glob"]
        lang["death.attack.shoggoth_acid"]="%1$s dissolved in Shoggoth acid" if locale=="en_us" else "%1$s wurde von Shoggoth-Säure aufgelöst"
        lang["death.attack.shoggoth_acid.player"]="%1$s dissolved in Shoggoth acid while fighting %2$s" if locale=="en_us" else "%1$s wurde im Kampf mit %2$s von Shoggoth-Säure aufgelöst"
        for habitat in HABITATS:
            for kind in KINDS:
                entity=name(kind,habitat)
                if locale=="en_us":
                    title=(kind.title()+" " if kind!="normal" else "")+(habitat.title()+" " if habitat!="overworld" else "")+"Shoggoth"
                else:
                    title=("Kleiner " if kind=="lesser" else "Großer " if kind=="greater" else "")+("Abyssaler " if habitat=="abyssal" else "Schreckens-" if habitat=="dreaded" else "")+"Shoggoth"
                lang["entity.abyssalport."+entity]=title
                lang["item.abyssalport."+entity+"_spawn_egg"]=title+(" Spawn Egg" if locale=="en_us" else "-Spawn-Ei")
        lang["screen.abyssalport.shoggoth_growth"]=(
            "Shoggoths hunt players, animals, ambient/water creatures, spiders, ordinary zombies and skeletons; only the registered animal/ambient/water/spider prey feeds them. A killed creature gives one food point per quarter block of body area, rounded up. Eight points turn a Lesser into a normal Shoggoth, while normal/Greater parents produce a Lesser/normal offspring. Growth waits for actual collision-safe admission, keeping food if blocked or cancelled. All forms climb, breathe underwater and resist poison, cactus and projectiles. Dimensional lairs can rarely supply Greater forms. Abyssal/Dreaded accepted hits apply the matching plague and their native remains feed dimensional crystallization. Head/body surfaces share native health. Larger forms spit acid every 120 ticks at distant targets; newborns can ride a spit. Acid wears ordinary armor and shields, but block corrosion obeys mob griefing and protects biomass, ooze, monolith stone and block entities. The later two worlds and deity catalogue remain development."
            if locale=="en_us" else
            "Shoggoths jagen Spieler, Tiere, Umgebungs-/Wasserkreaturen, Spinnen, normale Zombies und Skelette; nur registrierte Tier-/Umgebungs-/Wasser-/Spinnenbeute ernährt sie. Eine getötete Kreatur liefert einen Nahrungspunkt je Viertelblock Körperfläche, aufgerundet. Acht Punkte verwandeln einen kleinen in einen normalen Shoggoth; normale/große Eltern erzeugen kleine/normale Nachkommen. Wachstum wartet auf kollisionsfreie Aufnahme und behält Nahrung bei Hindernissen oder Vetos. Alle Formen klettern, atmen unter Wasser und widerstehen Gift, Kakteen und Projektilen. Dimensionshöhlen liefern selten große Formen. Abyssale/Schreckens-Treffer verursachen die passende Pest, ihre echten Reste dienen der Dimensionskristallisation. Kopf/Körper teilen native Lebenspunkte. Größere Formen spucken alle 120 Ticks Säure auf entfernte Ziele; Neugeborene können mitfliegen. Säure verschleißt normale Rüstung und Schilde; Blockkorrosion beachtet Mob-Griefing und schützt Biomasse, Schleim, Monolithstein und Block-Entities. Die zwei späteren Welten und der Gottheitenkatalog sind noch Entwicklung.")
        write(ASSETS,"lang/"+locale+".json",lang)


if __name__=="__main__": generate()
