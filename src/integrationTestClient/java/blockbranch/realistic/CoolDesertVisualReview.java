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
/** Native forest, stony-floor and shrub views; no staged terrain or seasonal weather. */
public final class CoolDesertVisualReview {
    private static final boolean ALLUVIAL=Boolean.getBoolean("blockbranch.alluvialVisual");
    private static final boolean SEASONAL=Boolean.getBoolean("blockbranch.seasonalForestVisual");
    private static final boolean XERIC=Boolean.getBoolean("blockbranch.warmXericVisual");
    private static final String WORLD=ALLUVIAL?"AlluvialVisualReview":SEASONAL?"SeasonalForestVisualReview":XERIC?"WarmXericVisualReview":"CoolDesertVisualReview";
    private static final Path DIR=Path.of(System.getProperty(ALLUVIAL?"blockbranch.alluvialVisualDirectory":SEASONAL?"blockbranch.seasonalForestVisualDirectory":XERIC?"blockbranch.warmXericVisualDirectory":"blockbranch.coolDesertVisualDirectory"));
    private static final boolean REOPEN=Boolean.getBoolean(ALLUVIAL?"blockbranch.alluvialVisualReopen":SEASONAL?"blockbranch.seasonalForestVisualReopen":"blockbranch.coolDesertVisualReopen");
    private static final long BEGAN=System.nanoTime();
    private static boolean opened,configured,saving;
    private static volatile boolean finished;
    private static volatile boolean posed;
    private static volatile int view;
    private static volatile Throwable failure;
    private static BlockPos edge;
    private static int positionedView=-1;
    private static final int[][] SCENES=ALLUVIAL?new int[][]{{1478348,-996764},{1468516,-675537},{1710195,-894731},{1478348,-996764}}:SEASONAL?new int[][]{{1488000,-304000},{1520000,-336000},{1520436,-336512},{1488000,-304000}}:XERIC?new int[][]{{432000,-784000},{1328000,-496000},{399776,-784512},{432000,-784000}}:new int[][]{{400000,-1200000},{656000,1264000},{400000,-1200000}};
    private static int settled;
    private static BlockPos camera;
    private static final List<Object> records=new ArrayList<>();
    private static final int VIEWS=SCENES.length;
    private static volatile float expectedYaw,expectedPitch;
    private static BoundingBox subject;

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
                if(view==VIEWS) {Files.writeString(DIR.resolve("result.txt"),"PASS settled native generated-world views: "+VIEWS);finished=true;return;}
                var commands=server.getCommands();var source=server.createCommandSourceStack();if(SEASONAL)commands.performPrefixedCommand(source,"gamemode creative "+player.getName().getString());
                commands.performPrefixedCommand(source,"time set noon");commands.performPrefixedCommand(source,"weather clear");
                var d=g.drainage(level.getChunkSource().randomState());
                if(positionedView!=view){
                    if(edge!=null)for(int z=(edge.getZ()-64)>>4;z<=(edge.getZ()+64)>>4;z++)for(int x=(edge.getX()-64)>>4;x<=(edge.getX()+64)>>4;x++)level.setChunkForced(x,z,false);
                    int x=SCENES[view][0],z=SCENES[view][1];edge=new BlockPos(x,d.sample(x,z).height()+1,z);positionedView=view;
                    for(int cz=(z-64)>>4;cz<=(z+64)>>4;cz++)for(int cx=(x-64)>>4;cx<=(x+64)>>4;cx++)level.setChunkForced(cx,cz,true);
                    Files.writeString(DIR.resolve("generator.json"),RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().toString());
                }
                int ex=edge.getX(),ey=edge.getY(),ez=edge.getZ();
                int cameraX=ex-24,cameraZ=ez-30;
                int cameraY=Math.max(ey+38,d.sample(cameraX,cameraZ).height()+38);
                if(!ALLUVIAL&&view==((XERIC||SEASONAL)?3:2)){
                    outer:for(int dz=-16;dz<=16;dz++)for(int dx=-16;dx<=16;dx++){
                        int wx=ex+dx,wz=ez+dz,h=d.sample(wx,wz).height();var canopy=g.canopy(wx,wz,level.getChunkSource().randomState());
                        if(canopy.state(wx,h+1,wz)==null&&canopy.state(wx,h+2,wz)==null){cameraX=wx;cameraZ=wz;cameraY=h+1;break outer;}
                    }
                }
                if(ALLUVIAL){
                    // Identical camera coordinates in both gate arms, derived from
                    // original terrain. Only native generated content differs.
                    var original=g.alluvialDefinition()>0?d.alluvialReference():d;ey=original.sample(ex,ez).height()+1;
                    if(view<3){cameraY=Math.max(original.sample(ex,ez).height()+39,original.sample(cameraX,cameraZ).height()+39);}
                    else{
                        // Frozen original review pose (visual269/reopen-views,
                        // view3). Geometry changes must not move the camera.
                        cameraX=1478324;cameraZ=-996788;cameraY=890;

                    }
                }
                camera=new BlockPos(cameraX,cameraY,cameraZ);

