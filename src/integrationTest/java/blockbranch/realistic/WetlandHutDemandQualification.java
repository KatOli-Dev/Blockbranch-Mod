package blockbranch.realistic;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.*;

/** One bounded native comparison: startup binding versus all deferred exact work. */
public final class WetlandHutDemandQualification {
    private static final com.sun.management.ThreadMXBean THREADS=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
    private static final com.sun.management.OperatingSystemMXBean OS=(com.sun.management.OperatingSystemMXBean)java.lang.management.ManagementFactory.getOperatingSystemMXBean();
    private record Stamp(long wall,long cpu,long allocation){static Stamp now(){return new Stamp(System.nanoTime(),OS.getProcessCpuTime(),THREADS.getTotalThreadAllocatedBytes());}Map<String,Long> elapsed(){return Map.of("wallNanos",System.nanoTime()-wall,"jvmCpuNanos",OS.getProcessCpuTime()-cpu,"jvmAllocatedBytes",THREADS.getTotalThreadAllocatedBytes()-allocation);}}
    public static void register(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{try{
        var level=server.overworld();var original=(RealisticGenerator)level.getChunkSource().getGenerator();
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess());
        var input=RealisticGenerator.CODEC.codec().encodeStart(ops,original).getOrThrow();
        var noise=level.registryAccess().lookupOrThrow(Registries.NOISE);var settings=level.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value();
        WetlandHutsReference.build(level.getSeed(),original,level.getChunkSource().randomState());
        var reports=new ArrayList<Object>();int index=0;
        for(long seed:new long[]{424242,1,-17}){
            var frozen=com.google.gson.JsonParser.parseString(Files.readString(Path.of(System.getProperty("blockbranch.hutFrozenCatalogs"),"sites-"+seed+".json")));
            var expected=WetlandHuts.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,frozen).getOrThrow();var report=new LinkedHashMap<String,Object>();report.put("seed",seed);
            for(int phase=0;phase<2;phase++){
                boolean baseline=(phase==0)==((index&1)==0);var g=RealisticGenerator.CODEC.codec().parse(ops,input).getOrThrow();var random=RandomState.create(noise,seed,settings);
                long setup=System.nanoTime();var d=g.drainage(random);long setupNanos=System.nanoTime()-setup;System.gc();
                if(baseline){var start=Stamp.now();var full=WetlandHuts.build(seed,g,random);report.put("eager",start.elapsed());require(WetlandHuts.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,full).getOrThrow().equals(frozen),"Eager frozen catalog");report.put("eagerTerrainSetupExcluded",setupNanos);continue;}
                var total=Stamp.now();var start=Stamp.now();var demand=WetlandHuts.bind(seed,g,random);report.put("bind",start.elapsed());
                require(demand.resolvedRegions()==0&&!demand.canSave(),"Binding samples no regions and saves no partial cache");report.put("regions",demand.regionCount());report.put("demandTerrainSetupExcluded",setupNanos);
                start=Stamp.now();for(int z=-4;z<=4;z++)for(int x=-4;x<=4;x++)demand.forStart(x,z);report.put("origin81ChunkQueries",start.elapsed());report.put("originRegionsResolved",demand.resolvedRegions());
                // First locate on a cold index: no chunk probe above touched a region.
                var touched=new HashSet<Integer>();start=Stamp.now();WetlandHuts.Plan nearestPlan=null;double near=6000000;
                for(var group:demand.orderedGroups(BlockPos.ZERO,near)){if(group.lowerDistance(BlockPos.ZERO)>near)break;touched.add(group.lake());var value=group.plan();if(value.isEmpty())continue;var v=value.get();double distance=Math.hypot(v.x(),v.z());if(distance<near||distance==near&&(nearestPlan==null||v.lake()>nearestPlan.lake())){near=distance;nearestPlan=v;}}
                report.put("firstColdLocateAtOrigin",start.elapsed());report.put("firstLocateRegions",touched.size());
                var expectedNearest=expected.plans().stream().min(Comparator.comparingDouble((WetlandHuts.Plan v)->Math.hypot(v.x(),v.z())).thenComparing(Comparator.comparingInt(WetlandHuts.Plan::lake).reversed())).orElseThrow();
                require(Objects.equals(nearestPlan,expectedNearest),"Cold locate exact nearest");
                // Find the maximum simultaneous cold envelope overlap at a legal
                // chunk center, by the lower-left intersection of each rectangle pair.
                var bounds=new ArrayList<int[]>();for(var group:demand.orderedGroups(BlockPos.ZERO,6000000)){int lake=group.lake();double largest=0;for(int angle=0;angle<64;angle++)largest=Math.max(largest,d.lakeShoreRadius(lake,angle*Math.PI/32));bounds.add(new int[]{lake,d.nodeX(lake),d.nodeZ(lake),(int)Math.ceil(largest)+16});}
                int overlap=0,overlapX=0,overlapZ=0;
                for(var a:bounds)for(var b:bounds){int minX=Math.max(a[1]-a[3],b[1]-b[3]),maxX=Math.min(a[1]+a[3],b[1]+b[3]),minZ=Math.max(a[2]-a[3],b[2]-b[3]),maxZ=Math.min(a[2]+a[3],b[2]+b[3]);
                    int x=Math.floorDiv(minX+7,16)*16+8,z=Math.floorDiv(minZ+7,16)*16+8;if(x>maxX||z>maxZ||Math.abs((long)x)>2097100||Math.abs((long)z)>2097100)continue;
                    int count=0;boolean cold=true;for(var box:bounds)if(Math.abs((long)x-box[1])<=box[3]&&Math.abs((long)z-box[2])<=box[3]){count++;if(touched.contains(box[0]))cold=false;}
                    if(cold&&count>overlap){overlap=count;overlapX=x;overlapZ=z;}
                }
                int resolvedBefore=demand.resolvedRegions();start=Stamp.now();var overlapPlan=demand.forStart(overlapX>>4,overlapZ>>4);report.put("coldMaximumOverlapQuery",start.elapsed());report.put("overlapOwners",overlap);report.put("overlapRegionsResolved",demand.resolvedRegions()-resolvedBefore);
                require(demand.resolvedRegions()-resolvedBefore==overlap,"Every overlapping owner resolved before admission");
                final int ox=overlapX,oz=overlapZ;require(overlapPlan.equals(expected.plans().stream().filter(v->(v.x()>>4)==(ox>>4)&&(v.z()>>4)==(oz>>4)).max(Comparator.comparingInt(WetlandHuts.Plan::lake))),"Overlapping start exact");
                var p=expected.plans().getFirst();start=Stamp.now();require(demand.forStart(p.x()>>4,p.z()>>4).equals(Optional.of(p)),"Cold accepted start ownership");report.put("coldAcceptedStart",start.elapsed());
                var partial=WetlandHuts.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,demand).getOrThrow();require(!partial.getAsJsonObject().has("plan_cache"),"Partial work is omitted from saves");
                var parsed=WetlandHuts.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,partial).getOrThrow();require(!parsed.valid(seed)&&parsed.plans().isEmpty(),"Partial save recovers as missing derived cache");
                var concurrent=expected.plans().getLast();try(var pool=Executors.newFixedThreadPool(4)){
                    var calls=new ArrayList<Callable<Optional<WetlandHuts.Plan>>>();for(int i=0;i<12;i++)calls.add(()->demand.forStart(concurrent.x()>>4,concurrent.z()>>4));
                    for(var result:pool.invokeAll(calls))require(result.get().equals(Optional.of(concurrent)),"Concurrent ownership");
                }
                var groups=new ArrayList<>(demand.orderedGroups(BlockPos.ZERO,6000000));groups.sort(Comparator.comparingInt(WetlandHuts.Candidate::lake).reversed());
                var interrupted=groups.getFirst();int before=demand.resolvedRegions();Thread.currentThread().interrupt();
                try{interrupted.plan();throw new AssertionError("Interrupted region published");}catch(CancellationException good){require(Thread.currentThread().isInterrupted(),"Interrupt flag retained");}finally{Thread.interrupted();}
                require(demand.resolvedRegions()==before,"Cancelled region unchanged");
                var timings=new ArrayList<Map<String,Object>>();
                for(var group:groups){int count=demand.resolvedRegions();start=Stamp.now();var result=group.plan();var elapsed=start.elapsed();
                    if(demand.resolvedRegions()>count){var row=new LinkedHashMap<String,Object>();row.put("lake",group.lake());row.put("accepted",result.isPresent());row.putAll(elapsed);timings.add(row);}
                }
                // Every individual result is still omitted until an atomic full snapshot.
                require(!demand.canSave(),"Individual region results are never a partial serialized catalog");
                demand.plans();report.put("allDeferredWorkIncludingBindAndQueries",total.elapsed());
                require(WetlandHuts.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,demand).getOrThrow().equals(frozen),"Full demand cache/checksum exactly frozen");
                require(demand.valid(seed)&&demand.plans().equals(expected.plans()),"Full catalog order and validation");
                for(var site:expected.plans())require(demand.forStart(site.x()>>4,site.z()>>4).equals(Optional.of(site)),"Every frozen start routes identically");
                // Classify exhaustion after measurements: an eligible lake that passes
                // the unchanged region climate bound and has no plan uses all2048 proposals.
                var climate=new ClimateField(seed);int exhausted=0;
                for(var row:timings){int lake=(int)row.get("lake");double largest=0;for(int angle=0;angle<64;angle++)largest=Math.max(largest,d.lakeShoreRadius(lake,angle*Math.PI/32));
                    boolean exhaustion=!(boolean)row.get("accepted")&&climate.swampBankPossible(d.nodeX(lake),d.nodeZ(lake),(int)Math.ceil(largest)+32,d.waterLevel(lake),d);row.put("exhausted2048",exhaustion);if(exhaustion)exhausted++;
                }
                timings.sort(Comparator.comparingLong(row->(long)row.get("wallNanos")));report.put("coldRegionTimings",timings);report.put("exhaustedRegions",exhausted);report.put("exactFrozenCatalog",true);report.put("sites",demand.plans().size());
                // Independent nearest simulation uses actual site distance/lake ties;
                // native qualification verifies StructureStart/skip-known integration.
                for(var origin:List.of(BlockPos.ZERO,p.center(),new BlockPos(-2097100,100,2097100))){
                    WetlandHuts.Plan best=null;double nearest=6000000;
                    for(var group:demand.orderedGroups(origin,nearest)){if(group.lowerDistance(origin)>nearest)break;var value=group.plan();if(value.isEmpty())continue;var v=value.get();double distance=Math.hypot(v.x()-(double)origin.getX(),v.z()-(double)origin.getZ());
                        if(distance<nearest||distance==nearest&&(best==null||v.lake()>best.lake())){best=v;nearest=distance;}}
                    final var at=origin;var want=expected.plans().stream().min(Comparator.comparingDouble((WetlandHuts.Plan v)->Math.hypot(v.x()-(double)at.getX(),v.z()-(double)at.getZ())).thenComparing(Comparator.comparingInt(WetlandHuts.Plan::lake).reversed())).orElse(null);
                    require(Objects.equals(best,want),"Exact lower-bound nearest/tie result");
                }
            }
            reports.add(report);index++;Files.writeString(Path.of("demand.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(reports));
        }
        Files.writeString(Path.of("demand-result.txt"),"PASS three exact frozen catalogs, zero sampled regions at bind, cold ownership, omitted partial caches, interruption/concurrency and nearest bounds");
    }catch(Throwable t){blockbranch.Blockbranch.LOGGER.error("HUT_DEMAND_FAIL",t);try{Files.writeString(Path.of("demand-result.txt"),"FAIL "+t);}catch(Exception ignored){}}finally{server.halt(false);}});}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
