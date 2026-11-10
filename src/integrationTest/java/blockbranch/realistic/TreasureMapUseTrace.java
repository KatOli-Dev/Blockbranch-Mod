package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/** QA-only native getChunk trace; never changes which chunks the map requests. */
public final class TreasureMapUseTrace implements AutoCloseable {
 private static final ThreadLocal<TreasureMapUseTrace> CURRENT=new ThreadLocal<>();
 private record Entry(int x,int z,boolean loadedBefore){}
 private final Map<Long,Entry> chunks=new LinkedHashMap<>();
 private final Entity player;private final String filename;private long calls,nanos;
 private TreasureMapUseTrace(Entity player,String filename){this.player=player;this.filename=filename;CURRENT.set(this);}
 public static int warmRequestedView(ServerLevel level,Entity player,String filename,int radius)throws Exception{
  if(radius<=0)return 0;
  var trace=com.google.gson.JsonParser.parseString(Files.readString(Path.of(filename))).getAsJsonObject();int count=0;
  for(var value:trace.getAsJsonArray("chunks")){
   var chunk=value.getAsJsonObject();int x=chunk.get("x").getAsInt(),z=chunk.get("z").getAsInt();
   if(ChunkTrackingView.isInViewDistance(player.chunkPosition().x(),player.chunkPosition().z(),radius,x,z)){level.getChunk(x,z);count++;}
  }
  return count;
 }
 public static TreasureMapUseTrace open(Entity player,String filename){return new TreasureMapUseTrace(player,filename);}
 public static LevelChunk chunk(Level level,int x,int z,Supplier<LevelChunk> nativeCall){
  var trace=CURRENT.get();if(trace==null)return nativeCall.get();
  trace.calls++;long key=(x&0xffffffffL)|((long)z<<32);
  trace.chunks.computeIfAbsent(key,ignored->new Entry(x,z,((ServerLevel)level).getChunkSource().hasChunk(x,z)));
  long start=System.nanoTime();try{return nativeCall.get();}finally{trace.nanos+=System.nanoTime()-start;}
 }
 @Override public void close(){CURRENT.remove();try{
  int cx=player.chunkPosition().x(),cz=player.chunkPosition().z(),max=0,out6=0,out8=0;long loaded=0;
  for(var chunk:chunks.values()){
   max=Math.max(max,Math.max(Math.abs(chunk.x-cx),Math.abs(chunk.z-cz)));if(chunk.loadedBefore)loaded++;
   if(!ChunkTrackingView.isInViewDistance(cx,cz,6,chunk.x,chunk.z))out6++;
   if(!ChunkTrackingView.isInViewDistance(cx,cz,8,chunk.x,chunk.z))out8++;
  }
  var report=new LinkedHashMap<String,Object>();report.put("playerChunk",List.of(cx,cz));report.put("nativeChunkCalls",calls);report.put("uniqueChunks",chunks.size());report.put("alreadyFullAtFirstRequest",loaded);report.put("getChunkNanos",nanos);report.put("maxChebyshevChunkDistance",max);report.put("outsideNativeViewDistance6",out6);report.put("outsideNativeViewDistance8",out8);report.put("chunks",chunks.values());
  Files.writeString(Path.of(filename),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
 }catch(Exception e){throw new RuntimeException(e);}}
}
