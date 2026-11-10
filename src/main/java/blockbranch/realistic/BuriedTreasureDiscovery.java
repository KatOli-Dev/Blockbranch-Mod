package blockbranch.realistic;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import net.minecraft.core.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/** Definition 1: one native buried chest budget per coherent low-relief coastal region. */
public final class BuriedTreasureDiscovery {
 public static final Codec<BuriedTreasureDiscovery> CODEC=RecordCodecBuilder.create(i->i.group(
  Codec.LONG.fieldOf("seed").forGetter(t->t.seed),Codec.BOOL.fieldOf("sandy_coasts").forGetter(t->t.sandy),
  Codec.INT_STREAM.xmap(s->s.limit(65537).toArray(),Arrays::stream).fieldOf("sites").forGetter(t->t.data),
  Codec.STRING.fieldOf("checksum").forGetter(t->t.checksum)
 ).apply(i,BuriedTreasureDiscovery::new));
 private final long[] chunks;
 private final Group[] chunkGroups;
 private final List<Group> groups;
 private volatile Binding binding;
 private record Binding(DrainageNetwork terrain,ClimateField climate){}
 private final long seed;private final boolean sandy;private final int[] data;private final String checksum;
 private BuriedTreasureDiscovery(long seed,boolean sandy,int[] data,String checksum){
  this.seed=seed;this.sandy=sandy;this.data=data.clone();this.checksum=checksum;var grouped=new LinkedHashMap<Integer,List<Site>>();
  for(int i=0;i+3<data.length;i+=4)grouped.computeIfAbsent(data[i+3],k->new ArrayList<>()).add(new Site(data[i],data[i+1],data[i+2]));
  var list=new ArrayList<Group>();var routes=new TreeMap<Long,Group>();
  grouped.forEach((id,sites)->{var group=new Group(id,List.copyOf(sites));list.add(group);for(var site:sites)routes.put(new ChunkPos(site.x()>>4,site.z()>>4).pack(),group);});
  groups=List.copyOf(list);chunks=new long[routes.size()];chunkGroups=new Group[routes.size()];int i=0;for(var entry:routes.entrySet()){chunks[i]=entry.getKey();chunkGroups[i++]=entry.getValue();}
 }
 public record Site(int x,int z,int regionCells){}
 public final class Group {
  private final int id;private final List<Site> anchors;private volatile Optional<Site> result;
  private Group(int id,List<Site> anchors){this.id=id;this.anchors=anchors;}
  public double lowerDistance(BlockPos origin){double distance=Double.POSITIVE_INFINITY;for(var p:anchors)distance=Math.min(distance,Math.hypot(p.x()-(double)origin.getX(),p.z()-(double)origin.getZ()));return distance;}
  public Optional<Site> site(){
   var ready=result;if(ready!=null)return ready;
   synchronized(this){if(result!=null)return result;var field=binding;if(field==null)throw new IllegalStateException("Unbound treasure catalog");
    for(var p:anchors){var t=field.terrain().sample(p.x(),p.z());if(t.waterLevel()!=DrainageNetwork.DRY||t.height()<63||t.height()>67)continue;
     if(CoastalBeach.contains(p.x(),p.z(),field.terrain(),field.climate(),(id&1)!=0))return result=Optional.of(p);}
    return result=Optional.empty();
   }
  }
 }
 void bind(DrainageNetwork d,ClimateField c){binding=new Binding(d,c);}
 public List<Group> orderedGroups(BlockPos origin,long range){var list=new ArrayList<Group>();for(var group:groups)if(group.lowerDistance(origin)<=range)list.add(group);list.sort(Comparator.comparingDouble(g->g.lowerDistance(origin)));return list;}
 public List<Site> sites(){var result=new ArrayList<Site>();for(var group:groups)group.site().ifPresent(result::add);return List.copyOf(result);}
 public boolean valid(long seed,boolean sandy){
  if(this.seed!=seed||this.sandy!=sandy||data.length>65536||data.length%4!=0||!checksum.equals(hash(seed,sandy,data)))return false;
  var seen=new HashSet<Long>();for(int i=0;i<data.length;i+=4)if(Math.abs((long)data[i])>2097100||Math.abs((long)data[i+1])>2097100||Math.floorMod(data[i],16)!=9||Math.floorMod(data[i+1],16)!=9||data[i+2]<4||data[i+2]>4194304||data[i+3]<0||data[i+3]>=8388608||!seen.add(new ChunkPos(data[i]>>4,data[i+1]>>4).pack()))return false;
  for(var group:groups)if(group.anchors.size()>4)return false;return true;
 }
 int[] anchorData(){return data.clone();}
 String anchorFingerprint(){return checksum;}
 Optional<Site> regionSite(int id){for(var group:groups)if(group.id==id)return group.site();return Optional.empty();}
 public int retainedBytes(){return data.length*4+chunks.length*8;}
 public boolean canSave(){return data.length<=65536;}
 public boolean containsChunk(int x,int z){int at=Arrays.binarySearch(chunks,new ChunkPos(x,z).pack());if(at<0)return false;var site=chunkGroups[at].site();return site.isPresent()&&(site.get().x()>>4)==x&&(site.get().z()>>4)==z;}
 public StructurePlacement placement(){return new StructurePlacement(){
  public boolean isStructureChunk(net.minecraft.world.level.chunk.ChunkGeneratorStructureState state,int x,int z){return containsChunk(x,z);}
  public MapCodec<? extends StructurePlacement> codec(){return MapCodec.unit(this);}
  public BlockPos getLocatePos(ChunkPos p){return new BlockPos(p.getMinBlockX()+9,64,p.getMinBlockZ()+9);}
 };}
 static BuriedTreasureDiscovery build(long seed,boolean sandy,DrainageNetwork d,ClimateField climate){
  var atlas=StructureRegionAtlas.labeled(seed^0x5452454153555245L,(x,z)->!coast(d,x,z)?0:climate.sample(x,z,d::structureClimateSample).meanTemperatureC()<2?2:1);
  int[] owners=atlas.substantialOwnerIndices(),candidates=atlas.candidateIndices(owners,4);var sites=new ArrayList<Integer>();var used=new HashSet<Long>();
  var shape=new TerrainShape(seed);blockbranch.Blockbranch.LOGGER.info("TREASURE_REGIONS {}",owners.length);
  int[][] edges={{0,0,2048,0},{2048,0,2048,2048},{2048,2048,0,2048},{0,2048,0,0},{0,0,2048,2048},{0,2048,2048,0}};
  for(int row=0;row<owners.length;row++)for(int k=0;k<4;k++){
   int ownerX=(owners[row]%2048-1024)*2048+1024,ownerZ=(owners[row]/2048-1024)*2048+1024;
   boolean snow=climate.sample(ownerX,ownerZ,d::structureClimateSample).meanTemperatureC()<2;
   int index=candidates[row*4+k];if(index<0)break;int cx=index%2048-1024,cz=index/2048-1024;
   for(int e=0;e<edges.length;e++){
    int[] edge=edges[(e+(int)Math.floorMod(OakBirchWoodland.mix(seed^index),6))%6];
    double ax=cx*2048+edge[0],az=cz*2048+edge[1],bx=cx*2048+edge[2],bz=cz*2048+edge[3];
    boolean aWet=shape.continuousHeight(ax,az)<63,bWet=shape.continuousHeight(bx,bz)<63;if(aWet==bWet)continue;
    for(int n=0;n<16;n++){double mx=(ax+bx)*.5,mz=(az+bz)*.5;if((shape.continuousHeight(mx,mz)<63)==aWet){ax=mx;az=mz;}else{bx=mx;bz=mz;}}
    double rx=(ax+bx)*.5,rz=(az+bz)*.5,nx=shape.continuousHeight(rx+32,rz)-shape.continuousHeight(rx-32,rz),nz=shape.continuousHeight(rx,rz+32)-shape.continuousHeight(rx,rz-32),length=Math.hypot(nx,nz);if(length<1e-9)continue;
    int distance=-16;
    int x=Math.floorDiv((int)Math.floor(rx+nx/length*distance),16)*16+9,z=Math.floorDiv((int)Math.floor(rz+nz/length*distance),16)*16+9;
    if(Math.abs((long)x)>2097100||Math.abs((long)z)>2097100)continue;
    var region=atlas.region(new BiomeRegionPlacement.Cell(Math.floorDiv(x,2048),Math.floorDiv(z,2048)));
    if(region.owner()==null||!region.owner().equals(new BiomeRegionPlacement.Cell(owners[row]%2048-1024,owners[row]/2048-1024)))continue;
    if(!used.add(new ChunkPos(x>>4,z>>4).pack()))continue;
    sites.add(x);sites.add(z);sites.add(region.cells());sites.add(owners[row]*2+(snow?1:0));break;
   }
  }
  blockbranch.Blockbranch.LOGGER.info("TREASURE_ANCHORS {}",sites.size()/4);
  int[] data=sites.stream().mapToInt(Integer::intValue).toArray();return new BuriedTreasureDiscovery(seed,sandy,data,hash(seed,sandy,data));
 }
 private static boolean coast(DrainageNetwork d,int x,int z){
  long center=d.structureClimatePacked(x,z);int height=(int)(center>>32);if((center&2)==0||Math.abs(height+1-TerrainShape.SEA_LEVEL)>64)return false;
  boolean edge=false;int min=height,max=height;
  for(int dz=-2048;dz<=2048;dz+=2048)for(int dx=-2048;dx<=2048;dx+=2048){if(dx==0&&dz==0)continue;long n=d.structureClimatePacked(x+dx,z+dz);int h=(int)(n>>32);min=Math.min(min,h);max=Math.max(max,h);edge|=(center&1)!=(n&1);}
  return edge&&max-min<=128;
 }
 private static String hash(long seed,boolean sandy,int[] data){try{var b=ByteBuffer.allocate(13+data.length*4).putInt(3).putLong(seed).put((byte)(sandy?1:0));for(int v:data)b.putInt(v);return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b.array()));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
