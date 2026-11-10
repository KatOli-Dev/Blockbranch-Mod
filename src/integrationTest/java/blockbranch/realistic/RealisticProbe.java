package blockbranch.realistic;

import blockbranch.Blockbranch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Only handled by the opt-in isolated client fixture. */
public record RealisticProbe(int stage) implements CustomPacketPayload {
    public static final Type<RealisticProbe> TYPE=new Type<>(Blockbranch.id("realistic_fixture_probe"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RealisticProbe> CODEC=new StreamCodec<>() {
        public RealisticProbe decode(RegistryFriendlyByteBuf b) { int stage=b.readVarInt(); if(stage<1 || stage>127) throw new IllegalArgumentException("Invalid probe"); return new RealisticProbe(stage); }
        public void encode(RegistryFriendlyByteBuf b,RealisticProbe p) { b.writeVarInt(p.stage); }
    };
    @Override public Type<RealisticProbe> type() { return TYPE; }
    public static final net.minecraft.core.BlockPos ROOM=new net.minecraft.core.BlockPos(1401431,-4096,-1045106);
    public static final net.minecraft.core.BlockPos LAMP=ROOM.below(4);
    public static final net.minecraft.core.BlockPos CHEST=ROOM.offset(2,-3,0);
}
