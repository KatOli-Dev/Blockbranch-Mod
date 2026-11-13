# Save compatibility

Use matching Blockbranch builds on client and server. Back up the **entire world
directory**, including `sections3d`, metadata, entities and region files. Keep
the mod installed when reopening. Do not downgrade, force safe-mode resets,
remove markers, or edit version fields to bypass rejection. There is no automatic
migration to newer generation definitions. Historical reader support is not
permission to resave a world with an older build.

## Current formats and authority

| Contract | Current new Realistic world | Source of truth |
| --- | --- | --- |
| Generator `shape_version` | 275 (complete274 + seasonal dry-forest crowns);274 (complete273 + ordinary-forest oak crowns);273 (complete272 + arched mangrove roots);272 (complete271 + boreal slope tolerance);271 montane needleleaf structure;270 alluvial;269 seasonal forest; 265 ice only, 266 litter only, 267 both, 268 warm xeric | [preset](../src/main/resources/data/blockbranch/worldgen/world_preset/realistic.json), [codec](../src/main/java/blockbranch/realistic/RealisticGenerator.java) |
| Base geometry | 82 plus saved gravel/deep-basin relief and land ice 1 | [TerrainShape](../src/main/java/blockbranch/realistic/TerrainShape.java), [GravelReliefSchema](../src/main/java/blockbranch/realistic/GravelReliefSchema.java), [DeepBasinSchema](../src/main/java/blockbranch/realistic/DeepBasinSchema.java) |
| Terrain profile | 77 for perennial spans (leaf-litter definition 1 adds cover codes inside existing arrays, no new version); 76 for land-ice roles; 75 for appended benthic states; 74 for short reeds; 72 for appended boreal roles; otherwise retained foundation | [TerrainProfile](../src/main/java/blockbranch/cubic/TerrainProfile.java) |
| Column / terrain-window protocol | 70 / 71 | [ColumnPacket](../src/main/java/blockbranch/cubic/ColumnPacket.java), [TerrainWindowPacket](../src/main/java/blockbranch/cubic/TerrainWindowPacket.java) |
| World marker | 4, content catalog 21 (legacy metallurgy key), explicit decoration bit | [ColumnWorldFormat](../src/main/java/blockbranch/cubic/ColumnWorldFormat.java) |
| Ore / layout definitions | 15 / 9 | [OreLayer](../src/main/java/blockbranch/realistic/OreLayer.java), generator codec |

These numbers describe separate contracts: schema 265 does not mean geometry or
profile 265. Validation checks complete feature combinations, holders and exact
field types as well as versions. Unsupported combinations reject loading.

The preset enables underground geometry, approaches, bounded entry routes,
icebergs and sandy coasts; dripstone/lit-cave growth 2, cave-biome 1 and structures
3; discovery/waygates 1, shipwreck templates 2, huts/shelters/warm/cold/deep ruins 1;
native huts/igloos and ordinary bodies, native Overworld coverage and layout 9.
Wing, gallery, boreal crown, gravel relief, sanctuary/mansion replacement,
biome-transition, redwood, surface-ownership, dry-forest, cool-desert, abyssal-trace, deep-basin-relief, land-ice, summer-dry, continental-ice, seasonal-forest, alluvial-floodplain, montane-forest and boreal-slope definitions are all 1. Consult the preset for exact
serialized names and biome holders; changing its defaults does not promote saves.

## World barriers and recovery

Unmarked initialized vanilla saves cannot be imported. Marked format-1 saves
upgrade to 2 on opening. Versioned decoration (lit growth or dripstone 2) requires
marker 3 and persists its barrier before levels load. Reverted incompatible
generator settings reject. Metallurgy uses marker 4 with canonical catalog and
saved decoration fields, including for legacy and ordinary worlds: new items can
enter inventories/player edits even when their natural generation is disabled.
Known supported markers upgrade before levels load without changing generation.
Malformed, extra-field or future markers reject. Older readers must refuse worlds
with names/components they cannot preserve; never remove the marker to downgrade.

Unreadable expanded-world chunks fail loading instead of being silently replaced.
Affected coordinates cannot save until valid data is restored offline and the
world reopened; unaffected coordinates can save normally. This does not repair
corruption or constrain arbitrary older binaries/external writers. Preserve a
copy before recovery work.

