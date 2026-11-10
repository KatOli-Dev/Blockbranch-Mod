package blockbranch.wildlife;

import blockbranch.Blockbranch;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native utility entry points and an observed hot-difficulty wolf scenario; isolated fixture only. */
final class VanillaUtilitySmoke {
    private static ServerLevel level;
    private static Chicken chicken;
    private static Bee bee;
    private static CountingWolf wolf;
    private static TestPlayer victim;
    private static int ticks,phase,phaseTicks,normalBeforeResume;
    private static boolean active,egg;
    private static final List<String> results=new ArrayList<>();
    private static final class TestPlayer extends Player {
        TestPlayer(String name){super(level,new GameProfile(UUID.randomUUID(),name));}
        @Override public GameType gameMode(){return GameType.SURVIVAL;}
    }
    private static final class CountingWolf extends Wolf {
        final int[] attempts=new int[4];
        CountingWolf(){super(EntityTypes.WOLF,level);}
        @Override public boolean doHurtTarget(ServerLevel world,Entity target){attempts[world.getDifficulty().getId()]++;return super.doHurtTarget(world,target);}
    }
    static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
            level=server.overworld();for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++)level.setChunkForced(x,z,true);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
            for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(entity instanceof Mob||entity instanceof Player||entity instanceof ItemEntity)entity.discard();
            for(var mode:Difficulty.values()){server.setDifficulty(mode,true);interactions(mode);ownerDefense(mode);}
            server.setDifficulty(Difficulty.NORMAL,true);
            chicken=EntityTypes.CHICKEN.create(level,EntitySpawnReason.COMMAND);chicken.snapTo(-12.5,-60,-16.5,0,0);chicken.eggTime=1;level.addFreshEntity(chicken);
            var flower=new BlockPos(12,-60,16);level.setBlock(flower,Blocks.DANDELION.defaultBlockState(),2);
            bee=EntityTypes.BEE.create(level,EntitySpawnReason.COMMAND);bee.snapTo(12.5,-59,16.5,0,0);bee.setSavedFlowerPos(flower);bee.getRandom().setSeed(42);level.addFreshEntity(bee);
            var cooldown=Bee.class.getDeclaredField("remainingCooldownBeforeLocatingNewFlower");cooldown.setAccessible(true);cooldown.setInt(bee,0);
            active=true;
        }catch(Throwable error){fail(error);}});
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!active)return;try{
            ++ticks;++phaseTicks;
            if(phase==0){
                egg|=level.getEntitiesOfClass(ItemEntity.class,chicken.getBoundingBox().inflate(24)).stream().anyMatch(item->item.getItem().is(Items.EGG)||item.getItem().is(Items.BLUE_EGG)||item.getItem().is(Items.BROWN_EGG));
                if(phaseTicks%200==0)Blockbranch.LOGGER.info("VANILLA_UTILITY_POLLINATION ticks={} nectar={} flower={} egg={}",phaseTicks,bee.hasNectar(),bee.getSavedFlowerPos(),egg);
                if(!bee.hasNectar()){if(phaseTicks>1200)throw new AssertionError("native bee pollination did not obtain nectar");return;}
                require(egg,"normal native chicken lays usable egg");results.add("native egg laying and bee pollination PASS ticks="+phaseTicks);
                chicken.discard();bee.discard();startWolf();phase=1;phaseTicks=0;return;
            }
            if(phase==1){
                if(victim.getHealth()==victim.getMaxHealth()){if(phaseTicks>100)throw new AssertionError("native wolf positive control did not attack");return;}
                Blockbranch.LOGGER.info("VANILLA_WOLF_NORMAL_POSITIVE attempts={} health={}",wolf.attempts[2],victim.getHealth());
                server.setDifficulty(Difficulty.EASY,true);victim.setHealth(victim.getMaxHealth());phase=2;phaseTicks=0;return;
            }
            if(phase==2&&phaseTicks>=60){
                results.add("wolf Easy attempts="+wolf.attempts[1]+" health="+victim.getHealth()+" activeTarget="+(wolf.getTarget()==victim)+" angerMatchesPlayer="+(wolf.getPersistentAngerTarget()!=null&&wolf.getPersistentAngerTarget().matches(victim)));
                normalBeforeResume=wolf.attempts[2];server.setDifficulty(Difficulty.NORMAL,true);phase=3;phaseTicks=0;return;
            }
            if(phase==3){
                if(wolf.attempts[2]==normalBeforeResume&&phaseTicks<120)return;
                results.add("wolf Normal resumes="+(wolf.attempts[2]>normalBeforeResume)+" ticks="+phaseTicks);
                victim.setHealth(victim.getMaxHealth());server.setDifficulty(Difficulty.PEACEFUL,true);phase=4;phaseTicks=0;return;
            }
            if(phase==4&&phaseTicks>=60){
                results.add("wolf Peaceful attempts="+wolf.attempts[0]+" health="+victim.getHealth()+" activeTarget="+(wolf.getTarget()==victim)+" angerMatchesPlayer="+(wolf.getPersistentAngerTarget()!=null&&wolf.getPersistentAngerTarget().matches(victim)));
                Blockbranch.LOGGER.info("VANILLA_UTILITY_ANGER_OBSERVATION {}",results);
                require(wolf.attempts[1]==0&&wolf.attempts[0]==0&&victim.getHealth()==victim.getMaxHealth(),"forbidden wolf attack attempts continue: "+results);
                Files.writeString(result(),"PASS\n"+String.join("\n",results)+"\n");active=false;Blockbranch.LOGGER.info("VANILLA_UTILITY_SMOKE PASS ticks={}",ticks);server.halt(false);
            }
        }catch(Throwable error){fail(error);}});
    }
    private static void interactions(Difficulty mode){
        var player=new TestPlayer("NativeUtility");player.snapTo(0,-60,-24,0,0);level.addFreshEntity(player);
        try{
            var horse=EntityTypes.HORSE.create(level,EntitySpawnReason.COMMAND);horse.snapTo(2,-60,-24,0,0);level.addFreshEntity(horse);
            try{
                require(horse.tameWithName(player)&&horse.isTamed(),"native horse taming success path "+mode);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SADDLE));horse.interact(player,InteractionHand.MAIN_HAND,Vec3.ZERO);
                require(horse.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE),"native saddle interaction "+mode);
                player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);horse.interact(player,InteractionHand.MAIN_HAND,Vec3.ZERO);
                require(player.getVehicle()==horse&&horse.getControllingPassenger()==player,"native saddled riding "+mode);player.stopRiding();
            }finally{horse.discard();}
            var donkey=EntityTypes.DONKEY.create(level,EntitySpawnReason.COMMAND);donkey.snapTo(2,-60,-24,0,0);level.addFreshEntity(donkey);
            try{
                donkey.tameWithName(player);int before=donkey.getInventorySize();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.CHEST));donkey.interact(player,InteractionHand.MAIN_HAND,Vec3.ZERO);
                require(donkey.hasChest()&&donkey.getInventorySize()>before&&player.getMainHandItem().isEmpty(),"native donkey chest interaction "+mode);
                require(donkey.getSlot(500).set(new ItemStack(Items.APPLE,3))&&donkey.getSlot(500).get().getCount()==3,"native pack slot stores usable items "+mode);
            }finally{donkey.discard();}
            var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);cow.snapTo(2,-60,-24,0,0);level.addFreshEntity(cow);
            try{player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LEAD));cow.interact(player,InteractionHand.MAIN_HAND,Vec3.ZERO);require(cow.isLeashed()&&cow.getLeashHolder()==player&&player.getMainHandItem().isEmpty(),"native lead interaction "+mode);}finally{cow.discard();}
            var cat=EntityTypes.CAT.create(level,EntitySpawnReason.COMMAND);cat.snapTo(2,-60,-24,0,0);level.addFreshEntity(cat);cat.getRandom().setSeed(42);
            for(int i=0;i<64&&!cat.isTame();i++){player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD));cat.interact(player,InteractionHand.MAIN_HAND,Vec3.ZERO);}
            require(cat.isTame()&&cat.isOwnedBy(player),"native cat fish taming "+mode);cat.discard();
            results.add("native horse tame/saddle/ride, donkey chest/storage, lead, cat tame "+mode+" PASS");
        }finally{player.discard();}
    }
    private static void ownerDefense(Difficulty mode){
        for(boolean playerThreat:new boolean[]{true,false}){
            if(!playerThreat&&mode==Difficulty.PEACEFUL){results.add("native hostile-owner-threat Peaceful N/A: native hostile creation is disabled");continue;}
            var owner=new TestPlayer("DefendedOwner");owner.snapTo(0,-60,-24,0,0);owner.tickCount=1;level.addFreshEntity(owner);
            var guard=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);guard.snapTo(1,-60,-24,0,0);level.addFreshEntity(guard);guard.tame(owner);
            LivingEntity attacker=playerThreat?new TestPlayer("PlayerThreat"):EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);attacker.snapTo(2,-60,-24,0,0);
            try{
                boolean injured=owner.hurtServer(level,playerThreat?level.damageSources().playerAttack((Player)attacker):level.damageSources().mobAttack(attacker),2);
                var goal=new net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal(guard);
                boolean canDefend=goal.canUse();boolean expected=playerThreat?mode==Difficulty.NORMAL||mode==Difficulty.HARD:mode!=Difficulty.PEACEFUL;
                require(canDefend==expected,"native owner-defense selection "+mode+" playerThreat="+playerThreat+" hurt="+injured);
                if(canDefend){goal.start();require(guard.getTarget()==attacker,"native owner-defense target");float health=attacker.getHealth();require(guard.doHurtTarget(level,attacker)&&attacker.getHealth()<health,"native owner-defense attack effect "+mode);}
            }finally{guard.discard();owner.discard();attacker.discard();}
        }
        results.add("native owner-defense player/nonanimal threat "+mode+" PASS");
    }
    private static void startWolf(){
        victim=new TestPlayer("WolfTransition");victim.snapTo(.5,-60,.5,0,0);level.addFreshEntity(victim);
        wolf=new CountingWolf();wolf.snapTo(1.5,-60,.5,0,0);wolf.setOnGround(true);level.addFreshEntity(wolf);
        wolf.setPersistentAngerTarget(EntityReference.of((LivingEntity)victim));wolf.setPersistentAngerEndTime(level.getGameTime()+6000);wolf.setTarget(victim);
    }
    private static Path result(){return Path.of(System.getProperty("blockbranch.utilityResult"));}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void fail(Throwable error){active=false;Blockbranch.LOGGER.error("VANILLA_UTILITY_SMOKE FAIL",error);try{Files.writeString(result(),"FAIL "+error+"\n"+String.join("\n",results));}catch(Exception ignored){}if(level!=null)level.getServer().halt(false);}
    private VanillaUtilitySmoke(){}
}
