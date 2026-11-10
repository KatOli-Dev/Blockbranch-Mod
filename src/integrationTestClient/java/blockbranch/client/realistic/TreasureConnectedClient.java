package blockbranch.client.realistic;

import java.nio.file.*;
import java.util.*;
import blockbranch.realistic.TreasureConnectedProbe;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;

/** Native client, actual integrated connection, normal movement keys and map packets. */
public final class TreasureConnectedClient {
 private static boolean opened,saving;private static int finishTicks;
 private static final int[] movementTicks=new int[2],updateClientTicks=new int[2],samples=new int[2];
 private static final long[] maxHeartbeatAge=new long[2],maxClientTickGap=new long[2];
 private static long lastTick,deadline;private static int seenCase=-1;
 public static void register(){deadline=System.nanoTime()+900_000_000_000L;
  ClientTickEvents.END_CLIENT_TICK.register(client->{try{
   long now=System.nanoTime();if(now>deadline)throw new AssertionError("Connected map fixture timeout, phase="+TreasureConnectedProbe.phase);
   if(TreasureConnectedProbe.failure!=null)throw new AssertionError("Server fixture failed",TreasureConnectedProbe.failure);
   if(!opened&&client.gui.screen() instanceof TitleScreen){opened=true;client.createWorldOpenFlows().openWorld("TreasureConnected",()->client.gui.setScreen(new TitleScreen()));return;}
   if(client.level==null||client.player==null)return;
   require(client.hasSingleplayerServer()&&client.getConnection()!=null&&client.getConnection().getConnection().isMemoryConnection(),"Actual native integrated memory connection");TreasureConnectedProbe.clientMemoryConnection=true;
   var phase=TreasureConnectedProbe.phase;
   if(blockbranch.realistic.CombinedMapWitness.ON&&phase.equals("combined-gate")){var gate=blockbranch.realistic.CombinedMapWitness.gateInside;if(gate!=null&&client.player.blockPosition().distSqr(gate)<100&&client.level.getBlockState(gate).is(net.minecraft.world.level.block.Blocks.NETHER_PORTAL))blockbranch.realistic.CombinedMapWitness.clientSawGate=true;return;}
   if(blockbranch.realistic.CombinedMapWitness.ON&&phase.equals("combined-treasure")){if(client.player.blockPosition().distSqr(blockbranch.realistic.CombinedMapWitness.TREASURE)<100&&client.player.containerMenu.containerId!=0&&client.player.containerMenu.getSlot(0).getItem().isEmpty())blockbranch.realistic.CombinedMapWitness.clientSawTreasure=true;return;}
   if(TreasureConnectedProbe.REOPEN&&TreasureConnectedProbe.mapId>=0){var restored=MapItem.getSavedData(new MapId(TreasureConnectedProbe.mapId),client.level);if(restored!=null&&restored.getDecorations().iterator().hasNext())TreasureConnectedProbe.clientSawMap=TreasureConnectedProbe.mapId;}
   if(phase.startsWith("settle-")){
    client.options.keyUp.setDown(false);
    if(Math.abs(client.player.getX()-TreasureConnectedProbe.X)<.1&&Math.abs(client.player.getZ()-TreasureConnectedProbe.Z)<.1&&client.player.getAbilities().flying){
     int received=0;var cp=client.player.chunkPosition();
     for(int z=cp.z()-2;z<=cp.z()+2;z++)for(int x=cp.x()-2;x<=cp.x()+2;x++)if(client.level.getChunkSource().hasChunk(x,z))received++;
     if(received==25)TreasureConnectedProbe.clientReady=true;
    }
   }
   int index=TreasureConnectedProbe.caseIndex;
   if(index>=0&&!phase.startsWith("settle-")&&!phase.equals("done")){
    if(index!=seenCase){seenCase=index;lastTick=now;}
    samples[index]++;if(lastTick>0)maxClientTickGap[index]=Math.max(maxClientTickGap[index],now-lastTick);lastTick=now;
    maxHeartbeatAge[index]=Math.max(maxHeartbeatAge[index],now-TreasureConnectedProbe.lastHeartbeat);
    if(TreasureConnectedProbe.updateActive)updateClientTicks[index]++;
    boolean movingPhase=phase.endsWith("update")||phase.endsWith("observe");boolean move=TreasureConnectedProbe.PROGRESSIVE?(index==1&&phase.equals("warm-paused")&&movementTicks[index]++<6):(movingPhase&&movementTicks[index]++<6);if(TreasureConnectedProbe.PROGRESSIVE&&index==1&&movementTicks[index]>6&&client.player.getX()>TreasureConnectedProbe.X+.5)TreasureConnectedProbe.pausedMovementDone=true;client.player.setYRot(-90);client.player.setXRot(35);client.options.keyUp.setDown(move);
    require(Math.abs(client.player.getX()-TreasureConnectedProbe.X)<5,"Bounded native movement remains within its chunk");
    var data=MapItem.getSavedData(new MapId(TreasureConnectedProbe.mapId),client.level);
    if(data!=null&&data.getDecorations().iterator().hasNext())TreasureConnectedProbe.clientSawMap=TreasureConnectedProbe.mapId;
   }
   if(TreasureConnectedProbe.done&&!saving){
    client.options.keyUp.setDown(false);if(++finishTicks<40)return;
    var data=MapItem.getSavedData(new MapId(TreasureConnectedProbe.mapId),client.level);require(data!=null,"Actual native map data reached client");
    var rows=new ArrayList<Map<String,Object>>();if(!TreasureConnectedProbe.REOPEN)for(int i=0;i<2;i++)rows.add(Map.of("case",i==0?(Boolean.getBoolean("blockbranch.treasureProgressiveWarmFixture")?"retained-coast-final-source":"cold-edge"):"warmed-edge","samples",samples[i],"clientTicksDuringNativeUpdate",updateClientTicks[i],"maxObservedServerHeartbeatAgeNanos",maxHeartbeatAge[i],"maxClientTickGapNanos",maxClientTickGap[i]));
    if(TreasureConnectedProbe.REOPEN)require(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data.colors)).equals(TreasureConnectedProbe.savedMapHash),"Restored native pixels also reach actual client unchanged");
    Files.writeString(TreasureConnectedProbe.dir().resolve(TreasureConnectedProbe.REOPEN?"reopen-client-results.json":"client-results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("nativeConnection",true,"cases",rows,"finalClientMapSha256",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data.colors)),"finalClientX",client.player.getX(),"finalServerX",TreasureConnectedProbe.serverX)));
    saving=true;if(TreasureConnectedProbe.PROGRESSIVE){Files.writeString(TreasureConnectedProbe.dir().resolve("result.txt"),TreasureConnectedProbe.REOPEN?"PASS progressive native partial-save reopen, current-position resume and client map pixels":(Boolean.getBoolean("blockbranch.treasureProgressiveWarmFixture")?"PASS retained-coast final source, two hands, put-away movement and partial save; reopen pending":"PASS progressive cold map, two hands, put-away movement and partial save; reopen pending"));client.stop();return;}client.gui.toastManager().clear();net.minecraft.client.Screenshot.grab(client.gameDirectory,TreasureConnectedProbe.REOPEN?"connected-treasure-map-reopen.png":"connected-treasure-map.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{Files.writeString(TreasureConnectedProbe.dir().resolve("result.txt"),TreasureConnectedProbe.REOPEN?"PASS actual player held map inventory, pixels and native client reconnect":"PASS native connected-player cold/warm map updates, native movement and map reception");client.stop();}catch(Exception e){throw new RuntimeException(e);}}));
   }
  }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("TREASURE_CONNECTED_CLIENT_FAIL",t);TreasureConnectedProbe.done=true;try{Files.writeString(TreasureConnectedProbe.dir().resolve("result.txt"),"FAIL "+t);}catch(Exception ignored){}client.stop();}});
 }
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
