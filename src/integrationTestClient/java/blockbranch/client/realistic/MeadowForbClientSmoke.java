package blockbranch.client.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import blockbranch.realistic.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Fixed normal-visibility photographs followed by native fescue support probes. */
public final class MeadowForbClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final FixtureSceneReadiness capture=new FixtureSceneReadiness();
        final long deadline=System.nanoTime()+1_200_000_000_000L;int stage,settled;boolean saving,done;
        public void onEndTick(net.minecraft.client.Minecraft client){
            if(done||saving)return;
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Meadow forb timeout stage="+stage+" "+capture.diagnostic());
                if(client.level==null||client.player==null){if(stage>0)throw new AssertionError("Disconnected during meadow forb capture");return;}
                var level=client.level;
                if(stage==0){if(Math.abs(client.player.getX()-1401431)>2)return;client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);stage=1;ClientPlayNetworking.send(new RealisticProbe(125));return;}
                if(stage==1||stage==2){
                    if(Math.abs(client.player.getX()-1401431)<2)return;
                    if(Math.abs(client.player.getXRot()-(stage==1?10:35))>.02||Math.abs(net.minecraft.util.Mth.wrapDegrees(client.player.getYRot()-45))>.02)return;
                    if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Meadow forb uses normal visibility");
                    if(!level.getBlockState(BlockPos.containing(client.player.getEyePosition())).isAir())throw new AssertionError("Meadow forb camera lens obstructed");
                    client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                    String file=stage==1?"meadow-forb-ground.png":"meadow-forb-overlook.png";
                    if(!capture.readySurface(client,file))return;
                    Blockbranch.LOGGER.info("MEADOW_FORB_CAPTURE_COMPLETE file={} {}",file,capture.diagnostic());saving=true;
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                        try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Missing meadow forb screenshot");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage==2?126:17));}
                        catch(Throwable failure){done=true;Blockbranch.LOGGER.error("MEADOW_FORB_CLIENT_FAIL",failure);client.stop();}
                    }));return;
                }
                var candidate=client.player.blockPosition().below(6);if(!level.getBiome(candidate).is(Blockbranch.id("rolling_lowland_forb_grass_meadow")))return;
                var state=level.getBlockState(candidate.above());var fescue=MeadowContent.MEADOW_FESCUE;
                boolean ready=switch(stage){case 3->state.is(fescue)&&!state.getValue(MeadowFescueBlock.CROPPED);case 4->state.is(fescue)&&state.getValue(MeadowFescueBlock.CROPPED);case 5->state.is(fescue)&&!state.getValue(MeadowFescueBlock.CROPPED);case 6->level.getBlockState(candidate).is(Blocks.STONE)&&state.isAir();default->state.is(fescue)&&!state.getValue(MeadowFescueBlock.CROPPED);};
                if(!ready)return;if(++settled<20)return;settled=0;
                if(stage<7){stage++;ClientPlayNetworking.send(new RealisticProbe(stage+14));return;}
                var base=((SparseColumn)level.getChunkAt(candidate)).blockbranch$terrain().base();if(level.getBlockState(candidate)!=base.state(candidate.getX(),candidate.getY(),candidate.getZ()))throw new AssertionError("Meadow soil restore missing");
                Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("MEADOW_FORB_CLIENT_DIVIDE_REGROW_SUPPORT_RESTORE_PASS bed={}",candidate);done=true;client.stop();
            }catch(Throwable failure){done=true;Blockbranch.LOGGER.error("MEADOW_FORB_CLIENT_FAIL",failure);client.stop();}
        }
    });}
    private MeadowForbClientSmoke(){}
}
