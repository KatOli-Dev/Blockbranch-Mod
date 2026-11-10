package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ChunkSkyLightSources.class)
abstract class ChunkSkyLightSourcesMixin {
    @Shadow @Final private int minY;
    @Shadow private void set(int index,int value) { throw new AssertionError(); }
    @Shadow private static boolean isEdgeOccluded(BlockState top,BlockState bottom) { throw new AssertionError(); }
    @Inject(method="fillFrom",at=@At("HEAD"),cancellable=true)
    private void blockbranch$fill(ChunkAccess chunk,CallbackInfo ci) {
        if(!((SparseColumn)chunk).blockbranch$expanded()) return;
        var terrain=((SparseColumn)chunk).blockbranch$terrain();
        if(terrain!=null && terrain.skySources!=null) {
            for(int i=0;i<256;i++) set(i,terrain.skySources[i]);
            ci.cancel(); return;
        }
        var sections=ColumnQueries.survey(chunk);
        for(int z=0;z<16;z++) for(int x=0;x<16;x++) set(x+16*z,blockbranch$find(chunk,sections,x,z,ColumnLimits.MAX_Y));
        ci.cancel();
    }
    @Inject(method="findLowestSourceBelow",at=@At("HEAD"),cancellable=true)
    private void blockbranch$below(BlockGetter getter,BlockPos pos,BlockState state,CallbackInfoReturnable<Integer> cir) {
        if(getter instanceof ChunkAccess chunk && ((SparseColumn)chunk).blockbranch$expanded()) cir.setReturnValue(blockbranch$find(chunk,ColumnQueries.survey(chunk),pos.getX()&15,pos.getZ()&15,pos.getY()));
    }
    @Unique private int blockbranch$find(ChunkAccess chunk,java.util.NavigableMap<Integer,net.minecraft.world.level.chunk.LevelChunkSection> sections,int x,int z,int ceiling) {
        var air=Blocks.AIR.defaultBlockState(); BlockState top=chunk.getBlockState(new BlockPos(x,ceiling,z)); int previous=ceiling;
        for(var e:sections.descendingMap().entrySet()) {
            int base=e.getKey()<<4; if(base>=ceiling || e.getValue().hasOnlyAir()) continue;
            int start=Math.min(base+15,ceiling-1);
            if(previous>start+1) {
                var gap=((SparseColumn)chunk).blockbranch$terrain()==null ? air : chunk.getBlockState(new BlockPos(x,previous-1,z));
                if(isEdgeOccluded(top,gap)) return previous; top=gap;
            }
            for(int y=start;y>=base;y--) { var bottom=e.getValue().getBlockState(x,y&15,z); if(isEdgeOccluded(top,bottom)) return y+1; top=bottom; previous=y; }
        }
        if(isEdgeOccluded(top,air)) return previous;
        return minY;
    }
}
