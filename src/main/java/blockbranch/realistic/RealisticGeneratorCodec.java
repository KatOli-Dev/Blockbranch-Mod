package blockbranch.realistic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Saved form of {@link RealisticGenerator}. Every field is flat and optional except the palette and
 * {@code shape_version}; absent fields decode to the published defaults, so older saves and presets keep
 * their behavior. Field names are a permanent save contract.
 *
 * <p>The {@link GenerationGate} fields are read by one table-driven codec instead of nested record codecs,
 * which avoids RecordCodecBuilder's arity limit without changing the flat saved layout. Each field still
 * uses the same DFU primitive as before ({@code intRange(..).optionalFieldOf(key, 0)} or
 * {@code BOOL.optionalFieldOf(key, false)}), and decoded field results combine through the same
 * {@link DataResult} applicative that RecordCodecBuilder uses, so errors and partial results accumulate alike.
 */
final class RealisticGeneratorCodec {
    /** All gate fields as one {@link GenerationSettings}. */
    static final MapCodec<GenerationSettings> GATES = new GateCodec();

    /** Saved placement caches; their presence also carries the corresponding discovery definitions. */
    record SavedCaches(Optional<BuriedTreasureDiscovery> treasure, Optional<DesertWaygates> waygates,
                       Optional<ShipwreckDiscovery> shipwrecks, Optional<WetlandHuts> wetlandHuts,
                       Optional<SnowShelters> snowShelters, Optional<WarmOceanRuins> warmRuins,
                       Optional<ColdOceanRuins> coldRuins, Optional<DeepColdOceanRuins> deepColdRuins,
                       Optional<DeepWarmOceanRuins> deepWarmRuins, Optional<ReimaginedSites> reimagined,
                       Optional<StructurePlacementCache> structureCache) {
        static final SavedCaches NONE = new SavedCaches(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

        static final MapCodec<SavedCaches> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuriedTreasureDiscovery.CODEC.lenientOptionalFieldOf("treasure_cache").forGetter(SavedCaches::treasure),
            DesertWaygates.CODEC.optionalFieldOf("desert_waygates").forGetter(SavedCaches::waygates),
            ShipwreckDiscovery.CODEC.optionalFieldOf("shipwrecks").forGetter(SavedCaches::shipwrecks),
            WetlandHuts.CODEC.optionalFieldOf("wetland_huts").forGetter(SavedCaches::wetlandHuts),
            SnowShelters.CODEC.optionalFieldOf("snow_shelters").forGetter(SavedCaches::snowShelters),
            WarmOceanRuins.CODEC.optionalFieldOf("warm_ruins").forGetter(SavedCaches::warmRuins),
            ColdOceanRuins.CODEC.optionalFieldOf("cold_ruins").forGetter(SavedCaches::coldRuins),
            DeepColdOceanRuins.CODEC.optionalFieldOf("deep_cold_ruins").forGetter(SavedCaches::deepColdRuins),
            DeepWarmOceanRuins.CODEC.optionalFieldOf("deep_warm_ruins").forGetter(SavedCaches::deepWarmRuins),
            ReimaginedSites.CODEC.optionalFieldOf("reimagined_layouts").forGetter(SavedCaches::reimagined),
            StructurePlacementCache.CODEC.lenientOptionalFieldOf("structure_cache").forGetter(SavedCaches::structureCache)
        ).apply(i, SavedCaches::new));
    }

    static final MapCodec<RealisticGenerator> GENERATOR = RecordCodecBuilder.<RealisticGenerator>mapCodec(i -> i.group(
        HabitatPalette.CODEC.forGetter(RealisticGenerator::palette),
        Codec.intRange(TerrainShape.SHAPE_VERSION, GeneratorSchema.LATEST).fieldOf("shape_version").forGetter(RealisticGenerator::saveSchema),
        GATES.forGetter(RealisticGenerator::settings),
        SavedCaches.CODEC.forGetter(RealisticGenerator::savedCaches)
    ).apply(i, RealisticGenerator::new)).validate(generator -> GeneratorSchema.firstError(generator.schemaInput())
        .<DataResult<RealisticGenerator>>map(message -> DataResult.error(() -> message))
        .orElseGet(() -> DataResult.success(generator)));

    private static MapCodec<Integer> field(GenerationGate gate) {
        if (gate.kind() == GenerationGate.Kind.BOOL)
            return Codec.BOOL.optionalFieldOf(gate.saveKey(), false).xmap(enabled -> enabled ? 1 : 0, value -> value != 0);
        Codec<Integer> values = Codec.intRange(gate.min(), gate.max());
        if (gate == GenerationGate.LIT_CAVE_GROWTH)
            values = values.validate(d -> gate.accepts(d) ? DataResult.success(d) : DataResult.error(() -> "Unsupported prototype lit cave definition"));
        return values.optionalFieldOf(gate.saveKey(), 0);
    }

    private static final class GateCodec extends MapCodec<GenerationSettings> {
        private final GenerationGate[] gates = GenerationGate.all();
        private final MapCodec<Integer>[] fields;

        @SuppressWarnings("unchecked")
        GateCodec() {
            fields = new MapCodec[gates.length];
            for (var gate : gates) fields[gate.ordinal()] = field(gate);
        }

        @Override public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Arrays.stream(fields).flatMap(field -> field.keys(ops));
        }

        @Override public <T> DataResult<GenerationSettings> decode(DynamicOps<T> ops, MapLike<T> input) {
            DataResult<int[]> values = DataResult.success(new int[gates.length]);
            for (int index = 0; index < fields.length; index++) {
                int slot = index;
                values = values.apply2((decoded, value) -> {
                    var next = decoded.clone();
                    next[slot] = value;
                    return next;
                }, fields[index].decode(ops, input));
            }
            return values.map(GenerationSettings::decoded);
        }

        @Override public <T> RecordBuilder<T> encode(GenerationSettings settings, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            for (var gate : gates) prefix = fields[gate.ordinal()].encode(settings.get(gate), ops, prefix);
            return prefix;
        }

        @Override public String toString() { return "RealisticGenerationGates"; }
    }

    private RealisticGeneratorCodec() {}
}
