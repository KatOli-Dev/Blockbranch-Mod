package blockbranch.client.qa;
import blockbranch.Blockbranch;
import blockbranch.realistic.RealisticGenerator;
import blockbranch.qa.SnapshotHoldProbe;
import java.lang.management.*;
import java.nio.file.*;
import java.util.*;
import jdk.jfr.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
/** One fresh UI-created world. Native forward/jump input, no terrain/mesh intervention. */
public final class IntegratedWalkingAudit {
 private static final boolean ON=Boolean.getBoolean("blockbranch.integratedWalking");
 private static final ThreadMXBean CPU=ManagementFactory.getThreadMXBean();
 private static final String WORLD="IntegratedWalking";
 private static final int LIMIT=16384;
 private static final long[][] FRAMES=new long[8][LIMIT],TICKS=new long[13][LIMIT];
 private static final Set<Long> unique=new HashSet<>(),uniqueFresh=new HashSet<>();
 private static final Map<Long,net.minecraft.world.level.chunk.LevelChunk> active=new HashMap<>();
 private static long frameBytes;private static int residentSections;
 private static long allocated(){return CPU instanceof com.sun.management.ThreadMXBean b&&b.isThreadAllocatedMemorySupported()?b.getThreadAllocatedBytes(Thread.currentThread().threadId()):-1;}
 private static final double[][] POSES=new double[3][LIMIT];
 private static int frameCount,tickCount,loaded,fresh,clientTicks,lastJump=-100;
 private static long frameStart,frameCpu,frameEpoch,tickStart,tickCpu,tickEpoch,phaseStart,deadline;
 private static long renderThreadId,serverThreadId;
 private static volatile String phase="startup";private static volatile boolean ready,done;private static volatile Throwable failure;
 private static boolean opened,configured,aimed;private static double originX,originY,originZ;
 private static long phaseTicks;
 private static Path directory(){return Path.of(System.getProperty("blockbranch.integratedWalkingResult")).getParent();}
 private static long cpu(){return CPU.isCurrentThreadCpuTimeSupported()&&CPU.isThreadCpuTimeEnabled()?CPU.getCurrentThreadCpuTime():-1;}
 public static void frameBegin(boolean render){if(!ON)return;var c=Minecraft.getInstance();frameStart=render&&c.level!=null&&c.player!=null&&!done?System.nanoTime():0;if(frameStart!=0){frameEpoch=System.currentTimeMillis();frameCpu=cpu();frameBytes=allocated();renderThreadId=Thread.currentThread().threadId();}}
 public static void frameEnd(){if(!ON||frameStart==0)return;long end=System.nanoTime(),endCpu=cpu(),elapsed=end-frameStart;int n=frameCount++;if(n>=LIMIT)throw new AssertionError("Frame buffer exhausted");FRAMES[0][n]=frameEpoch;FRAMES[1][n]=System.currentTimeMillis();FRAMES[2][n]=frameStart;FRAMES[3][n]=end;FRAMES[4][n]=frameCpu<0||endCpu<0?-1:endCpu-frameCpu;FRAMES[5][n]=phaseCode();FRAMES[6][n]=renderThreadId;long endBytes=allocated();FRAMES[7][n]=frameBytes<0||endBytes<0?-1:endBytes-frameBytes;if(elapsed>=100_000_000L){var e=new SlowFrame();e.phase=phase;e.startEpochMillis=frameEpoch;e.endEpochMillis=FRAMES[1][n];e.wallNs=elapsed;e.cpuNs=FRAMES[4][n];e.threadId=renderThreadId;e.allocatedBytes=FRAMES[7][n];e.commit();}frameStart=0;}
 @Name("blockbranch.IntegratedSlowFrame") @Label("Integrated slow frame") @StackTrace(false)
 static final class SlowFrame extends Event {String phase;long startEpochMillis,endEpochMillis,wallNs,cpuNs,threadId,allocatedBytes;}
 private static int phaseCode(){return switch(phase){case "stationary"->1;case "walking"->2;case "settling"->3;case "done"->4;default->0;};}
 private static void transition(String next){phase=next;SnapshotHoldProbe.phase=next;phaseStart=System.nanoTime();phaseTicks=0;Blockbranch.LOGGER.info("INTEGRATED_WALK_PHASE {} epoch={} origin={},{},{}",next,System.currentTimeMillis(),originX,originY,originZ);}
 public static void register(){
  deadline=System.nanoTime()+600_000_000_000L;
  try{Files.writeString(directory().resolve("process-id.txt"),Long.toString(ProcessHandle.current().pid()));}catch(Exception e){throw new RuntimeException(e);}
  ServerChunkEvents.CHUNK_LOAD.register((level,chunk,newly)->{if(level.dimension()==net.minecraft.world.level.Level.OVERWORLD){loaded++;long key=chunk.getPos().pack();unique.add(key);if(newly){fresh++;uniqueFresh.add(key);}active.put(key,chunk);require(unique.size()<32768,"Bounded unique-column instrumentation");}});
  ServerChunkEvents.CHUNK_UNLOAD.register((level,chunk)->{if(level.dimension()==net.minecraft.world.level.Level.OVERWORLD)active.remove(chunk.getPos().pack(),chunk);});
  ServerTickEvents.START_SERVER_TICK.register(server->{if(done)return;tickStart=System.nanoTime();tickEpoch=System.currentTimeMillis();tickCpu=cpu();serverThreadId=Thread.currentThread().threadId();});
  ServerTickEvents.END_SERVER_TICK.register(server->{
   if(done||server.isDedicatedServer())return;
   try{
    if(server.getPlayerList().getPlayers().isEmpty()||!ready)return;var p=server.getPlayerList().getPlayers().getFirst();
    if(phase.equals("startup")){require(server.overworld().getSeed()==424242L&&server.overworld().getChunkSource().getGenerator() instanceof RealisticGenerator,"Fresh realistic world");p.getAbilities().flying=false;p.onUpdateAbilities();originX=p.getX();originY=p.getY();originZ=p.getZ();transition("stationary");}
    require(!p.getAbilities().flying,"Ground walking must not become flight");long end=System.nanoTime(),endCpu=cpu();int n=tickCount++;require(n<LIMIT,"Tick buffer exhausted");TICKS[0][n]=tickEpoch;TICKS[1][n]=System.currentTimeMillis();TICKS[2][n]=tickStart;TICKS[3][n]=end;TICKS[4][n]=tickCpu<0||endCpu<0?-1:endCpu-tickCpu;TICKS[5][n]=phaseCode();TICKS[6][n]=loaded;TICKS[7][n]=fresh;TICKS[8][n]=server.overworld().getChunkSource().getLoadedChunksCount();TICKS[9][n]=unique.size();TICKS[10][n]=uniqueFresh.size();TICKS[11][n]=active.size();if(n%20==0){residentSections=0;for(var c:active.values())residentSections+=((blockbranch.cubic.SparseColumn)c).blockbranch$sections().size();}TICKS[12][n]=residentSections;POSES[0][n]=p.getX();POSES[1][n]=p.getY();POSES[2][n]=p.getZ();phaseTicks++;
    if(phase.equals("stationary")&&phaseTicks>=200&&end-phaseStart>=15_000_000_000L)transition("walking");
    else if(phase.equals("walking")&&(p.getX()>=originX+128||end-phaseStart>=60_000_000_000L))transition("settling");
    else if(phase.equals("settling")&&phaseTicks>=200&&end-phaseStart>=10_000_000_000L){require(p.getX()>=originX+128,"Walking route did not reach128m before deadline");transition("done");done=true;}
   }catch(Throwable e){failure=e;done=true;}
  });
  ClientTickEvents.END_CLIENT_TICK.register(c->{
   try{
    require(System.nanoTime()<deadline,"Integrated diagnostic deadline");if(failure!=null)throw new AssertionError("Integrated server probe",failure);
    if(done){c.options.keyUp.setDown(false);c.options.keyJump.setDown(false);write();c.disconnect(new TitleScreen(),false);SnapshotHoldProbe.write(directory());Files.writeString(Path.of(System.getProperty("blockbranch.integratedWalkingResult")),"PASS bounded integrated ground walking diagnostic; not performance qualification\n");c.stop();return;}
    var screen=c.gui.screen();String name=screen==null?"":screen.getClass().getSimpleName();
    if(!opened&&name.equals("AccessibilityOnboardingScreen")){press(screen,"Continue");return;}
    if(!opened&&screen instanceof TitleScreen){opened=true;require(!Files.exists(c.gameDirectory.toPath().resolve("saves/"+WORLD)),"Fresh world must not exist");CreateWorldScreen.openFresh(c,()->c.gui.setScreen(new TitleScreen()));return;}
    if(!configured&&screen instanceof CreateWorldScreen create){var state=create.getUiState();state.setName(WORLD);state.setSeed("424242");state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);state.setDifficulty(net.minecraft.world.Difficulty.NORMAL);state.setAllowCommands(true);var preset=state.getNormalPresetList().stream().filter(e->e.preset().unwrapKey().orElseThrow().identifier().equals(Blockbranch.id("realistic"))).findFirst().orElseThrow();state.setWorldType(preset);configured=true;require(press(create,"Create New World"),"Create button");return;}
    if(screen!=null&&(name.equals("ConfirmExperimentalFeaturesScreen")||name.equals("BackupConfirmScreen"))){press(screen,"Proceed");return;}
    if(c.level==null||c.player==null)return;require(c.hasSingleplayerServer()&&!c.isMultiplayerServer()&&c.getConnection().getConnection().isMemoryConnection(),"Actual integrated in-memory connection");
    if(!aimed){c.player.setYRot(-90);c.player.setXRot(0);aimed=true;ready=true;}
    boolean walk=phase.equals("walking");if(walk){c.player.setYRot(-90);c.player.setXRot(0);}c.options.keyUp.setDown(walk);clientTicks++;boolean jump=walk&&c.player.onGround()&&c.player.horizontalCollision&&clientTicks-lastJump>20;c.options.keyJump.setDown(jump);if(jump)lastJump=clientTicks;
   }catch(Throwable e){failure=e;done=true;Blockbranch.LOGGER.error("INTEGRATED_WALK_FAIL",e);try{write();Files.writeString(Path.of(System.getProperty("blockbranch.integratedWalkingResult")),"FAIL "+e+"\n");}catch(Exception ignored){}c.stop();}
  });
 }
 private static void write()throws Exception{
  var f=new StringBuilder("startEpochMillis,endEpochMillis,startNano,endNano,cpuNs,phase,threadId,allocatedBytes\n");for(int n=0;n<frameCount;n++){for(int k=0;k<8;k++){if(k>0)f.append(',');f.append(FRAMES[k][n]);}f.append('\n');}Files.writeString(directory().resolve("frames.csv"),f);
  var t=new StringBuilder("startEpochMillis,endEpochMillis,startNano,endNano,cpuNs,phase,loadedCallbacks,freshCallbacks,nativeHolders,uniqueLoadedColumns,uniqueFreshColumns,eventResidentColumns,residentBlockSectionsSample,x,y,z\n");for(int n=0;n<tickCount;n++){for(int k=0;k<13;k++){if(k>0)t.append(',');t.append(TICKS[k][n]);}for(int k=0;k<3;k++)t.append(',').append(POSES[k][n]);t.append('\n');}Files.writeString(directory().resolve("ticks.csv"),t);
  Files.writeString(directory().resolve("metadata.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("seed",424242,"renderThreadId",renderThreadId,"serverThreadId",serverThreadId,"origin",new double[]{originX,originY,originZ},"frames",frameCount,"ticks",tickCount,"phase",phase,"phaseCodes",Map.of("startup",0,"stationary",1,"walking",2,"settling",3,"done",4))));
 }
 private static boolean press(net.minecraft.client.gui.screens.Screen s,String label){for(var v:s.children())if(v instanceof Button b&&b.active&&b.getMessage().getString().equals(label)){b.onPress(new KeyEvent(257,0,0));return true;}return false;}
 private static void require(boolean v,String text){if(!v)throw new AssertionError(text);}
}
