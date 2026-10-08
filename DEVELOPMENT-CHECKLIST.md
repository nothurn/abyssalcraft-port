# Vollständiger Entwicklungsumfang

Stand: 08.10.2026. Der vollständige, am Original orientierte Entwicklungsumfang
bleibt Roadmap. Noah priorisiert jetzt ausdrücklich eine erste Feedback-Beta:
aktuelles Maschinenpaket abschließen, stabilen frühen Survival-Pfad prüfen und
den späteren Vollumfang nicht zur Voraussetzung erster Rückmeldungen machen.
`BETA-GATE.md` grenzt diese erste Beta ab. Client-Start und Installation bleiben
ohne neue ausdrückliche Freigabe gesperrt; kein Upload/Release ist beauftragt.

## Arbeits- und Nachweisregeln

- Die Originalreferenz bleibt `../upstream/` am in `../SOURCE-AUDIT.md` genannten
  Commit. Java-Herkunft/LGPL und die getrennte Asset-Lizenz bleiben erhalten.
- **Technisch geprüft** bedeutet passende geladene Rezepte/Registries, tatsächliche
  Server-Interaktionen, Persistenz und gepackte Ressourcen. Es bedeutet keine
  optische oder spielerische Abnahme.
- **Teilweise** ist kein fertiges System. Ein Item ohne erreichbaren Survival-Weg,
  ein Dimensions-Stub oder ein bloß registrierter Boss schließt keinen Punkt ab.
- [ ] bleibt offen; [x] bezeichnet ausschließlich den ausdrücklich genannten
  technischen Teilnachweis. Sichtprüfung wird gesammelt am Ende nachgeholt und
  blockiert die übrige Implementierung nicht.

## 1. Früher Overworld-Pfad

- [x] Necronomicon-Rezept, PE-Ladung und Speicherwerte, Ritualplatz, Podest- und
  Altar-Rechtsklicks, genau eine Basic Scroll: bestehende 29 Server-GameTests.
- [x] Erster Darklands-Biom-/Darkstone-/Schattenmaterialpfad; Generierung in zwei
  normalen Headless-Welten nachgewiesen (`BETA-GATE.md`).
- [ ] Vollständige Darklands-Familie: Plains/Forest/Highland/Mountains,
  eigenes Holz/Vegetation, Gebäude/Ritualplätze/Gräber und Shoggoth-Lairs.
  Referenz: `init/WorldHandler`, `common/world/biome/BiomeDarklands*`,
  `common/world/DarklandsStructureGenerator`, `common/structures/overworld`.
  **Teilnachweis Holz:** eigenes erneuerbares Darklands-Oak mit 9–11er Stamm,
  Wurzeln/aufsteigenden Ästen/hohler Variante, nativen Setzling-/Knochenmehlstufen,
  Hindernisschutz/Laub-Drops/Brettern/Holzkohle und vollständiger Basis-Holzfamilie.
  Tatsächliches Naturwelt-Sample: 968 Stämme/614 Laubblöcke in neun Chunks.
  Native Türhälften/Loot, Knopf-Redstone, Doppelplatten, Crafting, Brennstoff/Feuer
  sind geprüft. Vier weitere native Biome/Climate-/Oberflächenregeln sind nun
  mit tatsächlichem TerraBlender und Naturterrain belegt. Zehn eigene natürliche
  Häuser/Gedenkbauten/Schreine/Ritualplätze, leuchtende Ziegel und der echte
  Dunkelstein-Stufe-null-Ritualpfad sind geprüft. Ein Naturschrein lädt tatsächlich
  5 PE in 145 Server-Ticks. Sechs eigene natürliche Friedhof-NBTs, Stein-/Dunkelstein-
  Grabsteine und normale/Schatten-Ghouls sind jetzt verbunden: fünf echte lebende
  Schatten-Ghouls nach 235 Natursystem-Ticks, Nacht/Cap/Save/Vetos/Hindernisse,
  tatsächliche Todesdrops → Crystallizer und schädliche Nahrung geprüft.
  **Weiterer Teilnachweis:** vier Ghoul-Typen, 18 eigene Templates in drei echten
  Welten, sechs Grabmaterialien, Baby-/Namens-/Kopfloot-/Nahrungsregeln, native
  Dread-Erde/Gras und insgesamt 63 Crystallizer-Rezepte. Natürliche Ödland-/Dread-
  Friedhöfe erheben 5/4 lebende Ghouls durch 235 tatsächliche Ticks; NBT-Paletten
  werden gegen die Blockregistry geprüft. Omothol/Dark Realm, Spirit-Tablet-
  Grabschatz, Shoggoth-Lairs und die vollständige
  Statues-/Idol-of-Fading-Kette bleiben offen; daher der Gesamtpunkt weiterhin [ ].
- [x] Abyssalnite technisch geprüft: Erz → Schmelzen → Barren/Nuggets/Block → alle fünf Werkzeuge
  und vier Rüstungsteile, Reparatur, Effekte, Forschung und eigene Assets.
  Referenz: `BiomeDarklandsBase.decorate`, `BlockACOre`, `AbyssalCrafting`,
  `AbyssalCraftAPI.abyssalniteTool/Armor`, `ItemAbyssalniteArmor`.
  Drei neue Server-GameTests; normale Headless-Welt enthält tatsächliches Erz.
  Erzverteilung/Balance und Rüstungsdarstellung sind noch nicht spielerisch geprüft.
- [ ] Coralium/Nitre: natürliche Biome/Erze, Gems/Cluster/Perlen, Verarbeitung
  und erreichbare Zutaten für Transmutation/Gateway. Referenz:
  `AbyssalCraftWorldGenerator`, `AbyssalCrafting`, `init/ItemHandler`.
  **Teilnachweis Coralium:** Ocean-/Swamp-/Deep-Ocean-Platzierungen, 1–3 Gems,
  Cluster 2–9 und Rückrezepte, zwei Infused-Stone-Rezepte, Pearl-Schmelzen,
  300-PE-Infusion und ein zehnmal nutzbarer Transmutation Gem → Oblivion Shard.
  Fünf neue Server-GameTests; 3.281 Erzblöcke in einer frischen Ocean-Stichprobe.
  **Teilnachweis Nitre:** Overworld-Erz und Abyssales Nitre, natürliches Ödland-
  Sample, native Loot-/Behutsamkeit-/Glück-/Schmelzpfade sind geprüft. Höhere
  Verarbeitung ist weiter verbunden: Lapis → Silica/Schwefelkristalle → Cluster →
  Schwefelstaub; sieben Nitre/ein Schwefel/eine Holzkohle → vier Schwarzpulver.
  Beide realen Maschinen-/Crafting-Ketten sind serverseitig geprüft.
