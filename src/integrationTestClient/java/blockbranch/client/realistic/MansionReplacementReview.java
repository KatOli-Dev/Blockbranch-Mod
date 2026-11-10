package blockbranch.client.realistic;
import blockbranch.Blockbranch;
import blockbranch.realistic.*;
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
/** Actual generated crypt views in an opt-in disposable UI-created world. */
public final class MansionReplacementReview {
    private static final Path DIR=Path.of(System.getProperty("blockbranch.mansionReplacementDirectory"));
    private static final boolean REOPEN=Boolean.getBoolean("blockbranch.mansionReplacementReopen");
    private static final Set<Integer> SELECTED=Arrays.stream(System.getProperty("blockbranch.mansionReplacementViews","0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15").split(",")).map(String::trim).map(Integer::valueOf).collect(java.util.stream.Collectors.toSet());
    private static final long BEGAN=System.nanoTime();
    private static boolean opened,configured,saving;
    private static volatile boolean finished;
    private static volatile boolean posed;
    private static volatile int view;
    private static volatile Throwable failure;
    private static ReimaginedMansionPiece layout;
    private static StructureStart start;
    private static int settled;
    private static BlockPos camera;
    private static double expectedYaw,expectedPitch;
    private static final List<Object> records=new ArrayList<>();
    private static final int VIEWS=16;
    private static int finishTicks;
    private static boolean initialized;private static int warmTicks;private static int[][] cameras,aims;private static boolean[] night;
    private static void preparePoses(net.minecraft.nbt.CompoundTag tag)throws Exception {
        int[] c=tag.getIntArray("Core").orElseThrow();int floor=tag.getIntOr("Floor",0),x0=c[0]-10,x1=c[3]+10,z0=c[2]-10,z1=c[5]+10,mx=(x0+x1)/2;
        cameras=new int[][]{{mx-26,floor+11,z0-32},{mx,floor+1,z0-14},{x0-17,floor+22,z0-18},{x0+1,floor+9,z0+12},{x0+4,floor+1,z0+2},{x0-1,floor+10,z0-1},{x0+1,floor+11,z0+1},{x1+24,floor+24,z1+26},{},{},{},{},{},{},{},{}};
        aims=new int[][]{{mx,floor+4,z0},{mx,floor+3,z0},{x0,floor+15,z0},{x0+1,floor+10,z0+1},{x0+4,floor+5,z0+9},{x0-2,floor+12,z0-2},{x0-2,floor+16,z0-2},{x1,floor+10,z1},{},{},{},{},{},{},{},{}};night=new boolean[VIEWS];
        String file=System.getProperty("blockbranch.mansionReplacementPoseFile","");
        if(!file.isEmpty())for(var v:com.google.gson.JsonParser.parseString(Files.readString(Path.of(file))).getAsJsonArray()){var o=v.getAsJsonObject();int i=o.get("view").getAsInt();if(i<8||i>=VIEWS)throw new AssertionError("Only eight additional fixed mansion poses");cameras[i]=o.getAsJsonArray("camera").asList().stream().mapToInt(e->e.getAsInt()).toArray();aims[i]=o.getAsJsonArray("aim").asList().stream().mapToInt(e->e.getAsInt()).toArray();night[i]=o.get("nightVision").getAsBoolean();}
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server-> {
            if(finished||failure!=null||server.getPlayerList().getPlayers().isEmpty())return;
            if(posed){var p=server.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=true;p.onUpdateAbilities();p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);if(p.position().distanceToSqr(new net.minecraft.world.phys.Vec3(camera.getX()+.5,camera.getY()+.2,camera.getZ()+.5))>.001)server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+p.getName().getString()+" "+(camera.getX()+.5)+" "+(camera.getY()+.2)+" "+(camera.getZ()+.5)+" "+expectedYaw+" "+expectedPitch);return;}
            try {
                var level=server.overworld();
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,server);
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
                level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,false,server);
                var player=server.getPlayerList().getPlayers().getFirst();
                if(layout==null) {
                    // Preserve native occupied-mansion residents; assessment never changes blocks or containers.
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"difficulty normal");
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();
                    var plan=g.reimagined(level.getChunkSource().randomState()).ordered(4,BlockPos.ZERO,6000000).stream().limit(12).map(ReimaginedSites.Region::plan).flatMap(Optional::stream).findFirst().orElseThrow();
                    var key=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(ReimaginedStructures.key(4));
                    start=level.structureManager().getStartForStructure(key.value(),level.getChunk(plan.x()>>4,plan.z()>>4));
                    if(start==null||!start.isValid())throw new AssertionError("Native crypt start");
                    layout=(ReimaginedMansionPiece)start.getPieces().stream().filter(p->p instanceof ReimaginedMansionPiece).findFirst().orElseThrow();
                    var b=start.getBoundingBox();
                    for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++)for(int x=b.minX()>>4;x<=b.maxX()>>4;x++)level.setChunkForced(x,z,true);

                    preparePoses(layout.createTag(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level)));

                    var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
                    Files.writeString(DIR.resolve(REOPEN?"reopen-pieces.json":"pieces.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(start.getPieces().stream().map(p->p.createTag(context).toString()).toList()));
                    Files.writeString(DIR.resolve(REOPEN?"reopen-generator.json":"generator.json"),RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().toString());
                }
                if(!initialized){
                    // Tick the fixed photographic site before comparing scheduled leaf distances.
                    player.getAbilities().flying=true;player.onUpdateAbilities();int warm=Math.min(15,warmTicks/10);var p=cameras[warm];
                    if(warmTicks%10==0)server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" "+(p[0]+.5)+" "+(p[1]+.2)+" "+(p[2]+.5));
                    if(++warmTicks<160)return;
                    MansionReplacementExport.write(level,layout,start,DIR,REOPEN);
                    MansionReplacementQualification.preflight(level,layout,start,DIR,REOPEN);
                    var cameraAudit=new ArrayList<Object>();for(int fixed=0;fixed<VIEWS;fixed++){var at=cameras[fixed];double cx=at[0]+.5,cy=at[1]+.2,cz=at[2]+.5;var body=new net.minecraft.world.phys.AABB(cx-.3,cy,cz-.3,cx+.3,cy+player.getBbHeight(),cz+.3);boolean clear=level.getBlockState(new BlockPos(at[0],at[1],at[2])).getFluidState().isEmpty()&&level.getBlockState(BlockPos.containing(cx,cy+player.getEyeHeight(),cz)).getFluidState().isEmpty()&&!level.getBlockCollisions(null,body).iterator().hasNext();cameraAudit.add(Map.of("view",fixed,"camera",List.of(cx,cy,cz),"bodyAABB",body.toString(),"blockAndFluidClear",clear));Files.writeString(DIR.resolve(REOPEN?"reopen-preflight-camera-volumes.json":"preflight-camera-volumes.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(cameraAudit));if(!clear)throw new AssertionError("Preflight frozen native camera body/eye "+fixed+" "+body);}
                    initialized=true;
                }
                while(view<VIEWS&&!SELECTED.contains(view))view++;
                if(view==VIEWS) {
                    if(++finishTicks<80)return;
                    var qualified=MansionReplacementQualification.tick(level,layout,start,DIR,REOPEN);if(qualified==null)return;
                    Files.writeString(DIR.resolve(REOPEN?"reopen-qualification.json":"qualification.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(qualified));
                    String hash=hash(level,layout.getBoundingBox());
                    var witness=DIR.resolve("architecture.sha256");
                    if(REOPEN&&!Files.readString(witness).equals(hash))throw new AssertionError("Exact reopened architecture");
                    if(!REOPEN)Files.writeString(witness,hash);
                    Files.writeString(DIR.resolve(REOPEN?"reopen-result.txt":"result.txt"),"PASS actual native mansion replacement; "+records.size()+" settled views; "+(REOPEN?"exact architecture on reopen":"fresh saved architecture"));
                    finished=true;
                    return;
                }
                var commands=server.getCommands();
                var source=server.createCommandSourceStack();
                commands.performPrefixedCommand(source,"difficulty normal");
                commands.performPrefixedCommand(source,"time set 6000");
                commands.performPrefixedCommand(source,"weather clear");
                commands.performPrefixedCommand(source,"gamemode creative "+player.getName().getString());
                commands.performPrefixedCommand(source,"effect clear "+player.getName().getString());
                if(night[view])commands.performPrefixedCommand(source,"effect give "+player.getName().getString()+" minecraft:night_vision 9999 0 true");
                player.getAbilities().flying=true;
                player.onUpdateAbilities();
                if(cameras[view].length!=3||aims[view].length!=3)throw new AssertionError("Freeze additional poses from authentic anatomy before capture");
                camera=new BlockPos(cameras[view][0],cameras[view][1],cameras[view][2]);
                var aim=new BlockPos(aims[view][0],aims[view][1],aims[view][2]);
                double cx=camera.getX()+.5,cy=camera.getY()+.2,cz=camera.getZ()+.5,dx=aim.getX()+.5-cx,dz=aim.getZ()+.5-cz,dy=aim.getY()+.5-(cy+player.getEyeHeight());
                double yaw=Math.toDegrees(Math.atan2(-dx,dz)),pitch=Math.toDegrees(Math.atan2(-dy,Math.hypot(dx,dz)));
                var body=new net.minecraft.world.phys.AABB(cx-.3,cy,cz-.3,cx+.3,cy+player.getBbHeight(),cz+.3);
                if(!level.getBlockState(camera).getFluidState().isEmpty()||!level.getBlockState(BlockPos.containing(cx,cy+player.getEyeHeight(),cz)).getFluidState().isEmpty()||level.getBlockCollisions(null,body).iterator().hasNext())throw new AssertionError("Clear actual native camera body and eye "+view);

                expectedYaw=yaw;expectedPitch=pitch;
                commands.performPrefixedCommand(source,"tp "+player.getName().getString()+" "+cx+" "+cy+" "+cz+" "+yaw+" "+pitch);
                records.add(Map.of("view",view,"camera",List.of(cx,cy,cz),"yaw",yaw,"pitch",pitch,"nightVision",night[view],"eyeState",level.getBlockState(BlockPos.containing(cx,cy+player.getEyeHeight(),cz)).toString(),"eyeHeight",player.getEyeHeight(),"feetState",level.getBlockState(camera).toString(),"cameraBodyAABB",body.toString(),"cameraBlockCollisionFree",true));
                Files.writeString(DIR.resolve(REOPEN?"reopen-views.json":"views.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(records));
                posed=true;
            }
            catch(Throwable t) {
                failure=t;
            }
        }
        );
        ClientTickEvents.END_CLIENT_TICK.register(client-> {
            try {
                if(System.nanoTime()-BEGAN>900_000_000_000L)throw new AssertionError("Bounded structure visual deadline");
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
                    if(REOPEN)client.createWorldOpenFlows().openWorld("MansionAssessmentReview",()->client.gui.setScreen(new TitleScreen()));
                    else CreateWorldScreen.openFresh(client,()->client.gui.setScreen(new TitleScreen()));
                    return;
                }
                if(!configured&&screen instanceof CreateWorldScreen create) {
                    var state=create.getUiState();
                    state.setName("MansionAssessmentReview");
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
                if(MansionReplacementQualification.active()){
                    boolean walk=MansionReplacementQualification.walking();client.options.keyUp.setDown(walk);client.options.keyJump.setDown(false);
                    if(walk&&client.player!=null){var goal=MansionReplacementQualification.target();double dx=goal.x-client.player.getX(),dz=goal.z-client.player.getZ();client.player.setYRot((float)Math.toDegrees(Math.atan2(-dx,dz)));client.player.setXRot(0);}return;
                }
                if(!posed||client.level==null||client.player==null)return;
                client.player.getAbilities().flying=true;client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                if(client.player.position().distanceToSqr(new net.minecraft.world.phys.Vec3(camera.getX()+.5,camera.getY()+.2,camera.getZ()+.5))>.001) {
                    settled=0;
                    return;
                }
                client.options.fov().set(65);
                client.gui.toastManager().clear();
                if(!client.gui.hud.isHidden())client.gui.hud.toggle();
                var area=client.levelRenderer.viewArea();
                if(area==null)return;
                var b=layout.getBoundingBox();
                boolean complete=true;
                var cache=new net.minecraft.client.renderer.chunk.RenderRegionCache();
                int submitted=0;
                for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++)for(int x=b.minX()>>4;x<=b.maxX()>>4;x++)for(int y=b.minY()>>4;y<=b.maxY()>>4;y++) {
                    var chunk=client.level.getChunkSource().getChunk(x,z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);
                    if(chunk==null||((blockbranch.cubic.SparseColumn)chunk).blockbranch$terrain()==null) {
                        complete=false;
                        continue;
                    }
                    var section=area.getRenderSectionAt(new BlockPos(x*16+8,y*16+8,z*16+8));
                    if(section==null||section.getSectionNode()!=net.minecraft.core.SectionPos.asLong(x,y,z)) {
                        complete=false;
                        continue;
                    }
                    if(!client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum.isVisible(new net.minecraft.world.phys.AABB(x*16,y*16,z*16,x*16+16,y*16+16,z*16+16)))continue;
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
                String file=(REOPEN?"reopen-":"")+"mansion-"+view+".png";
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
                Blockbranch.LOGGER.error("STRUCTURE_VISUAL_FAIL",t);
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
    private static String hash(net.minecraft.server.level.ServerLevel level,BoundingBox b)throws Exception {
        var digest=java.security.MessageDigest.getInstance("SHA-256");
        for(var p:BlockPos.betweenClosed(b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ()))digest.update(level.getBlockState(p).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }
    private static void press(net.minecraft.client.gui.screens.Screen screen,String label) {
        for(var v:screen.children())if(v instanceof Button button&&button.active&&button.getMessage().getString().equals(label)) {
            button.onPress(new KeyEvent(257,0,0));
            return;
        }
        throw new AssertionError("Missing UI button "+label);
    }
}
