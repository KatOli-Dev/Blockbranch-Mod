package blockbranch.client.qa;
import blockbranch.Blockbranch;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
/** Opt-in UI-created startup witness. Observes existing work; never forces columns. */
public final class InitialLoadingAudit {
 private static final boolean ON=Boolean.getBoolean("blockbranch.initialLoading"),REOPEN=Boolean.getBoolean("blockbranch.initialLoadingReopen");
 private static final Path DIR=Path.of(System.getProperty("blockbranch.initialLoadingDirectory","build/initial-loading"));
 private static final String WORLD="InitialLoading";
 private static final long SEED=Long.getLong("blockbranch.initialLoadingSeed",424242);
 private static final Map<String,Object> generatorStartup=new TreeMap<>();
 private static final List<Map<String,Object>> stages=Collections.synchronizedList(new ArrayList<>());
 private static final long start=System.nanoTime();
 private static boolean opened,configured,clientPlayer,unobscured,saving;
 private static volatile boolean visible,done;
 private static volatile boolean serverPlayer,verified;
 private static volatile Throwable failure;
 private static int visibleTicks,serverTicks;
 private static volatile int columns,fresh;
 private static float startYaw;
 private static final java.util.Set<Long> loaded=new java.util.concurrent.ConcurrentSkipListSet<>();
 private static jdk.jfr.Recording recording;
 @jdk.jfr.Name("blockbranch.InitialLoadingStage") @jdk.jfr.StackTrace(false)
 static final class Stage extends jdk.jfr.Event {String stage;long elapsedNs,processCpuNs;}
 private static void mark(String stage){var os=(com.sun.management.OperatingSystemMXBean)java.lang.management.ManagementFactory.getOperatingSystemMXBean();var row=new LinkedHashMap<String,Object>();row.put("stage",stage);row.put("epochMillis",System.currentTimeMillis());row.put("elapsedNs",System.nanoTime()-start);row.put("processCpuNs",os.getProcessCpuTime());row.put("thread",Thread.currentThread().getName());row.put("uniqueColumns",loaded.size());row.put("freshCallbacks",fresh);row.put("threadCpuNs",java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime());stages.add(row);var event=new Stage();event.stage=stage;event.elapsedNs=(long)row.get("elapsedNs");event.processCpuNs=(long)row.get("processCpuNs");event.commit();Blockbranch.LOGGER.info("INITIAL_LOADING {}",row);}
 public static void register(){
  try{Files.createDirectories(DIR);Files.writeString(DIR.resolve("process-id.txt"),Long.toString(ProcessHandle.current().pid()));}catch(Exception e){throw new RuntimeException(e);}
  recording=new jdk.jfr.Recording();recording.enable("blockbranch.InitialLoadingStage");recording.enable("jdk.ExecutionSample").withPeriod(java.time.Duration.ofMillis(10));recording.enable("jdk.GCPhasePause");recording.enable("jdk.FileRead").withThreshold(java.time.Duration.ofMillis(10));recording.enable("jdk.FileWrite").withThreshold(java.time.Duration.ofMillis(10));recording.enable("jdk.ThreadPark").withThreshold(java.time.Duration.ofMillis(10));recording.enable("jdk.JavaMonitorEnter").withThreshold(java.time.Duration.ofMillis(10));recording.setMaxSize(128L*1024*1024);recording.start();mark("registered");
  ServerLifecycleEvents.SERVER_STARTING.register(s->mark("server-starting"));
  ServerLifecycleEvents.SERVER_STARTED.register(s->{mark("server-started");var g=s.overworld().getChunkSource().getGenerator();try{for(var method:g.getClass().getMethods())if(method.getParameterCount()==0&&(method.getName().endsWith("InitializationNanos")||method.getName().endsWith("CacheRestored")||method.getName().equals("treasureRestored")))generatorStartup.put(method.getName(),method.invoke(g));}catch(Exception e){failure=e;}});
  ServerChunkEvents.CHUNK_LOAD.register((l,c,n)->{if(l.dimension()==net.minecraft.world.level.Level.OVERWORLD){columns++;if(n)fresh++;loaded.add(c.getPos().pack());}});
  ServerTickEvents.END_SERVER_TICK.register(s->{try{
   if(done||s.getPlayerList().getPlayers().isEmpty())return;var p=s.getPlayerList().getPlayers().getFirst();
   if(!serverPlayer){serverPlayer=true;mark("server-player-tick");if(s.overworld().getSeed()!=SEED)throw new AssertionError("Seed");}
   if(!visible)return;
   if(++serverTicks==1){startYaw=p.getYRot();}
   if(serverTicks>=20&&!verified&&Math.abs(net.minecraft.util.Mth.wrapDegrees(p.getYRot()-startYaw))>=10){
    var file=DIR.resolve("edit.txt");net.minecraft.core.BlockPos pos;
    if(REOPEN){var xyz=Files.readString(file).trim().split(",");pos=new net.minecraft.core.BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));if(!s.overworld().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK))throw new AssertionError("Saved sparse edit");}
    else{pos=p.blockPosition().below(4);if(!s.overworld().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState())||!s.overworld().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK))throw new AssertionError("Fresh sparse edit readback");Files.writeString(file,pos.getX()+","+pos.getY()+","+pos.getZ());}
    verified=true;mark("input-and-edit-verified");
   }
  }catch(Throwable e){failure=e;}});
  ClientTickEvents.END_CLIENT_TICK.register(c->{try{
   if(System.nanoTime()-start>600_000_000_000L)throw new AssertionError("Startup deadline");if(saving)return;if(failure!=null)throw new AssertionError("Server witness",failure);
   var screen=c.gui.screen();String name=screen==null?"":screen.getClass().getSimpleName();
   if(!opened&&name.equals("AccessibilityOnboardingScreen")){press(screen,"Continue");return;}
   if(!opened&&screen instanceof TitleScreen){opened=true;if(REOPEN){mark("open-click");c.createWorldOpenFlows().openWorld(WORLD,()->c.gui.setScreen(new TitleScreen()));}else{if(Files.exists(c.gameDirectory.toPath().resolve("saves/"+WORLD)))throw new AssertionError("Fresh world exists");mark("create-screen-request");CreateWorldScreen.openFresh(c,()->c.gui.setScreen(new TitleScreen()));}return;}
   if(!configured&&screen instanceof CreateWorldScreen create){var state=create.getUiState();state.setName(WORLD);state.setSeed(Long.toString(SEED));state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);state.setAllowCommands(true);String preset=System.getProperty("blockbranch.initialLoadingPreset","blockbranch:realistic");state.setWorldType(state.getNormalPresetList().stream().filter(e->e.preset().unwrapKey().orElseThrow().identifier().toString().equals(preset)).findFirst().orElseThrow());configured=true;mark("create-click");if(!press(create,"Create New World"))throw new AssertionError("Create button");return;}
   if(screen!=null&&(name.equals("ConfirmExperimentalFeaturesScreen")||name.equals("BackupConfirmScreen"))){mark("confirm-click");press(screen,"Proceed");return;}
   if(c.level==null||c.player==null)return;
   if(!clientPlayer){if(!c.hasSingleplayerServer()||c.isMultiplayerServer()||!c.getConnection().getConnection().isMemoryConnection())throw new AssertionError("Integrated in-memory connection");boolean realistic=c.getSingleplayerServer().overworld().getChunkSource().getGenerator() instanceof blockbranch.realistic.RealisticGenerator;if(realistic!=System.getProperty("blockbranch.initialLoadingPreset","blockbranch:realistic").equals("blockbranch:realistic"))throw new AssertionError("Expected generator");clientPlayer=true;mark("client-player");}
   if(visible){c.player.setYRot(c.player.getYRot()+2);visibleTicks++;}
   if(verified&&visibleTicks>=40){mark("usable-play-complete");saving=true;net.minecraft.client.Screenshot.grab(c.gameDirectory,REOPEN?"reopen.png":"fresh.png",c.gameRenderer.mainRenderTarget(),1,message->c.execute(()->{done=true;c.disconnect(new TitleScreen(),false);mark("disconnect-saved");write("PASS startup, look packets and saved edit\n");c.stop();}));}
  }catch(Throwable e){done=true;Blockbranch.LOGGER.error("INITIAL_LOADING_FAIL",e);write("FAIL "+e+"\n");c.stop();}});
 }
 public static void frameEnd(){if(!ON||done||visible)return;var c=Minecraft.getInstance();if(c.gui.screen()==null&&c.level!=null&&c.player!=null&&c.level.hasChunkAt(c.player.blockPosition())){if(!unobscured){unobscured=true;mark("first-unobscured-frame");}var area=c.levelRenderer.viewArea();if(area==null)return;for(int dy:new int[]{-16,0,16}){var section=area.getRenderSectionAt(c.player.blockPosition().offset(0,dy,0));if(section!=null&&section.getSectionMesh().hasRenderableLayers()){visible=true;mark("first-terrain-mesh-frame");break;}}}}
 private static void write(String result){try{recording.stop();recording.dump(DIR.resolve(REOPEN?"reopen.jfr":"fresh.jfr"));recording.close();var out=new LinkedHashMap<String,Object>();out.put("stages",stages);out.put("generatorStartup",generatorStartup);out.put("seed",SEED);out.put("reopen",REOPEN);out.put("loadedCallbacks",columns);out.put("freshCallbacks",fresh);out.put("uniqueColumns",loaded.size());out.put("loadedCoordinates",loaded);out.put("preset",System.getProperty("blockbranch.initialLoadingPreset","blockbranch:realistic"));out.put("renderDistance",Minecraft.getInstance().options.renderDistance().get());out.put("simulationDistance",Minecraft.getInstance().options.simulationDistance().get());Files.writeString(DIR.resolve(REOPEN?"reopen.json":"fresh.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(out));Files.writeString(DIR.resolve(REOPEN?"reopen-result.txt":"fresh-result.txt"),result);}catch(Exception e){throw new RuntimeException(e);}}
 private static boolean press(net.minecraft.client.gui.screens.Screen s,String label){for(var v:s.children())if(v instanceof Button b&&b.active&&b.getMessage().getString().equals(label)){b.onPress(new KeyEvent(257,0,0));return true;}return false;}
}
