package blockbranch.client.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import blockbranch.realistic.RealisticGenerator;
import blockbranch.realistic.UndergroundSmoke;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.RenderRegionCache;

/** Opt-in actual integrated-world cave snapshots; night vision is diagnostic. */
public final class UndergroundClientSmoke {
    private static volatile int request=-1,applied=-2;
    private static volatile BlockPos witness,camera;
    private static volatile Throwable failure;
    private static boolean opened,busy,done;
    private static int stage,stable,compiledManually,waitTicks;
    public static void register(){
        long deadline=System.nanoTime()+240_000_000_000L;
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(done||server.isDedicatedServer()||failure!=null||request==applied||request<0)return;
            try{
                var level=server.overworld();var players=server.getPlayerList().getPlayers();if(players.isEmpty())return;
                var player=players.getFirst();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
                if(witness==null){witness=UndergroundSmoke.fixture(g,level.getChunkSource().randomState());var p=witness;var base=((SparseColumn)level.getChunkAt(p)).blockbranch$terrain().base();level.setBlock(p,base.state(p.getX(),p.getY(),p.getZ()),3);}
                camera=witness;
                if(request==2)for(int distance:new int[]{8,6,4}) {
                    var candidate=witness.south(distance);
                    if(level.getBlockState(candidate).isAir()&&level.getBlockState(candidate.above()).isAir()&&level.getBlockState(candidate.above(2)).isAir()){camera=candidate;break;}
                }
                String name=player.getName().getString();var source=server.createCommandSourceStack();
                server.getCommands().performPrefixedCommand(source,"gamemode spectator "+name);
                server.getCommands().performPrefixedCommand(source,"tp "+name+" "+(camera.getX()+.5)+" "+camera.getY()+" "+(camera.getZ()+.5)+" "+(request==2?180:0)+" 5");
                server.getCommands().performPrefixedCommand(source,request==0?"effect clear "+name+" minecraft:night_vision":"effect give "+name+" minecraft:night_vision 999999 0 true");
                Blockbranch.LOGGER.info("UNDERGROUND_REVIEW_CAMERA stage={} witness={} camera={} nightVisionDiagnostic={}",request,witness,camera,request!=0);applied=request;
            }catch(Throwable error){failure=error;}
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(done)return;
            try{
                if(failure!=null)throw new AssertionError("Integrated cave review server",failure);
                require(System.nanoTime()<deadline,"Native underground capture timed out stage="+stage);
                var screen=client.gui.screen();String name=screen==null?"":screen.getClass().getSimpleName();
                if(name.equals("AccessibilityOnboardingScreen")){press(screen,"Continue");return;}
                if(!opened&&screen instanceof TitleScreen){opened=true;require(Files.exists(client.gameDirectory.toPath().resolve("saves/UndergroundReview/level.dat")),"Review copy exists");client.createWorldOpenFlows().openWorld("UndergroundReview",()->client.gui.setScreen(new TitleScreen()));return;}
                if(screen!=null&&(name.equals("ConfirmExperimentalFeaturesScreen")||name.equals("BackupConfirmScreen"))){press(screen,"Proceed");return;}
                if(client.level==null||client.player==null)return;
                if(request<0){request=0;return;}
                if(busy||applied!=stage||camera==null||client.player.position().distanceToSqr(camera.getX()+.5,camera.getY(),camera.getZ()+.5)>.01)return;
                var area=client.levelRenderer.viewArea();if(area==null)return;
                var cache=new RenderRegionCache();boolean ready=true;int meshes=0,submitted=0;
                // Observe the full visible circular render window. Compile a bounded
                // missing native mesh if traversal has not submitted it yet.
                var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
                int radius=area.getViewDistance();
                for(int dz=-radius;dz<=radius;dz++)for(int dx=-radius;dx<=radius;dx++) {
                    if(dx*dx+dz*dz>radius*radius)continue;
                    int cx=(witness.getX()>>4)+dx,cz=(witness.getZ()>>4)+dz;
                    if(!frustum.isVisible(new net.minecraft.world.phys.AABB(cx*16,area.minSectionY()*16,cz*16,cx*16+16,(area.maxSectionY()+1)*16,cz*16+16)))continue;
                    var chunk=client.level.getChunkSource().getChunk(cx,cz,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);
                    var terrain=chunk==null?null:((SparseColumn)chunk).blockbranch$terrain();if(terrain==null){ready=false;continue;}
                    require(terrain.base().underground().enabled(),"Client decoded enabled cave snapshot");
                    for(int sy=area.minSectionY();sy<=area.maxSectionY();sy++) {
                        if(!frustum.isVisible(new net.minecraft.world.phys.AABB(cx*16,sy*16,cz*16,cx*16+16,sy*16+16,cz*16+16)))continue;
                        if(((SparseColumn)chunk).blockbranch$read(sy).hasOnlyAir())continue;
                        var mesh=area.getRenderSectionAt(new BlockPos(cx*16+8,sy*16+8,cz*16+8));
                        if(mesh==null||mesh.getSectionNode()!=SectionPos.asLong(cx,sy,cz)){ready=false;continue;}
                        if(mesh.getSectionMesh()==CompiledSectionMesh.UNCOMPILED){
                            ready=false;
                            if(submitted<2){var region=cache.createRegion(client.level,SectionPos.asLong(cx,sy,cz));if(region!=null){mesh.compileSync(region);submitted++;compiledManually++;}}
                        }else if(mesh.getSectionMesh().hasRenderableLayers())meshes++;
                    }
                }
                if(++waitTicks%100==0)Blockbranch.LOGGER.info("UNDERGROUND_REVIEW_MESH_WAIT stage={} ready={} renderable={} manualSubmissions={} window={}..{}",stage,ready,meshes,compiledManually,area.minSectionY(),area.maxSectionY());
                if(!ready||meshes==0){stable=0;return;}
                require(client.level.getBlockState(camera).isAir()&&client.level.getBlockState(camera.above(2)).isAir(),"Client cave air matches server camera clearance");
                if(++stable<40)return;
                String file=stage==0?"underground-normal-dark.png":stage==1?"underground-night-vision-forward.png":"underground-night-vision-reverse.png";
                busy=true;
                net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{
                    try{
                        require(Files.isReadable(client.gameDirectory.toPath().resolve("screenshots/"+file)),"Native screenshot saved");
                        Blockbranch.LOGGER.info("UNDERGROUND_REVIEW_CAPTURE file={} diagnostic={} manualNativeMeshSubmissions={}",file,stage!=0,compiledManually);
                        stable=0;stage++;busy=false;
                        if(stage==3){
                            Files.writeString(Path.of(System.getProperty("blockbranch.undergroundClientResult")),"PASS actual integrated cave rendering; normal darkness + two night-vision diagnostic captures; manualNativeMeshSubmissions="+compiledManually+"\n");
                            done=true;client.disconnect(new TitleScreen(),false);client.stop();
                        }else request=stage;
                    }catch(Throwable error){failure=error;busy=false;}
                }));
            }catch(Throwable error){done=true;Blockbranch.LOGGER.error("UNDERGROUND_CLIENT_FAIL",error);try{Files.writeString(Path.of(System.getProperty("blockbranch.undergroundClientResult")),"FAIL "+error+"\n");}catch(Exception ignored){}client.stop();}
        });
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void press(net.minecraft.client.gui.screens.Screen screen,String label){for(var child:screen.children())if(child instanceof Button button&&button.active&&button.getMessage().getString().equals(label)){button.onPress(new KeyEvent(257,0,0));return;}}
    private UndergroundClientSmoke(){}
}
