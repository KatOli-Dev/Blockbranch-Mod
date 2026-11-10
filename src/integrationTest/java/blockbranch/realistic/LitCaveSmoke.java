package blockbranch.realistic;

import blockbranch.cubic.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Native light, exact-output palette fast path, collision and overlay checks. */
public final class LitCaveSmoke {
 public static void verify(ServerLevel level,RealisticGenerator g,boolean reload)throws Exception{
  restoreEditsBeforeGeometry(level,g,reload);CaveApproachSmoke.verify(level,g,reload);plannerControls();require(g.litCaveGrowthDefinition()==2,"New world opts into entrance vegetation");
  var dir=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).getParent();var lines=Files.readAllLines(dir.resolve("approach-evidence.txt"));var values=new HashMap<String,String>();for(var line:lines){int i=line.indexOf('=');if(i>0)values.put(line.substring(0,i),line.substring(i+1));}
  var gson=new Gson();var approach=new CaveApproach(gson.fromJson(values.get("points"),int[].class),new CaveEntryRoute(gson.fromJson(values.get("entry_points"),int[].class)));var random=level.getChunkSource().randomState();var chunks=new LinkedHashSet<ChunkPos>();for(var point:approach.walkingRoute(g.drainage(random),new UndergroundStructure(random.seed())))for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)chunks.add(new ChunkPos((point[0]+dx)>>4,(point[2]+dz)>>4));
  Path marker=dir.resolve("moss-edits.json");var ecology=new GroundEcology.Field(g.drainage(random)::sample,new ClimateField(random.seed()));
  long sharedChecksum=0,sharedBlocks=0;int emptyGrowthColumns=0;int moss=0,paletteSections=0,rejectedSections=0,eligible=0;boolean codecChecked=false;var editSites=new ArrayList<BlockPos>();var review=new ArrayList<String>();
  for(var c:chunks){var chunk=level.getChunk(c.x(),c.z());var p=((SparseColumn)chunk).blockbranch$terrain().base();var bare=p.withLitCaves(LitCaveLayer.NONE);require(p.litCaves().enabled(),"Profile stores opted-in growth, including empty columns");var encoded=p.encode(level.palettedContainerFactory());require(encoded.getIntOr("version",0)==(p.caveBiomes().enabled()?70:g.dripstoneDefinition()==2?68:66),"Combined profile66");var decoded=TerrainProfile.decode(encoded,level.palettedContainerFactory());if(!codecChecked){codecControls(p,level.palettedContainerFactory());codecChecked=true;}var planned=LitCaveLayer.plan(bare,random.seed(),c.x(),c.z(),(wx,wz)->GroundEcology.borealMossSite(ecology.sample(wx,wz)));require(Arrays.equals(planned.placements(),p.litCaves().placements()),"Full profile plan repeats");require(Arrays.equals(planned.placements(),g.independentLitCavePlan(c.x(),c.z(),random).placements()),"Independent generation order and base-column plans agree");
   if(g.caveBiomeDefinition()>0)CaveBiomeSmoke.column(level,g,chunk);
   var shared=sharedDecorationReference(p,bare,level.palettedContainerFactory());sharedChecksum=Long.rotateLeft(sharedChecksum,1)^shared[0];sharedBlocks+=shared[1];if(p.litCaves().placements().length==0)emptyGrowthColumns++;
   int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;for(int z=0;z<16;z++)for(int x=0;x<16;x++){int ground=p.groundHeight(x,z);min=Math.min(min,ground);max=Math.max(max,ground);int floor=bare.top(Heightmap.Types.WORLD_SURFACE.isOpaque(),x,z,bare.ceiling()-1)-1;if(ground-floor>=1&&ground-floor<=12&&bare.underground().approachAt(x,floor+1,z)&&LitCaveLayer.host(bare.state(x,floor,z)))eligible++;
    for(int depth=-4;depth<=24;depth++){int y=ground-depth;var a=p.state(x,y,z);var b=bare.state(x,y,z);require(decoded.state(x,y,z)==a,"Snapshot growth agrees");if(a!=b)require(a.is(Blocks.MOSS_BLOCK)&&LitCaveLayer.host(b),"Only approved surviving floor host changes");if(OreReferenceSmoke.kind(b)!=null)require(a==b,"Near-surface ores retained");}}
   for(int sy=(min-352)>>4;sy<=(max+16)>>4;sy++){var palette=p.view(sy).getBiomes();boolean changed=false;for(int y=0;y<4;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++){var expected=p.biome(x,sy*4+y,z);require(palette.get(x,y,z).equals(expected),"Palette fast path equals unchanged absolute-Y selector");changed|=!expected.equals(p.biomes().get(x,y,z));}if(!p.dripstone().mayHaveBiome(sy,min,max)&&!p.caveBiomes().mayHaveBiome(sy,min,max)){require(!changed&&palette==p.biomes(),"Rejected section reuses immutable surface palette");rejectedSections++;}paletteSections++;}
   var placements=p.litCaves().placements();for(int i=0;i<placements.length;i+=2){int x=placements[i]&15,z=placements[i]>>4,y=placements[i+1];var pos=new BlockPos(c.getMinBlockX()+x,y,c.getMinBlockZ()+z);require(level.getBlockState(pos).is(Blocks.MOSS_BLOCK),"Native moss agrees with descriptor");require(level.getBrightness(LightLayer.SKY,pos.above())==15,"Actual native vertical sky light reaches growth");require(level.getHeight(Heightmap.Types.WORLD_SURFACE,pos.getX(),pos.getZ())==y+1,"No generated roof or canopy over growth");require(p.generate(y>>4).getBlockState(x,y&15,z).is(Blocks.MOSS_BLOCK),"Materialized growth agrees");require(g.getBaseColumn(pos.getX(),pos.getZ(),level,random).getBlock(y).is(Blocks.MOSS_BLOCK),"Native base column includes moss");for(var type:Heightmap.Types.values())require(p.top(type.isOpaque(),x,z,p.ceiling()-1)==bare.top(type.isOpaque(),x,z,bare.ceiling()-1),"All normal height predicates unchanged");require(GroundEcology.borealMossSite(ecology.sample(pos.getX(),pos.getZ())),"Every generated moss floor meets existing moisture suitability");require(p.view(y>>4).maybeHas(s->s.is(Blocks.MOSS_BLOCK)),"Procedural material predicate includes moss");require(p.view(y>>4).maybeHas(s->s.getLightEmission()>0)==bare.view(y>>4).maybeHas(s->s.getLightEmission()>0),"Moss adds no false emitter candidate");moss++;editSites.add(pos);if(review.size()<8)review.add(pos+" surface="+p.biomes().get(x>>2,0,z>>2).unwrapKey()+" wetness="+ecology.sample(pos.getX(),pos.getZ()).wetness()+" sky=15 depth="+(p.groundHeight(x,z)-y));}
   var bad=encoded.copy();bad.remove("lit_caves");boolean rejected=false;try{TerrainProfile.decode(bad,level.palettedContainerFactory());}catch(RuntimeException e){rejected=true;}require(rejected,"Missing growth descriptor rejected");
  }
  require(rejectedSections>0,"Actual fast-path sections exercised");
  if(Boolean.getBoolean("blockbranch.litCaveRequireMoss"))require(editSites.size()>=2,"Positive qualification needs two naturally eligible moss floors; observed="+editSites.size());
  if(editSites.size()>=2){
   var a=editSites.get(0);var b=editSites.get(1);level.setBlock(a,Blocks.AIR.defaultBlockState(),2);level.setBlock(b,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
   for(var pos:List.of(a,b)){var home=level.getChunkAt(pos);var terrain=((SparseColumn)home).blockbranch$terrain();terrain.flush();terrain.evictExcept(Set.of());home.markUnsaved();}
   require(level.getBlockState(a).isAir(),"Mined moss survives eviction without regrowth");require(level.getBlockState(b).is(Blocks.DIAMOND_BLOCK),"Backfill survives eviction without regrowth");
   Files.writeString(marker,gson.toJson(new int[][]{{a.getX(),a.getY(),a.getZ()},{b.getX(),b.getY(),b.getZ()}}));
  }
  if(Boolean.getBoolean("blockbranch.litCaveTiming"))profileTiming(level,g,chunks,dir);
  Files.writeString(dir.resolve(reload?"lit-cave-reload.txt":"lit-cave-fresh.txt"),"PASS seed="+random.seed()+" chunks="+chunks.size()+" eligibleDirectSkyRecessFloors="+eligible+" moss="+moss+" sharedBlockChecksum="+Long.toUnsignedString(sharedChecksum)+" sharedBlocksCompared="+sharedBlocks+" emptyGrowthColumns="+emptyGrowthColumns+" paletteSections="+paletteSections+" reusedSurfacePalettes="+rejectedSections+"\n"+String.join("\n",review));
 }
 private static long[] sharedDecorationReference(TerrainProfile actual,TerrainProfile bare,net.minecraft.world.level.chunk.PalettedContainerFactory factory)throws Exception{
  // Independent pre-prototype composition: original dripstone profile, then approved moss.
  var sections=new TreeSet<Integer>(actual.dripstone().sections());sections.addAll(actual.litCaves().sections());
  sections.add((actual.groundHeight(0,0)>>4)+2);sections.add((actual.groundHeight(0,0)>>4)-24);
  if(sections.size()==2)sections.add(actual.groundHeight(0,0)>>4);
  long expectedHash=0xcbf29ce484222325L,actualHash=expectedHash,count=0;
  var queries=new ArrayList<int[]>();
  for(int sy:sections)for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
   int ay=sy*16+y;var original=bare.state(x,ay,z);var expected=actual.litCaves().state(x,ay,z,original);var observed=actual.state(x,ay,z);
   require(observed==expected,"Shared lookup equals independent original composition at "+x+","+ay+","+z);
   expectedHash=(expectedHash^net.minecraft.world.level.block.Block.getId(expected))*0x100000001b3L;
   actualHash=(actualHash^net.minecraft.world.level.block.Block.getId(observed))*0x100000001b3L;count++;
   if(expected.is(Blocks.MOSS_BLOCK)||(count&255)==0)queries.add(new int[]{x,ay,z,net.minecraft.world.level.block.Block.getId(expected)});
  }
  require(expectedHash==actualHash,"Shared/reference block checksums identical");
  var before=actual.encode(factory);var reopened=TerrainProfile.decode(before,factory);
  require(before.equals(reopened.encode(factory)),"Shared runtime index does not alter saved descriptors");
  var mossOnly=actual.withDripstone(DripstoneLayer.NONE);var mossOnlyBare=mossOnly.withLitCaves(LitCaveLayer.NONE);
  for(int i=0;i<actual.litCaves().placements().length;i+=2){var a=actual.litCaves().placements();int x=a[i]&15,z=a[i]>>4,y=a[i+1];require(mossOnly.state(x,y,z)==actual.litCaves().state(x,y,z,mossOnlyBare.state(x,y,z)),"Shared lookup supports moss with dripstone disabled");}
  var workers=java.util.concurrent.Executors.newFixedThreadPool(4);
  try{var jobs=new ArrayList<java.util.concurrent.Callable<Boolean>>();for(int worker=0;worker<4;worker++)jobs.add(()->{for(int repeat=0;repeat<8;repeat++)for(var q:queries)require(net.minecraft.world.level.block.Block.getId(actual.state(q[0],q[1],q[2]))==q[3],"Immutable shared lookup concurrent reads");return true;});for(var f:workers.invokeAll(jobs))require(f.get(),"Concurrent shared lookup completed");}finally{workers.shutdownNow();}
  return new long[]{actualHash,count};
 }
 private static void restoreEditsBeforeGeometry(ServerLevel level,RealisticGenerator g,boolean reload)throws Exception{
  var dir=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).getParent();var marker=dir.resolve("moss-edits.json");if(!reload||!Files.exists(marker))return;
  int[][] a=new Gson().fromJson(Files.readString(marker),int[][].class);var mined=new BlockPos(a[0][0],a[0][1],a[0][2]);var fill=new BlockPos(a[1][0],a[1][1],a[1][2]);
  require(level.getBlockState(mined).isAir(),"Mined moss persists across full reopen");require(level.getBlockState(fill).is(Blocks.DIAMOND_BLOCK),"Backfill persists across full reopen");
  // Persistence is asserted before resetting these two known edits for pristine route checks.
  for(var pos:List.of(mined,fill)){var base=((SparseColumn)level.getChunkAt(pos)).blockbranch$terrain().base();level.setBlock(pos,base.state(pos.getX(),pos.getY(),pos.getZ()),2);}
  blockbranch.Blockbranch.LOGGER.info("LIT_CAVE_EDIT_RELOAD_PASS mined={} backfill={} resetAfterAssertions=true",mined,fill);
 }
 private static LitCaveLayer fixture(boolean approach,boolean suitable,boolean route,int ground,int floor,int top,net.minecraft.world.level.block.state.BlockState host){
  return LitCaveLayer.plan(new LitCaveLayer.Sampler(){
   public boolean hasApproach(){return approach;}public boolean suitable(int x,int z){return suitable;}public int ground(int x,int z){return ground;}public int top(int x,int z){return top;}
   public boolean approachAt(int x,int y,int z){return route&&y==floor+1;}
   public net.minecraft.world.level.block.state.BlockState state(int x,int y,int z){return y==floor?host:y<floor?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState();}
  },424242,0,0);
 }
 private static void plannerControls(){
  var stone=Blocks.STONE.defaultBlockState();var good=fixture(true,true,true,100,96,97,stone);require(good.placements().length>0&&good.placements().length<=128,"Suitable skylit floor positive and bounded");
  require(Arrays.equals(good.placements(),fixture(true,true,true,100,96,97,stone).placements()),"Planner deterministic");
  require(fixture(true,false,true,100,96,97,stone).placements().length==0,"Dry ecological negative");
  require(fixture(false,true,true,100,96,97,stone).placements().length==0,"No approach negative");
  require(fixture(true,true,false,100,96,97,stone).placements().length==0,"Unconnected recess negative");
  require(fixture(true,true,true,100,96,101,stone).placements().length==0,"Roof/shadow negative");
  require(fixture(true,true,true,100,87,88,stone).placements().length==0,"Deep floor negative");
  for(var host:List.of(Blocks.COAL_ORE,Blocks.DEEPSLATE_IRON_ORE,Blocks.CALCITE,Blocks.DRIPSTONE_BLOCK,Blocks.AIR))require(fixture(true,true,true,100,96,97,host.defaultBlockState()).placements().length==0,"Protected ore/mineral/unsupported floor negative");
  var positions=good.placements();int first=positions[0];positions[0]=(first+1)&255;require(good.placements()[0]==first,"Descriptor accessor isolation");
  var encoded=good.encode();var decoded=LitCaveLayer.decode(encoded);require(Arrays.equals(good.placements(),decoded.placements()),"Layer codec roundtrip");encoded.putInt("definition",1);boolean reject=false;try{LitCaveLayer.decode(encoded);}catch(RuntimeException expected){reject=true;}require(reject,"Unqualified definition1 rejected");
 }
 public static void verifyLegacy(ServerLevel level,RealisticGenerator g,boolean reload)throws Exception{
  require(g.litCaveGrowthDefinition()==0,"Existing saved generator stays opted out");var dir=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).getParent();var marker=dir.resolve("legacy-growth-edit.json");var random=level.getChunkSource().randomState();var origin=g.getOrigin(random);BlockPos edit;
  if(reload){int[] a=new Gson().fromJson(Files.readString(marker),int[].class);edit=new BlockPos(a[0],a[1],a[2]);require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Legacy saved edit remains after full reopen");}
  else{var p=((SparseColumn)level.getChunk(origin.x(),origin.z())).blockbranch$terrain().base();edit=new BlockPos(origin.getMinBlockX(),p.groundHeight(0,0)-20,origin.getMinBlockZ());level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),2);Files.writeString(marker,new Gson().toJson(new int[]{edit.getX(),edit.getY(),edit.getZ()}));}
  for(var c:List.of(origin,new ChunkPos(origin.x()+79,origin.z()-71))){var p=((SparseColumn)level.getChunk(c.x(),c.z())).blockbranch$terrain().base();require(!p.litCaves().enabled()&&p.encode(level.palettedContainerFactory()).getIntOr("version",0)<=64,"Existing and newly explored legacy columns do not gain moss");require(!TerrainProfile.decode(p.encode(level.palettedContainerFactory()),level.palettedContainerFactory()).litCaves().enabled(),"Legacy snapshot remains disabled");}
  require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Legacy generation retains edit");Files.writeString(dir.resolve(reload?"lit-legacy-reload.txt":"lit-legacy-fresh.txt"),"PASS existing generator=0, old/new columns<=64, saved gold block retained\n");
 }
 public static void costOnly(ServerLevel level,RealisticGenerator g)throws Exception{costOnly(level,g,false);}
 public static void adoptionReview(ServerLevel level,RealisticGenerator g)throws Exception{costOnly(level,g,true);}
 private static void costOnly(ServerLevel level,RealisticGenerator g,boolean reviewOnly)throws Exception{
  var dir=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).getParent();
  var lines=Files.readAllLines(dir.resolve("approach-evidence.txt"));var values=new HashMap<String,String>();for(var line:lines){int i=line.indexOf('=');if(i>0)values.put(line.substring(0,i),line.substring(i+1));}
  var gson=new Gson();var approach=new CaveApproach(gson.fromJson(values.get("points"),int[].class),new CaveEntryRoute(gson.fromJson(values.get("entry_points"),int[].class)));var random=level.getChunkSource().randomState();var chunks=new LinkedHashSet<ChunkPos>();for(var point:approach.walkingRoute(g.drainage(random),new UndergroundStructure(random.seed())))for(int dz=-2;dz<=2;dz++)for(int dx=-2;dx<=2;dx++)chunks.add(new ChunkPos((point[0]+dx)>>4,(point[2]+dz)>>4));
  Path marker=dir.resolve("moss-edits.json");var ecology=new GroundEcology.Field(g.drainage(random)::sample,new ClimateField(random.seed()));
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long tid=Thread.currentThread().threadId();var chunkList=new ArrayList<>(chunks);var bare=new ArrayList<TerrainProfile>();for(var c:chunks)bare.add(g.profile(level.getChunk(c.x(),c.z()),random).withLitCaves(LitCaveLayer.NONE));
  if(reviewOnly){MossAdoptionReview.verify(level,g,chunkList,bare,dir);return;}
  var rows=new ArrayList<Map<String,Object>>();for(int round=-1;round<3;round++){long cpu=bean.getCurrentThreadCpuTime(),bytes=bean.getThreadAllocatedBytes(tid),wall=System.nanoTime();int placements=0;for(int repeat=0;repeat<8;repeat++)for(int index=0;index<bare.size();index++){var p=bare.get(index);var c=chunkList.get(index);var result=LitCaveLayer.plan(p,random.seed(),c.x(),c.z(),(x,z)->GroundEcology.borealMossSite(ecology.sample(x,z)));placements+=result.placements().length/2;}if(round>=0)rows.add(Map.of("round",round,"profiles",bare.size()*8,"placements",placements,"cpuNs",bean.getCurrentThreadCpuTime()-cpu,"bytes",bean.getThreadAllocatedBytes(tid)-bytes,"wallNs",System.nanoTime()-wall));}Files.writeString(dir.resolve("lit-planner-cost.json"),new GsonBuilder().setPrettyPrinting().create().toJson(rows));
  if(Boolean.getBoolean("blockbranch.litCaveTiming"))profileTiming(level,g,chunks,dir);
 }
 private static void profileTiming(ServerLevel level,RealisticGenerator original,Set<ChunkPos> chunks,Path dir)throws Exception{
  var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
  var config=RealisticGenerator.CODEC.codec().encodeStart(ops,original).getOrThrow().getAsJsonObject();var off=config.deepCopy();off.remove("lit_cave_growth_definition");
  var rows=new ArrayList<Map<String,Object>>();var random=level.getChunkSource().randomState();
  var cpu=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();require(cpu.isCurrentThreadCpuTimeSupported()&&cpu.isThreadAllocatedMemoryEnabled(),"CPU/allocation counters enabled");long tid=Thread.currentThread().threadId();
  if(!Boolean.getBoolean("blockbranch.litCaveSectionsOnly"))for(int round=-1;round<3;round++){
   var generators=new RealisticGenerator[]{RealisticGenerator.CODEC.codec().parse(ops,off).getOrThrow(),RealisticGenerator.CODEC.codec().parse(ops,config).getOrThrow()};
   for(int sequence=0;sequence<2;sequence++){int enabled=(round&1)==0?sequence:1-sequence;var generator=generators[enabled];long[] ns=new long[2],cs=new long[2],bs=new long[2];
    for(int pass=0;pass<2;pass++){long start=System.nanoTime(),c0=cpu.getCurrentThreadCpuTime(),b0=cpu.getThreadAllocatedBytes(tid);for(var c:chunks)generator.profile(level.getChunk(c.x(),c.z()),random);ns[pass]=System.nanoTime()-start;cs[pass]=cpu.getCurrentThreadCpuTime()-c0;bs[pass]=cpu.getThreadAllocatedBytes(tid)-b0;}
    if(round>=0)rows.add(Map.of("round",round,"enabled",enabled==1,"coldAllProfilePlanningNs",ns[0],"warmAllProfilePlanningNs",ns[1],"chunks",chunks.size(),"coldCpuNs",cs[0],"warmCpuNs",cs[1],"coldBytes",bs[0],"warmBytes",bs[1]));
   }
   for(var c:chunks){var a=generators[0].profile(level.getChunk(c.x(),c.z()),random);var b=generators[1].profile(level.getChunk(c.x(),c.z()),random);for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=0;depth<=14;depth++){int y=a.groundHeight(x,z)-depth;var av=a.state(x,y,z);var bv=b.state(x,y,z);require(av==bv||bv.is(Blocks.MOSS_BLOCK)&&LitCaveLayer.host(av),"Matched generated profile only differs at permitted moss floors");}}
  }
  var queryRows=new ArrayList<Map<String,Object>>();
  var profiles=new ArrayList<TerrainProfile[]>();var sectionLists=new ArrayList<Set<Integer>>();
  for(var c:chunks){var on=original.profile(level.getChunk(c.x(),c.z()),random);profiles.add(new TerrainProfile[]{on.withLitCaves(LitCaveLayer.NONE),on});var sections=new TreeSet<Integer>(on.dripstone().sections());sections.addAll(on.litCaves().sections());sections.add(on.groundHeight(0,0)>>4);sections.add((on.groundHeight(0,0)>>4)-24);sectionLists.add(sections);}
  for(int round=-1;round<3;round++)for(int order=0;order<2;order++){
   int enabled=(round&1)==0?order:1-order;long start=System.nanoTime(),c0=cpu.getCurrentThreadCpuTime(),b0=cpu.getThreadAllocatedBytes(tid),count=0,sum=0;
   for(int repeat=0;repeat<16;repeat++)for(int n=0;n<profiles.size();n++)for(int sy:sectionLists.get(n)){var section=profiles.get(n)[enabled].generate(sy);count+=4096;sum+=net.minecraft.world.level.block.Block.getId(section.getBlockState(0,0,0));}
   if(round>=0)queryRows.add(Map.of("round",round,"enabled",enabled==1,"blocks",count,"cpuNs",cpu.getCurrentThreadCpuTime()-c0,"bytes",cpu.getThreadAllocatedBytes(tid)-b0,"wallNs",System.nanoTime()-start,"checksum",sum));
  }
  Files.writeString(dir.resolve("lit-section-cost.json"),new GsonBuilder().setPrettyPrinting().create().toJson(queryRows));
  if(!rows.isEmpty())Files.writeString(dir.resolve("lit-profile-cost.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("scope","All profile generation and planning, warm native chunk inputs; not end-to-end loading or cold JVM", "warmupPairs",1,"alternatingOrder",true,"samples",rows))+"\n");
 }
 private static void codecControls(TerrainProfile p,net.minecraft.world.level.chunk.PalettedContainerFactory factory){
  var legacy=p.withDripstone(DripstoneLayer.NONE).withLitCaves(LitCaveLayer.NONE).encode(factory);require(legacy.getIntOr("version",0)<=64,"Previous qualified profile remains64");
  require(!TerrainProfile.decode(legacy,factory).litCaves().enabled(),"Legacy profile stays growth-free");
  var tag=p.encode(factory);tag.putInt("version",65);rejectProfile(tag,factory,"Unqualified profile65 rejected");
  tag=legacy.copy();tag.put("lit_caves",p.litCaves().encode());rejectProfile(tag,factory,"Descriptor cannot opt in legacy snapshot");
  tag=p.encode(factory);tag.getCompoundOrEmpty("lit_caves").putLong("seed",p.underground().seed()+1);rejectProfile(tag,factory,"Growth seed mismatch rejected");
  tag=p.encode(factory);tag.getCompoundOrEmpty("lit_caves").putInt("column_x",Integer.MAX_VALUE);rejectProfile(tag,factory,"Growth owner bounds rejected");
  tag=p.encode(factory);tag.putInt("dripstone_definition",0);rejectProfile(tag,factory,"Layer definition/tag mismatch rejected");
 }
 private static void rejectProfile(net.minecraft.nbt.CompoundTag tag,net.minecraft.world.level.chunk.PalettedContainerFactory factory,String reason){boolean rejected=false;try{TerrainProfile.decode(tag,factory);}catch(RuntimeException expected){rejected=true;}require(rejected,reason);}
 private static void require(boolean v,String m){if(!v)throw new AssertionError(m);}
}
