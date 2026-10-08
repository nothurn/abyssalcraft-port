"""Owned bilingual labels/help for the actual source disruption catalogue. No media input."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]/"src/main/resources/assets/abyssalport/lang"
NAMES=[
    ("lightning","Lightning","Blitzschlag"),("fire","Ignition","Entzündung"),
    ("spawnShoggoth","Shoggoth arrival","Shoggoth erscheint"),("poisonPotion","Poison","Gift"),
    ("slownessPotion","Slowness","Verlangsamung"),("weaknessPotion","Weakness","Schwäche"),
    ("witherPotion","Wither","Wither"),("coraliumPotion","Coralium plague","Coraliumpest"),
    ("potentialEnergy","Energy backlash","Energierückschlag"),("freeze","Freeze","Erstarrung"),
    ("swarmShadow","Shadow swarm","Schattenschwarm"),("fireRain","Fire rain","Feuerregen"),
    ("displace","Displacement","Vertauschte Positionen"),("randomTeleport","Random teleport","Zufällige Teleportation"),
    ("potentialEnergyDrain","Energy drain","Energieverlust"),("swarmSheep","Sheep swarm","Schafschwarm"),
    ("animalCorruption","Animal corruption","Tierverderbnis"),("ooze","Ooze","Schleim"),
    ("randomSwarm","Biome swarm","Biomschwarm"),("randomSpawn","Biome creature","Biomkreatur"),
    ("spawnShubOffspring","Shub offspring","Shub-Nachkomme"),
    ("invisibleSwarmHastur","Invisible swarm","Unsichtbarer Schwarm"),("invisibleSwarmNyarlathotep","Invisible swarm","Unsichtbarer Schwarm"),
    ("sacrificeCorruptionJzahar","Sacrifice corruption","Opferverderbnis"),("sacrificeCorruptionYogSothoth","Sacrifice corruption","Opferverderbnis"),
    ("famineAzathoth","Famine","Hungersnot"),("famineShuNiggurath","Famine","Hungersnot"),
]
GUIDES={
    "en_us":"Unprotected statue charging accumulates tolerance: accepted book transfers add 2 (4 with a charm), collectors 1 (2 with a charm). At 100 the statue consumes its tolerance and selects one of the 18 common disruptions plus its deity's entries. Warning lightning is visual only; the selected effect can ignite, freeze, infect, teleport, drain PE or attract actual creatures. Shub's entries include animal corruption and offspring; equipped/ridden/leashed mounts are retained rather than losing gear. J'zahar and Yog-Sothoth can turn sacrifices into Depths Ghouls. Hastur and Nyarlathotep can summon invisible Endermen. Complete validated Places of Power prevent disruptions; saved labels alone do not protect you. The common config can disable disruptions. A single action-bar warning names the selected effect, without a permanent chat wall.",
    "de_de":"Ungeschütztes Statuenladen sammelt Toleranz: angenommene Buchtransfers kosten 2 (mit Charm 4), Sammler 1 (mit Charm 2). Bei 100 verbraucht die Statue ihre Toleranz und wählt eine der 18 allgemeinen Störungen oder einen Effekt ihrer Gottheit. Der Warnblitz ist nur sichtbar; der gewählte Effekt kann entzünden, erstarren, infizieren, teleportieren, PE entziehen oder echte Kreaturen anziehen. Shub kann Tiere verderben und Nachkommen rufen; ausgerüstete, gerittene oder angeleinte Reittiere bleiben erhalten, damit keine Ausrüstung verloren geht. J'zahar und Yog-Sothoth verwandeln Opfer in Tiefenghouls. Hastur und Nyarlathotep können unsichtbare Endermen rufen. Vollständige, aktuell geprüfte Orte der Macht verhindern Störungen; gespeicherte Markierungen allein schützen nicht. Die Common-Konfiguration kann Störungen abschalten. Ein einzelner Action-Bar-Hinweis benennt den Effekt, ohne den Chat dauerhaft zu füllen.",
}
for locale,column in (("en_us",1),("de_de",2)):
    path=ROOT/f"{locale}.json"
    data=json.loads(path.read_text(encoding="utf-8"))
    data.update({f"disruption.abyssalport.{row[0]}":row[column] for row in NAMES})
    data["message.abyssalport.disruption"]="Statue disruption: %s" if locale=="en_us" else "Statuen-Störung: %s"
    data["screen.abyssalport.disruptions.guide"]=GUIDES[locale]
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
