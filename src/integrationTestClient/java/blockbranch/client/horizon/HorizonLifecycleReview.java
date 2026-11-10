package blockbranch.client.horizon;
import static blockbranch.client.horizon.HorizonTileChecks.*;
import static blockbranch.horizon.HorizonTiles.key;
import blockbranch.Blockbranch;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
/**
 * End-to-end horizon lifecycle in one disposable UI-created world (seed 424242), with the
 * camera held still 10 chunks west of a target chunk that only the horizon draws:
 * <ol>
 * <li>edit: the target's level-0 tile is cached and drawn, then a gold layer is placed in it
 *     (force-loaded, 80 ticks after it can tick, so its generation refresh has normally landed); the cached tile must be
 *     replaced by one whose every sample tops out in gold, and that tile's mesh drawn;
 * <li>dimension return: after the target has unloaded, a nether trip must clear the client's
 *     tiles and the return must draw gold again, now read from the saved chunk;
 * <li>edits while away: on a second nether trip the target is loaded, given an emerald layer
 *     above the gold and unloaded again; the return must draw emerald. The refill after a
 *     return reads so many chunks that the target's cached saved source is usually evicted,
 *     so this step alone does not detect a missing invalidation;
 * <li>stale saved source while stationary: the return refill can push the target out of the
 *     saved-source cache, so once the fill is done the client evicts the target's tile and asks
 *     again, caching the unloaded target. The target then gets a diamond layer and unloads, and
 *     a second eviction reads it once more: the tile must show diamond, not the cached emerald.
 *     (A neighbour edit cannot do this: force-loading a neighbour keeps the target loaded too.)
 * <li>edit without block ticking: with the chunk two east force-loaded, the target is loaded
 *     at the full status but does not tick blocks, where {@code Level.setBlock} sends no block
 *     update. A lapis layer placed there must still replace the drawn tile;
 * <li>reconnect: save and quit must clear client tiles and server queues, and reopening must
 *     draw lapis from disk.
 * </ol>
 * Writes {@code result.txt} and a {@code HORIZON_LIFECYCLE} log line per step.
 */
