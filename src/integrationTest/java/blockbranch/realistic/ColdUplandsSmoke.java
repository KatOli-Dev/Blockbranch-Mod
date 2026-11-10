package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Native cold upland materials, true soil support, height queries and durable profiles/edits. */
public final class ColdUplandsSmoke {
    private static final Map<DrainageNetwork,BlockPos[]> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
    public static BlockPos fixture(RealisticGenerator g,RandomState r,int kind){
        var d=g.drainage(r);synchronized(FIXTURES){var a=FIXTURES.computeIfAbsent(d,k->new BlockPos[2]);if(a[kind-1]!=null)return a[kind-1];
            var p=blockbranch.realistic.ColdUplandsFixtures.fixture(d,r.seed(),kind);var bed=new BlockPos(p.x(),d.sample(p.x(),p.z()).height(),p.z());
            for(boolean elevated:new boolean[]{false,true}){var v=view(g,r,bed,kind,elevated);require(g.surfaceState((int)Math.floor(v.x()),(int)Math.floor(v.y()+1.62),(int)Math.floor(v.z()),r).isAir(),"Natural cold upland camera clear");}
            a[kind-1]=bed;Blockbranch.LOGGER.info("COLD_UPLAND_NATURAL_ANCHOR kind={} bed={} interior={}/81",kind,bed,ColdUplands.interiorCount(r.seed(),p.x(),p.z(),kind,d,new ClimateField(r.seed())));return bed;
        }
    }
    public static MountainPeaks.View view(RealisticGenerator g,RandomState r,BlockPos p,int kind,boolean elevated){return blockbranch.realistic.ColdUplandsFixtures.view(p.getX(),p.getY(),p.getZ(),kind,elevated,(x,z)->g.drainage(r).sample(x,z).height());}
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        var r=level.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());var source=g.getBiomeSource().createUncachedResolver(r);
        for(int kind:new int[]{1,2}){
            var bed=fixture(g,r,kind);var identity=kind==2?Biomes.FROZEN_PEAKS:Biomes.WINDSWEPT_GRAVELLY_HILLS;
            require(level.getChunkAt(bed).getNoiseBiome(bed.getX()>>2,0,bed.getZ()>>2).is(identity),"Actual stored cold upland holder");
            var home=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();var edit=bed.below();
            if(reload)require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Saved cold upland edit reopens");
            level.setBlock(edit,home.state(edit.getX(),edit.getY(),edit.getZ()),2);
            var column=g.getBaseColumn(bed.getX(),bed.getZ(),net.minecraft.world.level.LevelHeightAccessor.create(bed.getY()-16,64),r);
            for(int y=bed.getY()-12;y<=bed.getY()+20;y++)require(column.getBlock(y)==g.surfaceState(bed.getX(),y,bed.getZ(),r),"Bounded base-column query uses the same cold materials and canopy");
            int owned=0,gravel=0,soil=0,snow=0,ice=0,layers=0;
            for(int cz=(bed.getZ()>>4)-2;cz<=(bed.getZ()>>4)+2;cz++)for(int cx=(bed.getX()>>4)-2;cx<=(bed.getX()>>4)+2;cx++){
                var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var factory=level.palettedContainerFactory();var copy=TerrainProfile.decode(base.encode(factory),factory);
                for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                    int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height();var point=new BlockPos(wx,h,wz);
                    if(ColdUplands.surfaceKind(r.seed(),wx,wz,d,c)==kind){
                        owned++;var floor=level.getBlockState(point);
                        if(floor.is(Blocks.GRAVEL)){gravel++;require(level.getBlockState(point.below()).isSolidRender(),"Supported native gravel mantle");}
                        else if(floor.is(Blocks.COARSE_DIRT)||floor.is(Blocks.GRASS_BLOCK))soil++;
                        else if(floor.is(Blocks.SNOW_BLOCK))snow++;
                        else if(floor.is(Blocks.PACKED_ICE))ice++;
                        else throw new AssertionError("Unexpected upland surface "+floor+" at "+point);
                        var above=level.getBlockState(point.above());
                        if(above.is(Blocks.SNOW)){require(floor.is(Blocks.SNOW_BLOCK)&&above.canSurvive(level,point.above()),"Native snow layer supported by actual firn cap");layers++;}
                        if(above.is(Blocks.SHORT_GRASS)||above.is(Blocks.SHORT_DRY_GRASS))require(above.canSurvive(level,point.above()),"Native grass on actual soil");
                    }
                    for(int y=h-12;y<=h+2;y++)require(level.getBlockState(new BlockPos(wx,y,wz))==base.state(wx,y,wz)&&copy.state(wx,y,wz)==g.surfaceState(wx,y,wz,r),"Native/stored/decoded/analytic cold materials agree");
                    if(x%4!=0||z%4!=0)continue;
                    for(int y:new int[]{-3000,0,2000})require(chunk.getNoiseBiome(wx>>2,y,wz>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2))&&copy.biomes().get(x>>2,y&3,z>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2)),"All raw quart identities agree");
                    for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,r),"Cold upland heightmap "+type);
                }
            }
            require(owned>3500&&(kind==1?gravel+soil==owned:snow+ice==owned),"Physical native material domain");
            var roots=ColdUplands.roots(r.seed(),bed.getX()-64,bed.getZ()-64,bed.getX()+64,bed.getZ()+64,d,c);
            for(var root:roots){var pos=new BlockPos(root.x(),root.y()-1,root.z());require((level.getBlockState(pos).is(Blocks.COARSE_DIRT)||level.getBlockState(pos).is(Blocks.GRASS_BLOCK))&&level.getBlockState(pos.above()).is(root.family()==15?Blocks.SPRUCE_WOOD:Blocks.OAK_LOG),"Actual soil and native supported tree stem root="+root+" floor="+level.getBlockState(pos)+" stem="+level.getBlockState(pos.above()));}
            var encoded=home.encode(level.palettedContainerFactory());for(int invalid:new int[]{-1,14,15,32}){var tag=encoded.copy();var values=tag.getByteArray("cold_steppe").orElseThrow();values[0]=(byte)invalid;tag.putByteArray("cold_steppe",values);boolean rejected=false;try{TerrainProfile.decode(tag,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Invalid cold material code rejected");}
            level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);var chunk=level.getChunkAt(edit);var terrain=((SparseColumn)chunk).blockbranch$terrain();chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Arid edit survives sparse eviction");
            Blockbranch.LOGGER.info("COLD_UPLAND_NATIVE_PASS mode={} kind={} bed={} owned={} gravel={} soil={} snow={} ice={} layers={} roots={}",reload?"RELOAD":"NEW",kind,bed,owned,gravel,soil,snow,ice,layers,roots.size());
        }
    }
    public static void probe(ServerPlayer player,int stage){
        var level=player.level();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();int kind=Boolean.getBoolean("blockbranch.frozenPeaksClient")?2:1;var bed=fixture(g,r,kind);var edit=bed.below();
        if(stage<=92){
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set 6000");
            var v=stage==90?new MountainPeaks.View(bed.getX()+.5,bed.getY()+40,bed.getZ()+.5,0,30):view(g,r,bed,kind,stage==92);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+v.x()+" "+v.y()+" "+v.z()+" "+v.yaw()+" "+v.pitch());
            Blockbranch.LOGGER.info("COLD_UPLAND_CAPTURE_CAMERA kind={} stage={} bed={} view={}",kind,stage,bed,v);
        }else if(stage==93)level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);
        else{var base=((SparseColumn)level.getChunkAt(edit)).blockbranch$terrain().base();level.setBlock(edit,base.state(edit.getX(),edit.getY(),edit.getZ()),2);}
    }
    private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
    private ColdUplandsSmoke(){}
}
