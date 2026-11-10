package blockbranch.mixin.horizon;
import blockbranch.horizon.HorizonService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/**
 * Notes the chunk of every block change in a loaded server chunk for the horizon.
 *
 * <p>This hooks the chunk write rather than {@code ServerLevel.sendBlockUpdated}:
 * {@code Level.setBlock} sends that update only with flag 2 and only in chunks that tick
 * blocks, so edits in chunks loaded at the full status alone (the ring around force-loaded
 * or ticking chunks) or made without flag 2 never reach it. {@code setBlockState} returns
 * {@code null} when nothing changed, or when a removal callback already replaced the new block
 * through a write of its own, which is noted here in turn.
 */
@Mixin(LevelChunk.class)
abstract class HorizonBlockUpdateMixin {
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void blockbranch$horizonChanged(BlockPos pos,BlockState state,int flags,CallbackInfoReturnable<BlockState> cir) {
        if(!HorizonService.ENABLED||cir.getReturnValue()==null||!(((LevelChunk)(Object)this).getLevel() instanceof ServerLevel level))return;
        int x=pos.getX()>>4,z=pos.getZ()>>4;
        // The changed set belongs to the server thread; an off-thread write is noted there.
        var server=level.getServer();
        if(server.isSameThread())HorizonService.changed(level,x,z);
        else server.execute(()->HorizonService.changed(level,x,z));
    }
}
