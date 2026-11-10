package blockbranch.realistic;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.item.MapItem;
/** Dedicated server opens a stopped copy: native map file, edited chunks and exact map colors. */
public final class ProgressiveMapPersistenceAudit {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  ProgressiveMapPolicyQualification.check();ProgressiveMapPolicyQualification.checkQueues(server.overworld());
  var root=Path.of(System.getProperty("blockbranch.mapAuditInput"));var metadata=com.google.gson.JsonParser.parseString(Files.readString(root.resolve("final-audit-metadata.json"))).getAsJsonObject();
  var data=MapItem.getSavedData(new net.minecraft.world.level.saveddata.maps.MapId(metadata.get("mapId").getAsInt()),server.overworld());
  String saved=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data.colors));WellInteraction.require(saved.equals(metadata.get("sha256").getAsString()),"Native dedicated map data loads exact saved pixels");
  var edits=com.google.gson.JsonParser.parseString(Files.readString(root.resolve("edit-witness.json"))).getAsJsonArray();
  for(var edit:edits){var a=edit.getAsJsonArray();var p=new net.minecraft.core.BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());WellInteraction.require(server.overworld().getBlockState(p).is(net.minecraft.world.level.block.Blocks.WOOL.red()),"Native edited block survives save and dedicated reopen");}
  WellInteraction.require(ProgressiveMapDrawing.audit(server)[2]==0,"No persisted map-owned ticket on dedicated reopen");
  server.saveEverything(false,true,true);Files.writeString(Path.of("result.txt"),"PASS dedicated native saved map pixels and four edited blocks; no persisted map ticket\n");
 }catch(Throwable failure){blockbranch.Blockbranch.LOGGER.error("MAP_PERSISTENCE_AUDIT_FAIL",failure);try{Files.writeString(Path.of("result.txt"),"FAIL "+failure);}catch(Exception ignored){}}finally{server.halt(false);}});}
 private ProgressiveMapPersistenceAudit(){}
}
