package blockbranch.wildlife;

import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Native freshwater schooling fish: bucketable, passive, no feed-to-breed mechanic. */
public final class EuropeanPerch extends AbstractSchoolingFish {
    public EuropeanPerch(EntityType<? extends EuropeanPerch> type,Level level){super(type,level);}
    @Override protected void registerGoals(){
        super.registerGoals();
        // Native AvoidEntityGoal uses combat targeting; fear must survive our passive policy and Peaceful.
        goalSelector.removeAllGoals(goal->goal instanceof net.minecraft.world.entity.ai.goal.AvoidEntityGoal);
        goalSelector.addGoal(2,new WildlifeAvoidGoal<>(this,net.minecraft.world.entity.player.Player.class,8,1.6,1.4,
            entity->entity instanceof net.minecraft.world.entity.player.Player player&&!player.isCreative()&&!player.isSpectator()));
    }
    @Override public int getMaxSchoolSize(){return 6;}
    @Override public int getMaxSpawnClusterSize(){return 6;}
    @Override public boolean canAttack(LivingEntity target){return false;}
    @Override public ItemStack getBucketItemStack(){return new ItemStack(WildlifeItems.PERCH_BUCKET);}
    @Override protected SoundEvent getAmbientSound(){return SoundEvents.COD_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return WildlifeSounds.PERCH_HURT;}
    @Override protected SoundEvent getDeathSound(){return WildlifeSounds.PERCH_DEATH;}
    @Override protected SoundEvent getFlopSound(){return WildlifeSounds.PERCH_FLOP;}
}
