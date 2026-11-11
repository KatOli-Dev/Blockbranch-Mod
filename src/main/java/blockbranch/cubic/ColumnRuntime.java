package blockbranch.cubic;
import blockbranch.Blockbranch;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
public final class ColumnRuntime {
    public static void initialize() {
        VerticalRuntime.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(ColumnWorldFormat::check);
        PayloadTypeRegistry.serverboundPlay().register(ColumnHello.TYPE,ColumnHello.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ColumnHello.TYPE,(packet,context)-> {});
        PayloadTypeRegistry.clientboundPlay().register(ColumnPacket.TYPE,ColumnPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TerrainWindowPacket.TYPE,TerrainWindowPacket.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)-> {
            if(!ServerPlayNetworking.canSend(handler.player,ColumnPacket.TYPE)||!ServerPlayNetworking.canSend(handler.player,TerrainWindowPacket.TYPE)) { handler.disconnect(Component.literal("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 75 / canopy catalog 184 / boreal surface catalog 16 / short reed stacks / ore definition 15 / content catalog 21).")); return; }
        });
        Blockbranch.LOGGER.info("Sparse vanilla columns: Overworld Y [{}, {}), horizontal ±{}",ColumnLimits.MIN_Y,ColumnLimits.MAX_Y,ColumnLimits.HORIZONTAL_LIMIT);
    }
    public static void send(ServerGamePacketListenerImpl connection,LevelChunk chunk) {
        // Chunk sending can precede JOIN rejection. Check the full capability
        // pair before entering VerticalRuntime or sending any sparse state.
        if(!ServerPlayNetworking.canSend(connection.player,ColumnPacket.TYPE)||!ServerPlayNetworking.canSend(connection.player,TerrainWindowPacket.TYPE)) return;
        var sparse=(SparseColumn)chunk;
        if(!sparse.blockbranch$expanded()) return;
        if(sparse.blockbranch$terrain()!=null) { VerticalRuntime.send(connection,chunk); return; }
        sparse.blockbranch$sections().forEach((y,section)-> {
            if(y < -4 || y>19) ServerPlayNetworking.send(connection.player,ColumnPacket.create(chunk.getPos().x(),chunk.getPos().z(),y,section));
        });
        for(var entity:chunk.getBlockEntities().values()) if(entity.getBlockPos().getY() < -64 || entity.getBlockPos().getY()>319) connection.send(ClientboundBlockEntityDataPacket.create(entity));
    }
    private ColumnRuntime() {}
}
