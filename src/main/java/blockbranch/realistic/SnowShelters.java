package blockbranch.realistic;

import java.util.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/** Definition1: at most one shelter per complete connected snowy regional habitat.
 * A complete immutable ownership index persists. Site decisions are disposable, atomic and bounded. */
public final class SnowShelters {
 private record Cache(long seed,int[] cells,String checksum){static final Codec<Cache> CODEC=RecordCodecBuilder.create(i->i.group(Codec.LONG.fieldOf("seed").forGetter(Cache::seed),Codec.INT_STREAM.xmap(v->v.limit(65537).toArray(),Arrays::stream).fieldOf("cells").forGetter(Cache::cells),Codec.STRING.fieldOf("checksum").forGetter(Cache::checksum)).apply(i,Cache::new));}
 public static final Codec<SnowShelters> CODEC=RecordCodecBuilder.create(i->i.group(Codec.intRange(1,1).fieldOf("definition").forGetter(v->1),Cache.CODEC.lenientOptionalFieldOf("region_cache").forGetter(v->v.cache.filter(c->c.cells.length<=65536))).apply(i,(definition,cache)->new SnowShelters(cache)));
 private Optional<Cache> cache;
 private static final int[][] CORNERS={{0,2},{6,2},{0,7},{6,7},{3,0}},BIOMES={{3,5},{1,3},{5,3},{1,6},{5,6},{3,0}};
 public record Plan(int x,int y,int z,int rotation,int variant,int region){public BlockPos center(){return new BlockPos(x,y,z);}public Rotation turn(){return Rotation.values()[rotation];}public BlockPos origin(){return new BlockPos(x-3,y,z-5);}public BlockPos local(int lx,int ly,int lz){int dx=lx-3,dz=lz-5;return switch(rotation){case 1->new BlockPos(x-dz,y+ly,z+dx);case 2->new BlockPos(x-dx,y+ly,z-dz);case 3->new BlockPos(x+dz,y+ly,z-dx);default->new BlockPos(x+dx,y+ly,z+dz);};}}

