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

/** Offline native Desert render, active random growth, harvest and support-removal fixture. */
public final class SandyDesertClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final FixtureSceneReadiness capture=new FixtureSceneReadiness();
        int stage,settled;boolean finished,saving;BlockPos bed;long deadline=System.nanoTime()+600_000_000_000L,nextProgress;
        public void onEndTick(net.minecraft.client.Minecraft client){
            if(finished||saving)return;
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Desert client timed out at stage "+stage);
                if(client.player==null||client.level==null){if(stage>0)throw new AssertionError("Disconnected during Desert fixture stage "+stage);return;}var level=client.level;
                if(stage==0){stage=1;ClientPlayNetworking.send(new RealisticProbe(108));return;}
                if(stage==1&&bed==null){int x=client.player.blockPosition().getX()+18,z=client.player.blockPosition().getZ()-28,h=(int)Math.round(client.player.getY())-5;var p=new BlockPos(x,h,z);if(!level.getBiome(p).is(Biomes.DESERT)||!level.getBlockState(p).is(Blocks.SAND)||!level.getBlockState(p.above()).is(Blocks.CACTUS))return;bed=p;}
                if(stage==1||stage==2){
                    if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.gui.toastManager().clear();var area=client.levelRenderer.viewArea();if(area==null)return;int loaded=0,rendered=0,sand=0;
                    for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){
                        int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var t=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(t==null)continue;loaded++;
                        int x=cx*16+8,z=cz*16+8,h=t.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque(),x,z,ColumnLimits.MAX_Y-1)-1;
                        var mesh=area.getRenderSectionAt(new BlockPos(x,h,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;if(level.getBlockState(new BlockPos(x,h,z)).is(Blocks.SAND))sand++;
                    }
                    if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("SANDY_DESERT_VIEW_READY stage={} loaded={} rendered={} sand={}",stage,loaded,rendered,sand);}
                    if(loaded<121||rendered<40||sand<60||!capture.readySurface(client,"sandy-desert-"+stage)){settled=0;return;}if(++settled<100)return;Blockbranch.LOGGER.info("SANDY_DESERT_CAPTURE_COVERAGE {}",capture.diagnostic());settled=0;saving=true;String file=stage==1?"desert-ground.png":"desert-overlook.png";
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Desert screenshot missing");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage==2?109:110));}catch(Throwable e){finished=true;Blockbranch.LOGGER.error("SANDY_DESERT_CLIENT_FAIL",e);client.stop();}}));return;
                }
                if(stage==3){var s=level.getBlockState(bed.above(2));if(!s.is(Blocks.CACTUS)&&!s.is(Blocks.CACTUS_FLOWER))return;stage=4;ClientPlayNetworking.send(new RealisticProbe(111));return;}
                if(stage==4){if(!level.getBlockState(bed.above()).isAir()||!level.getBlockState(bed.above(2)).isAir())return;stage=5;ClientPlayNetworking.send(new RealisticProbe(112));return;}
                if(stage==5){if(!level.getBlockState(bed).isAir()||!level.getBlockState(bed.above()).isAir())return;stage=6;ClientPlayNetworking.send(new RealisticProbe(113));return;}
                if(stage==6){var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();for(int dy=-1;dy<=5;dy++){var p=bed.above(dy);if(level.getBlockState(p)!=base.state(p.getX(),p.getY(),p.getZ()))return;}stage=7;settled=0;ClientPlayNetworking.send(new RealisticProbe(114));return;}
                if(stage==7){int x=client.player.blockPosition().getX()+18,z=client.player.blockPosition().getZ()-28,h=(int)Math.round(client.player.getY())-5;if(h>=320)return;var p=new BlockPos(x,h,z);if(!level.getBiome(p).is(Biomes.DESERT)||!level.getBlockState(p.above()).is(Blocks.CACTUS))return;if(++settled<20)return;bed=p;stage=8;ClientPlayNetworking.send(new RealisticProbe(115));return;}
                if(stage==8){var state=level.getBlockState(bed.above(2));if(!state.is(Blocks.CACTUS)&&!state.is(Blocks.CACTUS_FLOWER))return;stage=9;ClientPlayNetworking.send(new RealisticProbe(116));return;}
                if(stage==9){var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();for(int dy=-1;dy<=5;dy++){var p=bed.above(dy);if(level.getBlockState(p)!=base.state(p.getX(),p.getY(),p.getZ()))return;}Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("SANDY_DESERT_CLIENT_RENDER_GROW_HARVEST_SUPPORT_BOTH_WINDOWS_PASS lowBed={}",bed);finished=true;client.stop();}
            }catch(Throwable e){finished=true;Blockbranch.LOGGER.error("SANDY_DESERT_CLIENT_FAIL",e);client.stop();}
        }
    });}
    private SandyDesertClientSmoke(){}
}
