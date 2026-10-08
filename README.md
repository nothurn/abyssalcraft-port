# AbyssalCraft Port Study (unreleased)

## Current Energy Depositioner / feedback-beta checkpoint — 2026-10-08

The original-oriented early Stone Tablet fuel path is now connected: the
Energy Depositioner consumes valid non-cursed sealed cargo over **200 ticks**,
generates its value in a **10,000 PE** buffer, returns the emptied tablet and
feeds isolated collectors with source charm factors/tolerance/disruptions.
The native original recipe, two-slot menu, hoppers, carried mining/placement
job, owned machine art, four Darkstone shapes and bilingual safety guide exist.
Names/custom components survive; full outputs do not overwrite or consume
queued fuel. Corrupt saved jobs remain carried/paused, not reset for PE replay.
Protection callbacks cannot reenter a tick or mine a source and then copy its
saved PE balance into a collector through the removed old instance.

Modern conversion uses whole 4×4 biome cells entirely inside the original
17×17 footprint, real negative/upper world Y, already loaded chunks, native
placement veto/whole-cell rollback and a cancellable `DepositionEvent`.
It currently operates only in the Overworld and never replaces an existing
blockentity. Original Ritual Altar/Pedestal conversion and other-dimension
conversion remain explicit parity gaps, not silently completed features.
Fuel cargo/overflow are consumed; use away from builds to preserve.

Final full proof: `test-artifacts/depositioner-gate-complete-20261008.log`,
**BUILD SUCCESSFUL in 7m12; 106 JUnit / 260 flat GameTests / 49 normal-world
checks**, all passed. **15 Python tests**, actual binary **517 models / 142
blockstates / zero local reference errors**. All **344 packaged project
classes** are byte-identical to compiled output. **2,156** source-JAR files
and **2,261** source/build ZIP files match the checkout exactly; bundled
LGPL/GPL/source notices are present. No private worlds/logs/config/brain,
upstream tree or preaccepted EULA is in the source/build bundle.

An initial native launch failed on new stairs accessing unbound Darkstone
before the base block register. `depositioner-native-first-20261008.log`
records that failure despite launcher exit zero; it is NOT a passed test run.
Registration order was corrected. Gradle run tasks now demand native pass/
completion messages and reject such zero-exit startup failures. Intermediate
positive logs are historical; the final full log above is the current gate.

Current local binary: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`,
**2,398,028 bytes**;
SHA-256 `82CF77BB09FF123BF22C0D7A83C2C01E06AA820CC3DA8F3252207AB46D647E22`.
Matching source JAR: `build/libs/abyssalcraft-port-study-0.1.0-dev-sources.jar`,
**1,600,530 bytes**;
SHA-256 `E10243BCC5CDBBB1D4A8AEE80BA1078DF21A207E36ABC82CADE7FAF01ED49F1D`.
Source/build ZIP: `build/distributions/abyssalcraft-port-study-0.1.0-dev-source-build.zip`,
**1,954,293 bytes**;
SHA-256 `A1D1E244DA5DA63551FA811EEBD831BD40C74DABBE8D2F4957082FDBFD37B776`.
Artifact/package verification follows `depositioner-final-package-20261008.log`;
this was no-op compilation plus the current source/build ZIP, not a new untested
production-code build.

Local checkpoint destination for this newer exact candidate:
`test-artifacts/beta-candidates/feedback-depositioner-20261008/`.
The older `feedback-restart-20261008/` remains unchanged: all three previously
recorded artifact hashes were rechecked. First feedback beta remains the
immediate user priority; this optional machine is not an added mandatory
playtest gate. Current metadata is still `0.1.0-dev`, not an approved public
beta. Short survival/render acceptance, final public beta metadata, matching
public source availability and explicit publication approval remain open.
No client, instance installation, upload, publication, deletion or commit.

Staging is verified in `depositioner-stage-candidate-20261008.log`: exactly
**11 copies** match the current inputs. The repeated existing-destination task
is deliberately rejected with exit one; its stacktrace confirms the guard,
and all eleven frozen hashes stay unchanged. See
`depositioner-stage-reuse-rejected-20261008.log`; this expected negative result
is not a failing product gate. The frozen JAR also passes its own asset audit.

Changed code: `EnergyDepositionerContent/Block/BlockEntity/Menu/Screen`,
`DarklandsDeposition`, `DepositionEvent`, registered `EnergyDepositionerGameTests`
and `EnergyDepositionerAssetsTest`; narrow integration in `AbyssalPort`,
`AbyssalPortClient`, `NecronomiconScreen`, `RitualCharmItem`,
`DeityStatueBlockEntity`, `StatueDisruptions`. `build.gradle` strengthens existing
headless evidence checks. New generator/resources plus charm/transformer guide
generators and beta/source/build/checklist documentation are updated. Existing
portal, transformer, player inventory and unrelated production systems were
not redesigned. The full development roadmap remains active beyond this beta.

## Frozen prior real-restart / first feedback-beta checkpoint — earlier 2026-10-08

The previously open cold return after a complete process restart now has an
actual two-JVM proof, not only a same-process block-entity deserialization.
`RestartAuditProbe` extends the existing opt-in WorldAuditTestRunner with
prepare/verify phases and a separate `gateway-audit-restart-*` world. Native
PlayerList save, full normal shutdown and native chunk reload persist the data.
No portal, player inventory or machine production behavior was changed.

- `test-artifacts/restart-prepare-first-20261008.log`: JVM **20532** creates
  fixture platforms/paired anchors, performs native outbound player travel,
  saves the actual Wasteland player file and a live machine at **73 ticks**,
  removes persisted force tickets and halts with complete chunk saving.
- `test-artifacts/restart-verify-first-20261008.log`: separate JVM **17544**
  loads that same world. The exact Overworld return chunk is absent before
  login and travel. The native player reload retains book/name/**1234.25 PE**,
  named stacks, filled crystal bag, research, XP, health and respawn position/
  angle. The portal processor loads the original return anchor and uses its
  exact UUID/reciprocal pair and safe arrival instead of recreating it.
  Machine progress is exactly **73** on native reload; normal tickers complete
  packing and unpacking with all 49 slot positions and item components intact.
  Neither a manual machine tick nor synthetic BE replacement is used.
- Negative `restart-prepare-reuse-rejected-20261008.log` deliberately exits
  **one**: reused prepare state is refused. Saved audit-state and native player
  bytes remain unchanged. Invalid world and phase logs also deliberately fail
  at Gradle configuration, before server startup. These red logs are expected
  rejection proofs, not successful product builds or hidden failures.
- Full regular gate `test-artifacts/restart-gate-full-native-20261008.log`:
  **BUILD SUCCESSFUL in 6m49, 104 JUnit / 248 flat GameTests / 48 normal-world
  checks**. Separate restart phases are not added to the GameTest count.
  **15 Python tests**, actual JAR **499 models / 137 blockstates / zero local
  reference errors**. All **330 packaged project classes** match compiled bytes.

Previous frozen binary: `test-artifacts/beta-candidates/feedback-restart-20261008/abyssalcraft-port-study-0.1.0-dev.jar`,
**2,330,376 bytes**;
SHA-256 `F6163F4AF673D2B3FAEA96D98C494F0392A4F6F30285715069744DCA71CF6352`.
Previous matching source JAR, in that same frozen folder: `abyssalcraft-port-study-0.1.0-dev-sources.jar`,
**1,564,372 bytes**;
SHA-256 `CBC656E0261E8BA97099AD1268A61D7E17044793A152BD88D9744E0F6A9A21D5`.
Source/build ZIP:
`test-artifacts/beta-candidates/feedback-restart-20261008/abyssalcraft-port-study-0.1.0-dev-source-build.zip`,
**1,908,714 bytes**;
SHA-256 `EFE298C26E7A3098FE18D876A6D14237E55B9CBD2FB6A6A8C7566C839FEDEBC1`.
Source JAR: **2,109 matching source/resource files**. Source ZIP: **2,211
matching file entries**, with reproducible hash over two task executions.
Both JARs retain LGPL/GPL/source notices. The source/build package includes the
project-authored loopback fixture and documented restart commands in BUILDING;
no saved world, private run config, preaccepted EULA or original media is bundled.

Local checkpoint staging destination for this exact candidate:
`test-artifacts/beta-candidates/feedback-restart-20261008/`. The Gradle
`stageBetaCandidate` task retains the three artifacts plus current test/license
documents (this README as CHECKPOINT.md), and refuses an existing destination.
This keeps the first feedback candidate separate from later development outputs;
it is neither an installed instance nor an approved/public beta.
Staging is now actually verified: **11 files** match their inputs at copy time,
and the frozen binary passes the same asset audit. Repeating the task is refused
with exit one and all eleven frozen hashes unchanged. Gradle's normal error
display wraps onlyIf failures; the specific refusal was confirmed with
`restart-stage-candidate-reuse-stacktrace-20261008.log`. The initial diagnostic
assertion expected that cause in the short output; it was a test-reporting
assumption, not an overwritten checkpoint or a product failure. CHECKPOINT.md
remains the original copied note; later maintainer-note edits do not modify it.

First feedback-beta scope remains `BETA-GATE.md`. This closes one server-side
restart gap, not client rendering, a real-account/multiplayer session, every
death/respawn/restart combination or full original parity. Client start,
installation/deploy, publication/upload, deletion and Git actions remain absent.
The public beta metadata/version is not silently substituted for `0.1.0-dev`.
After this technical gate, the short bundled playtest still awaits explicit
client authorization; missing original systems remain ongoing development.

## Historical Stone Tablet / beta preparation checkpoint — earlier 2026-10-08

The current connected packet is technically implemented: Stone Tablet +
State Transformer, not the distinct Spirit Tablet transport configurator.
Native survival recipes connect four Shadow Shards/four Monolith Stones/one
Shadow Gem to a blank tablet, and seven Monolith Stones/one wooden chest/one
Transmutation Gem use to the machine. The existing ten-use catalyst keeps its
real native remainder. Forty-nine cargo slots map exactly into the singleton's
native CONTAINER component. Both modes take 200 production ticks (10 seconds at
20 TPS). The source count*integer(64/maxStackSize) value is derived from cargo;
this tablet is not a rechargeable PE battery.

Names, damage, private components, exact slot positions and valid in-flight
save snapshots survive pack/unpack. Nested/native/capability inventories,
cursed/forged contents, occupied outputs, stale menus and mid-process mutation
cannot overwrite cargo. Native menu authority, live data sync, shift/drag/
pickup-all locks, maximum 49-full-slot cargo, sided hopper transfer and actual
mining drops are tested. Own thin 3D tablet/runic pixels/machine geometry and
bilingual UI/guide are included. The guide is on the reached infusion page;
client appearance is not claimed visually accepted.

Verification:

- `test-artifacts/tablet-full-native-first-20261008.log`: full build, **104
  JUnit / 247 flat GameTests / 48 normal-world checks**, exit zero. Real
  registered tickers perform 200-tick packing and unpacking in both worlds,
  restoring exact cargo/components (151 derived PE); actual hopper insertion,
  processing and chest extraction also complete without manual machine ticks.
- `test-artifacts/tablet-beta-native-final-20261008.log`: after final guide/UI
  text and actual menu-opening coverage, **104 JUnit / 248 flat GameTests**,
  build successful. A real native server player opens the registered 86-slot
  menu with an embedded test connection; client renderer was not started.
- **15 Python tests**; actual JAR asset audit: **499 models / 137 blockstates /
  zero local reference errors**. All **328 packaged project classes** match
  compiled bytes; packed transformer bytecode retains the 200-tick value,
  atomic seal/unpack and active-snapshot save fields.
- `test-artifacts/tablet-beta-packaging-20261008.log`: final build repackages
  only companion license/notice entries, Java unchanged; tests are up-to-date
  from the native run, not falsely counted as rerun. LGPL + accompanying GPL
  + source notice included in both JARs. Source JAR: **2,108 matching
  source/resource files**. Source/build ZIP: **2,209 matching file entries**,
  complete src/tests/wrapper/build/generators and no worlds/logs/caches/private
  brain/upstream tree. Build instructions in `BUILDING.md`.

Current local binary: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`,
**2,318,866 bytes**;
SHA-256 `DC919AFC22B3ECB3ED9CECCECEBEB1898C09AEC448C1ABCCF261A375B81BB290`.

