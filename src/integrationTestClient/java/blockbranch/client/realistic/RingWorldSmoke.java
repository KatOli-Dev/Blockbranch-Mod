package blockbranch.client.realistic;
import java.nio.file.*;import java.util.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
/** Copied realistic world; normal placement, resize and UI reload. Passive existing ray/mesh oracle. */
public final class RingWorldSmoke {
 private static final int[][] ROUTE={{0,0,0},{16,0,0},{32,0,16},{16,0,0},{16,16,0},{16,-16,0},{272,0,0},{0,0,0},{0,0,0},{0,0,0},{0,0,0},{16,0,16},{0,0,0}};
 private static final String[] NAMES={"initial","horizontal-boundary","diagonal","reverse","vertical-up","vertical-down","large-teleport","return","resize-down","resize-up","world-reload","reload-diagonal","reload-reverse"};
 private static volatile int request=-1,applied=-2;private static volatile BlockPos target;private static volatile Throwable error;
 private static int stage,ticks,good;private static boolean opened,done,reopening;private static long started,deadline;
 private static final List<Map<String,Object>> rows=new ArrayList<>();
 private static Path dir(){return Path.of(System.getProperty("blockbranch.ringWorldResult")).getParent();}
 public static void register(){
  deadline=System.nanoTime()+600_000_000_000L;
  ServerTickEvents.END_SERVER_TICK.register(s->{if(done||s.isDedicatedServer()||request<0||request==applied)return;try{
   if(s.getPlayerList().getPlayers().isEmpty())return;var p=s.getPlayerList().getPlayers().getFirst();var level=s.overworld();check(level.getSeed()==424242L,"seed");
   int[] d=ROUTE[request];target=new BlockPos(-84256+d[0],256+d[1],624288+d[2]);check(level.getBlockState(target).isAir()&&level.getBlockState(target.above()).isAir(),"camera air");
   var source=s.createCommandSourceStack();String name=p.getName().getString();s.getCommands().performPrefixedCommand(source,"gamemode creative "+name);p.getAbilities().flying=true;p.onUpdateAbilities();
   s.getCommands().performPrefixedCommand(source,"time set 6000");s.getCommands().performPrefixedCommand(source,"tp "+name+" "+(target.getX()+.5)+" "+target.getY()+" "+(target.getZ()+.5)+" -90 65");applied=request;
  }catch(Throwable t){error=t;}});
  ClientTickEvents.END_CLIENT_TICK.register(c->{if(done)return;try{
   if(error!=null)throw new AssertionError("server fixture",error);check(System.nanoTime()<deadline,"global deadline");var screen=c.gui.screen();
   if(!opened&&screen instanceof TitleScreen){opened=true;c.createWorldOpenFlows().openWorld("RingWorld",()->c.gui.setScreen(new TitleScreen()));return;}
   if(c.level==null||c.player==null)return;check(c.hasSingleplayerServer()&&c.getConnection().getConnection().isMemoryConnection(),"integrated client");
   if(request<0){started=System.nanoTime();request=stage;return;}
   CaveApproachTimeoutSnapshot.observe(c,target,stage,applied,request,false);
   if(System.nanoTime()-started>90_000_000_000L){Files.writeString(dir().resolve("timeout.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(CaveApproachTimeoutSnapshot.capture(c,target,stage,applied,request,false)));throw new AssertionError("90-second readiness timeout stage="+stage);}
   if(applied!=stage||target==null||c.player.position().distanceToSqr(target.getX()+.5,target.getY(),target.getZ()+.5)>.02)return;
   if(++ticks%10!=0)return;var audit=UndergroundAutomaticRenderSmoke.audit(c);
   if(((Number)audit.get("missing")).intValue()!=0||((Number)audit.get("hits")).intValue()<=40){good=0;if(ticks%100==0)blockbranch.Blockbranch.LOGGER.info("RING_WORLD_WAIT stage={} audit={}",stage,audit);return;}
   if(++good<2)return;
   int expected=stage==8?4:8;check(c.options.getEffectiveRenderDistance()==expected&&c.levelRenderer.viewArea().getViewDistance()==expected,"native resize applied");
   var row=new LinkedHashMap<String,Object>();row.put("stage",stage);row.put("name",NAMES[stage]);row.put("target",target.toShortString());row.put("seconds",(System.nanoTime()-started)/1e9);row.put("distance",expected);row.putAll(audit);rows.add(row);blockbranch.Blockbranch.LOGGER.info("RING_WORLD_PASS {}",row);
   Files.writeString(dir().resolve("observations.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));
   if(stage==ROUTE.length-1){done=true;c.disconnect(new TitleScreen(),false);blockbranch.qa.UniformBiomeReuseProbe.write(dir());Files.writeString(Path.of(System.getProperty("blockbranch.ringWorldResult")),"PASS 13 actual-world mesh/visibility views: horizontal, diagonal, reverse, vertical, teleport, resize, reload\n");c.stop();return;}
   stage++;ticks=good=0;started=System.nanoTime();
   if(stage==8)c.options.renderDistance().set(4);if(stage==9)c.options.renderDistance().set(8);
   if(stage==10){request=-1;applied=-2;target=null;c.disconnect(new TitleScreen(),false);opened=false;return;}
   request=stage;
  }catch(Throwable t){done=true;blockbranch.Blockbranch.LOGGER.error("RING_WORLD_FAIL",t);try{Files.writeString(Path.of(System.getProperty("blockbranch.ringWorldResult")),"FAIL "+t+"\n");}catch(Exception ignored){}c.stop();}});
 }
 private static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
}
