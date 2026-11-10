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

/** Focused native Jungle render/leaf-update fixture, without UI automation or user worlds. */
public final class JungleClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        int stage,settled;boolean finished,saving;BlockPos bed;long deadline=System.nanoTime()+600_000_000_000L,nextProgress;
        public void onEndTick(net.minecraft.client.Minecraft client){
            if(finished||saving)return;
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Jungle client timed out at stage "+stage);
                if(client.player==null||client.level==null)return;
                if(stage==0){stage=1;ClientPlayNetworking.send(new RealisticProbe(104));return;}
                var level=client.level;
                if(stage==1){
                    int x=client.player.blockPosition().getX()+18,z=client.player.blockPosition().getZ()-28;var t=((SparseColumn)level.getChunkAt(client.player.blockPosition())).blockbranch$terrain();if(t==null)return;
                    // Recover the known root from the fixture camera offset; no world data is fabricated.
                    int h=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
                    if(!level.getBiome(new BlockPos(x,h,z)).is(Biomes.JUNGLE))return;
                    // Server supplies root ground Y through a fixed 5 m camera offset.
                    int ground=(int)Math.round(client.player.getY())-5;bed=new BlockPos(x,ground,z);
                    if(!Boolean.getBoolean("blockbranch.jungleComparison")&&!level.getBlockState(bed.above()).is(Blocks.JUNGLE_LOG))return;
                }
                if(stage==1||stage==2){
                    if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.gui.toastManager().clear();
                    var area=client.levelRenderer.viewArea();if(area==null)return;int loaded=0,rendered=0,logs=0,leaves=0;
                    for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){
                        int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var t=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(t==null)continue;loaded++;
                        int x=cx*16+8,z=cz*16+8,h=t.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;
                        var mesh=area.getRenderSectionAt(new BlockPos(x,h,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;
                        for(int packed:t.base().canopy().encode()){int key=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS,y=(key>>>8)+blockbranch.realistic.TerrainShape.FLOOR;var s=t.base().state(cx*16+(key&15),y,cz*16+((key>>>4)&15));if(s.is(Blocks.JUNGLE_LOG))logs++;if(s.is(Blocks.JUNGLE_LEAVES))leaves++;}
                    }
                    if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("JUNGLE_VIEW_READY stage={} loaded={} rendered={} logs={} leaves={}",stage,loaded,rendered,logs,leaves);}
                    if(loaded<121||rendered<40||logs<20||leaves<150){settled=0;return;}
                    if(++settled<100)return;settled=0;saving=true;String file=stage==1?"jungle-ground.png":"jungle-canopy.png";
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                        try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Jungle screenshot missing");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage==2?105:106));}catch(Throwable e){finished=true;Blockbranch.LOGGER.error("JUNGLE_CLIENT_FAIL",e);client.stop();}
                    }));return;
                }
                var test=bed.above(44).east();
                if(stage==3){var state=level.getBlockState(test);if(!state.isAir()&&(!state.is(Blocks.JUNGLE_LEAVES)||state.getValue(LeavesBlock.DISTANCE)!=7))return;if(++settled<20)return;settled=0;stage=4;ClientPlayNetworking.send(new RealisticProbe(107));return;}
                if(stage==4){var base=((SparseColumn)level.getChunkAt(test)).blockbranch$terrain().base();if(level.getBlockState(test)!=base.state(test.getX(),test.getY(),test.getZ()))return;Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("JUNGLE_CLIENT_RENDER_LEAF_UPDATE_RESTORE_PASS bed={}",bed);finished=true;client.stop();}
            }catch(Throwable e){finished=true;Blockbranch.LOGGER.error("JUNGLE_CLIENT_FAIL",e);client.stop();}
        }
    });}
    private JungleClientSmoke(){}
}
