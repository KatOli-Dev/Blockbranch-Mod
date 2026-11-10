package blockbranch.realistic;

import java.util.Set;
import java.util.TreeSet;

/** Coherent physical sediment palette, narrow habitat ownership and square-mask controls. */
public final class FrozenSeabedCheck {
    public static void main(String[] args) {
        for (long seed : new long[] {424242, 1, -17}) {
            var d = new DrainageNetwork(new TerrainShape(seed), seed, true, true);
            var c = new ClimateField(seed);
            var materials = new TreeSet<Integer>();
            int samples = 0;
            for (int kind : new int[] {1, 2}) {
                var p = blockbranch.realistic.FrozenOceanFixtures.fixture(d, seed, kind, (x, z) -> IcebergField.habitat(seed, x, z, d, c));
                for (int z = p.z() - 64; z <= p.z() + 64; z += 4) for (int x = p.x() - 64; x <= p.x() + 64; x += 4) {
                    if (!DeepOcean.surfaceContains(x, z, d) && !MarineShelf.surfaceContains(x, z, d)) continue;
                    int code = FrozenSeabed.frozenCover(seed, x, z, d, c, true);
                    if (code == 0) continue;
                    var s = d.sample(x, z);
                    require(Set.of(5, 6, 7, 8, 9).contains(code), "Existing sediment/rock semantics only");
                    if (s.waterLevel() - s.height() - 1 > 40) require(code != 7, "Deep bed is fines/gravel/rock rather than arbitrary sand");
                    require(FrozenOcean.surfaceKind(seed, x, z, d, c) > 0 && !IcebergField.habitat(seed, x, z, d, c), "Exact frozen marine ownership, excluding accepted iceberg habitat");
                    materials.add(code); samples++;
                }
            }
            require(samples > 100 && materials.size() >= 3, "Meaningful natural sediment and exposure variety");
            var temperate = blockbranch.realistic.TemperateOceanFixtures.fixture(d, seed);
            require(FrozenSeabed.frozenCover(seed, temperate.x(), temperate.z(), d, c, true) == 0, "Temperate marine palette remains untouched");
            boolean iceberg = false;
            for (int i = 0; i < d.size(); i++) {
                int x = d.nodeX(i), z = d.nodeZ(i);
                if (!IcebergField.habitat(seed, x, z, d, c)) continue;
                require(FrozenSeabed.frozenCover(seed, x, z, d, c, true) == 0, "Actual iceberg domain retains its accepted material path");
                iceberg = true; break;
            }
            require(iceberg, "Positive real iceberg exclusion witness");
            require(FrozenSeabed.cover(seed, 0, 0, 1, 0, 0) == 0 && FrozenSeabed.cover(seed, 0, 0, 5000, 2, 1) == 9,
                "Dry/shallow limit and steep original-rock support");
            int plants=0;
            for(int z=-64;z<=64;z++)for(int x=-64;x<=64;x++){
                int original=MarineShelf.cover(seed,x,z,true,new MarineShelf.Site(57,64,true,false,false,.02),new ClimateField.Sample(5,.7));
                int candidate=FrozenSeabed.cover(seed,x,z,6,.02,0);
                int changed=FrozenSeabed.replacement(original,candidate);
                if(original>=10&&original<=12){require(changed==original,"Existing cold shelf seagrass and its native supporting bed remain unchanged");plants++;}
                require(FrozenSeabed.replacement(original,0)==original,"All unfrozen marine paths retain exact prior codes");
            }
            require(plants>50,"Positive native shallow-water plant mask exercised");
            checkField(seed, "sorting", 48, FrozenSeabed::sorting);
            checkField(seed, "fines", 36, FrozenSeabed::fines);
            System.out.println("FROZEN_SEABED_PALETTE_PRESERVATION seed=" + seed + " samples=" + samples + " materials=" + materials);
        }
        System.out.println("FROZEN_SEABED_BLOCK_RESOLUTION_SEMANTICS_PRESERVATION_PASS");
    }

    private static void checkField(long seed, String name, int scale, SurfaceMaterialFieldsCheck.Field actual) {
        var fresh = SurfaceMaterialFieldsCheck.measure(actual, seed, scale);
        // Deliberately synthetic square selector: a regression control, not a
        // claim that the new frozen-only policy previously used this sampler.
        var square = SurfaceMaterialFieldsCheck.measure((s, x, z) -> SurfaceMaterialFieldsCheck.archivedSwamp(s, 0x46524F5A4E45474CL, x, z, scale), seed, scale);
        require(fresh.fourfold() < .10 && square.fourfold() > .12, "Actual material field rejects directional square control: " + name);
        System.out.println("FROZEN_SEABED_FIELD " + name + " seed=" + seed + " fourfold=" + fresh.fourfold() + " squareControl=" + square.fourfold());
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private FrozenSeabedCheck() {}
}
