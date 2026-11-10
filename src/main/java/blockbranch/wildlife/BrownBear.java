package blockbranch.wildlife;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Solitary local omnivore. Food satisfies hunting, never grants ownership or pacifies defense. */
public final class BrownBear extends Animal {
    private static final EntityDataAccessor<Integer> ACTIVITY=SynchedEntityData.defineId(BrownBear.class,EntityDataSerializers.INT);
    private int huntCooldown=600+random.nextInt(600);
    private boolean hunting;
    public BrownBear(EntityType<? extends BrownBear> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return createAnimalAttributes().add(Attributes.MAX_HEALTH,32)
        .add(Attributes.MOVEMENT_SPEED,.25).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.ATTACK_KNOCKBACK,.5)
        .add(Attributes.KNOCKBACK_RESISTANCE,.3).add(Attributes.FOLLOW_RANGE,20);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTIVITY,0);}
    public int activity(){return entityData.get(ACTIVITY);}
    public int huntCooldown(){return huntCooldown;}
    void setHuntCooldown(int value){huntCooldown=Math.clamp(value,0,12000);}
    @Override protected void addAdditionalSaveData(ValueOutput output){super.addAdditionalSaveData(output);output.putInt("HuntCooldown",huntCooldown);}
    @Override protected void readAdditionalSaveData(ValueInput input){super.readAdditionalSaveData(input);setHuntCooldown(input.getIntOr("HuntCooldown",600));hunting=false;}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new PanicGoal(this,1.5){@Override protected boolean shouldPanic(){return isOnFire()||getLastHurtByMob()!=null&&(isBaby()||!canAttack(getLastHurtByMob()));}});
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.4,true));goalSelector.addGoal(3,new BreedGoal(this,1));
        goalSelector.addGoal(4,new GroundFood());goalSelector.addGoal(5,new TemptGoal(this,1,this::isFood,false));
        goalSelector.addGoal(6,new FollowParentGoal(this,1.1));goalSelector.addGoal(7,new QuietActivity());
        goalSelector.addGoal(8,new WaterAvoidingRandomStrollGoal(this,.75));goalSelector.addGoal(9,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(10,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new DefendCub());targetSelector.addGoal(3,new LocalHunt());
    }
    @Override public boolean canAttack(LivingEntity target){return !isBaby()&&level().getDifficulty()!=Difficulty.PEACEFUL&&WildlifeCombat.permits(this,target)&&super.canAttack(target);}
    @Override protected void customServerAiStep(ServerLevel level){
        if(huntCooldown>0)huntCooldown--;
        if(getTarget()!=null&&!canAttack(getTarget()))setTarget(null);
        super.customServerAiStep(level);
    }
    @Override public boolean isFood(ItemStack food){return food.is(Items.SWEET_BERRIES)||food.is(Items.SALMON)||food.is(Items.COD);}
    static boolean groundFood(ItemStack food){return food.is(WildlifeItems.PERCH)||food.is(WildlifeItems.COOKED_PERCH)||food.is(Items.SWEET_BERRIES)||food.is(Items.SALMON)||food.is(Items.COD)
        ||food.is(Items.RABBIT)||food.is(Items.CHICKEN)||food.is(Items.PORKCHOP)||food.is(Items.BEEF)||food.is(WildlifeItems.VENISON)
        ||food.is(Items.COOKED_SALMON)||food.is(Items.COOKED_COD)||food.is(Items.COOKED_RABBIT)||food.is(Items.COOKED_CHICKEN)
        ||food.is(Items.COOKED_PORKCHOP)||food.is(Items.COOKED_BEEF)||food.is(WildlifeItems.COOKED_VENISON);}
    private void satisfyHunger(){setHuntCooldown(2400);heal(2);if(hunting)setTarget(null);}
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);boolean food=isFood(stack);var result=super.mobInteract(player,hand);
        if(food&&!level().isClientSide()){
            if(result.consumesAction())satisfyHunger();
            else if(!isBaby()&&(getHealth()<getMaxHealth()||huntCooldown<600)){
                usePlayerItem(player,hand,stack);satisfyHunger();playEatingSound();return InteractionResult.SUCCESS_SERVER;
            }
        }
        return result;
    }
    @Override public BrownBear getBreedOffspring(ServerLevel level,AgeableMob partner){return WildlifeRuntime.BROWN_BEAR.create(level,EntitySpawnReason.BREEDING);}
    @Override public int getMaxSpawnClusterSize(){return 2;}
    @Override protected SoundEvent getAmbientSound(){return isBaby()?SoundEvents.POLAR_BEAR_AMBIENT_BABY:SoundEvents.POLAR_BEAR_AMBIENT;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.POLAR_BEAR_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.POLAR_BEAR_DEATH;}
    @Override protected void playEatingSound(){playSound(SoundEvents.GENERIC_EAT.value(),.6F,.8F);}
    @Override protected void playStepSound(BlockPos pos,BlockState state){playSound(SoundEvents.POLAR_BEAR_STEP,.2F,.9F);}
    @Override public float getVoicePitch(){return super.getVoicePitch()*.9F;}
    @Override public int getAmbientSoundInterval(){return 240;}
    private boolean prey(LivingEntity other){
        if(!other.isAlive()||other.hasCustomName()||other instanceof Mob mob&&mob.isPersistenceRequired())return false;
        return other instanceof Rabbit||other instanceof Chicken||other instanceof AbstractFish
            ||other instanceof RedDeer deer&&deer.isBaby()||other instanceof WildBoar boar&&boar.isBaby()
            ||other instanceof AmericanBison bison&&bison.isBaby();
    }
    private final class LocalHunt extends Goal {
        private LivingEntity victim;private int remaining,nextScan=getId()%20;
        LocalHunt(){setFlags(EnumSet.of(Flag.TARGET));}
        @Override public boolean canUse(){
            if(isBaby()||isInLove()||huntCooldown>0||level().getDifficulty()==Difficulty.PEACEFUL||tickCount<nextScan)return false;
            nextScan=tickCount+20;victim=level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(12,4,12),other->prey(other)&&canAttack(other)&&getSensing().hasLineOfSight(other))
                .stream().min(Comparator.comparingDouble(BrownBear.this::distanceToSqr)).orElse(null);return victim!=null;
        }
        @Override public void start(){hunting=true;remaining=200;setTarget(victim);}
        @Override public boolean canContinueToUse(){return --remaining>0&&huntCooldown==0&&victim!=null&&victim.isAlive()&&canAttack(victim)&&distanceToSqr(victim)<256&&getTarget()==victim;}
        @Override public void stop(){setHuntCooldown(Math.max(huntCooldown,victim!=null&&!victim.isAlive()?2400:600));if(getTarget()==victim)setTarget(null);victim=null;hunting=false;}
    }
    private final class DefendCub extends Goal {
        private LivingEntity threat;private int remaining,nextScan=getId()%10;
        DefendCub(){setFlags(EnumSet.of(Flag.TARGET));}
        @Override public boolean canUse(){
            if(isBaby()||tickCount<nextScan)return false;nextScan=tickCount+10;threat=null;
            for(var cub:level().getEntitiesOfClass(BrownBear.class,getBoundingBox().inflate(8,4,8),other->other.isAlive()&&other.isBaby())){
                var attacker=cub.getLastHurtByMob();
                if(attacker!=null&&cub.tickCount-cub.getLastHurtByMobTimestamp()<100&&canAttack(attacker)&&distanceToSqr(attacker)<144&&getSensing().hasLineOfSight(attacker)){threat=attacker;return true;}
                for(var player:level().getEntitiesOfClass(Player.class,cub.getBoundingBox().inflate(3),other->!other.isCreative()&&!other.isSpectator()&&canAttack(other)))
                    if(distanceToSqr(player)<144&&getSensing().hasLineOfSight(player)){threat=player;return true;}
            }
            return false;
        }
        @Override public void start(){remaining=160;setTarget(threat);playSound(SoundEvents.POLAR_BEAR_WARNING,.8F,.9F);}
        @Override public boolean canContinueToUse(){return --remaining>0&&threat!=null&&threat.isAlive()&&canAttack(threat)&&distanceToSqr(threat)<225;}
        @Override public void stop(){if(getTarget()==threat)setTarget(null);threat=null;}
    }
    private final class GroundFood extends Goal {
        private ItemEntity food;private int remaining,nextScan=getId()%20;
        GroundFood(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){
            if(getTarget()!=null||isInLove()||huntCooldown>=1200||tickCount<nextScan)return false;
            nextScan=tickCount+20;food=level().getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(8,3,8),item->item.isAlive()&&groundFood(item.getItem()))
                .stream().min(Comparator.comparingDouble(BrownBear.this::distanceToSqr)).orElse(null);return food!=null;
        }
        @Override public void start(){remaining=120;entityData.set(ACTIVITY,1);}
        @Override public boolean canContinueToUse(){return remaining>0&&food!=null&&food.isAlive()&&groundFood(food.getItem())&&getTarget()==null&&!isInLove();}
        @Override public void tick(){--remaining;getLookControl().setLookAt(food,30,30);getNavigation().moveTo(food,1);
            if(distanceToSqr(food)<2.25){food.getItem().shrink(1);if(food.getItem().isEmpty())food.discard();satisfyHunger();playEatingSound();remaining=0;}}
        @Override public void stop(){food=null;getNavigation().stop();entityData.set(ACTIVITY,0);}
    }
    private final class QuietActivity extends Goal {
        private int remaining;
        QuietActivity(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return onGround()&&!isInWater()&&!isInLove()&&getTarget()==null&&random.nextInt(180)==0;}
        @Override public void start(){getNavigation().stop();remaining=60+random.nextInt(100);entityData.set(ACTIVITY,level().getBlockState(blockPosition().below()).is(Blocks.GRASS_BLOCK)&&random.nextBoolean()?1:2);}
        @Override public boolean canContinueToUse(){return remaining>0&&onGround()&&!isInWater()&&!isInLove()&&getTarget()==null;}
        @Override public void tick(){remaining--;}
        @Override public void stop(){entityData.set(ACTIVITY,0);}
    }
}
