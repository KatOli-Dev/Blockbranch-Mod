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
/** Actual generated mansion-wing views in an opt-in disposable UI-created world. */
public final class MansionVisualReview {
    private static final Path DIR=Path.of(System.getProperty("blockbranch.mansionVisualDirectory"));
    private static final boolean REOPEN=Boolean.getBoolean("blockbranch.mansionVisualReopen");
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
    private static final List<Object> records=new ArrayList<>();
    private static final int VIEWS=8;
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
                if(layout==null) {
                    var g=(RealisticGenerator)level.getChunkSource().getGenerator();
                    var plan=g.reimagined(level.getChunkSource().randomState()).ordered(4,BlockPos.ZERO,6000000).stream().limit(12).map(ReimaginedSites.Region::plan).flatMap(Optional::stream).findFirst().orElseThrow();
                    var key=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(ReimaginedStructures.key(4));
                    start=level.structureManager().getStartForStructure(key.value(),level.getChunk(plan.x()>>4,plan.z()>>4));
                    if(start==null||!start.isValid())throw new AssertionError("Native mansion start");
                    layout=(ReimaginedMansionPiece)start.getPieces().stream().filter(p->p instanceof ReimaginedMansionPiece).findFirst().orElseThrow();
                    var b=start.getBoundingBox();
                    for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++)for(int x=b.minX()>>4;x<=b.maxX()>>4;x++)level.setChunkForced(x,z,true);
                    var context=net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level);
                    Files.writeString(DIR.resolve(REOPEN?"reopen-pieces.json":"pieces.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(start.getPieces().stream().map(p->p.createTag(context).toString()).toList()));
                    Files.writeString(DIR.resolve(REOPEN?"reopen-generator.json":"generator.json"),RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().toString());
                }
                if(view==VIEWS) {
                    Files.writeString(DIR.resolve(REOPEN?"reopen-qualification.json":"qualification.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(MansionWingQualification.verify(level,(RealisticGenerator)level.getChunkSource().getGenerator(),layout)));
                    MansionWingQualification.savedEdits(level,layout,DIR,REOPEN);
                    String hash=hash(level,layout.getBoundingBox());
                    var witness=DIR.resolve("architecture.sha256");
                    if(REOPEN&&!Files.readString(witness).equals(hash))throw new AssertionError("Exact reopened architecture");
                    if(!REOPEN)Files.writeString(witness,hash);
                    Files.writeString(DIR.resolve(REOPEN?"reopen-result.txt":"result.txt"),"PASS actual native mansion; eight settled views; "+(REOPEN?"exact architecture on reopen":"fresh saved architecture"));
                    finished=true;
                    return;
                }
                var commands=server.getCommands();
                var source=server.createCommandSourceStack();
                commands.performPrefixedCommand(source,"time set 6000");
                commands.performPrefixedCommand(source,"weather clear");
                commands.performPrefixedCommand(source,"gamemode creative "+player.getName().getString());
                commands.performPrefixedCommand(source,"effect clear "+player.getName().getString());
                player.getAbilities().flying=true;
                player.onUpdateAbilities();
                var tag=layout.createTag(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(level));
                var core=tag.getIntArray("Core").orElseThrow();int floor=tag.getIntOr("Floor",0);
                int wx=core[0]-20,wz=core[2]+8,ax=core[3]+12,az=core[5]-28;
                int[][] cameras={{wx-7,floor+12,wz-7},{wx-7,floor+2,wz+11},{ax+15,floor+12,az-7},{ax+15,floor+2,az+11},{(core[0]+core[3])/2-55,floor+50,(core[2]+core[5])/2-65},{wx+4,floor+2,wz+18},{wx-6,MansionWingQualification.ground(level,wx-6,wz+2)+1,wz+2},{ax+14,MansionWingQualification.ground(level,ax+14,az+2)+1,az+2}};
                int[][] aims={{wx+4,floor+5,wz+10},{wx,floor+3,wz+11},{ax+4,floor+5,az+10},{ax+8,floor+3,az+11},{(core[0]+core[3])/2,floor+10,(core[2]+core[5])/2},{wx+1,floor+2,wz+8},{wx-1,floor+1,wz+5},{ax+9,floor+1,az+5}};
                camera=new BlockPos(cameras[view][0],cameras[view][1],cameras[view][2]);
                var aim=new BlockPos(aims[view][0],aims[view][1],aims[view][2]);
                subject=view<2||view==5||view==6?new BoundingBox(wx-1,floor-12,wz-1,wx+9,floor+12,wz+23):view<4||view==7?new BoundingBox(ax-1,floor-12,az-1,ax+9,floor+12,az+21):layout.getBoundingBox();
                double cx=camera.getX()+.5,cy=camera.getY()+.2,cz=camera.getZ()+.5,dx=aim.getX()+.5-cx,dz=aim.getZ()+.5-cz,dy=aim.getY()+.5-(cy+player.getEyeHeight());
                double yaw=Math.toDegrees(Math.atan2(-dx,dz)),pitch=Math.toDegrees(Math.atan2(-dy,Math.hypot(dx,dz)));
                expectedYaw=(float)yaw;expectedPitch=(float)pitch;
                commands.performPrefixedCommand(source,"tp "+player.getName().getString()+" "+cx+" "+cy+" "+cz+" "+yaw+" "+pitch);
                records.add(Map.of("view",view,"camera",List.of(cx,cy,cz),"yaw",yaw,"pitch",pitch,"nightVision",false));
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
                    if(REOPEN)client.createWorldOpenFlows().openWorld("MansionVisualReview",()->client.gui.setScreen(new TitleScreen()));
                    else CreateWorldScreen.openFresh(client,()->client.gui.setScreen(new TitleScreen()));
                    return;
                }
                if(!configured&&screen instanceof CreateWorldScreen create) {
                    var state=create.getUiState();
                    state.setName("MansionVisualReview");
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
                var b=subject;
                boolean complete=true;
                var cache=new net.minecraft.client.renderer.chunk.RenderRegionCache();
                int submitted=0;
                for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++)for(int x=b.minX()>>4;x<=b.maxX()>>4;x++)for(int y=b.minY()>>4;y<=b.maxY()>>4;y++) {
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
        for(var p:BlockPos.betweenClosed(b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ()))digest.update(MansionWingQualification.architecturalState(level.getBlockState(p)).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
