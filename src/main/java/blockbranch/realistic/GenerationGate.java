package blockbranch.realistic;

/**
 * Every independently saved generation opt-in of the Realistic generator, with its flat saved field name
 * and accepted values. The saved form stays flat ({@code "humid_crown_definition": 1}); absence always means
 * the default 0/false, so older saves keep their published behavior.
 *
 * <p>This enum carries no Minecraft types, so the pure analytic classpath can enumerate it. The saved
 * schema chain that ties these values to a {@code shape_version} lives in {@link GeneratorSchema}; the codec
 * that reads and writes the fields lives in {@link RealisticGeneratorCodec}.
 *
 * <p>Adding a gate: append a constant here (save key, accepted range), add its schema layer to
 * {@link GeneratorSchema#LAYERS}, and read it from {@link GenerationSettings}. Never rename a save key or
 * reinterpret a published value.
 */
public enum GenerationGate {
    ICEBERGS("icebergs", Kind.BOOL),
    SANDY_COASTS("sandy_coasts", Kind.BOOL),
    UNDERGROUND("underground", Kind.BOOL),
    CAVE_APPROACHES("cave_approaches", Kind.BOOL),
    CAVE_ENTRY_ROUTES("cave_entry_routes", Kind.BOOL),
    ORE("ore_definition", 0, OreLayer.DEFINITION),
    DRIPSTONE("dripstone_definition", 0, 2),
    /** Definition 1 was an unpublished prototype; only 0 and 2 are accepted. */
    LIT_CAVE_GROWTH("lit_cave_growth_definition", 0, 2),
    CAVE_BIOME("cave_biome_definition"),
    STRUCTURE("structure_definition", 0, 3),
    BOREAL_CROWN("boreal_crown_definition"),

    SMALL_DISCOVERY("small_discovery_definition"),
    NATIVE_HUT_IGLOO("native_hut_igloo"),
    NATIVE_ORDINARY("native_ordinary_structures"),
    OVERWORLD("native_overworld_structures"),
    MANSION_WINGS("mansion_wing_definition"),
    PYRAMID_GALLERY("pyramid_gallery_definition"),

    GRAVEL_RELIEF("gravel_relief_definition"),
    ALLUVIAL("alluvial_floodplain_definition"),
    MONTANE_FOREST("montane_forest_definition"),
    BOREAL_SLOPE("boreal_slope_definition"),
    MANGROVE_ROOTS("mangrove_roots_definition"),
    BROADLEAF_CROWN("broadleaf_crown_definition"),
    SEASONAL_CROWN("seasonal_crown_definition"),
    BIRCH_CROWN("birch_crown_definition"),
    PINE_CROWN("pine_crown_definition"),
    COASTAL_OAK_CROWN("coastal_oak_crown_definition"),
    HUMID_CROWN("humid_crown_definition"),

    SANCTUARY_REPLACEMENT("sanctuary_replacement_definition"),
    MANSION_REPLACEMENT("mansion_replacement_definition"),
    BIOME_TRANSITION("biome_transition_definition"),
    REDWOOD_FOREST("redwood_forest_definition"),
    SURFACE_OWNERSHIP("surface_ownership_definition"),
    DRY_FOREST("dry_forest_definition"),
    SEASONAL_FOREST("seasonal_forest_definition"),
    COOL_DESERT("cool_desert_definition"),
    ABYSSAL_TRACE("abyssal_trace_definition"),
    DEEP_BASIN_RELIEF("deep_basin_relief_definition"),
    LAND_ICE("land_ice_definition"),
    SUMMER_DRY("summer_dry_definition"),
    CONTINENTAL_ICE("continental_ice_definition"),
    LEAF_LITTER("leaf_litter_definition"),
    WARM_XERIC("warm_xeric_definition");

    public enum Kind { BOOL, INT }

    private static final GenerationGate[] VALUES = values();

    private final String saveKey;
    private final Kind kind;
    private final int min, max;

    GenerationGate(String saveKey) { this(saveKey, 0, 1); }
    GenerationGate(String saveKey, Kind kind) { this(saveKey, kind, 0, 1); }
    GenerationGate(String saveKey, int min, int max) { this(saveKey, Kind.INT, min, max); }
    GenerationGate(String saveKey, Kind kind, int min, int max) {
        this.saveKey = saveKey;
        this.kind = kind;
        this.min = min;
        this.max = max;
    }

    public String saveKey() { return saveKey; }
    public Kind kind() { return kind; }
    public int min() { return min; }
    public int max() { return max; }

    /** Whether a decoded value is a published definition (range plus any reserved holes). */
    public boolean accepts(int value) {
        if (value < min || value > max) return false;
        return this != LIT_CAVE_GROWTH || value != 1;
    }

    static GenerationGate[] all() { return VALUES; }
}
