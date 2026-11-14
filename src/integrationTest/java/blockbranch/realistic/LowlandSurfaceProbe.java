package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;

/**
 * Opt-in native readback for listed points of the world seed: raw selector vs final biome, existing
 * woodland/meadow eligibility conjuncts (with and without the moisture ceiling), native surface and
 * above-ground blocks in a local window, and the selector composition of a 8 km neighbourhood.
 * It reports observations only; it asserts nothing about ecology and changes no generation. Trunk, leaf
 * and plant columns are block-id heuristics ("log"/"wood"/"stem", "leaves", anything else within 3 blocks of ground).
 * Points file: {@code seed x z group survey_biome survey_moisture survey_T survey_elevation}.
 */
public final class LowlandSurfaceProbe {
    static final int HALF = 24, STEP = 2, RING = 4096, RING_STEP = 128;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Path dir = Path.of(System.getProperty("blockbranch.lowlandDirectory"));
            try {
                var level = server.overworld();
                var g = (RealisticGenerator) level.getChunkSource().getGenerator();
                var random = level.getChunkSource().randomState();
                long seed = random.seed();
                var d = g.drainage(random);
                var climate = g.climate(random);
                var source = (ScreeBiomeSource) g.getBiomeSource();
                var rows = new StringBuilder("seed,x,z,group,survey_biome,survey_moisture,survey_T,survey_elev,T,moisture,elev,water,bedrock_coast,channel,slope64,slope32,reserved,"
                    + "stand,oak_potential,oak_nonclimate,oak_climate,forest_potential,forest_nonclimate,redwood_province,redwood_actual,redwood_ceilingfree,swamp,plains,meadow_actual,humid,"
                    + "raw_selector,native_quart,native_biome,cover_meadow,cover_woodland,cover_steppe,cover_warm,cover_coldsteppe,cover_desert,cover_reedbed,cover_rivermargin,cover_sandyshore,screeDepth,"
                    + "win_columns,win_surface_hist,win_above_hist,win_trunk_columns,win_leaf_columns,win_plant_columns,ring_hist,ring_moist_gt85,ring_cells\n");
                int n = 0;
                for (var line : Files.readAllLines(Path.of(System.getProperty("blockbranch.lowlandPoints")))) {
                    var p = line.trim().split(" ");
                    if (p.length < 8 || Long.parseLong(p[0]) != seed) continue;
                    int x = Integer.parseInt(p[1]), z = Integer.parseInt(p[2]);
                    var s = d.sample(x, z);
                    var q = climate.sample(x, z, d::sample);
                    int elev = s.height() + 1 - 64;
                    boolean dry = s.waterLevel() == DrainageNetwork.DRY;
                    boolean channel = d.channelAt(x, z).edge() >= 0;
                    double slope32 = Math.hypot((d.sample(x + 32, z).height() - d.sample(x - 32, z).height()) / 64.0, (d.sample(x, z + 32).height() - d.sample(x, z - 32).height()) / 64.0);
                    boolean reserved = LandVegetation.reserved(d, x, z);
                    double stand = OakBirchWoodland.stand(seed, x, z);
                    boolean oakNon = !reserved && dry && !channel && stand >= .55 && slope32 <= .35;
                    boolean oakPot = OakBirchWoodland.potential(seed, x, z, d, climate);
                    boolean oakClimate = OakBirchWoodland.climateSuitable(q);
                    boolean forestPot = Forest.potential(seed, x, z, d, climate);
                    boolean forestNon = oakNon && stand >= .65;
                    double province = RedwoodForest.province(seed, x, z);
                    boolean redwoodCeilingFree = province >= .42 && elev >= 0 && elev <= 900 && q.meanTemperatureC() >= 8 && q.meanTemperatureC() <= 16 && q.moisture() >= .68 && oakNon && !forestNon;
                    var pos = new BlockPos(x, s.height(), z);
                    level.getChunk(x >> 4, z >> 4);
                    var raw = source.surfaceResolver().getNoiseBiome(x >> 2, 0, z >> 2).unwrapKey().orElseThrow().identifier();
                    var quart = level.getNoiseBiome(x >> 2, s.height() >> 2, z >> 2).unwrapKey().orElseThrow().identifier();
                    var biome = level.getBiome(pos).unwrapKey().orElseThrow().identifier();
                    var surfaceHist = new TreeMap<String, Integer>();
                    var aboveHist = new TreeMap<String, Integer>();
                    int columns = 0, trunk = 0, leaf = 0, plant = 0;
                    for (int dz = -HALF; dz <= HALF; dz += STEP) for (int dx = -HALF; dx <= HALF; dx += STEP) {
                        int wx = x + dx, wz = z + dz, gy = d.sample(wx, wz).height();
                        columns++;
                        surfaceHist.merge(id(level.getBlockState(new BlockPos(wx, gy, wz))), 1, Integer::sum);
                        boolean hasTrunk = false, hasLeaf = false, hasPlant = false;
                        for (int dy = 1; dy <= 40; dy++) {
                            var name = id(level.getBlockState(new BlockPos(wx, gy + dy, wz)));
                            if (name.equals("minecraft:air") || name.equals("minecraft:cave_air")) continue;
                            aboveHist.merge(name, 1, Integer::sum);
                            if (name.contains("leaves")) hasLeaf = true;
                            else if (name.contains("log") || name.contains("wood") || name.contains("stem")) hasTrunk = true;
                            else if (dy <= 3) hasPlant = true;
                        }
                        if (hasTrunk) trunk++;
                        if (hasLeaf) leaf++;
                        if (hasPlant) plant++;
                    }
                    var ring = new TreeMap<String, Integer>();
                    int ringCells = 0, ringWet = 0;
                    for (int dz = -RING; dz <= RING; dz += RING_STEP) for (int dx = -RING; dx <= RING; dx += RING_STEP) {
                        int wx = x + dx, wz = z + dz;
                        ring.merge(source.surfaceResolver().getNoiseBiome(wx >> 2, 0, wz >> 2).unwrapKey().orElseThrow().identifier().toString(), 1, Integer::sum);
                        if (climate.sample(wx, wz, d::sample).moisture() > .85) ringWet++;
                        ringCells++;
                    }
                    rows.append(String.join(",", p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7],
                        f(q.meanTemperatureC()), f(q.moisture()), String.valueOf(elev), String.valueOf(s.waterLevel()), String.valueOf(s.bedrockCoast()), String.valueOf(channel),
                        f(SlopeCorrection.slope(d, x, z)), f(slope32), String.valueOf(reserved),
                        f(stand), String.valueOf(oakPot), String.valueOf(oakNon), String.valueOf(oakClimate), String.valueOf(forestPot), String.valueOf(forestNon), f(province),
                        String.valueOf(RedwoodForest.contains(seed, x, z, d, climate)), String.valueOf(redwoodCeilingFree), String.valueOf(Swamp.contains(seed, x, z, d, climate)),
                        String.valueOf(Plains.contains(seed, x, z, d, climate)), String.valueOf(LowlandMeadow.contains(x, z, d, climate)), String.valueOf(HumidWoodland.contains(seed, x, z, d, climate)),
                        String.valueOf(raw), String.valueOf(quart), String.valueOf(biome),
                        String.valueOf(g.meadowCover(x, z, random)), String.valueOf(g.woodlandCover(x, z, random)), String.valueOf(g.steppeCover(x, z, random)), String.valueOf(g.warmCover(x, z, random)),
                        String.valueOf(g.coldSteppeCover(x, z, random)), String.valueOf(g.desertCover(x, z, random)), String.valueOf(g.reedbedCover(x, z, random)), String.valueOf(g.riverMarginCover(x, z, random)),
                        String.valueOf(g.sandyShoreCover(x, z, random)), String.valueOf(g.screeDepth(x, z, random)),
                        String.valueOf(columns), hist(surfaceHist), hist(aboveHist), String.valueOf(trunk), String.valueOf(leaf), String.valueOf(plant), hist(ring), String.valueOf(ringWet), String.valueOf(ringCells))).append('\n');
                    n++;
                }
                if (n == 0) throw new AssertionError("No points for seed " + seed);
                Files.writeString(dir.resolve("surface.csv"), rows.toString());
                Files.writeString(dir.resolve("result.txt"), "PASS " + n + " points seed " + seed + "\n");
            } catch (Throwable t) {
                try { Files.writeString(dir.resolve("result.txt"), "FAIL " + t + "\n"); } catch (Exception ignored) {}
            } finally { server.halt(false); }
        });
    }

    private static String id(net.minecraft.world.level.block.state.BlockState state) { return state.getBlock().builtInRegistryHolder().key().identifier().toString(); }
    private static String f(double v) { return String.format(Locale.ROOT, "%.4f", v); }
    private static String hist(Map<String, Integer> m) {
        var parts = new ArrayList<String>();
        m.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).forEach(e -> parts.add(e.getKey() + "=" + e.getValue()));
        return String.join(";", parts);
    }
    private LowlandSurfaceProbe() {}
}
