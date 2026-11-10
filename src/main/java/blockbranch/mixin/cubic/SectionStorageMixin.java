package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;
import com.mojang.serialization.*;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(SectionStorage.class)
abstract class SectionStorageMixin<R,P> implements SparseSectionStorage<R> {
    @Shadow @Final protected LevelHeightAccessor levelHeightAccessor;
    @Shadow @Final private Long2ObjectMap<Optional<R>> storage;
    @Shadow @Final private LongSet loadedChunks;
    @Shadow @Final private BiFunction<P,Runnable,R> unpacker;
    @Shadow @Final private Function<R,P> packer;
    @Shadow @Final private Codec<P> codec;
    @Shadow protected abstract Optional<R> getOrLoad(long position);
    @Shadow protected abstract void onSectionLoad(long position);
    @Shadow protected abstract void setDirty(long position);
    @Unique private final Map<Long,Set<Integer>> blockbranch$occupied=new HashMap<>();
    @Inject(method="get",at=@At("RETURN"),cancellable=true)
    private void blockbranch$missing(long pos,CallbackInfoReturnable<Optional<R>> cir) {
        if(ColumnLimits.expanded(levelHeightAccessor) && cir.getReturnValue()==null && loadedChunks.contains(ChunkPos.pack(SectionPos.x(pos),SectionPos.z(pos)))) cir.setReturnValue(Optional.empty());
    }
    @Inject(method="unpackChunk(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/storage/SectionStorage$PackedChunk;)V",at=@At("HEAD"),cancellable=true)
    private void blockbranch$unpack(ChunkPos pos,@Coerce Object value,CallbackInfo ci) {
        if(!ColumnLimits.expanded(levelHeightAccessor)) return;
        if(value!=null) {
            var packed=(PackedSections<P>)value;
            packed.blockbranch$packed().forEach((y,p)-> {
                long key=SectionPos.asLong(pos.x(),y,pos.z());
                storage.put(key,Optional.of(unpacker.apply(p,()->setDirty(key))));
                blockbranch$occupied.computeIfAbsent(pos.pack(),ignored->new TreeSet<>()).add(y);
                onSectionLoad(key); if(packed.blockbranch$changed()) setDirty(key);
            });
        }
        ci.cancel();
    }
    @Inject(method="getOrCreate",at=@At("RETURN"))
    private void blockbranch$track(long pos,CallbackInfoReturnable<R> cir) {
        if(ColumnLimits.expanded(levelHeightAccessor)) blockbranch$occupied.computeIfAbsent(ChunkPos.pack(SectionPos.x(pos),SectionPos.z(pos)),ignored->new TreeSet<>()).add(SectionPos.y(pos));
    }
    @Inject(method="writeChunk(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;)Lcom/mojang/serialization/Dynamic;",at=@At("HEAD"),cancellable=true)
    private <T> void blockbranch$write(ChunkPos pos,DynamicOps<T> ops,CallbackInfoReturnable<Dynamic<T>> cir) {
        if(!ColumnLimits.expanded(levelHeightAccessor)) return;
        var sections=new HashMap<T,T>();
        for(int y:blockbranch$occupied.getOrDefault(pos.pack(),Set.of())) {
            var entry=storage.get(SectionPos.asLong(pos.x(),y,pos.z()));
            if(entry!=null && entry.isPresent()) sections.put(ops.createString(Integer.toString(y)),codec.encodeStart(ops,packer.apply(entry.get())).getOrThrow());
        }
        cir.setReturnValue(new Dynamic<>(ops,ops.createMap(Map.of(ops.createString("Sections"),ops.createMap(sections),ops.createString("DataVersion"),ops.createInt(SharedConstants.getCurrentVersion().dataVersion().version())))));
    }
    public boolean blockbranch$sparseStorage() { return ColumnLimits.expanded(levelHeightAccessor); }
    public Stream<R> blockbranch$inChunk(ChunkPos pos) {
        getOrLoad(SectionPos.asLong(pos.x(),0,pos.z()));
        return blockbranch$occupied.getOrDefault(pos.pack(),Set.of()).stream().map(y->storage.get(SectionPos.asLong(pos.x(),y,pos.z()))).filter(Objects::nonNull).flatMap(Optional::stream);
    }
}
