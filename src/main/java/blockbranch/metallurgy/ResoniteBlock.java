package blockbranch.metallurgy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/** Native block ticks debounce disturbances without retaining worlds or per-position caches. */
public final class ResoniteBlock extends Block {
    public static final int COOLDOWN=10;
    public ResoniteBlock(Properties properties){super(properties);}
    private void resonate(Level level,BlockPos pos,BlockState state,Entity source){
        if(!(level instanceof ServerLevel server)||server.getBlockTicks().hasScheduledTick(pos,this))return;
        server.scheduleTick(pos,this,COOLDOWN);
        server.playSound(null,pos,ResoniteContent.CHIME,SoundSource.BLOCKS,1,.95F+server.getRandom().nextFloat()*.1F);
        server.gameEvent(GameEvent.BLOCK_ACTIVATE,pos,GameEvent.Context.of(source,state));
    }
    @Override protected void attack(BlockState state,Level level,BlockPos pos,Player player){resonate(level,pos,state,player);}
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){resonate(level,pos,state,player);return super.playerWillDestroy(level,pos,state,player);}
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity){resonate(level,pos,state,entity);super.stepOn(level,pos,state,entity);}
    @Override protected void onProjectileHit(Level level,BlockState state,BlockHitResult hit,Projectile projectile){resonate(level,hit.getBlockPos(),state,projectile);}
}
