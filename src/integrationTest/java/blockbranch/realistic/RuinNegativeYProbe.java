package blockbranch.realistic;

import java.nio.file.*;import java.util.*;import net.minecraft.core.BlockPos;import net.minecraft.core.registries.Registries;import net.minecraft.world.level.levelgen.structure.BuiltinStructures;import net.minecraft.world.level.chunk.status.ChunkStatus;
/** Bounded reachability check of unchanged adopted definitions, distinct from queue fixtures. */
public final class RuinNegativeYProbe {
 public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{try{run(server.overworld());Files.writeString(Path.of("result.txt"),"PASS");}catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("ADOPTED_NEGATIVE_Y_PROBE_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
 public static void run(net.minecraft.server.level.ServerLevel level)throws Exception{
  var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();var out=new LinkedHashMap<String,Object>();
  for(boolean warm:new boolean[]{true,false}){var rows=new ArrayList<Object>();var plans=new ArrayList<int[]>();int checked=0;
   if(warm){for(var region:g.warmRuins(random).ordered(BlockPos.ZERO,6000000).stream().limit(24).toList()){checked++;var p=region.plan();if(p.isPresent()){var v=p.get();rows.add(v);if(v.y()< -64){plans.add(new int[]{v.x(),v.y(),v.z()});break;}}}}
   else{for(var region:g.coldRuins(random).ordered(BlockPos.ZERO,6000000).stream().limit(24).toList()){checked++;var p=region.plan();if(p.isPresent()){var v=p.get();rows.add(v);if(v.y()< -64){plans.add(new int[]{v.x(),v.y(),v.z()});break;}}}}
   var family=new LinkedHashMap<String,Object>();family.put("regionsChecked",checked);family.put("acceptedPlans",rows);family.put("negativePlanFound",!plans.isEmpty());
   if(!plans.isEmpty()){var p=plans.getFirst();var holder=level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(warm?BuiltinStructures.OCEAN_RUIN_WARM:BuiltinStructures.OCEAN_RUIN_COLD);var chunk=level.getChunk(p[0]>>4,p[2]>>4,ChunkStatus.STRUCTURE_STARTS);var start=level.structureManager().getStartForStructure(holder.value(),chunk);boolean valid=start!=null&&start.isValid();family.put("nativeStartValid",valid);if(valid){var b=start.getBoundingBox();for(int z=b.minZ()>>4;z<=b.maxZ()>>4;z++)for(int x=b.minX()>>4;x<=b.maxX()>>4;x++)level.getChunk(x,z);int archaeology=0;for(int y=b.minY();y<=b.maxY();y++)for(int z=b.minZ();z<=b.maxZ();z++)for(int x=b.minX();x<=b.maxX();x++)if(level.getBlockState(new BlockPos(x,y,z)).is(warm?net.minecraft.world.level.block.Blocks.SUSPICIOUS_SAND:net.minecraft.world.level.block.Blocks.SUSPICIOUS_GRAVEL))archaeology++;family.put("nativeBox",b.toString());family.put("archaeology",archaeology);family.put("nativePieces",start.getPieces().size());}}
   out.put(warm?"warmDefinition1":"coldDefinition1",family);
  }
  Files.writeString(Path.of("adopted-negative-y-probe.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(out));
 }
}