Position packing is 22/20/22 bits in every dimension. Named block-state palettes
and packed positions retain their meanings through the content updates below.
Blockbranch is not a compatibility layer for other chunk, lighting, generator or
rendering mods. See [development limits](DEVELOPMENT.md#storage-limits-and-historical-evidence).

## Metallurgy catalogs and ore definitions

Content catalogs protect registered names/components independently of ore geometry.
Canonical catalogs 1–19 upgrade to 20, preserving the decoration flag, saved
ore/generator definitions and edits. Each preceding published reader rejects the
next catalog before loading levels, even when it understands the same ore field.
New materials can be crafted from supplied ingredients in old worlds; ores are
not backfilled and unexplored columns retain their saved ore definition.
Native item components preserve damage, enchantments, names and armor trims.

| Catalog | Added persistent content | Ore definition at introduction |
| --- | --- | --- |
| 1 | Tin, bronze ingots/storage and initial pickaxe | 3 |
| 2 | Remaining bronze hand tools | 3 |
| 3 | Four bronze armor pieces | 3 |
| 4 | Zinc and brass ingot/block/slab/stair/bars | 4 |
| 5 | Manganese, steel storage, five tools and four armor pieces | 5 |
| 6 | Tungsten, tungsten steel ingots and four mining tools | 6 |
| 7 | Nickel/chromium, stainless ingots/building set and pickaxe | 7 |
| 8 | Remaining stainless tools and four armor pieces | 7 |
| 9 | Lead and lead-alloy building sets | 8 |
| 10 | Fool's-diamond ores and inert dust | 9 |
| 11 | Silver/platinum building sets and silver pickaxe | 10 |
| 12 | Remaining silver tools and four armor pieces | 10 |
| 13 | Platinum-alloy ingot/storage and pickaxe | 10 |
| 14 | Remaining platinum-alloy tools and four armor pieces | 10 |
| 15 | Titanium, nickel alloy/storage and speed boots | 11 |
| 16 | Five titanium-alloy tools and remaining armor | 11 |
| 17 | Magnetite ores/fragments and attraction boots | 12 |
| 18 | Resonite ore/crystals/placeable block | 13 |
| 19 | Uranium materials/tips, tipped netherite weapons, radiation | 14 |
| 20 | Bismuth materials and refined steel pickaxe | 15 |

Ore descriptors are self-versioned. Definitions 0–14 keep their original meanings;
1–14 are checked against independently compiled frozen readers. Each addition
occupies remaining eligible stone/deepslate only, preserving earlier ore fields
and genuine diamonds. Depth is below local ground, not absolute world Y.

| Definition | Added field and depth in blocks |
| --- | --- |
| 0–2 | Historical disabled/original/revised native ore fields |
| 3 | Tin, 12–192 |
| 4 | Zinc, 24–216 |
| 5 | Manganese, 40–224 |
| 6 | Tungsten, 112–224 |
| 7 | Nickel, 48–208; chromium, 72–224 |
| 8 | Lead, 24–184 |
| 9 | Fool's diamond, 96–208 |
| 10 | Silver, 40–208; platinum, 128–224 |
| 11 | Titanium, 80–224 |
| 12 | Magnetite, 48–192 |
| 13 | Resonite, 48–208 |
| 14 | Uranium, 8,000–10,000, separate deep field |
| 15 | Bismuth, 24–192, peak 96 |

The uranium field preserves the shallow 224-block index and skips the empty gap
in section/height queries. Ore14 readers reject ore15. Catalog 21 and ore15 require
matching peers before sparse-state exchange despite unchanged numeric protocols.
The terrain channel is currently
`terrain_window_v71_canopy184_boreal16_reed74_landice95_litter96w28_ore15_metals21`; hello derives from it.
Both channels are required before custom state is sent, including before JOIN
rejection. Capabilities identify supported catalogs, not exact build identity or
negotiated backward compatibility. Pre-guard builds through `2ba0671` could share
a capability while disagreeing on canopy169; guarded builds reject them.

No new custom entity/player timer formats accompany metallurgy: magnetite uses
native dropped items/equipment; resonite cooldown uses scheduled block ticks;
radiation uses native effects/item components; bismuth markers expire client-side
and never persist. Uranium smithing preserves native weapon component patches;
bismuth refines existing steel without changing original steel stats/recipes.
Runtime sprite derivation is client-only: existing item/model/texture IDs,
pack overrides and saved definitions remain. It writes no world or texture files.

## Daylight clock default

The Overworld native clock defaults once to rate `1/15`: 24,000 clock ticks take
360,000 simulation ticks, or five hours at 20 TPS. Only a vanilla rate of 1 is
changed; existing non-default rates, current time, fractional progress and paused
state are retained. Simulation ticks and Nether/End clocks are unchanged.
A lagging server takes longer in wall time.

Native clock data stores/synchronizes the rate. `blockbranch:daylight_cycle`
saved data records that the default was considered. Later `/time rate`, pause,
resume, set and add commands remain native and persist without reapplying it.
`/time rate 0.06666667` explicitly restores the five-hour rate. Sleep uses native
wake-up markers. Back up both clock data and marker. Removing the mod does not
reset the native rate and is not a supported world downgrade. No generator/profile
or network-format change accompanies this default.

## Saved generator schemas

Missing legacy flags mean false/0: no underground means solid terrain; no
approaches/routes means old cave access; absent ore/dripstone/lit-growth/cave-biome
settings add nothing; absent icebergs stay off. Underground descriptors 1–2 retain
their geometry, while 3 adds bounded entry routes. Ore/dripstone definition 1
remains original. New balancing needs explicit versions, not restored player edits.

The matrix identifies historical saved combinations, **including unexplored
chunks**. It is not a recipe for editing metadata. Exact validation lives in
[StructureGenerationSchema](../src/main/java/blockbranch/realistic/StructureGenerationSchema.java)
and the generator/wrapper codecs.

| Schema | Introduced scope |
| --- | --- |
| 82 | Base terrain definition; optional saved cave/ore/iceberg settings remain distinct |
| 83 | Entrance moss barrier |
| 84 | Revised dripstone/moss formations |
| 85 | Lush and sulfur cave identities |
| 86–88 | Initial, calibrated and atlas structure definitions, respectively |
| 89 | Treasure only |
| 90 | Treasure plus shipwrecks |
| 91 | Waygates only |
| 92 | Treasure plus waygates |
| 93 | Treasure, waygates and shipwrecks |
| 94 | Wetland huts |
| 95 | Snow shelters |
| 96 | Warm shelf ruins |
| 97 | Cold-family shelf ruins |
| 98 | Deep cold-family ruins |
| 99 | Deep warm ruins |
| 100 | Native hut/igloo bodies, other saved custom bodies retained |
| 101 | Native ordinary models for wells, portals, beached wrecks and all four ocean-ruin families; retains native huts/igloos |
| 102, layouts only | Saved reimagined crypt, desert pyramid, jungle temple, mansion and monument sanctuary; coverage stays off |
| 102, coverage only | Additional native Overworld structures and gated deep-dark identity; layouts stay off |
| 103 | Both coverage and original definition-1 layouts, with one owner for each shared structure key |
| 104 | Layout definition 2 with revised jungle exterior; coverage stays off |
| 105 | Coverage plus layout definition 2; revised jungle exterior in new worlds |
| 106 / 107 | Mansion wings plus original layout definition 1, without / with coverage |
| 108 / 109 | Mansion wings plus layout definition 2, without / with coverage |
| 110 / 111 | Pyramid galleries plus original layout definition 1, without / with coverage |
| 112 / 113 | Pyramid galleries plus layout definition 2, without / with coverage |
| 114 / 115 | Mansion wings and pyramid galleries plus layout definition 1, without / with coverage |
| 116 / 117 | Mansion wings and pyramid galleries plus layout definition 2, without / with coverage |
| 118 / 119 | Layout definition 3 sanctuary stonework, without / with coverage |
| 120 / 121 | Sanctuary stonework and mansion wings, without / with coverage |
| 126 / 127 | Sanctuary stonework and pyramid galleries, without / with coverage |
| 128 / 129 | Sanctuary stonework, mansion wings and pyramid galleries, without / with coverage |
| 130 / 131 | Layout definition 4 crypt stonework, without / with coverage |
| 132 / 133 | Crypt stonework and mansion wings, without / with coverage |
| 138 / 139 | Crypt stonework and pyramid galleries, without / with coverage |
| 140 / 141 | Crypt stonework, mansion wings and pyramid galleries, without / with coverage |
| 142 | Foundation 141 plus explicit boreal crown definition 1 |
| 160 / 161 | Layout definition 5 mansion perimeter, without / with coverage |
| 162 / 163 | Mansion perimeter and wings, without / with coverage |
| 168 / 169 | Mansion perimeter and pyramid galleries, without / with coverage |
| 170 / 171 | Mansion perimeter, wings and pyramid galleries, without / with coverage |
| 172 | Foundation 171 plus explicit boreal crown definition 1 |
| 200 / 201 | Jungle replacement layout 7, without / with coverage |
| 202 / 203 | Jungle replacement and mansion wings, without / with coverage |
| 208 / 209 | Jungle replacement and pyramid galleries, without / with coverage |
| 210 / 211 | Jungle replacement, mansion wings and galleries, without / with coverage |
| 212 | Foundation 211 plus explicit boreal crown definition 1 |
| 213 | Complete foundation 212 plus explicit gravel relief definition 1 |
| 220 / 221 | Pyramid replacement layout 8, without / with coverage |
| 222 / 223 | Pyramid replacement and mansion wings, without / with coverage |
| 228 / 229 | Pyramid replacement and gallery flag, without / with coverage |
| 230 / 231 | Pyramid replacement, mansion wings and gallery flag, without / with coverage |
| 232 | Foundation 231 plus explicit boreal crown definition 1 |
| 233 | Complete foundation 232 plus explicit gravel relief definition 1 |
| 234 | Complete schema 233 plus independent sanctuary replacement definition 1 |
| 240 / 241 | Crypt replacement layout 9, without / with coverage |
| 242 / 243 | Crypt replacement and mansion wings, without / with coverage |
| 248 / 249 | Crypt replacement and gallery flag, without / with coverage |
| 250 / 251 | Crypt replacement, mansion wings and gallery flag, without / with coverage |
| 252 | Foundation 251 plus explicit boreal crown definition 1 |
| 253 | Complete foundation 252 plus explicit gravel relief definition 1 |
| 254 | Complete schema 253 plus independent sanctuary replacement definition 1 |
| 255 | Complete schema 254 plus independent mansion replacement definition 1 |
| 256 | Complete schema 255 plus independent biome transition definition 1 |
| 257 | Complete schema 256 plus independent redwood forest definition 1 |
| 258 | Complete schema 257 plus independent surface ownership definition 1 |
| 259 | Complete schema 258 plus independent dry-forest definition 1 |
| 260 | Complete schema 259 plus independent cool-desert definition 1 |
| 261 | Complete schema 260 plus independent abyssal-trace definition 1 |
| 262 | Complete schema 261 plus independent deep-basin-relief definition 1 |
| 263 | Complete schema 262 plus independent land-ice definition 1 and biome holder |
| 264 | Complete schema 263 plus independent summer-dry woodland definition 1 and biome holder |
| 265 | Complete schema 264 plus polar climate/continental-ice definition 1, grounded sheet holder and compatible static spans |
| 266 | Complete schema 264 plus independent leaf-litter definition 1 (no ice) |
| 267 | Complete schema 264 plus both continental-ice definition 1 and leaf-litter definition 1; read only under exactly both |
| 268 | Complete267 plus warm-xeric definition1 and its holder; absent retains every old schema/placement |
| 269 | Complete268 plus seasonal-forest definition1; existing dry-forest identity/roles, no new holder |

A saved schema number identifies exactly one set of definitions (265 polar only, 266 litter only, 267 both), and `FoundationSchema` reads it only under those definitions. Schemas 122–125, 134–137, 143–159, 164–167, 173–199, 204–207, 214–219,
224–227, 235–239 and 244–247 are unsupported. The 220–233 groups belong to the pyramid
replacement; 234 independently wraps complete 233. Reserved layout 6 and its
private schema 192 remain unsupported.

Schemas 94–101 build on the complete preceding structure combination. Schemas
through 100 keep their exact saved models and opt-outs; new schema-101 native
models do not replace old structures or loot. Saved shipwreck template definition
1 retains its original templates, rails/stairs and edits; definition 2 is a
separate supported revision, rejected by binaries that lack it.

## Profile wrappers and fresh selectors

Supported profile versions are **57, 58, 59, 60, 61, 62, 63, 64, 66, 68, 70, 72,
74, 75**, subject to descriptor validation, not a continuous range. Cave-biome
foundation 70 can retain descriptor 1 or coverage-enabled deep-dark descriptor 2.

- Canopy codes 1–168 retain meaning; 169 adds native acacia wood, 170–172 mangrove
  bark on Y/X/Z axes; 173–184 add waterlogged sediment traces. Eight-bit packing remains. The canopy surface validation
  window is now 128 blocks above ground; older readers may reject newer codes.
- Boreal surface roles 15/16 mean podzol / podzol with moss carpet. Saved 3/6 keep
  full moss blocks (with original carpet for 6). Only profiles containing 15/16
  use wrapper 72 with required `base_profile_version` in 60,61,62,63,64,66,68,70.
  Missing/unknown foundations, new roles in legacy versions and empty wrappers reject.
- Short reeds use wrapper 74 with `reed_base_profile_version` in
  60,61,62,63,64,66,68,70,72. A 72 foundation still requires its own base field.
  Literal stem counts/mud/water retain meaning; cover 2 can be two stems at depth
  one and cover 3 three stems at depth two. Nested/unused wrappers, stray fields,
  invalid foundations and short forms on legacy versions reject.
- The wrapper decoder retains permissions through cave-biome foundation recursion.
  Unchanged stacks keep their prior encoding. Older canopy-only peers (including
  builds through `3030816`) cannot use the new surface capability. Chunk-load/save
  guards reject affected profiles, not entire worlds before opening; this is not
  downgrade support or automatic repair. No generator/marker migration is implied.

Stored material/canopy/meadow/woodland arrays and player overlays remain literal
and authoritative. Harvested flowers, grass, moss, shrubs and cactuses are not
regrown, recolored or repaired on upgrade. Some **fresh selectors are unversioned**
and affect new columns of old worlds; their output/base-height queries can differ
from stored profiles and old/new boundaries can combine different appearances:

| Fresh selector | Scope; retained contracts |
| --- | --- |
| Savanna acacia | Revised scaffolds; no persisted historical selector |
| Sunflowers | Existing meadow role 10; wider stands/grass gaps; clearing/foundation fit unchanged |
| Lowland meadow | Adds existing flower roles 4–6 on lit, canopy-clear sward; original plants/solid heights retained |
| Flower Forest | Fuller roles 30–36 and species choices; no change to soil, routing, spacing or trees |
| Sandy Desert | Some dead-bush roles become cactuses; no new bare-site planting; old cactus sites/heights/halo reserved |
| Dry steppe | Reallocates bare-soil/short-dry-grass roles 1/3; gravel, shrub and dead-stem sites retained |
| Gravel desert | Redistributes bare roles 1–4; plant roles 5–7/supports fixed; sediment still two blocks deep |
| Boreal/tundra floor | Fresh moss roles 3/6 select existing 15/16; saved old moss remains |
| Tundra bare ground | Clusters roles 2/10; coarse dirt over dirt vs gravel over coarse dirt remains literal |

These are native substrate changes, not just textures: excavation/falling sediment,
planting, bonemeal, drops, pollination or rabbit-eligible sand can differ at changed
sites. Tundra plant locations/supports, grass/shrub/fescue cover, height and rabbit
floor eligibility stay fixed. No new profile/catalog/marker version is implied.

The low-end `PlanarMath.length` substitution for `Math.hypot` is another deliberate
unversioned change: rounding can differ by about 1 ulp and rare terrain cells by a
block. Saved-world fresh generation was not protected against that difference;
a full analytic pass is not proof of bit-identical historical terrain.

## Independent generation opt-ins

Missing generation flags retain the old behavior in unexplored chunks; none is an
automatic migration. Existing arrays/edits remain authoritative in every case.

| Saved definition 1 | Required complete foundation → schema | Meaning |
| --- | --- | --- |
| Boreal crown | 141→142, 171→172, 211→212, 231→232, 251→252 | Whorled crowns and saved clearing footprint |
| Gravel relief | 212→213, 232→233, 252→253 | Bounded ±3-block dry-interior relief |
| Sanctuary replacement | 233→234, 253→254 | Independent native monument treatment |
| Mansion replacement | 254→255 | Native mansion architecture/roof/approaches |
| Biome transition | 255→256 | Temperate stand-edge feathering |
| Redwood forest | 256→257 | Maritime redwood/sequoia province |
| Surface ownership | 257→258 | Basin-owned marine habitats and cold exposed rockland |
| Dry forest | 258→259 | Idealized precipitation seasonality and a regional broadleaf forest analog |
| Seasonal forest | 268→269 | Additional source-inspired regional seasonal broadleaf ownership on existing forest domains |
| Cool desert | 259→260 | Supported cool-arid fallback with static thermal suitability and sparse dwarf shrubs |
| Abyssal traces | 260→261 | Static waterlogged microrelief on existing fine marine beds |
| Deep basin relief | 261→262 | Attenuated bank-scale variation on gentle offshore floors |
| Land ice | 262→263 | Bounded prescribed perennial snow/ice body over original rock bed |
| Summer-dry woodland | 263→264 | Coastal winter-rain oak/shrub structure; no ground-height/water/mean-climate change |
| Leaf litter | 264→266 | 16 vanilla facing×segment litter states in reserved cover codes and patchy canopy-aware presence in two woodlands; no soil, plant, terrain, water or climate change |

Gravel eligibility uses immutable old terrain, excluding wet/specialist/Badlands
cells. Regional climate/drainage/water datums remain; profiles, base-state/height,
caves and foundation fitting share the adjusted surface. Partial/wrong foundations
reject. Authentic212 readers reject 213; rich actual 172/212 final-relief reopening
was not established by the earlier material-only replay.

Transition definition 1 feathers stand strengths .49–.61 around old .55 without
changing terrain/biome routing or wet/tropical/warm/upland owners. Meadow/woodland
numeric vocabularies remain distinct. Clearing definitions 1/2 retain old roots,
3/4 transition roots; odd/even selects legacy/whorled boreal crowns.

Redwood requires its biome holder and complete schema 256. It occupies low maritime
temperate woodland, replacing Dark Forest/Cherry/dappled variants, not Forest/Birch
arrays. Clearing definitions 5–8 add redwoods to 1–4 and reserve 144 blocks above
construction rather than 80; saved boxes are checked exactly. Partial plans replay
their saved root/crown definition regardless of current flags. Clearing removes
logs/leaves inside a selected immutable tree footprint, including player-placed
ones. Fallen logs and wildlife share the saved definition/domain gating.

Surface ownership definition 1 requires the cold-rockland holder and complete schema 257. Absent = retained legacy selection/cover, including unexplored chunks; partial, malformed and future combinations reject. It repairs submerged marine ecology using the basin that created sea-level water, while geometry, dry backshore/fringe reservations and structural ocean ownership retain their contracts. Cold rockland only replaces final fallback below mean −6°C, with supported debris and static retained snow, not glacial ice. Existing loose-scree/canopy roles and protocols retain their meanings. The original schema-257 reader refuses 258 before loading the new biome; no migration or palette rewrite occurs in older saves.
Dry-forest definition 1 requires its holder and complete schema 258; absent retains old climate/biomes/cover/canopy in saved and unexplored chunks. It adds a static idealized precipitation-seasonality analog only within existing warm-woodland ownership; old mean climate, hydrology, terrain, weather, material roles and protocols retain their contracts. The original schema-258 reader rejects 259; partial/malformed/future foundations reject without promotion.
Cool-desert definition 1 requires its holder and complete schema 259; absent retains the published selector and physical cover, including unexplored chunks. Only final cool/arid fallback may gain the sparse stony-desert analog; all earlier owners are preserved. Its local idealized annual temperature cycle does not change ClimateField samples, geometry, hydrology, weather, tree plans, material-role meanings or protocols. Original schema-259 readers reject 260; partial/malformed/future foundations reject. Native, base-column and base-height paths intersect selected quart ownership with actual physical eligibility.
Abyssal-trace definition 1 requires complete schema260. Absent retains old placement, including untouched terrain. It appends canopy codes173–184 without changing 8-bit packing or old code meanings. Only profiles containing these states wrap their historical foundation with version75 and `benthic_base_profile_version`; absent/invalid foundations, empty wrappers, unwrapped appended states and future/nested versions reject. The original schema260 generator and original profile reader reject261/75 respectively. Source water, bed materials, biome identity and geometry remain; trace blocks are static, noncolliding and noncollectible.
Deep-basin-relief definition 1 requires complete schema261; absent retains exact old terrain, climate references, substrate/trace placement and untouched-chunk generation. Partial/malformed/future combinations reject; the published261 generator refuses262. Only the inherited rounded bank-noise contribution is attenuated on confirmed deep marine floors, using original continuous geometry with a buffered smooth gradient response. Underlying basin/ridge/trench relief, water datums/routes/ownership and old detailed regional climate references remain. Physical bed slope, material selection and existing trace eligibility may consequently change. All native/profile/base-column/base-height paths consume the same adjusted surface. No new profile vocabulary, marker, content catalog, wire version or coordinate packing is introduced; saved block overrides remain authoritative. No existing world's definition is promoted or rewritten.
Summer-dry definition1 requires complete263 and its biome holder. Absence retains original selection, cover and crowns, including untouched chunks; partial/future/malformed foundations reject and the actual original263 codec rejects264. It splits only the established warm woodland after dry-forest precedence, preserving explicit edits and reusing existing canopy/floor state roles. Profile versions, catalog21/marker4, packing and wire IDs do not change; matching builds remain required. Saved tree-clear bit8 selects the new whole oak/shrub footprints in definitions17–32; definitions1–16 retain exact old roots/crowns/boxes. Native qualification checks original whole-production hashes/profile encodings, authentic old-world reopening plus untouched neighbors, player-cleared woody stems and cross-process compressed plan replay. No height-limit migration is included.
Leaf-litter definition1 requires complete264 and writes schema266 (267 together with continental ice, see `FoundationSchema`); absent retains the original single-state litter (facing north, one segment) and the original80%/86% summer-dry/dry-forest presence rolls in saved and unexplored chunks. Partial/future/malformed foundations reject, the published264 codec rejects 265–267, and `FoundationSchema` reads 265 (ice only), 266 (litter only) and 267 (both) only under exactly the definitions each names, so a polar 265 world is never read as litter nor a litter 266 world as polar. Reserved cover codes hold the block's 16 states: woodland 65–80 (podzol litter) and 81–96 (rooted-dirt litter), warm array 13–28 (podzol soil); each code's soil, plant height and plant presence equal its unvaried code's, so variation never moves ground or plants. Bamboo keeps single-state codes and swamp has no litter; windswept moss/grass keeps single-state codes, while `windswept_forest` shaded ground uses the woodland podzol code and is varied. The profile version is unchanged: a reader without these codes rejects the profile, and the terrain-window type id carries the catalog sizes. Saved variants and edits stay authoritative: a gate-on world saved with a break, a state exchange and a placed block reopened in a new process with all 9,216 sampled ground columns identical (`runLeafLitterReopen`), and a closed authentic 264 world reproduces the original build's native block hashes with the gate absent. The qualification families strip the gate, so no 266 reopen exists per family.

Land-ice definition1 requires complete262 and its biome holder; absence retains original terrain/biomes including unexplored chunks. Partial/future/malformed combinations reject; original262 readers reject263. New dry ice bodies preserve original water ownership/routes/regional climate references, with physical ground raised by stored thickness. Cold-steppe byte roles32–95 encode1–64m of new solid ice; old roles retain their meanings. Only profiles containing those roles wrap their exact historical foundation in76 plus `land_ice_base_profile_version`; empty/stray/unwrapped/future/nested wrappers reject. Native/base-column/base-height paths share the body, leaving caves/ores/decorations outside its solid volume; native subsurface features use the original rock-bed datum. Ice edits remain authoritative section overrides, including player-cleared air after reopen. Existing packed-ice/snow blocks add no registry entry or catalog revision. Marker4/catalog21, global22/20/22 packing and packet numbers70/71 remain; the terrain-window/hello ID appends landice95 so old peers cannot accept new depth roles. No height-range migration is included.
Explicit new-content edits remain section overrides: an old schema260 world can retain a command-placed waterlogged trace without changing its generation gate or base profile. The existing content catalog advances to21 (same marker4/22,20,22 packing; the retained field name is `metallurgy-catalog`). Opening with this build conservatively upgrades older canonical catalog markers before levels load, even when the generation gate stays0. The actual catalog20 reader rejects21; the wire catalog/hello also requires matching peers. Matching builds remain required; never downgrade an edited world to a build lacking the block registry entry. Native lifecycle checks cover exact saved properties, source water after reopen/neighbor changes, support removal and edited natural traces staying removed.
Tree-clear definitions 9–16 add the saved dry-forest root bit to 1–8; new plans replace the old warm-tree footprints with complete broadleaf footprints in that domain. Definitions 1–8 keep exact historical root/crown choices and boxes, even when replayed against a newer generator. Only redwood-bearing plans reserve the 144-block upper halo; otherwise it remains 80. Clearing still follows its saved immutable log/leaf footprint, including player-placed logs/leaves within it.

**Continental ice:** schema 265/definition 1 wraps complete 264; absence retains legacy climate, physical cover, water, edits and untouched generation. Partial/future/malformed settings reject, and the actual original 264 reader rejects 265. Original native/profile/climate/selector hashes plus a closed authentic saved-edit world were recorded before production changes; renamed readers still share helpers and are not an independent oracle. Fixed-seed helpers and spawn use the saved climate. New tree-clear plans encode polar climate in definitions 33–64; definitions 1–32 keep their saved legacy climate; retained old generators preserve their physical inputs and footprints. No old definition is promoted. Under a sheet, deep-dark, Ancient City/stronghold/mineshaft/trial-chamber rules and cave entry routes read the ice-free bed, and cave approaches plan nothing on or under a sheet; dripstone formations and the dripstone/sulfur cave-biome windows use the same rock datum (`TerrainProfile.habitatGround`, exactly `groundHeight` without a sheet), so rock under a sheet is labelled and decorated as in the ice-free world and nothing is labelled or placed in ice. Development worlds from before these unpublished corrections (commits up to `dfe24ff`) may hold an Ancient City, deep-dark mask, stronghold, mineshaft, trial chamber, cave approach, or sheet-covered dripstone formation/cave label new generation would not produce, and are not a supported contract. Schema 265 means polar geography only, 266 leaf-litter variation only and 267 both; neither definition changes the other's meaning (ice alters climate, ownership, water and the underground datum; litter varies only litter-block state) and no number may be reused for another set.

Only profiles with new grounded-body or exposed-freshwater spans wrap their historical base in profile 77 plus `perennial_ice_base_profile_version` and a 256-int `perennial_ice` descriptor. Empty, stray, nested, malformed or future wrappers reject; original readers refuse 77. Grounded spans end at the raised ground and replace buried inland water, while anchoring cave-void and ore geometry to the original rock bed. Decode threads the perennial layer through the foundation recursion so the foundation is validated against the rock datum; a version-77 profile holding a formation outside the rock-datum window is rejected rather than reinterpreted (labels are not stored and are re-derived from the datum). This is not a new underground ecology model. Exposed lids retain water geometry/source water and use the existing frozen-river biome as a compatibility analog. Marine ownership stays with its corrected basin source and the inherited marine-ice policy; no new marine grounding or calving model is implied. Snow blocks/packed ice use existing immutable material behavior and section edits remain authoritative. The window/hello path appends `polar77` so old peers cannot accept new spans; marker 4/catalog 21, global 22/20/22 packing and packet numbers 70/71 stay unchanged. Native full/clipped column/height, mixed-edge, explicit edit/reopen and authentic old-world checks must qualify the final candidate. Global vertical bounds and skipped-level work remain deferred.

Warm-xeric definition 1 wraps complete 267 in schema 268; missing/malformed gates, missing holder, incomplete foundations and future versions reject. Original 264/265/266/267 worlds retain saved and unexplored generation and edits. The new subtype only splits selected steppe/gravel/sandy-desert quarts and physically supported inland dry blocks; intentional/fallback plains and specialists remain owned as before. Existing desert roles 1–7 supply low shrubs, dry grass and mineral/soil cover, so profile/wire catalogs are unchanged. Actual old267 reader rejection of268 has not been exercised; its source bounds schema to267. Frozen output hashes plus authentic267 reopening bound compatibility evidence to recorded inputs.

Seasonal-forest definition1 requires complete268 and writes269. Absence preserves original selection, ground cover and crowns, including untouched chunks. Missing/malformed/future gates and incomplete foundations reject. The existing dry-forest holder gains eligible warm-woodland/sparse-jungle ownership; prior dry forest remains and savanna/specialists are excluded. Reused broadleaf/soil/litter roles leave profile, registry, catalog and wire versions unchanged. Saved clear bit32 creates definitions65–128; definitions1–64 retain their original climate bit16, regional dry-forest owner, roots, boxes and crowns even under a269 generator. New plans suppress former warm/sparse roots before rasterization and clear whole broadleaf footprints. Frozen original268 hashes and two authentic saved-edit world reopens verify recorded grids/native states/profiles; cross-process269 checks retain edits, exact profiles and compressed clear-plan replay. This is finite compatibility evidence; actual original268 reader rejection of269 has not been run (its source bounds268). No migration, phenology or climate/hydrology rewrite occurs.
Alluvial-floodplain definition1 requires complete269 and writes270. Absence retains original terrain, water, regional climate, selection and untouched generation; incomplete/malformed/future combinations reject. No migration or new biome/profile/wire/catalog code is introduced. Saved clear definitions retain their vegetation algorithms and read their immutable saved world's terrain gate: definition128 under269 keeps269 ground; under270 it uses270 ground. This does not promise the same clearing footprint across different physical worlds. A producer repair drops dry-bank metadata13/14 only when woodland already fully shadows it in scalar material evaluation: keeping both formerly rejected profile validation. The decoder stays strict, and existing valid profile encodings and visible scalar states are required to remain identical. Original rejected profiles are explicit compatibility-evidence holes, not claimed successful original native generation. Frozen269 fixtures record those rejections and the actual successful native subsets separately.

Montane-forest definition1 requires complete270 and its needleleaf holder, writing271. Absence retains original selector, physical cover, canopy and untouched generation; missing/malformed/future fields and incomplete foundations reject. No migration, mean-climate/terrain/hydrology change or new material/profile/wire/catalog code occurs. Saved clear bit64 creates definitions129–256; plans1–128 retain original root choices and halos even inside a new needleleaf stand. New plans suppress only old ordinary roots inside actual new ownership and clear whole connected needleleaf footprints. Frozen original270 [provenance](../src/integrationTest/resources/blockbranch-fixtures/montane-forest-reader-270/ORIGIN.json) identifies the complete pre-change production trees, collector, compact observables, control preset and external authentic-world archive. Two candidate readers preserve2235928 original native state reads,168 profiles, exact original selector/climate hashes, saved215/271-cell clearing footprints and edits. Two new271 worlds preserve184 profiles,2448475 natural native reads,538/393-cell new plans and edits across JVM reopens; all256 plan tags round-trip and old128 footprints remain equal to the270 view. These are finite witnesses; an actual original270 runtime rejection of271 has not been executed (its source codec bounds270).

Boreal-slope definition1 requires complete271 and writes272. The woodland palette admits ground to a 0.40 slope, but published stems needed every 8 m footprint sample within 3 blocks of the stem (failing above roughly 0.27), so steep palette ground kept its label with no stems. Saved clear bit128 (definitions257–512) selects relief5 (`ceil(0.40*8*sqrt2)`); definitions1–256 and every world without the definition keep relief3 and its exact roots, including unexplored chunks of published 270 and montane 271 worlds. Tolerance only adds stems: published roots are a subset of tolerant roots, flat windows are identical, and no crown voxel enters terrain (checked on five frozen windows). Terrain, water, routing, biome selection and profile/wire/catalog versions are unchanged. Boreal roots are not filtered by the montane owner, so the guarantee is domain disjointness (montane eligibility needs mean temperature at least 6 C, boreal woodland below 3 C; the steepest 4-block temperature step sampled along x and z in seven windows implies a gap above 200 blocks there), tested on sampled cells and natively under both gates with additions-only canopy and clear-plan checks. Other tree families keep their own relief rules. Frozen published-270 oracle hashes, a pristine authentic 270 world and an authentic montane-271 world reopened by the 272 build, and a fresh 272 world reopened in a second process with a saved clear plan and an edit bound the evidence to recorded inputs.

Mangrove-roots definition1 requires complete272 and writes273. The published stilt roots ran from the trunk base to one shared knee at the stem height and then straight down, so a wet stand's roots formed a coplanar shelf with square vertical legs. The gated roots leave the trunk at a per-root height (stem base to two blocks up), swing outward and drop as a thin face-connected arch to a ground foot 3–5.5 blocks out (published 2–5, which piled against the trunk); a root that would run face to face with an earlier root beyond the two-block trunk collar turns aside by up to three 0.3-radian steps (otherwise neighbours share their flat apex and read as a wall), while stems, root count, the grounded-path test, crown, propagules and every other material are unchanged. Absence retains the exact published columns, including unexplored chunks of published 270..272 worlds; missing/malformed/future gates and incomplete foundations reject. Mangrove canopy is generated, never part of a saved clear plan, so this gate has no saved flag bit and clear definitions stay 1..512. Four frozen published windows (three fixture seeds, the audited seed-1 swamp) guard that gate-absent output is bit-identical; the gate's own effect is bounded by pure morphology checks, not by an old-reader rejection run.

Broadleaf-crown definition1 requires complete273 and writes274, with saved vegetation flag256 (clear definitions513–1024). The published mature oak was a bare pole under one steep log bundle and one fused ovoid (median crown depth/width 1.09 on 620 oaks sampled at four seed-1 sites). The gated oak divides the leader below the apex into 3–6 oblique limbs, each carrying its own lobe (large limbs fork a smaller twig lobe), with the apex on a short side limb. It applies only to mature non-birch family-1 oaks that `WoodlandTrees.roots` produces (ordinary and flower forest); cold-upland and badland oaks share family 1 but keep the published crown; birches, birch-led oak recruits, young stems and families that reuse the forest tree (dappled, dark/cherry, dry, summer-dry, windswept) keep their published voxels too, as do placement, spacing, height cohorts, materials and every other generator (fallen deadwood is accepted against the grown trees, so a few logs near gated oaks can differ). Tree-clear plans replay the saved generator's trees, so a plan carries the bit and erases the grown footprint; plans without it replay the published oak even for a gated world, and never-explored chunks of published270..273 worlds keep the published oak. Missing/malformed/future gates and incomplete foundations reject. Frozen published windows (15 rows: five sites, three flag sets, 5×5 chunks) were produced from the unmodified 6083e33 classes compiled separately and guard bit-identical gate-absent output. Measured on the same 620 stems: top within one block, median leaf count 509→524, limb wood 59→67 cells, median width 10→12 and depth/width 0.92, every log face-connected to the trunk base, leaf-plus-log voxel count comparable (about 600 per oak; no separate equality statistic was measured) and oak construction 57→64 µs on a warm JIT. This is automated geometry evidence plus native captures that are inspected, not user acceptance; no frame-rate claim is made.

Seasonal-crown definition1 requires complete274 and writes275, with saved vegetation flag512 (clear definitions1025–2048). It reshapes only mature family-18 stems of `DryForestTrees` (the whole `seasonal_dry_broadleaf_forest` biome, whether the regional or the seasonal-expansion owner placed them), reusing the oblique-limb crown with a wider reach, two extra limbs and smaller lobes; schema 274 oaks (family1) keep their exact arithmetic. Young stems, placement, spacing, height cohorts, materials, understory and every other family keep the published voxels. Tree-clear plans replay the saved generator's trees, so a plan carries the bit; plans without it replay the published crown, and never-explored chunks of published270..274 worlds keep the published crown. Missing/malformed/future gates and incomplete foundations reject. Frozen published windows (11 rows from the unmodified 4dc1b4a classes) guard bit-identical gate-absent output. On the pure check's mature stems (1049 across its sites) median leaf mass is 0.97× the published crown, limb wood 1.23×, median width 10→12 and crown box fill 0.40→0.32. This is automated geometry evidence plus inspected native captures at one scene: the change is modest and domed crowns remain; not user acceptance, no frame-rate claim.

Each generator schema number and each saved vegetation flag bit has exactly one meaning forever: a saved clear plan stores only the numeric definition, and only the world's saved generator names the feature that produced it, so plans are never portable across builds with different gates. A feature that finds its number or bit already taken renumbers itself (next schema number wrapping the complete earlier one, next flag bit with the clear ranges widened) instead of reusing it. Schema 271 and clear bit64 mean montane needleleaf structure only; schema 272 and bit128 mean boreal slope tolerance only; schema 273 means arched mangrove roots only and owns no flag bit; schema 274 and bit256 mean ordinary-forest oak crowns only; schema 275 and bit512 mean seasonal dry-forest crowns only. An unreleased draft of the boreal change wrote 271 with bit64 before the montane merge; worlds written by that draft are unsupported: the codec rejects them at load (the draft's saved world was opened by the final montane build and refused, and `fresh272` rejects mutated schema/definition JSON, with the run output not committed), and that rejection is not a migration path or a promise to read them later. `checkSavedGateRegistry` scans production sources and fails on a duplicate wrapper-schema `VERSION` (classes with `BASE` and `VERSION`) or `FLAG` bit; it does not cover the older multi-number schemas or inline literals in `vegetationFlags()`.


