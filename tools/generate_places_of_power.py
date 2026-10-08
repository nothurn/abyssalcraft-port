"""Own Places-of-Power guide/resources, using only locally authored stone pixels.

Run after generate_statues, generate_charms and generate_statue_energy.
The Java layout is authoritative for both the native guide diagrams and validation.
"""
import json
from generate_wasteland_stronghold import ROOT, ASSETS, DATA, ID, write
from generate_wasteland_minerals import append_tag


def generate():
    write(ASSETS, "models/block/place_of_power_base.json", {"parent": ID + "block/monolith_stone"})
    write(ASSETS, "blockstates/place_of_power_base.json", {"variants": {"": {"model": ID + "block/place_of_power_base"}}})
    write(DATA, "loot_table/blocks/place_of_power_base.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "entries": [{"type": "minecraft:item", "name": ID + "monolith_stone"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    append_tag(ROOT / "data/minecraft", "tags/block/mineable/pickaxe.json", [ID + "place_of_power_base"])
    texts = {
        "block.abyssalport.place_of_power_base": ("Place of Power", "Ort der Macht"),
        "screen.abyssalport.page.27": ("Places of Power: Basic", "Orte der Macht: Basis"),
        "screen.abyssalport.page.28": ("Places of Power: Totem", "Orte der Macht: Totem"),
        "screen.abyssalport.page.29": ("Places of Power: Archway", "Orte der Macht: Torbogen"),
        "screen.abyssalport.power.basic": ("Basic Structure", "Basisstruktur"),
        "screen.abyssalport.power.totempole": ("Totem Pole", "Totempfahl"),
        "screen.abyssalport.power.archway": ("Archway", "Torbogen"),
        "screen.abyssalport.power.guide": (
            "Build every shown block, then right-click the activation stone (*) with any Necronomicon. The book and its PE are not consumed. Any of the seven deity statues may be used, including mixed deities. Valid structures allow charging indoors and beside their other statues, and add no new tolerance. Break a required block and these benefits stop immediately; replace it to repair. Mining the activation block returns Monolith Stone, not a portable formed structure.",
            "Baue alle gezeigten Blöcke und rechtsklicke den Aktivierungsstein (*) mit einem beliebigen Necronomicon. Buch und PE bleiben erhalten. Alle sieben Gottheiten-Statuen sind erlaubt, auch gemischt. Gültige Strukturen laden drinnen und neben ihren anderen Statuen ohne neue Toleranz. Beim Abbau eines Pflichtblocks enden diese Vorteile sofort; Ersetzen repariert den Bau. Abbau des Aktivierungsblocks gibt Monolithstein zurück, keine tragbare fertige Struktur."),
        "screen.abyssalport.power.materials.basic": (
            "15 Monolith Stone + 4 Monolith Pillars + 4 statues. Activate the top center stone. Range +8 blocks; collector search radius +2 and depth +2. The lower 3x3 layer is solid stone; the middle layer uses pillars only in its four corners.",
            "15 Monolithsteine + 4 Monolithsäulen + 4 Statuen. Aktiviere den oberen Mittelstein. Reichweite +8 Blöcke; Sammler-Suchradius und Tiefe jeweils +2. Die untere 3x3-Lage ist voller Stein, die mittlere hat Säulen nur in den vier Ecken."),
        "screen.abyssalport.power.materials.totempole": (
            "1 Monolith Stone + 3 statues stacked directly above it. Activate the bottom stone. Range +12 blocks; collector search radius +3 and depth +3. Statues must be real deity statues, not prototype effigies.",
            "1 Monolithstein + 3 direkt darauf gestapelte Statuen. Aktiviere den unteren Stein. Reichweite +12 Blöcke; Sammler-Suchradius und Tiefe jeweils +3. Es zählen echte Gottheiten-Statuen, keine vorläufigen Bildnisse."),
        "screen.abyssalport.power.materials.archway": (
            "1 Monolith Stone + 1 statue + 6 walls + 4 stairs + 6 slabs. Any wall/stair/slab material works. Activate the floor center stone. Range +4 blocks; collector radius and depth +1. Both supports, all roof blocks, stair directions and upper/lower halves must match. Rotate the entire plan together for a north-south arch.",
            "1 Monolithstein + 1 Statue + 6 Mauern + 4 Treppen + 6 Stufen. Beliebige Mauer-/Treppen-/Stufenmaterialien sind erlaubt. Aktiviere den Mittelstein am Boden. Reichweite +4 Blöcke; Sammler-Radius und Tiefe +1. Beide Stützen, alle Dachteile, Treppenrichtungen und Hälften müssen stimmen. Für Nord-Süd den gesamten Plan gemeinsam drehen."),
        "screen.abyssalport.power.diagram": (
            "Top view per layer; north is up, east is right. * = activation stone. E/W = stair facing east/west; T/B = upper/lower half. Empty cells are not required air blocks. All layers share the same center.",
            "Draufsicht je Lage: Norden oben, Osten rechts. * = Aktivierungsstein. E/W = Treppe nach Osten/Westen; T/B = obere/untere Hälfte. Leere Felder sind keine vorgeschriebenen Luftblöcke. Alle Lagen haben denselben Mittelpunkt."),
        "screen.abyssalport.power.layer": ("Layer %s (activation = 0)", "Lage %s (Aktivierung = 0)"),
        "message.abyssalport.power.status": ("%s: %s", "%s: %s"),
        "message.abyssalport.power.active": ("Complete — bonuses active", "Vollständig — Boni aktiv"),
        "message.abyssalport.power.inactive": ("Incomplete/unloaded — no bonuses", "Unvollständig/ungeladen — keine Boni"),
        "message.abyssalport.power.unknown": ("Unknown structure", "Unbekannte Struktur"),
        "message.abyssalport.power.formation.created": ("Place of Power formed. Its bonuses require the complete structure.", "Ort der Macht geformt. Seine Boni brauchen den vollständigen Bau."),
        "message.abyssalport.power.formation.incomplete": ("Incomplete structure. See the Places of Power pages in your book.", "Unvollständige Struktur. Siehe die Orte-der-Macht-Seiten im Buch."),
        "message.abyssalport.power.formation.protected": ("You cannot form a structure here.", "Du darfst hier keine Struktur formen."),
        "message.abyssalport.power.formation.already_linked": ("A statue already belongs to another complete structure.", "Eine Statue gehört bereits zu einer anderen vollständigen Struktur."),
        "message.abyssalport.power.formation.not_stone": ("Activate an ordinary Monolith Stone.", "Aktiviere einen gewöhnlichen Monolithstein."),
        "message.abyssalport.power.formation.no_book": ("Use a Necronomicon to form this structure.", "Forme diese Struktur mit einem Necronomicon."),
    }
    for locale, index in (("en_us", 0), ("de_de", 1)):
        lang = json.loads((ASSETS / "lang" / (locale + ".json")).read_text("utf-8"))
        lang.update({key: value[index] for key, value in texts.items()})
        key = "screen.abyssalport.energy_note"
        lang[key] = lang[key].replace(
            "its complete disruption catalogue and validated structures are still development.",
            "complete disruptions are still development. The three Places of Power are explained at the end of this book.").replace(
            "kompletter Disruptionskatalog und validierte Strukturen sind noch Entwicklung.",
            "der komplette Disruptionskatalog ist noch Entwicklung. Die drei Orte der Macht stehen am Ende dieses Buchs.")
        key = "screen.abyssalport.energy"
        lang[key] = lang[key].replace("full disruptions and structure bonuses remain unfinished.", "the three validated structures are implemented; full disruptions remain unfinished.")
        lang[key] = lang[key].replace("volle Disruptions und Strukturboni bleiben unfertig.", "die drei validierten Strukturen sind implementiert; volle Disruptions bleiben unfertig.")
        write(ASSETS, "lang/" + locale + ".json", lang)


if __name__ == "__main__":
    generate()
