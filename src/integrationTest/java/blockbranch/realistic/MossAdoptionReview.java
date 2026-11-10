package blockbranch.realistic;

import blockbranch.cubic.*;
import com.google.gson.GsonBuilder;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletionException;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;

/** Bounded adoption diagnostics; no changed production policy or on-disk corrupt chunks. */
public final class MossAdoptionReview {
    private static volatile Object sink;

    public static void verify(ServerLevel level, RealisticGenerator generator,
            List<ChunkPos> chunks, List<TerrainProfile> bare, Path directory) throws Exception {
        var factory = level.palettedContainerFactory();
        var profiles = new ArrayList<TerrainProfile[]>();
        for (int i = 0; i < chunks.size(); i++) {
            var c = chunks.get(i);
            var enabled = generator.profile(level.getChunk(c.x(), c.z()), level.getChunkSource().randomState());
            profiles.add(new TerrainProfile[]{bare.get(i), enabled});
            for (var profile : profiles.getLast()) {
                var tag = profile.encode(factory);
                require(tag.equals(TerrainProfile.decode(tag, factory).encode(factory)), "Descriptor round trip");
            }
        }
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        require(bean.isCurrentThreadCpuTimeSupported() && bean.isThreadAllocatedMemoryEnabled(), "Counters available");
        long tid = Thread.currentThread().threadId();
        var rows = new ArrayList<Map<String,Object>>();
        for (boolean occupied : new boolean[]{false, true}) {
            var selected = profiles.stream().filter(p -> (p[1].litCaves().placements().length > 0) == occupied).toList();
            require(!selected.isEmpty(), "Empty and occupied input coverage");
            for (String operation : List.of("attach", "encode", "decode")) {
                var tags = selected.stream().map(p -> new net.minecraft.nbt.CompoundTag[]{p[0].encode(factory), p[1].encode(factory)}).toList();
                for (int round = -1; round < 3; round++) for (int order = 0; order < 2; order++) {
                    int enabled = (round & 1) == 0 ? order : 1 - order;
                    long cpu = bean.getCurrentThreadCpuTime(), bytes = bean.getThreadAllocatedBytes(tid), wall = System.nanoTime();
                    int count = 0;
                    for (int repeat = 0; repeat < 32; repeat++) for (int i = 0; i < selected.size(); i++) {
                        var pair = selected.get(i);
                        sink = switch (operation) {
                            // Disabled attachment is intentionally a no-op: report absolute added cost, not its ratio.
                            case "attach" -> enabled == 0 ? pair[0] : pair[0].withLitCaves(pair[1].litCaves());
                            case "encode" -> pair[enabled].encode(factory);
                            case "decode" -> TerrainProfile.decode(tags.get(i)[enabled], factory);
                            default -> throw new AssertionError(operation);
                        };
                        count++;
                    }
                    if (round >= 0) rows.add(Map.of("occupied", occupied, "operation", operation, "round", round,
                        "enabled", enabled == 1, "profiles", count, "cpuNs", bean.getCurrentThreadCpuTime()-cpu,
                        "bytes", bean.getThreadAllocatedBytes(tid)-bytes, "wallNs", System.nanoTime()-wall));
                }
            }
        }
        sink = null;
        Files.writeString(directory.resolve("lit-adoption-cost.json"), new GsonBuilder().setPrettyPrinting().create().toJson(rows));

        // Exercise the actual transformed parser and native failure handler. Mutate only a copied NBT value.
        var c = chunks.getFirst();
        var tag = SerializableChunkData.copyOf(level, level.getChunk(c.x(), c.z())).write();
        tag.getCompoundOrEmpty("blockbranch:terrain").putInt("version", 67);
        IllegalArgumentException rejected = null;
        try { SerializableChunkData.parse(level, factory, tag); }
        catch (IllegalArgumentException failure) { rejected = failure; }
        require(rejected != null && rejected instanceof ColumnProfileException && rejected.getCause().getMessage().contains("Unsupported procedural terrain version"), "Actual parser rejects future profile");
        var map = level.getChunkSource().chunkMap;
        var handler = map.getClass().getDeclaredMethod("handleChunkLoadFailure", Throwable.class, ChunkPos.class);
        handler.setAccessible(true);
        // Isolated unrequested coordinate; returned ProtoChunk is never scheduled, saved or installed.
        var probe = new ChunkPos(100000, -100000);
        require(!level.getChunkSource().hasChunk(probe.x(), probe.z()), "Probe coordinate is unloaded");
        boolean blocked = false;
        try { handler.invoke(map, new CompletionException(rejected), probe); }
        catch (java.lang.reflect.InvocationTargetException failure) {
            blocked = failure.getCause() instanceof net.minecraft.ReportedException
                && failure.getCause().getMessage().contains("Refusing to replace");
        }
        require(blocked, "Native handler preserves rejection instead of returning replacement");
        Files.writeString(directory.resolve("lit-version-rejection.txt"),
            "PASS: actual parser rejection remains exceptional through the native failure handler.\n" +
            "Use column_save_safety for asynchronous disk loading, blocked saving and restart recovery.\n" +
            "The pre-fix EMPTY_REPLACEMENT observation remains preserved at fbbe766.\n");
        blockbranch.Blockbranch.LOGGER.info("MOSS_ADOPTION_DIAGNOSTIC_COMPLETE inputs={} unsupportedProfilePolicy=REJECTED", profiles.size());
    }

    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private MossAdoptionReview() {}
}
