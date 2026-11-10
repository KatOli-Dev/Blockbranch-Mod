package blockbranch.realistic;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** A beach-grass root column binds at most the upper two metres; uprooting releases sand. */
public final class RootBoundSandBlock extends Block {
    public RootBoundSandBlock(Properties properties) { super(properties); }
    public static boolean bound(BlockGetter level,BlockPos pos) {
        for(int depth=1;depth<=2;depth++) {
            var above=level.getBlockState(pos.above(depth));
            if(above.is(SandyShoreContent.BEACH_GRASS)) return true;
            if(!above.is(SandyShoreContent.ROOT_BOUND_SAND)) return false;
        }
        return false;
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved) {
        if(!bound(level,pos)) level.scheduleTick(pos,this,2);
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,
            Direction direction,BlockPos neighbor,BlockState neighborState,RandomSource random) {
        if(direction==Direction.UP && !bound(level,pos)) ticks.scheduleTick(pos,this,2);
        return state;
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!bound(level,pos)) level.setBlock(pos,Blocks.SAND.defaultBlockState(),3);
    }
}
