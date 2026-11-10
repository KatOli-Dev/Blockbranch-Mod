package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Native summit identities, unchanged rock/talus, supported snow and durable local edits. */
public final class MountainPeaksSmoke {
    private static final Map<DrainageNetwork,BlockPos[]> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
    public static BlockPos fixture(RealisticGenerator g,RandomState r,int kind){
        var d=g.drainage(r);synchronized(FIXTURES){var a=FIXTURES.computeIfAbsent(d,k->new BlockPos[2]);if(a[kind-1]!=null)return a[kind-1];
            var p=blockbranch.realistic.MountainPeaksFixtures.fixture(d,r.seed(),kind);var bed=new BlockPos(p.x(),d.sample(p.x(),p.z()).height(),p.z());
            for(boolean elevated:new boolean[]{false,true}){var v=view(g,r,bed,kind,elevated);require(g.surfaceState((int)Math.floor(v.x()),(int)Math.floor(v.y()+1.62),(int)Math.floor(v.z()),r).isAir(),"Natural peak camera lens is clear");}
            a[kind-1]=bed;Blockbranch.LOGGER.info("PEAK_NATURAL_ANCHOR kind={} bed={} slope={} interior={}/81",kind,bed,SlopeCorrection.slope(d,p.x(),p.z()),MountainPeaks.interiorCount(r.seed(),p.x(),p.z(),kind,d,new ClimateField(r.seed())));return bed;
        }
    }
    public static MountainPeaks.View view(RealisticGenerator g,RandomState r,BlockPos p,int kind,boolean elevated){if(kind==2&&elevated)return MountainPeaks.overview(p.getX(),p.getY(),p.getZ(),(x,z)->g.drainage(r).sample(x,z).height());return blockbranch.realistic.MountainPeaksFixtures.view(p.getX(),p.getY(),p.getZ(),elevated,(x,z)->g.drainage(r).sample(x,z).height());}
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());var source=g.getBiomeSource().createUncachedResolver(r);
        for(int kind:new int[]{1,2}){
            var bed=fixture(g,r,kind);var identity=kind==2?Biomes.JAGGED_PEAKS:Biomes.STONY_PEAKS;
            require(level.getChunkAt(bed).getNoiseBiome(bed.getX()>>2,0,bed.getZ()>>2).is(identity),"Actual stored vanilla peak identity");
            var home=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();var edit=bed.below();
            if(reload)require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Saved peak edit survives reopen");
            level.setBlock(edit,home.state(edit.getX(),edit.getY(),edit.getZ()),2);
            int snow=0,stone=0,debris=0,selected=0;var layers=new TreeSet<Integer>();
            for(int cz=(bed.getZ()>>4)-2;cz<=(bed.getZ()>>4)+2;cz++)for(int cx=(bed.getX()>>4)-2;cx<=(bed.getX()>>4)+2;cx++){
                var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var copy=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
                require(Arrays.equals(base.canopy().encode(),copy.canopy().encode()),"Snow snapshot round trip");
                for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                    int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height();var p=new BlockPos(wx,h,wz);var floor=level.getBlockState(p);int depth=g.screeDepth(wx,wz,r);
                    require(base.groundHeight(wx,wz)==h&&copy.groundHeight(wx,wz)==h,"Decoded bare terrain datum retained independently of surface material");
                    boolean owned=MountainPeaks.surfaceKind(r.seed(),wx,wz,d,c)==kind;
                    if(owned){require(floor.is(depth>0?ScreeContent.LOOSE_SCREE:Blocks.STONE),"Existing physical rock/talus retained");if(floor.is(ScreeContent.LOOSE_SCREE))debris++;else stone++;}
                    if(MountainPeaks.kind(r.seed(),wx,wz,d,c)==kind)selected++;
                    var cover=level.getBlockState(p.above());
                    if(cover.is(Blocks.SNOW)){
                        if(owned){snow++;layers.add(cover.getValue(SnowLayerBlock.LAYERS));}
                        require(cover.canSurvive(level,p.above())&&d.sample(wx,wz).waterLevel()==DrainageNetwork.DRY&&level.getFluidState(p.above()).isEmpty(),"Native grounded dry snow support");
                    }
                    for(int y=h-3;y<=h+2;y++)require(level.getBlockState(new BlockPos(wx,y,wz))==base.state(wx,y,wz)&&copy.state(wx,y,wz)==g.surfaceState(wx,y,wz,r),"Native/profile/decoded/analytic materials agree");
                    if(x%4!=0||z%4!=0)continue;
                    for(int y:new int[]{-3000,0,2000})require(chunk.getNoiseBiome(wx>>2,y,wz>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2))&&copy.biomes().get(x>>2,y&3,z>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2)),"Stored/source/decoded quart identity at all altitudes");
                    for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,r),"Native peak heightmap "+type);
                }
            }
            require(selected>3000&&stone+debris>3000,"Representative native summit interior");
            require(kind==2?snow>100&&layers.size()>1:snow==0,"Cold peak drifts and warm bare peak distinction");
            level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);var chunk=level.getChunkAt(edit);var terrain=((SparseColumn)chunk).blockbranch$terrain();chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Peak edit survives sparse eviction");
            Blockbranch.LOGGER.info("PEAK_NATIVE_PASS mode={} kind={} bed={} selected={} stone={} debris={} snow={} layers={}",reload?"RELOAD":"NEW",kind,bed,selected,stone,debris,snow,layers);
        }
    }
    public static void probe(ServerPlayer player,int stage){
        var level=player.level();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();int kind=Boolean.getBoolean("blockbranch.jaggedPeaksClient")?2:1;var bed=fixture(g,r,kind);var edit=bed.below();
        if(stage<=92){
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set 6000");
            var v=stage==90?new MountainPeaks.View(bed.getX()+.5,bed.getY()+40,bed.getZ()+.5,0,30):view(g,r,bed,kind,stage==92);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+v.x()+" "+v.y()+" "+v.z()+" "+v.yaw()+" "+v.pitch());
            Blockbranch.LOGGER.info("PEAK_CAPTURE_CAMERA kind={} stage={} bed={} view={}",kind,stage,bed,v);
        }else if(stage==93)level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);
        else{var base=((SparseColumn)level.getChunkAt(edit)).blockbranch$terrain().base();level.setBlock(edit,base.state(edit.getX(),edit.getY(),edit.getZ()),2);}
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private MountainPeaksSmoke(){}
}
