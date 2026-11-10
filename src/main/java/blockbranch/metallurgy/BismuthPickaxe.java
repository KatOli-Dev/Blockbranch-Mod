package blockbranch.metallurgy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Native successful mining calls this even when the last use breaks the tool. */
public final class BismuthPickaxe extends Item {
    public BismuthPickaxe(Properties properties){super(properties);}
    @Override public boolean mineBlock(ItemStack stack,Level level,BlockState state,BlockPos pos,LivingEntity miner){
        boolean result=super.mineBlock(stack,level,state,pos,miner);
        if(miner instanceof ServerPlayer player&&!state.isAir()&&state.getDestroySpeed(level,pos)>0)OrePulse.afterMining(player,pos);
        return result;
    }
}
