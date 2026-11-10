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
public final class TropicalForestClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final boolean bamboo=Boolean.getBoolean("blockbranch.bambooJungleClient");final int kind=bamboo?2:Boolean.getBoolean("blockbranch.existingJungleClient")?3:1;final FixtureSceneReadiness capture=new FixtureSceneReadiness();
        final long deadline=System.nanoTime()+1_200_000_000_000L;int stage,settled;boolean saving,done;BlockPos bed;
        public void onEndTick(net.minecraft.client.Minecraft client){
            if(done||saving)return;
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Tropical client timeout stage="+stage+" "+capture.diagnostic());
                if(client.level==null||client.player==null){if(stage>0)throw new AssertionError("Disconnected during tropical fixture at stage "+stage);return;}var level=client.level;
                if(stage==0){if(Math.abs(client.player.getX()-1401431)>2)return;stage=1;ClientPlayNetworking.send(new RealisticProbe(90));return;}
                if(stage==1){
                    var q=client.player.blockPosition();var candidate=new BlockPos(q.getX(),(int)Math.round(client.player.getY())-40,q.getZ());
                    var terrain=((SparseColumn)level.getChunkAt(candidate)).blockbranch$terrain();if(terrain==null)return;
                    if(!level.getBiome(candidate).is(bamboo?Biomes.BAMBOO_JUNGLE:kind==3?Biomes.JUNGLE:Biomes.SPARSE_JUNGLE)||!level.getBlockState(candidate.above()).is(bamboo?Blocks.BAMBOO:Blocks.JUNGLE_LOG))return;
                    bed=candidate;stage=2;ClientPlayNetworking.send(new RealisticProbe(91));return;
                }
                if(stage==2||stage==3){
                    var expected=blockbranch.realistic.TropicalForestVariantsFixtures.view(bed.getX(),bed.getY(),bed.getZ(),stage==3,kind,(x,z)->ground(client,x,z));
                    if(client.player.position().distanceToSqr(expected.x(),expected.y(),expected.z())>.0025||Math.abs(net.minecraft.util.Mth.wrapDegrees(client.player.getYRot()-expected.yaw()))>.02||Math.abs(client.player.getXRot()-expected.pitch())>.02)return;
                    if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Tropical views use normal visibility");
                    if(!level.getBlockState(BlockPos.containing(client.player.getEyePosition())).isAir())throw new AssertionError("Tropical camera lens obstructed");
                    client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                    if(!capture.ready(client,(bamboo?"bamboo-jungle":kind==3?"jungle-large-fern":"sparse-jungle")+"-"+stage)){settled=0;return;}
                    if(++settled<100)return;settled=0;saving=true;
                    String file=(bamboo?"bamboo-jungle":kind==3?"jungle-large-fern":"sparse-jungle")+(stage==2?"-ground":"-elevated")+".png";
                    Blockbranch.LOGGER.info("TROPICAL_FOREST_CAPTURE_COVERAGE file={} {}",file,capture.diagnostic());
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                        try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Missing tropical screenshot");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage==3?92:93));}
                        catch(Throwable failure){done=true;Blockbranch.LOGGER.error("TROPICAL_FOREST_CLIENT_FAIL",failure);client.stop();}
                    }));return;
                }
                var profile=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();int height=bamboo?TropicalForestSmoke.culmHeight(profile,bed):40;var test=bamboo?bed.above(height):bed.above(40).east();
                if(stage==4){var state=level.getBlockState(test);if(bamboo?!state.isAir():!state.is(Blocks.JUNGLE_LEAVES)||state.getValue(LeavesBlock.DISTANCE)!=7||state.getValue(LeavesBlock.PERSISTENT))return;if(++settled<20)return;stage=5;ClientPlayNetworking.send(new RealisticProbe(94));return;}
                if(stage==5){var base=((SparseColumn)level.getChunkAt(test)).blockbranch$terrain().base();if(bamboo){for(int dy=1;dy<=height;dy++){var p=bed.above(dy);if(level.getBlockState(p)!=base.state(p.getX(),p.getY(),p.getZ()))return;}}else if(level.getBlockState(test)!=base.state(test.getX(),test.getY(),test.getZ()))return;Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("TROPICAL_FOREST_CLIENT_RENDER_NATIVE_UPDATE_RESTORE_PASS kind={} bed={}",kind,bed);done=true;client.stop();}
            }catch(Throwable failure){done=true;Blockbranch.LOGGER.error("TROPICAL_FOREST_CLIENT_FAIL",failure);client.stop();}
        }
    });}
    private static int ground(net.minecraft.client.Minecraft client,int x,int z){
        var chunk=client.level.getChunkSource().getChunk(x>>4,z>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);if(chunk==null)return Integer.MIN_VALUE;
        var terrain=((SparseColumn)chunk).blockbranch$terrain();if(terrain==null)return Integer.MIN_VALUE;
        return terrain.base().groundHeight(x,z);
    }
    private TropicalForestClientSmoke(){}
}
