package blockbranch.wildlife;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Local, noncombat waterfowl. Appearance is cosmetic and is saved. */
public final class Mallard extends Animal {
    private static final EntityDataAccessor<Boolean> DRAKE=SynchedEntityData.defineId(Mallard.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTIVITY=SynchedEntityData.defineId(Mallard.class,EntityDataSerializers.INT);
    private int flightTicks, flightCooldown;
    public Mallard(EntityType<? extends Mallard> type,Level level){super(type,level);if(!level.isClientSide())setDrake(random.nextBoolean());}
    public static AttributeSupplier.Builder attributes(){return createAnimalAttributes().add(Attributes.MAX_HEALTH,6).add(Attributes.MOVEMENT_SPEED,.24).add(Attributes.FOLLOW_RANGE,16);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(DRAKE,false);b.define(ACTIVITY,0);}
    public boolean isDrake(){return entityData.get(DRAKE);}
    public void setDrake(boolean value){entityData.set(DRAKE,value);}
    public int activity(){return entityData.get(ACTIVITY);}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putBoolean("Drake",isDrake());}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);setDrake(in.getBooleanOr("Drake",false));flightTicks=0;}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this){@Override public boolean canUse(){return isInWater()&&isInFluidDeeperThan(.18,net.minecraft.tags.FluidTags.WATER)||isInLava();}});goalSelector.addGoal(0,new ShortFlight());goalSelector.addGoal(1,new PanicGoal(this,1.4));
        goalSelector.addGoal(2,new WildlifeAvoidGoal<>(this,Wolf.class,10,1.1,1.4));
        goalSelector.addGoal(2,new WildlifeAvoidGoal<>(this,BrownBear.class,10,1.1,1.4));
        goalSelector.addGoal(2,new WildlifeAvoidGoal<>(this,Player.class,8,1.1,1.4,e->e instanceof Player p&&!p.isCreative()&&!p.isSpectator()&&!isFood(p.getMainHandItem())&&!isFood(p.getOffhandItem())));
        goalSelector.addGoal(3,new BreedGoal(this,1));goalSelector.addGoal(4,new TemptGoal(this,1,this::isFood,false));
        goalSelector.addGoal(5,new FollowParentGoal(this,1.1));goalSelector.addGoal(6,new Flock());
        goalSelector.addGoal(7,new QuietActivity());goalSelector.addGoal(8,new SwimWander());goalSelector.addGoal(9,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(9,new RandomLookAroundGoal(this));
    }
    @Override protected void customServerAiStep(ServerLevel level){
        super.customServerAiStep(level);
        if(flightCooldown>0)flightCooldown--;
        if(flightTicks>0){
            entityData.set(ACTIVITY,3);flightTicks--;if(flightTicks>24&&getDeltaMovement().y<.12)setDeltaMovement(getDeltaMovement().add(0,.025,0));
            if(flightTicks==0){entityData.set(ACTIVITY,0);flightCooldown=200+random.nextInt(200);}
        }else if(!isBaby()&&!isInLove()&&flightCooldown==0&&(onGround()||isInWater())&&random.nextInt(1000)==0){
            beginShortFlight();
        }
    }
    boolean beginShortFlight(){
        if(isBaby()||isInLove()||flightTicks>0||!onGround()&&!isInWater())return false;
        double angle=Math.toRadians(getYRot());if(!safeFlightRoute(angle))return false;
        getNavigation().stop();flightTicks=30;entityData.set(ACTIVITY,3);
        setDeltaMovement(-Math.sin(angle)*.18,.35,Math.cos(angle)*.18);syncVelocity=true;return true;
    }
    /** Conservative local corridor: headroom, nearby support/water and no known landing hazards. */
    private boolean safeFlightRoute(double angle){
        double fx=-Math.sin(angle),fz=Math.cos(angle);
        for(int step=0;step<=4;step++)for(double side:new double[]{-.35,0,.35}){
            var base=BlockPos.containing(getX()+fx*step+fz*side,getY(),getZ()+fz*step-fx*side);
            if(!level().hasChunkAt(base))return false;
            for(int y=0;y<=3;y++){
                var pos=base.above(y);var state=level().getBlockState(pos);
                if(flightHazard(state)||y>0&&!state.getCollisionShape(level(),pos).isEmpty())return false;
            }
            boolean supported=false;
            for(int down=0;down<=2;down++){
                var pos=base.below(down);var state=level().getBlockState(pos);
                if(flightHazard(state))return false;
                if(state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)||!state.getCollisionShape(level(),pos).isEmpty()){supported=true;break;}
            }
            if(!supported)return false;
        }
        return true;
    }
    private static boolean flightHazard(BlockState state){return state.is(net.minecraft.tags.BlockTags.FIRE)
        ||state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)||state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
        ||state.is(net.minecraft.world.level.block.Blocks.CACTUS)||state.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
        ||state.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)||state.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)
        ||state.is(net.minecraft.world.level.block.Blocks.WITHER_ROSE)||state.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW);}
    private final class ShortFlight extends Goal {
        ShortFlight(){setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){return flightTicks>0;}
        @Override public boolean canContinueToUse(){return flightTicks>0;}
        @Override public void start(){getNavigation().stop();}
    }
    @Override public void aiStep(){super.aiStep();if(!onGround()&&!isInWater()&&getDeltaMovement().y<0)setDeltaMovement(getDeltaMovement().multiply(1,.65,1));}
    @Override public boolean causeFallDamage(double distance,float multiplier,DamageSource source){return false;}
    @Override public boolean canAttack(LivingEntity target){return false;}
    @Override public boolean isFood(ItemStack stack){return stack.is(ItemTags.CHICKEN_FOOD);}
    @Override public Mallard getBreedOffspring(ServerLevel level,AgeableMob partner){return WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.BREEDING);}
    @Override public int getMaxSpawnClusterSize(){return 5;}
    @Override protected SoundEvent getAmbientSound(){return WildlifeSounds.MALLARD_QUACK;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return WildlifeSounds.MALLARD_HURT;}
    @Override protected SoundEvent getDeathSound(){return WildlifeSounds.MALLARD_DEATH;}
    @Override protected void playStepSound(BlockPos pos,BlockState state){playSound(SoundEvents.CHICKEN_STEP.value(),.15F,.8F);}
    @Override public float getVoicePitch(){return super.getVoicePitch()*.8F;}
    private final class Flock extends Goal {
        private Mallard leader;private int nextPath,expiresAt,retryAt,progressAt;private boolean pathAvailable;
        private net.minecraft.world.phys.Vec3 progressPosition;
        Flock(){setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){if(tickCount<retryAt||isBaby()||isInLove()||getLastHurtByMob()!=null||random.nextInt(40)!=0)return false;
            leader=level().getEntitiesOfClass(Mallard.class,getBoundingBox().inflate(10,3,10),m->m.isAlive()&&!m.isBaby()).stream().min(Comparator.comparing(Mallard::getUUID)).orElse(null);
            return leader!=null&&leader!=Mallard.this&&distanceToSqr(leader)>16;}
        @Override public void start(){expiresAt=tickCount+120;progressAt=tickCount+40;progressPosition=position();nextPath=10;pathAvailable=getNavigation().moveTo(leader,1);}
        @Override public boolean canContinueToUse(){return pathAvailable&&tickCount<expiresAt&&!getNavigation().isDone()&&leader!=null&&leader.isAlive()&&!isInLove()&&getLastHurtByMob()==null&&distanceToSqr(leader)>9&&distanceToSqr(leader)<225;}
        @Override public void tick(){
            if(tickCount>=progressAt){if(position().distanceToSqr(progressPosition)<.04){pathAvailable=false;return;}progressPosition=position();progressAt=tickCount+40;}
            if(--nextPath<=0){nextPath=10;pathAvailable=getNavigation().moveTo(leader,1);}
        }
        @Override public void stop(){leader=null;retryAt=tickCount+60+random.nextInt(40);getNavigation().stop();}
    }
    private final class QuietActivity extends Goal {
        private int remaining;
        QuietActivity(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return flightTicks==0&&(onGround()||isInWater())&&!isInLove()&&getLastHurtByMob()==null&&random.nextInt(180)==0;}
        @Override public void start(){getNavigation().stop();remaining=60+random.nextInt(80);entityData.set(ACTIVITY,random.nextBoolean()?1:2);}
        @Override public boolean canContinueToUse(){return remaining>0&&flightTicks==0&&!isInLove()&&getLastHurtByMob()==null;}
        @Override public void tick(){remaining--;}
        @Override public void stop(){if(flightTicks==0)entityData.set(ACTIVITY,0);}
    }
    private final class SwimWander extends Goal {
        private int remaining;private double dx,dz;
        SwimWander(){setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){return isInWater()&&flightTicks==0&&!isInLove()&&random.nextInt(40)==0;}
        @Override public void start(){remaining=30+random.nextInt(30);double angle=random.nextDouble()*Math.PI*2;dx=Math.sin(angle)*.025;dz=Math.cos(angle)*.025;setYRot((float)Math.toDegrees(Math.atan2(-dx,dz)));setYBodyRot(getYRot());setYHeadRot(getYRot());}
        @Override public boolean canContinueToUse(){return remaining>0&&isInWater()&&flightTicks==0&&!isInLove();}
        @Override public void tick(){remaining--;setDeltaMovement(getDeltaMovement().add(dx,0,dz));}
    }
}
