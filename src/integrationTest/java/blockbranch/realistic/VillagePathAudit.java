package blockbranch.realistic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Observe the terrain immediately before the native path write. */
public final class VillagePathAudit {
    public static final java.util.concurrent.atomic.LongAdder PREEXISTING=new java.util.concurrent.atomic.LongAdder();
    // Independent oracle: native content tags and named custom blocks, rather
    // than the production class predicate whose omissions this check must catch.
    public static boolean candidate(BlockState state){
        return !state.isAir()&&!state.is(BlockTags.LEAVES)&&state.getFluidState().isEmpty()&&
            (state.is(BlockTags.FLOWERS)||state.is(BlockTags.REPLACEABLE_BY_TREES)||state.is(Blocks.SWEET_BERRY_BUSH)
            ||state.is(Blocks.BROWN_MUSHROOM)||state.is(Blocks.RED_MUSHROOM)||state.is(MeadowContent.MEADOW_FESCUE)
            ||state.is(SteppeContent.SILVER_SHRUB)||state.is(SandyShoreContent.BEACH_GRASS));
    }
    public static boolean obstruction(BlockState state,LevelReader level,BlockPos pos){return candidate(state)&&!state.canSurvive(level,pos);}
    public static void before(WorldGenLevel level,BlockPos pos,BlockState state){
        if(Boolean.getBoolean("blockbranch.villagePaths")&&state.is(Blocks.DIRT_PATH)&&candidate(level.getBlockState(pos.above())))PREEXISTING.increment();
    }
}
