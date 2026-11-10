package blockbranch.client.realistic;
import blockbranch.Blockbranch;
import blockbranch.wildlife.AmericanBison;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Existing full readiness gate plus visible adult/calf metric review. */
public final class BisonClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final FixtureSceneReadiness readiness=new FixtureSceneReadiness();
        final long deadline=System.nanoTime()+600_000_000_000L;int settled;boolean saving,done,captured;blockbranch.client.wildlife.BisonAudioReview audio;
        WildlifeCloseups closeups;boolean closeupsDone;
        public void onEndTick(net.minecraft.client.Minecraft client){if(done||saving)return;try{
            if(System.nanoTime()>deadline)throw new AssertionError("Bison review timeout "+readiness.diagnostic());
            if(captured){
                if(!closeupsDone){if(closeups==null)closeups=new WildlifeCloseups(client,AmericanBison.class,"bison");if(!closeups.tick(client))return;closeupsDone=true;}
                if(audio!=null&&!audio.tick(client))return;
                Files.writeString(Path.of(System.getProperty("blockbranch.bisonClientResult")),"PASS\n");done=true;Blockbranch.LOGGER.info("BISON_CLIENT_CAPTURE PASS");client.stop();return;
            }
            if(client.level==null||client.player==null)return;
            var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;if(frustum==null)return;
            int subjects=0,natural=0;
            for(var entity:client.level.entitiesForRendering())if(entity instanceof AmericanBison animal){
                if(!animal.isNoAi())natural++;
                if(animal.isNoAi()&&animal.distanceToSqr(client.player)<400&&frustum.isVisible(animal.getBoundingBox()))subjects++;
            }
            if(subjects!=3)return;
            if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Normal visibility required");
            client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
            if(!readiness.ready(client,"american-bison-family")){settled=0;return;}if(++settled<60)return;
            for(var entity:client.level.entitiesForRendering())if(entity instanceof AmericanBison animal&&animal.isNoAi()&&animal.distanceToSqr(client.player)<400)blockbranch.client.wildlife.BisonScaleAudit.measure(client,animal);
            Blockbranch.LOGGER.info("BISON_CAPTURE_READY subjects={} naturallyTrackedBison={} {}",subjects,natural,readiness.diagnostic());saving=true;
            net.minecraft.client.Screenshot.grab(client.gameDirectory,"american-bison-family.png",client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{
                if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/american-bison-family.png")))throw new AssertionError("Bison PNG missing");
                captured=true;saving=false;if(Boolean.getBoolean("blockbranch.bisonAudioReview"))audio=new blockbranch.client.wildlife.BisonAudioReview();
            }catch(Throwable error){done=true;Blockbranch.LOGGER.error("BISON_CLIENT FAIL",error);client.stop();}}));
        }catch(Throwable error){done=true;Blockbranch.LOGGER.error("BISON_CLIENT FAIL",error);client.stop();}}
    });}
    private BisonClientSmoke(){}
}
