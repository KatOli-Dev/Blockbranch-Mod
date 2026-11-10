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

/** A live dryland shrub with a retained crown after cutting; regrowth is deliberate, not random. */
public final class SteppeShrubBlock extends Block implements BonemealableBlock {
    public static final BooleanProperty CLIPPED=BooleanProperty.create("clipped");
    public SteppeShrubBlock(Properties properties) { super(properties);registerDefaultState(stateDefinition.any().setValue(CLIPPED,true)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(CLIPPED); }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Block.column(12,0,state.getValue(CLIPPED) ? 4 : 13); }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        var below=level.getBlockState(pos.below());
        return (below.is(Blocks.COARSE_DIRT) || below.is(Blocks.DIRT) || below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.GRAVEL)) && level.getFluidState(pos).isEmpty();
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighbor,BlockState neighborState,RandomSource random) {
        return canSurvive(state,level,pos) ? state : Blocks.AIR.defaultBlockState();
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!stack.is(Items.SHEARS) || state.getValue(CLIPPED)) return super.useItemOn(stack,state,level,pos,player,hand,hit);
        if(level instanceof ServerLevel server) {
            clip(server,pos,state);stack.hurtAndBreak(1,player,hand.asEquipmentSlot());
            level.gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.SHEAR,pos);
        }
        return InteractionResult.SUCCESS;
    }
    public boolean clip(ServerLevel level,BlockPos pos,BlockState state) {
        if(!state.is(this) || state.getValue(CLIPPED) || level.getBlockState(pos)!=state) return false;
        level.setBlock(pos,state.setValue(CLIPPED,true),3);Block.popResource(level,pos,new ItemStack(Items.STICK));
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.SHEEP_SHEAR,net.minecraft.sounds.SoundSource.BLOCKS,0.8F,1.1F);return true;
    }
    @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state,BonemealSource source) { return state.getValue(CLIPPED) && canSurvive(state,level,pos); }
    @Override public boolean isBonemealSuccess(Level level,RandomSource random,BlockPos pos,BlockState state,BonemealSource source) { return true; }
    @Override public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state,BonemealSource source) {
        if(isValidBonemealTarget(level,pos,state,source)) level.setBlock(pos,state.setValue(CLIPPED,false),3);
    }
}
