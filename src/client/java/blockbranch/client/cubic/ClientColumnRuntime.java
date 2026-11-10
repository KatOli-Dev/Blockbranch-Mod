package blockbranch.client.cubic;
import blockbranch.cubic.*;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.status.ChunkStatus;
public final class ClientColumnRuntime {
    public static void initialize() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->ProceduralSectionCopies.clear());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler,sender,client)-> {
            if(!ClientPlayNetworking.canSend(ColumnHello.TYPE)) handler.getConnection().disconnect(net.minecraft.network.chat.Component.literal("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 15 / metallurgy catalog 20)."));
        });
        ClientPlayNetworking.registerGlobalReceiver(ColumnPacket.TYPE,(packet,context)->receiveColumn(packet,context.client()));
        ClientPlayNetworking.registerGlobalReceiver(TerrainWindowPacket.TYPE,(packet,context)->receiveWindow(packet,context.client()));
    }
    public static void receiveColumn(ColumnPacket packet,net.minecraft.client.Minecraft client) {
            var level=client.level;
            if(level==null || !ColumnLimits.expanded(level)) return;
            var chunk=level.getChunk(packet.x(),packet.z(),ChunkStatus.FULL,false);
            if(chunk==null) return;
            var section=((SparseColumn)chunk).blockbranch$write(packet.y());
            var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(packet.data()));
            try { section.read(buffer); if(buffer.isReadable()) throw new IllegalArgumentException("Trailing sparse section data"); }
            finally { buffer.release(); }
            if(section.maybeHas(net.minecraft.world.level.block.state.BlockState::hasBlockEntity)) {
                for(int y=0;y<16;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) if(section.getBlockState(x,y,z).hasBlockEntity())
                    ((net.minecraft.world.level.chunk.LevelChunk)chunk).getBlockEntity(new net.minecraft.core.BlockPos((packet.x()<<4)+x,(packet.y()<<4)+y,(packet.z()<<4)+z),net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.IMMEDIATE);
            }
            level.getChunkSource().onSectionEmptinessChanged(packet.x(),packet.y(),packet.z(),section.hasOnlyAir());
            level.getLightEngine().updateSectionStatus(SectionPos.of(chunk.getPos(),packet.y()),section.hasOnlyAir());
            blockbranch.client.realistic.ColumnReceiveBatch.changed(level,chunk,packet.y());
    }
    public static void receiveWindow(TerrainWindowPacket packet,net.minecraft.client.Minecraft client) {
            var level=client.level;
            if(level==null || !ColumnLimits.expanded(level)) return;
            var chunk=level.getChunk(packet.x(),packet.z(),ChunkStatus.FULL,false);
            if(chunk==null) return;
            var sparse=(SparseColumn)chunk;
            var old=sparse.blockbranch$terrain();
            var base=TerrainProfile.decode(packet.base(),sparse.blockbranch$factory());
            var occupancy=SectionOccupancy.decode(base,packet.min(),packet.max(),packet.edited(),packet.occupancy());
            var target=new java.util.HashSet<Integer>();
            for(int y=packet.min();y<=packet.max();y++)target.add(y);
            target.addAll(base.boundaries());
            var sources=packet.base().getIntArray("sky_sources").orElseThrow();
            if(sources.length!=256)throw new IllegalArgumentException("Invalid sky-source summary");
            for(int y:sources)if(y<ColumnLimits.MIN_Y-1||y>ColumnLimits.MAX_Y)throw new IllegalArgumentException("Sky source outside build bounds");
            // Validate the complete replacement before clearing existing sections or light state.
            var terrain=new ColumnTerrain(chunk,base,sparse.blockbranch$factory(),null,packet.edited());
            terrain.skySources=sources;
            if(old!=null) for(int y:old.lit) if(!target.contains(y)) {
                var section=SectionPos.of(chunk.getPos(),y);
                level.getLightEngine().updateSectionStatus(section,true);
                level.getLightEngine().queueSectionData(net.minecraft.world.level.LightLayer.BLOCK,section,null);
                level.getLightEngine().queueSectionData(net.minecraft.world.level.LightLayer.SKY,section,null);
            }
            for(int y:java.util.List.copyOf(sparse.blockbranch$sections().keySet())) sparse.blockbranch$replace(y,null);
            sparse.blockbranch$terrain(terrain); terrain.lit.addAll(target);
            for(int y:target) {
                boolean empty=windowEmpty(sparse,y,occupancy);
                level.getLightEngine().updateSectionStatus(SectionPos.of(chunk.getPos(),y),empty);
                level.getChunkSource().onSectionEmptinessChanged(packet.x(),y,packet.z(),empty);
                blockbranch.client.realistic.ColumnReceiveBatch.changed(level,chunk,y);
            }
            for(var pos:java.util.List.copyOf(((net.minecraft.world.level.chunk.LevelChunk)chunk).getBlockEntities().keySet()))
                if((pos.getY()>>4)<packet.min() || (pos.getY()>>4)>packet.max()) chunk.removeBlockEntity(pos);

    }
    private static boolean windowEmpty(SparseColumn sparse,int y,SectionOccupancy occupancy){return occupancy.empty(y);}
    private ClientColumnRuntime() {}
}
