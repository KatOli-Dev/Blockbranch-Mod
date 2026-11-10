package blockbranch.realistic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/** A stable pebble veneer over compact fines; shovelling disturbs the surface into loose gravel. */
public final class DesertPavementBlock extends Block {
    public DesertPavementBlock(Properties properties){super(properties);}
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!stack.is(ItemTags.SHOVELS))return super.useItemOn(stack,state,level,pos,player,hand,hit);
        if(level instanceof ServerLevel server){disturb(server,pos);stack.hurtAndBreak(1,player,hand.asEquipmentSlot());level.gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE,pos);}
        return InteractionResult.SUCCESS;
    }
    public boolean disturb(ServerLevel level,BlockPos pos){
        if(!level.getBlockState(pos).is(this))return false;
        level.setBlock(pos,Blocks.GRAVEL.defaultBlockState(),3);level.playSound(null,pos,net.minecraft.sounds.SoundEvents.GRAVEL_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,0.8F,0.9F);return true;
    }
}
