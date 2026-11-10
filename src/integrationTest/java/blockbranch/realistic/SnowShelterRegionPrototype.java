package blockbranch.realistic;

import java.util.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import com.mojang.serialization.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/** Definition1: one possible native igloo per complete coherent snowy habitat region.
 * The finite broad atlas is cheap; detailed candidates resolve only on demand. */
public final class SnowShelterRegionPrototype {
 public static final Codec<SnowShelterRegionPrototype> CODEC=Codec.intRange(1,1).fieldOf("definition").xmap(i->new SnowShelterRegionPrototype(),s->1).codec();
 public record Plan(int x,int y,int z,int rotation,int variant,int region){public BlockPos center(){return new BlockPos(x,y,z);}public Rotation turn(){return Rotation.values()[rotation];}}
 private long seed;private RealisticGenerator.HutPlanningTerrain terrain;private List<Region> regions=List.of();private final Long2ObjectOpenHashMap<Region> starts=new Long2ObjectOpenHashMap<>();
 private SnowShelterRegionPrototype(){}
 static SnowShelterRegionPrototype bind(long seed,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState random){
  if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();var out=new SnowShelterRegionPrototype();out.seed=seed;out.terrain=g.hutPlanningTerrain(random);var d=out.terrain.drainage();var c=new SnowShelterClimatePrototype(seed);
  var atlas=StructureRegionAtlas.labeled(seed,(x,z)->c.structureSnowHabitat(x,z,d));var owners=atlas.substantialOwnerIndices();var candidates=atlas.candidateIndices(owners,4);var found=new ArrayList<Region>();
  for(int row=0;row<owners.length;row++){
   var points=new ArrayList<BlockPos>();for(int k=0;k<4;k++){int cell=candidates[row*4+k];if(cell<0)break;int cx=(cell%StructureRegionAtlas.SIDE+StructureRegionAtlas.MIN_CELL)*StructureRegionAtlas.GRID+1024,cz=(cell/StructureRegionAtlas.SIDE+StructureRegionAtlas.MIN_CELL)*StructureRegionAtlas.GRID+1024;
    for(int attempt=0;attempt<64;attempt++){long h=OakBirchWoodland.mix(seed^0x534E4F5753495445L^((long)cell<<8)^attempt);int x=Math.floorDiv(cx+(int)Math.floorMod(h,1536)-768,16)*16+8,z=Math.floorDiv(cz+(int)Math.floorMod(h>>>24,1536)-768,16)*16+8;points.add(new BlockPos(x,0,z));}
   }var region=out.new Region(owners[row],List.copyOf(points));found.add(region);for(var p:points){var before=out.starts.put(new ChunkPos(p.getX()>>4,p.getZ()>>4).pack(),region);if(before!=null&&before!=region)throw new IllegalStateException("Distinct snow region candidate overlap");}
  }out.regions=List.copyOf(found);if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();return out;
 }
 public Optional<Plan> forStart(int cx,int cz){var region=starts.get(new ChunkPos(cx,cz).pack());if(region==null)return Optional.empty();var value=region.plan();return value.filter(p->(p.x>>4)==cx&&(p.z>>4)==cz);}
 public StructurePlacement placement(){return new StructurePlacement(){public boolean isStructureChunk(net.minecraft.world.level.chunk.ChunkGeneratorStructureState state,int x,int z){return forStart(x,z).isPresent();}public MapCodec<? extends StructurePlacement> codec(){return MapCodec.unit(this);}public BlockPos getLocatePos(ChunkPos p){return forStart(p.x(),p.z()).map(Plan::center).orElse(new BlockPos(p.getMinBlockX()+8,64,p.getMinBlockZ()+8));}};}
 public List<Region> ordered(BlockPos origin,double range){var out=new ArrayList<>(regions);out.removeIf(r->r.lower(origin)>range);out.sort(Comparator.comparingDouble((Region r)->r.lower(origin)).thenComparingInt(r->r.owner));return out;}
 List<Plan> plans(){var out=new ArrayList<Plan>();for(var r:regions)r.plan().ifPresent(out::add);return List.copyOf(out);}int regionCount(){return regions.size();}int candidateCount(){return starts.size();}
 public final class Region {
  private final int owner;private final List<BlockPos> points;private volatile Optional<Plan> result;private final int[] audit=new int[7];
  Region(int owner,List<BlockPos> points){this.owner=owner;this.points=points;}
  public int owner(){return owner;}public int[] audit(){return audit.clone();}
  public double lower(BlockPos p){double best=Double.POSITIVE_INFINITY;for(var q:points)best=Math.min(best,Math.hypot(p.getX()-(double)q.getX(),p.getZ()-(double)q.getZ()));return Math.max(0,Math.nextDown(best));}
  public synchronized Optional<Plan> plan(){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();if(result!=null)return result;var value=build(this);if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();return result=value;}
 }
 private boolean habitat(int x,int z){return SnowyPlains.contains(x,z,terrain.drainage(),terrain.climate())||SnowyTaiga.contains(seed,x,z,terrain.drainage(),terrain.climate());}
 private Optional<Plan> build(Region region){var d=terrain.drainage();long hash=OakBirchWoodland.mix(seed^0x534E4F57524F4F46L^region.owner);
  for(var point:region.points){
   if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException();int x=point.getX(),z=point.getZ();region.audit[0]++;if(!habitat(x,z))continue;region.audit[1]++;
   int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;boolean good=true;
   outer:for(int dz=-7;dz<=7;dz++)for(int dx=-7;dx<=7;dx++){var s=d.sample(x+dx,z+dz);min=Math.min(min,s.height());max=Math.max(max,s.height());if(s.waterLevel()!=DrainageNetwork.DRY||max-min>1){good=false;break outer;}}
   if(!good||max<64||max>TerrainShape.CEILING-12)continue;region.audit[2]++;
   for(int dz=-4;dz<=4;dz+=4)for(int dx=-4;dx<=4;dx+=4)if(!habitat(x+dx,z+dz))good=false;if(!good)continue;region.audit[3]++;
   var canopy=terrain.canopy().at(x,z);outer:for(int dz=-7;dz<=7;dz++)for(int dx=-7;dx<=7;dx++)if(canopy.topY(x+dx,z+dz)>d.sample(x+dx,z+dz).height()+1){good=false;break outer;}if(!good)continue;region.audit[4]++;
   return Optional.of(new Plan(x,max,z,(int)((hash>>>16)&3),(int)((hash>>>24)&1),region.owner));
  }return Optional.empty();
 }
}
