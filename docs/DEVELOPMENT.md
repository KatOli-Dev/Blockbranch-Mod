# Development, build and tests

Use Java 25 and the checked-in Gradle wrapper from the repository root. Versions are pinned in [gradle.properties](../gradle.properties). Dependencies and Minecraft assets must already be cached for offline runs; `--offline` does not provision anything.
Python is optional, never needed for Gradle build/check, IDE import, ordinary game launches or packaged gameplay. Approved assets are checked in.

## Build and repository layout

```sh
./gradlew build
./gradlew checkDocumentation checkProductionArchives checkDevelopmentClasspaths
./gradlew verifyWildlifeItemResources verifyNativePickaxeResources
./gradlew tasks --all
```

`build` compiles production/development code, runs canonical analytic JavaExec checks through `test`/`check`, and creates binary/source JARs in `build/libs/`. There are no framework-discovered tests; historical/manual diagnostics remain explicitly callable, not adopted by task-name prefix.

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

Ordinary `runServer`/`runClient` use production classpaths; QA Loom runs use the integration mod. `checkProductionArchives` rejects QA types, test mixins and fixture helpers in both archives. It also checks packaged credits and client-only manifest entries. `checkDevelopmentClasspaths` keeps QA outputs off ordinary server/client runtime classpaths. Both guards run in `check`.

Keep only this guide and [SAVE_COMPATIBILITY.md](SAVE_COMPATIBILITY.md) under `docs/`, each at most 500 lines. `checkDocumentation` enforces the limit for the repository's Markdown references and checks their local links. Put stable implementation rationale beside its code, not in a growing qualification journal. Do not regenerate approved assets, remove test inputs, or change licenses as cleanup.

## Real-world biome coverage

The goal is broad natural surface representation before rare/underground additions. **No measured 99% Earth coverage is established.** `represented` means a selected, implemented visual/structural analog; `partial` means a family has analogs but meaningful subtypes/drivers remain absent; `missing` means no dedicated representation. None means full ecology or visual acceptance. Inventory authority is the [preset](../src/main/resources/data/blockbranch/worldgen/world_preset/realistic.json), [surface selector](../src/main/java/blockbranch/realistic/ScreeBiomeSource.java), and [physical cover/overrides](../src/main/java/blockbranch/realistic/RealisticGenerator.java).

The 22 custom surface definitions below have status **represented analog** at definition level, not disjoint Earth biome families. Registry presence is separate from observed selection; generic shelf identity can be superseded by thermal variants that reuse its substrate. IDs have prefix `blockbranch:`; definitions are in [worldgen/biome](../src/main/resources/data/blockbranch/worldgen/biome). Placement/cover classes are in [realistic](../src/main/java/blockbranch/realistic); the class names below identify implementation evidence.

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
| `cool_continental_stony_desert` | Cool inland stony-desert structural analog; cold/arid final fallback, modeled freezing winter and warm season, supported gentle terrain | `CoolDesert`; saved cool-desert gate |
| `cold_exposed_rockland` | Ice-free cryogenic outcrops/debris; final fallback below mean −6°C, supported debris and sheltered static snow | `ColdRockland`; saved surface-ownership gate |
| `seasonal_dry_broadleaf_forest` | Narrow regional seasonally dry forest analog; idealized precipitation seasonality, warm-woodland affinity; dense broadleaf canopy/litter | `DrySeasonPolicy`, `DryForest`, `DryForestTrees`; saved dry-forest gate |
| `perennial_alpine_icefield` | Prescribed perennial snow/land-ice body; conservative static warm-season/supply screen and supported bed-plane geometry; continental sheets and microbial ecology missing | `LandIceField`; saved land-ice gate, native core/edge/profile/API/lifecycle checks |
| `maritime_redwood_fern_forest` | Coast redwood and giant-sequoia visual variants; cool/wet seeded province, elevation ≤900 m, no explicit coast-distance gate | `RedwoodForest`, `RedwoodTrees`; saved redwood gate |

Vanilla-named surface mappings below remain compatibility identities, not coverage gains merely because their names differ. Each family is **partial**, including families with several variants. All additional surface holders are mapped (`cold_rockland`, `dry_forest`, `cool_desert` and `land_ice` alias the custom analogs above); `lush_caves`, `dripstone_caves`, `sulfur_caves`, `deep_dark` are excluded from surface accounting.

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
| Deserts/xeric shrublands (19.3%) | Partial: sand/gravel/badlands plus a cool stony-desert analog; broad warm xeric shrublands, salt/dune/cold-desert subtypes missing | Aridity/soil gradients; verify shrub cover and dry/cold/warm separation |
| Tropical grassland/savanna/shrubland (15.8%) | Partial: acacia analog; trophic/pyric/hummock subtypes missing | Seasonal water deficit, substrate/disturbance proxies; canopy spacing/grass heights |
| Moist tropical broadleaf forest (14.4%) | Partial: humid jungle/bamboo; cloud/heath forests missing | Humidity/elevation/condensation; stratification and tree spacing |
| Boreal/taiga (11.4%) | Partial: spruce/birch/pine; wet peat/continental gradients missing | Frost/soil saturation; native floor and canopy checks |
| Temperate broadleaf/mixed forest (9.3%) | Partial: several stand analogs; wider regional floristics missing | Seasonal temperature/moisture; density and forest-edge checks |
| Tundra (8.7%) | Partial: shrub/moss/snow plus exposed cryogenic rock analogs; polar desert/wet tundra incomplete | Frost, exposure, saturation; distinguish land ice |
| Temperate grassland/savanna/shrubland (7.8%) | Partial: mesic plains/forb meadow and warm/cold steppe already split | Moisture/continentality, not another name-only split; verify sward heights/cover |
| Montane grassland/shrubland (3.6%) | Partial: meadow/scree; tropical alpine herbfields missing | Elevation plus frost/thermal regime; soil and slope limits |
| Tropical dry broadleaf forest (2.9%) | Partial: narrow regional precipitation-envelope analog; wetter/longer-drought subtypes and annual water balance remain absent | Idealized seasonal rain plus warm-woodland affinity; closed canopy/litter, not savanna or sparse jungle |
| Temperate conifer forest (2.8%) | Partial: redwood/sequoia/montane conifer analogs, not full family | Maritime/interior climatic separation; mature height and spacing |
| Mediterranean woodland/scrub (2.4%) | Missing dedicated summer-dry scrub; warm woodland insufficient | Summer drought/winter rain proxy and sclerophyll stature |
| Flooded grassland/savanna (0.9%) | Partial: local reed margins; broad seasonally flooded grassland missing | Hydroperiod/floodplain extent; not merely wooded swamp |
| Tropical conifer forest (0.5%) | Missing dedicated subtype; warm oak-pine only a loose analog | Warm elevation/seasonal suitability; distinct conifer structure |
| Mangroves (0.2%) | Partial: mangrove analog; tidal/salinity zonation absent | Connected coast, inundation/salinity proxy; root/canopy checks |

