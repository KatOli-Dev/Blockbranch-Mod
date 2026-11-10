package blockbranch.horizon;
import blockbranch.Blockbranch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Client's complete wanted-tile list, nearest first; it replaces any queued earlier list. */
public record HorizonRequest(int generation,long[] keys) implements CustomPacketPayload {
    public static final int MAX_KEYS=2048;
    public static final Type<HorizonRequest> TYPE=new Type<>(Blockbranch.id("horizon_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf,HorizonRequest> CODEC=new StreamCodec<>() {
        public HorizonRequest decode(RegistryFriendlyByteBuf b) {
            int generation=b.readVarInt(),n=b.readVarInt();
            if(n<0||n>MAX_KEYS)throw new IllegalArgumentException("Horizon request size "+n);
            long[] keys=new long[n];for(int i=0;i<n;i++)keys[i]=b.readLong();
            return new HorizonRequest(generation,keys);
        }
        public void encode(RegistryFriendlyByteBuf b,HorizonRequest p) {
            b.writeVarInt(p.generation);b.writeVarInt(p.keys.length);for(long k:p.keys)b.writeLong(k);
        }
    };
    @Override public Type<HorizonRequest> type(){return TYPE;}
}
