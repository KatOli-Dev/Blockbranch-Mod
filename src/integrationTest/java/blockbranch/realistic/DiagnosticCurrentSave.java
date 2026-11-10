package blockbranch.realistic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

/** Disposable current-format save fixture, not a structure or performance suite. */
public final class DiagnosticCurrentSave {
    private static final BlockPos EDIT = new BlockPos(8, 96, 8);
    private static final String EXPECTED = "diagnostic-expected-generator.json";

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                var level = server.overworld();
                Path root = server.getWorldPath(LevelResource.ROOT);
                boolean reopen = Boolean.getBoolean("blockbranch.diagnosticSaveReopen");
                if (!reopen) {
                    var definition = definition(level);
                    require(!Files.exists(root.resolve(EXPECTED)), "Never reset an existing fixture");
                    level.getChunk(0, 0);
                    require(level.setBlock(EDIT, Blocks.GOLD_BLOCK.defaultBlockState(), 3), "Create disposable saved edit");
                    Files.writeString(root.resolve(EXPECTED), definition.toString(), StandardOpenOption.CREATE_NEW);
                }
                var report = verify(level);
                report.addProperty("reopen", reopen);
                server.saveEverything(false, true, true);
                Files.writeString(Path.of(reopen ? "reopen.json" : "fresh.json"), report.toString());
                Files.writeString(Path.of("result.txt"), "PASS\n");
            } catch (Throwable failure) {
                blockbranch.Blockbranch.LOGGER.error("DIAGNOSTIC_CURRENT_SAVE_FAIL", failure);
                try { Files.writeString(Path.of("result.txt"), "FAIL " + failure); } catch (Exception ignored) {}
            } finally { server.halt(false); }
        });
    }

    private static JsonObject definition(ServerLevel level) {
        var generator = (RealisticGenerator) level.getChunkSource().getGenerator();
        var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        var result = RealisticGenerator.CODEC.codec().encodeStart(ops, generator).getOrThrow().getAsJsonObject();
        require(result.get("shape_version").getAsInt() == 101, "Current schema101");
        require(result.get("native_hut_igloo").getAsInt() == 1 && result.get("native_ordinary_structures").getAsInt() == 1, "Explicit current native definitions");
        var future = result.deepCopy(); future.addProperty("shape_version", 102);
        require(RealisticGenerator.CODEC.codec().parse(ops, future).error().isPresent(), "Unsupported future schema rejects");
        var missing = result.deepCopy(); missing.remove("native_ordinary_structures");
        require(RealisticGenerator.CODEC.codec().parse(ops, missing).error().isPresent(), "Missing schema101 definition rejects");
        return result;
    }

    public static JsonObject verify(ServerLevel level) throws Exception {
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        var actual = definition(level);
        require(actual.equals(JsonParser.parseString(Files.readString(root.resolve(EXPECTED)))), "Exact saved generator definition retained");
        require(level.getBlockState(EDIT).is(Blocks.GOLD_BLOCK), "Exact saved edit retained");
        ColumnSaveSafetySmoke.verifyStartupMarker(level,(RealisticGenerator)level.getChunkSource().getGenerator());
        var report = new JsonObject();
        report.addProperty("schema", 101);
        report.addProperty("nativeHutIgloo", 1);
        report.addProperty("nativeOrdinaryStructures", 1);
        report.addProperty("exactGeneratorDefinition", true);
        report.addProperty("savedEdit", true);
        report.addProperty("columnMarker4", true);
        report.addProperty("futureAndMissingDefinitionRejected", true);
        return report;
    }

    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
