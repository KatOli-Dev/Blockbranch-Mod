package blockbranch.wildlife;
import blockbranch.Blockbranch;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;

/** Native bison interactions, actual melee/calf defense, registry habitat and disk replay. */
final class BisonSmoke {
    private static final UUID WITNESS=UUID.fromString("5a29cc56-3b6d-47d4-b32c-0fdc1941757e");
    private static final List<AmericanBison> population=new ArrayList<>();
    private static AmericanBison first,second,defender,calf,selfDefender;
    private static Wolf calfThreat,selfThreat;
    private static boolean active,reload,defendedCalf,retaliated;private static int ticks;
    private static long tickStart,total,max;
    static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
            var level=server.overworld();for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++)level.setChunkForced(x,z,true);
            level.getChunk(0,0);
            var adult=WildlifeRuntime.AMERICAN_BISON.create(level,EntitySpawnReason.COMMAND);
            var baby=WildlifeRuntime.AMERICAN_BISON.create(level,EntitySpawnReason.COMMAND);baby.setAge(-24000);
            check(Math.abs(adult.getBbWidth()-1.3F)<.0001&&Math.abs(adult.getBbHeight()-1.8F)<.0001&&Math.abs(baby.getBbHeight()-.9F)<.0001,"native adult/calf dimensions");
            for(var difficulty:Difficulty.values()){
                server.setDifficulty(difficulty,true);
                var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);
                var player=new Player(level,new GameProfile(UUID.randomUUID(),"BisonPolicy")){
                    @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
                };
                boolean attacksPlayer=difficulty==Difficulty.NORMAL||difficulty==Difficulty.HARD;
                check(adult.canAttack(player)==attacksPlayer,"adult player policy "+difficulty);
                check(adult.canAttack(wolf)==(difficulty!=Difficulty.PEACEFUL),"adult animal policy "+difficulty);
                check(!baby.canAttack(player)&&!baby.canAttack(wolf),"calves never attack "+difficulty);
                check(wolf.hurtServer(level,level.damageSources().mobAttack(adult),1)==(difficulty!=Difficulty.PEACEFUL),"actual bison animal damage "+difficulty);
                check(player.hurtServer(level,level.damageSources().mobAttack(adult),1)==attacksPlayer,"actual bison player damage "+difficulty);
            }
            server.setDifficulty(Difficulty.NORMAL,true);
            for(boolean fire:new boolean[]{false,true}){
                adult.setRemainingFireTicks(fire?100:0);
                var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,adult)
                    .withParameter(LootContextParams.ORIGIN,adult.position()).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().generic()).create(LootContextParamSets.ENTITY);
                var drops=server.reloadableRegistries().getLootTable(adult.getLootTable().orElseThrow()).getRandomItems(params,613L);
                check(drops.stream().anyMatch(stack->stack.is(fire?Items.COOKED_BEEF:Items.BEEF)&&stack.getCount()>=2&&stack.getCount()<=5),"actual raw/cooked beef "+fire);
                check(drops.stream().anyMatch(stack->stack.is(Items.LEATHER)&&stack.getCount()>=1&&stack.getCount()<=3),"actual leather loot");
                check(drops.stream().allMatch(stack->stack.is(Items.LEATHER)||stack.is(fire?Items.COOKED_BEEF:Items.BEEF)),"native material identity");
            }
            var input=new SingleRecipeInput(new ItemStack(Items.BEEF));
            for(var type:List.of(RecipeType.SMELTING,RecipeType.SMOKING,RecipeType.CAMPFIRE_COOKING))
                check(server.getRecipeManager().getRecipeFor(type,input,level).orElseThrow().value().assemble(input).is(Items.COOKED_BEEF),"native beef cooking "+type);
            var registry=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
            for(String id:new String[]{"minecraft:plains","minecraft:sunflower_plains","blockbranch:rolling_lowland_forb_grass_meadow","blockbranch:dry_continental_scrub_steppe"}){
                var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.Identifier.parse(id));
                var settings=registry.getOrThrow(key).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,net.minecraft.world.level.biome.MobSpawnSettings.EMPTY);
                var entries=settings.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().filter(entry->entry.value().type()==WildlifeRuntime.AMERICAN_BISON).toList();
                check(entries.size()==1&&entries.getFirst().weight()==2&&entries.getFirst().value().count().minInclusive()==3&&entries.getFirst().value().count().maxInclusive()==5,"bounded unique bison habitat "+id);
            }
            var soilPosition=new BlockPos(8,-60,8);var oldFloor=level.getBlockState(soilPosition.below());
            for(var floor:List.of(net.minecraft.world.level.block.Blocks.GRASS_BLOCK,net.minecraft.world.level.block.Blocks.COARSE_DIRT,net.minecraft.world.level.block.Blocks.DIRT,net.minecraft.world.level.block.Blocks.STONE)){
                level.setBlock(soilPosition.below(),floor.defaultBlockState(),2);
                check(SpawnPlacements.checkSpawnRules(WildlifeRuntime.AMERICAN_BISON,level,EntitySpawnReason.CHUNK_GENERATION,soilPosition,level.getRandom())==(floor!=net.minecraft.world.level.block.Blocks.STONE),"grassland soil eligibility "+floor);
            }
            level.setBlock(soilPosition.below(),oldFloor,2);
            Path marker=result().resolveSibling("witness-created.txt");reload=Files.exists(marker);
            if(!reload){var saved=WildlifeRuntime.AMERICAN_BISON.create(level,EntitySpawnReason.COMMAND);saved.setUUID(WITNESS);saved.snapTo(8.5,-60,8.5,0,0);level.addFreshEntity(saved);saved.setHealth(32.5F);saved.setCustomName(net.minecraft.network.chat.Component.literal("Saved bison"));saved.setNoAi(true);saved.setPersistenceRequired();Files.writeString(marker,"created\n");}
            first=spawn(level,8.5,15.5);second=spawn(level,10.5,15.5);
            var feeder=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"BisonFeeder"),ClientInformation.createDefault());
            feeder.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));adult.mobInteract(feeder,InteractionHand.MAIN_HAND);
            check(feeder.getMainHandItem().is(Items.BUCKET)&&feeder.getMainHandItem().getCount()==1,"wild bison is not a cow milking shortcut");
            feeder.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WHEAT,2));
            first.mobInteract(feeder,InteractionHand.MAIN_HAND);second.mobInteract(feeder,InteractionHand.MAIN_HAND);
            check(first.isInLove()&&second.isInLove()&&feeder.getMainHandItem().isEmpty(),"real native feeding consumes wheat");
            check(!first.canMate(WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND)),"no cross-species breeding");
            for(int i=0;i<16;i++)population.add(spawn(level,24.5+(i%4)*3,24.5+(i/4)*3));
            defender=spawn(level,-20.5,8.5);calf=spawn(level,-18.5,8.5);calf.setAge(-24000);
            calfThreat=threat(level,-14.5,8.5);selfDefender=spawn(level,18.5,-20.5);selfThreat=threat(level,22.5,-20.5);
            active=true;
            Blockbranch.LOGGER.info("BISON_SMOKE_START mode={} activeGroup=16 plus interactions",reload?"RELOAD":"NEW");
        }catch(Throwable error){Blockbranch.LOGGER.error("BISON_SMOKE FAIL",error);server.halt(false);}});
        ServerTickEvents.START_SERVER_TICK.register(server->tickStart=System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!active)return;try{
            long duration=System.nanoTime()-tickStart;total+=duration;max=Math.max(max,duration);++ticks;
            var level=server.overworld();
            if(ticks==20){check(calf.hurtServer(level,level.damageSources().mobAttack(calfThreat),1),"actual wolf attacks calf");check(selfDefender.hurtServer(level,level.damageSources().mobAttack(selfThreat),1),"actual wolf provokes adult");}
            if(defender.getLastHurtMob()==calfThreat&&calfThreat.getHealth()<calfThreat.getMaxHealth())defendedCalf=true;
            if(selfDefender.getLastHurtMob()==selfThreat&&selfThreat.getHealth()<selfThreat.getMaxHealth())retaliated=true;
            if(ticks<180)return;
            check(defendedCalf&&retaliated,"actual navigation/melee for calf defense and retaliation calf="+defendedCalf+" self="+retaliated);
            check(first.getAge()>0&&second.getAge()>0&&!first.isInLove()&&!second.isInLove(),"native breeding cooldown");
            long offspring=level.getEntitiesOfClass(AmericanBison.class,first.getBoundingBox().inflate(8),other->other.isBaby()&&other!=calf).size();
            check(offspring==1,"exactly one native calf offspring "+offspring);
            var saved=level.getEntity(WITNESS);check(saved instanceof AmericanBison&&((AmericanBison)saved).isNoAi()&&((AmericanBison)saved).isPersistenceRequired()&&((AmericanBison)saved).getHealth()==32.5F&&saved.getName().getString().equals("Saved bison"),"disk health/name/AI witness");
            for(var subject:population)check(subject.tickCount>=160,"active bison population ticking");
            for(var subject:population)subject.discard();first.discard();second.discard();defender.discard();calf.discard();selfDefender.discard();calfThreat.discard();selfThreat.discard();
            for(var entity:level.getEntitiesOfClass(AmericanBison.class,new net.minecraft.world.phys.AABB(-32,-64,-32,48,-48,48),animal->animal.isBaby()))entity.discard();
            Files.writeString(result(),"PASS\n");
            Blockbranch.LOGGER.info("BISON_SMOKE PASS {} feeding=true cooking=3 loot=true calfDefense=true retaliation=true offspring=1 disk=true habitats=4 group=16 ticks={} meanWholeTickMs={} maxWholeTickMs={}",reload?"RELOAD":"NEW",ticks,total/1e6/ticks,max/1e6);
            active=false;server.halt(false);
        }catch(Throwable error){active=false;Blockbranch.LOGGER.error("BISON_SMOKE FAIL",error);server.halt(false);}});
    }
    private static AmericanBison spawn(ServerLevel level,double x,double z){var animal=WildlifeRuntime.AMERICAN_BISON.create(level,EntitySpawnReason.COMMAND);animal.snapTo(x,-60,z,0,0);level.addFreshEntity(animal);return animal;}
    private static Wolf threat(ServerLevel level,double x,double z){var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);wolf.snapTo(x,-60,z,0,0);wolf.setNoAi(true);level.addFreshEntity(wolf);return wolf;}
    private static Path result(){return Path.of(System.getProperty("blockbranch.bisonSmokeResult"));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private BisonSmoke(){}
}
