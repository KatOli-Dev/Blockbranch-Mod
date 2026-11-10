package blockbranch.wildlife;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Woodland herd animal. Appearance variants use ordinary vanilla two-adult breeding. */
public final class RedDeer extends Animal {
    private static final EntityDataAccessor<Boolean> ANTLERS = SynchedEntityData.defineId(RedDeer.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTIVITY = SynchedEntityData.defineId(RedDeer.class, EntityDataSerializers.INT);
    public RedDeer(EntityType<? extends RedDeer> type, Level level) {
        super(type, level);
        if (!level.isClientSide()) setAntlers(random.nextInt(4) == 0);
    }
    public static AttributeSupplier.Builder attributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 18).add(Attributes.MOVEMENT_SPEED, .28)
            .add(Attributes.FOLLOW_RANGE, 20);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);builder.define(ANTLERS, false);builder.define(ACTIVITY, 0);
    }
    public boolean hasAntlers() { return entityData.get(ANTLERS); }
    public void setAntlers(boolean value) { entityData.set(ANTLERS, value); }
    public int activity() { return entityData.get(ACTIVITY); }
    @Override protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);output.putBoolean("Antlers", hasAntlers());
    }
    @Override protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);setAntlers(input.getBooleanOr("Antlers", false));
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.7));
        goalSelector.addGoal(2, new WildlifeAvoidGoal<>(this, Wolf.class, 16, 1.35, 1.7));
        goalSelector.addGoal(2, new WildlifeAvoidGoal<>(this, Player.class, 10, 1.3, 1.7,
            entity -> entity instanceof Player player && !player.isSpectator() && !player.isCreative()
                && !isFood(player.getMainHandItem()) && !isFood(player.getOffhandItem())));
        goalSelector.addGoal(3, new BreedGoal(this, 1));
        goalSelector.addGoal(4, new TemptGoal(this, 1.05, this::isFood, false));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(6, new HerdCohesion());
        goalSelector.addGoal(7, new QuietActivity());
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, .75));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }
    @Override public boolean canAttack(LivingEntity target) { return false; }
    @Override public boolean isFood(ItemStack stack) { return stack.is(Items.WHEAT) || stack.is(Items.APPLE); }
    @Override public RedDeer getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return WildlifeRuntime.RED_DEER.create(level, EntitySpawnReason.BREEDING);
    }
    @Override public int getMaxSpawnClusterSize() { return 6; }
    @Override protected SoundEvent getAmbientSound() {
        return hasAntlers() && !isBaby() && isInLove() ? WildlifeSounds.DEER_BELLOW : WildlifeSounds.DEER_CONTACT;
    }
    @Override public int getAmbientSoundInterval() { return 240; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return WildlifeSounds.DEER_HURT; }
    @Override protected SoundEvent getDeathSound() { return WildlifeSounds.DEER_DEATH; }
    @Override protected void playEatingSound() { playSound(SoundEvents.HORSE_EAT, .3F, 1.1F); }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { playSound(SoundEvents.HORSE_STEP, .15F, 1.2F); }
    @Override protected float getSoundVolume() { return .6F; }

    private final class HerdCohesion extends Goal {
        private RedDeer leader;
        private int nextPath;
        HerdCohesion() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (isBaby() || isInLove() || getLastHurtByMob() != null || random.nextInt(40) != 0) return false;
            leader = level().getEntitiesOfClass(RedDeer.class, getBoundingBox().inflate(12, 4, 12),
                other -> other.isAlive() && !other.isBaby())
                .stream().min(Comparator.comparing(RedDeer::getUUID)).orElse(null);
            return leader != null && leader != RedDeer.this && distanceToSqr(leader) > 25;
        }
        @Override public boolean canContinueToUse() {
            return leader != null && leader.isAlive() && !isInLove() && getLastHurtByMob() == null
                && distanceToSqr(leader) > 9 && distanceToSqr(leader) < 400;
        }
        @Override public void start() { nextPath = 0; }
        @Override public void tick() { if (--nextPath <= 0) { nextPath = 10;getNavigation().moveTo(leader, 1); } }
        @Override public void stop() { leader = null;getNavigation().stop(); }
    }
    private final class QuietActivity extends Goal {
        private int remaining;
        QuietActivity() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            return onGround() && !isInWater() && !isInLove() && getLastHurtByMob() == null && random.nextInt(180) == 0;
        }
        @Override public void start() {
            getNavigation().stop();remaining = 60 + random.nextInt(80);
            boolean grass = level().getBlockState(blockPosition().below()).is(Blocks.GRASS_BLOCK);
            entityData.set(ACTIVITY, grass && random.nextInt(3) != 0 ? 1 : 2);
        }
        @Override public boolean canContinueToUse() {
            return remaining > 0 && onGround() && !isInWater() && !isInLove() && getLastHurtByMob() == null;
        }
        @Override public void tick() { remaining--; }
        @Override public void stop() { entityData.set(ACTIVITY, 0); }
    }
}
