package blockbranch.realistic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/** Definition 1: three-in-four complete desert-region budgets, never per-area retries. */
public final class DesertWaygates {
 private record Cache(long seed,boolean sandy,int[] data,String checksum){
  static final Codec<Cache> CODEC=RecordCodecBuilder.create(i->i.group(Codec.LONG.fieldOf("seed").forGetter(Cache::seed),Codec.BOOL.fieldOf("sandy_coasts").forGetter(Cache::sandy),Codec.INT_STREAM.xmap(s->s.limit(65537).toArray(),Arrays::stream).fieldOf("sites").forGetter(Cache::data),Codec.STRING.fieldOf("checksum").forGetter(Cache::checksum)).apply(i,Cache::new));
 }
 public static final Codec<DesertWaygates> CODEC=RecordCodecBuilder.create(i->i.group(Codec.intRange(1,1).fieldOf("definition").forGetter(g->1),Cache.CODEC.lenientOptionalFieldOf("plan_cache").forGetter(g->g.cache.filter(c->c.data.length<=65536))).apply(i,(definition,cache)->new DesertWaygates(cache)));
 public record Plan(int x,int y,int z,int rotation,int variant,int owner){
  public BlockPos origin(){return new BlockPos(x-6,y-2,z-6);}
  public BlockPos center(){return new BlockPos(x,y+1,z);}
  public Rotation turn(){return Rotation.values()[rotation];}
  public BlockPos local(int lx,int ly,int lz){return new BlockPos(x+rotateX(lx-6,lz-6,rotation),y-2+ly,z+rotateZ(lx-6,lz-6,rotation));}
 }
 private final Optional<Cache> cache;
 private final List<Plan> plans;
 private final Map<Long,Plan> starts;
 private final Set<Long> affected;
 public DesertWaygates(){this(Optional.empty());}
 private DesertWaygates(Optional<Cache> cache){
  this.cache=cache;var list=new ArrayList<Plan>();var start=new HashMap<Long,Plan>();var routes=new HashSet<Long>();
  int[] a=cache.map(Cache::data).orElseGet(()->new int[0]);
  if(a.length%6==0)for(int i=0;i<a.length;i+=6){var p=new Plan(a[i],a[i+1],a[i+2],a[i+3],a[i+4],a[i+5]);list.add(p);start.put(new ChunkPos(p.x>>4,p.z>>4).pack(),p);for(int z=(p.z-6)>>4;z<=(p.z+6)>>4;z++)for(int x=(p.x-6)>>4;x<=(p.x+6)>>4;x++)routes.add(new ChunkPos(x,z).pack());}
  plans=List.copyOf(list);starts=Map.copyOf(start);affected=Set.copyOf(routes);
 }
 public List<Plan> plans(){return plans;}
 public int retainedIntBytes(){return cache.map(c->c.data.length*4).orElse(0);}
 public boolean canSave(){return cache.isPresent()&&cache.get().data.length<=65536;}
 public boolean valid(long seed,boolean sandy){
  if(cache.isEmpty())return false;var c=cache.get();if(c.seed!=seed||c.sandy!=sandy||c.data.length>65536||c.data.length%6!=0||!c.checksum.equals(hash(seed,sandy,c.data)))return false;
  var owners=new HashSet<Integer>();for(var p:plans)if(Math.abs((long)p.x)>2097100||Math.abs((long)p.z)>2097100||p.y<TerrainShape.FLOOR+4||p.y>TerrainShape.CEILING-12||p.rotation<0||p.rotation>3||p.variant<0||p.variant>1||p.owner<0||p.owner>=4194304||!owners.add(p.owner))return false;
  return starts.size()==plans.size();
 }
 public Optional<Plan> forStart(int x,int z){return Optional.ofNullable(starts.get(new ChunkPos(x,z).pack()));}
 public boolean touches(int x,int z){return affected.contains(new ChunkPos(x,z).pack());}
 public StructurePlacement placement(){return new StructurePlacement(){
  public boolean isStructureChunk(net.minecraft.world.level.chunk.ChunkGeneratorStructureState state,int x,int z){return forStart(x,z).isPresent();}
  public com.mojang.serialization.MapCodec<? extends StructurePlacement> codec(){return com.mojang.serialization.MapCodec.unit(this);}
  public BlockPos getLocatePos(ChunkPos p){return forStart(p.x(),p.z()).map(Plan::center).orElseGet(()->new BlockPos(p.getMinBlockX()+8,64,p.getMinBlockZ()+8));}
 };}
 static DesertWaygates build(long seed,boolean sandy,DesertWell wells,DrainageNetwork d,ClimateField climate){
  var atlas=wells.atlas();int[] owners=atlas.substantialOwnerIndices(),candidates=atlas.candidateIndices(owners,4);var out=new ArrayList<Integer>();var occupied=new StructureProximity(48);
  for(int owner:owners)wells.plan(cell(owner)).ifPresent(p->occupied.add(p.x(),p.z()));
  for(int row=0;row<owners.length;row++){
   int owner=owners[row];if(Math.floorMod(OakBirchWoodland.mix(seed^0x5741594741544531L^owner),4)==0)continue;
   boolean placed=false;
   for(int k=0;k<4&&!placed;k++){
    int index=candidates[row*4+k];if(index<0)break;var c=cell(index);
    for(int attempt=0;attempt<8;attempt++){
     long h=OakBirchWoodland.mix(seed^0x504f5254414c5631L^((long)index<<8)^attempt);int x=c.blockX()+(int)Math.floorMod(h,768)-384,z=c.blockZ()+(int)Math.floorMod(h>>>24,768)-384;
     if(Math.abs((long)x)>2097100||Math.abs((long)z)>2097100)continue;
     if(occupied.near(x,z))continue;
     int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;boolean good=true;
     for(int dz=-6;dz<=6;dz+=12)for(int dx=-6;dx<=6;dx+=12){var s=d.sample(x+dx,z+dz);min=Math.min(min,s.height());max=Math.max(max,s.height());if(s.waterLevel()!=DrainageNetwork.DRY||max-min>2)good=false;}
     if(!good)continue;
     // Habitat predicates are constant in each quart; support heights still cover every column.
     int qx=Math.floorDiv(x-6,4),qz=Math.floorDiv(z-6,4),known=0,suitable=0;
     outer:for(int dz=-6;dz<=6;dz++)for(int dx=-6;dx<=6;dx++){
      var s=d.sample(x+dx,z+dz);min=Math.min(min,s.height());max=Math.max(max,s.height());if(s.waterLevel()!=DrainageNetwork.DRY||max-min>2){good=false;break outer;}
      int bit=1<<((Math.floorDiv(z+dz,4)-qz)*5+Math.floorDiv(x+dx,4)-qx);
      if((known&bit)==0){known|=bit;if(SandyDesert.contains(seed,x+dx,z+dz,d,climate))suitable|=bit;}
      if((suitable&bit)==0){good=false;break outer;}
     }
     int turn=(int)((h>>>48)&3);if(good)for(int dx=-1;dx<=1;dx++){var s=d.sample(x+rotateX(dx,-7,turn),z+rotateZ(dx,-7,turn));if(s.waterLevel()!=DrainageNetwork.DRY||s.height()<max-1||s.height()>max)good=false;}
     if(!good||max<TerrainShape.FLOOR+4||max>TerrainShape.CEILING-12)continue;
     var p=new Plan(x,max,z,turn,(int)(h&1),owner);occupied.add(x,z);for(int v:new int[]{x,max,z,turn,p.variant,owner})out.add(v);placed=true;break;
    }
   }
  }
  int[] data=out.stream().mapToInt(Integer::intValue).toArray();return new DesertWaygates(Optional.of(new Cache(seed,sandy,data,hash(seed,sandy,data))));
 }
 private static BiomeRegionPlacement.Cell cell(int index){return new BiomeRegionPlacement.Cell(index%2048-1024,index/2048-1024);}
 public static int rotateX(int x,int z,int r){return switch(r){case 1->-z;case 2->-x;case 3->z;default->x;};}
 public static int rotateZ(int x,int z,int r){return switch(r){case 1->x;case 2->-z;case 3->-x;default->z;};}
 private static String hash(long seed,boolean sandy,int[] data){try{var b=ByteBuffer.allocate(17+data.length*4).putInt(1).putInt(88).putLong(seed).put((byte)(sandy?1:0));for(int v:data)b.putInt(v);return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b.array()));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
