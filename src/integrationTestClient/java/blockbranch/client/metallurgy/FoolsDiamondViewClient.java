package blockbranch.client.metallurgy;
import blockbranch.metallurgy.FoolsDiamondContent;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
/** Real TCP overview, inventory and authoritative native close views of both diamond hosts. */
public final class FoolsDiamondViewClient {
 public static void register(){
  var dir=Path.of(System.getProperty("blockbranch.metallurgyViewDirectory"));boolean pack=Boolean.getBoolean("blockbranch.metallurgyViewPack");long deadline=System.nanoTime()+150_000_000_000L;int[] ticks={0},view={0};boolean[] saving={false},done={false};
  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{if(!done[0]){done[0]=true;write(dir,"FAIL disconnected: "+handler.getConnection().getDisconnectionDetails());client.stop();}});
  net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents.DISCONNECT.register((handler,client)->{if(!done[0]){done[0]=true;write(dir,"FAIL login disconnected");client.stop();}});
  ClientTickEvents.END_CLIENT_TICK.register(client->{if(done[0]||saving[0])return;try{
   if(System.nanoTime()>deadline)throw new AssertionError("fools comparison deadline");if(client.player==null||client.level==null||!client.player.getInventory().getItem(1).is(FoolsDiamondContent.ORE.asItem()))return;
   client.player.getAbilities().flying=true;client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);client.player.getInventory().setSelectedSlot(8);
   double x=view[0]<2?0:view[0]==2?-2:2,y=view[0]<2?67.25:65,z=view[0]<2?-8.5:-2.5;float pitch=view[0]<2?12:0;
   if(client.player.position().distanceToSqr(x,y,z)>.04||Math.abs(client.player.getYRot())>1||Math.abs(client.player.getXRot()-pitch)>1){ticks[0]=0;return;}
   if(!client.level.getBlockState(new net.minecraft.core.BlockPos(-2,66,1)).is(FoolsDiamondContent.ORE)){ticks[0]=0;return;}
   if(!client.player.getInventory().getItem(2).is(FoolsDiamondContent.DEEPSLATE_ORE.asItem())||!client.player.getInventory().getItem(3).is(FoolsDiamondContent.DUST))throw new AssertionError("real new item sync");
   if(client.gui.hud.isHidden()!=(view[0]!=1))client.gui.hud.toggle();if(++ticks[0]<100)return;
   if(client.getConnection().getConnection().isMemoryConnection())throw new AssertionError("real TCP required");if(pack){var res=client.getResourceManager().getResource(net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/diamond_ore.png")).orElseThrow();if(!res.sourcePackId().contains("tin-host-test"))throw new AssertionError("diamond override pack not selected");}
   saving[0]=true;String name=(pack?"retexture":"base")+switch(view[0]){case 0->"-blocks.png";case 1->"-items.png";case 2->"-stone-close.png";default->"-deep-close.png";};
   net.minecraft.client.Screenshot.grab(client.gameDirectory,name,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{if(!Files.exists(dir.resolve("screenshots/"+name)))throw new AssertionError("native image missing");if(++view[0]>=(pack?2:4)){write(dir,"PASS real TCP default diamond comparisons / native pack inheritance="+pack);done[0]=true;client.stop();}else if(view[0]>=2)client.getConnection().sendCommand("tp @s "+(view[0]==2?"-2.0":"2.0")+" 65.0 -2.5 0 0");ticks[0]=0;saving[0]=false;}catch(Throwable e){e.printStackTrace();write(dir,"FAIL "+e);done[0]=true;client.stop();}}));
  }catch(Throwable e){e.printStackTrace();write(dir,"FAIL "+e);done[0]=true;client.stop();}});
 }
 private static void write(Path d,String s){try{Files.writeString(d.resolve("result.txt"),s+"\n");}catch(Exception e){throw new RuntimeException(e);}}
 private FoolsDiamondViewClient(){}
}