- [x] Oblivion-Catalyst-Infusion: 5.000 PE, Eye of Ender, vier Redstone und vier
  Oblivion Shards; ausgewähltes erwachsenes Lebewesen im Vier-Block-Bereich,
  echte bestätigte Todespose, UUID/Persistenz und verlustfreie Abbrüche.
  Sechs neue Server-Tests, einschließlich verweigertem Death-Event und Unload.
- [x] Erster Gateway Key: echtes Rezept, Auswahl pro Item, geordnetes Ritual
  (1.000 PE, vier kardinale Shadow Gems), Schlüsselrückgabe, Säulen/Portalanker;
  native Hin-/Rückreise einschließlich Spieler, Inventar, PE, Forschung und
  Respawn-Punkt sowie abbrechbarem Reise-Event auf einem normalen Headless-Server.
  **Zusätzlicher Neustartnachweis:** zwei reale Server-JVMs auf isolierter Welt,
  zunächst ungeladener Rückportal-Chunk, native PlayerList lädt echte Spielerdatei
  aus Wasteland. Portal-UUIDs/exakte sichere Ankunft, Namen/Inventar/Kristalltasche,
  1234,25 Buch-PE/Forschung/XP/Respawn erhalten. Laufende Maschine beginnt nach
  nativem Chunkload exakt wieder bei Tick 73, echte Ticker vollenden Pack/Unpack.
  Kein Client-/Account-/allgemeiner Multiplayernachweis. Probe verweigert alten
  Prepare-Zustand ohne Änderung gespeicherter Prüf-/Spielerdateien.

## 2. Forschung, Bücher und Rituale

- [x] Sechsundzwanzig getrennte frühe Entdeckungen mit Spielerspeicherung; lesender
  siebenundzwanzigseitiger Feldführer mit scrollbarem Inhalt. Das vollständige Forschungssystem
  fehlt; die Scroll-Darstellung wurde ohne Client-Start nicht visuell abgenommen.
- [ ] Inhalts-/Kategoriennavigation und Freischaltungen für Biome, Kreaturen,
  Dimensionen, Buchstufen und Krankheiten. Referenz:
  `api/knowledge/ResearchItems`, `ResearchRegistry`, `init/MiscHandler`.
- [ ] Fünf Buchstufen einschließlich Abyssalnomicon, jeweilige PE-Kapazität,
  Upgrade/Persistenz ohne Energieverlust und vollständige hilfreiche Seiten.
  **Teilnachweis:** Basisbuch (5.000 PE), Ödland-Buch (10.000 PE) und Dreadlands-Buch
  (20.000 PE), echtes
  Skin-Upgrade mit erhaltener Energie/Komponenten, Rezept-Synchronisierung und
  Kapazitätserkennung aller bestehenden PE-Geräte sind technisch geprüft.
  Dreadlings → 100 Dread-Energie → Essenz → acht Fragmente/Haut → drei Häute/Buch,
  Stufe-zwei-Bruchstein-Ritualplatz und 2.000-PE-Stabupgrade samt drei gespeicherten
  Ladungen/Überlauf sind in einer normalen Dreadlands-Welt geprüft.
- [x] Erstes datengetriebenes Creation-Ritual mit Reload-Signatur, sicheren
  Abbrüchen und alten Saves. Basic Scroll und erste Coralium-Infusion sind vorhanden.
- [ ] Infusion mit zentralem Altar-Input, geordnete/ungeordnete Opfer,
  Buch-/Dimensionsstufen und höhere Ritualplatz-Materialien.
  **Teilnachweis:** ungeordnete Infusion mit echtem, gespeichertem zentralem Input,
  synchronisiertem Item-Renderer, Ergebnis auf dem Altar und sicherem Abbruch.
  Geordnete Opfer sind am Gateway technisch geprüft. Erste Ödland-Buch-/Dimensions-
  und Altarstufe inklusive Bruchstein-Formation, gemischtem Material, sicherem
  Abbruch und erhaltener Stabladung sind geprüft. Dreadlands-Stufe zwei ebenfalls;
  zwei höhere Stufen fehlen.
- [ ] Progressionsrituale: Transmutation Gem, Oblivion Catalyst, Portal,
  Asorah-Beschwörung, Dreadlands Key, Sealing Key, Cage, J'zahar-Wiederkehr.
- [ ] Depths Armor, Charms, sieben Statues, PE-Ringe, weitere Scrolls,
  Staffs/Spirit Tablet/Silver Key und übrige registrierte Gegenstandsrituale.
  **Teilnachweis Staffs:** Basis-, Ödland- und Dreadlands-Zerrüttungsstab, servereigene
  Zielprüfung/Schaden/Abklingzeit, Schatten- und Ödland-Essenz, sichere Einlösung
  und 1.000-/2.000-PE-Upgrades sind geprüft. Automatische Rending-Geräte, Sapping und
  die späteren Stäbe/Essenzkanäle fehlen weiterhin.
- [ ] Umwelt-/Beschwörungs-/Transformation-/Verzauberungsrituale und wirksame
  Scroll-/Spellbook-Pfade soweit tatsächlich im Original implementiert.
  Referenz für alle Ritualgruppen: `AbyssalCrafting.addRitualRecipes`,
  `api/ritual/Rituals`, `common/ritual`, `api/ritual`.

## 3. PE und Verarbeitung

- [x] Erste Collector/Relay/Container-Kette, Zwei-Buch-Inventar, Redstone-Pause,
  Energieerhaltung und Item-/Save-Persistenz serverseitig geprüft.