                var aim=new BlockPos(ex,ey,ez);
                if(view==3&&!XERIC&&!SEASONAL&&!ALLUVIAL)commands.performPrefixedCommand(source,"effect give "+player.getName().getString()+" minecraft:night_vision infinite 0 true");
                subject=new BoundingBox(ex-40,ey-16,ez-40,ex+40,Math.max(ey+40,64),ez+40);
                double cx=camera.getX()+.5,cy=camera.getY()+.2,cz=camera.getZ()+.5,dx=aim.getX()+.5-cx,dz=aim.getZ()+.5-cz,dy=aim.getY()+.5-(cy+player.getEyeHeight());
                double yaw=Math.toDegrees(Math.atan2(-dx,dz)),pitch=Math.toDegrees(Math.atan2(-dy,Math.hypot(dx,dz)));
                expectedYaw=(float)yaw;expectedPitch=(float)pitch;
                commands.performPrefixedCommand(source,"tp "+player.getName().getString()+" "+cx+" "+cy+" "+cz+" "+yaw+" "+pitch);
                records.add(Map.of("view",view,"camera",List.of(cx,cy,cz),"yaw",yaw,"pitch",pitch,"nightVision",view==3&&!XERIC&&!SEASONAL&&!ALLUVIAL,"target",List.of(ex,ey,ez),"gate",ALLUVIAL?g.alluvialDefinition():SEASONAL?g.seasonalForestDefinition():XERIC?g.warmXericDefinition():g.coolDesertDefinition()));
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
                    client.disconnect(new TitleScreen(),false);
                    client.stop();
                    return;
                }
                var screen=client.gui.screen();
                if(!opened&&screen instanceof TitleScreen) {
                    opened=true;
                    if(REOPEN)client.createWorldOpenFlows().openWorld(WORLD,()->client.gui.setScreen(new TitleScreen()));
                    else CreateWorldScreen.openFresh(client,()->client.gui.setScreen(new TitleScreen()));
                    return;
                }
                if(!configured&&screen instanceof CreateWorldScreen create) {
                    var state=create.getUiState();
                    state.setName(WORLD);
                    state.setSeed((SEASONAL||ALLUVIAL)?"1":"424242");
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
                saving=true;
                String file=(REOPEN?"reopen-":"")+(ALLUVIAL?"alluvial-":SEASONAL?"seasonal-forest-":XERIC?"warm-xeric-":"cool-desert-")+view+".png";
                net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()-> {
                    if(!Files.exists(DIR.resolve("screenshots/"+file)))failure=new AssertionError("Native capture missing");
                    view++;
                    posed=false;
                    saving=false;
                    settled=0;
                }
                ));
            }
            catch(Throwable t) {
                failure=t;
                finished=true;
                Blockbranch.LOGGER.error("COOL_DESERT_VISUAL_FAIL",t);
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
