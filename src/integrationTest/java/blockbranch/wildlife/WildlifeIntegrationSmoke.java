package blockbranch.wildlife;

import blockbranch.Blockbranch;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.MobSpawnSettings;

/** Opt-in native audit: stock goal execution and actual interaction entry points. */
public final class WildlifeIntegrationSmoke {
    private static ServerLevel level;
    private static int row=-1,ticks;
    private static boolean active;
    private static int[] fearTicks;
    private static final class ThreatPlayer extends net.minecraft.world.entity.player.Player {
        net.minecraft.world.level.GameType mode=net.minecraft.world.level.GameType.SURVIVAL;
        ThreatPlayer(){super(level,new GameProfile(UUID.randomUUID(),"FearTarget"));}
        void mode(net.minecraft.world.level.GameType value){mode=value;value.updatePlayerAbilities(getAbilities());}
        @Override public net.minecraft.world.level.GameType gameMode(){return mode;}
    }
    private static final List<PathfinderMob> actors=new ArrayList<>();
    private static final List<LivingEntity> threats=new ArrayList<>();
    private static final List<Double> starts=new ArrayList<>();
    private static final List<String> results=new ArrayList<>();
    public static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
            level=server.overworld();
            for(int z=-2;z<=2;z++)for(int x=-2;x<=6;x++)level.setChunkForced(x,z,true);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
            for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(entity instanceof Mob||entity instanceof net.minecraft.world.entity.player.Player)entity.discard();
            habitatMatrix();
            for(int x=80;x<=112;x++)for(int z=-20;z<=20;z++)for(int y=-63;y<=-61;y++)level.setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);
            active=true;
        }catch(Throwable error){fail(error);}});
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!active)return;try{
            ++ticks;
            if(row<0){
                if(ticks<40)return;
                for(int x:new int[]{0,32,64,96})if(!level.isPositionEntityTicking(new BlockPos(x,-60,0)))return;
                next();return;
            }
            if(ticks==40){
                var nearby=level.getEntitiesOfClass(net.minecraft.world.entity.animal.wolf.Wolf.class,actors.getFirst().getBoundingBox().inflate(10,3,10));
                check(nearby.size()==1&&nearby.getFirst()==threats.getFirst(),"one intended indexed wolf in fear lane; stale saved actors must not interfere");
            }
            for(int i=0;i<actors.size();i++)if(actors.get(i).getGoalSelector().getAvailableGoals().stream().anyMatch(g->g.isRunning()&&g.getGoal() instanceof AvoidEntityGoal))fearTicks[i]++;
            if(row==2&&ticks==40)server.setDifficulty(Difficulty.PEACEFUL,true);
            if(row==2&&ticks==80)server.setDifficulty(Difficulty.NORMAL,true);
            if(ticks==1||ticks==40||ticks==120){
                var rabbit=actors.getFirst();var threat=threats.getFirst();
                Blockbranch.LOGGER.info("STOCK_FEAR_DIAGNOSTIC row={} tick={} rabbit={} ground={} floor={} variant={} threat={} alive={} visible={} enemy={} goals={}",row,ticks,rabbit.position(),rabbit.onGround(),level.getBlockState(rabbit.blockPosition().below()),((net.minecraft.world.entity.animal.rabbit.Rabbit)rabbit).getVariant(),threat.position(),threat.isAlive(),rabbit.getSensing().hasLineOfSight(threat),threat.canBeSeenAsEnemy(),rabbit.getGoalSelector().getAvailableGoals().stream().map(g->g.getGoal().getClass().getSimpleName()+":"+g.isRunning()).toList());
            }
            if(ticks<160)return;
            for(int i=0;i<actors.size();i++){
                var animal=actors.get(i);double distance=animal.distanceTo(threats.get(i));
                check(fearTicks[i]>=10&&distance>starts.get(i)+2,"stock avoidance moves away "+animal.getType()+" "+server.getWorldData().getDifficulty()+" start="+starts.get(i)+" end="+distance+" activeGoalTicks="+fearTicks[i]);
                results.add("fear "+server.getWorldData().getDifficulty()+" "+BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType())+" "+starts.get(i)+" -> "+distance);
            }
            next();
        }catch(Throwable error){fail(error);}});
    }
    private static void habitatMatrix()throws Exception{
        var matrix=new TreeMap<String,Object>();
        for(var holder:level.registryAccess().lookupOrThrow(Registries.BIOME).listElements().toList()){
            var settings=holder.value().getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS,MobSpawnSettings.EMPTY);
            var categories=new TreeMap<String,Object>();
            for(var category:MobCategory.values()){
                var entries=new ArrayList<Object>();var unique=new HashSet<String>();
                for(var entry:settings.getMobsToSpawn(category).unwrap()){
                    String id=BuiltInRegistries.ENTITY_TYPE.getKey(entry.value().type()).toString();
                    if(!unique.add(id)&&id.startsWith("blockbranch:"))throw new AssertionError("duplicate added wildlife entry "+holder.key()+" "+id);
                    entries.add(Map.of("type",id,"weight",entry.weight(),"min",entry.value().count().minInclusive(),"max",entry.value().count().maxInclusive()));
                }
                if(!entries.isEmpty())categories.put(category.getName(),entries);
            }
            matrix.put(holder.key().identifier().toString(),categories);
        }
        String json=new GsonBuilder().setPrettyPrinting().create().toJson(matrix);
        var parsed=com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        var baseline=Path.of("../wildlife-native-habitat-baseline.json");
        if(Files.exists(baseline)){
            var previous=com.google.gson.JsonParser.parseString(Files.readString(baseline)).getAsJsonObject();
            for(var entry:previous.entrySet())if(entry.getKey().startsWith("minecraft:"))check(entry.getValue().equals(parsed.get(entry.getKey())),"native biome table preserved "+entry.getKey());
        }
        for(String biome:List.of("dappled_oak_birch_woodland","boreal_spruce_birch_woodland","warm_subhumid_oak_pine_woodland","rolling_lowland_forb_grass_meadow")){
            String entries=matrix.get("blockbranch:"+biome).toString();for(String animal:List.of("sheep","pig","chicken","cow"))check(entries.contains("minecraft:"+animal),"missing stock habitat role "+biome+" "+animal);
        }
        for(String biome:List.of("open_inland_lake_sediment_bed","open_routed_river_channel","inland_lake_reedbed","gravel_bed_river_margin")){
            String entries=matrix.get("blockbranch:"+biome).toString();for(String animal:List.of("minecraft:salmon","minecraft:squid","blockbranch:european_perch"))check(entries.contains(animal),"freshwater habitat role "+biome+" "+animal);
        }
        for(var allocation:List.of(
            new String[]{"alpine_scree_slope","minecraft:goat"},
            new String[]{"cold_continental_fescue_steppe","minecraft:rabbit"},
            new String[]{"cold_dwarf_shrub_tundra","minecraft:rabbit"},
            new String[]{"continental_gravel_desert","minecraft:rabbit"},
            new String[]{"dry_continental_scrub_steppe","minecraft:horse","minecraft:donkey","blockbranch:american_bison"},
            new String[]{"humid_subtropical_oak_fern_woodland","blockbranch:wild_boar","minecraft:rabbit"},
            new String[]{"open_marine_shelf_coastal_sea","minecraft:cod","minecraft:squid"},
            new String[]{"deep_open_ocean_basin_bed","minecraft:cod","minecraft:squid"})){
            var categories=parsed.getAsJsonObject("blockbranch:"+allocation[0]);var flat=new ArrayList<String>();
            for(var entry:categories.entrySet())for(var item:entry.getValue().getAsJsonArray())flat.add(item.getAsJsonObject().get("type").getAsString());
            for(int i=1;i<allocation.length;i++)check(Collections.frequency(flat,allocation[i])==1,"exactly one habitat role "+allocation[0]+" "+allocation[i]);
            if(allocation[0].contains("ocean")||allocation[0].contains("marine"))check(!flat.contains("minecraft:dolphin")&&!flat.contains("minecraft:nautilus"),"no new deep dolphin/uncertain nautilus fallback");
            if(allocation[0].equals("humid_subtropical_oak_fern_woodland"))for(String excluded:List.of("cow","sheep","pig","chicken","parrot","panda"))check(!flat.contains("minecraft:"+excluded),"humid woodland not a blanket farm/jungle table "+excluded);
        }
        var goatFoot=new BlockPos(-24,-60,-24);var oldFloor=level.getBlockState(goatFoot.below());
        level.setBlock(goatFoot.below(),blockbranch.realistic.ScreeContent.LOOSE_SCREE.defaultBlockState(),2);
        try{check(level.getBlockState(goatFoot.below()).is(net.minecraft.tags.BlockTags.GOATS_SPAWNABLE_ON),"loose scree is actual native goat substrate");
            check(SpawnPlacements.checkSpawnRules(EntityTypes.GOAT,level,EntitySpawnReason.NATURAL,goatFoot,level.getRandom()),"native goat predicate accepts loose scree");
            check(WildlifeHabitat.desertRabbitSpawn(EntityTypes.RABBIT,level,goatFoot)==null,"rabbit adaptation absent outside procedural gravel desert");
        }finally{level.setBlock(goatFoot.below(),oldFloor,2);}
        Files.writeString(result().resolveSibling("habitat-matrix.json"),json);
        Blockbranch.LOGGER.info("WILDLIFE_HABITAT_MATRIX holders={} duplicateAddedWildlifeEntries=false",matrix.size());
    }
    private static void next()throws Exception{
        for(var actor:actors)actor.discard();for(var threat:threats)threat.discard();actors.clear();threats.clear();starts.clear();
        // Forced saved chunks can finish entity loading after SERVER_STARTED cleanup.
        // Clear the isolated QA cohort after readiness and before every difficulty row.
        for(var entity:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(entity instanceof Mob||entity instanceof net.minecraft.world.entity.player.Player)entity.discard();
        if(++row==Difficulty.values().length){
            Files.writeString(result(),"PASS\n"+String.join("\n",results)+"\n");active=false;
            Blockbranch.LOGGER.info("WILDLIFE_INTEGRATION_SMOKE PASS rows={}",results);level.getServer().halt(false);return;
        }
        level.getServer().setDifficulty(Difficulty.values()[row],true);ticks=0;fearTicks=new int[4];
        utility();beePolicy();WildlifeCombatPaths.check(level);results.add("native llama collision/goat ram damage+knockback/hot switch "+Difficulty.values()[row]+" PASS");
        for(int i=0;i<4;i++){
            PathfinderMob actor=switch(i){case 0->EntityTypes.RABBIT.create(level,EntitySpawnReason.COMMAND);case 1->EntityTypes.CAT.create(level,EntitySpawnReason.COMMAND);case 2->EntityTypes.OCELOT.create(level,EntitySpawnReason.COMMAND);default->EntityTypes.COD.create(level,EntitySpawnReason.COMMAND);};
            int x=i*32;
            actor.snapTo(x+.5,i==3?-62:-60,.5,0,0);actor.setOnGround(true);actor.getRandom().setSeed(1234+i);level.addFreshEntity(actor);
            LivingEntity threat;
            if(i==0){var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);wolf.setNoAi(true);threat=wolf;}
            else threat=new ThreatPlayer();
            threat.snapTo(x+4.5,i==3?-62:-60,.5,0,0);level.addFreshEntity(threat);
            actor.removeAllGoals(goal->!(goal instanceof AvoidEntityGoal));
            if(threat instanceof ThreatPlayer viewer){
                for(var mode:List.of(net.minecraft.world.level.GameType.CREATIVE,net.minecraft.world.level.GameType.SPECTATOR)){
                    viewer.mode(mode);actor.getSensing().tick();
                    for(int attempt=0;attempt<16;attempt++)for(var goal:actor.getGoalSelector().getAvailableGoals())check(!goal.getGoal().canUse(),"native player eligibility preserved "+mode+" "+actor.getType());
                }
                viewer.mode(net.minecraft.world.level.GameType.SURVIVAL);actor.getSensing().tick();
                // A wall blocks selection while preserving native visibility tests.
                int baseY=i==3?-63:-60;
                for(int z=-12;z<=12;z++)for(int y=baseY;y<=-55;y++)level.setBlock(new BlockPos(x+2,y,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
                actor.getSensing().tick();
                for(int attempt=0;attempt<16;attempt++)for(var goal:actor.getGoalSelector().getAvailableGoals())check(!goal.getGoal().canUse(),"native line-of-sight filter "+actor.getType());
                for(int z=-12;z<=12;z++)for(int y=baseY;y<=-55;y++)level.setBlock(new BlockPos(x+2,y,z),i==3&&y<=-61?net.minecraft.world.level.block.Blocks.WATER.defaultBlockState():net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
                actor.getSensing().tick();
            }
            actors.add(actor);threats.add(threat);starts.add((double)actor.distanceTo(threat));
        }
    }
    private static void beePolicy(){
        for(boolean playerTarget:new boolean[]{false,true}){
            var bee=EntityTypes.BEE.create(level,EntitySpawnReason.COMMAND);
            LivingEntity victim=playerTarget?new net.minecraft.world.entity.player.Player(level,new GameProfile(UUID.randomUUID(),"BeePolicy")){
                @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
            }:EntityTypes.SHEEP.create(level,EntitySpawnReason.COMMAND);
            float before=victim.getHealth();boolean allowed=playerTarget?row>=2:row>=1;
            boolean hit=bee.doHurtTarget(level,victim);
            check(hit==allowed&&(victim.getHealth()<before)==allowed,"actual bee sting damage "+Difficulty.values()[row]+" player="+playerTarget);
            check(bee.hasStung()==allowed,"bee post-sting state "+Difficulty.values()[row]);
            check(victim.hasEffect(net.minecraft.world.effect.MobEffects.POISON)==(allowed&&row>=2),"actual bee poison "+Difficulty.values()[row]+" player="+playerTarget);
        }
        results.add("bee sting/poison "+Difficulty.values()[row]+" PASS");
    }
    private static void utility(){
        var player=new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"Utility"),ClientInformation.createDefault());
        var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));cow.interact(player,InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO);
        check(player.getMainHandItem().is(Items.MILK_BUCKET),"cow milk bucket "+Difficulty.values()[row]);
        var sheep=EntityTypes.SHEEP.create(level,EntitySpawnReason.COMMAND);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));sheep.interact(player,InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO);check(sheep.isSheared(),"native sheep shearing");
        var animals=List.of(EntityTypes.COW.create(level,EntitySpawnReason.COMMAND),EntityTypes.PIG.create(level,EntitySpawnReason.COMMAND),EntityTypes.CHICKEN.create(level,EntitySpawnReason.COMMAND),EntityTypes.RABBIT.create(level,EntitySpawnReason.COMMAND));
        var foods=List.of(Items.WHEAT,Items.CARROT,Items.WHEAT_SEEDS,Items.CARROT);
        for(int i=0;i<animals.size();i++){var animal=animals.get(i);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(foods.get(i)));animal.interact(player,InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO);check(animal.isInLove()&&player.getMainHandItem().isEmpty(),"native breeding food "+animal.getType());}
        var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);wolf.getRandom().setSeed(42);
        for(int i=0;i<64&&!wolf.isTame();i++){player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BONE));wolf.interact(player,InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO);}
        check(wolf.isTame()&&wolf.isOwnedBy(player),"native wolf bone taming");
        var cod=EntityTypes.COD.create(level,EntitySpawnReason.COMMAND);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WATER_BUCKET));cod.interact(player,InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO);
        check(cod.isRemoved()&&player.getMainHandItem().is(Items.COD_BUCKET),"native cod bucket capture");
        results.add("utility "+Difficulty.values()[row]+" milk/shear/4breedingFoods/wolfTame/codBucket PASS");
    }
    private static Path result(){return Path.of(System.getProperty("blockbranch.integrationResult"));}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void fail(Throwable error){active=false;Blockbranch.LOGGER.error("WILDLIFE_INTEGRATION_SMOKE FAIL",error);try{Files.writeString(result(),"FAIL "+error+"\n");}catch(Exception ignored){}if(level!=null)level.getServer().halt(false);}
    private WildlifeIntegrationSmoke(){}
}
