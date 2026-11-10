package blockbranch.client.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.RealisticProbe;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Normal-visibility bank/elevated views plus native bed update and profile restoration. */
public final class RiverClientSmoke {
 public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){final boolean frozen=Boolean.getBoolean("blockbranch.frozenRiverClientOnly");int stage,settled,waiting;boolean saving,done;BlockPos bed;int waterY;final FixtureSceneReadiness capture=new FixtureSceneReadiness();long deadline=System.nanoTime()+1_200_000_000_000L;
 public void onEndTick(net.minecraft.client.Minecraft client){if(done||saving)return;try{if(System.nanoTime()>deadline)throw new AssertionError("River client timeout stage="+stage+" "+capture.diagnostic());if(client.level==null||client.player==null)return;var l=client.level;
  if(stage==0){if(Math.abs(client.player.getX()-1401431)>2)return;stage=1;ClientPlayNetworking.send(new RealisticProbe(90));return;}
  if(stage==1){var q=client.player.blockPosition();var t=((SparseColumn)l.getChunkAt(q)).blockbranch$terrain();if(t==null)return;int y=t.base().top((frozen?Heightmap.Types.WORLD_SURFACE:Heightmap.Types.OCEAN_FLOOR).isOpaque(),q.getX(),q.getZ(),ColumnLimits.MAX_Y-1)-1;var p=new BlockPos(q.getX(),y,q.getZ());if(!l.getBiome(p).is(frozen?net.minecraft.world.level.biome.Biomes.FROZEN_RIVER:net.minecraft.world.level.biome.Biomes.RIVER)||l.getBlockState(p)!=t.base().state(p.getX(),p.getY(),p.getZ())||!(frozen?l.getBlockState(p).is(Blocks.ICE)&&l.getFluidState(p.below()).isSource():l.getFluidState(p.above()).isSource()))return;Blockbranch.LOGGER.info("RIVER_CAPTURE_BED {} block={}",p,l.getBlockState(p));bed=p;waterY=t.base().top(Heightmap.Types.WORLD_SURFACE.isOpaque(),p.getX(),p.getZ(),ColumnLimits.MAX_Y-1);stage=2;ClientPlayNetworking.send(new RealisticProbe(91));return;}
  if(stage==2||stage==3){if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("River captures use normal visibility");if(stage==2&&client.player.position().distanceToSqr(bed.getX(),client.player.getY(),bed.getZ())<64){settled=0;return;}if(stage==3&&Math.abs(client.player.getY()-(Boolean.getBoolean("blockbranch.terrainContourComparison")?119:waterY+55))>.05){settled=0;return;}client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();if(!capture.ready(client,(frozen?"frozen-river":"river")+"-"+stage)){settled=0;return;}var area=client.levelRenderer.viewArea();if(area==null)return;if(frozen&&!RealisticClientSmoke.collateralSceneReady(client)){settled=0;return;}int loaded=0,required=0,rendered=0,surfaceRequired=0,canopyRequired=0,canopyReady=0;
   var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
   int cameraX=client.player.blockPosition().getX()>>4,cameraZ=client.player.blockPosition().getZ()>>4;
   for(int dz=-16;dz<=16;dz++)for(int dx=-16;dx<=16;dx++){
    if(dx*dx+dz*dz>256)continue;int cx=cameraX+dx,cz=cameraZ+dz;
    if(!frustum.isVisible(new net.minecraft.world.phys.AABB(cx*16,blockbranch.realistic.TerrainShape.FLOOR,cz*16,cx*16+16,blockbranch.realistic.TerrainShape.CEILING,cz*16+16)))continue;
    required++;var t=((SparseColumn)l.getChunk(cx,cz)).blockbranch$terrain();if(t==null)continue;loaded++;
    for(int sy:t.base().canopy().sections()){
     if(!frustum.isVisible(new net.minecraft.world.phys.AABB(cx*16,sy*16,cz*16,cx*16+16,sy*16+16,cz*16+16)))continue;
     canopyRequired++;var cm=area.getRenderSectionAt(new BlockPos(cx*16+8,sy*16+8,cz*16+8));if(cm!=null&&cm.getSectionMesh().hasRenderableLayers())canopyReady++;
    }
    int x=cx*16+8,z=cz*16+8,y=t.base().top(Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,ColumnLimits.MAX_Y-1)-1;
    if(frustum.isVisible(new net.minecraft.world.phys.AABB(cx*16,y-16,cz*16,cx*16+16,y+16,cz*16+16))){surfaceRequired++;var mesh=area.getRenderSectionAt(new BlockPos(x,y,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;}
   }
   if(loaded<required||rendered<surfaceRequired||canopyReady<canopyRequired){if(++waiting%200==0)Blockbranch.LOGGER.info("RIVER_CAPTURE_WAIT columns={}/{} surface={}/{} canopy={}/{}",loaded,required,rendered,surfaceRequired,canopyReady,canopyRequired);settled=0;return;}
   if(!client.levelRenderer.hasRenderedAllSections()){settled=0;return;}if(++settled<100)return;Blockbranch.LOGGER.info("RIVER_CAPTURE_MESH_READY stage={} loaded={} surfaceMeshes={} canopySections={}/{}",stage,loaded,rendered,canopyReady,canopyRequired);settled=0;Blockbranch.LOGGER.info("RIVER_CAPTURE_COVERAGE {}",capture.diagnostic());saving=true;String file=(frozen?"frozen-river":"river")+(stage==2?"-ground.png":"-elevated.png");net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,msg->client.execute(()->{try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Missing River capture");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage==3?92:93));}catch(Throwable e){done=true;Blockbranch.LOGGER.error("RIVER_CLIENT_FAIL",e);client.stop();}}));return;}
  if(stage==4){if(!l.getBlockState(bed).is(Blocks.CLAY))return;if(++settled<20)return;settled=0;stage=5;ClientPlayNetworking.send(new RealisticProbe(94));return;}
  if(stage==5){var base=((SparseColumn)l.getChunkAt(bed)).blockbranch$terrain().base();if(l.getBlockState(bed)!=base.state(bed.getX(),bed.getY(),bed.getZ()))return;Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("RIVER_CLIENT_RENDER_NATIVE_BED_UPDATE_RESTORE_PASS {}",bed);done=true;client.stop();}
 }catch(Throwable e){done=true;Blockbranch.LOGGER.error("RIVER_CLIENT_FAIL",e);client.stop();}}
 });}
 private RiverClientSmoke(){}
}
