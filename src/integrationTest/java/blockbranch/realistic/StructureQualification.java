package blockbranch.realistic;
import java.util.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import com.google.gson.GsonBuilder;
public final class StructureQualification {
 private static int ticks;
 public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{
 if(++ticks==1){try{var q=Files.readString(Path.of("well.txt")).trim().split(",");server.overworld().setChunkForced(Integer.parseInt(q[0])>>4,Integer.parseInt(q[2])>>4,true);}catch(Exception e){throw new RuntimeException(e);}}
 if(ticks<40)return;boolean waiting=false;try{
  var level=server.overworld();var marker=Files.readString(Path.of("well.txt")).trim().split(",");
  long chunk=net.minecraft.world.level.ChunkPos.pack(Integer.parseInt(marker[0])>>4,Integer.parseInt(marker[2])>>4);
  if(!level.areEntitiesLoaded(chunk)||!level.getChunkSource().isPositionTicking(chunk)){
   WellInteraction.require(ticks<1200,"Native entity tracking ready before brush interaction");waiting=true;return;
  }
  var vanillaProbe=new net.minecraft.world.level.chunk.ProtoChunk(new net.minecraft.world.level.ChunkPos(-3,-3),net.minecraft.world.level.chunk.UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
  var highProbe=new net.minecraft.core.BlockPos(-40,400,-40);vanillaProbe.setBlockState(highProbe,net.minecraft.world.level.block.Blocks.SANDSTONE.defaultBlockState(),0);
  WellInteraction.require(!vanillaProbe.getBlockState(highProbe).is(net.minecraft.world.level.block.Blocks.SANDSTONE),"Nonprocedural generator write window unchanged");
  var ground=new int[256];var water=new int[256];Arrays.fill(ground,399);Arrays.fill(water,Integer.MIN_VALUE);var factory=level.palettedContainerFactory();
  var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DESERT);
  var base=new blockbranch.cubic.TerrainProfile(TerrainShape.FLOOR,TerrainShape.CEILING,ground,water,new net.minecraft.world.level.chunk.PalettedContainer<>(biome,factory.biomeStrategy()),factory.blockStatesStrategy());
  ((blockbranch.cubic.SparseColumn)vanillaProbe).blockbranch$terrain(new blockbranch.cubic.ColumnTerrain(vanillaProbe,base,factory,null,new int[0]));
  vanillaProbe.setBlockState(highProbe,net.minecraft.world.level.block.Blocks.SUSPICIOUS_SAND.defaultBlockState(),0);
  var highEntity=new net.minecraft.world.level.block.entity.BrushableBlockEntity(highProbe,net.minecraft.world.level.block.Blocks.SUSPICIOUS_SAND.defaultBlockState());
  highEntity.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY,7);vanillaProbe.setBlockEntity(highEntity);
  var promoted=new net.minecraft.world.level.chunk.LevelChunk(level,vanillaProbe,null);
  WellInteraction.require(promoted.getBlockEntity(highProbe)==highEntity,"Promotion preserves original high block entity before validation");
  var generator=(RealisticGenerator)level.getChunkSource().getGenerator();var codecOps=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
  var old=RealisticGenerator.CODEC.codec().encodeStart(codecOps,generator).getOrThrow().getAsJsonObject();
  if(generator.structureDefinition()==3){
   var interaction=WellInteraction.run(level);
   Files.writeString(Path.of("qualification.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("definition",3,"interaction",interaction,"highPromotion",true)));
   Files.writeString(Path.of("qualification-result.txt"),"PASS");return;
  }
  WellInteraction.require(generator.structureDefinition()==1,"Retained definition 1 save");
  var updated=old.deepCopy();updated.addProperty("shape_version",87);updated.addProperty("structure_definition",2);
  WellInteraction.require(RealisticGenerator.CODEC.codec().parse(codecOps,updated).getOrThrow().structureDefinition()==2,"Calibrated definition roundtrip");
  updated.addProperty("shape_version",86);WellInteraction.require(RealisticGenerator.CODEC.codec().parse(codecOps,updated).error().isPresent(),"Definition 2 rejects old schema");
  var report=new LinkedHashMap<String,Object>();var surveys=new ArrayList<Object>();
  var normal=WorldPresets.getNormalOverworld(level.registryAccess()).generator();
  var settings=level.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
  var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
  var desert=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DESERT);
  for(var group:desert.value().getGenerationSettings().features())for(var feature:group)if(feature.unwrapKey().orElseThrow().identifier().getPath().contains("desert_well"))report.put("vanillaFeature",net.minecraft.world.level.levelgen.placement.PlacedFeature.DIRECT_CODEC.encodeStart(ops,feature.value()).getOrThrow());
  for(long seed:new long[]{424242,1,-17}){
   var random=RandomState.create(level.registryAccess().lookupOrThrow(Registries.NOISE),seed,settings);var resolver=normal.getBiomeSource().createUncachedResolver(random);
   int n=128,step=128;boolean[] mask=new boolean[n*n];
   for(int z=0;z<n;z++)for(int x=0;x<n;x++)mask[z*n+x]=resolver.getNoiseBiome((x*step-8192)>>2,20,(z*step-8192)>>2).is(Biomes.DESERT);
   var sizes=new ArrayList<Integer>();int clipped=0;var seen=new BitSet(n*n);
   for(int start=0;start<n*n;start++){if(!mask[start]||seen.get(start))continue;var queue=new ArrayDeque<Integer>();queue.add(start);seen.set(start);int count=0;boolean edge=false;
    while(!queue.isEmpty()){int i=queue.remove(),x=i%n,z=i/n;count++;edge|=x==0||z==0||x==n-1||z==n-1;for(int j:new int[]{x>0?i-1:-1,x<n-1?i+1:-1,z>0?i-n:-1,z<n-1?i+n:-1})if(j>=0&&mask[j]&&!seen.get(j)){seen.set(j);queue.add(j);}}
    if(edge)clipped++;else sizes.add(count);
   }
   surveys.add(Map.of("seed",seed,"step",step,"windowMin",-8192,"windowMaxExclusive",8192,"closedRegionCells",sizes,"clippedRegionsExcluded",clipped,"expectedProposalsPerRegion",sizes.stream().map(c->c*step*step/256000.0).toList()));
  }
  report.put("vanillaRegions",surveys);
  Files.writeString(Path.of("vanilla-regions.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
  report.put("interaction",WellInteraction.run(level));
  report.put("planner",WellPlannerQualification.run(level));Files.writeString(Path.of("qualification.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
 Files.writeString(Path.of("qualification-result.txt"),"PASS");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("QUALIFICATION_FAIL",t);try{Files.writeString(Path.of("qualification-result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{if(!waiting)server.halt(false);}});}
}
