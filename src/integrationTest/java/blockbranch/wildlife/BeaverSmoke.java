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
final class BeaverSmoke {
    private static final UUID WITNESS=UUID.fromString("7c195fad-f04d-4136-8be7-6052450081b0");
    private static final List<EurasianBeaver> population=new ArrayList<>();
    private static EurasianBeaver first,second,fleeing,swimmer,traveler;
    private static int waterTicks;private static double swimDistance;private static net.minecraft.world.phys.Vec3 previous;private static boolean wasWater,entered,exited,exitRequested;
    private static net.minecraft.world.entity.animal.wolf.Wolf threat;
    private static boolean active,reload;private static int ticks;
    private static long start,total,max;
    static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
            var level=server.overworld();
            for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++)level.setChunkForced(x,z,true);
            level.getChunk(0,0);
            var deer=WildlifeRuntime.EURASIAN_BEAVER.create(level,EntitySpawnReason.COMMAND);
            var bank=new net.minecraft.core.BlockPos(8,-60,8);var water=bank.offset(2,-1,0);var wood=bank.offset(3,0,0);level.setBlock(wood,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
            check(!SpawnPlacements.checkSpawnRules(WildlifeRuntime.EURASIAN_BEAVER,level,EntitySpawnReason.CHUNK_GENERATION,bank,level.getRandom()),"dry bank rejected");
            var oldWater=level.getBlockState(water);level.setBlock(water,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
            check(!SpawnPlacements.checkSpawnRules(WildlifeRuntime.EURASIAN_BEAVER,level,EntitySpawnReason.CHUNK_GENERATION,bank,level.getRandom()),"unwooded wet bank rejected");
            level.setBlock(wood,net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState(),2);
            check(SpawnPlacements.checkSpawnRules(WildlifeRuntime.EURASIAN_BEAVER,level,EntitySpawnReason.CHUNK_GENERATION,bank,level.getRandom()),"wooded wet bank accepted");
            level.setBlock(water,oldWater,2);
            var registry=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
            for(String id:new String[]{"minecraft:river","minecraft:forest","minecraft:birch_forest","minecraft:taiga","blockbranch:dappled_oak_birch_woodland","blockbranch:boreal_spruce_birch_woodland","blockbranch:inland_lake_reedbed","blockbranch:gravel_bed_river_margin"}){
                var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.Identifier.parse(id));
                var settings=registry.getOrThrow(key).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,net.minecraft.world.level.biome.MobSpawnSettings.EMPTY);
                var entries=settings.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().filter(e->e.value().type()==WildlifeRuntime.EURASIAN_BEAVER).toList();
                check(entries.size()==1&&entries.getFirst().weight()==2&&entries.getFirst().value().count().minInclusive()==1&&entries.getFirst().value().count().maxInclusive()==3,"unique bounded habitat "+id);
            }
            for(var difficulty:Difficulty.values()){
                server.setDifficulty(difficulty,true);
                var victim=WildlifeRuntime.EURASIAN_BEAVER.create(level,EntitySpawnReason.COMMAND);
                var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);
                var player=new Player(level,new GameProfile(UUID.randomUUID(),"BeaverFoodTest")){
                    @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
                };
                check(!deer.canAttack(player)&&!deer.canAttack(wolf),"beaver remains noncombatant "+difficulty);
                check(victim.hurtServer(level,level.damageSources().mobAttack(wolf),1)==(difficulty!=Difficulty.PEACEFUL),"actual wolf/beaver difficulty damage "+difficulty);
            }
            check(deer.isFood(new ItemStack(Items.APPLE))&&deer.isFood(new ItemStack(Items.BIRCH_SAPLING))&&!deer.isFood(new ItemStack(Items.STICK)),"real plant food, not building sticks");
            var table=server.reloadableRegistries().getLootTable(deer.getLootTable().orElseThrow());
            var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,deer).withParameter(LootContextParams.ORIGIN,deer.position()).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().generic()).create(LootContextParamSets.ENTITY);
            for(long seed=0;seed<16;seed++)check(table.getRandomItems(params,seed).stream().allMatch(stack->stack.isEmpty()||stack.is(Items.LEATHER)&&stack.getCount()<=2),"usable hide loot without fabricated wood/fish/meat");
            var looter=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"BeaverLooter"),ClientInformation.createDefault());
            var sword=new ItemStack(Items.IRON_SWORD);sword.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),3);looter.setItemInHand(InteractionHand.MAIN_HAND,sword);
            var lootParams=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,deer).withParameter(LootContextParams.ORIGIN,deer.position()).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().playerAttack(looter)).withParameter(LootContextParams.ATTACKING_ENTITY,looter).withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY,looter).create(LootContextParamSets.ENTITY);
            int plain=0,enchanted=0;for(long seed=0;seed<32;seed++){for(var item:table.getRandomItems(params,seed))if(item.is(Items.LEATHER))plain+=item.getCount();for(var item:table.getRandomItems(lootParams,seed))if(item.is(Items.LEATHER))enchanted+=item.getCount();}
            check(enchanted>plain,"actual Looting III increases leather "+plain+"/"+enchanted);
            var kit=WildlifeRuntime.EURASIAN_BEAVER.create(level,EntitySpawnReason.COMMAND);kit.setAge(-24000);looter.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BIRCH_SAPLING));kit.mobInteract(looter,InteractionHand.MAIN_HAND);
            check(kit.getAge()>-24000&&kit.isBaby()&&looter.getMainHandItem().isEmpty(),"native kit feeding accelerates growth without taming");
            Path marker=result().resolveSibling("witness-created.txt");reload=Files.exists(marker);
            if(!reload){
                var witness=WildlifeRuntime.EURASIAN_BEAVER.create(level,EntitySpawnReason.COMMAND);witness.setUUID(WITNESS);
                witness.snapTo(8.5,-60,8.5,0,0);witness.setNoAi(true);witness.setPersistenceRequired();
                witness.setAge(-12000);witness.setHealth(8.5F);witness.setCustomName(net.minecraft.network.chat.Component.literal("Saved beaver"));
                level.addFreshEntity(witness);Files.writeString(marker,"created\n");
            }
            first=WildlifeRuntime.EURASIAN_BEAVER.create(level,EntitySpawnReason.COMMAND);second=WildlifeRuntime.EURASIAN_BEAVER.create(level,EntitySpawnReason.COMMAND);
            first.snapTo(8.5,-60,14.5,0,0);second.snapTo(9.5,-60,14.5,0,0);
            level.addFreshEntity(first);level.addFreshEntity(second);
            var feeder=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"BeaverFeeder"),ClientInformation.createDefault());
            feeder.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.CARROT,2));
            first.mobInteract(feeder,InteractionHand.MAIN_HAND);second.mobInteract(feeder,InteractionHand.MAIN_HAND);
            check(first.isInLove()&&second.isInLove()&&feeder.getMainHandItem().isEmpty(),"native two-adult feeding");
            check(!first.canMate(WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND)),"no cross-species reproduction");
            fleeing=WildlifeRuntime.EURASIAN_BEAVER.create(level,EntitySpawnReason.COMMAND);fleeing.getRandom().setSeed(113);
            fleeing.snapTo(-8.5,-60,-8.5,0,0);level.addFreshEntity(fleeing);
            threat=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);threat.snapTo(-4.5,-60,-8.5,0,0);threat.setNoAi(true);level.addFreshEntity(threat);
            for(int i=0;i<16;i++){
                var animal=WildlifeRuntime.EURASIAN_BEAVER.create(level,EntitySpawnReason.COMMAND);animal.snapTo(18+i%4*2,-60,12+i/4*2,0,0);
                level.addFreshEntity(animal);population.add(animal);
            }
            for(int x=-24;x<=-16;x++)for(int z=16;z<=24;z++)for(int y=-62;y<=-61;y++)level.setBlock(new net.minecraft.core.BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
            swimmer=WildlifeRuntime.EURASIAN_BEAVER.create(level,EntitySpawnReason.COMMAND);swimmer.snapTo(-20.5,-61,20.5,0,0);level.addFreshEntity(swimmer);
            var swim=swimmer.getGoalSelector().getAvailableGoals().stream().filter(g->g.getPriority()==9).findFirst().orElseThrow().getGoal();swimmer.removeAllGoals(g->g!=swim&&!(g instanceof net.minecraft.world.entity.ai.goal.FloatGoal));swimmer.getRandom().setSeed(1061);previous=swimmer.position();
            traveler=WildlifeRuntime.EURASIAN_BEAVER.create(level,EntitySpawnReason.COMMAND);traveler.snapTo(-20.5,-60,13.5,0,0);level.addFreshEntity(traveler);traveler.removeAllGoals(g->!(g instanceof net.minecraft.world.entity.ai.goal.FloatGoal));
            check(traveler.getNavigation().moveTo(-20.5,-61,20.5,1),"amphibious entry path created");
            active=true;
        }catch(Throwable error){Blockbranch.LOGGER.error("EURASIAN_BEAVER_SMOKE FAIL",error);server.halt(false);}});
        ServerTickEvents.START_SERVER_TICK.register(server->start=System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!active)return;long duration=System.nanoTime()-start;total+=duration;max=Math.max(max,duration);++ticks;
            if(swimmer.isInWater()){waterTicks++;if(wasWater&&swimmer.getGoalSelector().getAvailableGoals().stream().anyMatch(g->g.getPriority()==9&&g.isRunning())){double dx=swimmer.getX()-previous.x,dz=swimmer.getZ()-previous.z;swimDistance+=Math.sqrt(dx*dx+dz*dz);}}wasWater=swimmer.isInWater();previous=swimmer.position();
            if(ticks%80==0)Blockbranch.LOGGER.info("BEAVER_SWIM_TRACE tick={} pos={} velocity={} inWater={} swimDistance={} running={}",ticks,swimmer.position(),swimmer.getDeltaMovement(),swimmer.isInWater(),swimDistance,swimmer.getGoalSelector().getAvailableGoals().stream().filter(g->g.isRunning()).map(g->g.getPriority()+":"+g.getGoal().getClass().getSimpleName()).toList());
            if(traveler.isInWater())entered=true;
            if(entered&&!exitRequested){exitRequested=true;traveler.getNavigation().moveTo(-20.5,-60,13.5,1);}
            if(exitRequested&&!traveler.isInWater()&&traveler.onGround()&&traveler.getZ()<15.5)exited=true;
            if(ticks<480)return;
            try{
                var level=server.overworld();var witness=level.getEntity(WITNESS);
                check(witness instanceof EurasianBeaver,"saved beaver loads through native entity store");var saved=(EurasianBeaver)witness;
                check(saved.getAge()< -10000&&saved.isNoAi()&&saved.isPersistenceRequired()&&saved.getHealth()==8.5F&&saved.getCustomName().getString().equals("Saved beaver"),"health/name/persistence reload");
                check(entered&&exited,"actual amphibious entry and exit "+entered+"/"+exited+" pos="+traveler.position());
                check(swimmer.isAlive()&&swimmer.getAirSupply()>0&&waterTicks>=40&&swimDistance>.5,"horizontal swimming distance="+swimDistance+" waterTicks="+waterTicks);
                check(level.getBlockState(new net.minecraft.core.BlockPos(11,-60,8)).is(net.minecraft.world.level.block.Blocks.OAK_LOG),"nearby wood unchanged, no terrain engineering");
                check(first.getAge()>0&&second.getAge()>0&&!first.isInLove(),"native breeding goal and parent cooldown");
                int babies=0;for(var entity:level.getAllEntities())if(entity instanceof EurasianBeaver d&&d.isBaby()&&!d.getUUID().equals(WITNESS))babies++;
                check(babies==1,"actual offspring count "+babies);
                check(fleeing.distanceToSqr(threat)>64,"native avoidance increased predator distance "+fleeing.position());
                for(var animal:population)check(animal.tickCount>=120,"active herd tick count "+animal.tickCount);
                for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(entity instanceof EurasianBeaver&&!entity.getUUID().equals(WITNESS))entity.discard();threat.discard();
                Files.writeString(result(),"PASS\n");
                Blockbranch.LOGGER.info("EURASIAN_BEAVER_SMOKE PASS {} plantFood=true hideLoot=true looting=true kitFeeding=true offspring=1 predatorAvoidance=true amphibiousEntryExit=true horizontalSwim=true persistence=true noEngineering=true population=16+interactionActors ticks={} meanWholeTickMs={} maxWholeTickMs={}",reload?"RELOAD":"NEW",ticks,total/1e6/ticks,max/1e6);
            }catch(Throwable error){Blockbranch.LOGGER.error("EURASIAN_BEAVER_SMOKE FAIL",error);}finally{active=false;server.halt(false);}
        });
    }
    private static Path result(){return Path.of(System.getProperty("blockbranch.beaverSmokeResult"));}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
