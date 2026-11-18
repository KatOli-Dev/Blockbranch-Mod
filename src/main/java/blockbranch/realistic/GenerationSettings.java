package blockbranch.realistic;

import java.util.Arrays;

/**
 * Immutable values of every {@link GenerationGate} for one generator. A generator receives a complete
 * instance at construction and never changes it; worldgen threads therefore share it without locking.
 * Pure Java: no Minecraft types, so analytic checks can build and compare settings directly.
 */
public final class GenerationSettings {
    /** Every gate absent: the original definition-82 terrain foundation. */
    public static final GenerationSettings NONE = new GenerationSettings(new int[GenerationGate.all().length]);

    private final int[] values;

    private GenerationSettings(int[] values) { this.values = values; }

    public int get(GenerationGate gate) { return values[gate.ordinal()]; }
    public boolean enabled(GenerationGate gate) { return values[gate.ordinal()] > 0; }

    /** A copy with one gate changed; rejects values the gate never published. */
    public GenerationSettings with(GenerationGate gate, int value) {
        if (!gate.accepts(value)) throw new IllegalArgumentException("Unsupported " + gate.saveKey() + " " + value);
        if (values[gate.ordinal()] == value) return this;
        var copy = values.clone();
        copy[gate.ordinal()] = value;
        return new GenerationSettings(copy);
    }

    public GenerationSettings with(GenerationGate gate, boolean enabled) { return with(gate, enabled ? 1 : 0); }

    /**
     * Settings from codec field results. Each field codec has already range-checked its value; an errored
     * decode may still carry a partial result, which must not throw here.
     */
    static GenerationSettings decoded(int[] values) {
        if (values.length != GenerationGate.all().length) throw new IllegalArgumentException("Gate count " + values.length);
        return new GenerationSettings(values.clone());
    }

    /**
     * The fixed-seed generator view used by the lazy lit-cave growth planner. It shares the owner's canopy
     * caches, so it keeps every terrain, crown and vegetation definition; it has no cave biomes, structures
     * or discoveries of its own.
     */
    GenerationSettings litCaveGrowthView() {
        var view = NONE;
        for (var gate : VIEW_GATES) view = view.with(gate, get(gate));
        return view;
    }

    private static final GenerationGate[] VIEW_GATES = {
        GenerationGate.ICEBERGS, GenerationGate.SANDY_COASTS, GenerationGate.UNDERGROUND,
        GenerationGate.CAVE_APPROACHES, GenerationGate.CAVE_ENTRY_ROUTES, GenerationGate.ORE,
        GenerationGate.DRIPSTONE, GenerationGate.LIT_CAVE_GROWTH, GenerationGate.BOREAL_CROWN,
        GenerationGate.GRAVEL_RELIEF, GenerationGate.ALLUVIAL, GenerationGate.MONTANE_FOREST,
        GenerationGate.BOREAL_SLOPE, GenerationGate.MANGROVE_ROOTS, GenerationGate.BROADLEAF_CROWN,
        GenerationGate.SEASONAL_CROWN, GenerationGate.BIRCH_CROWN, GenerationGate.PINE_CROWN,
        GenerationGate.COASTAL_OAK_CROWN, GenerationGate.HUMID_CROWN, GenerationGate.BIOME_TRANSITION,
        GenerationGate.REDWOOD_FOREST, GenerationGate.SURFACE_OWNERSHIP, GenerationGate.DRY_FOREST,
        GenerationGate.SEASONAL_FOREST, GenerationGate.COOL_DESERT, GenerationGate.ABYSSAL_TRACE,
        GenerationGate.DEEP_BASIN_RELIEF, GenerationGate.LAND_ICE, GenerationGate.SUMMER_DRY,
        GenerationGate.CONTINENTAL_ICE, GenerationGate.LEAF_LITTER, GenerationGate.WARM_XERIC,
    };

    /**
     * Saved vegetation flags. Bits 0..13 are transitions, redwoods, dry forest, summer-dry woodland, polar
     * climate, seasonal forest, montane needleleaf, boreal slope tolerance and the broadleaf, seasonal,
     * birch, pine, coastal-oak and humid crowns. Each bit keeps its meaning forever because saved tree-clear
     * plans store only these numbers.
     */
    public int vegetationFlags() {
        return get(GenerationGate.BIOME_TRANSITION)
            | (enabled(GenerationGate.REDWOOD_FOREST) ? RedwoodForest.FLAG : 0)
            | (enabled(GenerationGate.DRY_FOREST) ? DryForest.FLAG : 0)
            | (enabled(GenerationGate.SUMMER_DRY) ? SummerDryWoodland.FLAG : 0)
            | (enabled(GenerationGate.CONTINENTAL_ICE) ? 16 : 0)
            | (enabled(GenerationGate.SEASONAL_FOREST) ? SeasonalForestExpansion.FLAG : 0)
            | (enabled(GenerationGate.MONTANE_FOREST) ? MontaneForest.FLAG : 0)
            | (enabled(GenerationGate.BOREAL_SLOPE) ? BorealSlope.FLAG : 0)
            | (enabled(GenerationGate.BROADLEAF_CROWN) ? BroadleafCrowns.FLAG : 0)
            | (enabled(GenerationGate.SEASONAL_CROWN) ? SeasonalCrowns.FLAG : 0)
            | (enabled(GenerationGate.BIRCH_CROWN) ? BirchCrowns.FLAG : 0)
            | (enabled(GenerationGate.PINE_CROWN) ? PineCrowns.FLAG : 0)
            | (enabled(GenerationGate.COASTAL_OAK_CROWN) ? CoastalOakCrowns.FLAG : 0)
            | (enabled(GenerationGate.HUMID_CROWN) ? HumidCrowns.FLAG : 0);
    }

    @Override public boolean equals(Object other) {
        return other instanceof GenerationSettings settings && Arrays.equals(values, settings.values);
    }
    @Override public int hashCode() { return Arrays.hashCode(values); }

    @Override public String toString() {
        var text = new StringBuilder("GenerationSettings{");
        for (var gate : GenerationGate.all())
            if (values[gate.ordinal()] != 0) text.append(gate.saveKey()).append('=').append(values[gate.ordinal()]).append(',');
        return text.append('}').toString();
    }
}
