package blockbranch.client.realistic;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
public final class TreasureView {
 private static volatile boolean posed;private static boolean opened,saving;private static int settled;private static long began;private static BlockPos target;
 public static void register(){
  ServerTickEvents.END_SERVER_TICK.register(server->{if(server.isDedicatedServer()||posed||server.getPlayerList().getPlayers().isEmpty())return;try{
   var q=Files.readString(Path.of(System.getProperty("blockbranch.treasureMarker"))).trim().split(",");int x=Integer.parseInt(q[0]),y=Integer.parseInt(q[1]),z=Integer.parseInt(q[2]);target=new BlockPos(x,y,z);
   var player=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();var commands=server.getCommands();var source=server.createCommandSourceStack();
   commands.performPrefixedCommand(source,"time set 6000");commands.performPrefixedCommand(source,"weather clear");commands.performPrefixedCommand(source,"gamemode creative "+player.getName().getString());
   // A disposable review excavation, not generated surface decoration.
   for(int dy=y+8;dy>y;dy--)for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)player.gameMode.destroyBlock(new BlockPos(x+dx,dy,z+dz));
   player.setPos(x+.5,y+1,z+1.5);player.gameMode.useItemOn(player,level,player.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(target),Direction.UP,target,false));
   if(player.containerMenu==player.inventoryMenu)throw new AssertionError("Native player opens excavated treasure chest");player.closeContainer();
   commands.performPrefixedCommand(source,"tp "+player.getName().getString()+" "+(x+4.5)+" "+(y+8)+" "+(z+5.5)+" 141 52");player.getAbilities().flying=true;player.onUpdateAbilities();posed=true;
  }catch(Exception e){throw new RuntimeException(e);}});
  ClientTickEvents.END_CLIENT_TICK.register(client->{try{
   if(opened&&System.nanoTime()-began>300_000_000_000L){Files.writeString(client.gameDirectory.toPath().resolve("result.txt"),"FAIL native render timeout");client.stop();return;}
   if(!opened&&client.gui.screen() instanceof TitleScreen){opened=true;began=System.nanoTime();client.createWorldOpenFlows().openWorld("TreasureReview",()->client.gui.setScreen(new TitleScreen()));return;}
   if(!posed||client.level==null||client.player==null||saving)return;
   if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.gui.toastManager().clear();var area=client.levelRenderer.viewArea();if(area==null)return;
   int ready=0;for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++){boolean visible=false;
    for(int dy=-16;dy<=32;dy+=16){var section=area.getRenderSectionAt(target.offset(x*16,dy,z*16));visible|=section!=null&&section.getSectionMesh().hasRenderableLayers();}if(visible)ready++;}
   var chestMesh=area.getRenderSectionAt(target);if(chestMesh==null||!chestMesh.getSectionMesh().hasRenderableLayers())return;
   if(ready<20){settled=0;return;}if(++settled<100)return;saving=true;
   net.minecraft.client.Screenshot.grab(client.gameDirectory,"buried-treasure-excavated.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{Files.writeString(client.gameDirectory.toPath().resolve("result.txt"),"PASS native chest opened; native excavated view; ready columns >= 20 plus chest section");client.stop();}catch(Exception e){throw new RuntimeException(e);}}));
  }catch(Exception e){throw new RuntimeException(e);}});
 }
}
