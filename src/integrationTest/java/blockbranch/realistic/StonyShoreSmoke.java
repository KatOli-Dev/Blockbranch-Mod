package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Actual formed coastal geometry, native bedrock/gravel, sparse storage and saved edits. */
public final class StonyShoreSmoke {
 private static final Map<DrainageNetwork,BlockPos> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
 public static BlockPos fixture(RealisticGenerator g,RandomState r){var d=g.drainage(r);synchronized(FIXTURES){return FIXTURES.computeIfAbsent(d,k->{var p=blockbranch.realistic.StonyShoreFixtures.fixture(d,r.seed());return new BlockPos(p.x(),d.sample(p.x(),p.z()).height(),p.z());});}}
 public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){var r=level.getChunkSource().randomState();var d=g.drainage(r);var bed=fixture(g,r);var source=g.getBiomeSource().createUncachedResolver(r);var home=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();var edit=bed.below();
  require(level.getBiome(bed).is(Biomes.STONY_SHORE),"Stored actual native Stony Shore");if(reload)require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Saved coastal edit reopens");level.setBlock(edit,home.state(edit.getX(),edit.getY(),edit.getZ()),2);
  int owned=0,stone=0,gravel=0,raised=0,wet=0;
  for(int cz=(bed.getZ()>>4)-2;cz<=(bed.getZ()>>4)+2;cz++)for(int cx=(bed.getX()>>4)-2;cx<=(bed.getX()>>4)+2;cx++){
   var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var factory=level.palettedContainerFactory();var copy=TerrainProfile.decode(base.encode(factory),factory);var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
   for(int z=0;z<16;z++)for(int x=0;x<16;x++){int wx=cx*16+x,wz=cz*16+z;var s=d.sample(wx,wz);var p=new BlockPos(wx,s.height(),wz);
    if(StonyShore.surfaceContains(wx,wz,d)){owned++;var state=level.getBlockState(p);if(state.is(Blocks.STONE))stone++;else if(state.is(Blocks.GRAVEL)){gravel++;require(level.getBlockState(p.below()).is(Blocks.STONE),"Thin native gravel supported by actual bedrock");}else throw new AssertionError("Nonrocky bluff surface "+state+" at "+p);if(s.height()>s.regionalHeight())raised++;if(s.waterLevel()!=DrainageNetwork.DRY)wet++;
     for(int v:base.canopy().encode()){int key=v>>>CanopyLayer.STATE_BITS;if((key&255)==z*16+x)throw new AssertionError("No inherited vegetation on exposed coastal bedrock");}
    }
    for(int y=s.height()-8;y<=Math.max(s.height()+2,TerrainShape.SEA_LEVEL);y++)require(level.getBlockState(new BlockPos(wx,y,wz))==base.state(wx,y,wz)&&copy.state(wx,y,wz)==g.surfaceState(wx,y,wz,r)&&copy.state(wx,y,wz)==sections.computeIfAbsent(y>>4,copy::generate).getBlockState(x&15,y&15,z&15),"Native/profile/decoded/analytic/section coastal geometry and material agreement");
    if(x%4!=0||z%4!=0)continue;for(int y:new int[]{-3000,0,2000})require(chunk.getNoiseBiome(wx>>2,y,wz>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2))&&copy.biomes().get(x>>2,y&3,z>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2)),"All stored/decoded/resolved coastal quart identities agree");for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,r),"Exact coastal heightmap "+type);
   }
  }
  require(owned>1000&&stone>100&&gravel>0&&raised>100,"Genuine native raised rock face and supported gravel");
  var column=g.getBaseColumn(bed.getX(),bed.getZ(),net.minecraft.world.level.LevelHeightAccessor.create(bed.getY()-16,64),r);for(int y=bed.getY()-16;y<bed.getY()+32;y++)require(column.getBlock(y)==g.surfaceState(bed.getX(),y,bed.getZ(),r),"Bounded actual raised coastal base-column query");
  var tag=home.encode(level.palettedContainerFactory());var invalid=tag.copy();var values=invalid.getByteArray("sandy_shore").orElseThrow();values[0]=5;invalid.putByteArray("sandy_shore",values);boolean rejected=false;try{TerrainProfile.decode(invalid,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Unknown coastal material code rejected");invalid=tag.copy();invalid.putInt("version",56);rejected=false;try{TerrainProfile.decode(invalid,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Old coastal profile version explicitly rejected");
  invalid=tag.copy();var coast=invalid.getByteArray("sandy_shore").orElseThrow();coast[0]=3;invalid.putByteArray("sandy_shore",coast);var soil=invalid.getByteArray("woodland").orElseThrow();soil[0]=3;invalid.putByteArray("woodland",soil);rejected=false;try{TerrainProfile.decode(invalid,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Coastal rock cannot decode under a competing soil palette");
  level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);var chunk=level.getChunkAt(edit);chunk.markUnsaved();var terrain=((SparseColumn)chunk).blockbranch$terrain();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Coastal edit survives sparse eviction");
  Blockbranch.LOGGER.info("STONY_SHORE_NATIVE_PASS mode={} bed={} owned={} stone={} gravel={} raised={} wet={}",reload?"RELOAD":"NEW",bed,owned,stone,gravel,raised,wet);
 }
 public static void probe(ServerPlayer player,int stage){var level=player.level();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();var bed=fixture(g,r);var edit=bed.below();
  if(stage<=92){level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set 6000");var v=stage==90?new MountainPeaks.View(bed.getX()+.5,bed.getY()+40,bed.getZ()+.5,0,30):blockbranch.realistic.StonyShoreFixtures.view(bed.getX(),bed.getY(),bed.getZ(),stage==92,(x,z)->g.drainage(r).sample(x,z).height());level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+v.x()+" "+v.y()+" "+v.z()+" "+v.yaw()+" "+v.pitch());Blockbranch.LOGGER.info("STONY_SHORE_CAPTURE_CAMERA stage={} bed={} view={}",stage,bed,v);
  }else if(stage==93)level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);else{var base=((SparseColumn)level.getChunkAt(edit)).blockbranch$terrain().base();level.setBlock(edit,base.state(edit.getX(),edit.getY(),edit.getZ()),2);}
 }
 private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 private StonyShoreSmoke(){}
}
