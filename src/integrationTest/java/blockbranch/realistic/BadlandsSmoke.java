package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Native arid materials, true soil support, height queries and durable profiles/edits. */
public final class BadlandsSmoke {
    private static final Map<DrainageNetwork,BlockPos[]> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
    public static BlockPos fixture(RealisticGenerator g,RandomState r,int kind){
        var d=g.drainage(r);synchronized(FIXTURES){var a=FIXTURES.computeIfAbsent(d,k->new BlockPos[3]);if(a[kind-1]!=null)return a[kind-1];
            var p=blockbranch.realistic.BadlandsFixtures.fixture(d,r.seed(),kind);var bed=new BlockPos(p.x(),d.sample(p.x(),p.z()).height(),p.z());
            for(boolean elevated:new boolean[]{false,true}){var v=view(g,r,bed,elevated);require(g.surfaceState((int)Math.floor(v.x()),(int)Math.floor(v.y()+1.62),(int)Math.floor(v.z()),r).isAir(),"Natural badlands camera clear");}
            a[kind-1]=bed;Blockbranch.LOGGER.info("BADLANDS_NATURAL_ANCHOR kind={} bed={} interior={}/81",kind,bed,Badlands.interiorCount(r.seed(),p.x(),p.z(),kind,d,new ClimateField(r.seed())));return bed;
        }
    }
    public static MountainPeaks.View view(RealisticGenerator g,RandomState r,BlockPos p,boolean elevated){return blockbranch.realistic.BadlandsFixtures.view(p.getX(),p.getY(),p.getZ(),elevated,(x,z)->g.drainage(r).sample(x,z).height(),Badlands.kind(r.seed(),p.getX(),p.getZ(),g.drainage(r),new ClimateField(r.seed())));}
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){verify(level,g,reload,1,2,3);}
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload,int... kinds){
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());var source=g.getBiomeSource().createUncachedResolver(r);
        for(int kind:kinds){
            var bed=fixture(g,r,kind);var identity=kind==3?Biomes.ERODED_BADLANDS:kind==2?Biomes.WOODED_BADLANDS:Biomes.BADLANDS;
            require(level.getChunkAt(bed).getNoiseBiome(bed.getX()>>2,0,bed.getZ()>>2).is(identity),"Actual stored badlands holder");
            var home=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();var edit=bed.below();
            if(reload)require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Saved arid edit reopens");
            level.setBlock(edit,home.state(edit.getX(),edit.getY(),edit.getZ()),2);
            int owned=0,soil=0,redSand=0,plants=0;var beds=new HashSet<net.minecraft.world.level.block.state.BlockState>();
            for(int cz=(bed.getZ()>>4)-2;cz<=(bed.getZ()>>4)+2;cz++)for(int cx=(bed.getX()>>4)-2;cx<=(bed.getX()>>4)+2;cx++){
                var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var factory=level.palettedContainerFactory();var copy=TerrainProfile.decode(base.encode(factory),factory);
                for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                    int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height();var p=new BlockPos(wx,h,wz);boolean selected=Badlands.surfaceKind(r.seed(),wx,wz,d,c)==kind;
                    if(selected){
                        owned++;var floor=level.getBlockState(p);if(floor.is(Blocks.COARSE_DIRT))soil++;else if(floor.is(Blocks.RED_SAND))redSand++;
                        for(int y=h-63;y<=h;y++)beds.add(base.state(wx,y,wz));
                        var plant=level.getBlockState(p.above());if(plant.is(Blocks.DEAD_BUSH)||plant.is(Blocks.SHORT_DRY_GRASS)){require(plant.canSurvive(level,p.above()),"Native dry plants have correct substrate");plants++;}
                    }
                    require(base.groundHeight(wx,wz)==h&&copy.groundHeight(wx,wz)==h,"Decoded terrain datum retained");
                    for(int y=h-65;y<=h+2;y++)require(level.getBlockState(new BlockPos(wx,y,wz))==base.state(wx,y,wz)&&copy.state(wx,y,wz)==g.surfaceState(wx,y,wz,r),"Native/stored/decoded/analytic material agreement through full formation");
                    if(x%4!=0||z%4!=0)continue;
                    for(int y:new int[]{-3000,0,2000})require(chunk.getNoiseBiome(wx>>2,y,wz>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2))&&copy.biomes().get(x>>2,y&3,z>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2)),"Raw source/stored/decoded quart identities agree");
                    for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,r),"Native badlands heightmap "+type);
                }
            }
            require(owned>4000&&beds.size()>=6,"Natural native formation and strata palette");
            var roots=Badlands.roots(r.seed(),bed.getX()-64,bed.getZ()-64,bed.getX()+64,bed.getZ()+64,d,c);
            for(var root:roots){var p=new BlockPos(root.x(),root.y()-1,root.z());require(level.getBlockState(p).is(Blocks.COARSE_DIRT)&&level.getBlockState(p.above()).is(Blocks.OAK_LOG),"Native oak anchored in actual soil");}
            var encoded=home.encode(level.palettedContainerFactory());for(int invalid:new int[]{-1,21,31}){var tag=encoded.copy();var values=tag.getByteArray("desert").orElseThrow();values[0]=(byte)invalid;tag.putByteArray("desert",values);boolean rejected=false;try{TerrainProfile.decode(tag,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Invalid arid code rejected");}
            level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);var chunk=level.getChunkAt(edit);var terrain=((SparseColumn)chunk).blockbranch$terrain();chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Arid edit survives sparse eviction");
            Blockbranch.LOGGER.info("BADLANDS_NATIVE_PASS mode={} kind={} bed={} owned={} soil={} redSand={} palette={} plants={} roots={}",reload?"RELOAD":"NEW",kind,bed,owned,soil,redSand,beds.size(),plants,roots.size());
        }
    }
    public static void probe(ServerPlayer player,int stage){
        var level=player.level();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();int kind=Boolean.getBoolean("blockbranch.erodedBadlandsClient")?3:Boolean.getBoolean("blockbranch.woodedBadlandsClient")?2:1;var bed=fixture(g,r,kind);var edit=bed.below();
        if(stage<=92){
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set 6000");
            var v=stage==90?new MountainPeaks.View(bed.getX()+.5,bed.getY()+40,bed.getZ()+.5,0,30):view(g,r,bed,stage==92);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+v.x()+" "+v.y()+" "+v.z()+" "+v.yaw()+" "+v.pitch());
            Blockbranch.LOGGER.info("BADLANDS_CAPTURE_CAMERA kind={} stage={} bed={} view={}",kind,stage,bed,v);
        }else if(stage==93)level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);
        else{var base=((SparseColumn)level.getChunkAt(edit)).blockbranch$terrain().base();level.setBlock(edit,base.state(edit.getX(),edit.getY(),edit.getZ()),2);}
    }
    private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
    private BadlandsSmoke(){}
}
