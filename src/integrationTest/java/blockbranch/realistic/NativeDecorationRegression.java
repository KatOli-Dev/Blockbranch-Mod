package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Actual native generator calls compared with the registry before the crypt was added. */
public final class NativeDecorationRegression {
 private static final Map<String,Map<String,Object>> structures=new TreeMap<>(),features=new TreeMap<>();
 private static final Map<String,Long> structureCalls=new TreeMap<>(),featureCalls=new TreeMap<>();
 private static final Map<Registry<Structure>,Map<Structure,Integer>> expected=new IdentityHashMap<>();
 static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static boolean enabled(){return Boolean.getBoolean("blockbranch.nativeDecorationRegression");}
 private static void seed(WorldGenLevel world,ChunkAccess chunk,long actual){var rng=new WorldgenRandom(new XoroshiroRandomSource(0));require(actual==rng.setDecorationSeed(world.getSeed(),chunk.getPos().getMinBlockX(),chunk.getPos().getMinBlockZ()),"Native decoration base seed unchanged");}
 public static synchronized void structure(WorldGenLevel world,ChunkAccess chunk,Structure structure,long seed,int index,int step){
  if(!enabled())return;seed(world,chunk,seed);var registry=world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
  var baseline=expected.computeIfAbsent(registry,r->{var result=new IdentityHashMap<Structure,Integer>();int[] counters=new int[GenerationStep.Decoration.values().length];for(var entry:r)if(!r.getKey(entry).equals(ReimaginedStructures.key(1).identifier()))result.put(entry,counters[entry.step().ordinal()]++);return result;});
  require(baseline.containsKey(structure)&&baseline.get(structure)==index&&structure.step().ordinal()==step,"Historical native structure seed index "+registry.getKey(structure)+" actual="+index+" expected="+baseline.get(structure));
  String dimension=world.getLevel().dimension().identifier().toString(),id=registry.getKey(structure).toString();structureCalls.merge(dimension,1L,Long::sum);
  structures.computeIfAbsent(dimension,d->new TreeMap<>()).put(id,Map.of("step",step,"index",index));
 }
 public static synchronized void feature(WorldGenLevel world,ChunkAccess chunk,PlacedFeature feature,FeatureSorter.StepFeatureData data,long seed,int index,int step){
  if(!enabled())return;seed(world,chunk,seed);require(index==data.indexMapping().applyAsInt(feature)&&data.features().get(index)==feature,"Native biome feature index unchanged");
  String dimension=world.getLevel().dimension().identifier().toString(),id=String.valueOf(world.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getKey(feature));featureCalls.merge(dimension,1L,Long::sum);
  features.computeIfAbsent(dimension,d->new TreeMap<>()).put(step+":"+id,Map.of("step",step,"index",index));
 }
 public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();require(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator,"Actual normal NoiseBased Overworld, no opt-ins");
  var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);var oldIndices=new LinkedHashMap<String,Integer>();var rawIndices=new LinkedHashMap<String,Integer>();int raw=0,old=0;
  for(var entry:registry)if(entry.step()==GenerationStep.Decoration.UNDERGROUND_STRUCTURES){String id=registry.getKey(entry).toString();rawIndices.put(id,raw++);if(!id.equals(ReimaginedStructures.key(1).identifier().toString()))oldIndices.put(id,old++);}
  require(rawIndices.get("minecraft:trail_ruins")==oldIndices.get("minecraft:trail_ruins")+1&&rawIndices.get("minecraft:trial_chambers")==oldIndices.get("minecraft:trial_chambers")+1,"Unfiltered7dd negative control reproduces index shift");
  var holder=registry.getOrThrow(BuiltinStructures.TRIAL_CHAMBERS);var state=level.getChunkSource().getGeneratorState();StructureStart chosen=null;
  outer:for(var placement:state.getPlacementsForStructure(holder))if(placement instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread)for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){
   var p=spread.getPotentialStructureChunk(level.getSeed(),x*spread.spacing(),z*spread.spacing());var chunk=level.getChunk(p.x(),p.z(),net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS);var start=level.structureManager().getStartForStructure(holder.value(),chunk);if(start!=null&&start.isValid()){chosen=start;break outer;}
  }
  require(chosen!=null,"Bounded actual native trial start");var box=chosen.getBoundingBox();int requested=0;
  for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++)for(int x=box.minX()>>4;x<=box.maxX()>>4;x++){level.getChunk(x,z);requested++;}
  var tables=new ArrayList<String>();int items=0,containers=0;
  for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++)for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)for(var be:level.getChunk(x,z).getBlockEntities().values())if(box.isInside(be.getBlockPos())&&be instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity chest&&chest.getLootTable()!=null){tables.add(chest.getLootTable().identifier().toString());containers++;chest.unpackLootTable(null);for(int i=0;i<chest.getContainerSize();i++)items+=chest.getItem(i).getCount();}
  require(containers>0&&items>0&&tables.stream().anyMatch(t->t.contains("trial_chambers")),"Actual normal native trial placement and original loot execution");
  for(var dimension:List.of(Level.NETHER,Level.END)){var other=server.getLevel(dimension);require(other!=null&&other.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator,"Unchanged normal dimension generator "+dimension);other.getChunk(0,0);}
  for(String dimension:List.of("minecraft:overworld","minecraft:the_nether","minecraft:the_end")){require(structureCalls.getOrDefault(dimension,0L)>0&&featureCalls.getOrDefault(dimension,0L)>0,"Actual base decoration and biome feature calls in "+dimension);require(!structures.get(dimension).containsKey(ReimaginedStructures.key(1).identifier().toString()),"No crypt in native seed order");}
  var report=new LinkedHashMap<String,Object>();report.put("normalNoiseGenerator",true);report.put("unfiltered7ddIndices",rawIndices);report.put("preCryptBaselineIndices",oldIndices);report.put("actualStructureCalls",structureCalls);report.put("actualFeatureCalls",featureCalls);report.put("actualStructureIndices",structures);report.put("actualBiomeFeatureIndices",features);report.put("nativeTrial",Map.of("bounds",box.toString(),"pieces",chosen.getPieces().size(),"requestedChunks",requested,"lootContainers",containers,"unpackedItems",items,"nativeTables",tables));
  Files.writeString(Path.of("native-decoration.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));server.saveEverything(false,true,true);Files.writeString(Path.of("result.txt"),"PASS");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("NATIVE_DECORATION_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
}
