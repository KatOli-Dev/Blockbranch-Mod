package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.*;

/** Natural canopy, clipped queries, old habitats, cross-chunk state and persistent tree edits. */
public final class WarmSmoke {
    public static BlockPos fixture(RealisticGenerator generator,RandomState random) {
        var p=blockbranch.realistic.WarmWoodlandFixtures.fixture(generator.drainage(random),random.seed());
        return new BlockPos(p.x(),generator.drainage(random).sample(p.x(),p.z()).height(),p.z());
    }
    public static BlockPos testLog(BlockPos bed) { return bed.above(30); }
    public static BlockPos testLeaf(BlockPos bed) { return testLog(bed).east(); }
    public static void restorePhysics(ServerLevel level,BlockPos bed) {
        for(var pos:List.of(testLog(bed),testLeaf(bed),bed.above())) {
            var base=((SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();
            level.setBlock(pos,base.state(pos.getX(),pos.getY(),pos.getZ()),3);
        }
    }
    public static void verify(ServerLevel level,RealisticGenerator generator,BlockPos bed,boolean reload) {
        var random=level.getChunkSource().randomState();var source=generator.getBiomeSource().createUncachedResolver(random);
        require(level.getBiome(bed).is(Blockbranch.id("warm_subhumid_oak_pine_woodland")),"Natural woodland biome");
        require(level.getBlockState(bed.above()).is(Blocks.SPRUCE_LOG),"Natural pine architecture using evergreen timber");
        var cut=bed.above(3);
        if(reload) require(level.getBlockState(cut).isAir(),"Saved removed trunk block survives reopen");
        var rootBase=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
        level.setBlock(cut,rootBase.state(cut.getX(),cut.getY(),cut.getZ()),2);
        int leaves=0,logs=0,oak=0;var components=new HashSet<Integer>();
        for(int cz=(bed.getZ()>>4)-1;cz<=(bed.getZ()>>4)+1;cz++) for(int cx=(bed.getX()>>4)-1;cx<=(bed.getX()>>4)+1;cx++) {
            var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();
            var decoded=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
            require(Arrays.equals(base.canopy().encode(),decoded.canopy().encode()),"Packed canopy round trip");
            var materialized=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
            for(int packed:base.canopy().encode()) {
                int key=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;
                var expected=base.state(x,y,z);var pos=new BlockPos(x,y,z);
                require(chunk.getBlockState(pos)==expected && decoded.state(x,y,z)==expected,"Native/saved canopy cell across chunk seam");
                require(materialized.computeIfAbsent(y>>4,decoded::generate).getBlockState(x&15,y&15,z&15)==expected,"Client canopy materialization");
                if(expected.is(Blocks.SPRUCE_LEAVES) || expected.is(Blocks.OAK_LEAVES)) {
                    leaves++;if(expected.is(Blocks.OAK_LEAVES)) oak++;
                    require(!expected.getValue(LeavesBlock.PERSISTENT) && expected.getValue(LeavesBlock.DISTANCE)<=6,"Non-persistent naturally supported leaves");
                    require(!expected.isRandomlyTicking(),"Supported leaves do not require random decay scans");
                    int distance=expected.getValue(LeavesBlock.DISTANCE);boolean support=false;
                    for(var direction:Direction.values()) {
                        var near=level.getBlockState(pos.relative(direction));
                        if((near.is(Blocks.SPRUCE_LOG) || near.is(Blocks.OAK_LOG)) && distance==1) support=true;
                        if((near.is(Blocks.SPRUCE_LEAVES) || near.is(Blocks.OAK_LEAVES)) && near.getValue(LeavesBlock.DISTANCE)<distance) support=true;
                    }
                    require(support,"Actual cross-chunk canopy retains a live leaf-support path");
                } else logs++;
            }
            for(int oz=0;oz<16;oz+=4) for(int ox=0;ox<16;ox+=4) {
                int x=cx*16+ox,z=cz*16+oz,h=generator.drainage(random).sample(x,z).height();
                int cover=generator.warmCover(x,z,random);components.add(cover);
                for(int y=h-3;y<=Math.max(h+2,base.canopy().topY(x,z));y++) require(base.state(x,y,z)==generator.surfaceState(x,y,z,random),"Tree and forest-floor query agreement");
                for(var bounds:new LevelHeightAccessor[]{level,LevelHeightAccessor.create(h-2,2),LevelHeightAccessor.create(h+1,2),LevelHeightAccessor.create(h+7,3),LevelHeightAccessor.create(h+25,2)}) {
                    var column=generator.getBaseColumn(x,z,bounds,random);
                    for(var type:Heightmap.Types.values()) {
                        int expected=bounds.getMinY();
                        for(int y=Math.min(bounds.getMaxY(),Math.max(h+2,base.canopy().topY(x,z)));y>=bounds.getMinY();y--)
                            if(type.isOpaque().test(column.getBlock(y))) { expected=y+1;break; }
                        require(generator.getBaseHeight(x,z,type,bounds,random)==expected,"Canopy-aware clipped generator height "+type);
                        if(bounds==level) require(chunk.getHeight(type,x&15,z&15)+1==expected && base.top(type.isOpaque(),x,z,ColumnLimits.MAX_Y-1)==expected,"Leaves/logs/floor heightmap distinction "+type);
                    }
                }
                for(int qy:new int[]{-3000,0,h>>2,2000}) require(chunk.getNoiseBiome(x>>2,qy,z>>2).equals(source.getNoiseBiome(x>>2,qy,z>>2)),"All-altitude stored woodland palette");
            }
        }
        boolean[] floorWitness=new boolean[9];int remaining=8;
        for(int dz=-128;dz<=128 && remaining>0;dz++) for(int dx=-128;dx<=128 && remaining>0;dx++) {
            int x=bed.getX()+dx,z=bed.getZ()+dz,c=generator.warmCover(x,z,random);
            if(c==0 || floorWitness[c])continue;
            int h=generator.drainage(random).sample(x,z).height();var chunk=level.getChunk(x>>4,z>>4);var base=((SparseColumn)chunk).blockbranch$terrain().base();
            if(base.canopy().state(x,h+1,z)!=null)continue;
            for(int y=h-3;y<=h+2;y++) require(chunk.getBlockState(new BlockPos(x,y,z))==RealisticGenerator.material(y,h,DrainageNetwork.DRY,0,0,0,0,0,0,0,0,c),"Every warm floor component reaches native blocks");
            if(c>=3 && c!=7)require(chunk.getBlockState(new BlockPos(x,h+1,z)).canSurvive(level,new BlockPos(x,h+1,z)),"Every natural warm plant has suitable support");
            floorWitness[c]=true;remaining--;components.add(c);
        }
        require(remaining==0,"All eight native warm floor components");
        boundary(level,generator,bed);
        require(leaves>500 && logs>30 && oak>30,"Mixed, nontrivial native canopy");
        require(components.contains(4) && components.contains(5) && components.contains(6) && components.contains(7),"Dry grass, ferns, fungi and podzol understory");
        var drops=Block.getDrops(Blocks.SPRUCE_LOG.defaultBlockState(),level,bed.above(),null,null,new ItemStack(Items.IRON_AXE));
        require(drops.size()==1 && drops.getFirst().is(Items.SPRUCE_LOG),"Ordinary usable timber drops");
        var leaf=Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,2);
        drops=Block.getDrops(leaf,level,bed.above(10),null,null,new ItemStack(Items.SHEARS));
        require(drops.size()==1 && drops.getFirst().is(Items.SPRUCE_LEAVES),"Sheared natural foliage item");
        var invalid=rootBase.encode(level.palettedContainerFactory());invalid.putInt("version",9);boolean rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"Pre-warm profile rejected");
        for(int[] bad:new int[][]{new int[CanopyLayer.MAX_BLOCKS+1],{0},{(rootBase.canopy().encode()[0]&~blockbranch.cubic.CanopyLayer.STATE_MASK)}}) {
            invalid=rootBase.encode(level.palettedContainerFactory());invalid.putIntArray("canopy",bad);rejected=false;
            try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
            require(rejected,"Malformed canopy position/code/size rejected");
        }
        var saved=bed.below(8);var savedState=Blocks.LEAF_LITTER.defaultBlockState();
        if(reload)require(level.getBlockState(saved)==savedState,"Saved forest-floor edit survives reopen");
        level.setBlock(saved.below(),Blocks.PODZOL.defaultBlockState(),2);level.setBlock(saved,savedState,2);
        for(var plant:List.of(Blocks.FERN.defaultBlockState(),Blocks.LEAF_LITTER.defaultBlockState(),Blocks.BROWN_MUSHROOM.defaultBlockState(),Blocks.DEAD_BUSH.defaultBlockState())) {
            var test=bed.below(12);level.setBlock(test.below(),Blocks.PODZOL.defaultBlockState(),2);level.setBlock(test,plant,2);
            require(plant.canSurvive(level,test),"Warm understory survives soil");level.setBlock(test.below(),Blocks.AIR.defaultBlockState(),3);require(level.getBlockState(test).isAir(),"Understory support removal");
        }
        var malformed=rootBase.encode(level.palettedContainerFactory());malformed.putByteArray("warm",new byte[255]);rejected=false;
        try { TerrainProfile.decode(malformed,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }require(rejected,"Truncated warm array rejected");
        var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();
        level.setBlock(cut,Blocks.AIR.defaultBlockState(),2);chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);
        require(level.getBlockState(cut).isAir() && level.getBlockState(saved)==savedState,"Removed trunk and forest-floor edit survive edit eviction");
        Blockbranch.LOGGER.info("WARM_SUBHUMID_OAK_PINE_WOODLAND_PASS {} bed={} logs={} leaves={} oakLeaves={} floorComponents={}",reload ? "RELOAD" : "NEW",bed,logs,leaves,oak,components);
    }
    private static void boundary(ServerLevel level,RealisticGenerator generator,BlockPos bed) {
        var random=level.getChunkSource().randomState();var source=generator.getBiomeSource().createUncachedResolver(random);
        var edge=blockbranch.realistic.WarmWoodlandFixtures.transition(generator.drainage(random),random.seed(),false);
        for(int dz=-32;dz<=32;dz+=16) for(int dx=-32;dx<=32;dx+=16) {
            int x=Math.floorDiv(edge.insideX()+dx,16)*16,z=Math.floorDiv(edge.insideZ()+dz,16)*16,count=0;
            for(int qz=0;qz<4;qz++) for(int qx=0;qx<4;qx++) if(source.getNoiseBiome((x>>2)+qx,0,(z>>2)+qz).is(Blockbranch.id("warm_subhumid_oak_pine_woodland"))) count++;
            if(count==0 || count==16) continue;
            for(int oz=0;oz<16;oz++) for(int ox=0;ox<20;ox++) {
                int wx=x+ox,wz=z+oz,h=generator.drainage(random).sample(wx,wz).height();var chunk=level.getChunk(wx>>4,wz>>4);
                int top=Math.max(h+2,((SparseColumn)chunk).blockbranch$terrain().base().canopy().topY(wx,wz));
                for(int y=h-3;y<=top;y++) require(chunk.getBlockState(new BlockPos(wx,y,wz))==generator.surfaceState(wx,y,wz,random),"Mixed woodland/native edge floor and canopy agreement");
                require(chunk.getNoiseBiome(wx>>2,0,wz>>2).equals(source.getNoiseBiome(wx>>2,0,wz>>2)),"Woodland biome edge agrees with source");
            }
            Blockbranch.LOGGER.info("WARM_SUBHUMID_OAK_PINE_WOODLAND_BOUNDARY_PASS x={} z={} woodlandQuartCells={}",x,z,count);return;
        }
        throw new AssertionError("No mixed woodland stand boundary");
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private WarmSmoke() {}
}