## Structure piece contracts

Coverage and layouts are separate saved opt-ins. The two historical schema 102
formats are exclusive and distinguished by explicit flags; neither enabled stays
through 101, both layout1+coverage requires 103, layout2 alone 104 and with coverage 105.
Malformed/future flags, missing holders or inconsistent schema/flag pairs reject.
A damaged derived region cache can rebuild without discarding saved feature flags.
Ordinary/coverage-only native decoration seed order and Nether/End stay native.
Shared keys have one placement/decoration/locate owner. Existing 101 worlds retain
both opt-outs; exclusive 102 worlds retain the other opt-out.

Schemas 94–101 build on complete preceding structures. Native 101 bodies do not
replace saved older models/loot. Shipwreck templates 1/2 remain distinct. Layouts
1–5, 7–9 preserve earlier recipes; reserved 6/private 192 stays unsupported.
Piece fields have field-specific legacy defaults: absent `JungleStonework` means
original definition 1, while absent wings/gallery/replacement opt-ins remain off.
Historical readers do not all enforce exact tag types (for example, wing fields
use native integer defaults); do not infer uniform validation from this summary.
New geometry uses saved immutable blueprints and only its owned chunk clip, not
neighboring generated chunks. Existing geometry, loot, residents and edits persist.

| Field / owner | Persistent meaning |
| --- | --- |
| `JungleStonework: 2` | Layout 2+ jungle exterior; same native core/bounds/routes/loot/traps |
| `WingDefinition: 1`, `WingSteps` | Four bounded terrain-fitted approach lengths; native rooms/IDs/residents/service passage retained |
| `PyramidGallery: 1` (kind 2) | Gallery materials/light/entrance; native core, stairs, TNT/chests/archaeology retained |
| `SanctuaryStonework: 1` | Layout 3+ exterior; native child boxes, guardian bounds, gold, sponges, elders retained |
| `CryptStonework: 1` | Layout 4+ finish; original monster-room seed/completion ownership retained |
| `MansionPerimeter: 1` | Layout 5/7+ perimeter treatment, independent of saved wings; omitted old fields stay omitted |
| `JungleReplacement: 1` (kind 3) | Layout 7+ stepped temple; original native trap/air/stair/loot connectivity retained |
| `PyramidReplacement: 1` (kind 2) | Layout 8+ body; entry 6–16 (old 0–32), sampled at x10 (old x4) |
| `SanctuaryReplacement: 1` | Independent sanctuary wrapper; native fitted children and ownership retained |
| `CryptReplacement: 1` (kind 1) | Layout 9 body; marker bounds/basis/direction/seed and native preparation retained |
| `MansionReplacement: 1`, `MansionReplacementFloor` | Native template processor; saved floor validated near template |

