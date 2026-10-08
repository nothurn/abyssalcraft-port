# First feedback beta — release preparation

Stand: 2026-10-08. This is a local preparation sheet, **not a release approval**.
The current tested artifact remains `0.1.0-dev`; `0.1.0-beta.1` is the proposed
first public beta version after the final gates. Do not label it stable/complete.

## Scope and positioning

The first beta focuses on the early survival loop: Darklands exploration,
Necronomicon discoveries, potential energy, basic rituals, early materials
and connected machines. Existing Abyssal Wasteland, Asorah and Dreadlands paths
may be explored as explicitly experimental content. Their presence does not
promise original-mod completeness or balanced endgame progression.

Omothol, Dark Realm, Chagaroth/J'zahar endgame, higher book tiers, the complete
research system, Energy Depositioner and Spirit Tablet transport are outside
this first beta's required scope. They remain on the full development roadmap.
Do not register fake substitutes merely to claim parity.

## English project text — draft

**Suggested title:** AbyssalCraft — Unofficial NeoForge Beta

**Summary:** An unofficial AbyssalCraft beta for Minecraft 1.21.1 NeoForge.
Explore Darklands, harness potential energy and perform rituals, with an early
survival loop and experimental dimension content.

**Description:**

Explore the Darklands, collect early materials and use the Necronomicon to learn
about potential energy and rituals. Build collectors, relays, reservoirs and
pedestals, process materials with the Crystallizer and Transmutator, and pack
ordinary items into a Stone Tablet using the State Transformer.

The Abyssal Wasteland, Asorah and Dreadlands are present as experimental paths.
This beta is incomplete: later dimensions, bosses, research and some original
systems are not implemented. It is not an official Shinoow release, a complete
replacement for AbyssalCraft, or a migration tool for 1.12.2 saves.

Use a fresh, disposable world. Report crashes, stuck progression, lost or
duplicated items, unclear instructions and rendering problems, including your
Minecraft/NeoForge versions, steps to reproduce and the relevant log.

Requires Minecraft 1.21.1, NeoForge and TerraBlender 4.1.0.8-compatible 4.x.
Developed and locally checked with NeoForge 21.1.234. Original Java provenance:
Shinoow and AbyssalCraft contributors; adapted source remains LGPL-3.0-only.
Matching source/build files and license notices must accompany distribution.
Original reserved media are not licensed by the Java license and are not
assumed available for this release.

## Factual initial-beta changelog — draft

### Added

- Early Darklands exploration, Necronomicon pages, PE charging and ritual paths.
- Connected early material/equipment and PE-device systems.
- Crystallizer, Transmutator and component-preserving Crystal Bags.
- Sacrificial Altars with reachable early tiers and saved energy/cooldown state.
- Stone Tablets and the State Transformer: 49 cargo slots, 200-tick pack/unpack,
  sided hopper support and inventory/component preservation.
- Experimental Abyssal Wasteland, Asorah and Dreadlands progression.
- Project-authored replacement pixel art, models and bilingual guide content.

### Known limitations

- Incomplete original feature coverage; see the scope above.
- Client rendering and a fresh-world survival session are not yet accepted.
- Source-informed timings/statistics are not a proven balance assessment.
- Several creatures still use Minecraft sounds; custom audio is unfinished.
- A two-process saved-player/machine/cold-return integration check is passed;
  broader death/respawn/restart combinations and real multiplayer remain open.
- No original 1.12.2 world migration or general third-party compatibility promise.

## Publication checklist

- [x] Current packet built, native interactions/menus/tickers tested; exact
  counts and artifact identity are tracked in the current `README.md` checkpoint.
- [x] LGPL text, accompanying GPL text and provenance notice bundled by Gradle.
- [x] Current matching source/build ZIP checked: all 2,211 file entries match
  the checkout, including sources/tests, wrapper/build scripts, licenses/notices,
  generators and the minimal loopback restart-test fixture. No worlds, logs,
  caches, private brain/config, upstream tree or preaccepted EULA are included.
  Source JAR: 2,109 matching source/resource files. Exact artifact identity is
  tracked in current README. Local validation is not public source availability.
- [ ] Bundled short playtest in `BETA-PLAYTEST.md`; no unresolved core crash,
  lost/duplicated items or unreachable required early progression.
- [ ] Confirm public name/branding and final beta metadata/version; current
  `0.1.0-dev` metadata deliberately still identifies the local unreleased study.
- [ ] Make the exact matching source/build bundle publicly available with the
  binary and declare TerraBlender as a required dependency.
- [ ] Explicit authorization to upload/publish. No such action is performed here.

No installer, instance modification or automatic release is part of this sheet.