 private long seed;private RealisticGenerator.HutPlanningTerrain terrain;private BiomeResolver surface;private List<Region> regions=List.of();private final Int2ObjectOpenHashMap<Region> cells=new Int2ObjectOpenHashMap<>();
 public SnowShelters(){this(Optional.empty());}private SnowShelters(Optional<Cache> cache){this.cache=cache;}
 public boolean canSave(){return cache.isPresent()&&cache.get().cells.length<=65536;}
 public boolean valid(long seed){if(cache.isEmpty())return false;var c=cache.get();if(c.seed!=seed||c.cells.length>65536||c.cells.length%4!=0||!hash(seed,c.cells).equals(c.checksum))return false;var seen=new it.unimi.dsi.fastutil.ints.IntOpenHashSet();for(int i=0;i<c.cells.length;i++){int v=c.cells[i];if(v<0||v>=4194304||!seen.add(v)||i%4==0&&i>0&&v<=c.cells[i-4])return false;}return true;}
 SnowShelters bindSaved(long seed,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState random){return bind(seed,g,random,valid(seed)?cache:Optional.empty());}
 static SnowShelters bind(long seed,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState random){
  return bind(seed,g,random,Optional.empty());}
 private static SnowShelters bind(long seed,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState random,Optional<Cache> saved){
  check();var out=new SnowShelters(saved);out.seed=seed;out.terrain=g.hutPlanningTerrain(random);out.surface=((ScreeBiomeSource)g.getBiomeSource()).surfaceResolver();var d=out.terrain.drainage();var climate=new SnowShelterClimate(seed);
  int[] candidates;if(saved.isPresent())candidates=saved.get().cells;else{var atlas=StructureRegionAtlas.labeled(seed,(x,z)->climate.structureSnowHabitat(x,z,d));candidates=atlas.candidateIndices(atlas.substantialOwnerIndices(),4);out.cache=Optional.of(new Cache(seed,candidates,hash(seed,candidates)));}var found=new ArrayList<Region>();
  for(int row=0;row<candidates.length/4;row++){check();var selected=Arrays.copyOfRange(candidates,row*4,row*4+4);var region=out.new Region(selected[0],selected);found.add(region);for(int cell:selected)if(out.cells.put(cell,region)!=null)throw new IllegalStateException("Distinct snow regions share a cell");}
  out.regions=List.copyOf(found);check();return out;
 }
 private static int cellIndex(int x,int z){int cx=Math.floorDiv(x,2048)+1024,cz=Math.floorDiv(z,2048)+1024;return cx<0||cx>=2048||cz<0||cz>=2048?-1:cz*2048+cx;}
 private long proposal(int cell,int attempt){long h=OakBirchWoodland.mix(seed^0x534E4F5753495445L^((long)cell<<8)^attempt);int cx=(cell%2048-1024)*2048+1024,cz=(cell/2048-1024)*2048+1024;int x=Math.floorDiv(cx+(int)Math.floorMod(h,1536)-768,16)*16+8,z=Math.floorDiv(cz+(int)Math.floorMod(h>>>24,1536)-768,16)*16+8;return ((long)x<<32)|(z&0xffffffffL);}
 public Optional<Plan> forStart(int cx,int cz){long x=((long)cx<<4)+8,z=((long)cz<<4)+8;if(Math.abs(x)>2097100||Math.abs(z)>2097100)return Optional.empty();int index=cellIndex((int)x,(int)z);var region=cells.get(index);if(region==null)return Optional.empty();long wanted=(x<<32)|(z&0xffffffffL);boolean possible=false;for(int i=0;i<64;i++)if(proposal(index,i)==wanted){possible=true;break;}if(!possible)return Optional.empty();return region.plan().filter(p->(p.x>>4)==cx&&(p.z>>4)==cz);}
 public StructurePlacement placement(){return new StructurePlacement(){public boolean isStructureChunk(net.minecraft.world.level.chunk.ChunkGeneratorStructureState state,int x,int z){return forStart(x,z).isPresent();}public MapCodec<? extends StructurePlacement> codec(){return MapCodec.unit(this);}public BlockPos getLocatePos(ChunkPos p){return forStart(p.x(),p.z()).map(Plan::center).orElse(new BlockPos(p.getMinBlockX()+8,64,p.getMinBlockZ()+8));}};}
 public List<Region> ordered(BlockPos origin,double range){var out=new ArrayList<>(regions);out.removeIf(r->r.lower(origin)>range);out.sort(Comparator.comparingDouble((Region r)->r.lower(origin)).thenComparingInt(r->r.owner));return out;}
 int regionCount(){return regions.size();}int candidateCount(){return cells.size();}
 public final class Region {
  private final int owner;private final int[] selected;private volatile Optional<Plan> result;
  Region(int owner,int[] selected){this.owner=owner;this.selected=selected;}
  public int owner(){return owner;}
  public double lower(BlockPos p){double best=Double.POSITIVE_INFINITY;for(int cell:selected){int x=(cell%2048-1024)*2048+1024,z=(cell/2048-1024)*2048+1024;best=Math.min(best,Math.hypot(Math.max(0,Math.abs((long)p.getX()-x)-776),Math.max(0,Math.abs((long)p.getZ()-z)-776)));}return Math.max(0,Math.nextDown(best));}
  public synchronized Optional<Plan> plan(){check();if(result!=null)return result;var value=build(this);check();return result=value;}
 }
 private boolean snowy(int x,int z){var b=surface.getNoiseBiome(Math.floorDiv(x,4),0,Math.floorDiv(z,4));return b.is(Biomes.SNOWY_PLAINS)||b.is(Biomes.SNOWY_TAIGA);}
 private Optional<Plan> build(Region region){var d=terrain.drainage();long h=OakBirchWoodland.mix(seed^0x534E4F57524F4F46L^region.owner);int rotation=(int)((h>>>16)&3),variant=(int)((h>>>24)&1);
  for(int cell:region.selected)for(int attempt=0;attempt<64;attempt++){
   check();long q=proposal(cell,attempt);int x=(int)(q>>32),z=(int)q;var probe=new Plan(x,0,z,rotation,variant,region.owner);int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;boolean good=true;
   // Exact necessary corners of the actual top footprint, before expensive biome queries.
   for(int[] v:CORNERS){var p=probe.local(v[0],0,v[1]);var s=d.sample(p.getX(),p.getZ());min=Math.min(min,s.height());max=Math.max(max,s.height());if(s.waterLevel()!=DrainageNetwork.DRY||max-min>2){good=false;break;}}
   if(!good)continue;if(!SnowyPlains.contains(x,z,d,terrain.climate())&&!SnowyTaiga.contains(seed,x,z,d,terrain.climate()))continue;
   outer:for(int lz=0;lz<8;lz++)for(int lx=0;lx<7;lx++){if(lz<2&&(lx<2||lx>4))continue;var p=probe.local(lx,0,lz);var s=d.sample(p.getX(),p.getZ());min=Math.min(min,s.height());max=Math.max(max,s.height());if(s.waterLevel()!=DrainageNetwork.DRY||max-min>2){good=false;break outer;}}
   if(!good||max<64||max>TerrainShape.CEILING-12)continue;
   var entrance=probe.local(3,0,-1);var approach=d.sample(entrance.getX(),entrance.getZ());if(approach.waterLevel()!=DrainageNetwork.DRY||approach.height()<max-1||approach.height()>max)continue;
   // Native biome identity, including priority over overlapping snowy predicates.
   for(int[] v:BIOMES){var p=probe.local(v[0],0,v[1]);if(!snowy(p.getX(),p.getZ())){good=false;break;}}if(!good)continue;
   var canopy=terrain.canopy().at(x,z);outer:for(int lz=-1;lz<8;lz++)for(int lx=0;lx<7;lx++){if(lz==-1&&(lx<2||lx>4))continue;var p=probe.local(lx,0,lz);if(canopy.topY(p.getX(),p.getZ())>d.sample(p.getX(),p.getZ()).height()+1){good=false;break outer;}}if(!good)continue;
   return Optional.of(new Plan(x,max,z,rotation,variant,region.owner));
  }return Optional.empty();
 }
 private static String hash(long seed,int[] cells){try{var b=java.nio.ByteBuffer.allocate(12+cells.length*4).putInt(0x534E4F31).putLong(seed);for(int cell:cells)b.putInt(cell);return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(b.array()));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private static void check(){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("Snow shelter planning interrupted before publication");}
}
