package blockbranch.client.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.SparseColumn;
import blockbranch.realistic.RealisticGenerator;
import blockbranch.realistic.UndergroundSmoke;
import blockbranch.realistic.UndergroundStructure;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.*;

/** Passive normal-render observer. Never submits meshes or alters render/cache state. */
public final class UndergroundAutomaticRenderSmoke {
    private static final String WORLD="UndergroundAutomaticTest";
    private static final long SEED=Long.getLong("blockbranch.undergroundAutomaticSeed",424242L);
    private static final boolean QUALIFY=Boolean.getBoolean("blockbranch.undergroundQualification");
    private static final boolean RELOAD=Boolean.getBoolean("blockbranch.undergroundAutomaticReload");
    private static volatile int request=-1,applied=-2;
    private static volatile BlockPos camera,witness,surface;
    private static volatile Throwable failure;
    private static volatile List<BlockPos> route;
    private static boolean opened,configured,busy,done;
    private static int stage,ticks,good,stages,poseTicks;
    private static long stageStart;
    private static final List<Map<String,Object>> observations=new ArrayList<>();
    private static Path directory(){return Path.of(System.getProperty("blockbranch.undergroundAutomaticResult")).getParent();}
    public static void register(){
        long deadline=System.nanoTime()+600_000_000_000L;
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(done||server.isDedicatedServer()||failure!=null||request<0||request==applied)return;
            try{
                var level=server.overworld();var players=server.getPlayerList().getPlayers();if(players.isEmpty())return;
                var player=players.getFirst();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
                require(g.undergroundEnabled(),"Actual saved generator enables caves");
                if(route==null){
                    require(level.getSeed()==SEED,"Actual configured world seed");
                    surface=player.blockPosition();
                    if(RELOAD&&QUALIFY){
                        var saved=com.google.gson.JsonParser.parseString(Files.readString(directory().resolve("automatic-fresh.json"))).getAsJsonObject();
                        require(saved.get("seed").getAsLong()==SEED,"Saved surface anchor belongs to configured seed");
                        var xyz=saved.get("surface").getAsString().split(", ");surface=new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));
                    }
                    witness=UndergroundSmoke.fixture(g,level.getChunkSource().randomState());
                    route=UndergroundSmoke.automaticRoute(g,level.getChunkSource().randomState(),witness);
                    stages=route.size()+2+(QUALIFY?2:0);
                    if(RELOAD)require(level.getBlockState(witness.below()).is(Blocks.COBBLESTONE),"Saved edit restored before automatic reload traversal");
                    else {var base=((SparseColumn)level.getChunkAt(witness)).blockbranch$terrain().base();require(level.getBlockState(witness.below())==base.state(witness.getX(),witness.getY()-1,witness.getZ()),"Fresh generated cave floor unedited");}
                }
                int caveStage=QUALIFY?request-1:request;
                boolean atSurface=QUALIFY&&(request==0||request==stages-1);
                camera=atSurface?surface:route.get(Math.max(0,Math.min(route.size()-1,caveStage-1)));
                if(request==editStage()){
                    camera=witness;
                    level.setBlock(witness.below(),Blocks.COBBLESTONE.defaultBlockState(),3);
                    var chunk=level.getChunkAt(witness);((SparseColumn)chunk).blockbranch$terrain().flush();chunk.markUnsaved();
                }
                require(level.getBlockState(camera).getCollisionShape(level,camera).isEmpty()&&level.getBlockState(camera.above()).getCollisionShape(level,camera.above()).isEmpty(),"Bounded camera stays in unobstructed actual native space");
                if(!atSurface)require(level.getBlockState(camera).isAir()&&level.getBlockState(camera.above()).isAir(),"Cave waypoints remain native air");
                String name=player.getName().getString();var source=server.createCommandSourceStack();
                server.getCommands().performPrefixedCommand(source,"gamemode creative "+name);
                player.getAbilities().flying=true;player.onUpdateAbilities();
                int yaw=atSurface?0:caveStage>route.size()/2?180:0,pitch=atSurface?20:request==editStage()?55:5;
                server.getCommands().performPrefixedCommand(source,"tp "+name+" "+(camera.getX()+.5)+" "+camera.getY()+" "+(camera.getZ()+.5)+" "+yaw+" "+pitch);
                server.getCommands().performPrefixedCommand(source,(atSurface||caveStage==0)?"effect clear "+name+" minecraft:night_vision":"effect give "+name+" minecraft:night_vision 999999 0 true");
                Blockbranch.LOGGER.info("UNDERGROUND_AUTOMATIC_CAMERA mode={} stage={}/{} camera={} yaw={} pitch={} creativeFlight=true",RELOAD?"RELOAD":"NEW",request,stages,camera,yaw,pitch);applied=request;
            }catch(Throwable error){failure=error;}
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(done)return;
            try{
                if(failure!=null)throw new AssertionError("Automatic cave fixture server",failure);
                require(System.nanoTime()<deadline,"Automatic cave fixture timeout");
                var screen=client.gui.screen();String name=screen==null?"":screen.getClass().getSimpleName();
                if(name.equals("AccessibilityOnboardingScreen")){press(screen,"Continue");return;}
                if(!opened&&screen instanceof TitleScreen){
                    opened=true;
                    if(RELOAD){require(Files.exists(client.gameDirectory.toPath().resolve("saves/"+WORLD+"/level.dat")),"Actual saved world exists");client.createWorldOpenFlows().openWorld(WORLD,()->client.gui.setScreen(new TitleScreen()));}
                    else{require(!Files.exists(client.gameDirectory.toPath().resolve("saves/"+WORLD)),"Fresh native Create World path");CreateWorldScreen.openFresh(client,()->client.gui.setScreen(new TitleScreen()));}return;
                }
                if(!RELOAD&&!configured&&screen instanceof CreateWorldScreen create){
                    var state=create.getUiState();state.setName(WORLD);state.setSeed(Long.toString(SEED));state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);state.setAllowCommands(true);
                    state.setWorldType(state.getNormalPresetList().stream().filter(e->e.preset().unwrapKey().orElseThrow().identifier().equals(Blockbranch.id("realistic"))).findFirst().orElseThrow());
                    configured=true;require(press(create,"Create New World"),"Native create button");return;
                }
                if(screen!=null&&(name.equals("ConfirmExperimentalFeaturesScreen")||name.equals("BackupConfirmScreen"))){press(screen,"Proceed");return;}
                if(client.level==null||client.player==null)return;
                require(client.hasSingleplayerServer()&&client.getConnection().getConnection().isMemoryConnection(),"Actual integrated normal client connection");
                if(request<0){stageStart=System.nanoTime();request=0;return;}
                if(busy||camera==null||applied!=stage)return;
                if(client.player.position().distanceToSqr(camera.getX()+.5,camera.getY(),camera.getZ()+.5)>.02){
                    if(++poseTicks%100==0)Blockbranch.LOGGER.info("UNDERGROUND_AUTOMATIC_POSE_WAIT stage={} requested={} actual={} flying={} onGround={} velocity={}",stage,camera,client.player.position(),client.player.getAbilities().flying,client.player.onGround(),client.player.getDeltaMovement());
                    require(System.nanoTime()-stageStart<(QUALIFY?90_000_000_000L:35_000_000_000L),"Camera pose prerequisite timed out: requested="+camera+" actual="+client.player.position());return;
                }
                poseTicks=0;
                if(++ticks%10!=0)return;
                boolean surfacePhase=QUALIFY&&(stage==0||stage==stages-1);
                int sky=client.level.getBrightness(net.minecraft.world.level.LightLayer.SKY,camera.above(2));
                var audit=new LinkedHashMap<String,Object>(audit(client));audit.put("skyLight",sky);
                boolean pass=((Number)audit.get("missing")).intValue()==0&&((Number)audit.get("hits")).intValue()>40&&(surfacePhase?sky>0:sky==0);
                if(ticks%100==0)Blockbranch.LOGGER.info("UNDERGROUND_AUTOMATIC_AUDIT stage={} seconds={} audit={}",stage,(System.nanoTime()-stageStart)/1e9,audit);
                if(!pass){good=0;if(System.nanoTime()-stageStart>(QUALIFY?90_000_000_000L:35_000_000_000L)){
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,"automatic-failure-stage-"+stage+".png",client.gameRenderer.mainRenderTarget(),1,message->{});
                    throw new AssertionError("Automatic visible cave faces failed to become compiled and visible: stage="+stage+" "+audit);
                }return;}
                if(++good<2)return;
                if(surfacePhase)require(client.level.getBrightness(net.minecraft.world.level.LightLayer.SKY,camera.above(2))>0,"Surface skylight restored across vertical-window travel");
                else {require(client.level.getBlockState(camera).isAir(),"Client camera remains inside generated air");require(client.level.getBrightness(net.minecraft.world.level.LightLayer.SKY,camera.above(2))==0,"Deep cave remains sealed from skylight");}
                if(stage==(QUALIFY?1:0)&&RELOAD)require(client.level.getBlockState(witness.below()).is(Blocks.COBBLESTONE),"Client receives saved edited cave floor");
                if(stage==editStage())require(client.level.getBlockState(witness.below()).is(Blocks.COBBLESTONE),"Client sees ordinary block edit before native save");
                var item=new LinkedHashMap<String,Object>();item.put("stage",stage);item.put("phase",surfacePhase?(stage==0?"surface-entry":"surface-return"):stage==editStage()?"floor-edit":"cave");item.put("windowMin",client.levelRenderer.viewArea().minSectionY());item.put("windowMax",client.levelRenderer.viewArea().maxSectionY());item.put("camera",camera.toShortString());item.put("secondsToReady",(System.nanoTime()-stageStart)/1e9);item.putAll(audit);observations.add(item);
                Blockbranch.LOGGER.info("UNDERGROUND_AUTOMATIC_STAGE_PASS {}",item);
                boolean capture=stage==0||stage==1||stage==2||stage==editStage()||stage==stages-1;
                if(capture){
                    busy=true;String file="automatic-"+(RELOAD?"reload":"fresh")+"-stage-"+stage+".png";
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{busy=false;advance(client);}));
                }else advance(client);
            }catch(Throwable error){fail(client,error);}
        });
    }
    /** Read-only ray hits identify faces actually visible from the player eye.
     * Buried/occluded sections need not have a mesh. */
    static Map<String,Object> audit(Minecraft client){
        int hits=0,missing=0;var reasons=new LinkedHashSet<String>();var nodes=new HashSet<Long>();
        var area=client.levelRenderer.viewArea();if(area==null)return Map.of("hits",0,"missing",1,"reasons",List.of("no view area"));
        var visible=new HashSet<Long>();for(var s:client.levelRenderer.visibleSections())visible.add(s.getSectionNode());
        var cameraState=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        if(!cameraState.initialized)return Map.of("hits",0,"missing",1,"reasons",List.of("camera not initialized"));
        var from=cameraState.pos;
        var samples=new ArrayList<double[]>();
        for(int iy=-5;iy<=5;iy++)for(int ix=-7;ix<=7;ix++)samples.add(new double[]{ix/7.0*.72,iy/5.0*.72,0,0});
        // These pixels cover the distant bright patch in the initial view.
        // Projection and native distance/window rules distinguish clipping from
        // a missing mesh for an actual in-range first-hit surface.
        for(int py:new int[]{279,282,285})for(int px:new int[]{483,487,491})samples.add(new double[]{2*(px+.5)/1024-1,1-2*(py+.5)/576,px,py});
        if(Boolean.getBoolean("blockbranch.caveApproachClientSmoke"))for(int py:new int[]{226,236,246})for(int px:new int[]{500,510,520})
            samples.add(new double[]{2*(px+.5)/1024-1,1-2*(py+.5)/576,px,py});
        var probes=new ArrayList<Map<String,Object>>();
        for(var sample:samples){
            var projected=new org.joml.Vector3f((float)(sample[0]/cameraState.projectionMatrix.m00()),(float)(sample[1]/cameraState.projectionMatrix.m11()),-1);
            new org.joml.Matrix4f(cameraState.viewRotationMatrix).invert().transformDirection(projected);projected.normalize();
            var dir=new Vec3(projected.x(),projected.y(),projected.z());
            double maxDistance=(area.getViewDistance()+2)*16.0;
            for(double d=0;d<=(area.getViewDistance()+2)*16.0;d+=.5){
                var q=BlockPos.containing(from.add(dir.scale(d)));
                if((q.getY()>>4)<area.minSectionY()||(q.getY()>>4)>area.maxSectionY()
                    ||!net.minecraft.server.level.ChunkTrackingView.isInViewDistance(cameraState.blockPos.getX()>>4,cameraState.blockPos.getZ()>>4,area.getViewDistance(),q.getX()>>4,q.getZ()>>4)){
                    maxDistance=Math.max(0,d-.51);break;
                }
            }
            var end=from.add(dir.scale(maxDistance));var hit=client.level.clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,client.player));
            double distance=hit.getType()==HitResult.Type.BLOCK?from.distanceTo(hit.getLocation()):maxDistance;
            boolean loaded=true;
            for(double d=0;d<=distance;d+=2){var q=BlockPos.containing(from.add(dir.scale(d)));var chunk=client.level.getChunkSource().getChunk(q.getX()>>4,q.getZ()>>4,ChunkStatus.FULL,false);if(chunk==null||((SparseColumn)chunk).blockbranch$terrain()==null){loaded=false;break;}}
            if(!loaded){missing++;reasons.add("profile missing on ray");continue;}
            if(sample[2]!=0)probes.add(Map.of("pixel",(int)sample[2]+","+(int)sample[3],"boundDistance",maxDistance,"hitDistance",distance,"type",hit.getType().toString(),"block",hit.getBlockPos().toShortString()));
            if(hit.getType()!=HitResult.Type.BLOCK)continue;
            hits++;var q=hit.getBlockPos();long node=SectionPos.asLong(q);nodes.add(node);
            var mesh=area.getRenderSectionAt(q);
            if(mesh==null||mesh.getSectionNode()!=node){missing++;reasons.add("slot mismatch "+SectionPos.of(q));}
            else if(!mesh.getSectionMesh().hasRenderableLayers()){
                missing++;reasons.add("no mesh "+SectionPos.of(q)+" graphNode="+(client.levelRenderer.sectionOcclusionGraph().getNode(mesh)!=null));
            }else if(!visible.contains(node)){missing++;reasons.add("not visible "+SectionPos.of(q));}
        }
        return Map.of("hits",hits,"missing",missing,"requiredSections",nodes.size(),"visibleSections",visible.size(),"reasons",reasons.stream().limit(8).toList(),"distantPixelProbes",probes);
    }
    private static void advance(Minecraft client){
        try{
            if(stage==stages-1){
                var report=new LinkedHashMap<String,Object>();report.put("mode",RELOAD?"RELOAD":"NEW");report.put("seed",SEED);report.put("renderDistance",client.options.getEffectiveRenderDistance());report.put("qualification",QUALIFY);report.put("surface",surface.toShortString());report.put("normalWorldUI",true);report.put("explicitMeshSubmissions",0);report.put("renderCacheMutations",0);report.put("route",route.stream().map(BlockPos::toShortString).toList());report.put("observations",observations);
                Files.writeString(directory().resolve("automatic-"+(RELOAD?"reload":"fresh")+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
                client.disconnect(new TitleScreen(),false);Files.writeString(Path.of(System.getProperty("blockbranch.undergroundAutomaticResult")),"PASS "+(RELOAD?"RELOAD":"NEW")+" automatic native cave rendering; visible ray-hit faces; no mesh/cache intervention\n");done=true;client.stop();
            }else{stage++;good=0;ticks=0;stageStart=System.nanoTime();request=stage;}
        }catch(Throwable error){fail(client,error);}
    }
    private static void fail(Minecraft client,Throwable error){done=true;Blockbranch.LOGGER.error("UNDERGROUND_AUTOMATIC_FAIL",error);try{Files.writeString(directory().resolve("automatic-failure.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("stage",stage,"error",error.toString(),"observations",observations))+"\n");Files.writeString(Path.of(System.getProperty("blockbranch.undergroundAutomaticResult")),"FAIL "+error+"\n");}catch(Exception ignored){}client.stop();}
    private static int editStage(){return stages-(QUALIFY?2:1);}
    private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static boolean press(net.minecraft.client.gui.screens.Screen screen,String label){for(var child:screen.children())if(child instanceof Button b&&b.active&&b.getMessage().getString().equals(label)){b.onPress(new KeyEvent(257,0,0));return true;}return false;}
    private UndergroundAutomaticRenderSmoke(){}
}
