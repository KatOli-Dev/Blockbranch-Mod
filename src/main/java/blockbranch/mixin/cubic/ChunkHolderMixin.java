package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import java.util.*;
import it.unimi.dsi.fastutil.shorts.*;
import net.minecraft.core.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ChunkHolder.class)
abstract class ChunkHolderMixin {
    @Shadow @Final private LevelHeightAccessor levelHeightAccessor;
    @Shadow @Final private ChunkHolder.PlayerProvider playerProvider;
    @Shadow private boolean hasChangedSections;
    @Shadow public abstract LevelChunk getTickingChunk();
    @Shadow private void broadcast(List<ServerPlayer> players,Packet<?> packet) { throw new AssertionError(); }
    @Shadow private void broadcastBlockEntityIfNeeded(List<ServerPlayer> players,Level level,BlockPos pos,BlockState state) { throw new AssertionError(); }
    @Unique private final Map<Integer,ShortSet> blockbranch$changes=new HashMap<>();
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/LevelHeightAccessor;getSectionsCount()I"))
    private int blockbranch$noArray(LevelHeightAccessor height) { return ColumnLimits.expanded(height) ? 0 : height.getSectionsCount(); }
    @Inject(method="blockChanged",at=@At("HEAD"),cancellable=true)
    private void blockbranch$change(BlockPos position,CallbackInfoReturnable<Boolean> cir) {
        if(!ColumnLimits.expanded(levelHeightAccessor)) return;
        if(getTickingChunk()==null) { cir.setReturnValue(false); return; }
        boolean had=hasChangedSections;
        hasChangedSections=true;
        blockbranch$changes.computeIfAbsent(position.getY()>>4,y->new ShortOpenHashSet()).add(SectionPos.sectionRelativePos(position));
        cir.setReturnValue(!had);
    }
    @Inject(method="broadcastChanges",at=@At("HEAD"))
    private void blockbranch$broadcast(LevelChunk chunk,CallbackInfo ci) {
        if(!ColumnLimits.expanded(levelHeightAccessor) || blockbranch$changes.isEmpty()) return;
        var players=playerProvider.getPlayers(chunk.getPos(),false);
        // Detach first: neighbor updates may mark the next batch while packets are produced.
        var batch=new HashMap<>(blockbranch$changes); blockbranch$changes.clear();
        for(var e:batch.entrySet()) {
            var recipients=((SparseColumn)chunk).blockbranch$terrain()==null ? players : players.stream().filter(p->VerticalRuntime.visible(p,e.getKey())).toList();
            var section=((SparseColumn)chunk).blockbranch$read(e.getKey());
            var packet=new ClientboundSectionBlocksUpdatePacket(SectionPos.of(chunk.getPos(),e.getKey()),e.getValue(),section);
            broadcast(recipients,packet);
            packet.runUpdates((position,state)->broadcastBlockEntityIfNeeded(recipients,chunk.getLevel(),position,state));
        }
    }
}
