/** Frozen placement reference from dd0c2e58612fe4d6b1e6b1665cb413c8c3065c44. QA only. */
package blockbranch.realistic;
import java.util.*;import java.nio.*;import java.nio.charset.StandardCharsets;import java.security.*;
import com.mojang.serialization.*;import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;import net.minecraft.world.level.*;import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
/** One budget per terminal lake's coherent swamp fringe, never per chunk or bank patch. */
public final class WetlandHutsReference {
 private static final double[] COS=new double[64],SIN=new double[64];
 static{for(int i=0;i<64;i++){double a=i*Math.PI/32;COS[i]=Math.cos(a);SIN[i]=Math.sin(a);}}
 private record Cache(long seed,int[] data,String hash){static final Codec<Cache> CODEC=RecordCodecBuilder.create(i->i.group(Codec.LONG.fieldOf("seed").forGetter(Cache::seed),Codec.INT_STREAM.xmap(s->s.limit(65537).toArray(),Arrays::stream).fieldOf("sites").forGetter(Cache::data),Codec.STRING.fieldOf("checksum").forGetter(Cache::hash)).apply(i,Cache::new));}
 public static final Codec<WetlandHutsReference> CODEC=RecordCodecBuilder.create(i->i.group(Codec.intRange(1,1).fieldOf("definition").forGetter(s->1),Cache.CODEC.lenientOptionalFieldOf("plan_cache").forGetter(s->s.cache.filter(c->c.data.length<=65536))).apply(i,(definition,cache)->new WetlandHutsReference(cache)));
 public record Plan(int x,int y,int z,int direction,int variant,int lake){
  public Direction facing(){return Direction.from2DDataValue(direction);}
  public int halfX(){return facing().getAxis()==Direction.Axis.X?4:3;}
  public int halfZ(){return facing().getAxis()==Direction.Axis.X?3:4;}
  public BlockPos center(){return new BlockPos(x,y+2,z);}
 }
 private final Optional<Cache> cache;private final List<Plan> plans;private final Map<Long,Plan> starts;
 public WetlandHutsReference(){this(Optional.empty());}
 private WetlandHutsReference(Optional<Cache> cache){this.cache=cache;var p=new ArrayList<Plan>();var map=new HashMap<Long,Plan>();var a=cache.map(Cache::data).orElse(new int[0]);if(a.length%6==0)for(int i=0;i<a.length;i+=6){var v=new Plan(a[i],a[i+1],a[i+2],a[i+3],a[i+4],a[i+5]);p.add(v);map.put(new ChunkPos(v.x>>4,v.z>>4).pack(),v);}plans=List.copyOf(p);starts=Map.copyOf(map);}
 public List<Plan> plans(){return plans;}public boolean canSave(){return cache.isPresent()&&cache.get().data.length<=65536;}public int retainedBytes(){return cache.map(c->c.data.length*4).orElse(0);}
 public boolean valid(long seed){if(cache.isEmpty())return false;var c=cache.get();if(c.seed!=seed||c.data.length>65536||c.data.length%6!=0||!c.hash.equals(hash(seed,c.data)))return false;var lakes=new HashSet<Integer>();for(var p:plans)if(Math.abs((long)p.x)>2097100||Math.abs((long)p.z)>2097100||Math.floorMod(p.x,16)!=8||Math.floorMod(p.z,16)!=8||p.y<64||p.y>668||p.direction<0||p.direction>3||p.variant<0||p.variant>1||p.lake<0||p.lake>=1050625||!lakes.add(p.lake))return false;return starts.size()==plans.size();}
 public Optional<Plan> forStart(int x,int z){return Optional.ofNullable(starts.get(new ChunkPos(x,z).pack()));}
 public StructurePlacement placement(){return new StructurePlacement(){public boolean isStructureChunk(net.minecraft.world.level.chunk.ChunkGeneratorStructureState s,int x,int z){return forStart(x,z).isPresent();}public MapCodec<? extends StructurePlacement> codec(){return MapCodec.unit(this);}public BlockPos getLocatePos(ChunkPos p){return forStart(p.x(),p.z()).map(Plan::center).orElse(new BlockPos(p.getMinBlockX()+8,64,p.getMinBlockZ()+8));}};}
 static WetlandHutsReference build(long seed,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState r){
  var d=g.drainage(r);var climate=new ClimateField(seed);var out=new ArrayList<Integer>();
  for(int lake=0;lake<d.size();lake++){
   if(d.parent(lake)!=-1||!d.closed(lake)||d.waterLevel(lake)<=64||d.waterLevel(lake)>664)continue;
   long budget=OakBirchWoodland.mix(seed^0x4855544C414B4531L^lake);if(Math.floorMod(budget,3)!=0)continue;
   var radii=new double[64];double largest=0;for(int angle=0;angle<64;angle++){radii[angle]=d.lakeShoreRadius(lake,angle*Math.PI/32);largest=Math.max(largest,radii[angle]);}
   if(!climate.swampBankPossible(d.nodeX(lake),d.nodeZ(lake),(int)Math.ceil(largest)+32,d.waterLevel(lake),d))continue;
   var visited=new HashSet<Long>();boolean placed=false;
   // A complete bounded proposal stream for this hydrological region. Its fixed
   // ordering and cap are generation definition1, independent of exploration.
   for(int attempt=0;attempt<2048&&!placed;attempt++){
    int angleIndex=(attempt*37+(int)(budget&63))&63,radial=16+((attempt/64+(int)((budget>>>8)%49))%49);
    double radius=radii[angleIndex]*radial/64.;
    int x=Math.floorDiv(d.nodeX(lake)+(int)Math.round(COS[angleIndex]*radius),16)*16+8,z=Math.floorDiv(d.nodeZ(lake)+(int)Math.round(SIN[angleIndex]*radius),16)*16+8;
    if(Math.abs((long)x)>2097100||Math.abs((long)z)>2097100||!visited.add(new ChunkPos(x>>4,z>>4).pack())||!OakBirchWoodland.glade(seed^0x5357414D50474150L,x,z)||!Swamp.contains(seed,x,z,d,climate)||d.sample(x,z).basin()!=lake)continue;
    int turn=(int)((budget>>>16)&3),variant=(int)((budget>>>24)&1);var probe=new Plan(x,0,z,turn,variant,lake);int hx=probe.halfX(),hz=probe.halfZ(),min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;boolean good=true;
    outer:for(int dz=-hz;dz<=hz;dz++)for(int dx=-hx;dx<=hx;dx++){
     var q=d.sample(x+dx,z+dz);min=Math.min(min,q.height());max=Math.max(max,q.height());if(q.waterLevel()!=DrainageNetwork.DRY||q.basin()!=lake||max-min>2||!Swamp.contains(seed,x+dx,z+dz,d,climate)){good=false;break outer;}
    }
    if(!good)continue;
    // Exact immutable canopy occupancy prevents severed trunks and floating crowns.
    var canopy=g.canopy(x,z,r);outer:for(int dz=-hz;dz<=hz;dz++)for(int dx=-hx;dx<=hx;dx++)if(canopy.topY(x+dx,z+dz)>d.sample(x+dx,z+dz).height()+1){good=false;break outer;}
    if(!good||max<d.waterLevel(lake))continue;
    // Native local front step must meet the bank instead of floating above it.
    int stepX=x,stepZ=z;switch(probe.facing()){case NORTH->stepZ+=4;case SOUTH->stepZ-=4;case WEST->stepX+=4;case EAST->stepX-=4;default->{}}
    int step=d.sample(stepX,stepZ).height();if(step<max-1)continue;
    switch(probe.facing()){case NORTH->stepZ++;case SOUTH->stepZ--;case WEST->stepX++;case EAST->stepX--;default->{}}
    var approach=d.sample(stepX,stepZ);if(approach.waterLevel()!=DrainageNetwork.DRY||approach.height()<max-1||approach.height()>max||canopy.topY(stepX,stepZ)>approach.height()+1)continue;
    for(int value:new int[]{x,max-1,z,turn,variant,lake})out.add(value);placed=true;
   }
  }
  int[] data=out.stream().mapToInt(Integer::intValue).toArray();return new WetlandHutsReference(Optional.of(new Cache(seed,data,hash(seed,data))));
 }
 private static String hash(long seed,int[] a){try{var b=ByteBuffer.allocate(12+a.length*4).putInt(0x48555434).putLong(seed);for(int v:a)b.putInt(v);return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b.array()));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
