package blockbranch.realistic;
import blockbranch.cubic.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
/** Fixed input, production-call count and frozen pre-change serialization reference. */
public final class SectionGenerationWorkSmoke {
 private record Case(String name,TerrainProfile profile,int section){}
 public static void verify(ServerLevel level,RealisticGenerator generator)throws Exception {
  var factory=level.palettedContainerFactory();var ground=new int[256];Arrays.fill(ground,40);var dry=new int[256];Arrays.fill(dry,Integer.MIN_VALUE);
  var uniform=new TerrainProfile(-64,128,ground,dry,factory.createForBiomes(),factory.blockStatesStrategy());
  var lateGround=ground.clone();lateGround[255]=14;var late=new TerrainProfile(-64,128,lateGround,dry,factory.createForBiomes(),factory.blockStatesStrategy());
  var earlyGround=ground.clone();earlyGround[1]=-1;var early=new TerrainProfile(-64,128,earlyGround,dry,factory.createForBiomes(),factory.blockStatesStrategy());
  var firstGround=ground.clone();firstGround[0]=-1;var first=new TerrainProfile(-64,128,firstGround,dry,factory.createForBiomes(),factory.blockStatesStrategy());
  var surface=new TerrainProfile(-64,128,new int[256],dry,factory.createForBiomes(),factory.blockStatesStrategy());
  var wetGround=new int[256];Arrays.fill(wetGround,-32);var water=new int[256];Arrays.fill(water,64);
  var wet=new TerrainProfile(-64,128,wetGround,water,factory.createForBiomes(),factory.blockStatesStrategy());
  var waterline=new int[256];Arrays.fill(waterline,8);var layered=new TerrainProfile(-64,128,new int[256],waterline,factory.createForBiomes(),factory.blockStatesStrategy());
  var mixedBiomes=factory.createForBiomes();var originalBiome=mixedBiomes.get(0,0,0);
  var alternate=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrThrow(originalBiome.is(net.minecraft.world.level.biome.Biomes.DESERT)?net.minecraft.world.level.biome.Biomes.PLAINS:net.minecraft.world.level.biome.Biomes.DESERT);
  for(int y=0;y<4;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++)mixedBiomes.set(x,y,z,((x+y+z)&1)==0?originalBiome:alternate);
  var mixed=new TerrainProfile(-64,128,new int[256],waterline,mixedBiomes,factory.blockStatesStrategy());
  var origin=generator.getOrigin(level.getChunkSource().randomState());var natural=((SparseColumn)level.getChunk(origin.x(),origin.z())).blockbranch$terrain().base();int top=natural.groundHeight(0,0)>>4;
  var cases=new ArrayList<Case>(List.of(new Case("uniform-rock",uniform,0),new Case("uniform-deepslate",uniform,-1),new Case("uniform-air",uniform,8),new Case("first-value-exception",first,0),new Case("early-difference",early,0),new Case("last-difference",late,0),new Case("surface",surface,0),new Case("uniform-water",wet,0),new Case("layered-water-air",layered,0),new Case("mixed-biomes-water",mixed,0),new Case("natural-surface",natural,top),new Case("natural-cave",natural,top-8),new Case("build-min",uniform,ColumnLimits.MIN_Y>>4),new Case("build-max",uniform,(ColumnLimits.MAX_Y>>4)-1)));
  for(int[] site:new int[][]{{-83949,175,623061},{237376,-39,-262139},{237509,-201,-262292}}){var p=((SparseColumn)level.getChunk(site[0]>>4,site[2]>>4)).blockbranch$terrain().base();cases.add(new Case("native-biome-"+site[1],p,site[1]>>4));}
  var rows=new ArrayList<Map<String,Object>>();long maximum=0;
  for(var c:cases){
   SectionGenerationWorkProbe.begin();LevelChunkSection actual;long calls;try{actual=c.profile.generate(c.section);}finally{calls=SectionGenerationWorkProbe.end();}
   var expected=reference(c.profile,c.section);
   SectionGenerationWorkProbe.begin();PalettedContainer<BlockState> raw;long paletteCalls;
   try{raw=c.profile.generateStates(c.section);paletteCalls=SectionGenerationWorkProbe.palettes();}finally{SectionGenerationWorkProbe.end();}
   require(paletteCalls==0,"Raw snapshot path must not construct discarded biomes");
   var wrapped=new LevelChunkSection(raw,c.profile.view(c.section).getBiomes());require(Arrays.equals(encoded(wrapped),encoded(expected)),"Raw block palette preserves exact native bytes: "+c.name);
   SectionGenerationWorkProbe.begin();try{c.profile.generate(c.section);require(SectionGenerationWorkProbe.palettes()==1,"Reference full path exercises discarded biome work");}finally{SectionGenerationWorkProbe.end();}

   for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
    require(actual.getBlockState(x,y,z)==expected.getBlockState(x,y,z),"Block mismatch "+c.name);
    require(actual.getFluidState(x,y,z)==expected.getFluidState(x,y,z),"Fluid mismatch "+c.name);
   }
   for(int y=0;y<4;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++)require(actual.getNoiseBiome(x,y,z).equals(expected.getNoiseBiome(x,y,z)),"Biome mismatch "+c.name);
   require(actual.hasOnlyAir()==expected.hasOnlyAir()&&actual.hasFluid()==expected.hasFluid()&&actual.isRandomlyTickingBlocks()==expected.isRandomlyTickingBlocks()&&actual.isRandomlyTickingFluids()==expected.isRandomlyTickingFluids(),"Section metadata mismatch "+c.name);
   require(Arrays.equals(encoded(actual),encoded(expected)),"Native section encoding mismatch "+c.name);
   rows.add(Map.of("name",c.name,"productionStateCalls",calls,"nativeSerializedBytes",encoded(actual).length));maximum=Math.max(maximum,calls);
  }
  fixedCost(cases,Path.of(System.getProperty("blockbranch.realisticSmokeResult")).getParent());
  SectionGenerationWorkProbe.begin();long knownAirCalls;
  try{uniform.generateStates(8);}finally{knownAirCalls=SectionGenerationWorkProbe.end();}
  require(knownAirCalls==0,"Exact known-air proof must avoid all state evaluations");
  var before=mixed.encode(factory);var a=mixed.generate(0);var b=mixed.generate(0);var original=mixed.state(0,0,0);
  a.setBlockState(0,0,0,Blocks.DIAMOND_BLOCK.defaultBlockState());a.fillBiomesFromNoise((x,y,z)->alternate,0,0,0);
  require(a.getBlockState(0,0,0).is(Blocks.DIAMOND_BLOCK)&&a.getNoiseBiome(0,0,0).equals(alternate),"Consumer mutation applied");
  require(b.getBlockState(0,0,0)==original&&b.getNoiseBiome(0,0,0).equals(originalBiome)&&before.equals(mixed.encode(factory)),"Consumer mutations cannot change another section/profile");
  var failedProfileBefore=uniform.encode(factory);boolean failed=false;SectionGenerationWorkProbe.begin(32);try{uniform.generate(0);}catch(SectionGenerationWorkProbe.InjectedFailure expected){failed=true;}finally{SectionGenerationWorkProbe.end();}
  require(failed,"Injected state exception propagates");require(failedProfileBefore.equals(uniform.encode(factory)),"Failed profile remains unchanged");require(Arrays.equals(encoded(uniform.generate(0)),encoded(reference(uniform,0))),"Generation recovers after failure");
  var concurrentProfile=natural.encode(factory);var jobs=new ArrayList<CompletableFuture<Boolean>>();
  for(int worker=0;worker<4;worker++)jobs.add(CompletableFuture.supplyAsync(()->{
   for(int repeat=0;repeat<4;repeat++){
    var empty=uniform.generateStates(8);empty.set(0,0,0,Blocks.GOLD_BLOCK.defaultBlockState());
    require(uniform.generateStates(8).get(0,0,0).isAir(),"Concurrent known-air template copies remain independent");
    var section=natural.generate(top);var copy=section.getStates().copy();var value=section.getBlockState(0,0,0);copy.set(0,0,0,Blocks.GOLD_BLOCK.defaultBlockState());
    require(section.getBlockState(0,0,0)==value&&natural.state(0,top*16,0)==value,"Concurrent consumer copy isolation");
    require(concurrentProfile.equals(natural.encode(factory)),"Concurrent profile encoding unchanged");
   }return true;
  },net.minecraft.util.Util.backgroundExecutor()));
  for(var job:jobs)require(job.get(30,TimeUnit.SECONDS),"Concurrent snapshot consumer");
  Path dir=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).getParent();Files.writeString(dir.resolve("section-work.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows)+"\n");
  Files.writeString(dir.resolve("section-review.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("cases",cases.size(),"maximumStateCalls",maximum,"exactBlocksFluidsBiomesAndBytes",true,"mutationIsolation",true,"exceptionPropagationAndRecovery",true,"concurrentConsumers",4,"executor","existing native background executor"))+"\n");
  require(maximum<=4097,"Production generator repeats state work: max calls="+maximum+", expected <=4097");
  blockbranch.Blockbranch.LOGGER.info("SECTION_GENERATION_WORK_PASS cases={} maximumCalls={} exactNativeEncoding=true mutationExceptionConcurrency=true",cases.size(),maximum);
 }
 private static volatile Object sink;
 private static void fixedCost(List<Case> cases,Path dir)throws Exception{
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long id=Thread.currentThread().threadId();require(bean.isThreadAllocatedMemoryEnabled()&&bean.isCurrentThreadCpuTimeSupported(),"Native CPU/allocation counters");
  var jit=java.lang.management.ManagementFactory.getCompilationMXBean();var gc=java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();var rows=new ArrayList<Map<String,Object>>();
  // Each case/repeat is paired immediately; reverse arm order and rotate case
  // order. Raw/raw controls measure harness/runtime variation on identical code.
  for(boolean control:new boolean[]{true,false})for(int round=-3;round<6;round++){
   long[] cpu=new long[2],bytes=new long[2],wall=new long[2],blocks=new long[2];long compilation=jit.getTotalCompilationTime(),collections=0,gcMs=0;
   for(var collector:gc){collections+=collector.getCollectionCount();gcMs+=collector.getCollectionTime();}
   for(int repeat=0;repeat<8;repeat++)for(int at=0;at<cases.size();at++){
    var c=cases.get(Math.floorMod(at+round,cases.size()));
    for(int order=0;order<2;order++){
     int arm=(repeat+at+round&1)==0?order:1-order;long c0=bean.getCurrentThreadCpuTime(),b0=bean.getThreadAllocatedBytes(id),w0=System.nanoTime();
     sink=(control||arm==1?c.profile.generateStates(c.section):c.profile.generate(c.section).getStates()).copy();
     wall[arm]+=System.nanoTime()-w0;bytes[arm]+=bean.getThreadAllocatedBytes(id)-b0;cpu[arm]+=bean.getCurrentThreadCpuTime()-c0;blocks[arm]+=4096;
    }
   }
   long gcCount=0,gcTime=0;for(var collector:gc){gcCount+=collector.getCollectionCount();gcTime+=collector.getCollectionTime();}
   if(round>=0)for(int arm=0;arm<2;arm++){
    var row=new LinkedHashMap<String,Object>();row.put("round",round);row.put("identicalRawControl",control);row.put("arm",arm);row.put("rawStates",control||arm==1);row.put("blocks",blocks[arm]);row.put("cpuNs",cpu[arm]);row.put("bytes",bytes[arm]);row.put("wallNs",wall[arm]);row.put("pairCompilationMs",jit.getTotalCompilationTime()-compilation);row.put("pairGcCount",gcCount-collections);row.put("pairGcMs",gcTime-gcMs);rows.add(row);
   }
  }
  sink=null;Files.writeString(dir.resolve("raw-snapshot-interleaved-cost.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows)+"\n");
 }
 // Preserved original block composition; biome selection is unchanged by the fix.
 private static LevelChunkSection reference(TerrainProfile p,int sy){
  int bottom=sy<<4;var first=p.state(0,bottom,0);boolean uniform=true;
  outer:for(int y=bottom;y<bottom+16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)if(p.state(x,y,z)!=first){uniform=false;break outer;}
  var strategy=Strategy.createForBlockStates(net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY);var blocks=new PalettedContainer<BlockState>(first,strategy);
  if(!uniform)for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)blocks.set(x,y,z,p.state(x,bottom+y,z));
  return new LevelChunkSection(blocks,p.view(sy).getBiomes());
 }
 private static byte[] encoded(LevelChunkSection s){var b=new FriendlyByteBuf(Unpooled.buffer());try{s.write(b);var data=new byte[b.readableBytes()];b.getBytes(0,data);return data;}finally{b.release();}}
 private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
}
