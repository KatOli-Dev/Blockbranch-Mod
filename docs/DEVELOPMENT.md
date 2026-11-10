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

## Real-world biome coverage

The goal is broad natural surface representation before rare/underground additions. **No measured 99% Earth coverage is established.** `represented` means a selected, implemented visual/structural analog; `partial` means a family has analogs but meaningful subtypes/drivers remain absent; `missing` means no dedicated representation. None means full ecology or visual acceptance. Inventory authority is the [preset](../src/main/resources/data/blockbranch/worldgen/world_preset/realistic.json), [surface selector](../src/main/java/blockbranch/realistic/ScreeBiomeSource.java), and [physical cover/overrides](../src/main/java/blockbranch/realistic/RealisticGenerator.java).

The 18 custom surface definitions below have status **represented analog** at definition level, not 18 disjoint Earth biome families. Registry presence is separate from observed selection; generic shelf identity can be superseded by thermal variants that reuse its substrate. IDs have prefix `blockbranch:`; definitions are in [worldgen/biome](../src/main/resources/data/blockbranch/worldgen/biome). Placement/cover classes are in [realistic](../src/main/java/blockbranch/realistic); the class names below identify implementation evidence.

| Custom ID | Real-world analog; placement drivers | Selector / cover evidence |
| --- | --- | --- |
| `alpine_scree_slope` | Alpine rock debris; elevation, slope, dry exposure | `AlpineScree` |
| `inland_lake_reedbed` | Emergent lake reeds; shallow water, sheltered margin | `InlandLakeReedbed`, `ReedbedContent` |
| `gravel_bed_river_margin` | Coarse river bank; routed channel, gradient | `GravelRiverMargin` |
| `low_gradient_sandy_shore` | Sandy littoral; marine water, low slope | `LowGradientSandyShore`, `SandyShoreContent` |
| `rolling_lowland_forb_grass_meadow` | Mesic rolling grass/forbs; moisture, soil support | `LowlandMeadow` |
| `dappled_oak_birch_woodland` | Temperate open mixed woodland; climate, stands, slope | `OakBirchWoodland`, `WoodlandTrees` |
| `dry_continental_scrub_steppe` | Warm semiarid bunchgrass/shrub steppe; moisture .12–.27, temperature 5–24°C, inland | `DrySteppe`, `SteppeContent` |
| `boreal_spruce_birch_woodland` | Open boreal mixed forest; cold mesic climate, soil | `BorealWoodland`, `BorealTrees` |
| `warm_subhumid_oak_pine_woodland` | Warm subhumid evergreen woodland; mean climate, stand mask | `WarmWoodland`, `WarmTrees` |
| `cold_continental_fescue_steppe` | Cold semiarid tussock steppe; .12–.32 moisture, −6–5°C | `ColdSteppe` |
| `continental_gravel_desert` | Stony continental desert; aridity, substrate/relief | `GravelDesert`, `GravelDesertRelief` |
| `humid_subtropical_oak_fern_woodland` | Humid subtropical woodland; warm wet climate, soil | `HumidWoodland`, `HumidTrees` |
| `cold_dwarf_shrub_tundra` | Shrub/moss tundra; −14–−6°C, moisture, exposure | `ColdTundra` |
| `open_inland_lake_sediment_bed` | Open lake substrate; closed inland water/depth | `LakeSediment` |
| `open_routed_river_channel` | Open river bed; drainage routing/channel width | `RiverChannel` |
| `open_marine_shelf_coastal_sea` | Shelf/coastal bed; connected marine water/depth | `MarineShelf` |
| `deep_open_ocean_basin_bed` | Deep basin substrate; marine depth | `DeepOcean` |
| `maritime_redwood_fern_forest` | Coast redwood and giant-sequoia visual variants; cool/wet seeded province, elevation ≤900 m, no explicit coast-distance gate | `RedwoodForest`, `RedwoodTrees`; saved redwood gate |

Vanilla-named surface mappings below remain compatibility identities, not coverage gains merely because their names differ. Each family is **partial**, including families with several variants. All additional surface holders are mapped; `lush_caves`, `dripstone_caves`, `sulfur_caves`, `deep_dark` are excluded from surface accounting.

