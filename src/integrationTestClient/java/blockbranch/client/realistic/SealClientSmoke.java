package blockbranch.client.realistic;
import blockbranch.Blockbranch;
import blockbranch.wildlife.HarborSeal;
import blockbranch.wildlife.WildlifeProbe;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Full readiness, metric family and close-ups, native audio and water/land review. */
public final class SealClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final FixtureSceneReadiness readiness=new FixtureSceneReadiness();
        final long deadline=System.nanoTime()+600_000_000_000L;int settled;boolean saving,done,captured;
        int defense;int behavior;int readyBehavior;String behaviorError;blockbranch.client.wildlife.SealAudioReview audio;
        {ClientPlayNetworking.registerGlobalReceiver(WildlifeProbe.TYPE,(packet,context)->context.client().execute(()->{if(packet.stage()<0)behaviorError=packet.message();else readyBehavior=packet.stage();}));}
        WildlifeCloseups closeups;boolean closeupsDone;
        public void onEndTick(net.minecraft.client.Minecraft client){if(done||saving)return;try{
            if(System.nanoTime()>deadline)throw new AssertionError("Seal review timeout "+readiness.diagnostic());
            if(captured){
                if(!closeupsDone){if(closeups==null)closeups=new WildlifeCloseups(client,HarborSeal.class,"seal");if(!closeups.tick(client))return;closeupsDone=true;}
                if(audio!=null){if(!audio.tick(client))return;audio=null;}
                if(behaviorError!=null)throw new AssertionError(behaviorError);
                if(defense==0){defense=1;ClientPlayNetworking.send(new WildlifeProbe(10,""));return;}
                if(defense==1){if(readyBehavior!=10)return;defense=2;}
                if(behavior==0){behavior=1;ClientPlayNetworking.send(new WildlifeProbe(20,""));return;}
                if(behavior==1&&readyBehavior==20){
                    if(!readiness.ready(client,"seal-swimming"))return;
                    saving=true;net.minecraft.client.Screenshot.grab(client.gameDirectory,"seal-swimming.png",client.gameRenderer.mainRenderTarget(),1,msg->client.execute(()->{saving=false;behavior=2;ClientPlayNetworking.send(new WildlifeProbe(21,""));}));return;
                }
                if(behavior==2&&readyBehavior==21){
                    saving=true;net.minecraft.client.Screenshot.grab(client.gameDirectory,"seal-land-return.png",client.gameRenderer.mainRenderTarget(),1,msg->client.execute(()->{saving=false;behavior=3;ClientPlayNetworking.send(new WildlifeProbe(22,""));}));return;
                }
                if(behavior<3)return;
                Files.writeString(Path.of(System.getProperty("blockbranch.sealClientResult")),"PASS\n");done=true;Blockbranch.LOGGER.info("SEAL_CLIENT_CAPTURE PASS");client.stop();return;
            }
            if(client.level==null||client.player==null)return;
            var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;if(frustum==null)return;
            int subjects=0,natural=0;
            for(var entity:client.level.entitiesForRendering())if(entity instanceof HarborSeal animal){
                if(!animal.isNoAi())natural++;
                if(animal.isNoAi()&&animal.distanceToSqr(client.player)<400&&frustum.isVisible(animal.getBoundingBox()))subjects++;
            }
            if(subjects!=3)return;
            if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Normal visibility required");
            client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
            if(!readiness.ready(client,"seal-family")){settled=0;return;}if(++settled<60)return;
            for(var entity:client.level.entitiesForRendering())if(entity instanceof HarborSeal animal&&animal.isNoAi()&&animal.distanceToSqr(client.player)<400)blockbranch.client.wildlife.SealScaleAudit.measure(client,animal);
            Blockbranch.LOGGER.info("SEAL_CAPTURE_READY subjects={} naturallyTrackedSeal={} {}",subjects,natural,readiness.diagnostic());saving=true;
            net.minecraft.client.Screenshot.grab(client.gameDirectory,"seal-family.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{
                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/seal-family.png")))throw new AssertionError("Seal PNG missing");
                captured=true;saving=false;if(Boolean.getBoolean("blockbranch.sealAudioReview"))audio=new blockbranch.client.wildlife.SealAudioReview();
            }catch(Throwable error){done=true;Blockbranch.LOGGER.error("SEAL_CLIENT FAIL",error);client.stop();}}));
        }catch(Throwable error){done=true;Blockbranch.LOGGER.error("SEAL_CLIENT FAIL",error);client.stop();}}
    });}
    private SealClientSmoke(){}
}
