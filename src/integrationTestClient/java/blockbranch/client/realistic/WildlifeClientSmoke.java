package blockbranch.client.realistic;

import blockbranch.Blockbranch;
import blockbranch.wildlife.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Reuses the existing full-column/mesh/frame/pose capture readiness gate. */
public final class WildlifeClientSmoke {
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            final FixtureSceneReadiness readiness=new FixtureSceneReadiness();
            final long deadline=System.nanoTime()+900_000_000_000L;
            final boolean scale=Boolean.getBoolean("blockbranch.wildlifeScaleReview");
            final String filename=scale?"wildlife-scale-reference.png":Boolean.getBoolean("blockbranch.deerReview")?"red-deer-family.png":"wild-boar-family.png";
            final boolean deer=Boolean.getBoolean("blockbranch.deerReview"),survey=Boolean.getBoolean("blockbranch.wildlifeSurvey");
            long nextSubjectDiagnostic;
            int settled;boolean saving,done,captured,surveySent,surveyDone;String surveyFailure;
            blockbranch.client.wildlife.WildlifeAudioReview audio;
            {if(survey)ClientPlayNetworking.registerGlobalReceiver(WildlifeProbe.TYPE,(packet,context)->context.client().execute(()->{
                if(packet.stage()==0)surveyDone=true;else if(packet.stage()<0)surveyFailure=packet.message();
            }));}
            public void onEndTick(net.minecraft.client.Minecraft client) {
                if(done||saving)return;
                try {
                    if(System.nanoTime()>deadline)throw new AssertionError("Wildlife capture timeout "+readiness.diagnostic());
                    if(client.level==null||client.player==null||Math.abs(client.player.getX())<100000)return;
                    if(captured){
                        if(surveyFailure!=null)throw new AssertionError(surveyFailure);
                        if(audio!=null){if(!audio.tick(client))return;audio=null;}
                        if(survey&&!surveySent){surveySent=true;ClientPlayNetworking.send(new WildlifeProbe(1,""));return;}
                        if(survey&&!surveyDone)return;
                        Files.writeString(Path.of(System.getProperty("blockbranch.wildlifeClientResult")),"PASS\n");
                        Blockbranch.LOGGER.info("WILDLIFE_CLIENT_CAPTURE_AND_SURVEY PASS deer={}",deer);done=true;client.stop();return;
                    }
                    int subjects=0;
                    var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
                    if(frustum==null)return;
                    for(var entity:client.level.entitiesForRendering())if((scale?(entity instanceof RedDeer||entity instanceof WildBoar):(deer?entity instanceof RedDeer:entity instanceof WildBoar)) && entity instanceof net.minecraft.world.entity.Mob mob && mob.isNoAi()
                        && mob.distanceToSqr(client.player)<400 && frustum.isVisible(mob.getBoundingBox()))subjects++;
                    if(subjects!=(scale?5:3)){
                        if(System.nanoTime()>nextSubjectDiagnostic){nextSubjectDiagnostic=System.nanoTime()+10_000_000_000L;
                            Blockbranch.LOGGER.info("WILDLIFE_SUBJECT_GATE deer={} visible={} camera={}",deer,subjects,client.player.position());
                            int shown=0;for(var entity:client.level.entitiesForRendering())if((scale?(entity instanceof RedDeer||entity instanceof WildBoar):(deer?entity instanceof RedDeer:entity instanceof WildBoar))&&shown++<8)
                                Blockbranch.LOGGER.info("WILDLIFE_SUBJECT_GATE_ENTITY pos={} noAi={} bounds={}",entity.position(),((net.minecraft.world.entity.Mob)entity).isNoAi(),entity.getBoundingBox());
                        }return;
                    }
                    if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Wildlife review requires normal visibility");
                    client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                    client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                    if(!readiness.ready(client,scale?"wildlife-scale-reference":deer?"red-deer-family":"wild-boar-family")){settled=0;return;}
                    if(++settled<60)return;
                    saving=true;
                    if(scale)for(var entity:client.level.entitiesForRendering())if((entity instanceof RedDeer||entity instanceof WildBoar)&&entity instanceof net.minecraft.world.entity.Mob mob&&mob.isNoAi()&&mob.distanceToSqr(client.player)<400)blockbranch.client.wildlife.WildlifeScaleAudit.measure(client,mob);
                    for(var entity:client.level.entitiesForRendering())if(entity instanceof WildBoar boar && boar.isNoAi())
                        Blockbranch.LOGGER.info("WILDLIFE_SUBJECT client={} baby={} bounds={} renderer={}",boar.position(),boar.isBaby(),boar.getBoundingBox(),client.getEntityRenderDispatcher().getRenderer(boar).getClass().getName());
                    Blockbranch.LOGGER.info("WILDLIFE_CAPTURE_READY subjects={} {}",subjects,readiness.diagnostic());
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,filename,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                        try {
                            if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+(filename))))throw new AssertionError("Screenshot missing");
                            Blockbranch.LOGGER.info("WILDLIFE_CLIENT_PIXELS_SAVED deer={}",deer);captured=true;saving=false;
                            if(Boolean.getBoolean("blockbranch.wildlifeAudioReview")){audio=new blockbranch.client.wildlife.WildlifeAudioReview();if(survey)ClientPlayNetworking.send(new WildlifeProbe(2,""));}
                        }catch(Throwable failure){done=true;Blockbranch.LOGGER.error("WILDLIFE_CLIENT_FAIL",failure);client.stop();}
                    }));
                }catch(Throwable failure){done=true;Blockbranch.LOGGER.error("WILDLIFE_CLIENT_FAIL",failure);client.stop();}
            }
        });
    }
    private WildlifeClientSmoke() {}
}
