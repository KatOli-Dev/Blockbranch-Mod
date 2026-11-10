package blockbranch.cubic;
import blockbranch.Blockbranch;
import io.netty.buffer.Unpooled;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.chunk.LevelChunkSection;
/** One section per payload bounds transfer units independently of total populated height. */
public record ColumnPacket(int x,int z,int y,byte[] data) implements CustomPacketPayload {
    public static final int MAX_BYTES=65536;
    private static final String VERSION=FabricLoader.getInstance().getModContainer("blockbranch").orElseThrow().getMetadata().getVersion().getFriendlyString().replaceAll("[^a-z0-9_.-]","_");
    public static final Type<ColumnPacket> TYPE=new Type<>(Blockbranch.id("column_v70_"+VERSION));
    public static final StreamCodec<RegistryFriendlyByteBuf,ColumnPacket> CODEC=new StreamCodec<>() {
        public ColumnPacket decode(RegistryFriendlyByteBuf buffer) {
            int x=buffer.readInt(),z=buffer.readInt(),y=buffer.readInt();
            if(x < -(ColumnLimits.HORIZONTAL_LIMIT>>4) || x >= (ColumnLimits.HORIZONTAL_LIMIT>>4) || z < -(ColumnLimits.HORIZONTAL_LIMIT>>4) || z >= (ColumnLimits.HORIZONTAL_LIMIT>>4) || y < (ColumnLimits.MIN_Y>>4) || y >= (ColumnLimits.MAX_Y>>4)) throw new IllegalArgumentException("Invalid sparse section coordinate");
            return new ColumnPacket(x,z,y,buffer.readByteArray(MAX_BYTES));
        }
        public void encode(RegistryFriendlyByteBuf buffer,ColumnPacket packet) {
            buffer.writeInt(packet.x); buffer.writeInt(packet.z); buffer.writeInt(packet.y); buffer.writeByteArray(packet.data);
        }
    };
    public static ColumnPacket create(int x,int z,int y,LevelChunkSection section) {
        var buffer=new FriendlyByteBuf(Unpooled.buffer());
        try { section.write(buffer); if(buffer.readableBytes()>MAX_BYTES) throw new IllegalStateException("Section exceeds transfer limit"); byte[] data=new byte[buffer.readableBytes()]; buffer.readBytes(data); return new ColumnPacket(x,z,y,data); }
        finally { buffer.release(); }
    }
    @Override public Type<ColumnPacket> type() { return TYPE; }
}
