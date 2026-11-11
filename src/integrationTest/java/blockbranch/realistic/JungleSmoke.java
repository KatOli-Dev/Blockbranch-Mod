package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Native jungle palette, saved snapshots and persistent edits in an isolated fixture only. */
public final class JungleSmoke {
    public static BlockPos fixture(RealisticGenerator g,RandomState r){var p=blockbranch.realistic.JungleFixtures.fixture(g.drainage(r),r.seed());return new BlockPos(p.x(),g.drainage(r).sample(p.x(),p.z()).height(),p.z());}
    public static BlockPos clientFixture(RealisticGenerator g,RandomState r){if(Boolean.getBoolean("blockbranch.jungleComparison")&&r.seed()==424242){int x=-1680015,z=-480035;if(Boolean.getBoolean("blockbranch.jungleSecondSite")){var d=g.drainage(r);var c=new ClimateField(r.seed());for(int[] delta:new int[][]{{512,512},{-512,512},{512,-512},{-512,-512},{1024,0}})for(var root:JungleTrees.roots(r.seed(),x+delta[0]-64,z+delta[1]-64,x+delta[0]+64,z+delta[1]+64,d,c))if(root.height()>=23)return new BlockPos(root.x(),root.y()-1,root.z());throw new AssertionError("No second Jungle site");}return new BlockPos(x,g.drainage(r).sample(x,z).height(),z);}return fixture(g,r);}
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        var random=level.getChunkSource().randomState();var bed=fixture(g,random);var source=g.getBiomeSource().createUncachedResolver(random);
        require(level.getBiome(bed).is(Biomes.JUNGLE),"Actual vanilla Jungle registry identity");
        require(level.getBlockState(bed.above()).is(Blocks.JUNGLE_LOG),"Natural usable jungle trunk");
        var cut=bed.above(2);if(reload)require(level.getBlockState(cut).isAir(),"Saved Jungle trunk cut survives reopen");
        var root=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();level.setBlock(cut,root.state(cut.getX(),cut.getY(),cut.getZ()),2);
        int logs=0,leaves=0,largeFerns=0;var covers=new HashSet<Integer>();
        for(int cz=(bed.getZ()>>4)-1;cz<=(bed.getZ()>>4)+1;cz++)for(int cx=(bed.getX()>>4)-1;cx<=(bed.getX()>>4)+1;cx++){
            var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var tag=base.encode(level.palettedContainerFactory());var decoded=TerrainProfile.decode(tag,level.palettedContainerFactory());
            require(Arrays.equals(base.canopy().encode(),decoded.canopy().encode()),"Seven-bit canopy snapshot round trip");
            for(int packed:base.canopy().encode()){
                int key=packed>>>CanopyLayer.STATE_BITS,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;var pos=new BlockPos(x,y,z);var state=base.state(x,y,z);
                require(chunk.getBlockState(pos)==state&&decoded.state(x,y,z)==state&&decoded.generate(y>>4).getBlockState(x&15,y&15,z&15)==state,"Native/saved/materialized canopy equality");
                if(state.is(Blocks.JUNGLE_LEAVES)){
                    leaves++;int distance=state.getValue(LeavesBlock.DISTANCE);require(distance<=6&&!state.getValue(LeavesBlock.PERSISTENT)&&!state.isRandomlyTicking(),"Natural supported leaves");boolean support=false;
                    for(var dir:Direction.values()){var near=level.getBlockState(pos.relative(dir));if(near.is(Blocks.JUNGLE_LOG)&&distance==1||near.is(Blocks.JUNGLE_LEAVES)&&near.getValue(LeavesBlock.DISTANCE)<distance)support=true;}
                    require(support,"Native jungle leaf support across chunk seams");
                }else{require(state.is(Blocks.JUNGLE_LOG),"Acacia material identity");logs++;}
            }
            for(int z=cz*16;z<cz*16+16;z+=4)for(int x=cx*16;x<cx*16+16;x+=4){
                int h=g.drainage(random).sample(x,z).height();covers.add(LeafLitter.woodlandBase(g.woodlandCover(x,z,random)));
                for(int y=h-3;y<=Math.max(h+2,base.canopy().topY(x,z));y++)require(base.state(x,y,z)==g.surfaceState(x,y,z,random),"Native Jungle floor and canopy query consistency");
                for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x&15,z&15)+1==base.top(type.isOpaque(),x,z,ColumnLimits.MAX_Y-1),"Native Jungle heightmap");
                for(var bounds:List.of(net.minecraft.world.level.LevelHeightAccessor.create(h+7,3),net.minecraft.world.level.LevelHeightAccessor.create(h+24,3),net.minecraft.world.level.LevelHeightAccessor.create(h+36,3))){var column=g.getBaseColumn(x,z,bounds,random);for(var type:Heightmap.Types.values()){int expected=bounds.getMinY();for(int y=bounds.getMaxY();y>=bounds.getMinY();y--)if(type.isOpaque().test(column.getBlock(y))){expected=y+1;break;}require(g.getBaseHeight(x,z,type,bounds,random)==expected,"Clipped tropical canopy tier heightmap "+type);}}

