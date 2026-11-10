package blockbranch.client.realistic;
import blockbranch.realistic.HutExplorationProbe;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
public final class HutExplorationClient {
 private static boolean opened,stopping;private static long deadline,last,maxGap,maxHeartbeat;private static int moving;
 public static void register(){deadline=System.nanoTime()+900_000_000_000L;ClientTickEvents.END_CLIENT_TICK.register(client->{try{
  long now=System.nanoTime();if(now>deadline)throw new AssertionError("Hut exploration timeout: "+HutExplorationProbe.phase);if(HutExplorationProbe.failure!=null)throw new AssertionError(HutExplorationProbe.failure);
  if(!opened&&client.gui.screen() instanceof TitleScreen){opened=true;client.createWorldOpenFlows().openWorld("HutExplore",()->client.gui.setScreen(new TitleScreen()));return;}
  if(client.level==null||client.player==null)return;
  if(!client.hasSingleplayerServer()||!client.getConnection().getConnection().isMemoryConnection())throw new AssertionError("Native player connection");
  if(HutExplorationProbe.phase.equals("settle")&&Math.abs(client.player.getX()-HutExplorationProbe.startX)<.1&&client.player.getAbilities().flying){int n=0;var p=client.player.chunkPosition();for(int z=p.z()-2;z<=p.z()+2;z++)for(int x=p.x()-2;x<=p.x()+2;x++)if(client.level.getChunkSource().hasChunk(x,z))n++;if(n==25)HutExplorationProbe.ready=true;}
  boolean move=HutExplorationProbe.phase.equals("movement");client.player.setYRot(-90);client.player.setXRot(0);client.options.keyUp.setDown(move);
  if(move){moving++;if(last!=0)maxGap=Math.max(maxGap,now-last);maxHeartbeat=Math.max(maxHeartbeat,now-HutExplorationProbe.heartbeat);last=now;}
  if(HutExplorationProbe.done&&!stopping){stopping=true;Files.writeString(HutExplorationProbe.dir().resolve("client-results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("movementClientTicks",moving,"maxClientTickGapNanos",maxGap,"maxServerHeartbeatAgeNanos",maxHeartbeat,"clientEndX",client.player.getX())));Files.writeString(HutExplorationProbe.dir().resolve("result.txt"),Boolean.getBoolean("blockbranch.hutExploreLocateOnly")?"PASS native connected cold locate trace":"PASS native connected movement and locate trace");client.stop();}
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("HUT_EXPLORE_CLIENT_FAIL",t);try{Files.writeString(HutExplorationProbe.dir().resolve("result.txt"),"FAIL "+t);}catch(Exception ignored){}client.stop();}});}
}
