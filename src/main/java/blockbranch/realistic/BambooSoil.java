package blockbranch.realistic;

/** Independent mineral-floor encoding: changing soil never moves existing plants. */
public final class BambooSoil {
    public static final int FIRST = 54, GRASS_LITTER = 63, GRASS_FERN = 64, LAST = GRASS_FERN;

    public static boolean encoded(int cover) { return cover >= FIRST && cover <= LAST; }
    public static int vegetation(int cover) { return cover == GRASS_LITTER ? 9 : cover == GRASS_FERN ? 10 : encoded(cover) ? 8 + (cover - FIRST) / 3 : cover; }
    /** 0 mineral soil, 1 coarse soil, 2 root-rich ground, 3 existing grass habitat. */
    public static int soil(int cover) {
        if (!encoded(cover)) throw new IllegalArgumentException("Not a bamboo soil variant");
        return cover >= GRASS_LITTER ? 3 : (cover - FIRST) % 3;
    }

    static int encode(long seed, int x, int z, GroundEcology.Site site, boolean clear, int legacy) {
        if (legacy < 8 || legacy > 10) return legacy; // Existing grass openings stay grass.
        if (clear && legacy >= 9 && TropicalForestVariants.bambooGrass(seed,x,z,site))
            return GRASS_LITTER + legacy - 9;
        double edge = SurfacePatches.signed(seed ^ 0x42414D4245444745L, x, z, 5);
        double roots = GroundEcology.colonyStrength(seed, 0x42414D424F4F434CL, x, z, 48, 12, 29, site);
        double coarse = SurfacePatches.signed(seed ^ 0x42414D42434F4152L, x, z, 24)
            + .12 * edge + .08 * site.exposure() - .08 * site.wetness();
        int soil = !clear || roots + .10 * edge > .35 ? 2 : coarse > .10 ? 1 : 0;
        return FIRST + (legacy - 8) * 3 + soil;
    }

    /** Remove only the plant; preserve exactly the version-1 soil beneath it. */
    static int withoutLitter(int cover) {
        if(cover==GRASS_LITTER)return 11;
        if(vegetation(cover)!=9)throw new IllegalArgumentException("Not bamboo litter");
        return cover-3;
    }

    private BambooSoil() {}
}
