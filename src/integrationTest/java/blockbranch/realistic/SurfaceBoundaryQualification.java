package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.RandomState;

/** Finite, dense boundary qualification around existing witnesses, not a reachability search. */
public final class SurfaceBoundaryQualification {
    private static final long SEED=424242;
    private static final int[][] ROCK={{-1328000,-1904000},{-1552000,-1840000},{1648000,-1744000}};
    private static final int[][] SEA={{-1008000,-1776000},{400000,-944000},{-1340916,-1687864}};
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server->{
            Path result=Path.of(System.getProperty("blockbranch.surfaceBoundaryResult"));
            var report=new LinkedHashMap<String,Object>();
            try {
                var level=server.overworld();var random=level.getChunkSource().randomState();
                var g=(RealisticGenerator)level.getChunkSource().getGenerator();
                require(random.seed()==SEED&&g.surfaceOwnershipDefinition()==1,"Fixed candidate world/gate");
                var encoded=RealisticGenerator.CODEC.codec().encodeStart(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,level.registryAccess()),g).getOrThrow().getAsJsonObject();
                report.put("seed",SEED);report.put("shapeVersion",encoded.get("shape_version").getAsInt());report.put("surfaceOwnershipDefinition",1);report.put("dryForestDefinition",g.dryForestDefinition());
                report.put("rockland",rockland(level,g,random));
                report.put("kelp",kelp(level,g,random));
                Files.writeString(result.resolveSibling("result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
                Files.writeString(result,"PASS dense native rockland/quart edges; bounded physical kelp versus packed/native columns, both gate paths\n");
                Blockbranch.LOGGER.info("SURFACE_BOUNDARY_PASS {}",report);
            } catch(Throwable failure) {
                Blockbranch.LOGGER.error("SURFACE_BOUNDARY_FAIL",failure);
                try{Files.writeString(result,"FAIL "+failure+"\n");}catch(Exception ignored){}
            } finally {server.halt(false);}
        });
    }
    private static Map<String,Object> rockland(ServerLevel level,RealisticGenerator g,RandomState random) {
        var d=g.drainage(random);var c=new ClimateField(SEED);var source=(ScreeBiomeSource)g.getBiomeSource();
        var owner=source.coldRocklandSurface();var surface=source.surfaceResolver();
        int columns=0,owned=0,otherQuart=0,ineligible=0,specialistEdge=0,quartEdges=0,states=0,scree=0,snow=0;
        int[] depths=new int[3];var witnesses=new LinkedHashMap<String,Object>();var chunks=new HashSet<Long>();
        for(var center:ROCK)for(int z=center[1]-64;z<=center[1]+64;z++)for(int x=center[0]-64;x<=center[0]+64;x++) {
            boolean quart=surface.getNoiseBiome(x>>2,0,z>>2).unwrapKey().orElseThrow().identifier().toString().equals("blockbranch:cold_exposed_rockland");
            boolean physical=ColdRockland.eligible(x,z,d,c),selected=owner.test(x,z);
            require(selected==(quart&&physical),"Owner is quart fallback intersected with physical cold/dry domain");
            if(physical&&!quart){otherQuart++;witnesses.putIfAbsent("physicalEligibleOtherQuart",List.of(x,z));}
            if(quart&&!physical){ineligible++;witnesses.putIfAbsent("quartPhysicallyIneligible",List.of(x,z));}
            if(selected&&(owner.test(x+1,z)!=selected||owner.test(x,z+1)!=selected||owner.test(x-1,z)!=selected||owner.test(x,z-1)!=selected)){
                quartEdges++;witnesses.putIfAbsent("ownerEdge",List.of(x,z));
            }
            var chunk=level.getChunk(x>>4,z>>4);chunks.add(((long)(x>>4)<<32)|((z>>4)&0xffffffffL));
            var profile=((SparseColumn)chunk).blockbranch$terrain().base();
            if(quart)require(profile.biomes().get((x&15)>>2,0,(z&15)>>2)==surface.getNoiseBiome(x>>2,0,z>>2),"Native final quart retains selected rockland identity");
            int h=d.sample(x,z).height();
            if(selected){
                owned++;int depth=ColdRockland.debrisDepth(SEED,x,z,d);depths[depth]++;
                require(g.screeDepth(x,z,random)==depth,"Native/analytic rockland debris bound");
                if(SnowySlopes.surfaceContains(x,z,d,c)||MountainPeaks.surfaceKind(SEED,x,z,d,c)>0||ColdUplands.surfaceKind(SEED,x,z,d,c)>0){
                    specialistEdge++;witnesses.putIfAbsent("physicalSpecialistInsideFallbackQuart",List.of(x,z));
                }
                var top=level.getBlockState(new BlockPos(x,h,z));
                require(depth>0?top.is(ScreeContent.LOOSE_SCREE):top.is(Blocks.STONE)||top.isAir(),"Owned ground is supported loose scree or exposed/cave-carved stone, never fertile soil/land ice at "+x+","+h+","+z+" depth="+depth+" native="+top+" analytic="+g.surfaceState(x,h,z,random));
            }
            for(int y=h-3;y<=h+3;y++) {
                var actual=level.getBlockState(new BlockPos(x,y,z));
                require(actual.equals(g.surfaceState(x,y,z,random)),"Dense native/analytic boundary state at "+x+","+y+","+z+": "+actual);
                if(actual.is(ScreeContent.LOOSE_SCREE))scree++;
                if(actual.is(Blocks.SNOW))snow++;
                states++;
            }
            columns++;
        }
        require(owned>0&&otherQuart>0&&quartEdges>0,"Nonvacuous owned ground and actual quart boundary witnesses");
        require(depths[0]>0&&depths[1]>0&&depths[2]>0&&snow>0,"Exposed rock, both debris depths and native snow exercised");
        var out=new LinkedHashMap<String,Object>();out.put("centers",ROCK);out.put("inclusiveRadiusBlocks",64);out.put("strideBlocks",1);
        out.put("columns",columns);out.put("nativeChunks",chunks.size());out.put("nativeStates",states);out.put("ownedColumns",owned);
        out.put("physicalEligibleOtherQuart",otherQuart);out.put("quartPhysicallyIneligible",ineligible);out.put("physicalSpecialistInsideFallbackQuart",specialistEdge);
        out.put("ownerBoundaryColumns",quartEdges);out.put("debrisDepthCounts",depths);out.put("looseScreeStates",scree);out.put("snowStates",snow);out.put("witnesses",witnesses);
        return out;
    }
    private record Patch(int cx,int cz,int roots,int nonanchorRoots,int quartDifference) {}
    private static Map<String,Object> kelp(ServerLevel level,RealisticGenerator g,RandomState random) {
        var c=new ClimateField(SEED);var fresh=g.drainage(random);
        var old=new DrainageNetwork(new TerrainShape(SEED),SEED,true,true,true);
        var gates=new ArrayList<Object>();var nativePatches=new LinkedHashMap<Long,Patch>();
        for(var d:List.of(old,fresh)) {
            int chunks=0,skipped=0,roots=0,nonanchor=0,different=0,entries=0,emptyPhysicalPatches=0;
            var sparse=new ArrayList<Patch>();
            for(var center:SEA) {
                var local=new ArrayList<Patch>();
                for(int cz=(center[1]>>4)-8;cz<=(center[1]>>4)+8;cz++)for(int cx=(center[0]>>4)-8;cx<=(center[0]>>4)+8;cx++) {
                    boolean any=false;for(int z=0;z<4;z++)for(int x=0;x<4;x++)any|=TemperateOcean.contains(SEED,cx*16+x*4,cz*16+z*4,d,c);
                    var expected=new TreeMap<Integer,Integer>();int n=0,na=0,qd=0;
                    for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
                        int wx=cx*16+x,wz=cz*16+z,h=TemperateOcean.kelpHeight(SEED,wx,wz,d,c);
                        if(h==0)continue;
                        n++;if((x&3)!=0||(z&3)!=0)na++;
                        if(!TemperateOcean.contains(SEED,wx,wz,d,c))qd++;
                        int ground=d.sample(wx,wz).height();
                        for(int y=1;y<=h;y++)expected.put(((ground+y-TerrainShape.FLOOR)<<8)|(z*16+x),y==h?TemperateOcean.KELP_HEAD:TemperateOcean.KELP_BODY);
                    }
                    int[] packed=expected.entrySet().stream().mapToInt(e->(e.getKey()<<CanopyLayer.STATE_BITS)|e.getValue()).toArray();
                    // This oracle deliberately never calls column's quart-any prefilter.
                    require(Arrays.equals(packed,TemperateOcean.column(SEED,cx,cz,d,c)),"No positive physical root skipped/extra packed plant at gate "+d.correctedHabitatOwnership()+" chunk "+cx+","+cz+" roots="+n+" anyQuart="+any);
                    if(!any){skipped++;require(n==0,"Quart-prefilter skip has no physical root within these bounds");}
                    if(n==0)emptyPhysicalPatches++;
                    var patch=new Patch(cx,cz,n,na,qd);if(n>0)local.add(patch);
                    chunks++;roots+=n;nonanchor+=na;different+=qd;entries+=packed.length;
                }
                // Eight sparsest nonempty patches per fixed neighborhood exercise
                // narrow colonies, not just a favorable dense center. No expanding search.
                local.sort(Comparator.comparingInt(Patch::roots).thenComparingInt(Patch::cx).thenComparingInt(Patch::cz));
                for(var patch:local.stream().limit(8).toList()){
                    sparse.add(patch);
                    if(d==fresh)nativePatches.put(((long)patch.cx<<32)|(patch.cz&0xffffffffL),patch);
                }
            }
            require(roots>0&&nonanchor>0&&skipped>0,"Nonvacuous plants, subquart roots and skipped chunks");
            var gate=new LinkedHashMap<String,Object>();gate.put("definition",d==old?0:1);gate.put("chunks",chunks);gate.put("physicalColumns",chunks*256);
            gate.put("prefilterSkippedChunks",skipped);gate.put("positiveRoots",roots);gate.put("nonanchorRoots",nonanchor);
            gate.put("rootsOutsideQuartMembership",different);gate.put("packedEntries",entries);gate.put("emptyPhysicalChunks",emptyPhysicalPatches);gate.put("sparsestPatches",sparse);
            gates.add(gate);
        }
        int states=0,plants=0,nonanchorPlants=0;
        for(var patch:nativePatches.values()) {
            level.getChunk(patch.cx,patch.cz);
            for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
                int wx=patch.cx*16+x,wz=patch.cz*16+z,ground=fresh.sample(wx,wz).height();
                int height=TemperateOcean.kelpHeight(SEED,wx,wz,fresh,c);
                for(int y=ground-2;y<fresh.sample(wx,wz).waterLevel();y++) {
                    var actual=level.getBlockState(new BlockPos(wx,y,wz));
                    require(actual.equals(g.surfaceState(wx,y,wz,random)),"Native/analytic sparse kelp or bed state at "+wx+","+y+","+wz);
                    if(height>0&&y>ground&&y<=ground+height){
                        require(actual.is(y==ground+height?Blocks.KELP:Blocks.KELP_PLANT),"Every physical root has native body/head, including subquart edges");plants++;
                        if((x&3)!=0||(z&3)!=0)nonanchorPlants++;
                    }
                    if(height>0&&y==ground)require(actual.is(Blocks.STONE),"Native kelp has its physical hard bed");
                    states++;
                }
            }
        }
        require(plants>0&&nonanchorPlants>0,"Native sparse kelp actually generated away from quart anchors");
        return Map.of("centers",SEA,"inclusiveRadiusChunks",8,"chunkStride",1,"blockStride",1,"gates",gates,
            "nativeSparseChunks",nativePatches.size(),"nativeStates",states,"nativeKelpStates",plants,"nativeNonanchorKelpStates",nonanchorPlants);
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private SurfaceBoundaryQualification() {}
}
