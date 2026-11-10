package blockbranch.realistic;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Actual evergreen palette,live support,ground behavior,snapshots and persisted edits. */
public final class TaigaSmoke {
 private static final Map<DrainageNetwork,BlockPos> FIXTURE=Collections.synchronizedMap(new WeakHashMap<>());
 public static BlockPos fixture(RealisticGenerator g,RandomState r){return FIXTURE.computeIfAbsent(g.drainage(r),d->{var p=blockbranch.realistic.TaigaFixtures.fixture(d,r.seed());return new BlockPos(p.x(),d.sample(p.x(),p.z()).height(),p.z());});}
 public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){verifyAt(level,g,reload,fixture(g,level.getChunkSource().randomState()),net.minecraft.world.level.biome.Biomes.TAIGA,"TAIGA");}
 static void verifyAt(ServerLevel level,RealisticGenerator g,boolean reload,BlockPos bed,net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biome,String label){var random=level.getChunkSource().randomState();var d=g.drainage(random);var ecology=new GroundEcology.Field(d::sample,new ClimateField(random.seed()));var cut=bed.above(2);require(level.getBiome(bed).is(biome),"Actual vanilla Taiga identity");if(reload)require(level.getBlockState(cut).isAir(),"Saved evergreen cut survives reopen");var home=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();require(home.state(bed.getX(),bed.getY()+1,bed.getZ()).is(Blocks.SPRUCE_WOOD),"Natural grounded evergreen stem");level.setBlock(cut,home.state(cut.getX(),cut.getY(),cut.getZ()),2);int logs=0,leaves=0,plants=0;
  for(int cz=(bed.getZ()>>4)-2;cz<=(bed.getZ()>>4)+2;cz++)for(int cx=(bed.getX()>>4)-2;cx<=(bed.getX()>>4)+2;cx++){
   var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var tag=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(tag,level.palettedContainerFactory());require(Arrays.equals(base.canopy().encode(),copy.canopy().encode()),"Packed evergreen profile roundtrip");var sections=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
   for(int packed:base.canopy().encode()){int key=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;var pos=new BlockPos(x,y,z);var state=base.state(x,y,z);require(chunk.getBlockState(pos)==state&&copy.state(x,y,z)==state&&sections.computeIfAbsent(y>>4,copy::generate).getBlockState(x&15,y&15,z&15)==state,"Native/saved/client evergreen materialization");require(d.sample(x,z).waterLevel()==DrainageNetwork.DRY&&y>d.sample(x,z).height(),"Dry supported forest geometry");
    if(state.is(Blocks.SPRUCE_WOOD))logs++;
    if(state.is(Blocks.SPRUCE_LEAVES)){leaves++;int distance=state.getValue(LeavesBlock.DISTANCE);boolean support=false;for(var direction:Direction.values()){var near=level.getBlockState(pos.relative(direction));if(near.is(net.minecraft.tags.BlockTags.LOGS)&&distance==1||near.is(net.minecraft.tags.BlockTags.LEAVES)&&near.getValue(LeavesBlock.DISTANCE)<distance)support=true;}require(support&&!state.getValue(LeavesBlock.PERSISTENT),"Real cross-chunk evergreen leaf support");}
   }
   for(int z=0;z<16;z++)for(int x=0;x<16;x++){int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height();var p=new BlockPos(wx,h+1,wz);var state=level.getBlockState(p);var soil=level.getBlockState(p.below());if(soil.is(Blocks.MOSS_BLOCK)||state.is(Blocks.MOSS_CARPET))require(GroundEcology.borealMossSite(ecology.sample(wx,wz).shade(base.canopy().topY(wx,wz)>h+2)),"Native moss requires moist or sheltered boreal habitat");if(state.is(Blocks.FERN)||state.is(Blocks.SHORT_GRASS)||state.is(Blocks.MOSS_CARPET)||state.is(Blocks.SWEET_BERRY_BUSH)){plants++;require(state.canSurvive(level,p),"Native suitable ground support");}if(x%4!=0||z%4!=0)continue;for(int y=h-3;y<=Math.max(h+2,base.canopy().topY(wx,wz));y++)require(base.state(wx,y,wz)==g.surfaceState(wx,y,wz,random),"Analytic/profile evergreen floor and canopy agreement");for(var type:Heightmap.Types.values())require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,random),"Native evergreen heightmap "+type);}
   var a=tag.getIntArray("canopy").orElseThrow();if(a.length>0){int old=base.canopy().encode()[0];a[0]=0;require(base.canopy().encode()[0]==old,"Immutable saved canopy snapshot");}
  }
  require(logs>100&&leaves>1000&&plants>10,"Characteristic living evergreen stand and sparse undergrowth");level.setBlock(cut,Blocks.AIR.defaultBlockState(),2);var chunk=level.getChunkAt(cut);var terrain=((SparseColumn)chunk).blockbranch$terrain();chunk.markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(cut).isAir(),"Persistent evergreen edit eviction");blockbranch.Blockbranch.LOGGER.info("{}_NATIVE_PASS {} bed={} logs={} leaves={} plants={}",label,reload?"RELOAD":"NEW",bed,logs,leaves,plants);
 }
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 private TaigaSmoke(){}
}
