package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.wildlife.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Opt-in staged review after the ordinary singleplayer observation; never runs in normal play. */
final class WildlifePlayerIntegrationReview {
    private static final boolean EGG_ART=Boolean.getBoolean("blockbranch.eggArtReview");
    private static final boolean MILESTONE=Boolean.getBoolean("blockbranch.milestoneVideoReview");
    private static final boolean SEAL_ART=Boolean.getBoolean("blockbranch.sealArtReview");
    private static final boolean BISON_THREAT_ART=Boolean.getBoolean("blockbranch.bisonThreatArtReview");
    private static final boolean BISON_GRAZE_ART=Boolean.getBoolean("blockbranch.bisonGrazeArtReview");
    private static final boolean BISON_ART=Boolean.getBoolean("blockbranch.bisonArtReview");
    private static final boolean BEAVER_GROOM_ART=Boolean.getBoolean("blockbranch.beaverGroomArtReview");
    private static final boolean BEAVER_ART=Boolean.getBoolean("blockbranch.beaverArtReview");
    private static final boolean BEAR_FORAGE_ART=Boolean.getBoolean("blockbranch.bearForageArtReview");
    private static final boolean BEAR_ART=Boolean.getBoolean("blockbranch.bearArtReview");
    private static final boolean PIGLET_ART=Boolean.getBoolean("blockbranch.pigletArtReview");
    private static final boolean BOAR_ROOT_ART=Boolean.getBoolean("blockbranch.boarRootArtReview");
    private static final boolean BOAR_ART=Boolean.getBoolean("blockbranch.boarArtReview");
    private static final boolean DEER_ANTLER_ART=Boolean.getBoolean("blockbranch.deerAntlerArtReview");
    private static final boolean DEER_ART=Boolean.getBoolean("blockbranch.deerArtReview");
    private static final boolean DEER_JUVENILE_ART=Boolean.getBoolean("blockbranch.deerJuvenileArtReview");
    private static final boolean DEER_REST_ART=Boolean.getBoolean("blockbranch.deerRestArtReview");
    private static final boolean DEER_GRAZE_ART=Boolean.getBoolean("blockbranch.deerGrazeArtReview");
    private static final boolean MALLARD_JUVENILE_ART=Boolean.getBoolean("blockbranch.mallardJuvenileArtReview");
    private static final boolean MALLARD_PECK_ART=Boolean.getBoolean("blockbranch.mallardPeckArtReview");
    private static final boolean MALLARD_ART=Boolean.getBoolean("blockbranch.mallardArtReview");
    private static final boolean FOCUSED_ART=SEAL_ART||BISON_ART||BEAVER_ART||BEAR_ART||MALLARD_ART||DEER_ART||PIGLET_ART;
    private static final String[] SUBJECTS=PIGLET_ART?new String[]{"boar"}:DEER_ART?new String[]{"deer"}:MALLARD_ART?new String[]{"mallard"}:BEAR_ART?new String[]{"bear"}:BEAVER_ART?new String[]{"beaver"}:BISON_ART?new String[]{"bison"}:SEAL_ART?new String[]{"seal"}:MILESTONE?new String[]{"mallard","seal"}:new String[]{"boar","deer","bear","bison","beaver","seal"};
    private static final int STEPS_PER_SPECIES=MALLARD_ART?18:(BISON_GRAZE_ART||BISON_THREAT_ART||BEAVER_GROOM_ART)?16:(BEAR_ART||DEER_ART||PIGLET_ART)?16:(BISON_ART||BEAVER_ART)?12:SEAL_ART?10:6;
    private static int stage,wait;
    private static volatile boolean serverReady,captured;
    private static double x,y,z;
    private static final List<UUID> actors=new ArrayList<>();
    private static UUID forageFood;
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    static boolean tick(Minecraft client)throws Exception{
        if(MILESTONE)WildlifeMilestoneRecorder.tick(client);
        if(stage==-1)return !MILESTONE||WildlifeMilestoneRecorder.finish(client);
        if(stage==0){
            require(!DEER_ANTLER_ART||DEER_ART,"antler review requires focused deer mode");
            require(!DEER_GRAZE_ART||DEER_ART&&!DEER_ANTLER_ART,"grazing review requires focused deer mode without antler mode");
            require(!DEER_JUVENILE_ART||DEER_ART&&!DEER_REST_ART&&!DEER_GRAZE_ART&&!DEER_ANTLER_ART,"juvenile review requires focused deer mode only");
            if(DEER_JUVENILE_ART)DeerJuvenileAudit.verify();
            require(!DEER_REST_ART||DEER_ART&&!DEER_GRAZE_ART&&!DEER_ANTLER_ART,"rest review requires focused deer mode only");
            if(DEER_REST_ART)DeerRestAudit.verify();
            if(DEER_GRAZE_ART){DeerGrazeAudit.verify();Files.writeString(client.gameDirectory.toPath().resolve("deer-graze-keyframes.txt"),"");}
            require(!MALLARD_JUVENILE_ART||MALLARD_ART&&!MALLARD_PECK_ART,"juvenile review requires focused mallard mode without peck mode");
            if(MALLARD_JUVENILE_ART)MallardJuvenileAudit.verify();
            require(!MALLARD_PECK_ART||MALLARD_ART,"peck review requires focused mallard mode");
            if(MALLARD_PECK_ART)MallardPeckAudit.verify();
            require(!BEAVER_GROOM_ART||BEAVER_ART,"groom review requires focused beaver mode");
            if(BEAVER_GROOM_ART)BeaverGroomAudit.verify();
            require(!BEAR_FORAGE_ART||BEAR_ART,"forage review requires focused bear mode");
            if(BEAR_FORAGE_ART)BearForageAudit.verify();
            require(!BISON_THREAT_ART||BISON_ART&&!BISON_GRAZE_ART,"threat review requires focused bison mode without grazing mode");
            if(BISON_THREAT_ART)BisonThreatAudit.verify();
            require(!BISON_GRAZE_ART||BISON_ART,"grazing review requires focused bison mode");
            if(BISON_GRAZE_ART)BisonGrazeAudit.verify();
            require(!BOAR_ROOT_ART||PIGLET_ART&&!BOAR_ART,"rooting review requires focused boar family mode without portrait mode");
            if(BOAR_ROOT_ART)BoarRootAudit.verify();
            require(!BOAR_ART||PIGLET_ART,"adult boar review requires focused boar family mode");
            require(!FOCUSED_ART||!MILESTONE,"focused art and milestone review modes are separate");
            require((SEAL_ART?1:0)+(BISON_ART?1:0)+(BEAVER_ART?1:0)+(BEAR_ART?1:0)+(MALLARD_ART?1:0)+(DEER_ART?1:0)+(PIGLET_ART?1:0)<=1,"select one focused species");
            if(SEAL_ART)SealScaleAudit.verifyMesh();
            PerchBucketDripCheck.verify(client.getResourceManager());BoarModelCheck.verifyRevision();BearModelCheck.verifyRevision();DeerModelCheck.verifyRevision();WildlifeModelGroundAudit.verify();WildlifeFaceAudit.verify();
            x=client.player.getX();y=client.player.getY();z=client.player.getZ();
            serverReady=false;client.getSingleplayerServer().execute(()->{
                var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();player.setGameMode(GameType.CREATIVE);
                int slot=0;for(var item:WildlifeItems.SPAWN_EGGS.values())player.getInventory().setItem(slot++,new ItemStack(item));
                player.getInventory().setItem(8,new ItemStack(WildlifeItems.PERCH_BUCKET));slot=9;
                for(var item:List.of(WildlifeItems.VENISON,WildlifeItems.COOKED_VENISON,WildlifeItems.DUCK,WildlifeItems.COOKED_DUCK,WildlifeItems.PERCH,WildlifeItems.COOKED_PERCH))player.getInventory().setItem(slot++,new ItemStack(item));
                player.inventoryMenu.broadcastChanges();serverReady=true;
            });stage=1;return false;
        }
        if(stage==1){if(!serverReady||!client.player.isCreative()||++wait<30)return false;
            var screen=new CreativeModeInventoryScreen(client.player,client.level.enabledFeatures(),false);client.gui.setScreen(screen);
            var select=CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab",CreativeModeTab.class);select.setAccessible(true);select.invoke(screen,CreativeModeTabs.searchTab());
            var field=CreativeModeInventoryScreen.class.getDeclaredField("searchBox");field.setAccessible(true);((EditBox)field.get(screen)).setValue(EGG_ART?"spawn":"blockbranch:spawn_egg");
            var refresh=CreativeModeInventoryScreen.class.getDeclaredMethod("refreshSearchResults");refresh.setAccessible(true);refresh.invoke(screen);
            if(EGG_ART)screen.getMenu().scrollTo(1);
            stage=2;wait=0;return false;
        }
        if(stage==2){
            if(EGG_ART)for(String axis:new String[]{"xpos","ypos"}){var field=net.minecraft.client.MouseHandler.class.getDeclaredField(axis);field.setAccessible(true);field.setDouble(client.mouseHandler,0);}
            if(++wait<50)return false;var screen=(CreativeModeInventoryScreen)client.gui.screen();
            var items=screen.getMenu().items;require(EGG_ART?items.size()>8:items.size()==8,"creative search count="+items.size());
            if(EGG_ART){
                int custom=0,nativeEggs=0;
                for(int slot=0;slot<45;slot++){var stack=screen.getMenu().getSlot(slot).getItem();if(WildlifeItems.SPAWN_EGGS.containsValue(stack.getItem()))custom++;else if(stack.getItem() instanceof SpawnEggItem)nativeEggs++;}
                require(custom==8&&nativeEggs>0,"visible adjacent vanilla/custom egg comparison custom="+custom+" native="+nativeEggs);
                Blockbranch.LOGGER.info("EGG_ART_NATIVE_COMPARISON PASS visibleCustom={} visibleVanilla={}",custom,nativeEggs);
            }
            for(var item:WildlifeItems.SPAWN_EGGS.values())require(items.stream().anyMatch(stack->stack.is(item)),"creative egg result "+item);
            for(var item:WildlifeItems.SPAWN_EGGS.values())require(!new ItemStack(item).getHoverName().getString().startsWith("item.blockbranch"),"translated egg name");
            capture(client,EGG_ART?"animal-eggs-vanilla-comparison-v2.png":"animal-eggs-search-v1.png");stage=3;return false;
        }
        if(stage==3&&EGG_ART){if(!captured)return false;client.gui.setScreen(null);
            Files.writeString(Path.of(System.getProperty("blockbranch.singleplayerResult")).getParent().resolve("egg-art-review.txt"),"PASS all eight custom egg icons visible adjacent to native eggs at normal creative UI scale; native model regressions retained\n");
            stage=-1;return true;}
        if(stage==3&&MILESTONE){
            if(!captured)return false;var screen=(CreativeModeInventoryScreen)client.gui.screen();
            var field=CreativeModeInventoryScreen.class.getDeclaredField("searchBox");field.setAccessible(true);((EditBox)field.get(screen)).setValue("duck");
            var refresh=CreativeModeInventoryScreen.class.getDeclaredMethod("refreshSearchResults");refresh.setAccessible(true);refresh.invoke(screen);
            Blockbranch.LOGGER.info("MILESTONE_VIDEO_ITEMS_BEGIN raw/cooked duck; silent staged native review");WildlifeMilestoneRecorder.start();stage=102;wait=0;return false;
        }
        if(stage==102){
            if(++wait<120)return false;var items=((CreativeModeInventoryScreen)client.gui.screen()).getMenu().items;
            require(items.stream().anyMatch(stack->stack.is(WildlifeItems.DUCK))&&items.stream().anyMatch(stack->stack.is(WildlifeItems.COOKED_DUCK)),"both duck food textures rendered in native search");
            capture(client,"duck-food-items-v2.png");stage=103;wait=0;return false;
        }
        if(stage==103){if(!captured||++wait<30)return false;client.gui.setScreen(null);stage=4;wait=0;return false;}
        if(stage==3){if(!captured)return false;var screen=(CreativeModeInventoryScreen)client.gui.screen();
            var select=CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab",CreativeModeTab.class);select.setAccessible(true);select.invoke(screen,BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.INVENTORY).value());stage=100;wait=0;return false;}
        if(stage==100){if(++wait<40)return false;capture(client,"animal-items-inventory-v1.png");stage=101;return false;}
        if(stage==101){if(!captured)return false;client.gui.setScreen(null);stage=4;wait=0;return false;}
        if(stage>=4&&stage<4+SUBJECTS.length*STEPS_PER_SPECIES){
            int species=(stage-4)/STEPS_PER_SPECIES,step=(stage-4)%STEPS_PER_SPECIES;String name=SUBJECTS[species];
            if(step==0){if(!captured)return false;stageActors(client,species);stage++;wait=0;return false;}
            if(step==1){if(!serverReady)return false;++wait;if(MILESTONE&&wait==20)startWalk(client);if(wait<70)return false;
                if(SEAL_ART){int measured=0;for(var entity:client.level.entitiesForRendering())if(entity instanceof HarborSeal seal&&actors.contains(seal.getUUID())){SealScaleAudit.measure(client,seal);measured++;}require(measured==3,"adult and pup native scale witnesses");}
                if(BISON_ART){int measured=0;for(var entity:client.level.entitiesForRendering())if(entity instanceof AmericanBison bison&&actors.contains(bison.getUUID())){BisonScaleAudit.measure(client,bison);measured++;}require(measured==3,"adult and calf native scale witnesses");}
                capture(client,name+"-grounded-family-v4.png");stage++;return false;}
            if(step==2||step==4||step==6||step==8||((BISON_ART||BEAVER_ART||BEAR_ART||MALLARD_ART||DEER_ART||PIGLET_ART)&&step==10)||((BEAR_ART||MALLARD_ART||DEER_ART||PIGLET_ART||BISON_GRAZE_ART||BISON_THREAT_ART||BEAVER_GROOM_ART)&&(step==12||step==14))||(MALLARD_ART&&step==16)){if(!captured)return false;stageCamera(client,(step-2)/2);stage++;wait=0;return false;}
            if(!serverReady||++wait<60)return false;
            if(MILESTONE)requireVisible(client,actors.get(step==3?0:1));
            if(MALLARD_PECK_ART)MallardPeckAudit.observe(client,actors.get(step==5||step==13||step==17?1:step==7||step==11?2:0));
            if(MALLARD_JUVENILE_ART)requireVisible(client,actors.get(step==3?0:step==5?1:2));
            else if(MALLARD_ART)requireVisible(client,actors.get(step==5||step==13||step==17?1:step==7||step==11?2:0));
            if(BISON_THREAT_ART){requireVisible(client,actors.get(step==5||step==11?1:step==7||step==15?2:0));BisonThreatAudit.observe(client,actors.get(step==5||step==11?1:step==7||step==15?2:0));}
            if(BISON_GRAZE_ART){requireVisible(client,actors.get(step==5?1:step==7||step==11||step==15?2:0));BisonGrazeAudit.observe(client,actors.get(step==5?1:step==7||step==11||step==15?2:0));}
            if(BOAR_ROOT_ART){requireVisible(client,actors.get(step==5?1:step==7||step==11||step==15?2:0));BoarRootAudit.observe(client,actors.get(step==5?1:step==7||step==11||step==15?2:0));}
            else if(PIGLET_ART)requireVisible(client,actors.get(step==5?1:step>=7&&(!BOAR_ART||step==7)?2:0));
            if(DEER_ART)requireVisible(client,actors.get(DEER_JUVENILE_ART?(step==3?0:step==5?1:2):DEER_REST_ART?(step==5||step==13?1:step==7||step==11||step==15?2:0):step==5?1:step==7||step==11||(!DEER_ANTLER_ART&&step==15)?2:0));
            if(DEER_GRAZE_ART)DeerGrazeAudit.observe(client,actors.get(step==5?1:step==7||step==11||step==15?2:0));
            if(BEAVER_GROOM_ART)requireVisible(client,actors.get(step==5?1:step==7||step==11||step==15?2:0));
            else if(BEAR_FORAGE_ART){requireVisible(client,actors.get(step==5?1:step==7||step==11||step==15?2:0));BearForageAudit.observe(client,actors.get(step==5?1:step==7||step==11||step==15?2:0));}
            else if(BEAVER_ART||BEAR_ART)requireVisible(client,actors.get(step==5?1:step==7||BEAR_ART&&(step==11||step==13)?2:0));
            if(MALLARD_JUVENILE_ART)capture(client,name+switch(step){case 3->"-adult-side-control.png";case 5->"-hen-rest-control.png";case 7->"-juvenile-side.png";case 9->"-juvenile-front.png";case 11->"-juvenile-rest-quarter.png";case 13->"-juvenile-forage-quarter.png";case 15->"-juvenile-flight.png";case 17->"-juvenile-rear-quarter.png";default->throw new AssertionError("mallard juvenile capture step");});
            else if(MALLARD_PECK_ART)capture(client,name+switch(step){case 3->"-peck-drake-side.png";case 5->"-hen-standing-control.png";case 7->"-peck-duckling-side.png";case 9->"-peck-drake-front.png";case 11->"-peck-duckling-front.png";case 13->"-peck-hen-side.png";case 15->"-peck-drake-quarter.png";case 17->"-peck-hen-front.png";default->throw new AssertionError("mallard peck capture step");});
            else if(MALLARD_ART)capture(client,name+switch(step){case 3->"-drake-side.png";case 5->"-hen-rest.png";case 7->"-duckling-side.png";case 9->"-drake-face.png";case 11->"-duckling-face.png";case 13->"-hen-side.png";case 15->"-wing-pose.png";case 17->"-hen-face.png";default->throw new AssertionError("mallard capture step");});
            else if(BOAR_ART&&step>=9)capture(client,name+switch(step){case 9->"-adult-face.png";case 11->"-adult-quarter.png";case 13->"-adult-other-side.png";case 15->"-adult-rear.png";default->throw new AssertionError("adult boar capture step");});
            else if(BEAR_FORAGE_ART)capture(client,name+switch(step){case 3->"-forage-adult-side.png";case 5->"-standing-control.png";case 7->"-forage-cub-side.png";case 9->"-forage-adult-front.png";case 11->"-forage-cub-front.png";case 13->"-forage-adult-quarter.png";case 15->"-forage-cub-quarter.png";default->throw new AssertionError("bear forage capture step");});
            else if(BISON_THREAT_ART)capture(client,name+switch(step){case 3->"-threat-side.png";case 5->"-neutral-side.png";case 7->"-calf-threat-guard-side.png";case 9->"-threat-front.png";case 11->"-neutral-front.png";case 13->"-threat-quarter.png";case 15->"-calf-threat-guard-front.png";default->throw new AssertionError("bison threat capture step");});
            else if(BISON_GRAZE_ART)capture(client,name+switch(step){case 3->"-graze-adult-side.png";case 5->"-standing-control.png";case 7->"-graze-calf-side.png";case 9->"-graze-adult-front.png";case 11->"-graze-calf-front.png";case 13->"-graze-adult-quarter.png";case 15->"-graze-calf-quarter.png";default->throw new AssertionError("bison grazing capture step");});
            else if(BOAR_ROOT_ART)capture(client,name+switch(step){case 3->"-root-adult-side.png";case 5->"-standing-control.png";case 7->"-root-piglet-side.png";case 9->"-root-adult-front.png";case 11->"-root-piglet-front.png";case 13->"-root-adult-quarter.png";case 15->"-root-piglet-quarter.png";default->throw new AssertionError("boar rooting capture step");});
            else if(PIGLET_ART)capture(client,name+switch(step){case 3->"-standing-side-v4.png";case 5->"-rest-side-v4.png";case 7->"-piglet-side.png";case 9->"-piglet-face.png";case 11->"-piglet-top.png";case 13->"-piglet-other-side.png";case 15->"-piglet-rear.png";default->throw new AssertionError("piglet capture step");});
            else if(BEAVER_GROOM_ART)capture(client,name+switch(step){case 3->"-groom-adult-side.png";case 5->"-standing-control.png";case 7->"-groom-kit-side.png";case 9->"-groom-adult-front.png";case 11->"-groom-kit-front.png";case 13->"-groom-adult-quarter.png";case 15->"-groom-kit-quarter.png";default->throw new AssertionError("beaver grooming capture step");});
            else if(DEER_JUVENILE_ART)capture(client,name+switch(step){case 3->"-adult-stag-control.png";case 5->"-adult-hind-rest-control.png";case 7->"-juvenile-side.png";case 9->"-juvenile-front.png";case 11->"-juvenile-rest-quarter.png";case 13->"-juvenile-graze-quarter.png";case 15->"-juvenile-rear-quarter.png";default->throw new AssertionError("deer juvenile capture step");});
            else if(DEER_REST_ART)capture(client,name+switch(step){case 3->"-rest-stag-side.png";case 5->"-rest-hind-side.png";case 7->"-rest-calf-side.png";case 9->"-standing-stag-control.png";case 11->"-standing-calf-control.png";case 13->"-rest-hind-front.png";case 15->"-rest-calf-quarter.png";default->throw new AssertionError("deer rest capture step");});
            else if(DEER_GRAZE_ART)capture(client,name+switch(step){case 3->"-graze-stag-side.png";case 5->"-standing-hind-control.png";case 7->"-graze-calf-side.png";case 9->"-graze-stag-front.png";case 11->"-graze-calf-front.png";case 13->"-graze-stag-quarter.png";case 15->"-graze-calf-quarter.png";default->throw new AssertionError("deer grazing capture step");});
            else if(DEER_ART)capture(client,name+switch(step){case 3->"-standing-side-v4.png";case 5->"-rest-side-v4.png";case 7->"-calf-side.png";case 9->DEER_ANTLER_ART?"-antler-front.png":"-face.png";case 11->"-calf-face.png";case 13->DEER_ANTLER_ART?"-antler-quarter.png":"-face-quarter.png";case 15->DEER_ANTLER_ART?"-antler-rear-quarter.png":"-calf-face-quarter.png";default->throw new AssertionError("deer capture step");});
            else capture(client,name+(step==13?"-cub-standing.png":step==15?"-rear-quarter.png":step==3?"-standing-side-v4.png":step==5?"-rest-side-v4.png":step==7?((BISON_ART||DEER_ART)?"-calf-side.png":BEAR_ART?"-cub-side.png":BEAVER_ART?"-kit-side.png":"-pup-side.png"):step==11?(BEAVER_ART?"-tail-top.png":BEAR_ART?"-cub-face.png":"-calf-face.png"):"-face.png"));stage++;return false;
        }
        if(!captured)return false;
        Files.writeString(Path.of(System.getProperty("blockbranch.singleplayerResult")).getParent().resolve("player-integration-review.txt"),MILESTONE?"PASS native duck food icons; staged mallard/seal navigation and rest captures; natural behavior not surveyed\n":"PASS native creative egg search; baked pose regressions; explicitly staged "+SUBJECTS.length+" species family/standing/rest views\n");
        Blockbranch.LOGGER.info("PLAYER_INTEGRATION_REVIEW PASS");stage=-1;return !MILESTONE;
    }
    private static void capture(Minecraft client,String file){captured=false;net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->captured=Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file))));}
    private static void startWalk(Minecraft client){client.getSingleplayerServer().execute(()->{
        var mob=(Mob)client.getSingleplayerServer().overworld().getEntity(actors.getFirst());
        require(mob!=null&&mob.onGround(),"native walking subject settled on review floor");
        require(mob.getNavigation().moveTo(mob.getX(),mob.getY(),mob.getZ()+2,.8),"staged native navigation walk");
        Blockbranch.LOGGER.info("MILESTONE_VIDEO_WALK species={} start={}",mob.getType(),mob.position());
    });}
    private static void requireVisible(Minecraft client,UUID id){
        var frustum=client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
        boolean visible=false;for(var entity:client.level.entitiesForRendering())if(entity.getUUID().equals(id)&&entity.isAlive()&&frustum!=null&&frustum.isVisible(entity.getBoundingBox()))visible=true;
        require(visible,"native review subject alive and inside capture frustum");
    }
    private static void stageCamera(Minecraft client,int target){boolean calfFace=(BISON_ART||BEAR_ART||MALLARD_ART||DEER_ART)&&target==4;boolean face=FOCUSED_ART&&target==3||calfFace||MALLARD_ART&&target==7;int selected=DEER_JUVENILE_ART?(target>=2?2:target):BEAVER_GROOM_ART?(target==1?1:target==2||target==4||target==6?2:0):DEER_REST_ART?(target==1||target==5?1:target==2||target==4||target==6?2:0):BISON_THREAT_ART?(target==1||target==4?1:target==2||target==6?2:0):MALLARD_JUVENILE_ART?(target>=2?2:target):BEAR_FORAGE_ART?(target==1?1:target==2||target==4||target==6?2:0):BISON_GRAZE_ART?(target==1?1:target==2||target==4||target==6?2:0):BOAR_ROOT_ART?(target==1?1:target==2||target==4||target==6?2:0):BOAR_ART&&target>=3?0:PIGLET_ART&&target>=2?2:DEER_ART&&target>=5?(DEER_ANTLER_ART||target==5?0:2):MALLARD_ART&&target>=5?(target==5||target==7?1:0):BEAR_ART&&target>=5?(target==5?2:0):BEAVER_ART&&target==4?0:calfFace?2:face?0:target;serverReady=false;if(DEER_ART)client.options.fov().set(DEER_ANTLER_ART||DEER_GRAZE_ART||DEER_REST_ART||DEER_JUVENILE_ART?70:target>=3?45:70);client.getSingleplayerServer().execute(()->{
        var level=client.getSingleplayerServer().overworld();for(int i=0;i<actors.size();i++){var e=level.getEntity(actors.get(i));
            if(MILESTONE||BEAVER_ART||BEAR_ART||MALLARD_ART||DEER_ART||PIGLET_ART)require(e!=null&&e.isAlive(),"staged subject survives camera changes");else if(e==null)continue;
            if(BEAVER_ART||BEAR_ART||MALLARD_ART||DEER_ART||PIGLET_ART)e.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            if(DEER_JUVENILE_ART&&i==2)try{var field=RedDeer.class.getDeclaredField("ACTIVITY");field.setAccessible(true);e.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),target==4?2:target==5?1:0);}catch(Exception ex){throw new RuntimeException(ex);}
            if(DEER_REST_ART&&i<3)try{var field=RedDeer.class.getDeclaredField("ACTIVITY");field.setAccessible(true);e.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),i==selected&&(target==3||target==4)?0:2);}catch(Exception ex){throw new RuntimeException(ex);}
            if(BEAR_ART&&!BEAR_FORAGE_ART&&target==5&&i==selected)try{var field=BrownBear.class.getDeclaredField("ACTIVITY");field.setAccessible(true);e.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),0);}catch(Exception ex){throw new RuntimeException(ex);}
            if(MALLARD_ART&&!MALLARD_PECK_ART&&!MALLARD_JUVENILE_ART&&(target==5||target==6)&&i==selected)try{var field=Mallard.class.getDeclaredField("ACTIVITY");field.setAccessible(true);e.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),target==5?0:3);}catch(Exception ex){throw new RuntimeException(ex);}
            if(MALLARD_JUVENILE_ART&&i==2)try{var field=Mallard.class.getDeclaredField("ACTIVITY");field.setAccessible(true);e.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),target==4?2:target==5?1:target==6?3:0);}catch(Exception ex){throw new RuntimeException(ex);}
            if(MILESTONE){if(e instanceof Mob mob)mob.getNavigation().stop();e.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);e.snapTo(i==selected?x+(selected-1)*2.4:x+4,y,i==selected?z:z-4+i*1.8,0,0);}
            else e.snapTo(i==selected?x+(selected-1)*2.4:x+20+i*3,y,z,0,0);
        }
        if(BEAR_FORAGE_ART){
            if(forageFood!=null){var previous=level.getEntity(forageFood);if(previous!=null)previous.discard();forageFood=null;}
            if(selected!=1){
                var food=new net.minecraft.world.entity.item.ItemEntity(level,x+(selected-1)*2.4,y,z+(selected==2?.6:1.12),new ItemStack(net.minecraft.world.item.Items.SWEET_BERRIES));
                food.setNeverPickUp();food.setUnlimitedLifetime();level.addFreshEntity(food);forageFood=food.getUUID();
            }
        }
        if(MALLARD_PECK_ART&&selected==1&&target>=5)try{var field=Mallard.class.getDeclaredField("ACTIVITY");field.setAccessible(true);level.getEntity(actors.get(1)).getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),1);}catch(Exception e){throw new RuntimeException(e);}
        var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
        boolean deerPortrait=selected==0&&SUBJECTS[(stage-4)/STEPS_PER_SPECIES].equals("deer");
        boolean pupPortrait=SEAL_ART&&selected==2;
        if(MALLARD_JUVENILE_ART&&selected==2){
            double actorX=x+2.4;
            if(target==3)player.connection.teleport(actorX,y-1.4,z+1.1,180,4);
            else if(target==7)player.connection.teleport(actorX+.8,y-1.35,z-1,38,5);
            else if(target==4||target==5)player.connection.teleport(actorX+.8,y-1.42,z+1,139,8);
            else player.connection.teleport(actorX+1.15,y-1.4,z+.4,109.29F,5);
            serverReady=true;return;
        }
        if(BEAVER_GROOM_ART){
            boolean kit=selected==2,front=target==3||target==4,quarter=target>=5;
            double actorX=x+(selected-1)*2.4,distance=kit?1.15:1.9;
            player.connection.teleport(actorX+(front?0:distance*(quarter?-.7:1)),y-(kit?1.3:1),z+distance*(front?1:quarter?.8:.35),front?180:quarter?-139:109.29F,front?4:8);serverReady=true;return;
        }
        if(MALLARD_PECK_ART){
            boolean duckling=selected==2,front=target==3||target==4||target==7,quarter=target==6;
            double actorX=x+(selected-1)*2.4,distance=duckling?1.15:1.7;
            player.connection.teleport(actorX+(front?0:distance*(quarter?.7:1)),y-(duckling?1.42:1.27),z+distance*(front?1:quarter?.8:.35),front?180:quarter?139:109.29F,front?6:8);serverReady=true;return;
        }
        if(MALLARD_ART){boolean duckling=selected==2;
            if(target==6){player.connection.teleport(x-1,y-.65,z+1.4,135,20);serverReady=true;return;}
            player.connection.teleport(x+(selected-1)*2.4+(face?0:duckling?1:1.5),y-(duckling?1.2:face?1:.9),z+(face?duckling?1:1.6:duckling?.35:.53),face?180:109.29F,face?8:12);serverReady=true;return;
        }
        if(BEAVER_ART){
            if(target==4){player.connection.teleport(x-2.4,y+.3,z+.25,180,90);serverReady=true;return;}
            boolean kit=selected==2;
            player.connection.teleport(x+(selected-1)*2.4+(face?0:kit?1.2:1.6),y-(face?1:kit?1.15:.9),z+(face?1.8:kit?.42:.56),face?180:109.29F,face?6:17);serverReady=true;return;
        }
        if(BEAR_FORAGE_ART){
            boolean cub=selected==2,front=target==3||target==4,quarter=target>=5;
            double actorX=x+(selected-1)*2.4,distance=cub?1.9:3.2;
            player.connection.teleport(actorX+(front?0:distance*(quarter?.7:1)),y-(cub?1:.4),z+distance*(front?1:quarter?.8:.35),front?180:quarter?139:109.29F,front?14:12);serverReady=true;return;
        }
        if(BEAR_ART&&target==6){player.connection.teleport(x+.1,y-.3,z-2.5,45,10);serverReady=true;return;}
        if(BEAR_ART){boolean cub=selected==2;
            if(calfFace){player.connection.teleport(x+2.4,y-1.05,z+1.8,180,5);serverReady=true;return;}
            player.connection.teleport(x+(selected-1)*2.4+(face?0:cub?1.8:2.4),y-(cub?.65:.3),z+(face?3:cub?.63:.84),face?180:109.29F,face?10:selected==0?12:17);serverReady=true;return;
        }
        if(BOAR_ART&&target>=3){double actorX=x-2.4;
            if(target==3)player.connection.teleport(actorX,y-.8,z+2.25,180,4);
            else if(target==4)player.connection.teleport(actorX+1.4,y-.8,z+1.8,142,5);
            else if(target==5)player.connection.teleport(actorX-2,y-.9,z+.7,250.71F,7);
            else player.connection.teleport(actorX+1.4,y-.5,z-1.8,38,12);
            serverReady=true;return;
        }
        if(BOAR_ROOT_ART){
            boolean piglet=selected==2,front=target==3||target==4,quarter=target>=5;
            double actorX=x+(selected-1)*2.4,distance=piglet?(front?1.4:1.5):(front?2.3:2.6);
            player.connection.teleport(actorX+(front?0:distance*(quarter?.65:1)),y-(selected==1?.5:piglet?1.25:.9),z+distance*(front?1:quarter?.8:.35),front?180:quarter?140:109.29F,front?11:12);
            serverReady=true;return;
        }
        if(PIGLET_ART&&selected==2){double actorX=x+2.4;
            if(target==3)player.connection.teleport(actorX,y-1.22,z+1.25,180,0);
            else if(target==4)player.connection.teleport(actorX,y-.5,z,0,90);
            else if(target==5)player.connection.teleport(actorX-1,y-1.25,z+.35,250.71F,5);
            else if(target==6)player.connection.teleport(actorX+.7,y-1,z-.9,38,15);
            else player.connection.teleport(actorX+1,y-1.25,z+.35,109.29F,5);
            serverReady=true;return;
        }
        if(DEER_ART){boolean calf=selected==2;
            if(DEER_JUVENILE_ART){
                double actorX=x+(selected-1)*2.4,distance=calf?1.65:3.5;
                if(target==3)player.connection.teleport(actorX,y-.8,z+1.9,180,7);
                else if(target==4||target==5)player.connection.teleport(actorX+1.2,y-.95,z+1.4,139,10);
                else if(target==6)player.connection.teleport(actorX-1.2,y-.8,z-1.4,-42,10);
                else player.connection.teleport(actorX+distance,y-(calf?.8:.1),z+distance*.35,109.29F,8);
                serverReady=true;return;
            }
            if(DEER_REST_ART){
                double actorX=x+(selected-1)*2.4,distance=calf?1.7:3.5;
                boolean front=target==5,quarter=target==6;
                player.connection.teleport(actorX+(front?0:distance*(quarter?.7:1)),y-(calf?1.1:.6),z+distance*(front?1:quarter?.8:.35),front?180:quarter?139:109.29F,target==3||target==4?12:5);serverReady=true;return;
            }
            if(DEER_GRAZE_ART){
                double actorX=x+(selected-1)*2.4,distance=selected==1?3.8:calf?1.7:3.2;
                boolean front=target==3||target==4,quarter=target>=5;
                player.connection.teleport(actorX+(front?0:distance*(quarter?.7:1)),y-(calf?1:.25),z+distance*(front?1:quarter?.8:.35),front?180:quarter?139:109.29F,selected==1?8:14);serverReady=true;return;
            }
            if(DEER_ANTLER_ART&&(target==3||target>=5)){
                double actorX=x+(selected-1)*2.4;
                if(target==3)player.connection.teleport(actorX,y+.45,z+3.1,180,12);
                else if(target==5)player.connection.teleport(actorX+2.5,y+.5,z+3.1,135,12);
                else player.connection.teleport(actorX-2.5,y+.5,z-2.5,-45,12);
                serverReady=true;return;
            }
            if(target>=5){player.connection.teleport(x+(selected-1)*2.4+(calf?.8:1.3),y+(calf?-.875:-.13),z+(calf?1.1:1.9),135,0);serverReady=true;return;}
            if(face){player.connection.teleport(x+(selected-1)*2.4,y+(calf?-.875:-.13),z+(calf?1.7:2.5),180,0);serverReady=true;return;}
            player.connection.teleport(x+(selected-1)*2.4+(face?0:calf?1.5:3.5),y-(calf?.85:face?.05:.1),z+(face?calf?1.5:3.5:calf?.53:1.2),face?180:109.29F,face?5:8);serverReady=true;return;
        }
        if(BISON_THREAT_ART){
            boolean calf=selected==2,front=target==3||target==4||target==6,quarter=target==5;
            double actorX=x+(selected-1)*2.4,distance=calf?2.2:4.1;
            player.connection.teleport(actorX+(front?0:distance*(quarter?.7:1)),y-(calf?.75:.1),z+distance*(front?1:quarter?.8:.35),front?180:quarter?139:109.29F,front?10:12);serverReady=true;return;
        }
        if(BISON_GRAZE_ART){
            boolean calf=selected==2,front=target==3||target==4,quarter=target>=5;
            double actorX=x+(selected-1)*2.4,distance=calf?2.2:4.1;
            player.connection.teleport(actorX+(front?0:distance*(quarter?.7:1)),y-(calf?.85:.25),z+distance*(front?1:quarter?.8:.35),front?180:quarter?139:109.29F,front?10:14);serverReady=true;return;
        }
        if(BISON_ART){boolean calf=selected==2;
            if(calfFace){player.connection.teleport(x+2.4,y-.9,z+2.6,180,5);serverReady=true;return;}
            player.connection.teleport(x+(selected-1)*2.4+(face?0:calf?1.8:3),y-(calf?.65:.3),z+(face?4.5:calf?.63:1.05),face?180:109.29F,face?10:selected==0?8:17);serverReady=true;return;
        }
        player.connection.teleport(x+(selected-1)*2.4+(face?0:pupPortrait?1.15:deerPortrait?3.5:2),y-(face||pupPortrait?1.05:deerPortrait?.1:.6),z+(face?1.8:pupPortrait?.4:deerPortrait?1.2:.7),face?180:109.29F,face?10:selected==0?8:17);serverReady=true;
    });}
    @SuppressWarnings("unchecked") private static void stageActors(Minecraft client,int species){serverReady=false;captured=true;client.getSingleplayerServer().execute(()->{
        if(MILESTONE)Blockbranch.LOGGER.info("MILESTONE_VIDEO_ACTORS species={} controlledWalk=true stagedRest=true",SUBJECTS[species]);
        var level=client.getSingleplayerServer().overworld();for(var id:actors){var e=level.getEntity(id);if(e!=null)e.discard();}actors.clear();
        var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();player.setGameMode(GameType.SPECTATOR);
        if(FOCUSED_ART){
            // Reloaded art worlds retain prior staged actors and references.
            for(var mob:level.getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(BlockPos.containing(x,y,z)).inflate(12)))
                if(mob instanceof HarborSeal||mob.entityTags().contains("blockbranch_art_reference")||mob.entityTags().contains("blockbranch_art_actor"))mob.discard();
        }
        var origin=BlockPos.containing(x,y-1,z);y=origin.getY()+1;
        for(int dx=-6;dx<=6;dx++)for(int dz=-5;dz<=(FOCUSED_ART?10:6);dz++){
            var pos=origin.offset(dx,0,dz);level.setBlock(pos,(BOAR_ROOT_ART?Blocks.COARSE_DIRT:(DEER_GRAZE_ART||BISON_GRAZE_ART||BISON_THREAT_ART||BEAR_FORAGE_ART||MALLARD_PECK_ART)?Blocks.GRASS_BLOCK:Blocks.SMOOTH_STONE).defaultBlockState(),3);
            for(int dy=1;dy<=5;dy++)level.setBlock(pos.above(dy),Blocks.AIR.defaultBlockState(),3);
        }
        for(int h=1;h<=2;h++)level.setBlock(origin.offset((BISON_ART||BEAR_ART||MALLARD_ART||DEER_ART||PIGLET_ART)?-6:-4,h,(BISON_ART||BEAR_ART||MALLARD_ART||DEER_ART||PIGLET_ART)?-4:0),Blocks.QUARTZ_BLOCK.defaultBlockState(),3);
        for(int i=0;i<3;i++){
            var type=switch(SUBJECTS[species]){case "boar"->WildlifeRuntime.WILD_BOAR;case "deer"->WildlifeRuntime.RED_DEER;case "bear"->WildlifeRuntime.BROWN_BEAR;case "bison"->WildlifeRuntime.AMERICAN_BISON;case "beaver"->WildlifeRuntime.EURASIAN_BEAVER;case "seal"->WildlifeRuntime.HARBOR_SEAL;case "mallard"->WildlifeRuntime.MALLARD;default->throw new AssertionError("review species");};
            var mob=(Mob)type.create(level,EntitySpawnReason.COMMAND);mob.setNoAi(!MILESTONE||i!=0);mob.setPersistenceRequired();if(FOCUSED_ART)mob.addTag("blockbranch_art_actor");if(BEAVER_ART||BEAR_ART||MALLARD_ART||DEER_ART||PIGLET_ART){mob.setPermanentlyInvulnerable(true);mob.setNoGravity(true);}
            if(MALLARD_ART)((Mallard)mob).setDrake(i==0);
            if(mob instanceof RedDeer deer)deer.setAntlers(i==0);
            mob.snapTo(x+(i-1)*2.4,y,z,0,0);if(i==2)((AgeableMob)mob).setAge(-24000);
            if(i!=0&&!((DEER_ART||PIGLET_ART)&&i==2))try{var field=mob.getClass().getDeclaredField("ACTIVITY");field.setAccessible(true);mob.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),2);}catch(Exception e){throw new RuntimeException(e);}
            if(MALLARD_PECK_ART)try{var field=mob.getClass().getDeclaredField("ACTIVITY");field.setAccessible(true);mob.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),i==1?0:1);}catch(Exception e){throw new RuntimeException(e);}
            if(BEAR_FORAGE_ART)try{var field=mob.getClass().getDeclaredField("ACTIVITY");field.setAccessible(true);mob.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),i==1?0:1);}catch(Exception e){throw new RuntimeException(e);}
            if(BISON_THREAT_ART)try{mob.setAggressive(i!=1);var field=mob.getClass().getDeclaredField("ACTIVITY");field.setAccessible(true);mob.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),0);}catch(Exception e){throw new RuntimeException(e);}
            if(BISON_GRAZE_ART)try{var field=mob.getClass().getDeclaredField("ACTIVITY");field.setAccessible(true);mob.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),i==1?0:1);}catch(Exception e){throw new RuntimeException(e);}
            if(BOAR_ROOT_ART)try{var field=mob.getClass().getDeclaredField("ACTIVITY");field.setAccessible(true);mob.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),i==1?0:1);}catch(Exception e){throw new RuntimeException(e);}
            if(BEAVER_GROOM_ART)try{var field=EurasianBeaver.class.getDeclaredField("ACTIVITY");field.setAccessible(true);mob.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),i==1?0:1);}catch(Exception e){throw new RuntimeException(e);}
            if(DEER_REST_ART)try{var field=RedDeer.class.getDeclaredField("ACTIVITY");field.setAccessible(true);mob.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),2);}catch(Exception e){throw new RuntimeException(e);}
            if(DEER_GRAZE_ART)try{var field=RedDeer.class.getDeclaredField("ACTIVITY");field.setAccessible(true);mob.getEntityData().set((net.minecraft.network.syncher.EntityDataAccessor<Integer>)field.get(null),i==1?0:1);}catch(Exception e){throw new RuntimeException(e);}
            level.addFreshEntity(mob);actors.add(mob.getUUID());
            if(MILESTONE&&i==0)mob.removeAllGoals(g->true);
        }
        if(FOCUSED_ART){int reference=0;for(var name:PIGLET_ART?List.of("pig","rabbit"):DEER_ANTLER_ART?List.of("cow","goat"):DEER_ART?List.of("cow","horse"):MALLARD_JUVENILE_ART?List.of("chicken","chicken"):MALLARD_ART?List.of("chicken","turtle"):BEAR_ART?List.of("polar_bear","cow"):BEAVER_ART?List.of("rabbit","fox"):BISON_ART?List.of("cow","sheep"):List.of("turtle","polar_bear")){
            var type=BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(name));
            var mob=(Mob)type.create(level,EntitySpawnReason.COMMAND);require(mob!=null,"vanilla style reference");
            mob.setNoAi(true);mob.setPersistenceRequired();mob.addTag("blockbranch_art_reference");if(BEAVER_ART||BEAR_ART||MALLARD_ART||DEER_ART||PIGLET_ART){mob.setPermanentlyInvulnerable(true);mob.setNoGravity(true);}
            if(MALLARD_JUVENILE_ART&&reference==1)((AgeableMob)mob).setAge(-24000);
            mob.snapTo(x+(reference++==0?((BISON_ART||BEAR_ART)?-5:-4):4),y,z-((BISON_ART||BEAVER_ART||BEAR_ART||MALLARD_ART||DEER_ART||PIGLET_ART)?0:3),0,0);level.addFreshEntity(mob);actors.add(mob.getUUID());
        }}
        for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(origin).inflate(8)))item.discard();
        player.connection.teleport(x,y+(MALLARD_ART?0:BEAVER_ART?.5:1.2),z+(MALLARD_ART?5:BEAVER_ART?5.7:7.5),180,MALLARD_ART?15:17);serverReady=true;
    });}
}
