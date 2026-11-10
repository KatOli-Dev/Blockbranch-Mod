package blockbranch.realistic;

import blockbranch.Blockbranch;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;

/** Native registry check of column reuse, coordinate bounds, and immutable resolver ownership. */
public final class BiomeColumnCacheSmoke {
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Path result = Path.of(System.getProperty("blockbranch.biomeColumnCacheResult"));
            try {
                var generator = (RealisticGenerator) server.overworld().getChunkSource().getGenerator();
                var source = (ScreeBiomeSource) generator.getBiomeSource();
                var raw = source.createResolver(null);
                int comparisons = 0;
                for (int[] pos : new int[][] {{-21066,156071},{-19018,158119},{-4,-4},{0,0},{31999,-32768},{-500000,500000}}) {
                    var cached = source.createResolverForChunk(null,pos[0],-16,pos[1],4,96,4);
                    for (int z=0;z<4;z++) for (int x=0;x<4;x++) for (int y : new int[] {-65536,-3072,-16,0,79,2303,65535}) {
                        require(cached.getNoiseBiome(pos[0]+x,y,pos[1]+z) == raw.getNoiseBiome(pos[0]+x,y,pos[1]+z),"actual seeded biome identity including vertical bounds");
                        comparisons++;
                    }
                    for (int[] outside : new int[][] {{-1,0},{4,0},{0,-1},{0,4}})
                        require(cached.getNoiseBiome(pos[0]+outside[0],77,pos[1]+outside[1]) == raw.getNoiseBiome(pos[0]+outside[0],77,pos[1]+outside[1]),"out-of-window fallback coordinates");
                }
                var biomes = source.possibleBiomes().stream().toList();
                require(biomes.size()>1,"distinct native biome holders");
                var calls = new AtomicInteger();
                BiomeResolver first = (x,y,z) -> {calls.incrementAndGet();return biomes.get(Math.floorMod(x*31+z,biomes.size()));};
                var cached = ScreeBiomeSource.columnResolver(first,-4,-8);
                require(calls.get()==16,"one sample per quart X/Z column");
                for (int y=-16;y<80;y++) for(int z=-8;z<-4;z++) for(int x=-4;x<0;x++)
                    require(cached.getNoiseBiome(x,y,z)==biomes.get(Math.floorMod(x*31+z,biomes.size())),"native fill order and negative coordinates");
                require(calls.get()==16,"1536 native height samples reuse 16 evaluations");
                var other = ScreeBiomeSource.columnResolver((x,y,z)->biomes.get(Math.floorMod(x+z+1,biomes.size())),-4,-8);
                try(var pool=Executors.newFixedThreadPool(4)) {
                    var jobs=new ArrayList<Callable<Boolean>>();
                    for(int t=0;t<8;t++)jobs.add(()->{
                        for(int y=-32;y<96;y++)for(int z=-8;z<-4;z++)for(int x=-4;x<0;x++) {
                            require(cached.getNoiseBiome(x,y,z)==biomes.get(Math.floorMod(x*31+z,biomes.size())),"concurrent original resolver isolation");
                            require(other.getNoiseBiome(x,y,z)==biomes.get(Math.floorMod(x+z+1,biomes.size())),"concurrent separate resolver isolation");
                        }return true;
                    });
                    for(var job:pool.invokeAll(jobs))require(job.get(),"parallel resolver checks");
                }
                require(calls.get()==16,"parallel in-window reads do not re-evaluate");
                require(source.createResolverForChunk(null,0,0,0,8,8,8)==raw,"non-native horizontal bounds retain original resolver");
                Files.writeString(result,"PASS seeded comparisons="+comparisons+" evaluations=16 nativeSamples=1536 concurrentIsolation=true\n");
                Blockbranch.LOGGER.info("BIOME_COLUMN_CACHE PASS comparisons={} nativeEvaluations=16 previousEvaluations=1536",comparisons);
            } catch(Throwable error) {
                Blockbranch.LOGGER.error("BIOME_COLUMN_CACHE FAIL",error);
                try {Files.writeString(result,"FAIL "+error+"\n");}catch(Exception ignored){}
            } finally {server.halt(false);}
        });
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private BiomeColumnCacheSmoke(){}
}
