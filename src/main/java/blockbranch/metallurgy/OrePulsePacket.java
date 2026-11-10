package blockbranch.metallurgy;

import blockbranch.Blockbranch;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Same-style position markers only: no block states, item IDs, names or rarity data. */
public record OrePulsePacket(Identifier dimension,BlockPos center,List<BlockPos> positions) implements CustomPacketPayload {
    public OrePulsePacket {
        Objects.requireNonNull(dimension);center=center.immutable();positions=List.copyOf(positions);
        if(positions.size()>OrePulse.MAX_MARKERS)throw new IllegalArgumentException("Too many ore markers");
        for(var pos:positions)if(pos.distSqr(center)>OrePulse.RADIUS*OrePulse.RADIUS)throw new IllegalArgumentException("Ore marker outside pulse");
    }
    public static final Type<OrePulsePacket> TYPE=new Type<>(Blockbranch.id("ore_pulse_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OrePulsePacket> CODEC=new StreamCodec<>(){
        public OrePulsePacket decode(RegistryFriendlyByteBuf b){var dimension=b.readIdentifier();var center=b.readBlockPos();int n=b.readVarInt();if(n<0||n>OrePulse.MAX_MARKERS)throw new IllegalArgumentException("Invalid ore marker count");var positions=new ArrayList<BlockPos>(n);for(int i=0;i<n;i++)positions.add(b.readBlockPos());return new OrePulsePacket(dimension,center,positions);}
        public void encode(RegistryFriendlyByteBuf b,OrePulsePacket p){b.writeIdentifier(p.dimension);b.writeBlockPos(p.center);b.writeVarInt(p.positions.size());for(var pos:p.positions)b.writeBlockPos(pos);}
    };
    @Override public Type<OrePulsePacket> type(){return TYPE;}
}
