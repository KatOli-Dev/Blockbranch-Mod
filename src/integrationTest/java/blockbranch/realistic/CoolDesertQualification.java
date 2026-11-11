package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.google.gson.*;
import com.mojang.serialization.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.*;

/** Original published259 output oracle, codec/save gates, native cover and finite edges. */
public final class CoolDesertQualification {
    private static final int[][] POINTS={{400000,-1200000},{656000,1264000}};
    public static void register(){ServerLifecycleEvents.SERVER_STARTED.register(server->{
        Path result=Path.of(System.getProperty("blockbranch.coolDesertResult"));
        try{
            var level=server.overworld();var random=level.getChunkSource().randomState();var ops=RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
            JsonObject fresh;try(var in=new InputStreamReader(Objects.requireNonNull(CoolDesertQualification.class.getResourceAsStream("/data/blockbranch/worldgen/world_preset/realistic.json")),StandardCharsets.UTF_8)){
                fresh=JsonParser.parseReader(in).getAsJsonObject().getAsJsonObject("dimensions").getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").deepCopy();
            }
            if(fresh.get("shape_version").getAsInt()==264){
                require(fresh.get("summer_dry_definition").getAsInt()==1,"Known complete summer-dry wrapper");
                fresh.remove("summer_dry_definition");fresh.addProperty("shape_version",263);fresh.getAsJsonObject("additional_biomes").remove(SummerDryWoodland.KEY);
            }
            if(fresh.get("shape_version").getAsInt()==263){
                require(fresh.get("land_ice_definition").getAsInt()==1,"Known complete land-ice wrapper");
                fresh.remove("land_ice_definition");fresh.addProperty("shape_version",262);fresh.getAsJsonObject("additional_biomes").remove(LandIceField.KEY);
            }
            if(fresh.get("shape_version").getAsInt()==262){
                require(fresh.get("deep_basin_relief_definition").getAsInt()==1,"Known complete deep-basin wrapper");
                fresh.remove("deep_basin_relief_definition");fresh.addProperty("shape_version",261);
            }
            if(fresh.get("shape_version").getAsInt()==261){
                require(fresh.get("abyssal_trace_definition").getAsInt()==1,"Known complete abyssal wrapper");
                fresh.remove("abyssal_trace_definition");fresh.addProperty("shape_version",260);
            }
            fresh.remove("type");if(fresh.has("continental_ice_definition")){fresh.remove("continental_ice_definition");fresh.addProperty("shape_version",264);fresh.getAsJsonObject("additional_biomes").remove(ContinentalIceField.KEY);}require(fresh.get("shape_version").getAsInt()==260,"Reviewed current preset");
            var candidate=RealisticGenerator.CODEC.codec().parse(ops,fresh).getOrThrow();
            require(RealisticGenerator.CODEC.codec().encodeStart(ops,candidate).getOrThrow().equals(fresh),"Fresh codec exact roundtrip");
            var old=fresh.deepCopy();old.remove("cool_desert_definition");old.addProperty("shape_version",259);old.getAsJsonObject("additional_biomes").remove(CoolDesert.KEY);
            var retained=RealisticGenerator.CODEC.codec().parse(ops,old).getOrThrow();
            require(retained.coolDesertDefinition()==0&&RealisticGenerator.CODEC.codec().encodeStart(ops,retained).getOrThrow().equals(old),"Absent gate retains exact259 settings");
            for(int version:new int[]{82,257,258,259,261}){var bad=fresh.deepCopy();bad.addProperty("shape_version",version);rejected(ops,bad,"Partial/future foundation");}
            for(int definition:new int[]{-1,0,2}){var bad=fresh.deepCopy();bad.addProperty("cool_desert_definition",definition);rejected(ops,bad,"Unsupported definition");}
            for(String key:new String[]{"cool_desert_definition","dry_forest_definition","surface_ownership_definition","redwood_forest_definition","biome_transition_definition"}){var bad=fresh.deepCopy();bad.remove(key);rejected(ops,bad,"Missing complete foundation");}
            var bad=fresh.deepCopy();bad.addProperty("cool_desert_definition","invalid");rejected(ops,bad,"Malformed definition");
            bad=fresh.deepCopy();bad.getAsJsonObject("additional_biomes").remove(CoolDesert.KEY);rejected(ops,bad,"Missing holder");
            @SuppressWarnings("unchecked") var reader=(MapCodec<Object>)Class.forName("blockbranch.realistic.Published259RealisticGenerator").getField("CODEC").get(null);
            require(reader.codec().parse(ops,fresh).error().isPresent(),"Actual original259 reader rejects260");
            var published=reader.codec().parse(ops,old).getOrThrow();
            boolean legacy=Boolean.getBoolean("blockbranch.coolDesertLegacy"),reopen=Boolean.getBoolean("blockbranch.coolDesertReopen");
            JsonObject baseline;try(var in=new InputStreamReader(Objects.requireNonNull(CoolDesertQualification.class.getResourceAsStream("/blockbranch-fixtures/cool-desert-reader-259/baseline.json")),StandardCharsets.UTF_8)){baseline=JsonParser.parseReader(in).getAsJsonObject();}
            if(!legacy){baseline.remove("nativeStates");baseline.remove("nativeSHA256");}
            var snapshot=Published259Snapshot.capture(level,retained,legacy);
            require(baseline.equals(snapshot),"Disabled gate equals independently recorded complete original259 outputs: "+snapshot);
            var report=new LinkedHashMap<String,Object>();report.put("independentPublished259",snapshot);
            report.put("selector",selectors(HabitatPalette.CODEC.codec().parse(ops,old).getOrThrow(),HabitatPalette.CODEC.codec().parse(ops,fresh).getOrThrow()));
            var actual=(RealisticGenerator)level.getChunkSource().getGenerator();require(actual.coolDesertDefinition()==(legacy?0:1),"Actual saved gate retained");
            var d=actual.drainage(random);var source=(ScreeBiomeSource)actual.getBiomeSource();
            var publishedState=published.getClass().getDeclaredMethod("surfaceState",int.class,int.class,int.class,net.minecraft.world.level.levelgen.RandomState.class);publishedState.setAccessible(true);
            int columns=0,owned=0,states=0,shrubs=0,grasses=0,bare=0,pavement=0,differentGround=0,publishedComparisons=0;var scenes=new ArrayList<Object>();
            for(var p:POINTS){
                scenes.add(Map.of("x",p[0],"z",p[1],"season",CoolDesert.season(p[1],new ClimateField(random.seed()).sample(p[0],p[1],d::sample).meanTemperatureC()),"biome",source.surfaceResolver().getNoiseBiome(p[0]>>2,0,p[1]>>2).unwrapKey().orElseThrow().identifier().toString()));
                for(int z=p[1]-32;z<=p[1]+32;z++)for(int x=p[0]-32;x<=p[0]+32;x++){
                    level.getChunk(x>>4,z>>4);int ground=d.sample(x,z).height();boolean domain=!legacy&&source.coolDesertSurface().test(x,z);columns++;
                    if(domain){owned++;require(CoolDesert.eligible(x,z,d,new ClimateField(random.seed())),"Physical cold-desert eligibility");}
                    for(int y=ground-3;y<=ground+8;y++){
                        var state=level.getBlockState(new BlockPos(x,y,z));require(state.equals(actual.surfaceState(x,y,z,random)),"Native/surfaceState at "+x+","+y+","+z);states++;
                        if(legacy){require(state.equals(publishedState.invoke(published,x,y,z,random)),"Legacy explored/new surrounding columns agree with original reader");publishedComparisons++;}
                        if(domain&&y==ground){if(state.is(DesertContent.PAVEMENT))pavement++;if(!state.equals(retained.surfaceState(x,y,z,random)))differentGround++;}
                        if(domain&&y==ground+1){if(state.is(SteppeContent.SILVER_SHRUB))shrubs++;else if(state.is(Blocks.SHORT_DRY_GRASS))grasses++;else if(state.isAir())bare++;require(!state.is(Blocks.CACTUS),"No warm-desert cactus");}
                    }
                }
            }
            if(!legacy){require(owned>1000&&shrubs>owned*.003&&shrubs<owned*.08&&grasses<shrubs,"Native sparse low shrubs rather than sward");require(bare>owned*.85&&pavement>owned*.10&&differentGround>owned*.50,"Distinct open mineral/soil mosaic rather than renamed fallback");report.put("boundaryAndBaseAPIs",boundary(level,actual));}
            var p=Published259Snapshot.POINTS[0];int ex=p[0]+1,ez=p[1]+1;var edit=new BlockPos(ex,d.sample(ex,ez).height()-2,ez);
            if(legacy||reopen)require(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Actual saved edit survives relaunch");else level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),3);
            report.put("actualLegacyWorld",legacy);report.put("columns",columns);report.put("ownedColumns",owned);report.put("nativeStates",states);report.put("shrubs",shrubs);report.put("shortGrasses",grasses);report.put("bareAboveGround",bare);report.put("pavement",pavement);report.put("differentGroundFrom259",differentGround);report.put("originalReaderPhysicalComparisons",publishedComparisons);report.put("scenes",scenes);
            server.saveEverything(false,true,true);Files.writeString(result.resolveSibling(result.getFileName()+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
            Files.writeString(result,"PASS original259 output hashes; exact saved gates/edit; original reader; fallback-only selector; native sparse stony desert and edge/base APIs\n");
            Blockbranch.LOGGER.info("COOL_DESERT_NATIVE_PASS {}",report);
        }catch(Throwable failure){Blockbranch.LOGGER.error("COOL_DESERT_NATIVE_FAIL",failure);try{Files.writeString(result,"FAIL "+failure+"\n");}catch(Exception ignored){}}
        finally{server.halt(false);}
    });}
    private static Map<String,Object> selectors(HabitatPalette oldPalette,HabitatPalette palette)throws Exception{
        int changed=0;var seeds=new ArrayList<Object>();var type=Class.forName("blockbranch.realistic.Published259ScreeBiomeSource");
        var bind=type.getDeclaredMethod("bind",long.class,DrainageNetwork.class,ClimateField.class);bind.setAccessible(true);
        for(long seed:new long[]{424242,1,-17}){
            var d=new DrainageNetwork(new TerrainShape(seed),seed,true,true,true,true);var c=new ClimateField(seed);
            var m=oldPalette.margins();var sentinel=Holder.direct(m.fallback().value());
            var tagged=new HabitatPalette(new HabitatPalette.Margins(sentinel,m.scree(),m.reedbed(),m.riverMargin(),m.sandyShore(),m.lake(),m.riverChannel(),m.shelf(),m.deepOcean()),oldPalette.land(),oldPalette.additional());
            var original=type.getConstructor(HabitatPalette.class,boolean.class,boolean.class).newInstance(tagged,true,true);
            for(String name:new String[]{"withRedwoodForests","withSurfaceOwnership","withDryForests"}){var method=type.getDeclaredMethod(name,boolean.class);method.setAccessible(true);method.invoke(original,true);}
            bind.invoke(original,seed,d,c);var before=(BiomeResolver)type.getMethod("surfaceResolver").invoke(original);
            var now=new ScreeBiomeSource(palette,true,true).withRedwoodForests(true).withSurfaceOwnership(true).withDryForests(true).withCoolDeserts(true);now.bind(seed,d,c);int count=0;
            for(int z=-1904000;z<1920000;z+=32000)for(int x=-1904000;x<1920000;x+=32000){
                var a=before.getNoiseBiome(x>>2,0,z>>2);var b=now.surfaceResolver().getNoiseBiome(x>>2,0,z>>2);
                if(a==sentinel&&b==palette.margins().fallback())continue;
                if(a!=b){require(a==sentinel&&b==palette.additional(CoolDesert.KEY)&&CoolDesert.eligible(x,z,d,c),"Only supported cool/arid final fallback can change: "+seed+" "+x+","+z);count++;}
            }
            require(count>0,"Nonvacuous fixed-seed subtype");changed+=count;seeds.add(Map.of("seed",seed,"samples",14400,"fallbackToCoolDesert",count));
        }
        return Map.of("samples",43200,"fallbackToCoolDesert",changed,"unrelatedOwnerChanges",0,"seeds",seeds);
    }
    private static Map<String,Object> boundary(net.minecraft.server.level.ServerLevel level,RealisticGenerator g){
        var random=level.getChunkSource().randomState();var d=g.drainage(random);var owner=((ScreeBiomeSource)g.getBiomeSource()).coolDesertSurface();
        int[] edge=null;int discoveryPoints=0;
        search:for(var center:POINTS)for(int z=center[1]-512;z<=center[1]+512;z+=16)for(int x=center[0]-512;x<=center[0]+512;x+=16){
            discoveryPoints++;if(!owner.test(x,z))continue;
            for(var direction:new int[][]{{16,0},{0,16},{-16,0},{0,-16}}){
                int bx=x+direction[0],bz=z+direction[1];
                if(Math.abs(bx-center[0])>512||Math.abs(bz-center[1])>512||owner.test(bx,bz))continue;
                int ax=x,az=z;
                while(Math.abs(ax-bx)+Math.abs(az-bz)>1){int mx=Math.floorDiv(ax+bx,2),mz=Math.floorDiv(az+bz,2);if(owner.test(mx,mz)){ax=mx;az=mz;}else{bx=mx;bz=mz;}}
                if(d.sample(ax,az).waterLevel()!=DrainageNetwork.DRY||d.sample(bx,bz).waterLevel()!=DrainageNetwork.DRY)continue;
                edge=new int[]{ax,az,bx,bz};break search;
            }
        }
        require(edge!=null,"Cool-desert edge must occur inside two fixed ±512-block candidate neighborhoods; never expand search");
        require(owner.test(edge[0],edge[1])&&!owner.test(edge[2],edge[3]),"Adjacent covered/uncovered edge witness");
        int columns=0,covered=0,uncovered=0,surfaceStates=0;var api=new LinkedHashSet<Long>();
        api.add((edge[0]&0xffffffffL)|((long)edge[1]<<32));api.add((edge[2]&0xffffffffL)|((long)edge[3]<<32));
        var chunks=new HashSet<Long>();
        for(int z=edge[1]-16;z<=edge[1]+16;z++)for(int x=edge[0]-16;x<=edge[0]+16;x++){
            var chunk=level.getChunk(x>>4,z>>4);chunks.add(((long)(x>>4)<<32)|((z>>4)&0xffffffffL));
            if(owner.test(x,z))covered++;else uncovered++;
            int ground=d.sample(x,z).height();
            for(int y=ground-3;y<=ground+40;y++){require(level.getBlockState(new BlockPos(x,y,z)).equals(g.surfaceState(x,y,z,random)),"Native/analytic mixed edge at "+x+","+y+","+z);surfaceStates++;}
            if(((x-edge[0])&3)==0&&((z-edge[1])&3)==0)api.add((x&0xffffffffL)|((long)z<<32));
            columns++;
        }
        require(covered>0&&uncovered>0,"Nonvacuous mixed native window");
        int apiCovered=0,apiUncovered=0,columnStates=0,heights=0;
        for(long key:api){
            int x=(int)key,z=(int)(key>>32),ground=d.sample(x,z).height();var chunk=level.getChunk(x>>4,z>>4);
            if(owner.test(x,z))apiCovered++;else apiUncovered++;
            for(var bounds:List.of((net.minecraft.world.level.LevelHeightAccessor)level,
                net.minecraft.world.level.LevelHeightAccessor.create(ground-3,3),
                net.minecraft.world.level.LevelHeightAccessor.create(ground+1,3),
                net.minecraft.world.level.LevelHeightAccessor.create(ground+8,8))){
                var column=g.getBaseColumn(x,z,bounds,random);
                for(int y=Math.max(ground-3,bounds.getMinY());y<=Math.min(ground+40,bounds.getMaxY());y++){
                    require(column.getBlock(y).equals(level.getBlockState(new BlockPos(x,y,z))),"Base-column/native changed surface contract at "+x+","+y+","+z);columnStates++;
                }
                for(var type:net.minecraft.world.level.levelgen.Heightmap.Types.values()){
                    int expected=bounds.getMinY();
                    if(bounds==level)expected=chunk.getHeight(type,x&15,z&15)+1;
                    else for(int y=bounds.getMaxY();y>=bounds.getMinY();y--)if(type.isOpaque().test(level.getBlockState(new BlockPos(x,y,z)))){expected=y+1;break;}
                    int actual=g.getBaseHeight(x,z,type,bounds,random);
                    require(actual==expected,"Base-height/native or clipped native predicate "+type+" at "+x+","+z+" min/max="+bounds.getMinY()+"/"+bounds.getMaxY()+" actual="+actual+" expected="+expected);heights++;
                }
            }
        }
        require(apiCovered>0&&apiUncovered>0,"Direct base APIs exercise both ownership branches");
        var report=new LinkedHashMap<String,Object>();report.put("seed",random.seed());report.put("candidateCenters",POINTS);
        report.put("discoveryInclusiveRadiusBlocks",512);report.put("discoveryStrideBlocks",16);report.put("discoveryPoints",discoveryPoints);report.put("adjacentCoveredUncoveredXZ",edge);
        report.put("nativeInclusiveRadiusBlocks",16);report.put("nativeStrideBlocks",1);report.put("nativeChunks",chunks.size());report.put("nativeColumns",columns);
        report.put("coveredColumns",covered);report.put("uncoveredColumns",uncovered);report.put("nativeSurfaceStates",surfaceStates);report.put("surfaceYBounds","local ground -3 through +40 inclusive");
        report.put("apiStrideBlocks",4);report.put("apiCoveredColumns",apiCovered);report.put("apiUncoveredColumns",apiUncovered);report.put("baseColumnNativeStates",columnStates);report.put("baseHeightNativeOrClippedComparisons",heights);
        report.put("apiBounds",List.of(List.of(level.getMinY(),level.getMaxY()),"[ground-3,ground-1]","[ground+1,ground+3]","[ground+8,ground+15]"));return report;
    }
    private static void rejected(RegistryOps<JsonElement> ops,JsonObject json,String message){require(RealisticGenerator.CODEC.codec().parse(ops,json).error().isPresent(),message);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private CoolDesertQualification(){}
}
