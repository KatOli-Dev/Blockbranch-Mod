package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Registry-backed coast identity, physical columns, snow support and saved edits. */
public final class CoastalBeachSmoke {
    private static final Map<DrainageNetwork, BlockPos[]> FIXTURES = Collections.synchronizedMap(new WeakHashMap<>());

    public static BlockPos fixture(RealisticGenerator g, RandomState r, boolean snow) {
        var d = g.drainage(r);
        synchronized (FIXTURES) {
            var pair = FIXTURES.computeIfAbsent(d, unused -> new BlockPos[2]);
            int index = snow ? 1 : 0;
            if (pair[index] == null) {
                var scene = CoastalBeachScenes.forWorld(r.seed(),snow,d,new ClimateField(r.seed()));
                var p = scene == null ? blockbranch.realistic.CoastalBeachFixtures.fixture(d, r.seed(), snow) : new CoastalBeach.Point(scene.x(), scene.z());
                require(CoastalBeach.contains(p.x(), p.z(), d, new ClimateField(r.seed()), snow), "Natural review biome witness");
                pair[index] = new BlockPos(p.x(), d.sample(p.x(), p.z()).height(), p.z());
            }
            return pair[index];
        }
    }

    public static void restore(ServerLevel level, BlockPos bed) {
        var base = ((SparseColumn) level.getChunkAt(bed)).blockbranch$terrain().base();
        for (int dy = 0; dy <= 1; dy++) {
            var p = bed.above(dy);
            level.setBlock(p, base.state(p.getX(), p.getY(), p.getZ()), 3);
        }
    }

    public static void verify(ServerLevel level, RealisticGenerator g, boolean reload) {
        var r = level.getChunkSource().randomState();
        var d = g.drainage(r);
        var resolver = g.getBiomeSource().createUncachedResolver(r);
        var ecology = new GroundEcology.Field(d::sample, new ClimateField(r.seed()));
        for (boolean snow : new boolean[] {false, true}) {
            var bed = fixture(g, r, snow);
            var saved = bed.below(8);
            if (reload) require(level.getBlockState(saved).is(Blocks.CLAY), "Saved coast edit survives reopen");
            restore(level, bed);
            for (boolean elevated : new boolean[] {false, true})
                Blockbranch.LOGGER.info("COASTAL_REVIEW_CAMERA snow={} elevated={} coordinates={}", snow, elevated, Arrays.toString(camera(g, r, bed, elevated)));
            require(level.getBiome(bed).is(snow ? Biomes.SNOWY_BEACH : Biomes.BEACH), "Natural vanilla beach identity");
            int snowCount = 0, wet = 0, mixed = 0;
            var layers = new HashSet<Integer>();
            for (int cz = (bed.getZ() >> 4) - 2; cz <= (bed.getZ() >> 4) + 2; cz++)
                for (int cx = (bed.getX() >> 4) - 2; cx <= (bed.getX() >> 4) + 2; cx++) {
                    var chunk = level.getChunk(cx, cz);
                    var base = ((SparseColumn) chunk).blockbranch$terrain().base();
                    var copy = TerrainProfile.decode(base.encode(level.palettedContainerFactory()), level.palettedContainerFactory());
                    var sections = new HashMap<Integer, net.minecraft.world.level.chunk.LevelChunkSection>();
                    int selected = 0;
                    for (int qz = 0; qz < 4; qz++) for (int qx = 0; qx < 4; qx++) {
                        int x = cx * 16 + qx * 4, z = cz * 16 + qz * 4;
                        int surfaceQuartY = d.sample(x, z).height() >> 2;
                        var surfaceExpected = resolver.getNoiseBiome(x >> 2, surfaceQuartY, z >> 2);
                        if (surfaceExpected.is(snow ? Biomes.SNOWY_BEACH : Biomes.BEACH)) selected++;
                        // The cached palette deliberately represents the surface; vertical cave
                        // overrides must instead use the profile's absolute-quart biome accessor.
                        for (int layer = 0; layer < 4; layer++)
                            require(copy.biomes().get(qx, layer, qz).equals(surfaceExpected), "Coast decoded surface-palette identity");
                        for (int y : new int[] {-3000, 0, bed.getY() >> 2, 2000, surfaceQuartY, surfaceQuartY-12, surfaceQuartY-24, surfaceQuartY-80}) {
                            var expected = resolver.getNoiseBiome(x >> 2, y, z >> 2);
                            require(chunk.getNoiseBiome(x >> 2, y, z >> 2).equals(expected)
                                && base.biome(qx, y, qz).equals(expected)
                                && copy.biome(qx, y, qz).equals(expected), "Coast source/native/profile/decoded vertical biome identity at quart Y="+y);
                        }
                    }
                    if (selected > 0 && selected < 16) mixed++;
                    for (int z = cz * 16; z < cz * 16 + 16; z++) for (int x = cx * 16; x < cx * 16 + 16; x++) {
                        var s = d.sample(x, z);
                        for (int y = s.height() - 5; y <= Math.max(s.height() + 2, s.waterLevel()); y++) {
                            var expected = g.surfaceState(x, y, z, r);
                            var p = new BlockPos(x, y, z);
                            require(level.getBlockState(p) == expected && copy.state(x, y, z) == expected
                                && sections.computeIfAbsent(y >> 4, copy::generate).getBlockState(x & 15, y & 15, z & 15) == expected,
                                "Coast native/profile/client material agreement across chunk and biome seams");
                            if (expected.is(Blocks.SNOW)) {
                                snowCount++;
                                layers.add(expected.getValue(SnowLayerBlock.LAYERS));
                                require(expected.getValue(SnowLayerBlock.LAYERS) == OpenSnowCover.beachLayers(r.seed(), x, z, ecology.sample(x, z)),
                                    "Native snow thickness follows unchanged exposure/retention policy");
                                require(snow && s.waterLevel() == DrainageNetwork.DRY && expected.canSurvive(level, p)
                                    && level.getBlockState(p.below()).is(Blocks.SAND), "Supported thin snow on dry sand only");
                            }
                            if (expected.is(Blocks.WATER)) { wet++; require(expected.getFluidState().isSource(), "Ocean source water retained"); }
                        }
                        if ((x & 3) == 0 && (z & 3) == 0) for (var type : Heightmap.Types.values())
                            require(chunk.getHeight(type, x & 15, z & 15) + 1 == g.getBaseHeight(x, z, type, level, r), "All coast heightmap predicates");
                    }
                }
            require(mixed > 0, "Mixed dry/wet shoreline biome boundary");
            if (snow) {
                require(snowCount > 100 && layers.stream().allMatch(n -> n >= 1 && n <= 3), "Natural supported thin snow field: count=" + snowCount + " layers=" + layers);
                level.setBlock(bed, Blocks.AIR.defaultBlockState(), 3);
                require(level.getBlockState(bed.above()).isAir(), "Native beach snow loses unsupported cover");
                restore(level, bed);
            } else require(snowCount == 0, "Temperate beach negative snow control");
            level.setBlock(saved, Blocks.CLAY.defaultBlockState(), 2);
            var terrain = ((SparseColumn) level.getChunkAt(bed)).blockbranch$terrain();
            level.getChunkAt(bed).markUnsaved(); terrain.flush(); terrain.evictOutside(-100, -90);
            require(level.getBlockState(saved).is(Blocks.CLAY), "Coast edit survives eviction");
            Blockbranch.LOGGER.info("COASTAL_BEACH_NATIVE_PASS snow={} mode={} bed={} snowBlocks={} layers={} wet={} mixedChunks={}", snow, reload ? "RELOAD" : "NEW", bed, snowCount, layers, wet, mixed);
        }
    }

