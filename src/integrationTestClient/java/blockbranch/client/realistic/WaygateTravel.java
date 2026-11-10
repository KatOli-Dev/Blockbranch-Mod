package blockbranch.client.realistic;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import blockbranch.realistic.DesertWaygates;
/** Disposable connected-player round trip through the already repaired native frame. */
public final class WaygateTravel {
 private static boolean opened;private static volatile boolean done,sawNether;private static volatile String failure;private static int phase,ticks;private static long began;private static DesertWaygates.Plan source;
 public static void register(){
  ServerTickEvents.END_SERVER_TICK.register(server->{if(server.isDedicatedServer()||done||failure!=null||server.getPlayerList().getPlayers().isEmpty())return;try{
   var player=server.getPlayerList().getPlayers().getFirst();
   if(phase==0){
    var data=com.google.gson.JsonParser.parseString(Files.readString(Path.of(System.getProperty("blockbranch.waygateNativeReport")))).getAsJsonObject();source=new com.google.gson.Gson().fromJson(data.get("witness"),DesertWaygates.Plan.class);var pos=source.local(6,4,7);var level=server.overworld();if(!level.getBlockState(pos).is(Blocks.NETHER_PORTAL))throw new AssertionError("Saved repaired source frame active");
    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamemode creative "+player.getName().getString());player.getAbilities().flying=true;player.onUpdateAbilities();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"execute in minecraft:overworld run tp "+player.getName().getString()+" "+(pos.getX()+.5)+" "+pos.getY()+" "+(pos.getZ()+.5));player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);player.setPortalCooldown(0);phase=1;ticks=0;
   }else if(phase==1&&player.level().dimension()==Level.NETHER){phase=2;ticks=0;}
   else if(phase==2&&++ticks>=20&&sawNether){
    BlockPos portal=null;var center=player.blockPosition();outer:for(var p:BlockPos.betweenClosed(center.offset(-8,-8,-8),center.offset(8,8,8)))if(player.level().getBlockState(p).is(Blocks.NETHER_PORTAL)){portal=p.immutable();break outer;}
    if(portal==null)throw new AssertionError("Native Nether exit portal exists");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"execute in minecraft:the_nether run tp "+player.getName().getString()+" "+(portal.getX()+.5)+" "+portal.getY()+" "+(portal.getZ()+.5));player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);player.setPortalCooldown(0);phase=3;ticks=0;
   }else if(phase==3&&player.level().dimension()==Level.OVERWORLD){
    if(Math.hypot(player.getX()-source.x(),player.getZ()-source.z())>128)throw new AssertionError("Native return remains near original gateway");if(!server.overworld().getBlockState(source.local(6,4,7)).is(Blocks.NETHER_PORTAL))throw new AssertionError("Original repaired portal remains intact");server.saveEverything(false,true,true);done=true;
   }
  }catch(Throwable t){failure=t.toString();blockbranch.Blockbranch.LOGGER.error("WAYGATE_TRAVEL_FAIL",t);}});
  ClientTickEvents.END_CLIENT_TICK.register(client->{try{
   if(!opened&&client.gui.screen() instanceof TitleScreen){opened=true;began=System.nanoTime();client.createWorldOpenFlows().openWorld("WaygateReview",()->client.gui.setScreen(new TitleScreen()));return;}
   if(client.level!=null&&client.level.dimension()==Level.NETHER)sawNether=true;
   boolean finished=done&&client.level!=null&&client.level.dimension()==Level.OVERWORLD;
   if(finished||failure!=null||opened&&System.nanoTime()-began>600_000_000_000L){Files.writeString(client.gameDirectory.toPath().resolve("travel-result.txt"),finished?"PASS real connected player entered vanilla Nether through repaired waygate and returned within 128m; original frame retained":"FAIL "+failure+" phase="+phase);client.stop();}
  }catch(Exception e){throw new RuntimeException(e);}});
 }
}
