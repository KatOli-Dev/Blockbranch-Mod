# Development, build and tests

Use Java 25 and the checked-in Gradle wrapper from the repository root. Versions
are pinned in [gradle.properties](../gradle.properties). Dependencies and Minecraft
assets must already be cached for offline runs; `--offline` does not provision
anything. Python is optional, never needed for Gradle build/check, IDE import,
ordinary game launches or packaged gameplay. Approved assets are checked in.

## Build and repository layout

```sh
./gradlew build
./gradlew checkDocumentation checkProductionArchives checkDevelopmentClasspaths
./gradlew verifyWildlifeItemResources verifyNativePickaxeResources
./gradlew tasks --all
```

`build` compiles production/development code, runs the canonical analytic JavaExec
checks through `test`/`check`, and creates binary/source JARs in `build/libs/`.
There are no framework-discovered tests. Historical/manual diagnostics stay
explicitly callable rather than being adopted by task-name prefix.

| Location | Purpose |
| --- | --- |
| `src/main`, `src/client` | Production code and assets |
| `src/test/java` | Analytic checks without booting Minecraft |
| `src/integrationTest` | Development-only Fabric mod and server fixtures |
| `src/integrationTestClient` | Client probes layered on shared integration code |
| `gradle/testing` | Task definitions, properties, source sets and guards |
| `scripts/assets`, `scripts/structures` | Opt-in asset preparation |
| `scripts/verification`, `scripts/performance` | Fixture inspection and measurements |
| `scripts/prototypes/underground` | Rejected experiments outside compiled source sets |
| `build/` | Ignored worlds, logs, reports, screenshots and profiles |

Ordinary `runServer`/`runClient` use production classpaths; QA Loom runs use the
integration mod. `checkProductionArchives` rejects QA types, test mixins and
fixture helpers in both archives. It also checks packaged credits and client-only
manifest entries. `checkDevelopmentClasspaths` keeps QA outputs off ordinary
server/client runtime classpaths. Both guards run in `check`.

Keep only this guide and [SAVE_COMPATIBILITY.md](SAVE_COMPATIBILITY.md) under
`docs/`, each at most 500 lines. `checkDocumentation` enforces the limit for the
repository's Markdown references and checks their local links. Put stable
implementation rationale beside its code, not in a growing qualification journal.
Do not regenerate approved assets, remove test inputs, or change licenses as cleanup.

## Native checks

Native runs are opt-in. Require the fixture's explicit result-file **PASS**, not
just process exit. Use unique fresh directories for changed definitions, then
repeat with the same directory for persistence checks. Preserve existing worlds;
upgrade checks use copies of closed, authentic baseline worlds. A missing historic
world directory creates today's preset, not the old definition being tested.
Run native/performance workloads serially to avoid port and CPU contention.
Provision server EULA/configuration with the runner's authorization and choose a
loopback port before launch. An empty directory is not server setup.

```sh
./gradlew -PcolumnSmokeDirectory=build/column-smoke-NEW runColumnSmoke
./gradlew -PcolumnSmokeDirectory=build/column-smoke-NEW runColumnSmoke
./gradlew runHabitatCodec
./gradlew -PfocusHabitat=profile_encoding -PrealisticSmokeDirectory=build/profile-NEW runRealisticSmoke
./gradlew runColumnLifecycle runLoadingLights
./gradlew runOreReference runOrePerformance
```

Column smoke uses loopback port 25579. Focused habitat/seed options are defined
in [realistic.gradle](../gradle/testing/realistic.gradle). `runOreNative` accepts
`-PoreNativeDirectory=build/<case>` for fresh/reopen checks. For loading lights,
use `-PloadingLightsDirectory=build/<case>`; the late-emitter regression covers
initial generation, late features and edits to already FULL chunks.
`-Dblockbranch.lightQueueDiagnostic=true` reports missing layers without hiding
native failures. A focused PASS does not establish every habitat or codec path.

### Metallurgy and resource packs

```sh
./gradlew --offline --no-daemon -x downloadAssets compileIntegrationTestClientJava \
  verifyBismuthResources verifyNativePickaxeResources \
  -PmetallurgyDirectory=build/metallurgy-NEW -PmetallurgyRealistic \
  -PmetallurgyExpectedOre=15 runMetallurgySmoke
```