| Preset additional keys (normally `minecraft:` IDs) | Analog and limits; placement / evidence |
| --- | --- |
| `plains`, `sunflower_plains`, `meadow` | Mesic low-relief grassland, sunflower colonies, montane meadow; `Plains`, `SunflowerPlains`, `MontaneMeadow`. Not all prairie types. Fallback also uses plains. |
| `forest`, `birch_forest`, `dappled_forest`, `flower_forest`, `old_growth_birch_forest` | Temperate mixed/birch stands and floristic variants; `Forest`, `BirchForest`, `DappledForest`, `TemperateForestVariants`. Climate plus seeded provinces; not extra global families. |
| `dark_forest`, `cherry_grove` | Dense broadleaf and cherry-dominated woodland analogs; `DarkCherryWoodland`. Retained game variants, not distinct global biome area. |
| `taiga`, `snowy_taiga`, `old_growth_pine_taiga`, `old_growth_spruce_taiga`, `grove` | Boreal/montane conifer and snowy birch-spruce stands; `Taiga`, `SnowyTaiga`, `OldGrowthTaiga`, `Grove`; mean climate/stand provinces, not all temperate conifers. |
| `jungle`, `sparse_jungle`, `bamboo_jungle` | Humid tropical forest, open warm forest, bamboo stands; `Jungle`, `TropicalForestVariants`; not a dry-deciduous forest or cloud-forest model. |
| `savanna`, `savanna_plateau`, `windswept_savanna` | Acacia grassland and upland variants; `Savanna`, `SavannaUplands`; mean moisture/slope, no trophic/pyric/hummock distinction. |
| `desert`, `badlands`, `wooded_badlands`, `eroded_badlands` | Sandy desert and sedimentary badland analogs; `SandyDesert`, `Badlands`; no broad thorn/succulent xeric shrubland. |
| `snowy_plains`, `snowy_slopes`, `windswept_hills`, `windswept_forest`, `windswept_gravelly_hills`, `stony_peaks`, `jagged_peaks`, `frozen_peaks` | Snowy open ground, exposed uplands and peaks; `SnowyPlains`, `SnowySlopes`, `WindsweptUplands`, `MountainPeaks`, `ColdUplands`; not continental ice sheets. |
| `swamp`, `mangrove_swamp`, `river`, `frozen_river` | Wooded wetland, mangrove, routed/frozen river; `Swamp`, `MangroveSwamp`, `RoutedRiver`, `FrozenRiver`; drainage/temperature, no full flood pulse or tidal salinity. |
| `beach`, `snowy_beach`, `stony_shore` | Sandy/snowy/rocky coasts; `CoastalBeach`, `StonyShore`; not estuary/tidal-flat coverage. |
| `ocean`, `cold_ocean`, `lukewarm_ocean`, `warm_ocean`, `deep_ocean`, `deep_cold_ocean`, `deep_lukewarm_ocean`, `frozen_ocean`, `deep_frozen_ocean`, `ice_spikes` | Thermal/depth marine variants plus sea ice and iceberg fields; corresponding ocean classes and `IcebergField`. `ice_spikes` is a marine iceberg alias, not land-ice coverage; no separate pelagic ecology. |
| `redwood_forest` → `blockbranch:maritime_redwood_fern_forest` | Same custom redwood definition listed above; never count twice. |

