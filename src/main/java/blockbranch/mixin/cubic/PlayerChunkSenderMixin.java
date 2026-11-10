package blockbranch.mixin.cubic;
import blockbranch.cubic.ColumnRuntime;
import net.minecraft.server.network.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(PlayerChunkSender.class)
abstract class PlayerChunkSenderMixin {
    @Inject(method="sendChunk",at=@At("TAIL"))
    private static void blockbranch$send(ServerGamePacketListenerImpl connection,ServerLevel level,LevelChunk chunk,CallbackInfo ci) { ColumnRuntime.send(connection,chunk); }
}
