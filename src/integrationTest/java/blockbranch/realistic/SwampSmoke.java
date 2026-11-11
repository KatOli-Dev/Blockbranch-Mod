package blockbranch.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;

/** Real registry/water/support/profile/save assertions for the wooded freshwater bank. */
public final class SwampSmoke {
 private static final Map<DrainageNetwork,Swamp.Point[]> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
 public static BlockPos fixture(RealisticGenerator g,RandomState r){var d=g.drainage(r);int index=Boolean.getBoolean("blockbranch.swampSecondSite")?1:0;var points=FIXTURES.computeIfAbsent(d,k->new Swamp.Point[2]);if(points[index]==null)points[index]=index==0?blockbranch.realistic.SwampFixtures.fixture(d,r.seed()):blockbranch.realistic.SwampFixtures.secondFixture(d,r.seed());var p=points[index];return new BlockPos(p.x(),d.sample(p.x(),p.z()).height(),p.z());}
 public static void verify(ServerLevel level,RealisticGenerator g,BlockPos bed,boolean reload){
  var random=level.getChunkSource().randomState();var d=g.drainage(random);var bank=Swamp.bank(bed.getX(),bed.getZ(),d);require(bank!=null,"Confirmed low freshwater bank");require(level.getBiome(bed).is(net.minecraft.world.level.biome.Biomes.SWAMP),"Actual vanilla Swamp identity");
  require(level.getBlockState(bed.above()).is(Blocks.OAK_LOG)&&level.getBlockState(bed).is(Blocks.ROOTED_DIRT)&&level.getBlockState(bed).isFaceSturdy(level,bed,Direction.UP),"Grounded swamp oak stem on a full solid footing");var cut=bed.above(2);if(reload)require(level.getBlockState(cut).isAir(),"Saved trunk cut survives reload");var root=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();level.setBlock(cut,root.state(cut.getX(),cut.getY(),cut.getZ()),2);
  int leaves=0,vines=0,lilies=0,flowers=0,dead=0;var floor=new HashSet<Integer>();
  for(int cz=(bed.getZ()>>4)-2;cz<=(bed.getZ()>>4)+2;cz++)for(int cx=(bed.getX()>>4)-2;cx<=(bed.getX()>>4)+2;cx++){
   var chunk=level.getChunk(cx,cz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var snapshot=base.encode(level.palettedContainerFactory());var copy=TerrainProfile.decode(snapshot,level.palettedContainerFactory());require(Arrays.equals(base.canopy().encode(),copy.canopy().encode()),"Water-compatible overlay serialization");var material=new HashMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection>();
   for(int packed:base.canopy().encode()){
    int key=packed>>>CanopyLayer.STATE_BITS,x=cx*16+(key&15),z=cz*16+((key>>>4)&15),y=(key>>>8)+TerrainShape.FLOOR;var pos=new BlockPos(x,y,z);var state=base.state(x,y,z);var s=d.sample(x,z);
    require(chunk.getBlockState(pos)==state&&copy.state(x,y,z)==state&&material.computeIfAbsent(y>>4,copy::generate).getBlockState(x&15,y&15,z&15)==state,"Native/saved/client materialized wetland overlay");require(y>s.height()&&(s.waterLevel()==DrainageNetwork.DRY||y>=s.waterLevel()),"No submerged trunks or leaves");
    if(state.is(Blocks.OAK_LEAVES)||state.is(Blocks.BIRCH_LEAVES)){leaves++;int distance=state.getValue(LeavesBlock.DISTANCE);boolean support=false;for(var direction:Direction.values()){var near=level.getBlockState(pos.relative(direction));if(near.is(BlockTags.LOGS)&&distance==1||near.is(BlockTags.LEAVES)&&near.getValue(LeavesBlock.DISTANCE)<distance)support=true;}require(support&&!state.getValue(LeavesBlock.PERSISTENT),"Live leaf support across wet/dry chunks");}
    if(state.is(Blocks.VINE)){vines++;require(state.canSurvive(level,pos)&&base.randomTickSections().contains(y>>4),"Native supported/ticking vine chain");}
    if(state.is(Blocks.LILY_PAD)){lilies++;require(state.canSurvive(level,pos)&&y==s.waterLevel()&&!level.getFluidState(pos.below()).isEmpty(),"Native lily rests on retained water");}
    if(state.is(Blocks.OAK_LOG)&&y==s.height()+1&&state.getValue(RotatedPillarBlock.AXIS)!=Direction.Axis.Y){dead++;require(s.waterLevel()==DrainageNetwork.DRY&&d.channelAt(x,z).edge()<0&&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP),"Fallen log is fully grounded and clear of channels");}
   }
   for(int z=0;z<16;z++)for(int x=0;x<16;x++){int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height();var p=new BlockPos(wx,h+1,wz);var state=level.getBlockState(p);if(state.is(Blocks.FERN)||state.is(Blocks.SHORT_GRASS)||state.is(Blocks.LEAF_LITTER)||state.is(Blocks.BLUE_ORCHID)||state.is(Blocks.BROWN_MUSHROOM))require(state.canSurvive(level,p),"Actual wetland ground plant support "+state+" on "+level.getBlockState(p.below())+" at "+p);}
   for(int z=0;z<16;z+=4)for(int x=0;x<16;x+=4){int wx=cx*16+x,wz=cz*16+z,h=d.sample(wx,wz).height();floor.add(LeafLitter.woodlandBase(g.woodlandCover(wx,wz,random)));
    for(int y=h-3;y<=Math.max(d.sample(wx,wz).waterLevel()==DrainageNetwork.DRY?h+2:d.sample(wx,wz).waterLevel()+2,base.canopy().topY(wx,wz));y++)require(base.state(wx,y,wz)==g.surfaceState(wx,y,wz,random),"Generator/profile floor,fluid and overhang queries agree");
    var plant=new BlockPos(wx,h+1,wz);if(level.getBlockState(plant).is(Blocks.BLUE_ORCHID)){flowers++;require(level.getBlockState(plant).canSurvive(level,plant),"Native marsh flower support");}
    for(var type:Heightmap.Types.values())require(chunk.getHeight(type,wx&15,wz&15)+1==g.getBaseHeight(wx,wz,type,level,random),"Water/foliage-aware native heightmap "+type);
    if(x==0&&z==0)for(int start:new int[]{h-2,h+3,d.sample(wx,wz).waterLevel()==DrainageNetwork.DRY?h+7:d.sample(wx,wz).waterLevel()-1}){
        var bounds=net.minecraft.world.level.LevelHeightAccessor.create(start,4);var column=g.getBaseColumn(wx,wz,bounds,random);
        for(var type:Heightmap.Types.values()){int expected=bounds.getMinY();for(int yy=bounds.getMaxY();yy>=bounds.getMinY();yy--)if(type.isOpaque().test(column.getBlock(yy))){expected=yy+1;break;}require(g.getBaseHeight(wx,wz,type,bounds,random)==expected,"Clipped wetland fluid/vine/leaf height query "+type);}
    }
   }
   int[] mutable=snapshot.getIntArray("canopy").orElseThrow();if(mutable.length>0){int original=base.canopy().encode()[0];mutable[0]=0;require(base.canopy().encode()[0]==original,"Serialized array snapshot isolated from profile");}
  }
  require(leaves>500&&vines>10&&lilies>0,"Actual oak/vine/lily stand");require(floor.stream().anyMatch(v->v>=13&&v<=29),"Distinct muddy/moss/rooted floor");
  var invalid=root.encode(level.palettedContainerFactory());invalid.putInt("version",24);boolean rejected=false;try{TerrainProfile.decode(invalid,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Old dry-only profile version rejected");
  // A non-waterlogged log remains invalid below the water datum;allowing overhangs is not allowing flooded stems.
  var wet=findLily(level,g,bed);var wetBase=((SparseColumn)level.getChunkAt(wet)).blockbranch$terrain().base();invalid=wetBase.encode(level.palettedContainerFactory());int key=((wet.getY()-1-TerrainShape.FLOOR)<<8)|((wet.getZ()&15)*16+(wet.getX()&15));invalid.putIntArray("canopy",new int[]{(key<<CanopyLayer.STATE_BITS)|1});rejected=false;try{TerrainProfile.decode(invalid,level.palettedContainerFactory());}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Submerged non-waterlogged wood rejected");
  level.setBlock(cut,Blocks.AIR.defaultBlockState(),2);var terrain=((SparseColumn)level.getChunkAt(cut)).blockbranch$terrain();level.getChunkAt(cut).markUnsaved();terrain.flush();terrain.evictOutside(-100,-90);require(level.getBlockState(cut).isAir(),"Saved cut survives lazy eviction");
  Blockbranch.LOGGER.info("SWAMP_NATIVE_PASS {} bed={} leaves={} vines={} lilies={} flowers={} nearbyDeadwood={} floor={}",reload?"RELOAD":"NEW",bed,leaves,vines,lilies,flowers,dead,floor);
 }
 static BlockPos findLily(ServerLevel level,RealisticGenerator g,BlockPos bed){
  for(int dz=-64;dz<=64;dz++)for(int dx=-64;dx<=64;dx++){int x=bed.getX()+dx,z=bed.getZ()+dz,w=g.drainage(level.getChunkSource().randomState()).sample(x,z).waterLevel();if(w==DrainageNetwork.DRY)continue;var p=new BlockPos(x,w,z);if(level.getBlockState(p).is(Blocks.LILY_PAD))return p;}
  throw new AssertionError("No native lily support witness");
 }
 public static void activeSetup(ServerLevel level,RealisticGenerator g,BlockPos bed){
  var anchor=bed.above(35);var vine=anchor.east();var state=Blocks.VINE.defaultBlockState().setValue(VineBlock.WEST,true);level.setBlock(anchor,Blocks.OAK_LOG.defaultBlockState(),3);level.setBlock(vine,state,3);level.setBlock(vine.below(),state,3);require(state.canSurvive(level,vine)&&state.canSurvive(level,vine.below()),"Active native vine support");level.setBlock(anchor,Blocks.AIR.defaultBlockState(),3);require(level.getBlockState(vine).isAir()&&level.getBlockState(vine.below()).isAir(),"Unsupported vine chain removed");
  var lily=findLily(level,g,bed);level.setBlock(lily.below(),Blocks.AIR.defaultBlockState(),3);require(!level.getBlockState(lily).is(Blocks.LILY_PAD),"Lily loses support after water removal");restore(level,lily.below());restore(level,lily);Blockbranch.LOGGER.info("SWAMP_ACTIVE_VINE_LILY_SUPPORT_PASS bed={}",bed);
 }
 public static void activeRestore(ServerLevel level,BlockPos bed){for(var p:List.of(bed.above(35),bed.above(35).east(),bed.above(35).east().below()))restore(level,p);}
 private static void restore(ServerLevel level,BlockPos p){var b=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain().base();level.setBlock(p,b.state(p.getX(),p.getY(),p.getZ()),3);}
 private static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 private SwampSmoke(){}
}
