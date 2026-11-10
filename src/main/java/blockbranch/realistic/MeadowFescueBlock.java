package blockbranch.realistic;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** A persistent tussock crown: divide once with shears, then deliberately regrow with bonemeal. */
public final class MeadowFescueBlock extends Block implements BonemealableBlock {
    public static final BooleanProperty CROPPED=BooleanProperty.create("cropped");
    public MeadowFescueBlock(Properties properties) { super(properties);registerDefaultState(stateDefinition.any().setValue(CROPPED,true)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(CROPPED); }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Block.column(12,0,state.getValue(CROPPED) ? 6 : 16).move(state.getOffset(pos)); }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        var below=level.getBlockState(pos.below());
        return (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.COARSE_DIRT)) && level.getFluidState(pos).isEmpty();
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,
            Direction direction,BlockPos neighbor,BlockState neighborState,RandomSource random) {
        return canSurvive(state,level,pos) ? state : Blocks.AIR.defaultBlockState();
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!stack.is(Items.SHEARS) || state.getValue(CROPPED)) return super.useItemOn(stack,state,level,pos,player,hand,hit);
        if(level instanceof ServerLevel server) {
            divide(server,pos,state);
            stack.hurtAndBreak(1,player,hand.asEquipmentSlot());
            level.gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.SHEAR,pos);
        }
        return InteractionResult.SUCCESS;
    }
    /** The common path for real interaction and the opt-in active-client fixture. */
    public boolean divide(ServerLevel level,BlockPos pos,BlockState state) {
        if(!state.is(this) || state.getValue(CROPPED) || level.getBlockState(pos)!=state) return false;
        level.setBlock(pos,state.setValue(CROPPED,true),3);
        Block.popResource(level,pos,new ItemStack(MeadowContent.FESCUE_ITEM));
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.SHEEP_SHEAR,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
        return true;
    }
    @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state,BonemealSource source) { return state.getValue(CROPPED) && canSurvive(state,level,pos); }
    @Override public boolean isBonemealSuccess(Level level,RandomSource random,BlockPos pos,BlockState state,BonemealSource source) { return true; }
    @Override public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state,BonemealSource source) {
        if(isValidBonemealTarget(level,pos,state,source)) level.setBlock(pos,state.setValue(CROPPED,false),3);
    }
}
