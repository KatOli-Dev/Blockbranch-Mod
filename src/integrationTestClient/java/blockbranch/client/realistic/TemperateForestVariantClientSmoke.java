package blockbranch.client.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import blockbranch.realistic.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;

/** Normal ground/elevated views with complete section coverage and a native leaf update. */
public final class TemperateForestVariantClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final boolean oldBirch=Boolean.getBoolean("blockbranch.oldBirchClient");final FixtureSceneReadiness capture=new FixtureSceneReadiness();final boolean visibility=Boolean.getBoolean("blockbranch.flowerVisibilityDiagnostic");final FlowerVisibilityDiagnostic diagnostic=visibility?new FlowerVisibilityDiagnostic():null;
        final long deadline=System.nanoTime()+1_200_000_000_000L;int stage,settled;boolean saving,done;BlockPos bed;
        public void onEndTick(net.minecraft.client.Minecraft client){
            if(done||saving)return;
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Temperate variant client timeout stage="+stage+" "+capture.diagnostic());
                if(client.level==null||client.player==null){if(stage>0)throw new AssertionError("Disconnected during temperate variant fixture at stage "+stage);return;}var level=client.level;
                if(stage==0){if(Math.abs(client.player.getX()-1401431)>2)return;stage=1;ClientPlayNetworking.send(new RealisticProbe(90));return;}
                if(stage==1){
                    var q=client.player.blockPosition();var candidate=new BlockPos(q.getX(),(int)Math.round(client.player.getY())-40,q.getZ());
                    var terrain=((SparseColumn)level.getChunkAt(candidate)).blockbranch$terrain();if(terrain==null)return;
                    if(!level.getBiome(candidate).is(oldBirch?Biomes.OLD_GROWTH_BIRCH_FOREST:Biomes.FLOWER_FOREST)||!level.getBlockState(candidate.above()).is(oldBirch?Blocks.BIRCH_LOG:Blocks.OAK_LOG))return;
                    bed=candidate;stage=2;ClientPlayNetworking.send(new RealisticProbe(91));return;
                }
                if(stage==2||stage==3||stage==6){
                    var expected=stage==6?TemperateForestVariantSmoke.intermediateView(bed):blockbranch.realistic.TemperateForestVariantsFixtures.view(bed.getX(),bed.getY(),bed.getZ(),stage==3,oldBirch?2:1);
                    if(client.player.position().distanceToSqr(expected.x(),expected.y(),expected.z())>.0025||Math.abs(net.minecraft.util.Mth.wrapDegrees(client.player.getYRot()-expected.yaw()))>.02||Math.abs(client.player.getXRot()-expected.pitch())>.02)return;
                    if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Temperate variant views use normal visibility");
                    if(!level.getBlockState(BlockPos.containing(client.player.getEyePosition())).isAir())throw new AssertionError("Temperate variant camera lens obstructed");
                    client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                    if(!capture.readySurface(client,(oldBirch?"old-growth-birch":"flower-forest")+"-"+stage)){settled=0;return;}
                    if(++settled<100)return;settled=0;if(visibility)diagnostic.sample(client,bed,stage);saving=true;
                    String file=(oldBirch?"old-growth-birch-forest":"flower-forest")+(stage==2?"-ground":stage==6?"-intermediate":"-elevated")+".png";
                    Blockbranch.LOGGER.info("TEMPERATE_VARIANT_CAPTURE_COVERAGE file={} {}",file,capture.diagnostic());
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                        try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Missing temperate variant screenshot");saving=false;if(visibility&&stage==3){stage=6;ClientPlayNetworking.send(new RealisticProbe(95));}else if(stage==6){stage=4;ClientPlayNetworking.send(new RealisticProbe(93));}else{stage++;ClientPlayNetworking.send(new RealisticProbe(stage==3?92:93));}}
                        catch(Throwable failure){done=true;Blockbranch.LOGGER.error("TEMPERATE_VARIANT_CLIENT_FAIL",failure);client.stop();}
                    }));return;
                }
                var test=bed.above(40).east();
                if(stage==4){var state=level.getBlockState(test);if(!state.is(Blocks.BIRCH_LEAVES)||state.getValue(LeavesBlock.DISTANCE)!=7||state.getValue(LeavesBlock.PERSISTENT))return;if(++settled<20)return;stage=5;ClientPlayNetworking.send(new RealisticProbe(94));return;}
                if(stage==5){var base=((SparseColumn)level.getChunkAt(test)).blockbranch$terrain().base();if(level.getBlockState(test)!=base.state(test.getX(),test.getY(),test.getZ()))return;Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("TEMPERATE_VARIANT_CLIENT_RENDER_LEAF_UPDATE_RESTORE_PASS oldBirch={} bed={}",oldBirch,bed);done=true;client.stop();}
            }catch(Throwable failure){done=true;Blockbranch.LOGGER.error("TEMPERATE_VARIANT_CLIENT_FAIL",failure);client.stop();}
        }
    });}
    private TemperateForestVariantClientSmoke(){}
}
