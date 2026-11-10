package blockbranch.cubic;
import blockbranch.Blockbranch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Capability channel identifies section, terrain-window and canopy-catalog synchronization. */
public record ColumnHello() implements CustomPacketPayload {
    public static final ColumnHello INSTANCE=new ColumnHello();
    public static final Type<ColumnHello> TYPE=new Type<>(Blockbranch.id(ColumnPacket.TYPE.id().getPath()+"_"+TerrainWindowPacket.TYPE.id().getPath()+"_hello"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ColumnHello> CODEC=StreamCodec.unit(INSTANCE);
    @Override public Type<ColumnHello> type() { return TYPE; }
}
