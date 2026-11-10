package blockbranch.metallurgy;

import blockbranch.Blockbranch;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** A bounded, loaded-only observation. No ore identity or world changes leave this scan. */
public final class OrePulse {
    public static final int RADIUS=6,MAX_MARKERS=64,DURATION=60,COOLDOWN=10;
    public static final TagKey<Block> ORES=TagKey.create(Registries.BLOCK,Blockbranch.id("prospectable_ores"));
    private static final Map<ServerPlayer,Integer> LAST=new WeakHashMap<>();
    private static final List<BlockPos> OFFSETS=offsets();
    private static List<BlockPos> offsets(){var values=new ArrayList<BlockPos>();for(int y=-RADIUS;y<=RADIUS;y++)for(int z=-RADIUS;z<=RADIUS;z++)for(int x=-RADIUS;x<=RADIUS;x++)if(x*x+y*y+z*z<=RADIUS*RADIUS)values.add(new BlockPos(x,y,z));values.sort(Comparator.comparingDouble(p->p.distSqr(BlockPos.ZERO)));return List.copyOf(values);}
    public static List<BlockPos> nearby(ServerLevel level,BlockPos center){
        var found=new ArrayList<BlockPos>();
        for(var offset:OFFSETS){var pos=center.offset(offset);if(!level.isInWorldBounds(pos))continue;var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);if(chunk!=null&&chunk.getBlockState(pos).is(ORES)){found.add(pos);if(found.size()==MAX_MARKERS)break;}}
        return List.copyOf(found);
    }
    public static void afterMining(ServerPlayer player,BlockPos center){
        if(!player.isAlive()||player.isSpectator()||player.isCreative()||!ServerPlayNetworking.canSend(player,OrePulsePacket.TYPE))return;
        int now=player.level().getServer().getTickCount();var previous=LAST.get(player);if(previous!=null&&now-previous<COOLDOWN)return;LAST.put(player,now);
        ServerPlayNetworking.send(player,new OrePulsePacket(player.level().dimension().identifier(),center,nearby(player.level(),center)));
    }
    private OrePulse(){}
}
