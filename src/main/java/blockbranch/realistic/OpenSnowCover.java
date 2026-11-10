package blockbranch.realistic;

import java.util.TreeMap;
import java.util.function.BiPredicate;

/** Reuses the native snow palette; static retention/exposure drifts, not seasons or snow transport. */
public final class OpenSnowCover {
    public static int layers(long seed, int x, int z, GroundEcology.Site site, boolean slope) {
        double patch = Taiga.groundPatch(seed ^ 0x4F50454E534E4F57L, x, z);
        if (site.exposure() > .72 && patch < .1) return 0;
        int n = (slope ? 3 : 4) + (patch > .05 ? 1 : 0) + (patch > .45 ? 1 : 0)
            + (site.retention() > .25 ? 1 : 0);
        return Math.min(8, n);
    }

    /** Find an existing canopy entry without changing its packed encoding. */
    private static int at(int[] base, int x, int y, int z) {
        int key = ((y - TerrainShape.FLOOR) << 8) | ((z & 15) * 16 + (x & 15));
        int lo = 0, hi = base.length - 1;
        while (lo <= hi) {
            int middle = (lo + hi) >>> 1, entryKey = base[middle] >>>blockbranch.cubic.CanopyLayer.STATE_BITS;
            if (entryKey == key) return base[middle] &blockbranch.cubic.CanopyLayer.STATE_MASK;
            if (entryKey < key) lo = middle + 1;
            else hi = middle - 1;
        }
        return 0;
    }

    public static int[] plainsColumn(long seed, int cx, int cz, int[] base,
            DrainageNetwork d, ClimateField c, GroundEcology.Field e) {
        return column(seed, cx, cz, base, d, e, (x, z) -> SnowyPlains.surfaceContains(x, z, d, c), false);
    }

    public static int[] slopesColumn(long seed, int cx, int cz, int[] base,
            DrainageNetwork d, ClimateField c, GroundEcology.Field e) {
        return column(seed, cx, cz, base, d, e, (x, z) -> SnowySlopes.surfaceContains(x, z, d, c), true);
    }

    public static int[] coldPeaksColumn(long seed,int cx,int cz,int[] base,DrainageNetwork d,ClimateField c,GroundEcology.Field e){
        return column(seed,cx,cz,base,d,e,(x,z)->d.sample(x,z).height()+1-TerrainShape.SEA_LEVEL>4200
            &&ColdUplands.surfaceKind(seed,x,z,d,c)==ColdUplands.FROZEN
            &&ColdUplands.snowCap(ColdUplands.cover(seed,x,z,ColdUplands.FROZEN,d,e)),true);
    }
    public static int[] peaksColumn(long seed,int cx,int cz,int[] base,DrainageNetwork d,ClimateField c,GroundEcology.Field e){
        return column(seed,cx,cz,base,d,e,(x,z)->MountainPeaks.surfaceKind(seed,x,z,d,c)==MountainPeaks.JAGGED,true);
    }

    public static int[] beachColumn(long seed, int cx, int cz, int[] base,
            DrainageNetwork d, ClimateField c, GroundEcology.Field e) {
        return column(seed, cx, cz, base, d, e,
            (x, z) -> CoastalBeach.surfaceContains(x, z, d, c, true), false, true);
    }

    static int beachLayers(long seed, int x, int z, GroundEcology.Site site) {
        return 1 + (SurfacePatches.sample(seed ^ 0x534E4F5742454143L, x, z, 23) > .1 ? 1 : 0)
            + (site.retention() > .25 ? 1 : 0);
    }

    static int[] column(long seed, int cx, int cz, int[] base, DrainageNetwork d,
            GroundEcology.Field e, BiPredicate<Integer, Integer> domain, boolean slope) {
        return column(seed, cx, cz, base, d, e, domain, slope, false);
    }

    private static int[] column(long seed, int cx, int cz, int[] base, DrainageNetwork d,
            GroundEcology.Field e, BiPredicate<Integer, Integer> domain, boolean slope, boolean beach) {
        // Snow follows each block's exposure/slope. The biome palette
        // remains quart-sized; using that palette as a paint mask made 4 m steps.
        int stride = 1, width = 16;
        boolean[] selected = new boolean[width * width];
        boolean any = false;
        for (int z = 0; z < width; z++) for (int x = 0; x < width; x++) {
            boolean value = domain.test(cx * 16 + x * stride, cz * 16 + z * stride);
            selected[z * width + x] = value;
            any |= value;
        }
        if (!any) return base;

        var snow = new TreeMap<Integer, Integer>();
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            if (!selected[(z / stride) * width + x / stride]) continue;
            int wx = cx * 16 + x, wz = cz * 16 + z;
            var s = d.sample(wx, wz);
            if (s.waterLevel() != DrainageNetwork.DRY || d.channelAt(wx, wz).edge() >= 0
                    || (!slope && !beach && LandVegetation.surfaceReserved(d, wx, wz))
                    || at(base, wx, s.height() + 1, wz) != 0) continue;
            int layers = beach ? beachLayers(seed, wx, wz, e.sample(wx, wz))
                : layers(seed, wx, wz, e.sample(wx, wz), slope);
            if (layers > 0) {
                int key = ((s.height() + 1 - TerrainShape.FLOOR) << 8) | (z * 16 + x);
                snow.put(key, SnowyTaiga.snowCode(layers));
            }
        }
        return SwampCanopy.merge(base, snow.entrySet().stream()
            .mapToInt(v -> (v.getKey() <<blockbranch.cubic.CanopyLayer.STATE_BITS) | v.getValue()).toArray());
    }

    private OpenSnowCover() {}
}
