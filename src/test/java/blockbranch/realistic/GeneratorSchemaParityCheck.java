package blockbranch.realistic;

import java.util.*;
import java.util.function.Predicate;

/**
 * Table-driven generator schema versus the frozen pre-table validator ({@link LegacyGeneratorValidation}).
 * For every compared input both must reject with the same first reason or both accept; the saved-schema
 * fold must return the same number or both throw; vegetation flags must match.
 *
 * <p>Inputs: every published chain prefix of the bundled all-gates preset (each modern layer removed from
 * the top down, each continental-ice/leaf-litter pair and each structure-definition era), every single-gate,
 * single-holder and saved-number perturbation of those, and seeded random combinations biased toward the
 * accepted region. Counts are inputs compared, not a proof over the whole input space.
 */
public final class GeneratorSchemaParityCheck {
    private static final List<String> HOLDERS = List.of(MontaneForest.KEY, WarmXericShrubland.KEY, ContinentalIceField.KEY,
        SummerDryWoodland.KEY, LandIceField.KEY, CoolDesert.KEY, DryForest.KEY, ColdRockland.KEY, RedwoodForest.KEY,
        "deep_dark", "lush_caves", "sulfur_caves", "dripstone_caves", "ice_spikes");
    /** The modern single-definition layers, top (newest) first. */
    private static final List<GenerationGate> MODERN = List.of(GenerationGate.HUMID_CROWN, GenerationGate.COASTAL_OAK_CROWN,
        GenerationGate.PINE_CROWN, GenerationGate.BIRCH_CROWN, GenerationGate.SEASONAL_CROWN, GenerationGate.BROADLEAF_CROWN,
        GenerationGate.MANGROVE_ROOTS, GenerationGate.BOREAL_SLOPE, GenerationGate.MONTANE_FOREST, GenerationGate.ALLUVIAL,
        GenerationGate.SEASONAL_FOREST, GenerationGate.WARM_XERIC, GenerationGate.CONTINENTAL_ICE, GenerationGate.LEAF_LITTER,
        GenerationGate.SUMMER_DRY, GenerationGate.LAND_ICE, GenerationGate.DEEP_BASIN_RELIEF, GenerationGate.ABYSSAL_TRACE,
        GenerationGate.COOL_DESERT, GenerationGate.DRY_FOREST, GenerationGate.SURFACE_OWNERSHIP, GenerationGate.REDWOOD_FOREST,
        GenerationGate.BIOME_TRANSITION, GenerationGate.MANSION_REPLACEMENT, GenerationGate.SANCTUARY_REPLACEMENT,
        GenerationGate.GRAVEL_RELIEF, GenerationGate.BOREAL_CROWN, GenerationGate.PYRAMID_GALLERY, GenerationGate.MANSION_WINGS,
        GenerationGate.OVERWORLD);

    /**
     * The gates and newest number the frozen legacy validator knows. A later gate is appended to the enum and
     * stays absent here, so this comparison keeps covering the transcribed contract.
     */
    private static final GenerationGate[] TRANSCRIBED = EnumSet.range(GenerationGate.ICEBERGS, GenerationGate.WARM_XERIC).toArray(GenerationGate[]::new);
    private static final int TRANSCRIBED_LATEST = 279;

    private record Case(GenerationSettings settings, GeneratorSchema.Discoveries found, Set<String> holders, int saved) {}

    private static int compared, accepted, schemaCompared;
    private static final Map<String, Integer> reasons = new TreeMap<>();

