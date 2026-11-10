package blockbranch.mixin.cubic;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(WorldGenRegion.class)
abstract class WorldGenRegionMixin {
    @Inject(method="ensureCanWrite",at=@At("HEAD"),cancellable=true)
    private void blockbranch$writeBounds(net.minecraft.core.BlockPos pos,CallbackInfoReturnable<Boolean> cir) {
        if(pos.getY()<blockbranch$minY() || pos.getY()>=blockbranch$minY()+blockbranch$heightValue()) cir.setReturnValue(false);
    }
    @Inject(method="getBlockState",at=@At("HEAD"),cancellable=true)
    private void blockbranch$readBounds(net.minecraft.core.BlockPos pos,CallbackInfoReturnable<net.minecraft.world.level.block.state.BlockState> cir) {
        if(pos.getY()<blockbranch$minY() || pos.getY()>=blockbranch$minY()+blockbranch$heightValue()) cir.setReturnValue(net.minecraft.world.level.block.Blocks.VOID_AIR.defaultBlockState());
    }
    @Inject(method="getHeight",at=@At("HEAD"),cancellable=true)
    private void blockbranch$nativeStructureHeight(net.minecraft.world.level.levelgen.Heightmap.Types type,int x,int z,CallbackInfoReturnable<Integer> cir){
        if(blockbranch.realistic.OverworldStructures.terrainQueries()&&level.getChunkSource().getGenerator() instanceof blockbranch.realistic.RealisticGenerator g&&g.overworldDefinition()>0)
            cir.setReturnValue(g.getBaseHeight(x,z,type,(WorldGenRegion)(Object)this,level.getChunkSource().randomState()));
    }
    @Inject(method="setBlock",at=@At("RETURN"))
    private void blockbranch$villagePath(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValueZ())blockbranch.realistic.VillagePaths.written(pos,state);
    }
    @Shadow @Final private ServerLevel level;
    @Unique private boolean blockbranch$realistic(){return level.getChunkSource().getGenerator() instanceof blockbranch.realistic.RealisticGenerator;}
    @Unique private int blockbranch$minY(){return blockbranch$realistic()?blockbranch.realistic.TerrainShape.FLOOR:level.dimensionType().minY();}
    @Unique private int blockbranch$heightValue(){return blockbranch$realistic()?blockbranch.realistic.TerrainShape.CEILING-blockbranch.realistic.TerrainShape.FLOOR:level.dimensionType().height();}
    @Inject(method="getMinY", at=@At("HEAD"), cancellable=true)
    private void blockbranch$min(CallbackInfoReturnable<Integer> cir) { cir.setReturnValue(blockbranch$minY()); }
    @Inject(method="getHeight()I", at=@At("HEAD"), cancellable=true)
    private void blockbranch$height(CallbackInfoReturnable<Integer> cir) { cir.setReturnValue(blockbranch$heightValue()); }
}
