# Building this local beta candidate

This is an unofficial, incomplete NeoForge 1.21.1 port study. Read
`SOURCE-NOTICE.md`, `LICENSE`, `LICENSE-GPL-3.0.txt` and `BETA-GATE.md` first.
The mod ID remains `abyssalport`; existing development saves are not renamed.

## Requirements

- Java 21, available through `JAVA_HOME` or Gradle toolchain discovery.
- The supplied Gradle wrapper; do not replace its pinned distribution.
- Network access for a fresh dependency cache. `--offline` works only with
  the necessary dependencies already cached.
- Minecraft 1.21.1, NeoForge 21.1.234, ModDev 2.0.141 and TerraBlender 4.1.0.8
  are pinned in `build.gradle`. TerraBlender is not bundled into our binary.
- Python 3.10 and Pillow for the supplied optional pixel/model generators.

## Build and local checks

Windows PowerShell, from the extracted source-build directory:

```powershell
.\gradlew.bat test build runGameTestServer --console=plain
.\gradlew.bat betaSourceBundle --console=plain
py -3.10 -m unittest discover -s tools -p test_*.py
py -3.10 tools\audit_jar_assets.py build\libs\abyssalcraft-port-study-0.1.0-dev.jar
```

The Flat GameTest server is supplied by Gradle and does not start a graphics
client. Normal-world checks additionally use the isolated headless server;
the source bundle includes a minimal loopback-only fixture, not saved worlds.
In a fresh extracted checkout, create `runs/world-audit/` and copy
`test-fixtures/world-audit/server.properties` there as `server.properties`.
Do not overwrite an existing configuration or reuse a valuable world.
The Minecraft server EULA must be accepted separately before starting; the
source bundle does not silently accept it or supply a preaccepted EULA file.

```powershell
.\gradlew.bat runWorldAuditServer -PgatewayAudit=true --console=plain
```

### Real two-process persistence / cold return probe

Use a **new** isolated world name for each complete attempt. Run prepare to
completion, then verify on that same name; these are separate server JVMs:

```powershell
.\gradlew.bat runWorldAuditServer -PrestartAuditPhase=prepare -PrestartAuditWorld=gateway-audit-restart-mycheck --console=plain
.\gradlew.bat runWorldAuditServer -PrestartAuditPhase=verify -PrestartAuditWorld=gateway-audit-restart-mycheck --console=plain
```

Prepare places fixture platforms/paired gateways and a live machine in this
disposable world, joins an embedded test player, performs native outbound travel
and stops after saving an in-flight transaction. Verify requires a different
process and an initially unloaded saved return chunk, reloads the native player
file, returns through the actual portal processor and resumes pack/unpack using
normal tickers. Both phases halt normally and save; neither starts a client,
uses a real account nor installs a JAR into an existing instance.

The existing WorldAuditTestRunner gates these probes to a development runtime,
loopback and `gateway-audit-restart-*`. Prepare refuses existing audit state;
invalid names/phases fail before startup. Repeating prepare on a completed world
is an intentional negative test expected to exit nonzero, not a successful run.

Outputs:

- `build/libs/abyssalcraft-port-study-0.1.0-dev.jar`: local binary candidate.
- `build/libs/abyssalcraft-port-study-0.1.0-dev-sources.jar`: source JAR.
- `build/distributions/abyssalcraft-port-study-0.1.0-dev-source-build.zip`:
  matching source, tests, wrapper, build files, generated resources and generators.

After verifying those outputs, retain an exact local checkpoint with:

```powershell
.\gradlew.bat stageBetaCandidate -PbetaCandidateName=my-verified-checkpoint --console=plain
```

It stages binary/source artifacts and their current test/license documents under
`test-artifacts/beta-candidates/my-verified-checkpoint/`, not an installed instance.
An existing checkpoint is refused instead of overwritten. This task is a local
copy step, not a substitute for the preceding tests, a version bump or publishing.

Resources are supplied already generated; running every generator is not
necessary to compile. Generators author our replacement media and reference
Minecraft resources rather than importing reserved upstream media. The bundle
intentionally excludes worlds, caches, logs, private project brains and the
upstream reference tree. ZIP ordering and entry timestamps are normalized;
this does not assert bit-identical Java compilation across all environments.

Do not start a client or deploy an artifact as part of these build checks.
Publishing requires separate approval and a matching publicly available source
bundle. A built artifact is not an in-game-accepted beta.