- [ ] Vollständige PE-Gerätestufen, Statues/Charms, ursprüngliche Rezepte,
  Netzwerk-/Reichweitenregeln und relevante PE-Effekte.
  - [x] Verbundene frühe PE-Rollen und vier erreichbare Gerätestufen: Basis,
    Oberwelt, Abyssales Ödland und Dreadlands; je Sammler, Relais, Speicher und
    Energiepodest, bestehende Basis-/Save-IDs erhalten. Alle 96 Basis-Rollen/
    Richtungen, sechs echte Platzierungsrichtungen, Redstone, volle erste
    Empfänger/Blockformen, exakte Reichweiten 4/6/8/10, fraktionale Einzelitems,
    große native Menüwerte und 24 tatsächliche Ambossentnahmen geprüft.
    Vier eigene Ringe, sechs 400-PE-Infusionen aus allen niedrigeren Ringen;
    vorhandene Buchstufen/Zutaten und exakte Verbrauchs-/Save-Gates verbunden.
    Sammler/Relais nun aus echtem Monolithstein. Speicher ausschließlich über
    originale 100-PE-Podestinfusion; PE-only-Komponententransfer, alte billige
    Truhenrezeptdatei deaktiviert, nicht gelöscht.
  - [x] Eigenständiges Energiepodest mit echter Einzelitem-Auflage und
    Statuen-Collector-Rolle: Originalrichtung Podest -> Item bei 20 PE/Tick,
    5.000/7.500/10.000/12.500 PE. Eigene Tilt-Modelle/Itemrenderer, native
    Auflage/Entnahme, voller Spieler hält das Item, Adventure, Properties,
    exakte separate Abbaudrops, Save/Sync/Fraktionen/Komponenten geprüft.
    Normale Welt: registrierte Ticker vom geladenen Geräte-Input über Speicher
    und Relais zum Buch auf dem Podest; total exakt 73,75 PE.
  - [x] Technischer echter PE-Stream-Pfad nach angenommenen Relais-/Statuen-
    Transfers: optional ausgehandeltes natives Payload, Beobachterradius,
    geladene Grenzen, native Codec-/Embedded-Verbindung und Ablehnungen geprüft.
    Fehlender Kanal unterbricht niemals einen Transfer. Eigener kleiner Sprite,
    begrenzte Geometrie und Partikeleinstellungen; Clientoptik weiterhin offen.
    Vollnachweis `pe-source-recipes-direction-full-second-20261007.log`:
    100 JUnit/231 Flat/46 Normalwelt/15 Python, echte JAR-Ressourcen geprüft.
    Frühere grüne Drain-Tests hatten eine falsche Quellenrichtung; ersetzt,
    nicht als Paritätsnachweis weiterverwenden.
  - [x] Technischer Sacrificial-Altar-Unterpfad mit vier erreichbaren Stufen:
    5.000/7.500/10.000/12.500 PE, 1/2/3/4 erwachsene Nicht-Untote/Nicht-Shadow-
    Ziele in 8/3/8 Erwerbsreichweite, echte angenommene Todesphase liefert maxHP
    einmal. Death-Veto/Discard liefern nichts, Loot-Veto und doMobLoot=false
    löschen keinen angenommenen Tod. Quellen-Integer-CollectionLimit bleibt
    getrennt von fraktionaler PE; Cap/5 löst 1.200/1.000/800/600 Cooldown-Ticks
    aus. Begrenzte bereits erfasste Todesfälle warten bei voll/Cooldown.
    Originalrezept, sechs tatsächliche native Ring-Ambossentnahmen und
    Name/Komponenten/PE/Cooldown-Erhalt verbunden. Native Einzelbuchauflage,
    voller Spieler/Adventure, Save, exakt zwei Abbaudrops und echte BlockItem-
    Wiederplatzierung geprüft. BE-Eingang in allen Stufen gesperrt; Basisitem
    gesperrt, höhere Items aufladbar, alle Items entladbar. Relais stoppt am
    ablehnenden ersten Altar, überspringt ihn nicht. Statue lädt ihn nicht.
    Eigene Modelle/Pixel/Renderer/reachable bilingualer Guide. Vollnachweis
    `sacrifice-full-native-first-20261008.log`: 102 JUnit/238 Flat/47 Normalwelt,
    15 Python; echte registrierte Ticker liefern 23,75 PE ins Buch. Sicht offen.
  - [ ] Omothol-Geräte/Ring erst mit echter Omothol-Buch-/Materialprogression;
    Spirit Tablet und übrige PE-Geräte weiterhin offen.
  - [x] Technischer **Stone Tablet -> State Transformer**-Unterpfad:
    echte Survival-Rezepte, 49 Cargo- + ein Tabletslot, zwei originale
    200-Tick-Pack/Unpack-Modi und PE = Count × integer(64 / MaxStackSize).
    Native Komponenten/Slotzuordnung/Namen/Haltbarkeit exakt erhalten;
    Tafel nicht als ladbaren PE-Speicher behandeln. Native Container-Komponente,
    Singleton, Nested-Inventar-/Capability-Gates, aktive Save-Snapshots,
    belegte Ausgabe und mutierte Inputs ohne Überschreiben/Verlust behandelt.
    Echte Crafting-Katalysatornutzung, serverautorisierte 86-Slot-Menüöffnung,
    Shift/Drag/Pickup-All/Modussperren, reale Trichter und native Abbaudrops
    geprüft. Registrierte Ticker packen/entpacken in Flat und Normalwelt,
    maximal 49 volle Slots/3136 PE geprüft. Eigene Pixel/3D-Items/Blockmodelle,
    bilinguale UI und erreichbarer Guide; Clientoptik offen.
    Vollnachweis und aktueller finaler Teststand ausschließlich README.
  - [x] Technischer **Stone Tablet -> Energy Depositioner**-Frühpfad:
    originales Rezept/fünf Monolithsteine/zwei Shadow Gems/Relais/eine
    Transmutation-Gem-Nutzung, 200 Ticks, 10.000-PE-Puffer. Versiegelte Cargo
    wird als Brennstoff verbraucht; nicht verfluchte/valide Singleton-Tafeln,
    interne nicht entnehmbare Jobs und voller Ausgang sind serverautoritär.
    Echte Menüöffnung/38 Slots/Datensynchronisierung, Trichter und natives
    Mining/BlockItem-Wiederplatzieren erhalten Inventar/Job/PE/Amulett ohne
    PE-Replay. Beschädigte Jobs bleiben erhalten und pausiert. Collectorrollen,
    originale Charmfaktoren, 200-Toleranz-Reset/Disruptions/Vetos und
    Callback-Reentry/Abbau-Duplizierung sind regressionsgeprüft. Vier zusätzliche
    native Darkstone-Bauformen, eigener Generator/3D-/Pixel-/GUI-Assets und Guide.
    Konversion auf geladene volle 4×4-Biomzellen innerhalb 17×17 und tatsächliche
    Welthöhe beschränkt; negative/obere Y und native Platzierungsvetos/Rollback
    geprüft. Gegenwärtig nur Oberwelt; vorhandene Blockentities werden nie
    ersetzt. Das ist ausdrücklich keine vollständige Original-Konversionsparität.
    Kein Pflichtumfang der ersten Feedback-Beta; konkrete Vollnachweise README.
  - [ ] Depositioner-Folgeschritte: sichere ursprüngliche Ritualaltar-/Pedestal-
    Variantenkonversion unter Erhalt laufender Inventare sowie weitere
    Dimensionskonversion nach dem jeweiligen Progressions-/Schutz-Audit.
    Spirit Tablet + Spirit Altar + Itemtransportsystem bleiben eigener offener
    Vollumfang; Stone Tablet ist nicht deren Itemtransport-Konfigurator.
    Keine registrierten Einzelitems als fertiges System zählen.
  **Nächster verbundener Pfad, gegen lokale Quellen geprüft:**
  - [x] Technischer Statuen-/Survival-Unterpfad: Cthulhu, Hastur, J'zahar, Azathoth,
    Nyarlathotep, Yog-Sothoth und Shub-Niggurath; je eigener platzierbarer,
    ausgerichteter/abbaubarer Statue mit eigener unterschiedlicher Ein-Block-
    Geometrie/Pixeln/Item, nativer BlockEntity und gespeicherter PE-Basisladung.
    Echte BlockItem-Platzierung, Spiegelung/Rotation, sieben reale Loots und
    nativer normaler Welt-Ticker für beide Hände geprüft; keine Client-Sichtabnahme.
    Die neuen Statuen haben nun unabhängige gehaltene/abgelegte Buch- und
    Sammlerpfade sowie vollständigen Disruptionsdispatch und Strukturboni. Dieser Gesamtpunkt
    bleibt deshalb [ ]; die sieben unterschiedlichen Modelle sind keine Behauptung
    vollständiger Mechanikparität.
  - [x] Native Shoggoth-Monolithen: 3×3 Monolithstein, 5–12 Höhe, Biomasse im
    Sockel und zufällige reale Gottheit. Bestehende Prototype-Effigy-Saves und
    historische Bauwerke erhalten; keine automatische Welt-Reparatur.
  - [ ] Verbundene Statue-PE-Regeln: 120-Tick-/5-PE-Basis, Sky-/Isolationsregeln,
    vorhandene Bücher/Collector sowie tatsächliche Dropped-Item-Pfade,
    persistente Toleranz und echte Disruptions-/Strukturregeln.
    - [x] Unabhängiger Transfer-Unterpfad: nächster echter Nicht-Spectator als
      Held-Timergate, beide Hände, 0–9 Neustartticks; abgelegte Bücher und isolierte
      Sammler je eigener 1:120/1:80-Chance ohne Spieler. Kapazitätsgrenzen, native
      Dropped-Stack-Synchronisierung/Komponenten, gespeicherte echte Toleranz ohne
      Overflow oder erfundenen Disruption-Reset. Echte 800 Normalwelt-Ticks ohne
      Spieler liefern Buch und Sammler PE, Held-Timer bleibt null.
    - [x] Vorhandene Portal-Monolithsäule korrekt wiederverwendet: kein zweiter
      Registereintrag. Vier echte Monolithsteine → eine schmale Säule mit eigener
      Geometrie/Pixeln und nativem Loot. Höchstens zwei direkt aufeinanderfolgende
      Säulen geben +4/+8 Buchreichweite und zusätzliche Sammler-Suchebenen;
      normale Steine/freischwebende zweite Säule zählen nicht. Cache auf zwanzig
      gefundene Positionen vor Voll-/Isolationsfilter begrenzt, live revalidiert,
      bei Änderung/alle 200 realen Ticks aktualisiert; keine Such-Chunkladungen.
    - [x] Drei echte Orte der Macht: Basis (15 Stein/4 Säulen/4 Statuen), Totem
      (1 Stein/3 Statuen), Torbogen (1 Stein/1 Statue/6 Mauern/4 Treppen/6 Stufen).
      Native Buchaktivierung ersetzt nur den Mittelstein, ohne Buch-/PE-Verbrauch;
      alle sieben/gemischte Gottheiten, vier Orientierungen und geladene
      Chunkgrenzen geprüft. Tatsächliche ganze Geometrie, Treppen/Stufenhälften,
      Berechtigung, NeoForge-Place-Veto/Rollback, Save/Load und Membership statt
      blindem NBT-Flag. Sofortige Bonusverluste bei Abbau, reparierbare Links;
      Quellen-Rangeboni 2/3/1 und untere Sammlerlagen, Indoor-Ladung ohne neue
      Toleranz. Alter Toleranzstand bleibt erhalten. Drei freigegebene Guide-Seiten
      nutzen dieselben echten Java-Schichtpläne, eigene Ressourcen. 900 native
      Normalwelt-Ticks unter Steindächern; Optik noch nicht abgenommen.
    - [x] Vollständiger technischer Disruptionskatalog und tatsächliche Effekte;
      Konkreter Quellkatalog geprüft (`AbyssalCrafting.addDisruptions`): 27
      tatsächlich registrierte Einträge, davon 18 allgemein und neun gebunden.
      Allgemein: Lightning, Fire, Shoggoth, Poison, Slowness, Weakness, Wither,
      Coralium, Potential Energy, Freeze, Shadow Swarm, Fire Rain, Displace,
      Random Teleport, Drain Nearby PE, Ooze, Random Swarm, Random Spawn.
      Gebunden: Shub Sheep Swarm/Animal Corruption/Offspring/Famine,
      Hastur/Nyarlathotep Invisible Swarm, J'zahar/Yog-Sothoth Sacrifice
      Corruption und Azathoth Famine. Monolith/Corruption sind auskommentiert,
      nicht als zusätzliche Originaleffekte zählen. `TileEntityStatue` prüft
      Toleranz >= 100 pro Tick, tatsächlicher Quellenreset null vor Callbacks;
      kein geratenes Intervall oder reduzierter Zufallspool. Globales natives
      Event/Disable und validierte Strukturen respektiert. Native Normalwelt-
      Ticker: Start 98, genau ein Dispatch, Hände 10/10 PE, Resttoleranz vier.
      Shadow Swarm benötigt vier echte Wesen jeder der drei Formen; Sheep Swarm
      echte Evil Sheep. Animal Corruption benötigt vier Evil-/Demon-Tierpfade,
      native Pferde/Katzen/Wölfe/Killerkaninchen; Offspring eine echte Kreatur.
      Native Schadens-/Teleport-/Konversions-/Spawn-Vetos, geladene Räume und
      vorhandene Drops müssen auch bei diesen Effekten autoritativ bleiben.
      Alle Effekte/nativen Vetos technisch geprüft: 92 JUnit/207 Flat/45 Normalwelt.
      Nicht-Spieler-Place-Snapshot meldet tatsächlich Feuer/Ooze; getrennte alte
      Snapshots ermöglichen Rollback. Bilingualer Guide, keine Originalmedien.
    - [x] Verlustfreie native Besitzrückgabe bei echter Tier-/Opferverwandlung:
      Ausrüstung, Pferdesattel/-rüstung, Eselinventar/Kiste, Pig-/Strider-Sattel
      und Villager-Inventar; exakte Stapel/Komponenten/PE und ItemEntity-Save.
      Ersatz und alle Items vor Besitzerentfernung zulassen. Veto/Reentry/
      Quellinventaränderung/späte Itemänderung rollen alle erzeugten Ausgaben
      zurück; keine Success-Notification bei Abbruch, Listeneränderungen erhalten.
      Vier neue tatsächliche native Tests, finale 211 Flat-/45 Normalwelt-Tests.
    - [ ] Rider-/Passenger-/Leash-Verknüpfungen vollständig behandeln; aktuell
      solche Wesen sicher behalten. Keine Fremdmod-Attachment-Migration behaupten.
      Frühe vier Gerätetiers/PE-Partikel/Rollen nun oben technisch verbunden;
      spätere Geräte und übrige Konfiguration weiterhin offen. Keine vollständige
      PE-Parität durch diese Teilnachweise behaupten.
  - [x] Generischer Survival-Charm-Unterpfad: acht Gold + Diamant, reale native
    Amboss-Forgings mit Pfeil/Schwarzpulver/Redstone und exakt einer Stufe/einer
    Zutat erst bei Entnahme; mehr als ein linker Charm wird ohne Verbrauch
    abgewiesen. Aktivierung aller sieben Statuen, kein Überschreiben/Mehrverbrauch,
    wiederverwendbarer Leer-Reset, Save/Load, eigene Pixel, Tooltips/Guide und
    Adventure-Berechtigung geprüft. Basis-PE-Hooks: aktiv zehn PE, Kraft 15,
    Dauer 60 Ticks, Reichweite elf / Sammlerradius fünf. Alte Prototype-Regeln
    erhalten; die komplette PE-Schicht ist noch offen. Dach prüft jetzt
    Himmelslicht an der Statue selbst, nicht am exponierten Dachblock.
  - [ ] Vollständige Gottheiten-Charms/Forging und Strukturintegration:
    passende/fremde Gottheit, spätere Survival-Infusion und kompletter PE-Fluss.
    Quellfaktoren: DURATION 4/2, POWER 2,5/1,5, RANGE 6/4.
    Basis-Charm: acht Goldbarren um einen Diamanten (`recipes/charm_0.json`).
    Verstärker-Forgings kosten laut `AnvilForging` standardmäßig eine Stufe.
    Leerer Charm deaktiviert einen aktiven Manipulator ohne Verbrauch; ein
    Verstärker aktiviert ausschließlich inaktive Manipulatoren und wird verbraucht.
    Benannte Gottheiten-Charms: acht passende Farbstoffe, zentraler Basis-Charm,
    2.000 PE und Buchstufe vier (`AbyssalCrafting` 485–499); aktuell sind nur drei
    frühere Bücher erreichbar. Nicht durch ein erfundenes Frührezept umgehen.
    Die drei ursprünglichen Strukturboni sind nun verbunden. Sämtliche
    allgemeinen/gottheitsspezifischen Disruptions weiter vollständig gegen deren
    konkrete Quellen prüfen.
  Referenz: `BlockStatue`, `TileEntityStatue`, `EnergyEnum`, `PEUtils`,
  `WorldGenShoggothMonolith`, `AbyssalCrafting.addForgingsFor`.
