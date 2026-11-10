package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.saveddata.maps.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.*;
import com.mojang.serialization.JsonOps;

/** One connected native fixture, exact native reference and actual save/reopen. */
public final class DeferredPreviewQualification {
 public static final boolean ON=Boolean.getBoolean("blockbranch.deferredPreview"),REOPEN=Boolean.getBoolean("blockbranch.deferredPreviewReopen"),ADOPTION=Boolean.getBoolean("blockbranch.deferredPreviewAdoption");
 public static volatile boolean done,clientExact;public static volatile Throwable failure;public static volatile int mapId=-1;public static volatile String phase="startup",hash="";
 public static boolean reference,policy;public static volatile boolean failOnce;public static int queries;public static long queryMax,pendingReads;
 public static final java.util.concurrent.atomic.AtomicLong workerQueries=new java.util.concurrent.atomic.AtomicLong(),workerCpu=new java.util.concurrent.atomic.AtomicLong(),workerWall=new java.util.concurrent.atomic.AtomicLong(),workerMax=new java.util.concurrent.atomic.AtomicLong();
 private static final List<Long> walls=new ArrayList<>(),cpus=new ArrayList<>();private static long totalQueries,queueWall,referenceWall,referenceCpu;private static int maxQueries,maxJobs;
 private static long tick,phaseTick;private static ServerPlayer player;private static ItemStack stack;private static MapItemSavedData data;private static byte[] pause;private static int completed;private static boolean sawRetryRelease;
 public static Path dir(){return Path.of(System.getProperty("blockbranch.deferredPreviewDir"));}
 @jdk.jfr.Name("blockbranch.DeferredPreviewTick") @jdk.jfr.Label("Deferred map preview tick")
 public static class TickEvent extends jdk.jfr.Event {public int queries;public long cpuNanos;public String phase;}
 @jdk.jfr.Name("blockbranch.PreviewFallback") @jdk.jfr.Threshold("10 ms")
 public static class QueryEvent extends jdk.jfr.Event {public long cpuNanos;public int x,z;}
 @jdk.jfr.Name("blockbranch.PreviewStage") @jdk.jfr.Threshold("5 ms")
 public static class StageEvent extends jdk.jfr.Event {public String stage;public long cpuNanos;}
 private static final java.lang.management.ThreadMXBean CPU=java.lang.management.ManagementFactory.getThreadMXBean();
 public static long cpu(){return CPU.getCurrentThreadCpuTime();}
 public static void tickMeasured(long wall,long cpu){if(!ON)return;if(queries>0&&!policy){walls.add(wall);cpus.add(cpu);totalQueries+=queries;maxQueries=Math.max(maxQueries,queries);}}
 public static void register(){
  ServerLifecycleEvents.SERVER_STOPPED.register(server->{if(!ON)return;check(DeferredMapPreview.audit(server)[0]==0,"Shutdown releases server and holders");});
  ServerTickEvents.END_SERVER_TICK.register(server->{if(done||server.isDedicatedServer())return;try{
   tick++;if(tick>16000)throw new AssertionError("Bounded fixture timeout: "+phase);var level=server.overworld();
   maxJobs=Math.max(maxJobs,DeferredMapPreview.audit(server)[0]);check(maxJobs<=8&&maxQueries<=64,"Production resource bounds");
   if(player==null){if(server.getPlayerList().getPlayers().isEmpty()||tick<60)return;player=server.getPlayerList().getPlayers().getFirst();
    check(level.getSeed()==424242,"Fixture seed");check(PreviewBiomeQueries.workerResolver(level)!=null,"Actual seeded fallback uses worker path");codecChecks(level);PreviewNativeStorageChecks.check(level);compatibility(level);
    PreviewNativeFutureChecks.check(level,player);
    if(ADOPTION){DeferredPreviewPolicyChecks.check(level,player);MapPreviewScopeQualification.check(level);writeReport();done=true;phase="done";return;}
    if(REOPEN){var saved=com.google.gson.JsonParser.parseString(Files.readString(dir().resolve("pending.json"))).getAsJsonObject();
     stack=player.getInventory().getItem(0);player.getInventory().setSelectedSlot(0);data=MapItem.getSavedData(stack,level);check(data!=null&&DeferredMapPreview.pending(data),"Actual pending marker reopened");check(hash(data.colors).equals(saved.get("pixels").getAsString()),"Exact partial native pixels reopened");check(((PendingMapPreview)data).blockbranch$previewRows()==saved.get("rows").getAsInt(),"Actual saved row progress reopened");mapId=stack.get(DataComponents.MAP_ID).id();check(mapId==saved.get("mapId").getAsInt(),"Native inventory map identity reopened");phase="reopened";return;
    }
    stack=MapItem.create(level,-504768,-1657792,(byte)1,true,true);data=MapItem.getSavedData(stack,level);mapId=stack.get(DataComponents.MAP_ID).id();
    long start=System.nanoTime();MapItem.renderBiomePreviewMap(level,stack);queueWall=System.nanoTime()-start;check(DeferredMapPreview.pending(data),"Native creation defers outline");
    MapItemSavedData.addTargetDecoration(stack,new net.minecraft.core.BlockPos(-504700,64,-1657700),"+",net.minecraft.world.level.saveddata.maps.MapDecorationTypes.RED_X);
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);phase="cold";return;
   }
   if(phase.equals("cold")&&DeferredMapPreview.audit(server)[1]>=15360){
    check(DeferredMapPreview.pending(data),"Partial still pending");pause=data.colors.clone();
    check(!Arrays.equals(pause,new byte[16384]),"Rows become visible before completion");
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);phase="paused";phaseTick=tick;return;
   }
   if(phase.equals("paused")){
    check(Arrays.equals(pause,data.colors),"Put-away retains exact partial pixels");check(DeferredMapPreview.audit(server)[0]==0,"Put-away releases jobs");
    if(tick-phaseTick<20)return;player.setPos(player.getX()+2,player.getY(),player.getZ());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);failOnce=true;phase="retry";phaseTick=tick;return;
   }
   if(phase.equals("retry")&&!sawRetryRelease&&!failOnce&&DeferredMapPreview.audit(server)[0]==0){check(DeferredMapPreview.pending(data)&&DeferredMapPreview.audit(server)[0]==0,"Failure releases sampling state and preserves pending");check(Arrays.equals(pause,data.colors),"Failure preserves completed rows");sawRetryRelease=true;}
   if((phase.equals("retry")||phase.equals("reopened"))&&!DeferredMapPreview.pending(data)){
    if(!REOPEN)check(sawRetryRelease,"Actual failed worker was observed released before recovery");byte[] actual=data.colors.clone();hash=hash(actual);
    var baseline=MapItem.create(level,data.centerX,data.centerZ,data.scale,true,true);var expected=MapItem.getSavedData(baseline,level);
    long cpu=cpu(),start=System.nanoTime();reference=true;try{MapItem.renderBiomePreviewMap(level,baseline);}finally{reference=false;}referenceWall=System.nanoTime()-start;referenceCpu=cpu()-cpu;
    check(Arrays.equals(actual,expected.colors),"Exact 16384 native pixels after deferred draw, pause/retry or reopening");
    check(hash.equals("f298e50bbe474b7d196a1accd8eb6ea6a270f2f1e8de1ba3d013afa8cb95d218"),"Retained native fixed-map golden/orientation");
    completed++;phase="packets";phaseTick=tick;return;
   }
   if(phase.equals("packets")){
    check(Arrays.equals(data.colors,MapItem.getSavedData(stack,level).colors),"Live map identity");if(!clientExact||tick-phaseTick<20)return;
    if(REOPEN){DeferredPreviewPolicyChecks.check(level,player);MapPreviewScopeQualification.check(level);writeReport();done=true;phase="done";return;}
    editsAndCopies(level); // Actual production guards, without a second expensive whole-world comparison.
    stack=MapItem.create(level,-504768,-1657792,(byte)1,true,true);data=MapItem.getSavedData(stack,level);MapItem.renderBiomePreviewMap(level,stack);mapId=stack.get(DataComponents.MAP_ID).id();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);phase="save-partial";return;
   }
   if(phase.equals("save-partial")&&DeferredMapPreview.audit(server)[1]>=15360){
    player.getInventory().setSelectedSlot(1);hash=hash(data.colors);Files.writeString(dir().resolve("pending.json"),new com.google.gson.Gson().toJson(Map.of("mapId",mapId,"pixels",hash,"rows",((PendingMapPreview)data).blockbranch$previewRows())));writeReport();done=true;phase="done";
   }
  }catch(Throwable t){try{writeReport();}catch(Exception ignored){}failure=t;done=true;blockbranch.Blockbranch.LOGGER.error("DEFERRED_PREVIEW_FAIL",t);try{Files.writeString(dir().resolve("result.txt"),"FAIL "+t);}catch(Exception ignored){}}});
 }
 private static void codecChecks(ServerLevel level){
  var ops=net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());var map=MapItemSavedData.createFresh(7,-31,(byte)2,true,true,level.dimension());
  map.setColor(2,3,(byte)45);DeferredMapPreview.mark(map,true);
  var encoded=MapItemSavedData.CODEC.encodeStart(ops,map).getOrThrow().getAsJsonObject();var decoded=MapItemSavedData.CODEC.parse(ops,encoded).getOrThrow();check(DeferredMapPreview.pending(decoded)&&Arrays.equals(map.colors,decoded.colors),"Pending native codec roundtrip");
  var invalid=encoded.deepCopy();invalid.addProperty("blockbranch_pending_preview",2);check(MapItemSavedData.CODEC.parse(ops,invalid).isError(),"Future preview marker rejected");invalid.addProperty("blockbranch_pending_preview",1.5);check(MapItemSavedData.CODEC.parse(ops,invalid).isError(),"Fractional marker rejected");invalid.addProperty("blockbranch_pending_preview",4294967297L);check(MapItemSavedData.CODEC.parse(ops,invalid).isError(),"Overflow marker rejected");invalid.addProperty("blockbranch_pending_preview","broken");check(MapItemSavedData.CODEC.parse(ops,invalid).isError(),"Malformed marker rejected");
  encoded.remove("blockbranch_pending_preview");check(!DeferredMapPreview.pending(MapItemSavedData.CODEC.parse(ops,encoded).getOrThrow()),"Legacy absent marker remains complete");
  check(!DeferredMapPreview.pending(map.locked())&&Arrays.equals(map.colors,map.locked().colors),"Lock freezes partial pixels without future writes");
  check(!DeferredMapPreview.pending(map.scaled()),"Native scale remains blank ordinary map");
  map.setColor(4,5,(byte)17);check(!DeferredMapPreview.pending(map)&&map.colors[4+128*5]==17,"External native edit cancels pending without overwrite");
 }
 private static void editsAndCopies(ServerLevel level){
  var edit=MapItem.create(level,300,-200,(byte)0,true,true);var d=MapItem.getSavedData(edit,level);MapItem.renderBiomePreviewMap(level,edit);d.updateColor(6,7,(byte)27);check(!DeferredMapPreview.pending(d)&&d.colors[6+7*128]==27,"updateColor production cancellation");
  var before=data.colors.clone();var oracle=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PreviewOracle"),ClientInformation.createDefault());oracle.setPos(0,80,0);
  DeferredMapPreview.mark(data,true);((MapItem)stack.getItem()).update(level,oracle,data);check(Arrays.equals(before,data.colors),"Pending guard precedes carried update");DeferredMapPreview.mark(data,false);
 }
 private static void compatibility(ServerLevel level)throws Exception{
  var ops=net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());var saved=RealisticGenerator.CODEC.codec().encodeStart(ops,(RealisticGenerator)level.getChunkSource().getGenerator()).getOrThrow().getAsJsonObject();check(saved.get("shape_version").getAsInt()==92&&!saved.has("shipwrecks"),"Native legacy92 remains opted out");
  try(var in=DeferredPreviewQualification.class.getClassLoader().getResourceAsStream("data/blockbranch/worldgen/world_preset/realistic.json")){
   var input=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in)).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");var result=RealisticGenerator.CODEC.codec().encodeStart(ops,RealisticGenerator.CODEC.codec().parse(ops,input).getOrThrow()).getOrThrow().getAsJsonObject();for(String key:List.of("shape_version","small_discovery_definition","desert_waygates","shipwrecks"))check(input.get(key).equals(result.get(key)),"Current93 metadata "+key);
   var v1=input.deepCopy();v1.getAsJsonObject("shipwrecks").addProperty("definition",1);var retained=RealisticGenerator.CODEC.codec().encodeStart(ops,RealisticGenerator.CODEC.codec().parse(ops,v1).getOrThrow()).getOrThrow().getAsJsonObject();check(retained.getAsJsonObject("shipwrecks").get("definition").getAsInt()==1,"Existing schema93 v1 shipwrecks remain v1");
  }
 }
 private static void writeReport()throws Exception{var out=new LinkedHashMap<String,Object>();out.put("reopen",REOPEN);out.put("queueWallNanos",queueWall);out.put("nativeReferenceWallNanos",referenceWall);out.put("nativeReferenceCpuNanos",referenceCpu);out.put("queryMaxNanos",queryMax);out.put("workerQueries",workerQueries.get());out.put("workerCpuNanos",workerCpu.get());out.put("workerWallNanos",workerWall.get());out.put("workerMaxQueryNanos",workerMax.get());out.put("queries",totalQueries);out.put("maxQueriesPerTick",maxQueries);out.put("maxJobs",maxJobs);out.put("activeTickWallNanos",walls);out.put("activeTickCpuNanos",cpus);out.put("completedNativePixelComparisons",completed);Files.writeString(dir().resolve(REOPEN?"reopen-server.json":"server.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(out));}
 public static String hash(byte[] bytes){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new RuntimeException(e);}}
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 private DeferredPreviewQualification(){}
}
