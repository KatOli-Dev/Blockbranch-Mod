# Blockbranch

An experimental Fabric mod for **Minecraft 26.3 / Java 25**, extending Overworld
build height while retaining vanilla X/Z chunk ownership. It also provides an
optional large-scale **Realistic** world generator.

| Limit | Range |
| --- | --- |
| Overworld build Y | −262,144 through +262,143 |
| Nether / End build Y | Vanilla ranges |
| Packed X/Z, all dimensions | −2,097,152 through +2,097,151 |
| Maximum world-border extent | ±2,097,136 |

The upper build bound 262,144 is exclusive. Default vanilla and superflat presets
keep their normal terrain elevations; the extra vertical space starts empty.

## Build and install

Use Java 25 and the checked-in Gradle wrapper:

```sh
./gradlew build
```

Install the generated mod JAR from `build/libs/` (not the sources JAR), Fabric
Loader and Fabric API for Minecraft 26.3 on both client and server. Current pinned
dependencies are in [gradle.properties](gradle.properties); supported runtime
requirements are in [fabric.mod.json](src/main/resources/fabric.mod.json).
Dependencies/assets must be available locally or downloadable during setup.
See [DEVELOPMENT.md](docs/DEVELOPMENT.md) for native checks and headless captures.

## Realistic worlds

Choose **Realistic** when creating a new world. For a dedicated server, set
`level-type=blockbranch:realistic` before creating its world. The default preset
still uses vanilla terrain; Nether and End retain their vanilla generators.

One block represents one meter on every axis. Sea level is Y=64; water occupies
blocks through Y=63. Physical elevation is `surfaceBlockY + 1 - 64` meters.
Realistic terrain occupies Y=[−12288,9216), with an unbreakable lower floor as a
storage/game abstraction; the wider build range remains available.

Current work includes continental landforms, deep marine basins, routed rivers
and inland lakes, varied terrestrial habitats, rocky/sandy coasts, static
cold-season sea ice and open-water glacial iceberg fields. The earlier vanilla-name
pass is implemented; names do not measure real-world area or ecological completeness.
See the [coverage ledger and roadmap](docs/DEVELOPMENT.md#real-world-biome-coverage)
for represented analogs, missing subtypes and reproducible current-selector surveys.
Representative hardware walking performance and visual acceptance remain open;
see [development limits](docs/DEVELOPMENT.md#storage-limits-and-historical-evidence).

Generation remains experimental and incomplete. Shapes/drainage and seasonal
appearance are static approximations, without a full erosion, weather, ocean
circulation or salinity simulation. Unassigned regions retain fallback habitats;
new Realistic worlds include a rock-dominant cave field and selected surface
approaches continuing over existing floors into a nearby chamber or junction.
These bounded routes preserve rough side passages. New worlds also enable an initial
versioned layer of existing Overworld ores, with local-surface depth bands and a
buried preference for valuable ores. The slower-progression target remains a
balancing goal, not measured playtime. New worlds include an initial dripstone
foundation: vertical biome pockets, sparse rock facings and supported pointed
formations with vanilla interactions. Entrance routes remain clear; surface-based
ore eligibility and surface-based spawn lists are retained. This build
adds restrained lush identity around moisture-gated, directly skylit moss floors
and a deep rocky sulfur biome using native ambience. Dark interiors remain rocky;
no sulfur resources, new spawn lists, fluids or custom hazards are enabled.
Aquifers are deferred. New worlds include region-fitted native desert wells,
repairable ruined portals, buried treasure, beached shipwrecks, swamp huts, igloos
and shallow/deep warm/cold ocean ruins. Generic stone/deepslate boundaries
and the sulfur province proxy are not a full geology simulation. New worlds also
enable restored native Overworld coverage and five approved layouts: crypt, desert
pyramid, jungle temple, woodland mansion and ocean monument sanctuary. Coverage
includes ancient cities with gated deep-dark identity. Existing saved opt-outs
remain unchanged; see the separate compatibility reference below.

## Low-end devices

On a new installation, a device with few cores, little RAM and no dedicated GPU gets Minecraft's
FAST graphics and shorter view distances automatically; existing settings are left alone. Give the
game at least `-Xmx2G` in the launcher's JVM arguments on such devices (a 4 GiB machine's default
heap is about 1 GiB); the log warns when the heap is smaller. `-Dblockbranch.hardwareProfile=off`
disables the automatic defaults. Hardware results so far are emulated; see the
[measurement guidance](docs/DEVELOPMENT.md#low-end-device-measurements).

## Saves and compatibility

Read the separate [save-compatibility reference](docs/SAVE_COMPATIBILITY.md)
before opening or moving a world. It covers current formats, saved opt-ins,
client/server matching, backups and unsupported downgrades.

## Maintained references

- [Development, build and tests](docs/DEVELOPMENT.md)
- [Save compatibility](docs/SAVE_COMPATIBILITY.md)
- [Audio credits](src/main/resources/AUDIO_CREDITS.txt) and [source provenance](src/main/resources/AUDIO_SOURCES.json)
- [License](LICENSE)
