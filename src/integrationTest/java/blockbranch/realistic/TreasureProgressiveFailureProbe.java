package blockbranch.realistic;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;

/** QA-only failed-future plus one unavailable-readiness observation, without editing any chunk. */
public final class TreasureProgressiveFailureProbe {
 private static ServerChunkCache source;private static ChunkPos pos;private static boolean injected,hidden,verified,seeded;private static int deniedTicks,waitingTicks;private static Object heldState,heldJob;
 private static Object read(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
 public static boolean hide(ServerChunkCache cache,int x,int z){
  if(heldState!=null&&deniedTicks==4&&!injected&&ProgressiveMapDrawing.current()!=null)throw new AssertionError("Native prefix was resampled while its FULL promise remained pending");
  if(Boolean.getBoolean("blockbranch.treasureProgressiveWarmFixture")&&!seeded&&(TreasureConnectedProbe.caseIndex==0||TreasureConnectedProbe.REOPEN)&&(TreasureConnectedProbe.phase.endsWith("update")||TreasureConnectedProbe.phase.endsWith("observe")||TreasureConnectedProbe.phase.equals("reopen-progressive"))&&ProgressiveMapDrawing.current()!=null&&ProgressiveMapDrawing.current().allowed){seeded=true;return true;}
  if(source==cache&&pos!=null&&!hidden&&ProgressiveMapDrawing.current()!=null&&pos.x()==x&&pos.z()==z){hidden=true;return true;}return false;
 }
 @SuppressWarnings("unchecked") public static void start(MinecraftServer server){
  if(heldState!=null&&deniedTicks<4&&!injected){try{((java.util.Set<?>)read(heldState,"allowed")).clear();deniedTicks++;}catch(Exception e){throw new RuntimeException(e);}}
 }
 static void check(MinecraftServer server)throws Exception{
  if(verified)return;
  if(injected){
   WellInteraction.require(hidden,"Failure fixture exercised production non-ready lookup");
   var audit=ProgressiveMapDrawing.audit(server);WellInteraction.require(audit[2]==0&&audit[3]==0,"Failed map future releases owned tickets and reference");
   verified=true;source=null;pos=null;heldState=heldJob=null;return;
  }
  if(!TreasureConnectedProbe.REOPEN&&(TreasureConnectedProbe.caseIndex!=0||!TreasureConnectedProbe.phase.endsWith("observe")))return;
  Field all=ProgressiveMapDrawing.class.getDeclaredField("SERVERS");all.setAccessible(true);
  Object state=((Map<?,?>)all.get(null)).get(server);if(state==null)return;Object job=read(state,"pending");if(job==null||read(job,"future")==null)return;
  if(heldState==null){heldState=state;heldJob=job;Field future=job.getClass().getDeclaredField("future");future.setAccessible(true);future.set(job,new CompletableFuture<>());return;}
  if(deniedTicks<4)return;
  WellInteraction.require(job==heldJob,"Held map retains owner across four denied admissions");
  if(waitingTicks++<2)return; // admitted ticks must wait for the promise without resampling/releasing its owner
  pos=(ChunkPos)read(job,"pos");source=((net.minecraft.server.level.ServerLevel)read(job,"level")).getChunkSource();
  Field future=job.getClass().getDeclaredField("future");future.setAccessible(true);future.set(job,CompletableFuture.failedFuture(new IllegalStateException("QA controlled map request failure")));injected=true;
 }
 static boolean verified(){return verified;}
 private TreasureProgressiveFailureProbe(){}
}
