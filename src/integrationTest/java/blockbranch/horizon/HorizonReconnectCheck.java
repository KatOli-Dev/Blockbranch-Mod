package blockbranch.horizon;

import blockbranch.Blockbranch;
import java.nio.file.*;
import java.util.Locale;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Dedicated-server half of the loopback horizon reconnect check; the client half is
 * {@code blockbranch.client.horizon.HorizonReconnectReview}. Files in the run directory only
 * coordinate the two processes; horizon traffic uses the real connection.
 * <ol>
 * <li>first join: the player is held still 10 chunks west of a target chunk, named in
 *     {@code target.txt}, while the client draws the target's tile;
 * <li>the client disconnects: with the server still running, the player's horizon queue must
 *     be gone. The server then loads the target, places a gold layer, lets it unload (saving
 *     it) and writes the layer height to {@code edited.txt};
 * <li>the same client rejoins and draws gold ({@code drawn.txt}): the server must hold
 *     exactly one queue, a new one. It writes {@code server.txt} and halts when the client leaves.
 * </ol>
 * The queue goes by two paths, the disconnect handler and the tick pump's offline-player
 * check, so the queue step fails only when both are missing.
 */
public final class HorizonReconnectCheck {
    private static final Path DIR=Path.of(System.getProperty("blockbranch.horizonReconnectDirectory"));
    private static final long BEGAN=System.nanoTime(),DEADLINE=900_000_000_000L;
    /** Target chunk distance east of the camera chunk: beyond the view distance plus the refresh margin. */
    private static final int OFFSET=10;
    private static Vec3 camera;
    private static int targetX,targetZ,phase,ticks;
    private static Object firstQueue;
    private static volatile boolean done;

