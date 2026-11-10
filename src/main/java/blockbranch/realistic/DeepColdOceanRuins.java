package blockbranch.realistic;

import java.util.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/** Definition1: at most one native ruin group per complete connected deep cold-family marine basin habitat.
 * A complete immutable ownership index persists. Site decisions are disposable, atomic and bounded. */
public final class DeepColdOceanRuins {
 private record Cache(long seed,int[] cells,String checksum){static final Codec<Cache> CODEC=RecordCodecBuilder.create(i->i.group(Codec.LONG.fieldOf("seed").forGetter(Cache::seed),Codec.INT_STREAM.xmap(v->v.limit(65537).toArray(),Arrays::stream).fieldOf("cells").forGetter(Cache::cells),Codec.STRING.fieldOf("checksum").forGetter(Cache::checksum)).apply(i,Cache::new));}
 public static final Codec<DeepColdOceanRuins> CODEC=RecordCodecBuilder.create(i->i.group(Codec.intRange(1,1).fieldOf("definition").forGetter(v->1),Cache.CODEC.lenientOptionalFieldOf("region_cache").forGetter(v->v.cache.filter(c->c.cells.length<=65536))).apply(i,(definition,cache)->new DeepColdOceanRuins(cache)));
 private Optional<Cache> cache;
 public record Plan(int x,int y,int z,int variant,int region){public BlockPos center(){return new BlockPos(x,y,z);}}

 private long seed;private RealisticGenerator.HutPlanningTerrain terrain;private BiomeResolver surface;private List<Region> regions=List.of();private final Int2ObjectOpenHashMap<Region> cells=new Int2ObjectOpenHashMap<>();
 public DeepColdOceanRuins(){this(Optional.empty());}private DeepColdOceanRuins(Optional<Cache> cache){this.cache=cache;}
 public boolean canSave(){return cache.isPresent()&&cache.get().cells.length<=65536;}
 public boolean valid(long seed){if(cache.isEmpty())return false;var c=cache.get();if(c.seed!=seed||c.cells.length>65536||c.cells.length%4!=0||!hash(seed,c.cells).equals(c.checksum))return false;var seen=new it.unimi.dsi.fastutil.ints.IntOpenHashSet();for(int i=0;i<c.cells.length;i++){int v=c.cells[i];if(v<0||v>=4194304||!seen.add(v)||i%4==0&&i>0&&v<=c.cells[i-4])return false;}return true;}
 DeepColdOceanRuins bindSaved(long seed,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState random){return bind(seed,g,random,valid(seed)?cache:Optional.empty());}
 static DeepColdOceanRuins bind(long seed,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState random){
  return bind(seed,g,random,Optional.empty());}
 private static DeepColdOceanRuins bind(long seed,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState random,Optional<Cache> saved){
  check();var out=new DeepColdOceanRuins(saved);out.seed=seed;out.terrain=g.hutPlanningTerrain(random);out.surface=((ScreeBiomeSource)g.getBiomeSource()).surfaceResolver();var d=out.terrain.drainage();var climate=new DeepColdRuinClimate(seed);
  int[] candidates;if(saved.isPresent())candidates=saved.get().cells;else{var atlas=StructureRegionAtlas.labeled(seed,(x,z)->climate.structureDeepColdBasin(x,z,d));candidates=atlas.candidateIndices(atlas.substantialOwnerIndices(),4);out.cache=Optional.of(new Cache(seed,candidates,hash(seed,candidates)));}var found=new ArrayList<Region>();
  for(int row=0;row<candidates.length/4;row++){check();var selected=Arrays.copyOfRange(candidates,row*4,row*4+4);var region=out.new Region(selected[0],selected);found.add(region);for(int cell:selected)if(out.cells.put(cell,region)!=null)throw new IllegalStateException("Distinct cold-ruin regions share a cell");}
  out.regions=List.copyOf(found);check();return out;
 }
 private static int cellIndex(int x,int z){int cx=Math.floorDiv(x,2048)+1024,cz=Math.floorDiv(z,2048)+1024;return cx<0||cx>=2048||cz<0||cz>=2048?-1:cz*2048+cx;}
 private long proposal(int cell,int attempt){long h=OakBirchWoodland.mix(seed^0x44454550434F4C44L^((long)cell<<8)^attempt);int cx=(cell%2048-1024)*2048+1024,cz=(cell/2048-1024)*2048+1024;int x=Math.floorDiv(cx+(int)Math.floorMod(h,1536)-768,16)*16+8,z=Math.floorDiv(cz+(int)Math.floorMod(h>>>24,1536)-768,16)*16+8;return ((long)x<<32)|(z&0xffffffffL);}
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
 private boolean deepCold(int x,int z){var b=surface.getNoiseBiome(Math.floorDiv(x,4),0,Math.floorDiv(z,4));return b.is(Biomes.DEEP_OCEAN)||b.is(Biomes.DEEP_COLD_OCEAN)||b.is(Biomes.DEEP_FROZEN_OCEAN);}
 private Optional<Plan> build(Region region){var d=terrain.drainage();int variant=0;
  for(int cell:region.selected)for(int attempt=0;attempt<64;attempt++){
   check();long q=proposal(cell,attempt);int x=(int)(q>>32),z=(int)q;var c=d.sample(x,z);int depth=63-c.height();if(depth<=200||c.height()<TerrainShape.FLOOR+16||!d.oceanWater(x,z,c)||!deepCold(x,z))continue;
   int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;boolean good=true;
   outer:for(int dz=-48;dz<=48;dz+=8)for(int dx=-48;dx<=48;dx+=8){var v=d.sample(x+dx,z+dz);min=Math.min(min,v.height());max=Math.max(max,v.height());if(!d.oceanWater(x+dx,z+dz,v)||v.height()>-137||v.height()<TerrainShape.FLOOR+16||max-min>64){good=false;break outer;}}
   if(!good)continue;for(int[] v:new int[][]{{-32,-32},{32,-32},{-32,32},{32,32}})if(!deepCold(x+v[0],z+v[1])){good=false;break;}
   if(good)return Optional.of(new Plan(x,c.height()+1,z,variant,region.owner));
  }return Optional.empty();
 }
 public int floorFor(net.minecraft.world.level.levelgen.structure.BoundingBox box){int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;var d=terrain.drainage();for(int z=box.minZ();z<=box.maxZ();z++)for(int x=box.minX();x<=box.maxX();x++){check();var s=d.sample(x,z);min=Math.min(min,s.height());max=Math.max(max,s.height());if(!d.oceanWater(x,z,s)||s.height()>-137||s.height()<TerrainShape.FLOOR+16||max-min>12)return Integer.MIN_VALUE;}return max+1;}
 private static String hash(long seed,int[] cells){try{var b=java.nio.ByteBuffer.allocate(12+cells.length*4).putInt(0x44435031).putLong(seed);for(int cell:cells)b.putInt(cell);return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(b.array()));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private static void check(){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("Deep cold ruin planning interrupted before publication");}
}
