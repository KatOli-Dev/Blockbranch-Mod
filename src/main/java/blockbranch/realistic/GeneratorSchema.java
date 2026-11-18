package blockbranch.realistic;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The saved {@code shape_version} chain of the Realistic generator, as one ordered table.
 *
 * <p>Each layer wraps the complete schema below it: a layer whose definition is set maps its foundation
 * number to its own published number, and an absent definition passes the number through unchanged.
 * Saving folds the layers bottom-up ({@link #saveSchema}); loading unwraps them top-down and rejects the
 * first layer whose saved number and definition disagree ({@link #firstError}). The per-layer arithmetic
 * stays in the published {@code *Schema} classes, which are frozen contracts with their own checks.
 *
 * <p>Requirement steps (a biome holder in the palette, or another gate) are interleaved where the
 * historical validator checked them, so a rejected save reports the same reason as before.
 * {@code GeneratorSchemaParityCheck} compares this table against the frozen pre-table validator.
 *
 * <p>Pure Java: the analytic classpath has no Minecraft classes.
 */
public final class GeneratorSchema {
    /** The newest published schema; the codec rejects any larger saved number. */
    public static final int LATEST = HumidCrownSchema.VERSION;

    // Structure-level save numbers for worlds before the structure discovery layers.
    public static final int MOSS_SAVE_VERSION = 83, FORMATION_SAVE_VERSION = 84, CAVE_BIOME_SAVE_VERSION = 85,
        STRUCTURE_SAVE_VERSION = 86, CALIBRATED_STRUCTURE_SAVE_VERSION = 87, ATLAS_STRUCTURE_SAVE_VERSION = 88;

    private static final int MISMATCH = Integer.MAX_VALUE;

    /** Structure discovery definitions carried by saved caches rather than integer fields. */
    public record Discoveries(int shipwreck, int waygate, int wetlandHuts, int snowShelters, int warmRuins,
                              int coldRuins, int deepColdRuins, int deepWarmRuins, int reimagined) {
        public static final Discoveries NONE = new Discoveries(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /** Everything validation reads: the saved number, the gates, cache-derived definitions and holders. */
    public record Input(int savedSchema, GenerationSettings settings, Discoveries discoveries, Predicate<String> holder) {}

    @FunctionalInterface private interface Unwrap { int base(int saved, GenerationSettings s); }
    @FunctionalInterface private interface Wrap { int schema(int base, GenerationSettings s); }
    @FunctionalInterface private interface Requirement { boolean violated(Input input); }

    /** One table row: a schema layer (unwrap/wrap) or a requirement check. */
    private record Step(String error, GenerationGate gate, Unwrap unwrap, Wrap wrap, Requirement requirement) {
        boolean layer() { return unwrap != null; }
    }

    private static Step layer(GenerationGate gate, String error, Unwrap unwrap, Wrap wrap) { return new Step(error, gate, unwrap, wrap, null); }
    private static Step requires(String error, Requirement requirement) { return new Step(error, null, null, null, requirement); }
    private static Requirement holderFor(GenerationGate gate, String key) {
        return input -> input.settings().enabled(gate) && !input.holder().test(key);
    }
    private static int d(GenerationSettings s, GenerationGate gate) { return s.get(gate); }

    /** Top (newest) to bottom. Saving applies the layers in reverse order. */
    static final List<Step> LAYERS = List.of(
        layer(GenerationGate.HUMID_CROWN, "Humid crown schema/definition mismatch",
            (v, s) -> HumidCrownSchema.baseSchema(v, d(s, GenerationGate.HUMID_CROWN)),
            (v, s) -> HumidCrownSchema.schema(v, d(s, GenerationGate.HUMID_CROWN))),
        layer(GenerationGate.COASTAL_OAK_CROWN, "Coastal oak crown schema/definition mismatch",
            (v, s) -> CoastalOakCrownSchema.baseSchema(v, d(s, GenerationGate.COASTAL_OAK_CROWN)),
            (v, s) -> CoastalOakCrownSchema.schema(v, d(s, GenerationGate.COASTAL_OAK_CROWN))),
        requires("Coastal oak crowns require summer-dry woodland",
            i -> i.settings().enabled(GenerationGate.COASTAL_OAK_CROWN) && !i.settings().enabled(GenerationGate.SUMMER_DRY)),
        layer(GenerationGate.PINE_CROWN, "Pine crown schema/definition mismatch",
            (v, s) -> PineCrownSchema.baseSchema(v, d(s, GenerationGate.PINE_CROWN)),
            (v, s) -> PineCrownSchema.schema(v, d(s, GenerationGate.PINE_CROWN))),
        layer(GenerationGate.BIRCH_CROWN, "Birch crown schema/definition mismatch",
            (v, s) -> BirchCrownSchema.baseSchema(v, d(s, GenerationGate.BIRCH_CROWN)),
            (v, s) -> BirchCrownSchema.schema(v, d(s, GenerationGate.BIRCH_CROWN))),
        layer(GenerationGate.SEASONAL_CROWN, "Seasonal crown schema/definition mismatch",
            (v, s) -> SeasonalCrownSchema.baseSchema(v, d(s, GenerationGate.SEASONAL_CROWN)),
            (v, s) -> SeasonalCrownSchema.schema(v, d(s, GenerationGate.SEASONAL_CROWN))),
        layer(GenerationGate.BROADLEAF_CROWN, "Broadleaf crown schema/definition mismatch",
            (v, s) -> BroadleafCrownSchema.baseSchema(v, d(s, GenerationGate.BROADLEAF_CROWN)),
            (v, s) -> BroadleafCrownSchema.schema(v, d(s, GenerationGate.BROADLEAF_CROWN))),
        layer(GenerationGate.MANGROVE_ROOTS, "Mangrove roots schema/definition mismatch",
            (v, s) -> MangroveRootsSchema.baseSchema(v, d(s, GenerationGate.MANGROVE_ROOTS)),
            (v, s) -> MangroveRootsSchema.schema(v, d(s, GenerationGate.MANGROVE_ROOTS))),
        layer(GenerationGate.BOREAL_SLOPE, "Boreal slope schema/definition mismatch",
            (v, s) -> BorealSlopeSchema.baseSchema(v, d(s, GenerationGate.BOREAL_SLOPE)),
            (v, s) -> BorealSlopeSchema.schema(v, d(s, GenerationGate.BOREAL_SLOPE))),
        layer(GenerationGate.MONTANE_FOREST, "Montane forest schema/definition mismatch",
            (v, s) -> MontaneForestSchema.baseSchema(v, d(s, GenerationGate.MONTANE_FOREST)),
            (v, s) -> MontaneForestSchema.schema(v, d(s, GenerationGate.MONTANE_FOREST))),
        requires("Montane forest requires holder", holderFor(GenerationGate.MONTANE_FOREST, MontaneForest.KEY)),
        layer(GenerationGate.ALLUVIAL, "Alluvial schema/definition mismatch",
            (v, s) -> AlluvialSchema.baseSchema(v, d(s, GenerationGate.ALLUVIAL)),
            (v, s) -> AlluvialSchema.schema(v, d(s, GenerationGate.ALLUVIAL))),
        layer(GenerationGate.SEASONAL_FOREST, "Seasonal forest schema/definition mismatch",
            (v, s) -> SeasonalForestSchema.baseSchema(v, d(s, GenerationGate.SEASONAL_FOREST)),
            (v, s) -> SeasonalForestSchema.schema(v, d(s, GenerationGate.SEASONAL_FOREST))),
        layer(GenerationGate.WARM_XERIC, "Warm xeric schema/definition mismatch",
            (v, s) -> WarmXericSchema.baseSchema(v, d(s, GenerationGate.WARM_XERIC)),
            (v, s) -> WarmXericSchema.schema(v, d(s, GenerationGate.WARM_XERIC))),
        requires("Warm xeric shrubs require holder", holderFor(GenerationGate.WARM_XERIC, WarmXericShrubland.KEY)),
        layer(null, "Continental ice / leaf-litter schema/definition mismatch",
            (v, s) -> FoundationSchema.baseSchema(v, d(s, GenerationGate.CONTINENTAL_ICE), d(s, GenerationGate.LEAF_LITTER)),
            (v, s) -> FoundationSchema.schema(v, d(s, GenerationGate.CONTINENTAL_ICE), d(s, GenerationGate.LEAF_LITTER))),
        requires("Continental ice requires biome holder", holderFor(GenerationGate.CONTINENTAL_ICE, ContinentalIceField.KEY)),
        layer(GenerationGate.SUMMER_DRY, "Summer-dry schema/definition mismatch",
            (v, s) -> SummerDrySchema.baseSchema(v, d(s, GenerationGate.SUMMER_DRY)),
            (v, s) -> SummerDrySchema.schema(v, d(s, GenerationGate.SUMMER_DRY))),
        requires("Summer-dry woodland requires biome holder", holderFor(GenerationGate.SUMMER_DRY, SummerDryWoodland.KEY)),
        layer(GenerationGate.LAND_ICE, "Land ice schema/definition mismatch",
            (v, s) -> LandIceSchema.baseSchema(v, d(s, GenerationGate.LAND_ICE)),
            (v, s) -> LandIceSchema.schema(v, d(s, GenerationGate.LAND_ICE))),
        requires("Land ice requires biome holder", holderFor(GenerationGate.LAND_ICE, LandIceField.KEY)),
        layer(GenerationGate.DEEP_BASIN_RELIEF, "Deep basin relief schema/definition mismatch",
            (v, s) -> DeepBasinSchema.baseSchema(v, d(s, GenerationGate.DEEP_BASIN_RELIEF)),
            (v, s) -> DeepBasinSchema.schema(v, d(s, GenerationGate.DEEP_BASIN_RELIEF))),
        layer(GenerationGate.ABYSSAL_TRACE, "Abyssal trace schema/definition mismatch",
            (v, s) -> AbyssalTraceSchema.baseSchema(v, d(s, GenerationGate.ABYSSAL_TRACE)),
            (v, s) -> AbyssalTraceSchema.schema(v, d(s, GenerationGate.ABYSSAL_TRACE))),
        layer(GenerationGate.COOL_DESERT, "Cool desert schema/definition mismatch",
            (v, s) -> CoolDesertSchema.baseSchema(v, d(s, GenerationGate.COOL_DESERT)),
            (v, s) -> CoolDesertSchema.schema(v, d(s, GenerationGate.COOL_DESERT))),
        requires("Cool desert requires biome holder", holderFor(GenerationGate.COOL_DESERT, CoolDesert.KEY)),
        layer(GenerationGate.DRY_FOREST, "Dry forest schema/definition mismatch",
            (v, s) -> DryForestSchema.baseSchema(v, d(s, GenerationGate.DRY_FOREST)),
            (v, s) -> DryForestSchema.schema(v, d(s, GenerationGate.DRY_FOREST))),
        requires("Dry forest requires biome holder", holderFor(GenerationGate.DRY_FOREST, DryForest.KEY)),
        layer(GenerationGate.SURFACE_OWNERSHIP, "Surface ownership schema/definition mismatch",
            (v, s) -> SurfaceOwnershipSchema.baseSchema(v, d(s, GenerationGate.SURFACE_OWNERSHIP)),
            (v, s) -> SurfaceOwnershipSchema.schema(v, d(s, GenerationGate.SURFACE_OWNERSHIP))),
        requires("Surface ownership requires cold rockland holder", holderFor(GenerationGate.SURFACE_OWNERSHIP, ColdRockland.KEY)),
        layer(GenerationGate.REDWOOD_FOREST, "Redwood forest schema/definition mismatch",
            (v, s) -> RedwoodForestSchema.baseSchema(v, d(s, GenerationGate.REDWOOD_FOREST)),
            (v, s) -> RedwoodForestSchema.schema(v, d(s, GenerationGate.REDWOOD_FOREST))),
        requires("Redwood forests require their biome holder", holderFor(GenerationGate.REDWOOD_FOREST, RedwoodForest.KEY)),
        layer(GenerationGate.BIOME_TRANSITION, "Biome transition schema/definition mismatch",
            (v, s) -> BiomeTransitionSchema.baseSchema(v, d(s, GenerationGate.BIOME_TRANSITION)),
            (v, s) -> BiomeTransitionSchema.schema(v, d(s, GenerationGate.BIOME_TRANSITION))),
        layer(GenerationGate.MANSION_REPLACEMENT, "Mansion replacement schema/definition mismatch",
            (v, s) -> MansionReplacementSchema.baseSchema(v, d(s, GenerationGate.MANSION_REPLACEMENT)),
            (v, s) -> MansionReplacementSchema.schema(v, d(s, GenerationGate.MANSION_REPLACEMENT))),
        layer(GenerationGate.SANCTUARY_REPLACEMENT, "Sanctuary replacement schema/definition mismatch",
            (v, s) -> SanctuaryReplacementSchema.baseSchema(v, d(s, GenerationGate.SANCTUARY_REPLACEMENT)),
            (v, s) -> SanctuaryReplacementSchema.schema(v, d(s, GenerationGate.SANCTUARY_REPLACEMENT))),
        layer(GenerationGate.GRAVEL_RELIEF, "Gravel relief schema/definition mismatch",
            (v, s) -> GravelReliefSchema.baseSchema(v, d(s, GenerationGate.GRAVEL_RELIEF)),
            (v, s) -> GravelReliefSchema.schema(v, d(s, GenerationGate.GRAVEL_RELIEF))),
        requires("Pyramid galleries require reimagined layouts",
            i -> i.settings().enabled(GenerationGate.PYRAMID_GALLERY) && i.discoveries().reimagined() == 0),
        requires("Overworld coverage requires deep_dark holder", holderFor(GenerationGate.OVERWORLD, "deep_dark")),
        // Boreal crowns and pyramid galleries have no separate mismatch message: an unmatched number
        // propagates and the structure step below rejects it.
        layer(GenerationGate.BOREAL_CROWN, null,
            (v, s) -> BorealCrownSchema.baseSchema(v, d(s, GenerationGate.BOREAL_CROWN)),
            (v, s) -> BorealCrownSchema.schema(v, d(s, GenerationGate.BOREAL_CROWN))),
        layer(GenerationGate.PYRAMID_GALLERY, null,
            (v, s) -> PyramidGallerySchema.baseSchema(v, d(s, GenerationGate.PYRAMID_GALLERY)),
            (v, s) -> PyramidGallerySchema.schema(v, d(s, GenerationGate.PYRAMID_GALLERY)))
    );

    /** Index of the warm-xeric layer; the number beneath it is the structure/cave foundation. */
    private static final int FOUNDATION_LAYER = indexOf(GenerationGate.WARM_XERIC);

    private static int indexOf(GenerationGate gate) {
        for (int i = 0; i < LAYERS.size(); i++) if (LAYERS.get(i).gate() == gate) return i;
        throw new IllegalStateException(gate.name());
    }

    /** Gates that own a schema layer, top to bottom; the parity check uses it to cover every layer. */
    static List<GenerationGate> layeredGates() {
        return LAYERS.stream().map(Step::gate).filter(java.util.Objects::nonNull).toList();
    }

    /** The structure-level number below every discovery layer, chosen by structure and cave definitions. */
    static int legacyStructureSchema(GenerationSettings s) {
        int structure = s.get(GenerationGate.STRUCTURE);
        if (structure == 3) return ATLAS_STRUCTURE_SAVE_VERSION;
        if (structure == 2) return CALIBRATED_STRUCTURE_SAVE_VERSION;
        if (structure > 0) return STRUCTURE_SAVE_VERSION;
        return inputSchemaFor(s);
    }

    /** The schema a programmatically built generator reports before any feature layer. */
    static int inputSchemaFor(GenerationSettings s) {
        if (s.enabled(GenerationGate.CAVE_BIOME)) return CAVE_BIOME_SAVE_VERSION;
        if (s.get(GenerationGate.DRIPSTONE) == 2) return FORMATION_SAVE_VERSION;
        if (s.enabled(GenerationGate.LIT_CAVE_GROWTH)) return MOSS_SAVE_VERSION;
        return TerrainShape.SHAPE_VERSION;
    }

    /** The {@code shape_version} to save. Throws for a combination no published schema describes. */
    public static int saveSchema(GenerationSettings s, Discoveries discoveries) {
        int schema = StructureGenerationSchema.versionFor(legacyStructureSchema(s),
            s.get(GenerationGate.SMALL_DISCOVERY), discoveries.shipwreck(), discoveries.waygate(),
            discoveries.wetlandHuts(), discoveries.snowShelters(), discoveries.warmRuins(), discoveries.coldRuins(),
            discoveries.deepColdRuins(), discoveries.deepWarmRuins(), s.get(GenerationGate.NATIVE_HUT_IGLOO),
            s.get(GenerationGate.NATIVE_ORDINARY), discoveries.reimagined(), s.get(GenerationGate.OVERWORLD),
            s.get(GenerationGate.MANSION_WINGS));
        for (int i = LAYERS.size() - 1; i >= 0; i--) {
            var step = LAYERS.get(i);
            if (step.layer()) schema = step.wrap().schema(schema, s);
        }
        return schema;
    }

    /** The first reason a decoded generator is inconsistent, or empty when it is a published combination. */
    public static Optional<String> firstError(Input input) {
        var s = input.settings();
        int current = input.savedSchema(), foundation = MISMATCH;
        for (int i = 0; i < LAYERS.size(); i++) {
            var step = LAYERS.get(i);
            if (step.layer()) {
                current = step.unwrap().base(current, s);
                if (current == MISMATCH && step.error() != null) return Optional.of(step.error());
                if (i == FOUNDATION_LAYER) foundation = current;
            } else if (step.requirement().violated(input)) return Optional.of(step.error());
        }
        var found = input.discoveries();
        if (!StructureGenerationSchema.matches(current, s.get(GenerationGate.SMALL_DISCOVERY), found.shipwreck(),
            found.waygate(), found.wetlandHuts(), found.snowShelters(), found.warmRuins(), found.coldRuins(),
            found.deepColdRuins(), found.deepWarmRuins(), s.get(GenerationGate.NATIVE_HUT_IGLOO),
            s.get(GenerationGate.NATIVE_ORDINARY), found.reimagined(), s.get(GenerationGate.OVERWORLD),
            s.get(GenerationGate.MANSION_WINGS)))
            return Optional.of("Structure feature schema mismatch");
        int structure = s.get(GenerationGate.STRUCTURE);
        if ((foundation >= ATLAS_STRUCTURE_SAVE_VERSION) != (structure == 3))
            return Optional.of("Structure atlas schema/definition mismatch");
        if ((foundation == CALIBRATED_STRUCTURE_SAVE_VERSION) != (structure == 2))
            return Optional.of("Calibrated structure schema/definition mismatch");
        if ((foundation == STRUCTURE_SAVE_VERSION) != (structure == 1))
            return Optional.of("Structure schema/definition mismatch");
        if ((foundation >= CAVE_BIOME_SAVE_VERSION) != s.enabled(GenerationGate.CAVE_BIOME))
            return Optional.of("Cave biome schema/definition mismatch");
        return caveAndCoastRequirements(input);
    }

    private static Optional<String> caveAndCoastRequirements(Input input) {
        var s = input.settings();
        var holder = input.holder();
        boolean underground = s.enabled(GenerationGate.UNDERGROUND), approaches = s.enabled(GenerationGate.CAVE_APPROACHES);
        if (s.enabled(GenerationGate.CAVE_BIOME) && (s.get(GenerationGate.DRIPSTONE) != 2
            || s.get(GenerationGate.LIT_CAVE_GROWTH) != 2 || !holder.test("lush_caves") || !holder.test("sulfur_caves")))
            return Optional.of("Cave biomes require formation/moss definitions and both holders");
        if (s.enabled(GenerationGate.LIT_CAVE_GROWTH) && (!underground || !approaches))
            return Optional.of("Lit cave growth requires existing cave approaches");
        if (s.enabled(GenerationGate.DRIPSTONE) && (!underground || !holder.test("dripstone_caves")))
            return Optional.of("Dripstone requires caves and its biome holder");
        if (s.enabled(GenerationGate.CAVE_ENTRY_ROUTES) && !approaches) return Optional.of("Entry routes require approaches");
        if (approaches && !underground) return Optional.of("Approaches require caves");
        if (s.enabled(GenerationGate.ICEBERGS) && !holder.test("ice_spikes"))
            return Optional.of("Iceberg habitat requires ice_spikes holder");
        return Optional.empty();
    }

    private GeneratorSchema() {}
}