    public static void register() {
        // The client run loads this test mod too; only the dedicated server drives the check.
        if(net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType()!=net.fabricmc.api.EnvType.SERVER)return;
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{if(done)server.execute(()->server.halt(false));});
        ServerTickEvents.END_SERVER_TICK.register(HorizonReconnectCheck::tick);
    }

    private static void tick(MinecraftServer server) {
        if(done)return;
        try {
            if(System.nanoTime()-BEGAN>DEADLINE)throw new AssertionError("Server deadline at phase "+phase);
            var level=server.overworld();var players=server.getPlayerList().getPlayers();
            var player=players.isEmpty()?null:players.getFirst();
            ticks++;
            if(player!=null&&camera!=null)hold(server,player);
            switch(phase) {
                case 0->{
                    if(player==null)return;
                    var rules=level.getGameRules();
                    rules.set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,server);
                    rules.set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
                    rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_TIME,false,server);
                    rules.set(net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER,false,server);
                    command(server,"time set noon");command(server,"weather clear");
                    player.setGameMode(GameType.CREATIVE);
                    var spawn=player.blockPosition();
                    int y=level.getChunk(spawn.getX()>>4,spawn.getZ()>>4).getHeight(Heightmap.Types.MOTION_BLOCKING,spawn.getX()&15,spawn.getZ()&15);
                    targetX=(spawn.getX()>>4)+OFFSET;targetZ=spawn.getZ()>>4;
                    camera=new Vec3(spawn.getX()+.5,y+30,spawn.getZ()+.5);
                    Files.writeString(DIR.resolve("target.txt"),targetX+" "+targetZ);
                    log("camera %s target chunk %d %d",camera,targetX,targetZ);next();
                }
                case 1->{
                    // Remember the first connection's queue; once the player has left it must be removed.
                    if(player!=null){if(firstQueue==null)firstQueue=queue(player.getUUID());ticks=0;return;}
                    if(firstQueue==null)throw new AssertionError("Player left before its horizon queue existed");
                    if(ticks<20)return;
                    if(HorizonService.queuedPlayers()!=0||!HorizonService.running())
                        throw new AssertionError("After the disconnect: queues="+HorizonService.queuedPlayers()+" running="+HorizonService.running());
                    log("disconnected: queues 0, workers running, %d players",players.size());
                    level.setChunkForced(targetX,targetZ,true);next();
                }
                case 2->{
                    if(player!=null)throw new AssertionError("Client rejoined before the edit");
                    long pos=ChunkPos.pack(targetX,targetZ);
                    if(level.getChunkSource().getChunkNow(targetX,targetZ)==null||!level.shouldTickBlocksAt(pos)){ticks=0;return;}
                    // Let the generation refresh land first, so only the edit can bring the gold layer.
                    if(ticks<80)return;
                    var chunk=level.getChunk(targetX,targetZ);int top=Integer.MIN_VALUE;
                    for(int i=0;i<256;i++)top=Math.max(top,chunk.getHeight(Heightmap.Types.MOTION_BLOCKING,i&15,i>>4));
                    int goldY=top+8;
                    for(int i=0;i<256;i++)level.setBlock(new BlockPos(targetX*16+(i&15),goldY,targetZ*16+(i>>4)),Blocks.GOLD_BLOCK.defaultBlockState(),3);
                    level.setChunkForced(targetX,targetZ,false);
                    Files.writeString(DIR.resolve("gold.txt"),Integer.toString(goldY));
                    log("placed gold layer at y=%d in chunk %d %d with no player connected",goldY,targetX,targetZ);next();
                }
                case 3->{
                    if(level.getChunkSource().getChunkNow(targetX,targetZ)!=null){ticks=0;return;}
                    if(ticks<60)return;
                    Files.move(DIR.resolve("gold.txt"),DIR.resolve("edited.txt"),StandardCopyOption.ATOMIC_MOVE);
                    log("target unloaded; client may rejoin");next();
                }
                case 4->{
                    // Rejoined and drawn: one queue again, and not the first connection's.
                    if(player==null||!Files.exists(DIR.resolve("drawn.txt")))return;
                    var current=queue(player.getUUID());
                    if(HorizonService.queuedPlayers()!=1||current==null||current==firstQueue)
                        throw new AssertionError("After rejoining: queues="+HorizonService.queuedPlayers()+" new queue="+(current!=null&&current!=firstQueue));
                    done=true;
                    Files.writeString(DIR.resolve("server.txt"),"PASS server: queue removed on disconnect while running, edit saved while away, one new queue after rejoining");
                    log("rejoined: one new queue");
                }
                default->throw new AssertionError("phase "+phase);
            }
        } catch(Throwable t) {
            done=true;Blockbranch.LOGGER.error("HORIZON_RECONNECT_FAIL",t);
            try{Files.writeString(DIR.resolve("server.txt"),"FAIL "+t);}catch(Exception ignored){}
            server.halt(false);
        }
    }
    private static void hold(MinecraftServer server,net.minecraft.server.level.ServerPlayer player) {
        player.getAbilities().flying=true;player.onUpdateAbilities();player.setDeltaMovement(Vec3.ZERO);
        if(player.position().distanceToSqr(camera)>.001||Math.abs(net.minecraft.util.Mth.wrapDegrees(player.getYRot()+90))>.02)
            command(server,"tp "+player.getName().getString()+" "+camera.x+" "+camera.y+" "+camera.z+" -90 5");
    }
    /** The player's horizon queue object, so a rejoin can be told from a kept queue. */
    private static Object queue(java.util.UUID id)throws ReflectiveOperationException {
        var f=HorizonService.class.getDeclaredField("QUEUES");f.setAccessible(true);return ((java.util.Map<?,?>)f.get(null)).get(id);
    }
    private static void next(){phase++;ticks=0;}
    private static void command(MinecraftServer server,String c){server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),c);}
    private static void log(String format,Object... args){Blockbranch.LOGGER.info("HORIZON_RECONNECT {}",String.format(Locale.ROOT,format,args));}
    private HorizonReconnectCheck(){}
}