Repeat for full-relaunch item/profile/edit retention. For old worlds, select a
copied closed fixture and its actual saved ore definition, not 15. The SHA-verified
`metallurgy-ore14-catalog19` fixture freezes the prior reader independently.
Individual `verify*Resources` tasks and frozen readers live in
[metallurgy.gradle](../gradle/testing/metallurgy.gradle) and the material-specific
scripts under [gradle/testing](../gradle/testing).

Run `runMetallurgyViewServer` and `runMetallurgyViewClient` as a connected pair,
with `-PbismuthView -PmetallurgyViewDirectory=build/metallurgy-view-NEW` on both.
Add `-PpickaxeArtView` on both for native gear/sprite comparisons, or
`-PmetallurgyViewPack` on the client for host overrides. Wait for server `Done`;
it stops after client disconnect. The gallery requires survival mining controls
and final tool break, then held-item captures, resource reload and synthetic
pack tests. Sprite sources explain derivation/override/fallback rules beside the
implementation in [client metallurgy](../src/client/java/blockbranch/client/metallurgy).
Native artwork is loaded at runtime, not bundled or written back to textures.

### Structures and retained fixtures

Use `checkStructureSchemas` for feature combinations and `checkSurfaceReservations`
for bounded exact caches. `checkDrainageBaseReuse`, `checkDrainageColdCache` and
`checkDrainageFlagTransition` cover exact reuse, concurrent cold reads,
collision/eviction and flag transitions; they are release gates in `test`/`check`.
Native task/property definitions are in [structures.gradle](../gradle/testing/structures.gradle)
and [integrated-overworld.gradle](../gradle/testing/integrated-overworld.gradle).

- `runIntegratedDefinitions` needs genuine historical saved definitions and native
  fixtures. `-PintegratedCodecOnly=true` skips placement, not fixture requirements.
- `prepareIntegratedReaders` and `prepareOverworldPriorReader` compile original
  codecs from byte-identical test fixtures with the Java 25 toolchain, without
  Python, historical Git objects or a required `JAVA_HOME`. The eight sources in
  `src/integrationTest/resources/blockbranch-fixtures/overworld-reader-*/` retain
  original commit/path provenance and SHA-256 digests in `ORIGIN.json`; extraction
  verifies those digests before compilation. Never replace these downgrade witnesses
  with current sources. `checkHistoricalReaderFixtures` also runs in `check`.
- The integrated harness also needs original schema-101 JSON and private
  schema-102 world NBT at its documented historical `/workspace/Blockbranch/build/`
  paths. Do not fabricate metadata or weaken assertions when inputs are absent.
- `src/integrationTest/resources/fixtures/wetland-hut/` contains unchanged
  seed 424242/1/-17 catalogs from planner `d61ff42`; demand/partial checks need them.
- `fixtures/overworld-coverage/` under the same resource root contains byte-identical
  schema-102 `starts.json`/`generator.json` from `ae86628`, consumed by
  `IntegratedDefinitionsQualification`. Keep fixtures out of production archives.
- Partial qualification needs the separately built historical JAR named by
  `structures.gradle`. `scripts/verification/legacy/` retains historical witnesses;
  setup is in source comments. They do not provision old worlds or run in `build`.
- `runNativeDecorationRegression` checks decoration ownership/seed preservation.
  `runVillagePaths` checks frozen natural villages, reverse chunk order and reopen;
  see [village-paths.gradle](../gradle/testing/village-paths.gradle) for baseline/probe flags.

Replacement fixtures require authentic closed-world references and named prior
JARs. Their scripts validate their exact properties and PASS artifacts:

| Task | Configuration |
| --- | --- |
| `runJungleReplacement` | [structures.gradle](../gradle/testing/structures.gradle) |
| `runPyramidReplacement` | [pyramid-visual.gradle](../gradle/testing/pyramid-visual.gradle) |
| `runSanctuaryCore` | [sanctuary-core.gradle](../gradle/testing/sanctuary-core.gradle) |
| `runCryptReplacement` | [crypt-visual.gradle](../gradle/testing/crypt-visual.gradle) |
| `runMansionReplacement` | [mansion-replacement.gradle](../gradle/testing/mansion-replacement.gradle) |

