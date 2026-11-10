package blockbranch.realistic;

import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Pioneer backshore grass; planting binds two metres of dry sand without random ticking. */
public final class BeachGrassBlock extends Block {
    public BeachGrassBlock(Properties properties) { super(properties); }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Block.column(12,0,14); }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        var below=level.getBlockState(pos.below());
        return (below.is(Blocks.SAND) || below.is(SandyShoreContent.ROOT_BOUND_SAND)) && level.getFluidState(pos).isEmpty();
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved) {
        if(level.isClientSide()) return;
        for(int depth=1;depth<=2;depth++) {
            var below=pos.below(depth);var substrate=level.getBlockState(below);
            if(substrate.is(Blocks.SAND)) level.setBlock(below,SandyShoreContent.ROOT_BOUND_SAND.defaultBlockState(),3);
            else if(!substrate.is(SandyShoreContent.ROOT_BOUND_SAND)) break;
        }
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,
            Direction direction,BlockPos neighbor,BlockState neighborState,RandomSource random) {
        return canSurvive(state,level,pos) ? state : Blocks.AIR.defaultBlockState();
    }
}