Replacement selectors enforce integer 0/1 and their kind-specific ownership;
invalid types, future values and enabled selectors on wrong kinds reject.
Replacement zero fields are omitted on save; old missing fields do not opt in.
The sanctuary keeps route connectivity, gold/sponges/elders/emitter positions and
original natural solids/pier contacts. The crypt protects the room plus 121
immediate floor dependencies and deeper columns: invisible changes can alter
native conditional RNG, spawner and loot. Native monster-room decoration occurs
only in its owning start chunk, once, not also through ordinary decoration.

Mansion templates retain order, palette RNG, positions, furniture, markers,
block entities, loot/emitter/resident creation and floor planes at relative 0/8/19.
The compound marker additionally saves `MansionRoofPlan` (format 2),
`MansionEntrance`, `MansionEntryProfile` and `MansionGalleryApproaches`; enabled
recipes reject missing/malformed data, disabled ones omit it. Roof bounds allow
six extra blocks inside the admission envelope; horizontal ownership/service
passage remain. Approaches save heights once to their first unchanged contact.
Replay never resamples terrain. Detailed collision/light/RNG protection rationale
lives beside the structure implementation and native qualification assertions.

## Maps and verification limits

Treasure maps retain completed pixels and unfinished outline rows through
put-away/reopen. Older builds cannot resume new unfinished outlines. Locking an
unfinished map freezes its partial image; normal exploration follows completion.

Codec round-trips are not old-world qualification. Retained historical native
witnesses cover closed-world architecture, typed inventories/UUIDs, repairs,
consumed loot, residents and authentic old-reader rejection, with different scope
per feature. Original results and outstanding checks remain only in the private
archive at cleanup base `00ed9d68be8da84be783c97525892bb8cdc235ae`; that object
is not included in this public snapshot. No cleanup claims to rerun them.
Use [development guidance](DEVELOPMENT.md#native-checks) for required inputs and
fresh/reopen workflows. Automated success does not establish visual approval,
full gameplay surveying, unsupported downgrade safety or physical-device FPS.
