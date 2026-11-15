package blockbranch.realistic;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.block.Blocks;

/**
 * Native witnesses for the cold-belt scree label (schema 280). One class serves the published build and the
 * candidate build; it never references the gated classes directly, so it also runs on the unmodified 279 build.
 *
 * <p>Points file lines: {@code x z group explored} with group belt|scree|fallback|other. Modes:
 * <ul>
 * <li>{@code record279}: unmodified published build, fresh world; only {@code explored=1} points are generated and saved.</li>
 * <li>{@code baseline279}: candidate build, gate-absent baseline preset; every point is generated. Must equal the record.</li>
 * <li>{@code fresh280}: candidate build, bundled preset; belt points change label only, every other point is identical.</li>
 * <li>{@code legacy279}: candidate build reopening a copy of the closed record world: explored chunks keep their stored
 * labels (a saved marker block proves they came from disk), unexplored belt chunks generate with the gate absent.
 * Compared against the baseline279 digest, which equals the record on explored points.</li>
 * </ul>
 * The optional grid file ({@code x z} lines) records only the raw selector, which needs no chunk. Digest lines:
 * {@code x z group raw quart visible surfaceHash belt}. The surface hash covers block ids of a 49 m window from 4 m
 * below the drainage surface to 24 m above it.
 */
public final class ScreeBeltQualification {
    static final int HALF = 24, STEP = 2, BELOW = 4, ABOVE = 24;

