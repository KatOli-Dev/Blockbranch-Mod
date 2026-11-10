package blockbranch.realistic;
import java.nio.file.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
public final class MapWaygateCompatibility {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();
  WellInteraction.require(g.waygateDefinition()==1&&g.discoveryDefinition()==0,"Actual schema91 world remains waygate-only");
  var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
  var saved=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();
  WellInteraction.require(saved.get("shape_version").getAsInt()==91&&!saved.has("small_discovery_definition")&&!saved.has("treasure_cache"),"Schema91 serialization does not opt into treasure");
  for(int schema:new int[]{88,89,91,92}){
   var input=saved.deepCopy();input.addProperty("shape_version",schema);
   if(schema==88||schema==89)input.remove("desert_waygates");
   if(schema==89||schema==92)input.addProperty("small_discovery_definition",1);
   var decoded=RealisticGenerator.CODEC.codec().parse(ops,input).getOrThrow();
   WellInteraction.require(decoded.discoveryDefinition()==(schema==89||schema==92?1:0)&&decoded.waygateDefinition()==(schema==91||schema==92?1:0),"Feature definitions preserved for schema"+schema);
   var encoded=RealisticGenerator.CODEC.codec().encodeStart(ops,decoded).getOrThrow();WellInteraction.require(encoded.equals(input),"Flat codec roundtrip loses no generation option/cache at schema"+schema);
   if(schema==91||schema==92){
    var state=decoded.createState(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET),level.getChunkSource().randomState(),level.getSeed());
    var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
    WellInteraction.require(!state.getPlacementsForStructure(registry.getOrThrow(BuiltinStructures.RUINED_PORTAL_DESERT)).isEmpty(),"Waygate placement retained");
    WellInteraction.require(state.getPlacementsForStructure(registry.getOrThrow(BuiltinStructures.BURIED_TREASURE)).isEmpty()==(schema==91),"Only combined definition gains treasure placement");
   }
  }
  for(int schema:new int[]{90,93}){var bad=saved.deepCopy();bad.addProperty("shape_version",schema);WellInteraction.require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"Unsupported/reserved schema rejected");}
  var bad=saved.deepCopy();bad.addProperty("small_discovery_definition",1);WellInteraction.require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"91 cannot silently gain discovery");
  bad=saved.deepCopy();bad.addProperty("shape_version",92);WellInteraction.require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"Combined schema requires both definitions");
  var p=new net.minecraft.core.BlockPos(-84256,207,624288);boolean reopened=Files.exists(Path.of("edit-proof.txt"));
  if(reopened)WellInteraction.require(level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.WOOL.red()),"Actual saved edit survives schema91 native reopen");
  else{level.setBlock(p,net.minecraft.world.level.block.Blocks.WOOL.red().defaultBlockState(),3);Files.writeString(Path.of("edit-proof.txt"),p.toShortString());}
  server.saveEverything(false,true,true);Files.writeString(Path.of(reopened?"reopen-result.txt":"fresh-result.txt"),"PASS schema91 native "+(reopened?"reopen/edit":"creation")+"; flat schemas88/89/91/92, definitions, caches, placements and rejection checks\n");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("MAP_WAYGATE_COMPAT_FAIL",t);try{Files.writeString(Path.of("failure.txt"),t.toString());}catch(Exception ignored){}}finally{server.halt(false);}});}
}