Compare complete architecture, native RNG/loot, block entities, residents,
consumed loot, repairs and typed player inventory through actual closed-world
reopening. Self-round-trips are not substitutes for authentic old readers/worlds.

### Forest and ground-cover compatibility

`checkForestGrasslandTransition` uses three seeds, edge confinement,
request-order checks and frozen historical roots from `146b2c5a`. Keep the frozen
selector independent. Native checks require the authentic prior named JAR:

```sh
./gradlew runRealisticMemory -PforestGrasslandOnly \
  -PtransitionPriorJar=/absolute/path/to/prior-146b2c5a.jar \
  -PrealisticMemoryDirectory=build/transition-NEW
# Repeat for save/reload and unexplored-chunk retention.
./gradlew runForestGrasslandVisual
```

`TransitionLegacySnapshotSmoke` / `-PtransitionLegacyOnly` prepares actual
schema-255 saved/future-column snapshots and edits against unchanged prior
production sources/preset. Run that copied directory with the candidate.
Keep snapshots, prior JARs and captures under ignored `build/`.

`checkRedwoodForest` checks schema chains, bounds/support, domain/spacing,
request-order independence, historical flags, fallen logs and resource references.
It does not load the biome registry; native codec validation requires Minecraft
bootstrap and `Biome.DIRECT_CODEC`.

```sh
mkdir -p build/redwood-evidence
printf '%s\n' '{"root":[-844018,430,-1692020],"height":109}' > build/redwood-evidence/witness.json
scripts/performance/run-headless-client.sh ./gradlew --offline --no-daemon -x downloadAssets \
  runRedwoodForestVisual -PredwoodVisualDirectory=build/redwood-evidence/visual-NEW
```

This witness is a seed-424242 coast redwood; a giant-sequoia alternative is
`[-860034,448,-1692011]`, height 90. Use `redwoodWitness` to select another JSON.
The run needs a working display/OpenGL runtime and about 3 GiB heap. It creates a
disposable UI world and four views; software rendering is slow for tall stands.

`runRealisticMemory` has focused fresh/saved-array/edit checks for boreal floors,
reed stacks, meadow flowers, tundra floors, sunflower placement, deserts and tree
clearing. Discover exact flags beside the fixture in `realistic.gradle`; use the
same flags/seed/directory on reopen. Upgrade evidence requires authentic prior
saved arrays, not a second fresh world.

## Client captures

Start the fixture server, wait for `Done`, then launch its client separately:

```sh
./gradlew -PseaIceClientOnly -PrealisticClientDirectory=build/ice-client-NEW \
  -PrealisticClientServerDirectory=build/ice-server-NEW runRealisticClientServer
scripts/performance/run-headless-client.sh ./gradlew --offline --no-daemon \
  --no-configuration-cache -x downloadAssets -PseaIceClientOnly \
  -PrealisticClientDirectory=build/ice-client-NEW \
  -PrealisticClientServerDirectory=build/ice-server-NEW runRealisticClientCheck
```

Both commands need matching route flags/directories. Graphical sessions can run
the client directly. The headless wrapper uses existing X11/Mesa/Xvfb tools and
installs nothing. Use a fresh Gradle JVM per display; keep Xvfb and the client in
one wrapper invocation so sandbox IPC namespaces do not break shared memory.
For Mesa sRGB/GLX failures, set `SDL_VIDEO_FORCE_EGL=1 LIBGL_ALWAYS_SOFTWARE=1`.
Cached asset indexes, fonts and visual packs are required by `-x downloadAssets`.
Prevent speaker output unless the person running the test has allowed playback.

