package blockbranch.client.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Submerged, pose-verified coverage; aphotic aid is explicitly labelled and removed. */
public final class LukewarmOceanClientSmoke {
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
        final boolean deep=Boolean.getBoolean("blockbranch.deepLukewarmOceanClientOnly");
        final FixtureSceneReadiness capture=new FixtureSceneReadiness();
        final long deadline=System.nanoTime()+1_200_000_000_000L;
        int stage,settled;boolean saving,done;BlockPos bed;
        public void onEndTick(net.minecraft.client.Minecraft client){
            if(done||saving)return;
            try{
                if(System.nanoTime()>deadline)throw new AssertionError("Warm marine client timeout stage="+stage+" "+capture.diagnostic());
                if(client.level==null||client.player==null)return;var level=client.level;
                if(stage==0){if(Math.abs(client.player.getX()-1401431)>2)return;stage=1;ClientPlayNetworking.send(new RealisticProbe(90));return;}
                if(stage==1){
                    var p=client.player.blockPosition();var terrain=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain();if(terrain==null)return;
                    int h=terrain.base().top(Heightmap.Types.OCEAN_FLOOR.isOpaque(),p.getX(),p.getZ(),ColumnLimits.MAX_Y-1)-1;
                    var candidate=new BlockPos(p.getX(),h,p.getZ());
                    if(!level.getBiome(candidate).is(LukewarmOceanSmoke.cold()?(deep?Biomes.DEEP_COLD_OCEAN:Biomes.COLD_OCEAN):(deep?Biomes.DEEP_LUKEWARM_OCEAN:Biomes.LUKEWARM_OCEAN))
                        ||level.getBlockState(candidate)!=terrain.base().state(p.getX(),h,p.getZ())
                        ||!level.getFluidState(candidate.above()).isSource()||!deep&&!level.getBlockState(candidate.above()).is(Blocks.SEAGRASS))return;
                    bed=candidate;Blockbranch.LOGGER.info("LUKEWARM_CAPTURE_BED deep={} bed={} depth={}",deep,bed,TerrainShape.SEA_LEVEL-h-1);
                    stage=2;ClientPlayNetworking.send(new RealisticProbe(91));return;
                }
                int lastCapture=deep?4:3;
                if(stage>=2&&stage<=lastCapture){
                    boolean second=stage==3,aided=deep&&stage>=3;
                    if(client.player.hasEffect(MobEffects.NIGHT_VISION)!=aided)return;
                    var requested=LukewarmOceanViews.at(bed.getX(),bed.getZ(),second);
                    var cameraColumn=((SparseColumn)level.getChunk(requested.x()>>4,requested.z()>>4)).blockbranch$terrain();if(cameraColumn==null)return;
                    int floor=cameraColumn.base().top(Heightmap.Types.OCEAN_FLOOR.isOpaque(),requested.x(),requested.z(),ColumnLimits.MAX_Y-1)-1;
                    if(client.player.position().distanceToSqr(requested.x()+.5,floor+requested.aboveBed(),requested.z()+.5)>.0025
                        ||Math.abs(net.minecraft.util.Mth.wrapDegrees(client.player.getYRot()-(float)requested.yaw()))>.02
                        ||Math.abs(client.player.getXRot()-(float)requested.pitch())>.02)return;
                    var eye=BlockPos.containing(client.player.getEyePosition());if(!level.getFluidState(eye).isSource())throw new AssertionError("Marine camera must remain underwater");
                    client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                    String scene=(LukewarmOceanSmoke.cold()?"cold-":"")+(deep?"deep-lukewarm":"lukewarm")+"-"+stage;
                    if(!capture.ready(client,scene)){settled=0;return;}
                    var root=client.levelRenderer.viewArea().getRenderSectionAt(bed);if(root==null||!root.getSectionMesh().hasRenderableLayers()){settled=0;return;}
                    if(++settled<100)return;settled=0;
                    String rawFile=deep?(stage==2?"deep-lukewarm-ocean-natural-darkness.png":stage==3?"deep-lukewarm-ocean-bed-night-vision.png":"deep-lukewarm-ocean-detail-night-vision.png")
                        :stage==2?"lukewarm-ocean-seagrass.png":"lukewarm-ocean-meadow.png";
                    String file=LukewarmOceanSmoke.cold()?rawFile.replace("lukewarm","cold"):rawFile;
                    Blockbranch.LOGGER.info("LUKEWARM_CAPTURE_COVERAGE file={} visibility={} {}",file,aided?"night_vision_diagnostic":"normal",capture.diagnostic());saving=true;
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                        try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Missing warm marine screenshot");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage<=lastCapture?(stage==3?92:95):93));}
                        catch(Throwable failure){done=true;Blockbranch.LOGGER.error("LUKEWARM_CLIENT_FAIL",failure);client.stop();}
                    }));return;
                }
                if(stage==lastCapture+1){if(!level.getBlockState(bed).is(Blocks.WATER)||!level.getFluidState(bed.above()).isSource())return;if(!deep&&!level.getBlockState(bed.above()).is(Blocks.WATER))return;if(++settled<20)return;settled=0;stage++;ClientPlayNetworking.send(new RealisticProbe(94));return;}
                if(stage==lastCapture+2){
                    if(client.player.hasEffect(MobEffects.NIGHT_VISION))return;var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();
                    for(int dy=0;dy<=2;dy++){var p=bed.above(dy);if(level.getBlockState(p)!=base.state(p.getX(),p.getY(),p.getZ()))return;}
                    if(level.getHeight(Heightmap.Types.WORLD_SURFACE,bed.getX(),bed.getZ())!=TerrainShape.SEA_LEVEL)throw new AssertionError("Warm marine water plane changed");
                    Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");
                    Blockbranch.LOGGER.info("LUKEWARM_CLIENT_RENDER_SUPPORT_WATER_RESTORE_PASS deep={} bed={} aidCleared=true",deep,bed);done=true;client.stop();
                }
            }catch(Throwable failure){done=true;Blockbranch.LOGGER.error("LUKEWARM_CLIENT_FAIL",failure);client.stop();}
        }
    });}
    private LukewarmOceanClientSmoke(){}
}
