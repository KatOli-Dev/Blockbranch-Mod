package blockbranch.realistic;

import blockbranch.cubic.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.attribute.EnvironmentAttributes;

/** Actual native source/profile/palette/spawn checks; no duplicated production selector. */
public final class CaveBiomeSmoke {
    private static long comparisons;
    private static final Set<net.minecraft.world.level.ChunkPos> columns=new LinkedHashSet<>();
    private static volatile Object costSink;
    public static void column(ServerLevel level,RealisticGenerator g,LevelChunk chunk)throws Exception{
        columns.add(chunk.getPos());var p=((SparseColumn)chunk).blockbranch$terrain().base();var factory=level.palettedContainerFactory();
        require(p.caveBiomes().enabled(),"New profiles explicitly opt into cave biomes");
        var tag=p.encode(factory);require(tag.getIntOr("version",0)==70,"Combined profile70");var copy=TerrainProfile.decode(tag,factory);
        require(copy.encode(factory).equals(tag),"Descriptor round trip exact");
        var source=(ScreeBiomeSource)g.getBiomeSource();var full=source.createUncachedResolver(level.getChunkSource().randomState());
        int cx=chunk.getPos().x(),cz=chunk.getPos().z();var cached=source.createResolverForChunk(null,cx*4,0,cz*4,4,96,4);
        var queries=new LinkedHashSet<List<Integer>>();
        for(int z=0;z<4;z++)for(int x=0;x<4;x++)for(int d:new int[]{-16,0,12,48,96,128,256,384,388,512})queries.add(List.of(x,Math.floorDiv(p.groundHeight(x*4,z*4)-d,4),z));
        var moss=p.litCaves().placements();for(int i=0;i<moss.length;i+=2)queries.add(List.of((moss[i]&15)>>2,(moss[i+1]+1)>>2,(moss[i]>>4)>>2));
        for(var q:queries){int x=q.get(0),y=q.get(1),z=q.get(2);var expected=p.biome(x,y,z);
            require(expected.equals(full.getNoiseBiome(cx*4+x,y,cz*4+z)),"Uncached source/profile at "+chunk.getPos()+" "+q);
            require(expected.equals(cached.getNoiseBiome(cx*4+x,y,cz*4+z)),"Cached source/profile");
            require(expected.equals(copy.biome(x,y,z)),"Decoded profile identity");
            require(expected.equals(chunk.getNoiseBiome(cx*4+x,y,cz*4+z)),"Native chunk biome");
            require(expected.equals(p.view(y>>2).getBiomes().get(x,y&3,z))&&expected.equals(p.generate(y>>2).getNoiseBiome(x,y&3,z)),"Read-only and materialized palette agree");comparisons++;
        }
        for(int i=0;i<moss.length;i+=2){int x=(moss[i]&15)>>2,y=(moss[i+1]+1)>>2,z=(moss[i]>>4)>>2;require(p.biome(x,y,z).is(Biomes.LUSH_CAVES),"Every lush cell anchored to actual approved moss");spawns(level,g,new BlockPos(cx*16+x*4+2,y*4+2,cz*16+z*4+2));}
        var pool=java.util.concurrent.Executors.newFixedThreadPool(3);
        try{var jobs=new ArrayList<java.util.concurrent.Callable<Boolean>>();for(int i=0;i<3;i++)jobs.add(()->{for(int k=0;k<3;k++)for(var q:queries)require(p.biome(q.get(0),q.get(1),q.get(2)).equals(cached.getNoiseBiome(cx*4+q.get(0),q.get(1),cz*4+q.get(2))),"Concurrent immutable selector");return true;});for(var f:pool.invokeAll(jobs))require(f.get(),"Selector worker");}finally{pool.shutdownNow();}
        for(String corruption:List.of("missing","seed","owner","definition","holders")){var bad=tag.copy();var layer=bad.getCompoundOrEmpty("cave_biomes");switch(corruption){case "missing"->bad.remove("cave_biomes");case "seed"->layer.putLong("seed",p.underground().seed()+1);case "owner"->layer.putInt("column_x",cx+1);case "definition"->layer.putInt("definition",2);case "holders"->layer.remove("holders");}boolean rejected=false;try{TerrainProfile.decode(bad,factory);}catch(RuntimeException expected){rejected=true;}require(rejected,"Malformed cave descriptor rejected: "+corruption);}
    }
    private static void spawns(ServerLevel level,RealisticGenerator g,BlockPos pos){
        var surface=level.getBiomeManager().withDifferentSource(((ScreeBiomeSource)g.getBiomeSource()).surfaceResolver());
        var expected=surface.getBiome(pos).value().getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS,MobSpawnSettings.EMPTY);
        var actual=level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS,pos);
        for(var category:MobCategory.values())require(expected.getMobsToSpawn(category).equals(actual.getMobsToSpawn(category)),"Native spawn policy unchanged: "+category+" at "+pos);
    }
    public static void verify(ServerLevel level,RealisticGenerator g)throws Exception{
        var dir=Path.of(System.getProperty("blockbranch.realisticSmokeResult")).getParent();var random=level.getChunkSource().randomState();var source=(ScreeBiomeSource)g.getBiomeSource();var origin=g.getOrigin(random);LevelChunk chosen=null;BlockPos sulfur=null;
        search:for(int ring=0;ring<24;ring++)for(int dz=-ring;dz<=ring;dz++)for(int dx=-ring;dx<=ring;dx++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=ring)continue;int cx=origin.x()+dx*4,cz=origin.z()+dz*4,wx=cx*16,wz=cz*16;
            if(DripstoneHabitat.province(random.seed()^0x73756c6675725f31L,wx,wz)<=.45||DripstoneHabitat.province(random.seed(),wx,wz)>.2)continue;
            chosen=level.getChunk(cx,cz);var p=((SparseColumn)chosen).blockbranch$terrain().base();
            for(int depth=112;depth<280;depth++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                int y=p.groundHeight(x,z)-depth;if(p.biome(x>>2,y>>2,z>>2).is(Biomes.SULFUR_CAVES)&&p.state(x,y,z).isAir()&&p.state(x,y+1,z).isAir()){
                    sulfur=new BlockPos(wx+x,y,wz+z);break search;
                }
            }
        }
        require(sulfur!=null,"Bounded search finds native rocky sulfur cave");column(level,g,chosen);spawns(level,g,sulfur);
        // The native biome does contain newly excluded defaults; the actual attribute
        // path above must still equal the pre-biome surface policy for every category.
        var nativeSulfur=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.SULFUR_CAVES).value().getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS,MobSpawnSettings.EMPTY);
        boolean differs=false;var surface=source.surfaceResolver().getNoiseBiome(sulfur.getX()>>2,sulfur.getY()>>2,sulfur.getZ()>>2).value().getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS,MobSpawnSettings.EMPTY);
        for(var category:MobCategory.values())differs|=!nativeSulfur.getMobsToSpawn(category).equals(surface.getMobsToSpawn(category));require(differs,"Test is sensitive to native sulfur spawn defaults");
        resolverLifetime(level,g,sulfur);
        preparedLifetime(level,g);
        if(Boolean.getBoolean("blockbranch.caveBiomeTiming"))fixedCost(level,g,dir);
        var moss=new Gson().fromJson(Files.readString(dir.resolve("moss-edits.json")),int[][].class);var a=moss[0];
        Files.writeString(dir.resolve("biome-cameras.json"),new Gson().toJson(Map.of("cameras",List.of(new int[]{a[0],a[1]+3,a[2]+3},new int[]{sulfur.getX(),sulfur.getY(),sulfur.getZ()}),"targets",List.of(new int[]{a[0],a[1],a[2]},new int[]{sulfur.getX()+4,sulfur.getY(),sulfur.getZ()}))));
        Files.writeString(dir.resolve("cave-biomes.txt"),"PASS comparisons="+comparisons+" nativeCachedUncachedProfilesPalettes=true spawnCategories=all sulfur="+sulfur+" lifetime=old-resolver-after-rebind boundedCache=64 concurrentReaders=3\n");
        blockbranch.Blockbranch.LOGGER.info("CAVE_BIOMES_NATIVE_PASS comparisons={} sulfur={}",comparisons,sulfur);
    }
    private static void fixedCost(ServerLevel level,RealisticGenerator g,Path dir)throws Exception{
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var on=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow().getAsJsonObject();var off=on.deepCopy();off.remove("cave_biome_definition");off.addProperty("shape_version",84);
        var inputs=new ArrayList<>(columns);var random=level.getChunkSource().randomState();var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();long tid=Thread.currentThread().threadId();var rows=new ArrayList<Map<String,Object>>();
        require(bean.isThreadAllocatedMemoryEnabled()&&bean.isCurrentThreadCpuTimeSupported(),"Fixed pipeline counters");
        for(int round=-1;round<3;round++){
            var arms=new RealisticGenerator[]{RealisticGenerator.CODEC.codec().parse(ops,off).getOrThrow(),RealisticGenerator.CODEC.codec().parse(ops,on).getOrThrow()};
            for(var arm:arms)arm.drainage(random); // Seed/graph construction outside profile pipeline measurement.
            var outputs=new ArrayList<List<TerrainProfile>>();outputs.add(new ArrayList<>());outputs.add(new ArrayList<>());
            for(int sequence=0;sequence<2;sequence++){
                int arm=(round&1)==0?sequence:1-sequence;var generator=arms[arm];var source=(ScreeBiomeSource)generator.getBiomeSource();
                for(int pass=0;pass<2;pass++){
                    long cpu=bean.getCurrentThreadCpuTime(),bytes=bean.getThreadAllocatedBytes(tid),wall=System.nanoTime();int queries=0;
                    for(var c:inputs){
                        var proto=new net.minecraft.world.level.chunk.ProtoChunk(c,net.minecraft.world.level.chunk.UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
                        generator.prepareCaveProfile(proto,random);
                        var resolver=source.createResolverForChunk(null,c.x()*4,-16,c.z()*4,4,96,4);
                        for(int y=-16;y<80;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++){costSink=resolver.getNoiseBiome(c.x()*4+x,y,c.z()*4+z);queries++;}
                        var profile=generator.profileForBuild(proto,random);costSink=profile;if(pass==1)outputs.get(arm).add(profile);
                    }
                    if(round>=0)rows.add(Map.of("round",round,"caveBiomes",arm==1,"warm",pass==1,"columns",inputs.size(),"queries",queries,"cpuNs",bean.getCurrentThreadCpuTime()-cpu,"bytes",bean.getThreadAllocatedBytes(tid)-bytes,"wallNs",System.nanoTime()-wall));
                }
            }
            for(int i=0;i<inputs.size();i++){
                var a=outputs.get(0).get(i).encode(level.palettedContainerFactory());var b=outputs.get(1).get(i).encode(level.palettedContainerFactory());b.remove("cave_biomes");b.putInt("version",68);require(a.equals(b),"Biome feature preserves exact complete terrain/decor/ore descriptors");
            }
        }
        costSink=null;Files.writeString(dir.resolve("cave-biome-fixed-cost.json"),new GsonBuilder().setPrettyPrinting().create().toJson(rows));
    }
    private static void preparedLifetime(ServerLevel level,RealisticGenerator g)throws Exception{
        var random=level.getChunkSource().randomState();var pos=columns.iterator().next();
        var first=new net.minecraft.world.level.chunk.ProtoChunk(pos,net.minecraft.world.level.chunk.UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
        g.prepareCaveProfile(first,random);var field=RealisticGenerator.class.getDeclaredField("preparedProfiles");field.setAccessible(true);
        @SuppressWarnings("unchecked") var pending=(Map<net.minecraft.world.level.chunk.ChunkAccess,TerrainProfile>)field.get(g);
        TerrainProfile prepared;synchronized(pending){prepared=pending.get(first);}require(prepared!=null,"Actual prepare stage populated handoff");
        var replacement=new net.minecraft.world.level.chunk.ProtoChunk(pos,net.minecraft.world.level.chunk.UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);
        require(g.profileForBuild(replacement,random)!=prepared,"Replacement chunk cannot consume another chunk object's pending profile");
        require(g.profileForBuild(first,random)==prepared,"Native terrain stage consumes exact prepared immutable profile once");
        synchronized(pending){require(!pending.containsKey(first),"Consumed profile removed");}
        // Reuse the same coordinate to exercise identities, cancellation and bounds
        // without generating 65 additional terrain regions.
        var retained=new ArrayList<net.minecraft.world.level.chunk.ProtoChunk>();
        for(int i=0;i<65;i++){var c=new net.minecraft.world.level.chunk.ProtoChunk(pos,net.minecraft.world.level.chunk.UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);g.prepareCaveProfile(c,random);retained.add(c);}
        synchronized(pending){require(pending.size()==64,"Real preparation cap is64 even with strongly retained chunks");}
        var abandoned=new ArrayList<java.lang.ref.WeakReference<net.minecraft.world.level.chunk.ProtoChunk>>();var identities=new HashSet<Integer>();
        for(var c:retained){abandoned.add(new java.lang.ref.WeakReference<>(c));identities.add(System.identityHashCode(c));}retained=null;
        for(int i=0;i<6&&abandoned.stream().anyMatch(ref->ref.get()!=null);i++){System.gc();Thread.sleep(20);}
        require(abandoned.stream().allMatch(ref->ref.get()==null),"Every abandoned fixture chunk is collectible");
        // Native generation may legitimately still own other pending chunks.
        // Observe the fixture's actual owners, not an unjustified global-empty condition.
        synchronized(pending){require(pending.keySet().stream().noneMatch(c->identities.contains(System.identityHashCode(c))),"All abandoned fixture entries expire");require(pending.size()<=64,"Remaining native handoff stays bounded");blockbranch.Blockbranch.LOGGER.info("CAVE_PREPARATION_LIFETIME_PASS abandoned=65 remainingNative={}",pending.size());}
    }
    private static void resolverLifetime(ServerLevel level,RealisticGenerator g,BlockPos point)throws Exception{
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());var config=RealisticGenerator.CODEC.codec().encodeStart(ops,g).getOrThrow();
        var detached=RealisticGenerator.CODEC.codec().parse(ops,config).getOrThrow();var random=level.getChunkSource().randomState();detached.drainage(random);var source=(ScreeBiomeSource)detached.getBiomeSource();var before=source.createUncachedResolver(random);
        var other=net.minecraft.world.level.levelgen.RandomState.create(level.registryAccess().lookupOrThrow(Registries.NOISE),random.seed()+1,level.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.OVERWORLD).value());
        detached.drainage(other);var after=source.createUncachedResolver(other);var original=g.getBiomeSource().createUncachedResolver(random);
        for(int d=0;d<70;d++){int x=(point.getX()>>2)+d*4,z=point.getZ()>>2,y=point.getY()>>2;require(before.getNoiseBiome(x,y,z).equals(original.getNoiseBiome(x,y,z)),"Retained old resolver uses its seed after rebind/cache eviction");}
        require(source.createUncachedResolver(other)==after,"Old lazy query never rebinds active source");
        // Inspect the real bounded cache captured by the actual production resolver.
        var field=ScreeBiomeSource.class.getDeclaredField("bound");field.setAccessible(true);var bound=field.get(g.getBiomeSource());var accessor=bound.getClass().getDeclaredMethod("caves");accessor.setAccessible(true);var callback=accessor.invoke(bound);boolean checked=false;
        for(var f:callback.getClass().getDeclaredFields()){f.setAccessible(true);var value=f.get(callback);if(value instanceof Map<?,?> map){synchronized(map){require(map.size()<=64,"Real selector cache remains bounded after 70 distinct columns");}checked=true;}}
        require(checked,"Observed actual production cache");
        var witness=new java.lang.ref.WeakReference<>(detached);detached=null;after=null;source=null;
        // Keep old resolver alive: it may own its immutable plan, but not the original generator.
        for(int i=0;i<6&&witness.get()!=null;i++){System.gc();Thread.sleep(20);}
        require(witness.get()==null,"Old resolver does not retain generator owner");java.lang.ref.Reference.reachabilityFence(before);
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
