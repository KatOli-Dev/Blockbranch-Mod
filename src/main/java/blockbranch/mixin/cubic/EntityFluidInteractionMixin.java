package blockbranch.mixin.cubic;
import blockbranch.cubic.*;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(EntityFluidInteraction.class)
abstract class EntityFluidInteractionMixin {
    @Inject(method="hasFluidAndLoaded",at=@At("HEAD"),cancellable=true)
    private static void blockbranch$fluid(Level level,int x0,int y0,int z0,int x1,int y1,int z1,CallbackInfoReturnable<Boolean> cir) {
        if(!ColumnLimits.expanded(level)) return;
        boolean fluid=false;
        for(int z=z0>>4;z<=z1>>4;z++) for(int x=x0>>4;x<=x1>>4;x++) {
            var chunk=level.getChunk(x,z,ChunkStatus.FULL,false);
            if(chunk==null) { cir.setReturnValue(false); return; }
            for(int y=y0>>4;y<=y1>>4;y++) fluid|=((SparseColumn)chunk).blockbranch$read(y).hasFluid();
        }
        cir.setReturnValue(fluid);
    }
}
