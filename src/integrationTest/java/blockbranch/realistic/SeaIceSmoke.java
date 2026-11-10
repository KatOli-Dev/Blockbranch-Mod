package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
/** Native sea surface, connected liquid support, sparse storage and saved edits. */
public final class SeaIceSmoke {
 public static BlockPos fixture(RealisticGenerator g,RandomState r){var p=blockbranch.realistic.FrozenOceanFixtures.fixture(g.drainage(r),r.seed(),Boolean.getBoolean("blockbranch.seaIceDeep")?2:1,(x,z)->g.icebergsEnabled()&&IcebergField.habitat(r.seed(),x,z,g.drainage(r),new ClimateField(r.seed())));return new BlockPos(p.x(),63,p.z());}
 public static void verify(ServerLevel level,RealisticGenerator g,boolean reload)throws Exception{var r=level.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());
  for(int kind:new int[]{1,2}){var q=blockbranch.realistic.FrozenOceanFixtures.fixture(d,r.seed(),kind,(x,z)->g.icebergsEnabled()&&IcebergField.habitat(r.seed(),x,z,d,c));var bed=new BlockPos(q.x(),63,q.z());var edit=bed.below();var home=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
   if(reload)require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Saved under-ice edit reopens");level.setBlock(edit,home.state(edit.getX(),edit.getY(),edit.getZ()),2);
   var copies=new java.util.HashMap<Long,TerrainProfile>();var resolver=g.getBiomeSource().createUncachedResolver(r);int ice=0,water=0;for(int z=q.z()-16;z<q.z()+16;z++)for(int x=q.x()-16;x<q.x()+16;x++){var pos=new BlockPos(x,63,z);var chunk=level.getChunkAt(pos);var base=((SparseColumn)chunk).blockbranch$terrain().base();var copy=copies.computeIfAbsent(((long)(x>>4)<<32)^((z>>4)&0xffffffffL),key->TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory()));
    for(int y:new int[]{62,63,64})require(level.getBlockState(new BlockPos(x,y,z))==base.state(x,y,z)&&copy.state(x,y,z)==g.surfaceState(x,y,z,r),"Native stored/decoded/analytic ice and water agree");
    if(FrozenOcean.ice(r.seed(),x,z,d,c)){ice++;require(level.getBlockState(pos).is(Blocks.ICE)&&level.getFluidState(pos.below()).isSource()&&level.getBlockState(pos.above()).isAir(),"Ice floats above connected source water with clear air");}else if(level.getFluidState(pos).isSource())water++;
    require(chunk.getNoiseBiome(x>>2,0,z>>2).equals(resolver.getNoiseBiome(x>>2,0,z>>2)),"Stored and resolved marine quart agree");
    for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x&15,z&15)+1==g.getBaseHeight(x,z,type,level,r),"Ice-aware heightmap "+type);
   }
   require(ice>100,"Actual sea ice cover in both depths");require(level.getBiome(bed).is(kind==1?net.minecraft.world.level.biome.Biomes.FROZEN_OCEAN:net.minecraft.world.level.biome.Biomes.DEEP_FROZEN_OCEAN),"Actual native frozen marine holder");
   var invalid=home.encode(level.palettedContainerFactory()).copy();int key=((63-TerrainShape.FLOOR)<<8)|((bed.getZ()&15)*16+(bed.getX()&15));invalid.putIntArray("canopy",new int[]{((key+256)<<CanopyLayer.STATE_BITS)|FrozenRiver.ICE});boolean rejected=false;try{TerrainProfile.decode(invalid,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Ice above physical sea surface rejected");
   require(home.randomTickSections().contains(63>>4),"Native ice remains tickable");
   var melt=net.minecraft.world.level.block.IceBlock.class.getDeclaredMethod("melt",net.minecraft.world.level.block.state.BlockState.class,net.minecraft.world.level.Level.class,BlockPos.class);melt.setAccessible(true);melt.invoke(Blocks.ICE,level.getBlockState(bed),level,bed);require(level.getBlockState(bed).is(Blocks.WATER)&&level.getFluidState(bed).isSource(),"Native sea-ice melt restores source water");RiverSmoke.restore(level,bed);require(level.getBlockState(bed).is(Blocks.ICE),"Procedural sea ice restores after native melt");
   level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);var chunk=level.getChunkAt(edit);chunk.markUnsaved();((SparseColumn)chunk).blockbranch$terrain().flush();
   Blockbranch.LOGGER.info("SEA_ICE_NATIVE_PASS mode={} kind={} bed={} ice={} openWater={} melt=source restored=true",reload?"RELOAD":"NEW",kind,bed,ice,water);
   FrozenSeabedSmoke.verify(level,g,reload,kind);
  }
 }
 public static void probe(ServerPlayer player,int stage){var level=player.level();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();var bed=SeaIceCaptureViews.fixture(g,r);var edit=bed.below();
  if(stage==90&&Boolean.getBoolean("blockbranch.seaIceUnderwater")){var base=((SparseColumn)level.getChunkAt(edit)).blockbranch$terrain().base();level.setBlock(edit,base.state(edit.getX(),edit.getY(),edit.getZ()),2);}
  if(stage<=92){level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
  if(stage>=91&&Boolean.getBoolean("blockbranch.seaIceUnderwaterDiagnostic"))level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"effect give "+player.getName().getString()+" minecraft:night_vision 999999 0 true");level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set 6000");var v=stage==90?new MountainPeaks.View(bed.getX()+.5,bed.getY()+40,bed.getZ()+.5,0,30):SeaIceCaptureViews.view(bed.getX(),bed.getY(),bed.getZ(),stage==92,(x,z)->g.drainage(r).sample(x,z).height());level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+v.x()+" "+v.y()+" "+v.z()+" "+v.yaw()+" "+v.pitch());Blockbranch.LOGGER.info("SEA_ICE_CAPTURE_CAMERA stage={} bed={} view={}",stage,bed,v);
  }else if(stage==93){
   if(Boolean.getBoolean("blockbranch.seaIceUnderwater"))level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+(bed.getX()+.5)+" 103 "+(bed.getZ()+.5));
   level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);
  }else{var base=((SparseColumn)level.getChunkAt(edit)).blockbranch$terrain().base();level.setBlock(edit,base.state(edit.getX(),edit.getY(),edit.getZ()),2);}
 }
 private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
 private SeaIceSmoke(){}
}