Keep disjoint assigned game footprints for dry/exposed land, inland open water, and connected ocean, plus an explicit unresolved-water bucket. Wet columns without ocean, terminal-lake or routed-channel ownership are not assumed freshwater just because ocean eligibility fails. Treat wetlands as attributed land/water samples, not another area added on top. For real-Earth targets choose a documented all-land denominator explicitly: [NSIDC](https://nsidc.org/learn/parts-cryosphere/glaciers/glacier-quick-facts) puts glacial ice near 10% of land; snowy plains, tundra and floating ice do not represent ice sheets. Ice is compacted multiyear snow, not seasonal cover; low precipitation alone does not preclude it where ablation is low. The bounded alpine perennial-ice snapshot is partial; justify broader ice-cap/sheet extent separately before claiming broad all-land coverage; accumulation/ablation and geometry need explicit assumptions, while ice flow is a separate mechanic decision.
Freshwater is represented by rivers/lakes/reeds but partial ecologically: prioritize lake trophic/depth types, marsh/floodplain and peat hydrology. Human land uses are missing at landscape scale (village farms do not suffice): propose a separately accounted optional overlay for cropland, pasture, plantation and settlement; keep natural-potential coverage separate and obtain direction before implementing land conversion.
[NOAA](https://oceanservice.noaa.gov/facts/howmanyoceans.html) puts ocean near 71% of Earth's surface. Keep ocean footprint separate from land; annotate benthic and pelagic completeness on the same footprint, never sum them ([ABS/IUCN accounting](https://www.abs.gov.au/methodologies/national-ecosystem-accounts-experimental-estimates-methodology/2022-23)). Shelf/basin and thermal/ice analogs are represented; slopes/abyssal substrates are partial and distinct pelagic communities missing. [Bell et al. 2025](https://pmc.ncbi.nlm.nih.gov/articles/PMC12057672/) estimates deep seafloor (≥200 m) at about 93% of ocean area: priority context, not a required game depth threshold or proof of deep-sea ecology. [NOAA abyssal plains](https://oceanexplorer.noaa.gov/ocean-fact/what-are-abyssal-plains/) describes flat sedimented seafloor, typically 3–6 km deep, with surface-derived food; prominent ridges/seamounts are excluded. Definitions yield widely differing area estimates, so no precise abyssal-area fraction is adopted.

Abyssal sediment traces are a **partial structural analog** on the existing benthic footprint, with no new biome identity or added area. [IUCN GET M3.3](https://www.bgci.org/wp/wp-content/uploads/2023/03/IUCN-global-ecosystem-typology-2.0.pdf) (printed p.150) identifies tracks, burrows and mounds as habitat structures. [Swanborn et al.2026](https://doi.org/10.1002/lno.70420) documents trace forms across deep-sea sites; varying survey effort makes those observations presence-only, not a universal density calibration. Static openings, paired mounds and shallow trails represent microrelief, not identified species, animal populations, active bioturbation, carbon supply or a complete food web. Pelagic ecology remains missing; continental land ice beyond the bounded alpine subtype still requires justified accumulation/ablation and geometry, while marsh/peat/trophic-lake types need hydroperiod, water-table or nutrient evidence beyond the current proxies.
Placement uses confirmed marine water at 3–6 km depth, selected mud/clay substrate and 32 m slope ≤.08. It does not classify every eligible patch as a geomorphic abyssal plain. The actual model footprint lies within one column (2..14/16 horizontally); that fine-bed block supports it without requiring neighbouring terraces to share a height. Bodies retain source water and have no collision. Model geometry (up to 3/16 m high), 12 m jittered cells and .35 candidate probability are disclosed voxel-scale conventions, not measured sizes/abundance. There is no collectible item, crafting, fauna or food mechanic; explicit command edits are still possible. Relief, water ownership, substrate roles and selector identities are retained.
Published schema261 diagnosis uses seeds 424242/1/−17, the existing centered 32 km grid in [−1920000,1920000), 14,400 samples/seed. Of 23,679 abyssal-depth marine samples, 1607 select fine sediment and 1520 also pass slope. These are game-grid observations, not Earth coverage; the narrow fine-bed support suggests a future review of inherited bed/relief treatment, not a reason to relax exclusions. Three original depth-only 243-chunk windows yield 2/6/8 traces; three windows centered on the first eligible coarse sites yield 14/5/11, with all three forms. The unsupported >20-per-terrain-window expectation was replaced by an all-eligible candidate control (143/153/155 traces in 400 cells) and actual nonzero/form/support checks in the finite world windows. No ecological exclusion or density convention was changed to obtain a pass; these are historical schema261 counts, before the separately gated deep-relief correction. Native seed424242 ±4-chunk windows at (−1744000,−1904000), (−528000,−1904000), (304000,−1904000) cover 243 chunks / 62,208 columns / 497,664 ground−3..ground+4 reads, 14 traces, 15,744 base-column reads and 17,712 height checks. Only source-water cells above fine beds change; 13 profiles append the new wrapper and 230 retain legacy encoding. These are finite witnesses, not a global proof or user visual acceptance.
Reproduce with `./gradlew --offline --no-daemon --no-parallel --max-workers=2 checkAbyssalTraces`; `surveyAbyssalTraceSites` diagnoses the fixed grid/windows without a density assertion. Use `-PabyssalTraceDirectory=build/abyssal-NEW runAbyssalTraceQualification`, then `-PabyssalTraceReopen` in that directory; the saved removal of a natural trace must remain source water. For old-world qualification copy the closed authentic schema260 `survey` world and add `-PabyssalTraceLegacy`. For the bounded block lifecycle/manual-content path in a separate old-world copy add `-PabyssalTraceBlockOnly`, then repeat with `-PabyssalTraceReopen`: exact waterlogged form/facing, lateral updates, support removal to source water and old generation/base-profile retention must hold. Require explicit PASS and retain all JSON/worlds. [Frozen provenance](../src/integrationTest/resources/blockbranch-fixtures/abyssal-reader-260/ORIGIN.json) records complete public `12e0512` production before edits: 43,200 grid samples, 372,136 physical and 3,803,672 native states, native profile hashes and a saved edit. Renamed generators share candidate helpers; the whole-original output oracle supplies independent finite evidence. The renamed original profile rejects75; its unchanged core is tested separately because cave validators name production TerrainProfile in JVM signatures. Never refresh the oracle from candidate output. `-Ppublished260Directory=build/original-NEW runPublished260Snapshot` requires that full original tree plus the digest-identified development collector/registration.
Native trace captures use `ALSOFT_DRIVERS=null scripts/performance/run-headless-client.sh ./gradlew --offline --no-daemon --no-configuration-cache -x downloadAssets -PabyssalTraceVisualDirectory=build/abyssal-visual-NEW runAbyssalTraceVisual`; record night vision for abyssal inspection, inspect actual images, and prevent speaker output. Catalog21 conservatively protects explicit new-block edits even in old-generation worlds; the frozen catalog20 reader must reject its canonical marker before interpreting content. The three original block models reference installed Minecraft textures; no source illustrations/datasets, texture downloads or new dependencies are imported.

The [current survey](../src/integrationTest/java/blockbranch/realistic/CurrentBiomeSurvey.java) runs `./gradlew --offline --no-daemon --no-parallel --max-workers=2 -PbiomeSurveyDirectory=build/biome-survey-NEW runCurrentBiomeSurvey` using cached Java/Fabric dependencies, no Python/downloads. It creates a disposable loopback-only world on port 25615; choose a fresh directory, retain outputs, and run native workloads serially. `survey.json` records the full preset/gates, seeds 424242/1/−17, centered 32 km grid in X/Z [−1920000,1920000), 14,400 samples/seed, disjoint denominators, per-footprint biome/fallback counts, aggregate fallback climate/elevation bins, and missing-in-sample IDs. `samples.csv` records every sample and water-ownership predicates. Missing-in-sample is not unreachable; no exhaustive reachability proof is attempted. Narrow habitats/map edges are under-resolved.
The survey executes production routing using a detached fallback sentinel to distinguish intentional plains. Native generated-world witnesses separately record final quart and visible/zoom biome reads, overrides and surface blocks; they do not convert the grid into a final-palette area survey. Legacy `checkBiomeCoverage` and related staged diagnostics retain their tests but do not inventory the complete selector. Legacy mean climate, fixed westerlies and priority-preserving selectors lack general seasonal climatology, frost extremes, tidal salinity and ice mass balance; the new habitat-local monthly controls remain idealized, not observed seasons; ground-cover routing also differs from quart biome routing. PASS establishes accounting/fixture assertions, not real-Earth percentages, visual approval or full ecology.
The frozen schema-257 audit at production base `4aee60e` sampled 43,200 cells: land 12,033 (867 fallback, 7.21%), connected ocean 31,025 (zero fallback), inland water 43 (zero fallback), unresolved open water 99 (all fallback). These are game-grid denominators only. Land fallbacks include 595 at elevation ≥1,500 m and 583 below mean 5°C; these overlapping clusters suggest highland/cold-ground ownership investigation, not a proven cause. All unresolved-water beds are 1–26 m below sea level but fail all three water-owner predicates; do not silently count them as ocean or freshwater. Four surface identities are missing from all three grids: generic shelf, frozen river, snowy beach and swamp; no unreachability claim follows. Seven native witnesses agree with selector/zoom identities, including stone highland fallback versus intentional grassy plains; no biome override was observed in those spots. Retain JSON/CSV/PASS evidence under the chosen ignored output directory.

The schema-258 definition-1 survey retains 12,033 land samples with 559 fallback (308 cold fallback cells become rockland), assigns all 99 unowned-water cases to verified basin-owned marine habitats, and records 31,126 ocean / 41 inland-water / zero unresolved-water samples. The two inland-to-marine changes are open-basin outlet cases, not closed lakes. Existing dry-land owners are retained outside cold fallback. Native qualification exercises 16,641 coastal columns and 81 chunks; physical sand excludes both aquatic sediment and quart-mask kelp. Four generated-world views show exposed debris, sheltered snow, shore sand and a submerged sediment bed. These remain game-grid and representation evidence, not Earth-area or full ecological coverage.
The two outlet witnesses are seed 424242 at (400000,−944000), bed Y=61, water Y=64 (2 m depth), and seed −17 at (1616000,464000), bed Y=59, water Y=64 (4 m). Published/candidate CSVs keep identical heights, datum, temperature/moisture and routed-channel flags; terminal-lake ownership is false. The grid assertion verifies open ocean basins and raw submerged seabed, identical `Sample`/`Channel` values, and unchanged legacy structural ocean ownership. The changed habitat is routed river → ocean. Connectivity is the existing solved basin graph, not an independently traced voxel-water path; neither carving nor hydrology is altered.
Dry forest definition 1 uses a new **idealized precipitation climatology**, not inferred rainfall/PET or simulated weather. The named regional SDTF analog follows [Meir & Pennington](https://www.gov.uk/research-for-development-outputs/climatic-change-and-seasonally-dry-tropical-forests): modeled annual rain below 1600 mm/model-year with 4–6 consecutive months below 100 mm/model-month; disconnected short droughts are excluded. It splits only the selector's established warm-woodland owner above mean 23°C, retaining savanna and protected aquatic/coastal/specialist owners. New closed broadleaf cohorts use existing oak scaffold/material roles and rooted-soil/litter floors, with rare grass/ferns. This is a static growing-season structural analog: deciduous phenology, floristic fidelity, fertility/fire feedback and IUCN T1.2's near-balanced annual water supply remain unmodeled.
**Chosen generator coefficients, not empirical calibration:** `ClimateField.atmosphericSupply` separately recomputes macro noise + maritime + windward − lee suitability, excluding 256 m wet-ground interpolation; existing mean-climate samples remain unchanged. Assign `Pmodel = 4000*a^1.5` (a=.18/.55/.65 → about 305/1632/2096 mm/model-year). These are declared arid/open-warm/humid design anchors, not a measured conversion of unitless moisture. Twelve weights `0.20 + exp(-0.5*((z/2097152 - .17*cos(2π*(month-5)/12))/.10)^2)` are normalized to conserve that total. Signed map warm-belt coordinates are not astronomical degrees. [NOAA ITCZ](https://www.noaa.gov/jetstream/tropical/convergence-zone) supports the migrating-belt mechanism and equatorial two-peak to one-season transition, not these spatial/annual coefficients. No new decorative seasonality noise is used.
**Published ecological anchors and exclusions:** [Smithsonian Barro Colorado](https://striresearch.si.edu/physical-monitoring/barro-colorado/) has about 2655.3 mm/year and a four-month dry season totaling 299 mm yet humid forest: dry-month count alone is insufficient. [Huang et al. 2021](https://doi.org/10.1002/ecs2.3824) reports Santa Rosa dry forest at 1723±679 mm/year over 1980–2017; its exclusion demonstrates this ceiling is not universal, and site periods differ. [FAO GEZ](https://www.fao.org/4/ad652e/ad652e07.htm) instead uses 5–8 dry months with P≤2T and vegetation evidence; that convention is not mixed into this subtype. [FAO-56](https://www.fao.org/4/X0490E/x0490e07.htm) requires daily maximum-minus-minimum temperatures for Hargreaves; SeaIcePolicy's annual marine thermal amplitude is not substituted. No PET or physical water-deficit calculation is claimed; drainage/retention proxies are not fertility.
**Independent validation versus design controls:** equal-total synthetic 1440 mm profiles (uniform 120/month versus six 20 plus six 220) distinguish seasonal duration; an alternating profile rejects disconnected droughts. They test the chosen mathematics, not empirical calibration. Reference aggregate profiles retain the published BCI totals and deliberate Santa Rosa exclusion without importing monthly observations. Fixed seeds 424242/1/−17 on the existing 43,200-point grid split 30 warm-woodland samples (11/18/1), with zero unrelated-owner changes. Two native ±32-block windows at stride 1 pass 371,800 state comparisons; canopy covers 6518/8450 owned columns versus 2661 under published woodland, with litter on 7055 columns. These finite generated-world observations are not Earth-area fractions, complete ecology or user visual acceptance. Interior checks are supplemented by a deterministic mixed edge at (1647636,−208512)/(1647635,−208512): discovery is limited to the two candidate centers ±512 blocks, stride 16, then adjacent-block refinement; the native ±16-block window at stride 1 covers 561 owned / 528 unowned columns and 47,916 surface states. Direct APIs cover 45 owned / 37 unowned columns (stride 4 plus the adjacent pair): 4756 base-column surface states and 1968 base-height assertions against native blocks/heightmaps, using expanded world bounds and clipped [ground−3,ground−1], [ground+1,ground+3], [ground+8,ground+15] ranges. Surface-state comparisons are limited to ground−3 through ground+40; these remain finite witness checks.
**Sensitivity:** annual scale, belt migration and width each vary ±5%, and the dry-month threshold varies 95/105 mm in a fixed nine-control check over the same grid. At most four samples per 14,400-point seed change; changes remain confined to existing warm-woodland affinity. Controls test behavior around declared coefficients rather than proving their real-world accuracy. Remaining broad subtype/driver gaps stay partial in the ledger; a rainfall-concentration index alone would not establish a climatic water deficit.
Reproduce with `./gradlew --offline --no-daemon --no-parallel --max-workers=2 checkDrySeasonPolicy` and `-PdryForestDirectory=build/dry-NEW runDryForestQualification`, then `-PdryForestReopen` in that same directory. For the bounded mixed-edge/base-API check use a fresh directory with `-PdryForestBoundaryOnly`. For legacy qualification copy the closed published schema-258 `survey` world, then add `-PdryForestLegacy`; never synthesize its metadata. [Reader provenance](../src/integrationTest/resources/blockbranch-fixtures/dry-season-reader-258/ORIGIN.json) freezes independently recorded complete public `e771c4e` outputs before any production edit: all 2018 main/client blobs, 43,200 selector/terrain/climate/ownership samples, 4569 physical and 28,433 native reads plus an actual saved edit. Renamed reader comparisons still share candidate helpers; frozen original-output hashes cover that gap for these inputs. Preserve the collector/baseline digests, worlds, JSON/PASS, captures and logs under ignored `build/`; never refresh the oracle from candidate code. Native visuals use `ALSOFT_DRIVERS=null scripts/performance/run-headless-client.sh ./gradlew --offline --no-daemon --no-configuration-cache -x downloadAssets -PdryForestVisualDirectory=build/dry-visual-NEW runDryForestVisual`; no staged terrain or speaker output.

Bounded roadmap comparison: schemas 258–260 add ownership, regional dry forest and a cool-desert analog; 261 adds only static abyssal traces, 262 corrects inherited deep-bed roughness. Neither marine change adds identities or disjoint area. Warm xeric shrublands/savanna need defensible shrub/grass/disturbance controls; Mediterranean scrub needs winter-rain/summer-dry forcing, which the tropical moving-belt model does not supply. Marsh/floodplain/peat and trophic-lake types need hydroperiod, water-table or nutrient evidence. Pelagic communities and benthic food webs remain missing. Continental land ice remains a larger explicit omission beyond the bounded alpine subtype: [NSIDC accumulation/ablation](https://nsidc.org/learn/parts-cryosphere/glaciers/science-glaciers) and [ice-sheet geometry](https://nsidc.org/learn/parts-cryosphere/ice-sheets) support a static ice-body state without requiring dynamics, but current mean climate/summit veneers do not establish its extent or thickness; justify those assumptions before adding it. No ice/ocean area priority is erased by bounded analogs. Keep broad natural families ahead of rare floristics and the deferred height task. Physical water deficit or broader dry-forest placement still needs defensible precipitation/evaporative-demand inputs.
Deep-basin relief definition 1 attenuates the 63 m bank field reused offshore, not structural ocean relief. The untouched `TerrainShape` already supplies kilometer-scale basin/ridge/trench terms. Disclosed generator choices: depth fades from 1 to 3 km; original continuous 256 m-support gradient response fades from .02 to .10, uses a one-cell rugged buffer, then quintic 256 m scalar interpolation; residual bank amplitude is .10. This avoids opposing-gradient cancellation and sharp response changes near narrow crests. These are model conventions, not empirical calibration or an abyssal-area target. Regional climate references, routed/terminal water, marine ownership and original substrate/slope exclusions stay unchanged. Fine-bed/trace eligibility can increase as a consequence; no density target is fitted. [NOAA](https://oceanexplorer.noaa.gov/ocean-fact/what-are-abyssal-plains/) supports relatively flat sedimented floors contrasted with prominent relief, not these coefficients or complete ecology.
Fixed seed 424242 witnesses ±128 m at stride 4 separate structure from detail: the previous fine-bed site's structural span is 2 m while bank RMS falls 8.38→.88m; another gentle basin spans 1 m with RMS 9.55→1.00m. Rugged margin/deep-floor/trench, shelf/coast, river/lake and upland windows retain exact physical heights; the trench's original structural span is 965 m. Crossing ±2048 m one-block transects bound blend-induced component variation below .25 m/block; scalar endpoint/monotonicity controls test depth/gradient continuity. Twelve native ±1-chunk windows cover 108 chunks / 27,648 columns: 6,681 physical heights change in four offshore windows, with eight controls unchanged, 272,243 surface-state, 7,344 base-column and 7,776 height checks. Authentic schema261 open/reopen and schema262 reopen retain their actual definitions and gold edits; original native/profile hashes match and old-mode windows have zero changed columns. The published schema262 43,200-point survey retains 71 identities (21 custom / 50 vanilla-named), 12,033 land / 31,126 connected ocean / 41 inland water / zero unresolved water and 533 land fallbacks; 21,186 physical heights change in the offshore component check, with no water-owner or mean-climate changes. Four unobserved identities remain unobserved, not proven unreachable. This finite physical qualification is not a performance benchmark, global geomorphic proof or trace-population calibration.
Reproduce with `./gradlew --offline --no-daemon --no-parallel --max-workers=2 checkDeepBasinRelief`; `assessDeepBasinRelief` records the bounded original-component survey without changing generation. Use a fresh `-PdeepBasinDirectory=build/deep-NEW runDeepBasinQualification`, then `-PdeepBasinReopen`; for legacy copy the closed authentic published 261 world and add `-PdeepBasinLegacy`. [Frozen provenance](../src/integrationTest/resources/blockbranch-fixtures/deep-basin-reader-261/ORIGIN.json) records all 2035 original main/client blobs at public `2b35817` before edits, 43,200 grid samples, 754,739 physical states, 7,720,412 native states/profile hashes and a real saved gold edit. Compiled renamed readers share candidate helpers; these independently recorded whole-production hashes bound that gap to the recorded inputs. Never refresh the oracle from candidate output or synthesize old metadata. Native captures use `ALSOFT_DRIVERS=null scripts/performance/run-headless-client.sh ./gradlew --offline --no-daemon --no-parallel --max-workers=2 --no-configuration-cache -x downloadAssets -PdeepBasinVisualDirectory=build/deep-visual-NEW runDeepBasinVisual`; matching old views add `-PdeepBasinVisualLegacy` with a copy of a closed schema 261 visual world at `saves/DeepBasinVisualReview`. Cameras use matching nearby unchanged reference columns within ±16 m (zero bank in changed windows) (underwater fog obscures high cameras); inspect actual images, record night vision and require null speaker output/PASS.
Every new placement/physical split requires a saved opt-in: 262 wraps complete 261, absent=legacy, rejecting partial/future foundations. Preserve aliases and explicit edits. Require aggregate `build`, resource/archive/classpath/documentation guards, focused codec/placement tests, independent frozen outputs/authentic reopen checks, native height/spacing/API witnesses and actual headless captures. Automated passes do not establish user visual acceptance.

Land-ice definition 1 adds a **bounded cold-alpine perennial icefield snapshot**, a partial [IUCN T6.1](https://jrfep.github.io/Ecosystem-profiles-comments/EFGs/T6.1) structural representation. [NSIDC](https://nsidc.org/learn/parts-cryosphere/glaciers/glacier-quick-facts) puts glacial ice near 10% of LAND, not Earth surface; neither game occupancy nor this subtype supplies that category's coverage. Continental sheets, warm/temperate ablation tongues, firn chronology, ice flow and microbial ecology remain missing. [NSIDC glacier science](https://nsidc.org/learn/parts-cryosphere/glaciers/science-glaciers) distinguishes accumulation/ablation and warns cold-arid ice can be lost by sublimation: cold mean alone is insufficient.
**Model assumptions, not empirical calibration:** reuse the existing local land sinusoid (half-range `4+16*abs(z/2097152)`°C); an additional bounded +4°C warm-spell convention must keep the modeled maximum below −1°C (full strength below −3°C). This is neither a measured daily range nor a universal Gaussian sigma/PDD budget. [Wake & Marshall 2015](https://doi.org/10.3189/2015JoG14J116) shows monthly approximations' within-month variability matters; [Fontrodona-Bach et al. 2026](https://doi.org/10.5194/hess-30-2613-2026) evaluates calibrated seasonal-snow models, not a universal glacier fit. Unitless atmospheric supply fades .45–.55; it is not snowfall, P/PET, salinity or net mass balance. Adequate multiyear net deposition is assumed only for this conservative snapshot; radiation, sublimation and wind redistribution are unmodeled. No elevation cutoff is imposed: current warm map belts make observed candidates alpine; synthetic cold non-alpine land is not categorically excluded.
**Prescribed geometry:** 128 m-spaced centers admit dry/unreserved circular 128 m footprints only with slope ≤.20 and a fitted-bed-plane residual ≤16 m on a 4 m mesh. Nine nearby thermal/supply probes plus block-resolution eligibility protect boundaries. The core target is a plane 32 m above the original center, quintic-tapered back to the original bed; overlapping bodies use union, never additive thickness.64m is a representation ceiling, not an estimate. Mixed edges retain exposed original terrain. Actual admitted thickness follows this geometry/suitability, not an invented glacier age or precipitation conversion. Preserve original regional climate/water references to avoid self-cooling feedback. Native geodes/rooms/fossils stay anchored below the pre-ice rock bed; prescribed solid bodies bury old approach openings and exclude ores/decorations within ice.
**Finite evidence and sensitivity:** fixed-grid-derived neighborhoods provide seven witness windows (±160 m, stride 4); windows may overlap; positive counts are sample observations, not disjoint occupancy or area. Three-seed maxima are 35/35/33 m. Warm-spell±1°C and supply-floor±.02 controls retain 704–699 positive samples in the first seed's fixed 1,323-point sensitivity set; slope .18/.22 admits 542/740 versus 704, and plane-fit 14/18 m admits 505/882. Geometry sensitivity is material and disclosed, not tuned away to hit a target. Counterexamples include cold arid rock, thawing seasonal snow, steep cold terrain and reserved ocean/coast/river/lake sites. Native qualification includes actual core/mixed-edge quart and visible-biome records; fuzzy zoom ownership can differ from block-resolution ice at a fringe.
Reproduce with `./gradlew --offline --no-daemon --no-parallel --max-workers=2 checkLandIce`; `assessLandIce` is a bounded diagnostic. Use fresh `-PlandIceDirectory=build/ice-NEW runLandIceQualification`, then `-PlandIceReopen` in the same directory. For legacy, copy the closed authentic public 262 `survey` world and add `-PlandIceLegacy`; never synthesize old metadata. [Original262 provenance](../src/integrationTest/resources/blockbranch-fixtures/land-ice-reader-262/ORIGIN.json) freezes all 2037 original main/client blobs, 43,200 grid samples, 758,345 physical / 7,757,764 native reads and profile hashes plus a real saved edit before production changes. Renamed readers share helpers; original whole-production hashes independently bound the recorded inputs. Packed ice and solid snow reuse permanent vanilla materials: literal depth/height APIs, support/neighbour/destruction updates, no random melting/source water and player-cleared ice persistence are explicitly checked; generation never regenerates edited ice. No new item/block registry catalog is needed.
Native comparisons use `ALSOFT_DRIVERS=null scripts/performance/run-headless-client.sh ./gradlew --offline --no-daemon --no-parallel --max-workers=2 --no-configuration-cache -x downloadAssets -PlandIceVisualDirectory=build/ice-visual-NEW runLandIceVisual`. For old matched views add `-PlandIceVisualLegacy` and copy a closed actual public 262 world to `saves/LandIceVisualReview`. Cameras use the same original bed datum; no staged terrain. Retain worlds,JSON/PASS,logs and inspected screenshots under ignored `build/`; master volume 0/null audio is required. Schema 263 wraps complete 262; absent retains all old placement/physical behavior including unexplored chunks, and profile 76/wire land-ice 95 barriers protect appended depth roles. Required aggregate/archive/classpath/documentation checks and independent review remain publication prerequisites.

Final schema263 survey:72 surface identities (22 custom/50 vanilla-named),12,033 land /31,126 connected ocean /41 inland water /0 unresolved,533 land fallbacks; no game footprint is added by treating ice as a terrestrial attribute. The ice identity is unobserved in all three32km-stride coarse samples but is demonstrably reachable at native core witnesses, not unreachable. Native75-chunk/19,200-column checks contain16,384 ice columns and2,816 rock columns,569,907 native/full-base/profile reads,5,100 additional clipped-column reads and5,400 height checks,65 profile76 and10 historical profiles. Cores resolve to the ice holder across selector/generated/visible paths; the tested1m fringe remains visible rockland under quart/zoom ownership. Actual packed-ice destruction leaves empty air/no water, support/lateral updates retain neighbouring ice, and the saved clearance/gold edits survive reopening; authentic old opening/reopening retains its original definition,whole-original hashes and zero generated ice. All observations are finite; no visual acceptance or Earth fraction follows.
**Deferred, low priority: Reduce vertical bounds and remove skipped Y-levels.** Queue this after the current biome tranche at a later suitable world-format/generator checkpoint; it is not the immediate next priority. The proposed binary target is vertical `[-16384,16384)` (32,768 total), replacing [ColumnLimits](../src/main/java/blockbranch/cubic/ColumnLimits.java)'s `[-262144,262144)` (524,288 total), not the horizontal radius. Begin by identifying and reproducing what “skipped Y-levels” means, resolving any material ambiguity with Oli before changing generation; [TerrainShape](../src/main/java/blockbranch/realistic/TerrainShape.java) already floors continuous heights to individual integer Y values, so do not assume sparse-section optimizations are the problem or remove them on that basis. Audit old saved blocks/edits outside the target, player/entity positions, persisted profiles, untouched-chunk generation, format/protocol barriers and global 22/20/22 position packing, heightmaps, light/streaming, portals/spawn and non-realistic Overworld handling before choosing compatibility behavior. The existing terrain envelope fits the target, but a constant-only bounds change could reject or strand saved data. Preserve old worlds and packed coordinates unless a reviewed migration is specifically authorized; never silently trim data. Treat performance effects as unmeasured until tested. Future qualification must include boundary-Y and native per-level witnesses, authentic old-world reopening and untouched-terrain checks. This is one deferred task, not authorization to implement a migration or height/generation change now.

Cool-desert definition 1 is a narrow [IUCN T5.4](https://www.bgci.org/wp/wp-content/uploads/2023/03/IUCN-global-ecosystem-typology-2.0.pdf) structural analog (printed p.65), not a quantified aridity class or exact crosswalk to shrub-steppe. It admits only final fallback with unitless moisture <.12 and mean −6≤T<5°C: existing rockland/steppe/warm-desert seams, not Earth-derived boundaries. A declared static land sinusoid has half-range `4+16*min(1,abs(z)/2097152)` °C; require winter below 0°C and at least three modeled months strictly above 10°C. Coefficients/thresholds are generator assumptions, not an empirical fit, a general land climate field or SeaIcePolicy's marine range. No simulated seasons, PET, rainfall units, salinity, fertility or soil-crust ecology are inferred. Physical support requires dry unreserved ground, slope ≤.30 and a supported 16 m footprint; a 2 km cardinal marine buffer is an inland proxy, not measured continentality. No altitude ban is imposed: specialist quart ownership and physical clipping exclude incompatible alpine/polar sites.
The fixed 43,200-point schema-259 grid contains 43 cool/arid land samples: 39 fallback and four scree. A physical screen retains 28; the thermal filter retains 26 (9/7/10 by seed), all previously fallback. The final schema-260 inventory has 71 surface identities (21 custom plus 50 vanilla-named); game footprints remain 12,033 land / 31,126 connected ocean / 41 inland water / zero unresolved water, with land fallback reduced from 559 to 533. The native cool-desert witness agrees across selector, generated quart, zoom and visible biome, with no override. Four identities remain unobserved across seeds (generic shelf, frozen river, snowy beach and swamp), not proven unreachable. Varying the thermal gradient ±10% admits 25–27 across the same fixed grid; negative controls cover year-round cool equatorial uplands, extreme cold, steppe/warm-desert seams, winter freezing and short growing seasons. These are model/placement checks, not empirical calibration or Earth-area coverage. New cover reuses <1 m silver shrubs, short grass and mineral/soil roles without cactus or deep sand: two native ±32-block windows (stride 1) contain 8450 owned columns, 170 shrubs, 69 short grasses, 8183 air cells above ground and 2747 pavement tops; all ground tops differ from old fallback. The mixed ±16-block edge at (399820,−1200512)/(399819,−1200512) contains 796 covered/293 uncovered columns, 47,916 state reads, 4756 base-column and 1968 base-height comparisons. Finite bounds do not prove global ecological fidelity.
Reproduce with `./gradlew --offline --no-daemon --no-parallel --max-workers=2 checkCoolDesert` and `-PcoolDesertDirectory=build/cool-NEW runCoolDesertQualification`, then `-PcoolDesertReopen` in the same directory. For legacy copy a closed original schema-259 `survey` world and add `-PcoolDesertLegacy`; never synthesize metadata. [Frozen public provenance](../src/integrationTest/resources/blockbranch-fixtures/cool-desert-reader-259/ORIGIN.json) identifies all 2023 original main/client blobs at public `831987b`, recorded before production changes: 43,200 grid samples, 16,875 physical and 168,959 native states, plus a saved edit. Renamed readers share unchanged helpers; the independently recorded full-production output oracle covers these finite inputs. Never refresh it from candidate output. Record an original with `-Ppublished259Directory=build/original-NEW runPublished259Snapshot` only against that complete public tree, with this digest-identified collector/development registration. Native captures use `ALSOFT_DRIVERS=null scripts/performance/run-headless-client.sh ./gradlew --offline --no-daemon --no-configuration-cache -x downloadAssets -PcoolDesertVisualDirectory=build/cool-visual-NEW runCoolDesertVisual`; require result-file PASS and inspect actual images, with no staged terrain or speaker output.

Surface ownership qualification uses `./gradlew --offline --no-daemon --no-parallel --max-workers=2 -PownershipDirectory=build/ownership-NEW runSurfaceOwnershipQualification`; repeat with `-PownershipReopen` in the same directory. For legacy, copy a closed actual published schema-257 world as `<directory>/survey`, then add `-PownershipLegacy`; it must contain the edit made by `PublishedSurfaceSnapshot`, never fabricated version fields. Run headless captures with `ALSOFT_DRIVERS=null scripts/performance/run-headless-client.sh ./gradlew --offline --no-daemon --no-configuration-cache -x downloadAssets -PownershipVisualDirectory=build/ownership-visual-NEW runSurfaceOwnershipVisual`. View 3 uses recorded night vision for the submerged bed; no staged terrain or speaker output.
Boundary qualification uses `./gradlew --offline --no-daemon --no-parallel --max-workers=2 -PsurfaceBoundaryDirectory=build/boundary-NEW runSurfaceBoundaryQualification`. Seed 424242, three fixed rockland witnesses ±64 blocks inclusive at stride 1: 49,923 columns / 243 native chunks / 349,461 native-versus-analytic states. It exercises 1,057 owner-edge columns, both debris depths, bare rock and snow. Rockland owns only cold/dry blocks within fallback-owned quarts; 16,445 physically eligible blocks in other quarts retain their owners, while 87 physical specialist sites inside fallback quarts intentionally follow rockland's material contract. No physically ineligible block inside a rockland quart occurred here; that clipping branch remains unexercised by these windows.
The same task checks three fixed marine neighborhoods ±8 chunks inclusive (867 chunks / 221,952 physical columns per gate, strides 1), comparing packed kelp against every positive root without the quart prefilter. Definition 0/1 has 897/2,024 roots and 578/293 prefilter-skipped chunks, with zero skipped roots; 20 sparsest candidate patches (including single-root chunks) pass 44,502 native bed/water/plant reads. Subquart plants are supported; physical hard-bed/shore exclusion remains mandatory. The retained prefilter has no demonstrated mismatch here, but these finite neighborhoods do not prove it sound for every seed/geometry or exercise a positive root outside quart membership. Results/bounds/witness coordinates are emitted as JSON/PASS under the selected ignored directory; no generation change follows from this qualification.
The [frozen public reader and baseline](../src/integrationTest/resources/blockbranch-fixtures/surface-ownership-reader-257) retain source/output digests and provenance in `ORIGIN.json`; ordinary builds verify them without Git/Python. Renamed compiled reader classes share today's helpers, so that comparison alone is not independent. `PublishedSurfaceSnapshot` separately recorded outputs in a native JVM with all 2,015 original main/client files verified against public `a41e39a`: 43,200 selector/terrain/climate/ownership samples, 3,594 physical reads and 23,825 native reads, plus an actual saved edit. Current gate-disabled checks compare those frozen hashes; they bound evidence to those inputs, not every possible world. Reproduction requires the original public tree and the collector identified by its digest, modifying only development registration/run setup. Never refresh frozen outputs from candidate code. Geometry, all 99 unowned-water cases, existing dry owners, malformed gates, original-reader rejection and actual surface materials also have explicit assertions.
No dataset geometry/content is imported. Before any future dataset import, verify its license (including One Earth terms); classification prose and source links do not grant data reuse rights.

## Native checks

Native runs are opt-in. Require the fixture's explicit result-file **PASS**, not just process exit. Use unique fresh directories for changed definitions, then repeat with the same directory for persistence checks. Preserve existing worlds; upgrade checks use copies of closed, authentic baseline worlds. A missing historic world directory creates today's preset, not the old definition being tested.
Run native/performance workloads serially to avoid port and CPU contention. Provision server EULA/configuration with the runner's authorization and choose a loopback port before launch. An empty directory is not server setup.
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

Both commands need matching route flags/directories. Graphical sessions can run the client directly. The headless wrapper uses installed X11/Mesa/Xvfb and installs nothing. Use a fresh Gradle JVM per display; keep Xvfb/client in one wrapper invocation so sandbox IPC namespaces do not break shared memory.
For Mesa sRGB/GLX failures use `SDL_VIDEO_FORCE_EGL=1 LIBGL_ALWAYS_SOFTWARE=1`. `-x downloadAssets` requires cached asset indexes, fonts and visual packs. Prevent speaker output unless the person running the test has allowed playback.

Route flags/screenshots live beside runs in `realistic.gradle` and [wildlife.gradle](../gradle/testing/wildlife.gradle). Examples include `plainsClientOnly`, `beachClientOnly`/`snowyBeachClient`, `tundraClientOnly`/`tundraFescueCloseup`, `steppeArtReview` and structure visual runs.
`runWildlifeSingleplayer -PsingleplayerDirectory=build/wildlife-NEW -PitemReview=true` captures staged icons/adults/resting/babies. Focused seal, mallard, food, perch and icon controls have mutual exclusions: read them first. Reloads require the saved world and matching witness UUID. Static poses/staged scenes establish neither natural locomotion nor acceptance.

Keep textures at native 16×16 and models deliberately simpler than textures; put fur/surface variation in pixels, not contour cuboids. Asset replacement/visual approval remain separate from automated test success.

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

Most Python utilities use the standard library. Ore art authoring needs Python 3 and Pillow (`generate_ore_art.py --out build/ore-art`); bismuth has a JDK-only `java scripts/assets/GenerateBismuth.java`. Audio candidate tools need the selected script's NumPy/SciPy/Matplotlib/FFmpeg prerequisites. Read source first; Gradle never installs or invokes those optional dependencies.

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
