package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import blockbranch.cubic.TerrainProfile;
import java.util.HashMap;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

/** Actual bed materials across column seams, stored profiles and saved native edits. */
final class FrozenSeabedSmoke {
    private record Snapshot(TerrainProfile base, HashMap<Integer, LevelChunkSection> sections) {}
    static void verify(ServerLevel level, RealisticGenerator generator, boolean reload, int kind) {
        var random = level.getChunkSource().randomState();
        var drainage = generator.drainage(random);
        var point = SeaIceCaptureViews.underwaterFixture(generator, random, kind);
        var bed = new BlockPos(point.getX(), drainage.sample(point.getX(), point.getZ()).height(), point.getZ());
        var home = ((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
        if (reload) require(level.getBlockState(bed).is(Blocks.COBBLESTONE), "Saved native seabed edit reopens");
        level.setBlock(bed, home.state(bed.getX(), bed.getY(), bed.getZ()), 2);
        var copies = new HashMap<Long, Snapshot>();
        var materials = new HashSet<String>();
        int samples = 0;
        for (int z = bed.getZ() - 64; z <= bed.getZ() + 64; z += 2) for (int x = bed.getX() - 64; x <= bed.getX() + 64; x += 2) {
            var sample = drainage.sample(x, z);
            var pos = new BlockPos(x, sample.height(), z);
            var chunk = level.getChunkAt(pos);
            var profile = ((SparseColumn)chunk).blockbranch$terrain().base();
            long key = ((long)(x >> 4) << 32) ^ ((z >> 4) & 0xffffffffL);
            var snapshot = copies.computeIfAbsent(key, ignored -> new Snapshot(TerrainProfile.decode(profile.encode(level.palettedContainerFactory()), level.palettedContainerFactory()), new HashMap<>()));
            var copy = snapshot.base();
            require(profile.groundHeight(x, z) == sample.height() && copy.groundHeight(x, z) == sample.height(), "Material-only change retains physical bed height");
            for (int y = sample.height() - 3; y <= sample.height() + 1; y++) {
                var expected = generator.surfaceState(x, y, z, random);
                require(level.getBlockState(new BlockPos(x, y, z)) == expected && copy.state(x, y, z) == expected
                    && snapshot.sections().computeIfAbsent(y >> 4, copy::generate).getBlockState(x & 15, y & 15, z & 15) == expected, "Native/analytic/decoded/materialized bed agree at seams");
            }
            require(level.getFluidState(pos.above()).isSource(), "Connected water remains above the bed");
            if ((x & 7) == 0 && (z & 7) == 0) for (var type : Heightmap.Types.values())
                require(chunk.getHeight(type, x & 15, z & 15) + 1 == generator.getBaseHeight(x, z, type, level, random), "Unchanged physical bed/water/ice heightmap " + type);
            materials.add(level.getBlockState(pos).getBlock().toString());
            samples++;
        }
        require(materials.size() > 1, "Actual material variation in the existing underwater witness");
        level.setBlock(bed, Blocks.COBBLESTONE.defaultBlockState(), 2);
        level.getChunkAt(bed).markUnsaved();
        ((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().flush();
        Blockbranch.LOGGER.info("FROZEN_SEABED_NATIVE_PASS mode={} kind={} bed={} samples={} materials={}", reload ? "RELOAD" : "NEW", kind, bed, samples, materials);
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private FrozenSeabedSmoke() {}
}
