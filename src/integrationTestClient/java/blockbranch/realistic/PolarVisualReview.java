package blockbranch.realistic;
import blockbranch.Blockbranch;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.*;
/** Native finite settled-view cost recording. Same surface-relative camera offsets,
 * dimensions and settings in both phases; displaced ice changes absolute camera Y.
 * 600 frames after 80 stable ticks, wall and render-thread CPU ns. No hardware FPS claim. */
public final class PolarVisualReview {
    private static final Path DIR=Path.of(System.getProperty("blockbranch.polarVisualDirectory","build/polar-ice-evidence/disabled"));
    private static final boolean REOPEN=Boolean.getBoolean("blockbranch.polarVisualReopen");
    private static final long BEGAN=System.nanoTime();
    private static boolean opened,configured,saving;
    private static volatile boolean finished;
    private static volatile boolean posed;
    private static volatile int view;
    private static volatile Throwable failure;
    private static BlockPos edge;
    private static int positionedView=-1;
    private static final int[][] SCENES=scenes();
    private static int[][] scenes(){var sites=Arrays.copyOf(PolarCostProbe.SITES,5);sites[4]=new int[]{-1589245,-1966053};return sites;}
    private static int settled;
    private static BlockPos camera;
    private static final List<Object> records=new ArrayList<>();
    private static final int VIEWS=5;
    private static volatile float expectedYaw,expectedPitch;
    private static BoundingBox subject;

