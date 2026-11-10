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

/** Native habitat, complete eight-bit profile/query contracts, biological support and saved edits. */
public final class WarmDappledSmoke {
 private static final Map<DrainageNetwork,BlockPos[]> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
 public static BlockPos fixture(RealisticGenerator g,RandomState r,boolean forest){var d=g.drainage(r);synchronized(FIXTURES){var a=FIXTURES.computeIfAbsent(d,k->new BlockPos[2]);int i=forest?1:0;if(a[i]!=null)return a[i];int x,z;if(forest){var p=blockbranch.realistic.DappledForestFixtures.fixture(d,r.seed());x=p.x();z=p.z();}else{var p=blockbranch.realistic.WarmOceanFixtures.fixture(d,r.seed());x=p.x();z=p.z();}return a[i]=new BlockPos(x,d.sample(x,z).height(),z);}}
 public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){var r=level.getChunkSource().randomState();var d=g.drainage(r);var c=new ClimateField(r.seed());var source=g.getBiomeSource().createUncachedResolver(r);
  for(boolean forest:new boolean[]{false,true}){var bed=fixture(g,r,forest);var identity=forest?Biomes.DAPPLED_FOREST:Biomes.WARM_OCEAN;var home=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();var edit=bed.below();
   require(level.getBiome(bed).is(identity),"Actual stored new vanilla biome");if(reload)require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Durable native edit reload");level.setBlock(edit,home.state(edit.getX(),edit.getY(),edit.getZ()),2);
   int population=0,plants=0,leaves=0;var states=new HashSet<String>();
   for(int cz=(bed.getZ()>>4)-1;cz<=(bed.getZ()>>4)+1;cz++)for(int cx=(bed.getX()>>4)-1;cx<=(bed.getX()>>4)+1;cx++){
    var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var factory=level.palettedContainerFactory();var tag=base.encode(factory);var copy=TerrainProfile.decode(tag,factory);var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
    for(int v:base.canopy().encode()){int key=v>>>CanopyLayer.STATE_BITS,code=v&CanopyLayer.STATE_MASK,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;var p=new BlockPos(x,y,z);var actual=level.getBlockState(p);
     require(actual==base.state(x,y,z)&&actual==copy.state(x,y,z)&&actual==g.surfaceState(x,y,z,r)&&actual==sections.computeIfAbsent(y>>4,copy::generate).getBlockState(x&15,y&15,z&15),"Native/profile/decoded/analytic/materialized eight-bit population agreement");
     if(code<128)continue;population++;states.add(actual.toString());require(actual.canSurvive(level,p),"Actual native biological support at "+p+" "+actual);
     if(actual.is(BlockTags.LEAVES)){leaves++;require(actual.getValue(LeavesBlock.DISTANCE)<=6&&!actual.getValue(LeavesBlock.PERSISTENT),"Actual nonpersistent supported poplar leaves");}
     if(forest){require(code<150&&d.sample(x,z).waterLevel()==DrainageNetwork.DRY,"Dry poplar community");if(code==149)plants++;}
     else{require(WarmOcean.aquatic(code)&&WarmOcean.surfaceContains(r.seed(),x,z,d,c)&&y<d.sample(x,z).waterLevel(),"Existing marine source-water eligibility");if(code>=155&&code<=163){plants++;require(actual.getFluidState().isSource(),"Native waterlogged fans/pickles retain source fluid");}else if(code<=154){boolean wet=false;for(var side:Direction.values())wet|=!level.getFluidState(p.relative(side)).isEmpty();require(wet,"Living coral touches actual source water");}}
    }
    for(int z=0;z<16;z++)for(int x=0;x<16;x++){int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height();for(int y=h-4;y<=h+3;y++)require(level.getBlockState(new BlockPos(wx,y,wz))==copy.state(wx,y,wz)&&copy.state(wx,y,wz)==g.surfaceState(wx,y,wz,r),"Physical surface/query consistency");if(x%4!=0||z%4!=0)continue;
     for(int y:new int[]{-3000,0,2000})require(chunk.getNoiseBiome(wx>>2,y,wz>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2))&&copy.biomes().get(x>>2,y&3,z>>2).equals(source.getNoiseBiome(wx>>2,y,wz>>2)),"Stored/decoded/resolved vanilla identities");
     for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,r),"Actual structure-aware heightmap "+type);
    }
   }
   require(population>50&&plants>0&&(!forest||leaves>100),"Meaningful actual native populations");
   var bounded=g.getBaseColumn(bed.getX(),bed.getZ(),net.minecraft.world.level.LevelHeightAccessor.create(bed.getY()-8,64),r);for(int y=bed.getY()-8;y<bed.getY()+40;y++)require(bounded.getBlock(y)==g.surfaceState(bed.getX(),y,bed.getZ(),r),"Bounded base-column structure query");
   var invalid=home.encode(level.palettedContainerFactory());invalid.putIntArray("canopy",new int[]{((((bed.getY()+1-TerrainShape.FLOOR)<<8)|((bed.getZ()&15)*16+(bed.getX()&15)))<<CanopyLayer.STATE_BITS)|169});boolean rejected=false;try{TerrainProfile.decode(invalid,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Unknown eight-bit palette entry rejected");
   level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);var chunk=level.getChunkAt(edit);chunk.markUnsaved();var terrain=((SparseColumn)chunk).blockbranch$terrain();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(edit).is(Blocks.COBBLESTONE),"Edit survives sparse eviction");
   Blockbranch.LOGGER.info("WARM_DAPPLED_NATIVE_PASS mode={} forest={} bed={} population={} plants={} leaves={} distinctStates={}",reload?"RELOAD":"NEW",forest,bed,population,plants,leaves,states.size());
  }
 }
 public static void probe(ServerPlayer player,int stage){var level=player.level();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var r=level.getChunkSource().randomState();boolean forest=Boolean.getBoolean("blockbranch.dappledForestClient");var bed=fixture(g,r,forest);var edit=bed.below();
  if(stage<=92){level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set 6000");
   var v=stage==90?new MountainPeaks.View(bed.getX()+.5,bed.getY()+40,bed.getZ()+.5,0,30):WarmDappledScenes.view(bed.getX(),bed.getY(),bed.getZ(),forest,stage==92,(x,z)->g.drainage(r).sample(x,z).height());level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+v.x()+" "+v.y()+" "+v.z()+" "+v.yaw()+" "+v.pitch());Blockbranch.LOGGER.info("WARM_DAPPLED_CAPTURE_CAMERA forest={} stage={} bed={} view={}",forest,stage,bed,v);
  }else if(stage==93)level.setBlock(edit,Blocks.COBBLESTONE.defaultBlockState(),2);else{var base=((SparseColumn)level.getChunkAt(edit)).blockbranch$terrain().base();level.setBlock(edit,base.state(edit.getX(),edit.getY(),edit.getZ()),2);}
 }
 private static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 private WarmDappledSmoke(){}
}
