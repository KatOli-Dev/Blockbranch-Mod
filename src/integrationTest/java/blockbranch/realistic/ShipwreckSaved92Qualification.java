package blockbranch.realistic;
import java.nio.file.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
public final class ShipwreckSaved92Qualification {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
  WellInteraction.require(g.discoveryDefinition()==1&&g.waygateDefinition()==1&&g.shipwreckDefinition()==0,"Actual main92 world does not gain shipwrecks");
  var old=com.google.gson.JsonParser.parseString(Files.readString(Path.of("main92-config.json")));var parsed=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();WellInteraction.require(RealisticGenerator.CODEC.codec().encodeStart(ops,parsed).getOrThrow().equals(old),"Actual main21fbe2e flat generation metadata preserved exactly");
  var encoded=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();WellInteraction.require(encoded.get("shape_version").getAsInt()==92&&!encoded.has("shipwrecks"),"Actual92 save retains barrier and missing shipwreck field");
  var p=new net.minecraft.core.BlockPos(-84256,207,624288);WellInteraction.require(level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.WOOL.red()),"Actual92 saved edit retained");
  var holder=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.SHIPWRECK_BEACHED);WellInteraction.require(level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder).isEmpty(),"Disabled shipwreck placement remains empty");
  server.saveEverything(false,true,true);Files.writeString(Path.of("result.txt"),"PASS actual main21fbe2e schema92 save reopened; exact metadata and red wool edit; shipwrecks stay disabled\n");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("SHIPWRECK_SAVED92_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
}
