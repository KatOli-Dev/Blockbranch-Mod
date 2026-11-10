package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.realistic.RealisticGenerator;
import blockbranch.wildlife.*;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.Difficulty;

/** Opt-in real Create World UI -> integrated server -> local observation -> save/exit.
 * Reload runs in a separate JVM. No player teleport or pregenerated world is used. */
public final class WildlifeSingleplayerSmoke {
    private static final String WORLD="ReadinessAnimalTest";
    private static final boolean RECONCILE=Boolean.getBoolean("blockbranch.singleplayerReconcileDiagnostic");
    private static final boolean MILESTONE=Boolean.getBoolean("blockbranch.milestoneVideoReview");
    private static final boolean RELOAD=Boolean.getBoolean("blockbranch.singleplayerReload");
    private static final Map<String,Set<UUID>> natural=new TreeMap<>();
    private static volatile int serverStage;
    private static volatile Throwable failure;
    private static int ticks,meshWait,meshStable;
    private static UUID witness;
    private static boolean configured,opened,captureStarted,captured,finished;
    private static String lastScreen="";
    private static Path directory(){return Path.of(System.getProperty("blockbranch.singleplayerResult")).getParent();}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void register(){
        long deadline=System.nanoTime()+900_000_000_000L;
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(failure!=null||finished||serverStage==2||server.isDedicatedServer())return;
            try{
                var level=server.overworld();var players=server.getPlayerList().getPlayers();if(players.isEmpty())return;
                var player=players.getFirst();if(!level.isPositionEntityTicking(player.blockPosition()))return;
                require(level.getChunkSource().getGenerator() instanceof RealisticGenerator,"actual Realistic integrated generator");
                require(level.getSeed()==424242L,"actual selected world seed");require(player.isAlive(),"local player alive");
                ++ticks;
                if(ticks==1)Blockbranch.LOGGER.info("SINGLEPLAYER_LOCAL_READY reload={} position={} seed={}",RELOAD,player.position(),level.getSeed());
                if(RELOAD){
                    if(ticks<200)return;
                    witness=UUID.fromString(Files.readString(directory().resolve("witness-uuid.txt")).trim());
                    require(level.getEntity(witness) instanceof WildBoar boar&&boar.isAlive()&&boar.hasCustomName(),"named controlled witness survives actual save/reopen");
                    Files.writeString(directory().resolve("reload-observation.txt"),"PASS controlled witness "+witness+" player="+player.position()+" ticks="+ticks+"\n");
                    serverStage=2;return;
                }
                if(serverStage==0){
                    if(!MILESTONE&&ticks%10==0)for(var e:level.getAllEntities())if(e instanceof Mob mob&&WildlifeCombat.isAnimal(mob)&&!mob.isPersistenceRequired()&&mob.distanceToSqr(player)<=128*128)
                        natural.computeIfAbsent(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(),ignored->new HashSet<>()).add(mob.getUUID());
                    if(ticks%200==0)Blockbranch.LOGGER.info("SINGLEPLAYER_NATURAL_PROGRESS ticks={} counts={}",ticks,naturalCounts());
                    if(ticks<(MILESTONE?40:1200))return;
                    var report=new LinkedHashMap<String,Object>();report.put("seed",level.getSeed());report.put("ticks",ticks);report.put("player",player.position().toString());report.put("naturalUnique",naturalCounts());report.put("terrainEdited",false);report.put("playerTeleported",false);
                    Files.writeString(directory().resolve(MILESTONE?"milestone-staged-observation.json":"new-natural-observation.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
                    var boar=WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND);require(boar!=null,"controlled witness creation");
                    boar.snapTo(player.getX()+4,player.getY()+1,player.getZ(),0,0);boar.setCustomName(Component.literal("Readiness witness"));boar.setPersistenceRequired();
                    require(level.addFreshEntity(boar),"controlled witness added");witness=boar.getUUID();Files.writeString(directory().resolve("witness-uuid.txt"),witness+"\n");serverStage=1;ticks=0;
                    Blockbranch.LOGGER.info("SINGLEPLAYER_CONTROLLED_WITNESS uuid={} explicitlyArtificial=true",witness);
                }else if(ticks>=(MILESTONE?40:200)){require(level.getEntity(witness) instanceof WildBoar boar&&boar.isAlive(),"controlled witness alive after local AI interval");serverStage=2;}
            }catch(Throwable error){failure=error;}
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(finished)return;
            try{
                if(failure!=null)throw new AssertionError("integrated server check",failure);
                require(System.nanoTime()<deadline,"singleplayer timeout; screen="+lastScreen+" serverStage="+serverStage);
                var screen=client.gui.screen();String name=screen==null?"<game>":screen.getClass().getSimpleName();
                if(name.equals("DisconnectedScreen"))throw new AssertionError("native singleplayer disconnected; see connection reason in log");
                if(!name.equals(lastScreen)){lastScreen=name;Blockbranch.LOGGER.info("SINGLEPLAYER_SCREEN {}",name);}
                if(!opened&&name.equals("AccessibilityOnboardingScreen")){press(screen,"Continue");return;}
                if(!opened&&screen instanceof TitleScreen){
                    opened=true;
                    if(RELOAD){require(Files.exists(client.gameDirectory.toPath().resolve("saves/"+WORLD+"/level.dat")),"reload world exists");client.createWorldOpenFlows().openWorld(WORLD,()->client.gui.setScreen(new TitleScreen()));}
                    else{require(!Files.exists(client.gameDirectory.toPath().resolve("saves/"+WORLD)),"fresh world must not exist");CreateWorldScreen.openFresh(client,()->client.gui.setScreen(new TitleScreen()));}
                    return;
                }
                if(!RELOAD&&!configured&&screen instanceof CreateWorldScreen create){
                    var state=create.getUiState();state.setName(WORLD);state.setSeed("424242");state.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);state.setDifficulty(Difficulty.NORMAL);state.setAllowCommands(true);
                    var preset=state.getNormalPresetList().stream().filter(e->e.preset().unwrapKey().orElseThrow().identifier().equals(Blockbranch.id("realistic"))).findFirst().orElseThrow(()->new AssertionError("Realistic preset missing from normal UI"));state.setWorldType(preset);
                    configured=true;require(press(create,"Create New World"),"native Create New World button present");return;
                }
                if(screen!=null&&(name.equals("ConfirmExperimentalFeaturesScreen")||name.equals("BackupConfirmScreen")))press(screen,"Proceed");
                if(client.level==null||client.player==null||serverStage!=2)return;
                require(client.hasSingleplayerServer()&&!client.isMultiplayerServer()&&client.getConnection().getConnection().isMemoryConnection(),"actual integrated singleplayer memory connection");
                if(Boolean.getBoolean("blockbranch.foodArtReview")&&!FoodArtReview.tick(client))return;
                if(Boolean.getBoolean("blockbranch.modIconArtReview")&&!ModIconArtReview.tick(client))return;
                if(Boolean.getBoolean("blockbranch.perchLandArtReview")&&!PerchLandArtReview.tick(client))return;
                if(Boolean.getBoolean("blockbranch.perchArtReview")&&!PerchArtReview.tick(client))return;
                if(Boolean.getBoolean("blockbranch.beaverStrideArtReview")&&!BeaverStrideArtReview.tick(client))return;
                if(Boolean.getBoolean("blockbranch.sealStrideArtReview")&&!SealStrideArtReview.tick(client))return;
                if(Boolean.getBoolean("blockbranch.beaverWaterArtReview")&&!BeaverWaterArtReview.tick(client))return;
                if(Boolean.getBoolean("blockbranch.sealSwimArtReview")&&!SealSwimArtReview.tick(client))return;
                if(Boolean.getBoolean("blockbranch.mallardJuvenileWaterReview")&&!MallardJuvenileWaterReview.tick(client))return;
                if(Boolean.getBoolean("blockbranch.mallardFlightArtReview")&&!MallardFlightArtReview.tick(client))return;
                if(Boolean.getBoolean("blockbranch.itemReview")&&!WildlifePlayerIntegrationReview.tick(client))return;
                if(!captureStarted){
                    var area=client.levelRenderer.viewArea();if(area==null)return;var foot=client.player.blockPosition().below();var mesh=area.getRenderSectionAt(foot);
                    if(++meshWait%100==0){var sparse=(blockbranch.cubic.SparseColumn)client.level.getChunkAt(foot);Blockbranch.LOGGER.info("SINGLEPLAYER_MESH_WAIT floor={} state={} profile={} sectionEmpty={} meshOrigin={} renderable={} range={}..{}",foot,client.level.getBlockState(foot),sparse.blockbranch$terrain()!=null,sparse.blockbranch$read(foot.getY()>>4).hasOnlyAir(),mesh==null?null:mesh.getRenderOrigin(),mesh!=null&&mesh.getSectionMesh().hasRenderableLayers(),area.minSectionY(),area.maxSectionY());}
                    if(mesh==null||!mesh.getSectionMesh().hasRenderableLayers()){
                        if(RECONCILE&&meshWait==400){
                            int x=foot.getX()>>4,y=foot.getY()>>4,z=foot.getZ()>>4;
                            client.level.getChunkSource().onSectionEmptinessChanged(x,y,z,false);
                            client.level.setSectionRangeDirty(x,y,z,x,y,z);
                            Blockbranch.LOGGER.info("SINGLEPLAYER_DIAGNOSTIC_RECONCILE nativeCacheOnly=true section={},{},{} noBlockEdits=true",x,y,z);
                        }
                        if(meshWait==600){net.minecraft.client.Screenshot.grab(client.gameDirectory,"singleplayer-mesh-failure.png",client.gameRenderer.mainRenderTarget(),1,message->{});throw new AssertionError("local ground render mesh missing after 600 client ticks; see diagnostic and screenshot");}return;
                    }
                    if(++meshStable<40)return; // Observe settled rendered frames before reading the framebuffer.
                    captureStarted=true;String file=RELOAD?"singleplayer-reload.png":"singleplayer-new.png";
                    net.minecraft.client.Screenshot.grab(client.gameDirectory,file,client.gameRenderer.mainRenderTarget(),1,message->client.execute(()->{captured=Files.exists(client.gameDirectory.toPath().resolve("screenshots/"+file));}));return;
                }
                if(!captured)return;
                client.disconnect(new TitleScreen(),false);
                require(Files.exists(client.gameDirectory.toPath().resolve("saves/"+WORLD+"/level.dat")),"world saved through native disconnect");
                Files.writeString(Path.of(System.getProperty("blockbranch.singleplayerResult")),MILESTONE?"PASS MILESTONE native staged visual showcase; natural spawn survey not run\n":(RECONCILE?"DIAGNOSTIC ":"PASS ")+(RELOAD?"RELOAD":"NEW")+" actual integrated singleplayer; native save/quit\n");
                Blockbranch.LOGGER.info("SINGLEPLAYER_SMOKE completed reload={} diagnosticRecovery={} natural={} witness={}",RELOAD,RECONCILE,naturalCounts(),witness);finished=true;client.stop();
            }catch(Throwable error){finished=true;Blockbranch.LOGGER.error("SINGLEPLAYER_SMOKE FAIL screen={} serverStage={}",lastScreen,serverStage,error);try{Files.writeString(Path.of(System.getProperty("blockbranch.singleplayerResult")),"FAIL "+error+"\n");}catch(Exception ignored){}client.stop();}
        });
    }
    private static Map<String,Integer> naturalCounts(){var out=new TreeMap<String,Integer>();natural.forEach((type,ids)->out.put(type,ids.size()));return out;}
    private static boolean press(net.minecraft.client.gui.screens.Screen screen,String label){
        for(var child:screen.children())if(child instanceof Button button&&button.active&&button.getMessage().getString().equals(label)){button.onPress(new KeyEvent(257,0,0));return true;}return false;
    }
    private WildlifeSingleplayerSmoke(){}
}