Route flags and screenshots live beside their runs in `realistic.gradle` and
[wildlife.gradle](../gradle/testing/wildlife.gradle), rather than being copied here.
Examples include `plainsClientOnly`, `beachClientOnly`/`snowyBeachClient`,
`tundraClientOnly`/`tundraFescueCloseup`, `steppeArtReview` and the structure visual
runs. `runWildlifeSingleplayer -PsingleplayerDirectory=build/wildlife-NEW
-PitemReview=true` captures staged icons/adults/resting/babies. Focused seal,
mallard, food, perch and icon flags have distinct controls; read their mutual
exclusions first. Reloads require the saved world and matching witness UUID.
Static poses and staged scenes do not establish natural locomotion or acceptance.

Keep block textures at native 16×16. Keep models deliberately simpler than
textures; put fur/surface variation in pixels instead of contour cuboids. Asset
replacement and visual approval remain separate from automated test success.

### Distant terrain horizon

The visual-only horizon beyond the render distance is off by default. Launch both
the server and client with `-Dblockbranch.horizon=true`; `blockbranch.horizon.radius`
(blocks, default 2048) and `blockbranch.horizon.quality` (detail falloff, default 64)
tune the client. Generated chunks are read from memory or their saved data, so
edits, structures and cave openings appear; elsewhere planned structures and atlas
wells are placed in detached in-memory columns and the rest is analytic. The horizon never asks the
level to generate or load a chunk. Its design and invariants are documented in
[horizon](../src/main/java/blockbranch/horizon) and the
[client renderer](../src/client/java/blockbranch/client/horizon). `checkHorizonSelection`
and `checkHorizonProtocol` (codecs, worker lifecycle, retry of failed, stale and
refreshed tiles, cache invalidation) run in `check`; `runColumnLifecycle` checks
that horizon reads never change a chunk while it is edited and evicted. Matched captures (horizon at a short
render distance, the same distance without it, and long native terrain) use:

```sh
ALSOFT_DRIVERS=null ./gradlew --offline --no-daemon -x downloadAssets \
  runHorizonVisual -PhorizonVisualDirectory=build/horizon-evidence/NEW
```

Add the headless wrapper without a display. On a laptop with a discrete GPU, check
the log's `Using graphics device` line: the game may use the integrated GPU even
when it says it prefers the discrete one. With NVIDIA, prefix
`__NV_PRIME_RENDER_OFFLOAD=1 __GLX_VENDOR_LIBRARY_NAME=nvidia`.
`-PhorizonVisualMaxFps=60` caps the frame rate (default 260, unlimited). Cap it
when timing settle or `HORIZON_SAMPLER`. In one laptop GPU run, uncapped rendering
coincided with mean tile sampling of 1,655 ms after 256 tiles, against 696 ms in a
later 60 FPS run with a different camera. `views.json` records settle time,
tile counts and the renderer; the server log reports `HORIZON_SAMPLER` tile cost.
Clouds are off in these captures because vanilla clouds sit below realistic
terrain. When open water lies within 16,000 blocks of spawn, a last view faces its
shore from its own camera; `HORIZON_VISUAL_WATER` in the log records where. Add
`-PhorizonVisualViews=water` to capture only that view, or `=structures` for a
placed tower 150 blocks east of spawn and the nearest planned surface structure
(`HORIZON_VISUAL_EDIT`, `HORIZON_VISUAL_STRUCTURE`, and `HORIZON_VISUAL_READERS`
comparing block and analytic columns). `=wells` views the nearest atlas well and
the nearest crossing a chunk border, each from 136 blocks south with a 30° field of
view (`HORIZON_VISUAL_WELL`); wells are sparse, the nearest about 711,000 blocks from
spawn. `-PhorizonVisualRadius=512` narrows the horizon so such views settle sooner.
Capture results are completion evidence, not visual approval or hardware performance.

`runHorizonWell -PhorizonWellDirectory=build/horizon-evidence/NEW` (server only, about
half a minute) samples those two wells' tiles before the world has generated their
chunks, then generates them and compares every column within the well's planning
square; native generation must show the well, across two chunks for the border well.