    public static void register(String mode) {
        var s = new Object() { List<String[]> points; int next, n, perTick; BufferedWriter out; Path dir; RealisticGenerator g;
            net.minecraft.world.level.levelgen.RandomState random; Map<String, String[]> baseline = new HashMap<>(); boolean done; long seed; };
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                s.dir = Path.of(System.getProperty("blockbranch.screeBeltDirectory"));
                var level = server.overworld();
                s.g = (RealisticGenerator) level.getChunkSource().getGenerator();
                s.random = level.getChunkSource().randomState();
                s.seed = s.random.seed();
                var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
                JsonObject saved = RealisticGenerator.CODEC.codec().encodeStart(ops, s.g).getOrThrow().getAsJsonObject();
                int shape = saved.get("shape_version").getAsInt();
                boolean gated = saved.has("cold_scree_belt_definition") && saved.get("cold_scree_belt_definition").getAsInt() == 1;
                switch (mode) {
                    case "record279", "baseline279", "legacy279" -> require(shape == 279 && !gated, "Saved generator is the gate-absent 279 foundation: " + shape + " " + gated);
                    case "fresh280" -> require(shape == 280 && gated, "Fresh bundled preset writes 280 with the gate: " + shape + " " + gated);
                    default -> throw new AssertionError("Unknown mode " + mode);
                }
                s.points = new ArrayList<>();
                for (var line : Files.readAllLines(Path.of(System.getProperty("blockbranch.screeBeltPoints")))) {
                    if (line.isBlank()) continue;
                    var p = line.trim().split(" ");
                    require(p.length == 4 && Set.of("belt", "scree", "fallback", "other").contains(p[2]), "Malformed point line: " + line);
                    if (mode.equals("record279") && !p[3].equals("1")) continue; // unexplored in the published world
                    s.points.add(p);
                }
                var baselinePath = System.getProperty("blockbranch.screeBeltBaseline");
                if (baselinePath != null && !baselinePath.isBlank() && !mode.equals("record279"))
                    for (var line : Files.readAllLines(Path.of(baselinePath))) {
                        if (line.startsWith("#") || line.isBlank()) continue;
                        var p = line.split(" ");
                        s.baseline.put(p[0] + " " + p[1], p);
                    }
                writeGrid(server, s.dir, s.g, s.random, saved);
                s.out = Files.newBufferedWriter(s.dir.resolve("digest.txt"));
                s.out.write("# mode=" + mode + " shape=" + shape + " gate=" + gated + " seed=" + s.seed + "\n");
                s.perTick = 2;
            } catch (Throwable t) { fail(server, s.dir, t, s.n); s.done = true; }
        });
        // A few points per tick so unticketed chunks can unload instead of exhausting the heap.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (s.points == null || s.done) return;
            try {
                var level = server.overworld();
                var d = s.g.drainage(s.random);
                var climate = s.g.climate(s.random);
                var source = (ScreeBiomeSource) s.g.getBiomeSource();
                for (int k = 0; k < s.perTick && s.next < s.points.size(); k++) {
                    var p = s.points.get(s.next++);
                    int x = Integer.parseInt(p[0]), z = Integer.parseInt(p[1]);
                    var surface = d.sample(x, z);
                    var chunk = level.getChunk(x >> 4, z >> 4);
                    var raw = source.surfaceResolver().getNoiseBiome(x >> 2, 0, z >> 2).unwrapKey().orElseThrow().identifier().toString();
                    var quart = chunk.getNoiseBiome(x >> 2, surface.height() >> 2, z >> 2).unwrapKey().orElseThrow().identifier().toString();
                    var visible = level.getBiome(new BlockPos(x, surface.height(), z)).unwrapKey().orElseThrow().identifier().toString();
                    var hash = windowHash(level, d, x, z);
                    boolean belt = beltReplica(d, climate, x, z);
                    var row = new String[] {p[0], p[1], p[2], raw, quart, visible, hash, String.valueOf(belt)};
                    s.out.write(String.join(" ", row) + "\n");
                    compare(mode, s.baseline, row, (px, pz) -> changedQuartNear(source, d, climate, px, pz));
                    // A disposable saved edit above the hashed window proves explored chunks are read back from disk.
                    var marker = new BlockPos(x, surface.height() + 30, z);
                    if (mode.equals("record279") && p[2].equals("belt")) level.setBlock(marker, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                    if (mode.equals("legacy279") && p[2].equals("belt"))
                        require(level.getBlockState(marker).is(Blocks.GOLD_BLOCK) == p[3].equals("1"), "Saved marker present exactly in explored chunks at " + p[0] + "," + p[1]);
                    s.n++;
                }
                if (s.n % 20 == 0) { s.out.flush(); Files.writeString(s.dir.resolve("progress.txt"), s.n + "\n"); }
                if (s.next >= s.points.size()) {
                    s.out.close();
                    if (mode.equals("record279")) server.saveEverything(false, true, true);
                    Files.writeString(s.dir.resolve("result.txt"), "PASS " + mode + " " + s.n + " points; visible-only plains->scree zoom shifts at non-belt points: " + zoomShifts[0] + "\n");
                    s.done = true;
                    server.halt(false);
                }
            } catch (Throwable t) { fail(server, s.dir, t, s.n); s.done = true; }
        });
    }

    /** A quart within one cell of (x, z) that the gate relabelled: eligible by the restated rule and read as scree below the 2,200 m scree floor. */
    private static boolean changedQuartNear(ScreeBiomeSource source, DrainageNetwork d, ClimateField climate, int x, int z) {
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            int qx = (x >> 2) + dx, qz = (z >> 2) + dz;
            if (beltReplica(d, climate, qx * 4, qz * 4)
                && source.surfaceResolver().getNoiseBiome(qx, 0, qz).unwrapKey().orElseThrow().identifier().toString().equals("blockbranch:alpine_scree_slope")) return true;
        }
        return false;
    }

    /** Raw selector at survey-grid cells; needs no chunk. */
    private static void writeGrid(net.minecraft.server.MinecraftServer server, Path dir, RealisticGenerator g,
                                  net.minecraft.world.level.levelgen.RandomState random, JsonObject saved) throws Exception {
        var gridPath = System.getProperty("blockbranch.screeBeltGrid");
        if (gridPath == null || gridPath.isBlank()) return;
        var d = g.drainage(random);
        var climate = g.climate(random);
        var source = (ScreeBiomeSource) g.getBiomeSource();
        var rows = new StringBuilder();
        int n = 0, belt = 0;
        for (var line : Files.readAllLines(Path.of(gridPath))) {
            if (line.isBlank()) continue;
            var p = line.trim().split(" ");
            int x = Integer.parseInt(p[0]), z = Integer.parseInt(p[1]);
            boolean b = beltReplica(d, climate, x, z);
            if (b) belt++;
            rows.append(x).append(' ').append(z).append(' ')
                .append(source.surfaceResolver().getNoiseBiome(x >> 2, 0, z >> 2).unwrapKey().orElseThrow().identifier()).append(' ').append(b).append('\n');
            n++;
        }
        Files.writeString(dir.resolve("grid.txt"), rows.toString());
        Files.writeString(dir.resolve("grid-summary.txt"), n + " cells, " + belt + " belt-eligible\n");
    }

    /** The belt rule restated from its definition: dry ground 1500..2199 m, 64 m slope .30..1.20, mean below 5 C. */
    static boolean beltReplica(DrainageNetwork d, ClimateField climate, int x, int z) {
        x = Math.floorDiv(x, 4) * 4; z = Math.floorDiv(z, 4) * 4;
        var s = d.sample(x, z);
        int elevation = s.height() + 1 - 64;
        if (s.waterLevel() > s.height() || elevation < 1500 || elevation >= 2200) return false;
        double slope = Math.hypot((d.sample(x + 32, z).height() - d.sample(x - 32, z).height()) / 64.0, (d.sample(x, z + 32).height() - d.sample(x, z - 32).height()) / 64.0);
        return slope >= 0.30 && slope <= 1.20 && climate.sample(x, z, d::sample).meanTemperatureC() < 5;
    }

    private static String windowHash(net.minecraft.server.level.ServerLevel level, DrainageNetwork d, int x, int z) throws Exception {
        var md = MessageDigest.getInstance("SHA-256");
        for (int dz = -HALF; dz <= HALF; dz += STEP) for (int dx = -HALF; dx <= HALF; dx += STEP) {
            int wx = x + dx, wz = z + dz, gy = d.sample(wx, wz).height();
            for (int y = gy - BELOW; y <= gy + ABOVE; y++)
                md.update(level.getBlockState(new BlockPos(wx, y, wz)).getBlock().builtInRegistryHolder().key().identifier().toString().getBytes(StandardCharsets.UTF_8));
            md.update((byte) '|');
        }
        return HexFormat.of().formatHex(md.digest()).substring(0, 24);
    }

    static final int[] zoomShifts = new int[1];

    private static void compare(String mode, Map<String, String[]> baseline, String[] row, java.util.function.BiPredicate<Integer, Integer> neighbourChanged) {
        if (baseline.isEmpty()) return;
        var b = baseline.get(row[0] + " " + row[1]);
        require(b != null, "Baseline has the point " + row[0] + "," + row[1]);
        boolean belt = row[2].equals("belt");
        String where = row[0] + "," + row[1] + " " + row[2];
        require(row[6].equals(b[6]), "Surface and above-ground blocks identical to baseline at " + where);
        require(row[7].equals(b[7]), "Eligibility replica identical at " + where);
        if (mode.equals("fresh280") && belt) {
            require(b[3].equals("minecraft:plains") && b[4].equals("minecraft:plains"), "Baseline belt point reads plains at " + where);
            require(row[3].equals("blockbranch:alpine_scree_slope") && row[4].equals("blockbranch:alpine_scree_slope"), "Gated belt point reads scree on selector and stored quart at " + where + ": " + row[3] + " " + row[4]);
            require(row[7].equals("true"), "Belt point is eligible at " + where);
        } else {
            require(row[3].equals(b[3]) && row[4].equals(b[4]), "Raw selector and stored quart identical to baseline at " + where + ": " + String.join(",", row[3], row[4]) + " vs " + String.join(",", b[3], b[4]));
            // The zoomed visible biome blends neighbouring quarts, so beside a relabelled belt cell it may move from plains to scree and nowhere else.
            if (!row[5].equals(b[5])) {
                require(mode.equals("fresh280") && b[5].equals("minecraft:plains") && row[5].equals("blockbranch:alpine_scree_slope"), "Visible difference is only plains->scree at " + where + ": " + row[5] + " vs " + b[5]);
                require(neighbourChanged.test(Integer.parseInt(row[0]), Integer.parseInt(row[1])), "Visible-only change has no changed belt quart within one cell of " + where);
                zoomShifts[0]++;
            }
        }
    }

    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    private static void fail(net.minecraft.server.MinecraftServer server, Path dir, Throwable t, int n) {
        try { Files.writeString(dir.resolve("result.txt"), "FAIL after " + n + " " + t + "\n"); } catch (Exception ignored) {}
        server.halt(false);
    }

    private ScreeBeltQualification() {}
}
