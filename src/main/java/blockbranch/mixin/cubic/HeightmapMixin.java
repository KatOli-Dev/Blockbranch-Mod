package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Heightmap.class)
abstract class HeightmapMixin {
    @Shadow @Final private ChunkAccess chunk;
    @Shadow @Final private Predicate<BlockState> isOpaque;
    @Shadow public abstract int getFirstAvailable(int x,int z);
    @Shadow private void setHeight(int x,int z,int height) { throw new AssertionError(); }
    @Inject(method="primeHeightmaps",at=@At("HEAD"),cancellable=true)
    private static void blockbranch$prime(ChunkAccess chunk,Set<Heightmap.Types> types,CallbackInfo ci) {
        var sparse=(SparseColumn)chunk; if(!sparse.blockbranch$expanded()) return;
        var sections=sparse.blockbranch$sections();
        for(var type:types) {
            var map=(HeightmapMixin)(Object)chunk.getOrCreateHeightmapUnprimed(type);
            for(int z=0;z<16;z++) for(int x=0;x<16;x++) map.setHeight(x,z,sparse.blockbranch$terrain()==null ? blockbranch$top(sections,type.isOpaque(),x,z,ColumnLimits.MAX_Y-1) : ColumnQueries.top(chunk,type.isOpaque(),x,z,ColumnLimits.MAX_Y-1));
        }
        ci.cancel();
    }
    @Inject(method="update",at=@At("HEAD"),cancellable=true)
    private void blockbranch$update(int x,int y,int z,BlockState state,CallbackInfoReturnable<Boolean> cir) {
        var sparse=(SparseColumn)chunk; if(!sparse.blockbranch$expanded()) return;
        int old=getFirstAvailable(x,z);
        if(y<=old-2) { cir.setReturnValue(false); return; }
        if(isOpaque.test(state)) { if(y>=old) { setHeight(x,z,y+1); cir.setReturnValue(true); } else cir.setReturnValue(false); }
        else if(y==old-1) { setHeight(x,z,sparse.blockbranch$terrain()==null ? blockbranch$top(sparse.blockbranch$sections(),isOpaque,x,z,y-1) : ColumnQueries.top(chunk,isOpaque,x,z,y-1)); cir.setReturnValue(true); }
        else cir.setReturnValue(false);
    }
    @Unique private static int blockbranch$top(java.util.NavigableMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection> sections,Predicate<BlockState> predicate,int x,int z,int ceiling) {
        for(var e:sections.descendingMap().entrySet()) {
            if((e.getKey()<<4)>ceiling || e.getValue().hasOnlyAir()) continue;
            for(int y=Math.min(15,ceiling-(e.getKey()<<4));y>=0;y--) if(predicate.test(e.getValue().getBlockState(x,y,z))) return (e.getKey()<<4)+y+1;
        }
        return ColumnLimits.MIN_Y;
    }
}
