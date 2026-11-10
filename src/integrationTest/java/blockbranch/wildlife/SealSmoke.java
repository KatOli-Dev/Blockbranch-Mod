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

/** Native feeding, hide loot, reproduction, amphibious movement, reload and bounded-load fixture. */
final class SealSmoke {
    private static final UUID WITNESS=UUID.fromString("1d140675-ad19-4a94-ac97-cb5396a4f905");
    private static final List<HarborSeal> population=new ArrayList<>();
    private static HarborSeal first,second,fleeing,swimmer,traveler,breather,idleSwimmer;
    private static int waterTicks;private static double swimDistance;private static net.minecraft.world.phys.Vec3 previous;private static boolean wasWater,entered,exited,exitRequested,surfaced;
    private static double goalGatedDistance,idleDistance;private static int swimActiveTicks;
    private static net.minecraft.world.phys.Vec3 previousIdle;private static boolean idleWasWater;
    private static net.minecraft.world.entity.animal.wolf.Wolf threat;
    private static HarborSeal hunter;private static net.minecraft.world.entity.animal.fish.Cod prey;private static int huntCase=-1,huntTicks;private static boolean fed;
    private static boolean active,reload;private static int ticks;
    private static final Set<UUID> fixtureActors=new HashSet<>();
    private static float lastHuntPreyHealth;
    private static long start,total,max;
    static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
            var level=server.overworld();
            for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++)level.setChunkForced(x,z,true);
            level.getChunk(0,0);
            resetTransientActors(level);
            // Keep the hunt arena separate from the independently moving fear/herd actors.
            for(int x=-25;x<=-15;x++)for(int z=-25;z<=-15;z++)if(x==-25||x==-15||z==-25||z==-15)
                for(int y=-64;y<=-58;y++)level.setBlock(new net.minecraft.core.BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(),2);
            var probe=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);
            var voiceVolume=LivingEntity.class.getDeclaredMethod("getSoundVolume");voiceVolume.setAccessible(true);
            for(int age:new int[]{0,-24000}){
                probe.setAge(age);
                check(probe.getAmbientSound()==WildlifeSounds.SEAL_CONTACT&&probe.getHurtSound(level.damageSources().generic())==WildlifeSounds.SEAL_HURT&&probe.getDeathSound()==WildlifeSounds.SEAL_DEATH,"adult/pup sound hooks retain separate approved random event mappings");
                check((float)voiceVolume.invoke(probe)==1F,"unchanged native seal sound volume");
                for(int i=0;i<32;i++){float pitch=probe.getVoicePitch();check(age==0?pitch>=.7999F&&pitch<=1.2001F:pitch>=1.2999F&&pitch<=1.7001F,"unchanged adult/pup voice pitch range");}
            }
            probe.setAge(0);
            var bank=new net.minecraft.core.BlockPos(8,-60,8);var water=bank.offset(2,-1,0);level.setBlock(water,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            check(!SpawnPlacements.checkSpawnRules(WildlifeRuntime.HARBOR_SEAL,level,EntitySpawnReason.CHUNK_GENERATION,bank,level.getRandom()),"dry bank rejected");
            level.setBlock(water,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
            check(SpawnPlacements.checkSpawnRules(WildlifeRuntime.HARBOR_SEAL,level,EntitySpawnReason.CHUNK_GENERATION,bank,level.getRandom()),"wet bank accepted");
            level.setBlock(water,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            var registry=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
            for(String id:new String[]{"minecraft:beach","minecraft:stony_shore","minecraft:snowy_beach","blockbranch:low_gradient_sandy_shore"}){
                var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.Identifier.parse(id));
                var settings=registry.getOrThrow(key).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,net.minecraft.world.level.biome.MobSpawnSettings.EMPTY);
                var entries=settings.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().filter(e->e.value().type()==WildlifeRuntime.HARBOR_SEAL).toList();
                check(entries.size()==1&&entries.getFirst().weight()==2&&entries.getFirst().value().count().minInclusive()==2&&entries.getFirst().value().count().maxInclusive()==4,"unique bounded habitat "+id);
            }
            for(var difficulty:Difficulty.values()){
                server.setDifficulty(difficulty,true);
                var victim=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);
                var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);
                var player=new Player(level,new GameProfile(UUID.randomUUID(),"SealFoodTest")){
                    @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
                };
                check(probe.canAttack(player)==(difficulty==Difficulty.NORMAL||difficulty==Difficulty.HARD)&&probe.canAttack(wolf)==(difficulty!=Difficulty.PEACEFUL),"seal difficulty targeting "+difficulty);
                check(victim.hurtServer(level,level.damageSources().mobAttack(wolf),1)==(difficulty!=Difficulty.PEACEFUL),"actual wolf/seal difficulty damage "+difficulty);
            }
            check(probe.isFood(new ItemStack(Items.COD))&&probe.isFood(new ItemStack(WildlifeItems.PERCH))&&!probe.isFood(new ItemStack(Items.STICK)),"raw fish food, not building sticks");
            var table=server.reloadableRegistries().getLootTable(probe.getLootTable().orElseThrow());
            var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,probe).withParameter(LootContextParams.ORIGIN,probe.position()).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().generic()).create(LootContextParamSets.ENTITY);
            for(long seed=0;seed<16;seed++)check(table.getRandomItems(params,seed).stream().allMatch(stack->stack.isEmpty()||stack.is(Items.LEATHER)&&stack.getCount()<=2),"usable hide loot without fabricated wood/fish/meat");
            var looter=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"SealLooter"),ClientInformation.createDefault());
            var sword=new ItemStack(Items.IRON_SWORD);sword.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),3);looter.setItemInHand(InteractionHand.MAIN_HAND,sword);
            var lootParams=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,probe).withParameter(LootContextParams.ORIGIN,probe.position()).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().playerAttack(looter)).withParameter(LootContextParams.ATTACKING_ENTITY,looter).withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY,looter).create(LootContextParamSets.ENTITY);
            int plain=0,enchanted=0;for(long seed=0;seed<32;seed++){for(var item:table.getRandomItems(params,seed))if(item.is(Items.LEATHER))plain+=item.getCount();for(var item:table.getRandomItems(lootParams,seed))if(item.is(Items.LEATHER))enchanted+=item.getCount();}
            check(enchanted>plain,"actual Looting III increases leather "+plain+"/"+enchanted);
            var kit=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);kit.setAge(-24000);looter.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(WildlifeItems.PERCH));kit.mobInteract(looter,InteractionHand.MAIN_HAND);
            check(kit.getAge()>-24000&&kit.isBaby()&&looter.getMainHandItem().isEmpty(),"native kit feeding accelerates growth without taming");
            Path marker=result().resolveSibling("witness-created.txt");reload=Files.exists(marker);
            if(!reload){
                var witness=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);witness.setUUID(WITNESS);
                witness.snapTo(8.5,-60,8.5,0,0);witness.setNoAi(true);witness.setPersistenceRequired();
                witness.setAge(-24000);witness.setHuntCooldown(4200);witness.setHealth(8.5F);witness.setCustomName(net.minecraft.network.chat.Component.literal("Saved seal"));
                level.addFreshEntity(witness);Files.writeString(marker,"created\n");
            }
            first=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);second=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);
            first.snapTo(8.5,-60,14.5,0,0);second.snapTo(9.5,-60,14.5,0,0);
            level.addFreshEntity(first);level.addFreshEntity(second);
            var feeder=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"SealFeeder"),ClientInformation.createDefault());
            feeder.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD,2));
            first.mobInteract(feeder,InteractionHand.MAIN_HAND);second.mobInteract(feeder,InteractionHand.MAIN_HAND);
            check(first.isInLove()&&second.isInLove()&&feeder.getMainHandItem().isEmpty(),"native two-adult feeding");
            check(!first.canMate(WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND)),"no cross-species reproduction");
            fleeing=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);fleeing.getRandom().setSeed(113);
            fleeing.snapTo(-8.5,-60,-8.5,0,0);level.addFreshEntity(fleeing);
            threat=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);threat.snapTo(-4.5,-60,-8.5,0,0);threat.setNoAi(true);level.addFreshEntity(threat);
            for(int i=0;i<16;i++){
                var animal=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);animal.snapTo(18+i%4*2,-60,12+i/4*2,0,0);
                level.addFreshEntity(animal);population.add(animal);
            }
            for(int x=-24;x<=-16;x++)for(int z=16;z<=24;z++)for(int y=-62;y<=-61;y++)level.setBlock(new net.minecraft.core.BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
            swimmer=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);swimmer.snapTo(-20.5,-61,20.5,0,0);level.addFreshEntity(swimmer);
            var swim=swimmer.getGoalSelector().getAvailableGoals().stream().filter(g->g.getPriority()==9).findFirst().orElseThrow().getGoal();swimmer.removeAllGoals(g->g!=swim&&!(g instanceof net.minecraft.world.entity.ai.goal.FloatGoal));swimmer.getRandom().setSeed(1061);previous=swimmer.position();
            // A separate calm pool checks that measured travel is propulsion, not water drift.
            for(int x=-31;x<=-27;x++)for(int z=16;z<=20;z++)for(int y=-62;y<=-61;y++)level.setBlock(new net.minecraft.core.BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
            idleSwimmer=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);idleSwimmer.snapTo(-29,-61,18.5,0,0);level.addFreshEntity(idleSwimmer);
            idleSwimmer.removeAllGoals(g->true);previousIdle=idleSwimmer.position();
            Blockbranch.LOGGER.info("SEAL_SWIM_PROBE entityId={} nativeAiParity={} start={} seed=1061 worldTime={}",swimmer.getId(),swimmer.getId()%2,previous,level.getGameTime());
            breather=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);breather.snapTo(-18.5,-62,22.5,0,0);level.addFreshEntity(breather);breather.setAirSupply(100);breather.removeAllGoals(g->!(g instanceof net.minecraft.world.entity.ai.goal.FloatGoal));
            traveler=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);traveler.snapTo(-20.5,-60,13.5,0,0);level.addFreshEntity(traveler);traveler.removeAllGoals(g->!(g instanceof net.minecraft.world.entity.ai.goal.FloatGoal));
            check(traveler.getNavigation().moveTo(-20.5,-61,20.5,1),"amphibious entry path created");
            for(var actor:List.of(first,second,fleeing,swimmer,traveler,breather,idleSwimmer,threat))fixtureActors.add(actor.getUUID());
            for(var actor:population)fixtureActors.add(actor.getUUID());
            active=true;
        }catch(Throwable error){Blockbranch.LOGGER.error("HARBOR_SEAL_SMOKE FAIL",error);server.halt(false);}});
        ServerTickEvents.START_SERVER_TICK.register(server->start=System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!active)return;long duration=System.nanoTime()-start;total+=duration;max=Math.max(max,duration);++ticks;
            // Saved entity sections can arrive after SERVER_STARTED. Do not retain old test actors.
            if(ticks<=40)resetTransientActors(server.overworld());
            boolean swimActive=swimmer.getGoalSelector().getAvailableGoals().stream().anyMatch(g->g.getPriority()==9&&g.isRunning());if(swimActive)swimActiveTicks++;
            if(swimmer.isInWater()){waterTicks++;if(wasWater){double dx=swimmer.getX()-previous.x,dz=swimmer.getZ()-previous.z,travel=Math.sqrt(dx*dx+dz*dz);swimDistance+=travel;if(swimActive)goalGatedDistance+=travel;}}wasWater=swimmer.isInWater();previous=swimmer.position();
            if(idleSwimmer.isInWater()&&idleWasWater){double dx=idleSwimmer.getX()-previousIdle.x,dz=idleSwimmer.getZ()-previousIdle.z;idleDistance+=Math.sqrt(dx*dx+dz*dz);}idleWasWater=idleSwimmer.isInWater();previousIdle=idleSwimmer.position();
            if(ticks%80==0)Blockbranch.LOGGER.info("SEAL_SWIM_TRACE tick={} pos={} velocity={} inWater={} swimDistance={} running={}",ticks,swimmer.position(),swimmer.getDeltaMovement(),swimmer.isInWater(),swimDistance,swimmer.getGoalSelector().getAvailableGoals().stream().filter(g->g.isRunning()).map(g->g.getPriority()+":"+g.getGoal().getClass().getSimpleName()).toList());
            if(breather.getAirSupply()>5000)surfaced=true;
            if(ticks%160==0)Blockbranch.LOGGER.info("SEAL_SHORE_TRACE pos={} velocity={} navDone={} entered={} exited={} breatherAir={}",traveler.position(),traveler.getDeltaMovement(),traveler.getNavigation().isDone(),entered,exited,breather.getAirSupply());
            if(traveler.isInWater())entered=true;
            if(entered&&!exitRequested){exitRequested=true;traveler.getNavigation().moveTo(-20.5,-60,13.5,1);}
            if(exitRequested&&!traveler.isInWater()&&traveler.onGround()&&traveler.getZ()<15.5)exited=true;
            if(ticks<480)return;
            if(huntCase<0){huntCase=0;startHunt(server);return;}
            ++huntTicks;
            var arenaSeals=server.overworld().getEntitiesOfClass(HarborSeal.class,new net.minecraft.world.phys.AABB(-24,-64,-24,-15,-58,-15));
            check(arenaSeals.size()==1&&arenaSeals.getFirst()==hunter,"only the intended hunter in isolated fish arena");
            if(prey.getHealth()!=lastHuntPreyHealth){
                var damage=prey.getLastDamageSource();var attacker=damage==null?null:damage.getEntity();
                Blockbranch.LOGGER.info("SEAL_HUNT_PREY_DAMAGE case={} tick={} fed={} health={} attackerType={} intendedHunter={} source={}",huntCase,huntTicks,fed,prey.getHealth(),attacker==null?null:attacker.getType(),attacker==hunter,damage);
                lastHuntPreyHealth=prey.getHealth();
            }
            if(huntCase==4){
                if(!fed&&hunter.getTarget()==prey){
                    var feeder=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),"SealHuntFeeder"),ClientInformation.createDefault());feeder.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COD));hunter.setHealth(17);
                    hunter.mobInteract(feeder,InteractionHand.MAIN_HAND);check(feeder.getMainHandItem().isEmpty()&&hunter.getHealth()==19&&!hunter.isInLove(),"feeding heals nonbreeding adult");fed=true;
                    Blockbranch.LOGGER.info("SEAL_HUNT_FED tick={} preyHealth={} targetPrey={} cooldown={}",huntTicks,prey.getHealth(),hunter.getTarget()==prey,hunter.huntCooldown());
                }
                if(huntTicks<120)return;
                try{check(fed&&prey.isAlive()&&prey.getHealth()==prey.getMaxHealth()&&hunter.getTarget()!=prey&&hunter.huntCooldown()>2000,"feeding interrupts active hunt and retains cooldown fed="+fed+" preyHealth="+prey.getHealth()+" cooldown="+hunter.huntCooldown());}
                catch(Throwable error){Blockbranch.LOGGER.error("HARBOR_SEAL_SMOKE FAIL",error);active=false;server.halt(false);return;}
                hunter.discard();prey.discard();huntCase=5;
            }else{
                if(huntTicks<160)return;
                boolean combat=net.minecraft.world.Difficulty.values()[huntCase]!=net.minecraft.world.Difficulty.PEACEFUL;
                try{check(combat?!prey.isAlive()&&hunter.huntCooldown()>0:prey.isAlive()&&prey.getHealth()==prey.getMaxHealth(),"actual fish hunt "+net.minecraft.world.Difficulty.values()[huntCase]+" preyHealth="+prey.getHealth());}
                catch(Throwable error){Blockbranch.LOGGER.error("HARBOR_SEAL_SMOKE FAIL",error);active=false;server.halt(false);return;}
                hunter.discard();prey.discard();if(++huntCase<=4){startHunt(server);return;}
            }
            try{
                var level=server.overworld();var witness=level.getEntity(WITNESS);
                check(witness instanceof HarborSeal,"saved seal loads through native entity store");var saved=(HarborSeal)witness;
                check(saved.getAge()>(reload?-22000:-23000)&&saved.getAge()<(reload?-21000:-22000)&&saved.huntCooldown()==4200&&saved.isNoAi()&&saved.isPersistenceRequired()&&saved.getHealth()==8.5F&&saved.getCustomName().getString().equals("Saved seal"),"health/name/persistence reload age="+saved.getAge()+" cooldown="+saved.huntCooldown());
                check(surfaced&&breather.isAlive()&&breather.getHealth()==breather.getMaxHealth(),"low-air surfacing without drowning air="+breather.getAirSupply());
                check(entered&&exited,"actual amphibious entry and exit "+entered+"/"+exited+" pos="+traveler.position());
                check(swimmer.isAlive()&&swimmer.getAirSupply()>0&&waterTicks>=40&&swimDistance>.5,"horizontal swimming distance="+swimDistance+" waterTicks="+waterTicks);
                check(idleSwimmer.isAlive()&&idleSwimmer.getAirSupply()>0&&swimActiveTicks>0&&swimDistance>idleDistance+.5,"swim propulsion exceeds idle water drift by .5 blocks; activeTicks="+swimActiveTicks+" travel="+swimDistance+" idle="+idleDistance);
                Blockbranch.LOGGER.info("SEAL_SWIM_MEASUREMENT actualWaterTravel={} oldGoalGatedTravel={} idleWaterTravel={} activeTicks={} waterTicks={}",swimDistance,goalGatedDistance,idleDistance,swimActiveTicks,waterTicks);
                check(level.getBlockState(new net.minecraft.core.BlockPos(10,-61,8)).is(net.minecraft.world.level.block.Blocks.STONE),"test bank remains unchanged");
                check(first.getAge()>0&&second.getAge()>0&&!first.isInLove(),"native breeding goal and parent cooldown");
                int babies=0;for(var entity:level.getAllEntities())if(entity instanceof HarborSeal d&&d.isBaby()&&!d.getUUID().equals(WITNESS))babies++;
                check(babies==1,"actual offspring count "+babies);
                check(fleeing.distanceToSqr(threat)>64,"native avoidance increased predator distance "+fleeing.position());
                for(var animal:population)check(animal.tickCount>=120,"active herd tick count "+animal.tickCount);
                for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(entity instanceof HarborSeal&&!entity.getUUID().equals(WITNESS))entity.discard();threat.discard();
                Files.writeString(result(),"PASS\n");
                Blockbranch.LOGGER.info("HARBOR_SEAL_SMOKE PASS {} fishFood=true hideLoot=true looting=true pupFeeding=true offspring=1 predatorAvoidance=true amphibiousEntryExit=true horizontalSwim=true persistence=true noEngineering=true actualHuntFourDifficulties=true activeHuntFeeding=true population=16+interactionActors ticks={} meanWholeTickMs={} maxWholeTickMs={}",reload?"RELOAD":"NEW",ticks,total/1e6/ticks,max/1e6);
            }catch(Throwable error){Blockbranch.LOGGER.error("HARBOR_SEAL_SMOKE FAIL",error);}finally{active=false;server.halt(false);}
        });
    }
    private static void startHunt(net.minecraft.server.MinecraftServer server){
        var level=server.overworld();server.setDifficulty(net.minecraft.world.Difficulty.values()[Math.min(huntCase,3)],true);huntTicks=0;
        for(int x=-24;x<=-16;x++)for(int z=-24;z<=-16;z++)for(int y=-63;y<=-61;y++)level.setBlock(new net.minecraft.core.BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
        hunter=WildlifeRuntime.HARBOR_SEAL.create(level,EntitySpawnReason.COMMAND);hunter.snapTo(huntCase==4?-22.5:-20.5,-61.5,-20.5,0,0);if(huntCase==4)hunter.setAge(6000);hunter.setHuntCooldown(0);level.addFreshEntity(hunter);hunter.removeAllGoals(g->!(g instanceof net.minecraft.world.entity.ai.goal.FloatGoal));
        prey=net.minecraft.world.entity.EntityTypes.COD.create(level,EntitySpawnReason.COMMAND);prey.snapTo(huntCase==4?-17.5:-18.5,-61.5,-20.5,0,0);prey.setNoAi(true);level.addFreshEntity(prey);
        lastHuntPreyHealth=prey.getHealth();
    }
    private static void resetTransientActors(ServerLevel level){
        int removed=0;
        for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(entity instanceof Mob&&!entity.getUUID().equals(WITNESS)&&!fixtureActors.contains(entity.getUUID())){entity.discard();removed++;}
        if(removed>0)Blockbranch.LOGGER.info("SEAL_FIXTURE_RESET removedTransientActors={} namedWitnessPreserved=true",removed);
    }
    private static Path result(){return Path.of(System.getProperty("blockbranch.sealSmokeResult"));}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
