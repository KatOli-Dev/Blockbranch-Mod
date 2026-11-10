package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Native tropical identities, supported culms/leaves, snapshots and honest clear-lens views. */
public final class TropicalForestSmoke {
    private static final Map<DrainageNetwork,BlockPos[]> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
    public static BlockPos fixture(RealisticGenerator g,RandomState r,int kind){
        var d=g.drainage(r);synchronized(FIXTURES){
            var points=FIXTURES.computeIfAbsent(d,unused->new BlockPos[3]);if(points[kind-1]!=null)return points[kind-1];
            var c=new ClimateField(r.seed());var p=kind==3?new TropicalForestVariants.Point(blockbranch.realistic.JungleFixtures.fixture(d,r.seed()).x(),blockbranch.realistic.JungleFixtures.fixture(d,r.seed()).z()):blockbranch.realistic.TropicalForestVariantsFixtures.fixture(d,r.seed(),kind);
            if(kind!=2){
                var roots=kind==3?JungleTrees.roots(r.seed(),p.x()-128,p.z()-128,p.x()+128,p.z()+128,d,c):TropicalVariantTrees.roots(r.seed(),p.x()-128,p.z()-128,p.x()+128,p.z()+128,d,c);
                for(var root:roots){
                    if(kind==1&&root.family()!=12||root.height()<(kind==3?25:18))continue;var bed=new BlockPos(root.x(),root.y()-1,root.z());
                    if(clearViews(g,r,bed,kind)){points[kind-1]=bed;return bed;}
                }
            }else{
                for(int dz=-96;dz<=96;dz++)for(int dx=-96;dx<=96;dx++){
                    var bed=new BlockPos(p.x()+dx,d.sample(p.x()+dx,p.z()+dz).height(),p.z()+dz);
                    if(!g.surfaceState(bed.getX(),bed.getY()+1,bed.getZ(),r).is(Blocks.BAMBOO))continue;
                    if(clearViews(g,r,bed,kind)){points[kind-1]=bed;return bed;}
                }
            }
            throw new AssertionError("No clear natural tropical view kind="+kind+" anchor="+p);
        }
    }
    public static TropicalForestVariants.View view(RealisticGenerator g,RandomState r,BlockPos bed,boolean elevated,int kind){return blockbranch.realistic.TropicalForestVariantsFixtures.view(bed.getX(),bed.getY(),bed.getZ(),elevated,kind,(x,z)->g.drainage(r).sample(x,z).height());}
    private static boolean clearViews(RealisticGenerator g,RandomState r,BlockPos bed,int kind){
        var d=g.drainage(r);var c=new ClimateField(r.seed());
        for(int dz=-32;dz<=32;dz+=16)for(int dx=-32;dx<=32;dx+=16)if(!contains(r.seed(),bed.getX()+dx,bed.getZ()+dz,d,c,kind))return false;
        for(boolean elevated:new boolean[]{false,true}){
            var v=view(g,r,bed,elevated,kind);if(!contains(r.seed(),(int)Math.floor(v.x()),(int)Math.floor(v.z()),d,c,kind))return false;
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<=3;dy++){var state=g.surfaceState((int)Math.floor(v.x())+dx,(int)Math.floor(v.y())+dy,(int)Math.floor(v.z())+dz,r);if(!state.isAir()&&!state.is(Blocks.LARGE_FERN))return false;}
        }
        return true;
    }
    private static boolean contains(long seed,int x,int z,DrainageNetwork d,ClimateField c,int kind){return kind==3?Jungle.contains(seed,x,z,d,c):TropicalForestVariants.kind(seed,x,z,d,c)==kind;}
    public static int culmHeight(TerrainProfile base,BlockPos bed){int height=0;while(height<16&&base.state(bed.getX(),bed.getY()+height+1,bed.getZ()).is(Blocks.BAMBOO))height++;return height;}
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var source=g.getBiomeSource().createUncachedResolver(r);
        for(int kind:new int[]{1,2}){
            var bed=fixture(g,r,kind);var biome=kind==2?Biomes.BAMBOO_JUNGLE:Biomes.SPARSE_JUNGLE;
            require(level.getBiome(bed).is(biome),"Actual tropical vanilla identity");
            var home=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();int height=kind==2?culmHeight(home,bed):2;var cut=bed.above(height);
            require(home.state(bed.getX(),bed.getY()+1,bed.getZ()).is(kind==2?Blocks.BAMBOO:Blocks.JUNGLE_LOG),"Natural supported anchor");
            if(reload)require(level.getBlockState(cut).isAir(),"Saved native cut survives reopening");level.setBlock(cut,home.state(cut.getX(),cut.getY(),cut.getZ()),2);
            int logs=0,leaves=0,culms=0,plants=0;var stages=new HashSet<Integer>();
            for(int cz=(bed.getZ()>>4)-2;cz<=(bed.getZ()>>4)+2;cz++)for(int cx=(bed.getX()>>4)-2;cx<=(bed.getX()>>4)+2;cx++){
                var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var tag=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
                require(Arrays.equals(base.canopy().encode(),copy.canopy().encode()),"Packed snapshot round trip");
                for(int packed:base.canopy().encode()){
                    int key=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;var pos=new BlockPos(x,y,z);var state=base.state(x,y,z);
                    require(chunk.getBlockState(pos)==state&&copy.state(x,y,z)==state&&sections.computeIfAbsent(y>>4,copy::generate).getBlockState(x&15,y&15,z&15)==state,"Native/saved/client canopy materialization");
                    require(d.sample(x,z).waterLevel()==DrainageNetwork.DRY&&y>d.sample(x,z).height(),"Supported dry terrain; water preserved");
                    if(state.is(BlockTags.LOGS))logs++;
                    if(state.is(Blocks.BAMBOO)){culms++;require(state.canSurvive(level,pos),"Native bamboo has actual plantable support");stages.add(state.getValue(BambooStalkBlock.STAGE));}
                    if(state.is(BlockTags.LEAVES)){
                        leaves++;int distance=state.getValue(LeavesBlock.DISTANCE);boolean support=false;
                        for(var direction:Direction.values()){var near=level.getBlockState(pos.relative(direction));if(near.is(BlockTags.LOGS)&&distance==1||near.is(BlockTags.LEAVES)&&near.getValue(LeavesBlock.DISTANCE)<distance)support=true;}
                        require(support&&!state.getValue(LeavesBlock.PERSISTENT),"Native cross-chunk leaf support; bamboo is not a tree-log substitute");
                    }
                }
                for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                    int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height();var pos=new BlockPos(wx,h+1,wz);var state=level.getBlockState(pos);
                    if(state.is(Blocks.FERN)||state.is(Blocks.LEAF_LITTER)||state.is(Blocks.SHORT_GRASS)){plants++;require(state.canSurvive(level,pos),"Actual floor plant support");}
                    if(x%4!=0||z%4!=0)continue;
                    for(int y:new int[]{-3000,0,2000})require(chunk.getNoiseBiome(wx>>2,y,wz>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2))&&copy.biomes().get(x>>2,y&3,z>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2)),"Stored/source/decoded all-altitude identity");
                    for(int y=h-3;y<=Math.max(h+2,base.canopy().topY(wx,wz));y++)require(base.state(wx,y,wz)==g.surfaceState(wx,y,wz,r),"Analytic/profile physical materials");
                    for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,r),"Native heightmap "+type);
                }
            }
            require(kind==1?logs>40&&leaves>300:culms>200&&stages.size()==2,"Characteristic native stand");require(plants>10,"Sparse irregular understory remains");
            if(kind==2)BambooGroundWitness.verify(level,g,bed);
            if(kind==2)require(Block.getDrops(home.state(cut.getX(),cut.getY(),cut.getZ()),level,cut,null,null,new ItemStack(Items.IRON_AXE)).stream().anyMatch(s->s.is(Items.BAMBOO)),"Native usable bamboo harvest");
            level.setBlock(cut,Blocks.AIR.defaultBlockState(),2);var chunk=level.getChunkAt(cut);var terrain=((SparseColumn)chunk).blockbranch$terrain();chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(cut).isAir(),"Persistent cut survives sparse eviction");
            Blockbranch.LOGGER.info("TROPICAL_VARIANT_NATIVE_PASS mode={} kind={} bed={} logs={} leaves={} bamboo={} plants={}",reload?"RELOAD":"NEW",kind,bed,logs,leaves,culms,plants);
        }
    }
    public static void probe(ServerPlayer player,int stage){
        var level=player.level();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();int kind=Boolean.getBoolean("blockbranch.bambooJungleClient")?2:Boolean.getBoolean("blockbranch.existingJungleClient")?3:1;var bed=fixture(g,r,kind);
        var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();int height=kind==2?culmHeight(base,bed):40;var test=kind==2?bed.above(height):bed.above(40).east();
        if(stage<=92){
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set 6000");
            var v=stage==90?new TropicalForestVariants.View(bed.getX()+.5,bed.getY()+40,bed.getZ()+.5,0,30):view(g,r,bed,stage==92,kind);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+v.x()+" "+v.y()+" "+v.z()+" "+v.yaw()+" "+v.pitch());
            Blockbranch.LOGGER.info("TROPICAL_CAPTURE_CAMERA kind={} stage={} bed={} view={}",kind,stage,bed,v);
        }else if(stage==93){if(kind==2)level.destroyBlock(test,true);else level.setBlock(test,Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,7).setValue(LeavesBlock.PERSISTENT,false),2);}
        else if(kind==2){for(int dy=1;dy<=height;dy++){var p=bed.above(dy);level.setBlock(p,base.state(p.getX(),p.getY(),p.getZ()),2);}}
        else level.setBlock(test,base.state(test.getX(),test.getY(),test.getZ()),2);
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private TropicalForestSmoke(){}
}
