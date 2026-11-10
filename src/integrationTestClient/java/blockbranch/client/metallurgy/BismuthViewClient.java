package blockbranch.client.metallurgy;

import blockbranch.metallurgy.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.*;

/** Real TCP/native survival mining and unedited captures, with bounded expiry/control probes. */
public final class BismuthViewClient {
 public static void register(){
  var d=Path.of(System.getProperty("blockbranch.metallurgyViewDirectory"));boolean pack=Boolean.getBoolean("blockbranch.metallurgyViewPack");boolean art=Boolean.getBoolean("blockbranch.pickaxeArtView");int[] stage={0},ticks={0},mining={0},gallery={0};boolean[] checked={false};boolean[] done={false},saving={false};long deadline=System.nanoTime()+180_000_000_000L;
  ClientTickEvents.END_CLIENT_TICK.register(c->{if(done[0]||saving[0])return;try{
   if(System.nanoTime()>deadline)throw new AssertionError("bismuth native view timeout stage="+stage[0]);
   if(c.player==null||c.level==null||!c.player.isAlive()||stage[0]<6&&!c.player.getInventory().getItem(1).is(BismuthContent.RAW))return;
   if(c.player.position().distanceToSqr(0,65,-5.5)>.04)return;
   if(!c.level.getBlockState(new BlockPos(-6,65,0)).is(BismuthContent.ORE)||!c.level.getBlockState(new BlockPos(0,66,0)).is(net.minecraft.world.level.block.Blocks.STONE))return;
   if(art&&!checked[0]){PickaxeSpriteCheck.verify(c.getResourceManager());IngotSpriteCheck.verify(c.getResourceManager());GearSpriteCheck.verify(c.getResourceManager());checked[0]=true;}
   var remainingField=OrePulseClient.class.getDeclaredField("remaining");remainingField.setAccessible(true);int remaining=remainingField.getInt(null);var positionsField=OrePulseClient.class.getDeclaredField("positions");positionsField.setAccessible(true);var positions=(List<?>)positionsField.get(null);
   if(stage[0]==0){c.player.getInventory().setSelectedSlot(0);if(remaining!=0)throw new AssertionError("no pulse before mining");if(++ticks[0]<100)return;if(c.getConnection().getConnection().isMemoryConnection())throw new AssertionError("real TCP required");if(pack&&!c.getResourceManager().getResource(net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/stone.png")).orElseThrow().sourcePackId().contains("tin-host-test"))throw new AssertionError("host pack inactive");capture(c,d,pack,"before",saving,()->{stage[0]=1;ticks[0]=0;});return;}
   if(stage[0]==1){if(remaining<=0){mine(c,new BlockPos(0,65,-3),0,mining);return;}if(!positions.containsAll(List.of(new BlockPos(-2,66,1),new BlockPos(2,66,1))))throw new AssertionError("both buried true and false diamond marked");if(c.player.getInventory().getItem(0).getDamageValue()!=1)return;capture(c,d,pack,"pulse",saving,()->{stage[0]=2;ticks[0]=0;});return;}
   if(stage[0]==2){if(++ticks[0]<OrePulse.DURATION+10)return;if(remaining!=0||!positions.isEmpty())throw new AssertionError("pulse must expire");capture(c,d,pack,"expired",saving,()->{stage[0]=3;mining[0]=0;ticks[0]=0;});return;}
   if(stage[0]==3){mine(c,new BlockPos(1,65,-3),5,mining);if(!c.level.getBlockState(new BlockPos(1,65,-3)).isAir())return;if(++ticks[0]<20)return;if(remaining!=0)throw new AssertionError("ordinary steel cannot pulse");stage[0]=4;mining[0]=0;ticks[0]=0;return;}
   if(stage[0]==4){mine(c,new BlockPos(-1,65,-3),6,mining);if(remaining<=0||!c.player.getInventory().getItem(6).isEmpty())return;stage[0]=5;ticks[0]=0;return;}
   if(stage[0]==5){if(++ticks[0]<OrePulse.DURATION+10)return;if(remaining!=0||!positions.isEmpty())throw new AssertionError("broken-tool pulse expires");if(art){c.player.getInventory().setSelectedSlot(8);stage[0]=6;ticks[0]=0;return;}Files.writeString(d.resolve("result.txt"),"PASS real TCP native survival mining/wear, buried generic markers, expiry, ordinary-tool control, last-use tool break; hostPack="+pack+"\n");done[0]=true;c.stop();}
   if(stage[0]==6){
    if(!c.player.getInventory().getItem(0).is(net.minecraft.world.item.Items.IRON_PICKAXE)||!c.player.getInventory().getItem(8).is(BismuthContent.PICKAXE))return;
    c.player.getInventory().setSelectedSlot(gallery[0]);if(++ticks[0]<30)return;
    String[] names={"vanilla_iron","bronze","steel","tungsten_steel","stainless_steel","silver","platinum_alloy","titanium_alloy","bismuth_steel"};
    capture(c,d,pack,"pickaxe-"+names[gallery[0]],saving,()->{ticks[0]=0;if(++gallery[0]>=names.length){stage[0]=7;saving[0]=true;c.reloadResourcePacks().thenRun(()->c.execute(()->{try{PickaxeSpriteCheck.verify(c.getResourceManager());IngotSpriteCheck.verify(c.getResourceManager());GearSpriteCheck.verify(c.getResourceManager());Files.writeString(d.resolve("result.txt"),"PASS native bismuth gameplay, seven vanilla-derived pickaxe sprites and inlay, nine vanilla/mod captures, direct/scale/fallback and actual resource reload; hostPack="+pack+"\n");done[0]=true;c.stop();}catch(Throwable e){e.printStackTrace();try{Files.writeString(d.resolve("result.txt"),"FAIL "+e);}catch(Exception ignored){}done[0]=true;c.stop();}}));}});return;
   }
  }catch(Throwable e){e.printStackTrace();try{Files.writeString(d.resolve("result.txt"),"FAIL "+e);}catch(Exception ignored){}done[0]=true;c.stop();}});
 }
 private static void mine(net.minecraft.client.Minecraft c,BlockPos pos,int slot,int[] mining){c.player.getInventory().setSelectedSlot(slot);if(c.level.getBlockState(pos).isAir())return;if(mining[0]++==0)c.gameMode.startDestroyBlock(pos,Direction.NORTH);else c.gameMode.continueDestroyBlock(pos,Direction.NORTH);}
 private static void capture(net.minecraft.client.Minecraft c,Path d,boolean pack,String suffix,boolean[] saving,Runnable next){saving[0]=true;String name=(pack?"retexture":"base")+"-"+suffix+".png";net.minecraft.client.Screenshot.grab(c.gameDirectory,name,c.gameRenderer.mainRenderTarget(),1,message->c.execute(()->{if(!Files.exists(d.resolve("screenshots/"+name)))throw new AssertionError("capture missing");next.run();saving[0]=false;}));}
 private BismuthViewClient(){}
}
