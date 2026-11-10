package blockbranch.realistic;
import java.nio.file.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
/** Reopens the retained adopted world, including its already consumed archaeology deposit. */
public final class WaygateLegacyQualification {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();WellInteraction.require(g.waygateDefinition()==0&&g.structureDefinition()==3,"Retained schema88 world remains opted out");
  var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());var config=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();WellInteraction.require(config.get("shape_version").getAsInt()==88&&!config.has("desert_waygates"),"Actual saved schema remains 88 without waygate field");
  var q=Files.readString(Path.of("well.txt")).trim().split(",");var p=new DesertWell.Plan(Integer.parseInt(q[0]),Integer.parseInt(q[1]),Integer.parseInt(q[2]),Long.parseLong(q[3]));var consumed=new BlockPos(p.x()-1,p.y()-1,p.z());var edited=new BlockPos(p.x()-3,p.y()+2,p.z()-3);int checked=0;
  for(var block:DesertWell.blocks(p)){
   if(block.pos().equals(consumed)){WellInteraction.require(level.getBlockState(consumed).is(Blocks.SAND)&&level.getBlockEntity(consumed)==null,"Previously brushed deposit stays consumed");continue;}
   if(block.pos().equals(edited)){WellInteraction.require(level.getBlockState(edited).isAir(),"Previous player edit remains air");continue;}
   WellInteraction.require(level.getBlockState(block.pos())==block.state(),"All other saved well blocks remain unchanged at "+block.pos());checked++;
  }
  server.saveEverything(false,true,true);Files.writeString(Path.of("waygate-legacy-result.txt"),"PASS actual schema88 reopen; no waygates; consumed archaeology and player edit retained; checked="+checked);
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("WAYGATE_LEGACY_FAIL",t);try{Files.writeString(Path.of("waygate-legacy-result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
}
