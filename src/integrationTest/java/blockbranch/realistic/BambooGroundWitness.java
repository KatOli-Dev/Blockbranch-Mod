package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Preserve the original canopy/terrain and version-1 floor; only requested litter may be thinned. */
final class BambooGroundWitness {
    private static final String[] BEFORE = {
        "a71484292b46f2311b15281fc7544698a9cc5b70807bdfc4758a709cca45470f",
        "94916bfa10265ab27f0df10b0ce190812da9963a755a086b0815369b8b218432",
        "73725d71cccefe61ff6e801eca455c92260abcaba1b0216d5ecd0f40ece81eaa"
    };

    static void verify(ServerLevel level, RealisticGenerator generator, BlockPos bed) {
        var random = level.getChunkSource().randomState();
        if (random.seed() != 424242) return;
        if (bed.getX() != 739891 || bed.getY() != 538 || bed.getZ() != -520121)
            throw new AssertionError("The ground-only revision moved the established natural Bamboo Jungle witness: " + bed);
        var canopy = sha256(); var plants = sha256(); var terrain = sha256();
        var drainage = generator.drainage(random); var climate = new ClimateField(random.seed());
        var soils = new TreeMap<Integer, Integer>();
        var ecology=new GroundEcology.Field(drainage::sample,climate);int oldLitter=0,newLitter=0,ferns=0,grassCells=0;
        for (int cz = (bed.getZ() >> 4) - 2; cz <= (bed.getZ() >> 4) + 2; cz++)
            for (int cx = (bed.getX() >> 4) - 2; cx <= (bed.getX() >> 4) + 2; cx++) {
                var base = ((SparseColumn)level.getChunk(cx, cz)).blockbranch$terrain().base();
                var tag = base.encode(level.palettedContainerFactory());
                int[] packed = base.canopy().encode(), ground = tag.getIntArray("ground").orElseThrow(), water = tag.getIntArray("water").orElseThrow();
                byte[] covers = tag.getByteArray("woodland").orElseThrow();
                var legacy=tag.copy();legacy.putInt("version",59);var oldCovers=covers.clone();
                for(int i=0;i<oldCovers.length;i++)if(oldCovers[i]==BambooSoil.GRASS_LITTER)oldCovers[i]=57;else if(oldCovers[i]==BambooSoil.GRASS_FERN)oldCovers[i]=60;
                legacy.putByteArray("woodland",oldCovers);
                var legacyCopy=blockbranch.cubic.TerrainProfile.decode(legacy,level.palettedContainerFactory());
                add(canopy, cx); add(canopy, cz); add(canopy, packed.length);
                for (int value : packed) add(canopy, value);
                for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    int wx = cx * 16 + x, wz = cz * 16 + z, i = z * 16 + x;
                    add(terrain, wx); add(terrain, wz); add(terrain, ground[i]); add(terrain, water[i]);
                    int cover = TropicalForestVariants.surfaceKind(random.seed(), wx, wz, drainage, climate) == 2 ? BambooSoil.vegetation(covers[i]) : 0;
                    if(covers[i]==BambooSoil.GRASS_LITTER||covers[i]==BambooSoil.GRASS_FERN){
                        if(!legacyCopy.state(wx,ground[i],wz).is(Blocks.DIRT)
                            ||legacyCopy.state(wx,ground[i]+1,wz)!=base.state(wx,ground[i]+1,wz))
                            throw new AssertionError("Version-59 floor and plant semantics changed on decode");
                    }
                    int priorPlant=cover;
                    if(cover!=0){
                        var site=ecology.sample(wx,wz).shade(base.canopy().topY(wx,wz)>ground[i]+2);
                        boolean clear=base.canopy().state(wx,ground[i]+1,wz)==null;
                        priorPlant=TropicalForestVariants.plantCover(random.seed(),wx,wz,site,clear,TropicalForestVariants.BAMBOO);
                        if(cover!=priorPlant&&!(priorPlant==9&&(cover==8||cover==11)))
                            throw new AssertionError("Only literal Bamboo leaf litter may change");
                        int prior=BambooSoil.encode(random.seed(),wx,wz,site,clear,priorPlant);
                        int soil=prior==11?3:BambooSoil.soil(prior);
                        var expected=soil==0?Blocks.DIRT:soil==1?Blocks.COARSE_DIRT:soil==3?Blocks.GRASS_BLOCK:Blocks.ROOTED_DIRT;
                        if(!level.getBlockState(new BlockPos(wx,ground[i],wz)).is(expected))
                            throw new AssertionError("Litter thinning changed version-1 floor materials");
                        if(soil==3)grassCells++;
                        if(priorPlant==9)oldLitter++;if(cover==9)newLitter++;if(cover==10)ferns++;
                        if(priorPlant==9&&cover!=9&&!level.getBlockState(new BlockPos(wx,ground[i]+1,wz)).isAir())
                            throw new AssertionError("Removed litter cell is not empty");
                    }
                    add(plants, wx); add(plants, wz); add(plants, priorPlant);
                    if (cover == 9 || cover == 10 || cover == 12) {
                        var expected = cover == 9 ? Blocks.LEAF_LITTER : cover == 10 ? Blocks.FERN : Blocks.SHORT_GRASS;
                        if (!level.getBlockState(new BlockPos(wx, ground[i] + 1, wz)).is(expected))
                            throw new AssertionError("A recorded floor plant changed: " + wx + "," + wz);
                    }
                    if (BambooSoil.encoded(covers[i])) {
                        int soil = BambooSoil.soil(covers[i]);
                        var expected = soil == 0 ? Blocks.DIRT : soil == 1 ? Blocks.COARSE_DIRT : soil == 3 ? Blocks.GRASS_BLOCK : Blocks.ROOTED_DIRT;
                        if (!level.getBlockState(new BlockPos(wx, ground[i], wz)).is(expected))
                            throw new AssertionError("Encoded mineral soil differs from native block");
                        soils.merge(soil, 1, Integer::sum);
                    }
                }
            }
        String[] actual = {hex(canopy), hex(plants), hex(terrain)};
        if (!Arrays.equals(actual, BEFORE)) throw new AssertionError("Bamboo revision changed canopy/prior-plants/height-water: " + Arrays.toString(actual));
        if(newLitter<20||newLitter>oldLitter*.35||oldLitter+ferns!=1547)
            throw new AssertionError("Restrained literal litter with all original ferns retained: "+oldLitter+" -> "+newLitter+" ferns="+ferns);
        Blockbranch.LOGGER.info("BAMBOO_LITERAL_LITTER_THINNING_PASS oldLitter={} newLitter={} ferns={} grassFloorCells={} floorMaterialsUnchanged=true",oldLitter,newLitter,ferns,grassCells);
        if (soils.size() < 2) throw new AssertionError("Established witness lacks meaningful mineral-soil variation: " + soils);
        Blockbranch.LOGGER.info("BAMBOO_GROUND_PRECHANGE_WITNESS_PASS canopy={} priorPlants={} heightWater={} soils={}", actual[0], actual[1], actual[2], soils);
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void add(MessageDigest digest, int value) {
        digest.update((byte)(value >>> 24)); digest.update((byte)(value >>> 16));
        digest.update((byte)(value >>> 8)); digest.update((byte)value);
    }
    private static String hex(MessageDigest digest) { return HexFormat.of().formatHex(digest.digest()); }
    private BambooGroundWitness() {}
}
