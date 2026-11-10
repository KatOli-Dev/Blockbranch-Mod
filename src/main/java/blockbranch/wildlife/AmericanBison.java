package blockbranch.wildlife;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.cow.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Local grassland herd animal; adults defend themselves and nearby injured calves. */
public final class AmericanBison extends Animal {
    private static final EntityDataAccessor<Integer> ACTIVITY=SynchedEntityData.defineId(AmericanBison.class,EntityDataSerializers.INT);
    public AmericanBison(EntityType<? extends AmericanBison> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return createAnimalAttributes().add(Attributes.MAX_HEALTH,40)
        .add(Attributes.MOVEMENT_SPEED,.22).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.ATTACK_KNOCKBACK,1)
        .add(Attributes.KNOCKBACK_RESISTANCE,.65).add(Attributes.FOLLOW_RANGE,20);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTIVITY,0);}
    public int activity(){return entityData.get(ACTIVITY);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new PanicGoal(this,1.5){@Override protected boolean shouldPanic(){return isOnFire()||getLastHurtByMob()!=null&&(isBaby()||!canAttack(getLastHurtByMob()));}});
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.5,true));
        goalSelector.addGoal(3,new BreedGoal(this,1));
        goalSelector.addGoal(4,new TemptGoal(this,1.05,this::isFood,false));
        goalSelector.addGoal(5,new FollowParentGoal(this,1.15));
        goalSelector.addGoal(6,new HerdCohesion());goalSelector.addGoal(7,new QuietActivity());
        goalSelector.addGoal(8,new WaterAvoidingRandomStrollGoal(this,.7));
        goalSelector.addGoal(9,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(10,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new DefendCalf());
    }
    @Override public boolean canAttack(LivingEntity target){return !isBaby()&&WildlifeCombat.permits(this,target)&&super.canAttack(target);}
    @Override protected void customServerAiStep(ServerLevel level){if(getTarget()!=null&&!canAttack(getTarget()))setTarget(null);super.customServerAiStep(level);}
    @Override public boolean isFood(ItemStack stack){return stack.is(Items.WHEAT);}
    @Override public AmericanBison getBreedOffspring(ServerLevel level,AgeableMob partner){return WildlifeRuntime.AMERICAN_BISON.create(level,EntitySpawnReason.BREEDING);}
    @Override public int getMaxSpawnClusterSize(){return 5;}
    private CowSoundVariant sounds(){return SoundEvents.COW_SOUNDS.get(CowSoundVariants.SoundSet.MOODY);}
    @Override protected SoundEvent getAmbientSound(){return sounds().ambientSound().value();}
    @Override protected SoundEvent getHurtSound(DamageSource source){return sounds().hurtSound().value();}
    @Override protected SoundEvent getDeathSound(){return sounds().deathSound().value();}
    @Override protected void playStepSound(BlockPos pos,BlockState state){playSound(sounds().stepSound().value(),.2F,.8F);}
    @Override protected void playEatingSound(){playSound(SoundEvents.HORSE_EAT,.3F,.8F);}
    @Override public int getAmbientSoundInterval(){return 220;}
    @Override public float getVoicePitch(){return super.getVoicePitch()*.75F;}
    @Override protected float getSoundVolume(){return .7F;}
    private final class DefendCalf extends Goal {
        private LivingEntity threat;private int remaining;
        DefendCalf(){setFlags(EnumSet.of(Flag.TARGET));}
        @Override public boolean canUse(){
            if(isBaby()||(tickCount+getId())%10!=0)return false;
            threat=level().getEntitiesOfClass(AmericanBison.class,getBoundingBox().inflate(8,4,8),calf->calf.isAlive()&&calf.isBaby())
                .stream().filter(calf->calf.tickCount-calf.getLastHurtByMobTimestamp()<100)
                .map(AmericanBison::getLastHurtByMob).filter(Objects::nonNull)
                .filter(other->other.isAlive()&&canAttack(other)&&distanceToSqr(other)<144&&getSensing().hasLineOfSight(other))
                .min(Comparator.comparingDouble(AmericanBison.this::distanceToSqr)).orElse(null);
            return threat!=null;
        }
        @Override public void start(){remaining=200;setTarget(threat);}
        @Override public boolean canContinueToUse(){return --remaining>0&&threat!=null&&threat.isAlive()&&canAttack(threat)&&distanceToSqr(threat)<400;}
        @Override public void stop(){if(getTarget()==threat)setTarget(null);threat=null;}
    }
    private final class HerdCohesion extends Goal {
        private AmericanBison leader;private int nextPath;
        HerdCohesion(){setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){
            if(isBaby()||isInLove()||getTarget()!=null||random.nextInt(40)!=0)return false;
            leader=level().getEntitiesOfClass(AmericanBison.class,getBoundingBox().inflate(14,4,14),other->other.isAlive()&&!other.isBaby())
                .stream().min(Comparator.comparing(AmericanBison::getUUID)).orElse(null);
            return leader!=null&&leader!=AmericanBison.this&&distanceToSqr(leader)>36;
        }
        @Override public boolean canContinueToUse(){return leader!=null&&leader.isAlive()&&!isInLove()&&getTarget()==null&&distanceToSqr(leader)>16&&distanceToSqr(leader)<400;}
        @Override public void start(){nextPath=0;}
        @Override public void tick(){if(--nextPath<=0){nextPath=10;getNavigation().moveTo(leader,1);}}
        @Override public void stop(){leader=null;getNavigation().stop();}
    }
    private final class QuietActivity extends Goal {
        private int remaining;
        QuietActivity(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return onGround()&&!isInWater()&&!isInLove()&&getTarget()==null&&random.nextInt(200)==0;}
        @Override public void start(){getNavigation().stop();remaining=60+random.nextInt(100);entityData.set(ACTIVITY,random.nextInt(4)==0?2:1);}
        @Override public boolean canContinueToUse(){return remaining>0&&onGround()&&!isInWater()&&!isInLove()&&getTarget()==null;}
        @Override public void tick(){remaining--;}
        @Override public void stop(){entityData.set(ACTIVITY,0);}
    }
}
