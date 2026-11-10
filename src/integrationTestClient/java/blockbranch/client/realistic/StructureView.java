package blockbranch.client.realistic;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
public final class StructureView {
 private static volatile boolean posed,cacheRestored;private static boolean opened,saving;private static int settled;private static BlockPos target;
 public static void register(){
  ServerTickEvents.END_SERVER_TICK.register(server->{if(server.isDedicatedServer()||posed||server.getPlayerList().getPlayers().isEmpty())return;try{
   var q=Files.readString(Path.of(System.getProperty("blockbranch.wellMarker"))).trim().split(",");int x=Integer.parseInt(q[0]),y=Integer.parseInt(q[1]),z=Integer.parseInt(q[2]);target=new BlockPos(x,y,z);
   var generator=server.overworld().getChunkSource().getGenerator();if(generator instanceof blockbranch.realistic.RealisticGenerator realistic&&realistic.structureDefinition()==3)cacheRestored=realistic.wells(server.overworld().getChunkSource().randomState()).restoredFromSave();
   var p=server.getPlayerList().getPlayers().getFirst();var source=server.createCommandSourceStack();var cmd=server.getCommands();
   cmd.performPrefixedCommand(source,"time set 6000");cmd.performPrefixedCommand(source,"weather clear");cmd.performPrefixedCommand(source,"gamemode creative "+p.getName().getString());
   boolean close=Boolean.getBoolean("blockbranch.structureViewClose");
   cmd.performPrefixedCommand(source,"tp "+p.getName().getString()+" "+(x+(close?8.5:15.5))+" "+(y+(close?3:10))+" "+(z+(close?10.5:19.5))+" 141 "+(close?12:22));p.getAbilities().flying=true;p.onUpdateAbilities();posed=true;
  }catch(Exception e){throw new RuntimeException(e);}});
  ClientTickEvents.END_CLIENT_TICK.register(client->{try{
   if(!opened&&client.gui.screen() instanceof TitleScreen){opened=true;client.createWorldOpenFlows().openWorld("StructureReview",()->client.gui.setScreen(new TitleScreen()));return;}
   if(!posed||client.level==null||client.player==null||saving)return;
   if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.gui.toastManager().clear();
   var area=client.levelRenderer.viewArea();if(area==null)return;
   int ready=0;for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++){var pos=target.offset(x*16,0,z*16);var mesh=area.getRenderSectionAt(pos);if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())ready++;}
   if(ready<20){settled=0;return;}if(++settled<100)return;saving=true;
   net.minecraft.client.Screenshot.grab(client.gameDirectory,Boolean.getBoolean("blockbranch.structureViewClose")?"well-close.png":"well-native.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{Files.writeString(client.gameDirectory.toPath().resolve("result.txt"),"PASS native rendered well; ready sections >= 20; cacheRestored="+cacheRestored);client.stop();}catch(Exception e){throw new RuntimeException(e);}}));
  }catch(Exception e){throw new RuntimeException(e);}});
 }
}
