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

/** Normal coastal bluff views with complete section coverage and a native local block edit. */
public final class StonyShoreClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final FixtureSceneReadiness capture=new FixtureSceneReadiness();
        final long deadline=System.nanoTime()+1_200_000_000_000L;int stage,settled;boolean saving,done;BlockPos bed;
        public void onEndTick(net.minecraft.client.Minecraft client){
            if(done||saving)return;
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Stony Shore client timeout stage="+stage+" "+capture.diagnostic());
                if(client.level==null||client.player==null){if(stage>0)throw new AssertionError("Disconnected during cold upland fixture at stage "+stage);return;}var level=client.level;
                if(stage==0){if(Math.abs(client.player.getX()-1401431)>2)return;client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);Blockbranch.LOGGER.info("STONY_SHORE_CAPTURE_SETTINGS clouds=off normalVisibility=true");stage=1;ClientPlayNetworking.send(new RealisticProbe(90));return;}
                if(stage==1){
                    var q=client.player.blockPosition();var candidate=new BlockPos(q.getX(),(int)Math.round(client.player.getY())-40,q.getZ());
                    var terrain=((SparseColumn)level.getChunkAt(candidate)).blockbranch$terrain();if(terrain==null)return;
                    if(!level.getBiome(candidate).is(Biomes.STONY_SHORE))return;
                    bed=candidate;stage=2;ClientPlayNetworking.send(new RealisticProbe(91));return;
                }
                if(stage==2||stage==3){
                    for(int dx:new int[]{-32,0,32})for(int dz:new int[]{-32,0,32})if(ground(client,bed.getX()+dx,bed.getZ()+dz)==Integer.MIN_VALUE)return;
                    var expected=blockbranch.realistic.StonyShoreFixtures.view(bed.getX(),bed.getY(),bed.getZ(),stage==3,(x,z)->ground(client,x,z));
                    if(expected==null)return;
                    if(client.player.position().distanceToSqr(expected.x(),expected.y(),expected.z())>.0025||Math.abs(net.minecraft.util.Mth.wrapDegrees(client.player.getYRot()-expected.yaw()))>.02||Math.abs(client.player.getXRot()-expected.pitch())>.02)return;
                    if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Stony Shore views use normal visibility");
                    if(!level.getBlockState(BlockPos.containing(client.player.getEyePosition())).isAir())throw new AssertionError("Stony Shore camera lens obstructed");
                    client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                    if(!capture.ready(client,"stony-shore"+"-"+stage)){settled=0;return;}
                    if(++settled<100)return;settled=0;saving=true;
                    String file="stony-shore"+(stage==2?"-ground":"-elevated")+".png";
                    Blockbranch.LOGGER.info("STONY_SHORE_CAPTURE_COVERAGE file={} {}",file,capture.diagnostic());
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                        try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Missing cold upland screenshot");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage==3?92:93));}
                        catch(Throwable failure){done=true;Blockbranch.LOGGER.error("STONY_SHORE_CLIENT_FAIL",failure);client.stop();}
                    }));return;
                }
                var test=bed.below();
                if(stage==4){var state=level.getBlockState(test);if(!state.is(Blocks.COBBLESTONE))return;if(++settled<20)return;stage=5;ClientPlayNetworking.send(new RealisticProbe(94));return;}
                if(stage==5){var base=((SparseColumn)level.getChunkAt(test)).blockbranch$terrain().base();if(level.getBlockState(test)!=base.state(test.getX(),test.getY(),test.getZ()))return;Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("STONY_SHORE_CLIENT_RENDER_NATIVE_UPDATE_RESTORE_PASS bed={}",bed);done=true;client.stop();}
            }catch(Throwable failure){done=true;Blockbranch.LOGGER.error("STONY_SHORE_CLIENT_FAIL",failure);client.stop();}
        }
    });}
    private static int ground(net.minecraft.client.Minecraft client,int x,int z){
        var chunk=client.level.getChunkSource().getChunk(x>>4,z>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);if(chunk==null)return Integer.MIN_VALUE;
        var terrain=((SparseColumn)chunk).blockbranch$terrain();if(terrain==null)return Integer.MIN_VALUE;
        return terrain.base().groundHeight(x,z);
    }
    private StonyShoreClientSmoke(){}
}
