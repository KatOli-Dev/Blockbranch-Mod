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
public final class DarkCherrySmoke {
    private static final Map<DrainageNetwork,BlockPos[]> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
    public static BlockPos fixture(RealisticGenerator g,RandomState r,int kind){
        var d=g.drainage(r);synchronized(FIXTURES){
            var points=FIXTURES.computeIfAbsent(d,unused->new BlockPos[2]);if(points[kind-1]!=null)return points[kind-1];
            var c=new ClimateField(r.seed());var p=blockbranch.realistic.DarkCherryWoodlandFixtures.fixture(d,r.seed(),kind);
            for(var root:WoodlandTrees.roots(r.seed(),p.x()-96,p.z()-96,p.x()+96,p.z()+96,d,c)){
                if(kind==2?root.family()!=11||root.height()<14:root.family()!=10||root.height()<21)continue;
                if(DarkCherryWoodland.kind(r.seed(),root.x(),root.z(),d,c)!=kind)continue;
                var bed=new BlockPos(root.x(),root.y()-1,root.z());
                boolean interior=true;for(int dz=-32;dz<=32;dz+=16)for(int dx=-32;dx<=32;dx+=16)if(DarkCherryWoodland.kind(r.seed(),root.x()+dx,root.z()+dz,d,c)!=kind)interior=false;if(!interior)continue;
                if(clearViews(g,r,bed,kind)){
                    points[kind-1]=bed;return bed;
                }
            }
            throw new AssertionError("No clear natural dark/cherry cameras kind="+kind+" anchor="+p);
        }
    }
    private static boolean clearViews(RealisticGenerator g,RandomState r,BlockPos bed,int kind){
        for(boolean elevated:new boolean[]{false,true}){
            var v=view(g,r,bed,elevated,kind);
            if(DarkCherryWoodland.kind(r.seed(),(int)Math.floor(v.x()),(int)Math.floor(v.z()),g.drainage(r),new ClimateField(r.seed()))!=kind)return false;
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<=3;dy++)if(!g.surfaceState((int)Math.floor(v.x())+dx,(int)Math.floor(v.y())+dy,(int)Math.floor(v.z())+dz,r).isAir())return false;
        }
        return true;
    }
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var source=g.getBiomeSource().createUncachedResolver(r);
        verifyBranchStates(level);
        for(int kind:new int[]{1,2}){
            var bed=fixture(g,r,kind);var expected=kind==2?Biomes.CHERRY_GROVE:Biomes.DARK_FOREST;var stem=kind==2?Blocks.CHERRY_LOG:Blocks.DARK_OAK_LOG;var cut=bed.above(2);
            require(level.getBiome(bed).is(expected),"Stored native temperate identity");
            var home=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();require(home.state(bed.getX(),bed.getY()+1,bed.getZ()).is(stem),"Natural supported selected stem");
            if(reload)require(level.getBlockState(cut).isAir(),"Saved cut survives reopening");level.setBlock(cut,home.state(cut.getX(),cut.getY(),cut.getZ()),2);
            int logs=0,leaves=0,plants=0,speciesLeaves=0,petals=0;var petalStates=new HashSet<String>();var flowers=new HashSet<Block>();
            for(int cz=(bed.getZ()>>4)-2;cz<=(bed.getZ()>>4)+2;cz++)for(int cx=(bed.getX()>>4)-2;cx<=(bed.getX()>>4)+2;cx++){
                var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var tag=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
                require(Arrays.equals(base.canopy().encode(),copy.canopy().encode()),"Packed canopy round trip");
                for(int packed:base.canopy().encode()){
                    int key=packed>>>CanopyLayer.STATE_BITS,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;var pos=new BlockPos(x,y,z);var state=base.state(x,y,z);
                    require(chunk.getBlockState(pos)==state&&copy.state(x,y,z)==state&&sections.computeIfAbsent(y>>4,copy::generate).getBlockState(x&15,y&15,z&15)==state,"Native/decoded/client canopy materialization");
                    require(d.sample(x,z).waterLevel()==DrainageNetwork.DRY&&y>d.sample(x,z).height(),"Canopy remains above dry native ground");
                    if(state.is(BlockTags.LOGS))logs++;
                    if(state.is(BlockTags.LEAVES)){
                        leaves++;if(state.is(kind==2?Blocks.CHERRY_LEAVES:Blocks.DARK_OAK_LEAVES))speciesLeaves++;int distance=state.getValue(LeavesBlock.DISTANCE);boolean support=false;
                        for(var direction:Direction.values()){var near=level.getBlockState(pos.relative(direction));if(near.is(BlockTags.LOGS)&&distance==1||near.is(BlockTags.LEAVES)&&near.getValue(LeavesBlock.DISTANCE)<distance)support=true;}
                        require(support&&!state.getValue(LeavesBlock.PERSISTENT),"Real cross-chunk leaf support");
                    }
                }
                for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                    int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height();var pos=new BlockPos(wx,h+1,wz);var state=chunk.getBlockState(pos);
                    if(state.is(Blocks.PINK_PETALS)){petals++;petalStates.add(state.toString());}
                    if(state.is(BlockTags.SMALL_FLOWERS)){flowers.add(state.getBlock());require(state.canSurvive(level,pos),"Native flower support");}
                    if(state.is(Blocks.PINK_PETALS)||state.is(Blocks.BROWN_MUSHROOM)||state.is(BlockTags.SMALL_FLOWERS)||state.is(Blocks.FERN)||state.is(Blocks.LEAF_LITTER)||state.is(Blocks.SHORT_GRASS)){plants++;require(state.canSurvive(level,pos),"Native understory support");require(copy.state(wx,h+1,wz)==state&&sections.computeIfAbsent((h+1)>>4,copy::generate).getBlockState(x,(h+1)&15,z)==state,"Native petal/understory state survives profile and client materialization");}
                    if(x%4!=0||z%4!=0)continue;
                    for(int y:new int[]{-3000,0,2000})require(chunk.getNoiseBiome(wx>>2,y,wz>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2))&&copy.biomes().get(x>>2,y&3,z>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2)),"Stored/source/decoded all-altitude identity");
                    for(int y=h-3;y<=Math.max(h+2,base.canopy().topY(wx,wz));y++)require(base.state(wx,y,wz)==g.surfaceState(wx,y,wz,r),"Analytic/profile floor and canopy agreement");
                    for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,r),"Native canopy/plant heightmap "+type);
                }
                var array=tag.getIntArray("canopy").orElseThrow();if(array.length>0){int old=base.canopy().encode()[0];array[0]=0;require(base.canopy().encode()[0]==old,"Immutable saved snapshot");}
            }
            require(logs>100&&leaves>1000&&plants>10,"Substantial native stand and understory");
            if(kind==2)require(petals>10&&petalStates.size()>4,"Actual supported petal colonies with state variation");require(speciesLeaves>leaves*.6,"Native new-species canopy dominance");
            level.setBlock(cut,Blocks.AIR.defaultBlockState(),2);var chunk=level.getChunkAt(cut);var terrain=((SparseColumn)chunk).blockbranch$terrain();chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(cut).isAir(),"Tree cut persists through lazy eviction");
            boundary(level,g,bed,kind);
            Blockbranch.LOGGER.info("DARK_CHERRY_NATIVE_PASS mode={} kind={} bed={} logs={} leaves={} speciesLeaves={} plants={} flowers={}",reload?"RELOAD":"NEW",kind,bed,logs,leaves,speciesLeaves,plants,flowers);
        }
    }
    private static void boundary(ServerLevel level,RealisticGenerator g,BlockPos bed,int kind){
        var r=level.getChunkSource().randomState();var source=g.getBiomeSource().createUncachedResolver(r);var target=kind==2?Biomes.CHERRY_GROVE:Biomes.DARK_FOREST;
        for(int dz=-512;dz<=512;dz+=16)for(int dx=-512;dx<=512;dx+=16){
            int x=Math.floorDiv(bed.getX()+dx,16)*16,z=Math.floorDiv(bed.getZ()+dz,16)*16,count=0;
            for(int qz=0;qz<4;qz++)for(int qx=0;qx<4;qx++)if(source.getNoiseBiome((x>>2)+qx,0,(z>>2)+qz).is(target))count++;
            if(count==0||count==16)continue;
            for(int oz=0;oz<16;oz++)for(int ox=0;ox<20;ox++){
                int wx=x+ox,wz=z+oz,h=g.drainage(r).sample(wx,wz).height();var chunk=level.getChunk(wx>>4,wz>>4);var base=((SparseColumn)chunk).blockbranch$terrain().base();
                for(int y=h-3;y<=Math.max(h+2,base.canopy().topY(wx,wz));y++)require(chunk.getBlockState(new BlockPos(wx,y,wz))==g.surfaceState(wx,y,wz,r),"Mixed native variant boundary material agreement");
                require(chunk.getNoiseBiome(wx>>2,0,wz>>2).equals(source.getNoiseBiome(wx>>2,0,wz>>2)),"Mixed native variant boundary identity");
            }
            Blockbranch.LOGGER.info("DARK_CHERRY_BOUNDARY_PASS kind={} x={} z={} selectedQuartCells={}",kind,x,z,count);return;
        }
        throw new AssertionError("No mixed dark/cherry boundary kind="+kind);
    }
    private static void verifyBranchStates(ServerLevel level){
        var pos=new BlockPos(0,100,0);
        for(int code=DarkOakBranches.FIRST;code<=DarkOakBranches.LAST;code++){
            int key=((pos.getY()-TerrainShape.FLOOR)<<8);
            var layer=new CanopyLayer(new int[]{(key<<CanopyLayer.STATE_BITS)|code});var state=layer.state(0,100,0);
            require(state.is(DarkOakBranchContent.BRANCH)&&state.is(BlockTags.LOGS),"Actual branch wood supports native leaves");
            require(state.getValue(DarkOakBranchBlock.AXES)==DarkOakBranches.axes(code)&&state.getValue(DarkOakBranchBlock.THICK)==DarkOakBranches.thick(code),"All fourteen branch states materialize correctly");
            require(!Block.isShapeFullBlock(state.getCollisionShape(level,pos)),"Branch collision is narrower than native trunk cube");
            var drops=Block.getDrops(state,level,pos,null,null,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_AXE));
            require(drops.size()==1&&drops.getFirst().is(DarkOakBranchContent.ITEM),"Natural branch harvest returns its actual branch item");
        }
        Blockbranch.LOGGER.info("DARK_OAK_BRANCH_NATIVE_STATES_SUPPORT_COLLISION_HARVEST_PASS states=14");
    }
    public static void probe(ServerPlayer player,int stage){
        var level=player.level();var g=(RealisticGenerator)level.getChunkSource().getGenerator();int kind=Boolean.getBoolean("blockbranch.cherryGroveClient")?2:1;var bed=fixture(g,level.getChunkSource().randomState(),kind);var test=bed.above(40).east();
        if(stage<=92){
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set 6000");
            var v=stage==90?new DarkCherryWoodland.View(bed.getX()+.5,bed.getY()+40,bed.getZ()+.5,0,30):view(g,level.getChunkSource().randomState(),bed,stage==92,kind);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+v.x()+" "+v.y()+" "+v.z()+" "+v.yaw()+" "+v.pitch());
            Blockbranch.LOGGER.info("DARK_CHERRY_CAPTURE_CAMERA kind={} stage={} bed={} view={}",kind,stage,bed,v);
        }else if(stage==93)level.setBlock(test,(kind==2?Blocks.CHERRY_LEAVES:Blocks.DARK_OAK_LEAVES).defaultBlockState().setValue(LeavesBlock.DISTANCE,7).setValue(LeavesBlock.PERSISTENT,false),2);
        else{var base=((SparseColumn)level.getChunkAt(test)).blockbranch$terrain().base();level.setBlock(test,base.state(test.getX(),test.getY(),test.getZ()),2);}
    }
    private static DarkCherryWoodland.View view(RealisticGenerator g,RandomState r,BlockPos bed,boolean elevated,int kind){return blockbranch.realistic.DarkCherryWoodlandFixtures.view(bed.getX(),bed.getY(),bed.getZ(),elevated,kind,(x,z)->g.drainage(r).sample(x,z).height());}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private DarkCherrySmoke(){}
}