Matching source JAR: `build/libs/abyssalcraft-port-study-0.1.0-dev-sources.jar`,
**1,559,027 bytes**;
SHA-256 `BC6C887A051F34551F76CB996A1ACA5154CB3B6040932A6B3E7418F57AD532F0`.
Source/build ZIP:
`build/distributions/abyssalcraft-port-study-0.1.0-dev-source-build.zip`,
**1,900,796 bytes**;
SHA-256 `8DEA9071EF810F6E9A7FCC58F764584D7F5BF3E227EB5616DCFB1C1D27612F57`.
Two independent task executions with unchanged input produce that same ZIP hash
(`tablet-beta-source-bundle-final-20261008.log`). Final entries were compared
again after the publication checklist update. This is normalized ZIP packaging,
not a promise of cross-environment bit-identical Java compilation.

Noah now prioritizes a **first feedback beta**, not complete original parity.
`BETA-GATE.md` defines the early survival core, `BETA-PLAYTEST.md` the bundled
short playtest, and `BETA-RELEASE.md` honest English release text/known limits.
Existing Wasteland/Asorah/Dreadlands are experimental extensions; late worlds,
bosses, full research, Energy Depositioner and Spirit transport remain roadmap.
The working metadata deliberately stays `0.1.0-dev` / unreleased study until
playtest, final branding/version and explicit publication approval. A local
source bundle is not public source availability. No client start, installation,
instance deployment, upload/publication, deletion or Git action occurred.

## Historical Sacrificial Altar checkpoint — earlier 2026-10-08

Four reachable native Sacrificial Altars (base/Overworld/Abyssal Wasteland/
Dreadlands) now connect the existing survival materials, native crafting,
paid ring upgrades and PE/book network. The canonical recipe consumes five
Monolith Stones, two Shadow Fragments, one Coralium Pearl and one Shadow Gem.
All six lower-to-higher native anvil takes charge one ring/XP level and preserve
the actual singleton's PE, name, private components and cooldown counters.

Loaded altars select 1/2/3/4 adult living mobs in the source 8/3/8 acquisition
box. Native undead tags and the real Shadow family (including Shadow Ghoul) are
excluded. Only an admitted native death grants maximum-health PE, exactly once
per tracked target; cancelled death or removed living entities grant none.
Cancelled loot and `doMobLoot=false` do not erase an admitted death. Source
integer collection-count truncation is retained separately from fractional PE.
The capacity/5 collection threshold begins the source 1200/1000/800/600-tick
cooldown. Bounded tracked deaths can wait while cooling/full, not an unbounded
death queue. Targets are not serialized; PE, collection count, cooldown and the
actual display item are. Mining returns the displayed item separately, while
native loot and subsequent BlockItem placement retain the altar counters.

All four placed altars reject incoming book, relay and statue PE; they can
output to a relay/book and charge their displayed native item at 20 PE/tick.
Their source Collector marker does not grant input capability. As items, the
base altar cannot accept PE, but higher-tier altars can; all can drain their
stored PE into a Reservoir. Native shift-click puts the non-chargeable base
item in the drain slot instead of an unusable charge slot. Full inventories and
Adventure restrictions keep the actual displayed item. Own basin geometry,
pixel inset, native renderer and a reachable bilingual network-book guide are
included; no client visual acceptance is claimed.

Full run `test-artifacts/sacrifice-full-native-first-20261008.log`, exit zero:
**BUILD SUCCESSFUL in 7m 7s: 102 JUnit / 238 flat GameTests / 47 normal-world
tests / 15 Python tests**. Both registered live-ticker paths transfer exactly
23.75 PE from an actual cow death into the named book, leaving zero altar PE
and source collection count 23. No manual altar ticks are used in those paths.
Actual JAR: **494 models / 136 blockstates / zero asset-reference errors**;
eighteen relevant packaged classes are byte-identical to compiled output,
including registration, receiver guards, native renderer and altar tests.
Packed bytecode contains the false BE-input capability, real death-event hook,
source integer collection arithmetic and saved counters. New media is owned;
original code provenance/LGPL remains. The first native compilation caught an
incorrect test registry reference; corrected to actual WastelandMobsContent,
with the failure log retained, not treated as a product run.

Current artifact: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`,
**2,253,673 bytes**;
SHA-256 `06054C19AFE25C18E4A7EC1D6A7AC38873EEF694221E7AFD0DA32264CABC6FD6`.

The complete mod is still in development. Next connected dependency:
**Stone Tablet -> State Transformer -> Energy Depositioner**, not a rename of
the distinct Spirit Tablet item-transport configurator. Actual source tablet,
transformer BE/block/container/slots, canonical recipes and depositioner were
read: 49 cargo slots + tablet, two 200-tick pack/unpack modes, preserved contents
and source item-derived PE. Modern native components, recursion prevention,
atomic unpack and native permission/loaded-bound checks must be implemented;
none of that next system is claimed finished. Spirit Tablet/Spirit Altar/item
transport, Omothol/later progressions/bosses/research/config/audio and rider/leash
migration remain open. No client, deployment, installation, external action,
deletion or Git action. No intermediate player test requested.

## Historical connected PE checkpoint — earlier 2026-10-08

Sixteen reachable native PE devices cover Collector, Relay, Reservoir and Energy
Pedestal at base/Overworld/Abyssal Wasteland/Dreadlands tiers. Existing base IDs and
saves remain intact. All 96 base role/direction combinations, first-receiver/full-
block stops, redstone, exact tier ranges, native large-value menu synchronization,
single-item component balances and all 24 funded anvil upgrade paths are tested.
Four independently drawn rings and six paid source-material infusions connect
the existing three book tiers; Omothol is not registered as a placeholder.

The source `PEUtils.transferPEFromContainer` means **device -> item**. The singleton
Energy Pedestal collects statue PE and charges its actual displayed item at
20 PE/tick, retaining fractional overflow, names and other components. Full player
inventory leaves the item on top; actual mining returns the item separately from
the PE-bearing device. Collector/Relay use original Monolith Stone materials.
The Reservoir now requires the original 100-PE Pedestal infusion; only PotEnergy
is retained and the cheap prototype chest craft is disabled, not deleted.
The registered live ticker connects a charged device input -> Reservoir -> Relay
-> Pedestal -> book with an exact total of 73.75 PE, without manual ticker calls.

Actual accepted Relay/statue transfers emit a negotiated, optional native PE
stream payload. Unnegotiated or distant players receive none and cannot crash or
change the transfer. Loaded bounds, rejected/full targets and a native signed-
position codec are tested with embedded connections, not a real client/socket.
An owned eight-pixel sprite and bounded shared geometry honor particle settings;
their actual client appearance has not been inspected. Models/guides are owned.

Full run `test-artifacts/pe-source-recipes-direction-full-second-20261007.log`,
**BUILD SUCCESSFUL in 6m 57s: 100 JUnit / 231 flat GameTests / 46 normal-world
tests / 15 Python tests**. Actual JAR: **486 models / 132 blockstates / zero
asset-reference errors**; sixteen packaged classes match compiled bytes, and
native direction, channel guards, optional registration and recipes were checked.
Artifact: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`, **2,210,731 bytes**;
SHA-256 `28B27FF93CF994B55416F72DECD9D3DF192A1310F5449DB893C04A17D78A9BFF`.

Earlier logs are retained: the first pedestal tests incorrectly expected item
draining and do not prove source alignment; the first stream run crashed on an
unnegotiated channel. Both expectations and product paths are corrected. The
first canonical-recipe full run found an older Darkstone crafting fixture; it is
now source-correct, and both complete gates passed in the second run.

Full-mod development remains active. Next: source Sacrificial Altar (adult
non-undead/non-Shadow targets, admitted death PE, saved cooldown/collection limit,
singleton charging, four reachable tiers and ring upgrades). Original base/tier
BE, blocks, item-role API and recipe have been read; it is **not implemented**.
Spirit Tablet/Depositioner, Omothol, later bosses/research/configuration/audio and
riding/leash conversion migration remain open. No client, installation, upload,
publication, deletion or Git action occurred; no intermediate player test needed.

## Historical disruption-possession checkpoint — 2026-10-07

Equipped animals are no longer exempt from the actual corruption/sacrifice
effects. New `energy/ConversionPossessions` returns original native inventory
and equipment stacks, horse armor/saddles, donkey cargo/chest, Pig/Strider saddles
and Villager belongings before removing the owner. Counts, names, durability,
PE and arbitrary item components are preserved. This returns possessions, not
invented death/species loot. The original horse-death equipment return is retained
without unsafe removal or destroying other native inventories.

Replacement and every returned ItemEntity must pass actual native admission.
Vetoes, reentry, source inventory edits or modification of an earlier returned
stack abort the transaction: staged outputs/replacement are removed, the owner
and current possessions remain. Listener edits are not overwritten by a stale
snapshot. Conversion Post is emitted only after a successful complete transaction.
Exact native ItemEntity save/load is checked, not just serialized JSON.

