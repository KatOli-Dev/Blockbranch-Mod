package blockbranch.horizon;
import static blockbranch.horizon.HorizonTiles.*;
import blockbranch.Blockbranch;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/**
 * One sampled horizon tile: {@link HorizonTiles#SAMPLES}² columns of {@link HorizonTiles#FIELDS}
 * ints. Block states use global state ids and biomes use synchronized registry ids, so
 * both endpoints must run the same registries (guaranteed by the play-phase handshake).
 */
public record HorizonTile(int generation,long key,int[] data) implements CustomPacketPayload {
    public static final int LENGTH=SAMPLES*SAMPLES*FIELDS;
    public static final Type<HorizonTile> TYPE=new Type<>(Blockbranch.id("horizon_tile"));
    public static final StreamCodec<RegistryFriendlyByteBuf,HorizonTile> CODEC=new StreamCodec<>() {
        public HorizonTile decode(RegistryFriendlyByteBuf b) {
            int generation=b.readVarInt();long key=b.readLong();
            if(!valid(key))throw new IllegalArgumentException("Horizon tile key");
            int[] states=palette(b),biomes=palette(b);int[] d=new int[LENGTH];int ground=b.readInt();
            for(int s=0,o=0;s<SAMPLES*SAMPLES;s++,o+=FIELDS) {
                if(s>0)ground+=zag(b.readVarInt());
                d[o+GROUND]=ground;d[o+TOP]=ground+zag(b.readVarInt());d[o+CANOPY_BOTTOM]=d[o+TOP]-zag(b.readVarInt());
                int water=b.readVarInt();d[o+WATER]=water==0?NONE:ground+water;
                d[o+TOP_STATE]=states[index(b,states)];d[o+GROUND_STATE]=states[index(b,states)];d[o+DEEP_STATE]=states[index(b,states)];
                d[o+BIOME]=biomes[index(b,biomes)];
            }
            return new HorizonTile(generation,key,d);
        }
        public void encode(RegistryFriendlyByteBuf b,HorizonTile p) {
            var d=p.data;var states=new Int2IntOpenHashMap();var stateList=new IntArrayList();var biomes=new Int2IntOpenHashMap();var biomeList=new IntArrayList();
            for(int o=0;o<LENGTH;o+=FIELDS) {
                for(int f:new int[]{TOP_STATE,GROUND_STATE,DEEP_STATE})if(!states.containsKey(d[o+f])){states.put(d[o+f],stateList.size());stateList.add(d[o+f]);}
                if(!biomes.containsKey(d[o+BIOME])){biomes.put(d[o+BIOME],biomeList.size());biomeList.add(d[o+BIOME]);}
            }
            b.writeVarInt(p.generation);b.writeLong(p.key);
            b.writeVarInt(stateList.size());stateList.forEach(b::writeVarInt);b.writeVarInt(biomeList.size());biomeList.forEach(b::writeVarInt);
            b.writeInt(d[GROUND]);
            for(int o=0;o<LENGTH;o+=FIELDS) {
                if(o>0)b.writeVarInt(zig(d[o+GROUND]-d[o-FIELDS+GROUND]));
                b.writeVarInt(zig(d[o+TOP]-d[o+GROUND]));
                b.writeVarInt(zig(d[o+TOP]-d[o+CANOPY_BOTTOM]));
                b.writeVarInt(d[o+WATER]==NONE?0:d[o+WATER]-d[o+GROUND]);
                b.writeVarInt(states.get(d[o+TOP_STATE]));b.writeVarInt(states.get(d[o+GROUND_STATE]));b.writeVarInt(states.get(d[o+DEEP_STATE]));
                b.writeVarInt(biomes.get(d[o+BIOME]));
            }
        }
    };
    /** Validates the invariants the encoder relies on: water strictly above ground. */
    public HorizonTile {
        if(data.length!=LENGTH)throw new IllegalArgumentException("Horizon tile length "+data.length);
        for(int o=0;o<LENGTH;o+=FIELDS)if(data[o+WATER]!=NONE&&data[o+WATER]<=data[o+GROUND])throw new IllegalArgumentException("Horizon water at or below ground");
    }
    private static int[] palette(RegistryFriendlyByteBuf b){int n=b.readVarInt();if(n<1||n>SAMPLES*SAMPLES*3)throw new IllegalArgumentException("Horizon palette "+n);int[] v=new int[n];for(int i=0;i<n;i++)v[i]=b.readVarInt();return v;}
    private static int index(RegistryFriendlyByteBuf b,int[] palette){int i=b.readVarInt();if(i<0||i>=palette.length)throw new IllegalArgumentException("Horizon palette index");return i;}
    private static int zig(int v){return (v<<1)^(v>>31);}
    private static int zag(int v){return (v>>>1)^-(v&1);}
    @Override public Type<HorizonTile> type(){return TYPE;}
}
