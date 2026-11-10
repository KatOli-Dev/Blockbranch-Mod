package blockbranch.client.realistic;
import blockbranch.Blockbranch;
import blockbranch.wildlife.BrownBear;
import blockbranch.wildlife.WildlifeProbe;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Existing full readiness gate plus adult/cub metric review and connected-player check. */
public final class BearClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final FixtureSceneReadiness readiness=new FixtureSceneReadiness();
        final long deadline=System.nanoTime()+600_000_000_000L;int settled;boolean saving,done,captured,playerCheckSent,playerCheckDone;String playerFailure;blockbranch.client.wildlife.BearAudioReview audio;
        {ClientPlayNetworking.registerGlobalReceiver(WildlifeProbe.TYPE,(packet,context)->context.client().execute(()->{if(packet.stage()==0)playerCheckDone=true;else if(packet.stage()<0)playerFailure=packet.message();}));}
        WildlifeCloseups closeups;boolean closeupsDone;
        public void onEndTick(net.minecraft.client.Minecraft client){if(done||saving)return;try{
            if(System.nanoTime()>deadline)throw new AssertionError("Bear review timeout "+readiness.diagnostic());
            if(captured){
                if(!closeupsDone){if(closeups==null)closeups=new WildlifeCloseups(client,BrownBear.class,"bear");if(!closeups.tick(client))return;closeupsDone=true;}
                if(audio!=null){if(!audio.tick(client))return;audio=null;}
                if(playerFailure!=null)throw new AssertionError(playerFailure);
                if(!playerCheckSent){playerCheckSent=true;ClientPlayNetworking.send(new WildlifeProbe(10,""));return;}
                if(!playerCheckDone)return;
                Files.writeString(Path.of(System.getProperty("blockbranch.bearClientResult")),"PASS\n");done=true;Blockbranch.LOGGER.info("BEAR_CLIENT_CAPTURE PASS");client.stop();return;
            }
            if(client.level==null||client.player==null)return;
            var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;if(frustum==null)return;
            int subjects=0,natural=0;
            for(var entity:client.level.entitiesForRendering())if(entity instanceof BrownBear animal){
                if(!animal.isNoAi())natural++;
                if(animal.isNoAi()&&animal.distanceToSqr(client.player)<400&&frustum.isVisible(animal.getBoundingBox()))subjects++;
            }
            if(subjects!=3)return;
            if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Normal visibility required");
            client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
            if(!readiness.ready(client,"brown-bear-family")){settled=0;return;}if(++settled<60)return;
            for(var entity:client.level.entitiesForRendering())if(entity instanceof BrownBear animal&&animal.isNoAi()&&animal.distanceToSqr(client.player)<400)blockbranch.client.wildlife.BearScaleAudit.measure(client,animal);
            Blockbranch.LOGGER.info("BEAR_CAPTURE_READY subjects={} naturallyTrackedBear={} {}",subjects,natural,readiness.diagnostic());saving=true;
            net.minecraft.client.Screenshot.grab(client.gameDirectory,"brown-bear-family.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{
                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/brown-bear-family.png")))throw new AssertionError("Bear PNG missing");
                captured=true;saving=false;if(Boolean.getBoolean("blockbranch.bearAudioReview"))audio=new blockbranch.client.wildlife.BearAudioReview();
            }catch(Throwable error){done=true;Blockbranch.LOGGER.error("BEAR_CLIENT FAIL",error);client.stop();}}));
        }catch(Throwable error){done=true;Blockbranch.LOGGER.error("BEAR_CLIENT FAIL",error);client.stop();}}
    });}
    private BearClientSmoke(){}
}