- [ ] Transmutator, Crystallizer, Materializer, Flüssigkeiten/Buckets,
  Crystal-Fraktionen, Forging/Rending und dazugehörige Menüs/Rezepte.
  Referenz: `init/BlockHandler`, `init/MiscHandler`, `AbyssalCrafting`.
  **Teilnachweis Transmutator:** echtes Plateau liefert Coralium-Stein, Abbau/
  zweimaliges Schmelzen die Ziegel; natives Maschinenrezept mit Fluid- und
  Katalysator-Rest. Sieben erreichbare Rezepte, 200 Ticks, eigene Brennstoffe,
  drei synchronisierte Slots/Shift-Klick, echter Öffnungs-Rechtsklick, sided Capabilities/Trichter, gespeicherte
  Timer/Komponenten/XP und einmaliger Abbau-Drop sind geprüft. Das Plateau-Sample
  enthält 1.151 Oberflächensteine. Dreadguard-Splitter → Barren ist das achte Rezept;
  fünf echte Welt-Drops → fünf Barren/1 XP sind geprüft.
  **Teilnachweis Crystallizer:** echte Dreadiumblock-/Ziegel-Zutaten, 56 geladene
  Rezepte/200 Ticks/zwei atomare Ausgaben, reine Wasserflaschen und Container-Reste,
  Welt-Save/Rezept-Reload/XP/40 Menüslots/Shift-Klick/Trichter sind geprüft.
  22 erreichbare Familien mit Fragment/Splitter/Kristall/Cluster und 132 verlustfreien
  Fraktionsrezepten; echte normale Dreadlands-Ticks liefern beide Wasser-Ausgaben.
  Carbon-Ring → Cluster → Obsidian-Kern/dichter Cluster → Diamant ist mit echtem
  Verbrauch geprüft. Transmutator jetzt 22 geladene Quell-Rückgewinnungsrezepte.
  Kristalltaschen mit 18/36/54 nativen Slots, Shift-Klick, Hand-/Zahlentastensperre,
  gespeicherten Einzelinhalten und verlustfreien komponentenerhaltenden Upgrades
  sind geprüft; eine gefüllte Tasche übersteht den nativen Dimensionswechsel.
  72 Omothol-Slots bleiben bis zu echten späteren Zutaten offen.
  Fehlende Fremdmetalle/späteres Fleisch/Methan/Materializer bleiben offen.
