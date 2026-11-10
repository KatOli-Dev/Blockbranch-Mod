package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.level.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(PoiManager.class)
abstract class PoiManagerMixin {
    @Shadow @Final private it.unimi.dsi.fastutil.longs.LongSet loadedChunks;
    @Inject(method="ensureLoadedAndValid",at=@At("HEAD"),cancellable=true)
    private void blockbranch$ensure(LevelReader reader,BlockPos center,int radius,CallbackInfo ci) {
        if(!((SparseSectionStorage<?>)this).blockbranch$sparseStorage()) return;
        ChunkPos.rangeClosed(ChunkPos.containing(center),Math.floorDiv(radius,16)).forEach(pos-> {
            if(loadedChunks.add(pos.pack())) reader.getChunk(pos.x(),pos.z(),net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY);
        });
        ci.cancel();
    }
    @Inject(method="getInChunk",at=@At("HEAD"),cancellable=true)
    private void blockbranch$search(Predicate<Holder<PoiType>> predicate,ChunkPos pos,PoiManager.Occupancy occupancy,CallbackInfoReturnable<Stream<PoiRecord>> cir) {
        if(!((SparseSectionStorage<?>)this).blockbranch$sparseStorage()) return;
        cir.setReturnValue(((SparseSectionStorage<PoiSection>)this).blockbranch$inChunk(pos).flatMap(section->section.getRecords(predicate,occupancy)));
    }
}