Full run `test-artifacts/disruption-possession-full-native-20261007.log`, exit
zero: **92 JUnit / 211 flat GameTests / 45 normal-world tests / 14 Python tests**.
Four new native possession tests plus the updated conversion test are green;
all 45 existing normal-world paths are green, including the actual statue ticker.
JAR audit: **452 models / 119 blockstates / zero asset-reference errors**; packaged
transaction/Saddleable/event-order bytecode inspected.
Artifact: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`, **2,077,450 bytes**;
SHA-256 `0B1751AA938867289829CFDD3B4542CFA3B3EB54CDDC61940192B3714325339A`.

Riding/passenger/leash links are still safely retained rather than silently
broken; complete link migration remains open. This is native built-in inventory
coverage, not a claim to migrate arbitrary third-party storage attachments.
Next connected PE work: current relay only pulls Collectors and only pushes
Containers, and the two-slot reservoir accepts books only. The inspected source
accepts all eligible PE containers/items; complete these roles, directional
chains, source tiers and real streams without replacing existing save IDs.
Higher progression/worlds/bosses/configuration/owned audio and final player
acceptance remain open. No client, installation or external action occurred;
the full-mod goal is active. Historical checkpoints follow.

## Historical full disruption-dispatch checkpoint — 2026-10-07

All 27 actually registered source disruptions are connected: 18 common effects
and nine deity-bound effects, with the complete eligible random pool. The actual
statue ticker consumes tolerance >=100 and resets it to zero before callbacks,
as the inspected source does. Native configuration, validated Places of Power,
loaded bounds and a cancellable disruption event remain authoritative.

Actual effects include player/status damage, source PE fractions, native weighted
biome spawning, all three Shadow forms, Evil sheep/Shub, native animal and Depths
Ghoul conversions, invisible Endermen, actual fireballs/lightning, layered Ooze
and validated event-directed teleports. Conversion/spawn/placement/teleport vetoes
are tested against native events, not mocked successful outcomes. A real defect
in non-player placement notifications was fixed: the notification snapshot now
describes the placed fire/Ooze, while a separate old snapshot supplies rollback.
The guide and 27 bilingual effect labels are owned resources; no original media.

Full run `test-artifacts/disruption-complete-first-world-full-20261007.log`, exit
zero: **92 JUnit / 207 flat GameTests / 45 normal-world tests / 14 Python tests**.
Actual JAR: **452 models / 119 blockstates / zero asset-reference errors**.
The normal-world statue starts at tolerance 98, dispatches once through its real
registered ticker, and finishes with actual hand energies 10/10 and tolerance 4.
Artifact: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`, **2,068,698 bytes**;
SHA-256 `EA0EE0F7B305BAE267227AE3F9BF974F7A0700F196F63C9CD846346AA1A2389C`.

Earlier red logs are retained: unsupported current native API assumptions, a
JUnit-only classpath fixture, fresh-player invulnerability, the real placement
snapshot defect and a swap fixture selecting unrelated native entities. Native
fixtures were corrected without weakening product rules. The complete catalogue
is technically connected, not a claim that the whole mod or all protection mods
are finished. Equipped mount/cargo migration and rider/leash handling are still
open; such mounts are currently retained rather than losing possessions. Higher
PE devices/streams, progression, dimensions, bosses and owned audio remain open.
No client restart, installation or external action occurred. Historical evidence
follows; the full-mod goal remains active without an intermediate player gate.

## Historical animal-ecology checkpoint — 2026-10-07

Four native Evil species (Cow/Chicken/Pig/Sheep), their four Demon replacements
and Shub offspring are connected: actual attributes, AI, source-default common
configuration, moon/altitude/ground spawn gates and live typed biome modifiers.
Loaded Plains/Nether entries have weight 15 per species; ordinary forests have
offspring weight five and Darklands/dark forests ten. Zero configured weight
refuses natural spawning. Source species drops, Demon 0–2 Rotten Flesh and the
offspring's empty loot are actual server loot, not guessed rewards.

Real survival-player shearing transforms one Evil animal, emits its material
once and wears the shears once. Conversion/finalize/join vetoes, loaded complete
replacement bounds, liquids/collisions and callback reentry are respected before
the victim is removed. Accepted Evil deaths can yield a Demon; Shoggoth feeding
does not. Shadow kills cannot produce both Demon and Shadow replacements.
An Evil Sheep records a real player's accepted death identity and supplied skin
properties, saves bounded data and subsequently shears Rotten Flesh. Native
client skin-cache rendering is asynchronous; its appearance is not yet accepted.

Demon burn state is synced/saved, extinguished by water, and follows source
Nether/optional Overworld fire rules. Actual entity-grief and block-place vetoes
protect the corner fire trail; vetoed changes restore their block snapshot.
Owned Mimic Fire uses a reduced native fire algorithm and vanilla fire visuals,
with native wet-biome tags, loaded-neighbor guards and doFireTick handling.
Nine independent articulated rigs/atlases, nine eggs and bilingual book guidance
are owned assets; no original models/textures/sounds were copied.

Final full run: **90 JUnit / 194 flat GameTests / 44 normal-world tests / 14 Python
tests**, **452 models / 119 blockstates / zero asset-reference errors**.
`test-artifacts/animal-ecology-reentry-final-full-20261007.log`, exit zero.
Native AI, without direct test damage, reached **298 entity ticks**; measured
movement was **4.56/4.56/4.44 blocks**, player HP **198/198/196** from 200.
The final packaged bytecode reserves conversion before native callbacks.
Artifact: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`, **2,019,227 bytes**;
SHA-256 `65ED031F99435F490E4EDEE1738D3C4DAF2951622E8AC96A047CA0573609806E`.

Earlier red runs remain evidence: reusing a scheduled Runnable erased its next
native schedule; a fresh wrapper fixes the fixture. Another fixture checked
combat before the source-speed creature had arrived. It now waits for actual
bounded AI hits, without weakening speed/cooldown. An older Basic Scroll test
also failed once; its isolated batch now waits for native item integration.
No general flake-free claim or unproven root-cause claim for that older failure.

New Java: `config/MobEcologyConfig`, `worldgen/MobEcologySpawns`,
`content/MobEcologyContent`, `entity/{CorruptedAnimalEntity,ShubOffspringEntity,
DemonFire}`, `block/MimicFireBlock`, five owned model/renderer/skin classes and
`AnimalEcologyGameTests`; registry/client/book/Shadow death wiring updated.
New generator `tools/generate_animal_ecology.py` and `AnimalEcologyAssetsTest`.
Run the generator after shared language/Darklands generators.

This is a connected animal core, not full original configuration/media parity:
global Hardcore armor-piercing and other families remain open, and the explicit
modern movement attribute is an adaptation. Client appearance/balance/audio and
later dimensions/progression remain unaccepted. Next: the complete registered
27-effect disruption dispatch, not a reduced pool or invented tolerance reset.
Full-mod goal stays active; no intermediate player gate. No client was restarted,
installation changed or external action performed. Historical checkpoints follow.

## Historical Shadow-family checkpoint — 2026-10-07

The existing Shadow Creature save/registry key is preserved. Real native Shadow
Monster (40 HP/eight melee damage/42 follow range) and Shadow Beast
(60 HP/ten melee damage/42 follow range) are registered with their source sizes,
fire/poison/underwater immunities, family-friendly damage immunity, spawn eggs,
independent articulated rigs/UV atlases, native headgear layer and October-31
holiday rule. The existing Creature's native movement adaptation is retained;
this is not a claim of finished original Hardcore/configuration/media parity.

The Beast waits 300 active entity ticks, then breathes for 100, with saved/clamped
`BreathTimer`, synced visual state, source smoke/Ghast-shoot effects and slowed
movement. The nearest living ray is block-clipped and refuses unloaded terrain.
Native absolute Shadow damage never submits negative amounts; blindness and
slowness follow accepted hits only. Shadows/Ghouls reject their family's attacks.
Original ray-through-wall and negative-distance-damage defects are intentionally
corrected rather than reproduced.

Native non-cancelled Shadow deaths can transform non-Shadow victims: Creature,
Monster or Beast by original height thresholds, Ghoul -> Shadow Ghoul; player
death leaves a named persistent Monster without discarding the player or replacing
native death/respawn/inventory handling. Real conversion/finalize/join vetoes,
loaded space and collision checks run before removing a non-player victim.
Refused transformations leave ordinary death handling intact. Shadow-type kills
and breath damage are both connected; unavailable Dark Realm features are not
declared implemented.

All five real Darklands biome keys contain the three source spawn entries once:
70/50/20 weights, 3/2/1 group sizes; Mountains 100/80/40. The older rare
Dark Forest/Swamp Creature fallback remains without duplicating Darklands entries.
Monster loot is 0–2 Shards and Beast loot 0–2 Gems, with native Looting increases.
Both forms supply actual Shadow Rending energy; the bilingual existing material
page explains the connected family. No original textures/models/sounds imported.

Verified full native run: **87 JUnit / 180 flat GameTests / 42 normal-world tests /
14 Python tests**, **443 models / 118 blockstates / zero asset-reference errors**.
`test-artifacts/shadow-family-native-measured-ticks-full-20261007.log`, exit zero.
Normal-world creatures of both native ID parities reached **338 actual entity
ticks**, breath timers **62/62**, and damaged real targets **200 -> 194 HP**.
This is server-mechanic evidence, not visual/animation/balance acceptance.
The first world run correctly failed its premature tick-count assertion:
340 world ticks had produced 310 entity ticks after chunk activation. The fixture
now waits for measured entity ticks; no product cooldown/AI was weakened.

Actual packaged classes, registry IDs, damage/loot, full UV coverage, loaded guards,
save clamps, veto/admission ordering and license were inspected, not just source.
Artifact: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`, **1,916,178 bytes**;
SHA-256 `43FFA14A69FD47A423A14BEFE9EB9F6204DC0BBE86038F0580A1F40700E26525`.
New Java: `content/ShadowFamilyContent`, `entity/ShadowFamily`,
`ShadowFamilyGameTests`; expanded `ShadowCreatureEntity`, owned model/renderer,
client/registry/book wiring. New `tools/generate_shadow_family.py`,
`ShadowFamilyAssetsTest`; Darklands asset regression updated for the real family.
Run the new generator after the older Darklands biome generator.

The complete registered disruption source catalogue was checked: 27 entries,
18 generic/nine deity-bound; tolerance threshold 100, not a guessed time interval.
Automatic disruptions remain **unconnected** until all real effect/entity
dependencies are present. No reduced pool or fictitious tolerance reset enabled.
Next: native Evil/Demon animals and Shub offspring, then complete event-respecting
disruption dispatch. Full-mod goal remains active; no intermediate player gate.
No additional client was launched, no installation or external actions.
The previously authorized frozen client snapshot was closed normally and does
not contain these newer family/structure changes. Historical checkpoints follow.

## Historical Places-of-Power checkpoint — 2026-10-07

