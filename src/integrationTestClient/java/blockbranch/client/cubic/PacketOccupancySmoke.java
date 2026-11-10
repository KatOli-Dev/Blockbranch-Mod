package blockbranch.client.cubic;
import blockbranch.cubic.*;
import blockbranch.qa.PacketOccupancyProbe;
import java.util.*;
import java.nio.file.*;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
/** Both arms invoke the actual receiver, actual native scan body and native callbacks. */
public final class PacketOccupancySmoke {
 private record Result(long states,long cpu,long bytes,long wall,List<String> events){}
 private static Result receive(Minecraft client,TerrainWindowPacket packet,boolean legacy){
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long id=Thread.currentThread().threadId();PacketOccupancyProbe.legacy=legacy;PacketOccupancyProbe.begin();long cpu=bean.getCurrentThreadCpuTime(),bytes=bean.getThreadAllocatedBytes(id),wall=System.nanoTime();
  try{ClientColumnRuntime.receiveWindow(packet,client);}finally{wall=System.nanoTime()-wall;cpu=bean.getCurrentThreadCpuTime()-cpu;bytes=bean.getThreadAllocatedBytes(id)-bytes;PacketOccupancyProbe.end();PacketOccupancyProbe.legacy=false;}
  return new Result(PacketOccupancyProbe.states,cpu,bytes,wall,List.copyOf(PacketOccupancyProbe.notifications));
 }
 private static TerrainWindowPacket packet(int x,int z,int min,int max,TerrainProfile server,PalettedContainerFactory factory,int[] sources,int[] edits){
  var tag=server.encode(factory);tag.putIntArray("sky_sources",sources.clone());return new TerrainWindowPacket(x,z,min,max,tag,edits,SectionOccupancy.create(server,min,max,edits));
 }
 public static void verify(Minecraft client,BlockPos camera)throws Exception{
  var level=client.level;var chunk=level.getChunk(camera.getX()>>4,camera.getZ()>>4,ChunkStatus.FULL,false);var sparse=(SparseColumn)chunk;var old=sparse.blockbranch$terrain();require(old!=null&&old.modified().length==0,"Disposable replay column has no saved edits");
  var previous=new TreeMap<>(sparse.blockbranch$sections());var factory=sparse.blockbranch$factory();var base=old.base();int center=camera.getY()>>4,min=center-34,max=center+34,x=chunk.getPos().x(),z=chunk.getPos().z();
  var server=TerrainProfile.decode(base.encode(factory),factory);var rows=new ArrayList<Map<String,Object>>();long serverFirst,serverReuse;
  PacketOccupancyProbe.begin();var initial=packet(x,z,min,max,server,factory,old.skySources,new int[0]);PacketOccupancyProbe.end();serverFirst=PacketOccupancyProbe.states;
  PacketOccupancyProbe.begin();for(int y:SectionOccupancy.targets(server,min,max))server.view(y).hasOnlyAir();PacketOccupancyProbe.end();serverReuse=PacketOccupancyProbe.states;
  require(serverFirst>4096&&serverReuse==0,"One server classification is reused by native-view lighting activation");
  // Exercise the actual registry-aware wire codec, including the appended summary.
  var wire=new RegistryFriendlyByteBuf(Unpooled.buffer(),level.registryAccess());try{TerrainWindowPacket.CODEC.encode(wire,initial);var round=TerrainWindowPacket.CODEC.decode(wire);require(!wire.isReadable()&&round.base().equals(initial.base())&&Arrays.equals(round.occupancy(),initial.occupancy()),"Native wire roundtrip exact");initial=round;}finally{wire.release();}
  try{
   var failingServer=TerrainProfile.decode(base.encode(factory),factory);var beforeFailure=failingServer.encode(factory);boolean scanFailed=false;PacketOccupancyProbe.begin();PacketOccupancyProbe.failStateAt=32;
   try{SectionOccupancy.create(failingServer,min,max,new int[0]);}catch(PacketOccupancyProbe.Injected expected){scanFailed=true;}finally{PacketOccupancyProbe.end();}
   require(scanFailed&&beforeFailure.equals(failingServer.encode(factory)),"Failed server scan publishes no wrong result or saved mutation");require(Arrays.equals(SectionOccupancy.create(failingServer,min,max,new int[0]),initial.occupancy()),"Retry after partial server memo produces exact summary");
   for(int pass=0;pass<6;pass++){
    Result legacy=null,candidate=null;
    for(int order=0;order<2;order++){boolean oldArm=(pass&1)==0?order==0:order!=0;var result=receive(client,initial,oldArm);if(oldArm)legacy=result;else candidate=result;}
    require(legacy.events.equals(candidate.events),"Exact ordered light/emptiness/dirty notifications");require(legacy.states==serverFirst&&candidate.states==0,"Fresh decoded client profiles use summary instead of repeating voxel work");
    rows.add(Map.of("pass",pass,"legacyStates",legacy.states,"candidateStates",candidate.states,"legacyCpuNs",legacy.cpu,"candidateCpuNs",candidate.cpu,"legacyBytes",legacy.bytes,"candidateBytes",candidate.bytes,"legacyWallNs",legacy.wall,"candidateWallNs",candidate.wall,"notifications",candidate.events));
   }
   for(int[] range:new int[][]{{min+1,max+1},{min-1,max-1},{center-8,center+8},{min,max},{ColumnLimits.MIN_Y>>4,(ColumnLimits.MIN_Y>>4)+1},{(ColumnLimits.MAX_Y>>4)-2,(ColumnLimits.MAX_Y>>4)-1}}){
    var next=packet(x,z,range[0],range[1],server,factory,old.skySources,new int[0]);receive(client,initial,false);var a=receive(client,next,true);receive(client,initial,false);var b=receive(client,next,false);require(a.events.equals(b.events)&&b.states==0,"Move/shrink/restore/build-bound callback oracle");
   }
   // A pending overlay deliberately advertises the same empty sentinel as before.
   int edited=center;var pending=packet(x,z,min,max,server,factory,old.skySources,new int[]{edited});receive(client,initial,false);var a=receive(client,pending,true);receive(client,initial,false);var b=receive(client,pending,false);require(a.events.equals(b.events)&&sparse.blockbranch$read(edited).hasOnlyAir(),"Pending edited occupancy remains the empty sentinel");
   var overlay=new LevelChunkSection(factory);overlay.setBlockState(0,0,0,Blocks.GLOWSTONE.defaultBlockState());ClientColumnRuntime.receiveColumn(ColumnPacket.create(x,z,edited,overlay),client);require(sparse.blockbranch$read(edited).getBlockState(0,0,0).is(Blocks.GLOWSTONE)&&!sparse.blockbranch$read(edited).hasOnlyAir(),"Ordered overlay becomes authoritative and luminous");
   receive(client,pending,false);require(sparse.blockbranch$read(edited).hasOnlyAir(),"Resent window restores pending state until overlay arrives");ClientColumnRuntime.receiveColumn(ColumnPacket.create(x,z,edited,overlay),client);require(!sparse.blockbranch$read(edited).hasOnlyAir(),"Repeated ordered overlay remains authoritative");
   for(String invalid:List.of("short","long","padding","sky","edited","owner","definition","window")){
    var tag=initial.base().copy();int badMin=min,badMax=max;var bits=initial.occupancy().clone();int[] edits=new int[0];
    switch(invalid){case "short"->bits=Arrays.copyOf(bits,bits.length-1);case "long"->bits=Arrays.copyOf(bits,bits.length+1);case "padding"->{if((SectionOccupancy.targets(base,min,max).length&7)!=0)bits[bits.length-1]|=(byte)128;else bits=Arrays.copyOf(bits,bits.length+1);}case "sky"->tag.putIntArray("sky_sources",new int[]{1});case "edited"->edits=new int[]{ColumnLimits.MAX_Y>>4};case "owner"->tag.getCompoundOrEmpty("underground").putInt("column_x",x+1);case "definition"->tag.putInt("version",999);case "window"->{badMin=Integer.MIN_VALUE;badMax=Integer.MAX_VALUE;}}
    var before=sparse.blockbranch$terrain();var sections=new TreeMap<>(sparse.blockbranch$sections());PacketOccupancyProbe.begin();boolean rejected=false;try{ClientColumnRuntime.receiveWindow(new TerrainWindowPacket(x,z,badMin,badMax,tag,edits,bits),client);}catch(RuntimeException expected){rejected=true;}finally{PacketOccupancyProbe.end();}
    require(rejected&&before==sparse.blockbranch$terrain()&&sections.equals(sparse.blockbranch$sections())&&PacketOccupancyProbe.notifications.isEmpty(),"Invalid packet rejected before any world mutation: "+invalid);
   }
   receive(client,initial,false);PacketOccupancyProbe.begin();PacketOccupancyProbe.failAt=7;boolean failed=false;try{ClientColumnRuntime.receiveWindow(initial,client);}catch(PacketOccupancyProbe.Injected expected){failed=true;}finally{PacketOccupancyProbe.end();}require(failed,"Native callback failure is propagated");var retry=receive(client,initial,false);require(retry.states==0&&retry.events.equals(((List<?>)rows.getFirst().get("notifications"))),"Retry completes unchanged final notification sequence");
   // Bounded profile memo and independent source descriptors; never cache edited state.
   var encoded=server.encode(factory);for(int y=-400;y<0;y++)server.sectionIsEmpty(y);var field=TerrainProfile.class.getDeclaredField("occupancyCache");field.setAccessible(true);synchronized(server){var cache=(Map<?,?>)field.get(server);require(cache!=null&&cache.size()<=256,"Profile occupancy cache has a hard256 entry cap");}
   require(encoded.equals(server.encode(factory)),"Runtime occupancy cache never changes saved definitions");
   var witness=witness(base,factory,min,max,initial.occupancy());for(int tries=0;tries<6&&witness.owner.get()!=null;tries++){System.gc();Thread.sleep(20);}require(witness.owner.get()==null,"Summary and memo cannot retain the profile/world owner");java.lang.ref.Reference.reachabilityFence(witness.summary);
   var pool=java.util.concurrent.Executors.newFixedThreadPool(4);try{var tasks=new ArrayList<java.util.concurrent.Callable<Boolean>>();for(int i=0;i<4;i++)tasks.add(()->{for(int y=min;y<=max;y++)require(server.sectionIsEmpty(y)==base.sectionIsEmpty(y),"Concurrent occupancy agreement");return true;});for(var f:pool.invokeAll(tasks))require(f.get(),"Occupancy reader finished");}finally{pool.shutdownNow();}
  }finally{PacketOccupancyProbe.end();PacketOccupancyProbe.legacy=false;for(int y:List.copyOf(sparse.blockbranch$sections().keySet()))sparse.blockbranch$replace(y,null);sparse.blockbranch$terrain(old);previous.forEach(sparse::blockbranch$replace);}
  var dir=Path.of(System.getProperty("blockbranch.caveApproachClientResult")).getParent();Files.writeString(dir.resolve("receiver-comparison.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("serverFirstStateEvaluations",serverFirst,"serverReuseStateEvaluations",serverReuse,"summaryBytes",initial.occupancy().length,"scenarios",rows,"callbackOracle","actual production receiver and native light/occupancy/dirty calls","overlaysMalformedFailureRetryConcurrencyBounds",true))+"\n");
 }
 private record Witness(SectionOccupancy summary,java.lang.ref.WeakReference<TerrainProfile> owner){}
 private static Witness witness(TerrainProfile base,PalettedContainerFactory factory,int min,int max,byte[] bits){var p=TerrainProfile.decode(base.encode(factory),factory);return new Witness(SectionOccupancy.decode(p,min,max,new int[0],bits),new java.lang.ref.WeakReference<>(p));}
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
