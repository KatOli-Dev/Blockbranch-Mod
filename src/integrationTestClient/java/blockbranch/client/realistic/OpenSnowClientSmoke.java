package blockbranch.client.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.RealisticProbe;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;

/** Actual ground/overlook snow rendering and streamed native support removal/restoration. */
public final class OpenSnowClientSmoke {
 public static void register(){final boolean slopes=Boolean.getBoolean("blockbranch.snowSlopesClientOnly");ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){int stage,settled;boolean saving,done;BlockPos bed;double groundY;long deadline=System.nanoTime()+(Boolean.getBoolean("blockbranch.terrainContourComparison")?900_000_000_000L:600_000_000_000L);
 public void onEndTick(net.minecraft.client.Minecraft client){if(done||saving)return;try{if(System.nanoTime()>deadline)throw new AssertionError("Open-snow client timeout stage="+stage);if(client.level==null||client.player==null)return;var level=client.level;if(stage==0){if(Math.abs(client.player.getX()-1401431)>2)return;stage=1;ClientPlayNetworking.send(new RealisticProbe(90));return;}
 if(stage==1){int x=client.player.blockPosition().getX(),z=client.player.blockPosition().getZ(),y=(int)Math.round(client.player.getY())-5;var p=new BlockPos(x,y,z);if(!level.getBiome(p).is(slopes?net.minecraft.world.level.biome.Biomes.SNOWY_SLOPES:net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS)||!level.getBlockState(p.above()).is(Blocks.SNOW))return;bed=p;stage=2;ClientPlayNetworking.send(new RealisticProbe(91));return;}if(bed==null)return;
 if(stage==2||stage==3){if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Open snow capture must use normal visibility");if(stage==2&&client.player.position().distanceToSqr(bed.getX(),client.player.getY(),bed.getZ())<64)return;if(stage==3&&client.player.getY()<groundY+20)return;client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();var area=client.levelRenderer.viewArea();if(area==null)return;if(Boolean.getBoolean("blockbranch.terrainContourComparison")&&!RealisticClientSmoke.collateralSceneReady(client)){settled=0;return;}int loaded=0,rendered=0;for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var t=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(t==null)continue;loaded++;int x=cx*16+8,z=cz*16+8,y=t.base().top(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,ColumnLimits.MAX_Y-1)-1;var mesh=area.getRenderSectionAt(new BlockPos(x,y,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;}if(loaded<121||rendered<30){settled=0;return;}if(++settled<100)return;settled=0;saving=true;if(stage==2)groundY=client.player.getY();String file=(slopes?"snowy-slopes":"snowy-plains")+(stage==2?"-ground.png":"-overlook.png");if(slopes&&stage==3&&Boolean.getBoolean("blockbranch.terrainContourComparison"))traceSnowPixels(client);net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,msg->client.execute(()->{try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Missing open snow image");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage==3?92:93));}catch(Throwable e){done=true;Blockbranch.LOGGER.error("OPEN_SNOW_CLIENT_FAIL",e);client.stop();}}));return;}
 if(stage==4){if(!level.getBlockState(bed).isAir()||!level.getBlockState(bed.above()).isAir())return;if(++settled<20)return;settled=0;stage=5;ClientPlayNetworking.send(new RealisticProbe(94));return;}
 if(stage==5){var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();for(int y=0;y<=1;y++)if(level.getBlockState(bed.above(y))!=base.state(bed.getX(),bed.getY()+y,bed.getZ()))return;Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("OPEN_SNOW_CLIENT_RENDER_SUPPORT_RESTORE_PASS {}",bed);done=true;client.stop();}
 }catch(Throwable e){done=true;Blockbranch.LOGGER.error("OPEN_SNOW_CLIENT_FAIL",e);client.stop();}}
 });}
 /** Trace the actual native camera basis; prior analytic estimates mirrored left/right. */
 private static void traceSnowPixels(net.minecraft.client.Minecraft client){
  var camera=client.gameRenderer.mainCamera();var plane=camera.getNearPlane(client.options.fov().get().floatValue());
  for(int[]pixel:new int[][]{{32,550},{120,530},{220,550}}){var direction=plane.getPointOnPlane(2*pixel[0]/1024f-1,1-2*pixel[1]/576f).normalize();
   for(double distance=1;distance<600;distance+=.25){var position=camera.position().add(direction.scale(distance));var p=BlockPos.containing(position);var state=client.level.getBlockState(p);if(!state.isAir()&&state.getFluidState().isEmpty()){Blockbranch.LOGGER.info("SNOW_PIXEL_NATIVE_WITNESS pixel={},{} camera={} hit={} block={}",pixel[0],pixel[1],camera.position(),p,state);break;}}
  }
 }
 private OpenSnowClientSmoke(){}
}
