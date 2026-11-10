package blockbranch.cubic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import blockbranch.Blockbranch;

/** A procedural definition and the overlay window, rather than every vertical section. */
public record TerrainWindowPacket(int x,int z,int min,int max,CompoundTag base,int[] edited,byte[] occupancy) implements CustomPacketPayload {
    // Append-only canopy codes are part of the wire contract even when the NBT
    // profile and packet layouts do not change. Pre-catalog peers cannot safely
    // advertise their decoder range, so both endpoints require the guarded ID.
    // Boreal surface codes and short reed geometry require matching profile-wrapper support.
    // Inventory catalog changes also require matching peers, independently of ore geometry.
    public static final Type<TerrainWindowPacket> TYPE=new Type<>(Blockbranch.id("terrain_window_v71_canopy"+CanopyLayer.LAST_CODE+"_boreal"+blockbranch.realistic.BorealSurface.LAST+"_reed74_ore"+blockbranch.realistic.OreLayer.DEFINITION+"_metals20"));
    public static final StreamCodec<RegistryFriendlyByteBuf,TerrainWindowPacket> CODEC=new StreamCodec<>() {
        public TerrainWindowPacket decode(RegistryFriendlyByteBuf b) {
            int x=b.readInt(),z=b.readInt(),min=b.readInt(),max=b.readInt();
            if(x<-(ColumnLimits.HORIZONTAL_LIMIT>>4) || x>=(ColumnLimits.HORIZONTAL_LIMIT>>4) || z<-(ColumnLimits.HORIZONTAL_LIMIT>>4) || z>=(ColumnLimits.HORIZONTAL_LIMIT>>4)
                || min<(ColumnLimits.MIN_Y>>4) || max>=(ColumnLimits.MAX_Y>>4) || min>max || max-min>128)
                throw new IllegalArgumentException("Invalid terrain window");
            var base=b.readNbt(); if(base==null) throw new IllegalArgumentException("Missing terrain definition");
            return new TerrainWindowPacket(x,z,min,max,base,b.readVarIntArray(ColumnLimits.HEIGHT>>4),b.readByteArray(SectionOccupancy.MAX_BYTES));
        }
        public void encode(RegistryFriendlyByteBuf b,TerrainWindowPacket p) {
            b.writeInt(p.x); b.writeInt(p.z); b.writeInt(p.min); b.writeInt(p.max); b.writeNbt(p.base); b.writeVarIntArray(p.edited);b.writeByteArray(p.occupancy);
        }
    };
    @Override public Type<TerrainWindowPacket> type() { return TYPE; }
}
