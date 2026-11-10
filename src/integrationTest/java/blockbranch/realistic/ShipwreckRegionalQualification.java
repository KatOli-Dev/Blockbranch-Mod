package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;

/** Focused whole finite coast catalogs; no animal, biome-render or map benchmark matrix. */
final class ShipwreckRegionalQualification {
 static void run() throws Exception {
  proximityBoundaries();
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
  long thread=Thread.currentThread().threadId();var reports=new ArrayList<Object>();
  for(long seed:new long[]{424242,1,-17}){
   long before=System.nanoTime(),allocated=bean.getThreadAllocatedBytes(thread);
   var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true);var climate=new ClimateField(seed);
   long terrainNanos=System.nanoTime()-before,terrainBytes=bean.getThreadAllocatedBytes(thread)-allocated;
   before=System.nanoTime();allocated=bean.getThreadAllocatedBytes(thread);
   var source=BuriedTreasureDiscovery.build(seed,true,d,climate);source.bind(d,climate);
   long sourceNanos=System.nanoTime()-before,sourceBytes=bean.getThreadAllocatedBytes(thread)-allocated;
   var raw=source.anchorData();var indexedTimes=new ArrayList<Long>();var referenceTimes=new ArrayList<Long>();var indexedBytes=new ArrayList<Long>();var referenceBytes=new ArrayList<Long>();
   ShipwreckDiscovery planner=null;ShipwreckDiscoveryReference reference=null;
   for(int trial=0;trial<3;trial++)for(int phase=0;phase<2;phase++){
    boolean indexed=(phase==0)==((trial&1)==0);before=System.nanoTime();allocated=bean.getThreadAllocatedBytes(thread);
    if(indexed)planner=ShipwreckDiscovery.build(seed,source);else reference=ShipwreckDiscoveryReference.build(seed,source);
    long nanos=System.nanoTime()-before,bytes=bean.getThreadAllocatedBytes(thread)-allocated;
    (indexed?indexedTimes:referenceTimes).add(nanos);(indexed?indexedBytes:referenceBytes).add(bytes);
   }
   var encoded=ShipwreckDiscovery.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,planner).getOrThrow();
   var baseline=ShipwreckDiscoveryReference.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,reference).getOrThrow();
   WellInteraction.require(encoded.equals(baseline),"Every anchor byte matches frozen c8b7518 reference at seed "+seed);
   planner.bind(d,climate,source);long maxNanos=0,totalNanos=0,totalBytes=0,maxBytes=0;
   for(var group:planner.orderedGroups(BlockPos.ZERO,6000000)){
    before=System.nanoTime();allocated=bean.getThreadAllocatedBytes(thread);group.plan();
    long nanos=System.nanoTime()-before,bytes=bean.getThreadAllocatedBytes(thread)-allocated;
    maxNanos=Math.max(maxNanos,nanos);totalNanos+=nanos;maxBytes=Math.max(maxBytes,bytes);totalBytes+=bytes;
   }
   var plans=planner.plans();WellInteraction.require(!plans.isEmpty(),"Representative seed contains beached wrecks");
   int minClearance=Integer.MAX_VALUE,maxRegion=0;var rotations=new TreeSet<Integer>();var variants=new TreeSet<Integer>();
   for(var p:plans){
    rotations.add(p.rotation());variants.add(p.variant());maxRegion=Math.max(maxRegion,p.cells());
    int hx=p.rotation()%2==0?4:15,hz=p.rotation()%2==0?15:4;
    minClearance=Math.min(minClearance,blockbranch.cubic.ColumnLimits.BORDER_LIMIT-Math.max(Math.abs(p.x())+hx,Math.abs(p.z())+hz));
    WellInteraction.require(minClearance>0,"Full rotated footprint is inside finite world border");
    int low=100,high=-100;
    for(int dz=-15;dz<=15;dz++)for(int dx=-4;dx<=4;dx++){
     int x=p.x()+ShipwreckDiscovery.rotateX(dx,dz,p.rotation()),z=p.z()+ShipwreckDiscovery.rotateZ(dx,dz,p.rotation());var s=d.sample(x,z);
     low=Math.min(low,s.height());high=Math.max(high,s.height());
     WellInteraction.require(s.height()>=61&&s.height()<=67&&!s.closed()&&(s.waterLevel()==DrainageNetwork.DRY||s.waterLevel()==64&&d.oceanWater(x,z,s)),"Whole footprint uses dry shore or real ocean, never inland lake");
    }
    WellInteraction.require(high-low<=3&&p.y()==high-2,"Fitted keel uses complete footprint, not center height");
    for(int dz=-4;dz<=-2;dz++){
     var s=d.sample(p.x()+ShipwreckDiscovery.rotateX(5,dz,p.rotation()),p.z()+ShipwreckDiscovery.rotateZ(5,dz,p.rotation()));
     WellInteraction.require(s.waterLevel()==DrainageNetwork.DRY&&s.height()>=high-1&&s.height()<=high,"Boarding approach remains usable");
    }
    WellInteraction.require(planner.forStart(p.x()>>4,p.z()>>4).orElseThrow().equals(p),"Start route resolves exact plan");
    for(int z=(p.z()-hz)>>4;z<=(p.z()+hz)>>4;z++)for(int x=(p.x()-hx)>>4;x<=(p.x()+hx)>>4;x++)WellInteraction.require(planner.touchesChunk(x,z),"Every footprint chunk routed");
   }
   for(int i=0;i<plans.size();i++)for(int j=0;j<i;j++)WellInteraction.require(Math.hypot(plans.get(i).x()-(double)plans.get(j).x(),plans.get(i).z()-(double)plans.get(j).z())>=48,"Selected wrecks keep global separation");
   var restored=ShipwreckDiscovery.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow();restored.bind(d,climate,source);
   var reverse=new ArrayList<>(restored.orderedGroups(BlockPos.ZERO,6000000));Collections.reverse(reverse);reverse.parallelStream().forEach(group->group.plan());
   WellInteraction.require(restored.valid(seed,source)&&restored.plans().equals(plans),"Saved cache, reverse order and concurrency preserve every plan");
   var rebuilt=ShipwreckDiscovery.build(seed,source);rebuilt.bind(d,climate,source);WellInteraction.require(rebuilt.plans().equals(plans),"Discarding derived cache preserves all placements");
   var sourceRegions=new LinkedHashMap<Integer,Integer>();for(int i=0;i<raw.length;i+=4)sourceRegions.put(raw[i+3],raw[i+2]);
   var offsetRegions=new HashSet<Integer>();var cacheData=encoded.getAsJsonObject().getAsJsonObject("anchor_cache").getAsJsonArray("anchors");for(int i=0;i<cacheData.size();i+=8)offsetRegions.add(cacheData.get(i+2).getAsInt());
   int gatedRegions=0,gatedWithTreasure=0,sourceWithTreasure=0,sourceLargeWithTreasure=0,largeGatedWithTreasure=0;
   for(var entry:sourceRegions.entrySet()){
    int id=entry.getKey();boolean hasTreasure=source.regionSite(id).isPresent(),gate=offsetRegions.contains(id)&&Math.floorMod(OakBirchWoodland.mix(seed^0x5348495057524543L^id),6)==0;
    if(hasTreasure){sourceWithTreasure++;if(entry.getValue()>=64)sourceLargeWithTreasure++;}
    if(gate){gatedRegions++;if(hasTreasure){gatedWithTreasure++;if(entry.getValue()>=64)largeGatedWithTreasure++;}}
   }
   var densityStages=Map.of("sourceRegions",sourceRegions.size(),"sourceWithTreasure",sourceWithTreasure,"offsetRegions",offsetRegions.size(),"gatedRegions",gatedRegions,"gatedWithTreasure",gatedWithTreasure,"sourceAtLeast64CellsWithTreasure",sourceLargeWithTreasure,"gatedAtLeast64CellsWithTreasure",largeGatedWithTreasure,"selectedAtLeast64Cells",plans.stream().filter(p->p.cells()>=64).count());
   var report=new LinkedHashMap<String,Object>();report.put("seed",seed);report.put("densityStages",densityStages);report.put("terrainNanos",terrainNanos);report.put("terrainAllocatedBytes",terrainBytes);report.put("sourceNanos",sourceNanos);report.put("sourceAllocatedBytes",sourceBytes);report.put("sourceAnchors",raw.length/4);report.put("groups",planner.groupCount());report.put("anchors",planner.anchorCount());report.put("retainedAnchorIntBytes",planner.retainedDataBytes());report.put("indexedNanos",indexedTimes);report.put("referenceNanos",referenceTimes);report.put("indexedAllocatedBytes",indexedBytes);report.put("referenceAllocatedBytes",referenceBytes);report.put("allPlansNanos",totalNanos);report.put("worstGroupNanos",maxNanos);report.put("allPlansAllocatedBytes",totalBytes);report.put("worstGroupAllocatedBytes",maxBytes);report.put("selected",plans.size());report.put("plans",plans);report.put("rotations",rotations);report.put("variants",variants);report.put("minimumFootprintWorldBorderClearance",minClearance);report.put("largestSelectedRegionCells",maxRegion);reports.add(report);
   Files.writeString(Path.of("regional-qualification.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(reports));
  }
  Files.writeString(Path.of("regional-result.txt"),"PASS all anchors equal c8b7518; three seed catalogs; all selected footprints; cache/order/concurrency identity; exact strict radius boundaries");
 }
 private static void proximityBoundaries(){
  for(int radius:new int[]{48,128}){
   var index=new ShipwreckDiscovery.NearbyPoints(radius);var points=new ArrayList<int[]>();
   for(int x:new int[]{-2097100,-129,-128,-49,-48,-1,0,1,47,48,127,128,2097100})for(int z:new int[]{-129,-48,-1,0,48,128}){index.add(x,z);points.add(new int[]{x,z});}
   for(var p:points)for(int dx:new int[]{-radius-1,-radius,-radius+1,-1,0,1,radius-1,radius,radius+1})for(int dz:new int[]{-radius,0,radius}){
    int x=p[0]+dx,z=p[1]+dz;boolean expected=points.stream().anyMatch(q->Math.hypot(x-(double)q[0],z-(double)q[1])<radius);
    WellInteraction.require(index.containsNear(x,z)==expected,"Spatial index strict threshold and negative/bucket/world-edge coordinates");
   }
  }
 }
}
