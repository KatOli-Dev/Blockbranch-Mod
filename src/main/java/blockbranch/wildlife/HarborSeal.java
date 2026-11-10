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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Local coastal pinniped; bounded swimming, hauling out and fish hunting, no terrain edits. */
public final class HarborSeal extends Animal {
    private static final EntityDataAccessor<Integer> ACTIVITY=SynchedEntityData.defineId(HarborSeal.class,EntityDataSerializers.INT);
    public HarborSeal(EntityType<? extends HarborSeal> type,Level level){super(type,level);setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER,0);}
    public static AttributeSupplier.Builder attributes(){return createAnimalAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.2).add(Attributes.FOLLOW_RANGE,16).add(Attributes.ATTACK_DAMAGE,3);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTIVITY,0);}
    public int activity(){return entityData.get(ACTIVITY);}
    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level){return new net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation(this,level);}
    @Override public int getMaxAirSupply(){return 6000;}
    @Override protected int increaseAirSupply(int current){return getMaxAirSupply();}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this){
            @Override public boolean canUse(){return isInLava()||isInWater()&&getAirSupply()<400;}
            @Override public void tick(){
                if(isInLava()){getJumpControl().jump();return;}
                // Surface buoyancy instead of repeated ground jumps from shallow water.
                double depth=getFluidHeight(net.minecraft.tags.FluidTags.WATER);
                var velocity=getDeltaMovement();if(velocity.y>.12)return; // Preserve native bank-exit jumps and external impulses.
                setDeltaMovement(velocity.x,.08,velocity.z);
            }
        });goalSelector.addGoal(1,new PanicGoal(this,1.4){@Override protected boolean shouldPanic(){return isOnFire()||getLastHurtByMob()!=null&&(isBaby()||!canAttack(getLastHurtByMob()));}});
        goalSelector.addGoal(1,new MeleeAttackGoal(this,1.3,true){@Override public boolean canUse(){return !isInWater()&&super.canUse();}@Override public boolean canContinueToUse(){return !isInWater()&&super.canContinueToUse();}});
        targetSelector.addGoal(1,new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));targetSelector.addGoal(2,new FishHunt());
        goalSelector.addGoal(2,new WildlifeAvoidGoal<>(this,Wolf.class,10,1.1,1.4));
        goalSelector.addGoal(2,new WildlifeAvoidGoal<>(this,BrownBear.class,10,1.1,1.4));
        goalSelector.addGoal(2,new WildlifeAvoidGoal<>(this,Player.class,8,1.1,1.4,e->e instanceof Player p&&!p.isCreative()&&!p.isSpectator()&&!isFood(p.getMainHandItem())&&!isFood(p.getOffhandItem())&&(getTarget()!=p||!canAttack(p))));
        goalSelector.addGoal(3,new BreedGoal(this,1));goalSelector.addGoal(4,new TemptGoal(this,1,this::isFood,false));
        goalSelector.addGoal(5,new FollowParentGoal(this,1.1));goalSelector.addGoal(6,new ShoreVisit());goalSelector.addGoal(7,new Family());
        goalSelector.addGoal(8,new QuietActivity());goalSelector.addGoal(9,new SwimWander());goalSelector.addGoal(10,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(11,new RandomLookAroundGoal(this));
    }
    private int huntCooldown=600,attackDelay;
    public int huntCooldown(){return huntCooldown;}
    void setHuntCooldown(int value){huntCooldown=Math.clamp(value,0,12000);}
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out){super.addAdditionalSaveData(out);out.putInt("HuntCooldown",huntCooldown);}
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in){super.readAdditionalSaveData(in);setHuntCooldown(in.getIntOr("HuntCooldown",600));}
    @Override public boolean canAttack(LivingEntity target){return !isBaby()&&level().getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL&&WildlifeCombat.permits(this,target)&&super.canAttack(target);}
    @Override protected void customServerAiStep(ServerLevel level){
        if(huntCooldown>0)huntCooldown--;if(attackDelay>0)attackDelay--;
        var target=getTarget();if(target!=null&&(!target.isAlive()||!canAttack(target))){setTarget(null);target=null;}
        if(isInWater()&&target!=null&&getAirSupply()>400&&distanceToSqr(target)<144){
            getNavigation().stop();
            var direction=target.position().add(0,target.getBbHeight()*.5,0).subtract(position());
            if(direction.lengthSqr()>.01)setDeltaMovement(getDeltaMovement().add(direction.normalize().scale(.045)));
            getLookControl().setLookAt(target,30,30);setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));setYBodyRot(getYRot());
            if(attackDelay==0&&distanceToSqr(target)<Math.pow(getBbWidth()+target.getBbWidth()+.25,2)){doHurtTarget(level,target);attackDelay=20;}
        }
        if(isInWater()&&target==null&&getAirSupply()>400&&getNavigation().isDone()){
            var velocity=getDeltaMovement();if(velocity.y<.12)setDeltaMovement(velocity.x,.025,velocity.z);
        }
        // Native water travel can leave a broad low-bodied swimmer below a bank lip.
        // Only assist a current local navigation path at a horizontal water collision.
        if(isInWater()&&horizontalCollision&&!getNavigation().isDone()){
            var velocity=getDeltaMovement();if(velocity.y<.18)setDeltaMovement(velocity.x,.18,velocity.z);
        }
        super.customServerAiStep(level);
    }
    @Override public boolean isFood(ItemStack stack){return stack.is(net.minecraft.world.item.Items.COD)||stack.is(net.minecraft.world.item.Items.SALMON)||stack.is(WildlifeItems.PERCH);}
    @Override public net.minecraft.world.InteractionResult mobInteract(Player player,net.minecraft.world.InteractionHand hand){
        var stack=player.getItemInHand(hand);boolean food=isFood(stack);var result=super.mobInteract(player,hand);
        if(food&&!level().isClientSide()){
            if(result.consumesAction()){setHuntCooldown(2400);heal(2);}
            else if(!isBaby()&&(getHealth()<getMaxHealth()||huntCooldown<600)){usePlayerItem(player,hand,stack);setHuntCooldown(2400);heal(2);playSound(SoundEvents.GENERIC_EAT.value(),.6F,1);return net.minecraft.world.InteractionResult.SUCCESS_SERVER;}
        }return result;
    }
    @Override public HarborSeal getBreedOffspring(ServerLevel level,AgeableMob partner){return WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.BREEDING);}
    @Override public int getMaxSpawnClusterSize(){return 4;}
    @Override protected SoundEvent getAmbientSound(){return WildlifeSounds.SEAL_CONTACT;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return WildlifeSounds.SEAL_HURT;}
    @Override protected SoundEvent getDeathSound(){return WildlifeSounds.SEAL_DEATH;}
    @Override protected void playStepSound(BlockPos pos,BlockState state){playSound(WildlifeSounds.SEAL_STEP,.15F,.8F);}
    @Override public float getVoicePitch(){return super.getVoicePitch();}
    /** A local visit to existing water only: never changes a block or creates a ticket. */
    private final class ShoreVisit extends Goal {
        private BlockPos water;private int retryAt,expiresAt;private boolean path,startedWet;
        ShoreVisit(){setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){
            if(tickCount<retryAt||getTarget()!=null||isInLove()||getLastHurtByMob()!=null||random.nextInt(100)!=0)return false;
            retryAt=tickCount+480;water=null;var origin=blockPosition();int pathAttempts=0;
            for(int r=2;r<=8;r++)for(int z=-r;z<=r;z++)for(int x=-r;x<=r;x++)if(Math.abs(x)==r||Math.abs(z)==r)for(int y=-2;y<=1;y++){
                var pos=origin.offset(x,y,z);if(!level().hasChunkAt(pos))continue;
                boolean candidate=isInWater()?level().getBlockState(pos).isAir()&&level().getBlockState(pos.below()).isSolidRender():level().getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)&&level().getBlockState(pos.above()).isAir();
                if(!candidate)continue;
                if(++pathAttempts>16)return false;
                var route=getNavigation().createPath(pos,0);if(route!=null&&route.canReach()){water=pos;return true;}
            }
            return false;
        }
        @Override public void start(){startedWet=isInWater();expiresAt=tickCount+160;path=getNavigation().moveTo(water.getX()+.5,water.getY()+.5,water.getZ()+.5,1);}
        @Override public boolean canContinueToUse(){return path&&isInWater()==startedWet&&getTarget()==null&&!isInLove()&&getLastHurtByMob()==null&&tickCount<expiresAt&&!getNavigation().isDone();}
        @Override public void stop(){getNavigation().stop();water=null;}
    }
    private final class FishHunt extends Goal {
        private net.minecraft.world.entity.animal.fish.AbstractFish prey;private int nextScan,expires;
        FishHunt(){setFlags(EnumSet.of(Flag.TARGET));}
        @Override public boolean canUse(){
            if(isBaby()||!isInWater()||isInLove()||huntCooldown>0||getAirSupply()<400||tickCount<nextScan||level().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return false;
            nextScan=tickCount+20;
            prey=level().getEntitiesOfClass(net.minecraft.world.entity.animal.fish.AbstractFish.class,getBoundingBox().inflate(10,4,10),fish->fish.isAlive()&&!fish.hasCustomName()&&!fish.requiresCustomPersistence()&&canAttack(fish)&&getSensing().hasLineOfSight(fish)).stream().min(Comparator.comparingDouble(HarborSeal.this::distanceToSqr)).orElse(null);
            return prey!=null;
        }
        @Override public void start(){expires=tickCount+200;setTarget(prey);}
        @Override public boolean canContinueToUse(){return huntCooldown==0&&tickCount<expires&&prey!=null&&prey.isAlive()&&getTarget()==prey&&canAttack(prey)&&!isInLove()&&getAirSupply()>400&&distanceToSqr(prey)<144;}
        @Override public void stop(){setHuntCooldown(Math.max(huntCooldown,prey!=null&&!prey.isAlive()?2400:600));if(getTarget()==prey)setTarget(null);prey=null;}
    }
    private final class Family extends Goal {
        private HarborSeal leader;private int nextPath,expiresAt,retryAt,progressAt;private boolean pathAvailable;
        private net.minecraft.world.phys.Vec3 progressPosition;
        Family(){setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){if(tickCount<retryAt||getTarget()!=null||isBaby()||isInLove()||getLastHurtByMob()!=null||random.nextInt(40)!=0)return false;
            leader=level().getEntitiesOfClass(HarborSeal.class,getBoundingBox().inflate(10,3,10),m->m.isAlive()&&!m.isBaby()).stream().min(Comparator.comparing(HarborSeal::getUUID)).orElse(null);
            return leader!=null&&leader!=HarborSeal.this&&distanceToSqr(leader)>16;}
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
        @Override public boolean canUse(){return onGround()&&!isInWater()&&getTarget()==null&&!isInLove()&&getLastHurtByMob()==null&&random.nextInt(180)==0;}
        @Override public void start(){getNavigation().stop();remaining=60+random.nextInt(80);entityData.set(ACTIVITY,2);}
        @Override public boolean canContinueToUse(){return remaining>0&&getTarget()==null&&!isInLove()&&getLastHurtByMob()==null;}
        @Override public void tick(){remaining--;}
        @Override public void stop(){entityData.set(ACTIVITY,0);}
    }
    private final class SwimWander extends Goal {
        private int remaining;private double dx,dz;
        SwimWander(){setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){return isInWater()&&getTarget()==null&&getAirSupply()>400&&!isInLove()&&random.nextInt(40)==0;}
        @Override public void start(){remaining=30+random.nextInt(30);double angle=random.nextDouble()*Math.PI*2;dx=Math.sin(angle)*.035;dz=Math.cos(angle)*.035;setYRot((float)Math.toDegrees(Math.atan2(-dx,dz)));setYBodyRot(getYRot());setYHeadRot(getYRot());}
        @Override public boolean canContinueToUse(){return remaining>0&&isInWater()&&getTarget()==null&&getAirSupply()>400&&!isInLove();}
        @Override public void tick(){remaining--;setDeltaMovement(getDeltaMovement().add(dx,0,dz));}
    }
}
