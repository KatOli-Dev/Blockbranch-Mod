package blockbranch.client.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import blockbranch.realistic.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Fixed normal-visibility photographs followed by the existing native pavement physics probes. */
public final class GravelDesertClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final FixtureSceneReadiness capture=new FixtureSceneReadiness();
        final long deadline=System.nanoTime()+1_200_000_000_000L;int stage,settled;boolean saving,done;
        public void onEndTick(net.minecraft.client.Minecraft client){
            if(done||saving)return;
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Gravel desert timeout stage="+stage+" "+capture.diagnostic());
                if(client.level==null||client.player==null){if(stage>0)throw new AssertionError("Disconnected during gravel desert capture");return;}
                var level=client.level;
                if(stage==0){if(Math.abs(client.player.getX()-1401431)>2)return;client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);stage=1;ClientPlayNetworking.send(new RealisticProbe(123));return;}
                if(stage==1||stage==2){
                    if(Math.abs(client.player.getX()-1401431)<2)return;
                    if(Math.abs(client.player.getXRot()-(stage==1?10:35))>.02||Math.abs(net.minecraft.util.Mth.wrapDegrees(client.player.getYRot()-45))>.02)return;
                    if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Gravel desert uses normal visibility");
                    if(!level.getBlockState(BlockPos.containing(client.player.getEyePosition())).isAir())throw new AssertionError("Gravel desert camera lens obstructed");
                    client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                    String file=stage==1?"gravel-desert-ground.png":"gravel-desert-overlook.png";
                    if(!capture.readySurface(client,file))return;
                    Blockbranch.LOGGER.info("GRAVEL_DESERT_CAPTURE_COMPLETE file={} {}",file,capture.diagnostic());saving=true;
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                        try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Missing gravel desert screenshot");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage==2?124:55));}
                        catch(Throwable failure){done=true;Blockbranch.LOGGER.error("GRAVEL_DESERT_CLIENT_FAIL",failure);client.stop();}
                    }));return;
                }
                var bed=client.player.blockPosition().below(6);if(!level.getBiome(bed).is(Blockbranch.id("continental_gravel_desert")))return;
                var terrain=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain();if(terrain==null)return;
                var base=terrain.base();var start=bed.above(6);var other=DesertSmoke.gravelStart(bed);
                int landingY=base.top(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR.isOpaque(),other.getX(),other.getZ(),blockbranch.cubic.ColumnLimits.MAX_Y-1);
                var landing=new BlockPos(other.getX(),landingY,other.getZ());
                if(stage==3){if(!level.getBlockState(bed).is(DesertContent.PAVEMENT))return;if(++settled<20)return;stage=4;settled=0;ClientPlayNetworking.send(new RealisticProbe(56));return;}
                if(stage==4){if(!level.getBlockState(start).is(DesertContent.PAVEMENT)||!level.getBlockState(other).isAir()||!level.getBlockState(landing).is(Blocks.GRAVEL))return;if(++settled<30)return;stage=5;settled=0;ClientPlayNetworking.send(new RealisticProbe(57));return;}
                if(stage==5){if(!level.getBlockState(start).isAir()||!level.getBlockState(bed.above()).is(Blocks.GRAVEL))return;if(++settled<20)return;stage=6;settled=0;ClientPlayNetworking.send(new RealisticProbe(58));return;}
                if(stage==6){if(!level.getBlockState(bed).is(Blocks.GRAVEL))return;if(++settled<20)return;stage=7;settled=0;ClientPlayNetworking.send(new RealisticProbe(59));return;}
                if(stage==7){for(var column:java.util.List.of(bed,other))for(int y=Math.min(bed.getY(),landingY)-1;y<=bed.getY()+7;y++)if(level.getBlockState(new BlockPos(column.getX(),y,column.getZ()))!=base.state(column.getX(),y,column.getZ()))return;if(++settled<20)return;Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("GRAVEL_DESERT_CLIENT_RENDER_SHOVEL_STABILITY_FALL_RESTORE_PASS bed={} independentCamera=true",bed);done=true;client.stop();}
            }catch(Throwable failure){done=true;Blockbranch.LOGGER.error("GRAVEL_DESERT_CLIENT_FAIL",failure);client.stop();}
        }
    });}
    private GravelDesertClientSmoke(){}
}
