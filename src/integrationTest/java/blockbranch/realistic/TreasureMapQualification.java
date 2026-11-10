package blockbranch.realistic;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
public final class TreasureMapQualification {
 public static final java.util.List<Long> PREVIEW_TIMES=new java.util.ArrayList<>();
 public static final java.util.List<Long> PREVIEW_ALLOCATIONS=new java.util.ArrayList<>();
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();
  var prior=Path.of("treasure-map-saved.txt");boolean reopened=Files.exists(prior);
  if(reopened){var saved=Files.readString(prior).split(",");var data=MapItem.getSavedData(new net.minecraft.world.level.saveddata.maps.MapId(Integer.parseInt(saved[0])),level);WellInteraction.require(data!=null&&java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data.colors)).equals(saved[1]),"Native map colors survive real save/reopen");}
  var q=Files.readString(Path.of("view.txt")).trim().split(",");int x=Integer.parseInt(q[0]),y=Integer.parseInt(q[1]),z=Integer.parseInt(q[2]);
  var table=server.reloadableRegistries().getLootTable(BuiltInLootTables.SHIPWRECK_MAP);
  var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,new net.minecraft.world.phys.Vec3(x+128,y,z)).create(LootContextParamSets.CHEST);
  var recording=new jdk.jfr.Recording();recording.enable("jdk.ExecutionSample").withPeriod(java.time.Duration.ofMillis(5));recording.start();
  long start=System.nanoTime();var items=table.getRandomItems(params,915L);long elapsed=System.nanoTime()-start;
  recording.stop();recording.dump(Path.of("treasure-map.jfr"));recording.close();
  Files.writeString(Path.of("treasure-map-attempt.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("nanos",elapsed,"items",items.toString())));
  var map=items.stream().filter(item->item.get(DataComponents.MAP_ID)!=null).findFirst().orElseThrow(()->new AssertionError("Native shipwreck loot creates filled treasure map"));
  WellInteraction.require(MapItem.getSavedData(map,level)!=null,"Native map saved data");
  var colorsHash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(MapItem.getSavedData(map,level).colors));
  WellInteraction.require(colorsHash.equals("f298e50bbe474b7d196a1accd8eb6ea6a270f2f1e8de1ba3d013afa8cb95d218"),"Every native preview pixel matches original path");var decorations=map.get(DataComponents.MAP_DECORATIONS);WellInteraction.require(decorations!=null&&!decorations.decorations().isEmpty(),"Native treasure target decoration");
  boolean target=false;for(var entry:decorations.decorations().values())if(entry.x()==x&&entry.z()==z)target=true;
  WellInteraction.require(target,"Native exploration map targets generated chest");
  var previews=new java.util.ArrayList<Long>();
  for(int repeat=0;repeat<2;repeat++){long before=System.nanoTime();MapItem.renderBiomePreviewMap(level,map);previews.add(System.nanoTime()-before);WellInteraction.require(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(MapItem.getSavedData(map,level).colors)).equals(colorsHash),"Repeat preview pixels unchanged");}
  Files.writeString(Path.of("treasure-preview-isolated.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("lootNanos",elapsed,"previewNanos",previews,"nativePreviewNanos",PREVIEW_TIMES,"nativePreviewAllocatedBytes",PREVIEW_ALLOCATIONS,"scale",MapItem.getSavedData(map,level).scale,"centerX",MapItem.getSavedData(map,level).centerX,"centerZ",MapItem.getSavedData(map,level).centerZ)));
  MapPreviewScopeQualification.check(level);
  var player=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TreasureMapper"),net.minecraft.server.level.ClientInformation.createDefault());
  player.setPos(x+128,y+2,z);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,map);
  int warmView=Integer.getInteger("blockbranch.treasureWarmView",0);int warmed=TreasureMapUseTrace.warmRequestedView(level,player,"treasure-map-chunk-trace.json",warmView);
  long useStart=System.nanoTime();try(var trace=TreasureMapUseTrace.open(player,"treasure-map-chunk-trace.json")){((MapItem)map.getItem()).inventoryTick(map,level,player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);}long useNanos=System.nanoTime()-useStart;
  var mapData=MapItem.getSavedData(map,level);WellInteraction.require(mapData.getUpdatePacket(map.get(DataComponents.MAP_ID),player)!=null,"Native carried map creates client update packet");
  Files.writeString(prior,map.get(DataComponents.MAP_ID).id()+","+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(mapData.colors)));
  Files.writeString(Path.of("treasure-map-use.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("prewarmedRequestedChunks",warmed,"simulatedViewDistance",warmView,"inventoryTickNanos",useNanos,"packet",true,"reopenedPriorMap",reopened,"scopeChecks",true,"updatedColorsSha256",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(mapData.colors)))));
  server.saveEverything(false,true,true);Files.writeString(Path.of("treasure-map.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("colorsSha256",colorsHash,"nanos",elapsed,"filledMap",true,"targetMatches",target,"items",items.toString(),"decorations",decorations.toString())));Files.writeString(Path.of("treasure-map-result.txt"),"PASS");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("TREASURE_MAP_FAIL",t);try{Files.writeString(Path.of("treasure-map-result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
}
