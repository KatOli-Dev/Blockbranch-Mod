package blockbranch.mixin.cubic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ClientboundLevelChunkPacketData.class)
abstract class ChunkPacketMixin {
    @Redirect(method="<init>(Lnet/minecraft/world/level/chunk/LevelChunk;)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/LevelChunk;getBlockEntities()Ljava/util/Map;"))
    private Map<BlockPos,BlockEntity> blockbranch$entities(LevelChunk chunk) {
        var result=new HashMap<BlockPos,BlockEntity>();
        chunk.getBlockEntities().forEach((pos,entity)-> { if(pos.getY()>=Short.MIN_VALUE && pos.getY()<=Short.MAX_VALUE) result.put(pos,entity); });
        return result;
    }
}
