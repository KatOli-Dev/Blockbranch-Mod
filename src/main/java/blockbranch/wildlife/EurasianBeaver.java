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

/** Local freshwater herbivore; no block harvesting, dam building or terrain edits. */
public final class EurasianBeaver extends Animal {
    private static final EntityDataAccessor<Integer> ACTIVITY=SynchedEntityData.defineId(EurasianBeaver.class,EntityDataSerializers.INT);
    public EurasianBeaver(EntityType<? extends EurasianBeaver> type,Level level){super(type,level);setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER,0);}
    public static AttributeSupplier.Builder attributes(){return createAnimalAttributes().add(Attributes.MAX_HEALTH,12).add(Attributes.MOVEMENT_SPEED,.22).add(Attributes.FOLLOW_RANGE,16);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ACTIVITY,0);}
    public int activity(){return entityData.get(ACTIVITY);}
    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level){return new net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation(this,level);}
    @Override public int getMaxAirSupply(){return 1200;}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this){
            @Override public boolean canUse(){return isInWater()||isInLava();}
            @Override public void tick(){
                if(isInLava()){getJumpControl().jump();return;}
                // Surface buoyancy instead of repeated ground jumps from shallow water.
                double depth=getFluidHeight(net.minecraft.tags.FluidTags.WATER);
                var velocity=getDeltaMovement();if(velocity.y>.12)return; // Preserve native bank-exit jumps and external impulses.
                setDeltaMovement(velocity.x,Math.clamp(.025+(depth-.22)*.2,-.06,.06),velocity.z);
            }
        });goalSelector.addGoal(1,new PanicGoal(this,1.4));
        goalSelector.addGoal(2,new WildlifeAvoidGoal<>(this,Wolf.class,10,1.1,1.4));
        goalSelector.addGoal(2,new WildlifeAvoidGoal<>(this,BrownBear.class,10,1.1,1.4));
        goalSelector.addGoal(2,new WildlifeAvoidGoal<>(this,Player.class,8,1.1,1.4,e->e instanceof Player p&&!p.isCreative()&&!p.isSpectator()&&!isFood(p.getMainHandItem())&&!isFood(p.getOffhandItem())));
        goalSelector.addGoal(3,new BreedGoal(this,1));goalSelector.addGoal(4,new TemptGoal(this,1,this::isFood,false));
        goalSelector.addGoal(5,new FollowParentGoal(this,1.1));goalSelector.addGoal(6,new WaterVisit());goalSelector.addGoal(7,new Family());
        goalSelector.addGoal(8,new QuietActivity());goalSelector.addGoal(9,new SwimWander());goalSelector.addGoal(10,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(11,new RandomLookAroundGoal(this));
    }
    @Override public boolean canAttack(LivingEntity target){return false;}
    @Override public boolean isFood(ItemStack stack){return stack.is(net.minecraft.world.item.Items.APPLE)||stack.is(net.minecraft.world.item.Items.CARROT)||stack.is(net.minecraft.world.item.Items.OAK_SAPLING)||stack.is(net.minecraft.world.item.Items.BIRCH_SAPLING);}
    @Override public EurasianBeaver getBreedOffspring(ServerLevel level,AgeableMob partner){return WildlifeRuntime.EURASIAN_BEAVER.create(level,EntitySpawnReason.BREEDING);}
    @Override public int getMaxSpawnClusterSize(){return 3;}
    @Override protected SoundEvent getAmbientSound(){return WildlifeSounds.BEAVER_CONTACT;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return WildlifeSounds.BEAVER_HURT;}
    @Override protected SoundEvent getDeathSound(){return WildlifeSounds.BEAVER_DEATH;}
    @Override protected void playStepSound(BlockPos pos,BlockState state){playSound(SoundEvents.RABBIT_JUMP,.15F,.8F);}
    @Override public float getVoicePitch(){return super.getVoicePitch()*.8F;}
    /** A local visit to existing water only: never changes a block or creates a ticket. */
    private final class WaterVisit extends Goal {
        private BlockPos water;private int retryAt,expiresAt;private boolean path;
        WaterVisit(){setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){
            if(tickCount<retryAt||isInWater()||isInLove()||getLastHurtByMob()!=null||random.nextInt(100)!=0)return false;
            retryAt=tickCount+240;water=null;var origin=blockPosition();int pathAttempts=0;
            for(int r=2;r<=6;r++)for(int z=-r;z<=r;z++)for(int x=-r;x<=r;x++)if(Math.abs(x)==r||Math.abs(z)==r)for(int y=-2;y<=1;y++){
                var pos=origin.offset(x,y,z);if(!level().hasChunkAt(pos)||!level().getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)||!level().getBlockState(pos.above()).isAir())continue;
                if(++pathAttempts>16)return false;
                var route=getNavigation().createPath(pos,0);if(route!=null&&route.canReach()){water=pos;return true;}
            }
            return false;
        }
        @Override public void start(){expiresAt=tickCount+100;path=getNavigation().moveTo(water.getX()+.5,water.getY()+.5,water.getZ()+.5,1);}
        @Override public boolean canContinueToUse(){return path&&!isInWater()&&!isInLove()&&getLastHurtByMob()==null&&tickCount<expiresAt&&!getNavigation().isDone();}
        @Override public void stop(){getNavigation().stop();water=null;}
    }
    private final class Family extends Goal {
        private EurasianBeaver leader;private int nextPath,expiresAt,retryAt,progressAt;private boolean pathAvailable;
        private net.minecraft.world.phys.Vec3 progressPosition;
        Family(){setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){if(tickCount<retryAt||isBaby()||isInLove()||getLastHurtByMob()!=null||random.nextInt(40)!=0)return false;
            leader=level().getEntitiesOfClass(EurasianBeaver.class,getBoundingBox().inflate(10,3,10),m->m.isAlive()&&!m.isBaby()).stream().min(Comparator.comparing(EurasianBeaver::getUUID)).orElse(null);
            return leader!=null&&leader!=EurasianBeaver.this&&distanceToSqr(leader)>16;}
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
        @Override public boolean canUse(){return (onGround()||isInWater())&&!isInLove()&&getLastHurtByMob()==null&&random.nextInt(180)==0;}
        @Override public void start(){getNavigation().stop();remaining=60+random.nextInt(80);entityData.set(ACTIVITY,random.nextBoolean()?1:2);}
        @Override public boolean canContinueToUse(){return remaining>0&&!isInLove()&&getLastHurtByMob()==null;}
        @Override public void tick(){remaining--;}
        @Override public void stop(){entityData.set(ACTIVITY,0);}
    }
    private final class SwimWander extends Goal {
        private int remaining;private double dx,dz;
        SwimWander(){setFlags(EnumSet.of(Flag.MOVE));}
        @Override public boolean canUse(){return isInWater()&&!isInLove()&&random.nextInt(40)==0;}
        @Override public void start(){remaining=30+random.nextInt(30);double angle=random.nextDouble()*Math.PI*2;dx=Math.sin(angle)*.035;dz=Math.cos(angle)*.035;setYRot((float)Math.toDegrees(Math.atan2(-dx,dz)));setYBodyRot(getYRot());setYHeadRot(getYRot());}
        @Override public boolean canContinueToUse(){return remaining>0&&isInWater()&&!isInLove();}
        @Override public void tick(){remaining--;setDeltaMovement(getDeltaMovement().add(dx,0,dz));}
    }
}