    private static final boolean ON=Boolean.getBoolean("blockbranch.polarVisual");
    private static final java.lang.management.ThreadMXBean CPU=java.lang.management.ManagementFactory.getThreadMXBean();
    private static final List<long[]> frameRows=new ArrayList<>();
    private static final List<Object> frameRecords=new ArrayList<>();
    private static boolean measuring,captureReady;private static long startNs,startCpu;
    public static void frameBegin(boolean render){if(ON&&measuring&&render){startNs=System.nanoTime();startCpu=CPU.getCurrentThreadCpuTime();}else startNs=0;}
    public static void frameEnd(){if(ON&&startNs!=0){if(measuring)frameRows.add(new long[]{System.nanoTime()-startNs,CPU.getCurrentThreadCpuTime()-startCpu});startNs=0;}}
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server-> {
            if(finished||server.getPlayerList().getPlayers().isEmpty())return;
            if(posed){var p=server.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=true;p.onUpdateAbilities();p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);if(p.position().distanceToSqr(new net.minecraft.world.phys.Vec3(camera.getX()+.5,camera.getY()+.2,camera.getZ()+.5))>.001)server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+p.getName().getString()+" "+(camera.getX()+.5)+" "+(camera.getY()+.2)+" "+(camera.getZ()+.5)+" "+expectedYaw+" "+expectedPitch);return;}
            try {
                var level=server.overworld();
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,server);
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,false,server);
                var player=server.getPlayerList().getPlayers().getFirst();
                var g=(RealisticGenerator)level.getChunkSource().getGenerator();
                if(view==VIEWS) {Files.writeString(DIR.resolve(REOPEN?"reopen-result.txt":"result.txt"),"PASS five settled native polar cost/visual views; software rendering is not hardware acceptance");finished=true;return;}
                var commands=server.getCommands();var source=server.createCommandSourceStack();
                commands.performPrefixedCommand(source,"time set noon");commands.performPrefixedCommand(source,"weather clear");
                var d=g.drainage(level.getChunkSource().randomState());
                if(positionedView!=view){
                    if(edge!=null)for(int z=(edge.getZ()-64)>>4;z<=(edge.getZ()+64)>>4;z++)for(int x=(edge.getX()-64)>>4;x<=(edge.getX()+64)>>4;x++)level.setChunkForced(x,z,false);
                    int x=view==3?PolarCostProbe.coastX(d):SCENES[view][0],z=SCENES[view][1];edge=new BlockPos(x,d.sample(x,z).height()+1,z);positionedView=view;
                    for(int cz=(z-64)>>4;cz<=(z+64)>>4;cz++)for(int cx=(x-64)>>4;cx<=(x+64)>>4;cx++)level.setChunkForced(cx,cz,true);
                    Files.writeString(DIR.resolve("generator.json"),RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().toString());
                }
                int ex=edge.getX(),ey=edge.getY(),ez=edge.getZ();
                int original=d.sample(ex,ez).height();
                int camX=ex-24,camZ=ez-30,camY=d.sample(camX,camZ).height()+30;
                camera=new BlockPos(camX,camY,camZ);var aim=new BlockPos(ex,original+8,ez);
                subject=new BoundingBox(ex-64,original-16,ez-64,ex+64,original+48,ez+64);
                double cx=camera.getX()+.5,cy=camera.getY()+.2,cz=camera.getZ()+.5,dx=aim.getX()+.5-cx,dz=aim.getZ()+.5-cz,dy=aim.getY()+.5-(cy+player.getEyeHeight());
                double yaw=Math.toDegrees(Math.atan2(-dx,dz)),pitch=Math.toDegrees(Math.atan2(-dy,Math.hypot(dx,dz)));
                expectedYaw=(float)yaw;expectedPitch=(float)pitch;
                commands.performPrefixedCommand(source,"tp "+player.getName().getString()+" "+cx+" "+cy+" "+cz+" "+yaw+" "+pitch);
                records.add(Map.of("view",view,"camera",List.of(cx,cy,cz),"yaw",yaw,"pitch",pitch,"nightVision",false,"target",List.of(ex,ey,ez),"summerDryGate",g.summerDryDefinition()));
                posed=true;
            }
            catch(Throwable t) {
                failure=t;
            }
        }
        );
        ClientTickEvents.END_CLIENT_TICK.register(client-> {
            try {
                if(System.nanoTime()-BEGAN>900_000_000_000L)throw new AssertionError("Bounded ownership visual deadline");
                if(failure!=null)throw new AssertionError("Server witness",failure);
                if(saving)return;
                if(finished) {
                    saving=true;
                    Files.writeString(DIR.resolve(REOPEN?"reopen-views.json":"views.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(records));
                    Files.writeString(DIR.resolve("render-cost.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(frameRecords));
                    client.disconnect(new TitleScreen(),false);
                    client.stop();
                    return;
                }
                var screen=client.gui.screen();
                if(!opened&&screen instanceof TitleScreen) {
                    opened=true;
                    if(REOPEN)client.createWorldOpenFlows().openWorld("PolarVisualReview",()->client.gui.setScreen(new TitleScreen()));
                    else CreateWorldScreen.openFresh(client,()->client.gui.setScreen(new TitleScreen()));
                    return;
                }
                if(!configured&&screen instanceof CreateWorldScreen create) {
                    var state=create.getUiState();
                    state.setName("PolarVisualReview");
                    state.setSeed("424242");
                    state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
                    state.setAllowCommands(true);
                    state.setWorldType(state.getNormalPresetList().stream().filter(e->e.preset().unwrapKey().orElseThrow().identifier().toString().equals("blockbranch:realistic")).findFirst().orElseThrow());
                    configured=true;
                    press(create,"Create New World");
                    return;
                }
                if(screen!=null&&screen.getClass().getSimpleName().equals("ConfirmExperimentalFeaturesScreen")) {
                    press(screen,"Proceed");
                    return;
                }
                if(!posed||client.level==null||client.player==null)return;
                client.player.getAbilities().flying=true;client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                if(client.player.position().distanceToSqr(new net.minecraft.world.phys.Vec3(camera.getX()+.5,camera.getY()+.2,camera.getZ()+.5))>.001||Math.abs(net.minecraft.util.Mth.wrapDegrees(client.player.getYRot()-expectedYaw))>.02||Math.abs(client.player.getXRot()-expectedPitch)>.02) {
                    settled=0;
                    return;
                }
                client.options.fov().set(65);
                client.gui.toastManager().clear();
                if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                var area=client.levelRenderer.viewArea();
                if(area==null)return;
                if(measuring){if(frameRows.size()<600)return;measuring=false;frameRecords.add(java.util.Map.of("view",view,"frames",new ArrayList<>(frameRows),"heapBytes",java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()));frameRows.clear();captureReady=true;}
                if(!captureReady){
                var b=subject;
                boolean complete=true;
                var cache=new net.minecraft.client.renderer.chunk.RenderRegionCache();
                int submitted=0;
                for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++)for(int x=b.minX()>>4;x<=b.maxX()>>4;x++)for(int y=b.minY()>>4;y<=b.maxY()>>4;y++) {
                    if(Math.abs(x-(client.player.blockPosition().getX()>>4))>area.getViewDistance()||Math.abs(z-(client.player.blockPosition().getZ()>>4))>area.getViewDistance())continue;
                    var chunk=client.level.getChunkSource().getChunk(x,z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);
                    if(chunk==null||((blockbranch.cubic.SparseColumn)chunk).blockbranch$terrain()==null) {
                        complete=false;
                        continue;
                    }
                    if(!client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum.isVisible(new net.minecraft.world.phys.AABB(x*16,y*16,z*16,x*16+16,y*16+16,z*16+16)))continue;
                    if(Math.abs(x-(client.player.blockPosition().getX()>>4))>area.getViewDistance()||Math.abs(z-(client.player.blockPosition().getZ()>>4))>area.getViewDistance())continue;
                    var section=area.getRenderSectionAt(new BlockPos(x*16+8,y*16+8,z*16+8));
                    if(section==null||section.getSectionNode()!=net.minecraft.core.SectionPos.asLong(x,y,z)) {
                        complete=false;
                        continue;
                    }
                    if(section.getSectionMesh()==net.minecraft.client.renderer.chunk.CompiledSectionMesh.UNCOMPILED) {
                        complete=false;
                        if(submitted<2) {
                            var region=cache.createRegion(client.level,section.getSectionNode());
                            if(region!=null) {
                                section.compileSync(region);
                                submitted++;
                            }
                        }
                    }
                }
                if(!complete||!client.levelRenderer.hasRenderedAllSections()) {
                    settled=0;
                    return;
                }
                if(++settled<80)return;
                frameRows.clear();measuring=true;return;
                }
                saving=true;
                String file=(REOPEN?"reopen-":"")+"polar-"+view+".png";
                net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()-> {
                    if(!Files.exists(DIR.resolve("screenshots/"+file)))failure=new AssertionError("Native capture missing");
                    view++;
                    posed=false;
                    saving=false;
                    settled=0;captureReady=false;
                }
                ));
            }
            catch(Throwable t) {
                failure=t;
                finished=true;
                Blockbranch.LOGGER.error("POLAR_VISUAL_FAIL",t);
                try {
                    Files.writeString(DIR.resolve(REOPEN?"reopen-result.txt":"result.txt"),"FAIL "+t);
                }
                catch(Exception ignored) {
                }
                client.stop();
            }
        }
        );
    }
    private static void press(net.minecraft.client.gui.screens.Screen screen,String label) {
        for(var v:screen.children())if(v instanceof Button button&&button.active&&button.getMessage().getString().equals(label)) {
            button.onPress(new KeyEvent(257,0,0));
            return;
        }
        throw new AssertionError("Missing UI button "+label);
    }
}
