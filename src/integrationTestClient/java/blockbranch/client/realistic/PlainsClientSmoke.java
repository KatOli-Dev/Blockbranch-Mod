package blockbranch.client.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.RealisticProbe;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;

/** Native open-grassland views plus cross-section paired-plant removal/restoration through live packets. */
public final class PlainsClientSmoke {
    public static void register(boolean sunflower){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        int stage,settled;boolean finished,saving,cameraRequested;BlockPos bed;long deadline=System.nanoTime()+600_000_000_000L,nextProgress;
        final FixtureSceneReadiness capture=new FixtureSceneReadiness();
        public void onEndTick(net.minecraft.client.Minecraft client){
            if(finished||saving)return;
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Plains client timed out at stage "+stage);
                if(client.player==null||client.level==null){if(stage>0)throw new AssertionError("Disconnected during Plains fixture stage "+stage);return;}var level=client.level;
                if(stage==0){stage=1;ClientPlayNetworking.send(new RealisticProbe(117));return;}
                if(stage==1&&bed==null){int x=client.player.blockPosition().getX()+(sunflower?-28:18),z=client.player.blockPosition().getZ()-(sunflower?18:28),h=(int)Math.round(client.player.getY())-5;var p=new BlockPos(x,h,z);if(!level.getBiome(p).is(sunflower?Biomes.SUNFLOWER_PLAINS:Biomes.PLAINS)||!level.getBlockState(p).is(Blocks.GRASS_BLOCK)||!level.getBlockState(p.above(2)).is(sunflower?Blocks.SUNFLOWER:Blocks.TALL_GRASS))return;bed=p;}
                if(stage==1&&bed!=null&&sunflower&&blockbranch.realistic.SunflowerCameraFixture.fixed()&&!cameraRequested){cameraRequested=true;ClientPlayNetworking.send(new RealisticProbe(122));return;}
                if(stage==1||stage==2){
                    var camera=sunflower?blockbranch.realistic.SunflowerCameraFixture.anchor(bed):bed;
                    if(Math.abs(client.player.getX()-(camera.getX()+(sunflower?28.5:-17.5)))>.05||Math.abs(client.player.getZ()-(camera.getZ()+(sunflower?18.5:28.5)))>.05||Math.abs(client.player.getY()-(camera.getY()+(stage==1?5:29)))>.05||Math.abs(client.player.getXRot()-(stage==1?7:27))>.01){settled=0;return;}
                    if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.gui.toastManager().clear();var area=client.levelRenderer.viewArea();if(area==null)return;int loaded=0,rendered=0;
                    for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){
                        int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var t=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(t==null)continue;loaded++;
                        int x=cx*16+8,z=cz*16+8,h=t.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque(),x,z,ColumnLimits.MAX_Y-1)-1;
                        var mesh=area.getRenderSectionAt(new BlockPos(x,h,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;
                    }
                    if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("PLAINS_VIEW_READY stage={} loaded={} rendered={}",stage,loaded,rendered);}
                    if(loaded<121||rendered<40||!capture.readySurface(client,(sunflower?"sunflower-plains":"plains")+"-"+stage)){settled=0;return;}if(++settled<100)return;
                    Blockbranch.LOGGER.info("PLAINS_CAPTURE_COVERAGE {}",capture.diagnostic());
                    settled=0;saving=true;String file=(sunflower?"sunflower-plains":"plains")+(stage==1?"-ground.png":"-overlook.png");
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Plains screenshot missing");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage==2?118:119));}catch(Throwable e){finished=true;Blockbranch.LOGGER.error("PLAINS_CLIENT_FAIL",e);client.stop();}}));return;
                }
                if(stage==3){if(!level.getBlockState(bed.above()).isAir()||!level.getBlockState(bed.above(2)).isAir())return;stage=4;ClientPlayNetworking.send(new RealisticProbe(120));return;}
                if(stage==4){if(!level.getBlockState(bed).is(Blocks.STONE)||!level.getBlockState(bed.above()).isAir()||!level.getBlockState(bed.above(2)).isAir())return;stage=5;ClientPlayNetworking.send(new RealisticProbe(121));return;}
                if(stage==5){var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();for(int dy=0;dy<=2;dy++){var p=bed.above(dy);if(level.getBlockState(p)!=base.state(p.getX(),p.getY(),p.getZ()))return;}Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("PLAINS_CLIENT_RENDER_PAIRED_SUPPORT_RESTORE_PASS sunflower={} bed={}",sunflower,bed);finished=true;client.stop();}
            }catch(Throwable e){finished=true;Blockbranch.LOGGER.error("PLAINS_CLIENT_FAIL",e);client.stop();}
        }
    });}
    private PlainsClientSmoke(){}
}