- [ ] Höhere Materialketten Coralium → Dreadium → Ethaxium mit Tools/Armor,
  Reparaturen, speziellen Effekten und allen Progressionsverbindungen.
  **Teilnachweis Refined Coralium:** Liquified-Erz benötigt Abyssalnit; Schmelzen
  liefert Barren, daraus alle fünf Tools/vier Rüstungsteile und umkehrbare
  Block/Nugget-Rezepte. Refined-Tools erschließen erst Pearlescent-Erz. Echte
  Ambossreparatur, endliche Helm/Brust/Boot-Effekte, Loot/Glück/Behutsamkeit,
  Forschungsentdeckung und eigene Item/Block/Armor-Assets sind serverseitig geprüft.
  **Teilnachweis Dreadium:** native Dreadguard-Tode liefern echte Splitter;
  Transmutator → Barren → Nugget/Block sowie alle fünf craftbaren/ambossreparierbaren
  Tools mit Stufe sechs/2.300 Haltbarkeit/14 Geschwindigkeit sind geprüft.
  Dreadstone-Ziegel erschließen Maschinenmaterial. Vier echte Dreadium-Rüstungsteile,
  Quellwerte/Rezepte/Ambossreparatur, finite Brust-/Stiefel-Effekte und eigene UVs
  sind geprüft. Native Verzauberungstags und echter Survival-Amboss-Buchpreview
  für alle 27 bestehenden Ausrüstungsteile/Werkzeuge sind nach einem roten
  Regressionstest korrigiert und geprüft. Samurai/Depths/weitere Sets bleiben offen.