`runHorizonLifecycle -PhorizonLifecycleDirectory=build/horizon-evidence/NEW`
(same wrapper, about six minutes on llvmpipe) holds the camera still and edits a
chunk only the horizon draws. It checks the cached tile is replaced and redrawn.
It checks nether trips clear client tiles, and returns draw the saved edits. It
checks an evicted tile re-read after a later edit isn't stale. It also checks
that an edit in a chunk loaded without block ticking (beside a force-loaded one)
still refreshes the tile, and that quit and reopen clear client and server
state and draw the edit from disk. `HORIZON_LIFECYCLE` log lines record each
step. With saved-source invalidation disabled, the check fails at the eviction
step; with the change hook on `ServerLevel.sendBlockUpdated`, at the
non-ticking edit.

The dedicated-server reconnect check is loopback-only, on port 25614 by default
(`-PhorizonReconnectPort`). Start the server, wait for `Done`, then run the
client with the same fresh directory:

```sh
./gradlew --offline --no-daemon -PhorizonReconnectDirectory=build/horizon-evidence/NEW \
  runHorizonReconnectServer
scripts/performance/run-headless-client.sh ./gradlew --offline --no-daemon \
  --no-configuration-cache -x downloadAssets \
  -PhorizonReconnectDirectory=build/horizon-evidence/NEW runHorizonReconnectClient
```

The client draws a distant tile and disconnects. The running server must have
dropped the player's queue; it then edits, saves and unloads the target. The
rejoining client must start with no tiles, draw the saved edit and get one new
queue. `HORIZON_RECONNECT` log lines, `server.txt` and `result.txt` record it.

## Startup and performance measurements

`runInitialLoading` uses the native Create World UI and records server start,
player arrival, first unobscured nearby mesh, synchronization and save completion.
It verifies a disposable sparse edit on reopen. This is a bounded readiness
witness, not settled-view, sustained walking, FPS or hardware acceptance.

```sh
BLOCKBRANCH_CAPTURE_MANIFEST=build/startup-NEW/launch.json \
  scripts/performance/run-headless-client.sh ./gradlew --offline --no-daemon \
  -x downloadAssets runInitialLoading -PinitialLoadingDirectory=build/startup-NEW
BLOCKBRANCH_CAPTURE_MANIFEST=build/startup-NEW/reopen-launch.json \
  scripts/performance/run-headless-client.sh ./gradlew --offline --no-daemon \
  -x downloadAssets runInitialLoading -PinitialLoadingDirectory=build/startup-NEW \
  -PinitialLoadingReopen=true
./gradlew checkTerrainPhases checkDrainageBaseReuse measureDrainageStartup
```

Defaults: seed 424242, view/simulation 6/6, preset `blockbranch:realistic`, heap 3G.
`initialLoadingSeed`, `initialLoadingView`, `initialLoadingSimulation`,
`initialLoadingPreset` and `initialLoadingHeap` override them. Use `minecraft:normal` for a
control and keep parameters identical on reopen. JFR is capped at 128 MiB with
10 ms execution/I/O/wait thresholds. Initialization getters overlap; do not sum
as exclusive time. Background completion varies, so compare saved coordinates
and statuses as well as elapsed time. Keep an external timeout beyond the native
ten-minute fixture deadline for blocked frames.

`measureDrainageStartup` builds one graph per requested seed (default 424242,1,-17;
`-PdrainageStartupSeeds=...` overrides), reports wall/thread CPU and hashes eight
graph arrays plus 10,000 shape pairs. Hashing is outside timing; timing is not a
build threshold. `checkTerrainPhases` checks exact old arithmetic/boundaries.

### Low-end device measurements

Target roughly four weak cores, 4 GiB RAM and integrated/software graphics:

```sh
SDL_VIDEO_FORCE_EGL=1 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null LP_NUM_THREADS=2 \
  taskset -c 0-3 scripts/performance/run-headless-client.sh ./gradlew --offline \
  --no-daemon --no-configuration-cache -x downloadAssets runInitialLoading \
  -PinitialLoadingDirectory=build/lowend/NEW -PinitialLoadingHeap=1536m
```

Use valid available CPU IDs and serial fresh cases. Emulation measures relative
CPU/memory effects, not an ARM board's GPU or per-core speed. Historical single
runs reduced server start 13.5→6.7 s and first frame 18.7→11.5 s; variance, steady
FPS, sustained exploration memory and physical low-end hardware remain unmeasured.
A 1 GiB-heap startup observation is not a minimum-heap guarantee.

