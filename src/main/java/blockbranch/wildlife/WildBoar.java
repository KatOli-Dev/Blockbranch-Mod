package blockbranch.wildlife;

import java.util.EnumSet;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/** A wild animal, deliberately without pig saddling, steering or lightning conversion. */
public final class WildBoar extends Animal {
    private static final EntityDataAccessor<Integer> ACTIVITY = SynchedEntityData.defineId(WildBoar.class, EntityDataSerializers.INT);
    public WildBoar(EntityType<? extends WildBoar> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, .25)
            .add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.FOLLOW_RANGE, 16);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(ACTIVITY, 0);
    }
    public int activity() { return entityData.get(ACTIVITY); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.6) {
            @Override protected boolean shouldPanic() { return isOnFire() || isBaby() && getLastHurtByMob() != null; }
        });
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.5, true));
        goalSelector.addGoal(3, new BreedGoal(this, 1));
        goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(6, new QuietActivity());
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, .8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }
    @Override public boolean canAttack(LivingEntity target) {
        return !isBaby() && WildlifeCombat.permits(this, target) && super.canAttack(target);
    }
    @Override protected void customServerAiStep(ServerLevel level) {
        if (getTarget() != null && !canAttack(getTarget())) setTarget(null);
        super.customServerAiStep(level);
    }
    @Override public boolean isFood(ItemStack stack) {
        return stack.is(Items.CARROT) || stack.is(Items.POTATO) || stack.is(Items.BEETROOT);
    }
    @Override public WildBoar getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return WildlifeRuntime.WILD_BOAR.create(level, EntitySpawnReason.BREEDING);
    }
    @Override public int getMaxSpawnClusterSize() { return 4; }
    @Override protected SoundEvent getAmbientSound() { return sounds().ambientSound().value(); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return sounds().hurtSound().value(); }
    @Override protected SoundEvent getDeathSound() { return sounds().deathSound().value(); }
    @Override protected void playEatingSound() { playSound(sounds().eatSound().value(), .7F, getVoicePitch()); }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { playSound(sounds().stepSound().value(), .15F, .7F); }
    private net.minecraft.world.entity.animal.pig.PigSoundVariant.PigSoundSet sounds() {
        var sounds = SoundEvents.PIG_SOUNDS.get(net.minecraft.world.entity.animal.pig.PigSoundVariants.SoundSet.BIG);
        return isBaby() ? sounds.babySounds() : sounds.adultSounds();
    }
    @Override public float getVoicePitch() { return super.getVoicePitch() * .7F; }
    private final class QuietActivity extends Goal {
        private int remaining;
        QuietActivity() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            return getTarget() == null && !isInWater() && onGround() && !isInLove() && random.nextInt(240) == 0;
        }
        @Override public void start() {
            getNavigation().stop(); remaining = 60 + random.nextInt(80);
            // Daytime rest and snout-rooting forage are visual behavior; they consume no terrain.
            entityData.set(ACTIVITY, level().isBrightOutside() && random.nextBoolean() ? 2 : 1);
        }
        @Override public boolean canContinueToUse() { return remaining > 0 && getTarget() == null && !isInWater() && !isInLove(); }
        @Override public void tick() { remaining--; }
        @Override public void stop() { entityData.set(ACTIVITY, 0); }
    }
}
