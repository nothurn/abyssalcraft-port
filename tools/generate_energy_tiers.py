"""Connected source PE tiers with native data and independently authored media.

Uses only this project's PE cuboids and pixels. Upstream informs values/recipes,
never textures/models. Omothol remains unregistered until its real progression.
"""
import copy
import json
from PIL import Image, ImageDraw, ImageOps
from build_pe_models import DEVICES
from generate_abyssalnite_assets import ROOT, ASSETS, DATA, resource, texture
from generate_wasteland_minerals import append_tag

TIERS = (
    ("overworld", "Overworld", "Oberwelt", "#292e23", "#c4d896", "minecraft:cobblestone", "minecraft:cobblestone"),
    ("abyssal_wasteland", "Abyssal Wasteland", "Abyssales Ödland", "#172e2c", "#94d5c3", "abyssalport:abyssal_cobblestone", "abyssalport:coralium_cobblestone"),
    ("dreadlands", "Dreadlands", "Dreadlands", "#301d26", "#e6a393", "abyssalport:dreadstone_cobblestone", "abyssalport:elysian_cobblestone"),
)


def ring_media(name, shade, tier):
    image = Image.new("RGBA", (16, 16)); draw = ImageDraw.Draw(image)
    # Small cut, square gold band; the tier inset never conceals its ring hole.
    draw.polygon([(5, 2), (10, 2), (13, 5), (13, 10), (10, 13), (5, 13), (2, 10), (2, 5)], fill="#82602b")
    draw.line([(5, 2), (10, 2), (13, 5), (13, 9)], fill="#f0d78b", width=2)
    draw.line([(2, 6), (2, 10), (5, 13), (10, 13)], fill="#b18b44", width=2)
    draw.polygon([(6, 5), (9, 5), (10, 6), (10, 9), (9, 10), (6, 10), (5, 9), (5, 6)], fill=(0, 0, 0, 0))
    draw.rectangle((6, 1, 9, 3), fill=shade); draw.point((6, 1), fill="#fff0c2")
    for step in range(tier): draw.point((12-step, 11), fill=shade)
    texture("item/"+name+".png", image)
    resource(ASSETS, "models/item/"+name+".json", {"parent": "minecraft:item/generated", "textures": {"layer0": "abyssalport:item/"+name}})


