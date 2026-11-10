package blockbranch.client.realistic;
import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import blockbranch.realistic.RealisticProbe;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;

/** Native Ocean surface/kelp rendering and observed growth/support restoration. */
public final class OceanClientSmoke {
 public static void register(){ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){int stage,settled;boolean saving,done;BlockPos bed;int height;long deadline=System.nanoTime()+600_000_000_000L;
 public void onEndTick(net.minecraft.client.Minecraft client){if(done||saving)return;try{if(System.nanoTime()>deadline)throw new AssertionError("Ocean client timeout stage="+stage);if(client.level==null||client.player==null)return;var level=client.level;
 if(stage==0){if(Math.abs(client.player.getX()-1401431)>2)return;stage=1;ClientPlayNetworking.send(new RealisticProbe(90));return;}
 if(stage==1&&bed==null){int x=client.player.blockPosition().getX(),z=client.player.blockPosition().getZ();var t=((SparseColumn)level.getChunkAt(client.player.blockPosition())).blockbranch$terrain();if(t==null)return;int y=t.base().top(Heightmap.Types.OCEAN_FLOOR.isOpaque(),x,z,ColumnLimits.MAX_Y-1)-1;var p=new BlockPos(x,y,z);if(!level.getBiome(p).is(net.minecraft.world.level.biome.Biomes.OCEAN)||!level.getBlockState(p.above()).is(Blocks.KELP_PLANT))return;bed=p;for(int h=1;h<12;h++)if(level.getBlockState(bed.above(h)).is(Blocks.KELP)){height=h;break;}if(height<2){bed=null;return;}}
 if(bed==null)return;var tip=bed.above(height);
 if(stage<=2){if(client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION))throw new AssertionError("Shallow Ocean capture must use normal visibility");client.gui.toastManager().clear();if(!client.gui.hud.isHidden())client.gui.hud.toggle();if(stage==2&&client.player.getY()>bed.getY()+3)return;var area=client.levelRenderer.viewArea();if(area==null)return;int loaded=0,rendered=0;for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){int cx=(bed.getX()>>4)+dx,cz=(bed.getZ()>>4)+dz;var t=((SparseColumn)level.getChunk(cx,cz)).blockbranch$terrain();if(t==null)continue;loaded++;int x=cx*16+8,z=cz*16+8,y=t.base().top((stage==1?Heightmap.Types.WORLD_SURFACE:Heightmap.Types.OCEAN_FLOOR).isOpaque(),x,z,ColumnLimits.MAX_Y-1)-1;var mesh=area.getRenderSectionAt(new BlockPos(x,y,z));if(mesh!=null&&mesh.getSectionMesh().hasRenderableLayers())rendered++;}if(loaded<121||rendered<30){settled=0;return;}if(++settled<100)return;settled=0;saving=true;String file=stage==1?"ocean-surface.png":"ocean-underwater-kelp.png";net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,msg->client.execute(()->{try{if(!Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file)))throw new AssertionError("Missing Ocean capture");saving=false;stage++;ClientPlayNetworking.send(new RealisticProbe(stage==2?91:92));}catch(Throwable e){done=true;Blockbranch.LOGGER.error("OCEAN_CLIENT_FAIL",e);client.stop();}}));return;}
 if(stage==3){if(!level.getBlockState(tip.above()).is(Blocks.KELP)||!level.getBlockState(tip).is(Blocks.KELP_PLANT))return;if(++settled<20)return;settled=0;stage=4;ClientPlayNetworking.send(new RealisticProbe(93));return;}
 if(stage==4){for(int y=1;y<=height;y++)if(!level.getBlockState(bed.above(y)).is(Blocks.WATER))return;if(++settled<20)return;settled=0;stage=5;ClientPlayNetworking.send(new RealisticProbe(94));return;}
 if(stage==5){var base=((SparseColumn)level.getChunkAt(bed)).blockbranch$terrain().base();for(int y=0;y<=height+1;y++)if(level.getBlockState(bed.above(y))!=base.state(bed.getX(),bed.getY()+y,bed.getZ()))return;Files.writeString(Path.of(System.getProperty("blockbranch.realisticClientResult")),"PASS\n");Blockbranch.LOGGER.info("OCEAN_CLIENT_RENDER_NATIVE_GROW_SUPPORT_WATER_RESTORE_PASS {}",bed);done=true;client.stop();}
 }catch(Throwable e){done=true;Blockbranch.LOGGER.error("OCEAN_CLIENT_FAIL",e);client.stop();}}
 });}
 private OceanClientSmoke(){}
}
