package blockbranch.client.metallurgy;

import blockbranch.metallurgy.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Unedited native captures using a real connection, then the same view with vanilla host overrides. */
public final class UraniumViewClient {
    public static void register(){
        var directory=Path.of(System.getProperty("blockbranch.metallurgyViewDirectory"));boolean pack=Boolean.getBoolean("blockbranch.metallurgyViewPack");
        boolean axeOnly=Boolean.getBoolean("blockbranch.uraniumAxeView");
        boolean[] heartsChecked={false};
        long deadline=System.nanoTime()+150_000_000_000L;int[] ticks={0},view={axeOnly?3:0};boolean[] saving={false},done={false};
        net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents.DISCONNECT.register((handler,client)->{
            if(done[0])return;done[0]=true;try{Files.writeString(directory.resolve("result.txt"),"FAIL login disconnected before world readiness; inspect server log");}catch(Exception ignored){}client.stop();
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{
            if(done[0])return;done[0]=true;try{Files.writeString(directory.resolve("result.txt"),"FAIL play disconnected: "+handler.getConnection().getDisconnectionDetails());}catch(Exception ignored){}client.stop();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{if(done[0]||saving[0])return;try{
            if(System.nanoTime()>deadline)throw new AssertionError("native metallurgy capture timeout");
            if(client.level==null||client.player==null||!client.player.getInventory().getItem(8).is(UraniumContent.SPEAR))return;
            if(!client.player.isAlive())throw new AssertionError("capture fixture died before completion");
            client.player.getAbilities().flying=true;client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            if(client.player.position().distanceToSqr(0,67.25,-10.5)>.04||Math.abs(client.player.getYRot())>1||Math.abs(client.player.getXRot()-12)>1){ticks[0]=0;return;}
            if(!client.level.getBlockState(new net.minecraft.core.BlockPos(-3,66,1)).is(UraniumContent.ORE)){ticks[0]=0;return;}
            client.player.getInventory().setSelectedSlot(view[0]==2?6:view[0]==3?7:view[0]==4?8:view[0]==5?3:0);
            net.minecraft.world.level.ItemLike[] expected={net.minecraft.world.item.Items.DIAMOND_PICKAXE,UraniumContent.ORE,UraniumContent.DEEPSLATE_ORE,UraniumContent.RAW,UraniumContent.INGOT,UraniumContent.TIP,UraniumContent.SWORD,UraniumContent.AXE,UraniumContent.SPEAR};for(int i=0;i<expected.length;i++)if(!client.player.getInventory().getItem(i).is(expected[i].asItem()))throw new AssertionError("native uranium item sync");
            if(client.gui.hud.isHidden()!=(view[0]==0))client.gui.hud.toggle();
            if(++ticks[0]<(view[0]==0?100:40))return;
            if(!client.player.hasEffect(UraniumContent.RADIATION)||client.player.getArmorValue()!=19||client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).getDamageValue()!=3)throw new AssertionError("native radiation/armor/wear sync");
            if(!heartsChecked[0]){checkHearts(client.player);heartsChecked[0]=true;}
            if(client.getConnection().getConnection().isMemoryConnection())throw new AssertionError("expected real TCP");
            if(pack){
                var host=client.getResourceManager().getResource(net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/stone.png")).orElseThrow();
                var overlay=client.getResourceManager().getResource(blockbranch.Blockbranch.id("textures/item/uranium_sword_tip.png")).orElseThrow();if(!overlay.sourcePackId().contains("tin-host-test"))throw new AssertionError("native weapon-overlay override not selected");
                if(!host.sourcePackId().contains("tin-host-test"))throw new AssertionError("host override pack not selected: "+host.sourcePackId());
            }
            saving[0]=true;String name=(pack?"retexture":"base")+new String[]{"-blocks.png","-items.png","-sword.png","-axe.png","-spear.png","-radiation.png"}[view[0]];
            net.minecraft.client.Screenshot.grab(client.gameDirectory,name,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                try{
                    if(!Files.exists(directory.resolve("screenshots/"+name)))throw new AssertionError("native capture missing");
                    if(axeOnly||++view[0]>=6){Files.writeString(directory.resolve("result.txt"),"PASS real TCP registered items, native uranium/radiation/wear sync, green/wither heart controls and captures, hostPack="+pack+"\n");done[0]=true;client.stop();}
                    ticks[0]=0;saving[0]=false;
                }catch(Throwable error){error.printStackTrace();try{Files.writeString(directory.resolve("result.txt"),"FAIL "+error);}catch(Exception ignored){}done[0]=true;client.stop();}
            }));
        }catch(Throwable error){error.printStackTrace();try{Files.writeString(directory.resolve("result.txt"),"FAIL "+error);}catch(Exception ignored){}done[0]=true;client.stop();}});
    }
    private static void checkHearts(net.minecraft.world.entity.player.Player player)throws Exception{
        var type=Class.forName("net.minecraft.client.gui.Hud$HeartType");var method=type.getDeclaredMethod("forPlayer",net.minecraft.world.entity.player.Player.class);method.setAccessible(true);var effect=new net.minecraft.world.effect.MobEffectInstance(player.getEffect(UraniumContent.RADIATION));
        if(!((Enum<?>)method.invoke(null,player)).name().equals("POISIONED"))throw new AssertionError("radiation must use native green heart style");
        player.removeEffect(UraniumContent.RADIATION);player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER,200));if(!((Enum<?>)method.invoke(null,player)).name().equals("WITHERED"))throw new AssertionError("ordinary wither hearts unchanged");player.addEffect(effect);if(!((Enum<?>)method.invoke(null,player)).name().equals("POISIONED"))throw new AssertionError("active radiation remains green alongside wither");player.removeEffect(net.minecraft.world.effect.MobEffects.WITHER);
    }
    private UraniumViewClient(){}
}