def main():
    sprite=Image.new("RGBA",(8,8)); pixels=sprite.load()
    for y in range(8):
        for x in range(8):
            distance=abs(x-3.5)+abs(y-3.5)
            if distance<4: pixels[x,y]=(163,225,183,255 if distance<2 else 150)
    texture("particle/pe_stream.png",sprite)
    resource(ASSETS,"particles/pe_stream.json",{"textures":["abyssalport:pe_stream"]})
    for kind, pattern in (("collector", ["###", "#&#", "###"]), ("relay", ["##%", "#&#", "##%"] )):
        key={"#":{"item":"abyssalport:monolith_stone"},"&":{"item":"abyssalport:shadow_gem"}}
        if kind=="relay": key["%"]={"item":"abyssalport:shadow_shard"}
        resource(DATA,"recipe/energy_"+kind+".json",{"type":"minecraft:crafting_shaped","category":"misc",
            "pattern":pattern,"key":key,"result":{"id":"abyssalport:energy_"+kind}})
        resource(DATA,"advancement/recipes/energy_"+kind+".json",{"criteria":{"has_material":{"trigger":"minecraft:inventory_changed",
            "conditions":{"items":[{"items":["abyssalport:shadow_gem"]}]}}},"rewards":{"recipes":["abyssalport:energy_"+kind]}})
    texts = {"item.abyssalport.embossed_ring": ("Embossed Ring", "Geprägter Ring")}
    ring_media("embossed_ring", "#d3aa53", 0)
    ring_names = ["embossed_ring"]
    resource(DATA, "recipe/embossed_ring.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["#%#", "% %", "#%#"], "key": {"#": {"item": "minecraft:blaze_powder"}, "%": {"item": "minecraft:gold_ingot"}},
        "result": {"id": "abyssalport:embossed_ring"}})
    resource(DATA, "advancement/recipes/embossed_ring.json", {"criteria": {"has_material": {"trigger": "minecraft:inventory_changed",
        "conditions": {"items": [{"items": ["minecraft:blaze_powder"]}]}}}, "rewards": {"recipes": ["abyssalport:embossed_ring"]}})
    blocks = []
    base = "energy_pedestal"
    device = DEVICES[base]
    textures = {key: "abyssalport:block/"+source for key, source in device["textures"].items()}
    textures["particle"] = "#stone"
    model = {"textures": textures, "elements": copy.deepcopy(device["elements"])}
    resource(ASSETS, "models/block/"+base+".json", model)
    tilted = copy.deepcopy(model)
    for part in tilted["elements"][-2:]:
        part["rotation"] = {"origin": [8, 14, 8], "axis": "y", "angle": 45, "rescale": False}
    resource(ASSETS, "models/block/"+base+"_tilted.json", tilted)
    resource(ASSETS, "models/item/"+base+".json", {"parent": "abyssalport:block/"+base, "display": {
        "gui": {"rotation": [30, 225, 0], "scale": [0.8]*3}, "ground": {"scale": [0.35]*3}, "fixed": {"scale": [0.5]*3}}})
    resource(ASSETS, "blockstates/"+base+".json", {"variants": {
        "tilted=false": {"model": "abyssalport:block/"+base}, "tilted=true": {"model": "abyssalport:block/"+base+"_tilted"}}})
    resource(DATA, "loot_table/blocks/"+base+".json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": "abyssalport:"+base, "functions": [{"function": "minecraft:copy_custom_data", "source": "block_entity",
            "ops": [{"source": "PotEnergy", "target": "PotEnergy", "op": "replace"}]}]}]}]})
    resource(DATA, "recipe/"+base+".json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["#%#", "#&#", "###"],
        "key": {"#": {"item": "abyssalport:monolith_stone"}, "%": {"item": "abyssalport:coralium_pearl"}, "&": {"item": "abyssalport:shadow_gem"}},
        "result": {"id": "abyssalport:"+base}})
    resource(DATA, "advancement/recipes/"+base+".json", {"criteria": {"has_material": {"trigger": "minecraft:inventory_changed",
        "conditions": {"items": [{"items": ["abyssalport:coralium_pearl"]}]}}}, "rewards": {"recipes": ["abyssalport:"+base]}})
    resource(DATA, "rituals/energy_container.json", {"energy_cost": 100, "duration_ticks": 20,
        "altar_input": "abyssalport:energy_pedestal", "preserve_energy": True,
        "offerings": ["abyssalport:energy_collector", "abyssalport:shadow_shard"]*4,
        "result": {"item": "abyssalport:energy_container", "count": 1}})
    # Keep the old prototype file as history, but never load its cheap bypass.
    old = json.loads((DATA/"recipe/energy_container.json").read_text(encoding="utf-8"))
    old["neoforge:conditions"] = [{"type": "neoforge:false"}]
    resource(DATA, "recipe/energy_container.json", old)
    blocks.append("abyssalport:"+base)
    texts["block.abyssalport.energy_pedestal"] = ("Energy Pedestal", "Energiepodest")
    for tier, (prefix, english, german, dark, light, first, second) in enumerate(TIERS, 1):
        ring = prefix+"_embossed_ring"; ring_media(ring, light, tier)
        texts["item.abyssalport."+ring] = (english+" Embossed Ring", german+"-Prägering")
        offers = ["abyssalport:shadow_shard", first, "abyssalport:coralium_gem", second]*2
        for centre in ring_names:
            resource(DATA, "rituals/"+ring+"_from_"+centre+".json", {"energy_cost": 400, "duration_ticks": 20,
                "book_tier": tier-1, "altar_tier": 0, "altar_input": "abyssalport:"+centre,
                "offerings": offers, "result": {"item": "abyssalport:"+ring, "count": 1}})
        ring_names.append(ring)
        for base, device in DEVICES.items():
            name = prefix+"_"+base; blocks.append("abyssalport:"+name)
            model = copy.deepcopy(json.loads((ASSETS/("models/block/"+base+".json")).read_text(encoding="utf-8")))
            for key, source in device["textures"].items():
                tinted_name = prefix+"_"+source
                # Reuse our authored texture detail/alpha, not upstream media.
                image = Image.open(ASSETS/("textures/block/"+source+".png")).convert("RGBA")
                tinted = ImageOps.colorize(ImageOps.grayscale(image), dark, light).convert("RGBA")
                tinted.putalpha(image.getchannel("A")); texture("block/"+tinted_name+".png", tinted)
                model["textures"][key] = "abyssalport:block/"+tinted_name
            resource(ASSETS, "models/block/"+name+".json", model)
            item = json.loads((ASSETS/("models/item/"+base+".json")).read_text(encoding="utf-8"))
            item["parent"] = "abyssalport:block/"+name
            resource(ASSETS, "models/item/"+name+".json", item)
            state = json.loads((ASSETS/("blockstates/"+base+".json")).read_text(encoding="utf-8"))
            for variant in state["variants"].values(): variant["model"] = "abyssalport:block/"+name
            resource(ASSETS, "blockstates/"+name+".json", state)
            loot = json.loads((DATA/("loot_table/blocks/"+base+".json")).read_text(encoding="utf-8"))
            for pool in loot["pools"]:
                for entry in pool["entries"]: entry["name"] = "abyssalport:"+name
            resource(DATA, "loot_table/blocks/"+name+".json", loot)
            kind = base.removeprefix("energy_")
            if kind == "pedestal":
                other = copy.deepcopy(model)
                for part in other["elements"][-2:]:
                    part["rotation"] = {"origin": [8, 14, 8], "axis": "y", "angle": 45, "rescale": False}
                resource(ASSETS, "models/block/"+name+"_tilted.json", other)
                state["variants"]["tilted=true"]["model"] = "abyssalport:block/"+name+"_tilted"
                resource(ASSETS, "blockstates/"+name+".json", state)
            en = {"collector": "Energy Collector", "relay": "Energy Relay", "container": "Energy Container", "pedestal": "Energy Pedestal"}[kind]
            de = {"collector": "Energiesammler", "relay": "Energierelais", "container": "Energiespeicher", "pedestal": "Energiepodest"}[kind]
            texts["block.abyssalport."+name] = (english+" "+en, german+"-"+de)
    for tag in ("mineable/pickaxe", "needs_stone_tool"):
        append_tag(ROOT/"data/minecraft", "tags/block/"+tag+".json", blocks)
    texts.update({
        "screen.abyssalport.network": (
            "Craft a Collector from eight Monolith Stones around a Shadow Gem; a Relay uses six Monolith Stones, one Shadow Gem and two Shadow Shards along one edge. A Relay draws from any PE device directly behind it and sends to the FIRST device ahead. Placement follows the clicked face; empty-hand use cycles six directions. Full blocks obstruct its beam; partial shapes do not. Redstone pauses it. A full first receiver is not skipped.",
            "Sammler: acht Monolithsteine um einen Schattenedelstein. Relais: sechs Monolithsteine, ein Schattenedelstein und zwei Schattensplitter an einer Kante. Ein Relais zieht PE aus jedem Gerät direkt dahinter und sendet zum ERSTEN Gerät davor. Platzierung folgt der angeklickten Seite; leere Hand schaltet sechs Richtungen. Vollblöcke sperren den Strahl, Teilformen nicht. Redstone pausiert. Volle erste Empfänger werden nicht übersprungen."),
        "screen.abyssalport.network_note": (
            "Base: 4-block reach, 10 PE per 20 ticks in, 20 per 40 ticks out, 10,000 reservoir capacity. Open the reservoir: left drains, right charges books or device items at 20 PE/tick. Craft an Embossed Ring with four gold ingots and four blaze powder. Infuse it on a base ritual ground with a 400-PE book; rings upgrade ONE device in an anvil for one level. Names and stored PE remain intact.",
            "Basis: 4 Blöcke Reichweite, 10 PE je 20 Ticks hinein, 20 je 40 Ticks hinaus, Speicher 10.000 PE. Im Speicher entlädt links und lädt rechts Bücher oder Geräte-Items mit 20 PE/Tick. Geprägter Ring: vier Goldbarren, vier Lohenstaub. Mit 400 PE auf Basis-Ritualgrund infundieren. Ring wertet EIN Gerät am Amboss für eine Stufe auf; Name und PE bleiben erhalten."),
        "tooltip.abyssalport.energy_upgrade": (
            "Anvil: one lower-tier device + this ring; one XP level.",
            "Amboss: ein schwächeres Gerät + dieser Ring; eine XP-Stufe."),
        "screen.abyssalport.energy_pedestal": (
            "Energy Pedestal: seven Monolith Stones, a Coralium Pearl and a Shadow Gem. Place one book or PE device on top; the pedestal charges it from its stored PE at 20 PE/tick. Empty-hand use retrieves the actual item; a full inventory keeps it on the pedestal. It receives statue PE like a Collector; adjacent collectors or energy pedestals block that charging. Relays can supply it or draw PE from it. Capacities: 5,000 / 7,500 / 10,000 / 12,500 PE.",
            "Energiepodest: sieben Monolithsteine, Coralium-Perle und Schattenedelstein. Ein Buch oder PE-Gerät auflegen: Das Podest lädt es mit 20 PE/Tick aus seinem Speicher. Leere Hand nimmt das echte Item zurück; bei vollem Inventar bleibt es liegen. Es empfängt Statuen-PE wie ein Sammler; direkt angrenzende Sammler/Podeste sperren die Ladung. Relais können PE zuführen oder abziehen. Kapazitäten: 5.000 / 7.500 / 10.000 / 12.500 PE."),
        "screen.abyssalport.energy_container_ritual": (
            "Reservoir infusion: place one Energy Pedestal on a base altar, offer four Energy Collectors and four Shadow Shards, and use a book with 100 PE. The pedestal's stored PE transfers to the reservoir, but not unrelated item tags. Empty its displayed item before mining it; breaking returns that item separately. The old prototype chest craft is no longer active.",
            "Speicher-Infusion: ein Energiepodest auf Basisaltar, vier Energiesammler und vier Schattensplitter opfern; Buch mit 100 PE verwenden. Gespeicherte Podest-PE gehen in den Speicher über, keine fremden Item-Tags. Aufgelegtes Item vor Abbau entnehmen; beim Abbau fällt es getrennt. Das alte Prototyp-Truhenrezept ist nicht mehr aktiv."),
        "tooltip.abyssalport.pe_ring.0": (
            "Infuse with a 400-PE Necronomicon on base ritual ground.",
            "Mit 400-PE-Necronomicon auf Basis-Ritualgrund infundieren."),
        "tooltip.abyssalport.pe_ring.1": (
            "Infusion: 2 Shadow Shards, 2 Coralium Gems, 4 Cobblestone.",
            "Infusion: 2 Schattensplitter, 2 Coralium-Edelsteine, 4 Bruchstein."),
        "tooltip.abyssalport.pe_ring.2": (
            "Tier-one book; 2 each: Shadow Shard, Coralium Gem, Abyssal and Coralium Cobblestone.",
            "Stufe-1-Buch; je 2: Schattensplitter, Coralium-Edelstein, Abyssal- und Coralium-Bruchstein."),
        "tooltip.abyssalport.pe_ring.3": (
            "Tier-two book; 2 each: Shadow Shard, Coralium Gem, Dreadstone and Elysian Cobblestone.",
            "Stufe-2-Buch; je 2: Schattensplitter, Coralium-Edelstein, Dreadstone- und Elysian-Bruchstein."),
    })
    for index, language in enumerate(("en_us", "de_de")):
        path = ASSETS/("lang/"+language+".json")
        values = json.loads(path.read_text(encoding="utf-8")); values.update({key: pair[index] for key, pair in texts.items()})
        resource(ASSETS, "lang/"+language+".json", values)
    print("Generated 13 new reachable PE devices, four owned ring icons, six ring infusions and canonical reservoir infusion; no Omothol placeholders.")


if __name__ == "__main__": main()
