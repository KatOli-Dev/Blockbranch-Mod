package blockbranch.realistic;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/** Frozen full c8b75186c5729dac8d31938a4a0537d67a08d1ba baseline: retained for exact output and matched construction measurements. */
final class ShipwreckDiscoveryReference {
 private record Cache(long seed,String source,int[] data,String checksum){
  static final Codec<Cache> CODEC=RecordCodecBuilder.create(i->i.group(Codec.LONG.fieldOf("seed").forGetter(Cache::seed),Codec.STRING.fieldOf("source").forGetter(Cache::source),Codec.INT_STREAM.xmap(s->s.limit(262145).toArray(),Arrays::stream).fieldOf("anchors").forGetter(Cache::data),Codec.STRING.fieldOf("checksum").forGetter(Cache::checksum)).apply(i,Cache::new));
 }
 public static final Codec<ShipwreckDiscoveryReference> CODEC=RecordCodecBuilder.create(i->i.group(Codec.intRange(1,1).fieldOf("definition").forGetter(s->1),Cache.CODEC.lenientOptionalFieldOf("anchor_cache").forGetter(s->s.cache.filter(c->c.data.length<=262144))).apply(i,(definition,cache)->new ShipwreckDiscoveryReference(cache)));
 private final Optional<Cache> cache;
 private final List<Group> groups;
 private final Map<Long,List<Group>> routes;
 private volatile Binding binding;
 private record Binding(DrainageNetwork terrain,ClimateField climate,BuriedTreasureDiscovery treasure){}
 private record Anchor(int x,int z,int id,int rotation,int variant,int cells,int parentX,int parentZ){}
 public record Plan(int x,int y,int z,int rotation,int variant,int cells){
  public Rotation turn(){return Rotation.values()[rotation];}
  public BlockPos origin(){return new BlockPos(x-4,y,z-15);}
  public BlockPos center(){return new BlockPos(x,y+3,z);}
 }
 public ShipwreckDiscoveryReference(){this(Optional.empty());}
 private ShipwreckDiscoveryReference(Optional<Cache> cache){
  this.cache=cache;var grouped=new LinkedHashMap<Integer,List<Anchor>>();int[] data=cache.map(Cache::data).orElseGet(()->new int[0]);
  if(data.length%8==0)for(int i=0;i<data.length;i+=8){var a=new Anchor(data[i],data[i+1],data[i+2],data[i+3],data[i+4],data[i+5],data[i+6],data[i+7]);grouped.computeIfAbsent(a.id,k->new ArrayList<>()).add(a);}
  var list=new ArrayList<Group>();var routing=new HashMap<Long,List<Group>>();
  grouped.forEach((id,anchors)->{var group=new Group(id,List.copyOf(anchors));list.add(group);
   for(var a:anchors)for(int z=(a.z-15)>>4;z<=(a.z+15)>>4;z++)for(int x=(a.x-15)>>4;x<=(a.x+15)>>4;x++){
    var entries=routing.computeIfAbsent(new ChunkPos(x,z).pack(),key->new ArrayList<>());if(!entries.contains(group))entries.add(group);
   }});
  groups=List.copyOf(list);var immutable=new HashMap<Long,List<Group>>();routing.forEach((key,value)->immutable.put(key,List.copyOf(value)));routes=Map.copyOf(immutable);
 }
 public boolean valid(long seed,BuriedTreasureDiscovery source){
  if(cache.isEmpty())return false;var c=cache.get();var data=c.data;
  if(c.seed!=seed||!c.source.equals(source.anchorFingerprint())||data.length>262144||data.length%8!=0||!c.checksum.equals(hash(seed,c.source,data)))return false;
  for(var group:groups){if(group.anchors.size()>8)return false;for(var a:group.anchors)if(Math.abs((long)a.x)>2097100||Math.abs((long)a.z)>2097100||Math.floorMod(a.x,16)!=9||Math.floorMod(a.z,16)!=9||a.rotation<0||a.rotation>3||a.variant<0||a.variant>1||a.cells<4||a.cells>4194304||a.id<0||a.id>=8388608)return false;}
  return true;
 }
 public boolean canSave(){return cache.isPresent()&&cache.get().data.length<=262144;}
 public int retainedDataBytes(){return cache.map(c->c.data.length*4).orElse(0);}
 public int anchorCount(){return cache.map(c->c.data.length/8).orElse(0);}
 public int groupCount(){return groups.size();}
 void bind(DrainageNetwork terrain,ClimateField climate,BuriedTreasureDiscovery treasure){binding=new Binding(terrain,climate,treasure);}
 public final class Group {
  private final int id;private final List<Anchor> anchors;private volatile Optional<Plan> result;
  private Group(int id,List<Anchor> anchors){this.id=id;this.anchors=anchors;}
  public double lowerDistance(BlockPos p){double d=Double.POSITIVE_INFINITY;for(var a:anchors)d=Math.min(d,Math.hypot(a.x-(double)p.getX(),a.z-(double)p.getZ()));return d;}
  public Optional<Plan> plan(){var ready=result;if(ready!=null)return ready;synchronized(this){
   if(result!=null)return result;if(cache.isEmpty()||Math.floorMod(OakBirchWoodland.mix(cache.get().seed^0x5348495057524543L^id),6)!=0)return result=Optional.empty();var b=binding;if(b==null)throw new IllegalStateException("Unbound wreck catalog");var treasure=b.treasure.regionSite(id);if(treasure.isEmpty())return result=Optional.empty();
   for(var a:anchors){if(a.parentX!=treasure.get().x()||a.parentZ!=treasure.get().z())continue;
    var center=b.terrain.sample(a.x,a.z);if(center.waterLevel()!=DrainageNetwork.DRY||!CoastalBeach.contains(a.x,a.z,b.terrain,b.climate,(id&1)!=0))continue;
    int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;boolean good=true;
    outer:for(int dz=-15;dz<=15;dz++)for(int dx=-4;dx<=4;dx++){
     int wx=a.x+rotateX(dx,dz,a.rotation),wz=a.z+rotateZ(dx,dz,a.rotation);var s=b.terrain.sample(wx,wz);
     min=Math.min(min,s.height());max=Math.max(max,s.height());
     if(s.height()<61||s.height()>67||max-min>3||s.closed()||s.waterLevel()!=DrainageNetwork.DRY&&(s.waterLevel()!=64||!b.terrain.oceanWater(wx,wz,s))){good=false;break outer;}
    }
    if(good)for(int dz=-4;dz<=-2;dz++){
     var approach=b.terrain.sample(a.x+rotateX(5,dz,a.rotation),a.z+rotateZ(5,dz,a.rotation));
     if(approach.waterLevel()!=DrainageNetwork.DRY||approach.height()<max-1||approach.height()>max){good=false;break;}
    }
    if(good)return result=Optional.of(new Plan(a.x,max-2,a.z,a.rotation,a.variant,a.cells));
   }
   return result=Optional.empty();
  }}
 }
 public List<Plan> plans(){var result=new ArrayList<Plan>();for(var group:groups)group.plan().ifPresent(result::add);return List.copyOf(result);}
 public List<Group> orderedGroups(BlockPos origin,long range){return groups.stream().filter(g->g.lowerDistance(origin)<=range).sorted(Comparator.comparingDouble(g->g.lowerDistance(origin))).toList();}
 public Optional<Plan> forStart(int x,int z){for(var group:routes.getOrDefault(new ChunkPos(x,z).pack(),List.of())){var plan=group.plan();if(plan.isPresent()&&(plan.get().x>>4)==x&&(plan.get().z>>4)==z)return plan;}return Optional.empty();}
 public boolean touchesChunk(int x,int z){for(var group:routes.getOrDefault(new ChunkPos(x,z).pack(),List.of())){var p=group.plan();if(p.isPresent()){var a=p.get();int hx=a.rotation%2==0?4:15,hz=a.rotation%2==0?15:4;if((a.x-hx)>>4<=x&&x<=(a.x+hx)>>4&&(a.z-hz)>>4<=z&&z<=(a.z+hz)>>4)return true;}}return false;}
 public StructurePlacement placement(){return new StructurePlacement(){
  public boolean isStructureChunk(net.minecraft.world.level.chunk.ChunkGeneratorStructureState state,int x,int z){return forStart(x,z).isPresent();}
  public MapCodec<? extends StructurePlacement> codec(){return MapCodec.unit(this);}
  public BlockPos getLocatePos(ChunkPos p){return new BlockPos(p.getMinBlockX()+9,64,p.getMinBlockZ()+9);}
 };}
 public static int rotateX(int x,int z,int turn){return switch(turn){case 1->-z;case 2->-x;case 3->z;default->x;};}
 public static int rotateZ(int x,int z,int turn){return switch(turn){case 1->x;case 2->-z;case 3->-x;default->z;};}
 static ShipwreckDiscoveryReference build(long seed,BuriedTreasureDiscovery source){
  int[] raw=source.anchorData();var shape=new TerrainShape(seed);var data=new ArrayList<Integer>();var used=new ArrayList<long[]>();
  for(int i=0;i<raw.length;i+=4){int px=raw[i],pz=raw[i+1];double nx=shape.continuousHeight(px+32,pz)-shape.continuousHeight(px-32,pz),nz=shape.continuousHeight(px,pz+32)-shape.continuousHeight(px,pz-32),length=Math.hypot(nx,nz);if(length<1e-9)continue;
   int turn=Math.abs(nx)>Math.abs(nz)?(nx>0?1:3):(nz>0?2:0);int variant=(int)(OakBirchWoodland.mix(seed^raw[i+3])&1);
   for(int sign:new int[]{1,-1}){
    int x=Math.floorDiv((int)Math.round(px-sign*nz/length*256),16)*16+9,z=Math.floorDiv((int)Math.round(pz+sign*nx/length*256),16)*16+9;
    if(Math.abs((long)x)>2097100||Math.abs((long)z)>2097100||Math.floorDiv(x,2048)!=Math.floorDiv(px,2048)||Math.floorDiv(z,2048)!=Math.floorDiv(pz,2048))continue;
    boolean close=false;for(int k=0;k<raw.length;k+=4)if(Math.hypot(x-(double)raw[k],z-(double)raw[k+1])<128){close=true;break;}if(close)continue;
    for(var p:used)if(Math.hypot(x-p[0],z-p[1])<48){close=true;break;}if(close)continue;
    used.add(new long[]{x,z});for(int value:new int[]{x,z,raw[i+3],turn,variant,raw[i+2],px,pz})data.add(value);
   }
  }
  int[] array=data.stream().mapToInt(Integer::intValue).toArray();var fingerprint=source.anchorFingerprint();return new ShipwreckDiscoveryReference(Optional.of(new Cache(seed,fingerprint,array,hash(seed,fingerprint,array))));
 }
 private static String hash(long seed,String source,int[] data){try{var buffer=ByteBuffer.allocate(12+source.length()+data.length*4).putInt(1).putLong(seed).put(source.getBytes(StandardCharsets.US_ASCII));for(int value:data)buffer.putInt(value);return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(buffer.array()));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
