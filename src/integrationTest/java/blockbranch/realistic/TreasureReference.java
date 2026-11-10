package blockbranch.realistic;
import java.util.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
public final class TreasureReference {
 public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
  var level=server.overworld();var report=new LinkedHashMap<String,Object>();var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
  var nativeSet=level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).listElements().filter(h->h.value().structures().stream().anyMatch(e->e.structure().is(net.minecraft.world.level.levelgen.structure.BuiltinStructures.BURIED_TREASURE))).findFirst().orElseThrow();
  report.put("nativePlacement",net.minecraft.world.level.levelgen.structure.StructureSet.DIRECT_CODEC.encodeStart(ops,nativeSet.value()).getOrThrow());var surveys=new ArrayList<Object>();
  var normal=WorldPresets.getNormalOverworld(level.registryAccess()).generator();var settings=level.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
  for(long seed:new long[]{424242,1,-17}){
   var random=RandomState.create(level.registryAccess().lookupOrThrow(Registries.NOISE),seed,settings);var resolver=normal.getBiomeSource().createUncachedResolver(random);int n=512,step=32;var mask=new byte[n*n];
   for(int z=0;z<n;z++)for(int x=0;x<n;x++){var b=resolver.getNoiseBiome((x*step-8192)>>2,20,(z*step-8192)>>2);mask[z*n+x]=(byte)(b.is(Biomes.BEACH)?1:b.is(Biomes.SNOWY_BEACH)?2:0);}
   var seen=new BitSet(n*n);var queue=new int[n*n];var sizes=new ArrayList<Integer>();int clipped=0;
   for(int start=0;start<mask.length;start++)if(mask[start]!=0&&!seen.get(start)){
    int count=1;queue[0]=start;seen.set(start);boolean edge=false;
    for(int at=0;at<count;at++){int i=queue[at],x=i%n,z=i/n;edge|=x==0||z==0||x==n-1||z==n-1;
     for(int j:new int[]{x>0?i-1:-1,x<n-1?i+1:-1,z>0?i-n:-1,z<n-1?i+n:-1})if(j>=0&&mask[j]==mask[start]&&!seen.get(j)){seen.set(j);queue[count++]=j;}}
    if(edge)clipped++;else sizes.add(count*step*step);
   }
   surveys.add(Map.of("seed",seed,"step",step,"windowMin",-8192,"windowMaxExclusive",8192,"biomeY",80,"clippedRegionsExcluded",clipped,"closedRegionArea",sizes,"expectedProposals",sizes.stream().map(area->area/25600.0).toList()));
  }
  report.put("surveys",surveys);Files.writeString(Path.of("treasure-reference.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));Files.writeString(Path.of("treasure-reference-result.txt"),"PASS");
 }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("TREASURE_REFERENCE_FAIL",t);}finally{server.halt(false);}});}
}