## 4. Dimensionen und Boss-Fortschritt

- [ ] Abyssal Wasteland: eigenes Terrain/Biome/Erze/Flüssigkeiten, sichere
  Hin-/Rückreise, Rückkehrpunkte, Stronghold/Loot und Überleben.
  **Teilnachweis:** echte native Noise-Dimension, drei Biome, eigene Stein-/Sand-
  Blöcke, Coralium-Flüssigkeit/Bucket und nicht mit Milch heilbarer Kontakt-Schaden;
  native Portalprozessoren und gespeicherte gegenseitige Anker sind serverseitig
  geprüft. Sieben natürlich nachgewiesene Erze und erste vollständige Refining-
  Ausrüstungskette sind vorhanden. 5×5-Ankunft mit vollständiger Standflächen-
  Prüfung ist mit einem breiteren Wesen getestet. Stronghold: native konzentrische
  Platzierung, sieben eigene Jigsaw-NBTs, 33 Teile/327 trockene Laufzellen,
  Loot/Powerstone, echtes Explorationsmarker-Save und Tracker-Wechsel sind geprüft.
  Vollständige Biome/Ökologie/
  Strukturen bleiben offen. Erster vollständiger Prozess-Neustart mit kaltem
  Rückportal ist jetzt technisch geprüft; weitere Tod-/Respawn-/Restart-
  Kombinationen und tatsächliche Sicht-/Spielabnahme bleiben offen.
- [ ] Asorah/Spectral Dragon: Beschwörung, Flug/Angriffe, Bossbar, eigener
  Render/Sound, Drops und Verbindung zum nächsten Gateway.
  **Teilnachweis:** echte ungeordnete Sieben-Opfer-Beschwörung (1.000 PE), native
  Spawn-/Buch-/Platz-Vetos ohne Verbrauch, Multipart-Hits, 400/30 HP, Bossbar,
  Flug/Anflug, Kopfkontakt/Flügel-Rückstoß und bezahlte Heilerenergie. 200-Tick-Tod,
  gespeicherter einmaliger Augen-Drop/5.000 XP, doMobLoot, Loot/Forschung und echte
  Ödland-Server-Ticks sind geprüft. Eigenes Modell/UV-Atlanten/Icons, noch Vanilla-
  Sounds. Der echte 10.000-PE-Folgegateway ist geprüft. Sichtprüfung und eigene
  Sounds fehlen weiterhin.
- [ ] Dreadlands: mehrere Biome, Dread-Plague, Mine/Powerstone/Chagaroth-Lair,
  erreichbare Ausrüstung und sichere Reisen.
  **Teilnachweis:** echte Noise-Dimension mit einem Biom, sechs Stufe-vier-
  Steinen/Erzen, drei natürliche Vorkommen und sichere native Hin-/Rückreise.
  Dreadling, Pest-Schaden/Erschöpfung/Milch-Immunität, Essenz/Buch/Ritualstufe zwei
  und der erst später zugängliche 5.000-PE-Powerstone-Ersatz sind geprüft.
  Dreadguard: 60 HP/20 Rüstung/4 Zähigkeit/10 Nahkampfschaden, gespeicherter
  60-Tick-Sprühangriff/200-Tick-Cooldown, stationäre Phase, echter Server-Ray/
  Wand-/Schadens-Veto und native Magic-/Pest-/Carrier-Regeln sind geprüft.
  Eigenes Gelenkmodell/UVs, noch Vanilla-Sounds; saisonale/hardcore Varianten offen.
  Dread-Brut/Große Brut/Niedere Bestie: native Quellwerte, Klettern/Immunitäten,
  echte Fernkampf-Projektile/Nahkampfwechsel, exakte 5→1-Fusion ohne Verlust bei
  Spawn-Veto, gespeicherte begrenzte Nachkommen-Timer und erst nach bestätigtem
  Tod zwei Nachkommen sind geprüft. Treffer-/Impact-/Death-Vetos, Wandkollision,
  gespeicherte Geschosslebensdauer, echte Shard-Loot→Dreadium- und Rending-Wege
  sind geprüft. Eigene achtbeinige Gelenkmodelle/UVs, noch Minecraft-Sounds.
  Weitere Biome, Ökologie, volle Pest-Verwandlungen, Mine/Lair fehlen weiterhin.
- [ ] Chagaroth: zusammengehörige Spawn-/Fist-/Bossmechanik, Phasen,
  Bossbar/Angriffe/Drops und Übergang nach Omothol.
- [ ] Omothol: schwebendes Terrain, Bauten, Ghouls/Remnants/Minions, Loot,
  J'zahar-Kampf und Fortsetzung nach Dark Realm.
- [ ] Dark Realm, Sacthoth und Abschlussprogression einschließlich tatsächlich
  implementierter Schlüssel/Cage/Staff-Funktionen.
  Referenz: `WorldHandler`, `common/world`, `common/structures`,
  `ItemGatewayKey`, `EntityDragonBoss`, `EntityChagaroth`, `EntityJzahar`,
  `EntitySacthoth`. Abyssal Wasteland und Dreadlands sind teilweise implementiert;
  Omothol und Dark Realm fehlen.

## 5. Kreaturen, Welt und Assets