    public static void main(String[] args) {
        require(TRANSCRIBED.length == 43, "Transcribed gate range is the 43 gates of the legacy validator");
        require(GeneratorSchema.layeredGates().containsAll(MODERN.stream().filter(g -> g != GenerationGate.CONTINENTAL_ICE
            && g != GenerationGate.LEAF_LITTER && g != GenerationGate.MANSION_WINGS && g != GenerationGate.OVERWORLD).toList()),
            "Every modern single-definition gate owns a schema layer");
        var bundled = bundled();
        var cases = new ArrayList<Case>();
        for (int cut = 0; cut <= MODERN.size(); cut++) {
            var s = bundled.settings();
            for (int i = 0; i < cut; i++) s = s.with(MODERN.get(i), 0);
            cases.add(withSaved(new Case(s, bundled.found(), bundled.holders(), 0)));
        }
        for (int ice = 0; ice <= 1; ice++) for (int litter = 0; litter <= 1; litter++) {
            var s = bundled.settings();
            for (var gate : MODERN.subList(0, MODERN.indexOf(GenerationGate.CONTINENTAL_ICE))) s = s.with(gate, 0);
            cases.add(withSaved(new Case(s.with(GenerationGate.CONTINENTAL_ICE, ice).with(GenerationGate.LEAF_LITTER, litter),
                bundled.found(), bundled.holders(), 0)));
        }
        for (int structure = 0; structure <= 3; structure++) for (int caves = 0; caves <= 1; caves++) {
            var s = GenerationSettings.NONE.with(GenerationGate.STRUCTURE, structure).with(GenerationGate.CAVE_BIOME, caves)
                .with(GenerationGate.UNDERGROUND, 1).with(GenerationGate.CAVE_APPROACHES, 1)
                .with(GenerationGate.DRIPSTONE, caves > 0 ? 2 : 0).with(GenerationGate.LIT_CAVE_GROWTH, caves > 0 ? 2 : 0);
            cases.add(withSaved(new Case(s, GeneratorSchema.Discoveries.NONE, Set.copyOf(HOLDERS), 0)));
        }
        int seeds = cases.size();
        for (int i = 0; i < seeds; i++) for (var perturbed : perturbations(cases.get(i))) cases.add(perturbed);
        var random = new Random(0x5EED_279L);
        for (int i = 0; i < 100_000; i++) cases.add(randomCase(random, bundled));
        for (var c : cases) compare(c);
        require(accepted >= seeds, "Every published chain prefix is accepted (" + accepted + " accepted)");
        // 48 distinct rejection reasons at base 1070bd3, plus acceptance: every branch was exercised.
        require(reasons.size() >= 49, "Every rejection reason was reached: " + reasons.keySet());
        System.out.println("GENERATOR_SCHEMA_PARITY_PASS compared=" + compared + " accepted=" + accepted
            + " schemaFolds=" + schemaCompared + " firstReasons=" + reasons);
    }

    private static Case bundled() {
        var s = GenerationSettings.NONE;
        for (var gate : TRANSCRIBED) if (gate.kind() == GenerationGate.Kind.BOOL || gate.max() == 1) s = s.with(gate, 1);
        s = s.with(GenerationGate.ORE, OreLayer.DEFINITION).with(GenerationGate.DRIPSTONE, 2)
            .with(GenerationGate.LIT_CAVE_GROWTH, 2).with(GenerationGate.STRUCTURE, 3);
        var found = new GeneratorSchema.Discoveries(1, 1, 1, 1, 1, 1, 1, 1, 9);
        return withSaved(new Case(s, found, Set.copyOf(HOLDERS), 0));
    }

    private static Case withSaved(Case c) {
        int saved;
        try { saved = legacy(c).saveSchema(); } catch (IllegalArgumentException e) { saved = TerrainShape.SHAPE_VERSION; }
        return new Case(c.settings(), c.found(), c.holders(), saved);
    }

    private static List<Case> perturbations(Case c) {
        var out = new ArrayList<Case>();
        for (var gate : TRANSCRIBED)
            for (int v = gate.min(); v <= gate.max(); v++)
                if (gate.accepts(v) && v != c.settings().get(gate)) out.add(new Case(c.settings().with(gate, v), c.found(), c.holders(), c.saved()));
        for (var key : HOLDERS) {
            var fewer = new HashSet<>(c.holders());
            if (fewer.remove(key)) out.add(new Case(c.settings(), c.found(), fewer, c.saved()));
        }
        for (int delta : new int[]{-2, -1, 1, 2}) out.add(new Case(c.settings(), c.found(), c.holders(), c.saved() + delta));
        int[] f = discoveries(c.found());
        for (int i = 0; i < f.length; i++) {
            var changed = f.clone();
            changed[i] = i == 8 ? (f[i] == 0 ? 1 : 0) : 1 - f[i];
            out.add(new Case(c.settings(), discoveries(changed), c.holders(), c.saved()));
        }
        return out;
    }

