package blockbranch.client.realistic;
import blockbranch.realistic.DeferredPreviewQualification;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
public final class DeferredPreviewClient {
 private static boolean opened,stopping;private static int settle;private static long start=System.nanoTime();
 public static void register(){ClientTickEvents.END_CLIENT_TICK.register(client->{try{
  if(stopping)return;if(DeferredPreviewQualification.failure!=null)throw new AssertionError(DeferredPreviewQualification.failure);
  if(System.nanoTime()-start>900_000_000_000L)throw new AssertionError("Native preview fixture timeout "+DeferredPreviewQualification.phase);
  if(!opened&&client.gui.screen() instanceof TitleScreen){opened=true;client.createWorldOpenFlows().openWorld("TreasureConnected",()->client.gui.setScreen(new TitleScreen()));return;}
  if(client.player==null||client.level==null)return;
  if(!client.getConnection().getConnection().isMemoryConnection())throw new AssertionError("Actual native player connection required");
  if(DeferredPreviewQualification.mapId>=0){var map=MapItem.getSavedData(new MapId(DeferredPreviewQualification.mapId),client.level);if(map!=null&&DeferredPreviewQualification.hash(map.colors).equals(DeferredPreviewQualification.hash))DeferredPreviewQualification.clientExact=true;}
  if(DeferredPreviewQualification.done&&++settle>=2){
   // Freeze server sampling before save by putting away. No frame readback is required for native packet equality.

   Files.writeString(DeferredPreviewQualification.dir().resolve(DeferredPreviewQualification.REOPEN?"reopen-result.txt":"result.txt"),"PASS actual native map packets, pending pause/retry, pixels and save qualification");stopping=true;client.stop();
  }
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("DEFERRED_PREVIEW_CLIENT_FAIL",t);try{Files.writeString(DeferredPreviewQualification.dir().resolve("result.txt"),"FAIL "+t);}catch(Exception ignored){}stopping=true;client.stop();}});}
}
