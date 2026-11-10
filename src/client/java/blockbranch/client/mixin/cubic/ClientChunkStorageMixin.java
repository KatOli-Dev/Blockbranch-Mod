package blockbranch.client.mixin.cubic;
import blockbranch.cubic.SparseColumn;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(targets="net.minecraft.client.multiplayer.ClientChunkCache$Storage")
abstract class ClientChunkStorageMixin {
    @Shadow private void markSectionEmpty(long node) { throw new AssertionError(); }
    @Shadow private void markSectionNotEmpty(long node) { throw new AssertionError(); }
    // Native refresh reads the physical arrays, which are intentionally empty for
    // procedural sections. Reconcile only active profile sections in that array
    // range; higher sparse sections retain their existing packet-driven updates.
    @Inject(method={"onChunkAdded","refreshEmptySections"},at=@At("TAIL"))
    private void blockbranch$logicalEmptiness(LevelChunk chunk,CallbackInfo ci) {
        var sparse=(SparseColumn)chunk;var terrain=sparse.blockbranch$terrain();
        if(!sparse.blockbranch$expanded()||terrain==null)return;
        int base=sparse.blockbranch$baseMinSection(),end=base+chunk.getSections().length;
        for(int y:terrain.lit)if(y>=base&&y<end){
            long node=SectionPos.asLong(chunk.getPos().x(),y,chunk.getPos().z());
            if(sparse.blockbranch$read(y).hasOnlyAir())markSectionEmpty(node);
            else markSectionNotEmpty(node);
        }
    }
    @Inject(method="onChunkRemoved",at=@At("HEAD"))
    private void blockbranch$remove(LevelChunk chunk,CallbackInfo ci) {
        if(((SparseColumn)chunk).blockbranch$expanded()) for(int y:((SparseColumn)chunk).blockbranch$sections().keySet()) if(y < -4 || y>19) markSectionEmpty(SectionPos.asLong(chunk.getPos().x(),y,chunk.getPos().z()));
    }
}
