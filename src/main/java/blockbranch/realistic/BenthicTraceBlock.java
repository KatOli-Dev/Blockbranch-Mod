package blockbranch.realistic;

import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;

/** Static sedimentary microrelief, not a living animal or a resource deposit. */
public final class BenthicTraceBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED=BlockStateProperties.WATERLOGGED;
    public static final IntegerProperty FORM=IntegerProperty.create("form",0,2);
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public BenthicTraceBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(WATERLOGGED,true).setValue(FORM,0).setValue(FACING,Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(WATERLOGGED,FORM,FACING);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(2,0,2,14,3,14);}
    @Override protected FluidState getFluidState(BlockState s){return s.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(WATERLOGGED,c.getLevel().getFluidState(c.getClickedPos()).is(Fluids.WATER)).setValue(FACING,c.getHorizontalDirection());}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var bed=l.getBlockState(p.below());return s.getValue(WATERLOGGED)&&(bed.is(Blocks.MUD)||bed.is(Blocks.CLAY));}
    @Override protected BlockState updateShape(BlockState s,LevelReader l,ScheduledTickAccess t,BlockPos p,Direction d,BlockPos n,BlockState v,RandomSource r){
        if(!canSurvive(s,l,p))return s.getFluidState().createLegacyBlock();
        t.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));return s;
    }
}