    private static Case randomCase(Random random, Case bundled) {
        var s = random.nextBoolean() ? bundled.settings() : GenerationSettings.NONE;
        int flips = 1 + random.nextInt(6);
        for (int i = 0; i < flips; i++) {
            var gate = TRANSCRIBED[random.nextInt(TRANSCRIBED.length)];
            int v;
            do v = gate.min() + random.nextInt(gate.max() - gate.min() + 1); while (!gate.accepts(v));
            s = s.with(gate, v);
        }
        int[] f = random.nextBoolean() ? discoveries(bundled.found()) : new int[9];
        for (int i = 0; i < f.length; i++) if (random.nextInt(8) == 0) f[i] = i == 8 ? random.nextInt(10) : random.nextInt(2);
        var holders = new HashSet<String>();
        for (var key : HOLDERS) if (random.nextInt(10) != 0) holders.add(key);
        var c = new Case(s, discoveries(f), holders, 0);
        int saved = switch (random.nextInt(3)) {
            case 0 -> withSaved(c).saved();
            case 1 -> TerrainShape.SHAPE_VERSION + random.nextInt(TRANSCRIBED_LATEST - TerrainShape.SHAPE_VERSION + 1);
            default -> withSaved(c).saved() + random.nextInt(5) - 2;
        };
        return new Case(s, c.found(), holders, saved);
    }

    private static void compare(Case c) {
        Predicate<String> holder = c.holders()::contains;
        var old = legacy(c);
        String expected = old.validate(holder);
        String actual = GeneratorSchema.firstError(new GeneratorSchema.Input(c.saved(), c.settings(), c.found(), holder)).orElse(null);
        require(Objects.equals(expected, actual), "Validation differs for " + c + ": legacy=" + expected + " table=" + actual);
        compared++;
        if (expected == null) accepted++;
        reasons.merge(expected == null ? "accepted" : expected, 1, Integer::sum);
        Integer legacySchema = null, tableSchema = null;
        try { legacySchema = old.saveSchema(); } catch (IllegalArgumentException ignored) {}
        try { tableSchema = GeneratorSchema.saveSchema(c.settings(), c.found()); } catch (IllegalArgumentException ignored) {}
        require(Objects.equals(legacySchema, tableSchema), "Saved schema differs for " + c + ": legacy=" + legacySchema + " table=" + tableSchema);
        if (legacySchema != null) schemaCompared++;
        require(old.vegetationFlags() == c.settings().vegetationFlags(), "Vegetation flags differ for " + c);
    }

