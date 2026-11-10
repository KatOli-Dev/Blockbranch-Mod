package blockbranch.realistic;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;

final class MapPreviewScopeQualification {
 static void check(net.minecraft.server.level.ServerLevel level) {
  var registry=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
  var beach=registry.getOrThrow(Biomes.BEACH);var ocean=registry.getOrThrow(Biomes.OCEAN);
  var calls=new AtomicInteger();BiomeResolver a=(x,y,z)->{calls.incrementAndGet();return beach;};BiomeResolver b=(x,y,z)->ocean;
  var outer=MapPreviewSamples.open();
  try(outer){
   WellInteraction.require(MapPreviewSamples.surface(-1,2,-1,a)==beach&&MapPreviewSamples.surface(-1,200,-1,a)==beach&&calls.get()==1,"Surface height independence is explicit");
   WellInteraction.require(MapPreviewSamples.surface(-1,2,-1,b)==ocean,"Immutable resolver binding separates owners and seeds");
   try(var inner=MapPreviewSamples.open()){MapPreviewSamples.surface(-1,2,-1,a);WellInteraction.require(calls.get()==2,"Nested request isolation");}
   MapPreviewSamples.surface(-1,2,-1,a);WellInteraction.require(calls.get()==2,"Nested close restores caller scope");
   for(int x=0;x<8200;x++){MapPreviewSamples.surface(x,2,0,a);}
   WellInteraction.require(outer.retainedSamples()==8192,"Fixed 8192-entry bound");
  }
  WellInteraction.require(outer.retainedSamples()==0,"Request releases retained values");
  int before=calls.get();MapPreviewSamples.surface(-1,2,-1,a);MapPreviewSamples.surface(-1,2,-1,a);WellInteraction.require(calls.get()==before+2,"No cache outside request");
  try(var ignored=MapPreviewSamples.open()){throw new IllegalStateException("fixture");}catch(IllegalStateException expected){}
  before=calls.get();MapPreviewSamples.surface(0,1,0,a);MapPreviewSamples.surface(0,1,0,a);WellInteraction.require(calls.get()==before+2,"Exceptional exit removes scope");
  var source=(ScreeBiomeSource)level.getChunkSource().getGenerator().getBiomeSource();var resolver=source.createResolver(null);
  var expected=new ArrayList<Holder<Biome>>();for(int i=0;i<64;i++)expected.add(resolver.getNoiseBiome((-504768>>2)+(i&7),15+(i&1),(-1657792>>2)+(i>>3)));
  var futures=new ArrayList<java.util.concurrent.CompletableFuture<Boolean>>();
  for(int worker=0;worker<2;worker++)futures.add(java.util.concurrent.CompletableFuture.supplyAsync(()->{try(var scope=MapPreviewSamples.open()){
   for(int i=63;i>=0;i--)if(!resolver.getNoiseBiome((-504768>>2)+(i&7),15+(i&1),(-1657792>>2)+(i>>3)).equals(expected.get(i)))return false;return true;
  }}));
  futures.forEach(future->WellInteraction.require(future.join(),"Concurrent exact resolver results and reverse order"));
 }
}
