package blockbranch.realistic;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;

/** Water-preserving emergent stalks. A submerged mud root supports at most four blocks. */
public final class LakeReedBlock extends Block implements SimpleWaterloggedBlock, BonemealableBlock {
    public static final BooleanProperty WATERLOGGED=BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty TIP=BooleanProperty.create("tip");
    public LakeReedBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED,false).setValue(TIP,true));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(WATERLOGGED,TIP); }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Block.column(12,0,16).move(state.getOffset(pos)); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
    }
    @Override protected FluidState getFluidState(BlockState state) { return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state); }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        var cursor=pos; var root=state;
        for(int length=1;length<=4;length++) {
            var below=level.getBlockState(cursor.below());
            if(below.is(Blocks.MUD)) return root.getValue(WATERLOGGED);
            if(!below.is(this)) return false;
            cursor=cursor.below(); root=below;
        }
        return false;
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,
            Direction direction,BlockPos neighbor,BlockState neighborState,RandomSource random) {
        if(!canSurvive(state,level,pos)) return state.getFluidState().createLegacyBlock();
        if(state.getValue(WATERLOGGED)) ticks.scheduleTick(pos,Fluids.WATER,Fluids.WATER.getTickDelay(level));
        return direction==Direction.UP ? state.setValue(TIP,!neighborState.is(this)) : state;
    }
    private BlockPos top(LevelReader level,BlockPos pos) {
        var top=pos;
        for(int i=0;i<3 && level.getBlockState(top.above()).is(this);i++) top=top.above();
        return top;
    }
    @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state,BonemealSource source) {
        var target=top(level,pos).above(); var current=level.getBlockState(target);
        var next=defaultBlockState().setValue(WATERLOGGED,current.getFluidState().is(Fluids.WATER));
        return (current.isAir() || current.is(Blocks.WATER)) && canSurvive(next,level,target);
    }
    @Override public boolean isBonemealSuccess(Level level,RandomSource random,BlockPos pos,BlockState state,BonemealSource source) { return true; }
    @Override public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state,BonemealSource source) {
        if(!isValidBonemealTarget(level,pos,state,source)) return;
        var target=top(level,pos).above();
        level.setBlock(target,defaultBlockState().setValue(WATERLOGGED,level.getFluidState(target).is(Fluids.WATER)),3);
    }
}