    private static LegacyGeneratorValidation legacy(Case c) {
        var s = c.settings();
        var g = new LegacyGeneratorValidation();
        g.inputSchema = c.saved();
        g.icebergsEnabled = s.enabled(GenerationGate.ICEBERGS);
        g.sandyCoastsEnabled = s.enabled(GenerationGate.SANDY_COASTS);
        g.undergroundEnabled = s.enabled(GenerationGate.UNDERGROUND);
        g.approachesEnabled = s.enabled(GenerationGate.CAVE_APPROACHES);
        g.entryRoutesEnabled = s.enabled(GenerationGate.CAVE_ENTRY_ROUTES);
        g.oreDefinition = s.get(GenerationGate.ORE);
        g.dripstoneDefinition = s.get(GenerationGate.DRIPSTONE);
        g.litCaveGrowthDefinition = s.get(GenerationGate.LIT_CAVE_GROWTH);
        g.caveBiomeDefinition = s.get(GenerationGate.CAVE_BIOME);
        g.structureDefinition = s.get(GenerationGate.STRUCTURE);
        g.borealCrownDefinition = s.get(GenerationGate.BOREAL_CROWN);
        g.discoveryDefinition = s.get(GenerationGate.SMALL_DISCOVERY);
        g.nativeHutIgloo = s.get(GenerationGate.NATIVE_HUT_IGLOO);
        g.nativeOrdinary = s.get(GenerationGate.NATIVE_ORDINARY);
        g.overworldDefinition = s.get(GenerationGate.OVERWORLD);
        g.mansionWingDefinition = s.get(GenerationGate.MANSION_WINGS);
        g.pyramidGalleryDefinition = s.get(GenerationGate.PYRAMID_GALLERY);
        g.gravelReliefDefinition = s.get(GenerationGate.GRAVEL_RELIEF);
        g.alluvialDefinition = s.get(GenerationGate.ALLUVIAL);
        g.montaneForestDefinition = s.get(GenerationGate.MONTANE_FOREST);
        g.borealSlopeDefinition = s.get(GenerationGate.BOREAL_SLOPE);
        g.mangroveRootsDefinition = s.get(GenerationGate.MANGROVE_ROOTS);
        g.broadleafCrownDefinition = s.get(GenerationGate.BROADLEAF_CROWN);
        g.seasonalCrownDefinition = s.get(GenerationGate.SEASONAL_CROWN);
        g.birchCrownDefinition = s.get(GenerationGate.BIRCH_CROWN);
        g.pineCrownDefinition = s.get(GenerationGate.PINE_CROWN);
        g.coastalOakCrownDefinition = s.get(GenerationGate.COASTAL_OAK_CROWN);
        g.humidCrownDefinition = s.get(GenerationGate.HUMID_CROWN);
        g.sanctuaryReplacementDefinition = s.get(GenerationGate.SANCTUARY_REPLACEMENT);
        g.mansionReplacementDefinition = s.get(GenerationGate.MANSION_REPLACEMENT);
        g.biomeTransitionDefinition = s.get(GenerationGate.BIOME_TRANSITION);
        g.redwoodForestDefinition = s.get(GenerationGate.REDWOOD_FOREST);
        g.surfaceOwnershipDefinition = s.get(GenerationGate.SURFACE_OWNERSHIP);
        g.dryForestDefinition = s.get(GenerationGate.DRY_FOREST);
        g.seasonalForestDefinition = s.get(GenerationGate.SEASONAL_FOREST);
        g.coolDesertDefinition = s.get(GenerationGate.COOL_DESERT);
        g.abyssalTraceDefinition = s.get(GenerationGate.ABYSSAL_TRACE);
        g.deepBasinReliefDefinition = s.get(GenerationGate.DEEP_BASIN_RELIEF);
        g.landIceDefinition = s.get(GenerationGate.LAND_ICE);
        g.summerDryDefinition = s.get(GenerationGate.SUMMER_DRY);
        g.continentalIceDefinition = s.get(GenerationGate.CONTINENTAL_ICE);
        g.leafLitterDefinition = s.get(GenerationGate.LEAF_LITTER);
        g.warmXericDefinition = s.get(GenerationGate.WARM_XERIC);
        var f = c.found();
        g.shipwreckValue = f.shipwreck(); g.waygateValue = f.waygate(); g.wetlandHutValue = f.wetlandHuts();
        g.snowShelterValue = f.snowShelters(); g.warmRuinValue = f.warmRuins(); g.coldRuinValue = f.coldRuins();
        g.deepColdRuinValue = f.deepColdRuins(); g.deepWarmRuinValue = f.deepWarmRuins(); g.reimaginedValue = f.reimagined();
        return g;
    }

    private static int[] discoveries(GeneratorSchema.Discoveries f) {
        return new int[]{f.shipwreck(), f.waygate(), f.wetlandHuts(), f.snowShelters(), f.warmRuins(), f.coldRuins(),
            f.deepColdRuins(), f.deepWarmRuins(), f.reimagined()};
    }
    private static GeneratorSchema.Discoveries discoveries(int[] f) {
        return new GeneratorSchema.Discoveries(f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8]);
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private GeneratorSchemaParityCheck() {}
}
