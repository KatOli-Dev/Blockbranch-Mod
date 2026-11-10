package blockbranch.client.metallurgy;

import blockbranch.metallurgy.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Native captures with real inventory sync, default host sprites and a host override pack. */
public final class ZincBrassViewClient {
    public static void register(){
        var directory=Path.of(System.getProperty("blockbranch.metallurgyViewDirectory"));boolean pack=Boolean.getBoolean("blockbranch.metallurgyViewPack");long deadline=System.nanoTime()+150_000_000_000L;int[] ticks={0},view={0};boolean[] saving={false},done={false};
        net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents.DISCONNECT.register((handler,client)->{if(!done[0]){done[0]=true;write(directory,"FAIL login disconnected");client.stop();}});
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{if(!done[0]){done[0]=true;write(directory,"FAIL play disconnected: "+handler.getConnection().getDisconnectionDetails());client.stop();}});
        ClientTickEvents.END_CLIENT_TICK.register(client->{if(done[0]||saving[0])return;try{
            if(System.nanoTime()>deadline)throw new AssertionError("zinc/brass native capture deadline");
            if(client.player==null||client.level==null||!client.player.getInventory().getItem(0).is(MetallurgyContent.BRONZE_PICKAXE))return;
            client.player.getAbilities().flying=true;client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);client.player.getInventory().setSelectedSlot(0);
            if(client.player.position().distanceToSqr(0,67.25,-8.5)>.04||Math.abs(client.player.getYRot())>1||Math.abs(client.player.getXRot()-12)>1){ticks[0]=0;return;}
            if(!client.level.getBlockState(new net.minecraft.core.BlockPos(-3,66,1)).is(ZincBrassContent.ZINC_ORE)){ticks[0]=0;return;}
            if(view[0]<2){
                var expected=java.util.List.of(ZincBrassContent.ZINC_ORE.asItem(),ZincBrassContent.DEEPSLATE_ZINC_ORE.asItem(),ZincBrassContent.RAW_ZINC,ZincBrassContent.ZINC_INGOT,ZincBrassContent.BRASS_INGOT,ZincBrassContent.BRASS_BLOCK.asItem(),ZincBrassContent.BRASS_SLAB.asItem(),ZincBrassContent.BRASS_STAIRS.asItem(),ZincBrassContent.BRASS_BARS.asItem());
                for(int i=0;i<expected.size();i++)if(!client.player.getInventory().getItem(i+1).is(expected.get(i)))throw new AssertionError("native zinc/brass inventory sync");
            }
            if(client.gui.hud.isHidden()!=(view[0]==0))client.gui.hud.toggle();
            if(view[0]>=2){var bars=client.player.getInventory().getItem(9);if(!bars.is(ZincBrassContent.BRASS_BARS.asItem()))throw new AssertionError("real brass bar inventory sync");client.player.getInventory().setItem(8,bars.copy());}
            if(++ticks[0]<100)return;
            if(client.getConnection().getConnection().isMemoryConnection())throw new AssertionError("real TCP expected");
            if(pack){var resource=client.getResourceManager().getResource(net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/stone.png")).orElseThrow();if(!resource.sourcePackId().contains("tin-host-test"))throw new AssertionError("override host pack not selected");}
            saving[0]=true;String name=(pack?"retexture":"base")+(view[0]==0?"-blocks.png":view[0]==1?"-items.png":"-building.png");
            net.minecraft.client.Screenshot.grab(client.gameDirectory,name,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                if(!Files.exists(directory.resolve("screenshots/"+name))){write(directory,"FAIL native screenshot missing");done[0]=true;client.stop();return;}
                if(++view[0]>=3){write(directory,"PASS zinc/brass real TCP inventory, native models and hostPack="+pack);done[0]=true;client.stop();}ticks[0]=0;saving[0]=false;
            }));
        }catch(Throwable error){error.printStackTrace();write(directory,"FAIL "+error);done[0]=true;client.stop();}});
    }
    private static void write(Path directory,String result){try{Files.writeString(directory.resolve("result.txt"),result+"\n");}catch(Exception e){throw new RuntimeException(e);}}
    private ZincBrassViewClient(){}
}
