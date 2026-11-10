package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.realistic.ScreeContent;
import blockbranch.wildlife.WildlifeItems;
import blockbranch.wildlife.WildlifeRuntime;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Opt-in inventory and first-person wildlife item art inspection beside native items. */
final class FoodArtReview {
    private static final String SUBJECT=System.getProperty("blockbranch.foodArtReviewSubject","duck");
    private static final List<Item> HELD=switch(SUBJECT){
        case "rock_fragment"->List.of(ScreeContent.ANGULAR_ROCK_FRAGMENT,Items.FLINT);
        case "mallard_spawn"->List.of(WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.MALLARD),Items.CHICKEN_SPAWN_EGG);
        case "boar_spawn"->List.of(WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.WILD_BOAR),Items.PIG_SPAWN_EGG);
        case "bison_spawn"->List.of(WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.AMERICAN_BISON),Items.COW_SPAWN_EGG);
        case "deer_spawn"->List.of(WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.RED_DEER),Items.GOAT_SPAWN_EGG);
        case "beaver_spawn"->List.of(WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.EURASIAN_BEAVER),Items.RABBIT_SPAWN_EGG);
        case "bear_spawn"->List.of(WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.BROWN_BEAR),Items.POLAR_BEAR_SPAWN_EGG);
        case "aquatic_eggs"->List.of(WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.EUROPEAN_PERCH),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.HARBOR_SEAL));
        case "bucket"->List.of(WildlifeItems.PERCH_BUCKET,Items.COD_BUCKET);
        case "duck"->List.of(WildlifeItems.DUCK,WildlifeItems.COOKED_DUCK);
        case "venison"->List.of(WildlifeItems.VENISON,WildlifeItems.COOKED_VENISON);
        case "perch"->List.of(WildlifeItems.PERCH,WildlifeItems.COOKED_PERCH);
        default->List.of(WildlifeItems.DUCK,WildlifeItems.COOKED_DUCK,WildlifeItems.VENISON,WildlifeItems.COOKED_VENISON,WildlifeItems.PERCH,WildlifeItems.COOKED_PERCH,WildlifeItems.PERCH_BUCKET,Items.COD_BUCKET);
    };
    private static final List<Item> MODELS=switch(SUBJECT){
        case "rock_fragment"->List.of(ScreeContent.ANGULAR_ROCK_FRAGMENT,Items.FLINT,Items.CLAY_BALL,Items.QUARTZ,Items.RAW_IRON);
        case "mallard_spawn"->List.of(HELD.get(0),HELD.get(1),Items.PARROT_SPAWN_EGG,Items.TURTLE_SPAWN_EGG,WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.HARBOR_SEAL));
        case "boar_spawn"->List.of(HELD.get(0),HELD.get(1),Items.HOGLIN_SPAWN_EGG,WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.AMERICAN_BISON),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.BROWN_BEAR));
        case "bison_spawn"->List.of(HELD.get(0),HELD.get(1),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.WILD_BOAR),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.BROWN_BEAR),Items.GOAT_SPAWN_EGG);
        case "deer_spawn"->List.of(HELD.get(0),HELD.get(1),Items.COW_SPAWN_EGG,Items.HORSE_SPAWN_EGG,WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.BROWN_BEAR));
        case "beaver_spawn"->List.of(HELD.get(0),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.BROWN_BEAR),HELD.get(1),Items.FOX_SPAWN_EGG,Items.TURTLE_SPAWN_EGG);
        case "bear_spawn"->List.of(HELD.get(0),HELD.get(1),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.WILD_BOAR),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.AMERICAN_BISON),Items.WOLF_SPAWN_EGG);
        case "aquatic_eggs"->List.of(HELD.get(0),HELD.get(1),Items.COD_SPAWN_EGG,Items.DOLPHIN_SPAWN_EGG,Items.TURTLE_SPAWN_EGG);
        case "bucket"->List.of(WildlifeItems.PERCH_BUCKET,Items.COD_BUCKET,Items.SALMON_BUCKET);
        case "duck"->List.of(WildlifeItems.DUCK,WildlifeItems.COOKED_DUCK,Items.CHICKEN,Items.PORKCHOP,Items.BEEF);
        case "perch"->List.of(WildlifeItems.PERCH,WildlifeItems.COOKED_PERCH,Items.COD,Items.COOKED_COD,Items.SALMON);
        case "venison"->List.of(WildlifeItems.VENISON,WildlifeItems.COOKED_VENISON,Items.COOKED_BEEF);
        default->throw new IllegalArgumentException("Unknown food art subject: "+SUBJECT);
    };
    private static int stage,wait,held;
    private static double x,y,z;
    private static volatile boolean ready;
    private static boolean captured=true;
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static boolean tick(Minecraft client)throws Exception{
        if(stage==0){
            if(SUBJECT.equals("bucket"))PerchBucketDripCheck.verify(client.getResourceManager());
            ready=false;client.getSingleplayerServer().execute(()->{
                var server=client.getSingleplayerServer();var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.CREATIVE);
                // An elevated board avoids tall-grass planes from surrounding ground.
                var origin=player.blockPosition().offset(24,3,0);x=origin.getX()+.5;y=origin.getY()+1;z=origin.getZ()+.5;
                for(int dx=-5;dx<=5;dx++)for(int dz=-4;dz<=7;dz++)for(int dy=0;dy<=4;dy++)level.setBlock(origin.offset(dx,dy,dz),(dy==0?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);
                for(var drop:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(12)))drop.discard();
                player.connection.teleport(origin.getX()+.5,origin.getY()+1,origin.getZ()+.5,0,15);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"time set 6000");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),"weather clear");
                List<Item> comparison=SUBJECT.equals("rock_fragment")?List.of(ScreeContent.ANGULAR_ROCK_FRAGMENT,Items.FLINT,Items.CLAY_BALL,Items.QUARTZ,Items.RAW_IRON,Items.BRICK,Items.IRON_NUGGET,Items.AMETHYST_SHARD):SUBJECT.equals("mallard_spawn")?List.of(HELD.get(0),HELD.get(1),Items.PARROT_SPAWN_EGG,Items.TURTLE_SPAWN_EGG,WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.HARBOR_SEAL),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.EUROPEAN_PERCH),Items.COD_SPAWN_EGG,Items.SALMON_SPAWN_EGG):SUBJECT.equals("boar_spawn")?List.of(HELD.get(0),HELD.get(1),Items.HOGLIN_SPAWN_EGG,WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.AMERICAN_BISON),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.BROWN_BEAR),Items.WOLF_SPAWN_EGG,Items.COW_SPAWN_EGG,WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.RED_DEER)):SUBJECT.equals("bison_spawn")?List.of(HELD.get(0),HELD.get(1),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.WILD_BOAR),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.BROWN_BEAR),Items.GOAT_SPAWN_EGG,WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.RED_DEER),Items.HORSE_SPAWN_EGG,Items.WOLF_SPAWN_EGG):SUBJECT.equals("deer_spawn")?List.of(HELD.get(0),HELD.get(1),Items.COW_SPAWN_EGG,Items.HORSE_SPAWN_EGG,WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.BROWN_BEAR),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.WILD_BOAR),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.AMERICAN_BISON),Items.WOLF_SPAWN_EGG):SUBJECT.equals("beaver_spawn")?List.of(HELD.get(0),HELD.get(1),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.BROWN_BEAR),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.WILD_BOAR),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.AMERICAN_BISON),Items.FOX_SPAWN_EGG,Items.PIG_SPAWN_EGG,Items.TURTLE_SPAWN_EGG):SUBJECT.equals("bear_spawn")?List.of(HELD.get(0),HELD.get(1),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.WILD_BOAR),WildlifeItems.SPAWN_EGGS.get(WildlifeRuntime.AMERICAN_BISON),Items.WOLF_SPAWN_EGG,Items.FOX_SPAWN_EGG,Items.PIG_SPAWN_EGG,Items.COW_SPAWN_EGG):SUBJECT.equals("aquatic_eggs")?List.of(HELD.get(0),HELD.get(1),Items.COD_SPAWN_EGG,Items.DOLPHIN_SPAWN_EGG,Items.TURTLE_SPAWN_EGG,Items.SALMON_SPAWN_EGG,Items.PUFFERFISH_SPAWN_EGG,Items.TROPICAL_FISH_SPAWN_EGG):List.of(WildlifeItems.DUCK,WildlifeItems.COOKED_DUCK,Items.CHICKEN,Items.COOKED_CHICKEN,Items.PORKCHOP,Items.COOKED_PORKCHOP,WildlifeItems.VENISON,WildlifeItems.COOKED_VENISON,Items.BEEF,
                    Items.COOKED_BEEF,Items.RABBIT,Items.COOKED_RABBIT,WildlifeItems.PERCH,WildlifeItems.COOKED_PERCH,Items.COD,Items.COOKED_COD,Items.SALMON,Items.COOKED_SALMON,
                    WildlifeItems.PERCH_BUCKET,Items.COD_BUCKET,Items.SALMON_BUCKET,Items.PUFFERFISH_BUCKET,Items.TROPICAL_FISH_BUCKET);
                for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,ItemStack.EMPTY);
                for(int slot=0;slot<comparison.size();slot++)player.getInventory().setItem(slot+9,new ItemStack(comparison.get(slot)));
                for(int slot=0;slot<HELD.size();slot++)player.getInventory().setItem(slot,new ItemStack(HELD.get(slot)));
                player.getInventory().setSelectedSlot(0);player.inventoryMenu.broadcastChanges();ready=true;
            });stage=1;return false;
        }
        if(!ready)return false;
        if(stage==1){if(++wait<40)return false;
            var screen=new CreativeModeInventoryScreen(client.player,client.level.enabledFeatures(),false);client.gui.setScreen(screen);
            var select=CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab",CreativeModeTab.class);select.setAccessible(true);select.invoke(screen,BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.INVENTORY).value());
            for(String axis:new String[]{"xpos","ypos"}){var field=net.minecraft.client.MouseHandler.class.getDeclaredField(axis);field.setAccessible(true);field.setDouble(client.mouseHandler,0);}
            stage=2;wait=0;return false;
        }
        if(stage==2){for(String axis:new String[]{"xpos","ypos"}){var field=net.minecraft.client.MouseHandler.class.getDeclaredField(axis);field.setAccessible(true);field.setDouble(client.mouseHandler,0);}
            if(++wait<50)return false;for(int i=0;i<HELD.size();i++)require(client.player.getInventory().getItem(i).is(HELD.get(i)),"native inventory item synced");capture(client,"food-inventory.png");stage=3;return false;}
        if(stage==3){if(!captured)return false;client.gui.setScreen(null);stage=4;wait=0;return false;}
        if(stage==4){
            if(!captured)return false;
            if(held==HELD.size()){
                ready=false;client.getSingleplayerServer().execute(()->{
                    var server=client.getSingleplayerServer();var player=server.getPlayerList().getPlayers().getFirst();player.setGameMode(GameType.SPECTATOR);player.connection.teleport(x,y+.4,z+5,180,0);
                    List<Item> items=MODELS;
                    for(int i=0;i<items.size();i++){
                        String id=BuiltInRegistries.ITEM.getKey(items.get(i)).toString();
                        String command="summon minecraft:item_display "+(x+(i-(items.size()-1)/2.0)*2)+" "+(y+2)+" "+z+" {item:{id:\""+id+"\",count:1},item_display:\"gui\",billboard:\"center\",transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],scale:[2f,2f,2f],right_rotation:[0f,0f,0f,1f]}}";
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),command);
                    }ready=true;
                });stage=6;wait=0;return false;
            }
            ready=false;int selected=held;client.getSingleplayerServer().execute(()->{var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();player.getInventory().setSelectedSlot(selected);player.inventoryMenu.broadcastChanges();ready=true;});
            client.player.getInventory().setSelectedSlot(selected);stage=5;wait=0;return false;
        }
        if(stage==6){
            if(++wait<80)return false;
            require(client.level.getBlockState(BlockPos.containing(client.player.getEyePosition())).isAir(),"comparison camera eye is in air");
            int visible=0;var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
            for(var entity:client.level.entitiesForRendering())if(entity instanceof net.minecraft.world.entity.Display.ItemDisplay display&&entity.distanceToSqr(client.player)<100&&frustum!=null&&frustum.isVisible(entity.getBoundingBox())){
                require(display.renderState()!=null,"comparison display render state ready");
                var scale=display.renderState().transformation().get(1).scale();
                require(Math.abs(scale.x()-2)<.001&&Math.abs(scale.y()-2)<.001&&Math.abs(scale.z()-2)<.001,"comparison display rendered at requested 2x scale");
                visible++;
            }
            require(visible==MODELS.size(),"all native comparison item displays in capture frustum");capture(client,SUBJECT+"-model-comparison.png");stage=7;return false;
        }
        if(stage==7){if(!captured)return false;Files.writeString(client.gameDirectory.toPath().resolve("food-art-review.txt"),"PASS native inventory, "+HELD.size()+" held views and "+MODELS.size()+" enlarged comparison item models; staged daylight only\n");return true;}
        if(++wait<50)return false;
        var item=HELD.get(held);require(client.player.getMainHandItem().is(item),"selected held item synced");
        String name=BuiltInRegistries.ITEM.getKey(item).getPath();capture(client,name+"-held.png");Blockbranch.LOGGER.info("FOOD_ART_CAPTURE {} stagedDaylight=true",name);held++;stage=4;return false;
    }
    private static void capture(Minecraft client,String file){captured=false;net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->captured=Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file))));}
    private FoodArtReview(){}
}