- [ ] Lesser Shoggoth vervollständigen: Lairs, Varianten, Gruppenverhalten,
  sichtbare Bewegung, Schleim und eigene fertige Darstellung.
  Der bestehende Monolith-/Schleim-Serverpfad ist bereits geprüft.
  **Teilnachweis Lairs:** drei eigene native Sumpf-/Flusshöhlen, tatsächliche
  Biomasse-Timer/Spielertrigger/Sechsercap/Nachbarhops/Fünfererschöpfung,
  Monolithstein-Abbau, native Death-Drops → acht Phosphor. Vier trockene und
  kollisionsfreie Shoggoths nach 440 echten Naturwelt-Ticks. Eigener wachsender
  Schleim erzeugte Einbettung; native Anhebung samt Deckenschutz regressionsgeprüft.
  **Teilnachweis Ökologie/Kampf:** drei Formen × drei erreichbare Habitate,
  echte Nahrungstode/Acht-Punkte-Wachstum/Nachkommen/gespeicherte Reste,
  Aufnahme-/Wachstumsvetos, eigene UV-Atlanten und zwei Dimensions-Lairs.
  Native Multipart-Hits/Tracking, absolute Säure, echtes Schildblocken,
  Ausrüstungsverschleiß, native Flug-/Impact-Vetos, Terrain-Schutz und
  kollisionsfreie gespeicherte Nachkommen-Freigabe sind technisch geprüft.
  Neue KI-Ziele wurden durch `tickCount % 20` für ungerade Entity-IDs dauerhaft
  ausgehungert: deadlinebasiert behoben, beide Phasen laufen nun echte 700 Ticks.
  Naturweltproben benutzen frische Knoten und bewahren erschöpfte alte Höhlen.
  Sieben reale Statues/Monolithen, spätere Habitate/Quellkonfiguration, fertige
  Audio-/Darstellungsabnahme bleiben offen; Gesamtpunkt deshalb weiterhin [ ].
- [ ] Shadow Creature/Monster/Beast sowie Ghouls/Abyssal Zombie und ihre
  Spawn-/Loot-/Kampf-/Immunitätsunterschiede.
  - [x] Verbundener Shadow-Familienkern: bestehende Creature-ID erhalten,
    echte Monster/Bestie mit Quellenattributen/Größen, Gift-/Feuer-/Wasser-
    und Familienimmunität, fünf Biom-Spawnlisten ohne doppelte Fallbacks,
    tatsächliche 0–2-Splitter-/Gem-Drops und Shadow-Rending. Eigene artikulierte
    Modelle/UVs, echte Kopf-Item-Layer/Halloween und bilingualer Guide.
    Beast-Atem 300/100 native Entity-Ticks, Blockclip/Loaded-Guard, absolute
    positive Treffer/akzeptierte Stati, gespeicherte/synchronisierte Phase.
    Native Death-/Conversion-/Finalize-/Join-Vetos und echte Namens-/Größen-
    Verwandlung; Spieler nicht entfernt, native Todesregeln erhalten. Zwei
    tatsächliche Normalwelt-ID-Phasen bei 338 eigenen Ticks mit echtem Schaden.
  - [ ] Original-Hardcore-/Quellkonfiguration, zukünftige Dark-Realm-/konvertierte
    Dimensionsökologie, fertiges eigenerzeugtes Audio und Sicht-/Balanceabnahme;
    nicht durch den geprüften frühen Familienkern als fertig behandeln.
  **Teilnachweis Abyssal Zombie:**
  eigener UV-Skin auf nativer Humanoid-Geometrie, Kindzustand/Größe/Tempo/Save,
  Attribute, Spawn-Einträge, Nahkampf-Pest, Immunitäten, tatsächliches Loot,
  Fleisch-Verzehr und Zwei-Barren-Chunk-Verarbeitung sind geprüft. Erfolgreiche
  Mob-Verwandlung und verweigerter Ersatzspawn sind getestet; Spieler-Tod bleibt
  unverändert. Vanilla-Sounds und Animation sind keine optische Abnahme; spezielle
  Original-Konfiguration/Halloween und weitere Kreaturen fehlen weiterhin.
- [ ] Dreadling/Dread Spawn/Dreadbeasts/Dreadguard, Gatekeeper Minions,
  Remnants, Omothol Ghoul und übrige registrierte Kreaturen/Projektile.
  **Teilnachweis Dreadling:** native Spawn-/Kampf-/Lootregeln, eigenes artikuliertes
  Modell mit geprüften UVs, Infektion und Dread-Rending; weitere Kreaturen offen.
- [ ] Anti-Mobs, Evil-/Demon-Animals, Coralium Squid und jeweilige Effekte,
  Drops und natürliche Erreichbarkeit. Referenz: `init/EntityHandler`.
  - [x] Technischer Evil-/Demon-Tierkern und Shub-Nachkommen: neun echte Wesen,
    native Common-Konfiguration, vier reale Biommodifier, Mond-/Höhen-/Lichtregeln,
    tatsächliche Loots und echte KI. Überlebensspieler-Scherung mit genau einem
    Ersatz/Drop/Werkzeugverschleiß; Death-/Conversion-/Finalize-/Join-Vetos,
    geladener Kollisionsraum und Callback-Reentry-Schutz. Kein Demon bei Shoggoth-
    Nahrungstod und keine doppelte Shadow-/Demonphase. Evil-Sheep-Spieleridentität,
    mitgelieferte Skin-Eigenschaften/Save und Fleisch-Scherung; Demonfeuer mit
    Grief-/Place-Veto/Rollback und eigener reduzierter Mimic-Fire-Regel geprüft.
    Eigene Modelle/UVs/Pixel/Guide, keine Originalmedien. Normalwelt weist native
    Spawngewichte und echte Bewegung/Nahkampftreffer nach; Volltest 90 JUnit/
    194 Flat-/44 Normalwelt-Tests grün. Vollständige Anti-Mobs/Coralium Squid,
    globale Hardcore-Rüstungsdurchdringung, Audio und Sicht-/Balanceabnahme fehlen;
    der Gesamtpunkt bleibt offen.
- [ ] Gebäude-/Baublockfamilien aus `init/BlockHandler`, eigene konsistente
  Modelle/Texturen, lesbare Items/GUI und sinnvolle lizenzierte/eigene Sounds.
- [ ] Platzhalter gezielt ersetzen; referenzierte Originalmedien dürfen nicht
  ins JAR gelangen. Verpackte Modelle/Texturen/Loot/Rezepte/Sprachen prüfen.

## 6. Technischer Abschluss und spätere Spielprüfung

- [ ] Zusammenhängende Progression von einer frischen Survival-Welt bis zum
  Endpfad ohne Befehle, Kreativgegenstände oder unerreichbare Zutaten.
