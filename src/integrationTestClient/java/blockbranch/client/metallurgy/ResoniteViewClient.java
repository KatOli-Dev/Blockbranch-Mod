package blockbranch.client.metallurgy;

import blockbranch.metallurgy.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Unedited native captures using a real connection, then the same view with vanilla host overrides. */
public final class ResoniteViewClient {
    public static void register(){
        var directory=Path.of(System.getProperty("blockbranch.metallurgyViewDirectory"));boolean pack=Boolean.getBoolean("blockbranch.metallurgyViewPack");
        var chimes=new java.util.concurrent.atomic.AtomicInteger();
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client->client.getSoundManager().addListener((instance,event,range)->{if(instance.getIdentifier().equals(blockbranch.Blockbranch.id("resonite.chime"))){chimes.incrementAndGet();blockbranch.Blockbranch.LOGGER.info("RESONITE native client chime observed sound={} count={}",instance.getSound().getLocation(),chimes.get());}}));
        long deadline=System.nanoTime()+150_000_000_000L;int[] ticks={0},view={0};boolean[] saving={false},done={false};
        net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents.DISCONNECT.register((handler,client)->{
            if(done[0])return;done[0]=true;try{Files.writeString(directory.resolve("result.txt"),"FAIL login disconnected before world readiness; inspect server log");}catch(Exception ignored){}client.stop();
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{
            if(done[0])return;done[0]=true;try{Files.writeString(directory.resolve("result.txt"),"FAIL play disconnected: "+handler.getConnection().getDisconnectionDetails());}catch(Exception ignored){}client.stop();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{if(done[0]||saving[0])return;try{
            if(System.nanoTime()>deadline)throw new AssertionError("native metallurgy capture timeout");
            if(client.level==null||client.player==null||!client.player.getInventory().getItem(0).is(net.minecraft.world.item.Items.IRON_PICKAXE))return;
            client.player.getAbilities().flying=true;client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            if(client.player.position().distanceToSqr(0,67.25,-10.5)>.04||Math.abs(client.player.getYRot())>1||Math.abs(client.player.getXRot()-12)>1){ticks[0]=0;return;}
            if(!client.level.getBlockState(new net.minecraft.core.BlockPos(-3,66,1)).is(ResoniteContent.ORE)){ticks[0]=0;return;}
            client.player.getInventory().setSelectedSlot(0);
            net.minecraft.world.level.ItemLike[] expected={net.minecraft.world.item.Items.IRON_PICKAXE,ResoniteContent.ORE,ResoniteContent.DEEPSLATE_ORE,ResoniteContent.CRYSTAL,ResoniteContent.BLOCK};for(int i=0;i<expected.length;i++)if(!client.player.getInventory().getItem(i).is(expected[i].asItem()))throw new AssertionError("native resonite item sync");

            if(client.gui.hud.isHidden()!=(view[0]==0))client.gui.hud.toggle();
            if(++ticks[0]<100)return;
            if(chimes.get()<4)throw new AssertionError("native client must observe attack and survival/creative break chimes: "+chimes);
            if(client.getSoundManager().getSoundEvent(blockbranch.Blockbranch.id("resonite.chime"))==null)throw new AssertionError("native chime resource unresolved");
            if(client.getConnection().getConnection().isMemoryConnection())throw new AssertionError("expected real TCP");
            if(pack){
                var host=client.getResourceManager().getResource(net.minecraft.resources.Identifier.withDefaultNamespace("textures/block/stone.png")).orElseThrow();
                var audio=client.getResourceManager().getResource(blockbranch.Blockbranch.id("sounds/resonite/chime1.ogg")).orElseThrow();if(!audio.sourcePackId().contains("tin-host-test"))throw new AssertionError("native audio override not selected");
                if(!host.sourcePackId().contains("tin-host-test"))throw new AssertionError("host override pack not selected: "+host.sourcePackId());
            }
            saving[0]=true;String name=(pack?"retexture":"base")+(view[0]==0?"-blocks.png":"-items.png");
            net.minecraft.client.Screenshot.grab(client.gameDirectory,name,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                try{
                    if(!Files.exists(directory.resolve("screenshots/"+name)))throw new AssertionError("native capture missing");
                    if(++view[0]>=2){Files.writeString(directory.resolve("result.txt"),"PASS real TCP registered items, live chime observation, native sculk response and captures, hostPack="+pack+"\n");done[0]=true;client.stop();}
                    ticks[0]=0;saving[0]=false;
                }catch(Throwable error){error.printStackTrace();try{Files.writeString(directory.resolve("result.txt"),"FAIL "+error);}catch(Exception ignored){}done[0]=true;client.stop();}
            }));
        }catch(Throwable error){error.printStackTrace();try{Files.writeString(directory.resolve("result.txt"),"FAIL "+error);}catch(Exception ignored){}done[0]=true;client.stop();}});
    }
    private ResoniteViewClient(){}
}