    public static double[] camera(RealisticGenerator g, RandomState r, BlockPos bed, boolean elevated) {
        var d = g.drainage(r);
        var scene = CoastalBeachScenes.forWorld(r.seed(),CoastalBeach.snowy(g.climate(bed.getX(),bed.getZ(),r)),d,new ClimateField(r.seed()));
        if (scene != null) {
            var view = CoastalBeachScenes.camera(scene, d, new ClimateField(r.seed()), elevated);
            double[] result = view.coordinates();
            int x = (int)Math.floor(view.x()), z = (int)Math.floor(view.z());
            var cover = g.surfaceState(x, d.sample(x, z).height() + 1, z, r);
            if (!elevated && cover.is(Blocks.SNOW)) result[1] += cover.getValue(SnowLayerBlock.LAYERS) / 8.0;
            int eye = (int)Math.floor(result[1] + 1.62);
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++)
                require(g.surfaceState(x + dx, eye + dy, z + dz, r).isAir(), "Clear natural review lens");
            return result;
        }
        double dx = 0, dz = 1; int lowest = Integer.MAX_VALUE;
        for (int n = 0; n < 16; n++) {
            double a = n * Math.PI / 8, ux = Math.cos(a), uz = Math.sin(a);
            int x = bed.getX() + (int) Math.round(ux * 128), z = bed.getZ() + (int) Math.round(uz * 128);
            var s = d.sample(x, z);
            if (d.oceanWater(x, z, s) && s.height() < lowest) { lowest = s.height(); dx = ux; dz = uz; }
        }
        require(lowest < Integer.MAX_VALUE, "Ocean in beach camera view");
        double x = bed.getX() - dx * (elevated ? 36 : 12) - dz * 24 + .5;
        double z = bed.getZ() - dz * (elevated ? 36 : 12) + dx * 24 + .5;
        int h = d.sample((int) Math.floor(x), (int) Math.floor(z)).height();
        double y = h + 1 + (elevated ? 42 : .375);
        for (int dy = 2; dy <= 3; dy++) require(g.surfaceState((int)Math.floor(x), h + dy, (int)Math.floor(z), r).isAir(), "Clear beach lens");
        double tx = bed.getX() + dx * (elevated ? 24 : 4) + dz * 36 + .5;
        double tz = bed.getZ() + dz * (elevated ? 24 : 4) - dx * 36 + .5;
        double yaw = Math.toDegrees(Math.atan2(-(tx - x), tz - z));
        return new double[] {x, y, z, yaw, elevated ? 28 : 5};
    }

    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private CoastalBeachSmoke() {}
}
