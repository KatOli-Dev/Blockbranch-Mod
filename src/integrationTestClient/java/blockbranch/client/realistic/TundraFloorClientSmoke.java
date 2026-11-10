package blockbranch.client.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import blockbranch.realistic.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Fixed normal-visibility photographs followed by native fescue and berry support probes. */
public final class TundraFloorClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final FixtureSceneReadiness capture=new FixtureSceneReadiness();
        final long deadline=System.nanoTime()+1_200_000_000_000L;int stage,settled;boolean saving,done;BlockPos bed,berry;
        public void onEndTick(net.minecraft.client.Minecraft client){
            if(done||saving)return;
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Tundra floor timeout stage="+stage+" "+capture.diagnostic());
                if(client.level==null||client.player==null){if(stage>0)throw new AssertionError("Disconnected during tundra floor capture");return;}
                var level=client.level;
                if(stage==0){if(Math.abs(client.player.getX()-1401431)>2)return;client.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);stage=1;ClientPlayNetworking.send(new RealisticProbe(125));return;}
                if(stage==1||stage==2){
                    if(Math.abs(client.player.getX()-1401431)<2)return;
                    if(Math.abs(client.player.getXRot()-(stage==1?10:35))>.02||Math.abs(net.minecraft.util.Mth.wrapDegrees(client.player.getYRot()-45))>.02)return;
                    if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Tundra floor uses normal visibility");
                    if(!level.getBlockState(BlockPos.containing(client.player.getEyePosition())).isAir())throw new AssertionError("Tundra floor camera lens obstructed");
                    client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                    String file=stage==1?"tundra-floor-ground.png":"tundra-floor-overlook.png";
                    if(!capture.readySurface(client,file))return;
                    Blockbranch.LOGGER.info("TUNDRA_FLOOR_CAPTURE_COMPLETE file={} {}",file,capture.diagnostic());saving=true;
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                        try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Missing tundra floor screenshot");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage==2?126:69));}
                        catch(Throwable failure){done=true;Blockbranch.LOGGER.error("TUNDRA_FLOOR_CLIENT_FAIL",failure);client.stop();}
                    }));return;
                }
                if(stage==3){
                    var candidate=client.player.blockPosition().below(8);if(!level.getBiome(candidate).is(Blockbranch.id("cold_dwarf_shrub_tundra"))||!level.getBlockState(candidate.above()).is(MeadowContent.MEADOW_FESCUE))return;
                    bed=candidate;
                    outer:for(int z=bed.getZ()-64;z<=bed.getZ()+64;z++)for(int x=bed.getX()-64;x<=bed.getX()+64;x++){
                        var p=new BlockPos(x,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z)-1,z);
                        if(level.getBlockState(p).is(Blocks.SWEET_BERRY_BUSH)&&level.getBiome(p).is(Blockbranch.id("cold_dwarf_shrub_tundra"))){berry=p;break outer;}
                    }
                    if(berry==null)return;stage=4;ClientPlayNetworking.send(new RealisticProbe(70));return;
                }
                var state=level.getBlockState(bed.above());var bush=level.getBlockState(berry);var fescue=MeadowContent.MEADOW_FESCUE;
                boolean ready=switch(stage){
                    case 4->state.is(fescue)&&state.getValue(MeadowFescueBlock.CROPPED);
                    case 5->state.is(fescue)&&!state.getValue(MeadowFescueBlock.CROPPED);
                    case 6->level.getBlockState(bed).is(Blocks.STONE)&&state.isAir();
                    case 7->state.is(fescue)&&!state.getValue(MeadowFescueBlock.CROPPED)&&bush.is(Blocks.SWEET_BERRY_BUSH)&&bush.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)==1;
                    case 8->bush.is(Blocks.SWEET_BERRY_BUSH)&&bush.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)==3;
                    case 9->bush.is(Blocks.SWEET_BERRY_BUSH)&&bush.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)==1;
                    case 10->bush.isAir()&&level.getBlockState(berry.below()).is(Blocks.STONE);
                    default->state.is(fescue)&&!state.getValue(MeadowFescueBlock.CROPPED)&&bush.is(Blocks.SWEET_BERRY_BUSH)&&bush.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE)==1;
                };
                if(!ready)return;if(++settled<15)return;settled=0;
                if(stage<11){stage++;ClientPlayNetworking.send(new RealisticProbe(stage+66));return;}
                var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();if(level.getBlockState(bed)!=base.state(bed.getX(),bed.getY(),bed.getZ()))throw new AssertionError("Tundra soil restore missing");
                Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("TUNDRA_FLOOR_CLIENT_DIVIDE_REGROW_BERRY_SUPPORT_RESTORE_PASS bed={} berry={}",bed,berry);done=true;client.stop();
            }catch(Throwable failure){done=true;Blockbranch.LOGGER.error("TUNDRA_FLOOR_CLIENT_FAIL",failure);client.stop();}
        }
    });}
    private TundraFloorClientSmoke(){}
}
