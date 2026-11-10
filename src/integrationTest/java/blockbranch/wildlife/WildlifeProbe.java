package blockbranch.wildlife;
import blockbranch.Blockbranch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Only handled by the explicit client review/survey fixture. */
public record WildlifeProbe(int stage,String message) implements CustomPacketPayload {
    public static final Type<WildlifeProbe> TYPE=new Type<>(Blockbranch.id("wildlife_fixture_probe_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WildlifeProbe> CODEC=new StreamCodec<>() {
        public WildlifeProbe decode(RegistryFriendlyByteBuf buffer){return new WildlifeProbe(buffer.readVarInt(),buffer.readUtf(256));}
        public void encode(RegistryFriendlyByteBuf buffer,WildlifeProbe packet){buffer.writeVarInt(packet.stage);buffer.writeUtf(packet.message,256);}
    };
    @Override public Type<WildlifeProbe> type(){return TYPE;}
}
