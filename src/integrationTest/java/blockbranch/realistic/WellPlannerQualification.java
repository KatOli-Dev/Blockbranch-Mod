package blockbranch.realistic;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
public final class WellPlannerQualification {
 static Object run(ServerLevel level){
  long seed=level.getSeed();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var d=g.drainage(level.getChunkSource().randomState());var c=new ClimateField(seed);var plans=new ArrayList<Object>();var owners=new HashSet<BiomeRegionPlacement.Cell>();
  var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long thread=Thread.currentThread().threadId();
  outer:for(int z=-1900000;z<=1900000;z+=16000)for(int x=-1900000;x<=1900000;x+=16000){
   if(!SandyDesert.contains(seed,x,z,d,c))continue;
   var cell=new BiomeRegionPlacement.Cell(Math.floorDiv(x,2048),Math.floorDiv(z,2048));var warm=new DesertWell(seed,d,c);var region=warm.region(cell);if(region.owner()==null||!owners.add(region.owner()))continue;
   var owner=region.owner();long terrainStart=System.nanoTime();var coldTerrain=new DrainageNetwork(new TerrainShape(seed),seed,true,true);long terrainConstructionNanos=System.nanoTime()-terrainStart;long bytes=bean.getThreadAllocatedBytes(thread),start=System.nanoTime();var cold=new DesertWell(seed,coldTerrain,new ClimateField(seed),2);var a=cold.plan(owner);long nanos=System.nanoTime()-start;bytes=bean.getThreadAllocatedBytes(thread)-bytes;
   start=System.nanoTime();var b=cold.plan(owner);long warmNanos=System.nanoTime()-start;WellInteraction.require(a.equals(b),"Warm cache same plan");
   // Evict all 64 plan entries with realistic neighboring cells, then query again.
   for(int i=0;i<70;i++)cold.plan(new BiomeRegionPlacement.Cell(owner.x()+i+10,owner.z()+10));
   WellInteraction.require(a.equals(cold.plan(owner)),"Plan survives actual cache eviction");
   var reverse=new DesertWell(seed,d,c,2);reverse.region(cell);WellInteraction.require(a.equals(reverse.plan(owner)),"Different entry cell / exploration order same plan");

   int checked=0;if(a.isPresent()&&!plans.isEmpty())for(var block:DesertWell.blocks(a.get())){WellInteraction.require(level.getBlockState(block.pos())==block.state(),"Representative native terrain/seam at "+block.pos()+" expected="+block.state()+" actual="+level.getBlockState(block.pos()));if(block.state().is(net.minecraft.world.level.block.Blocks.SUSPICIOUS_SAND)){
    var entity=level.getBlockEntity(block.pos());WellInteraction.require(entity!=null&&entity.saveWithoutMetadata(level.registryAccess()).toString().contains("archaeology/desert_well"),"Native high archaeology metadata "+block.pos());}checked++;}
   plans.add(Map.of("terrainConstructionNanos",terrainConstructionNanos,"nativeBlocks",checked,"owner",owner.toString(),"cells",region.cells(),"accepted",a.isPresent(),"plan",a.toString(),"coldNanos",nanos,"warmNanos",warmNanos,"allocatedBytes",bytes));
   if(plans.size()==6)break outer;
  }
  WellInteraction.require(plans.size()==6,"Six independent natural regions");return plans;
 }
}
