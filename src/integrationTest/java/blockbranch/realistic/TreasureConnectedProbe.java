package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Real integrated connection only. Normal player inventory ticking performs both map updates. */
public final class TreasureConnectedProbe {
 public static final boolean PROGRESSIVE=Boolean.getBoolean("blockbranch.treasureProgressive");
 public static final boolean ON=Boolean.getBoolean("blockbranch.treasureConnected");
 public static final boolean REOPEN=Boolean.getBoolean("blockbranch.treasureConnectedReopen");
 public static volatile String savedMapHash="";
 public static final double X=-504647.0,Y=100,Z=-1657815.0;
 public static volatile String phase="startup";
 public static volatile boolean clientReady,clientMemoryConnection,updateActive,done;
 public static volatile int caseIndex=-1,mapId=-1,clientSawMap=-1;
 public static volatile double serverX=X;
 public static volatile long lastHeartbeat=System.nanoTime();
 public static volatile Throwable failure;
 private static ServerPlayer player;
 private static long ticks,lastLoadTick,tickStart,updateEndTick;
 private static int stableTicks;private static boolean firstUpdate;
 private static final List<Map<String,Object>> cases=new ArrayList<>();
 private static Map<String,Object> current;
 private static long fresh,totalLoads;
 private static final List<Long> previewNanos=new ArrayList<>(),previewBytes=new ArrayList<>();
 private static final List<Long> updateTimes=new ArrayList<>();
 private static long caseFresh,peakTickets;
 private static byte[] previewPixels;
 private static long maxCaseTick,columnsAtStart;
 public static volatile boolean pausedMovementDone;
 private static ItemStack secondaryMap;
 private static ItemStack putAway;private static String pausedHash;private static long pauseTick;private static boolean pauseDone;
 public static Path dir(){return Path.of(System.getProperty("blockbranch.treasureConnectedDir"));}
 public static void preview(long nanos,long bytes){previewNanos.add(nanos);previewBytes.add(bytes);}
 public static void register(){
  ServerChunkEvents.CHUNK_LOAD.register((level,chunk,newly)->{if(level.dimension()==net.minecraft.world.level.Level.OVERWORLD){lastLoadTick=ticks;totalLoads++;if(current!=null&&newly)caseFresh++;if(updateActive&&newly)fresh++;}});
  ServerTickEvents.START_SERVER_TICK.register(server->{tickStart=System.nanoTime();if(PROGRESSIVE)TreasureProgressiveFailureProbe.start(server);});
  ServerLifecycleEvents.SERVER_STOPPED.register(server->{if(PROGRESSIVE)try{var state=ProgressiveMapDrawing.audit(server);WellInteraction.require(java.util.Arrays.stream(state).allMatch(n->n==0),"No map scheduler state survives shutdown");Files.writeString(dir().resolve(REOPEN?"reopen-cleanup.json":"cleanup.json"),java.util.Arrays.toString(state));}catch(Exception e){throw new RuntimeException(e);}});
  ServerTickEvents.END_SERVER_TICK.register(server->{if(server.isDedicatedServer()||done)return;try{
   ticks++;if(server.getPlayerList().getPlayers().isEmpty())return;var level=server.overworld();
   if(player==null){
    if(PROGRESSIVE)ProgressiveMapPolicyQualification.check();
    player=server.getPlayerList().getPlayers().getFirst();WellInteraction.require(level.getSeed()==424242&&level.getChunkSource().getGenerator() instanceof RealisticGenerator,"Exact connected fixture generator/seed");
    if(CombinedMapWitness.ON){server.getPlayerList().setViewDistance(6);CombinedMapWitness.check(level,player,REOPEN);if(!REOPEN){phase="combined-gate";return;}}
    if(Boolean.getBoolean("blockbranch.treasureProgressiveReview")){
     var stack=player.getMainHandItem();var actual=MapItem.getSavedData(stack,level);mapId=stack.get(DataComponents.MAP_ID).id();
     byte[] before=actual.colors.clone();
     var referenceStack=MapItem.create(level,actual.centerX,actual.centerZ,actual.scale,true,true);MapItem.renderBiomePreviewMap(level,referenceStack);
     var reference=MapItem.getSavedData(referenceStack,level);
     var oracle=new ServerPlayer(server,level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MapOracle"),net.minecraft.server.level.ClientInformation.createDefault());oracle.setPos(player.getX(),player.getY(),player.getZ());
     for(int i=0;i<128;i++)((MapItem)stack.getItem()).update(level,oracle,reference);
     var differences=new ArrayList<Map<String,Object>>();for(int i=0;i<before.length;i++)if(before[i]!=reference.colors[i])differences.add(Map.of("x",i%128,"z",i/128,"actual",before[i]&255,"expected",reference.colors[i]&255));
     Files.writeString(dir().resolve("pixel-review.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("position",List.of(player.getX(),player.getY(),player.getZ()),"actualCenter",List.of(actual.centerX,actual.centerZ),"referenceCenter",List.of(reference.centerX,reference.centerZ),"differences",differences,"actualBase64",Base64.getEncoder().encodeToString(before),"expectedBase64",Base64.getEncoder().encodeToString(reference.colors))));
     failure=new AssertionError("Review captured "+differences.size()+" differing pixels");done=true;return;
    }
    if(REOPEN){
     var before=com.google.gson.JsonParser.parseString(Files.readString(dir().resolve("server-results.json"))).getAsJsonObject().getAsJsonArray("cases").get(1).getAsJsonObject();
     mapId=before.get("mapId").getAsInt();var stack=PROGRESSIVE?player.getInventory().getItem(1):player.getMainHandItem();WellInteraction.require(stack.get(DataComponents.MAP_ID)!=null&&stack.get(DataComponents.MAP_ID).id()==mapId,"Actual player inventory restores the native held map");
     savedMapHash=com.google.gson.JsonParser.parseString(Files.readString(dir().resolve("client-results.json"))).getAsJsonObject().get("finalClientMapSha256").getAsString();
     var data=MapItem.getSavedData(stack,level);var hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data.colors));
     WellInteraction.require(hash.equals(savedMapHash),"Actual map pixels survive server save/reopen");if(PROGRESSIVE){
      var edits=new ArrayList<List<Integer>>();int ex=(int)Math.floor(player.getX()/2)*2-4,ez=(int)Math.floor(player.getZ()/2)*2;
      int ey=0;for(int dx=0;dx<2;dx++)for(int dz=0;dz<2;dz++)ey=Math.max(ey,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,ex+dx,ez+dz));
      for(int dx=0;dx<2;dx++)for(int dz=0;dz<2;dz++){var p=new net.minecraft.core.BlockPos(ex+dx,ey,ez+dz);WellInteraction.require(level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.WOOL.red())||level.setBlock(p,net.minecraft.world.level.block.Blocks.WOOL.red().defaultBlockState(),3),"Disposable native player edit");edits.add(List.of(p.getX(),p.getY(),p.getZ()));}
      Files.writeString(dir().resolve("edit-witness.json"),new com.google.gson.GsonBuilder().create().toJson(edits));
      previewPixels=data.colors.clone();columnsAtStart=ProgressiveMapDrawing.audit(server)[0];player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);player.getInventory().setItem(1,ItemStack.EMPTY);phase="reopen-progressive";}else phase="reopen-observe";return;
    }
    server.getPlayerList().setViewDistance(6);var commands=server.getCommands();var source=server.createCommandSourceStack();
    commands.performPrefixedCommand(source,"time set 6000");commands.performPrefixedCommand(source,"weather clear");commands.performPrefixedCommand(source,"gamemode creative "+player.getName().getString());
    pose();phase="settle-cold";lastLoadTick=ticks;return;
   }
   if(phase.equals("combined-gate")){if(CombinedMapWitness.clientSawGate){CombinedMapWitness.openTreasure(level,player);phase="combined-treasure";}return;}
   if(phase.equals("combined-treasure")){if(CombinedMapWitness.clientSawTreasure){player.closeContainer();Files.writeString(dir().resolve("combined-client-interactions.json"),"{\"schema\":92,\"clientSawActiveGate\":true,\"clientSawTreasureMenuAndLootRemoval\":true}");pose();phase="settle-cold";lastLoadTick=ticks;}return;}
   serverX=player.getX();
   if(PROGRESSIVE && current!=null && !REOPEN){
    TreasureProgressiveFailureProbe.check(server);
    var budget=ProgressiveMapDrawing.audit(server);
    WellInteraction.require(budget[1]<=64&&budget[2]<=18&&budget[3]<=1&&budget[4]<=1&&budget[5]<=1,"Global job/ticket/future bounds");
    current.put("completedColumns",budget[0]-columnsAtStart);current.put("ownedTickets",budget[2]);peakTickets=Math.max(peakTickets,budget[2]);
    if(caseIndex==1&&secondaryMap!=null&&ticks-updateEndTick>=12&&firstUpdate){
     WellInteraction.require(!hash(MapItem.getSavedData(secondaryMap,level).colors).equals(hash(previewPixels)),"Offhand second map made progress");
     WellInteraction.require(!hash(MapItem.getSavedData(player.getMainHandItem(),level).colors).equals(hash(previewPixels)),"Main hand map also made progress");
     player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);secondaryMap=null;current.put("bothHandsProgressed",true);
    }
    if(!pauseDone&&ticks-updateEndTick>=30&&firstUpdate){
     if(putAway==null){putAway=player.getMainHandItem();pausedHash=hash(MapItem.getSavedData(putAway,level).colors);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.getInventory().setItem(1,putAway);pauseTick=ticks;phase=caseIndex==0?"cold-paused":"warm-paused";}
     if(ticks-pauseTick>=20&&(caseIndex==0||pausedMovementDone)){WellInteraction.require(hash(MapItem.getSavedData(putAway,level).colors).equals(pausedHash),"Put-away pixels retained exactly");WellInteraction.require(budget[2]==0&&budget[3]==0,"Put-away releases active column tickets and future ownership");if(caseIndex==1){
      WellInteraction.require(player.getX()>X+.5,"Actual native forward movement while map put away");
      current.put("putAwayRetained",true);current.put("partialPausedSha256",pausedHash);current.put("partialPaused",true);cases.add(current);write();
      server.saveEverything(false,true,true);phase="done";done=true;return;
     }
     player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,putAway);player.getInventory().setItem(1,ItemStack.EMPTY);pauseDone=true;phase="cold-observe";current.put("putAwayRetained",true);}
     return;
    }
   }
   if(REOPEN){if(PROGRESSIVE)TreasureProgressiveFailureProbe.check(server);if(clientSawMap==mapId&&clientMemoryConnection&&ticks>=60&&(!PROGRESSIVE||ProgressiveMapDrawing.audit(server)[0]-columnsAtStart>=140)){
    if(PROGRESSIVE){WellInteraction.require(TreasureProgressiveFailureProbe.verified(),"Reopen deferred failure and denied-admission ownership checks completed");var actual=MapItem.getSavedData(player.getMainHandItem(),level);checkOracle(level,actual,previewPixels);savedMapHash=hash(actual.colors);int red=net.minecraft.world.level.block.Blocks.WOOL.red().defaultBlockState().getMapColor(level,player.blockPosition()).id;boolean painted=false;for(byte color:actual.colors)painted|=(color&255)/4==red;WellInteraction.require(painted,"Actual saved-edit red wool appears in native map pixels");}
    Files.writeString(dir().resolve("reopen-server-results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("heldMapId",mapId,"savedMapSha256",savedMapHash,"nativeClientReceived",true,"registeredPlayer",true)));if(PROGRESSIVE)Files.writeString(dir().resolve("final-audit-metadata.json"),new com.google.gson.GsonBuilder().create().toJson(Map.of("mapId",mapId,"sha256",savedMapHash,"position",List.of(player.getX(),player.getY(),player.getZ()))));level.getServer().saveEverything(false,true,true);phase="done";done=true;
   }return;}

   if(ticks%100==0)Files.writeString(dir().resolve("phase.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("phase",phase,"ticks",ticks,"clientReady",clientReady,"memoryConnection",clientMemoryConnection,"viewReady",viewReady(level),"ticksSinceChunkLoad",ticks-lastLoadTick,"loaded",level.getChunkSource().getLoadedChunksCount(),"position",List.of(player.getX(),player.getY(),player.getZ()))));
   if(phase.equals("settle-cold")||phase.equals("settle-warm")){
    if(!clientReady||!clientMemoryConnection||!viewReady(level)||ticks-lastLoadTick<80){stableTicks=0;return;}
    if(++stableTicks<40)return;giveMap(level,phase.equals("settle-cold")?0:1);return;
   }
   if(firstUpdate&&ticks-updateEndTick>=60&&clientSawMap==mapId&&(!PROGRESSIVE||ProgressiveMapDrawing.audit(server)[0]-columnsAtStart>=140)){
    if(PROGRESSIVE){
     var actual=MapItem.getSavedData(player.getMainHandItem(),level);long before=System.nanoTime();checkOracle(level,actual,previewPixels);
     WellInteraction.require(TreasureProgressiveFailureProbe.verified(),"Controlled failed request recovered before cold equality");current.put("failureRecovered",true);
     current.put("nativeOracleNanos",System.nanoTime()-before);current.put("finalPixelSha256",hash(actual.colors));current.put("nativePixelEquality",true);
    }
    current.put("maxServerTickNanos",maxCaseTick);if(PROGRESSIVE){current.put("progressiveInventoryTickNanos",List.copyOf(updateTimes));current.put("freshChunksAcrossCase",caseFresh);current.put("peakOwnedTickets",peakTickets);}current.put("clientSawNativeMap",true);cases.add(current);
    if(caseIndex==0){player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);pose();phase="settle-warm";clientReady=false;stableTicks=0;firstUpdate=false;lastLoadTick=ticks;}
    else{
     level.getServer().saveEverything(false,true,true);write();phase="done";done=true;
    }
   }
  }catch(Throwable t){try{Files.writeString(dir().resolve("failure-current.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(current));}catch(Exception ignored){}failure=t;done=true;blockbranch.Blockbranch.LOGGER.error("TREASURE_CONNECTED_FAIL",t);}finally{
   if(current!=null)maxCaseTick=Math.max(maxCaseTick,System.nanoTime()-tickStart);lastHeartbeat=System.nanoTime();
  }});
 }
 private static void pose(){
  var server=player.level().getServer();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" "+X+" "+Y+" "+Z+" -90 35");
  player.getAbilities().flying=true;player.onUpdateAbilities();
 }
 private static boolean viewReady(net.minecraft.server.level.ServerLevel level){
  var p=player.chunkPosition();int count=0;
  for(int z=p.z()-7;z<=p.z()+7;z++)for(int x=p.x()-7;x<=p.x()+7;x++)if(net.minecraft.server.level.ChunkTrackingView.isInViewDistance(p.x(),p.z(),6,x,z)){
   if(!level.getChunkSource().hasChunk(x,z))return false;count++;
  }
  return count>100;
 }
 private static void giveMap(net.minecraft.server.level.ServerLevel level,int index)throws Exception{
  WellInteraction.require(clientMemoryConnection&&player.connection!=null&&level.getServer().getPlayerList().getPlayers().contains(player),"Actual registered client player");
  WellInteraction.require(Math.abs(player.getX()-X)<.1&&Math.abs(player.getZ()-Z)<.1,"Matched first-update position");
  putAway=null;pauseDone=false;pausedMovementDone=false;
  updateTimes.clear();caseFresh=peakTickets=0;
  columnsAtStart=ProgressiveMapDrawing.audit(level.getServer())[0];
  caseIndex=index;phase=index==0?"cold-loot":"warm-loot";fresh=0;maxCaseTick=0;previewNanos.clear();previewBytes.clear();
  var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,new net.minecraft.world.phys.Vec3(X,61,Z)).create(LootContextParamSets.CHEST);
  long before=System.nanoTime();var items=level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SHIPWRECK_MAP).getRandomItems(params,915L);long loot=System.nanoTime()-before;
  var map=items.stream().filter(stack->stack.get(DataComponents.MAP_ID)!=null).findFirst().orElseThrow();
  var data=MapItem.getSavedData(map,level);previewPixels=data.colors.clone();String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data.colors));
  WellInteraction.require(hash.equals("f298e50bbe474b7d196a1accd8eb6ea6a270f2f1e8de1ba3d013afa8cb95d218"),"Connected native preview pixel identity");
  boolean target=false;for(var d:map.get(DataComponents.MAP_DECORATIONS).decorations().values())target|=d.x()==-504775&&d.z()==-1657815;
  WellInteraction.require(target,"Native shipwreck map target");mapId=map.get(DataComponents.MAP_ID).id();clientSawMap=-1;
  current=new LinkedHashMap<>();current.put("case",index==0?(CombinedMapWitness.ON?"combined92-structure-visited":Boolean.getBoolean("blockbranch.treasureProgressiveWarmFixture")?"retained-coast-final-source":"cold-edge"):"warmed-edge");current.put("lootNanos",loot);current.put("previewNanos",List.copyOf(previewNanos));current.put("previewAllocatedBytes",List.copyOf(previewBytes));current.put("previewSha256",hash);current.put("controlledInitialReadinessDenial",Boolean.getBoolean("blockbranch.treasureProgressiveWarmFixture"));current.put("mapId",mapId);current.put("serverPosition",List.of(player.getX(),player.getY(),player.getZ()));current.put("totalLoadedBefore",level.getChunkSource().getLoadedChunksCount());
  player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,map);
  if(PROGRESSIVE&&index==1){secondaryMap=MapItem.create(level,data.centerX,data.centerZ,data.scale,true,true);MapItem.getSavedData(secondaryMap,level).colors=previewPixels.clone();player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,secondaryMap);}
  firstUpdate=false;phase=index==0?"cold-given":"warm-given";
 }
 public static boolean measure(ItemStack stack,net.minecraft.world.entity.Entity entity){return ON&&current!=null&&!firstUpdate&&entity==player&&stack.get(DataComponents.MAP_ID)!=null&&stack.get(DataComponents.MAP_ID).id()==mapId;}
 public static void begin(){updateActive=true;phase=caseIndex==0?"cold-update":"warm-update";current.put("nativeUpdateStartedNs",System.nanoTime());current.put("nativeUpdatePosition",List.of(player.getX(),player.getY(),player.getZ()));current.put("loadsAtStart",totalLoads);}
 public static void end(long elapsed)throws Exception{
  current.put("inventoryTickNanos",elapsed);current.put("freshChunksDuringUpdate",fresh);current.put("loadsDuringUpdate",totalLoads-((Number)current.get("loadsAtStart")).longValue());current.put("nativeUpdateEndedNs",System.nanoTime());
  updateActive=false;firstUpdate=true;updateEndTick=ticks;phase=caseIndex==0?"cold-observe":"warm-observe";
 }
 private static void checkOracle(net.minecraft.server.level.ServerLevel level,net.minecraft.world.level.saveddata.maps.MapItemSavedData actual,byte[] base)throws Exception{
  var reference=net.minecraft.world.level.saveddata.maps.MapItemSavedData.createFresh(actual.centerX,actual.centerZ,actual.scale,true,true,actual.dimension);reference.colors=base.clone();
  var oracle=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MapOracle"),net.minecraft.server.level.ClientInformation.createDefault());oracle.setPos(player.getX(),player.getY(),player.getZ());
  for(int i=0;i<128;i++)((MapItem)player.getMainHandItem().getItem()).update(level,oracle,reference);
  Files.writeString(dir().resolve(REOPEN?"reopen-pixel-oracle.json":"cold-pixel-oracle.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("actualBase64",Base64.getEncoder().encodeToString(actual.colors),"expectedBase64",Base64.getEncoder().encodeToString(reference.colors),"position",List.of(player.getX(),player.getY(),player.getZ()))));
  WellInteraction.require(java.util.Arrays.equals(actual.colors,reference.colors),"Exact native final pixels at current player position");
 }
 public static void noteUpdate(ItemStack stack,long elapsed){if(PROGRESSIVE&&current!=null&&stack.get(DataComponents.MAP_ID)!=null&&stack.get(DataComponents.MAP_ID).id()==mapId){if(updateTimes.size()>=20000)throw new AssertionError("Bounded update samples");updateTimes.add(elapsed);}}
 private static String hash(byte[] bytes)throws Exception{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}
 private static void write()throws Exception{
  var data=new LinkedHashMap<String,Object>();data.put("seed",424242);data.put("viewDistance",6);data.put("simulationDistance",5);data.put("registeredPlayer",true);data.put("nativeMemoryConnection",clientMemoryConnection);data.put("cases",cases);
  Files.writeString(dir().resolve("server-results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(data));
 }
}
