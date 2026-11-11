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
public final class ForestSmoke {
    public static BlockPos fixture(RealisticGenerator generator,RandomState random) {
        var p=blockbranch.realistic.ForestFixtures.fixture(generator.drainage(random),random.seed());
        return new BlockPos(p.x(),generator.drainage(random).sample(p.x(),p.z()).height(),p.z());
    }
    public static BlockPos fixture(RealisticGenerator generator,RandomState random,boolean birch){
        if(!birch)return fixture(generator,random);var p=blockbranch.realistic.BirchForestFixtures.fixture(generator.drainage(random),random.seed(),Boolean.getBoolean("blockbranch.birchForestSecondSite"));return new BlockPos(p.x(),generator.drainage(random).sample(p.x(),p.z()).height(),p.z());
    }
    public static BlockPos testLog(BlockPos bed) { return bed.above(40); }
    public static BlockPos testLeaf(BlockPos bed) { return testLog(bed).east(); }
    public static void restorePhysics(ServerLevel level,BlockPos bed) {
        for(var pos:List.of(testLog(bed),testLeaf(bed),bed.above())) {
            var base=((SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();
            level.setBlock(pos,base.state(pos.getX(),pos.getY(),pos.getZ()),3);
        }
    }
    public static void verify(ServerLevel level,RealisticGenerator generator,BlockPos bed,boolean reload){verify(level,generator,bed,reload,false);}
    public static void verify(ServerLevel level,RealisticGenerator generator,BlockPos bed,boolean reload,boolean birchForest) {
        var random=level.getChunkSource().randomState();var source=generator.getBiomeSource().createUncachedResolver(random);
        require(level.getBiome(bed).is(birchForest?net.minecraft.world.level.biome.Biomes.BIRCH_FOREST:net.minecraft.world.level.biome.Biomes.FOREST),"Natural woodland biome");
        require(level.getBlockState(bed.above()).is(birchForest?Blocks.BIRCH_LOG:Blocks.OAK_LOG),"Natural selected woodland stem");
        var cut=bed.above(3);
        if(reload) require(level.getBlockState(cut).isAir(),"Saved removed trunk block survives reopen");
        var rootBase=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
        level.setBlock(cut,rootBase.state(cut.getX(),cut.getY(),cut.getZ()),2);
        int leaves=0,logs=0,birch=0,birchLogs=0;var components=new HashSet<Integer>();
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
                if(expected.is(Blocks.OAK_LEAVES) || expected.is(Blocks.BIRCH_LEAVES)) {
                    leaves++;if(expected.is(Blocks.BIRCH_LEAVES)) birch++;
                    require(!expected.getValue(LeavesBlock.PERSISTENT) && expected.getValue(LeavesBlock.DISTANCE)<=6,"Non-persistent naturally supported leaves");
                    require(!expected.isRandomlyTicking(),"Supported leaves do not require random decay scans");
                    int distance=expected.getValue(LeavesBlock.DISTANCE);boolean support=false;
                    for(var direction:Direction.values()) {
                        var near=level.getBlockState(pos.relative(direction));
                        if((near.is(Blocks.OAK_LOG) || near.is(Blocks.BIRCH_LOG)) && distance==1) support=true;
                        if((near.is(Blocks.OAK_LEAVES) || near.is(Blocks.BIRCH_LEAVES)) && near.getValue(LeavesBlock.DISTANCE)<distance) support=true;
                    }
                    require(support,"Actual cross-chunk canopy retains a live leaf-support path");
                } else {logs++;if(expected.is(Blocks.BIRCH_LOG))birchLogs++;}
            }
            for(int oz=0;oz<16;oz++)for(int ox=0;ox<16;ox++){
                int wx=cx*16+ox,wz=cz*16+oz,h=generator.drainage(random).sample(wx,wz).height();var pos=new BlockPos(wx,h+1,wz);var state=chunk.getBlockState(pos);
                if(state.is(Blocks.DANDELION)||state.is(Blocks.OXEYE_DAISY)||state.is(Blocks.FERN)||state.is(Blocks.BROWN_MUSHROOM)||state.is(Blocks.LEAF_LITTER)||state.is(Blocks.SHORT_GRASS))require(state.canSurvive(level,pos),"Native woodland ground plant support "+state+" on "+chunk.getBlockState(pos.below()));
            }
            for(int oz=0;oz<16;oz+=4) for(int ox=0;ox<16;ox+=4) {
                int x=cx*16+ox,z=cz*16+oz,h=generator.drainage(random).sample(x,z).height();
                int cover=generator.woodlandCover(x,z,random);components.add(cover);
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
        for(int dz=-96;dz<=96&&!(components.contains(4)&&components.contains(5)&&components.contains(7));dz++)for(int dx=-96;dx<=96;dx++){
            int x=bed.getX()+dx,z=bed.getZ()+dz,cover=LeafLitter.woodlandBase(generator.woodlandCover(x,z,random));if(cover!=4&&cover!=5&&cover!=7||components.contains(cover))continue;
            int h=generator.drainage(random).sample(x,z).height();var actual=level.getChunk(x>>4,z>>4);var profile=((SparseColumn)actual).blockbranch$terrain().base();var plant=new BlockPos(x,h+1,z);
            require(actual.getBlockState(plant)==profile.state(x,h+1,z)&&actual.getBlockState(plant).canSurvive(level,plant),"Actual clustered understory support");components.add(cover);
        }
        verifyGroundDetails(level,generator,bed,reload);
        boundary(level,generator,bed,birchForest);
        if(birchForest)require(birchLogs>logs*.80&&birch>leaves*.60,"Native birch dominance in woody and leaf structure");
        require(leaves>500 && logs>30 && birch>30,"Mixed, nontrivial native canopy");
        require(components.contains(4) && components.contains(5) && components.contains(7),"Fern, fungi and leaf-litter understory");
        var drops=Block.getDrops((birchForest?Blocks.BIRCH_LOG:Blocks.OAK_LOG).defaultBlockState(),level,bed.above(),null,null,new ItemStack(Items.IRON_AXE));
        require(drops.size()==1 && drops.getFirst().is(birchForest?Items.BIRCH_LOG:Items.OAK_LOG),"Ordinary usable timber drops");
        var leaf=(birchForest?Blocks.BIRCH_LEAVES:Blocks.OAK_LEAVES).defaultBlockState().setValue(LeavesBlock.DISTANCE,2);
        drops=Block.getDrops(leaf,level,bed.above(10),null,null,new ItemStack(Items.SHEARS));
        require(drops.size()==1 && drops.getFirst().is(birchForest?Items.BIRCH_LEAVES:Items.OAK_LEAVES),"Sheared natural foliage item");
        var invalid=rootBase.encode(level.palettedContainerFactory());invalid.putInt("version",25);boolean rejected=false;
        try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
        require(rejected,"Pre-detail profile rejected");
        for(int[] bad:new int[][]{new int[CanopyLayer.MAX_BLOCKS+1],{0},{(rootBase.canopy().encode()[0]&~blockbranch.cubic.CanopyLayer.STATE_MASK)}}) {
            invalid=rootBase.encode(level.palettedContainerFactory());invalid.putIntArray("canopy",bad);rejected=false;
            try { TerrainProfile.decode(invalid,level.palettedContainerFactory()); } catch(IllegalArgumentException expected) { rejected=true; }
            require(rejected,"Malformed canopy position/code/size rejected");
        }
        var chunk=level.getChunkAt(bed);var terrain=((SparseColumn)chunk).blockbranch$terrain();
        level.setBlock(cut,Blocks.AIR.defaultBlockState(),2);chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);
        require(level.getBlockState(cut).isAir(),"Removed trunk survives edit eviction instead of regenerating");
        Blockbranch.LOGGER.info("FOREST_PASS {} birchForest={} bed={} logs={} birchLogs={} leaves={} birchLeaves={} floorComponents={}",reload ? "RELOAD" : "NEW",birchForest,bed,logs,birchLogs,leaves,birch,components);
    }
    private static void boundary(ServerLevel level,RealisticGenerator generator,BlockPos bed,boolean birchForest) {
        var random=level.getChunkSource().randomState();var source=generator.getBiomeSource().createUncachedResolver(random);
        for(int dz=-512;dz<=512;dz+=16) for(int dx=-512;dx<=512;dx+=16) {
            int x=Math.floorDiv(bed.getX()+dx,16)*16,z=Math.floorDiv(bed.getZ()+dz,16)*16,count=0;
            for(int qz=0;qz<4;qz++) for(int qx=0;qx<4;qx++) if(source.getNoiseBiome((x>>2)+qx,0,(z>>2)+qz).is(birchForest?net.minecraft.world.level.biome.Biomes.BIRCH_FOREST:net.minecraft.world.level.biome.Biomes.FOREST)) count++;
            if(count==0 || count==16) continue;
            for(int oz=0;oz<16;oz++) for(int ox=0;ox<20;ox++) {
                int wx=x+ox,wz=z+oz,h=generator.drainage(random).sample(wx,wz).height();var chunk=level.getChunk(wx>>4,wz>>4);
                int top=Math.max(h+2,((SparseColumn)chunk).blockbranch$terrain().base().canopy().topY(wx,wz));
                for(int y=h-3;y<=top;y++) require(chunk.getBlockState(new BlockPos(wx,y,wz))==generator.surfaceState(wx,y,wz,random),"Mixed woodland/native edge floor and canopy agreement");
                require(chunk.getNoiseBiome(wx>>2,0,wz>>2).equals(source.getNoiseBiome(wx>>2,0,wz>>2)),"Woodland biome edge agrees with source");
            }
            Blockbranch.LOGGER.info("FOREST_BOUNDARY_PASS x={} z={} woodlandQuartCells={}",x,z,count);return;
        }
        throw new AssertionError("No mixed woodland stand boundary");
    }
    private static void verifyGroundDetails(ServerLevel level,RealisticGenerator g,BlockPos bed,boolean reload){
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());BlockPos witness=null;int cells=0;
        for(int cz=(bed.getZ()>>4)-4;cz<=(bed.getZ()>>4)+4;cz++)for(int cx=(bed.getX()>>4)-4;cx<=(bed.getX()>>4)+4;cx++)for(int v:WoodlandDetails.temperateLogs(r.seed(),cx,cz,d,c)){
            int key=v>>>CanopyLayer.STATE_BITS;var pos=new BlockPos(cx*16+(key&15),(key>>>8)+TerrainShape.FLOOR,cz*16+((key>>>4)&15));var chunk=level.getChunkAt(pos);var base=((SparseColumn)chunk).blockbranch$terrain().base();var expected=base.state(pos.getX(),pos.getY(),pos.getZ());
            if(witness==null){witness=pos;if(reload)require(level.getBlockState(pos).isAir(),"Saved fallen-timber cut survives reopen");level.setBlock(pos,expected,2);}
            var actual=level.getBlockState(pos);require(actual==expected&&(actual.is(Blocks.OAK_LOG)||actual.is(Blocks.BIRCH_LOG))&&actual.getValue(RotatedPillarBlock.AXIS)!=Direction.Axis.Y,"Native horizontal usable timber materialized across chunks");
            var terrain=d.sample(pos.getX(),pos.getZ());require(pos.getY()==terrain.height()+1&&terrain.waterLevel()==DrainageNetwork.DRY&&d.channelAt(pos.getX(),pos.getZ()).edge()<0&&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP),"Every fallen timber segment has dry solid ground support");cells++;
        }
        require(witness!=null&&cells>=3,"Actual natural fallen timber witness");boolean flower=false;
        for(int dz=-96;dz<=96&&!flower;dz++)for(int dx=-96;dx<=96&&!flower;dx++){int x=bed.getX()+dx,z=bed.getZ()+dz,cover=g.woodlandCover(x,z,r);if(cover!=30&&cover!=31)continue;var pos=new BlockPos(x,d.sample(x,z).height()+1,z);var chunk=level.getChunkAt(pos);var base=((SparseColumn)chunk).blockbranch$terrain().base();var state=chunk.getBlockState(pos);require(state==base.state(x,pos.getY(),z)&&state.is(cover==30?Blocks.DANDELION:Blocks.OXEYE_DAISY)&&state.canSurvive(level,pos)&&level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK),"Natural sparse native gap bloom has valid grass support/profile");flower=true;}
        require(flower,"Natural gap flower witness");var terrain=((SparseColumn)level.getChunkAt(witness)).blockbranch$terrain();level.setBlock(witness,Blocks.AIR.defaultBlockState(),2);level.getChunkAt(witness).markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(witness).isAir(),"Fallen timber edit survives lazy eviction");
        Blockbranch.LOGGER.info("WOODLAND_GROUND_DETAILS_NATIVE_PASS {} bed={} timberCells={} flower=true",reload?"RELOAD":"NEW",bed,cells);
    }
    /** Native fixture camera at existing deadwood;never places decorative blocks for a screenshot. */
    public static double[] detailView(ServerLevel level,RealisticGenerator g,BlockPos bed){
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());BlockPos target=null;int targetCode=0;double best=Double.MAX_VALUE;
        for(int cz=(bed.getZ()>>4)-4;cz<=(bed.getZ()>>4)+4;cz++)for(int cx=(bed.getX()>>4)-4;cx<=(bed.getX()>>4)+4;cx++)for(int v:WoodlandDetails.temperateLogs(r.seed(),cx,cz,d,c)){
            int key=v>>>CanopyLayer.STATE_BITS;var p=new BlockPos(cx*16+(key&15),(key>>>8)+TerrainShape.FLOOR,cz*16+((key>>>4)&15));double distance=p.distSqr(bed);if(distance<best){best=distance;target=p;targetCode=v&CanopyLayer.STATE_MASK;}
        }
        if(target==null)throw new AssertionError("No natural supported deadwood camera witness");
        for(int radius:new int[]{8,10,12})for(int n=0;n<16;n++){
            double a=.8+n*Math.PI/8;if(Math.abs(targetCode%3==2?Math.cos(a):Math.sin(a))>.4)continue;int x=target.getX()+(int)Math.round(Math.cos(a)*radius),z=target.getZ()+(int)Math.round(Math.sin(a)*radius),h=d.sample(x,z).height();if(d.sample(x,z).waterLevel()!=DrainageNetwork.DRY)continue;boolean open=true;
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(int y=2;y<=3;y++)if(!g.surfaceState(x+dx,h+y,z+dz,r).isAir())open=false;
            double tx=target.getX()+.5,tz=target.getZ()+.5,ty=target.getY()+.5,eye=h+2.62;
            for(int k=1;k<20;k++){double t=k/22.;if(!g.surfaceState((int)Math.floor(x+.5+(tx-x-.5)*t),(int)Math.floor(eye+(ty-eye)*t),(int)Math.floor(z+.5+(tz-z-.5)*t),r).isAir())open=false;}
            if(open)return new double[]{x+.5,h+1,z+.5,Math.toDegrees(Math.atan2(-(tx-x-.5),tz-z-.5)),-Math.toDegrees(Math.atan2(ty-eye,Math.hypot(tx-x-.5,tz-z-.5)))};
        }
        throw new AssertionError("No clear natural deadwood viewpoint");
    }
    private static void require(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private ForestSmoke() {}
}
