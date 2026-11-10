package blockbranch.mixin.cubic;
import net.minecraft.world.level.lighting.*;
import net.minecraft.world.level.chunk.DataLayer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(LayerLightSectionStorage.class)
public interface LightStorageAccessor {
    @Accessor("updatingSectionData") DataLayerStorageMap<?> blockbranch$updating();
    @Invoker("getDataLayerToWrite") DataLayer blockbranch$writeLayer(long node);
    @Invoker("getStoredLevel") int blockbranch$getLevel(long node);
    @Invoker("setStoredLevel") void blockbranch$setLevel(long node,int value);
    @Accessor("visibleSectionData") DataLayerStorageMap<?> blockbranch$visible();
    @Accessor("queuedSections") Long2ObjectMap<DataLayer> blockbranch$queued();
}
