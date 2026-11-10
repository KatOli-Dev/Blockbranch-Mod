package blockbranch.realistic;
import java.nio.file.*;import java.util.*;
/** Observe actual production seeding calls against the pre-crypt native key order. */
public final class IntegratedSeedAudit {
 private static final Map<Integer,Long> calls=new TreeMap<>();
 // Frozen native underground order before any reimagined registry entry existed.
 // Buried treasure(index0) has its own existing decoration pass.
 public static synchronized void seed(int index,int step){
  if(!Boolean.getBoolean("blockbranch.integratedSeedAudit")||step!=net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_STRUCTURES.ordinal())return;
  if(index<1||index>4)throw new AssertionError("New crypt registry entry shifted historical native coverage seed index: "+index);
  calls.merge(index,1L,Long::sum);
 }
 public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->{try{
  synchronized(IntegratedSeedAudit.class){if(!calls.keySet().equals(Set.of(1,2,3,4)))throw new AssertionError("Missing actual native seed calls: "+calls);
   if(new HashSet<>(calls.values()).size()!=1)throw new AssertionError("Each production decoration pass must seed every original native key once: "+calls);
   Files.writeString(Path.of("seed-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed",true,"actualProductionCalls",calls,"historicalKeys",List.of("minecraft:buried_treasure","minecraft:mineshaft","minecraft:mineshaft_mesa","minecraft:trail_ruins","minecraft:trial_chambers"))));}
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("INTEGRATED_SEED_AUDIT_FAIL",t);try{Files.writeString(Path.of("seed-audit.json"),"FAIL "+t);}catch(Exception ignored){}}});}
}