                for(int y:new int[]{-3000,0,h>>2,2000})require(chunk.getNoiseBiome(x>>2,y,z>>2).equals(source.getNoiseBiome(x>>2,y,z>>2)),"Stored and source biome identity at all altitudes");
            }
            for(int z=cz*16;z<cz*16+16;z++)for(int x=cx*16;x<cx*16+16;x++){
                int h=g.drainage(random).sample(x,z).height();var pos=new BlockPos(x,h+1,z);var plant=level.getBlockState(pos);
                if(!plant.is(Blocks.LARGE_FERN))continue;largeFerns++;
                require(plant.getValue(DoublePlantBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER&&plant.canSurvive(level,pos),"Native large fern lower half is supported");
                var upper=level.getBlockState(pos.above());require(upper.is(Blocks.LARGE_FERN)&&upper.getValue(DoublePlantBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER&&upper.canSurvive(level,pos.above()),"Native large fern upper half and headroom");
                require(level.getBlockState(pos.below()).is(Blocks.ROOTED_DIRT),"Mature fern keeps existing rooted Jungle soil");
                require(decoded.state(x,h+2,z)==upper&&g.surfaceState(x,h+2,z,random)==upper,"Large fern upper state survives snapshot and analytic query");
            }
            var original=tag.getByteArray("woodland").orElseThrow().clone();tag.getByteArray("woodland").orElseThrow()[0]=127;boolean rejected=false;
            try{TerrainProfile.decode(tag,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}
            require(rejected&&Arrays.equals(original,base.encode(level.palettedContainerFactory()).getByteArray("woodland").orElseThrow()),"Malformed snapshots rejected without profile mutation");
        }
        require(logs>5&&leaves>50,"Nontrivial native jungle canopy");
        boolean[] witness=new boolean[13];int remaining=5;
        for(int dz=-128;dz<=128&&remaining>0;dz++)for(int dx=-128;dx<=128&&remaining>0;dx++){
            int x=bed.getX()+dx,z=bed.getZ()+dz,code=LeafLitter.woodlandBase(g.woodlandCover(x,z,random));if(code<8||code>12||witness[code])continue;int h=g.drainage(random).sample(x,z).height();var base=((SparseColumn)level.getChunk(x>>4,z>>4)).blockbranch$terrain().base();if(base.canopy().state(x,h+1,z)!=null)continue;
            for(int y=h-3;y<=h+2;y++)require(level.getBlockState(new BlockPos(x,y,z))==g.surfaceState(x,y,z,random),"Every Jungle ground material reaches native terrain");
            if(code==9||code==10||code==12){var plant=new BlockPos(x,h+1,z);require(level.getBlockState(plant).canSurvive(level,plant),"Grass has supported dry soil");}
            witness[code]=true;remaining--;
        }
        require(remaining==0,"All five Jungle ground choices verified");
        require(largeFerns>0,"Natural mature fern colonies coexist with the five existing floor choices");
        Blockbranch.LOGGER.info("JUNGLE_LARGE_FERN_NATIVE_SUPPORT_SOIL_SNAPSHOT_PASS count={}",largeFerns);
        var drops=Block.getDrops(Blocks.JUNGLE_LOG.defaultBlockState(),level,bed.above(),null,null,new ItemStack(Items.IRON_AXE));require(drops.size()==1&&drops.getFirst().is(Items.JUNGLE_LOG),"Vanilla jungle timber loot");
        drops=Block.getDrops(Blocks.JUNGLE_LEAVES.defaultBlockState(),level,bed.above(),null,null,new ItemStack(Items.SHEARS));require(drops.size()==1&&drops.getFirst().is(Items.JUNGLE_LEAVES),"Vanilla jungle shearing loot");
        var old=root.encode(level.palettedContainerFactory());old.putInt("version",18);boolean rejected=false;try{TerrainProfile.decode(old,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Pre-Jungle floor/canopy profile is explicitly rejected");
        var saved=bed.below(8);if(reload)require(level.getBlockState(saved).is(Blocks.JUNGLE_LOG),"Saved Jungle subsurface edit survives reopen");level.setBlock(saved,Blocks.JUNGLE_LOG.defaultBlockState(),2);level.setBlock(cut,Blocks.AIR.defaultBlockState(),2);
        var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();level.getChunkAt(bed).markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(cut).isAir()&&level.getBlockState(saved).is(Blocks.JUNGLE_LOG),"Jungle edits survive section eviction");
        Blockbranch.LOGGER.info("JUNGLE_NATIVE_PASS {} bed={} logs={} leaves={} covers={}",reload?"RELOAD":"NEW",bed,logs,leaves,covers);
    }
    private static void require(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    private JungleSmoke(){}
}
