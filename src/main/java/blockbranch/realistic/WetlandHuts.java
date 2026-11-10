package blockbranch.realistic;
import java.util.*;import java.nio.*;import java.security.*;
import com.mojang.serialization.*;import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;import net.minecraft.world.level.*;import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
/** One budget per terminal lake's coherent swamp fringe, never per chunk or bank patch. */
public final class WetlandHuts {
 private static final double[] COS=new double[64],SIN=new double[64];
 static{for(int i=0;i<64;i++){double a=i*Math.PI/32;COS[i]=Math.cos(a);SIN[i]=Math.sin(a);}}
 private record Cache(long seed,int[] data,String hash){static final Codec<Cache> CODEC=RecordCodecBuilder.create(i->i.group(Codec.LONG.fieldOf("seed").forGetter(Cache::seed),Codec.INT_STREAM.xmap(s->s.limit(65537).toArray(),Arrays::stream).fieldOf("sites").forGetter(Cache::data),Codec.STRING.fieldOf("checksum").forGetter(Cache::hash)).apply(i,Cache::new));}
 public static final Codec<WetlandHuts> CODEC=RecordCodecBuilder.create(i->i.group(Codec.intRange(1,1).fieldOf("definition").forGetter(s->1),Cache.CODEC.lenientOptionalFieldOf("plan_cache").forGetter(s->s.savedCache().filter(c->c.data.length<=65536))).apply(i,(definition,cache)->new WetlandHuts(cache)));
 public record Plan(int x,int y,int z,int direction,int variant,int lake){
  public Direction facing(){return Direction.from2DDataValue(direction);}
  public int halfX(){return facing().getAxis()==Direction.Axis.X?4:3;}
  public int halfZ(){return facing().getAxis()==Direction.Axis.X?3:4;}
  public BlockPos center(){return new BlockPos(x,y+2,z);}
 }
 private final Optional<Cache> cache;private final List<Plan> plans;private final Map<Long,Plan> starts;private final WetlandHutRegions regions;
 public WetlandHuts(){this(Optional.empty());}
 private WetlandHuts(Optional<Cache> cache){this(cache,null);}
 private WetlandHuts(Optional<Cache> cache,WetlandHutRegions regions){this.regions=regions;this.cache=cache;var p=new ArrayList<Plan>();var map=new HashMap<Long,Plan>();var a=cache.map(Cache::data).orElse(new int[0]);if(a.length%6==0)for(int i=0;i<a.length;i+=6){var v=new Plan(a[i],a[i+1],a[i+2],a[i+3],a[i+4],a[i+5]);p.add(v);map.put(new ChunkPos(v.x>>4,v.z>>4).pack(),v);}plans=List.copyOf(p);starts=Map.copyOf(map);}
 private Optional<Cache> savedCache(){var full=regions==null?null:regions.completed();return regions==null?cache:full==null?Optional.empty():full.cache;}
 public List<Plan> plans(){return regions==null?plans:regions.all().plans();}
 public boolean canSave(){return savedCache().filter(c->c.data.length<=65536).isPresent();}
 public int retainedBytes(){return savedCache().map(c->c.data.length*4).orElse(0);}
 int regionCount(){return regions==null?plans.size():regions.regions().size();}
 int resolvedRegions(){return regions==null?plans.size():regions.resolvedCount();}
 public interface Candidate {int lake();double lowerDistance(BlockPos origin);Optional<Plan> plan();}
 private record SavedCandidate(Plan value) implements Candidate {
  public int lake(){return value.lake();}public double lowerDistance(BlockPos o){return Math.hypot(value.x()-(double)o.getX(),value.z()-(double)o.getZ());}public Optional<Plan> plan(){return Optional.of(value);}
 }
 public List<Candidate> orderedGroups(BlockPos origin,double range){
  var out=new ArrayList<Candidate>();if(regions!=null)out.addAll(regions.regions());else for(var plan:plans)out.add(new SavedCandidate(plan));
  out.removeIf(c->c.lowerDistance(origin)>range);out.sort(Comparator.comparingDouble((Candidate c)->c.lowerDistance(origin)).thenComparingInt(Candidate::lake));return out;
 }
 static WetlandHuts bind(long seed,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState r){HutPlanningWork.checkCancelled(Thread.currentThread()::isInterrupted);return new WetlandHuts(Optional.empty(),new WetlandHutRegions(seed,g.hutPlanningTerrain(r)));}
 static WetlandHuts fromPlans(long seed,List<Plan> plans){int[] data=new int[plans.size()*6];int i=0;for(var p:plans)for(int v:new int[]{p.x,p.y,p.z,p.direction,p.variant,p.lake})data[i++]=v;return new WetlandHuts(Optional.of(new Cache(seed,data,hash(seed,data))));}
 public boolean valid(long seed){if(regions!=null){var full=regions.completed();return full!=null&&full.valid(seed);}if(cache.isEmpty())return false;var c=cache.get();if(c.seed!=seed||c.data.length>65536||c.data.length%6!=0||!c.hash.equals(hash(seed,c.data)))return false;var lakes=new HashSet<Integer>();for(var p:plans)if(Math.abs((long)p.x)>2097100||Math.abs((long)p.z)>2097100||Math.floorMod(p.x,16)!=8||Math.floorMod(p.z,16)!=8||p.y<64||p.y>668||p.direction<0||p.direction>3||p.variant<0||p.variant>1||p.lake<0||p.lake>=1050625||!lakes.add(p.lake))return false;return starts.size()==plans.size();}
 public Optional<Plan> forStart(int x,int z){return regions==null?Optional.ofNullable(starts.get(new ChunkPos(x,z).pack())):regions.forStart(x,z);}
 public StructurePlacement placement(){return new StructurePlacement(){public boolean isStructureChunk(net.minecraft.world.level.chunk.ChunkGeneratorStructureState s,int x,int z){return forStart(x,z).isPresent();}public MapCodec<? extends StructurePlacement> codec(){return MapCodec.unit(this);}public BlockPos getLocatePos(ChunkPos p){return forStart(p.x(),p.z()).map(Plan::center).orElse(new BlockPos(p.getMinBlockX()+8,64,p.getMinBlockZ()+8));}};}
 static WetlandHuts build(long seed,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState r){
  HutPlanningWork.checkCancelled(Thread.currentThread()::isInterrupted);
  var terrain=g.hutPlanningTerrain(r);
  var out=HutPlanningWork.run(terrain.drainage().size(),(first,end,cancelled)->buildRange(seed,terrain,first,end,cancelled));
  int[] data=out.stream().mapToInt(Integer::intValue).toArray();return new WetlandHuts(Optional.of(new Cache(seed,data,hash(seed,data))));
 }
 /** Sequential fallback also serves exact matched qualification of the worker. */
 static WetlandHuts buildSequential(long seed,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState r){
  HutPlanningWork.checkCancelled(Thread.currentThread()::isInterrupted);
  var terrain=g.hutPlanningTerrain(r);var out=buildRange(seed,terrain,0,terrain.drainage().size(),Thread.currentThread()::isInterrupted);
  int[] data=out.stream().mapToInt(Integer::intValue).toArray();return new WetlandHuts(Optional.of(new Cache(seed,data,hash(seed,data))));
 }
 static List<Integer> buildRange(long seed,RealisticGenerator.HutPlanningTerrain terrain,int first,int end,java.util.function.BooleanSupplier cancelled){
  var d=terrain.drainage();var climate=terrain.climate();var out=new ArrayList<Integer>();
  for(int lake=first;lake<end;lake++){
   HutPlanningWork.checkCancelled(cancelled);
   if(d.parent(lake)!=-1||!d.closed(lake)||d.waterLevel(lake)<=64||d.waterLevel(lake)>664)continue;
   long budget=OakBirchWoodland.mix(seed^0x4855544C414B4531L^lake);if(Math.floorMod(budget,3)!=0)continue;
   var radii=new double[64];double largest=0;for(int angle=0;angle<64;angle++){radii[angle]=d.lakeShoreRadius(lake,angle*Math.PI/32);largest=Math.max(largest,radii[angle]);}
   if(!climate.swampBankPossible(d.nodeX(lake),d.nodeZ(lake),(int)Math.ceil(largest)+32,d.waterLevel(lake),d))continue;
   var visited=new it.unimi.dsi.fastutil.longs.LongOpenHashSet(512);boolean placed=false;
   // A complete bounded proposal stream for this hydrological region. Its fixed
   // ordering and cap are generation definition1, independent of exploration.
   for(int attempt=0;attempt<2048&&!placed;attempt++){
    HutPlanningWork.checkCancelled(cancelled);
    int angleIndex=(attempt*37+(int)(budget&63))&63,radial=16+((attempt/64+(int)((budget>>>8)%49))%49);
    double radius=radii[angleIndex]*radial/64.;
    int x=Math.floorDiv(d.nodeX(lake)+(int)Math.round(COS[angleIndex]*radius),16)*16+8,z=Math.floorDiv(d.nodeZ(lake)+(int)Math.round(SIN[angleIndex]*radius),16)*16+8;
    if(Math.abs((long)x)>2097100||Math.abs((long)z)>2097100||!visited.add(new ChunkPos(x>>4,z>>4).pack())||!OakBirchWoodland.glade(seed^0x5357414D50474150L,x,z)||!d.couldBeTerminalLakeBank(lake,x,z)||!Swamp.contains(seed,x,z,d,climate)||d.sample(x,z).basin()!=lake)continue;
    int turn=(int)((budget>>>16)&3),variant=(int)((budget>>>24)&1);var probe=new Plan(x,0,z,turn,variant,lake);int hx=probe.halfX(),hz=probe.halfZ(),min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;boolean good=true;int known=0,suitable=0,qx=Math.floorDiv(x-hx,4),qz=Math.floorDiv(z-hz,4);
    outer:for(int dz=-hz;dz<=hz;dz++)for(int dx=-hx;dx<=hx;dx++){
     var q=d.sample(x+dx,z+dz);min=Math.min(min,q.height());max=Math.max(max,q.height());if(q.waterLevel()!=DrainageNetwork.DRY||q.basin()!=lake||max-min>2){good=false;break outer;}
     int bit=1<<((Math.floorDiv(z+dz,4)-qz)*3+Math.floorDiv(x+dx,4)-qx);
     if((known&bit)==0){known|=bit;if(Swamp.contains(seed,x+dx,z+dz,d,climate))suitable|=bit;}
     if((suitable&bit)==0){good=false;break outer;}
    }
    if(!good||max<d.waterLevel(lake))continue;
    // Native local front step must meet the bank instead of floating above it.
    int stepX=x,stepZ=z;switch(probe.facing()){case NORTH->stepZ+=4;case SOUTH->stepZ-=4;case WEST->stepX+=4;case EAST->stepX-=4;default->{}}
    int step=d.sample(stepX,stepZ).height();if(step<max-1)continue;
    switch(probe.facing()){case NORTH->stepZ++;case SOUTH->stepZ--;case WEST->stepX++;case EAST->stepX--;default->{}}
    var approach=d.sample(stepX,stepZ);if(approach.waterLevel()!=DrainageNetwork.DRY||approach.height()<max-1||approach.height()>max)continue;
    // All pure terrain rejections precede the more expensive exact canopy.
    var canopy=terrain.canopy().at(x,z);outer:for(int dz=-hz;dz<=hz;dz++)for(int dx=-hx;dx<=hx;dx++)if(canopy.topY(x+dx,z+dz)>d.sample(x+dx,z+dz).height()+1){good=false;break outer;}
    if(!good||canopy.topY(stepX,stepZ)>approach.height()+1)continue;
    for(int value:new int[]{x,max-1,z,turn,variant,lake})out.add(value);placed=true;
   }
  }
  return out;
 }
 private static String hash(long seed,int[] a){try{var b=ByteBuffer.allocate(12+a.length*4).putInt(0x48555434).putLong(seed);for(int v:a)b.putInt(v);return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b.array()));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
