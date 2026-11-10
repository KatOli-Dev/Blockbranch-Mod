package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.item.MapItem;

/** Same native preview bounds in an ordinary vanilla overworld, without treasure locating. */
public final class TreasurePreviewControl {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();WellInteraction.require(!(level.getChunkSource().getGenerator() instanceof RealisticGenerator),"Vanilla generator control");
  var map=MapItem.create(level,-504768,-1657792,(byte)1,true,true);
  var times=new ArrayList<Long>();for(int n=0;n<3;n++){long start=System.nanoTime();MapItem.renderBiomePreviewMap(level,map);times.add(System.nanoTime()-start);}
  var player=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"MapControl"),net.minecraft.server.level.ClientInformation.createDefault());
  player.setPos(-504775+128,63,-1657815);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,map);
  long useStart=System.nanoTime();((MapItem)map.getItem()).inventoryTick(map,level,player,net.minecraft.world.entity.EquipmentSlot.MAINHAND);long useNanos=System.nanoTime()-useStart;
  WellInteraction.require(MapItem.getSavedData(map,level).getUpdatePacket(map.get(net.minecraft.core.component.DataComponents.MAP_ID),player)!=null,"Native vanilla map packet");
  Files.writeString(Path.of("preview-control.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("previewNanos",times,"inventoryTickNanos",useNanos,"scale",1,"centerX",MapItem.getSavedData(map,level).centerX,"centerZ",MapItem.getSavedData(map,level).centerZ)));
  Files.writeString(Path.of("result.txt"),"PASS");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("PREVIEW_CONTROL_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
}