All three source layouts are now reachable through a real Necronomicon block
interaction: Basic (15 Monolith Stone, four pillars, four statues), Totem Pole
(one stone, three statues) and Archway (one stone, one statue, six walls,
four stairs, six slabs). Only the activation stone changes; the book and PE
remain intact. All seven deities and mixed statues are accepted. Any actual
wall/stair/slab material may make the arch, with correct directions and halves.
The native ItemStack interaction retains NeoForge placement events and rollback;
an explicit veto regression confirms one event and no surviving authorization.
Fully loaded layouts may cross chunk boundaries; validation never loads chunks.

Basic/Totem/Archway supply source range amplifiers 2/3/1 (+8/+12/+4 book range,
+2/+3/+1 collector radius and lower search layers), combined with the existing
charm/pillar rules. Complete structures charge indoors and beside their own
statues without adding tolerance; previously stored tolerance is preserved.
Saved labels/pointers are never sufficient: real base, complete current geometry,
membership and ownership are checked. Breaking required blocks removes benefits
immediately; the 100-tick native base check can rebind a replacement statue after
repair. Geometry is checked once per synchronous charging invocation rather than
once for every target; no cached authorization survives that invocation.
The base mines/picks as ordinary Monolith Stone and has no portable item/recipe.

The complete intended source geometry fixes two original validation defects:
Basic's mutable/downward anchor and Archway's conjunctions accepting fragments.
The owned base uses existing owned Monolith Stone pixels. Three always-available
book pages show native item-based layer plans from the same Java layout that
validates the world, with orientation/half labels and bilingual instructions.
No original textures, models or sounds were imported; final book rendering is
not claimed visually accepted.

Verified: **84 JUnit / 173 flat GameTests / 41 normal-world tests / 14 Python
tests**, **440 models / 118 blockstates / zero reference errors**. Full run:
`test-artifacts/places-of-power-native-full-20261007.log`, exit zero, including
900 registered ticks under real stone roofs. Observed book totals were
Basic 110 PE / Totem 85 PE / Archway 90 PE, all statue tolerances zero and ranges
15/19/11; stochastic observations, not promised transfer totals. The final
expanded native-veto/boundary/all-deity regressions and build passed in
`test-artifacts/places-of-power-native-veto-final-flat-20261007.log` (173 tests).
Only test code changed after the full normal-world run. Packaged classes,
base model/loot, loaded guards and transfer/save bytecode were inspected.

Artifact: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`, **1,870,431 bytes**;
SHA-256 `E1B05FCDEBFB27AF3C3C2BCAF505EC9F9B0AB78ADBF3C3E1EDAC7AD3D22B1AAE`.
New Java: `energy/{PlaceOfPowerLayout,PlaceOfPowerFormation}`,
`content/PlaceOfPowerContent`, `block/{PlaceOfPowerBaseBlock,
entity/PlaceOfPowerBlockEntity}`, `PlaceOfPowerGameTests`; changed registry,
statue BE, Necronomicon interaction/guide. New resource generator
`tools/generate_places_of_power.py`, base model/blockstate/loot, pickaxe tag,
bilingual guide and `PlaceOfPowerAssetsTest`. Run this generator after the
statue/charm/independent-energy generators. Checklist, source audit and brain
updated; previous identities below are historical.

At Noah's explicit request a visible isolated project client was launched for
his own test, using an immutable copy of the preceding green statue-PE build:
`runs/client-checkpoints/statue-pe-20261007-d41afdee7507467bb7ccfaaf6acdd836/`.
It reached rendering and an actual `New World (2)` session, then logged normal
save/shutdown, not a crash. No GUI input was automated and no Modrinth instance
or installation was changed. That client did NOT contain the new structures;
its frozen code/resources were unaffected by development builds. A clean client
start/session is not visual/playability acceptance of the whole mod.

**Still incomplete:** full disruption catalogue, PE streams/higher devices,
configuration, named tier-four charm infusions and later dimensions/progression,
bosses and original-scope audio. Complete-development goal remains active; no
user interim test is required to continue, and no upload/publication/commit.

## Historical independent statue-energy / shared range-pillar checkpoint — 2026-10-07

Real statues now have separate PE paths rather than sharing the older effigy's
deterministic transfer clock. Held books use 120 active ticks and 0–9 restart
jitter, gated by the closest non-spectator holding a book. Both hands receive
the quanta; full books incur no tolerance. Dropped Necronomicons and isolated
collectors each have an independent 1/120 chance per tick (generic Duration:
1/80); neither needs a nearby player. The registered ticker uses the world RNG,
not the controlled entropy used in synchronous transfer regressions. Dropped
device BlockItems are not book transporters. Native stack synchronization,
custom components, capacities and accepted-only 2/4-book, 1/2-collector tolerance
are checked; tolerance is saved and overflow-safe.

The **existing** gateway `monolith_pillar` ID now supplies the source half-width
range amplifier, with one shared BlockItem, own pixel/voxel resources, hardness
6/resistance 24 and the original four-Monolith-Stone square recipe. Native
crafting consumes all four inputs and returns one correctly mapped pillar.
An initial duplicate-registration error in this development was caught by that
identity assertion and removed; portal/saved IDs are retained. One/two consecutive
pillars below a statue give +4/+8 book range and one/two lower collector layers.
Floating second pillars/full stone do not count. Twenty found collector positions
are cached before full/isolation filtering and revalidated live; initial/range
changes refresh immediately, otherwise once per 200 real ticks. This deliberate
source-dayTime difference avoids frozen-daylight scans/starvation. Searches do
not force-load chunks. Gateway asset regeneration delegates the pillar to the
single shared generator instead of overwriting it with a full cube.

Verified: **82 JUnit / 167 flat GameTests / 40 normal-world tests / 14 Python
tests**, **439 models / 117 blockstates / zero reference errors**. Full offline
build/server run: `test-artifacts/statue-independent-shared-pillar-full-20261007.log`,
exit zero. In 800 actual normal-world ticks with no nearby player, a dropped book
received 90 PE, its collector 110 PE, tolerance was exactly 58 and held timer zero.
These are observed stochastic values, not fixed future balance promises. Both
held hands still charge on all seven native statues; Range/Power/Duration in the
actual 180-tick audit were 10/15/30 PE. After the final attribution-only source
comment, build/JUnit and the complete 167-test flat suite passed again in
`test-artifacts/statue-independent-final-jar-flat-20261007.log`; no behavior was
changed after the full normal-world audit. Actual JAR bytecode, shared registry,
recipe/model/texture, tolerance persistence and loaded-query guards were inspected.

Artifact: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`, **1,828,970 bytes**;
SHA-256 `B191FE142786689FA2B3AD0E770B9B5A113DB255BEE3CEBD02D45167396A0C94`.
Changed Java: `block/{MonolithPillarBlock,entity/DeityStatueBlockEntity,
entity/PrototypeEffigyBlockEntity}`, `content/GatewayContent`, new
`StatueEnergyGameTests` plus `CharmGameTests` / `StatueGameTests` fixtures and
new `StatueEnergyAssetsTest`. Resources/generators: pillar recipe, model, texture,
blockstate/loot, bilingual guide; `generate_statue_energy.py` and gateway generator
delegation. The new generator runs after statues/charms for shared guide text.
Source audit, checklist and current project brain updated; earlier checkpoints
are historical, not current artifact identities.

**Still not a complete mod or full PE parity:** full tolerance-triggered
disruptions, actual multiblock validation/bonuses, PE streams/higher devices and
configuration, tier-four named charm infusions and later dimensional progression
remain required. Tolerance is not silently reset to pretend a disruption happened.
No interim playtest is requested. The complete-development goal stays active;
no client, installation, upload, deletion, publication or commit occurred.

## Historical generic charm / native anvil checkpoint — 2026-10-07

Eight gold ingots around one diamond now craft a real blank Ritual Charm.
In a native anvil, **one** blank charm plus one arrow, gunpowder or redstone
produces Range, Power or Duration respectively, costing **one level only when
the result is taken**. More than one charm in the left slot is rejected instead
of losing surplus items to the native menu's whole-left-slot consumption.
Preview, insufficient XP, reversed/invalid inputs, saved custom components and
actual rename behavior are covered. An unchanged auto-filled name must not
rename the resulting upgrade back to the blank charm's display name.

All seven statues accept a modifier exactly once while inactive. Already active
statues neither replace it nor consume another charm. A blank charm resets it
without consumption. Native adventure/interaction permissions remain enforced;
empty-hand inspection reports amplifier, PE, interval and range in the action
bar. Generic active quanta are ten PE (Power: fifteen), Duration gives sixty
active ticks, Range eleven blocks and a collector search radius of five.
Amplifier/deity survive world saves; matching/foreign factors and malformed tags
are tested, but **named charm infusions are not yet reachable**: source tier four
and 2,000 PE are not bypassed by invented recipes. Mining removes the consumed
modifier, as in the source; the guide explains this before relocation.

A real roof-query defect was found: full skylight at the exposed roof block was
incorrectly accepted by checking one cell above the statue. Real statues now
query their own cell like the source. The regression waits for native lighting
and checks roof removal, without relaxing the production sky rule. Neighboring
manipulators (including vertical distance two), collector isolation and loaded
chunk guards are maintained. Older prototype base behavior remains unchanged.

**This is not full statue-PE parity.** The current shared collector cadence is
still deterministic, not the source's independent random collector/dropped-item
cadence. Dropped items, tolerance, the complete real disruption catalog,
range-amplifier blocks, validated multiblocks and later deity infusions still
need connected implementation. Do not count these missing pieces as complete.

Verified final state: **80 JUnit / 163 flat GameTests / 39 normal-world tests /
14 Python tests**, **439 models / 117 blockstates / zero reference errors**.
`test-artifacts/charm-roof-native-full-20261007.log` passed the first complete
run; `test-artifacts/charm-affinity-name-final-full-20261007.log` passed the
final name/affinity regression and full normal-world repeat, exit zero.
Actual native server ticks charge both hands on Range / Power / Duration by
10 / 15 / 20 PE respectively after 180 elapsed ticks. No manual BE tick loop
is used for this normal-world check. The actual JAR's recipe, four own icons,
permission/cost guards and saved-state bytecode were inspected.

