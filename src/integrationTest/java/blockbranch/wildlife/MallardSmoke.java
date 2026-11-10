package blockbranch.wildlife;
import blockbranch.Blockbranch;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Native food/cooking, reproduction, flight, variant persistence and bounded load fixture. */
final class MallardSmoke {
    private static final UUID WITNESS=UUID.fromString("35f864f3-903c-454d-8bc0-80daa58bc84b");
    private static final List<Mallard> population=new ArrayList<>();
    private static Mallard first,second,fleeing,flyer,swimmer;
    private static Mallard flockFollower,flockLeader,openFollower,openLeader;private static boolean converged;private static boolean flockStarted,flockReleased;private static int flockRun,maxFlockRun,waterTicks;private static double horizontalSwim;private static net.minecraft.world.phys.Vec3 previousSwim;private static boolean previousWater;
    private static double maxFlightY;private static boolean launched,landed;
    private static net.minecraft.world.entity.animal.wolf.Wolf threat;
    private static boolean active,reload;private static int ticks;
    private static long start,total,max;
    static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
            var level=server.overworld();
            for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++)level.setChunkForced(x,z,true);
            level.getChunk(0,0);
            var duck=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);
            check(duck.getAmbientSound()==WildlifeSounds.MALLARD_QUACK,"ambient uses random-quack event");
            check(duck.getHurtSound(level.damageSources().generic())==WildlifeSounds.MALLARD_HURT&&duck.getDeathSound()==WildlifeSounds.MALLARD_DEATH,"distinct hurt/death hooks retained for approved random pitched mapping");
            for(boolean drake:new boolean[]{false,true})for(int age:new int[]{0,-24000}){
                duck.setDrake(drake);duck.setAge(age);
                check(duck.getAmbientSound()==WildlifeSounds.MALLARD_QUACK&&duck.getHurtSound(level.damageSources().generic())==WildlifeSounds.MALLARD_HURT&&duck.getDeathSound()==WildlifeSounds.MALLARD_DEATH,"all duck appearance/age states share approved event family");
            }
            duck.setAge(0);
            var soundVolume=LivingEntity.class.getDeclaredMethod("getSoundVolume");soundVolume.setAccessible(true);
            check((float)soundVolume.invoke(duck)==1F,"unchanged inherited mallard sound volume");
            duck.setAge(0);for(int i=0;i<32;i++){float pitch=duck.getVoicePitch();check(pitch>=.6399F&&pitch<=.9601F,"existing adult pitch range");}
            duck.setAge(-24000);for(int i=0;i<32;i++){float pitch=duck.getVoicePitch();check(pitch>=1.0399F&&pitch<=1.3601F,"existing duckling pitch range");}duck.setAge(0);
            var soilPosition=new net.minecraft.core.BlockPos(8,-60,8);
            check(!SpawnPlacements.checkSpawnRules(WildlifeRuntime.MALLARD,level,EntitySpawnReason.CHUNK_GENERATION,soilPosition,level.getRandom()),"dry ground rejected");
            var water=soilPosition.offset(2,-1,0);var old=level.getBlockState(water);level.setBlock(water,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
            check(SpawnPlacements.checkSpawnRules(WildlifeRuntime.MALLARD,level,EntitySpawnReason.CHUNK_GENERATION,soilPosition,level.getRandom()),"wet bank accepted");
            for(var floor:List.of(net.minecraft.world.level.block.Blocks.MUD,net.minecraft.world.level.block.Blocks.GRAVEL,net.minecraft.world.level.block.Blocks.SAND,net.minecraft.world.level.block.Blocks.CLAY,net.minecraft.world.level.block.Blocks.STONE)){
                level.setBlock(soilPosition.below(),floor.defaultBlockState(),2);
                check(SpawnPlacements.checkSpawnRules(WildlifeRuntime.MALLARD,level,EntitySpawnReason.CHUNK_GENERATION,soilPosition,level.getRandom())==(floor!=net.minecraft.world.level.block.Blocks.STONE),"native wet-bank substrate "+floor);
            }
            level.setBlock(soilPosition.below(),net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(),2);
            level.setBlock(water,old,2);
            var registry=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
            for(String id:new String[]{"minecraft:swamp","minecraft:mangrove_swamp","minecraft:river","blockbranch:inland_lake_reedbed","blockbranch:gravel_bed_river_margin"}){
                var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.Identifier.parse(id));
                var settings=registry.getOrThrow(key).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,net.minecraft.world.level.biome.MobSpawnSettings.EMPTY);
                var entries=settings.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().filter(e->e.value().type()==WildlifeRuntime.MALLARD).toList();
                check(entries.size()==1&&entries.getFirst().weight()==3&&entries.getFirst().value().count().minInclusive()==2&&entries.getFirst().value().count().maxInclusive()==5,"unique bounded mallard habitat "+id);
            }
            for(var difficulty:Difficulty.values()){
                server.setDifficulty(difficulty,true);
                var victim=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);
                var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);
                var player=new Player(level,new GameProfile(UUID.randomUUID(),"DeerFoodTest")){
                    @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
                };
                check(!duck.canAttack(player)&&!duck.canAttack(wolf),"duck remains noncombatant "+difficulty);
                check(victim.hurtServer(level,level.damageSources().mobAttack(wolf),1)==(difficulty!=Difficulty.PEACEFUL),"actual wolf/duck difficulty damage "+difficulty);
            }
            var eater=new Player(level,new GameProfile(UUID.randomUUID(),"VenisonFoodTest")){
                @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
            };
            for(var item:List.of(WildlifeItems.DUCK,WildlifeItems.COOKED_DUCK)){
                eater.getFoodData().setFoodLevel(10);
                new ItemStack(item).finishUsingItem(level,eater);
                check(eater.getFoodData().getFoodLevel()==(item==WildlifeItems.DUCK?12:16),"actual duck consumption "+item);
            }
            var input=new SingleRecipeInput(new ItemStack(WildlifeItems.DUCK));
            for(var type:List.of(RecipeType.SMELTING,RecipeType.SMOKING,RecipeType.CAMPFIRE_COOKING)){
                var recipe=server.getRecipeManager().getRecipeFor(type,input,level).orElseThrow();
                check(recipe.value().assemble(input).is(WildlifeItems.COOKED_DUCK),"native duck cooking "+type);
            }
            var table=server.reloadableRegistries().getLootTable(duck.getLootTable().orElseThrow());
            for(boolean fire:new boolean[]{false,true}){
                duck.setRemainingFireTicks(fire?100:0);
                var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,duck)
                    .withParameter(LootContextParams.ORIGIN,duck.position()).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().generic()).create(LootContextParamSets.ENTITY);
                var drops=table.getRandomItems(params,73L);
                check(drops.stream().anyMatch(stack->stack.is(fire?WildlifeItems.COOKED_DUCK:WildlifeItems.DUCK)&&stack.getCount()==1),"actual raw/cooked duck loot "+fire);
                check(drops.stream().allMatch(stack->stack.isEmpty()||stack.is(Items.FEATHER)||stack.is(fire?WildlifeItems.COOKED_DUCK:WildlifeItems.DUCK)),"loot material identity");
            }
            duck.setRemainingFireTicks(0);
            var looter=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"MallardLooter"),ClientInformation.createDefault());
            var sword=new ItemStack(Items.IRON_SWORD);sword.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),3);
            looter.setItemInHand(InteractionHand.MAIN_HAND,sword);
            var lootParams=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,duck).withParameter(LootContextParams.ORIGIN,duck.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().playerAttack(looter)).withParameter(LootContextParams.ATTACKING_ENTITY,looter).withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY,looter).create(LootContextParamSets.ENTITY);
            int looted=0;for(long seed=0;seed<16;seed++)for(var stack:table.getRandomItems(lootParams,seed))if(stack.is(WildlifeItems.DUCK))looted+=stack.getCount();
            check(looted>16,"actual Looting III adds usable duck drops total="+looted);
            Path marker=result().resolveSibling("witness-created.txt");reload=Files.exists(marker);
            if(!reload){
                var witness=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);witness.setUUID(WITNESS);
                witness.snapTo(8.5,-60,8.5,0,0);witness.setDrake(true);witness.setNoAi(true);witness.setPersistenceRequired();
                witness.setHealth(4.5F);witness.setCustomName(net.minecraft.network.chat.Component.literal("Saved mallard"));
                level.addFreshEntity(witness);Files.writeString(marker,"created\n");
            }
            first=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);second=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);
            first.snapTo(8.5,-60,14.5,0,0);second.snapTo(9.5,-60,14.5,0,0);first.setDrake(true);second.setDrake(false);
            level.addFreshEntity(first);level.addFreshEntity(second);
            var feeder=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"MallardFeeder"),ClientInformation.createDefault());
            feeder.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WHEAT_SEEDS,2));
            first.mobInteract(feeder,InteractionHand.MAIN_HAND);second.mobInteract(feeder,InteractionHand.MAIN_HAND);
            check(first.isInLove()&&second.isInLove()&&feeder.getMainHandItem().isEmpty(),"native two-adult feeding");
            check(!first.canMate(WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND)),"no cross-species reproduction");
            fleeing=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);fleeing.getRandom().setSeed(113);
            fleeing.snapTo(-8.5,-60,-8.5,0,0);level.addFreshEntity(fleeing);
            threat=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);threat.snapTo(-4.5,-60,-8.5,0,0);threat.setNoAi(true);level.addFreshEntity(threat);
            for(int i=0;i<16;i++){
                var animal=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);animal.snapTo(18+i%4*2,-60,12+i/4*2,0,0);
                level.addFreshEntity(animal);population.add(animal);
            }
            flyer=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);flyer.snapTo(-24.5,-60,-24.5,0,0);flyer.setAge(-24000);level.addFreshEntity(flyer);maxFlightY=-60;
            for(int x=-24;x<=-16;x++)for(int z=16;z<=24;z++)for(int y=-62;y<=-61;y++)level.setBlock(new net.minecraft.core.BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
            swimmer=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);swimmer.snapTo(-20.5,-61,20.5,0,0);level.addFreshEntity(swimmer);
            // Isolate propulsion from optional resting/flocking in this lane; the other
            // flock and interaction lanes retain their complete normal goal sets.
            var swimGoal=swimmer.getGoalSelector().getAvailableGoals().stream().filter(goal->goal.getPriority()==8).findFirst().orElseThrow().getGoal();
            swimmer.removeAllGoals(goal->goal!=swimGoal&&!(goal instanceof net.minecraft.world.entity.ai.goal.FloatGoal));swimmer.getRandom().setSeed(1061);

            flockLeader=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);flockLeader.setUUID(UUID.fromString("00000000-0000-0000-0000-000000000002"));flockLeader.snapTo(-18.5,-60,4.5,0,0);flockLeader.setNoAi(true);level.addFreshEntity(flockLeader);
            flockFollower=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);flockFollower.setUUID(UUID.fromString("00000000-0000-0000-0000-000000000003"));flockFollower.snapTo(-24.5,-60,4.5,0,0);flockFollower.getRandom().setSeed(822);level.addFreshEntity(flockFollower);
            for(int x=-27;x<=-22;x++)for(int z=2;z<=7;z++)if(x==-27||x==-22||z==2||z==7)for(int y=-60;y<=-58;y++)level.setBlock(new net.minecraft.core.BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            openLeader=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);openLeader.setUUID(UUID.fromString("00000000-0000-0000-0000-000000000004"));openLeader.snapTo(1.5,-60,26.5,0,0);openLeader.setNoAi(true);level.addFreshEntity(openLeader);
            openFollower=WildlifeRuntime.MALLARD.create(level,EntitySpawnReason.COMMAND);openFollower.setUUID(UUID.fromString("00000000-0000-0000-0000-000000000005"));openFollower.snapTo(-5.5,-60,26.5,0,0);openFollower.getRandom().setSeed(9167);level.addFreshEntity(openFollower);
            previousSwim=swimmer.position();previousWater=swimmer.isInWater();
            active=true;
        }catch(Throwable error){Blockbranch.LOGGER.error("MALLARD_SMOKE FAIL",error);server.halt(false);}});
        ServerTickEvents.START_SERVER_TICK.register(server->start=System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!active)return;long duration=System.nanoTime()-start;total+=duration;max=Math.max(max,duration);++ticks;
            if(ticks==20){
                flyer.setYRot(0);flyer.setAge(0);check(flyer.onGround(),"flight safety probes require grounded adult");var level=server.overworld();var origin=flyer.blockPosition();
                var overhead=origin.above(2);var oldOverhead=level.getBlockState(overhead);level.setBlock(overhead,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);check(!flyer.beginShortFlight(),"ceiling rejects takeoff");level.setBlock(overhead,oldOverhead,2);
                var obstacle=origin.offset(0,1,2);var oldObstacle=level.getBlockState(obstacle);level.setBlock(obstacle,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);check(!flyer.beginShortFlight(),"forward obstruction rejects takeoff");level.setBlock(obstacle,oldObstacle,2);
                var floor=origin.offset(0,-1,2);var oldFloor=level.getBlockState(floor);
                for(var hazard:List.of(net.minecraft.world.level.block.Blocks.LAVA,net.minecraft.world.level.block.Blocks.MAGMA_BLOCK,net.minecraft.world.level.block.Blocks.CACTUS)){
                    level.setBlock(floor,hazard.defaultBlockState(),2);check(!flyer.beginShortFlight(),"unsafe landing surface rejected "+hazard);level.setBlock(floor,oldFloor,2);
                }
                var lower=level.getBlockState(floor.below());level.setBlock(floor,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);level.setBlock(floor.below(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);check(!flyer.beginShortFlight(),"deep unsupported landing rejected");level.setBlock(floor,oldFloor,2);level.setBlock(floor.below(),lower,2);
                flyer.setDeltaMovement(.3,0,0);launched=flyer.beginShortFlight();
                check(launched&&Math.abs(flyer.getDeltaMovement().x)<.00001&&Math.abs(flyer.getDeltaMovement().z-.18)<.00001,"launch follows validated heading rather than carried momentum");
            }
            boolean inWater=swimmer.isInWater();
            if(inWater){waterTicks++;if(previousWater&&running(swimmer,8)){double dx=swimmer.getX()-previousSwim.x,dz=swimmer.getZ()-previousSwim.z;horizontalSwim+=Math.sqrt(dx*dx+dz*dz);}}
            previousWater=inWater;previousSwim=swimmer.position();
            if(running(flockFollower,6)){flockStarted=true;maxFlockRun=Math.max(maxFlockRun,++flockRun);}else{if(flockStarted)flockReleased=true;flockRun=0;}
            maxFlightY=Math.max(maxFlightY,flyer.getY());if(ticks>30&&maxFlightY> -59.6&&flyer.onGround())landed=true;
            if(openFollower.distanceToSqr(openLeader)<10)converged=true;
            if(ticks<480)return;
            try{
                var level=server.overworld();var witness=level.getEntity(WITNESS);
                check(witness instanceof Mallard,"stored mallard loads through native entity store");var saved=(Mallard)witness;
                check(saved.isDrake()&&saved.isNoAi()&&saved.isPersistenceRequired()&&saved.getHealth()==4.5F&&saved.getCustomName().getString().equals("Saved mallard"),"variant/health/name/persistence reload");
                check(launched&&maxFlightY> -59.6&&maxFlightY< -56&&landed,"actual bounded flight and landing maxY="+maxFlightY+" landed="+landed);
                check(swimmer.isAlive()&&swimmer.getAirSupply()>0&&waterTicks>=40&&horizontalSwim>.5,"actual swim propulsion over water intervals distance="+horizontalSwim+" waterTicks="+waterTicks);
                check(flockStarted&&flockReleased&&maxFlockRun<=125,"unreachable leader releases movement started="+flockStarted+" released="+flockReleased+" maxRun="+maxFlockRun);
                check(converged,"unobstructed flock convergence");
                Blockbranch.LOGGER.info("MALLARD_REVIEW_REGRESSIONS isolatedHorizontalSwim={} waterTicks={} maxBlockedFlockRun={} released={} openConverged={} hazardCases=6",horizontalSwim,waterTicks,maxFlockRun,flockReleased,converged);
                check(first.getAge()>0&&second.getAge()>0&&!first.isInLove(),"native breeding goal and parent cooldown");
                int babies=0;for(var entity:level.getAllEntities())if(entity instanceof Mallard d&&d.isBaby())babies++;
                check(babies==1,"actual offspring count "+babies);
                check(fleeing.distanceToSqr(threat)>64,"native avoidance increased predator distance "+fleeing.position());
                for(var animal:population)check(animal.tickCount>=120,"active herd tick count "+animal.tickCount);
                for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(entity instanceof Mallard&&!entity.getUUID().equals(WITNESS))entity.discard();threat.discard();
                Files.writeString(result(),"PASS\n");
                Blockbranch.LOGGER.info("MALLARD_SMOKE PASS {} food=true cooking=3 loot=true looting=true habitats=5 wetBanks=true offspring=1 predatorAvoidance=true shortFlightLanding=true hazardousTakeoffRejected=true horizontalSwim=true unreachableFlockRecovery=true variantPersistence=true population=16+interactionActors ticks={} meanWholeTickMs={} maxWholeTickMs={}",reload?"RELOAD":"NEW",ticks,total/1e6/ticks,max/1e6);
            }catch(Throwable error){Blockbranch.LOGGER.error("MALLARD_SMOKE FAIL",error);}finally{active=false;server.halt(false);}
        });
    }
    private static boolean running(Mallard actor,int priority){return actor.getGoalSelector().getAvailableGoals().stream().anyMatch(goal->goal.getPriority()==priority&&goal.isRunning());}
    private static Path result(){return Path.of(System.getProperty("blockbranch.mallardSmokeResult"));}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
