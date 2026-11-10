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

/** Focused native Forest render/leaf-update fixture, without UI automation or user worlds. */
public final class ForestClientSmoke {
    public static void register(boolean birch){register(birch,false);}
    public static void register(boolean birch,boolean swamp){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final boolean grove=Boolean.getBoolean("blockbranch.groveClientOnly");
        final boolean snowy=Boolean.getBoolean("blockbranch.snowyTaigaClientOnly");
        final boolean taiga=Boolean.getBoolean("blockbranch.taigaClientOnly");
        final boolean mangrove=Boolean.getBoolean("blockbranch.mangroveClientOnly");
        int stage,settled;boolean finished,saving,groundMoved;BlockPos bed;long deadline=System.nanoTime()+600_000_000_000L,nextProgress;
        public void onEndTick(net.minecraft.client.Minecraft client){
            if(finished||saving)return;
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Forest client timed out at stage "+stage);
                if(client.player==null||client.level==null){if(stage>0)throw new AssertionError("Disconnected during Forest fixture");return;}
                if(stage==0){stage=1;ClientPlayNetworking.send(new RealisticProbe(122));return;}
                var level=client.level;
                if(stage==1&&bed==null){
                    int x=client.player.blockPosition().getX()+18,z=client.player.blockPosition().getZ()-28;var t=((SparseColumn)level.getChunkAt(client.player.blockPosition())).blockbranch$terrain();if(t==null)return;
                    // Recover the known root from the fixture camera offset; no world data is fabricated.
                    int h=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
                    if(!level.getBiome(new BlockPos(x,h,z)).is(grove?Biomes.GROVE:snowy?Biomes.SNOWY_TAIGA:taiga?Biomes.TAIGA:mangrove?Biomes.MANGROVE_SWAMP:swamp?Biomes.SWAMP:birch?Biomes.BIRCH_FOREST:Biomes.FOREST))return;
                    // Server supplies root ground Y through a fixed 5 m camera offset.
                    int ground=(int)Math.round(client.player.getY())-5;bed=new BlockPos(x,ground,z);
                    if(!level.getBlockState(bed.above()).is(taiga||snowy||grove?Blocks.SPRUCE_WOOD:mangrove?Blocks.MANGROVE_WOOD:birch?Blocks.BIRCH_LOG:Blocks.OAK_LOG))return;
                }
                if((birch||swamp||mangrove||taiga||snowy||grove)&&stage==1&&!groundMoved){groundMoved=true;ClientPlayNetworking.send(new RealisticProbe(126));return;}
                if(stage==1||stage==2||stage==5){
                    if(!client.gui.hud.isHidden())client.gui.hud.toggle();client.gui.toastManager().clear();
                    var area=client.levelRenderer.viewArea();if(area==null)return;int loaded=0,rendered=0,logs=0,leaves=0;
                    for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){
                        int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var t=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(t==null)continue;loaded++;
                        int x=cx*16+8,z=cz*16+8,h=t.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,blockbranch.cubic.ColumnLimits.MAX_Y-1)-1;
                        var mesh=area.getRenderSectionAt(new BlockPos(x,h,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;
                        for(int packed:t.base().canopy().encode()){int key=packed>>>blockbranch.cubic.CanopyLayer.STATE_BITS,y=(key>>>8)+blockbranch.realistic.TerrainShape.FLOOR;var s=t.base().state(cx*16+(key&15),y,cz*16+((key>>>4)&15));if(s.is(taiga||snowy||grove?Blocks.SPRUCE_WOOD:mangrove?Blocks.MANGROVE_WOOD:birch?Blocks.BIRCH_LOG:Blocks.OAK_LOG))logs++;if(s.is(taiga||snowy||grove?Blocks.SPRUCE_LEAVES:mangrove?Blocks.MANGROVE_LEAVES:birch?Blocks.BIRCH_LEAVES:Blocks.OAK_LEAVES))leaves++;}
                    }
                    if(System.nanoTime()>nextProgress){nextProgress=System.nanoTime()+5_000_000_000L;Blockbranch.LOGGER.info("FOREST_VIEW_READY stage={} loaded={} rendered={} logs={} leaves={}",stage,loaded,rendered,logs,leaves);}
                    if(loaded<121||rendered<40||logs<20||leaves<150){settled=0;return;}
                    if(++settled<100)return;settled=0;saving=true;String file=(grove?"grove":snowy?"snowy-taiga":taiga?"taiga":mangrove?"mangrove-swamp":swamp?"swamp":birch?"birch-forest":"forest")+(stage==1?"-ground":stage==5?"-details":"-canopy")+(Boolean.getBoolean("blockbranch.birchForestSecondSite")||Boolean.getBoolean("blockbranch.swampSecondSite")||Boolean.getBoolean("blockbranch.mangroveSecondSite")?"-second":"")+".png";
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                        try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Forest screenshot missing");saving=false;if(stage==2&&!swamp&&!mangrove&&!taiga&&!snowy&&!grove){stage=5;ClientPlayNetworking.send(new RealisticProbe(127));}else if(stage==5){stage=3;ClientPlayNetworking.send(new RealisticProbe(124));}else{stage++;ClientPlayNetworking.send(new RealisticProbe(stage==2?123:124));}}catch(Throwable e){finished=true;Blockbranch.LOGGER.error("FOREST_CLIENT_FAIL",e);client.stop();}
                    }));return;
                }
                var test=bed.above(40).east();
                if(stage==3){if(swamp&&(!level.getBlockState(bed.above(35).east()).isAir()||!level.getBlockState(bed.above(35).east().below()).isAir()))return;var state=level.getBlockState(test);if(!state.isAir()&&(!state.is(taiga||snowy||grove?Blocks.SPRUCE_LEAVES:mangrove?Blocks.MANGROVE_LEAVES:birch?Blocks.BIRCH_LEAVES:Blocks.OAK_LEAVES)||state.getValue(LeavesBlock.DISTANCE)!=7))return;if(++settled<20)return;settled=0;stage=4;ClientPlayNetworking.send(new RealisticProbe(125));return;}
                if(stage==4){var base=((SparseColumn)level.getChunkAt(test)).blockbranch$terrain().base();if(level.getBlockState(test)!=base.state(test.getX(),test.getY(),test.getZ()))return;Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("FOREST_CLIENT_RENDER_LEAF_UPDATE_RESTORE_PASS birch={} swamp={} bed={}",birch,swamp,bed);finished=true;client.stop();}
            }catch(Throwable e){finished=true;Blockbranch.LOGGER.error("FOREST_CLIENT_FAIL",e);client.stop();}
        }
    });}
    private ForestClientSmoke(){}
}