[Dinerstein et al. 2017 Table 1](https://academic.oup.com/view-large/87788254) supplies priority context for 14 terrestrial families, not achieved game percentages or a denominator for all land. [IUCN GET](https://iucn.org/resources/conservation-tool/iucn-global-ecosystem-typology) adds functional distinctions; its [authored profiles](https://www.bgci.org/wp/wp-content/uploads/2023/03/IUCN-global-ecosystem-typology-2.0.pdf) pp. 40/60 distinguish seasonal dry forest and subhumid grassland. [USGS crosswalk](https://data.usgs.gov/usnvc-explorer/unitDetails/1298986) warns that dry grassland/steppe boundaries differ across systems. The following are implementation judgments from the inventory, not claims that classifications are interchangeable.

| Terrestrial family (Table 1 priority weight) | Status; distinction still needed | Placement/verification requirement beyond current analogs |
| --- | --- | --- |
| Deserts/xeric shrublands (19.3%) | Partial: sand/gravel/badlands; broad xeric shrublands missing | Aridity/soil gradients; verify shrub cover and dry/cold/warm separation |
| Tropical grassland/savanna/shrubland (15.8%) | Partial: acacia analog; trophic/pyric/hummock subtypes missing | Seasonal water deficit, substrate/disturbance proxies; canopy spacing/grass heights |
| Moist tropical broadleaf forest (14.4%) | Partial: humid jungle/bamboo; cloud/heath forests missing | Humidity/elevation/condensation; stratification and tree spacing |
| Boreal/taiga (11.4%) | Partial: spruce/birch/pine; wet peat/continental gradients missing | Frost/soil saturation; native floor and canopy checks |
| Temperate broadleaf/mixed forest (9.3%) | Partial: several stand analogs; wider regional floristics missing | Seasonal temperature/moisture; density and forest-edge checks |
| Tundra (8.7%) | Partial: shrub/moss/snow analogs; polar desert/wet tundra incomplete | Frost, exposure, saturation; distinguish land ice |
| Temperate grassland/savanna/shrubland (7.8%) | Partial: mesic plains/forb meadow and warm/cold steppe already split | Moisture/continentality, not another name-only split; verify sward heights/cover |
| Montane grassland/shrubland (3.6%) | Partial: meadow/scree; tropical alpine herbfields missing | Elevation plus frost/thermal regime; soil and slope limits |
| Tropical dry broadleaf forest (2.9%) | Missing dedicated dry forest; sparse jungle is not sufficient | Deterministic dry-season suitability plus warm climate; distinguish canopy/litter from jungle/savanna |
| Temperate conifer forest (2.8%) | Partial: redwood/sequoia/montane conifer analogs, not full family | Maritime/interior climatic separation; mature height and spacing |
| Mediterranean woodland/scrub (2.4%) | Missing dedicated summer-dry scrub; warm woodland insufficient | Summer drought/winter rain proxy and sclerophyll stature |
| Flooded grassland/savanna (0.9%) | Partial: local reed margins; broad seasonally flooded grassland missing | Hydroperiod/floodplain extent; not merely wooded swamp |
| Tropical conifer forest (0.5%) | Missing dedicated subtype; warm oak-pine only a loose analog | Warm elevation/seasonal suitability; distinct conifer structure |
| Mangroves (0.2%) | Partial: mangrove analog; tidal/salinity zonation absent | Connected coast, inundation/salinity proxy; root/canopy checks |

Keep disjoint assigned game footprints for dry/exposed land, inland open water, and connected ocean, plus an explicit unresolved-water bucket. Wet columns without ocean, terminal-lake or routed-channel ownership are not assumed freshwater just because ocean eligibility fails. Treat wetlands as attributed land/water samples, not another area added on top. For real-Earth targets choose a documented all-land denominator explicitly: [NSIDC](https://nsidc.org/learn/parts-cryosphere/glaciers/glacier-quick-facts) puts glacial ice near 10% of land; snowy plains, tundra and floating ice do not represent ice sheets. Plan a bounded static ice-cap/perennial-snow representation separately before claiming broad all-land coverage; ice flow is a separate mechanic decision.
Freshwater is represented by rivers/lakes/reeds but partial ecologically: prioritize lake trophic/depth types, marsh/floodplain and peat hydrology. Human land uses are missing at landscape scale (village farms do not suffice): propose a separately accounted optional overlay for cropland, pasture, plantation and settlement; keep natural-potential coverage separate and obtain direction before implementing land conversion.
[NOAA](https://oceanservice.noaa.gov/facts/howmanyoceans.html) puts ocean near 71% of Earth's surface. Keep ocean footprint separate from land; annotate benthic and pelagic completeness on the same footprint, never sum them ([ABS/IUCN accounting](https://www.abs.gov.au/methodologies/national-ecosystem-accounts-experimental-estimates-methodology/2022-23)). Shelf/basin and thermal/ice analogs are represented; slopes/abyssal substrates are partial and distinct pelagic communities missing. [Bell et al. 2025](https://pmc.ncbi.nlm.nih.gov/articles/PMC12057672/) estimates deep seafloor (≥200 m) at about 93% of ocean area: priority context, not a required game depth threshold or proof of deep-sea ecology.

The [current survey](../src/integrationTest/java/blockbranch/realistic/CurrentBiomeSurvey.java) runs `./gradlew --offline --no-daemon --no-parallel --max-workers=2 -PbiomeSurveyDirectory=build/biome-survey-NEW runCurrentBiomeSurvey` using cached Java/Fabric dependencies, no Python/downloads. It creates a disposable loopback-only world on port 25615; choose a fresh directory, retain outputs, and run native workloads serially. `survey.json` records the full preset/gates, seeds 424242/1/−17, centered 32 km grid in X/Z [−1920000,1920000), 14,400 samples/seed, disjoint denominators, per-footprint biome/fallback counts, aggregate fallback climate/elevation bins, and missing-in-sample IDs. `samples.csv` records every sample and water-ownership predicates. Missing-in-sample is not unreachable; no exhaustive reachability proof is attempted. Narrow habitats/map edges are under-resolved.
The survey executes production routing using a detached fallback sentinel to distinguish intentional plains. Native generated-world witnesses separately record final quart and visible/zoom biome reads, overrides and surface blocks; they do not convert the grid into a final-palette area survey. Legacy `checkBiomeCoverage` and related staged diagnostics retain their tests but do not inventory the complete selector. Mean climate, fixed westerlies and priority-preserving selectors lack season duration, frost extremes, tidal salinity and ice mass balance; ground-cover routing also differs from quart biome routing. PASS establishes accounting/fixture assertions, not real-Earth percentages, visual approval or full ecology.
At production base `4aee60e`, that command samples 43,200 cells: land 12,033 (867 fallback, 7.21%), connected ocean 31,025 (zero fallback), inland water 43 (zero fallback), unresolved open water 99 (all fallback). These are game-grid denominators only. Land fallbacks include 595 at elevation ≥1,500 m and 583 below mean 5°C; these overlapping clusters suggest highland/cold-ground ownership investigation, not a proven cause. All unresolved-water beds are 1–26 m below sea level but fail all three water-owner predicates; do not silently count them as ocean or freshwater. Four surface identities are missing from all three grids: generic shelf, frozen river, snowy beach and swamp; no unreachability claim follows. Seven native witnesses agree with selector/zoom identities, including stone highland fallback versus intentional grassy plains; no biome override was observed in those spots. Retain JSON/CSV/PASS evidence under the chosen ignored output directory.

Bounded roadmap: (1) retain this inventory and current-selector survey without generation changes; (2) weigh fallback ownership fixes (especially cold/exposed highlands and shallow coasts) against one tropical dry-forest subtype, using the measured game-grid gaps rather than name counts; (3) fill common xeric/savanna structural gaps, then Mediterranean/floodplain types; (4) ice-cap and ocean benthic/pelagic representation with explicit denominator decisions. Raise common-family completeness ahead of rare floristic additions. Static dry-season suitability, not gameplay weather, is the selected dry-forest design direction; specify credible field derivation/bounds before implementation and stop if it needs broader architecture. Mean moisture alone is insufficient. Any placement/split uses a new independent opt-in over complete schema 257, absent = legacy, rejects partial/future foundations and missing holders, and preserves aliases. Require aggregate `build`, resource/archive/classpath/documentation guards, focused codec/placement tests, frozen legacy results/authentic reopen checks where available, native density/height witnesses, and real generated-world headless captures with null audio. No automatic pass supplies user visual acceptance.

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

The horizon is off by default. Enable `-Dblockbranch.horizon=true` on both sides;
client radius defaults to 2048 blocks (`blockbranch.horizon.radius`), quality to 64
(`blockbranch.horizon.quality`). It reads generated/saved chunks, including edits,
structures and caves; elsewhere it uses detached planned structures/atlas wells
and analytic terrain. It never requests chunk generation or loading. See the
[planner](../src/main/java/blockbranch/horizon) and [renderer](../src/client/java/blockbranch/client/horizon).
`checkHorizonSelection`/`checkHorizonProtocol` cover codecs, workers, retries and
invalidation in `check`; `runColumnLifecycle` covers edit/eviction read isolation.
Matched short-distance horizon/off and long-distance native captures use:

```sh
ALSOFT_DRIVERS=null ./gradlew --offline --no-daemon -x downloadAssets \
  runHorizonVisual -PhorizonVisualDirectory=build/horizon-evidence/NEW
```

Add the headless wrapper without a display. Verify `Using graphics device` in logs;
NVIDIA offload can use `__NV_PRIME_RENDER_OFFLOAD=1 __GLX_VENDOR_LIBRARY_NAME=nvidia`.
Cap captures with `-PhorizonVisualMaxFps=60` (default 260/unlimited); uncapped rendering
can contend with tile sampling. Different-camera timings do not isolate the cause.
`views.json` records settle/counts/renderer; `HORIZON_SAMPLER` records server tile cost.
Clouds are disabled because vanilla clouds sit below realistic terrain. Select
`-PhorizonVisualViews=water` (shore within 16 km), `=structures` (tower 150 m east
and nearest planned structure), or `=wells` (nearest and chunk-border atlas wells,
136 m south, 30° field of view). `-PhorizonVisualRadius=512` reduces settle work.
The `HORIZON_VISUAL_WATER/EDIT/STRUCTURE/READERS/WELL` logs identify these scenes.
Captures establish completion, not visual acceptance or hardware performance.
`runHorizonWell -PhorizonWellDirectory=build/horizon-evidence/NEW` compares planned
well tiles with later native generation, including both chunks at the border.
`runHorizonLifecycle -PhorizonLifecycleDirectory=build/horizon-evidence/NEW` checks
distant edit replacement/redraw, Nether resets, edits after eviction, non-ticking
loaded-chunk edits and quit/reopen retention. `HORIZON_LIFECYCLE` records each step;
disabled saved-source invalidation fails eviction, and using `sendBlockUpdated`
as the hook fails non-ticking edits. Use the same headless wrapper for the client.
For reconnect, start the server, await `Done`, then use the same fresh directory
for the client; loopback port defaults to 25614 (`-PhorizonReconnectPort`):

```sh
./gradlew --offline --no-daemon -PhorizonReconnectDirectory=build/horizon-evidence/NEW \
  runHorizonReconnectServer
scripts/performance/run-headless-client.sh ./gradlew --offline --no-daemon \
  --no-configuration-cache -x downloadAssets \
  -PhorizonReconnectDirectory=build/horizon-evidence/NEW runHorizonReconnectClient
```

The disconnect must drop the player's queue. After server edit/save/unload, rejoin
must start empty, draw the saved edit and get one new queue. Require the
`HORIZON_RECONNECT` logs, `server.txt` and `result.txt` evidence.

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

### Optional chunk rendering patches

`RenderPatches` contains the master switch, per-patch switches and exact Minecraft
version guard. Other versions load no patch mixins. Use `-PrenderPatches=false`
to omit the version-specific patch sources during an update, or launch with
`-Dblockbranch.renderPatches=off` for the eager snapshot path. Both retain the
existing sparse-column behavior. Dedicated servers never load this client config.
The deferred path captures only immutable, unedited base profiles; edits and
received overlays retain eager snapshots. Disconnect clears the bounded cache,
and in-flight results cannot repopulate it.

`checkSectionGeneration` compares section ore masks to the original per-block
vein rules and cave densities bit for bit. It runs in `test`/`check`. The native
copy smoke also compares generated palettes to `TerrainProfile.state`, checks
cache concurrency, edit isolation and disconnect. Run startup fresh/reopen with
`-PsectionCopiesSmoke`; use `-PrenderPatchesRuntime=off` to check the fallback.
Use the software/no-audio environment from the client examples above.

For walking measurements, `runIntegratedWalking` accepts
`-PintegratedWalkingHeap=1536m`, `-PintegratedWalkingView=6` and
`-PintegratedWalkingSimulation=5`. Prepare `walking.jfc` in its fresh
`integratedWalkingDirectory` from the installed JDK's `lib/jfr/profile.jfc`.
Compare phase-2 `frames.csv` with matching baseline settings. Emulation is
relative CPU evidence; automated passes establish neither hardware FPS nor
visual acceptance. Run all builds/native workloads serially with
`--max-workers=1 --no-parallel`, starting offline with cached dependencies.

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
