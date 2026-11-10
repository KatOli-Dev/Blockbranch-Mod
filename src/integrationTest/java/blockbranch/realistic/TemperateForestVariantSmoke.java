package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Native identity, supported flora/canopy, saved edits and normal-visibility camera witnesses. */
public final class TemperateForestVariantSmoke {
    private static final Map<DrainageNetwork,BlockPos[]> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
    public static BlockPos fixture(RealisticGenerator g,RandomState r,int kind){
        var d=g.drainage(r);synchronized(FIXTURES){
            var points=FIXTURES.computeIfAbsent(d,unused->new BlockPos[2]);if(points[kind-1]!=null)return points[kind-1];
            var c=new ClimateField(r.seed());String requested=System.getProperty("blockbranch.flowerForestInteriorPoint");
            var p=blockbranch.realistic.TemperateForestVariantsFixtures.fixture(d,r.seed(),kind);
            if(requested!=null){
                require(kind==TemperateForestVariants.FLOWER,"Interior capture option is Flower Forest only");
                var xy=requested.split(",",-1);require(xy.length==2,"Interior point requires x,z");
                p=new TemperateForestVariants.Point(Integer.parseInt(xy[0]),Integer.parseInt(xy[1]));
                require(TemperateForestVariants.kind(r.seed(),p.x(),p.z(),d,c)==kind,"Requested natural interior anchor");
            }
            for(var root:WoodlandTrees.roots(r.seed(),p.x()-96,p.z()-96,p.x()+96,p.z()+96,d,c)){
                if(kind==2?root.family()!=9||!root.birch()||root.height()<24:root.family()!=1||root.birch()||root.height()<17)continue;
                if(TemperateForestVariants.kind(r.seed(),root.x(),root.z(),d,c)!=kind)continue;
                var bed=new BlockPos(root.x(),root.y()-1,root.z());
                if(requested!=null&&!flowerInterior(r.seed(),root.x(),root.z(),96,d,c))continue;
                if(clearViews(g,r,bed,kind)){
                    if(requested!=null)Blockbranch.LOGGER.info("FLOWER_FOREST_INTERIOR_WITNESS bed={} fullySelectedSquareRadius=96 sampleStep=4 ground={} elevated={}",bed,blockbranch.realistic.TemperateForestVariantsFixtures.view(bed.getX(),bed.getY(),bed.getZ(),false,kind),blockbranch.realistic.TemperateForestVariantsFixtures.view(bed.getX(),bed.getY(),bed.getZ(),true,kind));
                    points[kind-1]=bed;return bed;
                }
            }
            throw new AssertionError("No clear natural temperate variant cameras kind="+kind+" anchor="+p);
        }
    }
    private static boolean flowerInterior(long seed,int x,int z,int radius,DrainageNetwork d,ClimateField c){
        for(int dz=-radius;dz<=radius;dz+=4)for(int dx=-radius;dx<=radius;dx+=4)
            if(TemperateForestVariants.kind(seed,x+dx,z+dz,d,c)!=TemperateForestVariants.FLOWER||TemperateForestVariants.surfaceKind(seed,x+dx,z+dz,d,c)!=TemperateForestVariants.FLOWER)return false;
        return true;
    }
    private static boolean clearViews(RealisticGenerator g,RandomState r,BlockPos bed,int kind){
        for(boolean elevated:new boolean[]{false,true}){
            var v=blockbranch.realistic.TemperateForestVariantsFixtures.view(bed.getX(),bed.getY(),bed.getZ(),elevated,kind);
            if(!elevated&&Math.abs(g.drainage(r).sample((int)Math.floor(v.x()),(int)Math.floor(v.z())).height()-bed.getY())>1)return false;
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<=3;dy++)if(!g.surfaceState((int)Math.floor(v.x())+dx,(int)Math.floor(v.y())+dy,(int)Math.floor(v.z())+dz,r).isAir())return false;
        }
        return true;
    }
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var source=g.getBiomeSource().createUncachedResolver(r);
        for(int kind:new int[]{1,2}){
            var bed=fixture(g,r,kind);var expected=kind==2?Biomes.OLD_GROWTH_BIRCH_FOREST:Biomes.FLOWER_FOREST;var stem=kind==2?Blocks.BIRCH_LOG:Blocks.OAK_LOG;var cut=bed.above(2);
            require(level.getBiome(bed).is(expected),"Stored native temperate identity");
            var home=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();require(home.state(bed.getX(),bed.getY()+1,bed.getZ()).is(stem),"Natural supported selected stem");
            if(reload)require(level.getBlockState(cut).isAir(),"Saved cut survives reopening");level.setBlock(cut,home.state(cut.getX(),cut.getY(),cut.getZ()),2);
            int logs=0,leaves=0,plants=0,birchLeaves=0;var flowers=new HashSet<Block>();
            for(int cz=(bed.getZ()>>4)-2;cz<=(bed.getZ()>>4)+2;cz++)for(int cx=(bed.getX()>>4)-2;cx<=(bed.getX()>>4)+2;cx++){
                var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var tag=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
                require(Arrays.equals(base.canopy().encode(),copy.canopy().encode()),"Packed canopy round trip");
                for(int packed:base.canopy().encode()){
                    int key=packed>>>CanopyLayer.STATE_BITS,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;var pos=new BlockPos(x,y,z);var state=base.state(x,y,z);
                    require(chunk.getBlockState(pos)==state&&copy.state(x,y,z)==state&&sections.computeIfAbsent(y>>4,copy::generate).getBlockState(x&15,y&15,z&15)==state,"Native/decoded/client canopy materialization");
                    require(d.sample(x,z).waterLevel()==DrainageNetwork.DRY&&y>d.sample(x,z).height(),"Canopy remains above dry native ground");
                    if(state.is(BlockTags.LOGS))logs++;
                    if(state.is(BlockTags.LEAVES)){
                        leaves++;if(state.is(Blocks.BIRCH_LEAVES))birchLeaves++;int distance=state.getValue(LeavesBlock.DISTANCE);boolean support=false;
                        for(var direction:Direction.values()){var near=level.getBlockState(pos.relative(direction));if(near.is(BlockTags.LOGS)&&distance==1||near.is(BlockTags.LEAVES)&&near.getValue(LeavesBlock.DISTANCE)<distance)support=true;}
                        require(support&&!state.getValue(LeavesBlock.PERSISTENT),"Real cross-chunk leaf support");
                    }
                }
                for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                    int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height();var pos=new BlockPos(wx,h+1,wz);var state=chunk.getBlockState(pos);
                    if(state.is(BlockTags.SMALL_FLOWERS)){flowers.add(state.getBlock());require(state.canSurvive(level,pos),"Native flower support");}
                    if(state.is(BlockTags.SMALL_FLOWERS)||state.is(Blocks.FERN)||state.is(Blocks.LEAF_LITTER)||state.is(Blocks.SHORT_GRASS)){plants++;require(state.canSurvive(level,pos),"Native understory support");}
                    if(x%4!=0||z%4!=0)continue;
                    for(int y:new int[]{-3000,0,2000})require(chunk.getNoiseBiome(wx>>2,y,wz>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2))&&copy.biomes().get(x>>2,y&3,z>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2)),"Stored/source/decoded all-altitude identity");
                    for(int y=h-3;y<=Math.max(h+2,base.canopy().topY(wx,wz));y++)require(base.state(wx,y,wz)==g.surfaceState(wx,y,wz,r),"Analytic/profile floor and canopy agreement");
                    for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,r),"Native canopy/plant heightmap "+type);
                }
                var array=tag.getIntArray("canopy").orElseThrow();if(array.length>0){int old=base.canopy().encode()[0];array[0]=0;require(base.canopy().encode()[0]==old,"Immutable saved snapshot");}
            }
            require(logs>100&&leaves>1000&&plants>10,"Substantial native stand and understory");
            if(kind==1)require(flowers.size()>=2,"Multiple actual native flower species");else require(birchLeaves>leaves*.7,"Native old-birch canopy dominance");
            level.setBlock(cut,Blocks.AIR.defaultBlockState(),2);var chunk=level.getChunkAt(cut);var terrain=((SparseColumn)chunk).blockbranch$terrain();chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(cut).isAir(),"Tree cut persists through lazy eviction");
            boundary(level,g,bed,kind);
            Blockbranch.LOGGER.info("TEMPERATE_VARIANT_NATIVE_PASS mode={} kind={} bed={} logs={} leaves={} birchLeaves={} plants={} flowers={}",reload?"RELOAD":"NEW",kind,bed,logs,leaves,birchLeaves,plants,flowers);
        }
    }
    private static void boundary(ServerLevel level,RealisticGenerator g,BlockPos bed,int kind){
        var r=level.getChunkSource().randomState();var source=g.getBiomeSource().createUncachedResolver(r);var target=kind==2?Biomes.OLD_GROWTH_BIRCH_FOREST:Biomes.FLOWER_FOREST;
        for(int dz=-512;dz<=512;dz+=16)for(int dx=-512;dx<=512;dx+=16){
            int x=Math.floorDiv(bed.getX()+dx,16)*16,z=Math.floorDiv(bed.getZ()+dz,16)*16,count=0;
            for(int qz=0;qz<4;qz++)for(int qx=0;qx<4;qx++)if(source.getNoiseBiome((x>>2)+qx,0,(z>>2)+qz).is(target))count++;
            if(count==0||count==16)continue;
            for(int oz=0;oz<16;oz++)for(int ox=0;ox<20;ox++){
                int wx=x+ox,wz=z+oz,h=g.drainage(r).sample(wx,wz).height();var chunk=level.getChunk(wx>>4,wz>>4);var base=((SparseColumn)chunk).blockbranch$terrain().base();
                for(int y=h-3;y<=Math.max(h+2,base.canopy().topY(wx,wz));y++)require(chunk.getBlockState(new BlockPos(wx,y,wz))==g.surfaceState(wx,y,wz,r),"Mixed native variant boundary material agreement");
                require(chunk.getNoiseBiome(wx>>2,0,wz>>2).equals(source.getNoiseBiome(wx>>2,0,wz>>2)),"Mixed native variant boundary identity");
            }
            Blockbranch.LOGGER.info("TEMPERATE_VARIANT_BOUNDARY_PASS kind={} x={} z={} selectedQuartCells={}",kind,x,z,count);return;
        }
        throw new AssertionError("No mixed temperate variant boundary kind="+kind);
    }
    public static void probe(ServerPlayer player,int stage){
        var level=player.level();var g=(RealisticGenerator)level.getChunkSource().getGenerator();int kind=Boolean.getBoolean("blockbranch.oldBirchClient")?2:1;var bed=fixture(g,level.getChunkSource().randomState(),kind);var test=bed.above(40).east();
        if(stage<=92||stage==95){
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set 6000");
            var v=stage==95?intermediateView(bed):stage==90?new TemperateForestVariants.View(bed.getX()+.5,bed.getY()+40,bed.getZ()+.5,0,30):blockbranch.realistic.TemperateForestVariantsFixtures.view(bed.getX(),bed.getY(),bed.getZ(),stage==92,kind);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+v.x()+" "+v.y()+" "+v.z()+" "+v.yaw()+" "+v.pitch());
            Blockbranch.LOGGER.info("TEMPERATE_VARIANT_CAPTURE_CAMERA kind={} stage={} bed={} view={}",kind,stage,bed,v);
        }else if(stage==93)level.setBlock(test,Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,7).setValue(LeavesBlock.PERSISTENT,false),2);
        else{var base=((SparseColumn)level.getChunkAt(test)).blockbranch$terrain().base();level.setBlock(test,base.state(test.getX(),test.getY(),test.getZ()),2);}
    }
    public static TemperateForestVariants.View intermediateView(BlockPos bed){
        var ground=blockbranch.realistic.TemperateForestVariantsFixtures.view(bed.getX(),bed.getY(),bed.getZ(),false,TemperateForestVariants.FLOWER);
        double y=bed.getY()+18,target=bed.getY()+2;
        return new TemperateForestVariants.View(ground.x(),y,ground.z(),ground.yaw(),(float)-Math.toDegrees(Math.atan2(target-(y+1.62),Math.hypot(bed.getX()+.5-ground.x(),bed.getZ()+.5-ground.z()))));
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private TemperateForestVariantSmoke(){}
}