- [ ] Multiplayer-Autorität, Reload/Restart, Tod/Respawn, Dimensionstransfer,
  Energie-/Inventarerhaltung und keine doppelten Ritual-/Bossausgaben.
- [ ] Build, JUnit, GameTests und passende normale Headless-Welten grün;
  tatsächlich gebaute JAR samt Abhängigkeiten und Asset-Herkunft geprüft.
- [x] Gebündelte erste Feedback-Beta-Testfolge und ehrliche Umfangsgrenzen
  vorbereitet (`BETA-PLAYTEST.md`, `BETA-GATE.md`, `BETA-RELEASE.md`).
  Das ist keine In-Game-Abnahme und keine Veröffentlichung.
- [ ] Gesamte manuelle Vollumfang-Testfolge nach dessen Entwicklungsabschluss.
  Noahs abschließender Volltest bleibt ausdrücklich unbestätigt.

Eine Migration alter 1.12.2-Saves und Integrationen in fremde Mods waren nicht
zugesagt. Sie ersetzen keinen der oben genannten Kernpfade und werden nicht
stillschweigend als implementiert ausgegeben.

## Letzter technischer Checkpoint

08.10.2026: Build und 104 JUnit-Tests grün, 248 Flat-Server-GameTests und
48 normale Welt-Integrationstests bestanden; 15 Python-Audit-Tests.
JAR-Audit: 499 Modelle, 137 Blockstates, keine fehlenden lokalen Referenzen.
JAR-Identität und genaue Grenzen siehe aktueller `README.md`-Abschnitt.
Zusätzlich reale Zweiprozess-Prüfung (nicht in die GameTest-Zahl eingerechnet):
Cold-Return-Chunk nach Neustart ungeladen, native Spielerdatei/Portalpaar und
73-Tick-Transaktion erhalten; normale Ticker packen/entpacken. Erwartete negative
Reuse-/Welt-/Phasengates geprüft. Full-Log `restart-gate-full-native-20261008.log`
grün in 6m49; 330 gepackte Projektklassen bytegleich. Generalisierte Multiplayer-
oder Client-/allgemeine Restart-Abnahme wird dadurch nicht behauptet.
Aktueller Steintafel-/Transformer-Pfad einschließlich echter Menüöffnung,
Trichtern und Normalwelt-Ticker technisch geprüft; Lizenzen und matching
Binär-/Quellpakete lokal vorhanden. Erste Feedback-Beta ist jetzt unmittelbare
Priorität; gebündelter isolierter Spieltest erst nach Freigabe. Omothol,
Endgame/volle Forschung/volle Konversionsparität/Spirit-Transport bleiben spätere Roadmap.

### Historische weitere Notizen — Stand 07.10.2026

Stronghold/Powerstone, Dreadlands-Schlüssel/Reise und Essenz/Buch/Ritualstufe zwei
sind technisch geprüft. Rending-Drop-Duplikation reproduziert und behoben, native
Drop-Vetos weiter geprüft. Fehler-Exit-Probe nach vollständigem Speichern besteht.
Sieben unterschiedliche Statuen sind nun über neue quellförmige Monolithen
erreichbar; echte Platzierung/Loot/Save und Basisladung durch native Welt-Ticks
sind geprüft. Ältere Bauten bleiben erhalten. Kletterkäfig-Fixture und vorzeitig
gezählte Entity-Ticks korrigiert; keine Flake-Freiheit sämtlicher Tests behauptet.
Generische Charms über native Crafting-/Amboss-Entnahme erreichbar; exakt eine
Stufe/eine Zutat, gespeicherte Komponenten/Namen und kein Mehrverbrauch bei
aktiven Statuen. Echte normale Welt-Ticks laden beide Hände mit Range/Kraft/Dauer.
Reales Dach-Sky-Query korrigiert, Lichtausbreitung/Adventure/Manipulator-Nachbarn
regressionsgeprüft. Saved-Matching-/Foreign-Faktoren vorbereitet und geprüft;
keine tatsächliche Gottheiten-Infusion vorgezogen oder als fertig ausgegeben.
Unabhängiger Collector-/Dropped-Book-Takt und reale, bereits vorhandene
Monolithsäulen sind jetzt verbunden. Akzeptierte Übertragungen speichern Toleranz;
volle Ziele erzeugen keine falschen Kosten. Basis/Totem/Torbogen aktivieren native
Strukturboni nur bei tatsächlicher vollständiger Geometrie. Schutz-Vetos, Abbau,
Reparatur und vorhandene Toleranz sind regressionsgeprüft; drei native Dächer laden
ohne neue Kosten. Der komplette 27-Effekt-Disruptionsdispatch ist technisch
verbunden. Verlustfreie Mount-/Cargo-Verwandlung, PE-Partikel, höhere Geräte und
höhere Progression bleiben offen. Echte Shadow Monster/Beast jetzt
mit unabhängigen Modellen, originalen Drops/Spawnlisten und Rending verbunden.
Native Todes-/Verwandlungsvetos und realer Spieler geprüft; Beast-Atem erreicht
in beiden nativen ID-Phasen 338 eigene Ticks/62 Rest-Atemticks/194 Ziel-HP.
Test zählt nun gemessene Entity-Ticks statt vermeintlich sofortiger Chunk-
Aktivierung, keine Produktmechanik dafür abgeschaltet. Konkreter nächster
Voraussetzungspfad jetzt verbunden: vier Evil-/Demon-Tierarten und echte Shub-
Nachkommen, native Scher-/Todes-/Spawn-/Konversions-/Schutzvetos und reale KI.
Besitz/Inventare bei echten Verwandlungen nun verlustfrei nativ zurückgegeben,
einschließlich Schutz-Vetos/Callbackänderungen/Save; Rider-/Leash-Verknüpfungen offen.
Nächster Pfad: vollständige PE-Geräte-/Itemrollen und Richtungsnetze, danach höhere
PE-Geräte/Ströme. Globale Hardcore-Regeln
und übrige Kreaturen-Konfiguration weiterhin offen.
Named-Charms erst mit echter Buchstufe vier. Kein vollständiger PE-Port behauptet.
Keine Zwischen-Spielabnahme erforderlich. Auf ausdrückliche aktuelle Freigabe
ein sichtbarer, eingefrorener vorheriger Clientstand für Noah gestartet, normale
Spielsession/Speichern/Shutdown im Log. Neue Strukturen nicht in diesem Client;
keine Sichtabnahme behauptet, keine Modrinth-Instanz/Installation verändert.
