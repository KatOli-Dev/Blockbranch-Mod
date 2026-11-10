package blockbranch.mixin.cubic;
import blockbranch.cubic.ColumnLimits;
import com.mojang.serialization.*;
import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.storage.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(SectionStorage.PackedChunk.class)
abstract class PackedSectionsParseMixin {
    @Inject(method="parse",at=@At("HEAD"),cancellable=true)
    private static <P> void blockbranch$parse(Codec<P> codec,DynamicOps<Tag> ops,Tag tag,SimpleRegionStorage storage,LevelHeightAccessor height,CallbackInfoReturnable<SectionStorage.PackedChunk<P>> cir) {
        if(!ColumnLimits.expanded(height)) return;
        Dynamic<Tag> original=new Dynamic<>(ops,tag);
        Dynamic<Tag> fixed=storage.upgradeChunkTag(original,1945);
        Int2ObjectMap<P> sections=new Int2ObjectOpenHashMap<>();
        var entries=fixed.get("Sections").result().map(value->value.asMapOpt().result().orElse(java.util.stream.Stream.empty())).orElse(java.util.stream.Stream.empty());
        for(var entry:entries.toList()) {
            int y=Integer.parseInt(entry.getFirst().asString().getOrThrow());
            if(y<height.getMinSectionY() || y>height.getMaxSectionY()) throw new IllegalArgumentException("POI section outside build range: "+y);
            sections.put(y,codec.parse(entry.getSecond()).getOrThrow());
        }
        cir.setReturnValue(new SectionStorage.PackedChunk<>(sections,original!=fixed));
    }
}
