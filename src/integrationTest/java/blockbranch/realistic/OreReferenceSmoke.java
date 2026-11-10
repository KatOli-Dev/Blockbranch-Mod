package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.FeaturePlacer;

/** Controlled native vanilla placement reference; never writes the real world's chunks. */
public final class OreReferenceSmoke {
    private static final Set<String> TYPES=Set.of("coal","iron","copper","gold","redstone","lapis","diamond","emerald");
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        try{
            var level=server.overworld();var registry=level.registryAccess().lookupOrThrow(Registries.BIOME);var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
            var settings=level.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD);
            var data=new ArrayList<Map<String,Object>>();var features=new LinkedHashMap<String,Object>();
            for(boolean caves:new boolean[]{false,true})for(var key:List.of(Biomes.PLAINS,Biomes.WINDSWEPT_HILLS,Biomes.BADLANDS))for(long seed:new long[]{424242,1,-17,271828,8675309}){
                var biome=registry.getOrThrow(key);var nativeGenerator=new NoiseBasedChunkGenerator(new FixedBiomeSource(biome),settings);var fake=new Host(level,biome,seed,caves);var world=fake.proxy();int calls=0;
                for(var group:biome.value().getGenerationSettings().features())for(var holder:group){
                    String id=holder.unwrapKey().orElseThrow().identifier().getPath();if(!resource(id))continue;
                    features.putIfAbsent(id,Map.of("placement",PlacedFeature.DIRECT_CODEC.encodeStart(ops,holder.value()).getOrThrow(),"feature",net.minecraft.world.level.levelgen.feature.Feature.DIRECT_CODEC.encodeStart(ops,holder.value().feature().value()).getOrThrow()));
                    for(int cz=-1;cz<=8;cz++)for(int cx=-1;cx<=8;cx++){
                        long randomSeed=seed^cx*0x9E3779B97F4A7C15L^cz*0xD6E8FEB86659FD93L^id.hashCode();new FeaturePlacer(world,nativeGenerator).placeWithBiomeCheck(holder.value(),RandomSource.create(randomSeed),new BlockPos(cx*16,0,cz*16));calls++;
                    }
                }
                var nativePositions=new TreeMap<String,Set<BlockPos>>();var candidatePositions=new TreeMap<String,Set<BlockPos>>();
                var exposedNative=new TreeMap<String,Integer>();var exposedCandidate=new TreeMap<String,Integer>();var caveField=new UndergroundStructure(seed);
                var counts=new TreeMap<String,Integer>();var depths=new TreeMap<String,Map<Integer,Integer>>();
                for(int z=0;z<128;z++)for(int x=0;x<128;x++)for(int y=-64;y<=64;y++){
                    var state=fake.state(new BlockPos(x,y,z));String type=kind(state);if(type==null)continue;counts.merge(type,1,Integer::sum);nativePositions.computeIfAbsent(type,k->new HashSet<>()).add(new BlockPos(x,y,z));for(var direction:net.minecraft.core.Direction.values())if(fake.state(new BlockPos(x,y,z).relative(direction)).isAir()){exposedNative.merge(type,1,Integer::sum);break;}depths.computeIfAbsent(type,k->new TreeMap<>()).merge(64-y,1,Integer::sum);
                }
                boolean emeraldEligible=false,goldExtra=false;
                for(var group:biome.value().getGenerationSettings().features())for(var holder:group){String id=holder.unwrapKey().orElseThrow().identifier().getPath();emeraldEligible|=id.equals("ore_emerald");goldExtra|=id.equals("ore_gold_extra");}
                var candidateCounts=new TreeMap<String,Integer>();var candidateDepths=new TreeMap<String,Map<Integer,Integer>>();
                for(int cz=0;cz<8;cz++)for(int cx=0;cx<8;cx++){
                    var ore=new OreLayer(seed,cx,cz,2,emeraldEligible?65535:0,goldExtra?65535:0).bind(64,64);
                    for(int z=0;z<16;z++)for(int x=0;x<16;x++)for(int depth=4;depth<=OreLayer.MAX_DEPTH;depth++){
                        int y=64-depth;var host=TerrainProfile.material(TerrainShape.FLOOR,TerrainShape.CEILING,y,64,Integer.MIN_VALUE,0);if(caves&&caveField.voidAt(cx*16+x,y,cz*16+z,64))host=Blocks.AIR.defaultBlockState();var state=ore.state(x,y,z,64,host,caves);String type=kind(state);if(type==null)continue;candidateCounts.merge(type,1,Integer::sum);candidatePositions.computeIfAbsent(type,k->new HashSet<>()).add(new BlockPos(cx*16+x,y,cz*16+z));for(var direction:net.minecraft.core.Direction.values()){int yy=y+direction.getStepY();if(yy>64||caves&&caveField.voidAt(cx*16+x+direction.getStepX(),yy,cz*16+z+direction.getStepZ(),64)){exposedCandidate.merge(type,1,Integer::sum);break;}}candidateDepths.computeIfAbsent(type,k->new TreeMap<>()).merge(depth,1,Integer::sum);
                    }
                }
                var row=new LinkedHashMap<String,Object>();row.put("biome",key.identifier().toString());row.put("sharedCaveHost",caves);row.put("nativeExposedOreBlocks",exposedNative);row.put("candidateExposedOreBlocks",exposedCandidate);row.put("seed",seed);row.put("nativePlacementCalls",calls);row.put("centralColumns",16384);row.put("hostSurfaceY",64);row.put("nativeMinY",-64);row.put("nativeHeight",384);row.put("nativeGeneratedOreBlocks",counts);row.put("nativeDepthHistograms",depths);row.put("candidateOreBlocks",candidateCounts);row.put("candidateDepthHistograms",candidateDepths);row.put("nativeBranchMining",mining(nativePositions,depths));row.put("candidateBranchMining",mining(candidatePositions,candidateDepths));data.add(row);
            }
            var path=Path.of(System.getProperty("blockbranch.oreReferenceResult"));Files.writeString(path.resolveSibling("native-reference.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("minecraft","26.3","scope","Controlled flat ground64 native placed ore features; native -64..320 anchors, solid and shared seeded cave hosts compared; noise large veins/structures excluded. Neighbor chunks are virtual, all writes stay in the fixture map.","placedFeatures",features,"cases",data)));
            Files.writeString(path,"PASS native ore-placement reference collected\n");
        }catch(Throwable error){Blockbranch.LOGGER.error("ORE_REFERENCE_FAIL",error);try{Files.writeString(Path.of(System.getProperty("blockbranch.oreReferenceResult")),"FAIL "+error);}catch(Exception ignored){}}finally{server.halt(false);}
    });}
    // Fixed disjoint 112m two-block-high lanes at z=8,16,...120. Count ore
    // in the tunnel or touching its sides/floor/ceiling, without vein chaining.
    // Each arm samples all 16 alternative depths in its densest band; this is a yield
    // proxy per 3360 planned tunnel positions, never a playtime measurement.
    private static Map<String,Object> mining(Map<String,Set<BlockPos>> positions,Map<String,Map<Integer,Integer>> histograms){
        var result=new TreeMap<String,Object>();
        for(var entry:positions.entrySet()){
            var h=histograms.get(entry.getKey());int best=0,band=0;
            for(int depth=0;depth<=208;depth++){int sum=0;for(int d=depth;d<depth+16;d++)sum+=h.getOrDefault(d,0);if(sum>best){best=sum;band=depth;}}
            var levels=new ArrayList<Map<String,Integer>>();
            for(int depth=band;depth<band+16;depth++){
                int feet=64-depth,found=0;
                for(var p:entry.getValue()){
                    if(p.getX()<8||p.getX()>119)continue;
                    for(int lane=8;lane<=120;lane+=8)if(p.getZ()==lane&&p.getY()>=feet-1&&p.getY()<=feet+2||Math.abs(p.getZ()-lane)==1&&p.getY()>=feet&&p.getY()<=feet+1){found++;break;}
                }
                levels.add(Map.of("feetLocalDepth",depth,"accessibleOreBlocks",found));
            }
            result.put(entry.getKey(),Map.of("bestBandStartDepth",band,"plannedTunnelPositionsPerAlternativeLevel",3360,"alternativeLevels",levels));
        }
        return result;
    }
    private static boolean resource(String path){for(String type:TYPES)if(path.equals("ore_"+type)||path.startsWith("ore_"+type+"_"))return true;return false;}
    static String kind(BlockState state){for(var k:OreLayer.Kind.values())if(state==k.stone||state==k.deepslate)return k.name().toLowerCase(Locale.ROOT);return null;}
    private static final class Host implements InvocationHandler {
        final ServerLevel level;final Holder<Biome> biome;final long seed;final boolean caves;final UndergroundStructure field;final TerrainProfile base;final Map<Long,ChunkAccess> chunks=new HashMap<>();
        Host(ServerLevel level,Holder<Biome> biome,long seed,boolean caves){this.caves=caves;this.field=new UndergroundStructure(seed);this.level=level;this.biome=biome;this.seed=seed;int[] ground=new int[256],water=new int[256];Arrays.fill(ground,64);Arrays.fill(water,Integer.MIN_VALUE);var factory=level.palettedContainerFactory();base=new TerrainProfile(-64,320,ground,water,new PalettedContainer<>(biome,factory.biomeStrategy()),factory.blockStatesStrategy());}
        WorldGenLevel proxy(){return(WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class<?>[]{WorldGenLevel.class},this);}
        ChunkAccess chunk(int x,int z){if(Math.abs(x)>16||Math.abs(z)>16)throw new AssertionError("Reference escaped bounded chunk map");long key=(x&0xffffffffL)|((long)z<<32);return chunks.computeIfAbsent(key,k->{var c=new ProtoChunk(new ChunkPos(x,z),UpgradeData.EMPTY,level,level.palettedContainerFactory(),null);((SparseColumn)c).blockbranch$terrain(new ColumnTerrain(c,base,level.palettedContainerFactory(),null,new int[0]));if(caves)for(int sy=-4;sy<=3;sy++){var section=((SparseColumn)c).blockbranch$write(sy);for(int ly=0;ly<16;ly++)for(int zz=0;zz<16;zz++)for(int xx=0;xx<16;xx++){int yy=(sy<<4)+ly;var state=section.getBlockState(xx,ly,zz);if((state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE))&&field.voidAt(x*16+xx,yy,z*16+zz,64))section.setBlockState(xx,ly,zz,Blocks.AIR.defaultBlockState());}}return c;});}
        BlockState state(BlockPos p){if(p.getY()< -64||p.getY()>=320)return Blocks.VOID_AIR.defaultBlockState();return chunk(p.getX()>>4,p.getZ()>>4).getBlockState(p);}
        @Override public Object invoke(Object proxy,Method method,Object[] args)throws Throwable {
            String n=method.getName();
            if(n.equals("getHeight"))return args==null||args.length==0?384:65;
            if(n.equals("getHeightmapPos")){var p=(BlockPos)args[1];return new BlockPos(p.getX(),65,p.getZ());}if(n.equals("getMinY"))return -64;if(n.equals("getSeed"))return seed;
            if(n.equals("getBlockState"))return state((BlockPos)args[0]);if(n.equals("getFluidState"))return state((BlockPos)args[0]).getFluidState();
            if(n.equals("getBiome"))return biome;
            if(n.equals("getChunk")){if(args[0] instanceof BlockPos p)return chunk(p.getX()>>4,p.getZ()>>4);return chunk((Integer)args[0],(Integer)args[1]);}
            if(n.equals("ensureCanWrite")){var p=(BlockPos)args[0];return p.getY()>=-64&&p.getY()<320;}
            if(n.equals("setBlock")){var p=(BlockPos)args[0];chunk(p.getX()>>4,p.getZ()>>4).setBlockState(p,(BlockState)args[1],0);return true;}
            if(method.isDefault())return InvocationHandler.invokeDefault(proxy,method,args==null?new Object[0]:args);
            try{return method.invoke(level,args);}catch(InvocationTargetException error){throw error.getCause();}
        }
    }
}
