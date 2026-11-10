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
final class RedDeerSmoke {
    private static final UUID WITNESS=UUID.fromString("c693552a-41ce-4289-b5ef-408a538ce9cf");
    private static final List<RedDeer> population=new ArrayList<>();
    private static RedDeer first,second,fleeing;
    private static net.minecraft.world.entity.animal.wolf.Wolf threat;
    private static boolean active,reload;private static int ticks;
    private static long start,total,max;
    static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
            var level=server.overworld();
            for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++)level.setChunkForced(x,z,true);
            level.getChunk(0,0);
            var deer=WildlifeRuntime.RED_DEER.create(level,EntitySpawnReason.COMMAND);
            deer.setAge(0);deer.setAntlers(true);deer.setInLove(null);
            check(deer.getAmbientSound()==WildlifeSounds.DEER_BELLOW&&deer.getSoundVolume()==.6F,"adult in-love stag bellow hook and unchanged volume");
            deer.setAntlers(false);check(deer.getAmbientSound()==WildlifeSounds.DEER_CONTACT,"female retains contact hook; approved source selected through resource mapping");
            deer.setAntlers(true);deer.setAge(-24000);check(deer.getAmbientSound()==WildlifeSounds.DEER_CONTACT,"calf retains contact hook; approved source selected through resource mapping");
            deer.setAge(0);deer.resetLove();check(deer.getAmbientSound()==WildlifeSounds.DEER_CONTACT,"non-rutting contact unchanged");
            check(deer.getHurtSound(level.damageSources().generic())==WildlifeSounds.DEER_HURT&&deer.getDeathSound()==WildlifeSounds.DEER_DEATH,"distinct hurt/death hooks retained for approved pitched resource mapping");
            var soilPosition=new net.minecraft.core.BlockPos(8,-60,8);
            var oldFloor=level.getBlockState(soilPosition.below());
            for(var floor:List.of(net.minecraft.world.level.block.Blocks.GRASS_BLOCK,net.minecraft.world.level.block.Blocks.PODZOL,
                net.minecraft.world.level.block.Blocks.COARSE_DIRT,net.minecraft.world.level.block.Blocks.ROOTED_DIRT,
                net.minecraft.world.level.block.Blocks.MOSS_BLOCK,net.minecraft.world.level.block.Blocks.STONE)){
                level.setBlock(soilPosition.below(),floor.defaultBlockState(),2);
                for(var type:List.of(WildlifeRuntime.WILD_BOAR,WildlifeRuntime.RED_DEER))
                    check(SpawnPlacements.checkSpawnRules(type,level,EntitySpawnReason.CHUNK_GENERATION,soilPosition,level.getRandom())==(floor!=net.minecraft.world.level.block.Blocks.STONE),"woodland soil eligibility "+floor+" "+type);
            }
            level.setBlock(soilPosition.below(),oldFloor,2);

            for(var difficulty:Difficulty.values()){
                server.setDifficulty(difficulty,true);
                var victim=WildlifeRuntime.RED_DEER.create(level,EntitySpawnReason.COMMAND);
                var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);
                var player=new Player(level,new GameProfile(UUID.randomUUID(),"DeerFoodTest")){
                    @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
                };
                check(!deer.canAttack(player)&&!deer.canAttack(wolf),"deer remains noncombatant "+difficulty);
                check(victim.hurtServer(level,level.damageSources().mobAttack(wolf),1)==(difficulty!=Difficulty.PEACEFUL),"actual wolf/deer difficulty damage "+difficulty);
            }
            var eater=new Player(level,new GameProfile(UUID.randomUUID(),"VenisonFoodTest")){
                @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
            };
            for(var item:List.of(WildlifeItems.VENISON,WildlifeItems.COOKED_VENISON)){
                eater.getFoodData().setFoodLevel(10);
                new ItemStack(item).finishUsingItem(level,eater);
                check(eater.getFoodData().getFoodLevel()==(item==WildlifeItems.VENISON?13:18),"actual venison consumption "+item);
            }
            var input=new SingleRecipeInput(new ItemStack(WildlifeItems.VENISON));
            for(var type:List.of(RecipeType.SMELTING,RecipeType.SMOKING,RecipeType.CAMPFIRE_COOKING)){
                var recipe=server.getRecipeManager().getRecipeFor(type,input,level).orElseThrow();
                check(recipe.value().assemble(input).is(WildlifeItems.COOKED_VENISON),"native venison cooking "+type);
            }
            var table=server.reloadableRegistries().getLootTable(deer.getLootTable().orElseThrow());
            for(boolean fire:new boolean[]{false,true}){
                deer.setRemainingFireTicks(fire?100:0);
                var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,deer)
                    .withParameter(LootContextParams.ORIGIN,deer.position()).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().generic()).create(LootContextParamSets.ENTITY);
                var drops=table.getRandomItems(params,73L);
                check(drops.stream().anyMatch(stack->stack.is(fire?WildlifeItems.COOKED_VENISON:WildlifeItems.VENISON)&&stack.getCount()>=1&&stack.getCount()<=3),"actual raw/cooked deer loot "+fire);
                check(drops.stream().allMatch(stack->stack.isEmpty()||stack.is(Items.LEATHER)||stack.is(fire?WildlifeItems.COOKED_VENISON:WildlifeItems.VENISON)),"loot material identity");
            }
            Path marker=result().resolveSibling("witness-created.txt");reload=Files.exists(marker);
            if(!reload){
                var witness=WildlifeRuntime.RED_DEER.create(level,EntitySpawnReason.COMMAND);witness.setUUID(WITNESS);
                witness.snapTo(8.5,-60,8.5,0,0);witness.setAntlers(true);witness.setNoAi(true);witness.setPersistenceRequired();
                witness.setHealth(12.5F);witness.setCustomName(net.minecraft.network.chat.Component.literal("Saved red deer"));
                level.addFreshEntity(witness);Files.writeString(marker,"created\n");
            }
            first=WildlifeRuntime.RED_DEER.create(level,EntitySpawnReason.COMMAND);second=WildlifeRuntime.RED_DEER.create(level,EntitySpawnReason.COMMAND);
            first.snapTo(8.5,-60,14.5,0,0);second.snapTo(9.5,-60,14.5,0,0);first.setAntlers(true);second.setAntlers(false);
            level.addFreshEntity(first);level.addFreshEntity(second);
            var feeder=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"DeerFeeder"),ClientInformation.createDefault());
            feeder.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WHEAT,2));
            first.mobInteract(feeder,InteractionHand.MAIN_HAND);second.mobInteract(feeder,InteractionHand.MAIN_HAND);
            check(first.isInLove()&&second.isInLove()&&feeder.getMainHandItem().isEmpty(),"native two-adult feeding");
            check(!first.canMate(WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND)),"no cross-species reproduction");
            fleeing=WildlifeRuntime.RED_DEER.create(level,EntitySpawnReason.COMMAND);fleeing.getRandom().setSeed(113);
            fleeing.snapTo(-8.5,-60,-8.5,0,0);level.addFreshEntity(fleeing);
            threat=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);threat.snapTo(-4.5,-60,-8.5,0,0);threat.setNoAi(true);level.addFreshEntity(threat);
            for(int i=0;i<16;i++){
                var animal=WildlifeRuntime.RED_DEER.create(level,EntitySpawnReason.COMMAND);animal.snapTo(18+i%4*2,-60,12+i/4*2,0,0);
                level.addFreshEntity(animal);population.add(animal);
            }
            active=true;
        }catch(Throwable error){Blockbranch.LOGGER.error("RED_DEER_SMOKE FAIL",error);server.halt(false);}});
        ServerTickEvents.START_SERVER_TICK.register(server->start=System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!active)return;long duration=System.nanoTime()-start;total+=duration;max=Math.max(max,duration);if(++ticks<160)return;
            try{
                var level=server.overworld();var witness=level.getEntity(WITNESS);
                check(witness instanceof RedDeer,"stored deer loads through native entity store");var saved=(RedDeer)witness;
                check(saved.hasAntlers()&&saved.isNoAi()&&saved.isPersistenceRequired()&&saved.getHealth()==12.5F&&saved.getCustomName().getString().equals("Saved red deer"),"variant/health/name/persistence reload");
                check(first.getAge()>0&&second.getAge()>0&&!first.isInLove(),"native breeding goal and parent cooldown");
                int babies=0;for(var entity:level.getAllEntities())if(entity instanceof RedDeer d&&d.isBaby())babies++;
                check(babies==1,"actual offspring count "+babies);
                check(fleeing.distanceToSqr(threat)>64,"native avoidance increased predator distance "+fleeing.position());
                for(var animal:population)check(animal.tickCount>=120,"active herd tick count "+animal.tickCount);
                for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(entity instanceof RedDeer&&!entity.getUUID().equals(WITNESS))entity.discard();threat.discard();
                Files.writeString(result(),"PASS\n");
                Blockbranch.LOGGER.info("RED_DEER_SMOKE PASS {} food=true cooking=3 loot=true offspring=1 flight=true variantPersistence=true population=16+interactionActors ticks={} meanWholeTickMs={} maxWholeTickMs={}",reload?"RELOAD":"NEW",ticks,total/1e6/ticks,max/1e6);
            }catch(Throwable error){Blockbranch.LOGGER.error("RED_DEER_SMOKE FAIL",error);}finally{active=false;server.halt(false);}
        });
    }
    private static Path result(){return Path.of(System.getProperty("blockbranch.deerSmokeResult"));}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
