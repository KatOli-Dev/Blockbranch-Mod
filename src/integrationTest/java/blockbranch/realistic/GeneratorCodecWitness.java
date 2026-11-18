package blockbranch.realistic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Codec witness: decodes every registered Realistic world preset through {@link RealisticGenerator#CODEC},
 * re-encodes it, and decodes a fixed corpus of single-field mutations (each flat field absent, below,
 * inside and above its range; shape_version shifted). It records, per input, acceptance and a digest of the
 * canonical re-encoded JSON. The output depends only on the codec, so running it on two commits and
 * comparing {@code witness.txt} shows whether a codec change kept the saved contract. Error texts go to
 * {@code errors.txt} for reading; accumulated DFU messages are not part of the contract.
 *
 * <p>Uses only the public codec and JSON, so the same file compiles on older commits.
 */
public final class GeneratorCodecWitness {
    private static final String[] INT_FIELDS = {"ore_definition", "dripstone_definition", "lit_cave_growth_definition",
        "cave_biome_definition", "structure_definition", "boreal_crown_definition", "small_discovery_definition",
        "native_hut_igloo", "native_ordinary_structures", "native_overworld_structures", "mansion_wing_definition",
        "pyramid_gallery_definition", "gravel_relief_definition", "alluvial_floodplain_definition", "montane_forest_definition",
        "boreal_slope_definition", "mangrove_roots_definition", "broadleaf_crown_definition", "seasonal_crown_definition",
        "birch_crown_definition", "pine_crown_definition", "coastal_oak_crown_definition", "humid_crown_definition",
        "sanctuary_replacement_definition", "mansion_replacement_definition", "biome_transition_definition",
        "redwood_forest_definition", "surface_ownership_definition", "dry_forest_definition", "seasonal_forest_definition",
        "cool_desert_definition", "abyssal_trace_definition", "deep_basin_relief_definition", "land_ice_definition",
        "summer_dry_definition", "continental_ice_definition", "leaf_litter_definition", "warm_xeric_definition"};
    private static final String[] BOOL_FIELDS = {"icebergs", "sandy_coasts", "underground", "cave_approaches", "cave_entry_routes"};
    private static final String[] CACHE_FIELDS = {"treasure_cache", "desert_waygates", "shipwrecks", "wetland_huts",
        "snow_shelters", "warm_ruins", "cold_ruins", "deep_cold_ruins", "deep_warm_ruins", "reimagined_layouts", "structure_cache"};

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var dir = Path.of(System.getProperty("blockbranch.generatorCodecWitnessDirectory"));
            try {
                var ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
                var presets = new TreeMap<String, JsonObject>();
                server.registryAccess().lookupOrThrow(Registries.WORLD_PRESET).listElements().forEach(holder -> {
                    var stem = holder.value().createWorldDimensions().get(LevelStem.OVERWORLD).orElse(null);
                    if (stem != null && stem.generator() instanceof RealisticGenerator g)
                        presets.put(holder.key().identifier().toString(), RealisticGenerator.CODEC.codec().encodeStart(ops, g).getOrThrow().getAsJsonObject());
                });
                if (presets.size() < 10) throw new AssertionError("Too few Realistic presets: " + presets.keySet());
                var lines = new ArrayList<String>();
                var errors = new ArrayList<String>();
                int accepted = 0, rejected = 0;
                for (var entry : presets.entrySet()) {
                    var name = entry.getKey();
                    var json = entry.getValue();
                    lines.add(name + " encoded " + digest(json));
                    var inputs = new LinkedHashMap<String, JsonObject>();
                    inputs.put("as-encoded", json);
                    for (var field : INT_FIELDS) for (var value : new Integer[]{null, -1, 0, 1, 2, 3, 4, 15, 16}) inputs.put(field + "=" + value, with(json, field, value == null ? null : new JsonPrimitive(value)));
                    for (var field : BOOL_FIELDS) for (var value : new Boolean[]{null, false, true}) inputs.put(field + "=" + value, with(json, field, value == null ? null : new JsonPrimitive(value)));
                    for (var field : CACHE_FIELDS) inputs.put(field + "=absent", with(json, field, null));
                    int shape = json.get("shape_version").getAsInt();
                    for (int delta = -3; delta <= 3; delta++) inputs.put("shape_version" + (delta >= 0 ? "+" : "") + delta, with(json, "shape_version", new JsonPrimitive(shape + delta)));
                    for (var input : inputs.entrySet()) {
                        var result = RealisticGenerator.CODEC.codec().parse(ops, input.getValue());
                        var decoded = result.result();
                        if (decoded.isPresent()) {
                            accepted++;
                            lines.add(name + " " + input.getKey() + " OK " + digest(RealisticGenerator.CODEC.codec().encodeStart(ops, decoded.get()).getOrThrow()));
                        } else {
                            rejected++;
                            lines.add(name + " " + input.getKey() + " REJECT");
                            errors.add(name + " " + input.getKey() + " " + result.error().map(e -> e.message()).orElse("?"));
                        }
                    }
                }
                Files.createDirectories(dir);
                Files.write(dir.resolve("witness.txt"), lines);
                Files.write(dir.resolve("errors.txt"), errors);
                Files.writeString(dir.resolve("result.txt"), "PASS presets=" + presets.size() + " accepted=" + accepted + " rejected=" + rejected + "\n");
            } catch (Throwable error) {
                try { Files.createDirectories(dir); Files.writeString(dir.resolve("result.txt"), "FAIL " + error + "\n"); } catch (Exception ignored) {}
                error.printStackTrace();
            }
            server.halt(false);
        });
    }

    private static JsonObject with(JsonObject json, String field, JsonElement value) {
        var copy = json.deepCopy();
        copy.remove(field);
        if (value != null) copy.add(field, value);
        return copy;
    }

    private static String digest(JsonElement json) throws Exception {
        var bytes = canonical(json).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).substring(0, 16);
    }

    private static String canonical(JsonElement json) {
        if (json.isJsonObject()) {
            var text = new StringBuilder("{");
            new TreeMap<>(json.getAsJsonObject().asMap()).forEach((key, value) -> text.append(key).append(':').append(canonical(value)).append(','));
            return text.append('}').toString();
        }
        if (json.isJsonArray()) {
            var text = new StringBuilder("[");
            for (var value : json.getAsJsonArray()) text.append(canonical(value)).append(',');
            return text.append(']').toString();
        }
        return json.toString();
    }

    private GeneratorCodecWitness() {}
}
