package blockbranch.realistic;

/** Dry sandy coasts. The 2 C boundary describes a static cold-season snow scene,
 * not sea ice, annual snow persistence, tides, or a seasonal simulation. */
public final class CoastalBeach {
    public static final double SNOW_TEMPERATURE = 2;

    public static boolean snowy(ClimateField.Sample climate) {
        return climate.meanTemperatureC() < SNOW_TEMPERATURE;
    }

    public static boolean contains(int x, int z, DrainageNetwork d, ClimateField c, boolean snow) {
        return surfaceContains(Math.floorDiv(x, 4) * 4, Math.floorDiv(z, 4) * 4, d, c, snow);
    }

    static boolean surfaceContains(int x, int z, DrainageNetwork d, ClimateField c, boolean snow) {
        var site = LowGradientSandyShore.site(d, x, z);
        // Reject inland columns before doing climate or neighboring-coast queries.
        return !d.sample(x,z).bedrockCoast() && site.dry() && site.elevation() >= 0 && LowGradientSandyShore.eligible(site)
            && LowGradientSandyShore.surfaceContains(x, z, (wx, wz) -> LowGradientSandyShore.site(d, wx, wz))
            && snowy(c.sample(x, z, d::sample)) == snow
            && !MangroveSwamp.exact(c.seed(), x, z, d, c);
    }

    /** Ordinary Beach pioneers occupy the upper dry backshore, with half the
     * existing within-colony density. The caller protects Snowy Beach and other habitats. */
    static int pioneerCover(long seed,int x,int z,LowGradientSandyShore.Site site,int existing) {
        if(existing!=2)return existing;
        if(site.elevation()<1)return 1;
        // Eligible cells already pass the shore's low-bit draw. An independent
        // fine field retains approximately half as clustered rooted pioneers.
        long cell=OakBirchWoodland.mix(seed ^ x*0x165667B19E3779F9L ^ z*0xD1B54A32D192ED03L);
        double colony=SurfacePatches.sample(seed ^ 0x4245414348475241L,x,z,3);
        double edge=(((cell>>>12)&255)/255.0-.5)*.08;
        return colony+edge>=.5?2:1;
    }

    public record Point(int x, int z) {}



    private CoastalBeach() {}
}
