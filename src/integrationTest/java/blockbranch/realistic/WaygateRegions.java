package blockbranch.realistic;
import java.util.*;
import java.nio.file.*;
final class WaygateRegions {
 static void run(RealisticGenerator generator,DesertWaygates current,net.minecraft.world.level.levelgen.RandomState random,net.minecraft.server.level.ServerLevel level)throws Exception{
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long thread=Thread.currentThread().threadId();var reports=new ArrayList<Object>();
  for(long seed:new long[]{424242,1,-17}){
   var d=seed==random.seed()?generator.drainage(random):new DrainageNetwork(new TerrainShape(seed),seed,true,true);var c=new ClimateField(seed);
   var wells=seed==random.seed()?generator.wells(random):new DesertWell(seed,d,c,3);
   var wellBefore=Arrays.stream(wells.planArrays()).map(int[]::clone).toArray(int[][]::new);long start=System.nanoTime(),allocated=bean.getThreadAllocatedBytes(thread);var gates=DesertWaygates.build(seed,true,wells,d,c);long elapsed=System.nanoTime()-start;allocated=bean.getThreadAllocatedBytes(thread)-allocated;
   WellInteraction.require(Arrays.deepEquals(wellBefore,wells.planArrays()),"Waygates preserve all existing well plans");
   if(seed==random.seed())WellInteraction.require(gates.plans().equals(current.plans()),"Fresh cacheless build equals initialized plans");
   var owners=new HashSet<Integer>();int minClearance=Integer.MAX_VALUE;
   for(var p:gates.plans()){
    WellInteraction.require(owners.add(p.owner())&&Math.floorMod(OakBirchWoodland.mix(seed^0x5741594741544531L^p.owner()),4)!=0,"One budget per gated coherent region");
    var region=wells.atlas().region(new BiomeRegionPlacement.Cell(Math.floorDiv(p.x(),2048),Math.floorDiv(p.z(),2048)));WellInteraction.require(region.owner()!=null&&region.owner().x()+1024+(region.owner().z()+1024)*2048==p.owner(),"Chosen point retains complete atlas owner");
    int low=100000,high=-100000;
    for(int dz=-6;dz<=6;dz++)for(int dx=-6;dx<=6;dx++){
     var s=d.sample(p.x()+dx,p.z()+dz);low=Math.min(low,s.height());high=Math.max(high,s.height());WellInteraction.require(s.waterLevel()==DrainageNetwork.DRY&&SandyDesert.contains(seed,p.x()+dx,p.z()+dz,d,c),"Every support column is dry desert");
    }
    WellInteraction.require(high-low<=2&&p.y()==high,"Whole-footprint fit");
    for(int dx=-1;dx<=1;dx++){var s=d.sample(p.x()+DesertWaygates.rotateX(dx,-7,p.rotation()),p.z()+DesertWaygates.rotateZ(dx,-7,p.rotation()));WellInteraction.require(s.waterLevel()==DrainageNetwork.DRY&&s.height()>=high-1&&s.height()<=high,"Approach fit");}
    minClearance=Math.min(minClearance,blockbranch.cubic.ColumnLimits.BORDER_LIMIT-Math.max(Math.abs(p.x())+6,Math.abs(p.z())+6));
    for(int z=(p.z()-6)>>4;z<=(p.z()+6)>>4;z++)for(int x=(p.x()-6)>>4;x<=(p.x()+6)>>4;x++)WellInteraction.require(gates.touches(x,z),"Every affected chunk routes");
    for(var wp:wells.atlas().substantialOwnerIndices())wells.plan(new BiomeRegionPlacement.Cell(wp%2048-1024,wp/2048-1024)).ifPresent(w->WellInteraction.require(Math.hypot(p.x()-(double)w.x(),p.z()-(double)w.z())>=48,"No overlap with existing well"));
   }
   WellInteraction.require(minClearance>0,"Complete footprint inside finite world");
   var encoded=DesertWaygates.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,gates).getOrThrow();var restored=DesertWaygates.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow();WellInteraction.require(restored.valid(seed,true)&&restored.plans().equals(gates.plans()),"Saved cache exactness");
   restored.plans().parallelStream().forEach(p->WellInteraction.require(restored.forStart(p.x()>>4,p.z()>>4).orElseThrow().equals(p),"Concurrent immutable lookup"));
   int gated=0;for(int owner:wells.atlas().substantialOwnerIndices())if(Math.floorMod(OakBirchWoodland.mix(seed^0x5741594741544531L^owner),4)!=0)gated++;
   reports.add(Map.of("seed",seed,"regions",wells.atlas().substantialOwnerIndices().length,"gatedRegions",gated,"selected",gates.plans().size(),"buildNanos",elapsed,"allocatedBytes",allocated,"retainedIntBytes",gates.retainedIntBytes(),"minimumWorldBorderClearance",minClearance,"plans",gates.plans()));
   Files.writeString(Path.of("regions.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(reports));
  }
 }
}
