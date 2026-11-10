package blockbranch.client.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import blockbranch.realistic.RealisticProbe;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;

/** Focused native Savanna render/leaf-update fixture, without UI automation or user worlds. */
public final class SavannaClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        int stage,settled;boolean finished,saving;BlockPos bed;long deadline=System.nanoTime()+600_000_000_000L,nextProgress;
        final FixtureSceneReadiness capture=new FixtureSceneReadiness();
        public void onEndTick(net.minecraft.client.Minecraft client){
            if(finished||saving)return;
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Savanna client timed out at stage "+stage);
                if(client.player==null||client.level==null){if(stage>0)throw new AssertionError("Disconnected during Savanna fixture stage "+stage);return;}
                if(stage==0){stage=1;ClientPlayNetworking.send(new RealisticProbe(100));return;}
                var level=client.level;
                if(stage==1){
                    int x=client.player.blockPosition().getX()+18,z=client.player.blockPosition().getZ()-28;var t=((SparseColumn)level.getChunkAt(client.player.blockPosition())).blockbranch$terrain();if(t==null)return;
                    // Recover the known root from the fixture camera offset; no world data is fabricated.
                    int h=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
                    if(!level.getBiome(new BlockPos(x,h,z)).is(Biomes.SAVANNA))return;
                    // Server supplies root ground Y through a fixed 5 m camera offset.
                    int ground=(int)Math.round(client.player.getY())-5;bed=new BlockPos(x,ground,z);
                    if(!level.getBlockState(bed.above()).is(Blocks.ACACIA_LOG))return;
                }
                if(stage==1||stage==2){
                    if(Math.abs(client.player.getY()-(bed.getY()+(stage==1?5:23)))>.05||Math.abs(client.player.getXRot()-(stage==1?7:27))>.01){settled=0;return;}
                    if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.gui.toastManager().clear();
                    var area=client.levelRenderer.viewArea();if(area==null)return;int loaded=0,rendered=0,logs=0,leaves=0,woods=0;
                    for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){
                        int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var t=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(t==null)continue;loaded++;
                        int x=cx*16+8,z=cz*16+8,h=t.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;
                        var mesh=area.getRenderSectionAt(new BlockPos(x,h,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;
                        for(int packed:t.base().canopy().encode()){int key=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS,y=(key>>>8)+blockbranch.realistic.TerrainShape.FLOOR;var s=t.base().state(cx*16+(key&15),y,cz*16+((key>>>4)&15));if(s.is(Blocks.ACACIA_LOG)||s.is(Blocks.ACACIA_WOOD))logs++;
                            if((packed&blockbranch.cubic.CanopyLayer.STATE_MASK)==blockbranch.cubic.CanopyLayer.ACACIA_WOOD_CODE){
                                if(!s.is(Blocks.ACACIA_WOOD))throw new AssertionError("Received acacia wood palette material");
                                if(woods++==0&&level.getBlockState(new BlockPos(cx*16+(key&15),y,cz*16+((key>>>4)&15)))!=s)throw new AssertionError("Received acacia wood native materialization");
                            }
                            if(s.is(Blocks.ACACIA_LEAVES))leaves++;}
                    }
                    if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("SAVANNA_VIEW_READY stage={} loaded={} rendered={} logs={} leaves={}",stage,loaded,rendered,logs,leaves);}
                    if(loaded<121||rendered<40||logs<20||leaves<150||!capture.readySurface(client,"savanna-"+stage)){settled=0;return;}
                    if(++settled<100)return;Blockbranch.LOGGER.info("SAVANNA_CAPTURE_COVERAGE {} acaciaWood={}",capture.diagnostic(),woods);settled=0;saving=true;String file=stage==1?"savanna-ground.png":"savanna-canopy.png";
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                        try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Savanna screenshot missing");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage==2?101:102));}catch(Throwable e){finished=true;Blockbranch.LOGGER.error("SAVANNA_CLIENT_FAIL",e);client.stop();}
                    }));return;
                }
                var test=bed.above(30).east();
                if(stage==3){var state=level.getBlockState(test);if(!state.isAir()&&(!state.is(Blocks.ACACIA_LEAVES)||state.getValue(LeavesBlock.DISTANCE)!=7))return;if(++settled<20)return;settled=0;stage=4;ClientPlayNetworking.send(new RealisticProbe(103));return;}
                if(stage==4){var base=((SparseColumn)level.getChunkAt(test)).blockbranch$terrain().base();if(level.getBlockState(test)!=base.state(test.getX(),test.getY(),test.getZ()))return;Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("SAVANNA_CLIENT_RENDER_LEAF_UPDATE_RESTORE_PASS bed={}",bed);finished=true;client.stop();}
            }catch(Throwable e){finished=true;Blockbranch.LOGGER.error("SAVANNA_CLIENT_FAIL",e);client.stop();}
        }
    });}
    private SavannaClientSmoke(){}
}
