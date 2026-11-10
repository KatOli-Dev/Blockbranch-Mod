package blockbranch.realistic;
import java.util.*;import net.minecraft.core.BlockPos;
/** Explain the fixed twelve-region proposal stream without expanding it. */
final class DeepColdRuinDiagnosis {
 static void run(DeepColdOceanRuins planner,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState random)throws Exception{
  var proposal=DeepColdOceanRuins.class.getDeclaredMethod("proposal",int.class,int.class);proposal.setAccessible(true);
  var selected=DeepColdOceanRuins.Region.class.getDeclaredField("selected");selected.setAccessible(true);
  var d=g.hutPlanningTerrain(random).drainage();var surface=((ScreeBiomeSource)g.getBiomeSource()).surfaceResolver();var reports=new ArrayList<Object>();
  for(var region:planner.ordered(BlockPos.ZERO,6000000).stream().limit(12).toList()){
   int depthPass=0,waterPass=0,biomePass=0,minY=Integer.MAX_VALUE,maxY=Integer.MIN_VALUE;var biomes=new TreeMap<String,Integer>();var samples=new ArrayList<Object>();
   for(int cell:(int[])selected.get(region))for(int attempt=0;attempt<64;attempt++){
    long q=(long)proposal.invoke(planner,cell,attempt);int x=(int)(q>>32),z=(int)q;var c=d.sample(x,z);minY=Math.min(minY,c.height());maxY=Math.max(maxY,c.height());if(63-c.height()<=200||c.height()<TerrainShape.FLOOR+16)continue;depthPass++;
    if(!d.oceanWater(x,z,c))continue;waterPass++;var biome=surface.getNoiseBiome(Math.floorDiv(x,4),0,Math.floorDiv(z,4));biomes.merge(biome.unwrapKey().orElseThrow().identifier().toString(),1,Integer::sum);
    if(!biome.is(net.minecraft.world.level.biome.Biomes.DEEP_OCEAN)&&!biome.is(net.minecraft.world.level.biome.Biomes.DEEP_COLD_OCEAN)&&!biome.is(net.minecraft.world.level.biome.Biomes.DEEP_FROZEN_OCEAN))continue;biomePass++;
    if(samples.size()>=8)continue;int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;boolean ocean=true,deep=true;for(int dz=-48;dz<=48;dz+=8)for(int dx=-48;dx<=48;dx+=8){var v=d.sample(x+dx,z+dz);low=Math.min(low,v.height());high=Math.max(high,v.height());ocean&=d.oceanWater(x+dx,z+dz,v);deep&=v.height()<=-137;}
    samples.add(Map.of("x",x,"y",c.height(),"z",z,"groupRelief",high-low,"allOcean",ocean,"allDeep",deep));
   }
   reports.add(Map.of("owner",region.owner(),"depthPass",depthPass,"waterPass",waterPass,"biomePass",biomePass,"minY",minY,"maxY",maxY,"biomes",biomes,"firstEligibleEnvelopeSamples",samples));
  }
  java.nio.file.Files.writeString(java.nio.file.Path.of("ruin-diagnosis.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(reports));
 }
}
