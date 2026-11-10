package blockbranch.cubic;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
/** Opt-in local multiplayer fixtures, including block changes after initial column transfer. */
public final class ColumnNetworkSmoke {
    private record PlayerState(ServerPlayer player,int y,int ticks) {}
    private static final Queue<ServerPlayer> JOINED=new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final Map<UUID,PlayerState> ACTIVE=new HashMap<>();
    public static void joined(ServerPlayer player) { JOINED.add(player); }
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server-> {
            ServerPlayer player;
            while((player=JOINED.poll())!=null) {
                String name=player.getGameProfile().name(); int y=name.endsWith("Low") ? -262140 : 262140;
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamemode spectator "+name);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+name+" 1 "+y+" 1");
                server.overworld().setBlock(new BlockPos(12,y+1,12),Blocks.AIR.defaultBlockState(),3);
                ACTIVE.put(player.getUUID(),new PlayerState(player,y,0));
            }
            ACTIVE.replaceAll((id,state)-> {
                int tick=state.ticks()+1;
                if(tick==40 || tick==60) server.overworld().setBlock(new BlockPos(12,state.y()+1,12),(tick==40 ? Blocks.DIAMOND_BLOCK : Blocks.LAPIS_BLOCK).defaultBlockState(),3);
                if(tick==40) server.overworld().setBlock(new BlockPos(10,100000,10),Blocks.STONE.defaultBlockState(),3);
                if(tick==80) {
                    // Suppress the incremental removal: the replacement snapshot must clear it.
                    server.overworld().setBlock(new BlockPos(10,100000,10),Blocks.AIR.defaultBlockState(),16);
                    var chunk=server.overworld().getChunk(0,0);
                    state.player().connection.send(new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(chunk,server.overworld().getLightEngine(),null,null));
                    ColumnRuntime.send(state.player().connection,chunk);
                }
                if(Boolean.getBoolean("blockbranch.columnDimensionSmoke")) {
                    String name=state.player().getGameProfile().name();
                    if(tick==300) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"execute in minecraft:the_nether run tp "+name+" 1 100 1");
                    if(tick==600) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"execute in minecraft:the_end run tp "+name+" 1 100 1");
                    if(tick==900 || tick==1300) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"execute in minecraft:overworld run tp "+name+" 1 "+state.y()+" 1");
                    if(tick==1100) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+name+" 1024 "+state.y()+" 1024");
                }
                return new PlayerState(state.player(),state.y(),tick);
            });
            ACTIVE.values().removeIf(state->state.ticks()>1800 || state.player().hasDisconnected());
        });
    }
    private ColumnNetworkSmoke() {}
}
