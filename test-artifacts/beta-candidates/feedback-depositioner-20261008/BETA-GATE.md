# Erste Feedback-Beta — Prüfgrenze

Stand: 08.10.2026. Noah priorisiert jetzt eine erste spielbare Feedback-Beta;
vollständige Original-Parität ist dafür ausdrücklich nicht erforderlich.
Der volle Umfang bleibt in `DEVELOPMENT-CHECKLIST.md` erhalten.

## Pflichtumfang der ersten Beta

Eine frische Survival-Welt verbindet Darklands/Frühmaterialien, Necronomicon,
PE-Quelle, 100-PE-Basic-Scroll-Ritual und sichere Speicherung. Frühe Metallurgie,
Infusionen und erreichbare Maschinen bilden den anschließenden Erkundungspfad.
Crystallizer, Transmutator, Crystal Bags, PE-Netz/Opferaltäre und die neue
49-Slot-Steintafel können getestet werden; ihre Präsenz ist keine Behauptung,
dass jedes Originalsystem bereits vollständig ist.

Abyssal Wasteland, Asorah und Dreadlands sind bereits implementierte,
serverseitig geprüfte, aber noch nicht spielerisch abgenommene Zusatzpfade.
Sie werden als experimentell beschrieben. Omothol/Dark Realm, spätere Bosse,
höhere Bücher, vollständige Forschung und Spirit-Tablet-Transport sind keine
Voraussetzung der ersten Beta und bleiben offene Roadmap. Der neue optionale
Energy-Depositioner-Frühpfad ist technisch vorhanden, bleibt aber hinsichtlich
Spiel-/Sichtabnahme experimentell: versiegelte Cargo wird endgültig verbraucht,
Biomkonversion ist auf geladene Oberweltzellen begrenzt. Vorhandene Maschinen
und Ritualinventare werden nicht in andere Varianten umgeschrieben.

## Aktueller lokaler Kandidat

Die maßgebliche JAR-Identität und die konkreten Logs stehen ausschließlich
im aktuellen Abschnitt von `README.md`. Die Arbeitsversion bleibt
`0.1.0-dev`; sie wurde weder installiert noch veröffentlicht.
`BETA-RELEASE.md` enthält Umfang, englische Entwürfe und Veröffentlichungsgrenzen.

- Minecraft 1.21.1 / Java 21; getestet mit NeoForge 21.1.234 und
  TerraBlender 4.1.0.8. TerraBlender bleibt eine separate Pflichtabhängigkeit.
- Genaue aktuelle Testzahlen, Artefaktidentität und vollständige Laufnachweise
  stehen ausschließlich im aktuellen README-Abschnitt. Der eingefrorene ältere
  Restart-Kandidat wird durch weitere Builds nicht ersetzt.
- Das neue Maschinenpaket ist über echte native Menüs, Crafting, Saves,
  Server-Ticker, Trichter und ein tatsächliches serverseitiges Öffnen geprüft.
  Zwei vollständige 200-Tick-Pack/Unpack-Pfade in Flat- und Normalwelt erhalten
  die 49 Slotpositionen und Item-Komponenten. Das ist kein Client-Rendering-Test.
- Frühe Weltgenerierung, Material-/Ritualpfade, PE-Geräte, Dimensionsreisen
  und vorhandene Kreaturen haben lokale Server-Nachweise. Diese Tests beweisen
  weder angenehme Suchdauer, natürliche Spielbalance noch flotte Darstellung.
- Zusätzlich zwei tatsächlich getrennte Server-JVMs: gespeicherter Rückportal-
  Chunk beim Neustart zunächst ungeladen, native PlayerList-Datei aus Wasteland
  geladen, identische Portal-UUIDs/exakte sichere Rückreise, Buch-PE 1234,25,
  Inventar/Kristalltasche/Forschung/XP/Respawn erhalten. Native Maschine setzt
  gespeicherte Verarbeitung bei Tick 73 fort und packt/entpackt alle Cargo-Slots
  ohne manuelle Maschinenticks. Dies ist ein isolierter Server-/Embedded-
  Nachweis, kein echter Account-/Client-/Multiplayer-Test.
- Historische Headless-Samples mit Seeds `6910881343714234347` und `123456789`
  belegen Darklands und freie Monolithen in begrenzten Ausschnitten. Sie sind
  keine allgemeine Häufigkeitsstatistik und wurden hier nicht erneut erzeugt.

## Vor der Freigabe der Feedback-Beta

Die gebündelte kurze Folge steht in `BETA-PLAYTEST.md`. Keine Zwischenabnahmen
weiterer Roadmap-Systeme sind nötig; währenddessen kann weiter entwickelt werden.

1. Nach ausdrücklicher Freigabe den Kandidaten in einer isolierten Umgebung
   starten, eine neue normale Welt anlegen und Buch/GUI/Modelle auf grobe
   Darstellungsfehler prüfen. Kein wichtiger Spielstand und keine 1.12.2-Migration.
2. Buch → tatsächlich auffindbare PE-Quelle → 100 PE → neun Bruchsteine →
   vier Bücher → genau eine Basic Scroll ohne Befehle/Kreativmodus abschließen.
   Suchdauer und unklare Anleitung sind gewünschtes erstes Feedback.
3. Speichern, Client regulär beenden, denselben Kandidaten erneut starten:
   Buch-PE, Entdeckungen und ein gefülltes Maschineninventar müssen erhalten sein.
   Der serverseitige Zweiprozess-Neustart mit ungeladenem Rückportal ist jetzt
   geprüft; Client-Neustart und echter Spielablauf bleiben diese manuelle Grenze.
4. Steintafel mit benanntem/beschädigtem Item packen und in leere Ausgabeslots
   entpacken: 10 Sekunden je Vorgang bei 20 TPS, kein Überschreiben/Verlust.
   UI-Lesbarkeit und Handmodell bleiben echte Sichtprüfungen.
5. Lokales Binär-/Quellpaket, Lizenzen, eindeutige inoffizielle Kennzeichnung,
   Pflichtabhängigkeit und finale Beta-Metadaten passend veröffentlichungsfertig
   machen; erst nach ausdrücklichem Auftrag tatsächlich hochladen.

Core-Crash, Datenverlust/Duplikation oder unerreichbare Pflichtprogression
blockieren eine Freigabe. Kleinere Art-/Balance-Mängel und fehlender späterer
Umfang können offen als Beta-Grenzen stehen bleiben. Die automatischen Tests
sind grün; Client-/Survival-Abnahme und Veröffentlichung sind nicht erfolgt.
