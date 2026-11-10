package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import java.util.NavigableMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(DataLayerStorageMap.class)
abstract class DataLayerStorageMapMixin implements LightColumnIndex {
    @Shadow @Final protected Long2ObjectOpenHashMap<DataLayer> map;
    @ModifyVariable(method="<init>",at=@At("HEAD"),argsOnly=true)
    private static Long2ObjectOpenHashMap<DataLayer> blockbranch$indexed(Long2ObjectOpenHashMap<DataLayer> initial){
        return initial instanceof IndexedDataLayerMap?initial:new IndexedDataLayerMap(initial);
    }
    @Inject(method="setLayer",at=@At("RETURN"))
    private void blockbranch$set(long node,DataLayer value,CallbackInfo ci){((IndexedDataLayerMap)map).indexAdd(node);}
    @Inject(method="removeLayer",at=@At("RETURN"))
    private void blockbranch$remove(long node,CallbackInfoReturnable<DataLayer> cir){((IndexedDataLayerMap)map).indexRemove(node);}
    public java.util.NavigableSet<Integer> blockbranch$sectionYs(long node){return ((IndexedDataLayerMap)map).sectionYs(node);}
    public DataLayer blockbranch$layer(long node){return map.getOrDefault(node,null);}
    // Compatibility/debug view. Production navigation uses heights and native values separately.
    public NavigableMap<Integer,DataLayer> blockbranch$layers(long node){
        var result=new java.util.TreeMap<Integer,DataLayer>();
        for(int y:blockbranch$sectionYs(node))result.put(y,map.get(net.minecraft.core.SectionPos.asLong(net.minecraft.core.SectionPos.x(node),y,net.minecraft.core.SectionPos.z(node))));
        return java.util.Collections.unmodifiableNavigableMap(result);
    }
}
