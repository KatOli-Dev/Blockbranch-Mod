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
/** Native elevated and ground views of boreal slope sites (seed 1). Camera poses are fixed
 * functions of each site, never of trees, so a candidate and the published-270 build use the
 * same pose; no blocks are staged. Software rendering is evidence, not acceptance. */
public final class BorealSlopeVisualReview {
    private static final Path DIR=Path.of(System.getProperty("blockbranch.borealSlopeVisualDirectory","build/boreal-slope-evidence/disabled-visual"));
    private static final boolean REOPEN=Boolean.getBoolean("blockbranch.borealSlopeVisualReopen");
    private static final long BEGAN=System.nanoTime();
    private static boolean opened,configured,saving;
    private static volatile boolean finished;
    private static volatile boolean posed;
    private static volatile int view;
    private static volatile Throwable failure;
    private static BlockPos edge;
    private static int positionedView=-1;
    private static final int[][] SCENES={{-1392000,-1616000},{-1412000,-1588000},{68000,-1644000}};
    /** Ground poses recorded by an earlier run ({@code view -> x,z}); a build must stand at exactly these. */
    private static final Map<Integer,int[]> REFERENCE=reference();
    private static Map<Integer,int[]> reference(){
        var path=System.getProperty("blockbranch.borealSlopeReferenceViews");if(path==null||path.isBlank())return null;
        try{var map=new HashMap<Integer,int[]>();for(var e:com.google.gson.JsonParser.parseString(Files.readString(Path.of(path))).getAsJsonArray()){var o=e.getAsJsonObject();var cam=o.getAsJsonArray("camera");map.put(o.get("view").getAsInt(),new int[]{(int)Math.floor(cam.get(0).getAsDouble()),(int)Math.floor(cam.get(2).getAsDouble())});}return map;}
        catch(Exception e){throw new IllegalStateException("Unreadable reference views",e);}
    }
    /** Air through a 5 x 5 footprint from below the ground to head height and above, so the camera never stands in a stem or crown. */
    private static boolean clear(RealisticGenerator g,DrainageNetwork d,net.minecraft.world.level.levelgen.RandomState random,int x,int z){
        int h=d.sample(x,z).height();
        for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++){var canopy=g.canopy(x+dx,z+dz,random);for(int y=h+1;y<=h+5;y++){var st=canopy.state(x+dx,y,z+dz);if(st!=null&&!st.isAir())return false;}}
        return true;
    }
    private static int settled;
    private static BlockPos camera;
    private static final List<Object> records=new ArrayList<>();
    private static final int VIEWS=2*SCENES.length;
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
                if(view==VIEWS) {Files.writeString(DIR.resolve(REOPEN?"reopen-result.txt":"result.txt"),"PASS settled native boreal slope views; no staged terrain");finished=true;return;}
                var commands=server.getCommands();var source=server.createCommandSourceStack();
                commands.performPrefixedCommand(source,"time set noon");commands.performPrefixedCommand(source,"weather clear");
                var d=g.drainage(level.getChunkSource().randomState());
                if(positionedView!=view){
                    if(edge!=null)for(int z=(edge.getZ()-64)>>4;z<=(edge.getZ()+64)>>4;z++)for(int x=(edge.getX()-64)>>4;x<=(edge.getX()+64)>>4;x++)level.setChunkForced(x,z,false);
                    int x=SCENES[view/2][0],z=SCENES[view/2][1];edge=new BlockPos(x,d.sample(x,z).height()+1,z);positionedView=view;
                    for(int cz=(z-64)>>4;cz<=(z+64)>>4;cz++)for(int cx=(x-64)>>4;cx<=(x+64)>>4;cx++)level.setChunkForced(cx,cz,true);
                    Files.writeString(DIR.resolve("generator.json"),RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().toString());
                }
                int ex=edge.getX(),ey=edge.getY(),ez=edge.getZ();
                boolean ground=view%2==1;int cameraX=ex-24,cameraZ=ez-30,cameraY;int[] relocatedFrom=null;
                if(ground){
                    var random=level.getChunkSource().randomState();int[] pick=REFERENCE!=null?REFERENCE.get(view):null;
                    if(pick==null){
                        double best=Double.MAX_VALUE;
                        for(int z=ez-40;z<=ez;z++)for(int x=ex-40;x<=ex;x++){
                            if(!clear(g,d,random,x,z))continue;double dist=Math.hypot(x-(ex-16),z-(ez-16));
                            if(dist<best){best=dist;pick=new int[]{x,z};}
                        }
                        if(pick==null)throw new AssertionError("No tree-free ground camera within 40 m of the site");
                    }else if(!clear(g,d,random,pick[0],pick[1])){
                        // A stand that now exists may occupy the recorded stance; move to the nearest clear block, never farther than 12 m.
                        int[] moved=null;double best=Double.MAX_VALUE;
                        for(int z=pick[1]-12;z<=pick[1]+12;z++)for(int x=pick[0]-12;x<=pick[0]+12;x++){
                            double dist=Math.hypot(x-pick[0],z-pick[1]);if(dist>12||dist>=best||!clear(g,d,random,x,z))continue;best=dist;moved=new int[]{x,z};
                        }
                        if(moved==null)throw new AssertionError("No clear ground camera within 12 m of the reference pose");
                        relocatedFrom=pick;pick=moved;
                    }
                    cameraX=pick[0];cameraZ=pick[1];cameraY=d.sample(cameraX,cameraZ).height()+1;
                }else cameraY=Math.max(ey+38,d.sample(cameraX,cameraZ).height()+38);
                camera=new BlockPos(cameraX,cameraY,cameraZ);
                var aim=new BlockPos(ex+(ground?12:0),ey+(ground?3:0),ez+(ground?12:0));
                subject=new BoundingBox(ex-40,ey-16,ez-40,ex+40,Math.max(ey+48,64),ez+40);
                double cx=camera.getX()+.5,cy=camera.getY()+.2,cz=camera.getZ()+.5,dx=aim.getX()+.5-cx,dz=aim.getZ()+.5-cz,dy=aim.getY()+.5-(cy+player.getEyeHeight());
                double yaw=Math.toDegrees(Math.atan2(-dx,dz)),pitch=Math.toDegrees(Math.atan2(-dy,Math.hypot(dx,dz)));
                expectedYaw=(float)yaw;expectedPitch=(float)pitch;
                commands.performPrefixedCommand(source,"tp "+player.getName().getString()+" "+cx+" "+cy+" "+cz+" "+yaw+" "+pitch);
                records.add(Map.of("view",view,"camera",List.of(cx,cy,cz),"yaw",yaw,"pitch",pitch,"nightVision",false,"target",List.of(ex,ey,ez),"seed",level.getSeed(),"biome",level.getBiome(edge).unwrapKey().orElseThrow().identifier().toString(),"relocatedFromReferenceXZ",relocatedFrom==null?List.of():List.of(relocatedFrom[0],relocatedFrom[1])));
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
                    if(REOPEN)client.createWorldOpenFlows().openWorld("BorealSlopeVisualReview",()->client.gui.setScreen(new TitleScreen()));
                    else CreateWorldScreen.openFresh(client,()->client.gui.setScreen(new TitleScreen()));
                    return;
                }
                if(!configured&&screen instanceof CreateWorldScreen create) {
                    var state=create.getUiState();
                    state.setName("BorealSlopeVisualReview");
                    state.setSeed("1");
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
                String file=(REOPEN?"reopen-":"")+"slope-"+view+".png";
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
                Blockbranch.LOGGER.error("BOREAL_SLOPE_VISUAL_FAIL",t);
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
