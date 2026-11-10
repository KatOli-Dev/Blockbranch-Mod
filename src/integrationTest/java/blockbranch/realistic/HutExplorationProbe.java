package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;

/** QA only: registered player movement owns chunk admission; no direct chunk loads. */
public final class HutExplorationProbe {
 public static final boolean ON=Boolean.getBoolean("blockbranch.hutExplore");
 public static volatile String phase="startup";public static volatile boolean ready,done;public static volatile Throwable failure;
 public static volatile double startX,startZ;public static volatile long heartbeat=System.nanoTime();
 public static final List<Map<String,Object>> calls=new CopyOnWriteArrayList<>();
 public static volatile Thread serverThread;public static final java.util.concurrent.atomic.AtomicInteger active=new java.util.concurrent.atomic.AtomicInteger();public static final List<Map<String,Object>> serverSamples=new CopyOnWriteArrayList<>();
 private static int ticks,phaseTick;private static long beginTick,maxMovingTick;private static net.minecraft.server.level.ServerPlayer player;
 public static Path dir(){return Path.of(System.getProperty("blockbranch.hutExploreDir"));}
 public static void prepare(){ServerLifecycleEvents.SERVER_STARTED.register(server->{server.saveEverything(false,true,true);server.halt(false);});}
 public static void record(int lake,String phase,String thread,StackTraceElement[] stack,long wall,long cpu,long bytes,boolean accepted){var row=new LinkedHashMap<String,Object>();row.put("lake",lake);row.put("phase",phase);row.put("thread",thread);row.put("wallNanos",wall);row.put("threadCpuNanos",cpu);row.put("threadAllocatedBytes",bytes);row.put("accepted",accepted);row.put("stack",Arrays.stream(stack).map(Object::toString).toList());calls.add(row);blockbranch.Blockbranch.LOGGER.info("HUT_COLD_REGION lake={} phase={} thread={} wallMs={}",lake,phase,thread,wall/1e6);}
 public static void register(){
  ServerTickEvents.START_SERVER_TICK.register(server->{serverThread=Thread.currentThread();beginTick=System.nanoTime();});
  ServerLifecycleEvents.SERVER_STARTED.register(server->{var sampler=new Thread(()->{while(!done){try{Thread.sleep(10);}catch(InterruptedException e){return;}var thread=serverThread;if(active.get()>0&&thread!=null&&serverSamples.size()<512)serverSamples.add(Map.of("phase",phase,"state",thread.getState().toString(),"stack",Arrays.stream(thread.getStackTrace()).map(Object::toString).toList()));}},"Hut QA bounded stack sampler");sampler.setDaemon(true);sampler.start();});
  ServerTickEvents.END_SERVER_TICK.register(server->{heartbeat=System.nanoTime();if(done||server.isDedicatedServer())return;try{
   ticks++;if(server.getPlayerList().getPlayers().isEmpty())return;
   var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var huts=g.wetlandHuts(level.getChunkSource().randomState());
   if(player==null){
    player=server.getPlayerList().getPlayers().getFirst();if(Boolean.getBoolean("blockbranch.hutExploreLocateOnly")){phase="post-movement";phaseTick=ticks-21;return;}var d=g.hutPlanningTerrain(level.getChunkSource().randomState()).drainage();int lake=899615;double largest=0;for(int a=0;a<64;a++)largest=Math.max(largest,d.lakeShoreRadius(lake,a*Math.PI/32));startX=d.nodeX(lake)-(int)Math.ceil(largest)-16-320;startZ=d.nodeZ(lake);
    require(!huts.canSave(),"Cold partial cache");
    var command=server.getCommands();var source=server.createCommandSourceStack();command.performPrefixedCommand(source,"gamemode creative "+player.getName().getString());phase="settle";command.performPrefixedCommand(source,"tp "+player.getName().getString()+" "+startX+" 900 "+startZ+" -90 0");player.getAbilities().flying=true;player.onUpdateAbilities();phaseTick=ticks;return;
   }
   if(phase.equals("settle")&&ready&&ticks-phaseTick>80){require(calls.stream().noneMatch(r->r.get("lake").equals(899615)),"Target region remains cold before movement");phase="movement";phaseTick=ticks;}
   else if(phase.equals("movement")){
    maxMovingTick=Math.max(maxMovingTick,System.nanoTime()-beginTick);
    if(calls.stream().anyMatch(r->r.get("lake").equals(899615))){require(player.getX()>startX+32,"Actual movement packets reached server");phase="post-movement";phaseTick=ticks;}
   }else if(phase.equals("post-movement")&&ticks-phaseTick>20){
    phase="locate";boolean[] located={false};long before=System.nanoTime();server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withCallback((success,result)->located[0]=success),"execute positioned 139720 483 733576 run locate structure minecraft:swamp_hut");long locate=System.nanoTime()-before;require(located[0],"Native locate command succeeds at frozen lake 708821");require(calls.stream().anyMatch(r->r.get("lake").equals(708821)&&r.get("phase").equals("locate")),"Native locate evaluates cold frozen region");
    var out=new LinkedHashMap<String,Object>();out.put("seed",-17);out.put("nativeConnectedPlayer",true);out.put("locateOrigin",List.of(139720,483,733576));out.put("locateOnly",Boolean.getBoolean("blockbranch.hutExploreLocateOnly"));out.put("start",List.of(startX,900,startZ));out.put("serverEndX",player.getX());out.put("maxMovementServerTickNanos",maxMovingTick);out.put("nativeLocateCommandNanos",locate);out.put("coldCalls",calls);out.put("serverSamplesDuringColdWork",serverSamples);out.put("resolvedRegions",huts.resolvedRegions());out.put("partialCacheAbsent",!huts.canSave());
    Files.writeString(dir().resolve("server-results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(out));server.saveEverything(false,true,true);phase="done";done=true;
   }
   if(ticks%100==0)Files.writeString(dir().resolve("phase.txt"),phase+" "+ticks+" "+player.getX());
  }catch(Throwable t){failure=t;done=true;blockbranch.Blockbranch.LOGGER.error("HUT_EXPLORE_FAIL",t);}});
 }
 private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
}