Artifact: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`, **1,808,374 bytes**;
SHA-256 `3A26CC43FBC09258B9AF0C3281081E9C7C428AF9E957E0817BC4117ECCB20BF4`.
Own resources: `tools/generate_charms.py` (run after `generate_statues.py` when
regenerating the shared guide text). No upstream reserved image/audio is read.
No client, installation, publication or external action was performed. Full
development remains active; no interim user test is required. Next: independent
PE transfer cadence, real tolerance/disruptions and validated structure bonuses.

## Historical seven-deity / native monolith checkpoint — 2026-10-07

New Shoggoth-built and fallback monoliths now use **3x3 Monolith Stone**, a
**five-to-twelve-block pillar**, a finite living biomass core and one randomly
chosen, horizontally facing **Cthulhu, Hastur, J'zahar, Azathoth, Nyarlathotep,
Yog-Sothoth or Shub-Niggurath statue**. Each has its own independently authored
one-block cuboid sculpture, stone pixels and block/item model, native placement,
rotation/mirroring and mining drop. Old prototype-era pillars remain untouched
and still participate in spacing checks. No free crafting shortcut is added.

The seven real statue BEs currently **reuse the verified base PE implementation**:
five PE per 120 active ticks, both held books and the existing isolated collector
ring, with saved charging progress. Fourteen held books receive five PE through
actual normal-world native BE ticks. This is a connected survival/model/base-PE
checkpoint, **not the full original statue mechanism**. Dropped-book transfers,
manipulator isolation, charms, tolerance, disruption catalog and multiblock
bonuses remain unfinished. Model/UV reference checks and an offline cuboid
overview are not Minecraft rendering or gameplay acceptance.

The independent previous repetition exposed remaining test prerequisites, not
an all-green repeat: a five-block-radius climbing cage obstructed valid 3x3
sites, and three elapsed world ticks could leave every acid-test parent at
`tickCount=0`. The construction fixture now starts a ready native group on an
open supported field; both native entity-ID phases build without disabling AI.
Helpers wait when already assembled instead of constantly pushing into the
leader. The acid fixture waits for actual native entity ticks, isolates old
players and counts admitted shots even after impact. No combat cooldown,
navigation/climbing attribute or product tick rate was relaxed for the tests.

Verified current state: **78 JUnit, 157 flat GameTests, 38 normal-world tests,
14 Python tests**, plus **435 models / 117 blockstates / 0 reference errors**.
Full integration: `test-artifacts/seven-statue-monolith-full-20261007.log`;
final guide/statue-resistance recheck:
`test-artifacts/seven-statue-monolith-guide-final-flat-20261007.log` and
`test-artifacts/seven-statue-guide-final-world-20261007.log`, all exit zero.
The actual packed monolith/BE/registry bytecode is checked. JAR:
`build/libs/abyssalcraft-port-study-0.1.0-dev.jar`, **1,779,805 bytes**,
SHA-256 `CB2A48F6DF09B47B75A71EE731B2BD2FE5EDEB4848DD6EA8DFBCE0D93C1CF5FA`.

Next connected layer: source base charm (eight gold around a diamond), actual
one-level anvil forging, statue activators and complete PE/tolerance/disruption
rules. Source named-charm infusions require book tier four; do not invent an
early recipe while later books/worlds are not yet connected. Full development
remains active. No original reserved media, client, installation or external
action; no interim user playtest is required.

## Historical Shoggoth ecology / acid / native AI checkpoint — 2026-10-07

Three forms in each of the three reachable habitats now connect native food,
growth/birth, matching death remains and dimensional crystallization. Actual
kills feed the eight-point cycle; vetoed or obstructed replacements spend no
food. Native head/body surfaces share parent health, tracking and ray hits.
Dimensional lairs use the source-informed Lesser/normal/rare Greater draw and
own UV atlases, native structures and bilingual guide text.

Larger forms spit through native throwing physics every 120 ticks; newborns
can ride an accepted spit. Absolute six-point impacts corrode ordinary armor,
while a correctly facing shield takes twelve durability and one chip HP.
The shield direction probe uses a blockable native damage type, because
Minecraft's armor-bypassing tag also transitively bypasses shields. Damage,
impact and block vetoes remain authoritative. Protected blocks and disabled
griefing end flight without terrain deletion. Flight is bounded to 600 ticks;
a trapped/vetoed passenger keeps a stationary pending-release carrier until a
collision-safe native dismount is possible. Clear fallback positions and this
pending state survive save/load. No forced release into blocks or orphan rider.

The autonomous monolith goal had a real phase-starvation bug: Minecraft checks
new goals on alternating entity-ID ticks, incompatible with an even modulo
gate. An elapsed search deadline now handles both phases; assembly advances
on actual ticks. Odd- and even-ID native groups build with real navigation.
The controlled test arena includes a climbing lip and explicitly uses night;
these fixture prerequisites are not product navigation or sunlight changes.
Persistent natural-world audits select unused lairs without resetting, deleting
or replacing old finite biomass nodes; diagnostics tolerate normal exhaustion.

Verified: **76 JUnit, 155 flat GameTests, 37 normal-world tests, 14 Python tests**;
`test-artifacts/shoggoth-acid-ai-phase-native-full-20261007.log`, exit zero.
Fresh native lairs admit **4 Overworld / 3 Abyssal / 2 Dreaded** live, dry,
collision-clear Shoggoths through **440 real ticks**, with actual native remains.
JAR `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`: **1,743,858 bytes**;
SHA-256 `04834255204F24370845E17E2F46B89B569C7648691AF659CA39EB221FE097B0`.
Packed audit: **421 models / 110 blockstates / 0 reference errors**; actual JAR
bytecode confirms blockable-facing acid, persisted safe release and AI deadlines.

Not full-mod completion: the seven deity statues/charms and their PE/disruption
chain, source-shaped monoliths, later world variants/configuration, finished
audio, higher progression/dimensions/bosses and final visual/balance acceptance
remain open. No original reserved media, client, installation or external action.
Full development goal remains active; no interim user playtest is required.

## Historical early Shoggoth lair / biomass checkpoint — 2026-10-05

Three own descending swamp/river lairs now contain native saved Biomass nodes.
The 400-tick timer, 16-block player trigger, shared six-living-Shoggoth cap within
32 blocks, two loaded neighbour hops and five successful-admission exhaustion
are connected. Finalize/join vetoes and blocked terrain never spend the node.
The proper source Monolith Stone is harvestable; biomass never drops, even with
Silk Touch. Real death remains feed the loaded eight-phosphorus recipe. Guide
instructions are bilingual and scroll within the existing energy page.

The normal-world audit caught growing residue embedding its own creature.
Native upward displacement now preserves collision clearance, with a headroom
guard against ceilings. The preserved earlier cave records the failure; a fresh
Monolith-Stone cave at bounds `(359494..359520,53..66,360320..360336)` admits
four living, dry, unembedded Shoggoths through **440 real server ticks**.

Verified: **73 JUnit, 133 flat GameTests, 34 normal-world tests, 14 Python tests**;
`test-artifacts/shoggoth-lair-residue-repeat-full-20261005.log`, exit zero.
JAR `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`: **1,585,735 bytes**;
SHA-256 `8C2736D2CB205389FCD54950B41BBE142BD5D1C18E2A87A7EF0A730EE00E5AB8`.
Packed audit: **411 models / 110 blockstates / 0 reference errors**; actual JAR
bytecode confirms saved biomass and stone registration. One earlier randomized
700-tick autonomous-monolith fixture missed completion; the repeat passed.
Its timing sensitivity is not claimed fixed by a repeat and needs deterministic
regression coverage while the remaining Shoggoth ecology is developed.

Higher forms/food-growth/acid/multipart behavior, dimensional lairs and the seven
named deity statues remain incomplete. No original reserved media, client,
installation or external action. Continue the connected full development scope;
do not request an interim user test. Earlier sections are historical checkpoints.

## Historical dimensional graveyard / Ghoul checkpoint — 2026-10-05

Normal, Shadow, Depths and Dreaded Ghouls now connect eighteen independently
authored graveyard templates in the Overworld, Abyssal Wasteland and Dreadlands.
Source health/melee, baby speed/size, named Depths variants, native death loot,
rare player-only heads, harmful food, immunity/rending tags and five additional
Crystallizer paths are functional. Four heads can be placed or worn. Own skins,
models, pixels and templates; sounds still use Minecraft.

Verified: **71 JUnit, 125 flat GameTests, 33 normal-world tests, 13 Python tests**;
`test-artifacts/dimensional-graves-palette-soil-final-20261005.log`. Naturally
generated graveyards admitted five Shadow, five Depths and four Dreaded Ghouls
through 235 real server ticks. Actual dimensional attacks/rending, loot-to-machine
processing, save/load, vetoes, native grass lifecycle and every packed NBT palette
registry entry are checked. The missing Dread grass/dirt registration was fixed;
old audit chunks were preserved, and new terrain was sampled separately.

JAR: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`, **1,541,059 bytes**;
SHA-256 `2DB3B6B73F52EBB4B48BB0039C6C1422A702C0737C636BD52286D5BC4016A382`.
Packed audit: **407 models / 108 blockstates / 0 reference errors**; actual JAR
bytecode contains all four Ghouls and native Dread soil. No client, installation
or external action. Omothol/Dark Realm variants, rare Spirit Tablet grave loot,
Shoggoth lairs/ecology, the full deity catalogue and later progression remain
development. Continue without requesting an interim user test.

## Historical Overworld graveyard / Ghoul checkpoint — 2026-10-05

Six independently authored native graveyard templates provide four, six or
eight active Stone/Darkstone tombstones, saved night-only timers, loaded-space
spawn checks and a shared five-Ghoul limit. Buried chest omission and ordinary
remains use native processors/loot tables. Normal/Shadow Ghouls, actual death
drops, harmful food, one-time research and two Crystallizer recipes are connected.
Their jointed model, UV pixels, flesh icons and tombstone geometry are own assets;
sounds currently use Minecraft, not the original reserved media.

Verified: **70 JUnit, 118 flat GameTests, 31 normal-world tests, 13 Python tests**;
`test-artifacts/graveyards-player-present-full-20261005.log`. The natural Darklands
graveyard raised **five living Shadow Ghouls from six tombstones through 235
actual server ticks**, with a nearby server player and no manual ticker calls.
Spawn/finalize vetoes, obstruction, native death-drop machine inputs, persistence,
daylight/Peaceful/mob-rule pauses and packed templates are tested. A previous
world fixture lacked a nearby player and correctly triggered ordinary despawning;
product despawn rules were not relaxed to obtain the passing test.