public final class HorizonLifecycleReview {
    private static final Path DIR=Path.of(System.getProperty("blockbranch.horizonLifecycleDirectory"));
    private static final String WORLD="HorizonLifecycleReview";
    private static final long BEGAN=System.nanoTime(),DEADLINE=1_200_000_000_000L,STEP_DEADLINE=240_000_000_000L;
    /** Target chunk distance east of the camera chunk: beyond the view distance plus the refresh margin. */
    private static final int OFFSET=10;
    private static volatile Throwable failure;
    /** Camera (overworld) and target chunk, chosen once on the first server. */
    private static volatile Vec3 camera;
    private static volatile int targetX,targetZ,goldY,emeraldY,diamondY,lapisY;
    /** Whether the server holds the player at the camera; off for nether trips. */
    private static volatile boolean hold;
    /** Server work requested by the client and the last one finished. */
    private static volatile String task="",doneTask="";
    private static int taskTicks;
    private static boolean opened,configured,finished;
    private static int phase;
    private static long phaseBegan=System.nanoTime(),lastLog;
    private static int[] baseline;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(HorizonLifecycleReview::serverTick);
        ClientTickEvents.END_CLIENT_TICK.register(HorizonLifecycleReview::tick);
    }

    private static void serverTick(MinecraftServer server) {
        if(finished||server.getPlayerList().getPlayers().isEmpty())return;
        var player=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();
        try {
            if(camera==null) {
                var rules=level.getGameRules();
                rules.set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,server);
                rules.set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
                rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,false,server);
                rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER,false,server);
                command(server,"time set noon");command(server,"weather clear");
                var spawn=player.blockPosition();
                int y=level.getChunk(spawn.getX()>>4,spawn.getZ()>>4).getHeight(Heightmap.Types.MOTION_BLOCKING,spawn.getX()&15,spawn.getZ()&15);
                targetX=(spawn.getX()>>4)+OFFSET;targetZ=spawn.getZ()>>4;
                camera=new Vec3(spawn.getX()+.5,y+30,spawn.getZ()+.5);hold=true;
                log("camera %s target chunk %d %d",camera,targetX,targetZ);
            }
            if(hold&&player.level().dimension()==Level.OVERWORLD) {
                player.getAbilities().flying=true;player.onUpdateAbilities();player.setDeltaMovement(Vec3.ZERO);
                if(player.position().distanceToSqr(camera)>.001||Math.abs(net.minecraft.util.Mth.wrapDegrees(player.getYRot()+90))>.02)
                    command(server,"tp "+player.getName().getString()+" "+camera.x+" "+camera.y+" "+camera.z+" -90 5");
            }
            if(!task.equals(doneTask))taskTick(server,level,player.getName().getString());
        } catch(Throwable t){failure=t;}
    }
    /**
     * One tick of the requested server work. Edits load the target with a force ticket, wait
     * until it ticks blocks (as where players build), then place the layer; unloads
     * drop the ticket and wait until the chunk is gone and a few change passes have run.
     */
    private static void taskTick(MinecraftServer server,ServerLevel level,String name) {
        taskTicks++;var chunks=level.getChunkSource();long pos=net.minecraft.world.level.ChunkPos.pack(targetX,targetZ);
        switch(task) {
            case "nether"->{command(server,"execute in minecraft:the_nether run tp "+name+" "+camera.x+" 128 "+camera.z);finish();}
            case "return"->{command(server,"execute in minecraft:overworld run tp "+name+" "+camera.x+" "+camera.y+" "+camera.z+" -90 5");finish();}
            case "gold","emerald","diamond"->{
                if(taskTicks==1)level.setChunkForced(targetX,targetZ,true);
                if(chunks.getChunkNow(targetX,targetZ)==null||!level.shouldTickBlocksAt(pos)){taskTicks=Math.min(taskTicks,2);return;}
                // Let the generation refresh land first, so only the edit can bring the new layer.
                if(task.equals("gold")&&taskTicks<80)return;
                var chunk=level.getChunk(targetX,targetZ);
                if(task.equals("gold")) {
                    int top=Integer.MIN_VALUE;
                    for(int i=0;i<256;i++)top=Math.max(top,chunk.getHeight(Heightmap.Types.MOTION_BLOCKING,i&15,i>>4));
                    goldY=top+8;emeraldY=goldY+4;diamondY=emeraldY+4;
                }
                int y=switch(task){case "gold"->goldY;case "emerald"->emeraldY;default->diamondY;};
                var state=layer(task);
                for(int i=0;i<256;i++)level.setBlock(new BlockPos(targetX*16+(i&15),y,targetZ*16+(i>>4)),state,3);
                log("placed %s layer at y=%d in chunk %d %d",task,y,targetX,targetZ);finish();
            }
            case "borderEdit"->{
                // Force-load the chunk two east of the target: the target is then loaded at the
                // full status only, so it does not tick blocks and Level.setBlock skips
                // sendBlockUpdated for it. Fail rather than test a target that ticks.
                int bx=targetX+2;
                if(taskTicks==1)level.setChunkForced(bx,targetZ,true);
                var target=chunks.getChunkNow(targetX,targetZ);
                if(target==null||!level.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.pack(bx,targetZ))){taskTicks=Math.min(taskTicks,2);return;}
                if(taskTicks<40)return;
                var before=target.getFullStatus();
                if(before!=net.minecraft.server.level.FullChunkStatus.FULL||level.shouldTickBlocksAt(pos))throw new AssertionError("Target is not loaded without block ticking: "+before);
                lapisY=diamondY+4;
                for(int i=0;i<256;i++)level.setBlock(new BlockPos(targetX*16+(i&15),lapisY,targetZ*16+(i>>4)),layer("lapis"),3);
                var after=target.getFullStatus();
                if(after!=net.minecraft.server.level.FullChunkStatus.FULL||level.shouldTickBlocksAt(pos))throw new AssertionError("Target started ticking during the edit: "+after);
                log("placed lapis layer at y=%d in chunk %d %d, status %s before and %s after, block ticking %s",lapisY,targetX,targetZ,before,after,level.shouldTickBlocksAt(pos));finish();
            }
            case "unloadBorder"->{
                if(taskTicks==1)level.setChunkForced(targetX+2,targetZ,false);
                if(chunks.getChunkNow(targetX,targetZ)!=null||chunks.getChunkNow(targetX+2,targetZ)!=null){taskTicks=Math.min(taskTicks,2);return;}
                if(taskTicks>=60){log("target and its neighbour unloaded");finish();}
            }
            case "unload"->{
                if(taskTicks==1)level.setChunkForced(targetX,targetZ,false);
                if(chunks.getChunkNow(targetX,targetZ)!=null){taskTicks=Math.min(taskTicks,2);return;}
                if(taskTicks>=60){log("target unloaded");finish();}
            }
            default->throw new AssertionError("Unknown server task "+task);
        }
    }
    private static void finish(){doneTask=task;}
    private static void request(String t){taskTicks=0;task=t;}

    private static void tick(Minecraft client) {
        if(finished)return;
        try {
            long now=System.nanoTime();
            if(now-BEGAN>DEADLINE)throw new AssertionError("Bounded lifecycle deadline; phase "+phase);
            if(now-phaseBegan>STEP_DEADLINE)throw new AssertionError("Phase "+phase+" deadline; tile "+describe(tile()));
            if(failure!=null)throw new AssertionError("Server driver",failure);
            var screen=client.gui.screen();String name=screen==null?"":screen.getClass().getSimpleName();
            if(name.equals("DisconnectedScreen"))throw new AssertionError("Disconnected");
            if(name.equals("AccessibilityOnboardingScreen")){press(screen,"Continue");return;}
            if(!opened&&screen instanceof TitleScreen) {
                if(Files.exists(client.gameDirectory.toPath().resolve("saves/"+WORLD)))throw new AssertionError("Lifecycle world must be fresh");
                opened=true;CreateWorldScreen.openFresh(client,()->client.gui.setScreen(new TitleScreen()));return;
            }
            if(!configured&&screen instanceof CreateWorldScreen create) {
                var state=create.getUiState();state.setName(WORLD);state.setSeed("424242");
                state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);state.setAllowCommands(true);
                state.setWorldType(state.getNormalPresetList().stream().filter(e->e.preset().unwrapKey().orElseThrow().identifier().toString().equals("blockbranch:realistic")).findFirst().orElseThrow());
                configured=true;press(create,"Create New World");return;
            }
            if(name.equals("ConfirmExperimentalFeaturesScreen")||name.equals("BackupConfirmScreen")){press(screen,"Proceed");return;}
            if(phase==11) {
                // Quit to the title: client tiles and server queues must be gone with the server.
                if(!(screen instanceof TitleScreen)||client.getSingleplayerServer()!=null)return;
                var stats=HorizonClient.stats();
                if(stats.tiles()!=0)throw new AssertionError("Client tiles left after quitting: "+stats);
                if(serverState("running")||serverQueues()!=0)throw new AssertionError("Server horizon state left after quitting: running="+serverState("running")+" queues="+serverQueues());
                log("quit: client %s, server stopped with no queues",stats);
                client.createWorldOpenFlows().openWorld(WORLD,()->client.gui.setScreen(new TitleScreen()));next();return;
            }
            if(client.level==null||client.player==null||camera==null)return;
            client.gui.toastManager().clear();
            boolean atCamera=client.level.dimension()==Level.OVERWORLD&&client.player.position().distanceToSqr(camera)<.001;
            if(now-lastLog>15_000_000_000L){lastLog=now;log("phase=%d task=%s done=%s camera=%s tile=%s stats=%s",phase,task,doneTask,atCamera,describe(tile()),HorizonClient.stats());}
            switch(phase) {
                case 0->{
                    // Baseline: the target's tile cached and drawn before its chunk exists.
                    var t=tile();
                    if(!atCamera||!settled(t))return;
                    baseline=t.data();
                    if(count(t.data(),Blocks.GOLD_BLOCK.defaultBlockState())!=0)throw new AssertionError("Gold before the edit");
                    log("baseline cached and drawn: %s",describe(t));request("gold");next();
                }
                case 1->{
                    // Stationary edit: the cached tile is replaced and its new mesh drawn.
                    if(!doneTask.equals("gold"))return;
                    var t=tile();
                    if(!atCamera)throw new AssertionError("Camera moved during the edit check");
                    if(!settled(t)||!uniform(t.data(),Blocks.GOLD_BLOCK.defaultBlockState(),goldY))return;
                    log("edit refresh drawn: %s",describe(t));request("unload");next();
                }
                case 2->{if(doneTask.equals("unload")){hold=false;request("nether");next();}}
                case 3,5->{
                    // Leaving the overworld releases every client tile.
                    if(!doneTask.equals("nether")||client.level.dimension()!=Level.NETHER)return;
                    if(HorizonClient.stats().tiles()!=0)return;
                    log("nether: client tiles cleared %s",HorizonClient.stats());
                    if(phase==3){hold=true;request("return");}else request("emerald");
                    next();
                }
                case 4,8->{
                    // Back in the overworld: tiles come again, from the saved target chunk.
                    if(!doneTask.equals("return")||!atCamera)return;
                    var t=tile();
                    if(!settled(t))return;
                    boolean emerald=phase==8;
                    if(!uniform(t.data(),(emerald?Blocks.EMERALD_BLOCK:Blocks.GOLD_BLOCK).defaultBlockState(),emerald?emeraldY:goldY)) {
                        // A drawn tile that is still gold after the emerald edit is a stale saved source; give it a few seconds to settle.
                        if(System.nanoTime()-phaseBegan>30_000_000_000L)throw new AssertionError("Return drew stale data: "+describe(t));
                        return;
                    }
                    log("returned: %s",describe(t));
                    if(phase==4){hold=false;request("nether");next();}
                    else{phase=20;phaseBegan=System.nanoTime();}
                }
                case 6->{
                    if(!doneTask.equals("emerald"))return;
                    if(HorizonClient.stats().tiles()!=0)throw new AssertionError("Tiles received in the nether");
                    request("unload");next();
                }
                case 7->{
                    if(!doneTask.equals("unload"))return;
                    if(HorizonClient.stats().tiles()!=0)throw new AssertionError("Tiles received in the nether");
                    hold=true;request("return");next();
                }
                case 20->{
                    // Stationary saved-source cache: once the fill is done little else is sampled, so
                    // the target's saved entry stays cached. Evicting its tile makes the client ask
                    // again, and the server reads the unloaded target, caching its emerald state.
                    var stats=HorizonClient.stats();
                    if(stats.missing()!=0||stats.meshing()!=0)return;
                    log("filled %s; evicting the target tile",stats);HorizonClient.drop(key(0,targetX,targetZ));next();
                }
                case 21->{
                    var t=tile();
                    if(!settled(t))return;
                    if(!uniform(t.data(),layer("emerald"),emeraldY))throw new AssertionError("Re-requested tile lost the emerald layer: "+describe(t));
                    log("re-requested: %s",describe(t));request("diamond");next();
                }
                case 22->{if(doneTask.equals("diamond")){request("unload");next();}}
                case 23->{
                    var t=tile();
                    if(!doneTask.equals("unload")||!settled(t)||!uniform(t.data(),layer("diamond"),diamondY))return;
                    log("diamond drawn after unload: %s; evicting the target tile",describe(t));HorizonClient.drop(key(0,targetX,targetZ));next();
                }
                case 24->{
                    // Requested again, the unloaded target must be read as diamond, not the cached emerald.
                    var t=tile();
                    if(!settled(t))return;
                    if(!uniform(t.data(),layer("diamond"),diamondY))throw new AssertionError("Stale saved source after eviction: "+describe(t));
                    log("re-requested: %s",describe(t));request("borderEdit");next();
                }
                case 25->{
                    // An edit in a loaded chunk that does not tick blocks must still refresh the drawn tile.
                    if(!doneTask.equals("borderEdit"))return;
                    var t=tile();
                    if(settled(t)&&uniform(t.data(),layer("lapis"),lapisY)){log("non-ticking edit drawn: %s",describe(t));request("unloadBorder");next();return;}
                    if(System.nanoTime()-phaseBegan>60_000_000_000L)throw new AssertionError("Stale tile after an edit in a loaded chunk that does not tick blocks: "+describe(t));
                }
                case 26->{if(doneTask.equals("unloadBorder")){client.disconnect(new TitleScreen(),false);phase=11;phaseBegan=System.nanoTime();}}
                case 12->{
                    // Reopened from disk with a new server: the last (lapis) layer is drawn again.
                    var t=tile();
                    if(!atCamera||!settled(t))return;
                    if(!uniform(t.data(),layer("lapis"),lapisY)) {
                        if(System.nanoTime()-phaseBegan>60_000_000_000L)throw new AssertionError("Reopened world drew stale data: "+describe(t));
                        return;
                    }
                    if(serverQueues()!=1)throw new AssertionError("Server queues after reopening: "+serverQueues());
                    log("reopened: %s",describe(t));
                    finished=true;
                    Files.writeString(DIR.resolve("result.txt"),"PASS horizon lifecycle: stationary edit refresh, dimension return, saved-source invalidation on return and while stationary, quit and reopen");
                    client.disconnect(new TitleScreen(),false);client.stop();
                }
                default->{}
            }
        } catch(Throwable t) {
            finished=true;Blockbranch.LOGGER.error("HORIZON_LIFECYCLE_FAIL",t);
            try{Files.writeString(DIR.resolve("result.txt"),"FAIL "+t);}catch(Exception ignored){}
            client.stop();
        }
    }
    private static void next(){phase++;phaseBegan=System.nanoTime();}
    private static HorizonClient.TileState tile(){return camera==null?null:HorizonClient.tile(key(0,targetX,targetZ));}
    private static String describe(HorizonClient.TileState t) {
        return HorizonTileChecks.describe(t)+(t!=null&&baseline!=null&&t.data()==baseline?" (baseline array)":"");
    }
    /** Package-private server lifecycle state, read reflectively from the client test. */
    private static boolean serverState(String method)throws ReflectiveOperationException {
        var m=blockbranch.horizon.HorizonService.class.getDeclaredMethod(method);m.setAccessible(true);return (boolean)m.invoke(null);
    }
    private static int serverQueues()throws ReflectiveOperationException {
        var m=blockbranch.horizon.HorizonService.class.getDeclaredMethod("queuedPlayers");m.setAccessible(true);return (int)m.invoke(null);
    }
    private static void command(MinecraftServer server,String c){server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),c);}
    private static void log(String format,Object... args){Blockbranch.LOGGER.info("HORIZON_LIFECYCLE {}",String.format(Locale.ROOT,format,args));}
    private HorizonLifecycleReview(){}
}
