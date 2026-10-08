# Erste Feedback-Beta: gebündelter Spieltest

Stand: 08.10.2026. Dies ist eine vorbereitete Testfolge, kein Auftrag zum
Client-Start, Installieren oder Veröffentlichen. Kandidat/Identität siehe
aktuelles `README.md`; nicht versehentlich ein älteres JAR testen.

Für den Test nach Freigabe eine isolierte Instanz mit Minecraft 1.21.1,
NeoForge 21.1.234, TerraBlender 4.1.0.8 und genau einem Kandidaten verwenden.
Keine wichtige Welt und kein 1.12.2-Save. Der vorhandene
`Start-PE-Playtest.cmd` startet stattdessen den aktuellen Dev-Quellstand;
das beweist nicht, dass die verpackte JAR im normalen Loader funktioniert.
Keine zwei Clients auf demselben `runs/client/`-Verzeichnis starten.

## Pflichtpfad: frische Survival-Welt

1. Eine **neue normale Survival-Welt** anlegen; Name und Seed notieren.
   Start, erste Chunk-Ladung und Inventar dürfen nicht abstürzen. Auf fehlende
   Modelle/Texturen und Fehlermeldungen achten.
2. Necronomicon aus **6 Rotten Flesh, 2 Iron Ingots, 1 Book** herstellen.
   Öffnen, mehrere Seiten lesen und scrollen: Schrift/Diagramm/Buttons
   dürfen nicht verschwimmen oder abgeschnitten sein. Handmodell zum Spieler
   beurteilen. Den Kernpfad ohne Cheats/Kreativmodus spielen.
3. In Darklands oder einem passenden frühen Monolith-Gebiet eine zugängliche
   PE-Quelle finden. Das Buch nahe einer freien Monolith-Spitze in der Hand
   halten und auf steigende PE achten. Die konkrete Statue/Struktur kann
   Reichweite und Ladung beeinflussen; keine pauschale Such-/Zeitgarantie.
   Suchdauer, Distanz und eventuell unverständliche Buchhinweise notieren.
4. Bei mindestens **100 PE** neun gewöhnliche Cobblestone-Blöcke nach dem
   Diagramm auf Buchseite 3 setzen, vollständig in einem Chunk und mit freiem
   Platz neben den äußeren Blöcken. Buch auf den mittleren Block anwenden:
   Altar + acht Podeste. Vier gewöhnliche Bücher auf vier Podeste legen.
5. Mit dem geladenen Necronomicon auf den Altar rechtsklicken, nahe bleiben:
   genau eine Basic Scroll, genau vier verbrauchte Bücher und 100 PE Kosten.
   Ein Abbruch darf keine Zutaten oder Energie vorzeitig vernichten.
6. Buch-PE/Entdeckungen und einen gefüllten verfügbaren Gerätespeicher notieren.
   Speichern, Client regulär beenden, neu starten und dieselbe Welt öffnen:
   Werte/Items müssen erhalten sein. Erneut Buch und Maschine öffnen.

## Kurzer Zusatz: neues Maschinenpaket

Dieser Zusatz kann in derselben Fortschrittswelt stattfinden. Falls die
Zutaten noch nicht erreicht wurden, für die reine UI-Prüfung eine **separate**
wegwerfbare Kreativwelt verwenden und das klar als Kreativ-/Sichttest melden;
das ersetzt keinen Nachweis der Survival-Rezepte.

7. Steintafel craften: vier Shadow Shards, vier Monolith Stones, ein Shadow Gem.
   State Transformer craften: sieben Monolith Stones, eine Holztruhe und
   eine Nutzung des Transmutation Gem. Im Buch steht der Guide auf der
   Infusionsseite; Rezeptanordnung entspricht den geladenen Crafting-Rezepten.
8. Maschine öffnen, leere Tafel in den separaten Slot und gewöhnliche Items
   in verschiedene Slots des 7×7-Feldes legen, darunter ein benanntes Item und
   ein beschädigtes Werkzeug. **Packen** abwarten (200 Ticks ≈ 10 s bei 20 TPS).
   Währenddessen darf das Maschineninventar nicht verändert werden.
9. Gefüllte Tafel herausnehmen/erneut einlegen, leeres Cargo-Feld sicherstellen,
   **Lösen/Unpack** wählen. Nach weiteren 200 Ticks müssen Items in ihren
   ursprünglichen Slots mit Namen/Haltbarkeit erscheinen und die Tafel leer sein.
   Gefüllte Taschen/Shulkerboxen und weitere Tafeln sollen als Cargo abgewiesen
   werden. Volle Ausgabe darf nichts überschreiben.
10. Geladene oder gefüllte Maschine speichern/neustarten, danach abbauen:
    Maschine und tatsächliche Inhalte dürfen nicht verloren gehen/dupliziert
    werden. 7×7-Slots, Fortschritt und beide Modusbeschriftungen auf Lesbarkeit
    sowie die Tafel in Inventar/Hand auf grobe Darstellungsfehler prüfen.

## Optionales Feedback, kein Vollumfangstest

Vorhandene Abyssal-Wasteland-/Asorah-/Dreadlands-Pfade sind experimentell.
Wer sie erreicht, meldet besonders sichere Hin-/Rückreise, gute Wegführung,
Kampfdarstellung und Sackgassen. Omothol/späteres Endgame wird nicht erwartet.
Nicht die ganze Roadmap abnehmen und nicht künstlich alle Materialien ercheaten.

**Fehler melden:** Schritt, Kandidatenversion, Welt/Seed, Koordinaten,
erwartetes/tatsächliches Ergebnis, Screenshot und das passende `logs/latest.log`.
Crash, verlorene/duplizierte Items oder nicht abschließbarer Pflichtpfad sind
Blocker. Bis zu diesem Test lautet der Stand **lokal technisch geprüft,
noch nicht im Spiel abgenommen**. Upload/Freigabe bleiben getrennt.
