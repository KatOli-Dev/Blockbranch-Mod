package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.*;

/** Current production selector, not a copied historical decision tree or an Earth-area estimate. */
public final class CurrentBiomeSurvey {
    private static final long[] SEEDS = {424242, 1, -17};
    private static final int MIN = -1920000, MAX = 1920000, STRIDE = 32000;
    private static final Set<String> CAVES = Set.of("lush_caves", "dripstone_caves", "sulfur_caves", "deep_dark");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private record Point(int x, int z) {}

    /** Newest reviewed generator schema. A later schema fails the survey until its gate is reviewed against the selector wiring below. */
    private static final int FIRST_REVIEWED_SCHEMA = 257, LAST_REVIEWED_SCHEMA = 279;
    /**
     * Wrapper schemas 272..279 and the definition each one adds. Reviewed 2026-11 against schema 279: production reads these definitions only as saved
     * vegetation flags (tree/canopy generation, saved tree-clear plans and their replay guards), never in the biome source, DrainageNetwork or
     * ClimateField that this survey reconstructs, so the surface selector, footprints and fallback sentinel do not depend on them. Presence
     * and exact coupling are still asserted so a stale or hand-edited preset cannot be surveyed under the wrong schema number.
     */
    private static final String[][] TREE_ONLY_WRAPPERS = {{"272", "boreal_slope_definition"}, {"273", "mangrove_roots_definition"},
        {"274", "broadleaf_crown_definition"}, {"275", "seasonal_crown_definition"}, {"276", "birch_crown_definition"},
        {"277", "pine_crown_definition"}, {"278", "coastal_oak_crown_definition"}, {"279", "humid_crown_definition"}};
    /**
     * Every biome-source gate the production generator can set, reviewed against {@link #source}: the nine surface gates below are replicated
     * from the saved preset; {@code withDeepDark} only selects the cave-only holder, which the inventory already excludes from surface biomes.
     */
    private static final Set<String> REVIEWED_SOURCE_GATES = Set.of("withMontaneForests", "withSeasonalForests", "withWarmXeric", "withSummerDryWoodlands",
        "withLandIce", "withCoolDeserts", "withDryForests", "withSurfaceOwnership", "withRedwoodForests", "withDeepDark");

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Path directory = Path.of(System.getProperty("blockbranch.currentBiomeSurveyDirectory"));
            try {
                survey(server.overworld(), directory);
                Files.writeString(directory.resolve("result.txt"), "PASS accounting and selector checks; no Earth-area or visual-acceptance claim\n");
            } catch (Throwable failure) {
                Blockbranch.LOGGER.error("CURRENT_BIOME_SURVEY FAIL", failure);
                try { Files.writeString(directory.resolve("result.txt"), "FAIL " + failure + "\n"); }
                catch (IOException ignored) {}
            } finally { server.halt(false); }
        });
    }

    private static void survey(ServerLevel level, Path directory) throws IOException {
        checkFootprints();
        checkFoundationReview();
        checkSourceGates();
        var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        JsonObject preset;
        try (var stream = Objects.requireNonNull(CurrentBiomeSurvey.class.getResourceAsStream(
                "/data/blockbranch/worldgen/world_preset/realistic.json"));
                var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            preset = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").deepCopy();
        }
        // Fail rather than quietly surveying a different generation foundation after a future bump.
        reviewFoundation(preset);
        var generator = (RealisticGenerator) level.getChunkSource().getGenerator();
        var random = level.getChunkSource().randomState();
        require(random.seed() == SEEDS[0], "Native witness world must use first survey seed");
        var encoded = RealisticGenerator.CODEC.codec().encodeStart(ops, generator).getOrThrow().getAsJsonObject();
        for (var entry : preset.entrySet()) {
            if (!entry.getKey().equals("type"))
                requireSettings(entry.getValue(), encoded.get(entry.getKey()), entry.getKey());
        }
        var palette = HabitatPalette.CODEC.codec().parse(ops, preset).getOrThrow();
        var m = palette.margins();
        // Fallback and intentional plains share one registry holder. A detached direct holder
        // tags only fallback returns, without editing production routing, registry or saved data.
        var sentinel = Holder.direct(m.fallback().value());
        var tagged = new HabitatPalette(new HabitatPalette.Margins(sentinel, m.scree(), m.reedbed(),
            m.riverMargin(), m.sandyShore(), m.lake(), m.riverChannel(), m.shelf(), m.deepOcean()), palette.land(), palette.additional());
        var inventory = new TreeSet<String>();
        var report = new JsonObject();
        report.addProperty("interpretation", "Game-grid selector samples only; not real-Earth coverage, ecology, or visual approval");
        report.add("preset", preset);
        report.add("seeds", JSON.toJsonTree(SEEDS));
        report.addProperty("bounds", "X/Z [" + MIN + "," + MAX + ") blocks; equal square cells, sampled at centers");
        report.addProperty("strideBlocks", STRIDE);
        report.addProperty("samplesPerSeed", ((MAX - MIN) / STRIDE) * ((MAX - MIN) / STRIDE));
        report.addProperty("denominators", "Disjoint land, freshwater, ocean, unresolved_water footprints; no water-column plus seabed sum");
        report.addProperty("footprintDefinitions", "Land: no open water above ground. Ocean: open water and DrainageNetwork.habitatOceanWater connectivity. Freshwater: open terminal-lake or routed-channel water, an inland-water proxy without modeled salinity. Other open water: unresolved_water, never assumed freshwater. Sea ice/icebergs retain marine ownership");
        report.addProperty("fallbackBinIntervals", "Lower-inclusive, upper-exclusive except temperature 19C and moisture .85 included in preceding bin. Elevation=groundY+1-64m. Bin dimensions overlap; never sum temperature plus elevation plus moisture");
        report.addProperty("reachability", "Not assessed exhaustively. Missing-in-sample means unobserved, never unreachable");
        report.addProperty("limitations", "32 km grid misses narrow/rare habitats and map edges; quart surface selector, not final generated palette. Native spot checks separate below; no ecology completeness inferred from a selected holder");
        var seedReports = new JsonArray();
        var aggregateTotals = emptyFootprints();
        var aggregateFallbacks = emptyFootprints();
        var fallbackBins = new TreeMap<String, TreeMap<String, Integer>>();
        try (var csv = Files.newBufferedWriter(directory.resolve("samples.csv"), StandardCharsets.UTF_8)) {
            csv.write("seed,x,z,footprint,selected_biome,fallback,height,water,mean_temperature_c,moisture,connected_ocean,terminal_lake,routed_channel,continental_ice_depth,permanent_freshwater_lid,atmospheric_supply,annual_model_rain_mm,coldest_model_month_c\n");
            for (long seed : SEEDS) {
                var drainage = seed == random.seed() ? generator.drainage(random) : detachedDrainage(preset, seed);
                var climate = new ClimateField(seed,preset.has("continental_ice_definition")&&preset.get("continental_ice_definition").getAsInt()==1);
                // Seeds 1 and -17 reuse the detached constructor path, so prove it equals the production drainage and climate for the saved settings on the
                // native seed's full grid (a direct comparison only for that seed).
                if (seed == random.seed()) report.addProperty("detachedFieldAgreementSamples", requireDetachedAgreement(preset, seed, drainage, generator.climate(random)));
                var spawnBounds=new SpawnRegions.Bounds(-2097136,-2097136,2097136,2097136).reserve(10);
                var spawn=SpawnRegions.select(seed,spawnBounds,drainage,climate);require(SpawnRegions.eligible(spawn.x(),spawn.z(),drainage,climate)&&spawnBounds.allows(spawn.x(),spawn.z()),"Current saved climate retains mild dry interior spawn policy");
                var source = source(palette, preset, seed, drainage, climate);
                var taggedSource = source(tagged, preset, seed, drainage, climate);
                source.possibleBiomes().forEach(b -> inventory.add(id(b)));
                // Cave-only holders must not be labelled absent surface biomes.
                for (String key : CAVES) inventory.remove(id(palette.additional(key)));
                var counts = new TreeMap<String, TreeMap<String, Integer>>();
                var fallbacks = emptyFootprints();
                var witnesses = new LinkedHashMap<String, Point>();
                var originalWitnesses = new HashMap<Point, Holder<Biome>>();
                int samples = 0;
                for (int z = MIN + STRIDE / 2; z < MAX; z += STRIDE) {
                    for (int x = MIN + STRIDE / 2; x < MAX; x += STRIDE) {
                        var site = drainage.sample(x, z);
                        boolean ocean = drainage.habitatOceanWater(x, z, site);
                        boolean lake = drainage.terminalLakeWater(x, z, site);
                        boolean channel = drainage.channelAt(x, z).edge() >= 0;
                        String footprint = footprint(site, ocean, lake, channel);
                        var selected = source.surfaceResolver().getNoiseBiome(x >> 2, 0, z >> 2);
                        var marked = taggedSource.surfaceResolver().getNoiseBiome(x >> 2, 0, z >> 2);
                        require(selected.value() == marked.value(), "Sentinel changed selected biome value");
                        boolean fallback = marked == sentinel;
                        counts.computeIfAbsent(footprint, unused -> new TreeMap<>()).merge(id(selected), 1, Integer::sum);
                        if (fallback) fallbacks.merge(footprint, 1, Integer::sum);
                        var c = climate.sample(x, z, drainage::sample);
                        if (fallback) {
                            var bins = fallbackBins.computeIfAbsent(footprint, unused -> new TreeMap<>());
                            int elevation = site.height() + 1 - TerrainShape.SEA_LEVEL;
                            bins.merge("elevation:" + (elevation < 0 ? "below_sea_level" : elevation < 500 ? "0_to_500m" :
                                elevation < 1500 ? "500_to_1500m" : elevation < 3000 ? "1500_to_3000m" : "at_least_3000m"), 1, Integer::sum);
                            double t = c.meanTemperatureC(), moisture = c.moisture();
                            bins.merge("temperature:" + (t < -14 ? "below_-14C" : t < -6 ? "-14_to_-6C" : t < 5 ? "-6_to_5C" : t <= 19 ? "5_to_19C" : "above_19C"), 1, Integer::sum);
                            bins.merge("moisture:" + (moisture < .12 ? "below_.12" : moisture < .27 ? ".12_to_.27" :
                                moisture < .55 ? ".27_to_.55" : moisture <= .85 ? ".55_to_.85" : "above_.85"), 1, Integer::sum);
                        }
                        double supply=climate.atmosphericSupply(x,z,drainage::sample);
                        csv.write(String.format(Locale.ROOT, "%d,%d,%d,%s,%s,%s,%d,%d,%.6f,%.6f,%s,%s,%s,%d,%s,%.9f,%.6f,%.6f%n",
                            seed, x, z, footprint, id(selected), fallback, site.height(), site.waterLevel(), c.meanTemperatureC(), c.moisture(), ocean, lake, channel, drainage.continentalIceThickness(x,z), drainage.permanentFreshwaterLid(x,z),supply,DrySeasonPolicy.annualModelRain(supply),CoolDesert.season(z,c.meanTemperatureC()).coldestC()));
                        witnesses.putIfAbsent(footprint, new Point(x, z));
                        if (fallback) witnesses.putIfAbsent("fallback", new Point(x, z));
                        else if (id(selected).equals("minecraft:plains")) witnesses.putIfAbsent("intentional_plains", new Point(x, z));
                        if (witnesses.containsValue(new Point(x, z))) originalWitnesses.put(new Point(x, z), selected);
                        samples++;
                    }
                    if ((z - MIN - STRIDE / 2) % (STRIDE * 30) == 0)
                        Blockbranch.LOGGER.info("CURRENT_BIOME_SURVEY seed={} samples={}", seed, samples);
                }
                var entry = new JsonObject();
                entry.addProperty("seed", seed);entry.add("preferredSpawn",new Gson().toJsonTree(spawn));
                entry.addProperty("samples", samples);
                var totals = emptyFootprints();
                counts.forEach((realm, biomes) -> totals.put(realm, biomes.values().stream().mapToInt(Integer::intValue).sum()));
                totals.forEach((realm, count) -> aggregateTotals.merge(realm, count, Integer::sum));
                fallbacks.forEach((realm, count) -> aggregateFallbacks.merge(realm, count, Integer::sum));
                require(totals.values().stream().mapToInt(Integer::intValue).sum() == samples, "Disjoint footprint accounting");
                require(samples == 14400, "Full fixed survey grid");
                entry.add("footprintDenominators", JSON.toJsonTree(totals));
                entry.add("selectedBiomesByFootprint", JSON.toJsonTree(counts));
                entry.add("fallbackByFootprint", JSON.toJsonTree(fallbacks));
                var observed = new TreeSet<String>();
                counts.values().forEach(map -> observed.addAll(map.keySet()));
                var absent = new TreeSet<>(inventory); absent.removeAll(observed);
                entry.add("missingInSampleNotUnreachable", JSON.toJsonTree(absent));
                // A repeat in reverse order catches mutable resolver/caching ownership errors.
                var reverse = new ArrayList<>(witnesses.values()); Collections.reverse(reverse);
                for (var point : reverse) {
                    var a = source.surfaceResolver().getNoiseBiome(point.x >> 2, 0, point.z >> 2);
                    var b = taggedSource.surfaceResolver().getNoiseBiome(point.x >> 2, 0, point.z >> 2);
                    require(a == originalWitnesses.get(point) && a.value() == b.value(), "Reverse witness reads retain original value");
                }
                if (seed == random.seed()) {
                    require(witnesses.containsKey("fallback") && witnesses.containsKey("intentional_plains"), "Fallback/plains attribution witnesses");
                    if(preset.has("surface_ownership_definition")){witnesses.put("basin_connected_coast",new Point(-1008000,-1776000));witnesses.put("cold_rockland_reference",new Point(-1328000,-1904000));}
                    if(preset.has("dry_forest_definition"))witnesses.put("dry_forest_reference",new Point(1648000,-208000));
                    if(preset.has("continental_ice_definition"))witnesses.put("polar_ice_reference",new Point(-1552000,-1904000));
                    if(preset.has("cool_desert_definition"))witnesses.put("cool_desert_reference",new Point(400000,-1200000));
                    witnesses.put("redwood_reference", new Point(-844018, -1692020));
                    entry.add("nativeWitnesses", nativeWitnesses(level, generator, drainage, source, witnesses));
                }
                seedReports.add(entry);
                Blockbranch.LOGGER.info("CURRENT_BIOME_SURVEY seed={} footprints={} fallback={}", seed, totals, fallbacks);
            }
        }
        report.add("surfaceInventory", JSON.toJsonTree(inventory));
        report.add("aggregateFootprintDenominators", JSON.toJsonTree(aggregateTotals));
        report.add("aggregateFallbackByFootprint", JSON.toJsonTree(aggregateFallbacks));
        report.add("fallbackClimateElevationBinsByFootprint", JSON.toJsonTree(fallbackBins));
        report.add("surveys", seedReports);
        Files.writeString(directory.resolve("survey.json"), JSON.toJson(report) + "\n");
    }

    /** The survey's own drainage for seeds the native world does not hold; its constructor arguments mirror the production terrain state. */
    private static DrainageNetwork detachedDrainage(JsonObject preset, long seed) {
        return new DrainageNetwork(
            new TerrainShape(seed), seed,DrainageNetwork.Options.DEFAULT.withSandyCoasts(preset.get("sandy_coasts").getAsBoolean()).withGravelRelief(preset.get("gravel_relief_definition").getAsInt() == 1).withSurfaceOwnership(preset.has("surface_ownership_definition") && preset.get("surface_ownership_definition").getAsInt() == 1).withDeepBasinRelief(preset.has("deep_basin_relief_definition") && preset.get("deep_basin_relief_definition").getAsInt() == 1).withLandIce(preset.has("land_ice_definition") && preset.get("land_ice_definition").getAsInt() == 1).withContinentalIce(preset.has("continental_ice_definition") && preset.get("continental_ice_definition").getAsInt()==1).withAlluvial(preset.has("alluvial_floodplain_definition") && preset.get("alluvial_floodplain_definition").getAsInt()==1));
    }

    /** Compares the detached drainage and climate construction with the production instances at every survey cell of the native seed; returns the cell count. */
    private static int requireDetachedAgreement(JsonObject preset, long seed, DrainageNetwork production, ClimateField productionClimate) {
        var detached = detachedDrainage(preset, seed);
        var climate = new ClimateField(seed, preset.has("continental_ice_definition") && preset.get("continental_ice_definition").getAsInt() == 1);
        int cells = 0;
        for (int z = MIN + STRIDE / 2; z < MAX; z += STRIDE) {
            for (int x = MIN + STRIDE / 2; x < MAX; x += STRIDE) {
                var a = production.sample(x, z); var b = detached.sample(x, z);
                String where = " at " + x + "," + z;
                require(a.equals(b), "Detached drainage sample differs from production" + where);
                require(production.habitatOceanWater(x, z, a) == detached.habitatOceanWater(x, z, b)
                    && production.terminalLakeWater(x, z, a) == detached.terminalLakeWater(x, z, b)
                    && production.channelAt(x, z).edge() == detached.channelAt(x, z).edge(), "Detached water ownership differs from production" + where);
                require(production.continentalIceThickness(x, z) == detached.continentalIceThickness(x, z)
                    && production.permanentFreshwaterLid(x, z) == detached.permanentFreshwaterLid(x, z), "Detached ice fields differ from production" + where);
                var ca = productionClimate.sample(x, z, production::sample); var cb = climate.sample(x, z, detached::sample);
                require(ca.meanTemperatureC() == cb.meanTemperatureC() && ca.moisture() == cb.moisture()
                    && productionClimate.atmosphericSupply(x, z, production::sample) == climate.atmosphericSupply(x, z, detached::sample), "Detached climate differs from production" + where);
                cells++;
            }
        }
        return cells;
    }

    private static ScreeBiomeSource source(HabitatPalette palette, JsonObject preset, long seed, DrainageNetwork d, ClimateField c) {
        var source = new ScreeBiomeSource(palette, preset.get("icebergs").getAsBoolean(), preset.get("sandy_coasts").getAsBoolean())
            .withRedwoodForests(preset.get("redwood_forest_definition").getAsInt() == 1)
            .withSurfaceOwnership(preset.has("surface_ownership_definition") && preset.get("surface_ownership_definition").getAsInt() == 1)
            .withDryForests(preset.has("dry_forest_definition") && preset.get("dry_forest_definition").getAsInt() == 1)
            .withCoolDeserts(preset.has("cool_desert_definition") && preset.get("cool_desert_definition").getAsInt() == 1);
        source.withMontaneForests(preset.has("montane_forest_definition") && preset.get("montane_forest_definition").getAsInt()==1);
        source.withSeasonalForests(preset.has("seasonal_forest_definition") && preset.get("seasonal_forest_definition").getAsInt()==1);
        source.withWarmXeric(preset.has("warm_xeric_definition") && preset.get("warm_xeric_definition").getAsInt()==1);
        source.withSummerDryWoodlands(preset.has("summer_dry_definition") && preset.get("summer_dry_definition").getAsInt()==1);
        source.withLandIce(preset.has("land_ice_definition") && preset.get("land_ice_definition").getAsInt()==1).bind(seed, d, c);
        return source;
    }

    private static JsonArray nativeWitnesses(ServerLevel level, RealisticGenerator generator, DrainageNetwork d,
            ScreeBiomeSource source, Map<String, Point> witnesses) {
        var output = new JsonArray();
        for (var witness : witnesses.entrySet()) {
            var p = witness.getValue(); var s = d.sample(p.x, p.z);
            int y = Math.max(s.height() + 1, s.waterLevel());
            level.getChunk(p.x >> 4, p.z >> 4);
            var pos = new BlockPos(p.x, y, p.z);
            var zoom = new PreviewBiomeQueries.Selector(level).select(pos);
            var raw = source.surfaceResolver().getNoiseBiome(p.x >> 2, y >> 2, p.z >> 2);
            var seeded = ((ScreeBiomeSource) generator.getBiomeSource()).surfaceResolver();
            require(raw == seeded.getNoiseBiome(p.x >> 2, y >> 2, p.z >> 2), "Detached/native current selector agreement");
            var zoomSurface = seeded.getNoiseBiome(zoom.x(), zoom.y(), zoom.z());
            var finalQuart = level.getNoiseBiome(p.x >> 2, y >> 2, p.z >> 2);
            var visible = level.getBiome(pos);
            var record = new JsonObject();
            record.addProperty("purpose", witness.getKey()); record.addProperty("x", p.x);
            record.addProperty("y", y); record.addProperty("z", p.z);
            record.addProperty("surfaceSelector", id(raw)); record.addProperty("generatedQuartBiome", id(finalQuart));
            record.addProperty("nativeZoomSurfaceSelector", id(zoomSurface)); record.addProperty("visibleBiome", id(visible));
            record.addProperty("quartOverrideObserved", !raw.equals(finalQuart));
            record.addProperty("visibleDiffersFromZoomSurface", !zoomSurface.equals(visible));
            record.addProperty("surfaceBlock", level.getBlockState(new BlockPos(p.x, s.height(), p.z)).toString());
            output.add(record);
        }
        return output;
    }

    static String footprint(DrainageNetwork.Sample s, boolean connectedOcean, boolean terminalLake, boolean routedChannel) {
        // Sea ice/icebergs retain the marine footprint. Wet inland columns count once,
        // even at sea-level altitude; closed basins are not inferred to be ocean.
        if (s.waterLevel() == DrainageNetwork.DRY || s.height() + 1 >= s.waterLevel()) return "land";
        return connectedOcean ? "ocean" : terminalLake || routedChannel ? "freshwater" : "unresolved_water";
    }
    private static TreeMap<String, Integer> emptyFootprints() {
        return new TreeMap<>(Map.of("land", 0, "freshwater", 0, "ocean", 0, "unresolved_water", 0));
    }
    private static void checkFootprints() {
        var wet = new DrainageNetwork.Sample(60, 64, 0, false);
        require(footprint(wet, true, false, false).equals("ocean"), "Connected marine water");
        require(footprint(wet, false, true, false).equals("freshwater"), "Sea-level terminal lake remains inland");
        require(footprint(wet, false, false, true).equals("freshwater"), "Routed inland water");
        require(footprint(wet, false, false, false).equals("unresolved_water"), "Unowned water is not assumed marine or fresh");
        require(footprint(new DrainageNetwork.Sample(63, 64, 0, false), true, false, false).equals("land"), "No water block above exposed ground");
        require(footprint(new DrainageNetwork.Sample(100, DrainageNetwork.DRY, 0, false), false, false, false).equals("land"), "Dry land");
    }
    /**
     * The survey accepts schemas FIRST_REVIEWED_SCHEMA..LAST_REVIEWED_SCHEMA. Schemas 257..271 are accepted as before; each of 272..279 must
     * have exactly its own definition (set to 1) when the schema is at least its number and none above, so the chain is complete and ordered.
     */
    static void reviewFoundation(JsonObject preset) {
        int shape = preset.get("shape_version").getAsInt();
        require(shape >= FIRST_REVIEWED_SCHEMA && shape <= LAST_REVIEWED_SCHEMA, "Review survey foundation for new schema " + shape);
        for (var wrapper : TREE_ONLY_WRAPPERS) {
            boolean expected = shape >= Integer.parseInt(wrapper[0]);
            boolean present = preset.has(wrapper[1]) && preset.get(wrapper[1]).getAsInt() == 1;
            require(present == expected && (expected || !preset.has(wrapper[1])), "Schema " + shape + " and " + wrapper[1] + " disagree (wrapper " + wrapper[0] + ")");
        }
    }
    private static void checkFoundationReview() {
        for (int shape : new int[] {257, 271, 272, 275, 278, 279}) reviewFoundation(chain(shape));
        for (int shape : new int[] {256, 280, 1000}) requireRejected(() -> reviewFoundation(chain(shape)), "Unreviewed schema " + shape);
        var missing = chain(279); missing.remove("humid_crown_definition");
        requireRejected(() -> reviewFoundation(missing), "Schema 279 without its definition");
        var early = chain(278); early.addProperty("humid_crown_definition", 1);
        requireRejected(() -> reviewFoundation(early), "Schema 278 carrying the 279 definition");
        var off = chain(279); off.addProperty("pine_crown_definition", 0);
        requireRejected(() -> reviewFoundation(off), "Schema 279 with a wrapper definition switched off");
    }
    private static JsonObject chain(int shape) {
        var json = new JsonObject(); json.addProperty("shape_version", shape);
        for (var wrapper : TREE_ONLY_WRAPPERS) if (shape >= Integer.parseInt(wrapper[0])) json.addProperty(wrapper[1], 1);
        return json;
    }
    /** A new biome-source gate must be reviewed here, and replicated in {@link #source} if it changes the surface selector. */
    private static void checkSourceGates() {
        var found = new TreeSet<String>();
        for (var method : ScreeBiomeSource.class.getDeclaredMethods())
            if (method.getName().startsWith("with") && method.getReturnType() == ScreeBiomeSource.class) found.add(method.getName());
        require(found.equals(new TreeSet<>(REVIEWED_SOURCE_GATES)), "Review new or removed biome-source gate: " + found);
    }
    private static void requireRejected(Runnable action, String message) {
        try { action.run(); } catch (AssertionError expected) { return; }
        throw new AssertionError("Expected rejection: " + message);
    }
    private static String id(Holder<Biome> biome) { return biome.unwrapKey().orElseThrow().identifier().toString(); }
    private static void requireSettings(JsonElement expected, JsonElement actual, String path) {
        require(actual != null, "Missing native setting: " + path);
        // Discovery planners append cached placements to their saved objects during startup.
        // Verify every preset setting recursively, allowing only additional saved data.
        if (expected.isJsonObject() && actual.isJsonObject()) {
            for (var entry : expected.getAsJsonObject().entrySet())
                requireSettings(entry.getValue(), actual.getAsJsonObject().get(entry.getKey()), path + "." + entry.getKey());
        } else require(expected.equals(actual), "Native world/preset mismatch: " + path);
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private CurrentBiomeSurvey() {}
}
