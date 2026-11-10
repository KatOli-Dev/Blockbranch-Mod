package blockbranch.realistic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;

/** Disposable native/client witnesses;never existing user saves. */
public final class MangroveSmoke {
 private static final Map<DrainageNetwork,BlockPos[]> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
 public static BlockPos fixture(RealisticGenerator g,RandomState r){var d=g.drainage(r);int index=Boolean.getBoolean("blockbranch.mangroveSecondSite")?1:0;var values=FIXTURES.computeIfAbsent(d,k->new BlockPos[2]);if(values[index]==null){var p=blockbranch.realistic.MangroveSwampFixtures.fixture(d,r.seed(),index==1);var c=new ClimateField(r.seed());var stem=MangroveTrees.roots(r.seed(),p.x(),p.z(),p.x(),p.z(),d,c).stream().filter(s->s.x()==p.x()&&s.z()==p.z()).findFirst().orElseThrow();values[index]=new BlockPos(stem.x(),stem.y()-1,stem.z());}return values[index];}
 public static void verify(net.minecraft.server.level.ServerLevel level,RealisticGenerator g,boolean reload){
  var random=level.getChunkSource().randomState();var d=g.drainage(random);var bed=fixture(g,random);var cut=bed.above();
  require(level.getBiome(bed).is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP),"Actual vanilla Mangrove Swamp identity");
  if(reload)require(level.getBlockState(cut).isAir(),"Saved mangrove stem cut survives reopen");
  var original=((blockbranch.cubic.SparseColumn)level.getChunkAt(cut)).blockbranch$terrain().base().state(cut.getX(),cut.getY(),cut.getZ());require(original.is(net.minecraft.world.level.block.Blocks.MANGROVE_WOOD),"Natural supported mangrove fixture stem");level.setBlock(cut,original,2);
  int leaves=0,roots=0,wet=0,props=0;BlockPos propagule=null,wetRoot=null;
  for(int cz=(bed.getZ()>>4)-2;cz<=(bed.getZ()>>4)+2;cz++)for(int cx=(bed.getX()>>4)-2;cx<=(bed.getX()>>4)+2;cx++){
   var chunk=level.getChunk(cx,cz);var base=((blockbranch.cubic.SparseColumn)chunk).blockbranch$terrain().base();var snapshot=base.encode(level.palettedContainerFactory());var copy=blockbranch.cubic.TerrainProfile.decode(snapshot,level.palettedContainerFactory());var material=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
   require(Arrays.equals(base.canopy().encode(),copy.canopy().encode()),"Root/water/propagule profile roundtrip");
   for(int packed:base.canopy().encode()){
    int key=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;var pos=new BlockPos(x,y,z);var state=base.state(x,y,z);var sample=d.sample(x,z);
    require(chunk.getBlockState(pos)==state&&copy.state(x,y,z)==state&&material.computeIfAbsent(y>>4,copy::generate).getBlockState(x&15,y&15,z&15)==state,"Native/saved/materialized mangrove content");
    if(state.is(net.minecraft.world.level.block.Blocks.MANGROVE_ROOTS)){roots++;boolean waterlogged=state.getValue(net.minecraft.world.level.block.MangroveRootsBlock.WATERLOGGED);require(waterlogged==(sample.waterLevel()!=DrainageNetwork.DRY&&y<sample.waterLevel()),"Exact existing source-water root state");if(waterlogged){wet++;wetRoot=pos;require(level.getFluidState(pos).isSource()&&level.getFluidState(pos).is(net.minecraft.world.level.material.Fluids.WATER),"Waterlogged root retains native source fluid");}}
    if(state.is(net.minecraft.world.level.block.Blocks.MANGROVE_LEAVES)){leaves++;int distance=state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE);boolean supported=false;for(var direction:net.minecraft.core.Direction.values()){var near=level.getBlockState(pos.relative(direction));if(near.is(net.minecraft.tags.BlockTags.LOGS)&&distance==1||near.is(net.minecraft.tags.BlockTags.LEAVES)&&near.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE)<distance)supported=true;}require(supported&&!state.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT),"Native live leaf distance across chunks");}
    if(state.is(net.minecraft.world.level.block.Blocks.MANGROVE_PROPAGULE)){props++;propagule=pos;require(state.getValue(net.minecraft.world.level.block.MangrovePropaguleBlock.HANGING)&&state.canSurvive(level,pos)&&base.randomTickSections().contains(y>>4),"Native hanging/ticking propagule support");}
   }
   for(int z=0;z<16;z+=4)for(int x=0;x<16;x+=4){int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height();for(int y=h-2;y<=Math.max(h+2,base.canopy().topY(wx,wz));y++)require(base.state(wx,y,wz)==g.surfaceState(wx,y,wz,random),"Analytic/profile root,fluid,bed and ground agreement");for(var type:net.minecraft.world.level.levelgen.Heightmap.Types.values())require(chunk.getHeight(type,x,z)+1==g.getBaseHeight(wx,wz,type,level,random),"Native mangrove heightmap "+type);}
   int[] mutable=snapshot.getIntArray("canopy").orElseThrow();if(mutable.length>0){int old=base.canopy().encode()[0];mutable[0]=0;require(base.canopy().encode()[0]==old,"Snapshot array isolation");}
  }
  require(leaves>500&&roots>50&&wet>0&&props>0,"Actual mangrove canopy,stilt roots,water roots and propagules");
  var target=propagule;var support=target.above();var supportState=level.getBlockState(support);var propState=level.getBlockState(target);level.setBlock(support,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);require(!level.getBlockState(target).is(net.minecraft.world.level.block.Blocks.MANGROVE_PROPAGULE),"Unsupported hanging propagule breaks natively");level.setBlock(support,supportState,3);level.setBlock(target,propState,3);
  var rootState=level.getBlockState(wetRoot);level.setBlock(wetRoot,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),3);require(level.getFluidState(wetRoot).isSource(),"Root removal leaves original source-water cell restorable");level.setBlock(wetRoot,rootState,3);
  var profile=((blockbranch.cubic.SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();var invalid=profile.encode(level.palettedContainerFactory());invalid.putInt("version",26);boolean rejected=false;try{blockbranch.cubic.TerrainProfile.decode(invalid,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Previous dry-only canopy profile rejected");
  level.setBlock(cut,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);var terrain=((blockbranch.cubic.SparseColumn)level.getChunkAt(cut)).blockbranch$terrain();level.getChunkAt(cut).markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(cut).isAir(),"Mangrove edit survives lazy eviction");
  blockbranch.Blockbranch.LOGGER.info("MANGROVE_NATIVE_PASS {} bed={} leaves={} roots={} wetRoots={} propagules={}",reload?"RELOAD":"NEW",bed,leaves,roots,wet,props);
 }
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 private MangroveSmoke(){}
}