JAR: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`, **1,448,999 bytes**;
SHA-256 `A9B08CDC98C99EA51A38D0195AC8CDBBA352AEBF4A78060048870567C9916E90`.
Packed audit: **383 models / 98 blockstates / 0 reference errors**; actual JAR
bytecode contains the Ghoul and saved tombstone implementations.
No client, installation or external action. Depths/Dreaded/later dimensional
Ghouls/graves, Shoggoth lairs and the named statue progression remain incomplete.
Continue the full development scope without requesting an interim player test.
Earlier sections below are historical checkpoints.

## Historical Darklands family / surface-ruin checkpoint — 2026-10-05

The legacy dense Darklands remains compatible. Plains, Forest, Highland and
Mountains now have distinct native climate/surface/vegetation rules, verified
through actual TerraBlender selection and ordinary terrain. Ten independently
authored houses, memorials, shrines and unused ritual grounds use saved native
structure starts. Glowing bricks use the audited diamond/glowstone/obsidian
recipe, harvest tier and light/strength values. Uniform Darkstone Cobblestone
grounds form and run the real tier-zero ritual; mixed rings are rejected.

Verified: **67 JUnit, 111 flat GameTests, 30 normal-world tests, 11 Python tests**;
`test-artifacts/darklands-ruins-native-pe-full-20261005.log`. The naturally
generated shrine at `(168662,64,168458)` supplies **5 PE through 145 actual
server ticks**, without manually invoking its ticker. Template access, loaded
recipes, admitted ritual output and packed references are checked. Native
jigsaw depth zero omitted the initial piece; depth one fixes generation without
adding rooms. Large searches were bounded and split over ticks, not exempted
from the server watchdog. Old audit chunks were preserved.

JAR: `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`, **1,382,656 bytes**;
SHA-256 `9F1C6BCE611BAF53528880730604301BEF80C4BFB4C6BFA40723BA5D04F9C334`.
Packed audit: **376 models / 96 blockstates / 0 reference errors**.
No client, installation or external action. Graves/Ghouls, Shoggoth lairs,
seven named statues/Idol of Fading and later progression remain development;
the current effigy does not stand in for the missing deity catalogue.
Earlier sections below are historical checkpoints.

## Current renewable wood / Dread ecology checkpoint — 2026-10-04

Darklands now naturally supplies the own 9..11-block, rooted, ascending-branch
Oak family. Normal/hollow logs, all-bark states, native sapling/bonemeal stages,
non-destructive obstruction checks, renewable leaf loot, four-plank/charcoal
routes and door/stairs/slab/fence/gate/button/plate recipes are connected.
Native door placement/halves/loot, button redstone, doubled slabs and fuel/fire
are tested. Own pixels/native shape templates; no upstream media.

The Dreadlands biome now has small/greater/beast forms with native 20/50/100 HP,
6/12/18 melee, climbing, immune tags, actual ranged projectiles and the source
five-to-one cycle. Vetoed admission preserves all participants; accepted fusion
removes exactly five, not a sixth. Saved bounded offspring timers, native death
acceptance, six-damage accepted-hit plague, owner/friendly protection, impact veto,
solid collision and saved projectile lifetime are checked. The larger forms'
actual shard drops feed Dreadium processing; all three are rending carriers.
Own articulated eight-legged model/atlases; Minecraft sounds, not bespoke audio.

Verified: **64 JUnit, 106 flat GameTests, 28 normal-world tests, eight Python tests**;
`test-artifacts/darklands-dread-ecology-full-20261004.log`.
Normal Darklands sample `(96136,64,95656)`: **968 logs / 614 leaves in nine chunks**;
native harvested wood supplies four planks. Normal ecology sample supplies three
admitted shards -> three actual machine ingots. These are existence/integration
samples, not measured biome balance or visual/gameplay acceptance.
JAR `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`: **1,324,369 bytes**; SHA-256
`126BBF03F0B2BE184869867961DC1F3FAE8A9A34A275D18EF5E1DF8FB093C5FA`.
Static audit: **370 models, 93 blockstates, zero missing local references**;
`javap` confirms tree, creature and projectile methods in the actual JAR.
The sporadic older Rending fixture cause is identified: `LivingEntity.getViewYRot`
uses independently ticked head yaw, not `moveTo`'s body yaw. Explicit native
fixture head aiming passed subsequent full runs without changing product targeting.
No client, installation or external action. Four-biome Darklands, structures,
remaining ecology/bosses/dimensions/machines/audio are not complete. Continue the
connected full scope; do not request an interim player test.

## Historical crystal compounds / bags checkpoint — 2026-10-04

The native Carbon ring -> Carbon Cluster -> Obsidian-centred Dense Carbon Cluster
-> Diamond chain and Lapis -> Sulfur -> Sulfur Dust -> Gunpowder chain consume
their real inputs. The Crystallizer now loads 56 recipes, the Transmutator 22;
source-recovery recipes retain their deliberately lossy one-item yields/XP.
Native Crystal Bags have 18/36/54 slots, crystal-only insertion, shift-click,
held-slot/number-key locks and independent immutable native container components.
Both upgrades consume their original bag once and preserve every occupied slot,
stack and custom name. Invalid/oversized contents are rejected without truncation.
The 72-slot Omothol tier remains absent until its real ingredients are reachable.
Guide: 25 discoveries / 26 pages. All bag pixels are independently drawn.

Verified: **60 JUnit, 96 flat GameTests, 26 normal-world tests, eight Python tests**;
`test-artifacts/crystal-bags-final-recheck-20261004.log`. Native player gateway
travel now also proves a filled bag and its serialized contents survive.
JAR `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`: **1,212,116 bytes**; SHA-256
`A234A474FAD812B54E1145156485115DF8B6E0CB7D76E2BE1D667B77707AC6F9`.
Asset audit: **323 models, 81 blockstates, zero missing local references**.
An older Rending ray fixture failed intermittently in two intermediate runs;
chunk ownership and richer geometric diagnostics were added, but the cause is
not yet established. The final complete run passed; repeat stability is not
claimed. No client, installation or external action. Next connected development:
renewable Darklands Oak, then the missing dimensional/progression systems.

## Historical Crystallizer / armour checkpoint — 2026-10-04

The source-reachable Crystallizer uses six Dreadstone Bricks, two Dreadium Blocks
and a furnace. Its four native slots process 55 loaded recipes in 200 ticks with
two atomic, component-safe outputs, exact water-potion matching, returned buckets
and bottles, saved reload fingerprints/counters/XP, a 40-slot menu and sided
automation. All 22 present crystal families have three fractions and a cluster
block, 132 reversible nine-to-one recipes and independently drawn pixels. The
shared processing engine preserves the previous Transmutator save keys and its
existing tests. Absent optional metals, later flesh, methane and materialization
are not claimed implemented. Guide: 24 discoveries / 25 pages.

Four Dreadium armour pieces now have native ingot recipes/anvil repair, multiplier
40, defense 3/8/6/3, enchantability 15 and toughness one. Chest/boots refresh finite
Resistance/Speed only while equipped; no invented water breathing or plague
immunity. Own armour UV layers are packed. A deliberately failing native test
exposed missing enchantment item tags on previous gear. All 27 pieces/tools across
Abyssalnite, Refined Coralium and Dreadium now accept the appropriate native
enchantments and a real enchanted-book preview on a Survival player's anvil.

Verified: **55 JUnit, 90 flat GameTests, 26 normal-world integration tests**;
`test-artifacts/crystallizer-dreadium-equipment-full-20261004.log`. The normal
dimension audit uses actual block-entity ticks, not a manual processing loop,
and produces 12 Hydrogen Crystals / six Oxygen Shards / one returned bucket.
The preceding Crystallizer checkpoint also passed **eight Python tests**.
JAR `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`: **1,166,185 bytes**; SHA-256
`B0CDE72ECFD364716973B0B80869429D7644AF6D40286C5D30873094665466D0`.
Static audit: **317 models, 81 blockstates, zero missing local references**.
No client/installation/external action; full scope remains active and unaccepted
in gameplay. Next connected work: source Carbon-cluster/Sulfur/transmutation
recipes, then Materializer. Previous checkpoints below are historical.

## Historical Dreadguard / Dreadium checkpoint — 2026-10-04

The native Dreadlands biome now has rare source-weight-eight Dreadguards:
60 health, 20 armour/four toughness, ten-damage plague melee, three-block size,
fire/underwater/plague immunity and Dread rending. A saved/synchronized 60-tick
stationary spray follows a 200-tick cooldown. Server ray/solid clipping, positive
distance falloff, armour-bypassing native magic damage and accepted-hit disease
are tested; friendly Dread carriers are excluded from spray damage. Original
accidental negative/double damage is not copied. Own articulated 128x128 model
atlas includes knees, elbows and spray jaw; sounds still reuse Minecraft.

Native deaths actually supply 0..2 Dreaded Shards, not fabricated ore drops.
The eighth loaded Transmutator recipe yields one Dreadium Ingot/0.2 XP per shard.
Nine-to-one block/nugget conversions, level-four Dreadstone Bricks and all five
level-six tools (2,300 durability/14 speed) are loaded, craftable and anvil-repairable.
Lower tiers cannot mine Dreadium storage; the new pickaxe can. Native right-click
also opens the real 39-slot Transmutator menu. Guide: 23 discoveries/24 pages.

Verified: **50 JUnit, 81 flat GameTests, 25 normal-world integration tests and
eight Python tests**; `test-artifacts/dreadguard-dreadium-full-20261004.log`.
The normal audit obtains five admitted shards from actual deaths, processes
five native ingots/exactly one accumulated XP and proves carrier rending.
JAR `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`: **943,389 bytes**, SHA-256
`D844104F244140EF3A5862D2875FEE5AF0AEB089A25AE8ECF1EF60C21F57D23F`.
Static audit: **200 models, 58 blockstates, zero missing local references**;
`javap` confirms spray/persistence/tool classes in the actual artifact.
No client or installation. Full Dreadium armour, bespoke sounds/seasonal variants,
higher creatures and Crystallizer remain development work, not a full original
parity claim. Previous identities below are historical.

## Current processing checkpoint — 2026-10-04

The native Abyssal Plateau now supplies Coralium Stone: pickaxe mining returns
cobblestone, two ordinary furnace steps yield stone and bricks. A real crafting
recipe consumes five bricks/two Refined Coralium Blocks, returns the fluid bucket
and preserves one damaged catalyst copy. The native three-slot Transmutator has
seven loaded reciprocal stone recipes, 200-tick processing, source-informed fuel,
saved counters/inventory, component-safe outputs, a synchronized menu, shift-click,
sided handlers/hoppers, fractional XP and one-time native inventory drops.
Burning a catalyst consumes it; crafting remains ten-use. A failing native test
exposed the erroneous reusable fuel remainder before its fix. A separate old
ray fixture now explicitly owns/restores its corridor after an observed stray
terrain collision, without weakening product targeting. The field guide has
22 discoveries/23 pages; both languages explain actual fuel and automation.

Verified: **47 JUnit, 78 flat GameTests, 24 normal-world integration tests and
eight Python tests**; `test-artifacts/transmutator-full-recheck-20261004.log`.
Native plateau sample: `(33104,64,30964)`, 1,151 surface stones in nine chunks;
first harvest `(33088,96,30944)` feeds the actual cobble -> stone -> brick recipes.
JAR `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`: **893,358 bytes**, SHA-256
`53AA3230234C9B52D5B73359EEB70BBAB2D32A9D78416186FBFA30B3DB45F9CA`.
Static audit: **187 models, 56 blockstates, zero missing local references**.
No client or installation. This is not the complete original processing system:
Dreadium/Crystallizer, full recipes, ecology and bespoke audio remain development
work. Previous checkpoint identities below are historical.

## Current Stronghold / Dreadlands progression checkpoint — 2026-10-04

The first Powerstone comes from real native concentric Wasteland Stronghold
placement and seven own Jigsaw templates, not a creative/chest shortcut. A normal
world sample has 33 pieces, 327 dry walkable cells and chest loot. Removing the
Powerstone marks that structure explored; merely throwing its tracker does not.

The 10,000-PE tier-one infusion consumes a Transmutation Gem, Powerstone and Eye
of the Abyss to return the higher Gateway Key. Native paired portals connect
Wasteland and Dreadlands, never directly Overworld and Dreadlands. Saved anchors,
collision, cooldown and exact return are tested. Six tier-four stones/ores,
native varied terrain, three natural deposits and the first 30-HP/six-damage
Dreadling with non-milk-curable plague are reachable. This is entry ecology,
not all original Dreadlands biomes, disease conversions or bosses.

Dread rending (100 energy per essence), eight fragments per skin, three skins
plus the Wasteland book, a component-preserving 20,000-PE book, Dreadstone
Cobblestone ritual grounds and the 2,000-PE charged staff upgrade form the next
chain. Three-damage drains retain threshold overflow and every stored channel.
The later tier-two 5,000-PE/eight-essence Powerstone infusion cannot replace the
first Stronghold visit. Own book/staff/ritual/Dreadling media and English/German
guide entries are packed: 21 discoveries and 22 pages. Visual/balance acceptance
is not claimed.

A reproduced full-inventory rending duplication is fixed: native drop admission,
not immediate tracking lookup, proves delivery. Join-veto still retains energy.
The failing-before/passing-after world test remains. The separate audit exit
probe returned exit 1 only after full world saving, without the old shutdown hang.

Verified: **44 JUnit, 72 flat GameTests, 23 normal-world integration tests and
eight Python tests**; `test-artifacts/dreadlands-progression-full-20261004.log`.
JAR `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`: **840,736 bytes**, SHA-256
`49C261AA7F83D9F78CA7AFFC5C65DD745FFA4568968A1219295BD7C6A312EC95`.
Static audit: **179 models, 53 blockstates, zero missing local references**.
No client, installation, publication or Git action. Full scope remains open in
`DEVELOPMENT-CHECKLIST.md`; next: real processing/material progression.

## Earlier connected boss checkpoint — 2026-10-03

The tier-one Wasteland chain now reaches Asorah: seven unordered offerings (four
gold ingots, a Transmutation Gem, a Coralium Bucket, an enchanting table), an empty
central altar, a tier-one book and 1,000 PE. The native entity outcome has no fake
item result. Solid obstructions, Peaceful, native join/finalize vetoes or a book
changed by a spawn hook abort without consuming PE or ingredients.

Asorah has 400 HP, a native tracked boss bar, five independently hit-testable
parts, player/explosion damage rules, reduced non-head hits, bounded high damage,
local sweep/approach flight, native head contact and wing knockback. Nearby
30-HP Spectral Dragons transfer their own health; exhausted/dead/distant donors
cannot generate free healing. Their rare Wasteland biome entry and dimension-only
spawn predicate are registered. Own articulated block-grid model/UV atlases and
Eye icon are generated without original media; sounds currently reuse Minecraft's
licensed dragon events, not finished bespoke audio or visual acceptance.

The 200-tick boss death saves its pending/paid reward state, attacker and damage
type. It emits one Eye through native loot/drop hooks, two of each listed Coralium
reward and 5,000 native XP, then removes the boss. XP consolidates the original
ten 500-XP waves; fixed material rewards avoid the original per-orb loop bug.
Both items and XP respect doMobLoot. Reload at tick 100, delayed payout, a paid
save and disabled loot are tested. Eye collection records one player discovery.
The field guide now has 18 pages and 17 discoveries.

Verified checkpoint: **36 JUnit, 63 flat GameTests, 15 normal-world integration
tests, five Python auditor tests**. Native Wasteland ticks additionally prove the
accepted boss is tracked, moves around its actual summon site and registers its
parts. Immediate entity queries cannot prove spawn failure in a not-yet-tracked
chunk; the summon fixture captures accepted entities and cleans them explicitly.
World audits now pass `--nogui`; failure exit waits for the server thread before
starting JVM shutdown. A fresh nonzero-exit probe is still outstanding.

JAR: 693,414 bytes; SHA-256
`C34F7AD338C036424369A77E3E8A2E38AF118E6E659F9EBFF8AEC69697C5B464`.
Asset audit: 136 models/38 blockstates, zero missing local references.
An earlier flat run had one intermittent staff-ray fixture failure; the two
subsequent full runs passed. Diagnostic context is retained; absolute flake freedom
is not claimed. No client, installation, upload or publication.
The boss-to-Dreadlands gateway, complete Wasteland structures/ecology and later
dimensions remain open. Next connected development: Stronghold/Powerstone access.

## Earlier book/ritual checkpoint — 2026-10-03

The reachable book chain now continues through the Staff of Rending, Wasteland
Essence, eight plagued flesh per skin and three skins plus the base Necronomicon
to the 10,000-PE Wasteland book. The native shaped upgrade serializer retains PE,
custom names and component data without returning a duplicate base book; native
recipe-network serialization and mirrored patterns are tested. All existing PE
devices and rituals recognize each book's actual capacity, including offhand use.
The server-owned 50-block staff ray checks walls, allowed target types, successful
damage and cooldown; cancelled inventory/drop delivery retains energy.

The Wasteland book forms tier-one ritual grounds from Abyssal Cobblestone only in
the Wasteland. The source-informed unordered 1,000-PE infusion consumes a base
staff, one Shadow Gem, four Abyssal Stone and three plagued flesh, producing the
charged tier-one staff after 20 ticks. Its two-energy drains preserve threshold
overflow; staff names and both energy channels survive the infusion and reload.
Wrong book, dimension, mixed ground materials or interrupted requirements cannot
consume the ritual inputs. New pixel textures/models are independently drawn by
`generate_wasteland_progression.py` using only this project's own base assets.

Latest verified suite: **33 JUnit, 59 flat GameTests, 11 normal-world integration
tests and five Python auditor tests**; 134 models/38 blockstates with zero missing
local references. The field guide has 17 pages and 16 player-scoped discoveries.
Packaged checkpoint: 637,401 bytes, SHA-256
`BAC01D1D1363414860A9D5243B602AE71EF2934A77CF616244DEE53AEE5BDCB3`.
The audit's failed-exit handler now avoids a server-thread/shutdown-hook deadlock.
Shoggoth helpers reserve movement while assisting instead of letting random stroll
overwrite the builder's navigation. One subsequent full flat run passed; this is
not a claim of absolute flake freedom. No client, installation or publication.
Full original-inspired scope remains open in `DEVELOPMENT-CHECKLIST.md`.

## Earlier Wasteland mining checkpoint — 2026-10-02

The Gateway/sacrifice chain now connects to actual Wasteland mining: seven
dimension ores, source-informed random vein-size ranges, mining-tier restrictions,
refined Coralium ingots/nuggets/storage, five tools and four armor pieces with
actual anvil repair and finite equipped effects. A normal 16-chunk sample contained
58 liquified ore blocks, 52 pearlescent, 411 Coralium, 265 iron, 56 gold, 39 diamond
and 291 Nitre blocks. Placement centers follow source height ranges; native veins
can extend below/above their center height. The Coralium attempt count uses a
uniform 8–15 modern approximation, not the original correlated random draws.
Overworld Nitre also loads through a biome modifier; its later processing remains
unfinished. Own textures come from `generate_wasteland_minerals.py`.

A first Abyssal Zombie has its own humanoid skin, 25-health / 6-damage stats,
underwater/plague immunity, persisted half-size babies and original-style hostile
targets. Successful Wasteland melee infects; defeated zombies/villagers can convert
on Normal/Hard, only after a replacement spawn succeeds. Player death/inventory
handling is never suppressed or discarded. Player-only rare Coralium Chunks refine
into two ingots; plagued flesh has verified food hazards. Vanilla-native humanoid
animation, armor rendering and zombie sounds are used; bespoke sounds, special
config/Halloween behavior and complete creature ecology remain unfinished.

Eight normal-dedicated-world tests verify gateway ritual completion/cancellation,
travel vetoes, entity/player roundtrips, inventory/PE/research/respawn, serialized
anchor pairing, broad-entity landing support, natural ores and disease transmission.
The landing platform is 5×5 and checks the whole footprint before reusing an anchor.
Wasteland structures, further ecology, bosses and a full process-restart return test
are still open. This is not a complete dimension or finished mod.

`runGameTestServer --offline` covers 53 flat-world GameTests; 30 JUnit and five
Python auditor tests also pass. Minecraft's stock GameTestServer excludes custom
datapack dimensions, so its count does **not** include the eight world tests.
Use `runWorldAuditServer -PgatewayAudit=true --offline` for those tests. This
explicit opt-in runs only on loopback in `runs/world-audit/gateway-audit-20261002`,
tracks real GameTest results and shuts the headless server down after completion.
Production loading ignores this development-only flag. No client or installation
is required. The field guide has fifteen pages and fourteen discoveries.

Packaged checkpoint: 583,728 bytes, SHA-256
`BA317B3D0F80D3EC964B29C81F4237AF870AB1F57BEC6642F874E0DD67841BBD`;
125 models, 36 blockstates, zero missing local asset references.
Gameplay, balance, renderer and multiplayer acceptance remain unverified.

## New early mineral and infusion chains — 2026-10-02

Abyssalnite now generates in stone within the actual Darklands at source-informed Y 4–31, with 3–8 attempts and 1–3-block veins. Iron-tier mining yields the ore, which smelts into an ingot (200 ticks, 3 XP; blasting 100 ticks). Standard recipes provide all five tools and four armor pieces, nuggets/storage block with reverse recipes, and actual anvil repair. The 1,261-use / 10-speed tool tier and armor values follow the inspected original. The equipped helmet refreshes finite Water Breathing and the boots finite Speed; items sitting in inventory do not activate effects. Own item/block/armor textures come from `tools/generate_abyssalnite_assets.py`. A normal fresh-world region contained actual ore, but the small sample does not establish ore abundance or balance.

Coralium Ore is added to shallow oceans (12 attempts, Y 0–39), deep oceans (12 attempts, Y -20–19) and swamps (6 attempts, Y 0–39), with source-size eight veins. The old biome categories are mapped to explicit modern biome tags that datapacks can extend. Iron-tier mining yields 1–3 gems; Fortune/Silk Touch use native loot handling. Clusters of 2–9 gems have lossless unpacking and source-informed combination recipes. Six stone plus three triple clusters, or eight stone around a nine-gem cluster, craft Coralium-Infused Stone; smelting produces a Coralium Pearl. Its direct mining route requires diamond-tier tools.

Place one pearl on the central altar and put 2 diamonds, 2 ender pearls and 4 blaze powder on its eight pedestals, in any order. A book holding 300 PE starts a 20-tick infusion. Its input, active definition fingerprint and progress persist; invalidated definitions/ingredients cancel without consuming PE or pedestal offerings. The finished Transmutation Gem remains on the altar and is collected with an empty hand. The central item is synchronized to an independently implemented renderer and drops when the altar is broken. Four Shadow Gems around the catalyst craft an Oblivion Shard. The catalyst loses one use per actual crafting recipe and disappears after the tenth use; this explicitly bounds the original `ItemCorb` remainder mechanic instead of retaining a zero-durability extra use. This is not yet the complete Catalyst/Gateway progression.

The field guide contains eleven pages and ten early, player-scoped discoveries. Body content has scrolling and a scissor viewport separate from its buttons; no client render acceptance is claimed. `tools/generate_coralium_assets.py` draws all new mineral icons/textures independently. The server suite verifies both infusion completion and cancellation, actual block interactions, central-item save/load, recipe/biome data, all equipment recipes and the finite catalyst. A fresh normal-world ocean region (`runs/world-audit/coralium-audit-20261002/region/r.0.-1.mca`, seed 123456789) contained 3,281 Coralium Ore blocks in 460 stored chunks, at occupied Y -3..39. This is an actual terrain sample, not a survival or frequency guarantee.

The full agreed development scope is tracked in `DEVELOPMENT-CHECKLIST.md`. The old narrow beta gate in `BETA-GATE.md` remains a historical technical slice, not a reason to stop development for an intermediate user test. No client launch, installation or publication is authorized.

The field guide's Shadow Materials page now unlocks separately for each player when they pick up a Shadow Fragment, Shard or Gem, or craft a Shard or Gem. Opening the book also recognizes samples already in that player's inventory from an older save. This sixth early discovery persists. Entering the actual Darklands biome records a seventh player-scoped discovery; fallback soil patches do not count. This is not the original full research system.

This is an early NeoForge 1.21.1 port study, not an official AbyssalCraft release and not a playable replacement for the original mod. It currently registers Darkstone, a craftable Necronomicon, a prototype effigy, a first-tier Energy Collector/Relay/Container chain, a first-pass Lesser Shoggoth, and a first-pass Shadow Creature. Shoggoths spawn rarely in groups of four in dark forests and swamps; when undisturbed, they can build a 3x3 monolith after 1,500 active ticks and a 40-tick uninterrupted assembly. Construction waits for a clear site so it does not enclose living entities, and site searches are throttled. Built monoliths need at least 14 blocks of horizontal separation; all four participants restart their cooldown after a successful build. Their ooze slows other entities, stacks up to eight layers, and loses one layer every 20–40 seconds in bright light while its chunk is ticking. Monolith building and ooze placement respect the `mobGriefing` gamerule. Rare abandoned monoliths in those biomes still provide a **temporary** survival route to the effigy until normal gameplay confirms the Shoggoth path. Under open sky, the effigy charges every nearby player's held Necronomicon by 5 PE every 120 active server ticks, including both hands; charging progress is saved. The book stores and displays potential energy under the original `PotEnergy` tag. Right-clicking the book in air requests a server-authoritative, read-only eleven-page field guide. Monolith energy, a formed ritual ground, a studied Collector/Relay and a completed Basic Scroll add separate, persistent discoveries to the player; relevant later pages reveal only after their prerequisites. This is an early discovery layer, **not** the original research tree or research table.

The new collector stores up to 1,000 PE. Place it in the horizontal ring three blocks away from an open-sky effigy, at the same height, without another collector touching it. It receives 5 PE per 120 active effigy ticks even while no player is present; it does not force-load chunks. Use a Necronomicon on it to take up to 100 PE, or sneak-use to return up to 100 PE. Stored PE survives saves and remains in the mined block item, where the tooltip shows it. Its recipe now uses eight Darkstone and a Shadow Gem. The Shadow Creature drops 1–3 Shadow Fragments; four Fragments craft a Shard and four Shards craft a Gem. Both steps reverse at a crafting table, as in the upstream recipes. A first independent Darklands Overworld biome now registers through TerraBlender 4.1.0.8 (required at runtime): rare humid inland pockets with Shadowed Soil surface rules, dark-forest vegetation, vanilla caves/ores, Darkstone veins at Y 0–64, and early creature/monolith access. This follows the original early access route: seven vein attempts per biome chunk, replacing only stone, while the prior monolith source remains. Mining Darkstone normally now drops Darkstone Cobblestone; Silk Touch keeps the block, and smelting the cobblestone restores Darkstone in 200 ticks for 0.1 XP. A 2x2 grid of Darkstone now crafts four Darkstone Bricks; furnace-smelting one Brick makes a Cracked Darkstone Brick in 200 ticks for 0.1 XP. The seven Darkstone blocks are pickaxe-mineable without a stone-tier requirement. Slabs, stairs and chiseled bricks now use the source-aligned crafting patterns; five independently drawn 16x16 textures cover the family; their final look still needs in-game visual review. Small patches of independently textured Shadowed Soil remain in dark forests and swamps as a **temporary** fallback. Natural Shadow Creature spawns require that soil underneath. This first biome is not the original complete Darklands or its dimension. Its actual natural distribution, surface appearance, creature behavior, item silhouettes and balance still need in-game review.

The Relay and Container complete a first functional PE transport chain. Place the horizontally facing Relay directly next to a Collector, with the Collector behind its front face, and a Container up to four blocks ahead across a clear path. While unpowered, it pulls 10 PE from the Collector every 20 active ticks and pushes up to 20 PE to the Container every 40 active ticks. The Relay holds 500 PE; the Container holds 10,000 PE. A redstone signal pauses the Relay. Stored PE survives saves and item drops in all three devices; each can exchange up to 100 PE with the book by right-click or sneak-right-click. Using the Container without a held book opens a two-slot menu: the left slot drains a Necronomicon into the reservoir and the right slot charges one, up to 20 PE per active tick. Books remain saved in the block and drop when it is removed. All three use independently authored 16x16 textures and distinct three-dimensional block/hotbar silhouettes: a compact framed Collector, a directional upright Relay and an open four-post Container. The Relay's selection shape follows its facing. These revised forms and the new menu are not visually accepted in-game. The Relay recipe now uses two Shadow Shards and one Gem; the Container recipe and horizontal-only transfer rule remain **temporary**. Higher-tier PE networks and other PE-capable items are absent.

A first experimental basic-scroll ritual is present. Build a ground from nine cobblestone blocks: one center and eight at offsets `(-3,0,0)`, `(0,0,-3)`, `(3,0,0)`, `(0,0,3)`, `(-2,0,2)`, `(-2,0,-2)`, `(2,0,2)`, `(2,0,-2)`. The center and all eight outer blocks must be in the same chunk; keep the adjacent space around each outer block clear. Use the Necronomicon on the center block to form an altar and eight pedestals, without consuming the book or PE. This formation follows the original first-tier cobblestone layout. Put one vanilla book on any four pedestals and leave the others empty, as allowed by the original unordered offering match. Hold a Necronomicon with at least 100 PE and use it on the altar. The server completes the ritual after 20 active ticks if the player, book, and offerings remain available. Only on success does it consume 100 PE and four books and spawn one basic scroll. The offerings render on the pedestals, and the ritual's timer and offerings are saved. Ritual inputs, output, energy cost and duration now load from `data/<namespace>/rituals/*.json`; a definition removed or changed during an active ritual cancels it without charging PE. Pre-datapack Basic Scroll saves resume only when the loaded definition still matches their original fingerprint. This is still only an introductory slice, **not** the original research/ritual system. A second, central-input Coralium infusion is implemented as described below; dimensions, research table and higher rituals remain absent.

The upstream source is kept separately at `../upstream` for inspection. This is an independently developed modern implementation rather than a direct fork or an official port. The Java energy, ritual and Shadow Creature progression are adapted from Shinoow's AbyssalCraft source under LGPL-3.0; the license is included in the build. The prototype contains no upstream textures, models, or sounds. Its first-pass Shoggoth model has an independently generated 128x128 skin, with its full-size source kept in `art-concepts/`. The PE-device textures and block/item models are authored locally by `tools/paint_pe_textures.py` and `tools/build_pe_models.py`; their generated concept reference under `art-concepts/` is **not** packaged. The Necronomicon has an independently authored thin 3D item model and four 16x16 textures from `tools/paint_necronomicon.py` and `tools/build_necronomicon_model.py`. Five independently authored Darkstone block textures are generated by `tools/paint_darkstone_textures.py`; the Shadow Creature concept under `art-concepts/` is also **not** packaged; its own 64x64 UV skin, three 16x16 material icons, and Shadowed Soil textures come from `tools/paint_shadow_progression.py`. Some other temporary models still use vanilla Minecraft textures. The older generated Necronomicon icon study remains under `art-concepts/` **outside the packaged resources**; its native size caused a Minecraft atlas mipmap warning and that image is not a game asset. Do not distribute the upstream assets without separate permission. See `LICENSE` and `../upstream/LICENSE_ASSETS.md`.

Build with `gradlew.bat test build`; run the server-side integration tests with `gradlew.bat runGameTestServer`. TerraBlender is resolved automatically for development runs but is a separate required mod alongside this JAR for normal installations. Only after explicit approval, the isolated client can use `Start-PE-Playtest.cmd` and follow `PE-PLAYTEST.md`; do not start a second copy against the same game directory. The spacing rule checks loaded chunks for the effigies of complete Darkstone pillars; a standalone PE effigy does not reserve a monolith site, and missing neighboring chunks prevent construction rather than forcing them to load. Server-side GameTests cover autonomous four-Shoggoth construction, collector spacing/charge, relay timing, redstone pause, clear-path checks, facing-dependent Relay selection, PE conservation, mined-item restoration, container book transfer, inventory drops, save persistence, player-isolated discoveries, Darklands biome/region data, Shadowed Soil feature registration/placement and substrate-restricted Shadow Creature spawns, material loot and crafting. The current source passed 23 JUnit and 37 GameTests; the tests also check slab/stair/chiseled recipes, stair shapes, double-slab drops, block assets and pickaxe tags. A separate loopback-only headless normal-world audit confirmed Darklands soil, Darkstone ore and open-topped PE monoliths in one seeded world; `BETA-GATE.md` records its limits. `tools/audit_jar_assets.py` validates the packaged local model/texture references. Offline texture and approximate model previews are under `test-artifacts/`; these are not Minecraft renderer screenshots. The revised book first-person orientation, server-opened field guide, page layout, new Shadow Creature and container menu have not been checked in the client. The creatures still need art and motion refinement, and ordinary survival play has not been validated. The local dev save `runs/client/saves/New World` contains the test platform and may open in Spectator mode. Automated tests do not establish Noah's gameplay or visual acceptance. No installer or public release is prepared.
