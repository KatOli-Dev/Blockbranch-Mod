package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraft.world.level.levelgen.structure.structures.*;

/** Four real codecs, placement states and native graph adapters, without a new site survey. */
public final class IntegratedDefinitionsQualification {
 private static final Path ROOT=Path.of(System.getProperty("blockbranch.integratedRoot"));
 private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
 private static int checks;
 static void require(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{
  try{run(server.overworld());server.saveEverything(false,true,true);Files.writeString(Path.of("result.txt"),"PASS");}
  catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("INTEGRATED_DEFINITIONS_FAIL",t);try{Files.writeString(Path.of("result.txt"),"FAIL "+t);}catch(Exception ignored){}}
  finally{server.halt(false);}
 });}
 static void run(ServerLevel level)throws Exception{
  var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
  var current=(RealisticGenerator)level.getChunkSource().getGenerator();
  var meta=RealisticGenerator.CODEC.codec().encodeStart(ops,current).getOrThrow().getAsJsonObject();
  require(meta.get("shape_version").getAsInt()==BorealCrownSchema.schema(171,current.borealCrownDefinition())&&current.borealCrownDefinition()==1&&current.pyramidGalleryDefinition()==1&&current.mansionWingDefinition()==1&&current.reimaginedDefinition()==5&&current.overworldDefinition()==1,"Combined mansion perimeter and crown definition");
  var definitions=new ArrayList<JsonObject>();var rows=new ArrayList<Object>();
  // Frozen native coverage witnesses from published ae86628; required inputs, not run output.
  var witness=JsonParser.parseString(Files.readString(ROOT.resolve("src/integrationTest/resources/fixtures/overworld-coverage/starts.json"))).getAsJsonObject();
  var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);var random=level.getChunkSource().randomState();
  for(int mode=0;mode<4;mode++){
   boolean reimagined=(mode&1)!=0,coverage=(mode&2)!=0;
   var definition=meta.deepCopy();definition.remove("boreal_crown_definition");definition.remove("mansion_wing_definition");definition.remove("pyramid_gallery_definition");if(!reimagined)definition.remove("reimagined_layouts");else definition.getAsJsonObject("reimagined_layouts").addProperty("definition",1);if(!coverage)definition.remove("native_overworld_structures");
   definition.addProperty("shape_version",mode==0?101:mode==3?103:102);definitions.add(definition);
   var generator=RealisticGenerator.CODEC.codec().parse(ops,definition).getOrThrow();
   require(generator.reimaginedDefinition()==(reimagined?1:0)&&generator.overworldDefinition()==(coverage?1:0),"Explicit flag combination "+mode);
   require(RealisticGenerator.CODEC.codec().encodeStart(ops,generator).getOrThrow().equals(definition),"Lossless definition roundtrip "+mode);
   if(Boolean.getBoolean("blockbranch.integratedCodecOnly"))continue;
   var threads=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long ownCpu=threads.getCurrentThreadCpuTime(),ownAllocated=threads.getThreadAllocatedBytes(Thread.currentThread().threadId());
   long begin=System.nanoTime(),cpu=ReimaginedRemainingQualification.cpu(),allocated=ReimaginedRemainingQualification.allocated();
   var state=generator.createState(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET),random,random.seed());
   var row=new LinkedHashMap<String,Object>();row.put("mode",mode);row.put("stateCurrentThreadCpuNanos",threads.getCurrentThreadCpuTime()-ownCpu);row.put("stateCurrentThreadAllocatedBytes",threads.getThreadAllocatedBytes(Thread.currentThread().threadId())-ownAllocated);row.put("stateWallNanos",System.nanoTime()-begin);row.put("stateProcessCpuNanos",ReimaginedRemainingQualification.cpu()-cpu);row.put("stateProcessAllocatedBytes",ReimaginedRemainingQualification.allocated()-allocated);
   var ownership=new TreeMap<String,Integer>();
   for(int kind=1;kind<=5;kind++){
    var key=ReimaginedStructures.key(kind);var holder=registry.getOrThrow(key);int count=state.getPlacementsForStructure(holder).size();
    require(count==(reimagined||coverage&&kind!=1?1:0),"Single effective placement owner "+mode+" "+key+" count="+count);
    require(OverworldStructures.additional(key.identifier(),generator)==(kind!=1&&!reimagined),"Adapter/decoration/locate owner "+mode+" "+key);ownership.put(key.identifier().toString(),count);
   }
   for(String id:List.of("village_plains","ancient_city","trial_chambers","trail_ruins","stronghold","mineshaft","shipwreck","ruined_portal_swamp"))require(!state.getPlacementsForStructure(registry.getOrThrow(ResourceKey.create(Registries.STRUCTURE,Identifier.withDefaultNamespace(id)))).isEmpty()==coverage,"Coverage placement opt-in "+mode+" "+id);
   for(String id:List.of("fortress","bastion_remnant","nether_fossil","end_city","ruined_portal_nether")){
    var key=ResourceKey.create(Registries.STRUCTURE,Identifier.withDefaultNamespace(id));require(state.getPlacementsForStructure(registry.getOrThrow(key)).isEmpty(),"No other-dimension placement "+id);require(!OverworldStructures.additional(key.identifier(),generator),"No other-dimension adapter "+id);
   }
   row.put("ownership",ownership);
   if(reimagined){
    int[][] points={{1,228504,-267736},{2,-701960,270984},{3,495048,117832},{4,-18040,-439192},{5,156904,-566872}};
    for(var point:points){var holder=registry.getOrThrow(ReimaginedStructures.key(point[0]));var at=new ChunkPos(point[1]>>4,point[2]>>4);var start=generate(level,generator,holder,at);require(start.isValid(),"Exact reimagined native generation "+mode+"/"+point[0]);
     require(start.getPieces().stream().anyMatch(p->p instanceof ReimaginedLayoutPiece||p instanceof ReimaginedMansionPiece||p instanceof ReimaginedSanctuaryPiece),"Versioned layout marker "+point[0]);
     if(point[0]==5)row.put("monument",monument(level,start,at,random.seed(),true));
    }
   }else if(coverage){
    var entry=witness.getAsJsonObject("monument").getAsJsonArray("chunk");var at=new ChunkPos(entry.get(0).getAsInt(),entry.get(1).getAsInt());var start=generate(level,generator,registry.getOrThrow(BuiltinStructures.OCEAN_MONUMENT),at);
    require(start.isValid(),"Coverage-only natural monument");row.put("monument",monument(level,start,at,random.seed(),false));
   }
   var city=witness.getAsJsonObject("ancient_city").getAsJsonArray("chunk");int cx=city.get(0).getAsInt(),cz=city.get(1).getAsInt();
   require(generator.getBiomeSource().createResolverForChunk(random.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.builder().enableCaches().build()),0,0,0,1,1,1).getNoiseBiome((cx*16+8)>>2,-16,(cz*16+8)>>2).is(Biomes.DEEP_DARK)==coverage,"Live deep-dark flag gate "+mode);
   if(coverage){var start=generate(level,generator,registry.getOrThrow(BuiltinStructures.ANCIENT_CITY),new ChunkPos(cx,cz));require(start.isValid(),"Unchanged final ancient-city fit "+mode);var box=StructurePiece.createBoundingBox(start.getPieces().stream());require(box.maxY()+16<=blockbranch.cubic.TerrainProfile.DEEPSLATE_TOP,"Entire city/cavern stays in actual deepslate "+mode);row.put("ancientCityBounds",box.toString());}
   rows.add(row);Files.writeString(Path.of("four-modes.json"),JSON.toJson(rows));
   blockbranch.Blockbranch.LOGGER.info("INTEGRATED_MODE_PASS {}",mode);
  }
  for(int mode=0;mode<4;mode++)for(int version:new int[]{100,101,102,103,104}){var bad=definitions.get(mode).deepCopy();bad.addProperty("shape_version",version);boolean expected=version==(mode==0?101:mode==3?103:102);require(RealisticGenerator.CODEC.codec().parse(ops,bad).result().isPresent()==expected,"Strict schema truth table "+mode+"/"+version);}
  for(String field:List.of("native_overworld_structures","native_ordinary_structures","native_hut_igloo")){var bad=meta.deepCopy();bad.addProperty(field,2);require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"Unknown flag "+field);}
  var bad=meta.deepCopy();bad.getAsJsonObject("reimagined_layouts").addProperty("definition",6);require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"Future layout rejected");
  bad=meta.deepCopy();bad.getAsJsonObject("additional_biomes").remove("deep_dark");require(RealisticGenerator.CODEC.codec().parse(ops,bad).error().isPresent(),"Coverage requires actual deep-dark holder");
  for(JsonElement malformed:List.of(new JsonPrimitive("invalid"),new JsonPrimitive(2),new JsonArray(),new JsonObject()))for(int mode:new int[]{1,3}){var invalid=definitions.get(mode).deepCopy();invalid.add("reimagined_layouts",malformed);require(RealisticGenerator.CODEC.codec().parse(ops,invalid).error().isPresent(),"Malformed layout cannot fall back to coverage-only102");}
  for(int mode:new int[]{2,3}){var invalid=definitions.get(mode).deepCopy();invalid.add("native_overworld_structures",new JsonObject());require(RealisticGenerator.CODEC.codec().parse(ops,invalid).error().isPresent(),"Malformed coverage cannot fall back to reimagined-only102");}
  var corrupt=meta.deepCopy();corrupt.getAsJsonObject("reimagined_layouts").addProperty("region_cache","corrupt-derived-cache");require(RealisticGenerator.CODEC.codec().parse(ops,corrupt).result().isPresent(),"Derived cache damage is recoverable without dropping opt-in");
  var readerRows=new ArrayList<Object>();int readerIndex=0;
  for(String label:List.of("released101","reimagined102","coverage102")){
   try(var reader=new java.net.URLClassLoader(new java.net.URL[]{ROOT.resolve("build/integrated-readers/"+label+".jar").toUri().toURL()},IntegratedDefinitionsQualification.class.getClassLoader()){
    @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
     if(!name.startsWith("blockbranch.realistic.RealisticGenerator")&&!name.equals("blockbranch.realistic.StructureGenerationSchema")&&!(label.equals("coverage102")&&name.startsWith("blockbranch.realistic.ScreeBiomeSource")))return super.loadClass(name,resolve);
     synchronized(getClassLoadingLock(name)){var c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;}
    }
   }){
    var codec=((com.mojang.serialization.MapCodec<?>)reader.loadClass("blockbranch.realistic.RealisticGenerator").getField("CODEC").get(null)).codec();var accepted=new ArrayList<Boolean>();
    for(int mode=0;mode<4;mode++){boolean actual=codec.parse(ops,definitions.get(mode)).result().isPresent();require(actual==(mode==0||mode==readerIndex&&readerIndex>0),"Actual historical codec "+label+" mode="+mode);accepted.add(actual);}readerRows.add(Map.of("reader",label,"acceptedModes",accepted));
   }readerIndex++;
  }
  for(String file:List.of("/workspace/Blockbranch/build/native-ordinary-qualified/generator.json",ROOT.resolve("src/integrationTest/resources/fixtures/overworld-coverage/generator.json").toString())){
   var saved=JsonParser.parseString(Files.readString(Path.of(file)));var decoded=RealisticGenerator.CODEC.codec().parse(ops,saved).getOrThrow();require(RealisticGenerator.CODEC.codec().encodeStart(ops,decoded).getOrThrow().equals(saved),"Actual saved definition unchanged: "+file);
  }
  var savedNbt=net.minecraft.nbt.NbtIo.readCompressed(Path.of("/workspace/Blockbranch/build/reimagined-first-corrected/structures/data/minecraft/world_gen_settings.dat"),net.minecraft.nbt.NbtAccounter.unlimitedHeap()).getCompoundOrEmpty("data").getCompoundOrEmpty("dimensions").getCompoundOrEmpty("minecraft:overworld").getCompoundOrEmpty("generator");
  var nbtOps=RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,level.registryAccess());var actualReimagined=net.minecraft.world.level.chunk.ChunkGenerator.CODEC.parse(nbtOps,savedNbt).getOrThrow();
  require(actualReimagined instanceof RealisticGenerator r&&r.reimaginedDefinition()==1&&r.overworldDefinition()==0,"Actual private102 save retains coverage opt-out");
  require(net.minecraft.world.level.chunk.ChunkGenerator.CODEC.encodeStart(nbtOps,actualReimagined).getOrThrow().equals(savedNbt),"Actual private102 NBT definition unchanged");
  require(!(level.getServer().getLevel(Level.NETHER).getChunkSource().getGenerator() instanceof RealisticGenerator)&&!(level.getServer().getLevel(Level.END).getChunkSource().getGenerator() instanceof RealisticGenerator),"Nether/End generators unchanged");
  Files.writeString(Path.of("historical-readers.json"),JSON.toJson(readerRows));Files.writeString(Path.of("definition-result.json"),JSON.toJson(Map.of("checks",checks,"schemaMapping","neither<=101; exclusive102; combined103 (local provisional)","actualOldDefinitionsUnchanged",true)));
 }
 static StructureStart generate(ServerLevel level,RealisticGenerator g,Holder.Reference<Structure> holder,ChunkPos at){var random=level.getChunkSource().randomState();return holder.value().generate(holder,level.dimension(),level.registryAccess(),g,g.getBiomeSource(),random.createClimateSampler(net.minecraft.world.level.levelgen.densityfunction.SamplerContext.builder().enableCaches().build()),random,level.getServer().getStructureTemplateManager(),random.seed(),at,0,OverworldCoverageQualification.generationBounds(),holder.value().biomes()::contains);}
 static Object monument(ServerLevel level,StructureStart start,ChunkPos at,long seed,boolean reimagined){
  var context=StructurePieceSerializationContext.fromLevel(level);var tag=start.createTag(context,at);var loaded=StructureStart.loadStaticStart(context,tag,seed);require(loaded!=null&&loaded.isValid(),"Native saved monument read");
  var rebuilt=OceanMonumentStructure.regeneratePiecesAfterLoad(at,seed,new PiecesContainer(loaded.getPieces()));
  var core=(OceanMonumentPieces.MonumentBuilding)start.getPieces().getFirst();var after=(OceanMonumentPieces.MonumentBuilding)rebuilt.pieces().getFirst();
  require(ReimaginedRemainingQualification.childBoxes(core).equals(ReimaginedRemainingQualification.childBoxes(after)),"Every child fitted after native graph reconstruction");
  require(core.getBoundingBox().equals(after.getBoundingBox()),"Root fitted after native graph reconstruction");
  require(rebuilt.pieces().size()==(reimagined?2:1),"Marker ownership on reload");
  require(core.getBoundingBox().minY()==(reimagined?-247:39),"Expected fitted/native original height");
  if(reimagined)require(start.getPieces().get(1).createTag(context).equals(rebuilt.pieces().get(1).createTag(context)),"Sanctuary Basis/LayoutDefinition/BB unchanged");
  return Map.of("rootY",core.getBoundingBox().minY(),"children",ReimaginedRemainingQualification.childBoxes(after),"marker",reimagined);
 }
}
