package blockbranch.realistic;

import java.util.*;
import java.nio.file.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.saveddata.maps.*;

/** Controlled owners exercise the production scheduler directly; not a multi-TCP-client claim. */
public final class DeferredPreviewPolicyChecks {
 public static volatile boolean synthetic;
 private static Holder<Biome> water,land;
 public static volatile PreviewBiomeQueries.Point heldPoint,overridePoint;
 public static volatile boolean pendingRead;
 public static Holder<Biome> override(PreviewBiomeQueries.Point p){return p.equals(overridePoint)?(quart(p)==water?land:water):null;}

 public static volatile java.util.concurrent.CountDownLatch entered,release;
 public static void awaitWorker(){var gate=release;if(gate!=null){entered.countDown();try{if(!gate.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("Controlled worker gate timed out");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}}

 public static Holder<Biome> quart(PreviewBiomeQueries.Point point){return Math.floorMod(point.x()*31L+point.z()*17L,13)<6?water:land;}
 public static Holder<Biome> sample(ServerLevel level,BlockPos pos){
  return synthetic?level.getBiomeManager().withDifferentSource((x,y,z)->{var p=new PreviewBiomeQueries.Point(x,y,z);var edited=override(p);return edited==null?quart(p):edited;}).getBiome(pos):null;
 }
 static void check(ServerLevel level,ServerPlayer connected)throws Exception{
  var server=level.getServer();var tick=DeferredMapPreview.class.getDeclaredMethod("tick",net.minecraft.server.MinecraftServer.class);tick.setAccessible(true);
  var added=new ArrayList<ServerPlayer>();var stacks=new ArrayList<ItemStack>();var datas=new ArrayList<MapItemSavedData>();
  var held=connected.getMainHandItem();connected.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
  DeferredPreviewQualification.policy=true;water=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.OCEAN);land=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.BEACH);synthetic=true;
  try{
   for(int i=0;i<6;i++){
    var p=new ServerPlayer(server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PreviewPolicy"+i),ClientInformation.createDefault());added.add(p);server.getPlayerList().getPlayers().add(p);
    for(int hand=0;hand<2;hand++){
     var stack=MapItem.create(level,(i-3)*2048,(hand==0?-1:1)*2048,(byte)(i%5),true,true);MapItem.renderBiomePreviewMap(level,stack);stacks.add(stack);datas.add(MapItem.getSavedData(stack,level));p.setItemInHand(hand==0?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND,stack);
    }
   }
   tick.invoke(null,server);require(DeferredMapPreview.audit(server)[0]==8,"Eight active sample arrays despite twelve held maps");
   // Direct array mutation while active is detected before the next sampling/raster pass.
   datas.getFirst().colors[0]=77;for(int n=0;n<64&&DeferredMapPreview.pending(datas.getFirst());n++){tick.invoke(null,server);Thread.sleep(1);require(datas.getFirst().colors[0]==77,"Edited pixel never overwritten while cancellation awaits its turn");}require(!DeferredMapPreview.pending(datas.getFirst())&&datas.getFirst().colors[0]==77,"Direct active edit cancels rather than overwrites");
   // Put-away and admission across owners. Failure backoff frees capacity for a waiting map.
   added.get(1).setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);DeferredPreviewQualification.failOnce=true;
   for(int i=0;i<1000&&(DeferredPreviewQualification.failOnce||DeferredMapPreview.audit(server)[2]>0);i++){tick.invoke(null,server);Thread.sleep(1);}
   require(!DeferredPreviewQualification.failOnce,"Worker failure injection exercised");
   for(var d:datas)((PendingMapPreview)d).blockbranch$previewRetry(0); // Controlled clock seam; real 10-second retry is tested by the connected fixture.
   added.get(1).setItemInHand(InteractionHand.MAIN_HAND,stacks.get(2));
   int passes=0;while(datas.stream().anyMatch(DeferredMapPreview::pending)&&passes++<15000){tick.invoke(null,server);Thread.sleep(1);require(DeferredPreviewQualification.queries<=64,"Attempt budget includes failed and pending lookups");}
   require(datas.stream().noneMatch(DeferredMapPreview::pending),"All waiting maps and both hands finish fairly");
   require(DeferredMapPreview.audit(server)[0]==0,"Finished jobs release sample arrays and caches");
   for(int i=1;i<datas.size();i++){
    var d=datas.get(i);var ref=MapItem.create(level,d.centerX,d.centerZ,d.scale,true,true);DeferredPreviewQualification.reference=true;
    try{MapItem.renderBiomePreviewMap(level,ref);}finally{DeferredPreviewQualification.reference=false;}
    require(Arrays.equals(d.colors,MapItem.getSavedData(ref,level).colors),"Native exact raster/orientation scale "+d.scale+" signed coordinates "+i);
   }
   // Native copied stacks share one map identity/job; a locked copy owns independent frozen pixels.
   var shared=MapItem.create(level,9000,-11000,(byte)1,true,true);MapItem.renderBiomePreviewMap(level,shared);
   var sharedData=MapItem.getSavedData(shared,level);var cloned=shared.copy();
   require(MapItem.getSavedData(cloned,level)==sharedData,"Native stack clone shares saved map identity");
   added.getFirst().setItemInHand(InteractionHand.MAIN_HAND,shared);added.getFirst().setItemInHand(InteractionHand.OFF_HAND,cloned);
   added.get(1).setItemInHand(InteractionHand.MAIN_HAND,cloned.copy());tick.invoke(null,server);
   require(DeferredMapPreview.audit(server)[0]==1,"Shared clones do not duplicate jobs");
   var locked=sharedData.locked();var frozen=locked.colors.clone();
   added.getFirst().setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
   for(int n=0;n<96;n++){tick.invoke(null,server);Thread.sleep(1);}
   require(DeferredMapPreview.audit(server)[0]==1&&!Arrays.equals(sharedData.colors,new byte[16384]),"Holding only clones continues visible partial drawing");
   require(!DeferredMapPreview.pending(locked)&&Arrays.equals(frozen,locked.colors),"Lock stays frozen while original/clone progresses");
   added.getFirst().setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);added.get(1).setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);tick.invoke(null,server);
   for(int n=0;n<1000&&DeferredMapPreview.audit(server)[2]>0;n++){tick.invoke(null,server);Thread.sleep(1);}
   require(DeferredMapPreview.audit(server)[0]==0&&DeferredMapPreview.audit(server)[2]==0,"Last clone put away releases job and drains work");
   Files.writeString(DeferredPreviewQualification.dir().resolve("clones-lock.json"),"{\"sharedIdentity\":true,\"singleJob\":true,\"clonesContinue\":true,\"lockFrozen\":true,\"lastCloneCleanup\":true}");
   // Each held session is shorter than a full preview; saved progress must still reach completion.
   var intermittent=MapItem.create(level,-13000,15000,(byte)1,true,true);MapItem.renderBiomePreviewMap(level,intermittent);
   var intermittentData=MapItem.getSavedData(intermittent,level);int sessions=0,previousRows=0;
   var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
   while(DeferredMapPreview.pending(intermittentData)&&sessions++<25){
    added.getFirst().setItemInHand(InteractionHand.MAIN_HAND,intermittent);
    for(int n=0;n<96&&DeferredMapPreview.pending(intermittentData);n++){tick.invoke(null,server);Thread.sleep(1);}
    added.getFirst().setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);tick.invoke(null,server);
    for(int n=0;n<1000&&DeferredMapPreview.audit(server)[2]>0;n++){tick.invoke(null,server);Thread.sleep(1);}
    if(DeferredMapPreview.pending(intermittentData)){
     int rows=((PendingMapPreview)intermittentData).blockbranch$previewRows();require(rows>previousRows,"Short held sessions make cumulative row progress");previousRows=rows;
     var record=MapItemSavedData.CODEC.encodeStart(ops,intermittentData).getOrThrow();var read=MapItemSavedData.CODEC.parse(ops,record).getOrThrow();
     require(((PendingMapPreview)read).blockbranch$previewRows()==rows&&Arrays.equals(read.colors,intermittentData.colors),"Partial row cursor and pixels save together");
    }
   }
   require(!DeferredMapPreview.pending(intermittentData)&&sessions>1,"Intermittent holds eventually finish");
   var intermittentRef=MapItem.create(level,intermittentData.centerX,intermittentData.centerZ,intermittentData.scale,true,true);DeferredPreviewQualification.reference=true;
   try{MapItem.renderBiomePreviewMap(level,intermittentRef);}finally{DeferredPreviewQualification.reference=false;}
   require(Arrays.equals(intermittentData.colors,MapItem.getSavedData(intermittentRef,level).colors),"Exact native pixels across interrupted row/halo resumes");
   Files.writeString(DeferredPreviewQualification.dir().resolve("intermittent.json"),new com.google.gson.Gson().toJson(Map.of("sessions",sessions,"ticksPerSession",96,"cumulativeProgress",true,"codecProgress",true,"exactNativePixels",true)));
   // A loaded/saved override arrives while fallback work is already in flight.
   entered=new java.util.concurrent.CountDownLatch(1);release=new java.util.concurrent.CountDownLatch(1);heldPoint=null;
   var late=MapItem.create(level,13000,-17000,(byte)0,true,true);MapItem.renderBiomePreviewMap(level,late);var lateData=MapItem.getSavedData(late,level);added.getFirst().setItemInHand(InteractionHand.MAIN_HAND,late);tick.invoke(null,server);
   require(entered.await(2,java.util.concurrent.TimeUnit.SECONDS),"Fallback dispatched before late loaded override");overridePoint=heldPoint;pendingRead=true;release.countDown();release=null;
   for(int n=0;n<1000&&DeferredMapPreview.audit(server)[2]>0;n++){tick.invoke(null,server);Thread.sleep(1);}
   require(sampleZero(server,lateData)==null,"Pending native BIOMES result never becomes fallback pixels prematurely");pendingRead=false;
   for(int n=0;n<100&&sampleZero(server,lateData)==null;n++){tick.invoke(null,server);Thread.sleep(1);}
   require(sampleZero(server,lateData)==override(overridePoint),"Authoritative late loaded override supersedes computed fallback");
   for(int n=0;n<2000&&DeferredMapPreview.pending(lateData);n++){tick.invoke(null,server);Thread.sleep(1);}require(!DeferredMapPreview.pending(lateData),"Late override map completes");
   var lateRef=MapItem.create(level,lateData.centerX,lateData.centerZ,lateData.scale,true,true);DeferredPreviewQualification.reference=true;try{MapItem.renderBiomePreviewMap(level,lateRef);}finally{DeferredPreviewQualification.reference=false;}
   require(Arrays.equals(lateData.colors,MapItem.getSavedData(lateRef,level).colors),"Exact native pixels with late loaded override");overridePoint=null;
   Files.writeString(DeferredPreviewQualification.dir().resolve("late-loaded.json"),"{\"pendingReadWaited\":true,\"fallbackReplacedByLoadedOverride\":true,\"exactNativePixels\":true}");
   // Hold the actual pure worker at an owned QA gate, replace its player/world, then deliver its result.
   entered=new java.util.concurrent.CountDownLatch(1);release=new java.util.concurrent.CountDownLatch(1);
   var stale=MapItem.create(level,7000,-9000,(byte)1,true,true);MapItem.renderBiomePreviewMap(level,stale);var staleData=MapItem.getSavedData(stale,level);var untouched=staleData.colors.clone();
   var old=added.getFirst();old.setItemInHand(InteractionHand.MAIN_HAND,stale);tick.invoke(null,server);
   require(entered.await(2,java.util.concurrent.TimeUnit.SECONDS),"Actual preview worker held at controlled boundary");
   server.getPlayerList().getPlayers().remove(old);
   var replacement=new ServerPlayer(server,server.getLevel(net.minecraft.world.level.Level.NETHER),old.getGameProfile(),ClientInformation.createDefault());replacement.setItemInHand(InteractionHand.MAIN_HAND,stale);added.add(replacement);server.getPlayerList().getPlayers().add(replacement);
   tick.invoke(null,server);require(DeferredMapPreview.audit(server)[0]==0&&DeferredMapPreview.audit(server)[2]==1,"Stale-world job removed, dispatched computation still drains");
   release.countDown();release=null;
   for(int n=0;n<1000&&DeferredMapPreview.audit(server)[2]>0;n++){tick.invoke(null,server);Thread.sleep(1);}
   require(DeferredMapPreview.audit(server)[2]==0&&Arrays.equals(untouched,staleData.colors),"Prior-world completion is discarded without mutation");
   server.getPlayerList().getPlayers().remove(replacement);server.getPlayerList().getPlayers().add(old);
   old.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
   // Remove a registered owner completely while work is pending: there must be no retained job/callback.
   var fresh=MapItem.create(level,1000,2000,(byte)1,true,true);MapItem.renderBiomePreviewMap(level,fresh);added.getFirst().setItemInHand(InteractionHand.MAIN_HAND,fresh);tick.invoke(null,server);
   server.getPlayerList().getPlayers().remove(added.getFirst());tick.invoke(null,server);require(DeferredMapPreview.audit(server)[0]==0,"Disconnected owner releases pending job");
   Files.writeString(DeferredPreviewQualification.dir().resolve("policy.json"),new com.google.gson.Gson().toJson(Map.of("heldMaps",12,"activeBound",8,"productionTickInvocations",passes,"nativePixelComparisons",11,"scales","0 through 4","controlledAdditionalOwners",6,"fairness",true,"externalEdits",true,"disconnect",true,"staleWorldCompletionRejected",true)));
  }finally{
   pendingRead=false;overridePoint=null;if(release!=null){release.countDown();release=null;}
   for(var p:added)server.getPlayerList().getPlayers().remove(p);tick.invoke(null,server);for(int i=0;i<1000&&DeferredMapPreview.audit(server)[2]>0;i++){tick.invoke(null,server);Thread.sleep(1);}require(DeferredMapPreview.audit(server)[2]==0,"Cancelled worker actually drained");synthetic=false;DeferredPreviewQualification.reference=false;connected.setItemInHand(InteractionHand.MAIN_HAND,held);tick.invoke(null,server);DeferredPreviewQualification.policy=false;
  }
  loadedOverrideAndTag(level,connected,tick);loot(level);
 }
 private static void loadedOverrideAndTag(ServerLevel level,ServerPlayer player,java.lang.reflect.Method tick)throws Exception{
  DeferredPreviewQualification.policy=true;var held=player.getMainHandItem();
  var beach=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.BEACH);var oldTags=beach.tags().toList();
  var bind=Holder.Reference.class.getDeclaredMethod("bindTags",Collection.class);bind.setAccessible(true);
  var chunk=level.getChunkAt(player.blockPosition());var sparse=(blockbranch.cubic.SparseColumn)chunk;var terrain=sparse.blockbranch$terrain();int y=level.getSeaLevel();
  for(int sy=(y-3)>>4;sy<=((y+3)>>4);sy++){
   var section=terrain.materialize(sy);section.fillBiomesFromNoise((x,q,z)->beach,chunk.getPos().x()*4,sy*4,chunk.getPos().z()*4);sparse.blockbranch$replace(sy,section);terrain.modified(sy);
  }
  terrain.flush();chunk.markUnsaved();var witness=new BlockPos(chunk.getPos().getMinBlockX()+8,y,chunk.getPos().getMinBlockZ()+8);
  try{
   var tags=new ArrayList<>(oldTags);tags.add(net.minecraft.tags.BiomeTags.WATER_ON_MAP_OUTLINES);bind.invoke(beach,tags);
   require(level.getBiome(witness).is(Biomes.BEACH)&&level.getBiome(witness).is(net.minecraft.tags.BiomeTags.WATER_ON_MAP_OUTLINES),"Actual native loaded biome edit and modified outline tag");
   var stack=MapItem.create(level,witness.getX(),witness.getZ(),(byte)0,true,true);var data=MapItem.getSavedData(stack,level);MapItem.renderBiomePreviewMap(level,stack);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
   // This controlled check advances scheduler calls inside one native server callback. Service the
   // native chunk task queue between calls, as the real event loop does between actual server ticks.
   // Without this, the QA loop itself prevents pending BIOMES futures from completing.
   int passes=0,pumped=0;long pendingBefore=DeferredPreviewQualification.pendingReads;
   while(DeferredMapPreview.pending(data)&&passes++<4000){
    tick.invoke(null,level.getServer());if(passes>100&&level.getChunkSource().pollTask())pumped++;Thread.sleep(1);
   }
   Files.writeString(DeferredPreviewQualification.dir().resolve("loaded-task-progress.json"),new com.google.gson.Gson().toJson(Map.of("schedulerCalls",passes,"chunkTasksPumped",pumped,"pendingReads",DeferredPreviewQualification.pendingReads-pendingBefore,"complete",!DeferredMapPreview.pending(data))));
   require(!DeferredMapPreview.pending(data),"Actual loaded biome path finishes");
   var ref=MapItem.create(level,data.centerX,data.centerZ,data.scale,true,true);DeferredPreviewQualification.reference=true;try{MapItem.renderBiomePreviewMap(level,ref);}finally{DeferredPreviewQualification.reference=false;}
   require(Arrays.equals(data.colors,MapItem.getSavedData(ref,level).colors),"Loaded saved-biome override and nonstandard tag produce native exact pixels");
   byte[] preview=data.colors.clone();var oracle=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PreviewExplore"),ClientInformation.createDefault());oracle.setPos(witness.getX(),player.getY(),witness.getZ());
   // Native baseline update on this small, already player-loaded footprint, after completion.
   ((MapItem)stack.getItem()).update(level,oracle,data);require(!Arrays.equals(preview,data.colors),"Native exploration updates pixels after preview completion");
   Files.writeString(DeferredPreviewQualification.dir().resolve("loaded-tag.json"),new com.google.gson.Gson().toJson(Map.of("loadedBiomeOverride",true,"nonstandardOutlineTag",true,"exactNativePixels",true,"nativeExplorationAfterPreview",true,"witness",List.of(witness.getX(),witness.getY(),witness.getZ()))));
  }finally{bind.invoke(beach,oldTags);player.setItemInHand(InteractionHand.MAIN_HAND,held);DeferredPreviewQualification.reference=false;tick.invoke(null,level.getServer());DeferredPreviewQualification.policy=false;}
 }
 private static Object sampleZero(net.minecraft.server.MinecraftServer server,MapItemSavedData data)throws Exception{
  var f=DeferredMapPreview.class.getDeclaredField("STATES");f.setAccessible(true);var state=((Map<?,?>)f.get(null)).get(server);var j=state.getClass().getDeclaredField("jobs");j.setAccessible(true);
  for(var job:(List<?>)j.get(state)){var d=job.getClass().getDeclaredField("data");d.setAccessible(true);if(d.get(job)==data){var a=job.getClass().getDeclaredField("samples");a.setAccessible(true);return ((Object[])a.get(job))[0];}}throw new AssertionError("Expected active job");
 }
 private static void loot(ServerLevel level)throws Exception{
  var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,new net.minecraft.world.phys.Vec3(-504647,61,-1657815)).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
  long start=System.nanoTime();var items=level.getServer().reloadableRegistries().getLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SHIPWRECK_MAP).getRandomItems(params,915L);long wall=System.nanoTime()-start;
  var map=items.stream().filter(item->item.get(DataComponents.MAP_ID)!=null).findFirst().orElseThrow();var data=MapItem.getSavedData(map,level);
  require(data!=null&&DeferredMapPreview.pending(data),"Actual native shipwreck loot creates registered pending map");
  require(map.get(DataComponents.MAP_DECORATIONS).decorations().values().stream().anyMatch(d->d.x()==-504775&&d.z()==-1657815),"Actual native loot target and decoration ordering unchanged");
  Files.writeString(DeferredPreviewQualification.dir().resolve("loot.json"),new com.google.gson.Gson().toJson(Map.of("nativeShipwreckLoot",true,"targetUnchanged",true,"pendingMapRegistered",true,"wholeLootWallNanos",wall)));
 }
 private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
