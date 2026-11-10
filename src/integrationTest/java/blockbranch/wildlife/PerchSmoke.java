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
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
/** Native freshwater placement, food, bucket capture/release, schooling, save/reload and local load. */
final class PerchSmoke {
    private static final UUID WITNESS=UUID.fromString("e198bfcb-7f80-444b-9735-3295be172b25");
    private static final List<EuropeanPerch> population=new ArrayList<>();
    private static EuropeanPerch leader,follower,swimmer,dry;private static boolean active,reload;private static int ticks,waterTicks,flopTicks;
    private static double swimDistance,nearest=Double.MAX_VALUE;private static net.minecraft.world.phys.Vec3 previous;
    private static net.minecraft.world.phys.AABB bucketReleaseArea;
    private static Set<UUID> priorBucketIds;
    private static long start,total,max;
    static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
            var level=server.overworld();for(int z=-2;z<=2;z++)for(int x=-2;x<=2;x++)level.setChunkForced(x,z,true);
            for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)for(int y=-63;y<=-61;y++)level.setBlock(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState(),2);
            var registry=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
            for(String id:new String[]{"minecraft:river","minecraft:swamp","blockbranch:inland_lake_reedbed","blockbranch:gravel_bed_river_margin"}){
                var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.Identifier.parse(id));
                var settings=registry.getOrThrow(key).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,net.minecraft.world.level.biome.MobSpawnSettings.EMPTY);
                var entries=settings.getMobsToSpawn(MobCategory.WATER_AMBIENT).unwrap().stream().filter(e->e.value().type()==WildlifeRuntime.EUROPEAN_PERCH).toList();
                check(entries.size()==1&&entries.getFirst().weight()==4&&entries.getFirst().value().count().minInclusive()==3&&entries.getFirst().value().count().maxInclusive()==6,"unique freshwater entry "+id);
            }
            for(int y:new int[]{-512,512}){
                var pos=new BlockPos(20,y,20);for(int dy=-1;dy<=1;dy++)level.setBlock(pos.offset(0,dy,0),Blocks.AIR.defaultBlockState(),2);check(!SpawnPlacements.checkSpawnRules(WildlifeRuntime.EUROPEAN_PERCH,level,EntitySpawnReason.NATURAL,pos,level.getRandom()),"dry rejected at "+y);
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=-2;dy<=2;dy++)level.setBlock(pos.offset(dx,dy,dz),Blocks.GLASS.defaultBlockState(),2);
                for(int dy=-1;dy<=1;dy++)level.setBlock(pos.offset(0,dy,0),Blocks.WATER.defaultBlockState(),2);
                check(SpawnPlacements.checkSpawnRules(WildlifeRuntime.EUROPEAN_PERCH,level,EntitySpawnReason.NATURAL,pos,level.getRandom()),"water accepted independent of sea elevation "+y);
            }
            var fish=WildlifeRuntime.EUROPEAN_PERCH.create(level,EntitySpawnReason.COMMAND);
            var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);
            for(var difficulty:Difficulty.values()){
                server.setDifficulty(difficulty,true);var victim=WildlifeRuntime.EUROPEAN_PERCH.create(level,EntitySpawnReason.COMMAND);
                check(!fish.canAttack(wolf),"passive fish "+difficulty);
                check(victim.hurtServer(level,level.damageSources().mobAttack(wolf),1)==(difficulty!=Difficulty.PEACEFUL),"actual predator damage "+difficulty);
            }
            var player=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"PerchKeeper"),ClientInformation.createDefault());
            for(var item:List.of(WildlifeItems.PERCH,WildlifeItems.COOKED_PERCH)){player.getFoodData().setFoodLevel(10);new ItemStack(item).finishUsingItem(level,player);check(player.getFoodData().getFoodLevel()==(item==WildlifeItems.PERCH?12:16),"actual perch food");}
            var input=new SingleRecipeInput(new ItemStack(WildlifeItems.PERCH));
            for(var type:List.of(RecipeType.SMELTING,RecipeType.SMOKING,RecipeType.CAMPFIRE_COOKING))check(server.getRecipeManager().getRecipeFor(type,input,level).orElseThrow().value().assemble(input).is(WildlifeItems.COOKED_PERCH),"perch cooking "+type);
            check(new ItemStack(WildlifeItems.PERCH).is(net.minecraft.tags.ItemTags.CAT_FOOD)&&new ItemStack(WildlifeItems.PERCH).is(net.minecraft.tags.ItemTags.OCELOT_FOOD)&&new ItemStack(WildlifeItems.COOKED_PERCH).is(net.minecraft.tags.ItemTags.FISHES),"native fish/cat/ocelot food tags");
            var fishing=server.reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
            var fishingParams=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,new net.minecraft.world.phys.Vec3(.5,-62,.5)).withParameter(LootContextParams.TOOL,new ItemStack(Items.FISHING_ROD)).create(LootContextParamSets.FISHING);
            int catches=0;for(long seed=0;seed<128;seed++){var drops=fishing.getRandomItems(fishingParams,seed);check(drops.size()==1,"rod catch cardinality preserved");if(drops.getFirst().is(WildlifeItems.PERCH))catches++;}check(catches>0&&catches<64,"actual freshwater rod fishing "+catches);
            check(!PerchFishing.freshwater("minecraft:ocean")&&!PerchFishing.freshwater("minecraft:deep_ocean"),"marine habitats excluded");
            var table=server.reloadableRegistries().getLootTable(fish.getLootTable().orElseThrow());
            for(boolean fire:new boolean[]{false,true}){
                fish.setRemainingFireTicks(fire?100:0);var params=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,fish).withParameter(LootContextParams.ORIGIN,fish.position()).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().generic()).create(LootContextParamSets.ENTITY);
                var drops=table.getRandomItems(params,73L);check(drops.stream().anyMatch(s->s.is(fire?WildlifeItems.COOKED_PERCH:WildlifeItems.PERCH)&&s.getCount()==1),"actual raw/fire-cooked loot "+fire);
            }
            fish.setRemainingFireTicks(0);
            var sword=new ItemStack(Items.IRON_SWORD);sword.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),3);player.setItemInHand(InteractionHand.MAIN_HAND,sword);
            var lootingParams=new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY,fish).withParameter(LootContextParams.ORIGIN,fish.position()).withParameter(LootContextParams.DAMAGE_SOURCE,level.damageSources().playerAttack(player)).withParameter(LootContextParams.ATTACKING_ENTITY,player).withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY,player).create(LootContextParamSets.ENTITY);
            for(long seed=0;seed<16;seed++){var drops=table.getRandomItems(lootingParams,seed);check(drops.size()==1&&drops.getFirst().is(WildlifeItems.PERCH)&&drops.getFirst().getCount()==1,"one whole fish even with Looting");}
            fish.snapTo(0.5,-62,8.5,0,0);fish.setNoAi(true);fish.setHealth(2);fish.setCustomName(net.minecraft.network.chat.Component.literal("Bucket perch"));level.addFreshEntity(fish);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));fish.interact(player,InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO);
            var bucket=player.getMainHandItem();check(fish.isRemoved()&&bucket.is(WildlifeItems.PERCH_BUCKET),"actual water bucket pickup");
            var releasedPos=new BlockPos(-8,-62,8);var releaseArea=new net.minecraft.world.phys.AABB(releasedPos).inflate(2);var priorIds=level.getEntitiesOfClass(EuropeanPerch.class,releaseArea).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());check(((MobBucketItem)WildlifeItems.PERCH_BUCKET).emptyContents(player,level,releasedPos,null),"native bucket empty into water");((MobBucketItem)WildlifeItems.PERCH_BUCKET).checkExtraContent(player,level,bucket,releasedPos);
            // Newly forced neighboring chunks become entity-accessible on a server tick.
            // Preserve the real native release and validate its data after that transition.
            bucketReleaseArea=releaseArea;priorBucketIds=priorIds;
            Path marker=result().resolveSibling("witness-created.txt");reload=Files.exists(marker);
            if(!reload){var saved=WildlifeRuntime.EUROPEAN_PERCH.create(level,EntitySpawnReason.COMMAND);saved.setUUID(WITNESS);saved.snapTo(0.5,-62,8.5,0,0);saved.setNoAi(true);saved.setFromBucket(true);saved.setHealth(2);saved.setCustomName(net.minecraft.network.chat.Component.literal("Saved perch"));level.addFreshEntity(saved);Files.writeString(marker,"created\n");}
            leader=WildlifeRuntime.EUROPEAN_PERCH.create(level,EntitySpawnReason.COMMAND);leader.snapTo(.5,-62,.5,0,0);leader.setNoAi(true);level.addFreshEntity(leader);
            follower=WildlifeRuntime.EUROPEAN_PERCH.create(level,EntitySpawnReason.COMMAND);follower.snapTo(6.5,-62,.5,0,0);level.addFreshEntity(follower);follower.startFollowing(leader);check(follower.isFollower()&&leader.hasFollowers()&&leader.getMaxSchoolSize()==6,"native school membership");
            swimmer=WildlifeRuntime.EUROPEAN_PERCH.create(level,EntitySpawnReason.COMMAND);swimmer.snapTo(-8.5,-62,-8.5,0,0);level.addFreshEntity(swimmer);previous=swimmer.position();
            for(int i=0;i<16;i++){var p=WildlifeRuntime.EUROPEAN_PERCH.create(level,EntitySpawnReason.COMMAND);p.snapTo(-8+i%4*3,-62,4+i/4*2,0,0);level.addFreshEntity(p);population.add(p);}
            dry=WildlifeRuntime.EUROPEAN_PERCH.create(level,EntitySpawnReason.COMMAND);dry.snapTo(26.5,-60,.5,0,0);dry.removeAllGoals(g->true);level.addFreshEntity(dry);
            active=true;
        }catch(Throwable error){Blockbranch.LOGGER.error("EUROPEAN_PERCH_SMOKE FAIL",error);server.halt(false);}});
        ServerTickEvents.START_SERVER_TICK.register(server->start=System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!active)return;long duration=System.nanoTime()-start;total+=duration;max=Math.max(max,duration);ticks++;
            if(ticks==5)try{
                var released=server.overworld().getEntitiesOfClass(EuropeanPerch.class,bucketReleaseArea).stream().filter(entity->!priorBucketIds.contains(entity.getUUID())).findFirst().orElseThrow(()->new AssertionError("bucket release not visible after chunk activation"));
                check(released.fromBucket()&&released.requiresCustomPersistence()&&!released.removeWhenFarAway(10000)&&released.getHealth()==2&&released.isNoAi()&&released.getCustomName().getString().equals("Bucket perch"),"native bucket data and persistence");
                Blockbranch.LOGGER.info("PERCH_BUCKET_RELEASE_PASS afterActivationTicks={} position={} fromBucket={} noAI={} health={}",ticks,released.position(),released.fromBucket(),released.isNoAi(),released.getHealth());released.discard();
            }catch(Throwable error){Blockbranch.LOGGER.error("EUROPEAN_PERCH_SMOKE FAIL bucket release",error);active=false;server.halt(false);return;}
            if(dry.isAlive()&&!dry.isInWater()&&dry.getDeltaMovement().y>.05)flopTicks++;
            if(ticks==10)check(swimmer.getNavigation().moveTo(-2.5,-62,-8.5,1),"native swimming path after water-state initialization");
            nearest=Math.min(nearest,follower.distanceToSqr(leader));if(swimmer.isInWater()){waterTicks++;double dx=swimmer.getX()-previous.x,dz=swimmer.getZ()-previous.z;swimDistance+=Math.sqrt(dx*dx+dz*dz);}previous=swimmer.position();
            if(ticks<480)return;
            try{
                var level=server.overworld();var witness=level.getEntity(WITNESS);check(witness instanceof EuropeanPerch,"native stored entity type");var saved=(EuropeanPerch)witness;
                check(saved.fromBucket()&&saved.requiresCustomPersistence()&&saved.isNoAi()&&saved.getHealth()==2&&saved.getCustomName().getString().equals("Saved perch"),"native fish save/reload");
                check(nearest<9,"native school convergence distance2="+nearest);check(swimmer.isAlive()&&waterTicks>100&&swimDistance>2,"actual horizontal swim "+swimDistance+" waterTicks="+waterTicks);
                check(!dry.isAlive()&&flopTicks>0,"native dry-land flopping and suffocation flops="+flopTicks+" alive="+dry.isAlive()+" pos="+dry.position());
                for(var p:population)check(p.isAlive()&&p.tickCount>=120,"active native fish ticks");
                for(var e:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(e instanceof EuropeanPerch&&!e.getUUID().equals(WITNESS))e.discard();
                Files.writeString(result(),"PASS\n");Blockbranch.LOGGER.info("EUROPEAN_PERCH_SMOKE PASS {} habitats=4 elevationIndependent=true food=true cooking=3 bucketPickupRelease=true bucketDataPersistence=true landFlopSuffocation=true schoolConvergence={} horizontalSwim={} population=16+actors ticks={} meanWholeTickMs={} maxWholeTickMs={}",reload?"RELOAD":"NEW",nearest,swimDistance,ticks,total/1e6/ticks,max/1e6);
            }catch(Throwable error){Blockbranch.LOGGER.error("EUROPEAN_PERCH_SMOKE FAIL",error);}finally{active=false;server.halt(false);}
        });
    }
    private static Path result(){return Path.of(System.getProperty("blockbranch.perchSmokeResult"));}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