Parallelization/caching should preserve output exactly. Hash drainage arrays,
`StructureRegionAtlas.snapshotArrays()` and representative sample grids against
unchanged baseline classes. For `TerrainShape`, cover heights, relief,
continentality and spawn over several seeds; for drainage, include `sample`,
`structuralSample`, `channelAt`, `closestT`, `edgeX`/`edgeZ` around rivers, coasts,
lakes and every distance cutoff. Random points rarely touch rivers. Existing
analytic checks alone do not prove equivalence; compare warm pinned call cost and
cold construction too. `runAtlasQualification` needs runner-provisioned EULA.

`LowEndProfile` selects FAST/render 6/simulation 5 only if `options.txt` was absent
and `HardwareTier` classifies LOW (two of ≤6 GiB RAM, ≤4 logical cores, integrated/
software GPU). `-Dblockbranch.hardwareProfile=off|low` overrides detection.
`checkHardwareTier` covers thresholds. Give a 4 GiB device at least `-Xmx2G` for
normal play; its default heap is roughly 1 GiB. Existing options stay untouched.

### Optional tools and interpretation

```sh
python3 scripts/performance/check-terrain-loading.py --output build/loading-NEW
java scripts/performance/TerrainJfrSummary.java build/<case>-server/server.jfr
python3 scripts/verification/compare-terrain-profiles.py <baseline-overworld> <candidate-overworld>
```

The loading runner needs Python 3.11+, Java 25, cached assets/dependencies and
headless display tools (or `--desktop`). Inspect `summary.json` and every PASS;
its 100 ms p95 alarm is an engineering threshold, not universal gameplay approval.
For paired terrain audits use `runTerrainAuditServer`/`runTerrainAuditClient`,
matching `origin.txt` routes and complete saved outputs. Equal requested chunks
are not equal completed work. `scripts/verification/crypt-admission-work.py`
compares completed crypt fixtures; forced admission is not connected-player FPS.
Record commit, JDK/hardware, seed, distances, commands and fresh/reopen state.
Cumulative allocation is not peak/live memory; cloud timing is not laptop FPS.

Most Python utilities use the standard library. Ore art authoring needs Python 3
and Pillow (`generate_ore_art.py --out build/ore-art`); bismuth has a JDK-only
`java scripts/assets/GenerateBismuth.java`. Audio candidate tools need the selected
script's NumPy/SciPy/Matplotlib/FFmpeg prerequisites. Read source first; Gradle
never installs or invokes those optional dependencies.

## Storage limits and historical evidence

Vanilla X/Z columns retain tickets, entities, block entities, ticks and region
ownership. Sparse absolute-Y sections avoid allocation on absent reads; only
edits occupy the procedural section store. Client meshes use a bounded vertical
window. This is not replacement entity storage. Cold planning/admission,
synchronous locates and edited-section disk I/O can still stall; bounded route
checks do not establish global cave accessibility. Progressive map outlines share
a budget and can take roughly 26 seconds or longer under contention.

Development continues in [Blockbranch-Mod](https://github.com/KatOli-Dev/Blockbranch-Mod),
which starts from a clean snapshot of private archive commit
`f969594f7ec7bac6f1f73e6004d50812423feff6`. Earlier commits and PRs are not part of
this public repository. Fixture `ORIGIN.json` references identify the private
archive, not objects that a public clone can fetch; their checked-in bytes and
SHA-256 digests are the reproducible test inputs.

Detailed qualification records remain only in the private archive: cleanup base
`00ed9d68be8da84be783c97525892bb8cdc235ae` and earlier records/assets at
`ae86628dbfe7e836fa1e8dcfe16b8b76c0ce2186`. They are not publicly accessible, and
this guide does not claim to rerun them. Maintainers with archive access can
inspect those commits there, restoring selected evidence only into isolated
worktrees or ignored output, not back into a status journal. Rejected cave-network prototypes and the private
Trail redesign are not playable features. Aquifers, full geology/erosion/weather/
ocean simulation, sulfur resources and new hazards remain deferred.
