package blockbranch.wildlife;
import blockbranch.Blockbranch;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Real hunting/defense phases on every difficulty, food pickup, breeding and disk replay. */
final class BrownBearSmoke {
    private static final UUID WITNESS=UUID.fromString("3631e4d7-d420-4b45-a981-c08a48427fae");
    private static final List<BrownBear> population=new ArrayList<>();
    private static BrownBear hunter,defender,guardian,cub,first,second,forager;
    private static Rabbit rabbit;private static Wolf threat,cubThreat;private static ItemEntity bait;
    private static int ticks,phase,phaseTicks;private static boolean active,reload,hunted,retaliated,protectedCub;
    private static long tickStart,total,max;
    static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
            var level=server.overworld();for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++)level.setChunkForced(x,z,true);level.getChunk(0,0);
            var adult=WildlifeRuntime.BROWN_BEAR.create(level,EntitySpawnReason.COMMAND);
            for(var difficulty:Difficulty.values()){
                server.setDifficulty(difficulty,true);
                var player=new Player(level,new GameProfile(UUID.randomUUID(),"BearPolicy")){
                    @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
                };
                boolean attack=difficulty==Difficulty.NORMAL||difficulty==Difficulty.HARD;
                check(adult.canAttack(player)==attack,"player targeting "+difficulty);
                check(player.hurtServer(level,level.damageSources().mobAttack(adult),1)==attack,"actual player damage "+difficulty);
                var baby=WildlifeRuntime.BROWN_BEAR.create(level,EntitySpawnReason.COMMAND);baby.setAge(-24000);
                check(!baby.canAttack(player)&&!baby.canAttack(adult),"cubs never attack "+difficulty);
            }
            check(Math.abs(adult.getBbWidth()-1.1F)<.0001&&Math.abs(adult.getBbHeight()-1.35F)<.0001,"adult native dimensions");
            var registry=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
            for(String id:new String[]{"minecraft:forest","minecraft:birch_forest","minecraft:old_growth_birch_forest","minecraft:dappled_forest",
                    "minecraft:taiga","minecraft:old_growth_pine_taiga","minecraft:old_growth_spruce_taiga",
                    "blockbranch:dappled_oak_birch_woodland","blockbranch:boreal_spruce_birch_woodland"}){
                var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.Identifier.parse(id));
                var settings=registry.getOrThrow(key).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,net.minecraft.world.level.biome.MobSpawnSettings.EMPTY);
                var entries=settings.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().filter(entry->entry.value().type()==WildlifeRuntime.BROWN_BEAR).toList();
                check(entries.size()==1&&entries.getFirst().weight()==1&&entries.getFirst().value().count().minInclusive()==1&&entries.getFirst().value().count().maxInclusive()==2,"bounded unique bear habitat "+id);
            }
            var soil=new net.minecraft.core.BlockPos(8,-60,8);var oldFloor=level.getBlockState(soil.below());
            for(var floor:List.of(net.minecraft.world.level.block.Blocks.GRASS_BLOCK,net.minecraft.world.level.block.Blocks.PODZOL,
                    net.minecraft.world.level.block.Blocks.COARSE_DIRT,net.minecraft.world.level.block.Blocks.DIRT,net.minecraft.world.level.block.Blocks.STONE)){
                level.setBlock(soil.below(),floor.defaultBlockState(),2);
                check(SpawnPlacements.checkSpawnRules(WildlifeRuntime.BROWN_BEAR,level,EntitySpawnReason.CHUNK_GENERATION,soil,level.getRandom())==(floor!=net.minecraft.world.level.block.Blocks.STONE),"woodland bear soil eligibility "+floor);
            }
            level.setBlock(soil.below(),oldFloor,2);
            var table=server.reloadableRegistries().getLootTable(adult.getLootTable().orElseThrow());
            var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,adult).withParameter(LootContextParams.ORIGIN,adult.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().generic()).create(LootContextParamSets.ENTITY);
            for(long seed=0;seed<8;seed++)check(table.getRandomItems(params,seed).stream().allMatch(stack->stack.isEmpty()||stack.is(Items.LEATHER)&&stack.getCount()<=2),"actual hide loot has no substituted meat/fish");
            var marker=result().resolveSibling("witness-created.txt");reload=Files.exists(marker);
            if(!reload){var saved=WildlifeRuntime.BROWN_BEAR.create(level,EntitySpawnReason.COMMAND);saved.setUUID(WITNESS);saved.snapTo(8.5,-60,8.5,0,0);saved.setHealth(25.5F);saved.setNoAi(true);saved.setPersistenceRequired();saved.setHuntCooldown(7777);saved.setCustomName(net.minecraft.network.chat.Component.literal("Saved brown bear"));level.addFreshEntity(saved);Files.writeString(marker,"created\n");}
            first=spawn(level,8.5,16.5);second=spawn(level,10.5,16.5);
            var feeder=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"BearFeeder"),ClientInformation.createDefault());
            feeder.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SWEET_BERRIES));first.mobInteract(feeder,InteractionHand.MAIN_HAND);
            feeder.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SALMON));second.mobInteract(feeder,InteractionHand.MAIN_HAND);
            check(first.isInLove()&&second.isInLove()&&feeder.getMainHandItem().isEmpty(),"real berry/fish feeding and breeding");
            check(first.huntCooldown()>=2390&&!net.minecraft.world.entity.TamableAnimal.class.isInstance(first),"feeding satisfies hunting without taming");
            check(!first.canMate(WildlifeRuntime.RED_DEER.create(level,EntitySpawnReason.COMMAND)),"no crossbreeding");
            forager=spawn(level,20.5,20.5);forager.setHealth(20);forager.setHuntCooldown(600);forager.setAge(6000);
            bait=new ItemEntity(level,23.5,-60,20.5,new ItemStack(Items.SWEET_BERRIES,2));level.addFreshEntity(bait);
            for(int i=0;i<12;i++)population.add(spawn(level,26.5+(i%3)*3,26.5+(i/3)*3));
            phase=0;startPhase(server);active=true;
        }catch(Throwable error){Blockbranch.LOGGER.error("BROWN_BEAR_SMOKE FAIL",error);server.halt(false);}});
        ServerTickEvents.START_SERVER_TICK.register(server->tickStart=System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!active)return;try{
            long duration=System.nanoTime()-tickStart;total+=duration;max=Math.max(max,duration);ticks++;phaseTicks++;
            var level=server.overworld();var difficulty=Difficulty.values()[phase];
            if(phaseTicks==20){
                boolean combat=difficulty!=Difficulty.PEACEFUL;
                check(defender.hurtServer(level,level.damageSources().mobAttack(threat),1)==combat,"native adult provocation "+difficulty);
                check(cub.hurtServer(level,level.damageSources().mobAttack(cubThreat),1)==combat,"native cub provocation "+difficulty);
            }
            if(hunter.getLastHurtMob()==rabbit&&!rabbit.isAlive())hunted=true;
            if(defender.getLastHurtMob()==threat&&threat.getHealth()<threat.getMaxHealth())retaliated=true;
            if(guardian.getLastHurtMob()==cubThreat&&cubThreat.getHealth()<cubThreat.getMaxHealth())protectedCub=true;
            if(phaseTicks<140)return;
            if(difficulty==Difficulty.PEACEFUL)check(!hunted&&!retaliated&&!protectedCub&&rabbit.isAlive()&&rabbit.getHealth()==rabbit.getMaxHealth(),"no peaceful animal combat");
            else check(hunted&&retaliated&&protectedCub,"actual hunt/retaliation/cub protection "+difficulty+" "+hunted+"/"+retaliated+"/"+protectedCub);
            Blockbranch.LOGGER.info("BROWN_BEAR_COMBAT_PHASE_PASS difficulty={} preyKilled={} adultRetaliation={} cubProtected={}",difficulty,hunted,retaliated,protectedCub);
            for(var entity:List.of(hunter,defender,guardian,cub,rabbit,threat,cubThreat))entity.discard();
            for(var item:level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(-32,-64,-32,-6,-48,-6)))item.discard();
            if(++phase<4){startPhase(server);return;}
            check(first.getAge()>0&&second.getAge()>0&&!first.isInLove()&&!second.isInLove(),"native reproduction cooldown");
            check(level.getEntitiesOfClass(BrownBear.class,first.getBoundingBox().inflate(8),bear->bear.isBaby()).size()==1,"exactly one native cub");
            check(forager.getHealth()==22&&bait.isAlive()&&bait.getItem().getCount()==1&&forager.huntCooldown()>1500,"actual bounded food navigation/consumption/healing hp="+forager.getHealth()+" bait="+bait.getItem()+" cooldown="+forager.huntCooldown());
            var saved=level.getEntity(WITNESS);check(saved instanceof BrownBear,"actual saved bear loaded");var witness=(BrownBear)saved;
            check(witness.isNoAi()&&witness.isPersistenceRequired()&&witness.getHealth()==25.5F&&witness.huntCooldown()>7000&&witness.getName().getString().equals("Saved brown bear"),"disk hunger/health/name/AI persistence");
            witness.setHuntCooldown(7777);for(var bear:population){check(bear.tickCount>=500,"active bear population ticking");bear.discard();}first.discard();second.discard();forager.discard();bait.discard();
            for(var child:level.getEntitiesOfClass(BrownBear.class,new net.minecraft.world.phys.AABB(-32,-64,-32,48,-48,48),bear->bear.isBaby()))child.discard();
            Files.writeString(result(),"PASS\n");Blockbranch.LOGGER.info("BROWN_BEAR_SMOKE PASS {} difficulties=4 actualPredation=true defense=true feeding=true groundFood=true offspring=1 disk=true population=12+lanes ticks={} meanWholeTickMs={} maxWholeTickMs={}",reload?"RELOAD":"NEW",ticks,total/1e6/ticks,max/1e6);active=false;server.halt(false);
        }catch(Throwable error){active=false;Blockbranch.LOGGER.error("BROWN_BEAR_SMOKE FAIL",error);server.halt(false);}});
    }
    private static void startPhase(net.minecraft.server.MinecraftServer server){var level=server.overworld();server.setDifficulty(Difficulty.values()[phase],true);phaseTicks=0;hunted=retaliated=protectedCub=false;
        hunter=spawn(level,-20.5,-20.5);hunter.setHuntCooldown(0);rabbit=EntityTypes.RABBIT.create(level,EntitySpawnReason.COMMAND);rabbit.snapTo(-16.5,-60,-20.5,0,0);rabbit.setNoAi(true);level.addFreshEntity(rabbit);
        defender=spawn(level,20.5,-20.5);threat=wolf(level,24.5,-20.5);guardian=spawn(level,-20.5,20.5);cub=spawn(level,-18.5,20.5);cub.setAge(-24000);cubThreat=wolf(level,-14.5,20.5);
    }
    private static BrownBear spawn(ServerLevel level,double x,double z){var bear=WildlifeRuntime.BROWN_BEAR.create(level,EntitySpawnReason.COMMAND);bear.snapTo(x,-60,z,0,0);bear.setHuntCooldown(12000);level.addFreshEntity(bear);return bear;}
    private static Wolf wolf(ServerLevel level,double x,double z){var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);wolf.snapTo(x,-60,z,0,0);wolf.setNoAi(true);level.addFreshEntity(wolf);return wolf;}
    private static Path result(){return Path.of(System.getProperty("blockbranch.bearSmokeResult"));}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private BrownBearSmoke(){}
}
