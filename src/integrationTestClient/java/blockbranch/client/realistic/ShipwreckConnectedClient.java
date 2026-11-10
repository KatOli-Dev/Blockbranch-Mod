package blockbranch.client.realistic;
import java.nio.file.*;
import blockbranch.realistic.ShipwreckConnectedProbe;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
public final class ShipwreckConnectedClient {
 private static boolean opened,saving;private static int settled;private static long deadline,maxHeartbeat;
 public static void register(){deadline=System.nanoTime()+900_000_000_000L;ClientTickEvents.END_CLIENT_TICK.register(client->{try{
  if(System.nanoTime()>deadline)throw new AssertionError("Shipwreck connected timeout: "+ShipwreckConnectedProbe.phase);
  if(ShipwreckConnectedProbe.failure!=null)throw new AssertionError("Native server fixture failed",ShipwreckConnectedProbe.failure);
  if(!opened&&client.gui.screen() instanceof TitleScreen){opened=true;client.createWorldOpenFlows().openWorld("ShipwreckConnected",()->client.gui.setScreen(new TitleScreen()));return;}
  if(client.player==null||client.level==null||saving)return;
  if(!client.getConnection().getConnection().isMemoryConnection())throw new AssertionError("Actual native connection required");
  if(ShipwreckConnectedProbe.chestPosition!=null&&client.player.blockPosition().distSqr(ShipwreckConnectedProbe.chestPosition)<100)ShipwreckConnectedProbe.clientAtWreck=true;
  if(ShipwreckConnectedProbe.phase.equals("chest")&&client.player.containerMenu.containerId!=0)ShipwreckConnectedProbe.sawMenu=true;
  var map=ShipwreckConnectedProbe.mapId<0?null:MapItem.getSavedData(new MapId(ShipwreckConnectedProbe.mapId),client.level);if(map!=null)ShipwreckConnectedProbe.sawMap=true;
  if(ShipwreckConnectedProbe.phase.equals("drawing"))maxHeartbeat=Math.max(maxHeartbeat,System.nanoTime()-ShipwreckConnectedProbe.heartbeat);
  if(!ShipwreckConnectedProbe.done)return;
  if(++settled<30||map==null)return;
  var hash=ShipwreckConnectedProbe.hash(map.colors);if(!hash.equals(ShipwreckConnectedProbe.serverHash))throw new AssertionError("Native client received exact partial pixels");
  var dir=Path.of(System.getProperty("blockbranch.shipwreckConnectedDir"));Files.writeString(dir.resolve(ShipwreckConnectedProbe.REOPEN?"reopen-client-results.json":"client-results.json"),"{\"nativeConnection\":true,\"exactClientPartialPixels\":true,\"maxHeartbeatAgeNanos\":"+maxHeartbeat+",\"sha256\":\""+hash+"\"}");saving=true;
  net.minecraft.client.Screenshot.grab(client.gameDirectory,ShipwreckConnectedProbe.REOPEN?"shipwreck-map-reopen.png":"shipwreck-map.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{Files.writeString(dir.resolve("result.txt"),"PASS actual shipwreck loot map, native connected drawing, exact client partial pixels, save and cleanup");client.stop();}catch(Exception e){throw new RuntimeException(e);}}));
 }catch(Throwable t){try{Files.writeString(Path.of(System.getProperty("blockbranch.shipwreckConnectedDir")).resolve("result.txt"),"FAIL "+t);}catch(Exception ignored){}blockbranch.Blockbranch.LOGGER.error("SHIPWRECK_CONNECTED_CLIENT_FAIL",t);client.stop();}});}
}
