package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Actual grading, biome boundaries, height/fluid queries, drops and overlay persistence. */
public final class RiverMarginSmoke {
    public static BlockPos fixture(RealisticGenerator generator,RandomState random) {
        var point=blockbranch.realistic.GravelRiverMarginFixtures.fixture(generator.drainage(random),random.seed());
        return new BlockPos(point.x(),generator.drainage(random).sample(point.x(),point.z()).height(),point.z());
    }
    private static List<BlockPos> cases(RealisticGenerator generator,RandomState random,BlockPos center) {
        BlockPos[] found=new BlockPos[4];
        for(int z=center.getZ()-32;z<center.getZ()+32;z++) for(int x=center.getX()-32;x<center.getX()+32;x++) {
            int cover=generator.riverMarginCover(x,z,random);
            if(cover>0 && cover<=4 && found[cover-1]==null) found[cover-1]=new BlockPos(x,generator.drainage(random).sample(x,z).height(),z);
            if(Arrays.stream(found).allMatch(Objects::nonNull)) return List.of(found);
        }
        throw new AssertionError("Natural shingle/gravel/soil/grass witnesses required");
    }
    public static BlockPos gravelStart(BlockPos bed) { return bed.offset((bed.getX()&15)<14 ? 1 : -1,6,0); }
    public static BlockPos gravelLanding(ServerLevel level,BlockPos bed) {
        var pos=gravelStart(bed); var base=((SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();
        return new BlockPos(pos.getX(),base.top(Heightmap.Types.OCEAN_FLOOR.isOpaque(),pos.getX(),pos.getZ(),ColumnLimits.MAX_Y-1),pos.getZ());
    }
    public static void restorePhysics(ServerLevel level,BlockPos bed) {
        for(var column:List.of(bed,gravelStart(bed))) {
            var base=((SparseColumn)level.getChunkAt(column)).blockbranch$terrain().base();
            int bottom=Math.min(bed.getY(),gravelLanding(level,bed).getY())-1;
            for(int y=bottom;y<=bed.getY()+7;y++) level.setBlock(new BlockPos(column.getX(),y,column.getZ()),base.state(column.getX(),y,column.getZ()),3);
        }
    }
    public static void verify(ServerLevel level,RealisticGenerator generator,BlockPos bed,boolean reload) {
        var random=level.getChunkSource().randomState(); var resolver=generator.getBiomeSource().createUncachedResolver(random);
        require(level.getBiome(bed).is(Blockbranch.id("gravel_bed_river_margin")),"Actual river-margin identity");
        int[] counts=new int[5];
        for(var witness:cases(generator,random,bed)) {
            var chunk=level.getChunkAt(witness); var base=((SparseColumn)chunk).blockbranch$terrain().base();
            var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            int wx=witness.getX(),wz=witness.getZ(),h=witness.getY(),cover=generator.riverMarginCover(wx,wz,random);
            var sample=generator.drainage(random).sample(wx,wz); counts[cover]++;
            var expectedSurface=switch(cover) { case 1->RiverMarginContent.PACKED_RIVER_SHINGLE; case 2->Blocks.GRAVEL; default->Blocks.COARSE_DIRT; };
            require(level.getBlockState(witness).is(expectedSurface),"Natural sorted river substrate "+cover);
            for(int y=h-3;y<=Math.max(h+2,sample.waterLevel());y++) {
                var expected=RealisticGenerator.material(y,h,sample.waterLevel(),0,0,cover);
                require(chunk.getBlockState(new BlockPos(wx,y,wz))==expected && decoded.state(wx,y,wz)==expected,"Generated and serialized sediment/foliage/water");
                require(decoded.generate(y>>4).getBlockState(wx&15,y&15,wz&15)==expected,"Materialized/client river surface");
                if(y>h && y<sample.waterLevel()) require(expected.is(Blocks.WATER) && expected.getFluidState().isSource(),"River water preserved above new bed");
            }
            require(base.top(s->s.is(Blocks.SHORT_GRASS),wx,wz,ColumnLimits.MAX_Y-1)==(cover==4 ? h+2 : ColumnLimits.MIN_Y),"Pioneer grass top query");
            if(cover==4) require(level.getBlockState(witness.above()).canSurvive(level,witness.above()),"Grass rooted on the sediment pocket");
            for(var bounds:new LevelHeightAccessor[]{level,LevelHeightAccessor.create(h-2,2),LevelHeightAccessor.create(h,2),LevelHeightAccessor.create(h+1,2),LevelHeightAccessor.create(h+4,2)}) {
                var column=generator.getBaseColumn(wx,wz,bounds,random);
                for(var type:Heightmap.Types.values()) {
                    int expected=bounds.getMinY();
                    for(int y=Math.min(bounds.getMaxY(),Math.max(h+2,sample.waterLevel()==DrainageNetwork.DRY ? h : sample.waterLevel()));y>=bounds.getMinY();y--)
                        if(type.isOpaque().test(column.getBlock(y))) { expected=y+1; break; }
                    require(generator.getBaseHeight(wx,wz,type,bounds,random)==expected,"Clipped river height query "+type);
                    if(bounds==level) require(chunk.getHeight(type,wx&15,wz&15)+1==expected,"Actual river heightmap "+type);
                }
            }
            for(int y:new int[]{-3000,0,h>>2,2000}) require(chunk.getNoiseBiome(wx>>2,y,wz>>2).equals(resolver.getNoiseBiome(wx>>2,y,wz>>2))
                && decoded.biomes().get((wx&15)>>2,y&3,(wz&15)>>2).equals(resolver.getNoiseBiome(wx>>2,y,wz>>2)),"River biome source/storage round trip");
        }
        for(int cover=1;cover<=4;cover++) require(counts[cover]>0,"Every grading zone exercised");
        boundary(level,generator,bed);
        var drops=Block.getDrops(RiverMarginContent.PACKED_RIVER_SHINGLE.defaultBlockState(),level,bed,null,null,new ItemStack(Items.IRON_SHOVEL));
        require(drops.size()==1 && drops.getFirst().is(RiverMarginContent.SHINGLE_ITEM) && drops.getFirst().getCount()==1,"Shingle harvested without extra ingredient item");
        require(new ItemStack(Items.IRON_SHOVEL).getDestroySpeed(RiverMarginContent.PACKED_RIVER_SHINGLE.defaultBlockState())>
            new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(RiverMarginContent.PACKED_RIVER_SHINGLE.defaultBlockState()),"Shovel is effective for packed shingle");
        var chunk=level.getChunkAt(bed); var terrain=((SparseColumn)chunk).blockbranch$terrain();
        var invalid=terrain.base().encode(level.palettedContainerFactory()); invalid.putInt("version",3);
        boolean rejected=false; try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"Pre-river procedural profile rejected");
        invalid=terrain.base().encode(level.palettedContainerFactory()); invalid.putByteArray("river_margin",new byte[255]);rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"Malformed river cover rejected");
        var saved=bed.below(8);
        if(reload) require(level.getBlockState(saved).is(RiverMarginContent.PACKED_RIVER_SHINGLE),"Saved shingle edit survives reopen");
        level.setBlock(saved,RiverMarginContent.PACKED_RIVER_SHINGLE.defaultBlockState(),2);
        chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);
        require(level.getBlockState(saved).is(RiverMarginContent.PACKED_RIVER_SHINGLE),"Shingle overlay survives section eviction");
        require(level.getBlockState(bed).is(RiverMarginContent.PACKED_RIVER_SHINGLE),"Natural shingle survives overlay reload");
        Blockbranch.LOGGER.info("GRAVEL_RIVER_MARGIN_PASS {} bed={} gradingWitnesses={} retainedSections={}",reload ? "RELOAD" : "NEW",bed,Arrays.toString(counts),((SparseColumn)chunk).blockbranch$sections().size());
    }
    private static void boundary(ServerLevel level,RealisticGenerator generator,BlockPos bed) {
        var random=level.getChunkSource().randomState();var source=generator.getBiomeSource().createUncachedResolver(random);
        for(int dz=-64;dz<=64;dz+=16) for(int dx=-64;dx<=64;dx+=16) {
            int wx=Math.floorDiv(bed.getX()+dx,16)*16,wz=Math.floorDiv(bed.getZ()+dz,16)*16,count=0;
            for(int z=0;z<4;z++) for(int x=0;x<4;x++) if(source.getNoiseBiome((wx>>2)+x,0,(wz>>2)+z).is(Blockbranch.id("gravel_bed_river_margin"))) count++;
            if(count==0 || count==16) continue;
            for(int x=0;x<20;x++) for(int z=0;z<16;z++) {
                var sample=generator.drainage(random).sample(wx+x,wz+z);var chunk=level.getChunk((wx+x)>>4,(wz+z)>>4);
                int cover=generator.riverMarginCover(wx+x,wz+z,random);
                for(int y=sample.height()-2;y<=sample.height()+2;y++) require(chunk.getBlockState(new BlockPos(wx+x,y,wz+z))
                    ==generator.surfaceState(wx+x,y,wz+z,random),"Native chunk edge sediment agreement");
                require(chunk.getNoiseBiome((wx+x)>>2,0,(wz+z)>>2).equals(source.getNoiseBiome((wx+x)>>2,0,(wz+z)>>2)),"River biome edge agreement");
            }
            Blockbranch.LOGGER.info("GRAVEL_RIVER_MARGIN_BOUNDARY_PASS x={} z={} riverQuartCells={}",wx,wz,count);return;
        }
        throw new AssertionError("No mixed river-margin boundary");
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private RiverMarginSmoke() {}
}
