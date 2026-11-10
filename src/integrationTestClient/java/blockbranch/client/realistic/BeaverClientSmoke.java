package blockbranch.client.realistic;
import blockbranch.Blockbranch;
import blockbranch.wildlife.EurasianBeaver;
import blockbranch.wildlife.WildlifeProbe;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Full readiness, metric family and close-ups, native audio and water/land review. */
public final class BeaverClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final FixtureSceneReadiness readiness=new FixtureSceneReadiness();
        final long deadline=System.nanoTime()+600_000_000_000L;int settled;boolean saving,done,captured;
        int behavior;int readyBehavior;String behaviorError;blockbranch.client.wildlife.BeaverAudioReview audio;
        {ClientPlayNetworking.registerGlobalReceiver(WildlifeProbe.TYPE,(packet,context)->context.client().execute(()->{if(packet.stage()<0)behaviorError=packet.message();else readyBehavior=packet.stage();}));}
        WildlifeCloseups closeups;boolean closeupsDone;
        public void onEndTick(net.minecraft.client.Minecraft client){if(done||saving)return;try{
            if(System.nanoTime()>deadline)throw new AssertionError("Beaver review timeout "+readiness.diagnostic());
            if(captured){
                if(!closeupsDone){if(closeups==null)closeups=new WildlifeCloseups(client,EurasianBeaver.class,"beaver");if(!closeups.tick(client))return;closeupsDone=true;}
                if(audio!=null){if(!audio.tick(client))return;audio=null;}
                if(behaviorError!=null)throw new AssertionError(behaviorError);
                if(behavior==0){behavior=1;ClientPlayNetworking.send(new WildlifeProbe(20,""));return;}
                if(behavior==1&&readyBehavior==20){
                    if(!readiness.ready(client,"beaver-swimming"))return;
                    saving=true;net.minecraft.client.Screenshot.grab(client.gameDirectory,"beaver-swimming.png",client.gameRenderer.mainRenderTarget(),1,msg->client.execute(()->{saving=false;behavior=2;ClientPlayNetworking.send(new WildlifeProbe(21,""));}));return;
                }
                if(behavior==2&&readyBehavior==21){
                    saving=true;net.minecraft.client.Screenshot.grab(client.gameDirectory,"beaver-land-return.png",client.gameRenderer.mainRenderTarget(),1,msg->client.execute(()->{saving=false;behavior=3;ClientPlayNetworking.send(new WildlifeProbe(22,""));}));return;
                }
                if(behavior<3)return;
                Files.writeString(Path.of(System.getProperty("blockbranch.beaverClientResult")),"PASS\n");done=true;Blockbranch.LOGGER.info("BEAVER_CLIENT_CAPTURE PASS");client.stop();return;
            }
            if(client.level==null||client.player==null)return;
            var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;if(frustum==null)return;
            int subjects=0,natural=0;
            for(var entity:client.level.entitiesForRendering())if(entity instanceof EurasianBeaver animal){
                if(!animal.isNoAi())natural++;
                if(animal.isNoAi()&&animal.distanceToSqr(client.player)<400&&frustum.isVisible(animal.getBoundingBox()))subjects++;
            }
            if(subjects!=3)return;
            if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Normal visibility required");
            client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
            if(!readiness.ready(client,"beaver-family")){settled=0;return;}if(++settled<60)return;
            for(var entity:client.level.entitiesForRendering())if(entity instanceof EurasianBeaver animal&&animal.isNoAi()&&animal.distanceToSqr(client.player)<400)blockbranch.client.wildlife.BeaverScaleAudit.measure(client,animal);
            Blockbranch.LOGGER.info("BEAVER_CAPTURE_READY subjects={} naturallyTrackedBeaver={} {}",subjects,natural,readiness.diagnostic());saving=true;
            net.minecraft.client.Screenshot.grab(client.gameDirectory,"beaver-family.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{
                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/beaver-family.png")))throw new AssertionError("Beaver PNG missing");
                captured=true;saving=false;if(Boolean.getBoolean("blockbranch.beaverAudioReview"))audio=new blockbranch.client.wildlife.BeaverAudioReview();
            }catch(Throwable error){done=true;Blockbranch.LOGGER.error("BEAVER_CLIENT FAIL",error);client.stop();}}));
        }catch(Throwable error){done=true;Blockbranch.LOGGER.error("BEAVER_CLIENT FAIL",error);client.stop();}}
    });}
    private BeaverClientSmoke(){}
}
