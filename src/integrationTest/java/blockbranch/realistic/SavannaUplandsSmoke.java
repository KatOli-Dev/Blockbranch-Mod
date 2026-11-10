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

/** Native upland identities, soil/rock materials, supported trees and ordinary current-build views. */
public final class SavannaUplandsSmoke {
    private static final Map<DrainageNetwork,BlockPos[]> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
    public static BlockPos fixture(RealisticGenerator g,RandomState r,int kind){
        var d=g.drainage(r);synchronized(FIXTURES){var points=FIXTURES.computeIfAbsent(d,k->new BlockPos[2]);if(points[kind-1]!=null)return points[kind-1];
            var c=new ClimateField(r.seed());
            var p=blockbranch.realistic.SavannaUplandsFixtures.fixture(d,r.seed(),kind);
            var candidates=SavannaUplands.roots(r.seed(),p.x()-128,p.z()-128,p.x()+128,p.z()+128,d,c);
            candidates.sort(Comparator.comparingDouble(root->Math.hypot(root.x()-p.x(),root.z()-p.z())));
            for(var root:candidates){
                if(root.height()<10||SavannaUplands.kind(r.seed(),root.x(),root.z(),d,c)!=kind||SavannaUplands.interiorCount(r.seed(),root.x(),root.z(),kind,d,c)<60)continue;
                var bed=new BlockPos(root.x(),root.y()-1,root.z());boolean clear=true;
                for(boolean elevated:new boolean[]{false,true}){var v=view(g,r,bed,elevated);
                    for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(int dy=1;dy<=3;dy++)if(!g.surfaceState((int)Math.floor(v.x())+dx,(int)Math.floor(v.y())+dy,(int)Math.floor(v.z())+dz,r).isAir())clear=false;
                }
                if(clear){points[kind-1]=bed;Blockbranch.LOGGER.info("SAVANNA_UPLAND_NATURAL_ANCHOR kind={} bed={} slope={}",kind,bed,SlopeCorrection.slope(d,bed.getX(),bed.getZ()));return bed;}
            }
            throw new AssertionError("No clear natural upland view kind="+kind+" anchor="+p);
        }
    }
    public static SavannaUplands.View view(RealisticGenerator g,RandomState r,BlockPos p,boolean elevated){return blockbranch.realistic.SavannaUplandsFixtures.view(p.getX(),p.getY(),p.getZ(),elevated,(x,z)->g.drainage(r).sample(x,z).height());}
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var climate=new ClimateField(r.seed());var source=g.getBiomeSource().createUncachedResolver(r);
        for(int kind:new int[]{1,2}){
            var bed=fixture(g,r,kind);require(level.getBiome(bed).is(kind==2?Biomes.WINDSWEPT_SAVANNA:Biomes.SAVANNA_PLATEAU),"Actual vanilla upland identity");
            for(var root:SavannaUplands.roots(r.seed(),bed.getX()-96,bed.getZ()-96,bed.getX()+96,bed.getZ()+96,d,climate)){
                var soilPos=new BlockPos(root.x(),root.y()-1,root.z());
                require(SavannaUplands.surfaceKind(r.seed(),root.x(),root.z(),d,climate)>0&&level.getBlockState(soilPos).is(Blocks.GRASS_BLOCK),"Every new root has actual native soil support at its physical block");
            }
            var home=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();var cut=bed.above(2);
            require(home.state(cut.getX(),cut.getY(),cut.getZ()).is(BlockTags.LOGS),"Natural rooted tree anchor");
            if(reload)require(level.getBlockState(cut).isAir(),"Saved cut survives reopen");
            level.setBlock(cut,home.state(cut.getX(),cut.getY(),cut.getZ()),2);
            int logs=0,leaves=0,plants=0,rock=0,soil=0;
            for(int cz=(bed.getZ()>>4)-2;cz<=(bed.getZ()>>4)+2;cz++)for(int cx=(bed.getX()>>4)-2;cx<=(bed.getX()>>4)+2;cx++){
                var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var tag=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
                require(Arrays.equals(base.canopy().encode(),copy.canopy().encode()),"Packed snapshot round trip");
                for(int packed:base.canopy().encode()){
                    int key=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;var pos=new BlockPos(x,y,z);var state=base.state(x,y,z);
                    require(chunk.getBlockState(pos)==state&&copy.state(x,y,z)==state&&sections.computeIfAbsent(y>>4,copy::generate).getBlockState(x&15,y&15,z&15)==state,"Native/saved/client canopy materialization");
                    require(d.sample(x,z).waterLevel()==DrainageNetwork.DRY&&y>d.sample(x,z).height(),"Supported dry terrain; water preserved");
                    if(state.is(BlockTags.LOGS))logs++;
                    if(state.is(BlockTags.LEAVES)){
                        leaves++;int distance=state.getValue(LeavesBlock.DISTANCE);boolean support=false;
                        for(var direction:Direction.values()){var near=level.getBlockState(pos.relative(direction));if(near.is(BlockTags.LOGS)&&distance==1||near.is(BlockTags.LEAVES)&&near.getValue(LeavesBlock.DISTANCE)<distance)support=true;}
                        require(support&&!state.getValue(LeavesBlock.PERSISTENT),"Native cross-chunk leaf support");
                    }
                }
                for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                    int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height();var pos=new BlockPos(wx,h+1,wz);var state=level.getBlockState(pos);
                    if(state.is(Blocks.SHORT_DRY_GRASS)||state.is(Blocks.SHORT_GRASS)){plants++;require(state.canSurvive(level,pos),"Actual floor plant support");}
                    if(SavannaUplands.surfaceKind(r.seed(),wx,wz,d,climate)>0){var floor=level.getBlockState(pos.below());if(!SavannaUplands.soil(r.seed(),wx,wz,d))require(floor.is(Blocks.STONE),"Exposed core scarps retain actual native stone");if(floor.is(Blocks.STONE))rock++;else if(floor.is(Blocks.GRASS_BLOCK)||floor.is(Blocks.COARSE_DIRT))soil++;}
                    if(x%4!=0||z%4!=0)continue;
                    for(int y:new int[]{-3000,0,2000})require(chunk.getNoiseBiome(wx>>2,y,wz>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2))&&copy.biomes().get(x>>2,y&3,z>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2)),"Stored/source/decoded all-altitude identity");
                    for(int y=h-3;y<=Math.max(h+2,base.canopy().topY(wx,wz));y++)require(base.state(wx,y,wz)==g.surfaceState(wx,y,wz,r),"Analytic/profile physical materials");
                    for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,r),"Native heightmap "+type);
                }
            }
            require(logs>10&&leaves>30&&plants>0&&soil>0,"Actual trees, grass colonies and supported soil; rock counted separately");
            level.setBlock(cut,Blocks.AIR.defaultBlockState(),2);var chunk=level.getChunkAt(cut);var terrain=((SparseColumn)chunk).blockbranch$terrain();chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(cut).isAir(),"Saved edit survives sparse eviction");
            Blockbranch.LOGGER.info("SAVANNA_UPLAND_NATIVE_PASS mode={} kind={} bed={} logs={} leaves={} plants={} soil={} rock={}",reload?"RELOAD":"NEW",kind,bed,logs,leaves,plants,soil,rock);
        }
        verifyRiverBank(level,g);
    }
    private static void verifyRiverBank(ServerLevel level,RealisticGenerator g){
        var r=level.getChunkSource().randomState();if(r.seed()!=424242)return;var d=g.drainage(r);var c=new ClimateField(r.seed());
        for(int x:new int[]{1551688,1551691}){
            int z=-495152;var p=new BlockPos(x,d.sample(x,z).height(),z);var chunk=level.getChunkAt(p);var base=((SparseColumn)chunk).blockbranch$terrain().base();
            require(RoutedRiver.bankContains(r.seed(),x,z,d,c)&&SavannaUplands.kind(r.seed(),x,z,d,c)==0&&SavannaUplands.surfaceKind(r.seed(),x,z,d,c)==0,"Existing River identity protected at recorded overlap");
            // Physical getBiome uses Minecraft's jittered nearest-quart sample.
            // Compare the stored quart with its source, independently of that blend.
            for(int y:new int[]{-3000,0,p.getY()>>2,2000})require(chunk.getNoiseBiome(x>>2,y,z>>2).is(Biomes.RIVER)&&g.getBiomeSource().createUncachedResolver(r).getNoiseBiome(x>>2,y,z>>2).is(Biomes.RIVER),"Stored/source River quart holder retained");
            require(g.warmCover(x,z,r)==0&&level.getBlockState(p)==g.surfaceState(x,p.getY(),z,r)&&base.state(x,p.getY(),z)==level.getBlockState(p),"No competing upland cap on existing bank; native/profile/analytic materials agree");
            if(x==1551688)require(level.getBlockState(p).is(Blocks.MUD)||level.getBlockState(p).is(Blocks.SAND),"Physical bank keeps its accepted native mud/sand");
            Blockbranch.LOGGER.info("SAVANNA_UPLAND_RETAINED_RIVER_BANK_NATIVE_PASS bed={} floor={} physicalBank={} sampledBiome={} quartBiome={}",p,level.getBlockState(p),RoutedRiver.surfaceBankContains(r.seed(),x,z,d,c),level.getBiome(p).unwrapKey(),chunk.getNoiseBiome(x>>2,p.getY()>>2,z>>2).unwrapKey());
        }
    }
    public static void probe(ServerPlayer player,int stage){
        var level=player.level();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();int kind=Boolean.getBoolean("blockbranch.windsweptSavannaClient")?2:1;var bed=fixture(g,r,kind);var test=bed.above(40).east();
        if(stage<=92){
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set 6000");
            var v=stage==90?new SavannaUplands.View(bed.getX()+.5,bed.getY()+40,bed.getZ()+.5,0,30):view(g,r,bed,stage==92);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+v.x()+" "+v.y()+" "+v.z()+" "+v.yaw()+" "+v.pitch());
            Blockbranch.LOGGER.info("SAVANNA_UPLAND_CAPTURE_CAMERA kind={} stage={} bed={} view={}",kind,stage,bed,v);
        }else if(stage==93)level.setBlock(test,Blocks.ACACIA_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,7).setValue(LeavesBlock.PERSISTENT,false),2);
        else{var base=((SparseColumn)level.getChunkAt(test)).blockbranch$terrain().base();level.setBlock(test,base.state(test.getX(),test.getY(),test.getZ()),2);}
    }
    private static void require(boolean v,String message){if(!v)throw new AssertionError(message);}
    private SavannaUplandsSmoke(){}
}
