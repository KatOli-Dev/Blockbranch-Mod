package blockbranch.wildlife;

import blockbranch.Blockbranch;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;

/** Native player-facing integration: registry/reverse lookup, creative tabs, use,
 * dispenser dispatch, book unlocks, food utility and the existing fish bucket. */
final class WildlifeItemSmoke {
    private static ServerLevel level;
    private static int ticks;
    private static boolean active,dispensersChecked;
    private static BrownBear scavenger;
    private static net.minecraft.world.entity.item.ItemEntity fishFood;
    private static final Map<BlockPos,EntityType<?>> dispensers=new LinkedHashMap<>();
    private static final List<String> rows=new ArrayList<>();
    private static final class TestPlayer extends net.minecraft.world.entity.player.Player {
        private final GameType mode;
        TestPlayer(GameType mode){super(level,new GameProfile(UUID.randomUUID(),"ItemTester"));this.mode=mode;getAbilities().instabuild=mode==GameType.CREATIVE;}
        @Override public GameType gameMode(){return mode;}
    }
    static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
            level=server.overworld();for(int z=-2;z<=2;z++)for(int x=-2;x<=3;x++)level.setChunkForced(x,z,true);
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS,false,server);
            for(var e:com.google.common.collect.ImmutableList.copyOf(level.getAllEntities()))if(e instanceof Mob||e instanceof net.minecraft.world.entity.player.Player)e.discard();
            CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(),true,level.registryAccess());
            var tab=BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.SPAWN_EGGS).value();
            var search=CreativeModeTabs.searchTab();
            require(WildlifeItems.SPAWN_EGGS.size()==8,"exactly eight egg types");
            for(var entry:WildlifeItems.SPAWN_EGGS.entrySet()){
                var type=entry.getKey();var item=entry.getValue();var stack=new ItemStack(item);
                require(item instanceof SpawnEggItem&&SpawnEggItem.getType(stack)==type&&SpawnEggItem.spawnsEntity(stack,type),"native egg entity data "+type);
                require(SpawnEggItem.byId(type).orElseThrow().value()==item,"reverse egg lookup "+type);
                var specimen=(Mob)type.create(level,EntitySpawnReason.COMMAND);require(specimen.getPickResult().is(item),"creative pick-block "+type);
                require(tab.getDisplayItems().stream().filter(s->s.is(item)).count()==1&&search.contains(stack),"creative category/search exactly once "+type);
                for(var mode:List.of(GameType.SURVIVAL,GameType.CREATIVE)){
                    var player=new TestPlayer(mode);var egg=new ItemStack(item,2);player.setItemInHand(InteractionHand.MAIN_HAND,egg);
                    var floor=new BlockPos(-8,-61,-8);if(type==WildlifeRuntime.EUROPEAN_PERCH)for(int y=-60;y<=-58;y++)level.setBlock(floor.atY(y),Blocks.WATER.defaultBlockState(),2);
                    var before=mobIds(type);var result=egg.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(floor),Direction.UP,floor,false)));
                    var after=mobIds(type);after.removeAll(before);require(result.consumesAction()&&after.size()==1,"actual egg use "+type+" "+mode);
                    require(egg.getCount()==(mode==GameType.CREATIVE?2:1),"native egg material consumption "+mode);
                    for(var uuid:after)level.getEntity(uuid).discard();
                    for(int y=-60;y<=-58;y++)level.setBlock(floor.atY(y),Blocks.AIR.defaultBlockState(),2);
                    if(specimen instanceof AgeableMob){
                        var offspring=(Mob)SpawnEggItem.spawnOffspringFromSpawnEgg(player,specimen,(EntityType)type,level,new Vec3(-4.5,-60,-8.5),new ItemStack(item,2)).orElseThrow();
                        require(offspring.getType()==type&&((AgeableMob)offspring).isBaby(),"native same-species baby egg interaction "+type);offspring.discard();
                    }
                }
                specimen.discard();rows.add("egg/pick/category/search/survival/creative "+BuiltInRegistries.ENTITY_TYPE.getKey(type));
                int x=dispensers.size()*5;var pos=new BlockPos(x,-59,8);level.setBlock(pos,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.SOUTH),3);
                var be=(DispenserBlockEntity)level.getBlockEntity(pos);be.setItem(0,new ItemStack(item,2));dispensers.put(pos,type);level.scheduleTick(pos,Blocks.DISPENSER,1);
                if(type==WildlifeRuntime.EUROPEAN_PERCH)for(int y=-60;y<=-58;y++)level.setBlock(new BlockPos(x,y,9),Blocks.WATER.defaultBlockState(),2);
            }
            var foodTab=BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.FOOD_AND_DRINKS).value();
            for(var item:List.of(WildlifeItems.VENISON,WildlifeItems.COOKED_VENISON,WildlifeItems.DUCK,WildlifeItems.COOKED_DUCK,WildlifeItems.PERCH,WildlifeItems.COOKED_PERCH)){
                require(foodTab.contains(new ItemStack(item))&&search.contains(new ItemStack(item)),"food creative/search "+item);
                var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);require(wolf.isFood(new ItemStack(item)),"native wolf diet "+item);
                for(var mode:List.of(GameType.SURVIVAL,GameType.CREATIVE)){
                    var feeder=new TestPlayer(mode);wolf.tame(feeder);wolf.setHealth(wolf.getMaxHealth()-8);float health=wolf.getHealth();
                    var food=new ItemStack(item,2);feeder.setItemInHand(InteractionHand.MAIN_HAND,food);
                    require(wolf.mobInteract(feeder,InteractionHand.MAIN_HAND).consumesAction()&&wolf.getHealth()>health,"actual native wolf feeding "+item+" "+mode);
                    require(food.getCount()==(mode==GameType.CREATIVE?2:1),"native feeding material consumption "+mode);
                }
            }
            require(BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.TOOLS_AND_UTILITIES).value().contains(new ItemStack(WildlifeItems.PERCH_BUCKET))&&search.contains(new ItemStack(WildlifeItems.PERCH_BUCKET)),"bucket creative/search");
            require(new ItemStack(WildlifeItems.PERCH_BUCKET).is(net.minecraft.tags.ItemTags.NAUTILUS_BUCKET_FOOD),"native fish bucket food parity");
            require(BrownBear.groundFood(new ItemStack(WildlifeItems.PERCH))&&BrownBear.groundFood(new ItemStack(WildlifeItems.COOKED_PERCH)),"bear already-hunted fish scavenging");
            for(var id:List.of("minecraft:river","minecraft:swamp","blockbranch:open_inland_lake_sediment_bed","blockbranch:open_routed_river_channel","blockbranch:inland_lake_reedbed","blockbranch:gravel_bed_river_margin"))require(PerchFishing.freshwater(id),"existing perch habitat fishing parity "+id);
            require(!PerchFishing.freshwater("minecraft:ocean"),"no marine fishing expansion");
            var nautilus=EntityTypes.NAUTILUS.create(level,EntitySpawnReason.COMMAND);require(!nautilus.isFood(new ItemStack(WildlifeItems.PERCH_BUCKET)),"do not expand native wild nautilus taming diet");nautilus.tame(new TestPlayer(GameType.SURVIVAL));require(nautilus.isFood(new ItemStack(WildlifeItems.PERCH_BUCKET)),"actual native nautilus fish-bucket food");
            scavenger=WildlifeRuntime.BROWN_BEAR.create(level,EntitySpawnReason.COMMAND);scavenger.snapTo(-16.5,-60,-16.5,0,0);scavenger.setHealth(scavenger.getMaxHealth()-5);level.addFreshEntity(scavenger);
            fishFood=new net.minecraft.world.entity.item.ItemEntity(level,-16.3,-60,-16.3,new ItemStack(WildlifeItems.PERCH));level.addFreshEntity(fishFood);
            recipes(server);
            var bucketPos=new BlockPos(-12,-59,8);level.setBlock(bucketPos,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.SOUTH),3);
            ((DispenserBlockEntity)level.getBlockEntity(bucketPos)).setItem(0,new ItemStack(WildlifeItems.PERCH_BUCKET));level.scheduleTick(bucketPos,Blocks.DISPENSER,1);
            active=true;
        }catch(Throwable error){fail(error);}});
        ServerTickEvents.END_SERVER_TICK.register(server->{if(!active||++ticks<10)return;try{
            if(!dispensersChecked){
            for(var e:dispensers.entrySet()){
                var be=(DispenserBlockEntity)level.getBlockEntity(e.getKey());require(be.getItem(0).getCount()==1,"actual dispenser egg consumed "+e.getValue());
                require(level.getEntitiesOfClass(Mob.class,new AABB(e.getKey().relative(Direction.SOUTH)).inflate(2),m->m.getType()==e.getValue()).size()==1,"actual dispenser spawns correct mob "+e.getValue());
            }
            var be=(DispenserBlockEntity)level.getBlockEntity(new BlockPos(-12,-59,8));require(be.getItem(0).is(Items.BUCKET),"fish bucket dispenser returns real empty bucket");
            require(level.getFluidState(new BlockPos(-12,-59,9)).is(net.minecraft.tags.FluidTags.WATER),"fish bucket dispenser places water");
            require(level.getEntitiesOfClass(EuropeanPerch.class,new AABB(new BlockPos(-12,-59,9)).inflate(2)).size()==1,"fish bucket dispenser releases perch");
            dispensersChecked=true;
            }
            if(ticks<80)return;
            require(!fishFood.isAlive()&&scavenger.huntCooldown()>2000&&scavenger.getHealth()>scavenger.getMaxHealth()-5,"actual bear scavenges perch item");
            Files.writeString(result(),"PASS\n"+String.join("\n",rows)+"\n8 native dispensers; fish bucket dispenser;9 recipe unlocks;food utility\n");
            Blockbranch.LOGGER.info("WILDLIFE_ITEM_SMOKE PASS {}",rows);active=false;server.halt(false);
        }catch(Throwable error){fail(error);}});
    }
    private static void recipes(net.minecraft.server.MinecraftServer server){
        var profile=new GameProfile(UUID.randomUUID(),"RecipeTester");var player=new ServerPlayer(server,level,profile,ClientInformation.createDefault());
        player.connection=new ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,CommonListenerCookie.createInitial(profile,false));
        player.getAdvancements();
        for(var raw:List.of(WildlifeItems.VENISON,WildlifeItems.DUCK,WildlifeItems.PERCH)){
            var stack=new ItemStack(raw);player.getInventory().setItem(0,stack);
            net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(player,player.getInventory(),stack);
            String path=BuiltInRegistries.ITEM.getKey(raw).getPath();
            for(String method:List.of("smelting","smoking","campfire_cooking")){
                var id=Blockbranch.id("cooked_"+path+"_from_"+method);var key=ResourceKey.create(Registries.RECIPE,id);
                require(player.getRecipeBook().contains(key),"inventory acquisition unlocks cooking recipe "+id);
                require(server.getRecipeManager().byKey(key).isPresent(),"unlocked recipe exists "+id);
            }
        }
    }
    private static Set<UUID> mobIds(EntityType<?> type){var ids=new HashSet<UUID>();for(var e:level.getAllEntities())if(e.getType()==type)ids.add(e.getUUID());return ids;}
    private static Path result(){return Path.of(System.getProperty("blockbranch.itemSmokeResult"));}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void fail(Throwable error){Blockbranch.LOGGER.error("WILDLIFE_ITEM_SMOKE FAIL",error);try{Files.writeString(result(),"FAIL "+error+"\n");}catch(Exception ignored){}active=false;if(level!=null)level.getServer().halt(false);}
}
