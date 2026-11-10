package blockbranch.realistic;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
public final class AtlasQualification {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  topologyReference();
  var typed=StructureRegionAtlas.labeled(7,(x,z)->x<0?1:2);
  var left=typed.region(new BiomeRegionPlacement.Cell(-1,0));var right=typed.region(new BiomeRegionPlacement.Cell(0,0));
  WellInteraction.require(!left.owner().equals(right.owner())&&left.cells()==2097152&&right.cells()==2097152,"Adjacent biome labels remain distinct complete world-scale components");
  var large=new StructureRegionAtlas(7,64,-32,(x,z)->true);var a=large.region(new BiomeRegionPlacement.Cell(-32,-32));var b=large.region(new BiomeRegionPlacement.Cell(31,31));
  WellInteraction.require(a.equals(b)&&a.cells()==4096&&!a.overflow(),"Oversized connected region remains one complete budget");
  var wholeWorld=new StructureRegionAtlas(7,(x,z)->true);var whole=wholeWorld.region(new BiomeRegionPlacement.Cell(-1024,-1024));WellInteraction.require(whole.cells()==4194304&&whole.equals(wholeWorld.region(new BiomeRegionPlacement.Cell(1023,1023))),"Entire finite world is one region, no oversized omission");
  var split=new StructureRegionAtlas(7,64,-32,(x,z)->x<0||x>2048);WellInteraction.require(!split.region(new BiomeRegionPlacement.Cell(-1,0)).owner().equals(split.region(new BiomeRegionPlacement.Cell(1,0)).owner()),"Disconnected regions remain distinct");
  var level=server.overworld();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var random=level.getChunkSource().randomState();var d=g.drainage(random);var c=new ClimateField(random.seed());
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long thread=Thread.currentThread().threadId(),bytes=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();
  var atlas=new StructureRegionAtlas(random.seed(),(x,z)->SandyDesert.province(random.seed(),x,z)>=.48&&c.structureDesert(x,z,d));
  long elapsed=System.nanoTime()-start;bytes=bean.getThreadAllocatedBytes(thread)-bytes;
  var regions=new ArrayList<Object>();for(int[] p:new int[][]{{197,-593},{222,-575},{207,-538},{187,-363},{208,-351},{201,-340}})regions.add(atlas.region(new BiomeRegionPlacement.Cell(p[0],p[1])).toString());
  for(int z=-2000000;z<2000000;z+=32000)for(int x=-2000000;x<2000000;x+=32000)WellInteraction.require(c.structureDesert(x,z,d)==(d.structureClimateSample(x,z).waterLevel()==DrainageNetwork.DRY&&SandyDesert.climateSuitable(c.sample(x,z,d::structureClimateSample))),"Primitive atlas climate agrees with reference");
  var fits=new ArrayList<Object>();long initializationStart=System.nanoTime(),initializationBytes=bean.getThreadAllocatedBytes(thread);var wells=new DesertWell(random.seed(),d,c,3);long initializationNanos=System.nanoTime()-initializationStart;initializationBytes=bean.getThreadAllocatedBytes(thread)-initializationBytes;
  int[] accepted={0};wells.atlas().forEachSubstantialOwner(owner->{if(wells.plan(owner).isPresent())accepted[0]++;});
  var concurrent=java.util.stream.IntStream.range(0,4096).parallel().mapToObj(i->wells.region(new BiomeRegionPlacement.Cell((i%64)-32,(i/64)-32))).toList();
  for(int i=0;i<4096;i++)WellInteraction.require(concurrent.get(i).equals(wells.region(new BiomeRegionPlacement.Cell((i%64)-32,(i/64)-32))),"Concurrent immutable lookup");
  var seen=new HashSet<BiomeRegionPlacement.Cell>();
  for(int[] point:new int[][]{{197,-593},{222,-575},{207,-538},{187,-363},{208,-351},{201,-340}}){
   var owner=wells.region(new BiomeRegionPlacement.Cell(point[0],point[1])).owner();if(owner==null||!seen.add(owner))continue;
   long fitBytes=bean.getThreadAllocatedBytes(thread),fitStart=System.nanoTime();var plan=wells.plan(owner);long fitNanos=System.nanoTime()-fitStart;fitBytes=bean.getThreadAllocatedBytes(thread)-fitBytes;
   var expected=plan;for(int i=0;i<70;i++)wells.plan(new BiomeRegionPlacement.Cell(owner.x()+i+1,owner.z()+8));WellInteraction.require(expected.equals(wells.plan(owner)),"Atlas plan independent of eviction");
   fits.add(Map.of("owner",owner.toString(),"plan",plan.toString(),"fitNanos",fitNanos,"fitBytes",fitBytes));
  }
  var selected=new HashSet<DesertWell.Plan>();wells.atlas().forEachSubstantialOwner(owner->{wells.plan(owner).ifPresent(p->{
   WellInteraction.require(selected.add(p),"Distinct components never share a plan");
   var physical=new BiomeRegionPlacement.Cell(Math.floorDiv(p.x(),2048),Math.floorDiv(p.z(),2048));
   WellInteraction.require(owner.equals(wells.region(physical).owner()),"Fallback remains within its own complete region");
   WellInteraction.require(wells.forChunk(new net.minecraft.world.level.ChunkPos(p.x()>>4,p.z()>>4)).orElseThrow().equals(p),"Fallback reachable through native chunk path");
  });});
  long queryBytes=bean.getThreadAllocatedBytes(thread),queryStart=System.nanoTime();int hits=0;
  for(int i=0;i<1000000;i++){long h=OakBirchWoodland.mix(i);if(wells.forChunk(new net.minecraft.world.level.ChunkPos((int)Math.floorMod(h,262144)-131072,(int)Math.floorMod(h>>>24,262144)-131072)).isPresent())hits++;}
  long queryNanos=System.nanoTime()-queryStart;queryBytes=bean.getThreadAllocatedBytes(thread)-queryBytes;
  var rebuilt=new DesertWell(random.seed(),d,c,3);wells.atlas().forEachSubstantialOwner(owner->WellInteraction.require(wells.plan(owner).equals(rebuilt.plan(owner)),"Fresh full rebuild identical independent of terrain cache history"));
  var saved=StructurePlacementCache.capture(random.seed(),true,wells).orElseThrow();
  var encoded=StructurePlacementCache.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,saved).getOrThrow();
  var decoded=StructurePlacementCache.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow();
  var restored=decoded.restore(random.seed(),true);WellInteraction.require(restored!=null,"Saved placement cache restores");
  var cachedWells=new DesertWell(random.seed(),d,c,3,restored);WellInteraction.require(cachedWells.restoredFromSave()&&digest(wells).equals(digest(cachedWells)),"Saved cache preserves every placement");
  WellInteraction.require(decoded.restore(random.seed()+1,true)==null&&decoded.restore(random.seed(),false)==null,"Seed and terrain option mismatch invalidate only cache");
  var broken=encoded.deepCopy().getAsJsonObject();broken.addProperty("checksum","bad");
  WellInteraction.require(StructurePlacementCache.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,broken).getOrThrow().restore(random.seed(),true)==null,"Corruption invalidates only cache");
  var chosen=List.copyOf(selected);var parallelPlans=java.util.stream.IntStream.range(0,4096).parallel().mapToObj(i->{var p=chosen.get(i%chosen.size());return wells.forChunk(new net.minecraft.world.level.ChunkPos(p.x()>>4,p.z()>>4)).orElseThrow();}).toList();
  for(int i=0;i<parallelPlans.size();i++)WellInteraction.require(parallelPlans.get(i).equals(chosen.get(i%chosen.size())),"Concurrent occupied chunks produce identical plans");
  var otherSeeds=new ArrayList<Object>();for(long seed:new long[]{1,-17}){
   var nd=new DrainageNetwork(new TerrainShape(seed),seed,true,true);var nc=new ClimateField(seed);long t=System.nanoTime(),ab=bean.getThreadAllocatedBytes(thread);
   var nw=new DesertWell(seed,nd,nc,3);long ns=System.nanoTime()-t;ab=bean.getThreadAllocatedBytes(thread)-ab;int[] count={0};nw.atlas().forEachSubstantialOwner(owner->{if(nw.plan(owner).isPresent())count[0]++;});
   WellInteraction.require(digest(nw).equals(seed==1?"9922591c1ea2663b99ad5c161f025d833b6eff4c1cf02c280a421b6ce37be21a":"71b4e1da5599309cc02051a4a315c9077a97c14878fd28e19d445c0522a79a6a"),"Every alternate-seed placement matches original dense definition 3");
   otherSeeds.add(Map.of("placementDigest",digest(nw),"seed",seed,"statistics",nw.atlas().statistics(),"initializationNanos",ns,"allocatedBytes",ab,"accepted",count[0],"retainedBytes",nw.retainedPlanBytes()+nw.atlas().retainedArrayBytes()));
  }
  var report=new java.util.LinkedHashMap<String,Object>();WellInteraction.require(digest(wells).equals("6c00a690c10d46c435c824459e7c325510a09a7b186ace140bf372595f1dff5a"),"Every primary-seed placement matches original dense definition 3");report.put("placementDigest",digest(wells));report.put("millionChunkQueries",Map.of("nanos",queryNanos,"allocatedBytes",queryBytes,"hits",hits));report.put("initialization",Map.of("nanos",initializationNanos,"allocatedBytes",initializationBytes,"retainedBytes",wells.retainedPlanBytes()+wells.atlas().retainedArrayBytes(),"accepted",accepted[0],"wholeWorldRegionCells",whole.cells()));report.putAll(Map.of("fits",fits,"otherSeeds",otherSeeds,"buildNanos",elapsed,"allocatedBytes",bytes,"retainedBytes",atlas.retainedArrayBytes(),"statistics",atlas.statistics(),"witnessRegions",regions,"thread",Thread.currentThread().getName()));
  Files.writeString(Path.of("atlas.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));Files.writeString(Path.of("atlas-result.txt"),"PASS");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("ATLAS_FAIL",t);try{Files.writeString(Path.of("atlas-result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
 private static String digest(DesertWell wells){
  try{var md=java.security.MessageDigest.getInstance("SHA-256");wells.atlas().forEachSubstantialOwner(owner->md.update((owner+"/"+wells.region(owner).cells()+"/"+wells.plan(owner)+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));return java.util.HexFormat.of().formatHex(md.digest());}catch(Exception e){throw new RuntimeException(e);}
 }
 private static void topologyReference(){
  int n=64;var random=new Random(811);boolean[] mask=new boolean[n*n];for(int i=0;i<mask.length;i++)mask[i]=random.nextDouble()<.62;
  var atlas=new StructureRegionAtlas(7,n,-32,(x,z)->mask[(Math.floorDiv(z,2048)+32)*n+Math.floorDiv(x,2048)+32]);
  var seen=new BitSet(mask.length);
  for(int start=0;start<mask.length;start++)if(mask[start]&&!seen.get(start)){
   var queue=new ArrayList<Integer>();queue.add(start);seen.set(start);
   for(int at=0;at<queue.size();at++){int i=queue.get(at),x=i%n,z=i/n;
    for(int j:new int[]{x>0?i-1:-1,x<n-1?i+1:-1,z>0?i-n:-1,z<n-1?i+n:-1})if(j>=0&&mask[j]&&!seen.get(j)){seen.set(j);queue.add(j);}}
   var expected=atlas.region(new BiomeRegionPlacement.Cell(start%n-32,start/n-32));
   WellInteraction.require(expected.cells()==queue.size(),"Atlas size agrees with independent flood reference");
   for(int i:queue)WellInteraction.require(expected.equals(atlas.region(new BiomeRegionPlacement.Cell(i%n-32,i/n-32))),"Atlas full connectivity agrees with independent flood reference");
  }
 }
}
