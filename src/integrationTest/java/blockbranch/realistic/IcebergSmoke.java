package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;

public final class IcebergSmoke {
 public static BlockPos fixture(RealisticGenerator g,RandomState r){
  if(!g.icebergsEnabled())throw new AssertionError("Icebergs not enabled by saved palette");
  var b=g.icebergs(r).fixture(Integer.getInteger("blockbranch.icebergForm",-1));int y=g.icebergs(r).column(b.x()>>4,b.z()>>4).top(b.x(),b.z());return new BlockPos(b.x(),y,b.z());
 }
 public static MountainPeaks.View view(int x,int y,int z,boolean elevated){
  double a=Math.toRadians(elevated?70:35),distance=elevated?128:132,cx=x+.5+Math.sin(a)*distance,cz=z+.5-Math.cos(a)*distance;
  double cy=elevated?192:66,target=64+Math.max(3,(y-63)*.5);
  return new MountainPeaks.View(cx,cy,cz,(float)Math.toDegrees(Math.atan2(cx-x-.5,z+.5-cz)),(float)-Math.toDegrees(Math.atan2(target-(cy+1.62),distance)));
 }
 public static void verify(ServerLevel level,RealisticGenerator g,boolean reload)throws Exception{
  var r=level.getChunkSource().randomState();var bed=fixture(g,r);var edit=bed.below();var home=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
  if(reload)require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Saved iceberg edit reopens");level.setBlock(edit,home.state(edit.getX(),edit.getY(),edit.getZ()),2);
  require(level.getBiome(bed).is(net.minecraft.world.level.biome.Biomes.ICE_SPIKES),"Actual marine Ice Spikes holder");
  boolean weatherChecked=false;int columns=0,solids=0,submerged=0,preserved=0;var source=g.getBiomeSource().createUncachedResolver(r);
  for(int cz=(bed.getZ()>>4)-2;cz<=(bed.getZ()>>4)+2;cz++)for(int cx=(bed.getX()>>4)-2;cx<=(bed.getX()>>4)+2;cx++){
   var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var copy=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
   for(int z=0;z<16;z++)for(int x=0;x<16;x++){
    int wx=cx*16+x,wz=cz*16+z,low=base.floatingIce().bottom(wx,wz),high=base.floatingIce().top(wx,wz);
    if(low!=Integer.MIN_VALUE){columns++;require(low>base.groundHeight(wx,wz)+8,"Marine keel clears seabed");
     for(int y=low-1;y<=high+1;y++){var p=new BlockPos(wx,y,wz);require(level.getBlockState(p)==base.state(wx,y,wz)&&copy.state(wx,y,wz)==g.surfaceState(wx,y,wz,r),"Native/stored/decoded/analytic floating solids");if(y>=low&&y<=high){solids++;if(y<64)submerged++;}}
    }
    if(low==Integer.MIN_VALUE){var pos=new BlockPos(wx,63,wz);boolean ice=FrozenOcean.ice(r.seed(),wx,wz,g.drainage(r),new ClimateField(r.seed()))&&!IcebergField.habitat(r.seed(),wx,wz,g.drainage(r),new ClimateField(r.seed()));require(level.getBlockState(pos).is(ice?Blocks.ICE:Blocks.WATER)&&level.getFluidState(pos.below()).isSource(),"Open iceberg water and separate frozen-ocean ice follow their own habitats");preserved++;}
    if((x&3)==0&&(z&3)==0){require(chunk.getNoiseBiome(wx>>2,0,wz>>2).equals(source.getNoiseBiome(wx>>2,0,wz>>2)),"Stored marine biome agrees with source");for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,r),"Iceberg-aware heightmap "+type);}
   }
  }
  for(int dz=-192;dz<=192;dz+=32)for(int dx=-192;dx<=192;dx+=32){
   int x=bed.getX()+dx,z=bed.getZ()+dz;var s=g.drainage(r).sample(x,z);
   if(s.waterLevel()!=64||64-s.height()-1<16||g.icebergs(r).column(x>>4,z>>4).bottom(x,z)!=Integer.MIN_VALUE)continue;
   boolean ice=FrozenOcean.ice(r.seed(),x,z,g.drainage(r),new ClimateField(r.seed()))&&!IcebergField.habitat(r.seed(),x,z,g.drainage(r),new ClimateField(r.seed()));
   var water=new BlockPos(x,63,z);require(level.getBlockState(water).is(ice?Blocks.ICE:Blocks.WATER),"Open-water habitat and neighbouring ice cover retained");preserved++;
   if(!weatherChecked&&g.keepsIcebergWaterOpen(x,z,r)){
    require(!level.getBiome(water).value().shouldFreeze(level,water,false),"Native freezing decision respects the open-water habitat");
    level.tickPrecipitation(water);require(level.getBlockState(water).is(Blocks.WATER),"Native weather does not recreate an ice sheet");weatherChecked=true;
   }
  }
  require(weatherChecked&&preserved>100&&columns>500&&solids>8192&&submerged>solids*.8,"Real partly submerged marine body");
  // A real legacy profile lacks the new field; reading it must retain its blocks.
  var origin=new BlockPos(0,g.drainage(r).sample(0,0).height(),0);var old=((SparseColumn)level.getChunkAt(origin)).blockbranch$terrain().base();var legacy=old.encode(level.palettedContainerFactory());legacy.putInt("version",57);legacy.remove("floating_ice");var decoded=TerrainProfile.decode(legacy,level.palettedContainerFactory());
  for(int dy=-8;dy<=8;dy++)require(decoded.state(0,origin.getY()+dy,0)==old.state(0,origin.getY()+dy,0),"Version-57 profile blocks retained");
  var version58=home.encode(level.palettedContainerFactory());version58.putInt("version",58);var decoded58=TerrainProfile.decode(version58,level.palettedContainerFactory());
  require(java.util.Arrays.equals(home.floatingIce().encode(),decoded58.floatingIce().encode()),"Version-58 floating body retained by material-format reader");
  for(int dy=-32;dy<=8;dy++)require(decoded58.state(bed.getX(),bed.getY()+dy,bed.getZ())==home.state(bed.getX(),bed.getY()+dy,bed.getZ()),"Version-58 ice/water/air blocks retained");
  var bad=home.encode(level.palettedContainerFactory());bad.putIntArray("floating_ice",new int[255]);boolean rejected=false;try{TerrainProfile.decode(bad,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Malformed compact layer rejected");
  level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);var chunk=level.getChunkAt(edit);chunk.markUnsaved();var terrain=((SparseColumn)chunk).blockbranch$terrain();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Iceberg edit survives eviction");
  Blockbranch.LOGGER.info("ICEBERG_NATIVE_PASS mode={} bed={} columns={} solids={} submerged={} preservedSeaIce={} legacy57=true legacy58=true openWeather=true",reload?"RELOAD":"NEW",bed,columns,solids,submerged,preserved);
 }
 public static void probe(ServerPlayer player,int stage){
  var level=player.level();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();var bed=fixture(g,r);var edit=bed.below();
  if(stage<=92){level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set 6000");
   var v=stage==90?new MountainPeaks.View(bed.getX()+.5,bed.getY()+40,bed.getZ()+.5,0,30):view(bed.getX(),bed.getY(),bed.getZ(),stage==92);
   level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+v.x()+" "+v.y()+" "+v.z()+" "+v.yaw()+" "+v.pitch());Blockbranch.LOGGER.info("ICEBERG_CAPTURE_CAMERA stage={} bed={} view={}",stage,bed,v);
  }else if(stage==93)level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);else{var base=((SparseColumn)level.getChunkAt(edit)).blockbranch$terrain().base();level.setBlock(edit,base.state(edit.getX(),edit.getY(),edit.getZ()),2);}
 }
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 private IcebergSmoke(){}
}
